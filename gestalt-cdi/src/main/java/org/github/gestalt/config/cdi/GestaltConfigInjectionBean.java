/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.github.gestalt.config.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Provider;

import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.GestaltCore;
import org.github.gestalt.config.exceptions.GestaltException;
import org.github.gestalt.config.reflect.TypeCapture;

/**
 * Based on https://github.com/smallrye/smallrye-config/tree/3.1.1/cdi
 *
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 * @author Colin Redmond (c) 2023.
 */
public class GestaltConfigInjectionBean<T> implements Bean<T>, PassivationCapable {

    private static final Set<Annotation> QUALIFIERS = new HashSet<>();

    static {
        QUALIFIERS.add(new GestaltConfigLiteral());
    }

    private final BeanManager bm;
    private final Class<?> clazz;

    /**
     * only access via {@link #getConfig()}.
     */
    private Gestalt config;

    public GestaltConfigInjectionBean(BeanManager bm, Class<?> clazz) {
        this.bm = bm;
        this.clazz = clazz;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public Class<?> getBeanClass() {
        return GestaltConfigInjectionBean.class;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T create(CreationalContext<T> context) {
        InjectionPoint ip = (InjectionPoint) bm.getInjectableReference(new MetadataInjectionPoint(), context);
        Annotated annotated = ip.getAnnotated();
        GestaltConfig configProperty = annotated.getAnnotation(GestaltConfig.class);
        String key = GestaltConfigProducerUtil.getConfigKey(ip, configProperty);
        String defaultValue = configProperty.defaultValue();

        try {
            if (annotated.getBaseType() instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) annotated.getBaseType();
                Type rawType = paramType.getRawType();

                // handle Provider<T> and Instance<T>
                if (rawType instanceof Class &&
                    (((Class<?>) rawType).isAssignableFrom(Provider.class) ||
                        ((Class<?>) rawType).isAssignableFrom(Instance.class)) &&
                    paramType.getActualTypeArguments().length == 1) {
                    Class<?> paramTypeClass = (Class<?>) paramType.getActualTypeArguments()[0];
                    return (T) getConfig().getConfig(key, paramTypeClass);
                }
            } else {
                Class<?> annotatedTypeClass = (Class<?>) annotated.getBaseType();
                if (defaultValue.length() == 0) {
                    return (T) getConfig().getConfig(key, annotatedTypeClass);
                } else {
                    Optional<T> optionalValue = (Optional<T>) getConfig().getConfigOptional(key, annotatedTypeClass);
                    return optionalValue.orElseGet(
                        () -> (T) ((GestaltCore) getConfig()).getDecoderService()
                                                             .decodeNode(key, defaultValue, TypeCapture.of(annotatedTypeClass))
                    );
                }
            }
        } catch (GestaltException e) {
            throw new GestaltConfigException("Unable to get configuration for ", key, e);
        }

        throw new GestaltConfigException("Unknown configuration ", key);
    }

    public Gestalt getConfig() {
        if (config == null) {
            config = GestaltConfigProvider.getGestaltConfig();
        }
        return config;
    }

    @Override
    public void destroy(T instance, CreationalContext<T> context) {

    }

    @Override
    public Set<Type> getTypes() {
        return Collections.singleton(clazz);
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return QUALIFIERS;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public String getName() {
        return "ConfigInjectionBean_" + clazz;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public String getId() {
        return "ConfigInjectionBean_" + clazz;
    }

    private static class GestaltConfigLiteral extends AnnotationLiteral<GestaltConfig> implements GestaltConfig {

        @Override
        public String path() {
            return "";
        }

        @Override
        public String defaultValue() {
            return "";
        }
    }
}