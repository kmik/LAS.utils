package utils;

import err.toolException;
import org.apache.commons.math3.ml.distance.EarthMoversDistance;

import java.util.*;

public class simulatedAnnealingForestSimulatorAlternatingSize {


    int[] target;

    ArrayList<int[]> targetSpecies;
    List<Double> residual;

    double cost;
    ArrayList<forestTree> current_solution;
    HashMap<Integer, forestTree> current_solution_map;
    public List<forestTree> best_solution;
    public HashMap<Integer, forestTree> best_solution_map;

    List<forestTree> originalPlot;
    HashMap<Integer, forestTree> originalPlot_map;
    List<forestTree> searchSpace;
    HashMap<Integer, forestTree> searchSpace_map;
    double maxTemperature = 1.0;
    double minTemperature = 0.05;

    double cooling = 0.98;

    double maxValue = -1;
    double[] maxValue_array = new double[]{0,0,0};


    int iterPerTemp = 250;

    double switch_prop = 0.5;

    int numParamsSwitched;

    double bins;
    Random r = new Random();

    public simulatedAnnealingForestSimulatorAlternatingSize(){

        r.setSeed(1234);
    }

    public void initialSolution(ArrayList<forestTree> parameters){
        this.current_solution = parameters;
        this.originalPlot = (ArrayList<forestTree>)parameters.clone();
        numParamsSwitched = parameters.size();
        this.best_solution = (ArrayList<forestTree>)parameters.clone();
    }

    public void initialSolution(HashMap<Integer, forestTree> parameters){
        this.current_solution_map = (HashMap<Integer, forestTree>) parameters.clone();
        this.originalPlot_map = (HashMap<Integer, forestTree>)parameters.clone();
        numParamsSwitched = parameters.size() / 2;
        this.best_solution_map = (HashMap<Integer, forestTree>)parameters.clone();
    }


    public void params(ArrayList<forestTree> parameters){
        this.current_solution = parameters;
    }

    public void setTarget(int[] target, double bins, double maxValue){
        this.bins = bins;
        this.target = target;
        this.maxValue = maxValue;
    }

    public void setTarget(int[] target, double bins, double[] maxValue){
        this.bins = bins;
        this.target = target;
        this.maxValue_array = maxValue;
    }


    public void setTargetSpeciesSpecific(ArrayList<int[]> target, double bins){
        this.bins = bins;
        this.targetSpecies = target;
    }

    public void setSearchSpace(ArrayList<forestTree> searchSpace){
        this.searchSpace = (ArrayList<forestTree>)searchSpace.clone();
    }
    public void setSearchSpace(HashMap<Integer, forestTree> searchSpace){
        this.searchSpace_map = (HashMap<Integer, forestTree>)searchSpace.clone();
    }

    public double cost(){
        return cost;
    }

