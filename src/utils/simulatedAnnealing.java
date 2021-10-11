package utils;

import org.ejml.data.DMatrixRMaj;

import java.util.Random;

public class simulatedAnnealing {

    ResidFunction rs;

    Random random = new Random();

    //private DMatrixRMaj parameters = null;
    public DMatrixRMaj residuals = new DMatrixRMaj(1,1);

    double t;
    double maxT = 1.0;
    double minT = 0.05;

    int numParamsSwitched;

    double cooling = 0.8;

    int iterPerTemp = 20;

    double step = 1e-1d;
    double initialCost;

    public simulatedAnnealing(){}

    public void optimize(ResidFunction rs, DMatrixRMaj parameters ){

        this.rs = rs;

        residuals.set(0,0, 0);

        DMatrixRMaj parameters_temp = new DMatrixRMaj(parameters.numRows,parameters.numCols);
        DMatrixRMaj parameters_optimum = new DMatrixRMaj(parameters.numRows,parameters.numCols);
        int[] indexes = new int[parameters.numRows];

        for(int i = 0; i < indexes.length; i++){
            indexes[i] = i;
        }

        numParamsSwitched = parameters.numRows;
        t = maxT;

        double previousCost = initialCost = cost(parameters);
        double currentCost;
        double all_time_best_cost = Double.POSITIVE_INFINITY;

        while(t >= minT){

            step *= t;

            numParamsSwitched = (int)Math.ceil(numParamsSwitched * t);

            for(int p = 0; p < iterPerTemp; p++) {

                shuffleArray(indexes);

                for (int i = 0; i < numParamsSwitched; i++) {
                    parameters_temp.set(indexes[i], 0, parameters.get(indexes[i], 0) + (( random.nextBoolean() ? 1 : -1 ) * step));
                }

                currentCost = cost(parameters_temp);

                if(accept(t, previousCost, currentCost)){

                    for(int k = 0; k < parameters.numRows; k++){
                        parameters.set(k, 0, parameters_temp.get(k, 0));
                    }

                    previousCost = currentCost;

                    if(currentCost < all_time_best_cost){
                        for(int k = 0; k < parameters.numRows; k++){
                            parameters_optimum.set(k, 0, parameters.get(k, 0));
                        }

                        all_time_best_cost = currentCost;
                    }
                }
            }

            System.out.println("bestCost = " + all_time_best_cost + " \nt: " + t + "\nnumparamsSwitched: " + numParamsSwitched + "\nstep: " + step);
            System.out.println("-------------------");
            t *= cooling;

        }

        for(int k = 0; k < parameters.numRows; k++){
            parameters.set(k, 0, parameters_optimum.get(k, 0));
        }

    }

    public boolean accept(double t, double oldCost, double newCost){

        double e = Math.exp(1);
        double random = Math.random();
        double powi = ((oldCost - newCost) / t);
        double a = Math.pow(e, powi);

        return random < a;

    }

    public double cost(DMatrixRMaj parameters){

        rs.compute(parameters, residuals);

        return residuals.get(0,0);
    }

    public void shuffleArray(int[] array) {
        int index;

        for (int i = array.length - 1; i > 0; i--)
        {
            index = random.nextInt(i + 1);
            if (index != i)
            {
                array[index] ^= array[i];
                array[i] ^= array[index];
                array[index] ^= array[i];
            }
        }
    }
}
