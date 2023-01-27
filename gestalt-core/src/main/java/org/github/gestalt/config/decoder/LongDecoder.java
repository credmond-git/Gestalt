package org.github.gestalt.config.decoder;

import org.github.gestalt.config.entity.ValidationError;
import org.github.gestalt.config.node.ConfigNode;
import org.github.gestalt.config.reflect.TypeCapture;
import org.github.gestalt.config.utils.StringUtils;
import org.github.gestalt.config.utils.ValidateOf;

/**
 * Decode a Long.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2023.
 */
public class LongDecoder extends LeafDecoder<Long> {

    @Override
    public Priority priority() {
        return Priority.MEDIUM;
    }

    @Override
    public String name() {
        return "Long";
    }

    @Override
    public boolean matches(TypeCapture<?> klass) {
        return Long.class.isAssignableFrom(klass.getRawType()) || long.class.isAssignableFrom(klass.getRawType());
    }

    @Override
    protected ValidateOf<Long> leafDecode(String path, ConfigNode node) {
        ValidateOf<Long> results;

        String value = node.getValue().orElse("");
        if (StringUtils.isInteger(value)) {
            try {
                Long longVal = Long.parseLong(value);
                results = ValidateOf.valid(longVal);
            } catch (NumberFormatException e) {
                results = ValidateOf.inValid(new ValidationError.DecodingNumberFormatException(path, node, name()));
            }
        } else {
            results = ValidateOf.inValid(new ValidationError.DecodingNumberParsing(path, node, name()));
        }

        return results;
    }
}
