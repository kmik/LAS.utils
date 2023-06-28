package utils;

import LASio.LasBlock;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.ejml.data.DMatrixRMaj;
import org.jblas.DoubleMatrix;


import java.util.*;

public class ResidFunctionGroundMatch implements LevenbergMarquardt.ResidualFunction {

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

    public ResidFunctionGroundMatch(double deltaT){
        this.deltaT = deltaT;
    }

    public double getSomething(){

        return 1.0;

    }

    public void compute(DMatrixRMaj param , DMatrixRMaj residual){

            double yaw = param.get(0,0);
            double pitch = param.get(1,0);
            double roll = param.get(2,0);
            double x = param.get(3,0);
            double y = param.get(4,0);
            double z = param.get(5,0);
            double orig_x = param.get(6,0);
            double orig_y = param.get(7,0);
            double orig_z = param.get(8,0);

            double[] point = new double[3];
            double[] rotated_point = new double[3];
/*
            for (org.tinfour.common.Vertex v : tin1.getVertices()) {

                pointMatrix.put(0, 0, v.x);
                pointMatrix.put(0, 1, v.y);
                pointMatrix.put(0, 2, v.getZ());

                point[0] = v.x;
                point[1] = v.y;
                point[2] = v.getZ();

                rotatePoint3_cp(point, rotated_point, yaw, pitch, roll, x, y, z, orig_x, orig_y, orig_z);

                if (tin2.isPointInsideTin(rotated_point[0], rotated_point[1])) {

                    double distiSigned = (rotated_point[2] - polator2.interpolate(rotated_point[0], rotated_point[1], valuator));

                    normal = polator2.getSurfaceNormal();

                    distiSigned = (normal[0] * rotated_point[0] + normal[1] * rotated_point[1] + normal[2] * rotated_point[2] -
                            (normal[0] * rotated_point[0] + normal[1] * rotated_point[1] + normal[2] * (rotated_point[2] - distiSigned))) /
                            Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);

                    //distiSigned = distiSigned * (1.0 + (1.0 - polator2.getSurfaceNormal()[2]));

                    double disti = Math.abs(distiSigned);

                    if(!Double.isNaN(disti)) {
                        count++;
                        distanceSum += (disti);

                        average += (distiSigned - average) / count;
                        pwrSumAvg += (distiSigned * distiSigned - pwrSumAvg) / count;
                        stdDev = Math.sqrt((pwrSumAvg * count - count * average * average) / (count - 1));

                    }
                }
            }

*/
        //System.gc();
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

/*
    public void rotatePoint3_cp(double[] point,
                                double[] rotatedValue,
                                double yaw,
                                double pitch,
                                double roll,
                                double x,
                                double y,
                                double z,
                                double orig_x,
                                double orig_y,
                                double orig_z){

        double[] tempTrans = new double[3];

        double yaw = Math.toRadians(po_z_rot_sparseArray[id].value(time));
        double pitch = Math.toRadians(po_y_rot_sparseArray[id].value(time));
        double roll = Math.toRadians(po_x_rot_sparseArray[id].value(time));
        tempTrans[0] = po_x_sparseArray[id].value(time) ;
        tempTrans[1] = po_y_sparseArray[id].value(time) ;
        tempTrans[2] = po_z_sparseArray[id].value(time) ;

        //if(true)
        //return;

        if(false)
            if( point.prev_yaw == yaw  &&
                    point.prev_pitch == pitch &&
                    point.prev_roll == roll &&
                    point.prev_x == tempTrans[0] &&
                    point.prev_y == tempTrans[1] &&
                    point.prev_z == tempTrans[2] ){

                return;

            }

        point.prev_yaw = yaw;
        point.prev_pitch = pitch;
        point.prev_roll = roll;
        point.prev_x = tempTrans[0];
        point.prev_y = tempTrans[1];
        point.prev_z = tempTrans[2];

        double[] locationOfAircraft = new double[3];
        Map.Entry<Double, double[]> hehe;

        if(trajectory.size() > 0) {
            hehe = trajectory.ceilingEntry(time);

            locationOfAircraft = hehe.getValue();
        }

        //locationOfAircraft = hehe.getValue();

        double new_x = locationOfAircraft[0] - point.x;
        double new_y = locationOfAircraft[1] - point.y;
        double new_z = locationOfAircraft[2] - point.z;

        double temp_x = new_x;
        double temp_y = new_y;
        double temp_z = new_z;

        double cosa = Math.cos(yaw);
        double sina = Math.sin(yaw);

        double cosb = Math.cos(pitch);
        double sinb = Math.sin(pitch);

        double cosc = Math.cos(roll);
        double sinc = Math.sin(roll);

        double Axx = cosa*cosb;
        double Axy = cosa*sinb*sinc - sina*cosc;
        double Axz = cosa*sinb*cosc + sina*sinc;

        double Ayx = sina*cosb;
        double Ayy = sina*sinb*sinc + cosa*cosc;
        double Ayz = sina*sinb*cosc - cosa*sinc;

        double Azx = -sinb;
        double Azy = cosb*sinc;
        double Azz = cosb*cosc;

        double px = temp_x;
        double py = temp_y;
        double pz = temp_z;

        temp_x = Axx*px + Axy*py + Axz*pz;
        temp_y = Ayx*px + Ayy*py + Ayz*pz;
        temp_z = Azx*px + Azy*py + Azz*pz;

        point.x_rot = locationOfAircraft[0] - temp_x;
        point.y_rot = locationOfAircraft[1] - temp_y;
        point.z_rot = locationOfAircraft[2] - temp_z;

        rotatedValue[0] = locationOfAircraft[0] - temp_x + tempTrans[0];
        rotatedValue[1] = locationOfAircraft[1] - temp_y + tempTrans[1];
        rotatedValue[2] = locationOfAircraft[2] - temp_z + tempTrans[2];

        point.x_rot += tempTrans[0];
        point.y_rot += tempTrans[1];
        point.z_rot += tempTrans[2];

    }

    public void translatePoint(DoubleMatrix point, double[] translation){

        new_x = point.get(0,0) + translation[0];
        new_y = point.get(0,1) + translation[1];
        new_z = point.get(0,2) + translation[2];

        point.put(0,0, new_x);
        point.put(0,1, new_y);
        point.put(0,2, new_z);
    }

 */
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
