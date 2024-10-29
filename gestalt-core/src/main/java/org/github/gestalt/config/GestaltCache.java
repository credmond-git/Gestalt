package org.github.gestalt.config;

import org.github.gestalt.config.decoder.DecoderContext;
import org.github.gestalt.config.decoder.DecoderService;
import org.github.gestalt.config.entity.GestaltConfig;
import org.github.gestalt.config.exceptions.GestaltException;
import org.github.gestalt.config.node.TagMergingStrategy;
import org.github.gestalt.config.observations.ObservationService;
import org.github.gestalt.config.reflect.TypeCapture;
import org.github.gestalt.config.reload.CoreReloadListener;
import org.github.gestalt.config.secret.rules.SecretChecker;
import org.github.gestalt.config.tag.Tags;
import org.github.gestalt.config.utils.GResultOf;
import org.github.gestalt.config.utils.Triple;

import java.util.*;

/**
 * A cache layer that stores configurations by path and type.
 * Expects to be registered as a CoreReloadListener and will clear the configs
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2024.
 */
@SuppressWarnings("OverloadMethodsDeclarationOrder")
public class GestaltCache implements Gestalt, CoreReloadListener {
    private final Gestalt delegate;
    private final Map<Triple<String, TypeCapture<?>, Tags>, GResultOf<Object>> cache = Collections.synchronizedMap(new HashMap<>());
    private final Tags defaultTags;
    private final ObservationService observationService;
    private final GestaltConfig gestaltConfig;
    private final TagMergingStrategy tagMergingStrategy;
    private final List<SecretChecker> nonCacheableSecrets;

    /**
     * Constructor for the GestaltCache that accepts a delegate.
     *
     * @param delegate           real Gestalt to call for configs to cache.
     * @param defaultTags        Default set of tags to apply to all calls to get a configuration where tags are not provided.
     * @param observationService Observations service for submitting Observations
     * @param gestaltConfig      Gestalt Configuration
     * @param tagMergingStrategy The strategy to merge tags
     * @param nonCacheableSecrets secrets that we should not be caching.
     */
    public GestaltCache(Gestalt delegate, Tags defaultTags, ObservationService observationService, GestaltConfig gestaltConfig,
                        TagMergingStrategy tagMergingStrategy, List<SecretChecker> nonCacheableSecrets) {
        Objects.requireNonNull(tagMergingStrategy);
        this.delegate = delegate;
        this.defaultTags = defaultTags;
        this.observationService = observationService;
        this.gestaltConfig = gestaltConfig;
        this.tagMergingStrategy = tagMergingStrategy;
        this.nonCacheableSecrets = nonCacheableSecrets;
    }

    @Override
    public void loadConfigs() throws GestaltException {
        delegate.loadConfigs();
        cache.clear();
    }

    @Override
    public <T> T getConfig(String path, Class<T> klass) throws GestaltException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(klass);

