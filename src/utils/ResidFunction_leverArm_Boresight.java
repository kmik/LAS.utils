package utils;

import LASio.LasBlock;
import LASio.LASReader;
import LASio.LasPoint;
import org.ejml.data.DMatrixRMaj;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;
import org.tinfour.common.Vertex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class ResidFunction_leverArm_Boresight implements LevenbergMarquardt.ResidualFunction  {

    List<org.tinfour.standard.IncrementalTin> tins = new ArrayList<>();

    org.tinfour.standard.IncrementalTin tin1;
    org.tinfour.standard.IncrementalTin tin2;
    //org.tinfour.interpolation.TriangularFacetInterpolator polator1;
    org.tinfour.interpolation.TriangularFacetInterpolator polator2;
    org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();

    int numFunctions = 1;

    float[][] image1;
    float[] extent1;
    float[][] image2;
    float[] extent2;

    //Mat boreRotation = new Mat(3, 3, CV_64F);
    DoubleMatrix boreRotationMatrix = new DoubleMatrix(3,3);

    //Mat leverTranslation = new Mat(1, 3, CV_64F);
    DoubleMatrix leverTranslationMatrix = new DoubleMatrix(1,3);

    double[] locationOfAircraft;
    TreeMap<Integer,double[]> trajectory = new TreeMap<>();
    Map.Entry<Integer, double[]> hehe;

    DoubleMatrix tempRotation = new DoubleMatrix(3, 3);

    double new_x;
    double new_y;
    double new_z;

    //Mat dummyMat = new Mat();

    //Mat point = new Mat(1,3, CV_64F);
    DoubleMatrix pointMatrix = new DoubleMatrix(1,3);


    //Mat rotatedpoint = new Mat(1,3, CV_64F);
    DoubleMatrix rotatedPointMatrix = new DoubleMatrix(1,3);

    ArrayList<float[][]> images = new ArrayList<>();
    ArrayList<float[][]> imageTimeStamps = new ArrayList<>();

    LasBlock block;

    double resolution = 1.0;

    public ResidFunction_leverArm_Boresight(){

        //System.loadLibrary("opencv_java320");

    }

    public double getSomething(){

        return 1.0;

    }

    public void compute(DMatrixRMaj param , DMatrixRMaj residual){

        makeRotationMatrix(boreRotationMatrix, param.get(0, 0), param.get(1, 0), param.get(2, 0));
        leverTranslationMatrix.put(0,0, param.get(3,0));
        leverTranslationMatrix.put(0,1, param.get(4,0));
        leverTranslationMatrix.put(0,2, param.get(5,0));

        /*
        System.out.println(param.get(0,0));
        System.out.println(param.get(1,0));
        System.out.println(param.get(2,0));
        System.out.println(param.get(3,0));
        System.out.println(param.get(4,0));
        System.out.println(param.get(5,0));
        */
        double distanceSum = 0.0;
        double dist = 0.0;
        int count = 0;

        for(int i = 0; i < tins.size() - 1; i += 2){


            org.tinfour.standard.IncrementalTin tinTemp = new org.tinfour.standard.IncrementalTin();


            //tin1 = tins.get(i);
            //tin2 = tins.get(i+1);

            for(org.tinfour.common.Vertex v : tins.get(i).getVertices()){


                pointMatrix.put(0, 0, v.x);
                pointMatrix.put(0, 1, v.y);
                pointMatrix.put(0, 2, v.getZ());

                rotatePoint(pointMatrix, boreRotationMatrix, v.getIndex(), 1);

                tinTemp.add(new Vertex(pointMatrix.get(0,0), pointMatrix.get(0,1), pointMatrix.get(0,2)));

            }

            org.tinfour.interpolation.TriangularFacetInterpolator polator1 = new org.tinfour.interpolation.TriangularFacetInterpolator(tins.get(i));

            for(org.tinfour.common.Vertex v : tins.get(i+1).getVertices()){

                pointMatrix.put(0, 0, v.x);
                pointMatrix.put(0, 1, v.y);
                pointMatrix.put(0, 2, v.getZ());

                rotatePoint(pointMatrix, boreRotationMatrix, v.getIndex(), 1);

                dist = Math.abs(pointMatrix.get(0, 2) - polator1.interpolate(pointMatrix.get(0,0), pointMatrix.get(0,1), valuator));

                if(!Double.isNaN(dist)) {
                    count++;
                    distanceSum += dist;
                }

            }

            tinTemp.clear();
            tinTemp.dispose();
            polator1 = null;
            tinTemp = null;
        }

        System.gc();
        residual.set(0, 0, distanceSum / (double) count);
        //System.out.println("housda: " + (distanceSum / (double) count));

        this.numFunctions = residual.numRows;
    }


    public void setBlock(LasBlock block) throws IOException{
        this.block = block;
    }

    public void setTrajectory(TreeMap<Integer,double[]> trajectory){

        this.trajectory = trajectory;
    }

    public void setup() throws IOException{

        int[] currentFiles;
        int numberOfPairs = (int)Math.ceil(block.order.size() / 2.0);
        int count = 0;

        LASReader file;
        int n;

        LasPoint tempPoint = new LasPoint();

        for(int k = 0; k < block.order.size(); k++){

            currentFiles = block.order.get(k);
            file = block.pointClouds.get(currentFiles[0]);
            n = (int)file.getNumberOfPointRecords();

            org.tinfour.standard.IncrementalTin tempTin1 = new org.tinfour.standard.IncrementalTin();

            for(int i = 0; i < n; i++) {

                if (block.includedPointsFile1.get(k).contains(i)) {
                    file.readRecord(i, tempPoint);
                    org.tinfour.common.Vertex tempV = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z);
                    tempV.setIndex((int) (tempPoint.gpsTime * 1000));
                    tempTin1.add(tempV);
                }
            }

            org.tinfour.standard.IncrementalTin tempTin2 = new org.tinfour.standard.IncrementalTin();

            file = block.pointClouds.get(currentFiles[1]);
            n = (int)file.getNumberOfPointRecords();

            for(int i = 0; i < n; i++) {

                if (block.includedPointsFile2.get(k).contains(i)) {
                    file.readRecord(i, tempPoint);
                    org.tinfour.common.Vertex tempV = new org.tinfour.common.Vertex(tempPoint.x, tempPoint.y, tempPoint.z);
                    tempV.setIndex((int) (tempPoint.gpsTime * 1000));
                    tempTin2.add(tempV);
                }
            }

            tins.add(tempTin1);
            tins.add(tempTin2);

            if(++count >= numberOfPairs)
                break;
        }


        //System.out.println(tins.size());

    }

    public void makeRotationMatrix(DoubleMatrix output, double x, double y, double z){

        x = Math.toRadians(x);
        y = Math.toRadians(y);
        z = Math.toRadians(z);

        // FIRST ROW DONE!
        output.put(0,0, (Math.cos(y) * Math.cos(z)));
        output.put(0,1, (Math.cos(x) * Math.sin(z)) + Math.sin(x) * Math.sin(y) * Math.cos(z));
        output.put(0,2, (Math.sin(x) * Math.sin(z)) - Math.cos(x) * Math.sin(y) * Math.cos(z));

        // SECOND ROW DONE!
        output.put(1,0, (-Math.cos(y) * Math.sin(z)));
        output.put(1,1, (Math.cos(x) * Math.cos(z)) - Math.sin(x) * Math.sin(y) * Math.sin(z));
        output.put(1,2, (Math.sin(x) * Math.cos(z)) + Math.cos(x) * Math.sin(y) * Math.sin(z));

        // THIRD ROW
        output.put(2,0, Math.sin(y));
        output.put(2,1, -Math.sin(x) * Math.cos(y));
        output.put(2,2, Math.cos(x) * Math.cos(y));

    }

    public void rotatePoint(DoubleMatrix point, DoubleMatrix rotationMatrix, int time, int flightLine){

        hehe = trajectory.ceilingEntry(time);

        locationOfAircraft = hehe.getValue();

        makeRotationMatrix(tempRotation, locationOfAircraft[3], locationOfAircraft[4], locationOfAircraft[5]);

        new_x = locationOfAircraft[0] - point.get(0, 0);
        new_y = locationOfAircraft[1] - point.get(0, 1);
        new_z = locationOfAircraft[2] - point.get(0, 2);

        point.put(0, 0, new_x);
        point.put(0, 1, new_y);
        point.put(0, 2, new_z);


        rotatedPointMatrix = point.mmul(Solve.pinv(tempRotation));
        //Core.gemm(point, tempRotation.inv(), 1, dummyMat, 0, rotatedpoint);
        //System.out.println("HERE!");

        // Rotate the boresight
        rotatedPointMatrix = rotatedPointMatrix.mmul(rotationMatrix);
        //Core.gemm(rotatedpoint, rotationMatrix, 1, dummyMat, 0, rotatedpoint);

        new_x = rotatedPointMatrix.get(0, 0) + leverTranslationMatrix.get(0,0);
        new_y = rotatedPointMatrix.get(0, 1) + leverTranslationMatrix.get(0,1);
        new_z = rotatedPointMatrix.get(0, 2) + leverTranslationMatrix.get(0,2);

        rotatedPointMatrix.put(0,0, new_x);
        rotatedPointMatrix.put(0,1, new_y);
        rotatedPointMatrix.put(0,2, new_z);

        // Rotate the point back
        //makeRotationMatrix(tempRotation, locationOfAircraft[3], locationOfAircraft[4], locationOfAircraft[5]);
        rotatedPointMatrix = rotatedPointMatrix.mmul(tempRotation);
        //Core.gemm(rotatedpoint, tempRotation, 1, dummyMat, 0, rotatedpoint);

        point.put(0, 0, locationOfAircraft[0] - rotatedPointMatrix.get(0, 0));
        point.put(0, 1, locationOfAircraft[1] - rotatedPointMatrix.get(0, 1));
        point.put(0, 2, locationOfAircraft[2] - rotatedPointMatrix.get(0, 2));

    }

    /**
     * Number of functions in output
     * @return function count
     */
    public int numFunctions(){

        return this.numFunctions;
    }

    public void release(){

        for(int i = 0; i < tins.size(); i++){
            for(org.tinfour.common.Vertex v : tins.get(i).getVertices())
                v = null;
            for(org.tinfour.common.IQuadEdge e : tins.get(i).getEdges())
                e = null;
            tins.get(i).clear();
            tins.get(i).dispose();
        }

        System.gc();

    }

    public boolean rejectSolution(){

        return false;
    }


    public void onlyModifyThis(int i){

    }

}
