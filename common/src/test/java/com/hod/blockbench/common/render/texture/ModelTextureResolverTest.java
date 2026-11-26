package com.hod.blockbench.common.render.texture;

import static org.junit.jupiter.api.Assertions.*;

import com.hod.blockbench.model.ModelTexture;
import java.io.FileNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelTextureResolverTest {

    private static ModelTexture externalTexture(
        String name,
        String path,
        String namespace
    ) {
        return new ModelTexture(
            "texture-1",
            name,
            64,
            64,
            64,
            64,
            1,
            "loop",
            "",
            false,
            "",
            null,
            path,
            "block",
            namespace,
            false,
            true
        );
    }

    @Test
    void resolvesExternalTexture() throws Exception {
        byte[] expectedData = { 1, 2, 3, 4 };

        ModelTexture texture = externalTexture(
            "textile_machine.png",
            "block/textile_machine.png",
            "havahstextile"
        );

        List<ResolvedModelTexture> resolved = ModelTextureResolver.resolve(
            List.of(texture),
            (namespace, resourcePath) -> {
                assertEquals("havahstextile", namespace);
                assertEquals(
                    "textures/block/textile_machine.png",
                    resourcePath
                );

                return expectedData;
            }
        );

        assertEquals(1, resolved.size());
        assertEquals(0, resolved.getFirst().textureIndex());
        assertEquals("textile_machine.png", resolved.getFirst().name());
        assertArrayEquals(expectedData, resolved.getFirst().imageData());
    }

    @Test
    void missingExternalTexturePropagatesIOException() {
        ModelTexture texture = externalTexture(
            "missing.png",
            "block/missing.png",
            "havahstextile"
        );

        assertThrows(FileNotFoundException.class, () ->
            ModelTextureResolver.resolve(
                List.of(texture),
                (namespace, resourcePath) -> {
                    throw new FileNotFoundException(resourcePath);
                }
            )
        );
    }

    @Test
    void externalTextureTakesPriorityOverEmbeddedImage() throws Exception {
        byte[] embeddedData = { 10, 20, 30 };
        byte[] externalData = { 40, 50, 60 };

        ModelTexture texture = new ModelTexture(
            "texture-1",
            "textile_machine.png",
            64,
            64,
            64,
            64,
            1,
            "loop",
            "",
            false,
            "data:image/png;base64,test",
            embeddedData,
            "block/textile_machine.png",
            "block",
            "havahstextile",
            true,
            true
        );

        List<ResolvedModelTexture> resolved = ModelTextureResolver.resolve(
            List.of(texture),
            (namespace, resourcePath) -> {
                assertEquals("havahstextile", namespace);
                assertEquals(
                    "textures/block/textile_machine.png",
                    resourcePath
                );

                return externalData;
            }
        );

        assertEquals(1, resolved.size());
        assertArrayEquals(externalData, resolved.getFirst().imageData());
    }

    @Test
    void preservesOriginalTextureIndices() throws Exception {
        byte[] firstData = { 1 };
        byte[] thirdData = { 3 };

        ModelTexture first = new ModelTexture(
            "first",
            "first.png",
            64,
            64,
            64,
            64,
            1,
            "loop",
            "",
            false,
            "",
            firstData,
            "",
            "block",
            "havahstextile",
            true,
            false
        );

        ModelTexture missing = new ModelTexture(
            "missing",
            "missing.png",
            64,
            64,
            64,
            64,
            1,
            "loop",
            "",
            false,
            "",
            null,
            "",
            "block",
            "havahstextile",
            false,
            false
        );

        ModelTexture third = externalTexture(
            "third.png",
            "block/third.png",
            "havahstextile"
        );

        List<ResolvedModelTexture> resolved = ModelTextureResolver.resolve(
            List.of(first, missing, third),
            (namespace, resourcePath) -> {
                assertEquals("havahstextile", namespace);
                assertEquals("textures/block/third.png", resourcePath);
                return thirdData;
            }
        );

        assertEquals(2, resolved.size());

        assertEquals(0, resolved.get(0).textureIndex());
        assertArrayEquals(firstData, resolved.get(0).imageData());

        assertEquals(2, resolved.get(1).textureIndex());
        assertArrayEquals(thirdData, resolved.get(1).imageData());
    }

    @Test
    void rejectsPathTraversal() {
        ModelTexture texture = externalTexture(
            "invalid.png",
            "block/../outside.png",
            "havahstextile"
        );

        assertThrows(java.io.IOException.class, () ->
            ModelTextureResolver.resolve(
                List.of(texture),
                (namespace, resourcePath) -> new byte[] { 1 }
            )
        );
    }
}
