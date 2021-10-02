package utils;

import LASio.LasBlock;
import LASio.LASReader;
import LASio.LasPoint;
import org.apache.commons.math3.geometry.Vector;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.ejml.data.DMatrixRMaj;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;
import org.opencv.core.Mat;
import org.tinfour.common.Vertex;
import org.tinfour.standard.IncrementalTin;

import java.io.IOException;
import java.util.*;
import org.apache.commons.math3.*;

import static org.opencv.core.CvType.CV_64F;

public class ResidFunctionCylinderFit implements LevenbergMarquardt.ResidualFunction  {


    int numFunctions = 1;

    ArrayList<double[]> points = new ArrayList<>();
    double[] line = new double[8];

    double[] cylinderZ = new double[2];

    double[] cylinderPerim = new double[4];

    int counter = 0;

    double costValue = 0.0;
    double diameter = 0.0;


    public ResidFunctionCylinderFit(){

        //System.loadLibrary("opencv_java320");

    }

    public void setNumFunctions(int in){

        this.numFunctions = in;

    }

    public double getSomething(){

        return diameter;

    }

    public void compute(DMatrixRMaj param, DMatrixRMaj residual){
/*
        line[0] = param.get(0, 0);
        line[1] = param.get(1, 0);
        line[2] = param.get(2, 0);
        line[3] = param.get(3, 0);
        line[4] = param.get(4, 0);
        line[5] = param.get(5, 0);
        line[6] = param.get(6, 0);

 */
        line[0] = param.data[0];
        line[1] = param.data[1];
        line[2] = param.data[2]-1.0;
        line[3] = param.data[3];
        line[4] = param.data[4];
        line[5] = param.data[5]+1.0;
        line[6] = param.data[6];
        //line[7] = param.get(7, 0);

        double distanceSum = 0.0;
        double distance = 0.0;
        double distance2 = 0.0;

        double[] point = new double[3];
        double[] lineStart = new double[3];
        double[] lineEnd = new double[3];

        lineStart[0] = line[0];
        lineStart[1] = line[1];
        lineStart[2] = line[2];

        lineEnd[0] = line[3];
        lineEnd[1] = line[4];
        lineEnd[2] = line[5];

        double count = 0.0;

        RealVector pointMat = new ArrayRealVector(3);
        RealVector startMat = new ArrayRealVector(3);
        RealVector endMat = new ArrayRealVector(3);

        startMat.setEntry(0,line[0]);
        startMat.setEntry(1,line[1]);
        startMat.setEntry(2,line[2]);

        endMat.setEntry(0,line[3]);
        endMat.setEntry(1,line[4]);
        endMat.setEntry(2,line[5]);

        //boolean outside = line[0] < cylinderPerim[0] || line[1] > cylinderPerim[1] ||
          //      line[2] < cylinderPerim[2] || line[3] > cylinderPerim[3];

        //double dx = distance(line[0], line[1], line[2], line[3], line[4], line[5]);
        //double dy = line[7] - line[6];
        //double slope = dy / dx;

        //double slopeValue = 0.0;

        for(int i = 1; i < this.points.size(); i++){

            point[0] = this.points.get(i)[0];
            point[1] = this.points.get(i)[1];
            point[2] = this.points.get(i)[2];

            pointMat.setEntry(0, this.points.get(i)[0]);
            pointMat.setEntry(1, this.points.get(i)[1]);
            pointMat.setEntry(2, this.points.get(i)[2]);

            //distance = betweenPointAndLine(point, lineStart, lineEnd);

            //distance = distance( pointMat.getEntry(0), pointMat.getEntry(1), pointMat.getEntry(2),
              //      startMat.getEntry(0), startMat.getEntry(1), startMat.getEntry(2),
                //    endMat.getEntry(0), endMat.getEntry(1), endMat.getEntry(2));


            distance = shortDistance(startMat, endMat, pointMat);

            //System.out.println(distance + " ?==? " + distance2);

            //slopeValue = line[6] + (line[5] - point[2]) * slope;
            //distance = Math.abs(slopeValue - distance);
            //System.out.println("Distance: " + distance);
            //distance = distance * distance;

            distance = (distance - line[6]);

            if(distance < 0) {
                distance *= -4.0;
            }

           // System.out.println("distance: " + distance);

            distanceSum += distance;

            count++;

            //residual.data[i] = distance;
        }
        this.diameter = line[6];
        this.costValue = distanceSum / count;
        residual.data[0] = distanceSum;
        //residual.set(0, 0, );

        //System.out.println("Mean distance: " + (distanceSum / count));

        //System.out.println((distanceSum / count) + " " + line[4]);
        //this.numFunctions = residual.numRows;

        counter++;

    }

