package utils;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.ejml.data.DMatrixRMaj;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Ransac {

    ArrayList<double[]> data;

    int n;
    int d;

    int maxIter = 100;
    double threshold;

    double bestFit = -1.0;
    double minError = Double.MAX_VALUE;
    double percentage;
    double percentage2;

    double[] bestParameters;

    ArrayList<Integer> indexes;

    RealVector pointMat = new ArrayRealVector(3);
    RealVector startMat = new ArrayRealVector(3);
    RealVector endMat = new ArrayRealVector(3);

    LevenbergMarquardt lm;
    ResidFunctionCylinderFit residualFunction;
    DMatrixRMaj param;

    public Ransac(){}

    public Ransac(ArrayList<double[]> data, double percentage, double percentage2, ResidFunctionCylinderFit rf, LevenbergMarquardt lm, DMatrixRMaj param){

        this.param = param;
        this.percentage = percentage;
        this.percentage2 = percentage2;
        this.lm = lm;
        this.residualFunction = rf;
        this.data = data;

        this.threshold = 0.05;

    }

    public double[] optimize(int n_params){

        double[] optimizedValues = new double[n_params];
        //double[] bestParameters = new double[initialValues.length];

        int n = Math.max((int)Math.floor(this.data.size() * percentage), 3);

        n = Math.min(3, n);

        this.d = Math.max((int)Math.floor(this.data.size() * percentage2), 2);

        double[] bestParameters = new double[n_params];
        double[] maybeGoodParameters = new double[n_params];

        //System.arraycopy(initialValues, 0, bestParameters, 0, n_params);

        ArrayList<double[]> maybeInliers = new ArrayList<>();
        ArrayList<double[]> alsoInliers = new ArrayList<>();

        double distance;

        for(int i = 0; i < maxIter; i++){

            Collections.shuffle(data);
            maybeInliers.clear();
            alsoInliers.clear();

            for(int j = 0; j < n; j++){

                maybeInliers.add(data.get(j));

            }

            maybeGoodParameters = estimateParameters(maybeInliers);



            startMat.setEntry(0, maybeGoodParameters[0]);
            startMat.setEntry(1, maybeGoodParameters[1]);
            startMat.setEntry(2, maybeGoodParameters[2]);

            endMat.setEntry(0, maybeGoodParameters[3]);
            endMat.setEntry(1, maybeGoodParameters[4]);
            endMat.setEntry(2, maybeGoodParameters[5]);

            for(int j = n; j < this.data.size(); j++){

                pointMat.setEntry(0, data.get(j)[0]);
                pointMat.setEntry(1, data.get(j)[1]);
                pointMat.setEntry(2, data.get(j)[2]);

                distance = shortDistance(startMat, endMat, pointMat);

                distance = (distance - maybeGoodParameters[6]);

                if(distance < 0) {
                    distance *= -1.0;
                }

                if(distance < threshold)
                    alsoInliers.add(data.get(j));
            }

            //System.out.println("ALSOINLIERS: " + alsoInliers.size() + " < " + d);
            if(alsoInliers.size() > d){

                maybeInliers.addAll(alsoInliers);
                maybeGoodParameters = estimateParameters(maybeInliers);

                double cost = cost(maybeInliers, maybeGoodParameters);

                //System.out.println("cost: " + cost);

                if(cost < minError){

                    System.arraycopy(maybeGoodParameters, 0,bestParameters , 0, maybeGoodParameters.length);
                    minError = cost;

                }
            }
        }

        return bestParameters;
    }

    public double[] estimateParameters(ArrayList<double[]> points){

        lm = new LevenbergMarquardt(1);
        lm.setDelta(0.01);

        residualFunction.setPoints(points);

        lm.optimize(residualFunction, param);

        return new double[]{param.data[0], param.data[1], param.data[2], param.data[3],
                param.data[4], param.data[5], param.data[6]};
    }

    public double cost(ArrayList<double[]> points, double[] parameters){

        startMat.setEntry(0, parameters[0]);
        startMat.setEntry(1, parameters[1]);
        startMat.setEntry(2, parameters[2]);

        endMat.setEntry(0, parameters[3]);
        endMat.setEntry(1, parameters[4]);
        endMat.setEntry(2, parameters[5]);
        double distance;
        double count = 0;
        double distanceSum = 0;

        for(int i = 0; i < points.size(); i++){

            pointMat.setEntry(0, points.get(i)[0]);
            pointMat.setEntry(1, points.get(i)[1]);
            pointMat.setEntry(2, points.get(i)[2]);

            distance = shortDistance(startMat, endMat, pointMat);

            distance = (distance - parameters[6]);

            if(distance < 0) {
                distance *= -1.0;
            }

            distanceSum += distance;

            count++;
        }
        return distanceSum / count;
    }

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

    RealVector crossProduct(RealVector a, RealVector b){

        double x1, y1, z1;

        x1 = a.getEntry(1) * b.getEntry(2) - a.getEntry(2) * b.getEntry(1);
        y1 = a.getEntry(2) * b.getEntry(0) - a.getEntry(0) * b.getEntry(2);
        z1 = a.getEntry(0) * b.getEntry(1) - a.getEntry(1) * b.getEntry(0);

        return new ArrayRealVector(new double[]{x1, y1, z1});

    }

}
