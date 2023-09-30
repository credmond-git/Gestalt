package org.github.gestalt.config.decoder;

import org.github.gestalt.config.entity.ValidationError;
import org.github.gestalt.config.node.ArrayNode;
import org.github.gestalt.config.node.ConfigNode;
import org.github.gestalt.config.node.LeafNode;
import org.github.gestalt.config.reflect.TypeCapture;
import org.github.gestalt.config.utils.ValidateOf;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for decoding collections. Will handle validation of the node type.
 *
 * @param <T> generic type of the collection
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2023.
 */
public abstract class CollectionDecoder<T extends Collection<?>> implements Decoder<T> {

    @Override
    public Priority priority() {
        return Priority.MEDIUM;
    }

    @Override
    public ValidateOf<T> decode(String path, ConfigNode node, TypeCapture<?> type, DecoderContext decoderContext) {
        ValidateOf<T> results;
        if (node instanceof ArrayNode) {
            if (node.size() > 0) {
                results = arrayDecode(path, node, type, decoderContext);
            } else {
                results = ValidateOf.inValid(new ValidationError.DecodingArrayMissingValue(path, name()));
            }
        } else if (node instanceof LeafNode) {
            if (node.getValue().isPresent()) {
                String value = node.getValue().get();
                String[] array = value.split("(?<!\\\\),");
                List<ConfigNode> leafNodes = Arrays.stream(array)
                                                   .map(String::trim)
                                                   .map(it -> it.replace("\\,", ","))
                                                   .map(LeafNode::new)
                                                   .collect(Collectors.toList());

                results = arrayDecode(path, new ArrayNode(leafNodes), type, decoderContext);
            } else {
                results = ValidateOf.inValid(new ValidationError.DecodingLeafMissingValue(path, name()));
            }
        } else {
            results = ValidateOf.inValid(new ValidationError.DecodingExpectedArrayNodeType(path, node, name()));
        }
        return results;
    }

    /**
     * Decode an array values.
     *
     * @param path           Current path we are decoding
     * @param node           current node we are decoding
     * @param klass          class to decode into
     * @param decoderContext The context of the current decoder.
     * @return ValidateOf array built from the config node
     */
    protected abstract ValidateOf<T> arrayDecode(String path, ConfigNode node,
                                                 TypeCapture<?> klass, DecoderContext decoderContext);
}
