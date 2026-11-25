package com.hod.blockbench.common.render.texture;

import com.hod.blockbench.model.ModelTexture;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class MinecraftModelTextureResolver {

    private MinecraftModelTextureResolver() {
    }

    public static List<ResolvedModelTexture> resolve(
            List<ModelTexture> textures,
            ResourceManager resourceManager
    ) throws IOException {
        Objects.requireNonNull(
                resourceManager,
                "ResourceManager cannot be null."
        );

        return ModelTextureResolver.resolve(
                textures,
                (namespace, resourcePath) -> loadExternal(
                        namespace,
                        resourcePath,
                        resourceManager
                )
        );
    }

    private static byte[] loadExternal(
            String namespace,
            String resourcePath,
            ResourceManager resourceManager
    ) throws IOException {
        ResourceLocation location;

        try {
            location = ResourceLocation.fromNamespaceAndPath(
                    namespace,
                    resourcePath
            );
        } catch (IllegalArgumentException exception) {
            throw new IOException(
                    "Invalid external texture resource location: "
                            + namespace + ":" + resourcePath,
                    exception
            );
        }

        Resource resource = resourceManager.getResource(location)
                .orElseThrow(() -> new FileNotFoundException(
                        "External Blockbench texture not found: " + location
                ));

        try (var input = resource.open()) {
            return input.readAllBytes();
        }
    }
}