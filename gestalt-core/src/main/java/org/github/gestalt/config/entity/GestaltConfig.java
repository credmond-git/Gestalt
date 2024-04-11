package org.github.gestalt.config.entity;

import org.github.gestalt.config.decoder.ProxyDecoderMode;
import org.github.gestalt.config.lexer.PathLexer;
import org.github.gestalt.config.lexer.SentenceLexer;
import org.github.gestalt.config.post.process.transform.TransformerPostProcessor;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Gestalt.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2024.
 */
public class GestaltConfig {

    @SuppressWarnings("rawtypes")
    private final Map<Class, GestaltModuleConfig> modulesConfig = new HashMap<>();
    // Treat all warnings as errors
    private boolean treatWarningsAsErrors = false;
    // Treat missing array index's as errors. If false it will inject null values for missing array index's.
    private boolean treatMissingArrayIndexAsError = true;
    // Treat missing object values as errors. If false it will leave the default values or null.
    private boolean treatMissingValuesAsErrors = true;
    // Treat missing discretionary values as errors
    private boolean treatMissingDiscretionaryValuesAsErrors = false;
    // For the proxy decoder, if we should use a cached value or call gestalt for the most recent value.
    private ProxyDecoderMode proxyDecoderMode = ProxyDecoderMode.CACHE;
    // Provide the log level when we log a message when a config is missing, but we provided a default, or it is Optional.
    private System.Logger.Level logLevelForMissingValuesWhenDefaultOrOptional = System.Logger.Level.DEBUG;
    // Java date decoder format.
    private DateTimeFormatter dateDecoderFormat = DateTimeFormatter.ISO_DATE_TIME;
    // Java local date time decoder format.
    private DateTimeFormatter localDateTimeFormat = DateTimeFormatter.ISO_DATE_TIME;
    // Java local date decoder format.
    private DateTimeFormatter localDateFormat = DateTimeFormatter.ISO_DATE_TIME;
    // Token that represents the opening of a string substitution.
    private String substitutionOpeningToken = "${";
    // Token that represents the closing of a string substitution.
    private String substitutionClosingToken = "}";
    // the maximum nested substitution depth.
    private int maxSubstitutionNestedDepth = 5;
    // the regex used to parse string substitutions.
    // Must have a named capture group transform, key, and default, where the key is required and the transform and default are optional.
    private String substitutionRegex = TransformerPostProcessor.DEFAULT_SUBSTITUTION_REGEX;

    // if metrics should be enabled
    private boolean metricsEnabled = false;

    // if validation should be enabled.
    private boolean validationEnabled = false;
    // The sentence lexer used for gestalt.
    private SentenceLexer sentenceLexer = new PathLexer();

    /**
     * Treat all warnings as errors.
     *
     * @return Treat all warnings as errors
     */
    public boolean isTreatWarningsAsErrors() {
        return treatWarningsAsErrors;
    }

    /**
     * Treat all warnings as errors.
     *
     * @param treatWarningsAsErrors Treat all warnings as errors.
     */
    public void setTreatWarningsAsErrors(boolean treatWarningsAsErrors) {
        this.treatWarningsAsErrors = treatWarningsAsErrors;
    }

    /**
     * Treat missing array index's as errors.
     *
     * @return Treat missing array index's as errors.
     */
    public boolean isTreatMissingArrayIndexAsError() {
        return treatMissingArrayIndexAsError;
    }

    /**
     * Treat missing array index's as errors.
     *
     * @param treatMissingArrayIndexAsError Treat missing array index's as errors.
     */
    public void setTreatMissingArrayIndexAsError(boolean treatMissingArrayIndexAsError) {
        this.treatMissingArrayIndexAsError = treatMissingArrayIndexAsError;
    }

    /**
     * Treat missing object values as errors.
     *
     * @return Treat missing object values as errors
     */
    public boolean isTreatMissingValuesAsErrors() {
        return treatMissingValuesAsErrors;
    }

    /**
     * Treat missing object values as errors.
     *
     * @param treatMissingValuesAsErrors Treat missing object values as errors
     */
    public void setTreatMissingValuesAsErrors(boolean treatMissingValuesAsErrors) {
        this.treatMissingValuesAsErrors = treatMissingValuesAsErrors;
    }

