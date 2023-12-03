package org.github.gestalt.config.decoder;

import org.github.gestalt.config.node.ConfigNode;
import org.github.gestalt.config.reflect.TypeCapture;
import org.github.gestalt.config.tag.Tags;
import org.github.gestalt.config.utils.ValidateOf;

/**
 * Decode a boolean.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2023.
 */
public final class BooleanDecoder extends LeafDecoder<Boolean> {

    @Override
    public Priority priority() {
        return Priority.MEDIUM;
    }

    @Override
    public String name() {
        return "Boolean";
    }

    @Override
    public boolean canDecode(String path, Tags tags, ConfigNode node, TypeCapture<?> type) {
        return Boolean.class.isAssignableFrom(type.getRawType()) || boolean.class.isAssignableFrom(type.getRawType());
    }

    @Override
    protected ValidateOf<Boolean> leafDecode(String path, ConfigNode node) {
        String value = node.getValue().orElse("");
        return ValidateOf.valid(Boolean.parseBoolean(value));
    }
}