    public void optimize(){

        //double[] diameterDistribution_ = calculateDiameterDistribution(current_solution);
        int[] diameterDistribution_ = calculateDiameterDistribution_(current_solution_map);
        ArrayList<int[]> diameterDistribution_species = calculateSpeciesSpecificDiameterDistributions(current_solution_map);

        //double initialCost = hellingerDistance(target, diameterDistribution_);
        //double initialCost = hellingerDistance_species(targetSpecies, diameterDistribution_species);
        double initialCost = costWithEMD(targetSpecies, diameterDistribution_species);



        System.out.println(Arrays.toString(target));
        System.out.println(Arrays.toString(diameterDistribution_));
        System.out.println("initialCost: " + initialCost);
        System.out.println("bins: " + bins);
        //System.exit(0);
        double previousCost = initialCost;

        double all_time_best_cost = initialCost;
        double t = maxTemperature;
        int maxParamsSwitched = numParamsSwitched;
        double orig_switch_probability = switch_prop;

        while(t >= this.minTemperature){

            //numParamsSwitched = (int)Math.ceil(maxParamsSwitched * t);
            //numParamsSwitched = maxParamsSwitched;
            //System.out.println("numParamsSwitched: " + numParamsSwitched);
            //switch_prop = orig_switch_probability * (t * 1.5);
            switch_prop = t - 0.1;
            int swap_this_many = (int)Math.ceil(switch_prop * numParamsSwitched);
            swap_this_many = Math.max(1, swap_this_many);
            int average_n_swapped = 0;


            for(int i = 0; i < iterPerTemp; i++) {

                ///Collections.shuffle(current_solution);
                int n_swapped = swapStuff(current_solution_map, searchSpace_map, switch_prop, swap_this_many);
                average_n_swapped += n_swapped;
                //int[] diamterDistribution = calculateDiameterDistribution_(current_solution_map);
                ArrayList<int[]> diamterDistribution = calculateSpeciesSpecificDiameterDistributions(current_solution_map);

                //double newCost = bhattacharyyaDistance(target, diamterDistribution);
                //double newCost = hellingerDistance_species(targetSpecies, diamterDistribution);
                double newCost = costWithEMD(targetSpecies, diamterDistribution);


                if(accept(t, previousCost, newCost)) {
                //if (acceptanceProbability(previousCost, newCost, t) > r.nextDouble()){

                    //if(newCost > previousCost)
                    //    System.out.println("ACCEPTED WORSE SOLUTION");

                    if(newCost < all_time_best_cost){
                        all_time_best_cost = newCost;
                        best_solution_map = (HashMap<Integer, forestTree>) current_solution_map.clone();
                    }

                    cost = newCost;
                    previousCost = newCost;

                }


            }



            t *= cooling;
            System.out.println("temperature: " + t + " best cost: " + all_time_best_cost + " previous_cost " + previousCost + " average n swapped: " + average_n_swapped / iterPerTemp);

        }

        switch_prop = orig_switch_probability;

        System.out.println("target(s): " );
        for(int i = 0; i < targetSpecies.size(); i++){
            System.out.println(Arrays.toString(targetSpecies.get(i)));
        }
        System.out.println("init(s): ");
        for(int i = 0; i < targetSpecies.size(); i++){
            System.out.println(Arrays.toString(calculateSpeciesSpecificDiameterDistributions(originalPlot_map).get(i)));
        }
        System.out.println("final(s): ");
        for(int i = 0; i < targetSpecies.size(); i++){
            System.out.println(Arrays.toString(calculateSpeciesSpecificDiameterDistributions(best_solution_map).get(i)));
        }

    }

    private double acceptanceProbability(double currentCost, double newCost, double temperature) {
        if (newCost < currentCost) {
            return 1.0;
        }
        return Math.exp((currentCost - newCost) / temperature);
    }

    public void swapStuff(List<forestTree> list1, List<forestTree> list2, double switchProbability){

        HashSet<Integer> alreadySwitched = new HashSet<Integer>();
        int switchIndex = 0;

        for(int i = 0; i < list1.size(); i++){

            if(list1.get(i).hasCrown)
                if(r.nextDouble() < switchProbability){
                    forestTree temp = list1.get(i);

                    while(alreadySwitched.contains(switchIndex = r.nextInt(list2.size()))){
                        System.out.println("Already sampled " + switchIndex);
                    }

                    list1.set(i, list2.get(switchIndex));
                    list2.set(i, temp);
                }
        }
    }

