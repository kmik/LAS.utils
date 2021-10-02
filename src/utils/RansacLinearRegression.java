package utils;


import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.ejml.data.DMatrixRMaj;

import java.lang.reflect.Array;
import java.text.CollationElementIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class RansacLinearRegression {

    ArrayList<double[]> data;

    int n;
    int d;

    int maxIter = 10000;
    double threshold;

    double bestFit = -1.0;
    double minError = Double.MAX_VALUE;
    double percentage;
    double percentage2;

    double[] bestParameters;

    boolean includeIntercept = true;

    ArrayList<Integer> indexes;

    RealVector pointMat = new ArrayRealVector(3);
    RealVector startMat = new ArrayRealVector(3);
    RealVector endMat = new ArrayRealVector(3);

    LevenbergMarquardt lm;
    ResidFunctionCylinderFit residualFunction;
    DMatrixRMaj param;

    SimpleRegression maybeGoodRegression;
    SimpleRegression maybeBestRegression;
    SimpleRegression simpleRegressionBest;
    int seed = -1;

    double constrain_x = -1;

    public boolean ransacFailed = false;

    double constrain_y = -1;
    double constrain_y_min = -1;


    double maxRadius = 0.25;

    ArrayList<double[]> fixedPoints = new ArrayList<double[]>();
    public boolean[] inlierIndexes;

    ArrayList<Double> weights = new ArrayList<>();

    public RansacLinearRegression (){

    }

    public void setMaxRad(double maxRad){

        this.maxRadius = maxRad;

    }

    public void percentage2(double percentage2){

        this.percentage2 = percentage2;
    }


    public void addWeights(ArrayList<Double> weight){

        this.weights = weight;

    }

    public void constrain(double x, double y, double y_min){

        this.constrain_x = x;
        this.constrain_y = y;
        this.constrain_y_min = y_min;

    }

    public void intercept(boolean includeIntercept){

        this.includeIntercept = includeIntercept;

    }
    public RansacLinearRegression (ArrayList<double[]> data, double percentage, double percentage2, double threshold, int seed){

        this.percentage = percentage;
        this.percentage2 = percentage2;

        this.data = data;

        this.threshold = 0.05;

        this.threshold = threshold;

        this.seed = seed;

    }

    public void set_n(int n_in){

        this.n = n_in;

    }

    public void addFixedPoint(double[] in){

        this.fixedPoints.add(in);

    }

    public SimpleRegression optimize(){

        int n = Math.max((int)Math.floor(this.data.size() * percentage), 3);

        n = Math.min(3, n);

        this.d = Math.max((int)Math.floor(this.data.size() * percentage2), 2);

        ArrayList<Integer> indexes = new ArrayList<>();

        for(int i = 0; i < data.size(); i++)
            indexes.add(i);

        simpleRegressionBest = new SimpleRegression(true);

        //System.arraycopy(initialValues, 0, bestParameters, 0, n_params);

        ArrayList<double[]> maybeInliers = new ArrayList<>();
        ArrayList<double[]> alsoInliers = new ArrayList<>();
        ArrayList<double[]> bestObservations = new ArrayList<>();

        inlierIndexes = new boolean[data.size()];

        //ArrayList<Integer> maybeGoodIndexes = new ArrayList<>();
        boolean[] maybeGoodIndexes;

        double distance;

        for(int i = 0; i < maxIter; i++){

            //Collections.shuffle(data);
            Collections.shuffle(indexes);
            maybeInliers.clear();
            alsoInliers.clear();
            maybeGoodIndexes = new boolean[data.size()];

            maybeGoodRegression = new SimpleRegression(true);

            for(int j = 0; j < fixedPoints.size(); j++){

                maybeGoodRegression.addData(fixedPoints.get(j)[0], fixedPoints.get(j)[1]);

            }

            for(int j = 0; j < n; j++){

                /*
                maybeInliers.add(data.get(j));
                maybeGoodRegression.addData(data.get(j)[0], data.get(j)[1]);
                   */
                maybeGoodIndexes[indexes.get(j)] = true;
                maybeInliers.add(data.get(indexes.get(j)));
                maybeGoodRegression.addData(data.get(indexes.get(j))[0], data.get(indexes.get(j))[1]);

            }

            for(int j = n; j < this.data.size(); j++){

                //distance = maybeGoodRegression.predict(data.get(j)[0]) - data.get(j)[1];
                distance = maybeGoodRegression.predict(data.get(indexes.get(j))[0]) - data.get(indexes.get(j))[1];

                if(distance < 0) {
                    distance *= -1.0;
                }

                if(distance < threshold) {
                    maybeGoodIndexes[indexes.get(j)] = true;
                    alsoInliers.add(data.get(indexes.get(j)));
                }

            }

            if(alsoInliers.size() > d){

                maybeInliers.addAll(alsoInliers);

                maybeBestRegression = new SimpleRegression(true);

                for(int j = 0; j < maybeInliers.size(); j++){

                    maybeBestRegression.addData(maybeInliers.get(j)[0], maybeInliers.get(j)[1]);

                }

                double cost = maybeBestRegression.getMeanSquareError();

                if(cost < minError){

                    this.inlierIndexes = maybeGoodIndexes.clone();
                    bestObservations.clear();
                    bestObservations.addAll((ArrayList<double[]>)maybeInliers.clone());

                    minError = cost;

                }
            }
        }

        simpleRegressionBest = new SimpleRegression(true);

        for(int i = 0; i < bestObservations.size(); i++){

            simpleRegressionBest.addData(bestObservations.get(i)[0], bestObservations.get(i)[1]);

        }

        return simpleRegressionBest;
    }

    public SimpleRegression optimize2(){

        //int n = Math.max((int)Math.floor(this.data.size() * percentage), 3);

        //n = Math.min(3, n);
        this.maxIter = 1000;
        this.d = Math.max((int)Math.floor((this.data.size() - this.n) * percentage2), 1);

        ArrayList<Integer> indexes = new ArrayList<>();

        for(int i = 0; i < data.size(); i++)
            indexes.add(i);

        simpleRegressionBest = new SimpleRegression(includeIntercept);

        //System.arraycopy(initialValues, 0, bestParameters, 0, n_params);

        ArrayList<double[]> maybeInliers = new ArrayList<>();
        ArrayList<double[]> alsoInliers = new ArrayList<>();
        ArrayList<double[]> bestObservations = new ArrayList<>();

        inlierIndexes = new boolean[data.size()];

        //ArrayList<Integer> maybeGoodIndexes = new ArrayList<>();
        boolean[] maybeGoodIndexes;

        double distance;

        for(int i = 0; i < maxIter; i++){

            //Collections.shuffle(data);
            if(seed == -1)
                Collections.shuffle(indexes);
            else
                Collections.shuffle(indexes,new Random(seed));

            maybeInliers.clear();
            alsoInliers.clear();
            maybeGoodIndexes = new boolean[data.size()];

            maybeGoodRegression = new SimpleRegression(includeIntercept);

            for(int j = 0; j < fixedPoints.size(); j++){

                maybeGoodRegression.addData(fixedPoints.get(j)[0], fixedPoints.get(j)[1]);

            }

            for(int j = 0; j < n; j++){

                /*
                maybeInliers.add(data.get(j));
                maybeGoodRegression.addData(data.get(j)[0], data.get(j)[1]);
                   */
                maybeGoodIndexes[indexes.get(j)] = true;
                maybeInliers.add(data.get(indexes.get(j)));
                maybeGoodRegression.addData(data.get(indexes.get(j))[0], data.get(indexes.get(j))[1]);

            }

            for(int j = n; j < this.data.size(); j++){

                //distance = maybeGoodRegression.predict(data.get(j)[0]) - data.get(j)[1];
                distance = maybeGoodRegression.predict(data.get(indexes.get(j))[0]) - data.get(indexes.get(j))[1];

                if(distance < 0) {
                    distance *= -1.0;
                }

                if(distance < threshold) {
                    maybeGoodIndexes[indexes.get(j)] = true;
                    alsoInliers.add(data.get(indexes.get(j)));
                }

            }

            if(alsoInliers.size() > d){

                maybeInliers.addAll(alsoInliers);

                maybeBestRegression = new SimpleRegression(includeIntercept);

                for(int j = 0; j < maybeInliers.size(); j++){

                    maybeBestRegression.addData(maybeInliers.get(j)[0], maybeInliers.get(j)[1]);

                }

                double cost = maybeBestRegression.getMeanSquareError();

                if(false)
                if(maybeBestRegression.getIntercept() <= 0.0) //maybeBestRegression.getSlope() < 0.0 ||
                    cost = Double.POSITIVE_INFINITY;

                if(true){

                    if(constrain_y != -1.0){

                        double pred_y = maybeBestRegression.predict(constrain_x);
                        if(pred_y > constrain_y  || pred_y <= constrain_y_min)
                            cost = Double.POSITIVE_INFINITY;

                    }else{

                    }

                }
                if(cost < minError){

                    this.inlierIndexes = maybeGoodIndexes.clone();
                    bestObservations.clear();
                    bestObservations = new ArrayList<>(maybeInliers);

                    minError = cost;

                }
            }
        }

        simpleRegressionBest = new SimpleRegression(includeIntercept);

        for(int i = 0; i < bestObservations.size(); i++){

            simpleRegressionBest.addData(bestObservations.get(i)[0], bestObservations.get(i)[1]);

        }

        if(bestObservations.size() == 0)
            ransacFailed = true;


        return simpleRegressionBest;
    }

    public SimpleRegression optimize3(){

        //int n = Math.max((int)Math.floor(this.data.size() * percentage), 3);
        this.ransacFailed = false;

        //n = Math.min(3, n);
        this.maxIter = 1000;
        this.d = Math.max((int)Math.floor((this.data.size() - this.n) * percentage2), 2);

        ArrayList<Integer> indexes = new ArrayList<>();

        for(int i = 0; i < data.size(); i++)
            indexes.add(i);

        simpleRegressionBest = new SimpleRegression(includeIntercept);

        //System.arraycopy(initialValues, 0, bestParameters, 0, n_params);

        ArrayList<double[]> maybeInliers = new ArrayList<>();
        ArrayList<double[]> alsoInliers = new ArrayList<>();
        ArrayList<double[]> bestObservations = new ArrayList<>();

        inlierIndexes = new boolean[data.size()];

        //ArrayList<Integer> maybeGoodIndexes = new ArrayList<>();
        boolean[] maybeGoodIndexes;
        int maxInliers = 0;

        double distance;

        for(int i = 0; i < maxIter; i++){

            //Collections.shuffle(data);
            if(seed == -1)
                Collections.shuffle(indexes);
            else
                Collections.shuffle(indexes,new Random(seed));

            maybeInliers.clear();
            alsoInliers.clear();
            maybeGoodIndexes = new boolean[data.size()];

            maybeGoodRegression = new SimpleRegression(includeIntercept);

            for(int j = 0; j < fixedPoints.size(); j++){

                maybeGoodRegression.addData(fixedPoints.get(j)[0], fixedPoints.get(j)[1]);

            }

            for(int j = 0; j < n; j++){

                /*
                maybeInliers.add(data.get(j));
                maybeGoodRegression.addData(data.get(j)[0], data.get(j)[1]);
                   */
                maybeGoodIndexes[indexes.get(j)] = true;
                maybeInliers.add(data.get(indexes.get(j)));
                maybeGoodRegression.addData(data.get(indexes.get(j))[0], data.get(indexes.get(j))[1]);

            }

            for(int j = n; j < this.data.size(); j++){

                //distance = maybeGoodRegression.predict(data.get(j)[0]) - data.get(j)[1];
                distance = maybeGoodRegression.predict(data.get(indexes.get(j))[0]) - data.get(indexes.get(j))[1];

                if(distance < 0) {
                    distance *= -1.0;
                }

                if(distance < threshold) {
                    maybeGoodIndexes[indexes.get(j)] = true;
                    alsoInliers.add(data.get(indexes.get(j)));
                }

            }

            if(alsoInliers.size() > d){

                maybeInliers.addAll(alsoInliers);

                maybeBestRegression = new SimpleRegression(includeIntercept);

                for(int j = 0; j < maybeInliers.size(); j++){

                    maybeBestRegression.addData(maybeInliers.get(j)[0], maybeInliers.get(j)[1]);

                }

                double cost = maybeBestRegression.getMeanSquareError();

                int n_inliers = maybeInliers.size();


                if(false)
                    if(maybeBestRegression.getSlope() < 0.0 || maybeBestRegression.getIntercept() < 0.0)
                        n_inliers = Integer.MIN_VALUE;
                    else
                        n_inliers = maybeInliers.size();

                if(true){

                    if(constrain_y != -1.0){

                        double pred_y = maybeBestRegression.predict(constrain_x);
                        if(pred_y > constrain_y  || pred_y <= constrain_y_min)
                            n_inliers = Integer.MIN_VALUE;

                    }else{
                        n_inliers = maybeInliers.size();
                    }

                }

                if(n_inliers >= maxInliers){

                    if(n_inliers == maxInliers)
                        if(cost > minError)
                            continue;


                    maxInliers = n_inliers;

                    this.inlierIndexes = maybeGoodIndexes.clone();
                    bestObservations.clear();
                    bestObservations.addAll((ArrayList<double[]>)maybeInliers.clone());

                    minError = cost;

                }
            }
        }

        simpleRegressionBest = new SimpleRegression(includeIntercept);

        for(int i = 0; i < bestObservations.size(); i++){

            simpleRegressionBest.addData(bestObservations.get(i)[0], bestObservations.get(i)[1]);

        }

        if(bestObservations.size() == 0)
            ransacFailed = true;

        return simpleRegressionBest;
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

