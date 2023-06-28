package utils;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.analysis.solvers.LaguerreSolver;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.analysis.solvers.PolynomialSolver;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.solvers.BisectionSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;

import java.util.Arrays;

public class curveFitting {

    argumentReader aR;
    public curveFitting(){

    }

    public curveFitting(argumentReader aR){
        this.aR = aR;
    }


    public static double findIntersection(double[] x, double[] y, int degree) {

        if(x.length < 2){
            return -1;
        }
        WeightedObservedPoints points = new WeightedObservedPoints();
        for (int i = 0; i < x.length; i++) {
            points.add(x[i], y[i]);
        }

        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(degree);



        double[] coefficients = fitter.fit(points.toList());

        PolynomialFunction polynomialFunction = new PolynomialFunction(coefficients);

        coefficients = polynomialFunction.getCoefficients();

        // Specify the range of x-values to check for intersections
        double minX = 1.3;
        double maxX = 45;
        double stepSize = 0.1;

        double intersection = 0;

        // Iterate over the range of x-values and check if y is approximately zero
        for (double x_ = minX; x_ <= maxX; x_ += stepSize) {

            double y_ = polynomialFunction.value(x_);

            //System.out.println(y_);
            if (y_ <= 0.0) { // Adjust the threshold as needed
                intersection = x_;
                break;
            }
        }

        //System.out.println("#########################");
        return intersection;
    }


}