        TypeCapture<T> typeCapture = TypeCapture.of(klass);
        return getConfigInternal(path, typeCapture, null).results();
    }

    @Override
    public <T> T getConfig(String path, Class<T> klass, Tags tags) throws GestaltException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(klass);
        Objects.requireNonNull(tags);

        TypeCapture<T> typeCapture = TypeCapture.of(klass);
        return getConfigInternal(path, typeCapture, tags).results();
    }

    @Override
    public <T> T getConfig(String path, TypeCapture<T> klass) throws GestaltException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(klass);

        return getConfigInternal(path, klass, null).results();
    }

    @Override
    public <T> T getConfig(String path, TypeCapture<T> klass, Tags tags) throws GestaltException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(klass);
        Objects.requireNonNull(tags);

        return getConfigInternal(path, klass, tags).results();
    }

    @Override
    public <T> GResultOf<T> getConfigResult(String path, TypeCapture<T> klass, Tags tags) throws GestaltException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(klass);
        Objects.requireNonNull(tags);

        return getConfigInternal(path, klass, tags);
    }

    @SuppressWarnings("unchecked")
    private <T> GResultOf<T> getConfigInternal(String path, TypeCapture<T> klass, Tags tags) throws GestaltException {

        Tags resolvedTags = tagMergingStrategy.mergeTags(tags, defaultTags);
        Triple<String, TypeCapture<?>, Tags> key = new Triple<>(path, klass, resolvedTags);
        if (cache.get(key) != null) {
            if (gestaltConfig.isObservationsEnabled() && observationService != null) {
                observationService.recordObservation("cache.hit", 1, Tags.of());
            }
            return (GResultOf<T>) cache.get(key);
        } else {
            GResultOf<T> result = delegate.getConfigResult(path, klass, resolvedTags);
            updateCache(path, key, result);
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void updateCache(String path, Triple<String, TypeCapture<?>, Tags> key, GResultOf<T> result) {
        if (shouldCacheValue(path)) {
            cache.put(key, (GResultOf<Object>) result);
        }
    }

    @Override
    public <T> T getConfig(String path, T defaultVal, Class<T> klass) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(klass);

        TypeCapture<T> typeCapture = TypeCapture.of(klass);
        return getConfigInternal(path, defaultVal, typeCapture, null).results();
    }

    @Override
    public <T> T getConfig(String path, T defaultVal, Class<T> klass, Tags tags) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(klass);
        Objects.requireNonNull(tags);

        TypeCapture<T> typeCapture = TypeCapture.of(klass);
        return getConfigInternal(path, defaultVal, typeCapture, tags).results();
    }


    @Override

    public <T> T getConfig(String path, T defaultVal, TypeCapture<T> klass) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(klass);

        return getConfigInternal(path, defaultVal, klass, null).results();
    }

    @Override
    public <T> T getConfig(String path, T defaultVal, TypeCapture<T> klass, Tags tags) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(klass);
        Objects.requireNonNull(tags);

        return getConfigInternal(path, defaultVal, klass, tags).results();
    }

    @SuppressWarnings("unchecked")
    private <T> GResultOf<T> getConfigInternal(String path, T defaultVal, TypeCapture<T> klass, Tags tags) {
        Tags resolvedTags = tagMergingStrategy.mergeTags(tags, defaultTags);
        Triple<String, TypeCapture<?>, Tags> key = new Triple<>(path, klass, resolvedTags);
        if (cache.containsKey(key)) {
            GResultOf<T> result = (GResultOf<T>) cache.get(key);
            if (result == null) {
                result = GResultOf.result(defaultVal);
            }

            if (gestaltConfig.isObservationsEnabled() && observationService != null) {
                observationService.recordObservation("cache.hit", 1, Tags.of());
            }

            return result;

        } else {
            Optional<GResultOf<T>> resultOptional = delegate.getConfigOptionalResult(path, klass, resolvedTags);
            GResultOf<T> result = resultOptional.orElse(null);
            updateCache(path, key, result);
            if (result != null) {
                return result;
            } else {
                return GResultOf.result(defaultVal);
            }
        }
    }

    @Override
    public <T> Optional<T> getConfigOptional(String path, Class<T> klass) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(klass);

        TypeCapture<T> typeCapture = TypeCapture.of(klass);
        return getConfigOptionalInternal(path, typeCapture, null).map(GResultOf::results);
    }

    @Override
    public <T> Optional<T> getConfigOptional(String path, Class<T> klass, Tags tags) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(klass);
        Objects.requireNonNull(tags);

        TypeCapture<T> typeCapture = TypeCapture.of(klass);
        return getConfigOptionalInternal(path, typeCapture, tags).map(GResultOf::results);
    }

    @Override
    public <T> Optional<T> getConfigOptional(String path, TypeCapture<T> klass) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(klass);

        return getConfigOptionalInternal(path, klass, null).map(GResultOf::results);
    }

    @Override
    public <T> Optional<T> getConfigOptional(String path, TypeCapture<T> klass, Tags tags) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(klass);
        Objects.requireNonNull(tags);

        return getConfigOptionalInternal(path, klass, tags).map(GResultOf::results);
    }

    @Override
    public <T> Optional<GResultOf<T>> getConfigOptionalResult(String path, TypeCapture<T> klass, Tags tags) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(klass);
        Objects.requireNonNull(tags);

        return getConfigOptionalInternal(path, klass, tags);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<GResultOf<T>> getConfigOptionalInternal(String path, TypeCapture<T> klass, Tags tags) {

        Tags resolvedTags = tagMergingStrategy.mergeTags(tags, defaultTags);
        Triple<String, TypeCapture<?>, Tags> key = new Triple<>(path, klass, resolvedTags);
        if (cache.containsKey(key)) {
            if (gestaltConfig.isObservationsEnabled() && observationService != null) {
                observationService.recordObservation("cache.hit", 1, Tags.of());
            }

            GResultOf<T> result = (GResultOf<T>) cache.get(key);
            return Optional.ofNullable(result);
        } else {
            Optional<GResultOf<T>> resultOptional = delegate.getConfigOptionalResult(path, klass, resolvedTags);
            GResultOf<T> result = resultOptional.orElse(null);
            updateCache(path, key, result);
            return Optional.ofNullable(result);
        }
    }

    private boolean shouldCacheValue(String path) {
        return nonCacheableSecrets.stream().noneMatch(it -> it.isSecret(path));
    }

    @Override
    public void registerListener(CoreReloadListener listener) {
        delegate.registerListener(listener);
    }

    @Override
    public void removeListener(CoreReloadListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    public String debugPrint(Tags tags) {
        return delegate.debugPrint(tags);
    }

    @Override
    public String debugPrint() {
        return delegate.debugPrint();
    }

    @Override
    public void reload() {
        cache.clear();
    }

    /**
     * Get the delegate for the cache.
     *
     * @return the delegate for the cache
     */
    public Gestalt getDelegate() {
        return delegate;
    }

    @Override
    public DecoderService getDecoderService() {
        return delegate.getDecoderService();
    }

    @Override
    public DecoderContext getDecoderContext() {
        return delegate.getDecoderContext();
    }
}
