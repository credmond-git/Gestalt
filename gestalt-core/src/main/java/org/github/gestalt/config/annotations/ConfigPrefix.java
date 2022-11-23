package org.github.gestalt.config.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotate a class to provide a prefix.
 */
@Target(value = { TYPE })
@Retention(value = RUNTIME)
public @interface ConfigPrefix {
    String prefix();
}