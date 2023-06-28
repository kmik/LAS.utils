package utils;

import LASio.LasBlock;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.ejml.data.DMatrixRMaj;
import org.jblas.DoubleMatrix;
import org.tinfour.common.IQuadEdge;
import org.tinfour.utils.Polyside;

import java.util.*;

import static org.tinfour.utils.Polyside.isPointInPolygon;


public class ResidFunction implements LevenbergMarquardt.ResidualFunction {

    int numFunctions = 1;

    boolean[] optimize = new boolean[]{false, false};

    org.tinfour.standard.IncrementalTin tin1;
    org.tinfour.standard.IncrementalTin tin2;

    org.tinfour.standard.IncrementalTin tin1_2 = new org.tinfour.standard.IncrementalTin();
    org.tinfour.standard.IncrementalTin tin2_2 = new org.tinfour.standard.IncrementalTin();

    org.tinfour.interpolation.TriangularFacetInterpolator polator1;
    org.tinfour.interpolation.TriangularFacetInterpolator polator2;

    org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();

    KdTree tree1 = new KdTree();
    KdTree tree2 = new KdTree();

    TreeMap<Integer,double[]> trajectory = new TreeMap<>();

    //Mat rotationMatrix1 = new Mat(3, 3, CV_64F);
    DoubleMatrix rotationMatrix1_2 = new DoubleMatrix(3,3);
    //Mat rotationMatrix2 = new Mat(3, 3, CV_64F);
    DoubleMatrix rotationMatrix2_2 = new DoubleMatrix(3,3);

    float[] translation_1 = new float[]{0f,0f,0f};
    float[] translation_2 = new float[]{0f,0f,0f};

    //Mat point = new Mat(1,3, CV_64F);
    DoubleMatrix pointMatrix = new DoubleMatrix(1,3);
    //Mat rotatedpoint = new Mat(1,3, CV_64F);
    DoubleMatrix rotatedPointMatrix = new DoubleMatrix(1,3);

    KdTree.XYZPoint p = new KdTree.XYZPoint(0,0,0);
    List<KdTree.XYZPoint> closestPoint = null;

    org.tinfour.common.Vertex v_temp;

    //Mat dummyMat = new Mat();

    double[] locationOfAircraft;

    double new_x;
    double new_y;
    double new_z;

    double[] file1Pivot;
    double[] file2Pivot;

    Map.Entry<Integer, double[]> hehe;

    ArrayList<DoubleMatrix> matrices_rot = new ArrayList<>();
    ArrayList<double[]> matrices_trans = new ArrayList<>();

    double[] minMax2;
    double[] minMax1;

    double deltaT = 5.0;

    SplineInterpolator interpolatori = new SplineInterpolator();
    PolynomialSplineFunction po_x_rot;
    PolynomialSplineFunction po_y_rot;
    PolynomialSplineFunction po_z_rot;
    PolynomialSplineFunction po_x;
    PolynomialSplineFunction po_y;
    PolynomialSplineFunction po_z;

    double[] x_segments;
    double[] y_x;
    double[] y_y;
    double[] y_z;

    double[] y_x_rot;
    double[] y_y_rot;
    double[] y_z_rot;

    DoubleMatrix tempMatrix = new DoubleMatrix(3,3);
    double[] tempTrans = new double[3];
    int numberOfSegments = 0;

    LasBlock blokki;

    int nComputes = 0;

    double[] normal = null;

    public ResidFunction(double deltaT){
        this.deltaT = deltaT;
    }