    public int swapStuff(HashMap<Integer, forestTree> plotTrees, HashMap<Integer, forestTree> treeBank, double switchProbability, int swap_this_many){

        //System.out.println( "treeBank.size: " + treeBank.size());
        int total_size = plotTrees.size() + treeBank.size();

        HashSet<Integer> alreadySwitched = new HashSet<Integer>();
        int switchIndex = 0;

        ArrayList<Integer> ind = new ArrayList<Integer>(plotTrees.keySet());

        Collections.shuffle(ind);
        ArrayList<forestTree> all_trees = new ArrayList<forestTree>(treeBank.values());

        ArrayList<Integer> remove_these = new ArrayList<>();
        ArrayList<Integer> add_these = new ArrayList<>();
        int n_switches = 0;
        //for(int i : plotTrees.keySet()){
        for(int i : ind){

            if(plotTrees.get(i).hasCrown){

                //System.out.println(plotTrees.get(i).maxDBH + " " + this.maxValue);


                //if(r.nextDouble() < switchProbability){


                    switchIndex = r.nextInt(all_trees.size());
                    int loops = 0;
                    boolean failed = false;
                    // THIS GOES INFINITE SOMETIMES
                    while(alreadySwitched.contains(switchIndex) || !all_trees.get(switchIndex).hasCrown
                     || all_trees.get(switchIndex).maxDBH > this.maxValue_array[all_trees.get(switchIndex).treeSpecies]){
                        //System.out.println("Already sampled " + switchIndex);
                        switchIndex = r.nextInt(all_trees.size());
                        if(loops++ > 1000){
                            failed = true;
                            break;
                        }
                    }
                if(failed)
                    continue;
                n_switches++;

                //System.out.println("HERE");
                //forestTree temp = plotTrees.get(i);

                remove_these.add(i);
                //list1.remove(i);

                    alreadySwitched.add(switchIndex);



                    forestTree temp2 = all_trees.get(switchIndex);
                    //System.out.println(treeBank.containsKey(all_trees.get(switchIndex).getTreeID_unique()));
                    add_these.add(temp2.getTreeID_unique());
                    //System.out.println(treeBank.containsKey(temp2.getTreeID_unique()));

                }
            if(n_switches >= swap_this_many){
                break;
            }
        }

        int removed1 = 0;
        int removed2 = 0;
        int added1 = 0;
        int added2 = 0;

        for(int i : remove_these){

            forestTree temp = plotTrees.get(i);

            if(!temp.hasCrown){
                throw new toolException("tree does not have crown! " + temp.getTreeID_unique() + " " + temp.getTreeITCid());
            }
            plotTrees.remove(i);
            removed1++;

            treeBank.put(temp.getTreeID_unique(), temp);
            added2++;

            for(int j = 0; j < temp.treesBeneath.size(); j++){

                if(!plotTrees.containsKey(temp.treesBeneath.get(j).getTreeID_unique())){
                    throw new toolException("tree not in plot!");
                }

                //System.out.println("tree beneath remove!");
                plotTrees.remove(temp.treesBeneath.get(j).getTreeID_unique());
                if(temp.treesBeneath.get(j).hasCrown){
                    throw new toolException("tree has a crown when it should not have one!");
                }
                //emove_these.add(temp.treesBeneath.get(j).getTreeID_unique());
                removed1++;
                added2++;


                treeBank.put(temp.treesBeneath.get(j).getTreeID_unique(), temp.treesBeneath.get(j));
                //System.out.println(treeBank.size());
            }
            //
        }

        //System.out.println("-----------------");
        for(int i : add_these){

            //System.out.println(i);
            forestTree temp = treeBank.get(i);

            if(!temp.hasCrown){
                throw new toolException("tree does not have crown! " + temp.getTreeID_unique() + " " + temp.getTreeITCid());
            }

            treeBank.remove(temp.getTreeID_unique());
            removed2++;

            plotTrees.put(temp.getTreeID_unique(), temp);
            added1++;

            for(int j = 0; j < temp.treesBeneath.size(); j++){
                //System.out.println("tree beneath add!");
                //System.out.println("beneath: " + temp.treesBeneath.get(j).beneathCrownId + " " + temp.treesBeneath.get(j).getTreeDBH());

                if(!treeBank.containsKey(temp.treesBeneath.get(j).getTreeID_unique())){
                    throw new toolException("tree not in treebank!");
                }
                treeBank.remove(temp.treesBeneath.get(j).getTreeID_unique());

                if(temp.treesBeneath.get(j).hasCrown){
                    throw new toolException("tree has a crown when it should not have one!");
                }
                //emove_these.add(temp.treesBeneath.get(j).getTreeID_unique());
                removed2++;
                added1++;
                plotTrees.put(temp.treesBeneath.get(j).getTreeID_unique(), temp.treesBeneath.get(j));

            }
            //System.out.println("-----------------");

        }
        /*
        System.out.println("removed from list1: " + removed1);
        System.out.println("removed from list2: " + removed2);
        System.out.println("added to list1: " + added1);
        System.out.println("added to list2: " + added2);
        System.out.println("list1 size: " + plotTrees.size());
        System.out.println("list2 size: " + treeBank.size());
*/
        int size_after = plotTrees.size() + treeBank.size();
        //System.out.println("n_switches: " + n_switches);
        if(false)
        if(size_after != total_size){

            throw new toolException("size after is not equal to size before!");
        }
        return n_switches;
    }

