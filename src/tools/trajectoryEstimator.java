package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import utils.argumentReader;
import utils.pointWriterMultiThread;

import java.io.File;
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
    HashMap<Double, ArrayList<double[]>> firsts_and_lasts = new HashMap<>();

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

        int current_scan_angle = 0;
        int previous_scan_angle = 1;

        double current_gps_time = 0;
        double previous_gps_time = 0;

        int n_first_and_last = 0;

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

            if(current_scan_angle == previous_scan_angle && current_gps_time == previous_gps_time){
                // Searching for pairs for this one

                if( (tempPoint.numberOfReturns > 1 && tempPoint.returnNumber == 1) || (tempPoint.numberOfReturns > 1 && tempPoint.returnNumber == tempPoint.numberOfReturns) ) {

                    first_and_last.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z});

                }
                //System.out.println("HERE");

            }else{

               if(first_and_last.size() > 1){
                   n_first_and_last++;
                   System.out.println(tempPoint.gpsTime);
                   System.out.println("---");
               }

               first_and_last.clear();

            }

            previous_gps_time = current_gps_time;
            previous_scan_angle = current_scan_angle;

        }

        System.out.println(n_first_and_last);

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
