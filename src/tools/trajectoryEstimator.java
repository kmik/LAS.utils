package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import org.ejml.simple.SimpleMatrix;
import utils.argumentReader;
import utils.pointWriterMultiThread;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class trajectoryEstimator {

    LASReader pointCloud;

    argumentReader aR;
    TreeMap<Double, ArrayList<double[]>> trajectory = new TreeMap<>();
    TreeMap<Double, ArrayList<double[]>> time_points_first = new TreeMap<>();
    //TreeMap<Double, double[]> first_and_last = new TreeMap<>();
    TreeMap<Double, ArrayList<double[]>> time_points_last = new TreeMap<>();

    ArrayList<double[]> first_and_last = new ArrayList<>();
    TreeMap<Integer, ArrayList<double[]>> firsts_and_lasts = new TreeMap<>();

    public trajectoryEstimator(){


    }
    public trajectoryEstimator(LASReader pointCloud, argumentReader aR) throws IOException {

        this.aR = aR;

        this.pointCloud = pointCloud;

        File outFile;
        pointWriterMultiThread pw;
        int maxi;

        int thread_n = aR.pfac.addReadThread(pointCloud);

        try {
            outFile = aR.createOutputFile(pointCloud);
            pw = new pointWriterMultiThread(outFile, pointCloud, "las2las", aR);

        }catch (Exception e){
            System.out.println("Error creating output file");
            return;
        }

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);

        LasPoint tempPoint = new LasPoint();

        LasPoint previousPoint = new LasPoint();

        int current_scan_angle = 0;
        int previous_scan_angle = 1;

        double current_gps_time = 0;
        double previous_gps_time = 0;

        int n_first_and_last = 0;

        int counter = 0;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i++) {

            pointCloud.readRecord(i, tempPoint);

            if(i == 0){
                current_gps_time = tempPoint.gpsTime;
                previous_gps_time = tempPoint.gpsTime;

                current_scan_angle = tempPoint.scanAngleRank;
                previous_scan_angle = tempPoint.scanAngleRank;
            }

            current_scan_angle = tempPoint.scanAngleRank;
            current_gps_time = tempPoint.gpsTime;

            if(current_scan_angle == previous_scan_angle && current_gps_time == previous_gps_time &&
                    ((tempPoint.numberOfReturns > 1 && tempPoint.returnNumber == 1) || (tempPoint.numberOfReturns > 1 && tempPoint.returnNumber == tempPoint.numberOfReturns)) ) {
                // Searching for pairs for this one
                if( (tempPoint.numberOfReturns > 1 && tempPoint.returnNumber == 1) || (tempPoint.numberOfReturns > 1 && tempPoint.returnNumber == tempPoint.numberOfReturns) ) {

                    if(first_and_last.size() == 0){
                        first_and_last.add(new double[]{previousPoint.x, previousPoint.y, previousPoint.z, previousPoint.scanAngleRank, previousPoint.gpsTime});
                    }
                    first_and_last.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, tempPoint.scanAngleRank, tempPoint.gpsTime});

                }
                //System.out.println("HERE");

            }else{

               if(first_and_last.size() == 2){
                   n_first_and_last++;
                   //System.out.println(first_and_last.size());
                   //System.out.println(tempPoint.gpsTime);
                   //System.out.println("---");
                   ArrayList<double[]> klooni = (ArrayList<double[]>)first_and_last.clone();

                   firsts_and_lasts.put(counter++, klooni);

               }
                first_and_last.clear();

            }

            try {
                previousPoint = (LasPoint)tempPoint.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }

            previous_gps_time = current_gps_time;
            previous_scan_angle = current_scan_angle;

        }

        // Using an iterator to go through the TreeMap two entries at a time
        Iterator<Map.Entry<Integer, ArrayList<double[]>>> iterator = firsts_and_lasts.entrySet().iterator();

        File outputFile = new File("/home/koomikko/Documents/processing_directory/las2traj/output.txt");

        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));

        while (iterator.hasNext()) {
            // Get the first entry
            Map.Entry<Integer, ArrayList<double[]>> entry1 = iterator.next();
            //System.out.println("Key: " + entry1.getKey() + ", Value: " + entry1.getValue());
            double scanAngle1 = entry1.getValue().get(0)[3];
            double time1 = entry1.getValue().get(0)[4];
            boolean breakki = false;
            // Check if there is a second entry
            while(!breakki)
            if (iterator.hasNext()) {
                Map.Entry<Integer, ArrayList<double[]>> entry2 = iterator.next();
                //System.out.println("Key: " + entry2.getKey() + ", Value: " + entry2.getValue());

                double absDifferenceInScanAngle = Math.abs(scanAngle1 - entry2.getValue().get(0)[3]);

                if(absDifferenceInScanAngle > 10) {
                    double[] point = findClosestPointBetweenLines(entry1.getValue(), entry2.getValue());
                    System.out.println(time1 + "\t" + entry2.getValue().get(0)[4]);
                    System.out.println(scanAngle1 + "\t" + entry2.getValue().get(0)[3]);
                    bw.write(point[0] + "\t" + point[1] + "\t" + point[2] + "\n");
                    System.out.println(Arrays.toString(point));
                    breakki = true;
                }


            }
        }

        bw.close();

        System.out.println(n_first_and_last);
        System.out.println(firsts_and_lasts.size());

    }

    public static double pointToLineDistance(double[] point, double[] lineStart, double[] lineEnd) {
        double[] lineVector = new double[] {lineEnd[0] - lineStart[0], lineEnd[1] - lineStart[1], lineEnd[2] - lineStart[2]};
        double[] pointVector = new double[] {point[0] - lineStart[0], point[1] - lineStart[1], point[2] - lineStart[2]};
        double lineVectorMagnitude = Math.sqrt(Math.pow(lineVector[0], 2) + Math.pow(lineVector[1], 2) + Math.pow(lineVector[2], 2));
        double pointVectorMagnitude = Math.sqrt(Math.pow(pointVector[0], 2) + Math.pow(pointVector[1], 2) + Math.pow(pointVector[2], 2));
        double dotProduct = (pointVector[0] * lineVector[0]) + (pointVector[1] * lineVector[1]) + (pointVector[2] * lineVector[2]);
        double angle = Math.acos(dotProduct / (lineVectorMagnitude * pointVectorMagnitude));
        return angle * pointVectorMagnitude;
    }

    // Method to compute the best-fit line for a set of points (returns centroid and direction vector)
