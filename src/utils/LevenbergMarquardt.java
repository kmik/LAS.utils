package utils;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;

/**
 * <p>
 * This is a straight forward implementation of the Levenberg-Marquardt (LM) algorithm. LM is used to minimize
 * non-linear cost functions:<br>
 * <br>
 * S(P) = Sum{ i=1:m , [y<sub>i</sub> - f(x<sub>i</sub>,P)]<sup>2</sup>}<br>
 * <br>
 * where P is the set of parameters being optimized.
 * </p>
 *
 * <p>
 * In each iteration the parameters are updated using the following equations:<br>
 * <br>RcF.cost_all
 * P<sub>i+1</sub> = (H + &lambda; I)<sup>-1</sup> d <br>
 * d =  (1/N) Sum{ i=1..N , (f(x<sub>i</sub>;P<sub>i</sub>) - y<sub>i</sub>) * jacobian(:,i) } <br>
 * H =  (1/N) Sum{ i=1..N , jacobian(:,i) * jacobian(:,i)<sup>T</sup> }
 * </p>
 * <p>
 * Whenever possible the allocation of new memory is avoided.  This is accomplished by reshaping matrices.
 * A matrix that is reshaped won't grow unless the new shape requires more memory than it has available.
 * </p>
 */
public class LevenbergMarquardt {
    // Convergence criteria
    private int maxIterations = 100;
    private double ftol = 1e-12;
    private double gtol = 1e-12;

    // how much the numerical jacobian calculation perturbs the parameters by.
    // In better implementation there are better ways to compute this delta.  See Numerical Recipes.
    //private double DELTA = 1e-1;
    private double DELTA = 0.1;

    // Dampening. Larger values means it's more like gradient descent
    private final double initialLambda;

    // the function that is optimized
    private ResidualFunction function;

    // the optimized parameters and associated costs
    private final DMatrixRMaj candidateParameters = new DMatrixRMaj(1,1);

    public double initialResiduals;
    public double finalResiduals;

    private double initialCost;
    public double finalCost;

    public double diameter = 0.0;

    // used by matrix operations
    private final DMatrixRMaj g = new DMatrixRMaj(1,1);            // gradient
    private final DMatrixRMaj g_simple = new DMatrixRMaj(1,1);            // gradient
    private final DMatrixRMaj H = new DMatrixRMaj(1,1);            // Hessian approximation
    private final DMatrixRMaj Hdiag = new DMatrixRMaj(1,1);
    private final DMatrixRMaj negativeStep = new DMatrixRMaj(1,1);

    // variables used by the numerical jacobian algorithm
    private final DMatrixRMaj temp0 = new DMatrixRMaj(1,1);
    private final DMatrixRMaj temp1 = new DMatrixRMaj(1,1);
    // used when computing d and H variables
    public DMatrixRMaj residuals = new DMatrixRMaj(1,1);

    // Where the numerical Jacobian is stored.
    private final DMatrixRMaj jacobian = new DMatrixRMaj(1,1);

    public double getInitialCost() {
        return initialCost;
    }

    public double getFinalCost() {
        return finalCost;
    }

    public double getFinalResiduals(){

        return this.finalResiduals;

    }

    public double getInitialResiduals(){

        return this.initialResiduals;

    }

    /**
     *
     * @param initialLambda Initial value of dampening parameter. Try 1 to start
     */
    public LevenbergMarquardt(double initialLambda) {
        this.initialLambda = initialLambda;
    }

    /**
     * Specifies convergence criteria
     *
     * @param maxIterations Maximum number of iterations
     * @param ftol convergence based on change in function value. try 1e-12
     * @param gtol convergence based on residual magnitude. Try 1e-12
     */
    public void setConvergence( int maxIterations , double ftol , double gtol ) {
        this.maxIterations = maxIterations;
        this.ftol = ftol;
        this.gtol = gtol;
    }

    public void setDelta(double delta){
        this.DELTA = delta;
    }

