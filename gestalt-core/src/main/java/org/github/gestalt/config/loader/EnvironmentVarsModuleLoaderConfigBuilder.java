package org.github.gestalt.config.loader;

import org.github.gestalt.config.lexer.SentenceLexer;
import org.github.gestalt.config.parser.ConfigParser;

/**
 * Gestalt module config for the Environment Variable Module builder.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2024.
 */
public final class EnvironmentVarsModuleLoaderConfigBuilder {
    private ConfigParser parser;
    private SentenceLexer lexer;

    private EnvironmentVarsModuleLoaderConfigBuilder() {
    }

    public static EnvironmentVarsModuleLoaderConfigBuilder builder() {
        return new EnvironmentVarsModuleLoaderConfigBuilder();
    }

    /**
     * Set the Config Parse to use with the Environment Variable module.
     * Used to converts the Environment Variables to a config tree.
     *
     * @param parser the Config Parse
     * @return Config Parse
     */
    public EnvironmentVarsModuleLoaderConfigBuilder setConfigParser(ConfigParser parser) {
        this.parser = parser;
        return this;
    }

    /**
     * set the SentenceLexer for the Environment Variable module, used to build paths into tokens.
     *
     * @param lexer SentenceLexer for the Environment Variable module.
     * @return the SentenceLexer
     */
    public EnvironmentVarsModuleLoaderConfigBuilder setLexer(SentenceLexer lexer) {
        this.lexer = lexer;
        return this;
    }

    public EnvironmentVarsLoaderModuleConfig build() {
        return new EnvironmentVarsLoaderModuleConfig(parser, lexer);
    }
}
