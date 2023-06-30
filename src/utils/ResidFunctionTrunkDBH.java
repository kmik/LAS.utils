package utils;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.ejml.data.DMatrixRMaj;
import tools.Statistics;

import java.util.ArrayList;
import java.util.List;

public class ResidFunctionTrunkDBH  implements LevenbergMarquardt.ResidualFunction {

    int numFunctions = 0;
    ArrayList<ArrayList<double[]>> points = new ArrayList<>();

    ArrayList<List<int[]>> combinations = new ArrayList<>();

    double sliceSize = 0.1;

    public boolean canBeOptimized = false;
    double initialCost = 0.0;

    public void compute(DMatrixRMaj param, DMatrixRMaj residual){

        ArrayList<double[]> dataForRansac = new ArrayList<>();

        int residualCounter = 0;
        RANSAC_circleFitter RcF = new RANSAC_circleFitter();
        SimpleRegression regression = new SimpleRegression(true);

        for(int i = 0; i < this.points.size(); i++){

            if(this.points.get(i).size() <= 10)
                continue;

            double threshold = 0, threshold_bound = 0, d_ = 0, d_bound = 0;
            int n = 3;
            d_ = param.get(i * 2 + 0, 0);

            //if(d_ >= 0.9 || d_ <= 0.20)
            d_bound = 0.20 + (Math.sin(d_) + 1) * ( (0.9 - 0.2) / 2.0);
            //else
            //    d_bound = d_;

            threshold = param.get(i * 2 + 1, 0);

            //if(threshold >= 0.07 || threshold <= 0.001)
            threshold_bound = 0.001 + (Math.sin(threshold) + 1) * ( (0.07 - 0.001) / 2.0);
            int d = (int)(points.get(i).size() * d_bound);

            //System.out.println("NUMBER OF POINTS: " + this.points.get(i).size());
            RcF.initialize4(this.points.get(i), n, d, threshold_bound, 1000, 1, -1);

            if(RcF.ransacFailed){

                try {
                    RcF.initialize_arraylist_a(this.points.get(i), n, d, threshold_bound, 5000);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            if(!Double.isNaN(RcF.radius_ransac)){
                dataForRansac.add(new double[]{this.points.get(i).get(this.points.get(i).size()-1)[2] - sliceSize * 1.5, RcF.radius_ransac});


                regression.addData(this.points.get(i).get(this.points.get(i).size()-1)[2] - sliceSize * 1.5, RcF.radius_ransac);



            }

            residual.set(residualCounter++, 0, RcF.cost_all);
            //residual.set(residualCounter++, 0, 0);
        }


        residual.set(residualCounter++, 0,regression.getMeanSquareError());

        //residual.set(0,0, regression.getMeanSquareError());
        //System.out.println(residual);
        //System.exit(1);
    }

    /**
     * Number of functions in output
     * @return function count
     */
    public int numFunctions(){

        return this.numFunctions;
    }

    public double getSomething(){

        return 0.0;

    }

    public double getDbh(ArrayList<ArrayList<double[]>> points, DMatrixRMaj param) throws Exception{

        ArrayList<double[]> dataForRansac = new ArrayList<>();
        int residualCounter = 0;
        double residualSum = 0.0;
        RANSAC_circleFitter RcF = new RANSAC_circleFitter();
        SimpleRegression regression = new SimpleRegression(true);
       // RansacLinearRegression RLR = new RansacLinearRegression();
        for(int i = 0; i < this.points.size(); i++){

            if(this.points.get(i).size() <= 10)
                continue;
            double threshold = 0, threshold_bound = 0, d_ = 0, d_bound = 0;
            int n = 3;
            d_ = param.get(i * 2 + 0, 0);

            //if(d_ >= 0.9 || d_ <= 0.20)
            d_bound = 0.20 + (Math.sin(d_) + 1) * ( (0.9 - 0.2) / 2.0);
            //else
            //    d_bound = d_;

            threshold = param.get(i * 2 + 1, 0);

            //if(threshold >= 0.07 || threshold <= 0.001)
            threshold_bound = 0.001 + (Math.sin(threshold) + 1) * ( (0.07 - 0.001) / 2.0);
            //else
            //   threshold_bound = threshold;

            //System.out.println("threshold: " + threshold + " t_bound: " + threshold_bound + " " + this.points.get(i).size());
            int d = (int)(points.get(i).size() * d_bound);
            RcF.initialize4(this.points.get(i), n, d, threshold_bound, 5000, 1, -1);

            /* What to do in case ransac fails? Add a penalty? */
            if(RcF.ransacFailed){
                RcF.initialize_arraylist_a(this.points.get(i), n, d, threshold, 5000);
            }

            if(!Double.isNaN(RcF.radius_ransac)){
                dataForRansac.add(new double[]{RcF.center_x, RcF.center_y, this.points.get(i).get(this.points.get(i).size()-1)[2] - sliceSize * 1.5, RcF.radius_ransac});


                regression.addData(this.points.get(i).get(this.points.get(i).size()-1)[2] - sliceSize * 1.5, RcF.radius_ransac);
            }


        }

        if(dataForRansac.size() > 3){
            RansacLinearRegression RLR = new RansacLinearRegression(dataForRansac, 0.2, 0.4, 0.025, -1);
            RLR.set_n(3);
            RLR.optimize2();
            double predicted_dbh = regression.predict(1.3);
            double newDbh_maybe = dbh_straight(dataForRansac, RLR.inlierIndexes);

            System.out.println("LEVENBERG DBH: " + predicted_dbh + " ? " + newDbh_maybe);

            return predicted_dbh;
        }


        return -1.0;
    }

    public double dbh_straight(ArrayList<double[]> ransacPoints, boolean[] inliers){

        double output = -1.0;

        double prev = 0.0;
        boolean prevInlier = false;
        double current = 0.0;

        double diamSum = 0.0;
        double counter = 0.0;
        Statistics stats = new Statistics();

        ArrayList<Double> diams = new ArrayList<>();

        SimpleRegression reg = new SimpleRegression(true);

        for(int i = 0; i < ransacPoints.size(); i++){

            current = ransacPoints.get(i)[2];
/*
            if(current >= 1.0 && current <= 2.0){

                reg.addData(current, ransacPoints.get(i)[3]);
                counter++;
            }
*/

            //current = ransacPoints.get(i)[2];

            if(current > 1.3){

                if(inliers[i] && prevInlier){

                    if(prev == 0.0) {
                        return -1.0;
                    }

                    return (ransacPoints.get(i)[3] + ransacPoints.get(i-1)[3]) / 2.0;
                }

            }

            prev = current;
            prevInlier = inliers[i];

        }
/*
        if(counter == 0.0)
            return -1.0;
        else{
            return reg.predict(1.3);
        }
*/
        return -1;
    }

    public void setNumFunctions(int n_funk){

        this.numFunctions = n_funk;

    }

    public void setup(ArrayList<ArrayList<double[]>> points, DMatrixRMaj param) throws Exception{

        setPoints(points);
        this.setNumFunctions(this.points.size() + 1);
        //this.setNumFunctions(1);
        ArrayList<double[]> dataForRansac = new ArrayList<>();
        int residualCounter = 0;
        double residualSum = 0.0;
        RANSAC_circleFitter RcF = new RANSAC_circleFitter();
        SimpleRegression regression = new SimpleRegression(true);

        for(int i = 0; i < this.points.size(); i++){

            if(this.points.get(i).size() <= 10)
                continue;
            double threshold = 0, threshold_bound = 0, d_ = 0, d_bound = 0;
            int n = 3;
            d_ = param.get(i * 2 + 0, 0);

            //if(d_ >= 0.9 || d_ <= 0.20)
            d_bound = 0.20 + (Math.sin(d_) + 1) * ( (0.9 - 0.2) / 2.0);
            //else
            //    d_bound = d_;

            threshold = param.get(i * 2 + 1, 0);

            //if(threshold >= 0.07 || threshold <= 0.001)
                threshold_bound = 0.001 + (Math.sin(threshold) + 1) * ( (0.07 - 0.001) / 2.0);
            //else
             //   threshold_bound = threshold;

            //System.out.println("threshold: " + threshold + " t_bound: " + threshold_bound + " " + this.points.get(i).size());
            int d = (int)(points.get(i).size() * d_bound);
            RcF.initialize4(this.points.get(i), n, d, threshold_bound, 5000, 1, -1);

            /* What to do in case ransac fails? Add a penalty? */
            if(RcF.ransacFailed){
                RcF.initialize_arraylist_a(this.points.get(i), n, d, threshold, 5000);
            }

            if(!Double.isNaN(RcF.radius_ransac)){
                dataForRansac.add(new double[]{this.points.get(i).get(this.points.get(i).size()-1)[2] - sliceSize * 1.5, RcF.radius_ransac});


                    regression.addData(this.points.get(i).get(this.points.get(i).size()-1)[2] - sliceSize * 1.5, RcF.radius_ransac);
            }

            residualSum += RcF.radius_ransac;
            residualSum += RcF.cost_all;
            residualCounter += 2;
        }

        if(dataForRansac.size() >= 2){

            this.canBeOptimized = true;

            initialCost = regression.getMeanSquareError();


            System.out.println("Initial cost: " + initialCost);

        }else{
            this.canBeOptimized = false;
        }


    }

    public void setPoints(ArrayList<ArrayList<double[]>> points){

        this.points = points;

    }

    public boolean rejectSolution(){

        return false;
    }
    public void onlyModifyThis(int i){

    }

}
