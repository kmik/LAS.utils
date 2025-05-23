package utils;

import err.toolException;
import org.apache.commons.math3.ml.distance.EarthMoversDistance;
import tools.plotAugmentator;

import java.io.*;
import java.util.*;

public class plotSimulator {

    ArrayList<simulationInformation> simulationRaport = new ArrayList<>();

    public plotAugmentator augmentator;
    public plotSimulator(plotAugmentator augmentator){

        this.augmentator = augmentator;

    }

    public void addOneIfZero(double[] array, int index){

        double sum = 0;
        for(double d : array){
            sum += d;
        }

        if(sum == 0.0)
            array[index] += 1.0;

    }

    public synchronized void addReport(simulationInformation info){

        simulationRaport.add(info);

    }

    public double costWithEMDSINGLE(ArrayList<int[]> histogram1, ArrayList<int[]> histogram2){

        double costSum = 0;

        int[] hist1 = new int[histogram1.get(0).length * histogram1.size()];
        int[] hist2 = new int[histogram1.get(0).length * histogram1.size()];

        int counter = 0;

        for(int i = 0; i < histogram1.size(); i++){

            for(int j = 0; j < histogram1.get(i).length; j++){
                hist1[counter] = histogram1.get(i)[j];
                hist2[counter] = histogram2.get(i)[j];
                counter++;
            }

        }
        ArrayList<Double> emds = new ArrayList<>();
        ArrayList<Double> maxEMDs = new ArrayList<>();
        ArrayList<double[]> d1 = new ArrayList<>();
        ArrayList<double[]> d2 = new ArrayList<>();


        //double emd = earthMoverDistance(histogram1.get(i), histogram2.get(i));
        double[] doubles1 = Arrays.stream(hist1).asDoubleStream().toArray();
        double[] doubles2 = Arrays.stream(hist2).asDoubleStream().toArray();
        addOneIfZero(doubles1, 0);
        addOneIfZero(doubles2, 1);
        d1.add(doubles1);
        d2.add(doubles2);

        double emd = new EarthMoversDistance().compute(doubles1, doubles2);

        if(Double.isNaN(emd))
            throw new toolException("emd is NaN!");

        //System.out.println("emd: " + emd + " apache_emd: " + apache_emd);
        double maxEMD = 0.0;
        for (int h = 0; h < hist1.length; h++) {
            maxEMD += Math.abs(hist1[h] - hist2[h]);
        }
        //System.out.println("maxEMD: " + maxEMD);
        if(maxEMD != 0.0)
            costSum += emd / maxEMD;
        else
            costSum += 0.0;

        emds.add(emd);
        maxEMDs.add(maxEMD);



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


        return costSum ;
    }

    public double costWithEMDSINGLE(int[] hist1, int[] hist2){

        double costSum = 0;

        ArrayList<Double> emds = new ArrayList<>();
        ArrayList<Double> maxEMDs = new ArrayList<>();
        ArrayList<double[]> d1 = new ArrayList<>();
        ArrayList<double[]> d2 = new ArrayList<>();


        //double emd = earthMoverDistance(histogram1.get(i), histogram2.get(i));
        double[] doubles1 = Arrays.stream(hist1).asDoubleStream().toArray();
        double[] doubles2 = Arrays.stream(hist2).asDoubleStream().toArray();
        addOneIfZero(doubles1, 0);
        addOneIfZero(doubles2, 1);
        d1.add(doubles1);
        d2.add(doubles2);

        double emd = new EarthMoversDistance().compute(doubles1, doubles2);

        if(Double.isNaN(emd))
            throw new toolException("emd is NaN!");

        //System.out.println("emd: " + emd + " apache_emd: " + apache_emd);
        double maxEMD = 0.0;
        for (int h = 0; h < hist1.length; h++) {
            maxEMD += Math.abs(hist1[h] - hist2[h]);
        }
        //System.out.println("maxEMD: " + maxEMD);
        if(maxEMD != 0.0)
            costSum += emd / maxEMD;
        else
            costSum += 0.0;

        emds.add(emd);
        maxEMDs.add(maxEMD);



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


        return costSum ;
    }

    public int findCandidatePlot(forestPlot in, double bin, int[] target){

        ArrayList<int[]> targetti = calculateSpeciesSpecificDiameterDistributions(in.trees, bin, target);

        double minDistance = Double.MAX_VALUE;
        int chosenPlot = -1;
        for(int i : augmentator.plots.keySet()){

            double distance = costWithEMDSINGLE(targetti, calculateSpeciesSpecificDiameterDistributions(augmentator.plots.get(i).trees, bin, target));
            if(distance < minDistance){
                minDistance = distance;
                chosenPlot = i;
            }
        }
        /*
        int treesOver10cm = 0;
        for(int i : in.trees.keySet()){

            if(in.trees.get(i).treeDBH >= 15.0){
                treesOver10cm++;
            }

        }



        int closest_ = 100000;
        int closest_plot = -1;


        for(int i : augmentator.plots.keySet()){

            if(Math.abs(augmentator.plots.get(i).trees.size() - treesOver10cm) < closest_){
                closest_ = Math.abs(augmentator.plots.get(i).treesOverDBH(15.0) - treesOver10cm);
                closest_plot = i;
            }

        }

*/
        return chosenPlot;
    }

    public static double calculateRelativeDifference(double x, double y) {
        double max = Math.max(Math.abs(x), Math.abs(y));
        return max == 0.0 ? 0.0 : Math.abs(x - y) / max;
    }

    public static double relativeRMSE(double[] actual, double[] predicted) {

        double sumSquares = 0;
        double sumSquaresPredicted = 0;

        for (int i = 0; i < actual.length; i++) {
            sumSquares += Math.pow(actual[i], 2);
            sumSquaresPredicted += Math.pow(predicted[i], 2);
        }

        double mse = 0;
        for (int i = 0; i < actual.length; i++) {
            double error = actual[i] - predicted[i];
            mse += Math.pow(error, 2);
        }

        double rmse = Math.sqrt(mse / actual.length);
        double maxVal = Math.sqrt(sumSquares / actual.length);

        return rmse / maxVal;
    }

    public TreeMap<Double, Integer> findCandidatePlots(forestPlot in, double bin, int[] target){

        TreeMap<Double, Integer> output = new TreeMap<>();

        ArrayList<int[]> targetti = calculateSpeciesSpecificDiameterDistributions(in.trees, bin, target);

        int[] diameterDistr = diameterDistribution(in.trees, bin, target);

        double minDistance = Double.MAX_VALUE;
        int chosenPlot = -1;

        double train_area = Math.PI * Math.pow(9, 2);
        //System.out.println("Target volume: " + sumOfArray(calculateSpeciesSpecificVolumes(in.trees, bin, target, train_area)));
        //System.out.println(Arrays.toString(calculateSpeciesSpecificVolumes(in.trees, bin, target, in.area)));



        for(int i : augmentator.plots.keySet()){

            //double distance = costWithEMDSINGLE(targetti, calculateSpeciesSpecificDiameterDistributions(augmentator.plots.get(i).trees, bin, target));
            double distance = costWithEMDSINGLE(diameterDistr, diameterDistribution(augmentator.plots.get(i).trees, bin, target));

            double stemCountPerHectare = augmentator.plots.get(i).trees.size() / (augmentator.plots.get(i).area / 10000.0);
            double targetStemCountPerHectare = in.trees.size() / (train_area / 10000.0);

            double relDif = calculateRelativeDifference(sumOfArray(calculateSpeciesSpecificVolumes(augmentator.plots.get(i).trees, bin, target, augmentator.plots.get(i).area)), sumOfArray(calculateSpeciesSpecificVolumes(in.trees, bin, target, train_area)));

            //relDif = relativeRMSE(calculateSpeciesSpecificVolumes(augmentator.plots.get(i).trees, bin, target, augmentator.plots.get(i).area), calculateSpeciesSpecificVolumes(in.trees, bin, target, train_area));
            //double relDifStemCount = calculateRelativeDifference(stemCountPerHectare, targetStemCountPerHectare);
            //System.out.println(relDifStemCount);
            //System.out.println("Plot " + i + " volume: " + sumOfArray(calculateSpeciesSpecificVolumes(augmentator.plots.get(i).trees, bin, target, augmentator.plots.get(i).area)));
            output.put(relDif, i);

            //System.out.println(relDif);
        }
        //System.exit(1);
        return output;
    }

