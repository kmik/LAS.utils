package utils;

import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;

public class ResidFunctionCircleFit_fixed_radius implements LevenbergMarquardt.ResidualFunction{

    int num_functions = 0;

    double radius = 0;
    ArrayList<double[]> points = new ArrayList<double[]>();

    public void compute(DMatrixRMaj param, DMatrixRMaj residual){

        for(int i = 0; i < points.size(); i++){

            double cost = Math.abs(euclideanDistance(points.get(i)[0], points.get(i)[1], param.get(0,0), param.get(1,0)) - radius);

            residual.set(i,0, cost);

        }
    }

    public double getSomething(){

        return 0.0;

    }

    public void setRadius(double radius){
        this.radius = radius;
    }

    public void setPoints(ArrayList<double[]> points){

        this.points = points;
        this.num_functions = points.size();

    }
    public double euclideanDistance(double x1, double y1, double x2, double y2){


        return Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

    }


    public int numFunctions(){
        return num_functions;
    }
}
