package utils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DBSCAN {
    private double eps;
    private int minPts;

    public DBSCAN(double eps, int minPts) {
        this.eps = eps;
        this.minPts = minPts;
    }

    public List<Set<Integer>> performDBSCAN(double[][] points) {
        List<Set<Integer>> clusters = new ArrayList<>();
        int n = points.length;
        boolean[] visited = new boolean[n];

        for (int i = 0; i < n; i++) {
            if (visited[i]) {
                continue;
            }
            visited[i] = true;

            Set<Integer> clusterIndices = getNeighbors(points, i);
            if (clusterIndices.size() >= minPts) {
                expandCluster(points, visited, clusterIndices, i);
                clusters.add(clusterIndices);
            }
        }
        return clusters;
    }

    private Set<Integer> getNeighbors(double[][] points, int i) {
        Set<Integer> neighbors = new HashSet<>();
        for (int j = 0; j < points.length; j++) {
            if (distance(points[i], points[j]) <= eps) {
                neighbors.add(j);
            }
        }
        return neighbors;
    }

    private void expandCluster(double[][] points, boolean[] visited, Set<Integer> clusterIndices, int i) {
        List<Integer> seedList = new ArrayList<>(clusterIndices);
        int index = 0;
        while (index < seedList.size()) {
            int pointIndex = seedList.get(index);
            index++;

            Set<Integer> neighbors = getNeighbors(points, pointIndex);
            if (neighbors.size() >= minPts) {
                for (int neighborIndex : neighbors) {
                    if (!visited[neighborIndex]) {
                        visited[neighborIndex] = true;
                        seedList.add(neighborIndex);
                        clusterIndices.add(neighborIndex);
                    }
                }
            }
        }
    }

    private double distance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }
}