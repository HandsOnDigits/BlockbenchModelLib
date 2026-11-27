package com.hod.blockbench.model;

/**
 * A single corner of a polygon face.
 *
 * UV coordinates belong to the face corner rather than the geometric
 * vertex, because a shared vertex can have different UVs on different faces.
 */
public record ModelFaceVertex(String vertexUuid, float u, float v) {}