    /**
     * Finds the best fit parameters.
     *
     * @param function The function being optimized
     * @param parameters (Input/Output) initial parameter estimate and storage for optimized parameters
     * @return true if it succeeded and false if it did not.
     */
    public boolean optimize(ResidualFunction function, DMatrixRMaj parameters )
    {
        configure(function,parameters.getNumElements());

        // save the cost of the initial parameters so that it knows if it improves or not
        double previousCost = initialCost = cost(parameters);
        this.initialResiduals = residuals.data[0];
        // iterate until the difference between the costs is insignificant
        double lambda = initialLambda;

        // if it should recompute the Jacobian in this iteration or not
        boolean computeHessian = true;

        for( int iter = 0; iter < maxIterations; iter++ ) {
            if( computeHessian ) {
                // compute some variables based on the gradient
                computeGradientAndHessian(parameters);
                computeHessian = false;

                // check for convergence using gradient test
                boolean converged = true;
                for (int i = 0; i < g.getNumElements(); i++) {
                    if( Math.abs(g.data[i]) > gtol ) {
                        converged = false;
                        break;
                    }
                }
                if( converged )
                    return true;
            }

            // H = H + lambda*I
            for (int i = 0; i < H.numRows; i++) {
                H.set(i,i, Hdiag.get(i) + lambda);
            }

            //System.out.println("HERE");
            // In robust implementations failure to solve is handled much better
            if( !CommonOps_DDRM.solve(H, g, negativeStep) ) {
                return false;
            }
            //System.out.println("HERE");
            // compute the candidate parameters
            CommonOps_DDRM.subtract(parameters, negativeStep, candidateParameters);

            double cost = cost(candidateParameters);
            //System.out.println(cost + " " + previousCost);
            if( cost <= previousCost ) {
               // System.out.println("cost: " + cost);
                // the candidate parameters produced better results so use it
                computeHessian = true;
                parameters.set(candidateParameters);
                //System.out.println(parameters);

                this.finalResiduals = residuals.data[0];
                this.diameter = function.getSomething();
                // check for convergence
                // ftol <= (cost(k) - cost(k+1))/cost(k)
                boolean converged = ftol*previousCost >= previousCost-cost;

                previousCost = cost;
                lambda /= 10.0;

                if( converged ) {
                    //System.out.println( iter + " CONVEREGD!");
                    return true;
                }
            } else {
                lambda *= 10.0;
            }
/*
            System.out.print("\33[2K");
            System.out.println("Best solution: " + previousCost + "\r");
*/
            //System.out.println("iteration: " + iter + " \tfx: " + previousCost + " \tlambda: " + lambda);

        }

        //System.out.println();
        finalCost = previousCost;
        return true;
    }

    /**
     * Performs sanity checks on the input data and reshapes internal matrices.  By reshaping
     * a matrix it will only declare new memory when needed.
     */
    protected void configure(ResidualFunction function , int numParam )
    {
        this.function = function;
        int numFunctions = function.numFunctions();
        //System.out.println("what the cuck: " + numFunctions);
        // reshaping a matrix means that new memory is only declared when needed
        candidateParameters.reshape(numParam,1);
        g.reshape(numParam,1);
        H.reshape(numParam,numParam);
        negativeStep.reshape(numParam,1);

        // Normally these variables are thought of as row vectors, but it works out easier if they are column
        temp0.reshape(numFunctions,1);
        temp1.reshape(numFunctions,1);
        residuals.reshape(numFunctions,1);
        //System.out.println("reshaping:  " + numFunctions);
        //System.exit(1);
        jacobian.reshape(numFunctions,numParam);
    }

    /**
     * Computes the d and H parameters.
     *
     * d = J'*(f(x)-y)    <--- that's also the gradient
     * H = J'*J
     */
    private void computeGradientAndHessian(DMatrixRMaj param  )
    {
        // residuals = f(x) - y
        function.compute(param, residuals);

        computeNumericalJacobian(param,jacobian);
        CommonOps_DDRM.multTransA(jacobian, residuals, g);

        CommonOps_DDRM.multTransA(jacobian, jacobian,  H);

        CommonOps_DDRM.extractDiag(H,Hdiag);

    }


    /**
     * Computes the "cost" for the parameters given.
     *
     * cost = (1/N) Sum (f(x) - y)^2
     */
    private double cost(DMatrixRMaj param )
    {
        function.compute(param, residuals);
        //System.out.println(param);
        double error = NormOps_DDRM.normF(residuals);

        return error*error / (double)residuals.numRows;
    }