    /**
     * Gets treat missing discretionary values (optional, fields with defaults, fields with default annotations) as an error.
     * If this is false you will be able to get the configuration with default values or an empty Optional.
     * If this is true, if a field is missing and would have had a default it will fail and throw an exception.
     *
     * @return treatMissingDiscretionaryValuesAsErrors the settings for treating missing discretionary values as errors.
     */
    public boolean isTreatMissingDiscretionaryValuesAsErrors() {
        return treatMissingDiscretionaryValuesAsErrors;
    }

    /**
     * Sets treat missing discretionary values (optional, fields with defaults, fields with default annotations) as an error.
     * If this is false you will be able to get the configuration with default values or an empty Optional.
     * If this is true, if a field is missing and would have had a default it will fail and throw an exception.
     *
     * @param treatMissingDiscretionaryValuesAsErrors the settings for treating missing discretionary values as errors.
     */
    public void setTreatMissingDiscretionaryValuesAsErrors(boolean treatMissingDiscretionaryValuesAsErrors) {
        this.treatMissingDiscretionaryValuesAsErrors = treatMissingDiscretionaryValuesAsErrors;
    }

    /**
     * Treat null values in classes after decoding as errors.
     *
     * @return Treat null values in classes after decoding as errors.
     * @deprecated This value is no longer used, Please use {@link #setTreatMissingDiscretionaryValuesAsErrors(boolean)}
     *     and {@link #setTreatMissingValuesAsErrors(boolean)}
     */
    @Deprecated(since = "0.25.0", forRemoval = true)
    public boolean isTreatNullValuesInClassAsErrors() {
        return false;
    }

    /**
     * Treat null values in classes after decoding as errors.
     *
     * @param treatNullValuesInClassAsErrors Treat null values in classes after decoding as errors.
     * @deprecated This value is no longer used, Please use {@link #setTreatMissingDiscretionaryValuesAsErrors(boolean)}
     *     and {@link #setTreatMissingValuesAsErrors(boolean)}
     */
    @Deprecated(since = "0.25.0", forRemoval = true)
    public void setTreatNullValuesInClassAsErrors(boolean treatNullValuesInClassAsErrors) {
    }

    /**
     * Get For the proxy decoder mode, if we should use a cached value or call gestalt for the most recent value.
     *
     * @return the proxy decoder mode
     */
    public ProxyDecoderMode getProxyDecoderMode() {
        return proxyDecoderMode;
    }

    /**
     * Set For the proxy decoder mode, if we should use a cached value or call gestalt for the most recent value.
     *
     * @param proxyDecoderMode if we should use a cached value or call gestalt for the most recent value.
     */
    public void setProxyDecoderMode(ProxyDecoderMode proxyDecoderMode) {
        this.proxyDecoderMode = proxyDecoderMode;
    }


    /**
     * Provide the log level when we log a message when a config is missing, but we provided a default, or it is Optional.
     *
     * @return Log level
     */
    public System.Logger.Level getLogLevelForMissingValuesWhenDefaultOrOptional() {
        return logLevelForMissingValuesWhenDefaultOrOptional;
    }

    /**
     * Provide the log level when we log a message when a config is missing, but we provided a default, or it is Optional.
     *
     * @param logLevelForMissingValuesWhenDefaultOrOptional Log level
     */
    public void setLogLevelForMissingValuesWhenDefaultOrOptional(System.Logger.Level logLevelForMissingValuesWhenDefaultOrOptional) {
        this.logLevelForMissingValuesWhenDefaultOrOptional = logLevelForMissingValuesWhenDefaultOrOptional;
    }

    /**
     * Java date decoder format.
     *
     * @return Java date decoder format
     */
    public DateTimeFormatter getDateDecoderFormat() {
        return dateDecoderFormat;
    }

    /**
     * Java date decoder format.
     *
     * @param dateDecoderFormat Java date decoder format
     */
    public void setDateDecoderFormat(DateTimeFormatter dateDecoderFormat) {
        this.dateDecoderFormat = dateDecoderFormat;
    }

    /**
     * Java local date time decoder format.
     *
     * @return Java local date time decoder format.
     */
    public DateTimeFormatter getLocalDateTimeFormat() {
        return localDateTimeFormat;
    }

    /**
     * Java local date time decoder format.
     *
     * @param localDateTimeFormat Java local date time decoder format.
     */
    public void setLocalDateTimeFormat(DateTimeFormatter localDateTimeFormat) {
        this.localDateTimeFormat = localDateTimeFormat;
    }

    /**
     * Java local date decoder format.
     *
     * @return Java local date decoder format.
     */
    public DateTimeFormatter getLocalDateFormat() {
        return localDateFormat;
    }