    public TreeMap<Double, Integer> findCandidatePlots(double[] targetVolumes, double bin, int[] target){

        TreeMap<Double, Integer> output = new TreeMap<>();

        //ArrayList<int[]> targetti = calculateSpeciesSpecificDiameterDistributions(in.trees, bin, target);

        //int[] diameterDistr = diameterDistribution(in.trees, bin, target);

        double minDistance = Double.MAX_VALUE;
        int chosenPlot = -1;

        double train_area = Math.PI * Math.pow(9, 2);
        //System.out.println("Target volume: " + sumOfArray(calculateSpeciesSpecificVolumes(in.trees, bin, target, train_area)));
        //System.out.println(Arrays.toString(calculateSpeciesSpecificVolumes(in.trees, bin, target, in.area)));



        for(int i : augmentator.plots.keySet()){

            //double distance = costWithEMDSINGLE(targetti, calculateSpeciesSpecificDiameterDistributions(augmentator.plots.get(i).trees, bin, target));
            //double distance = costWithEMDSINGLE(diameterDistr, diameterDistribution(augmentator.plots.get(i).trees, bin, target));

            //double stemCountPerHectare = augmentator.plots.get(i).trees.size() / (augmentator.plots.get(i).area / 10000.0);
            //double targetStemCountPerHectare = in.trees.size() / (train_area / 10000.0);

            double relDif = calculateRelativeDifference(sumOfArray(calculateSpeciesSpecificVolumes(augmentator.plots.get(i).trees, bin, target, augmentator.plots.get(i).area)), sumOfArray(targetVolumes));

            //relDif = relativeRMSE(calculateSpeciesSpecificVolumes(augmentator.plots.get(i).trees, bin, target, augmentator.plots.get(i).area), calculateSpeciesSpecificVolumes(in.trees, bin, target, train_area));
            //double relDifStemCount = calculateRelativeDifference(stemCountPerHectare, targetStemCountPerHectare);
            //System.out.println(relDifStemCount);
            //System.out.println("Plot " + i + " volume: " + sumOfArray(calculateSpeciesSpecificVolumes(augmentator.plots.get(i).trees, bin, target, augmentator.plots.get(i).area)));
            output.put(relDif, i);

            //System.out.println(relDif);
        }
        //System.exit(1);
        return output;
    }

    public static double calculateStandardDeviation(HashMap<Integer, forestTree> arr) {
        int n = arr.size();
        double sum = 0.0;
        double mean = 0.0;
        double deviation = 0.0;
        n = 0;

        // Calculate the sum of the elements in the array
        for (int i  : arr.keySet()) {
            if(arr.get(i).hasCrown) {
                n++;
                sum += arr.get(i).getTreeHeight_ITC();
            }
        }

        // Calculate the mean value of the array
        mean = sum / n;

        // Calculate the sum of the squares of the differences between each element and the mean
        for (int i  : arr.keySet()) {
            if(arr.get(i).hasCrown)
                deviation += Math.pow((arr.get(i).getTreeHeight_ITC() - mean), 2);
        }

        // Divide the sum of the squares of the differences by the length of the array minus one,
        // and then take the square root to get the standard deviation

        double standardDeviation = Math.sqrt(deviation / (n - 1));

        // Calculate the relative standard deviation as a percentage
        double relativeStandardDeviation = (standardDeviation / mean);

        return relativeStandardDeviation;
    }


    public void normalizeBetween(int[] arr, int min, int max){
        int minVal = Integer.MAX_VALUE;
        int maxVal = Integer.MIN_VALUE;
        for(int i = 0; i < arr.length; i++){
            if(arr[i] < minVal){
                minVal = arr[i];
            }
            if(arr[i] > maxVal){
                maxVal = arr[i];
            }
        }
        for(int i = 0; i < arr.length; i++){
            arr[i] = (int) (((double) (arr[i] - minVal) / (double) (maxVal - minVal)) * (double) (max - min) + min);
        }
    }