    /**
     * Computes a simple numerical Jacobian in parallel using all available logical
     * processors.
     *
     * @param param (input) The set of parameters that the Jacobian is to be computed at.
     * @param jacobian (output) Where the jacobian will be stored
     */
    protected void computeNumericalJacobian( DMatrixRMaj param ,
                                             DMatrixRMaj jacobian )
    {
        double invDelta = 1.0/DELTA;

        function.compute(param, temp0);

        int n_threads = Runtime.getRuntime().availableProcessors();

        int n_funk_per_thread = (int)Math.ceil((double)param.numRows / (double)n_threads);

        Thread[] threads = new Thread[n_threads];

        long startTime = System.currentTimeMillis();
        if(true)
        if(param.getNumElements() >= n_threads * 10) {
            for (int i = 0; i < n_threads; i++) {

                DMatrixRMaj param_temp = param.copy();
                DMatrixRMaj resid_temp = residuals.copy();
                DMatrixRMaj temp1_copy = temp1.copy();
                DMatrixRMaj temp0_copy = temp0.copy();
                int mini = i * n_funk_per_thread;
                int maxi = Math.min(param.numRows, mini + n_funk_per_thread);
                threads[i] = new jacobianParallel(function, mini, maxi, jacobian, param_temp, resid_temp, DELTA, temp0_copy, temp1_copy);
                threads[i].start();
            }

            for (int i = 0; i < threads.length; i++) {

                try {
                    threads[i].join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        long estimatedTime = System.currentTimeMillis() - startTime;

        // compute the jacobian by perturbing the parameters slightly
        // then seeing how it effects the results.
        if(false)
        for( int i = 0; i < param.getNumElements(); i++ ) {

            param.data[i] += DELTA;
            function.compute(param, temp1);
            // compute the difference between the two parameters and divide by the delta
            // temp1 = (temp1 - temp0)/delta
            CommonOps_DDRM.add(invDelta,temp1,-invDelta,temp0,temp1);

            // copy the results into the jacobian matrix
            // J(i,:) = temp1
            CommonOps_DDRM.insert(temp1,jacobian,0,i);

            param.data[i] -= DELTA;
        }

    }

    /**
     * The function that is being optimized. Returns the residual. f(x) - y
     */
    public interface ResidualFunction {
        /**
         * Computes the residual vector given the set of input parameters
         * Function which goes from N input to M outputs
         *
         * @param param (Input) N by 1 parameter vector
         * @param residual (Output) M by 1 output vector to store the residual = f(x)-y
         */
        void compute(DMatrixRMaj param , DMatrixRMaj residual );

        /**
         * Number of functions in output
         * @return function count
         */
        int numFunctions();

        double getSomething();
    }

}

class jacobianParallel extends Thread{

    int min, max;
    double DELTA;
    double invDelta;
    LevenbergMarquardt.ResidualFunction function;

    DMatrixRMaj jacobian, param, residual, temp1, temp0;

    public jacobianParallel(LevenbergMarquardt.ResidualFunction function, int min, int max, DMatrixRMaj jacobian, DMatrixRMaj param, DMatrixRMaj residual, double DELTA,
                            DMatrixRMaj temp0, DMatrixRMaj temp1){

        this.min = min;
        this.max = max;
        this.function = function;
        this.DELTA = DELTA;

        this.invDelta = 1.0/this.DELTA;

        this.jacobian = jacobian;
        this.param = param;
        this.residual = residual;

        this.temp0 = temp0;
        this.temp1 = temp1;
    }

    @Override
    public void run(){

        for( int i = min; i < this.max; i++ ) {

            param.data[i] += DELTA;
            function.compute(param, temp1);
            // compute the difference between the two parameters and divide by the delta
            // temp1 = (temp1 - temp0)/delta
            CommonOps_DDRM.add(invDelta,temp1,-invDelta,temp0,temp1);

            // copy the results into the jacobian matrix
            // J(i,:) = temp1
            CommonOps_DDRM.insert(temp1,jacobian,0,i);

            param.data[i] -= DELTA;
        }

    }

}