    /**
     * Java local date decoder format.
     *
     * @param localDateFormat Java local date decoder format.
     */
    public void setLocalDateFormat(DateTimeFormatter localDateFormat) {
        this.localDateFormat = localDateFormat;
    }

    /**
     * Get the token that represents the opening of a string substitution.
     *
     * @return Token that represents the opening of a string substitution.
     */
    public String getSubstitutionOpeningToken() {
        return substitutionOpeningToken;
    }

    /**
     * Set the token that represents the opening of a string substitution.
     *
     * @param substitutionOpeningToken Token that represents the opening of a string substitution.
     */
    public void setSubstitutionOpeningToken(String substitutionOpeningToken) {
        this.substitutionOpeningToken = substitutionOpeningToken;
    }

    /**
     * Get the token that represents the closing of a string substitution.
     *
     * @return Token that represents the closing of a string substitution.
     */
    public String getSubstitutionClosingToken() {
        return substitutionClosingToken;
    }

    /**
     * Set the token that represents the opening of a string substitution.
     *
     * @param substitutionClosingToken Token that represents the closing of a string substitution.
     */
    public void setSubstitutionClosingToken(String substitutionClosingToken) {
        this.substitutionClosingToken = substitutionClosingToken;
    }

    /**
     * Get the maximum string substitution nested depth.
     * If you have nested or recursive substitutions that go deeper than this it will fail.
     *
     * @return the maximum string substitution nested depth.
     */
    public int getMaxSubstitutionNestedDepth() {
        return maxSubstitutionNestedDepth;
    }

    /**
     * Set the maximum string substitution nested depth.
     * If you have nested or recursive substitutions that go deeper than this it will fail.
     *
     * @param maxSubstitutionNestedDepth the maximum string substitution nested depth.
     */
    public void setMaxSubstitutionNestedDepth(int maxSubstitutionNestedDepth) {
        this.maxSubstitutionNestedDepth = maxSubstitutionNestedDepth;
    }


    /**
     * the regex used to parse string substitutions.
     * Must have a named capture group transform, key, and default, where the key is required and the transform and default are optional.
     *
     * @return the string substitution regex
     */
    public String getSubstitutionRegex() {
        return substitutionRegex;
    }

    /**
     * the regex used to parse string substitutions.
     * Must have a named capture group transform, key, and default, where the key is required and the transform and default are optional.
     *
     * @param substitutionRegex the string substitution regex
     */
    public void setSubstitutionRegex(String substitutionRegex) {
        this.substitutionRegex = substitutionRegex;
    }

    /**
     * Get if the metrics are enabled.
     *
     * @return if the metrics are enabled
     */
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    /**
     * set if the metrics are enabled.
     *
     * @param metricsEnabled if the metrics are enabled
     */
    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    /**
     * Get if validation is enabled.
     *
     * @return if validation is enabled
     */
    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    /**
     * Set if validation is enabled.
     *
     * @param validationEnabled if validation is enabled.
     */
    public void setValidationEnabled(boolean validationEnabled) {
        this.validationEnabled = validationEnabled;
    }

    /**
     * Get the sentence lexer that will be passed through to the DecoderRegistry.
     * it is used to convert the path requested to tokens, so we can navigate the config tree using the tokens.
     *
     * @return SentenceLexer the lexer
     */
    public SentenceLexer getSentenceLexer() {
        return sentenceLexer;
    }

    /**
     * Set the sentence lexer that will be passed through to the DecoderRegistry.
     * it is used to convert the path requested to tokens, so we can navigate the config tree using the tokens.
     *
     * @param sentenceLexer for the DecoderRegistry
     */
    public void setSentenceLexer(SentenceLexer sentenceLexer) {
        this.sentenceLexer = sentenceLexer;
    }

    /**
     * Register an external module configuration.
     *
     * @param module configuration
     */
    public void registerModuleConfig(GestaltModuleConfig module) {
        modulesConfig.put(module.getClass(), module);
    }

    /**
     * Register external module configurations.
     *
     * @param module configuration
     */
    @SuppressWarnings("rawtypes")
    public void registerModuleConfig(Map<Class, GestaltModuleConfig> module) {
        modulesConfig.putAll(module);
    }

    /**
     * Get an external module configuration.
     *
     * @param klass type of configuration to get
     * @param <T> type of the class
     * @return the module config for a class
     */
    @SuppressWarnings("unchecked")
    public <T extends GestaltModuleConfig> T getModuleConfig(Class<T> klass) {
        return (T) modulesConfig.get(klass);
    }
}
