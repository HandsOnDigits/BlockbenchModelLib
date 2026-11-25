# BlockbenchModelLib – Mod Developer Guide

**A guide to integrating Blockbench models into Minecraft mods**

- Minecraft: 1.21.1
- Mod loader: NeoForge 21.1.x
- Java: 21
- Library: BlockbenchModelLib

---

## 1. Introduction

BlockbenchModelLib is a Java library designed to make it easier for Minecraft mod developers to use models created with Blockbench.

Instead of manually converting Blockbench models into Minecraft-compatible geometry, developers can load `.bbmodel` files and use the library to parse their contents, access model data, generate geometry, resolve textures, and render models.

The library is designed to be reusable across different mods, reducing the amount of custom model-loading and rendering code developers need to maintain.

### Main capabilities

- Load Blockbench Generic Model (`.bbmodel`) files.
- Parse model metadata and geometry.
- Access cubes, faces, UV coordinates, and textures.
- Preserve model hierarchy and nested groups.
- Handle embedded Base64-encoded PNG textures.
- Generate Minecraft-compatible model geometry.
- Render model geometry using Minecraft's rendering infrastructure.
- Support multiple textures and texture indices.

## 2. Project modules

BlockbenchModelLib is divided into three modules.

| Module | Responsibility |
|---|---|
| `core` | Model parsing, model data structures, and Minecraft-independent functionality. |
| `common` | Minecraft integration, geometry generation, texture handling, and rendering. |
| `neoforge` | NeoForge-specific integration. |

As a mod developer, you will primarily interact with the public APIs exposed by these modules rather than implementing the underlying parsing and geometry logic yourself.

## 3. Preparing Blockbench models

Before using a model, prepare it in Blockbench.

### Supported model format

The current parser targets Blockbench's Generic Model format:

- Model format: `free`
- File extension: `.bbmodel`

The current implementation supports:

- Cube elements.
- Individual cube faces.
- Face-specific UV coordinates.
- Texture indices.
- Face rotations.
- Cube rotations and inflation.
- Model groups and nested hierarchy.
- Embedded PNG textures.

Other Blockbench model formats should not be assumed to work.

### Texture recommendations

For the most straightforward workflow, embed your textures in the `.bbmodel` file.

This allows the model and its texture data to be distributed together.

External texture resolution is being developed separately and should not be assumed to be available in every library version.

## 4. Adding the library to your mod

BlockbenchModelLib is built using Gradle.

The library's local Maven repository is generated at:

`BlockbenchModelLib/build/repo/`

For local development, add that repository to your mod project's Gradle configuration.

The exact dependency coordinates should be taken from the published library version.

```gradle
repositories {
    maven {
        url = uri("path/to/BlockbenchModelLib/build/repo")
    }
}

dependencies {
    // Add the published BlockbenchModelLib modules here.
}
```

Use the published artifacts appropriate for your integration rather than depending on the library's internal source directories.

## 5. Loading a Blockbench model

The core module provides the model-loading API.

The main entry point is `BlockbenchModelLoader`.

You can load a model from a file, path, or input stream.

### Loading from a file

```java
import com.hod.blockbench.model.BlockbenchModel;
import com.hod.blockbench.model.BlockbenchModelLoader;

import java.nio.file.Path;

BlockbenchModel model = BlockbenchModelLoader.load(
    Path.of("models/example.bbmodel")
);
```

### Loading from an input stream

```java
try (var input = resource.open()) {
    BlockbenchModel model = BlockbenchModelLoader.load(input);
}
```

The loader parses the model and returns a `BlockbenchModel`.

Invalid model data can result in a parsing exception, while file access failures are reported through `IOException`.

## 6. Working with model data

Once loaded, the model exposes structured information about its contents.

Important model classes include:

| Class | Description |
|---|---|
| `BlockbenchModel` | Complete parsed model. |
| `ModelMetadata` | Model metadata and properties. |
| `ModelCube` | Cube geometry and properties. |
| `ModelFace` | Individual cube faces and UV data. |
| `ModelBone` | Model hierarchy and group information. |
| `ModelOutlinerNode` | Outliner hierarchy representation. |
| `ModelTexture` | Texture metadata and image data. |
| `ModelVector3` | Three-dimensional vector data. |

### Finding model elements

The model provides lookup methods such as:

```java
model.findCube("cube_name");
model.findTexture("texture_name");
```

You can use these methods to access individual elements without manually searching through the model's internal collections.

### Texture information

A `ModelTexture` provides information about its associated texture.

Relevant methods include:

```java
texture.hasEmbeddedImage();
texture.hasExternalPath();
texture.decodeImage();
```

This allows developers to inspect texture availability and decode embedded image data.

## 7. Geometry generation

BlockbenchModelLib provides `ModelGeometryBuilder` to convert parsed model data into geometry suitable for Minecraft rendering.

