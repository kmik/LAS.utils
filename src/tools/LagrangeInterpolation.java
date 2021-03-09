package tools;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.ejml.data.Matrix;
import org.jblas.Solve;
import org.jblas.DoubleMatrix;
import org.apache.commons.math3.*;

/**
 * Given n points (x0,y0)...(xn-1,yn-1), the following methid computes
 * the polynomial factors of the n-1't degree polynomial passing through
 * the n points.
 *
 * Example: Passing in three points (2,3) (1,4) and (3,7) will produce
 * the results [2.5, -8.5, 10] which means that the points is on the
 * curve y = 2.5x² - 8.5x + 10.
 *
 */

public class LagrangeInterpolation {

    double[] output = new double[3];

    public LagrangeInterpolation(){

    }

    public void nPoints(int n){
        output = new double[n];
    }

    public double[] findPolynomialFactors (double[] x, double[] y)
            throws RuntimeException {
        int n = x.length;

        double[][] data = new double[n][n];
        double[]   rhs  = new double[n];

        for (int i = 0; i < n; i++) {
            double v = 1;
            for (int j = 0; j < n; j++) {
                data[i][n-j-1] = v;
                v *= x[i];
            }

            rhs[i] = y[i];
        }

        // Solve m * s = b

        //Matrix s = m.solve (b);
        //RealMatrix m = new Array2DRowRealMatrix(data);
        //RealMatrix b = new Array2DRowRealMatrix(rhs);

        //DecompositionSolver solver = new LUDecomposition(m).getSolver();
        DoubleMatrix m = new DoubleMatrix(data);
        DoubleMatrix b = new DoubleMatrix (rhs);
        //RealMatrix s = solver.solve(b);
        DoubleMatrix s = Solve.solve(m,b);


        for(int i = 0; i < output.length; i++){

            //output[i] = s.get(i,0);
            //s.getEntry(i,0)
        }

        return output;
    }

}
