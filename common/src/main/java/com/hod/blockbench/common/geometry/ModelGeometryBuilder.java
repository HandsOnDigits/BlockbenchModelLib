package com.hod.blockbench.common.geometry;

import com.hod.blockbench.model.BlockbenchModel;
import com.hod.blockbench.model.ModelElement;
import com.hod.blockbench.model.ModelFaceVertex;
import com.hod.blockbench.model.ModelOutlinerNode;
import com.hod.blockbench.model.ModelPolygonFace;
import com.hod.blockbench.model.ModelTexture;
import com.hod.blockbench.model.ModelVector3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ModelGeometryBuilder {

    private static final float EPSILON = 0.000001F;

    private ModelGeometryBuilder() {}

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * Builds flat geometry with all group and element transformations baked
     * into vertex positions.
     */
    public static ModelGeometry build(BlockbenchModel model) {
        List<ModelTriangle> triangles = new ArrayList<>();

        Map<String, ModelElement> elementsByUuid = indexElements(model);

        Set<String> referencedElements = new HashSet<>();
        collectElementReferences(model.outliner(), referencedElements);

        Set<String> emittedElements = new HashSet<>();

        for (ModelOutlinerNode node : model.outliner()) {
            buildFlatNode(
                model,
                node,
                elementsByUuid,
                ModelTransform.identity(),
                true,
                emittedElements,
                triangles
            );
        }

        // Include elements that do not appear in the outliner.
        for (ModelElement element : model.elements()) {
            if (element == null || element.uuid() == null) {
                continue;
            }

            if (referencedElements.contains(element.uuid())) {
                continue;
            }

            if (!emittedElements.add(element.uuid())) {
                continue;
            }

            buildElement(model, element, ModelTransform.identity(), triangles);
        }

        return new ModelGeometry(triangles);
    }

    /**
     * Builds geometry while preserving the outliner group hierarchy.
     *
     * Group transforms are retained for runtime rendering.
     * Element transforms are baked into their own vertices.
     */
    public static ModelGeometryHierarchy buildHierarchy(BlockbenchModel model) {
        Map<String, ModelElement> elementsByUuid = indexElements(model);

        Set<String> referencedElements = new HashSet<>();
        collectElementReferences(model.outliner(), referencedElements);

        Set<String> emittedElements = new HashSet<>();

        List<ModelGeometryNode> roots = new ArrayList<>();
        List<ModelTriangle> rootTriangles = new ArrayList<>();

        for (ModelOutlinerNode node : model.outliner()) {
            appendHierarchyNode(
                model,
                node,
                elementsByUuid,
                emittedElements,
                rootTriangles,
                roots,
                true
            );
        }

        // Include elements that do not appear in the outliner.
        for (ModelElement element : model.elements()) {
            if (element == null || element.uuid() == null) {
                continue;
            }

            if (referencedElements.contains(element.uuid())) {
                continue;
            }

            if (!emittedElements.add(element.uuid())) {
                continue;
            }

            buildElement(
                model,
                element,
                ModelTransform.identity(),
                rootTriangles
            );
        }

        return new ModelGeometryHierarchy(roots, rootTriangles);
    }

    // ---------------------------------------------------------------------
    // Element lookup
    // ---------------------------------------------------------------------

    private static Map<String, ModelElement> indexElements(
        BlockbenchModel model
    ) {
        Map<String, ModelElement> result = new HashMap<>();

        for (ModelElement element : model.elements()) {
            if (element != null && element.uuid() != null) {
                result.put(element.uuid(), element);
            }
        }

        return result;
    }

    private static void collectElementReferences(
        List<ModelOutlinerNode> nodes,
        Set<String> references
    ) {
        for (ModelOutlinerNode node : nodes) {
            if (node == null) {
                continue;
            }

            if (node.isElement() && node.elementUuid() != null) {
                references.add(node.elementUuid());
            }

            collectElementReferences(node.children(), references);
        }
    }

    // ---------------------------------------------------------------------
    // Flat geometry
    // ---------------------------------------------------------------------

    private static void buildFlatNode(
        BlockbenchModel model,
        ModelOutlinerNode node,
        Map<String, ModelElement> elementsByUuid,
        ModelTransform parentTransform,
        boolean parentVisible,
        Set<String> emittedElements,
        List<ModelTriangle> output
    ) {
        if (node == null) {
            return;
        }

        boolean visible = parentVisible && node.visible();

        if (!visible) {
            return;
        }

        ModelTransform localTransform = ModelTransform.identity();

        if (node.isGroup()) {
            localTransform = ModelTransform.aroundOrigin(
                originOrZero(node.origin()),
                rotationOrZero(node.rotation())
            );
        }

        ModelTransform accumulatedTransform = parentTransform.multiply(
            localTransform
        );

        if (node.isElement()) {
            String elementUuid = node.elementUuid();

            if (elementUuid == null || !emittedElements.add(elementUuid)) {
                return;
            }

            ModelElement element = elementsByUuid.get(elementUuid);

            if (element == null || !element.visible()) {
                return;
            }

            buildElement(model, element, accumulatedTransform, output);

            return;
        }

        for (ModelOutlinerNode child : node.children()) {
            buildFlatNode(
                model,
                child,
                elementsByUuid,
                accumulatedTransform,
                visible,
                emittedElements,
                output
            );
        }
    }

    // ---------------------------------------------------------------------
    // Hierarchical geometry
    // ---------------------------------------------------------------------

    private static void appendHierarchyNode(
        BlockbenchModel model,
        ModelOutlinerNode node,
        Map<String, ModelElement> elementsByUuid,
        Set<String> emittedElements,
        List<ModelTriangle> parentTriangles,
        List<ModelGeometryNode> parentChildren,
        boolean parentVisible
    ) {
        if (node == null) {
            return;
        }

        boolean visible = parentVisible && node.visible();

        if (!visible) {
            return;
        }

        if (node.isElement()) {
            String elementUuid = node.elementUuid();

            if (elementUuid == null || !emittedElements.add(elementUuid)) {
                return;
            }

            ModelElement element = elementsByUuid.get(elementUuid);

            if (element == null || !element.visible()) {
                return;
            }

            // Element rotation is baked into its local geometry.
            // Parent group transformations remain runtime transforms.
            buildElement(
                model,
                element,
                ModelTransform.identity(),
                parentTriangles
            );

            return;
        }

        if (node.isGroup()) {
            List<ModelTriangle> groupTriangles = new ArrayList<>();
            List<ModelGeometryNode> groupChildren = new ArrayList<>();

            for (ModelOutlinerNode child : node.children()) {
                appendHierarchyNode(
                    model,
                    child,
                    elementsByUuid,
                    emittedElements,
                    groupTriangles,
                    groupChildren,
                    visible
                );
            }

            parentChildren.add(
                new ModelGeometryNode(
                    node.uuid(),
                    node.name(),
                    originOrZero(node.origin()),
                    rotationOrZero(node.rotation()),
                    groupTriangles,
                    groupChildren
                )
            );

            return;
        }

        // Other nodes do not introduce a transform.
        for (ModelOutlinerNode child : node.children()) {
            appendHierarchyNode(
                model,
                child,
                elementsByUuid,
                emittedElements,
                parentTriangles,
                parentChildren,
                visible
            );
        }
    }

    // ---------------------------------------------------------------------
    // Element geometry
    // ---------------------------------------------------------------------

    private static void buildElement(
        BlockbenchModel model,
        ModelElement element,
        ModelTransform parentTransform,
        List<ModelTriangle> output
    ) {
        if (!element.visible() || !element.hasGeometry()) {
            return;
        }

        ModelVector3 origin = originOrZero(element.origin());
        ModelVector3 rotation = rotationOrZero(element.rotation());

        ModelTransform elementTransform;

        if ("mesh".equalsIgnoreCase(element.originalType())) {
            // Free-format mesh vertices are element-local.
            // Rotate locally, then position the element at its origin.
            elementTransform = ModelTransform.translation(
                origin.x(),
                origin.y(),
                origin.z()
            ).multiply(ModelTransform.rotation(rotation));
        } else {
            // Cube vertices are already in model coordinates.
            // Rotate them around the element's pivot.
            elementTransform = ModelTransform.aroundOrigin(origin, rotation);
        }

        ModelTransform transform = parentTransform.multiply(elementTransform);

        for (ModelPolygonFace face : element.faces()) {
            if (face == null || face.vertices().size() < 3) {
                continue;
            }

            Optional<ModelTexture> textureOptional = model.findTexture(
                face.texture()
            );

            if (textureOptional.isEmpty()) {
                continue;
            }

            ModelTexture texture = textureOptional.get();

            if (texture.uvWidth() <= 0 || texture.uvHeight() <= 0) {
                continue;
            }

            buildFace(element, face, texture, transform, output);
        }
    }

    private static void buildFace(
        ModelElement element,
        ModelPolygonFace face,
        ModelTexture texture,
        ModelTransform transform,
        List<ModelTriangle> output
    ) {
        List<ModelVertex> vertices = new ArrayList<>(face.vertices().size());

        for (ModelFaceVertex corner : face.vertices()) {
            if (corner == null) {
                return;
            }

            ModelVector3 position = element.vertices().get(corner.vertexUuid());

            if (position == null) {
                return;
            }

            ModelVector3 transformedPosition = transform.transformPosition(
                position
            );

            float u = corner.u() / texture.uvWidth();
            float v = corner.v() / texture.uvHeight();

            vertices.add(
                new ModelVertex(transformedPosition, u, v, ModelVector3.ZERO)
            );
        }

        ModelVector3 normal = calculateNormal(vertices);

        if (normal == null) {
            return;
        }

        ModelVector3 transformedNormal = normalize(normal);

        if (lengthSquared(transformedNormal) <= EPSILON) {
            return;
        }

        List<ModelVertex> normalizedVertices = new ArrayList<>(vertices.size());

        for (ModelVertex vertex : vertices) {
            normalizedVertices.add(
                new ModelVertex(
                    vertex.position(),
                    vertex.u(),
                    vertex.v(),
                    transformedNormal
                )
            );
        }

        List<int[]> triangulation = triangulate(normalizedVertices);

        for (int[] triangleIndices : triangulation) {
            output.add(
                new ModelTriangle(
                    face.texture(),
                    List.of(
                        normalizedVertices.get(triangleIndices[0]),
                        normalizedVertices.get(triangleIndices[1]),
                        normalizedVertices.get(triangleIndices[2])
                    )
                )
            );
        }
    }

    // ---------------------------------------------------------------------
    // Polygon normal
    // ---------------------------------------------------------------------

    /**
     * Calculates a polygon normal using Newell's method.
     *
     * This works for arbitrary planar polygons, including concave polygons.
     */
    private static ModelVector3 calculateNormal(List<ModelVertex> vertices) {
        float nx = 0;
        float ny = 0;
        float nz = 0;

        int count = vertices.size();

        for (int i = 0; i < count; i++) {
            ModelVector3 current = vertices.get(i).position();
            ModelVector3 next = vertices.get((i + 1) % count).position();

            nx += (current.y() - next.y()) * (current.z() + next.z());
            ny += (current.z() - next.z()) * (current.x() + next.x());
            nz += (current.x() - next.x()) * (current.y() + next.y());
        }

        return normalize(new ModelVector3(nx, ny, nz));
    }

    // ---------------------------------------------------------------------
    // Polygon triangulation
    // ---------------------------------------------------------------------

    /**
     * Triangulates a polygon using ear clipping.
     *
     * The polygon is projected onto its dominant plane before clipping.
     * Original corner indices are retained, so UV coordinates remain
     * attached to their corresponding vertices.
     *
     * Degenerate or self-intersecting polygons that cannot be triangulated
     * are skipped instead of generating invalid geometry.
     */
    private static List<int[]> triangulate(List<ModelVertex> vertices) {
        int count = vertices.size();

        if (count < 3) {
            return List.of();
        }

        if (count == 3) {
            return List.of(new int[] { 0, 1, 2 });
        }

        ModelVector3 normal = calculateNormal(vertices);

        if (normal == null) {
            return List.of();
        }

        int dominantAxis = dominantAxis(normal);

        List<Point2> points = new ArrayList<>(count);

        for (ModelVertex vertex : vertices) {
            points.add(project(vertex.position(), dominantAxis));
        }

        float signedArea = signedArea(points);

        if (Math.abs(signedArea) <= EPSILON) {
            return List.of();
        }

        float orientation = Math.signum(signedArea);

        List<Integer> remaining = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            remaining.add(i);
        }

        List<int[]> result = new ArrayList<>(count - 2);

        int attemptsWithoutEar = 0;
        int maximumAttempts = count * count;

        while (remaining.size() > 3) {
            boolean earFound = false;

            for (int i = 0; i < remaining.size(); i++) {
                int previousIndex = remaining.get(
                    (i + remaining.size() - 1) % remaining.size()
                );

                int currentIndex = remaining.get(i);

                int nextIndex = remaining.get((i + 1) % remaining.size());

                Point2 previous = points.get(previousIndex);
                Point2 current = points.get(currentIndex);
                Point2 next = points.get(nextIndex);

                float cross = cross2(previous, current, next);

                // The corner must be convex for the polygon winding.
                if (orientation * cross <= EPSILON) {
                    continue;
                }

                boolean containsAnotherPoint = false;

                for (int candidate : remaining) {
                    if (
                        candidate == previousIndex ||
                        candidate == currentIndex ||
                        candidate == nextIndex
                    ) {
                        continue;
                    }

                    if (
                        pointInsideTriangle(
                            points.get(candidate),
                            previous,
                            current,
                            next,
                            orientation
                        )
                    ) {
                        containsAnotherPoint = true;
                        break;
                    }
                }

                if (containsAnotherPoint) {
                    continue;
                }

                result.add(new int[] {
                    previousIndex,
                    currentIndex,
                    nextIndex,
                });

                remaining.remove(i);
                earFound = true;
                attemptsWithoutEar = 0;
                break;
            }

            if (!earFound) {
                attemptsWithoutEar++;

                if (attemptsWithoutEar >= maximumAttempts) {
                    return List.of();
                }

                // Remove a collinear point if one is present.
                boolean removedCollinear = false;

                for (int i = 0; i < remaining.size(); i++) {
                    int previousIndex = remaining.get(
                        (i + remaining.size() - 1) % remaining.size()
                    );

                    int currentIndex = remaining.get(i);

                    int nextIndex = remaining.get((i + 1) % remaining.size());

                    float cross = cross2(
                        points.get(previousIndex),
                        points.get(currentIndex),
                        points.get(nextIndex)
                    );

                    if (Math.abs(cross) <= EPSILON) {
                        remaining.remove(i);
                        removedCollinear = true;
                        break;
                    }
                }

                if (!removedCollinear) {
                    return List.of();
                }
            }
        }

        if (remaining.size() == 3) {
            result.add(new int[] {
                remaining.get(0),
                remaining.get(1),
                remaining.get(2),
            });
        }

        return List.copyOf(result);
    }

    private record Point2(float x, float y) {}

    private static int dominantAxis(ModelVector3 normal) {
        float x = Math.abs(normal.x());
        float y = Math.abs(normal.y());
        float z = Math.abs(normal.z());

        if (x >= y && x >= z) {
            return 0;
        }

        if (y >= x && y >= z) {
            return 1;
        }

        return 2;
    }

    private static Point2 project(ModelVector3 position, int dominantAxis) {
        return switch (dominantAxis) {
            case 0 -> new Point2(position.y(), position.z());
            case 1 -> new Point2(position.x(), position.z());
            default -> new Point2(position.x(), position.y());
        };
    }

    private static float signedArea(List<Point2> points) {
        float area = 0;

        for (int i = 0; i < points.size(); i++) {
            Point2 current = points.get(i);
            Point2 next = points.get((i + 1) % points.size());

            area += current.x() * next.y() - next.x() * current.y();
        }

        return area * 0.5F;
    }

    private static float cross2(Point2 a, Point2 b, Point2 c) {
        return (
            (b.x() - a.x()) * (c.y() - a.y()) -
            (b.y() - a.y()) * (c.x() - a.x())
        );
    }

    private static boolean pointInsideTriangle(
        Point2 point,
        Point2 a,
        Point2 b,
        Point2 c,
        float orientation
    ) {
        float ab = orientation * cross2(a, b, point);
        float bc = orientation * cross2(b, c, point);
        float ca = orientation * cross2(c, a, point);

        return ab >= -EPSILON && bc >= -EPSILON && ca >= -EPSILON;
    }

    // ---------------------------------------------------------------------
    // Vector helpers
    // ---------------------------------------------------------------------

    private static ModelVector3 normalize(ModelVector3 vector) {
        float lengthSquared = lengthSquared(vector);

        if (!Float.isFinite(lengthSquared) || lengthSquared <= EPSILON) {
            return ModelVector3.ZERO;
        }

        float inverseLength = 1.0F / (float) Math.sqrt(lengthSquared);

        return vector.multiply(inverseLength);
    }

    private static float lengthSquared(ModelVector3 vector) {
        return (
            vector.x() * vector.x() +
            vector.y() * vector.y() +
            vector.z() * vector.z()
        );
    }

    private static ModelVector3 originOrZero(ModelVector3 origin) {
        return origin != null ? origin : ModelVector3.ZERO;
    }

    private static ModelVector3 rotationOrZero(ModelVector3 rotation) {
        return rotation != null ? rotation : ModelVector3.ZERO;
    }
}
