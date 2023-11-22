package org.github.gestalt.config.source;

import org.github.gestalt.config.builder.SourceBuilder;
import org.github.gestalt.config.exceptions.GestaltException;

/**
 * ConfigSourceBuilder for the URL Config Source.
 *
 * <p>Create a URLConfigSource to load a config from a URL.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2023.
 */
public final class URLConfigSourceBuilder extends SourceBuilder<URLConfigSourceBuilder, URLConfigSource> {

    private String sourceURL;

    /**
     * private constructor, use the builder method.
     */
    private URLConfigSourceBuilder() {

    }

    /**
     * Static function to create the builder.
     *
     * @return the builder
     */
    public static URLConfigSourceBuilder builder() {
        return new URLConfigSourceBuilder();
    }

    /**
     * Get the URL to find the config source at.
     *
     * @return the URL to find the config source at.
     */
    public String getSourceURL() {
        return sourceURL;
    }

    /**
     * Set the URL to find the config source at.
     *
     * @param sourceURL the URL to find the config source at.
     */
    public void setSourceURL(String sourceURL) {
        this.sourceURL = sourceURL;
    }

    @Override
    public ConfigSourcePackage<URLConfigSource> build() throws GestaltException {
        return buildPackage(new URLConfigSource(sourceURL, tags));
    }
}
