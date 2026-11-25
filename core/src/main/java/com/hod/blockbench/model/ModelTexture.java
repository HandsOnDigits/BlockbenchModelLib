package com.hod.blockbench.model;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public record ModelTexture(
        String id,
        String name,
        int width,
        int height,
        int uvWidth,
        int uvHeight,
        int frameTime,
        String frameOrderType,
        String frameOrder,
        boolean frameInterpolate,
        String source,
        byte[] imageData,
        String path,
        String folder,
        String namespace,
        boolean internal,
        boolean saved
) {

    public ModelTexture {
        imageData = imageData == null ? null : imageData.clone();
    }

    public ModelTexture(
            String id,
            String name,
            int width,
            int height,
            int uvWidth,
            int uvHeight,
            int frameTime,
            String frameOrderType,
            String frameOrder,
            boolean frameInterpolate,
            String source,
            byte[] imageData
    ) {
        this(
                id,
                name,
                width,
                height,
                uvWidth,
                uvHeight,
                frameTime,
                frameOrderType,
                frameOrder,
                frameInterpolate,
                source,
                imageData,
                "",
                "",
                "",
                false,
                false
        );
    }

    @Override
    public byte[] imageData() {
        return imageData == null ? null : imageData.clone();
    }

    public boolean hasEmbeddedImage() {
        return imageData != null && imageData.length > 0;
    }

    public boolean hasExternalPath() {
        return path != null && !path.isBlank();
    }

    public BufferedImage decodeImage() throws IOException {
        if (!hasEmbeddedImage()) {
            throw new IllegalStateException(
                    "Texture does not contain embedded image data: " + name
            );
        }

        BufferedImage image = ImageIO.read(
                new ByteArrayInputStream(imageData)
        );

        if (image == null) {
            throw new IOException(
                    "Unsupported or invalid texture image: " + name
            );
        }

        return image;
    }
}