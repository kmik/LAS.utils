package utils;

import java.util.ArrayList;

public class Quickhull {
    public static ArrayList<double[]> findConvexHull(ArrayList<double[]> p, double threshold) {
        // Find the leftmost and rightmost points
        double[] leftmost = p.get(0);
        double[] rightmost = p.get(0);
        for (double[] point : p) {
            if (point[0] < leftmost[0]) {
                leftmost = point;
            }
            if (point[0] > rightmost[0]) {
                rightmost = point;
            }
        }

        // Add the leftmost and rightmost points to the hull
        ArrayList<double[]> hull = new ArrayList<double[]>();
        hull.add(leftmost);
        hull.add(rightmost);

        // Recursively find the points above and below the line connecting the leftmost and rightmost points
        findHull(p, leftmost, rightmost, hull, threshold);
        findHull(p, rightmost, leftmost, hull, threshold);

        return hull;
    }

    private static void findHull(ArrayList<double[]> p, double[] a, double[] b, ArrayList<double[]> hull, double threshold) {
        // Find the point farthest from the line connecting a and b, but only if it is farther than the threshold
        double maxDist = -1;
        double[] farthest = null;
        for (double[] point : p) {
            double dist = distToLine(point, a, b);
            if (dist > maxDist && dist > threshold) {
                maxDist = dist;
                farthest = point;
            }
        }

        // If no point is found, we're done
        if (farthest == null) {
            return;
        }

        // Add the farthest point to the hull
        hull.add(hull.indexOf(b), farthest);

        // Recursively find the points above and below the line connecting a and farthest, but only if they are farther than the threshold
        findHull(p, a, farthest, hull, threshold);
        findHull(p, farthest, b, hull, threshold);
    }

    private static double distToLine(double[] point, double[] a, double[] b) {
        // Compute the distance from point to the line connecting a and b
        double x1 = a[0];
        double y1 = a[1];
        double x2 = b[0];
        double y2 = b[1];
        double x0 = point[0];
        double y0 = point[1];
        return Math.abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1) / Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
    }
}