    public boolean accept(double t, double oldCost, double newCost){

        double e = Math.exp(1);
        double random = Math.random();
        double powi = ((oldCost - newCost) / t);
        double a = Math.pow(e, powi);

        //if(t > 0.5)
        //System.out.println("acceptance probability: " + a + " oldCost: " + oldCost + " newCost: " + newCost + " t: " + t);
        return random < a;

    }

    public double[] calculateDiameterDistribution(List<forestTree> plot){

         double[] diameterDistribution = new double[target.length];

        for(int i = 0; i < plot.size(); i++){
            int bin = (int)Math.floor(plot.get(i).getTreeDBH() / bins);

            if(bin >= diameterDistribution.length){
                bin = diameterDistribution.length - 1;
            }

            diameterDistribution[bin]++;
        }
        double sum = 0.0;
        for (double value : diameterDistribution) {
            sum += value;
        }
        for (int i = 0; i < diameterDistribution.length; i++) {
            diameterDistribution[i] = diameterDistribution[i] / sum;
        }

        return diameterDistribution;
    }

    public double[] calculateDiameterDistribution(HashMap<Integer, forestTree> plot){

        double[] diameterDistribution = new double[target.length];

        for(int i : plot.keySet()){
            int bin = (int)Math.floor(plot.get(i).getTreeDBH() / bins);

            if(bin >= diameterDistribution.length){
                bin = diameterDistribution.length - 1;
            }

            diameterDistribution[bin]++;
        }
        double sum = 0.0;
        for (double value : diameterDistribution) {
            sum += value;
        }
        for (int i = 0; i < diameterDistribution.length; i++) {
            diameterDistribution[i] = diameterDistribution[i] / sum;
        }

        return diameterDistribution;
    }

    public static double bhattacharyyaDistance(double[] distribution1, double[] distribution2) {
        if (distribution1.length != distribution2.length) {
            throw new IllegalArgumentException("Distributions must have the same length");
        }
        double sum = 0;
        for (int i = 0; i < distribution1.length; i++) {
            sum += Math.sqrt(distribution1[i] * distribution2[i]);
        }
        return -Math.log(sum);
    }

    public static double hellingerDistance(double[] distribution1, double[] distribution2) {
        if (distribution1.length != distribution2.length) {
            throw new IllegalArgumentException("Distributions must have the same length");
        }
        double sum = 0;
        for (int i = 0; i < distribution1.length; i++) {
            sum += Math.pow(Math.sqrt(distribution1[i]) - Math.sqrt(distribution2[i]), 2);
        }
        return Math.sqrt(sum) / Math.sqrt(2);
    }

