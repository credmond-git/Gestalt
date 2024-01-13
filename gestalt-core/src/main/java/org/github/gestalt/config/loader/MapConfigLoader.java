package org.github.gestalt.config.loader;

import org.github.gestalt.config.entity.ConfigNodeContainer;
import org.github.gestalt.config.exceptions.GestaltException;
import org.github.gestalt.config.lexer.PathLexer;
import org.github.gestalt.config.lexer.SentenceLexer;
import org.github.gestalt.config.node.ConfigNode;
import org.github.gestalt.config.node.MapNode;
import org.github.gestalt.config.parser.ConfigParser;
import org.github.gestalt.config.parser.MapConfigParser;
import org.github.gestalt.config.source.ConfigSource;
import org.github.gestalt.config.source.MapConfigSource;
import org.github.gestalt.config.utils.GResultOf;
import org.github.gestalt.config.utils.Pair;

import java.util.List;
import java.util.Map;

/**
 * Loads an in memory map from MapConfigSource.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2024.
 */
public final class MapConfigLoader implements ConfigLoader {

    private final ConfigParser parser;
    private final SentenceLexer lexer;

    /**
     * Construct a default Map Config loader using the default path lexer for "." separated paths.
     */
    public MapConfigLoader() {
        this(new PathLexer("."), new MapConfigParser());
    }

    /**
     * Construct a Map Config loader providing a lexer and a config parser.
     *
     * @param lexer  SentenceLexer to create tokens for the path.
     * @param parser Parser for the Map Config files
     */
    public MapConfigLoader(SentenceLexer lexer, ConfigParser parser) {
        this.lexer = lexer;
        this.parser = parser;
    }

    @Override
    public String name() {
        return "MapConfigLoader";
    }

    @Override
    public boolean accepts(String format) {
        return MapConfigSource.MAP_CONFIG.equals(format);
    }

    @Override
    public GResultOf<List<ConfigNodeContainer>> loadSource(ConfigSource source) throws GestaltException {
        List<Pair<String, String>> configs;
        if (source.hasList()) {
            configs = source.loadList();
        } else {
            throw new GestaltException("Config source: " + source.name() + " does not have a list to load.");
        }

        if (configs.isEmpty()) {
            return GResultOf.result(List.of(new ConfigNodeContainer(new MapNode(Map.of()), source)));
        }

        GResultOf<ConfigNode> loadedNode = ConfigCompiler.analyze(source.failOnErrors(), lexer, parser, configs);

        return loadedNode.mapWithError((result) -> List.of(new ConfigNodeContainer(result, source)));
    }
}
