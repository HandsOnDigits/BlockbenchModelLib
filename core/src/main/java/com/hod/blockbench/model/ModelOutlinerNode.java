package com.hod.blockbench.model;

import java.util.List;

/**
 * A node in the Blockbench outliner.
 *
 * Elements and groups are represented independently.
 * Groups retain their transforms and hierarchy for animation.
 */
public record ModelOutlinerNode(
    String uuid,
    String name,
    Type type,
    ModelVector3 origin,
    ModelVector3 rotation,
    String elementUuid,
    List<ModelOutlinerNode> children,
    String rawJson,
    boolean visible,
    boolean export,
    boolean locked,
    int color,
    boolean isOpen,
    boolean shade,
    boolean mirrorUv,
    int autoUv
) {
    public ModelOutlinerNode {
        children = children == null ? List.of() : List.copyOf(children);

        origin = origin != null ? origin : ModelVector3.ZERO;
        rotation = rotation != null ? rotation : ModelVector3.ZERO;
        rawJson = rawJson != null ? rawJson : "";
    }

    public boolean isGroup() {
        return type == Type.GROUP;
    }

    public boolean isElement() {
        return type == Type.ELEMENT;
    }

    public enum Type {
        GROUP,
        ELEMENT,
        OTHER,
    }
}