    public static double hellingerDistance(int[] histogram1, int[] histogram2) {
        int n = histogram1.length;
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            sum += Math.pow(Math.sqrt(histogram1[i] / (double) sum(histogram1)) -
                    Math.sqrt(histogram2[i] / (double) sum(histogram2)), 2);
        }
        return Math.sqrt(sum) / Math.sqrt(2);
    }

    public static double hellingerDistance_species(ArrayList<int[]> histogram1, ArrayList<int[]> histogram2) {
        int n = histogram1.get(0).length;
        double sum1 = 0.0;
        double sum2 = 0.0;
        double sum3 = 0.0;
        for (int i = 0; i < n; i++) {
            sum1 += Math.pow(Math.sqrt(histogram1.get(0)[i] / (double) sum(histogram1.get(0))) -
                    Math.sqrt(histogram2.get(0)[i] / (double) sum(histogram2.get(0))), 2);
            sum2 += Math.pow(Math.sqrt(histogram1.get(1)[i] / (double) sum(histogram1.get(1))) -
                    Math.sqrt(histogram2.get(1)[i] / (double) sum(histogram2.get(1))), 2);
            sum3 += Math.pow(Math.sqrt(histogram1.get(2)[i] / (double) sum(histogram1.get(2))) -
                    Math.sqrt(histogram2.get(2)[i] / (double) sum(histogram2.get(2))), 2);
        }


        double result1 = Math.sqrt(sum1) / Math.sqrt(2);
        double result2 = Math.sqrt(sum2) / Math.sqrt(2);
        double result3 = Math.sqrt(sum3) / Math.sqrt(2);

        double sum = 0;

        if(!Double.isNaN(result1))
            sum += result1;
        if(!Double.isNaN(result2))
            sum += result2;
        if(!Double.isNaN(result3))
            sum += result3;

        //System.out.println("result1: " + result1 + " result2: " + result2 + " result3: " + result3);
        return (sum) / 3.0;
    }

    private static int sum(int[] histogram) {
        int sum = 0;
        for (int value : histogram) {
            sum += value;
        }
        return sum;
    }

    public int[] calculateDiameterDistribution_(HashMap<Integer, forestTree> plot){

        int[] diameterDistribution = new int[target.length];

        for(int i : plot.keySet()){
            int bin = (int)Math.floor(plot.get(i).getTreeDBH() / bins);

            if(bin >= diameterDistribution.length){
                bin = diameterDistribution.length - 1;
            }

            diameterDistribution[bin]++;
        }

        return diameterDistribution;

    }

    public ArrayList<int[]> calculateSpeciesSpecificDiameterDistributions(HashMap<Integer, forestTree> plot){

        int[] diameterDistribution_pine = new int[target.length];
        int[] diameterDistribution_spruce = new int[target.length];
        int[] diameterDistribution_decid = new int[target.length];

        for(int i : plot.keySet()){
            int bin = (int)Math.floor(plot.get(i).getTreeDBH() / bins);


            if(bin >= diameterDistribution_pine.length){
                bin = diameterDistribution_pine.length - 1;
            }

            if(plot.get(i).treeSpecies == 0)
                diameterDistribution_pine[bin]++;
            else if(plot.get(i).treeSpecies == 1)
                diameterDistribution_spruce[bin]++;
            else if(plot.get(i).treeSpecies == 2)
                diameterDistribution_decid[bin]++;
            else
                throw new toolException("species not recognized!");
        }

        ArrayList<int[]> speciesSpecificDiameterDistributions = new ArrayList<>();
        speciesSpecificDiameterDistributions.add(diameterDistribution_pine);
        speciesSpecificDiameterDistributions.add(diameterDistribution_spruce);
        speciesSpecificDiameterDistributions.add(diameterDistribution_decid);

        return speciesSpecificDiameterDistributions;

    }

    public double costWithEMD(ArrayList<int[]> histogram1, ArrayList<int[]> histogram2){

        double costSum = 0;

        ArrayList<Double> emds = new ArrayList<>();
        ArrayList<Double> maxEMDs = new ArrayList<>();
        ArrayList<double[]> d1 = new ArrayList<>();
        ArrayList<double[]> d2 = new ArrayList<>();


        for(int i = 0; i < histogram1.size(); i++){
            double emd = earthMoverDistance(histogram1.get(i), histogram2.get(i));
            double[] doubles1 = Arrays.stream(histogram1.get(i)).asDoubleStream().toArray();
            double[] doubles2 = Arrays.stream(histogram2.get(i)).asDoubleStream().toArray();
            addOneIfZero(doubles1, 0);
            addOneIfZero(doubles2, 1);
            d1.add(doubles1);
            d2.add(doubles2);

            emd = new EarthMoversDistance().compute(doubles1, doubles2);

            if(Double.isNaN(emd))
                throw new toolException("emd is NaN!");

            //System.out.println("emd: " + emd + " apache_emd: " + apache_emd);
            double maxEMD = 0.0;
            for (int h = 0; h < histogram1.get(i).length; h++) {
                maxEMD += Math.abs(histogram1.get(i)[h] - histogram2.get(i)[h]);
            }
            //System.out.println("maxEMD: " + maxEMD);
            if(maxEMD != 0.0)
                costSum += emd / maxEMD;
            else
                costSum += 0.0;

            emds.add(emd);
            maxEMDs.add(maxEMD);

        }

        if(Double.isNaN(costSum)) {
            System.out.println(Arrays.toString(emds.toArray()));
            System.out.println(Arrays.toString(maxEMDs.toArray()));
            for(int i = 0; i < 3 ; i++) {

                System.out.println(Arrays.toString(d1.get(i)));
                System.out.println(Arrays.toString(d2.get(i)));
            }
            throw new toolException("costsum is NaN!");
        }

        if(Double.isInfinite(costSum)) {
            System.out.println(Arrays.toString(emds.toArray()));
            System.out.println(Arrays.toString(maxEMDs.toArray()));
            for(int i = 0; i < 3 ; i++) {

                System.out.println(Arrays.toString(d1.get(i)));
                System.out.println(Arrays.toString(d2.get(i)));
            }
            throw new toolException("costsum is infinite!");
        }


        return costSum / (double)histogram1.size();
    }

    public static double earthMoversDistance(int[] histogram1, int[] histogram2) {

        int n = histogram1.length;
        int m = histogram2.length;
        double[] cdf1 = cumulativeDistributionFunction(histogram1);
        double[] cdf2 = cumulativeDistributionFunction(histogram2);
        double emd = 0.0;
        int i = 0, j = 0;
        while (i < n && j < m) {
            if (cdf1[i] < cdf2[j]) {
                emd += (cdf2[j] - cdf1[i]) * histogram1[i];
                i++;
            } else {
                emd += (cdf1[i] - cdf2[j]) * histogram2[j];
                j++;
            }
        }
        while (i < n) {
            emd += cdf1[n - 1] * histogram1[i];
            i++;
        }
        while (j < m) {
            emd += cdf2[m - 1] * histogram2[j];
            j++;
        }

        double maxEMD = 0.0;
        for (int h = 0; h < histogram1.length; h++) {
            maxEMD += Math.abs(histogram1[h] - histogram2[h]);
        }
        return emd / maxEMD;

    }

    public double earthMoverDistance(int[] distribution1, int[] distribution2) {
        if (distribution1.length != distribution2.length) {
            throw new IllegalArgumentException("Distributions must have the same length");
        }

        int n = distribution1.length;
        double[] cumulant1 = new double[n + 1];
        double[] cumulant2 = new double[n + 1];
        for (int i = 0; i < n; i++) {
            cumulant1[i + 1] = cumulant1[i] + distribution1[i];
            cumulant2[i + 1] = cumulant2[i] + distribution2[i];
        }
        double emd = 0.0;
        for (int i = 0; i < n; i++) {
            double d1 = cumulant1[i + 1] - cumulant1[i];
            double d2 = cumulant2[n] - cumulant2[i];
            emd += Math.abs(d1 - d2);
        }
        return emd;
    }

    public void addOneIfZero(double[] array, int index){

        double sum = 0;
        for(double d : array){
            sum += d;
        }

        if(sum == 0.0)
            array[index] += 1.0;

    }

    private static double[] cumulativeDistributionFunction(int[] histogram) {
        int n = histogram.length;
        double[] cdf = new double[n];
        cdf[0] = histogram[0];
        for (int i = 1; i < n; i++) {
            cdf[i] = cdf[i - 1] + histogram[i];
        }
        return cdf;
    }




}
