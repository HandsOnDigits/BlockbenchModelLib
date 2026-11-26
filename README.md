# BlockbenchModelLib

**A reusable Java library for loading and rendering Blockbench models in Minecraft mods.**

BlockbenchModelLib brings Blockbench's Generic Model (`.bbmodel`) format into Minecraft, providing mod developers with a reusable model-loading, geometry-generation, texture-management, and rendering pipeline.

Instead of implementing custom model parsers and rendering systems for every mod, developers can use BlockbenchModelLib to integrate their Blockbench creations directly into Minecraft.

## Features

- **Model Loading** – Load and parse Blockbench `.bbmodel` files.
- **Geometry Generation** – Convert Blockbench model elements into Minecraft-compatible geometry.
- **Texture Management** – Resolve and register model textures, including embedded texture data.
- **UV Mapping** – Preserve face-specific UV coordinates and rotations.
- **Model Hierarchy** – Process model groups and nested structures.
- **Model Bounds** – Inspect generated geometry bounds for accurate positioning and scaling.
- **Rendering** – Render model instances using Minecraft's rendering infrastructure.
- **Resource Integration** – Load models through Minecraft's resource manager.

## Compatibility

| Component | Version |
|---|---|
| Minecraft | 1.21.1 |
| Mod loader | NeoForge 21.1.x |
| Java | 21 |
| Blockbench format | Generic Model (`.bbmodel`) |

## Architecture

BlockbenchModelLib is divided into three modules:

| Module | Description |
|---|---|
| `core` | Minecraft-independent model data structures and parsing. |
| `common` | Geometry generation, texture management, and rendering. |
| `neoforge` | NeoForge-specific integration. |

This modular architecture separates model processing from Minecraft-specific functionality, making the library easier to maintain and extend.

## Quick Start

The primary runtime API is `BlockbenchModelInstance`.

### 1. Load a model

```java
Resource resource = minecraft
    .getResourceManager()
    .getResource(MODEL_LOCATION)
    .orElseThrow();

BlockbenchModel model;

try (InputStream input = resource.open()) {
    model = BlockbenchModelLoader.load(input);
}
```

### 2. Create a model instance

```java
BlockbenchModelInstance instance =
    BlockbenchModelInstance.create(
        model,
        new ModelTextureManager(
            minecraft.getTextureManager()
        ),
        minecraft.getResourceManager(),
        false
    );
```

### 3. Render the model

```java
instance.render(
    poseStack,
    bufferSource,
    packedLight,
    packedOverlay
);
```

### 4. Calculate model positioning

```java
ModelBounds bounds = instance
    .geometry()
    .bounds()
    .orElseThrow();

ModelVector3 min = bounds.min();
ModelVector3 center = bounds.center();

poseStack.translate(
    -center.x() / 16.0F,
    -min.y() / 16.0F,
    -center.z() / 16.0F
);
```

The example horizontally centers the model and aligns its lowest geometry point with the origin.

Remember to close model instances when they are no longer needed.

## Documentation

- **[Mod Developer Guide](docs/MOD_DEVELOPER_GUIDE.md)** – Installation, model loading, rendering, texture management, positioning, and complete integration examples.
- **API Reference** – JavaDoc documentation (planned).
- **Examples** – Additional integration examples (planned).

## Installation

BlockbenchModelLib is built using Gradle and distributed through Maven artifacts.

For local development, the Maven repository is generated at:

```text
build/repo/
```

See the [Mod Developer Guide](docs/MOD_DEVELOPER_GUIDE.md) for dependency configuration and integration instructions.

## Example Projects

The library is developed alongside Minecraft mod integrations, including Havah's Textile.

These integrations help demonstrate practical usage of BlockbenchModelLib in real mod development.

## Contributing

Contributions, bug reports, and feature requests are welcome.

When reporting issues, include:

- BlockbenchModelLib version.
- Minecraft and NeoForge versions.
- A minimal example `.bbmodel` file when relevant.
- Relevant logs or error messages.
- Expected and actual behavior.

## License

BlockbenchModelLib is open source.

See [LICENSE](LICENSE) for licensing information.

---
AI is used in some parts of the project development; changes are edited and tested by humans.
All images and sounds are human-made.
---

**BlockbenchModelLib**

*Making Blockbench models easier to integrate into Minecraft mods.*
