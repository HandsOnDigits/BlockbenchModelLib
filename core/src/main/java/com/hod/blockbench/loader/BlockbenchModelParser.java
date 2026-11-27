package com.hod.blockbench.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.hod.blockbench.model.*;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BlockbenchModelParser {

    private BlockbenchModelParser() {}

    static BlockbenchModel parse(
        Reader reader
    ) throws BlockbenchModelException {
        try {
            JsonElement element = JsonParser.parseReader(reader);

            if (!element.isJsonObject()) {
                throw new BlockbenchModelException(
                    "The Blockbench model root must be a JSON object."
                );
            }

            return parseModel(element.getAsJsonObject());
        } catch (JsonParseException | IllegalStateException exception) {
            throw new BlockbenchModelException(
                "Failed to parse Blockbench model JSON.",
                exception
            );
        }
    }

    private static BlockbenchModel parseModel(
        JsonObject root
    ) throws BlockbenchModelException {
        JsonObject meta = object(root, "meta");

        String modelFormat = string(meta, "model_format", "");

        if (!modelFormat.equals("free")) {
            throw new BlockbenchModelException(
                "Unsupported Blockbench model format: " + modelFormat
            );
        }

        JsonObject resolution = object(root, "resolution");

        ModelMetadata metadata = new ModelMetadata(
            string(meta, "format_version", ""),
            modelFormat,
            bool(meta, "box_uv", false),
            integer(resolution, "width", 16),
            integer(resolution, "height", 16)
        );

        List<ModelCube> cubes = parseCubes(array(root, "elements"));
        List<ModelTexture> textures = parseTextures(array(root, "textures"));
        List<ModelAnimation> animations = parseAnimations(
            array(root, "animations")
        );

        Set<String> cubeIds = new HashSet<>();

        for (ModelCube cube : cubes) {
            if (!cubeIds.add(cube.uuid())) {
                throw new BlockbenchModelException(
                    "Duplicate cube UUID: " + cube.uuid()
                );
            }
        }

        List<ModelOutlinerNode> outliner = parseOutliner(
            array(root, "outliner"),
            array(root, "groups"),
            cubes
        );

        return new BlockbenchModel(
            string(root, "name", "Unnamed Model"),
            string(root, "model_identifier", ""),
            metadata,
            cubes,
            outliner,
            textures,
            animations,
            root.toString()
        );
    }

    private static List<ModelAnimation> parseAnimations(
        JsonArray elements
    ) throws BlockbenchModelException {
        List<ModelAnimation> animations = new ArrayList<>();

        Set<String> animationIds = new HashSet<>();

        for (JsonElement element : elements) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject animation = element.getAsJsonObject();

            String uuid = string(animation, "uuid", "");

            if (uuid.isBlank()) {
                throw new BlockbenchModelException(
                    "An animation is missing its UUID."
                );
            }

            if (!animationIds.add(uuid)) {
                throw new BlockbenchModelException(
                    "Duplicate animation UUID: " + uuid
                );
            }

            animations.add(
                new ModelAnimation(
                    uuid,
                    string(animation, "name", ""),
                    string(animation, "loop", "once"),
                    bool(animation, "override", false),
                    decimal(animation, "length", 0),
                    integer(animation, "snapping", 0),
                    bool(animation, "selected", false),
                    string(animation, "group_name", ""),
                    integer(animation, "scope", 0),
                    string(animation, "anim_time_update", ""),
                    string(animation, "blend_weight", ""),
                    string(animation, "start_delay", ""),
                    string(animation, "loop_delay", ""),
                    parseAnimators(object(animation, "animators"))
                )
            );
        }

        return List.copyOf(animations);
    }

    private static Map<String, ModelAnimator> parseAnimators(
        JsonObject animators
    ) throws BlockbenchModelException {
        Map<String, ModelAnimator> result = new LinkedHashMap<>();

        for (Map.Entry<String, JsonElement> entry : animators.entrySet()) {
            String targetUuid = entry.getKey();

            if (!entry.getValue().isJsonObject()) {
                continue;
            }

            JsonObject animator = entry.getValue().getAsJsonObject();

            if (targetUuid.isBlank()) {
                throw new BlockbenchModelException(
                    "An animator is missing its target UUID."
                );
            }

            result.put(
                targetUuid,
                new ModelAnimator(
                    targetUuid,
                    string(animator, "name", ""),
                    string(animator, "type", ""),
                    bool(animator, "rotation_global", false),
                    bool(animator, "quaternion_interpolation", false),
                    parseKeyframes(array(animator, "keyframes"))
                )
            );
        }

        return result;
    }

    private static List<AnimationKeyframe> parseKeyframes(
        JsonArray keyframes
    ) throws BlockbenchModelException {
        List<AnimationKeyframe> result = new ArrayList<>();

        Set<String> keyframeIds = new HashSet<>();

        for (JsonElement element : keyframes) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject keyframe = element.getAsJsonObject();

            String uuid = string(keyframe, "uuid", "");

            if (uuid.isBlank()) {
                throw new BlockbenchModelException(
                    "An animation keyframe is missing its UUID."
                );
            }

            if (!keyframeIds.add(uuid)) {
                throw new BlockbenchModelException(
                    "Duplicate animation keyframe UUID: " + uuid
                );
            }

            result.add(
                new AnimationKeyframe(
                    uuid,
                    string(keyframe, "channel", ""),
                    decimal(keyframe, "time", 0),
                    string(keyframe, "interpolation", "linear"),
                    integer(keyframe, "color", -1),
                    parseAnimationDataPoints(array(keyframe, "data_points"))
                )
            );
        }

        return List.copyOf(result);
    }

    private static List<AnimationDataPoint> parseAnimationDataPoints(
        JsonArray dataPoints
    ) {
        List<AnimationDataPoint> result = new ArrayList<>();

        for (JsonElement element : dataPoints) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject dataPoint = element.getAsJsonObject();

            result.add(
                new AnimationDataPoint(
                    string(dataPoint, "x", "0"),
                    string(dataPoint, "y", "0"),
                    string(dataPoint, "z", "0")
                )
            );
        }

        return List.copyOf(result);
    }

    private static List<ModelCube> parseCubes(
        JsonArray elements
    ) throws BlockbenchModelException {
        List<ModelCube> cubes = new ArrayList<>();

        for (JsonElement element : elements) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject cube = element.getAsJsonObject();

            String uuid = string(cube, "uuid", null);

            if (uuid == null || uuid.isBlank()) {
                throw new BlockbenchModelException(
                    "A cube is missing its UUID."
                );
            }

            Map<ModelFace.Direction, ModelFace> faces = new EnumMap<>(
                ModelFace.Direction.class
            );

            JsonObject faceObject = object(cube, "faces");

            for (ModelFace.Direction direction : ModelFace.Direction.values()) {
                String key = direction.name().toLowerCase();

                if (
                    !faceObject.has(key) || !faceObject.get(key).isJsonObject()
                ) {
                    continue;
                }

                JsonObject face = faceObject.getAsJsonObject(key);

                JsonArray uv = array(face, "uv");

                if (uv.size() != 4) {
                    throw new BlockbenchModelException(
                        "Invalid UV coordinates on cube " + uuid
                    );
                }

                faces.put(
                    direction,
                    new ModelFace(
                        direction,
                        new ModelFace.ModelUv(
                            uv.get(0).getAsFloat(),
                            uv.get(1).getAsFloat(),
                            uv.get(2).getAsFloat(),
                            uv.get(3).getAsFloat()
                        ),
                        integer(face, "texture", -1),
                        integer(face, "rotation", 0)
                    )
                );
            }

            cubes.add(
                new ModelCube(
                    uuid,
                    string(cube, "name", uuid),
                    bool(cube, "visibility", true),
                    vector(cube, "from"),
                    vector(cube, "to"),
                    vector(cube, "origin"),
                    vector(cube, "rotation"),
                    decimal(cube, "inflate", 0),
                    faces
                )
            );
        }

        return cubes;
    }

    private static String normalizeTexturePath(
        String path,
        String relativePath,
        String folder
    ) {
        if (path != null && !path.isBlank()) {
            return path;
        }

        if (relativePath == null || relativePath.isBlank()) {
            return "";
        }

        String normalized = relativePath.replace('\\', '/');

        String texturePrefix = "../textures/";

        if (normalized.startsWith(texturePrefix)) {
            normalized = normalized.substring(texturePrefix.length());
        }

        if (normalized.startsWith("textures/")) {
            normalized = normalized.substring("textures/".length());
        }

        if (normalized.startsWith("../") || normalized.startsWith("/")) {
            throw new IllegalArgumentException(
                "Invalid relative texture path: " + relativePath
            );
        }

        if (
            normalized.indexOf('/') < 0 && folder != null && !folder.isBlank()
        ) {
            normalized = folder + "/" + normalized;
        }

        return normalized;
    }

    private static List<ModelTexture> parseTextures(
        JsonArray elements
    ) throws BlockbenchModelException {
        List<ModelTexture> textures = new ArrayList<>();

        for (JsonElement element : elements) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject texture = element.getAsJsonObject();

            String source = string(texture, "source", "");
            byte[] imageData = decodeTexture(source);

            textures.add(
                new ModelTexture(
                    string(texture, "id", ""),
                    string(texture, "name", ""),
                    integer(texture, "width", 0),
                    integer(texture, "height", 0),
                    integer(texture, "uv_width", 0),
                    integer(texture, "uv_height", 0),
                    integer(texture, "frame_time", 1),
                    string(texture, "frame_order_type", ""),
                    string(texture, "frame_order", ""),
                    bool(texture, "frame_interpolate", false),
                    source,
                    imageData,
                    normalizeTexturePath(
                        string(texture, "path", ""),
                        string(texture, "relative_path", ""),
                        string(texture, "folder", "")
                    ),
                    string(texture, "folder", ""),
                    string(texture, "namespace", ""),
                    bool(texture, "internal", false),
                    bool(texture, "saved", false)
                )
            );
        }

        return textures;
    }

    private static byte[] decodeTexture(
        String source
    ) throws BlockbenchModelException {
        if (source == null || source.isBlank()) {
            return null;
        }

        String prefix = "base64,";

        int separator = source.indexOf(prefix);

        if (separator < 0) {
            return null;
        }

        try {
            return Base64.getDecoder().decode(
                source.substring(separator + prefix.length())
            );
        } catch (IllegalArgumentException exception) {
            throw new BlockbenchModelException(
                "Invalid embedded texture data.",
                exception
            );
        }
    }

    private static List<ModelOutlinerNode> parseOutliner(
        JsonArray outliner,
        JsonArray groups,
        List<ModelCube> cubes
    ) throws BlockbenchModelException {
        Map<String, JsonObject> groupDefinitions = new HashMap<>();
        Map<String, ModelCube> cubeDefinitions = new HashMap<>();

        for (ModelCube cube : cubes) {
            cubeDefinitions.put(cube.uuid(), cube);
        }

        // Index group definitions.
        for (JsonElement element : groups) {
            if (element.isJsonObject()) {
                JsonObject group = element.getAsJsonObject();
                String uuid = string(group, "uuid", "");

                if (!uuid.isBlank()) {
                    groupDefinitions.put(uuid, group);
                }
            }
        }

        // Also index groups embedded directly in the outliner.
        indexGroups(outliner, groupDefinitions);

        List<ModelOutlinerNode> result = new ArrayList<>();
        Set<String> resolving = new HashSet<>();

        for (JsonElement element : outliner) {
            result.add(
                resolveNode(
                    element,
                    groupDefinitions,
                    cubeDefinitions,
                    resolving
                )
            );
        }

        return result;
    }

    private static void indexGroups(
        JsonArray elements,
        Map<String, JsonObject> definitions
    ) {
        for (JsonElement element : elements) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject object = element.getAsJsonObject();
            String uuid = string(object, "uuid", "");

            if (!uuid.isBlank()) {
                definitions.putIfAbsent(uuid, object);
            }

            indexGroups(array(object, "children"), definitions);
        }
    }

    private static ModelOutlinerNode resolveNode(
        JsonElement element,
        Map<String, JsonObject> groups,
        Map<String, ModelCube> cubes,
        Set<String> resolving
    ) throws BlockbenchModelException {
        // Outliner entries can reference cubes or groups by UUID.
        if (
            element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
        ) {
            String uuid = element.getAsString();

            ModelCube cube = cubes.get(uuid);

            if (cube != null) {
                return new ModelOutlinerNode(
                    uuid,
                    cube.name(),
                    ModelOutlinerNode.Type.CUBE,
                    ModelVector3.ZERO,
                    ModelVector3.ZERO,
                    uuid,
                    List.of(),
                    element.toString()
                );
            }

            JsonObject group = groups.get(uuid);

            if (group != null) {
                return resolveGroup(group, groups, cubes, resolving);
            }

            return new ModelOutlinerNode(
                uuid,
                uuid,
                ModelOutlinerNode.Type.OTHER,
                ModelVector3.ZERO,
                ModelVector3.ZERO,
                null,
                List.of(),
                element.toString()
            );
        }

        // Outliner group objects may contain only partial group data.
        if (element.isJsonObject()) {
            JsonObject occurrence = element.getAsJsonObject();

            String uuid = string(occurrence, "uuid", "");
            JsonObject definition = groups.get(uuid);

            if (definition != null && definition != occurrence) {
                JsonObject merged = definition.deepCopy();

                // Preserve the full group definition while allowing
                // outliner-specific properties to override it.
                for (Map.Entry<
                    String,
                    JsonElement
                > entry : occurrence.entrySet()) {
                    merged.add(entry.getKey(), entry.getValue().deepCopy());
                }

                return resolveGroup(merged, groups, cubes, resolving);
            }

            return resolveGroup(occurrence, groups, cubes, resolving);
        }

        return new ModelOutlinerNode(
            "",
            "",
            ModelOutlinerNode.Type.OTHER,
            ModelVector3.ZERO,
            ModelVector3.ZERO,
            null,
            List.of(),
            element.toString()
        );
    }

    private static ModelOutlinerNode resolveGroup(
        JsonObject group,
        Map<String, JsonObject> groups,
        Map<String, ModelCube> cubes,
        Set<String> resolving
    ) throws BlockbenchModelException {
        String uuid = string(group, "uuid", "");

        boolean tracked = !uuid.isBlank();

        if (tracked && !resolving.add(uuid)) {
            throw new BlockbenchModelException(
                "Circular outliner reference: " + uuid
            );
        }

        try {
            List<ModelOutlinerNode> children = new ArrayList<>();

            for (JsonElement child : array(group, "children")) {
                children.add(resolveNode(child, groups, cubes, resolving));
            }

            return new ModelOutlinerNode(
                uuid,
                string(group, "name", ""),
                ModelOutlinerNode.Type.GROUP,
                vector(group, "origin"),
                vector(group, "rotation"),
                null,
                children,
                group.toString(),

                // Group properties
                bool(group, "visibility", true),
                bool(group, "export", true),
                bool(group, "locked", false),
                integer(group, "color", -1),
                bool(group, "isOpen", true),
                bool(group, "shade", true),
                bool(group, "mirror_uv", false),
                integer(group, "autouv", 0)
            );
        } finally {
            if (tracked) {
                resolving.remove(uuid);
            }
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent.has(key) && parent.get(key).isJsonObject()) {
            return parent.getAsJsonObject(key);
        }

        return new JsonObject();
    }

    private static JsonArray array(JsonObject parent, String key) {
        if (parent.has(key) && parent.get(key).isJsonArray()) {
            return parent.getAsJsonArray(key);
        }

        return new JsonArray();
    }

    private static String string(
        JsonObject object,
        String key,
        String fallback
    ) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }

        return object.get(key).getAsString();
    }

    private static boolean bool(
        JsonObject object,
        String key,
        boolean fallback
    ) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }

        return object.get(key).getAsBoolean();
    }

    private static int integer(JsonObject object, String key, int fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }

        return object.get(key).getAsInt();
    }

    private static float decimal(
        JsonObject object,
        String key,
        float fallback
    ) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }

        return object.get(key).getAsFloat();
    }

    private static ModelVector3 vector(
        JsonObject object,
        String key
    ) throws BlockbenchModelException {
        JsonArray values = array(object, key);

        if (values.isEmpty()) {
            return ModelVector3.ZERO;
        }

        if (values.size() != 3) {
            throw new BlockbenchModelException("Invalid vector: " + key);
        }

        return new ModelVector3(
            values.get(0).getAsFloat(),
            values.get(1).getAsFloat(),
            values.get(2).getAsFloat()
        );
    }
}