    public double getSomething(){

        return 1.0;

    }
    public void createPolynomialSplines(DMatrixRMaj param){

        x_segments = new double[numberOfSegments + 3];

        y_x = new double[numberOfSegments + 3];
        y_y = new double[numberOfSegments + 3];
        y_z = new double[numberOfSegments + 3];

        y_x_rot = new double[numberOfSegments + 3];
        y_y_rot = new double[numberOfSegments + 3];
        y_z_rot = new double[numberOfSegments + 3];


        int counti = 1;
        if(!blokki.aligned[blokki.currentFiles[0]]) {
            for (int e = 0; e <= numberOfSegments; e++) {
                x_segments[counti] = blokki.pointCloudTimeExtent.get(blokki.currentFiles[0])[0] + e * deltaT;
                counti++;
            }
            x_segments[0] = x_segments[1]-2;
            x_segments[x_segments.length-2] = blokki.pointCloudTimeExtent.get(blokki.currentFiles[0])[1];
            x_segments[x_segments.length-1] = x_segments[x_segments.length-2]+2;
        }
        else if(!blokki.aligned[blokki.currentFiles[1]]) {
            //System.out.println("HERE!");
            for (int e = 0; e <= numberOfSegments; e++) {
                x_segments[counti] = blokki.pointCloudTimeExtent.get(blokki.currentFiles[1])[0] + e * deltaT;
                counti++;
            }
            x_segments[0] = x_segments[1]-2;
            x_segments[x_segments.length-2] = blokki.pointCloudTimeExtent.get(blokki.currentFiles[1])[1];
            x_segments[x_segments.length-1] = x_segments[x_segments.length-2]+2;
        }
        counti = 1;

        y_x_rot[0] = param.get(0 + 0,0);
        y_y_rot[0] = param.get(0 + 1,0);
        y_z_rot[0] = param.get(0 + 2,0);

        y_x[0] = param.get(0 + 3,0);
        y_y[0] = param.get(0 + 4,0);
        y_z[0] = param.get(0 + 5,0);

        y_x_rot[y_x.length-2] = param.get(param.numRows - 6,0);
        y_y_rot[y_x.length-2] = param.get(param.numRows - 5,0);
        y_z_rot[y_x.length-2] = param.get(param.numRows - 4,0);

        y_x[y_x.length-2] = param.get(param.numRows - 3,0);
        y_y[y_x.length-2] = param.get(param.numRows - 2,0);
        y_z[y_x.length-2] = param.get(param.numRows - 1,0);

        y_x_rot[y_x.length-1] = param.get(param.numRows - 6,0);
        y_y_rot[y_x.length-1] = param.get(param.numRows - 5,0);
        y_z_rot[y_x.length-1] = param.get(param.numRows - 4,0);

        y_x[y_x.length-1] = param.get(param.numRows - 3,0);
        y_y[y_x.length-1] = param.get(param.numRows - 2,0);
        y_z[y_x.length-1] = param.get(param.numRows - 1,0);

        for(int h = 0; h < param.numRows; h += 6){

            y_x_rot[counti] = param.get(h + 0,0);
            y_y_rot[counti] = param.get(h + 1,0);
            y_z_rot[counti] = param.get(h + 2,0);

            y_x[counti] = param.get(h + 3,0);
            y_y[counti] = param.get(h + 4,0);
            y_z[counti] = param.get(h + 5,0);

            counti++;
        }

        po_x_rot = interpolatori.interpolate(x_segments, y_x_rot);
        po_y_rot = interpolatori.interpolate(x_segments, y_z_rot);
        po_z_rot = interpolatori.interpolate(x_segments, y_y_rot);
        po_x = interpolatori.interpolate(x_segments, y_x);
        po_y = interpolatori.interpolate(x_segments, y_y);
        po_z = interpolatori.interpolate(x_segments, y_z);


    }

    public void setBlock(LasBlock blokki){
        this.blokki = blokki;
    }

