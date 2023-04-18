package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

public class ConcaveHull {

    private static final double DEFAULT_ALPHA = 2.0;

    public ConcaveHull(){

    }
    public static List<double[]> concaveHull(List<double[]> points) {
        return concaveHull(points, DEFAULT_ALPHA);
    }

    public static List<double[]> concaveHull(List<double[]> points, double alpha) {
        List<double[]> hull = new ArrayList<>();
        if (points.size() < 3) {
            return points;
        }
        // Sort points by x-coordinate
        Collections.sort(points, new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                return Double.compare(o1[0], o2[0]);
            }
        });
        // Find the leftmost and rightmost points
        double[] leftmost = points.get(0);
        double[] rightmost = points.get(points.size() - 1);
        // Add leftmost and rightmost points to the hull
        hull.add(leftmost);
        hull.add(rightmost);
        // Divide the points into two subsets: above and below the line connecting the leftmost and rightmost points
        List<double[]> above = new ArrayList<>();
        List<double[]> below = new ArrayList<>();
        for (int i = 1; i < points.size() - 1; i++) {
            double[] p = points.get(i);
            if (orientation(leftmost, rightmost, p) >= 0) {
                above.add(p);
            } else {
                below.add(p);
            }
        }
        // Recursively compute the concave hull of the above and below subsets
        computeHull(above, leftmost, rightmost, alpha, hull);
        computeHull(below, rightmost, leftmost, alpha, hull);
        return hull;
    }

    private static void computeHull(List<double[]> points, double[] p1, double[] p2, double alpha, List<double[]> hull) {
        if (points.isEmpty()) {
            return;
        }
        if (points.size() == 1) {
            hull.add(points.get(0));
            return;
        }
        // Find the point with the maximum distance from the line connecting p1 and p2
        double maxDist = Double.NEGATIVE_INFINITY;
        double[] farthest = null;
        for (double[] p : points) {
            double dist = distanceToLine(p1, p2, p);
            if (dist > maxDist) {
                maxDist = dist;
                farthest = p;
            }
        }
        // If the maximum distance is less than alpha, add the endpoints of the segment to the hull and return
        if (maxDist < alpha) {
            if (!hull.contains(p1)) {
                hull.add(hull.indexOf(p2), p1);
            }
            if (!hull.contains(p2)) {
                hull.add(hull.indexOf(p1), p2);
            }
            return;
        }
        // Divide the remaining points into two subsets: those above and those below the line connecting p1 and p2
        List<double[]> above = new ArrayList<>();
        List<double[]> below = new ArrayList<>();
        for (double[] p : points) {
            if (orientation(p1, farthest, p) >= 0) {
                above.add(p);
            }
            if (orientation(farthest, p2, p) >= 0) {
                below.add(p);
            }
        }
        // Recursively compute the concave hull of the above and below subsets
        computeHull(above, p1, farthest, alpha, hull);
        computeHull(below, farthest, p2, alpha, hull);
    }

    private static int orientation(double[] p1, double[] p2, double[] p3) {
        double val = (p2[1] - p1[1]) * (p3[0] - p2[0]) - (p2[0] - p1[0]) * (p3[1] - p2[1]);
        if (val == 0.0) {
            return 0;
        } else {
            return (val > 0.0) ? 1 : -1;
        }
    }

    private static double distanceToLine(double[] p1, double[] p2, double[] p3) {
        double x1 = p1[0];
        double y1 = p1[1];
        double x2 = p2[0];
        double y2 = p2[1];
        double x3 = p3[0];
        double y3 = p3[1];
        double numer = Math.abs((y2 - y1) * x3 - (x2 - x1) * y3 + x2 * y1 - y2 * x1);
        double denom = Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
        return numer / denom;
    }

}
