package org.github.gestalt.config.processor.config.annotation;

import org.github.gestalt.config.metadata.IsSecretMetadata;
import org.github.gestalt.config.metadata.MetaDataValue;

import java.util.List;
import java.util.Map;

public class SecretAnnotationMetadataTransform implements AnnotationMetadataTransform {
    @Override
    public String name() {
        return "secret";
    }

    @Override
    public Map<String, List<MetaDataValue<?>>> annotationTransform(String name, String parameter) {
        boolean value = true;
        if (parameter != null && !parameter.isEmpty()) {
            value = Boolean.parseBoolean(parameter);
        }

        return Map.of(IsSecretMetadata.SECRET, List.of(new IsSecretMetadata(value)));
    }
}