    public void compute(DMatrixRMaj param , DMatrixRMaj residual){

        this.numberOfSegments = param.numRows / 6;

        double disti = 0.0;

        double distiSigned = 0.0;

        double stdDev = 0.0;
        double average = 0.0;
        double pwrSumAvg = 0.0;

        createPolynomialSplines(param);

        int index;
        double interpolatedvalue = 0;

        int vertexCount = 0;

        if(optimize[0] && !optimize[1]){

            List<IQuadEdge> tin2_perimeter = tin2.getPerimeter();
            //makeRotationMatrix(rotationMatrix1_2, param.get(0, 0), param.get(1, 0), param.get(2, 0));

            //translation_1[0] = (float) param.get(3, 0);
            //translation_1[1] = (float) param.get(4, 0);
            //translation_1[2] = (float) param.get(5, 0);

            double distanceSum = 0.0;
            int count = 0;

            for (org.tinfour.common.Vertex v : tin1.getVertices()) {

                index = (int)(((double)v.getIndex()/1000.0d - minMax1[0]) / deltaT);

                pointMatrix.put(0, 0, v.x);
                pointMatrix.put(0, 1, v.y);
                pointMatrix.put(0, 2, v.getZ());

                //rotatePoint(pointMatrix, matrices_rot.get(index), v.getIndex(), 1);
                rotatePoint2(pointMatrix, (double)v.getIndex()/1000.0d);
                //translatePoint(pointMatrix, matrices_trans.get(index));

                new_x = pointMatrix.get(0, 0) + tempTrans[0];
                new_y = pointMatrix.get(0, 1) + tempTrans[1];
                new_z = pointMatrix.get(0, 2) + tempTrans[2];

                pointMatrix.put(0,0, new_x);
                pointMatrix.put(0,1, new_y);
                pointMatrix.put(0,2, new_z);


                //if (tin2.isPointInsideTin(pointMatrix.get(0, 0), pointMatrix.get(0, 1))) {
                if (isPointInPolygon(tin2_perimeter, pointMatrix.get(0,0), pointMatrix.get(0,1)) == Polyside.Result.Inside) {
                //if (!Double.isNaN(interpolatedvalue)) {
                    interpolatedvalue = polator2.interpolate(pointMatrix.get(0, 0), pointMatrix.get(0, 1), valuator);
                    distiSigned = (pointMatrix.get(0, 2) - interpolatedvalue);

                    normal = polator2.getSurfaceNormal();

                    distiSigned = (normal[0] * pointMatrix.get(0, 0) + normal[1] * pointMatrix.get(0, 1) + normal[2] * pointMatrix.get(0, 2) -
                            (normal[0] * pointMatrix.get(0, 0) + normal[1] * pointMatrix.get(0, 1) + normal[2] * (pointMatrix.get(0, 2) - distiSigned))) /
                            Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);

                    //distiSigned = distiSigned * (1.0 + (1.0 - polator2.getSurfaceNormal()[2]));

                    disti = Math.abs(distiSigned);

                    if(!Double.isNaN(disti)) {
                        count++;
                        distanceSum += (disti);

                        average += (distiSigned - average) / count;
                        pwrSumAvg += (distiSigned * distiSigned - pwrSumAvg) / count;
                        stdDev = Math.sqrt((pwrSumAvg * count - count * average * average) / (count - 1));

                        residual.set(vertexCount++, 0, disti);
                    }
                }else{
                    System.out.println("POINT OUTSIDE??? SHOULD NOT HAPPEN, EVER!!");
                    residual.set(vertexCount++, 0, 20.0);

                }

            }

           // residual.set(0, 0, distanceSum / (double) count);
            //this.numFunctions = residual.numRows;

        }
        else if(!optimize[0] && optimize[1]){

            List<IQuadEdge> tin1_perimeter = tin1.getPerimeter();

            double distanceSum = 0.0;
            int count = 0;

            for (org.tinfour.common.Vertex v : tin2.getVertices()) {

                index = (int)(((double)v.getIndex()/1000.0d - minMax2[0]) / deltaT);

                pointMatrix.put(0, 0, v.x);
                pointMatrix.put(0, 1, v.y);
                pointMatrix.put(0, 2, v.getZ());

                //rotatePoint(pointMatrix, matrices_rot.get(index), v.getIndex(), 2);
                rotatePoint2(pointMatrix, (double)v.getIndex()/1000.0d);
                //translatePoint(pointMatrix, matrices_trans.get(index));

                new_x = pointMatrix.get(0, 0) + tempTrans[0];
                new_y = pointMatrix.get(0, 1) + tempTrans[1];
                new_z = pointMatrix.get(0, 2) + tempTrans[2];

                pointMatrix.put(0,0, new_x);
                pointMatrix.put(0,1, new_y);
                pointMatrix.put(0,2, new_z);


                //if (tin1.isPointInsideTin(pointMatrix.get(0, 0), pointMatrix.get(0, 1))) {
                if (isPointInPolygon(tin1_perimeter, pointMatrix.get(0,0), pointMatrix.get(0,1)) == Polyside.Result.Inside) {
                //if (!Double.isNaN(interpolatedvalue)) {
                    interpolatedvalue = polator1.interpolate(pointMatrix.get(0, 0), pointMatrix.get(0, 1), valuator);

                    distiSigned = (pointMatrix.get(0, 2) - interpolatedvalue);

                    normal = polator1.getSurfaceNormal();

                    distiSigned = (normal[0] * pointMatrix.get(0, 0) + normal[1] * pointMatrix.get(0, 1) + normal[2] * pointMatrix.get(0, 2) -
                            (normal[0] * pointMatrix.get(0, 0) + normal[1] * pointMatrix.get(0, 1) + normal[2] * (pointMatrix.get(0, 2) - distiSigned))) /
                            Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);

                    disti = Math.abs(distiSigned);

                    if(!Double.isNaN(disti)) {
                        count++;
                        distanceSum += (disti);

                        average += (distiSigned - average) / count;
                        pwrSumAvg += (distiSigned * distiSigned - pwrSumAvg) / count;
                        stdDev = Math.sqrt((pwrSumAvg * count - count * average * average) / (count - 1));
                        residual.set(vertexCount++, 0, disti);
                    }
                }else{
                    residual.set(vertexCount++, 0, 20.0);
                }

            }