    public void simulatePlotAnnealingWay2(int simulationID, boolean fill, boolean original, List<double[]> targets, String odir){

        simulationInformation sInfo = new simulationInformation(simulationID);
        Random r = new Random();

        //r.setSeed(1234 + simulationID);

        List<Integer> keysAsArray = new ArrayList<Integer>(augmentator.plots.keySet());

        Collections.shuffle(keysAsArray, r);

        HashMap<Integer, forestTree> before = (HashMap<Integer, forestTree>) augmentator.trees.clone();
        double sumSDs = 0.0;
/*

        for(int i : augmentator.plots.keySet()){

            System.out.println("sd " + calculateStandardDeviation(augmentator.plots.get(i).trees));
            sumSDs += calculateStandardDeviation(augmentator.plots.get(i).trees);

        }

        System.out.println("Average SD: " + sumSDs / augmentator.plots.size());
        System.exit(1);
*/
        //forestPlot plot_target = augmentator.targets.get(targets.get(simulationID));
        //forestPlot plot_target = augmentator.targets.get(42003);

        //double dbh_max = 0.0;
        //double dbh_max_pine = 0.0;
        //double dbh_max_spruce = 0.0;
        //double dbh_max_decid = 0.0;
/*
        for(forestTree tree : plot_target.trees.values()){

            if(tree.treeSpecies == 0 && tree.getTreeDBH() > dbh_max_pine){
                dbh_max_pine = tree.getTreeDBH();
            }
            if(tree.treeSpecies == 1 && tree.getTreeDBH() > dbh_max_spruce){
                dbh_max_spruce = tree.getTreeDBH();
            }
            if(tree.treeSpecies == 2 && tree.getTreeDBH() > dbh_max_decid){
                dbh_max_decid = tree.getTreeDBH();
            }

            if(tree.getTreeDBH() > dbh_max){
                dbh_max = tree.getTreeDBH();
            }
        }
*/
        double bin = 2.0;
        //double max = dbh_max;
        //int[] initialSolution = new int[ (int) (max / bin) ];
        int[] target = new int[ (int) (20 / bin) ];


        //System.out.println("TARGET PLOT IS: " + plot_target.id + " and it has " + plot_target.trees.size() + " trees");

        //int candidatePlot = findCandidatePlot(plot_target, bin, target);

        double train_area = Math.PI * Math.pow(9, 2);

        //HashMap<Integer, forestTree> plotTrees_map_target = plot_target.trees_unique_id;

        double[] target_species_volumes = targets.get(simulationID);

        //target = diameterDistribution(plotTrees_map_target, bin, target);

        //System.out.println("before: " + Arrays.toString(target));
        //normalizeBetween(target, 0, 100);
        //System.out.println("after: " + Arrays.toString(target));
        //System.exit(1);


        TreeMap<Double, Integer> candidatePlots = findCandidatePlots(targets.get(simulationID), bin, target);


        //System.out.println("Candidate PLOT IS: " + candidatePlot + " and it has " + augmentator.plots.get(candidatePlot).trees.size() + " trees");

        //System.exit(1);

        //forestPlot plot = augmentator.plots.get(keysAsArray.get(r.nextInt(keysAsArray.size())));

        //augmentator.plots.get(candidatePlot).lockPlot();

        //candidatePlot = 56;
        List<Double> keysAsArray_ = new ArrayList<Double>(candidatePlots.keySet());

        int candidatePlot = candidatePlots.get(keysAsArray_.get(0));
        // Instead just get one randomly
        candidatePlot = candidatePlots.get(keysAsArray_.get(r.nextInt(keysAsArray_.size())));
        //candidatePlot = candidatePlots.get(keysAsArray_.get(keysAsArray_.size() - 1));

        //System.out.println(keysAsArray_);
        //System.out.println("----------------------------------");
        //System.out.println((keysAsArray_.get(0)));

        forestPlot plot = augmentator.plots.get(candidatePlot);

        //System.out.println("INITIAL PLOT IS: " + plot.id + " " + sumOfArray(calculateSpeciesSpecificVolumes(plot.trees, bin, target, plot.area)));
        //System.out.println("TARGET PLOT IS: " + plot_target.id + " " + sumOfArray(target_species_volumes));
        //System.exit(1);
        //plot = augmentator.plots.get(8);

        //forestPlot plot_target = augmentator.plots.get(keysAsArray.get(r.nextInt(keysAsArray.size())));

        //plot_target = augmentator.plots.get(127);

        //System.out.println("TARGET PLOT IS: " + plot_target.id );
        //System.out.println("FROM PLOT IS: " + plot.id );

        //if(plot_target.id == plot.id){
        //    return;
        //}

        ArrayList<forestTree> plotTrees = new ArrayList<forestTree>(plot.trees.values());

        HashMap<Integer, forestTree> plotTrees_map = plot.trees_unique_id_for_simulation;



        ArrayList<Integer> removeThese = new ArrayList<Integer>();


        //int[] target_pine = new int[ (int) (dbh_max_pine / bin) ];
        //int[] target_spruce = new int[ (int) (dbh_max_spruce / bin) ];
        //int[] target_decid = new int[ (int) (dbh_max_decid / bin) ];


        //dbh_max_decid += bin * 2;
        //dbh_max_spruce += bin * 2;
        //dbh_max_pine += bin * 2;


        //ArrayList<int[]> target_species = this.calculateSpeciesSpecificDiameterDistributions(plotTrees_map_target, bin, target);
        //ArrayList<int[]> target_species_height = this.calculateSpeciesSpecificDiameterDistributions(plotTrees_map_target, bin, target);


        //double target_species_dgm_pine =calculateDGM(plotTrees_map_target, 0);
        //double target_species_dgm_spruce =calculateDGM(plotTrees_map_target, 1);
        //double target_species_dgm_decid =calculateDGM(plotTrees_map_target, 2);

        //double[] target_species_dgm = new double[]{target_species_dgm_pine, target_species_dgm_spruce, target_species_dgm_decid};


        //initialSolution = this.calculateDiameterDistribution(plotTrees_map, bin, initialSolution);



        //System.out.println(Arrays.toString(target));

        simulatedAnnealingForestSimulator sa = new simulatedAnnealingForestSimulator(augmentator, simulationID);

        sa.setCandidatePlots(candidatePlots);

        sa.setAllPlots(augmentator.plots);
        //sa.setTarget(target, bin, max);
        //sa.setTarget(target, bin, new double[]{dbh_max_pine, dbh_max_spruce, dbh_max_decid});
        //sa.setTargetSpeciesSpecific(target_species, bin);
        //sa.setTargetSpeciesSpecificHeight(target_species_height, bin);

        sa.setTargetVolume(target_species_volumes);
        //sa.setTargetDGM(target_species_dgm);

        List<forestTree> trees_all = new ArrayList<forestTree>(augmentator.itc_with_tree.values());
        //HashMap<Integer, forestTree> trees_all_map = augmentator.itc_with_tree_unique_id;
        HashMap<Integer, forestTree> trees_all_map = (HashMap<Integer, forestTree>)augmentator.trees.clone();

/*
        for(int i : trees_all_map.keySet()){
            if(trees_all_map.get(i).getPlotID() == plot.id || trees_all_map.get(i).getPlotID() == plot_target.id){
                removeThese.add(i);
            }
        }

        for(int i : removeThese){
            trees_all_map.remove(i);
        }
*/
        sa.initialSolution(plotTrees_map, plot.id);
        sa.setSearchSpace(trees_all_map);

/*
        for(int i : plotTrees_map.keySet()){
            System.out.println(plotTrees_map.get(i).getTreeITCid() + " " + plotTrees_map.get(i).getTreeDBH());
        }

 */


        sa.optimize();

        //System.out.println(sa.startId + " " + sa.bestSolutionPlotId);
        plot = augmentator.plots.get(sa.bestSolutionPlotId);


        try {
            while (plot.isLocked()){
                Thread.sleep(1000);
                System.out.println("plot " + plot.id + " is locked for thread " +  Thread.currentThread().getId());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        plot.lockPlot();
        System.out.println("Locked plot " + plot.id + " for thread " +  Thread.currentThread().getId());

        plot.targetVolume = target_species_volumes;

        plot.prepareSimulationOffsets();



/*
        System.out.println("----------------");
        for(int i : sa.best_solution_map.keySet()){
            System.out.println(sa.best_solution_map.get(i).getTreeITCid() + " " + sa.best_solution_map.get(i).getTreeDBH());
        }


 */
        int count_original = 0;
        int count_new = 0;
        int count_unchanged = 0;

        HashSet<Integer> ITC_segments_original = new HashSet<>();
        HashSet<Integer> ITC_segments_optimized = new HashSet<>();
        HashSet<Integer> ITC_segments_optimized_all_trees = new HashSet<>();
        HashSet<Integer> keep = new HashSet<>();
        int count1 = 0, count2 = 0;

        //System.exit(1);

        count1 = 0;
        count2 = 0;


        for(int i : plot.trees_unique_id_for_simulation.keySet()){

            if(plot.trees_unique_id_for_simulation.get(i).hasCrown) {
                //System.out.println(plot.trees_unique_id_for_simulation.get(i).getTreeITCid() + " " + ITC_segments_original.contains(plot.trees_unique_id_for_simulation.get(i).getTreeITCid()));

                ITC_segments_original.add(plot.trees_unique_id_for_simulation.get(i).getTreeITCid());
                count1++;
            }

        }

        System.out.println("ITC_segments_original.size() " + ITC_segments_original.size() + " " + count1);

        int counter2 = 0;

        for(int i : sa.best_solution_map.keySet()){

            if(plot.trees_unique_id_for_simulation.containsKey(i)){
                counter2++;
            }
            if(sa.best_solution_map.get(i).hasCrown) {
                count2++;
                if(false)
                    if( ITC_segments_original.contains(i) ) {
                        ITC_segments_original.remove(i);
                        //keep.add(i);
                    }else{
                        ITC_segments_optimized.add(sa.best_solution_map.get(i).getTreeITCid());
                    }

                ITC_segments_optimized_all_trees.add(sa.best_solution_map.get(i).getTreeITCid());
            }

        }

        //System.out.println("ITC_segments_optimized.size() " + ITC_segments_optimized_all_trees.size() + " " + count2);

        if(false)
            if(counter2 == plot.trees_unique_id_for_simulation.size()){
                System.out.println("Solution is identical to the original!");
                plot.unlock();
                return;
            }


        plot.simulated_plot = sa.best_solution_map;

        if(count1 != count2){
            plot.unlock();
            //System.out.println("simulation: " + simulationID + " from " + plot.id + " plot to " + plot_target.id +  " plot. " + " same as in orig: " + counter2);
            throw new toolException("Optimized and original have different number of ITC segments   ! " + count1 + " " + count2 + " " + " same as in orig: " + counter2);

        }

        ArrayList<Integer> ITC_segments_original_array
                = new ArrayList<>(ITC_segments_original);
        ArrayList<Integer> ITC_segments_optimized_array
                = new ArrayList<>(ITC_segments_optimized_all_trees);

        if(ITC_segments_original.size() != ITC_segments_optimized_all_trees.size()){
            System.out.println(count1 + " " + count2);
            plot.unlock();
            throw new toolException("ITC segments are not equal! " + ITC_segments_original.size() + " " + ITC_segments_optimized_all_trees.size());
        }

        HashSet<Integer> remove = new HashSet<>();
        TreeMap<Integer, Integer> add = new TreeMap<>();
        TreeSet<Integer> plots_ = new TreeSet<>();
        int counter = 0;

        ArrayList<Double> optim = new ArrayList<>();
        ArrayList<Double> orig = new ArrayList<>();

        for(int i : ITC_segments_optimized_array){
            optim.add(augmentator.itc_with_tree.get(i).getTreeCrownArea());
        }

        for(int i : ITC_segments_original_array){
            orig.add(augmentator.itc_with_tree.get(i).getTreeCrownArea());
        }

        //System.out.println(optim.size() + " " + orig.size());
        Map<Integer, Integer> res = matchClosest(optim, orig);

        //System.out.println(Arrays.toString(res.toArray()));
        //System.exit(1);

        for(int i = 0; i <  ITC_segments_original_array.size(); i++){

            remove.add(ITC_segments_original_array.get(i));
            //add.put(ITC_segments_optimized_array.get(i), ITC_segments_original_array.get(i));
            add.put(ITC_segments_optimized_array.get(res.get(i)), ITC_segments_original_array.get(i));

            plots_.add(augmentator.itc_with_tree.get(ITC_segments_optimized_array.get(i)).getPlotID());

        }
/*

        sInfo.setTarget(  new double[]{sumOfArray(sa.targetVolume)});

        sInfo.setSimulated( new double[]{sumOfArray(this.calculateSpeciesSpecificVolumes(sa.best_solution_map, bin, target, plot.getArea()))});
        sInfo.setFromFile(plot.getPlotLASFile().getName());
        sInfo.setInitial( new double[]{sumOfArray(this.calculateSpeciesSpecificVolumes(plot.trees_unique_id_for_simulation, bin, target, plot.getArea()))});
*/

        sInfo.setTarget(Arrays.copyOf(sa.targetVolume, sa.targetVolume.length));
        System.out.println(Arrays.toString(sa.targetVolume));
        //System.exit(1);
        double[] tmp = this.calculateSpeciesSpecificVolumes(sa.best_solution_map, bin, target, plot.getArea());
        sInfo.setSimulated(Arrays.copyOf(tmp, tmp.length));
        double[] tmp2 = this.calculateSpeciesSpecificVolumes(plot.trees_unique_id_for_simulation, bin, target, plot.getArea());
        sInfo.setInitial(Arrays.copyOf(tmp2, tmp2.length));
        sInfo.setFromFile(plot.getPlotLASFile().getName());

        //sInfo.setSimulated( new double[]{sumOfArray(this.calculateSpeciesSpecificVolumes(sa.best_solution_map, bin, target, plot.getArea()))});
        //sInfo.setInitial( new double[]{sumOfArray(this.calculateSpeciesSpecificVolumes(plot.trees_unique_id_for_simulation, bin, target, plot.getArea()))});



        this.addReport(sInfo);

        //System.out.println("from plot: " + plot.getPlotID());

        //for(int i : plots_)
        //    System.out.println("from plot: " + i);
        //System.out.println("remove.size() " + remove.size() + " " + keep.size());
        //System.exit(1);
        if(fill)
            plot.createSimulationFile("filled_simulation_" + simulationID, odir);
        else if(original)
            plot.createSimulationFile("original_" + simulationID, odir);
        else if(!fill)
            plot.createSimulationFile("holey_simulation_" + simulationID, odir);


        try {
            plot.writeSimulationFile(this, remove, add, plots_, fill, original, simulationID, keep);
        }catch (Exception e) {
            plot.unlock();
            e.printStackTrace();
        }
/*
        try {
            plot.writeSimulationFile(this, remove, add, plots_, fill, original, simulationID);
        }catch (Exception e) {
            e.printStackTrace();
        }

 */
        /*
        Object[] orig_keys = plot.trees_unique_id.keySet().toArray();
        Object[] opti_keys = sa.best_solution_map.keySet().toArray();

        List<Integer> orig_keys_ = new ArrayList<Integer>(plot.trees_unique_id.keySet());
        List<Integer> opti_keys_ = new ArrayList<Integer>(sa.best_solution_map.keySet());

        System.out.println(Arrays.toString(orig_keys));


        HashSet<Integer> remove_2 = new HashSet<>();
        HashSet<Integer> unchanged = new HashSet<>();

        for(int i : sa.best_solution_map.keySet()){

            if(plot.trees_unique_id.keySet().contains(i)) {

                if(plot.trees_unique_id.get(i).hasCrown) {
                    remove_2.add(i);
                    count_unchanged++;
                    unchanged.add(sa.best_solution_map.get(i).getTreeID_unique());
                }
            }else {
                remove.add(augmentator.trees.get(i).getTreeITCid());

            }

            if(sa.best_solution_map.get(i).hasCrown)
                count_new++;

        }

        for(int i : remove_2)
            sa.best_solution_map.remove(i);

        opti_keys_ = new ArrayList<Integer>(sa.best_solution_map.keySet());
        Collections.shuffle(opti_keys_);
*/

        //System.out.println("Original: " + count_original + " New: " + count_new + " Unchanged: " + count_unchanged);
        //System.out.println("DONE!");
        simulateFieldPlot(9.0, plot);
        plot.unlock();
        System.out.println("Unlocked plot " + plot.id);
        //System.exit(1);

    }

    public void simulateFieldPlot(double radius, forestPlot plot){

        File trees = plot.simulationPlotFile;

        // Read the trees from the file line by line
        try (BufferedReader br = new BufferedReader(new FileReader(trees))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                if(parts.length < 3)
                    continue;
                double x = Double.parseDouble(parts[3]);
                double y = Double.parseDouble(parts[4]);
                int treeSpecies = Integer.parseInt(parts[8]);
                double volume = Double.parseDouble(parts[parts.length - 5]);

                forestTree tree = new forestTree();
                tree.setTreeX(x);
                tree.setTreeY(y);
                tree.setTreeVolume(volume);
                tree.setTreeSpecies(treeSpecies);
                //System.out.println(tree.toString());

                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //System.exit(1);


    }
    public void simulatePlotAnnealingWay(int simulationID, boolean fill, boolean original, List<Integer> targets, String odir){

        simulationInformation sInfo = new simulationInformation(simulationID);
        Random r = new Random();

        //r.setSeed(1234 + simulationID);

        List<Integer> keysAsArray = new ArrayList<Integer>(augmentator.plots.keySet());

        Collections.shuffle(keysAsArray, r);

        HashMap<Integer, forestTree> before = (HashMap<Integer, forestTree>) augmentator.trees.clone();
        double sumSDs = 0.0;
/*

        for(int i : augmentator.plots.keySet()){

            System.out.println("sd " + calculateStandardDeviation(augmentator.plots.get(i).trees));
            sumSDs += calculateStandardDeviation(augmentator.plots.get(i).trees);

        }

        System.out.println("Average SD: " + sumSDs / augmentator.plots.size());
        System.exit(1);
*/
        forestPlot plot_target = augmentator.targets.get(targets.get(simulationID));
        //forestPlot plot_target = augmentator.targets.get(42003);

        double dbh_max = 0.0;
        double dbh_max_pine = 0.0;
        double dbh_max_spruce = 0.0;
        double dbh_max_decid = 0.0;

        for(forestTree tree : plot_target.trees.values()){

            if(tree.treeSpecies == 0 && tree.getTreeDBH() > dbh_max_pine){
                dbh_max_pine = tree.getTreeDBH();
            }
            if(tree.treeSpecies == 1 && tree.getTreeDBH() > dbh_max_spruce){
                dbh_max_spruce = tree.getTreeDBH();
            }
            if(tree.treeSpecies == 2 && tree.getTreeDBH() > dbh_max_decid){
                dbh_max_decid = tree.getTreeDBH();
            }

            if(tree.getTreeDBH() > dbh_max){
                dbh_max = tree.getTreeDBH();
            }
        }

        double bin = 2.0;
        double max = dbh_max;
        int[] initialSolution = new int[ (int) (max / bin) ];
        int[] target = new int[ (int) (max / bin) ];


        System.out.println("TARGET PLOT IS: " + plot_target.id + " and it has " + plot_target.trees.size() + " trees");

        //int candidatePlot = findCandidatePlot(plot_target, bin, target);

        double train_area = Math.PI * Math.pow(9, 2);

        HashMap<Integer, forestTree> plotTrees_map_target = plot_target.trees_unique_id;
        double[] target_species_volumes = this.calculateSpeciesSpecificVolumes(plotTrees_map_target, bin, target, train_area);

        target = diameterDistribution(plotTrees_map_target, bin, target);

        //System.out.println("before: " + Arrays.toString(target));
        //normalizeBetween(target, 0, 100);
        //System.out.println("after: " + Arrays.toString(target));
        //System.exit(1);


        TreeMap<Double, Integer> candidatePlots = findCandidatePlots(plot_target, bin, target);
        //System.out.println("Candidate PLOT IS: " + candidatePlot + " and it has " + augmentator.plots.get(candidatePlot).trees.size() + " trees");

        //System.exit(1);

        //forestPlot plot = augmentator.plots.get(keysAsArray.get(r.nextInt(keysAsArray.size())));

        //augmentator.plots.get(candidatePlot).lockPlot();

        //candidatePlot = 56;
        List<Double> keysAsArray_ = new ArrayList<Double>(candidatePlots.keySet());

        int candidatePlot = candidatePlots.get(keysAsArray_.get(0));

        //System.out.println(keysAsArray_);
        //System.out.println("----------------------------------");
        //System.out.println((keysAsArray_.get(0)));

        forestPlot plot = augmentator.plots.get(candidatePlot);

        //System.out.println("INITIAL PLOT IS: " + plot.id + " " + sumOfArray(calculateSpeciesSpecificVolumes(plot.trees, bin, target, plot.area)));
        //System.out.println("TARGET PLOT IS: " + plot_target.id + " " + sumOfArray(target_species_volumes));
        //System.exit(1);
        //plot = augmentator.plots.get(8);

        //forestPlot plot_target = augmentator.plots.get(keysAsArray.get(r.nextInt(keysAsArray.size())));

        //plot_target = augmentator.plots.get(127);

        //System.out.println("TARGET PLOT IS: " + plot_target.id );
        //System.out.println("FROM PLOT IS: " + plot.id );

        if(plot_target.id == plot.id){
            return;
        }

        ArrayList<forestTree> plotTrees = new ArrayList<forestTree>(plot.trees.values());

        HashMap<Integer, forestTree> plotTrees_map = plot.trees_unique_id_for_simulation;



        ArrayList<Integer> removeThese = new ArrayList<Integer>();


        int[] target_pine = new int[ (int) (dbh_max_pine / bin) ];
        int[] target_spruce = new int[ (int) (dbh_max_spruce / bin) ];
        int[] target_decid = new int[ (int) (dbh_max_decid / bin) ];


        dbh_max_decid += bin * 2;
        dbh_max_spruce += bin * 2;
        dbh_max_pine += bin * 2;


        ArrayList<int[]> target_species = this.calculateSpeciesSpecificDiameterDistributions(plotTrees_map_target, bin, target);
        ArrayList<int[]> target_species_height = this.calculateSpeciesSpecificDiameterDistributions(plotTrees_map_target, bin, target);


        double target_species_dgm_pine =calculateDGM(plotTrees_map_target, 0);
        double target_species_dgm_spruce =calculateDGM(plotTrees_map_target, 1);
        double target_species_dgm_decid =calculateDGM(plotTrees_map_target, 2);

        double[] target_species_dgm = new double[]{target_species_dgm_pine, target_species_dgm_spruce, target_species_dgm_decid};


        initialSolution = this.calculateDiameterDistribution(plotTrees_map, bin, initialSolution);



        //System.out.println(Arrays.toString(target));

        simulatedAnnealingForestSimulator sa = new simulatedAnnealingForestSimulator(augmentator, simulationID);

        sa.setCandidatePlots(candidatePlots);

        sa.setAllPlots(augmentator.plots);
        sa.setTarget(target, bin, max);
        sa.setTarget(target, bin, new double[]{dbh_max_pine, dbh_max_spruce, dbh_max_decid});
        sa.setTargetSpeciesSpecific(target_species, bin);
        sa.setTargetSpeciesSpecificHeight(target_species_height, bin);

        sa.setTargetVolume(target_species_volumes);
        sa.setTargetDGM(target_species_dgm);

        List<forestTree> trees_all = new ArrayList<forestTree>(augmentator.itc_with_tree.values());
        //HashMap<Integer, forestTree> trees_all_map = augmentator.itc_with_tree_unique_id;
        HashMap<Integer, forestTree> trees_all_map = (HashMap<Integer, forestTree>)augmentator.trees.clone();

/*
        for(int i : trees_all_map.keySet()){
            if(trees_all_map.get(i).getPlotID() == plot.id || trees_all_map.get(i).getPlotID() == plot_target.id){
                removeThese.add(i);
            }
        }

        for(int i : removeThese){
            trees_all_map.remove(i);
        }
*/
        sa.initialSolution(plotTrees_map, plot.id);
        sa.setSearchSpace(trees_all_map);

/*
        for(int i : plotTrees_map.keySet()){
            System.out.println(plotTrees_map.get(i).getTreeITCid() + " " + plotTrees_map.get(i).getTreeDBH());
        }

 */

        sa.optimize();

        //System.out.println(sa.startId + " " + sa.bestSolutionPlotId);
        plot = augmentator.plots.get(sa.bestSolutionPlotId);


        try {
            while (plot.isLocked()){
                Thread.sleep(1000);
                System.out.println("plot " + plot.id + " is locked for thread " +  Thread.currentThread().getId());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        plot.lockPlot();
        System.out.println("Locked plot " + plot.id + " for thread " +  Thread.currentThread().getId());

        plot.targetVolume = target_species_volumes;

        plot.prepareSimulationOffsets();



/*
        System.out.println("----------------");
        for(int i : sa.best_solution_map.keySet()){
            System.out.println(sa.best_solution_map.get(i).getTreeITCid() + " " + sa.best_solution_map.get(i).getTreeDBH());
        }


 */
        int count_original = 0;
        int count_new = 0;
        int count_unchanged = 0;

        HashSet<Integer> ITC_segments_original = new HashSet<>();
        HashSet<Integer> ITC_segments_optimized = new HashSet<>();
        HashSet<Integer> ITC_segments_optimized_all_trees = new HashSet<>();
        HashSet<Integer> keep = new HashSet<>();
        int count1 = 0, count2 = 0;

        //System.exit(1);

        count1 = 0;
        count2 = 0;


        for(int i : plot.trees_unique_id_for_simulation.keySet()){


            if(plot.trees_unique_id_for_simulation.get(i).hasCrown) {
                //System.out.println(plot.trees_unique_id_for_simulation.get(i).getTreeITCid() + " " + ITC_segments_original.contains(plot.trees_unique_id_for_simulation.get(i).getTreeITCid()));

                ITC_segments_original.add(plot.trees_unique_id_for_simulation.get(i).getTreeITCid());
                count1++;
            }

        }

        System.out.println("ITC_segments_original.size() " + ITC_segments_original.size() + " " + count1);

        int counter2 = 0;

        for(int i : sa.best_solution_map.keySet()){

            if(plot.trees_unique_id_for_simulation.containsKey(i)){
                counter2++;
            }
            if(sa.best_solution_map.get(i).hasCrown) {
                count2++;
                if(false)
                if( ITC_segments_original.contains(i) ) {
                    ITC_segments_original.remove(i);
                    //keep.add(i);
                }else{
                    ITC_segments_optimized.add(sa.best_solution_map.get(i).getTreeITCid());
                }

                ITC_segments_optimized_all_trees.add(sa.best_solution_map.get(i).getTreeITCid());
            }

        }

        //System.out.println("ITC_segments_optimized.size() " + ITC_segments_optimized_all_trees.size() + " " + count2);

        if(false)
        if(counter2 == plot.trees_unique_id_for_simulation.size()){
            System.out.println("Solution is identical to the original!");
            plot.unlock();
            return;
        }


        plot.simulated_plot = sa.best_solution_map;

        if(count1 != count2){
            plot.unlock();
            System.out.println("simulation: " + simulationID + " from " + plot.id + " plot to " + plot_target.id +  " plot. " + " same as in orig: " + counter2);
            throw new toolException("Optimized and original have different number of ITC segments   ! " + count1 + " " + count2 + " " + " same as in orig: " + counter2);

        }

        ArrayList<Integer> ITC_segments_original_array
                = new ArrayList<>(ITC_segments_original);
        ArrayList<Integer> ITC_segments_optimized_array
                = new ArrayList<>(ITC_segments_optimized_all_trees);

        if(ITC_segments_original.size() != ITC_segments_optimized_all_trees.size()){
            System.out.println(count1 + " " + count2);
            plot.unlock();
            throw new toolException("ITC segments are not equal! " + ITC_segments_original.size() + " " + ITC_segments_optimized_all_trees.size());
        }

        HashSet<Integer> remove = new HashSet<>();
        TreeMap<Integer, Integer> add = new TreeMap<>();
        TreeSet<Integer> plots_ = new TreeSet<>();
        int counter = 0;

        ArrayList<Double> optim = new ArrayList<>();
        ArrayList<Double> orig = new ArrayList<>();

        for(int i : ITC_segments_optimized_array){
            optim.add(augmentator.itc_with_tree.get(i).getTreeCrownArea());
        }

        for(int i : ITC_segments_original_array){
            orig.add(augmentator.itc_with_tree.get(i).getTreeCrownArea());
        }

        //System.out.println(optim.size() + " " + orig.size());
        Map<Integer, Integer> res = matchClosest(optim, orig);

        //System.out.println(Arrays.toString(res.toArray()));
        //System.exit(1);

        for(int i = 0; i <  ITC_segments_original_array.size(); i++){

            remove.add(ITC_segments_original_array.get(i));
            //add.put(ITC_segments_optimized_array.get(i), ITC_segments_original_array.get(i));
            add.put(ITC_segments_optimized_array.get(res.get(i)), ITC_segments_original_array.get(i));

            plots_.add(augmentator.itc_with_tree.get(ITC_segments_optimized_array.get(i)).getPlotID());

        }


        sInfo.setTarget(  new double[]{sumOfArray(sa.targetVolume)});

        sInfo.setSimulated( new double[]{sumOfArray(this.calculateSpeciesSpecificVolumes(sa.best_solution_map, bin, target, plot.getArea()))});
        sInfo.setFromFile(plot.getPlotLASFile().getName());
        sInfo.setInitial( new double[]{sumOfArray(this.calculateSpeciesSpecificVolumes(plot.trees_unique_id_for_simulation, bin, target, plot.getArea()))});

        this.addReport(sInfo);

        //System.out.println("from plot: " + plot.getPlotID());

        //for(int i : plots_)
        //    System.out.println("from plot: " + i);
        //System.out.println("remove.size() " + remove.size() + " " + keep.size());
        //System.exit(1);
        if(fill)
            plot.createSimulationFile("filled_simulation_" + simulationID, odir);
        else if(original)
            plot.createSimulationFile("original_" + simulationID, odir);
        else if(!fill)
            plot.createSimulationFile("holey_simulation_" + simulationID, odir);


        try {
            plot.writeSimulationFile(this, remove, add, plots_, fill, original, simulationID, keep);
        }catch (Exception e) {
            plot.unlock();
            e.printStackTrace();
        }
/*
        try {
            plot.writeSimulationFile(this, remove, add, plots_, fill, original, simulationID);
        }catch (Exception e) {
            e.printStackTrace();
        }

 */
        /*
        Object[] orig_keys = plot.trees_unique_id.keySet().toArray();
        Object[] opti_keys = sa.best_solution_map.keySet().toArray();

        List<Integer> orig_keys_ = new ArrayList<Integer>(plot.trees_unique_id.keySet());
        List<Integer> opti_keys_ = new ArrayList<Integer>(sa.best_solution_map.keySet());

        System.out.println(Arrays.toString(orig_keys));


        HashSet<Integer> remove_2 = new HashSet<>();
        HashSet<Integer> unchanged = new HashSet<>();

        for(int i : sa.best_solution_map.keySet()){

            if(plot.trees_unique_id.keySet().contains(i)) {

                if(plot.trees_unique_id.get(i).hasCrown) {
                    remove_2.add(i);
                    count_unchanged++;
                    unchanged.add(sa.best_solution_map.get(i).getTreeID_unique());
                }
            }else {
                remove.add(augmentator.trees.get(i).getTreeITCid());

            }

            if(sa.best_solution_map.get(i).hasCrown)
                count_new++;

        }

        for(int i : remove_2)
            sa.best_solution_map.remove(i);

        opti_keys_ = new ArrayList<Integer>(sa.best_solution_map.keySet());
        Collections.shuffle(opti_keys_);
*/

        //System.out.println("Original: " + count_original + " New: " + count_new + " Unchanged: " + count_unchanged);
        //System.out.println("DONE!");

        plot.unlock();
        System.out.println("Unlocked plot " + plot.id);
        //System.exit(1);

    }

    public void scaleToHectare(int[] array, double areaInSquareMeters){

        double frac = 10000 / areaInSquareMeters;

        for(int i = 0; i < array.length; i++){
            array[i] = (int) Math.round(array[i] * frac);
        }

    }

    public static Map<Integer, Integer> matchClosest(List<Double> a, List<Double> b) {
        // make copies of the input lists to avoid modifying the originals
        List<Double> sortedA = new ArrayList<>(a);
        List<Double> sortedB = new ArrayList<>(b);

        // sort both lists in ascending order
        Collections.sort(sortedA);
        Collections.sort(sortedB);

        int n = a.size();
        Map<Integer, Integer> result = new HashMap<>(n);
        boolean[] used = new boolean[n];

        // for each element in list a, find the closest element in list b that hasn't been used yet
        for (int i = 0; i < n; i++) {
            double diff = Double.MAX_VALUE;
            int closestIndex = -1;

            // search for the closest unused element in list b using the two-pointer algorithm
            int j = 0, m = b.size();
            while (j < m) {
                if (used[j]) {
                    j++;
                    continue;
                }
                double curDiff = Math.abs(sortedA.get(i) - sortedB.get(j));
                if (curDiff < diff) {
                    diff = curDiff;
                    closestIndex = j;
                }
                if (sortedB.get(j) >= sortedA.get(i)) {
                    break;
                }
                j++;
            }

            // if no closest match was found for the current element in list a, throw an exception
            if (closestIndex == -1) {
                throw new IllegalStateException("No closest match found for element in list A");
            }

            // add the indices of the closest match to the result map
            result.put(i, closestIndex);

            // mark the indices of the matched elements in list a and list b as used
            used[closestIndex] = true;
        }

        // check if any elements in list b are left unused
        for (int i = 0, m = b.size(); i < m; i++) {
            if (!used[i]) {
                throw new IllegalStateException("Unused element found in list B");
            }
        }

        return result;
    }


    public void simulatePlotDumbWay(int simulationID, boolean fill, boolean original){

        List<Integer> keysAsArray = new ArrayList<Integer>(augmentator.plots.keySet());
        List<Integer> keysAsArray_trees = new ArrayList<Integer>(augmentator.itc_with_tree.keySet());
        Random r = new Random();

        System.out.println(augmentator.itc_with_tree.keySet().size());

        //System.exit(1);

        if(augmentator.aR.set_seed > 0) {
            r.setSeed(augmentator.aR.set_seed * simulationID);
        }
        forestPlot plot = augmentator.plots.get(keysAsArray.get(r.nextInt(keysAsArray.size())));

        plot.prepareSimulation();

        double switch_probability = augmentator.aR.prob;

        HashSet<Integer> remove = new HashSet<>();
        TreeMap<Integer, Integer> add = new TreeMap<>();
        TreeSet<Integer> plots_ = new TreeSet<>();
        HashSet<Integer> alreadySampled = new HashSet<>();

        int tree_ = 0;
        double tree_height = 0;

        for(int tree : plot.trees.keySet()){

            if(plot.trees.get(tree).getTreeITCid() != -1){
                if(r.nextDouble() < switch_probability){

                    remove.add(plot.trees.get(tree).treeITCid);
                    //System.out.println(augmentator.trees_with_itc.get(keysAsArray_trees.get(r.nextInt(keysAsArray_trees.size()))).toString());

                    while(alreadySampled.contains(tree_ = keysAsArray_trees.get(r.nextInt(keysAsArray_trees.size()))) ){
                        System.out.println("Already sampled " + tree_);
                    }

                    add.put(augmentator.itc_with_tree.get(tree_).getTreeITCid(), plot.trees.get(tree).treeITCid);


                    plots_.add(augmentator.itc_with_tree.get(tree_).getPlotID());

                    //System.out.println("Switching " + plot.trees.get(tree).getTreeITCid() + " to " + augmentator.trees_with_itc.get(keysAsArray_trees.get(r.nextInt(keysAsArray_trees.size()))).getTreeITCid());
                    //System.out.println(augmentator.trees_with_itc.get(keysAsArray_trees.get(r.nextInt(keysAsArray_trees.size()))).getTreeITCid() + " from plot: " + augmentator.trees_with_itc.get(keysAsArray_trees.get(r.nextInt(keysAsArray_trees.size()))).getPlotID());
                }
            }
        }
        System.out.println("Removing: " + remove.size());
        System.out.println("Adding " + add.size());
        System.out.println("plots " + plots_.size());


        if(fill)
            plot.createSimulationFile("filled_simulation_" + simulationID);
        else if(original)
            plot.createSimulationFile("original_" + simulationID);
        else if(!fill)
            plot.createSimulationFile("holey_simulation_" + simulationID);
        try {
            plot.writeSimulationFile(this, remove, add, plots_, fill, original, simulationID, new HashSet<Integer>());
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    public double[] calculateDiameterDistribution(List<forestTree> plot, double bins, double[] target){

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

    public int[] calculateDiameterDistribution(HashMap<Integer, forestTree> plot, double bins, int[] target){



        int[] diameterDistribution = new int[target.length];

        for(int i : plot.keySet()){
            int bin = (int)Math.floor(plot.get(i).getTreeDBH() / bins);

            if(bin >= diameterDistribution.length){
                bin = diameterDistribution.length - 1;
            }

            diameterDistribution[bin]++;
        }

        return normalizeIntArray(diameterDistribution);

    }

    public static int[] normalizeIntArray(int[] arr) {
        int[] result = new int[arr.length];
        int min = arr[0];
        int max = arr[0];

        // Find the minimum and maximum values in the array
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < min) {
                min = arr[i];
            }
            if (arr[i] > max) {
                max = arr[i];
            }
        }

        // Scale each value to be between 0 and 100
        double range = (double)(max - min);
        for (int i = 0; i < arr.length; i++) {
            result[i] = (int)Math.round(((arr[i] - min) / range) * 100);
        }

        return result;
    }

    public double[] calculateSpeciesSpecificVolumes(HashMap<Integer, forestTree> plot, double bins, int[] target, double area){

        double[] volumes = new double[]{0,0,0};

        for(int i : plot.keySet()){

            volumes[plot.get(i).treeSpecies] += plot.get(i).getTreeVolume();

        }

        double frac = 10000 / area;

        for(int i = 0; i < volumes.length; i++){
            volumes[i] = volumes[i] * frac / 1000;
        }

        return volumes;

    }

    public int[] diameterDistribution(HashMap<Integer, forestTree> plot, double bins, int[] target){

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

    public ArrayList<int[]> calculateSpeciesSpecificDiameterDistributions(HashMap<Integer, forestTree> plot, double bins, int[] target){

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
                throw new toolException("species " +  plot.get(i).treeSpecies + " not recognized!");
        }

        ArrayList<int[]> speciesSpecificDiameterDistributions = new ArrayList<>();
        speciesSpecificDiameterDistributions.add(diameterDistribution_pine);
        speciesSpecificDiameterDistributions.add(diameterDistribution_spruce);
        speciesSpecificDiameterDistributions.add(diameterDistribution_decid);

        return speciesSpecificDiameterDistributions;

    }
    public ArrayList<int[]> calculateSpeciesSpecificHeightDistributions(HashMap<Integer, forestTree> plot, double bins, int[] target){

        int[] diameterDistribution_pine = new int[target.length];
        int[] diameterDistribution_spruce = new int[target.length];
        int[] diameterDistribution_decid = new int[target.length];

        for(int i : plot.keySet()){
            int bin = (int)Math.floor(plot.get(i).getTreeHeight() / bins);


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
                throw new toolException("species " +  plot.get(i).treeSpecies + " not recognized!");
        }

        ArrayList<int[]> speciesSpecificDiameterDistributions = new ArrayList<>();
        speciesSpecificDiameterDistributions.add(diameterDistribution_pine);
        speciesSpecificDiameterDistributions.add(diameterDistribution_spruce);
        speciesSpecificDiameterDistributions.add(diameterDistribution_decid);

        return speciesSpecificDiameterDistributions;

    }

    public static double calculateDGM(HashMap<Integer, forestTree> trees, int treeSpecies  ) {
        // Sort the diameters in ascending order


        ArrayList<Double> treeDiameters = new ArrayList<Double>();

        int counter = 0;
        for(int tree : trees.keySet()){

            if(trees.get(tree).getTreeSpecies() == treeSpecies)
                treeDiameters.add(trees.get(tree).getTreeDBH());
        }


        //System.out.println(treeDiameters.size());


        if(treeDiameters.size() == 0)
            return 0.0;

        Collections.sort(treeDiameters);

        // Calculate the total basal area
        double totalBasalArea = 0.0;
        for (double diameter : treeDiameters) {
            //System.out.println("diameter: " + diameter);
            double radius = diameter / 2.0;
            double basalArea = Math.PI * radius * radius;
            totalBasalArea += basalArea;
        }

        // Calculate the basal area per tree
        double basalAreaPerTree = totalBasalArea / 2.0;

        // Find the diameter of the basal area median tree
        double basalAreaSum = 0.0;
        int i = 0;
        while (basalAreaSum < basalAreaPerTree && i < treeDiameters.size()) {
            double radius = treeDiameters.get(i) / 2.0;
            double basalArea = Math.PI * radius * radius;
            basalAreaSum += basalArea;
            i++;
        }
        // Return the diameter of the basal area median tree
        return treeDiameters.get(i - 1);
    }

    public double sumOfArray(double[] array){
        double sum = 0;
        for(int i = 0; i < array.length; i++){
            sum += array[i];
        }
        return sum;
    }

    public static double calculateDGM(HashMap<Integer, forestTree> trees) {
        // Sort the diameters in ascending order

        double[] treeDiameters = new double[trees.size()];

        int counter = 0;
        for(int tree : trees.keySet()){
            treeDiameters[counter++] = trees.get(tree).getTreeDBH();
        }

        Arrays.sort(treeDiameters);

        // Calculate the total basal area
        double totalBasalArea = 0.0;
        for (double diameter : treeDiameters) {
            double radius = diameter / 2.0;
            double basalArea = Math.PI * radius * radius;
            totalBasalArea += basalArea;
        }

        // Calculate the basal area per tree
        double basalAreaPerTree = totalBasalArea / treeDiameters.length;

        // Find the diameter of the basal area median tree
        double basalAreaSum = 0.0;
        int i = 0;
        while (basalAreaSum < basalAreaPerTree && i < treeDiameters.length) {
            double radius = treeDiameters[i] / 2.0;
            double basalArea = Math.PI * radius * radius;
            basalAreaSum += basalArea;
            i++;
        }
        // Return the diameter of the basal area median tree
        return 2.0 * treeDiameters[i - 1];
    }

    public void writeSimulationRaport(File output){

        try {
            FileWriter fw = new FileWriter(output);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write("SimulationID\tTarget\tInitial\tSimulated\t" +
                    "Target_p\tInitial_p\tSimulated_p\t" +
                    "Target_s\tInitial_s\tSimulated_s\t" +
                    "Target_d\tInitial_d\tSimulated_d\t" +
                    "FromFile");
            bw.newLine();

            for(simulationInformation sim : simulationRaport){
                bw.write(sim.getSimulationID() + "\t");
                bw.write(sumOfArray(sim.getTarget()) + "\t");
                bw.write(sumOfArray(sim.getInitial()) + "\t");
                bw.write(sumOfArray(sim.getSimulated()) + "\t");

                bw.write((sim.getTarget()[0]) + "\t");
                bw.write((sim.getInitial()[0]) + "\t");
                bw.write((sim.getSimulated()[0]) + "\t");

                bw.write((sim.getTarget()[1]) + "\t");
                bw.write((sim.getInitial()[1]) + "\t");
                bw.write((sim.getSimulated()[1]) + "\t");

                bw.write((sim.getTarget()[2]) + "\t");
                bw.write((sim.getInitial()[2]) + "\t");
                bw.write((sim.getSimulated()[2]) + "\t");

                bw.write(sim.getFromFile());
                bw.newLine();
            }

            bw.close();
            fw.close();
        } catch (IOException ex) {

        }

    }
}



class simulationInformation{

        public int simulationID;

        public double[] target;
        public double[] initial;
        public double[] simulated;

        public String fromFile;

        public simulationInformation(int simulationID){
            this.simulationID = simulationID;
        }

    public int getSimulationID() {
        return simulationID;
    }

    public void setSimulationID(int simulationID) {
        this.simulationID = simulationID;
    }

    public double[] getTarget() {
        return target;
    }

    public void setTarget(double[] target) {
        this.target = target;
    }

    public double[] getSimulated() {
        return simulated;
    }

    public void setSimulated(double[] simulated) {
        this.simulated = simulated;
    }

    public String getFromFile() {
        return fromFile;
    }

    public void setFromFile(String fromFile) {
        this.fromFile = fromFile;
    }

    public double[] getInitial() {
        return initial;
    }

    public void setInitial(double[] initial) {
        this.initial = initial;
    }
}