// Method to compute the best-fit line for a set of points (returns centroid and direction vector)
    public static double[][] findBestFitLine(ArrayList<double[]> points) {
        int n = points.size();

        // Calculate the centroid of the points
        double[] centroid = new double[3];
        for (double[] point : points) {
            centroid[0] += point[0];
            centroid[1] += point[1];
            centroid[2] += point[2];
        }
        centroid[0] /= n;
        centroid[1] /= n;
        centroid[2] /= n;

        // Create a matrix where each row is (point - centroid)
        SimpleMatrix matrix = new SimpleMatrix(n, 3);
        for (int i = 0; i < n; i++) {
            double[] point = points.get(i);
            matrix.set(i, 0, point[0] - centroid[0]);
            matrix.set(i, 1, point[1] - centroid[1]);
            matrix.set(i, 2, point[2] - centroid[2]);
        }

        // Perform Singular Value Decomposition (SVD) to find the principal component
        SimpleMatrix covariance = matrix.transpose().mult(matrix);
        SimpleMatrix eigenVectors = covariance.svd().getV();

        // The first column of V is the direction vector (principal component)
        double[] direction = new double[]{
                eigenVectors.get(0, 0),
                eigenVectors.get(1, 0),
                eigenVectors.get(2, 0)
        };

        return new double[][]{centroid, direction};
    }

    // Helper method to compute the dot product of two vectors
    public static double dotProduct(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    // Main method to find the closest point of intersection between two lines
    public static double[] findClosestPointBetweenLines(ArrayList<double[]> pointsLine1, ArrayList<double[]> pointsLine2) {
        // Find best fit lines for both sets of points
        double[][] line1 = findBestFitLine(pointsLine1);
        double[][] line2 = findBestFitLine(pointsLine2);

        double[] p1 = line1[0]; // Centroid of line 1
        double[] d1 = line1[1]; // Direction vector of line 1
        double[] p2 = line2[0]; // Centroid of line 2
        double[] d2 = line2[1]; // Direction vector of line 2

        // Compute the closest point between the two lines
        double[] p1p2 = {p2[0] - p1[0], p2[1] - p1[1], p2[2] - p1[2]};
        double d1d1 = dotProduct(d1, d1);
        double d2d2 = dotProduct(d2, d2);
        double d1d2 = dotProduct(d1, d2);
        double d1p1p2 = dotProduct(d1, p1p2);
        double d2p1p2 = dotProduct(d2, p1p2);
        double denom = d1d1 * d2d2 - d1d2 * d1d2;

        if (Math.abs(denom) < 1e-6) {
            throw new IllegalArgumentException("Lines are parallel or nearly parallel.");
        }

        double t1 = (d1p1p2 * d2d2 - d2p1p2 * d1d2) / denom;
        double t2 = (d1p1p2 + t1 * d1d2) / d2d2;

        double[] closestPoint1 = {p1[0] + t1 * d1[0], p1[1] + t1 * d1[1], p1[2] + t1 * d1[2]};
        double[] closestPoint2 = {p2[0] + t2 * d2[0], p2[1] + t2 * d2[1], p2[2] + t2 * d2[2]};

        System.out.println("distance: " + euclideanDistance3d(closestPoint1, closestPoint2));

        // Return the midpoint of the closest points on the two lines
        return new double[]{
                (closestPoint1[0] + closestPoint2[0]) / 2.0,
                (closestPoint1[1] + closestPoint2[1]) / 2.0,
                (closestPoint1[2] + closestPoint2[2]) / 2.0
        };
    }

    public static double euclideanDistance3d(double[] point1, double[] point2) {
        return Math.sqrt(Math.pow(point1[0] - point2[0], 2) + Math.pow(point1[1] - point2[1], 2) + Math.pow(point1[2] - point2[2], 2));
    }
    
}



class pointWithTime implements Comparable<pointWithTime> {
    private double[] values;
    private double time;

    public pointWithTime(double time, double[] values) {
        this.values = values;
        this.time = time;
    }

    // getters
    public double[] getValues() {
        return values;
    }

    public double getTime() {
        return time;
    }
    @Override
    public int compareTo(pointWithTime other) {
        return this.getTime() > other.getTime() ? 1 : 0;
    }
}
