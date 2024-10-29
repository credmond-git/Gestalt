package org.github.gestalt.config.processor.config.annotation;

import org.github.gestalt.config.metadata.IsSecretMetadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SecretAnnotationMetadataTransformTest {

    @Test
    public void getName() {
        SecretAnnotationMetadataTransform secretAnnotationMetadataTransform = new SecretAnnotationMetadataTransform();

        Assertions.assertEquals("secret", secretAnnotationMetadataTransform.name());
    }

    @Test
    public void transformTrueDefault() {
        SecretAnnotationMetadataTransform secretAnnotationMetadataTransform = new SecretAnnotationMetadataTransform();
        var results = secretAnnotationMetadataTransform.annotationTransform("secret", "");

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(1, results.get(IsSecretMetadata.SECRET).size());
        Assertions.assertEquals(true, results.get(IsSecretMetadata.SECRET).get(0).getMetadata());
    }

    @Test
    public void transformTrue() {
        SecretAnnotationMetadataTransform secretAnnotationMetadataTransform = new SecretAnnotationMetadataTransform();
        var results = secretAnnotationMetadataTransform.annotationTransform("secret", "true");

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(1, results.get(IsSecretMetadata.SECRET).size());
        Assertions.assertEquals(true, results.get(IsSecretMetadata.SECRET).get(0).getMetadata());
    }


    @Test
    public void transformFalseType() {
        SecretAnnotationMetadataTransform secretAnnotationMetadataTransform = new SecretAnnotationMetadataTransform();
        var results = secretAnnotationMetadataTransform.annotationTransform("secret", "false");

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(1, results.get(IsSecretMetadata.SECRET).size());
        Assertions.assertEquals(false, results.get(IsSecretMetadata.SECRET).get(0).getMetadata());
    }
}
