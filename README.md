# BlockbenchModelLib

BlockbenchModelLib is an open-source Java library for loading, parsing, validating, and rendering [Blockbench](https://www.blockbench.net/) models in Minecraft.

The library provides a reusable model-loading and rendering system that brings Blockbench's Generic Model (`.bbmodel`) format into Minecraft, preserving model geometry, textures, UV mappings, transformations, and hierarchical group structures.

Designed with modularity and extensibility in mind, BlockbenchModelLib separates Minecraft-independent model processing from Minecraft rendering and mod-loader integration.

### Features
- **Model Loading:** Parse Blockbench Generic Model (`.bbmodel`) files.
- **Model Validation:** Structured validation with descriptive errors and warnings.
- **Geometry:** Generate model geometry from Blockbench cubes and faces.
- **UV Mapping:** Support face-specific UV coordinates, rotations, and texture mappings.
- **Textures:** Support embedded textures and external resource-pack textures.
- **Hierarchy:** Preserve nested groups, pivots, and transformations.
- **Rendering:** Integrate Blockbench models with Minecraft's native rendering system.
- **Modular Architecture:** Separate core functionality, Minecraft integration, and NeoForge integration.

### Modules
- `core` – Minecraft-independent model parsing, data structures, and validation.
- `common` – Minecraft geometry generation, transformations, and rendering.
- `neoforge` – NeoForge integration, resource handling, and runtime registration.

**Current target:** Minecraft 1.21.1 · NeoForge 21.1.x · Java 21

BlockbenchModelLib aims to make using custom Blockbench models in Minecraft mods easier, more reusable, and less dependent on individually implemented model-loading systems.
