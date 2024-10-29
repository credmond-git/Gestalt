package org.github.gestalt.config.processor.config.annotation;

import org.github.gestalt.config.metadata.IsNoCacheMetadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class NoCacheAnnotationMetadataTransformTest {
    @Test
    public void getName() {
        NoCacheAnnotationMetadataTransform transform = new NoCacheAnnotationMetadataTransform();

        Assertions.assertEquals("nocache", transform.name());
    }

    @Test
    public void transformTrueDefault() {
        NoCacheAnnotationMetadataTransform transform = new NoCacheAnnotationMetadataTransform();
        var results = transform.annotationTransform("nocache", "");

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(1, results.get(IsNoCacheMetadata.NO_CACHE).size());
        Assertions.assertEquals(true, results.get(IsNoCacheMetadata.NO_CACHE).get(0).getMetadata());
    }

    @Test
    public void transformTrue() {
        NoCacheAnnotationMetadataTransform transform = new NoCacheAnnotationMetadataTransform();
        var results = transform.annotationTransform("nocache", "true");

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(1, results.get(IsNoCacheMetadata.NO_CACHE).size());
        Assertions.assertEquals(true, results.get(IsNoCacheMetadata.NO_CACHE).get(0).getMetadata());
    }


    @Test
    public void transformFalseType() {
        NoCacheAnnotationMetadataTransform transform = new NoCacheAnnotationMetadataTransform();
        var results = transform.annotationTransform("nocache", "false");

        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(1, results.get(IsNoCacheMetadata.NO_CACHE).size());
        Assertions.assertEquals(false, results.get(IsNoCacheMetadata.NO_CACHE).get(0).getMetadata());
    }
}
