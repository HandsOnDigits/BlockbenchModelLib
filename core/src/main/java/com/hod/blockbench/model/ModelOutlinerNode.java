
package com.hod.blockbench.model;

import java.util.List;

public record ModelOutlinerNode(
        String uuid,
        String name,
        Type type,
        ModelVector3 origin,
        ModelVector3 rotation,
        String cubeUuid,
        List<ModelOutlinerNode> children,
        String rawJson,

        // Group properties
        boolean visible,
        boolean export,
        boolean locked,
        int color,
        boolean isOpen,
        boolean shade,
        boolean mirrorUv,
        int autoUv
) {
    public enum Type {
        GROUP,
        CUBE,
        OTHER
    }

    public ModelOutlinerNode {
        children = List.copyOf(children);
    }

    /**
     * Compatibility constructor for nodes without explicit group properties.
     */
    public ModelOutlinerNode(
            String uuid,
            String name,
            Type type,
            ModelVector3 origin,
            ModelVector3 rotation,
            String cubeUuid,
            List<ModelOutlinerNode> children,
            String rawJson
    ) {
        this(
                uuid,
                name,
                type,
                origin,
                rotation,
                cubeUuid,
                children,
                rawJson,
                true,
                true,
                false,
                -1,
                true,
                true,
                false,
                0
        );
    }

    public boolean isGroup() {
        return type == Type.GROUP;
    }

    public boolean isCube() {
        return type == Type.CUBE;
    }
}
