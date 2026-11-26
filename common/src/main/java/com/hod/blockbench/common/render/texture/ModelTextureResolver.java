package com.hod.blockbench.common.render.texture;

import com.hod.blockbench.model.ModelTexture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves embedded and external texture data without depending on Minecraft.
 */
public final class ModelTextureResolver {

    private ModelTextureResolver() {}

    @FunctionalInterface
    public interface ExternalTextureLoader {
        byte[] load(String namespace, String resourcePath) throws IOException;
    }

    public static List<ResolvedModelTexture> resolve(
        List<ModelTexture> textures,
        ExternalTextureLoader externalTextureLoader
    ) throws IOException {
        Objects.requireNonNull(textures, "Textures cannot be null.");
        Objects.requireNonNull(
            externalTextureLoader,
            "External texture loader cannot be null."
        );

        List<ResolvedModelTexture> resolved = new ArrayList<>();

        for (int index = 0; index < textures.size(); index++) {
            ModelTexture texture = Objects.requireNonNull(
                textures.get(index),
                "Texture cannot be null."
            );

            byte[] imageData;

            // External Minecraft assets take priority.
            if (texture.hasExternalPath()) {
                String namespace = texture.namespace();

                if (namespace == null || namespace.isBlank()) {
                    throw new IOException(
                        "External texture has no namespace: " + texture.name()
                    );
                }

                String resourcePath = createResourcePath(texture);

                imageData = externalTextureLoader.load(namespace, resourcePath);
            } else if (texture.hasEmbeddedImage()) {
                // Fall back to embedded image data.
                imageData = texture.imageData();
            } else {
                // No embedded data or external path.
                continue;
            }

            resolved.add(
                new ResolvedModelTexture(index, texture.name(), imageData)
            );
        }

        return List.copyOf(resolved);
    }

    private static String createResourcePath(
        ModelTexture texture
    ) throws IOException {
        String path = texture.path();

        if (path == null || path.isBlank()) {
            throw new IOException(
                "External texture has no path: " + texture.name()
            );
        }

        if (path.startsWith("/") || path.contains("\\")) {
            throw new IOException("Invalid external texture path: " + path);
        }

        String[] segments = path.split("/", -1);

        for (String segment : segments) {
            if (
                segment.isEmpty() || segment.equals(".") || segment.equals("..")
            ) {
                throw new IOException("Invalid external texture path: " + path);
            }
        }

        // Library convention:
        // namespace + textures/ + model texture path
        return "textures/" + path;
    }
}