    double distance(double x1, double y1, double z1, double x2, double y2, double z2){

        return Math.sqrt( (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2) );

    }

    RealVector crossProduct(RealVector a, RealVector b){

        double x1, y1, z1;

        x1 = a.getEntry(1) * b.getEntry(2) - a.getEntry(2) * b.getEntry(1);
        y1 = a.getEntry(2) * b.getEntry(0) - a.getEntry(0) * b.getEntry(2);
        z1 = a.getEntry(0) * b.getEntry(1) - a.getEntry(1) * b.getEntry(0);

        return new ArrayRealVector(new double[]{x1, y1, z1});

    }

    // calculate shortest dist. from point to line
    double shortDistance(RealVector line_point1, RealVector line_point2,
                        RealVector point) {

        RealVector AB = line_point1.subtract(line_point2);
        RealVector AC = point.subtract(line_point2);


        //double area = magnitude(AB.ebeMultiply(AC));

        double area = magnitude(crossProduct(AB, AC));

        double CD = area / magnitude(AB);

        return CD;
    }

    public double magnitude(RealVector v){

        return Math.sqrt( v.getEntry(0) * v.getEntry(0) + v.getEntry(1) * v.getEntry(1) +
                v.getEntry(2) * v.getEntry(2));

    }

    public static double distance(DoubleMatrix point, DoubleMatrix A, DoubleMatrix B){

        return  (point.sub(A).mul(point.sub(B))).div(B.sub(A)).get(0,0);

    }

    public static double distance(double x1, double y1, double z1,
                                  double x2, double y2, double z2,
                                  double x3, double y3, double z3) {
        double b = Math.sqrt(Math.pow((x2 - x3), 2)
                + Math.pow((y2 - y3), 2)
                + Math.pow((z2 - z3), 2));

        double S = Math.sqrt(Math.pow((y2 - y1) * (z3 - z1) - (z2 - z1) * (y3 - y1), 2) +
                Math.pow((z2 - z1) * (x3 - x1) - (x2 - x1) * (z3 - z1), 2) +
                Math.pow((x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1), 2)) / 2;

        return  2*S / b;
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

        double distance = (float) (Math.sqrt(TotalThing[0]*TotalThing[0] + TotalThing[1]*TotalThing[1] + TotalThing[2]*TotalThing[2]) /
                Math.sqrt(lineEnd[0] * lineEnd[0] + lineEnd[1] * lineEnd[1] + lineEnd[2] * lineEnd[2] ));


        return distance;
    }

    public void setup() throws IOException{



    }

    public void setPoints(ArrayList<double[]> points){

        this.points = points;

    }

    public void setCylinder(double minX, double maxX, double minY, double maxY, double minZ, double maxZ){

        this.cylinderZ[0] = minZ;
        this.cylinderZ[1] = maxZ;

        this.cylinderPerim[0] = minX;
        this.cylinderPerim[1] = maxX;
        this.cylinderPerim[2] = minY;
        this.cylinderPerim[3] = maxY;

    }


    /**
     * Number of functions in output
     * @return function count
     */
    public int numFunctions(){

        return this.numFunctions;
    }

    public double pointToLineDistance(double x1, double y1, double x2, double y2, double x_p, double y_p){

        double normalLength = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));

        return Math.abs((x_p-x1)*(y2-y1)-(y_p-y1)*(x2-x1))/normalLength;
    }

    public double pointToLineDistance(org.tinfour.common.Vertex A, org.tinfour.common.Vertex B, org.tinfour.common.Vertex P) {

        double normalLength = Math.sqrt((B.x-A.x)*(B.x-A.x)+(B.y-A.y)*(B.y-A.y));
        return Math.abs((P.x-A.x)*(B.y-A.y)-(P.y-A.y)*(B.x-A.x))/normalLength;
    }
/*
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
*/
}
