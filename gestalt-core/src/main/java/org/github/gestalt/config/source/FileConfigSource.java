package org.github.gestalt.config.source;

import org.github.gestalt.config.exceptions.GestaltException;
import org.github.gestalt.config.tag.Tags;
import org.github.gestalt.config.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Load a config source from a local file.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2023.
 */
public class FileConfigSource implements ConfigSource {
    private static final Logger logger = LoggerFactory.getLogger(FileConfigSource.class.getName());

    private final Path path;
    private final UUID id = UUID.randomUUID();
    private final Tags tags;

    /**
     * Constructor for a File Config Source.
     *
     * @param file where to load the File with the configuration
     * @throws GestaltException any exceptions.
     */
    public FileConfigSource(File file) throws GestaltException {
        this(Objects.requireNonNull(file, "file can not be null").toPath(), Tags.of());
    }

    /**
     * Constructor for a File Config Source.
     *
     * @param file where to load the File with the configuration
     * @param tags tags associated with the source
     * @throws GestaltException any exceptions.
     */
    public FileConfigSource(File file, Tags tags) throws GestaltException {
        this(Objects.requireNonNull(file, "file can not be null").toPath(), tags);
    }

    /**
     * Constructor for a File Config Source.
     *
     * @param path where to load the File with the configuration
     * @throws GestaltException any exceptions.
     */
    public FileConfigSource(Path path) throws GestaltException {
        this(path, Tags.of());
    }

    /**
     * Constructor for a File Config Source.
     *
     * @param path where to load the File with the configuration
     * @param tags tags associated with the source
     * @throws GestaltException any exceptions.
     */
    public FileConfigSource(Path path, Tags tags) throws GestaltException {
        this.path = validatePath(path);
        this.tags = tags;
    }

    private Path validatePath(Path path) throws GestaltException {
        Objects.requireNonNull(path, "Path can not be null");
        if (!Files.exists(path)) {
            throw new GestaltException("File does not exist from path: " + path);
        } else if (!Files.isRegularFile(path)) {
            throw new GestaltException("Path is not a regular file: " + path);
        } else if (!Files.isReadable(path)) {
            throw new GestaltException("Path is not a readable: " + path);
        } else if ("".equals(format(path))) {
            logger.debug("Unable to find a format for the file: {}", path);
        }
        return path;
    }

    /**
     * The path to the file.
     *
     * @return path to the file
     */
    public Path getPath() {
        return path;
    }

    @Override
    public boolean hasStream() {
        return true;
    }

    @Override
    public InputStream loadStream() throws GestaltException {
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new GestaltException("Unable to load file from path " + path, e);
        }
    }

    @Override
    public boolean hasList() {
        return false;
    }

    @Override
    public List<Pair<String, String>> loadList() throws GestaltException {
        throw new GestaltException("Unsupported operation loadList on an FileConfigSource");
    }

    @Override
    public String format() {
        return format(this.path);
    }

    /**
     * Finds the extension of a file to get the file format.
     *
     * @param path the name of the file
     * @return the extension of the file
     */
    protected String format(Path path) {
        String fileName = path.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        if (index > 0) {
            return fileName.substring(index + 1);
        } else {
            return "";
        }
    }

    @Override
    public String name() {
        return "File source: " + path.toString();
    }

    @Override
    public UUID id() {  //NOPMD
        return id;
    }

    @Override
    public Tags getTags() {
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileConfigSource)) {
            return false;
        }
        FileConfigSource that = (FileConfigSource) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
