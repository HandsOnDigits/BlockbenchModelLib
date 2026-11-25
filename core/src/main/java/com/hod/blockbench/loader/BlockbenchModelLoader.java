package com.hod.blockbench.loader;

import com.hod.blockbench.model.BlockbenchModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class BlockbenchModelLoader {

    private BlockbenchModelLoader() {
    }

    public static BlockbenchModel load(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return load(input);
        }
    }

    public static BlockbenchModel load(File file) throws IOException {
        return load(file.toPath());
    }

    /**
     * Loads a model from an input stream.
     *
     * The caller retains ownership of the supplied stream.
     */
    public static BlockbenchModel load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "Input stream cannot be null.");

        return load(new InputStreamReader(input, StandardCharsets.UTF_8));
    }

    /**
     * Loads a model from a character reader.
     *
     * The caller retains ownership of the supplied reader.
     */
    public static BlockbenchModel load(Reader reader) throws IOException {
        Objects.requireNonNull(reader, "Reader cannot be null.");

        return BlockbenchModelParser.parse(reader);
    }
}