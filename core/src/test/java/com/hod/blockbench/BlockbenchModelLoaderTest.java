package com.hod.blockbench;

import static org.junit.jupiter.api.Assertions.*;

import com.hod.blockbench.loader.BlockbenchModelLoader;
import com.hod.blockbench.model.BlockbenchModel;
import com.hod.blockbench.model.ModelElement;
import com.hod.blockbench.model.ModelFace;
import com.hod.blockbench.model.ModelOutlinerNode;
import com.hod.blockbench.model.ModelTexture;
import com.hod.blockbench.validation.BlockbenchModelValidator;
import com.hod.blockbench.validation.ValidationIssue;
import com.hod.blockbench.validation.ValidationSeverity;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BlockbenchModelLoaderTest {

    @Test
    void nestedOutlinerGroupsPreserveHierarchyAndElementReferences() throws Exception {
        String json = """
        {
          "name": "Nested Group Test",
          "meta": {
            "format_version": "4.10",
            "model_format": "free",
            "box_uv": false
          },
          "resolution": {
            "width": 16,
            "height": 16
          },
          "elements": [
            {
              "uuid": "cube-1",
              "name": "Cube 1"
            },
            {
              "uuid": "cube-2",
              "name": "Cube 2"
            },
            {
              "uuid": "cube-3",
              "name": "Cube 3"
            },
            {
              "uuid": "cube-4",
              "name": "Cube 4"
            }
          ],
          "textures": [],
          "groups": [
            {
              "uuid": "root",
              "name": "Root",
              "visibility": true,
              "export": true,
              "locked": false,
              "color": 0,
              "isOpen": true,
              "shade": true,
              "mirror_uv": false,
              "autouv": 0,
              "children": [
                "middle",
                "cube-4"
              ]
            },
            {
              "uuid": "middle",
              "name": "Middle",
              "visibility": true,
              "export": true,
              "locked": false,
              "color": 1,
              "isOpen": true,
              "shade": true,
              "mirror_uv": false,
              "autouv": 0,
              "children": [
                "inner",
                "cube-3"
              ]
            },
            {
              "uuid": "inner",
              "name": "Inner",
              "visibility": true,
              "export": true,
              "locked": false,
              "color": 2,
              "isOpen": true,
              "shade": true,
              "mirror_uv": false,
              "autouv": 0,
              "children": [
                "cube-1",
                "cube-2"
              ]
            }
          ],
          "outliner": [
            {
              "uuid": "root",
              "isOpen": true,
              "children": [
                "middle",
                "cube-4"
              ]
            }
          ]
        }
        """;

        BlockbenchModel model = BlockbenchModelLoader.load(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(4, model.elements().size());
        assertEquals(1, model.outliner().size());

        // Root group
        ModelOutlinerNode root = model.outliner().getFirst();

        assertTrue(root.isGroup());
        assertEquals("Root", root.name());
        assertEquals(2, root.children().size());

        // Middle group
        ModelOutlinerNode middle = root.children().get(0);

        assertTrue(middle.isGroup());
        assertEquals("Middle", middle.name());
        assertEquals(2, middle.children().size());

        // Inner group
        ModelOutlinerNode inner = middle.children().get(0);

        assertTrue(inner.isGroup());
        assertEquals("Inner", inner.name());
        assertEquals(2, inner.children().size());

        // Elements inside the innermost group
        ModelOutlinerNode element1 = inner.children().get(0);
        ModelOutlinerNode element2 = inner.children().get(1);

        assertTrue(element1.isElement());
        assertTrue(element2.isElement());

        assertEquals("cube-1", element1.elementUuid());
        assertEquals("cube-2", element2.elementUuid());

        // Element directly inside Middle
        ModelOutlinerNode element3 = middle.children().get(1);

        assertTrue(element3.isElement());
        assertEquals("cube-3", element3.elementUuid());

        // Element directly inside Root
        ModelOutlinerNode element4 = root.children().get(1);

        assertTrue(element4.isElement());
        assertEquals("cube-4", element4.elementUuid());

        // Verify that all outliner references resolve to parsed elements.
        for (String uuid : List.of("cube-1", "cube-2", "cube-3", "cube-4")) {
            ModelElement element = model.findElement(uuid).orElseThrow();

            assertEquals(uuid, element.uuid());
        }

        assertEquals(
            List.of("cube-1", "cube-2"),
            model
                .findElementsInNode(inner)
                .stream()
                .map(ModelElement::uuid)
                .toList()
        );

        assertEquals(
            List.of("cube-1", "cube-2", "cube-3", "cube-4"),
            model
                .findAllOutlinerElements()
                .stream()
                .map(ModelElement::uuid)
                .toList()
        );
    }

    @Test
    void normalizationUsesPerTextureUvDimensions() {
        ModelTexture texture = new ModelTexture(
            "texture-2",
            "high-resolution",
            128,
            128,
            64,
            64,
            1,
            "custom",
            "",
            false,
            "",
            null
        );

        ModelFace face = new ModelFace(
            ModelFace.Direction.NORTH,
            new ModelFace.ModelUv(0, 0, 32, 32),
            0,
            0
        );

        var normalized = face.normalizedUv(texture);

        assertEquals(0.0f, normalized.u1(), 0.0001f);
        assertEquals(0.0f, normalized.v1(), 0.0001f);
        assertEquals(0.5f, normalized.u2(), 0.0001f);
        assertEquals(0.5f, normalized.v2(), 0.0001f);
    }

    @Test
    void reversedUvBoundsArePreserved() {
        ModelTexture texture = new ModelTexture(
            "texture-1",
            "test",
            64,
            64,
            64,
            64,
            1,
            "custom",
            "",
            false,
            "",
            null
        );

        ModelFace face = new ModelFace(
            ModelFace.Direction.UP,
            new ModelFace.ModelUv(19, 37, 18, 18),
            0,
            0
        );

        var normalized = face.normalizedUv(texture);

        assertEquals(19.0f / 64.0f, normalized.u1(), 0.0001f);
        assertEquals(37.0f / 64.0f, normalized.v1(), 0.0001f);
        assertEquals(18.0f / 64.0f, normalized.u2(), 0.0001f);
        assertEquals(18.0f / 64.0f, normalized.v2(), 0.0001f);
    }

    @Test
    void textureMetadataIsPreserved() throws Exception {
        String json = """
        {
          "name": "Texture Metadata Test",
          "meta": {
            "format_version": "4.10",
            "model_format": "free",
            "box_uv": false
          },
          "resolution": {
            "width": 64,
            "height": 64
          },
          "elements": [],
          "textures": [
            {
              "name": "textile_machine.png",
              "path": "",
              "relative_path": "../textures/block/textile_machine.png",
              "folder": "block",
              "namespace": "havahstextile",
              "id": "0",
              "width": 64,
              "height": 64,
              "uv_width": 64,
              "uv_height": 64,
              "frame_time": 1,
              "frame_order_type": "loop",
              "frame_order": "",
              "frame_interpolate": false,
              "internal": true,
              "saved": false,
              "source": ""
            }
          ],
          "outliner": []
        }
        """;

        BlockbenchModel model = BlockbenchModelLoader.load(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(1, model.textures().size());

        ModelTexture texture = model.textures().getFirst();

        assertEquals("textile_machine.png", texture.name());
        assertEquals("block/textile_machine.png", texture.path());
        assertEquals("block", texture.folder());
        assertEquals("havahstextile", texture.namespace());
        assertTrue(texture.internal());
        assertFalse(texture.saved());
    }
}
