package com.hod.blockbench.model;

import java.util.Map;

public record ModelCube(
        String uuid,
        String name,
        boolean visible,
        ModelVector3 from,
        ModelVector3 to,
        ModelVector3 origin,
        ModelVector3 rotation,
        float inflate,
        Map<ModelFace.Direction, ModelFace> faces
) {
    public ModelCube {
        faces = Map.copyOf(faces);
    }

    public ModelVector3 size() {
        return to.subtract(from);
    }

    public ModelTexture findTexture(
            ModelFace.Direction direction,
            BlockbenchModel model
    ) {
        ModelFace face = faces.get(direction);

        if (face == null || face.texture() < 0) {
            return null;
        }

        if (face.texture() >= model.textures().size()) {
            return null;
        }

        return model.textures().get(face.texture());
    }
}