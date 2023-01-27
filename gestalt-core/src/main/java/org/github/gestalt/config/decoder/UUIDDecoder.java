package org.github.gestalt.config.decoder;

import org.github.gestalt.config.entity.ValidationError;
import org.github.gestalt.config.node.ConfigNode;
import org.github.gestalt.config.reflect.TypeCapture;
import org.github.gestalt.config.utils.ValidateOf;

import java.util.UUID;

/**
 * Decode a UUID.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2023.
 */
public class UUIDDecoder extends LeafDecoder<UUID> {

    @Override
    public Priority priority() {
        return Priority.MEDIUM;
    }

    @Override
    public String name() {
        return "UUID";
    }

    @Override
    public boolean matches(TypeCapture<?> klass) {
        return UUID.class.isAssignableFrom(klass.getRawType());
    }

    @Override
    protected ValidateOf<UUID> leafDecode(String path, ConfigNode node) {
        String value = node.getValue().orElse("");

        try {
            return ValidateOf.valid(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return ValidateOf.inValid(new ValidationError.ErrorDecodingException(path, node, name()));
        }
    }
}