            //residual.set(0, 0, distanceSum / (double) count);

            //this.numFunctions = residual.numRows;

        }

        nComputes += 1;

        if(nComputes % 50 == 0) {
            System.gc();
            System.gc();
            System.gc();
        }


        //System.gc();
    }

    public static double betweenPointAndLine(double[] point, double[] lineStart, double[] lineEnd){
        double[] PointThing = new double[3];
        double[] TotalThing = new double[3];
        PointThing[0] = lineStart[0] - point[0];
        PointThing[1] = lineStart[1] - point[1];
        PointThing[2] = lineStart[2] - point[2];

        TotalThing[0] = (PointThing[1]*lineEnd[2] - PointThing[2]*lineEnd[1]);
        TotalThing[1] = -(PointThing[0]*lineEnd[2] - PointThing[2]*lineEnd[0]);
        TotalThing[2] = (PointThing[0]*lineEnd[1] - PointThing[1]*lineEnd[0]);

        double distance = (Math.sqrt(TotalThing[0]*TotalThing[0] + TotalThing[1]*TotalThing[1] + TotalThing[2]*TotalThing[2]) /
                Math.sqrt(lineEnd[0] * lineEnd[0] + lineEnd[1] * lineEnd[1] + lineEnd[2] * lineEnd[2] ));


        return distance;
    }

    public void release(){
        for(org.tinfour.common.Vertex v : tin1.getVertices())
            v = null;

        for(org.tinfour.common.Vertex v : tin2.getVertices())
            v = null;

        for(org.tinfour.common.IQuadEdge e : tin1.getEdges())
            e = null;

        for(org.tinfour.common.IQuadEdge e : tin2.getEdges())
            e = null;

        tin1.clear();
        tin2.clear();

        tin1.dispose();
        tin2.dispose();

        System.gc();
        System.gc();
        System.gc();

    }

    public double distance(double x1, double y1, double z1, double x2, double y2, double z2){

        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2));

    }

    public void setPivotPoints(double[] file1Pivot, double[] file2Pivot){

        this.file1Pivot = file1Pivot;
        this.file2Pivot = file2Pivot;
    }

    public void rotatePoint(DoubleMatrix point, DoubleMatrix rotationMatrix, int time, int flightLine){

        //Find the closest timestamp in the trajectory file
        if(trajectory.size() > 0) {
            hehe = trajectory.ceilingEntry(time);

            locationOfAircraft = hehe.getValue();
        }else if(flightLine == 1){
            locationOfAircraft = file1Pivot;
        }else if(flightLine == 2){
            locationOfAircraft = file2Pivot;
        }

        //locationOfAircraft = hehe.getValue();

        new_x = locationOfAircraft[0] - point.get(0, 0);
        new_y = locationOfAircraft[1] - point.get(0, 1);
        new_z = locationOfAircraft[2] - point.get(0, 2);

        point.put(0, 0, new_x);
        point.put(0, 1, new_y);
        point.put(0, 2, new_z);


        rotatedPointMatrix = point.mmul(rotationMatrix);

        //Core.gemm(point, rotationMatrix, 1, dummyMat, 0, rotatedpoint);

        point.put(0, 0, locationOfAircraft[0] - rotatedPointMatrix.get(0, 0));
        point.put(0, 1, locationOfAircraft[1] - rotatedPointMatrix.get(0, 1));
        point.put(0, 2, locationOfAircraft[2] - rotatedPointMatrix.get(0, 2));

    }


    public void rotatePoint2(DoubleMatrix point, double time){

        /*
        makeRotationMatrix(tempMatrix,
                interpolate(polynomial_pitch, time),
                interpolate(polynomial_roll, time),
                interpolate(polynomial_yaw, time));

        rotatePoint(point, tempMatrix, (int)(time*1000), 0);

        tempTrans[0] = interpolate(polynomial_x, time);
        tempTrans[1] = interpolate(polynomial_y, time);
        tempTrans[2] = interpolate(polynomial_z, time);

         */
        //System.out.println(time);
        makeRotationMatrix(tempMatrix,
                po_x_rot.value(time),
                po_y_rot.value(time),
                po_z_rot.value(time));

        rotatePoint(point, tempMatrix, (int)(time*1000), 0);

        tempTrans[0] = po_x.value(time);
        tempTrans[1] = po_y.value(time);
        tempTrans[2] = po_z.value(time);

    }

    public void translatePoint(DoubleMatrix point, double[] translation){

        new_x = point.get(0,0) + translation[0];
        new_y = point.get(0,1) + translation[1];
        new_z = point.get(0,2) + translation[2];

        point.put(0,0, new_x);
        point.put(0,1, new_y);
        point.put(0,2, new_z);
    }
    /**
     * Number of functions in output
     * @return function count
     */
    public int numFunctions(){

        return this.numFunctions;
    }

    public void setTins(org.tinfour.standard.IncrementalTin tin1, boolean optimize1, double[] minMax1,
                        org.tinfour.standard.IncrementalTin tin2, boolean optimize2, double[] minMax2){

        this.minMax1 = minMax1;
        this.minMax2 = minMax2;
        optimize[0] = optimize1;
        optimize[1] = optimize2;

        this.tin1 = tin1;
        this.tin2 = tin2;

        polator1 = new org.tinfour.interpolation.TriangularFacetInterpolator(tin1);
        polator2 = new org.tinfour.interpolation.TriangularFacetInterpolator(tin2);

        if(optimize1)
            this.numFunctions = tin1.getVertices().size();
        else
            this.numFunctions = tin2.getVertices().size();

    }

    public void setTrajectory(TreeMap<Integer,double[]> trajectory){

        this.trajectory = trajectory;
    }

    public void setTrees(KdTree tree1, KdTree tree2){

        this.tree1 = tree1;
        this.tree2 = tree2;

    }

    public void makeRotationMatrixes(DMatrixRMaj in){

        this.matrices_rot.clear();
        this.matrices_trans.clear();

        for(int i = 0; i < in.numRows; i += 6){

            DoubleMatrix temp = new DoubleMatrix(3,3);
            makeRotationMatrix(temp, in.get(i,0), in.get(i+1,0), in.get(i+2,0));
            matrices_rot.add(temp);
            matrices_trans.add(new double[]{in.get(i+3, 0), in.get(i+4,0), in.get(i+5,0)});

        }
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

    public boolean rejectSolution(){

        return false;
    }

}