The generated geometry is represented by `ModelGeometry`.

The geometry generation process handles the conversion of Blockbench model geometry into the representation used by the Minecraft rendering integration.

This includes:

- Cube vertex generation.
- Face generation.
- UV coordinate conversion.
- Texture index preservation.
- Model transformations.

Developers should use the geometry builder rather than implementing their own cube-to-vertex conversion.

## 8. Rendering models

The common module provides `BlockbenchModelRenderer`.

It integrates generated model geometry with Minecraft's rendering infrastructure.

The renderer works with Minecraft rendering objects such as:

- `PoseStack`
- `VertexConsumer`
- Packed lighting values.
- Overlay values.

It also supports supplying vertex consumers based on texture indices, allowing models with multiple textures to be rendered.

### Rendering considerations

BlockbenchModelLib uses Blockbench's model coordinate conventions and converts model dimensions to Minecraft's rendering scale.

When integrating the renderer into your mod, make sure that:

1. Your model is generated before rendering.
2. The correct texture consumer is selected for each texture index.
3. The appropriate transformation stack is supplied.
4. Lighting and overlay values are passed through.
5. The rendering call is made from the appropriate Minecraft rendering context.

The renderer's API is available in `com.hod.blockbench.common.render.BlockbenchModelRenderer`.

## 9. Texture management

Textures are an important part of the model-loading pipeline.

BlockbenchModelLib provides:

- `ModelTextureResolver`
- `ModelTextureManager`
- `ResolvedModelTexture`
- `ModelTextureRegistration`

The texture resolver processes the texture information stored in a model.

The texture manager handles registration of resolved textures and provides the registration information required by the rendering integration.

Embedded texture data is supported by the current implementation.

### Multiple textures

Models can contain multiple textures.

Texture indices must be preserved during geometry generation and texture registration so that each face uses the correct texture.

Do not assume that every model uses only one texture.

## 10. Error handling

Model loading and resource resolution can fail for several reasons.

Examples include:

- Invalid JSON structure.
- Unsupported Blockbench model formats.
- Missing required model properties.
- Invalid cube or face data.
- Invalid texture data.
- Missing external resources.

Handle loading and resource errors explicitly.

For example:

```java
try {
    BlockbenchModel model =
        BlockbenchModelLoader.load(modelPath);

    // Continue with model processing.
} catch (IOException exception) {
    // Handle file access or loading errors.
}
```

Do not silently ignore malformed model data. Report enough information to help mod developers identify the problematic model.

## 11. Recommended integration workflow

A typical model integration follows this sequence:

1. Load the `.bbmodel` file.
2. Validate that the model is supported.
3. Inspect model metadata if necessary.
4. Generate `ModelGeometry`.
5. Resolve and register the model's textures.
6. Retain the generated geometry and texture registration.
7. Render the geometry when required.
8. Release associated resources when they are no longer needed.

Avoid loading and rebuilding models every frame. Model loading, geometry generation, and texture registration should happen outside the normal rendering loop.

## 12. Performance considerations

For models that are rendered frequently:

- Cache parsed models.
- Reuse generated geometry.
- Avoid repeatedly decoding embedded textures.
- Avoid registering duplicate textures.
- Avoid unnecessary allocations during rendering.
- Keep resource creation outside rendering callbacks.

The library is intended to provide reusable model-loading infrastructure, not to replace sensible resource management in individual mods.

## 13. Current limitations

BlockbenchModelLib is under active development.

Developers should consult the documentation and implementation for the specific library version they are using.

In particular:

- Generic Model (`free`) is the current supported model format.
- Other Blockbench model formats are not guaranteed to work.
- Embedded PNG textures are supported.
- External texture resource resolution is subject to the version's implementation.
- Minecraft rendering integration is available, but visual compatibility should be tested with actual models.
- Advanced Blockbench features not explicitly documented should not be assumed to be supported.

## 14. Troubleshooting

| Problem | Things to check |
|---|---|
| Model fails to load | Verify the `.bbmodel` file and model format. |
| Model has missing geometry | Check cube elements and outliner references. |
| Incorrect texture | Check face texture indices and UV coordinates. |
| Missing texture | Verify embedded image data or resource resolution. |
| Incorrect model scale | Check the model's coordinate system and rendering transformations. |
| Incorrect orientation | Check cube rotations, pivots, and the supplied `PoseStack`. |
| Rendering errors | Verify the rendering context and vertex consumers. |

## 15. Contributing and support

BlockbenchModelLib is an open-source project.

Mod developers are encouraged to report bugs, unsupported model features, and integration problems.

When reporting an issue, include:

- BlockbenchModelLib version.
- Minecraft version.
- Mod loader and version.
- A minimal example `.bbmodel` file.
- Relevant error messages or logs.
- Expected and actual behavior.

---

**BlockbenchModelLib**

Making Blockbench models easier to use in Minecraft mods.
