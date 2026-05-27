package sketchlib.scratchview;

import android.graphics.Path;
import android.graphics.PointF;
import android.util.SparseArray;

public class BezierPathManager {
    private final SparseArray<Path> paths = new SparseArray<>();
    private final SparseArray<PointF> previousPoints = new SparseArray<>();
    private final SparseArray<PointF> startPoints = new SparseArray<>();

    public Path startPath(int pointerId, float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        paths.put(pointerId, path);
        previousPoints.put(pointerId, new PointF(x, y));
        startPoints.put(pointerId, new PointF(x, y));
        return path;
    }

    public Path updatePath(int pointerId, float x, float y) {
        Path path = paths.get(pointerId);
        PointF prev = previousPoints.get(pointerId);
        
        if (path != null && prev != null) {
            float dx = Math.abs(x - prev.x);
            float dy = Math.abs(y - prev.y);
            
            // Smoothing threshold
            if (dx >= 3f || dy >= 3f) {
                // Bezier curve smoothing
                path.quadTo(prev.x, prev.y, (x + prev.x) / 2, (y + prev.y) / 2);
                prev.set(x, y);
            }
        }
        return path;
    }

    public void endPath(int pointerId) {
        paths.remove(pointerId);
        previousPoints.remove(pointerId);
        startPoints.remove(pointerId);
    }

    public void clear() {
        paths.clear();
        previousPoints.clear();
        startPoints.clear();
    }
}
