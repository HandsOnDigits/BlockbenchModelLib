package com.hod.blockbench.model;

public record ModelMetadata(
        String formatVersion,
        String modelFormat,
        boolean boxUv,
        int textureWidth,
        int textureHeight
) {
}