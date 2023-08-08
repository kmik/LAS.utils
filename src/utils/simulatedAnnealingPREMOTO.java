package utils;

import org.ejml.data.DMatrixRMaj;

import java.util.Random;

public class simulatedAnnealingPREMOTO {

    ResidFunctionPREMOTO rs;

    Random random = new Random();

    //private DMatrixRMaj parameters = null;
    public DMatrixRMaj residuals = new DMatrixRMaj(1,1);

    double t;
    double maxT = 1.0;
    double minT = 0.05;

    int numParamsSwitched;

    double cooling = 0.7;

    int iterPerTemp = 500;

    double step = 1e-1d;
    double step2 = 1e-1d;
    double initialCost;


    double[] rangeParam1 = new double[]{2.0, 10.0};
    double[] rangeParam2 = new double[]{0.0, 360.0};


    public simulatedAnnealingPREMOTO(){}

    public void optimize(ResidFunctionPREMOTO rs, DMatrixRMaj parameters ){

        this.rs = rs;

        residuals.set(0,0, 0);
        residuals = new DMatrixRMaj(rs.trees.size(), 1);

        DMatrixRMaj parameters_temp = new DMatrixRMaj(parameters.numRows,parameters.numCols);
        DMatrixRMaj parameters_optimum = new DMatrixRMaj(parameters.numRows,parameters.numCols);
        int[] indexes = new int[parameters.numRows];

        for(int i = 0; i < indexes.length; i++){
            indexes[i] = i;
        }

        numParamsSwitched = parameters.numRows;
        t = maxT;

        step = (rangeParam1[1] - rangeParam1[0]) / 10;
        step2 = (rangeParam2[1] - rangeParam2[0]) / 10;

        double previousCost = initialCost = cost(parameters);

        System.out.println("INITIAL COST: " + initialCost);

        double currentCost;
        double all_time_best_cost = Double.POSITIVE_INFINITY;
        double stepNow = step;

        while(t >= minT){

            step *= t;
            step2 *= t;

            step = 0.5;
            step2 = 2.5;

            System.out.println("step distance: " + step + " step angle " + step2);

            numParamsSwitched = (int)Math.ceil(numParamsSwitched * t);

            numParamsSwitched = (int) (0.5 * parameters.data.length);

            for(int p = 0; p < iterPerTemp; p++) {

                shuffleArray(indexes);

                for (int i = 0; i < numParamsSwitched; i++) {

                    if( indexes[i] % 2 == 0) {
                        stepNow = step;
                    }
                    else {
                        stepNow = step2;
                    }
                    //System.out.println("before: " + parameters.get(indexes[i], 0));
                    double amountOfChange = (( random.nextBoolean() ? 1 : -1 ) * stepNow);

                    if(amountOfChange < 0)
                        amountOfChange *= -1;

                    double randomDistance = randomDouble(2, 10);
                    double randomAngle = randomDouble(0, 360);

                    if(indexes[i] % 2 == 0) {

                        parameters_temp.set(indexes[i], 0, randomDistance);

                        if (parameters.get(indexes[i], 0) + amountOfChange > rangeParam1[1])

                            amountOfChange = parameters.get(indexes[i], 0) + amountOfChange - rangeParam1[1];

                    }else{

                        parameters_temp.set(indexes[i], 0, randomAngle);

                        if (parameters.get(indexes[i], 0) + amountOfChange > rangeParam2[1])

                            amountOfChange = parameters.get(indexes[i], 0) + amountOfChange - rangeParam2[1];

                    }



                    //parameters_temp.set(indexes[i], 0, parameters.get(indexes[i], 0) + amountOfChange);
                    //System.out.println("after: " + parameters_temp.get(indexes[i], 0));
                    //System.out.println("----------------------");
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

    public void optimize2(ResidFunctionPREMOTO rs, DMatrixRMaj parameters ){

        this.rs = rs;

        residuals.set(0,0, 0);
        residuals = new DMatrixRMaj(rs.trees.size(), 1);

        DMatrixRMaj parameters_temp = new DMatrixRMaj(parameters.numRows,parameters.numCols);
        DMatrixRMaj parameters_optimum = new DMatrixRMaj(parameters.numRows,parameters.numCols);
        int[] indexes = new int[parameters.numRows];

        for(int i = 0; i < indexes.length; i++){
            indexes[i] = i;
        }

        numParamsSwitched = parameters.numRows;
        t = maxT;

        step = (rangeParam1[1] - rangeParam1[0]) / 10;
        step2 = (rangeParam2[1] - rangeParam2[0]) / 10;

        double previousCost = initialCost = cost(parameters);

        System.out.println("INITIAL COST: " + initialCost);

        double currentCost;
        double all_time_best_cost = Double.POSITIVE_INFINITY;
        double stepNow = step;

        while(t >= minT){

            step *= t;
            step2 *= t;

            step = 0.5;
            step2 = 2.5;

            System.out.println("step distance: " + step + " step angle " + step2);

            numParamsSwitched = (int)Math.ceil(numParamsSwitched * t);

            numParamsSwitched = (int) (1.0 * parameters.data.length);

            for(int p = 0; p < iterPerTemp; p++) {

                shuffleArray(indexes);

                for (int i = 0; i < numParamsSwitched; i++) {



                    double randomDistance = randomDouble(2, 10);
                    double randomAngle = randomDouble(-110, 110);

                    //System.out.println("before: " + parameters.get(indexes[i], 0));
                    parameters_temp.set(indexes[i], 0, randomAngle);
                    //parameters_temp.set(indexes[i], 0, parameters.get(indexes[i], 0) + amountOfChange);
                    //System.out.println("after: " + parameters_temp.get(indexes[i], 0));
                    //System.out.println("----------------------");
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

    public void apply(DMatrixRMaj parameters){

        this.rs.applyToTrees(parameters);

    }

    public double randomDouble(double min, double max){
        return min + (max - min) * Math.random();
    }


    public boolean accept(double t, double oldCost, double newCost){

        double e = Math.exp(1);
        double random = Math.random();
        double powi = ((oldCost - newCost) / t);
        double a = Math.pow(e, powi);

        return random < a;

    }

    public double cost(DMatrixRMaj parameters){

        rs.compute2(parameters, residuals);

        if(residuals.data.length > 0){
            return average(residuals.data);
        }
        else
            return residuals.get(0,0);

    }

    public double average(double[] in){

        double sum = 0;

        for(int i = 0; i < in.length; i++){
            sum += in[i];
        }

        return sum / in.length;

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
