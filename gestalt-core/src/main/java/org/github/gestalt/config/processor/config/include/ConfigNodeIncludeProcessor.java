package org.github.gestalt.config.processor.config.include;

import org.github.gestalt.config.entity.ValidationError;
import org.github.gestalt.config.lexer.SentenceLexer;
import org.github.gestalt.config.node.ConfigNode;
import org.github.gestalt.config.node.LeafNode;
import org.github.gestalt.config.node.MapNode;
import org.github.gestalt.config.node.MergeNodes;
import org.github.gestalt.config.processor.config.ConfigNodeProcessor;
import org.github.gestalt.config.processor.config.ConfigNodeProcessorConfig;
import org.github.gestalt.config.source.factory.ConfigSourceFactoryService;
import org.github.gestalt.config.utils.GResultOf;
import org.github.gestalt.config.utils.Pair;
import org.github.gestalt.config.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Processor that scans map nodes looking for the import token. If found it will inject the loaded ConfigSource at that path.
 *
 * <p>Load a config source from a classpath resource using the getResourceAsStream method.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2024.
 */
public class ConfigNodeIncludeProcessor implements ConfigNodeProcessor {

    private ConfigSourceFactoryService configSourceFactoryService;
    private String nodeImportKeyword;
    private SentenceLexer lexer;

    private static List<Pair<Integer, ConfigNode>> buildOrderedImportNodes(String importKey, GResultOf<List<ConfigNode>> loadedConfigNode) {
        // you can order the imports by having $import:3, pull out the order variable.
        int order;
        String[] importDetails = importKey.split(":");
        if (importDetails.length > 1 && StringUtils.isInteger(importDetails[1])) {
            order = Integer.parseInt(importDetails[1]);
        } else {
            order = -1;
        }

        // create a list of pairs with the order and the configNode.
        return loadedConfigNode.results().stream()
            .map(it -> new Pair<>(order, it))
            .collect(Collectors.toList());
    }

    @SuppressWarnings("Indentation")
    private static Map<String, String> convertStringToParameters(String path, String paramtersString, List<ValidationError> errors) {
        return Arrays.stream(paramtersString.split(",")).map(it -> {
                var parts = it.split("=");
                if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                    errors.add(new ValidationError.ConfigNodeImportParameterHasWrongSize(path, paramtersString, it));
                    return null;
                } else {
                    return new Pair<>(parts[0], parts[1]);
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    @Override
    public void applyConfig(ConfigNodeProcessorConfig config) {
        this.configSourceFactoryService = config.getConfigSourceFactoryService();
        this.nodeImportKeyword = config.getConfig().getNodeImportKeyword();
        this.lexer = config.getLexer();
    }

    @Override
    public GResultOf<ConfigNode> process(String path, ConfigNode currentNode) {
        if (configSourceFactoryService == null || !(currentNode instanceof MapNode)) {
            return GResultOf.result(currentNode);
        }

        MapNode mapNode = (MapNode) currentNode;

        // get the internal map.
        Map<String, ConfigNode> internalMap = mapNode.getMapNode();

        // find if any nodes start with the import keyword.
        Map<String, ConfigNode> importingNodes = internalMap.entrySet().stream()
            .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getKey().startsWith(nodeImportKeyword))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // if none of the current nodes match the node import keyword, then return the original node.
        if (importingNodes.isEmpty()) {
            return GResultOf.result(currentNode);
        }

        List<ValidationError> errors = new ArrayList<>();
        //one or more of the current nodes represents an import.
        var nodeAndOrderPair = new ArrayList<Pair<Integer, ConfigNode>>();
        Map<String, ConfigNode> originNodesNoImport = new HashMap<>(mapNode.getMapNode());

        // for each of the nodes we are importing
        for (Map.Entry<String, ConfigNode> importEntries : importingNodes.entrySet()) {
            // ensure the node is a leaf node, so we can get its source parameters.
            if (!(importEntries.getValue() instanceof LeafNode)) {
                errors.add(new ValidationError.ConfigNodeImportWrongNodeType(path, importEntries.getValue()));
                break;
            }

            LeafNode importLeafParameters = (LeafNode) importEntries.getValue();

            // ensure that the node has some parameters and is not empty
            if (importLeafParameters.getValue().isEmpty() || importLeafParameters.getValue().get().isEmpty()) {
                errors.add(new ValidationError.ConfigNodeImportNodeEmpty(path));
                break;
            }

            var paramtersString = importLeafParameters.getValue().get();
            // convert the string formatted map into its pairs.
            Map<String, String> parameters = convertStringToParameters(path, paramtersString, errors);

            // from the parameters generate the config source.
            GResultOf<List<ConfigNode>> configNodesResult = configSourceFactoryService.build(parameters);

            errors.addAll(configNodesResult.getErrors());
            if (configNodesResult.hasResults()) {
                var orderedImportNodes = buildOrderedImportNodes(importEntries.getKey(), configNodesResult);

                // add these new nodes to the list of all ordered nodes.
                nodeAndOrderPair.addAll(orderedImportNodes);

                // since we imported this node, remove it from the original map
                originNodesNoImport.remove(importEntries.getKey());
            }

        }

        // add in the original nodes
        nodeAndOrderPair.add(new Pair<>(0, new MapNode(originNodesNoImport)));

        // if there are no nodes to merge, return the original nodes.
        if (nodeAndOrderPair.size() <= 1) {
            return GResultOf.resultOf(new MapNode(originNodesNoImport), errors);
        } else {
            // if there are multiple nodes to merge.
            // Merge the imported nodes along with the original config node minus the import entries.
            ConfigNode mergedNode = mergeOrderedNodes(path, nodeAndOrderPair, errors);

            var nestedNodes = process(path, mergedNode);
            errors.addAll(nestedNodes.getErrors());

            if (nestedNodes.hasResults()) {
                return GResultOf.resultOf(nestedNodes.results(), errors);
            } else {
                return GResultOf.errors(errors);
            }
        }
    }

    private ConfigNode mergeOrderedNodes(String path, ArrayList<Pair<Integer, ConfigNode>> nodeAndOrderPair, List<ValidationError> errors) {
        // create an ordered list of nodes
        var orderedNodesList = nodeAndOrderPair.stream()
            .sorted(Comparator.comparingInt(Pair::getFirst))
            .map(Pair::getSecond)
            .collect(Collectors.toList());

        // get the first node in the order.
        ConfigNode mergedNode = orderedNodesList.get(0);

        // if there is more than one node, merge the nodes in order, so each new node overrides and adds to the merged node.
        if (orderedNodesList.size() > 1) {
            for (ConfigNode it : orderedNodesList.subList(1, orderedNodesList.size())) {
                GResultOf<ConfigNode> resultOfMerge = MergeNodes.mergeNodes(path, lexer, mergedNode, it);

                errors.addAll(resultOfMerge.getErrors());
                if (resultOfMerge.hasResults()) {
                    mergedNode = resultOfMerge.results();
                }
            }
        }
        return mergedNode;
    }
}