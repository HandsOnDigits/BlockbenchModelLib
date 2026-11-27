# BlockbenchModelLib

**A modular Java library for loading, animating, and rendering Blockbench models in Minecraft mods.**

BlockbenchModelLib brings Blockbench's Generic Model (`.bbmodel`) format into Minecraft, providing a reusable model-loading, geometry-generation, texture-management, animation, and rendering pipeline.

The library allows mod developers to use their Blockbench creations directly in Minecraft without having to implement their own model parsers, geometry generators, animation systems, or texture management infrastructure.

> **Unofficial project:** BlockbenchModelLib is an independent, community-developed project. It is not officially made by, affiliated with, endorsed by, or maintained by the Blockbench team. Blockbench and its associated trademarks belong to their respective owners.

## Features

- **Model Loading** – Load and parse Blockbench Generic Model (`.bbmodel`) files.
- **Geometry Generation** – Convert Blockbench model elements into Minecraft-compatible geometry.
- **Texture Management** – Resolve and register model textures, including embedded texture data.
- **UV Mapping** – Preserve face-specific UV coordinates and rotations.
- **Model Hierarchy** – Preserve model groups, pivots, and nested structures.
- **Model Bounds** – Inspect generated geometry bounds for accurate positioning and scaling.
- **Animation System** – Load and play Blockbench animations with position, rotation, and scale transformations.
- **Animation Playback** – Control playback, pause, resume, stop, seek, speed, and looping.
- **Hierarchical Animation** – Apply animated transformations throughout nested model hierarchies.
- **Rendering** – Render static and animated models using Minecraft's rendering infrastructure.
- **Resource Integration** – Load models and textures through Minecraft's resource manager.

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
| `core` | Minecraft-independent model data structures, parsing, and animation evaluation. |
| `common` | Geometry generation, hierarchy, texture management, animation playback, and rendering. |
| `neoforge` | NeoForge-specific integration. |

This modular architecture separates model processing from Minecraft-specific functionality.

## Quick Start

The primary runtime API is `BlockbenchModelInstance`.

### 1. Load a model

```java
ResourceLocation MODEL_LOCATION =
    ResourceLocation.fromNamespaceAndPath(
        "mymod",
        "bbmodels/machine.bbmodel"
    );

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

### 4. Position the model using its bounds

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

This horizontally centers the model and aligns its lowest geometry point with the origin.

## Animation Quick Start

BlockbenchModelLib supports animations defined in Blockbench Generic Model files.

### Play an animation

```java
instance.playAnimationByName("Spin");
```

### Update playback

```java
instance.updateAnimation(deltaSeconds);
```

Call this once per update tick, using elapsed time in seconds.

### Pause and resume

```java
instance.pauseAnimation();
instance.resumeAnimation();
```

### Stop playback

```java
instance.stopAnimation();
```

### Change animation speed

```java
instance.setAnimationSpeed(2.0F);
```

The example plays the animation at twice its normal speed.

For more detailed examples, see the Mod Developer Guide.

## Documentation

- **[Mod Developer Guide](docs/MOD_DEVELOPER_GUIDE.md)** – Installation, model loading, geometry, textures, animation, rendering, positioning, and integration examples.
- **API Reference** – JavaDoc documentation.
- **Examples** – Practical Minecraft mod integration examples.

## Installation

BlockbenchModelLib is built using Gradle and distributed through Maven artifacts.

For local development, the Maven repository is generated at:

```text
build/repo/
```

See the [Mod Developer Guide](docs/MOD_DEVELOPER_GUIDE.md) for dependency configuration and integration instructions.

## Example Projects

BlockbenchModelLib is developed alongside Minecraft mod integrations, including Havah's Textile.

These integrations demonstrate practical usage of the library, including custom item rendering, block entity rendering, model positioning, texture management, and animated models.

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

Some parts of the codebase contain AI-generated content that is later edited by humans.

All images, sounds, and models are made by humans.

---

**BlockbenchModelLib**

*Bringing Blockbench models, geometry, and animations into Minecraft mods.*
