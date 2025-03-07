package utils;


import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import com.github.jaiimageio.impl.plugins.gif.GIFImageWriter;
import com.github.jaiimageio.impl.plugins.gif.GIFImageWriterSpi;
import com.sun.imageio.plugins.gif.GIFImageReader;
import com.sun.imageio.plugins.gif.GIFImageReaderSpi;
import err.toolException;
import io.vertx.core.net.impl.HandlerHolder;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.ml.distance.EarthMoversDistance;
import tools.plotAugmentator;
import tools.GifSequenceWriter;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.lang.Math.round;
import static utils.simulatedAnnealingForestSimulator.multiplyMatrices;

public class simulatedAnnealingForestSimulator {

    boolean visualize = false;
    plotAugmentator augmentator;
    double z_score_threshold = 3.0;
    double area;
    double train_area = Math.PI * Math.pow(9, 2);
    int[] target;

    HashMap<Integer, forestPlot> allPlots;
    HashSet<Integer> donePlots = new HashSet<>();
    ArrayList<int[]> targetSpecies;
    ArrayList<int[]> targetSpecies_height;
    List<Double> residual;
    double cost;
    ArrayList<forestTree> current_solution;
    HashMap<Integer, forestTree> current_solution_map;
    public List<forestTree> best_solution;
    public HashMap<Integer, forestTree> best_solution_map;

    public int bestSolutionPlotId = -1;
    public int currentPlotId = -1;

    public int startId = -1;
    TreeMap<Double, Integer> candidatePlots;
    List<forestTree> originalPlot;
    HashMap<Integer, forestTree> originalPlot_map;
    List<forestTree> searchSpace;
    HashMap<Integer, forestTree> searchSpace_map;
    HashMap<Integer, forestTree> original_searchSpace_map;

    double maxTemperature = 2.0;
    double minTemperature = 0.05;

    double cooling = 0.98;
    //double cooling = 0.80;

    double maxValue = -1;
    double[] maxValue_array = new double[]{0,0,0};


    int iterPerTemp = 100;

    double switch_prop = 0.5;

    int numParamsSwitched;

    double[] targetVolume = new double[]{0,0,0};

    double[] targetDGM = new double[]{0,0,0};

    ArrayList<Integer> remove_these = new ArrayList<>();
    ArrayList<Integer> add_these = new ArrayList<>();

    double bins;
    Random r = new Random();

    int simulationId = -1;
    List<List<double[]>> pointDataForAnimation = new ArrayList<>();
    List<double[]> volumesForAnimation = new ArrayList<>();
    List<Double> costsForAnimation = new ArrayList<>();

    double allTimeBestCost = 0;
    public simulatedAnnealingForestSimulator(plotAugmentator augmentator, int simulationId){

        this.simulationId = simulationId;

        this.augmentator = augmentator;
        //r.setSeed(1234);

    }



    public void initialSolution(ArrayList<forestTree> parameters){
        this.current_solution = parameters;
        this.originalPlot = (ArrayList<forestTree>)parameters.clone();
        numParamsSwitched = parameters.size();
        this.best_solution = (ArrayList<forestTree>)parameters.clone();
    }

    public void initialSolution(HashMap<Integer, forestTree> parameters, int plotId){

        this.current_solution_map = (HashMap<Integer, forestTree>) parameters.clone();
        this.originalPlot_map = (HashMap<Integer, forestTree>)parameters.clone();


        int count = 0;

        for(int j : current_solution_map.keySet()){
            if(current_solution_map.get(j).hasCrown)
                count++;
        }

        numParamsSwitched = count / 2;



        this.best_solution_map = (HashMap<Integer, forestTree>)parameters.clone();
        this.bestSolutionPlotId = plotId;
        this.currentPlotId = plotId;
        this.startId = plotId;


    }

    public void setAllPlots(HashMap<Integer, forestPlot> allPlots){

        this.allPlots = allPlots;

        for(int i : allPlots.keySet()){

             this.area = allPlots.get(i).area;
             break;
        }

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

    public void setCandidatePlots(TreeMap<Double, Integer> candidatePlots){
        this.candidatePlots = candidatePlots;
    }


    public void setTargetSpeciesSpecific(ArrayList<int[]> target, double bins){
        this.bins = bins;
        this.targetSpecies = target;
    }

    public void setTargetVolume(double[] target){

        this.targetVolume = target;
    }

    public void setTargetDGM(double[] targetDGM){

        this.targetDGM = targetDGM;

    }

    public void setTargetSpeciesSpecificHeight(ArrayList<int[]> target, double bins){
        this.bins = bins;
        this.targetSpecies_height = target;
    }



    public void setSearchSpace(ArrayList<forestTree> searchSpace){
        this.searchSpace = (ArrayList<forestTree>)searchSpace.clone();
    }
    public void setSearchSpace(HashMap<Integer, forestTree> searchSpace){
        this.searchSpace_map = (HashMap<Integer, forestTree>)searchSpace.clone();
        this.original_searchSpace_map = (HashMap<Integer, forestTree>)searchSpace.clone();
    }

    public double cost(){
        return cost;
    }

    public void duplicateTrees(){


    }
    public void optimize(){

        boolean includeCanopyCover = sumOfArray(targetVolume) > 60 ? false : true;
        //double[] diameterDistribution_ = calculateDiameterDistribution(current_solution);

        ArrayList<Integer> removeThese = new ArrayList<>();

        for(int i : searchSpace_map.keySet()){
            if(searchSpace_map.get(i).getPlotID() == currentPlotId){
                removeThese.add(i);
            }
        }

        for(int i : removeThese){
            searchSpace_map.remove(i);

        }

        //double initialDGM_pine = calculateDGM(current_solution_map, 0);
        //double initialDGM_spruce = calculateDGM(current_solution_map, 1);
        //double initialDGM_decid = calculateDGM(current_solution_map, 2);

        //double[] initialDGM = new double[]{initialDGM_pine, initialDGM_spruce, initialDGM_decid};

        //System.out.println("dgm: " + Arrays.toString(initialDGM));
        //System.exit(1);

        //System.out.println(Arrays.toString(target));
        //System.out.println(Arrays.toString(diameterDistribution_));
        //System.out.println("initialCost: " + initialCost);
        //System.out.println("bins: " + bins);
        //System.exit(0);


        int maxParamsSwitched = numParamsSwitched;
        double orig_switch_probability = switch_prop;
        double all_time_best_cost = Double.MAX_VALUE;
        double crownAreaTotal_original = calculateCrownArea(current_solution_map);
        double currentCrownArea = crownAreaTotal_original;




        //for(int restart = 0; restart < (int)(this.allPlots.size() * 0.1); restart++) {
        //for(int restart = 0; restart < (int)(this.allPlots.size() * 0.05); restart++) {
        for(int restart = 0; restart < 3; restart++) {

            double t = maxTemperature;

            //ArrayList<int[]> diameterDistribution_species = calculateSpeciesSpecificDiameterDistributions(current_solution_map);
            //ArrayList<int[]> heightDistribution_species = calculateSpeciesSpecificHeightDistributions(current_solution_map);

            List<List<Double>> outliers = outliersModifiedZScore(current_solution_map, z_score_threshold);

            int n_outliers_original = outliers.get(0).size();
            //List<Double> original
            double[] speciesVolume = calculateSpeciesSpecificVolumes(current_solution_map, this.area);

            double[] targetVolumes = new double[]{targetVolume[0], targetVolume[1],targetVolume[2], sumOfArray(targetVolume)};
            double[] volumesCurrent = new double[]{speciesVolume[0],speciesVolume[1],speciesVolume[2], sumOfArray(speciesVolume)};

            //double dgm_1 = calculateDGM(current_solution_map, 0);
            //double dgm_2 = calculateDGM(current_solution_map, 1);
            //double dgm_3 = calculateDGM(current_solution_map, 2);
            crownAreaTotal_original = calculateCrownArea(current_solution_map);
            currentCrownArea = crownAreaTotal_original;

            //double[] dgm = new double[]{dgm_1, dgm_2, dgm_3};

            //double initialCost = costWithEMDSINGLE(targetSpecies, diameterDistribution_species);

            //double distance = costWithEMDSINGLE(diameterDistribution(current_solution_map, this.bins, this.target), target);


            // NO NEED TO ADD CROWN AREA TO THIS ONE BECAUSE THE DIFFERENCE SHOULD BE 0;
            double initialCost = relativeRMSE(targetVolume, speciesVolume) + calculateRelativeDifference(crownAreaTotal_original, currentCrownArea);
            double sd_itcs = calculateStandardDeviation(current_solution_map);
            initialCost = relativeDifference(sumOfArray(targetVolume), sumOfArray(speciesVolume)) + relativeRMSE(targetVolume, speciesVolume);

            volumesCurrent[0] = speciesVolume[0];
            volumesCurrent[1] = speciesVolume[1];
            volumesCurrent[2] = speciesVolume[2];
            volumesCurrent[3] = sumOfArray(speciesVolume);

            initialCost = relativeRMSE(targetVolumes, volumesCurrent);

            // THIS WAS RECENTLY COMMENTED OUT (09.01.2025)
            //initialCost += sd_itcs;
            //initialCost = relativeDifference(sumOfArray(targetVolume), sumOfArray(speciesVolume));
            //                + relativeRMSE(targetDGM, dgm);
                    //+ costWithEMDSINGLE(targetSpecies, diameterDistribution_species);
            double initial_sd = sd_itcs;

            //initialCost += sd_itcs;

            //if(includeCanopyCover)

            //if(sumOfArray(targetVolume) > 55)
            //initialCost += calculateRelativeDifference(crownAreaTotal_original, currentCrownArea);

            //double initialCost_height = costWithEMDSINGLE(targetSpecies_height, heightDistribution_species);

            //initialCost += initialCost_height;
            //double initialCost = bhattacharyyaSimilarity(targetSpecies, diameterDistribution_species);
            //double initialCost = bhattacharyyaDistance(targetSpecies, diameterDistribution_species);
            double previousCost = initialCost;

            if(restart == 0) {


                all_time_best_cost = initialCost;

                if(visualize)
                    this.addFrameToAnimation();
                this.volumesForAnimation.add(volumesCurrent.clone());
                this.costsForAnimation.add(all_time_best_cost);

            }

            System.out.println("initial_cost: " + initialCost + " restart + " + restart);

            while (t >= this.minTemperature) {

                //numParamsSwitched = (int)Math.ceil(maxParamsSwitched * t);
                //numParamsSwitched = maxParamsSwitched;
                //System.out.println("numParamsSwitched: " + numParamsSwitched);
                //switch_prop = orig_switch_probability * (t * 1.5);
                switch_prop = t * 0.33;
                int swap_this_many = (int) Math.ceil(switch_prop * numParamsSwitched);
                swap_this_many = Math.max(1, swap_this_many);
                int average_n_swapped = 0;

                switch_prop = Math.min(switch_prop, 0.5);

                for (int i = 0; i < iterPerTemp; i++) {

                    ///Collections.shuffle(current_solution);
                    int n_swapped = swapStuff(current_solution_map, searchSpace_map, switch_prop, swap_this_many);
                    average_n_swapped += n_swapped;
                    //int[] diamterDistribution = calculateDiameterDistribution_(current_solution_map);
                    //ArrayList<int[]> diamterDistribution = calculateSpeciesSpecificDiameterDistributions(current_solution_map);
                    //ArrayList<int[]> heightDistribution = calculateSpeciesSpecificHeightDistributions(current_solution_map);

                    speciesVolume = calculateSpeciesSpecificVolumes(current_solution_map, this.area);
                    double crownArea = calculateCrownArea(current_solution_map);

                    volumesCurrent[0] = speciesVolume[0];
                    volumesCurrent[1] = speciesVolume[1];
                    volumesCurrent[2] = speciesVolume[2];
                    volumesCurrent[3] = sumOfArray(speciesVolume);

                    //double newCost = bhattacharyyaDistance(target, diamterDistribution);
                    //double newCost = hellingerDistance_species(targetSpecies, diamterDistribution);
                    //double newCost = costWithEMDSINGLE(targetSpecies, diamterDistribution);

                    //initialDGM_pine = calculateDGM(current_solution_map, 0);
                    //initialDGM_spruce = calculateDGM(current_solution_map, 1);
                    //initialDGM_decid = calculateDGM(current_solution_map, 2);

                    //dgm = new double[]{initialDGM_pine, initialDGM_spruce, initialDGM_decid};

                    double newCost = relativeRMSE(targetVolume, speciesVolume) + calculateRelativeDifference(crownAreaTotal_original, crownArea);

                    newCost = relativeDifference(sumOfArray(targetVolume), sumOfArray(speciesVolume)) + relativeRMSE(targetVolume, speciesVolume);
                    newCost = relativeDifference(sumOfArray(targetVolume), sumOfArray(speciesVolume));
                    newCost = relativeRMSE(targetVolumes, volumesCurrent);
                            //+ relativeRMSE(targetDGM, dgm);
                            //+ costWithEMDSINGLE(targetSpecies, diamterDistribution);;
                    //double newCost_height = costWithEMDSINGLE(targetSpecies_height, heightDistribution);

                    //System.out.println(Arrays.toString(speciesVolume) + " " + Arrays.toString(targetVolume));

                    //distance = costWithEMDSINGLE(diameterDistribution(current_solution_map, this.bins, this.target), target);
                    //System.out.println("distance: " + distance);
                    //if(includeCanopyCover)
                    //if(sumOfArray(targetVolume) > 55)

                        //newCost += calculateRelativeDifference(crownAreaTotal_original, crownArea);

                    //newCost += newCost_height;
                    //double newCost = bhattacharyyaSimilarity(targetSpecies, diamterDistribution);

                    sd_itcs = calculateStandardDeviation(current_solution_map);
                    //newCost += relativeDifference(sd_itcs, initial_sd);

                    // THIS WAS RECENTLY COMMENTED OUT (09.01.2025)
                    //newCost += sd_itcs;

                    List<List<Double>> outliers_ = outliersModifiedZScore(current_solution_map, z_score_threshold);



                    //System.out.println("outliers: " + outliers.size() + " " + outliers);
                   // if (accept(t, previousCost, newCost)) {
                    //if(outliers.size() <= n_outliers_original) {
                    if(outliers_.get(0).size() == 0 || true) {
                    //if(sd_itcs <= 5.0) {
                        if (acceptanceProbability(previousCost, newCost, t) > r.nextDouble()) {

                            //if(newCost > previousCost)
                            //    System.out.println("ACCEPTED WORSE SOLUTION");


                            if (newCost < all_time_best_cost) {

                                all_time_best_cost = newCost;
                                best_solution_map = (HashMap<Integer, forestTree>) current_solution_map.clone();
                                currentCrownArea = crownArea;

                                this.bestSolutionPlotId = this.currentPlotId;

                                if(visualize)
                                    this.addFrameToAnimation();
                                this.volumesForAnimation.add(volumesCurrent.clone());
                                this.costsForAnimation.add(all_time_best_cost);
                            }

                            cost = newCost;
                            previousCost = newCost;

                        }
                        else{
                            undoSwap(current_solution_map, searchSpace_map);
                        }
                    }else{
                        undoSwap(current_solution_map, searchSpace_map);
                    }


                }


                t *= cooling;
                //System.out.println("temperature: " + t + " best cost: " + all_time_best_cost + " previous_cost " + previousCost + " average n swapped: " + average_n_swapped / iterPerTemp + " ?==? " + swap_this_many);


            }

            restart();
            prepare();
        }

        System.out.println("All time best cost: " + all_time_best_cost);



        if(visualize)
            try {
                outputAnimation();
                System.exit(1);
            }catch (Exception e){
                e.printStackTrace();
            }



        switch_prop = orig_switch_probability;
/*
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
*/
        System.out.println("Difference in crown area: " + calculateRelativeDifference(crownAreaTotal_original, currentCrownArea));
        System.out.println("Target dgm: " + Arrays.toString(targetDGM));
        System.out.println("Initial dgm: " + Arrays.toString(new double[]{calculateDGM(originalPlot_map, 0), calculateDGM(originalPlot_map, 1), calculateDGM(originalPlot_map, 2)}));
        System.out.println("Final dgm: " + Arrays.toString(new double[]{calculateDGM(best_solution_map, 0), calculateDGM(best_solution_map, 1), calculateDGM(best_solution_map, 2)}));
        System.out.println("target(s): " );
        System.out.println(sumOfArray(targetVolume));
        System.out.println("init(s): ");
        System.out.println(sumOfArray(calculateSpeciesSpecificVolumes(originalPlot_map, this.area)));
        System.out.println("final(s): ");
        System.out.println(sumOfArray(calculateSpeciesSpecificVolumes(best_solution_map, this.area )));

        //System.exit(1);
    }

    public static double calculateRelativeStandardDeviation(double[] arr) {
        int n = arr.length;
        double sum = 0.0;
        double mean = 0.0;
        double deviation = 0.0;

        // Calculate the sum of the elements in the array
        for (double d : arr) {
            sum += d;
        }

        // Calculate the mean value of the array
        mean = sum / n;

        // Calculate the sum of the squares of the differences between each element and the mean
        for (double d : arr) {
            deviation += Math.pow((d - mean), 2);
        }

        // Divide the sum of the squares of the differences by the length of the array minus one,
        // and then take the square root to get the standard deviation
        double standardDeviation = Math.sqrt(deviation / (n - 1));

        // Calculate the relative standard deviation as a percentage
        double relativeStandardDeviation = (standardDeviation / mean) * 100;

        return relativeStandardDeviation;

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
    public void restart(){

        //System.out.println("RESTARTING");

        this.donePlots.add(this.currentPlotId);

        this.searchSpace_map = (HashMap<Integer, forestTree>)this.original_searchSpace_map.clone();

        int counter = 0;

        List<Double> keysAsArray = new ArrayList<Double>(candidatePlots.keySet());

        Collections.shuffle(keysAsArray, r);


        //if(false)
        for(double d : this.candidatePlots.keySet()){

            if(counter++ >= donePlots.size()){
                this.currentPlotId = this.candidatePlots.get(d);
                break;
            }

        }

        if(false)
        for(double d : keysAsArray){

            if(!donePlots.contains(this.candidatePlots.get(d))){
                this.currentPlotId = this.candidatePlots.get(d);
                break;
            }

        }

        ArrayList<Integer> removeThese = new ArrayList<>();

        for(int i : searchSpace_map.keySet()){
            if(searchSpace_map.get(i).getPlotID() == currentPlotId){
                removeThese.add(i);
            }
        }

        for(int i : removeThese){
            searchSpace_map.remove(i);
        }


        this.current_solution_map = (HashMap<Integer, forestTree>)this.allPlots.get(currentPlotId).trees_unique_id_for_simulation.clone();

        int count = 0;

        for(int j : current_solution_map.keySet()){
            if(current_solution_map.get(j).hasCrown)
                count++;
        }

        this.numParamsSwitched = count / 2;


    }

    public void addFrameToAnimation(){

        HashSet<Integer> ITC_segments_optimized = new HashSet<>();
        HashSet<Integer> ITC_segments_original = new HashSet<>();
        HashSet<Integer> ITC_segments_optimized_all_trees = new HashSet<>();

        for(int i : this.allPlots.get(currentPlotId).trees_unique_id_for_simulation.keySet()){
            if(this.allPlots.get(currentPlotId).trees_unique_id_for_simulation.get(i).hasCrown) {
                //System.out.println(plot.trees_unique_id_for_simulation.get(i).getTreeITCid() + " " + ITC_segments_original.contains(plot.trees_unique_id_for_simulation.get(i).getTreeITCid()));

                ITC_segments_original.add(this.allPlots.get(currentPlotId).trees_unique_id_for_simulation.get(i).getTreeITCid());
            }
        }

        for(int i : this.current_solution_map.keySet()){

            if(this.allPlots.get(currentPlotId).trees_unique_id_for_simulation.containsKey(i)){

            }
            if(this.current_solution_map.get(i).hasCrown) {

                ITC_segments_optimized_all_trees.add(this.current_solution_map.get(i).getTreeITCid());
            }
        }

        ArrayList<Integer> ITC_segments_original_array
                = new ArrayList<>(ITC_segments_original);
        ArrayList<Integer> ITC_segments_optimized_array
                = new ArrayList<>(ITC_segments_optimized_all_trees);

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

        //System.out.println(res.size());

        TreeMap<Integer, Integer> add = new TreeMap<>();

        TreeSet<Integer> plots_ = new TreeSet<>();

        for(int i = 0; i <  ITC_segments_original_array.size(); i++){

            add.put(ITC_segments_optimized_array.get(res.get(i)), ITC_segments_original_array.get(i));
            plots_.add(augmentator.itc_with_tree.get(ITC_segments_optimized_array.get(i)).getPlotID());

        }

        addFrame(add, plots_);

        if(false)
        for(int i = 0; i < this.pointDataForAnimation.size(); i++){
            for(int p = 0; p < this.pointDataForAnimation.get(i).size(); p++){
                System.out.println( Arrays.toString(this.pointDataForAnimation.get(i).get(p)));
            }
        }
    }

    public void addFrame(TreeMap<Integer, Integer> add, TreeSet<Integer> plots){

        HashSet<Integer> keep_these_tree_unique_ids = new HashSet<Integer>();

        for(int i : add.keySet()){
            keep_these_tree_unique_ids.add(augmentator.itc_with_tree.get(i).getTreeID_unique());
            for(forestTree tree : augmentator.itc_with_tree.get(i).treesBeneath){
                keep_these_tree_unique_ids.add(tree.getTreeID_unique());
            }
        }

        List<double[]> frame = new ArrayList<>();

        LASReader lasReader = null;
        try {
            lasReader = new LASReader(this.allPlots.get(currentPlotId).plotLASFile);
        }catch (Exception e){
            e.printStackTrace();
        }

        LasPoint tempPoint = new LasPoint();
        int tree_id = -1;
        int tree_unique_id = -1;
        try {
            tree_id = lasReader.extraBytes_names.get("ITC_id");

        }catch (Exception e){
            throw new toolException("Cannot find ITC_id extra byte VLR.");
        }
        try {
            tree_unique_id = lasReader.extraBytes_names.get("tree_id");

        }catch (Exception e){
            throw new toolException("Cannot find tree_id extra byte VLR.");
        }

        double[] pointxy = new double[2];

        HashSet<Integer> wroteITCIds = new HashSet<Integer>();
        int ITC_offset = this.allPlots.get(currentPlotId).ITC_offset;
        int treeOffset = this.allPlots.get(currentPlotId).treeOffset;

        for(long i = 0; i < lasReader.getNumberOfPointRecords(); i++) {

            try {
                lasReader.readRecord(i, tempPoint);
            }catch (Exception e) {
                e.printStackTrace();
            }

            int itc_id = tempPoint.getExtraByteInt(tree_id);
            int tree_id_ = tempPoint.getExtraByteInt(tree_unique_id);

            if(!augmentator.aR.inclusionRule.ask(tempPoint, i, true)){
                continue;
            }

            tempPoint.x += this.allPlots.get(currentPlotId).simulationOffsetX * this.allPlots.get(currentPlotId).simulationLocationCountX;
            tempPoint.y -= this.allPlots.get(currentPlotId).simulationOffsetY * this.allPlots.get(currentPlotId).simulationLocationCountY;

            if(tempPoint.z < augmentator.aR.z_cutoff && tempPoint.classification == 2) {
                //System.out.println("HERE!! YAY!!");
                //augmentator.aR.pfac.writePoint(tempPoint, i, thread_n);
                frame.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, 0, tempPoint.z});
                continue;
            }

            if(add.containsKey(itc_id + ITC_offset)){
                frame.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, 0, tempPoint.z});
                wroteITCIds.add(itc_id + this.allPlots.get(currentPlotId).treeOffset);
                continue;
            }

            if(keep_these_tree_unique_ids.contains(tree_id_ + this.allPlots.get(currentPlotId).treeOffset)){
                frame.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, itc_id, tempPoint.z});

                continue;
            }


            pointxy[0] = tempPoint.x;
            pointxy[1] = tempPoint.y;

            // Setting this to false wil make the results worse!
            if(true)
                if (this.allPlots.get(currentPlotId).pointInPlot(pointxy, this.allPlots.get(currentPlotId).plotBounds_simulated.get(this.allPlots.get(currentPlotId).plotBounds_simulated.size()-1))) {

                    if(itc_id <= 0){
                        frame.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, 0, tempPoint.z});
                    }
                }
        }

        try{
            lasReader.close();

        }catch (Exception e){
            e.printStackTrace();
        }

        for(int p_ : plots){

            if(augmentator.plots.get(p_).id == this.allPlots.get(currentPlotId).id)
                continue;


            LASReader tmpReader2 = null;

            try {
                tmpReader2 = new LASReader(new File(this.allPlots.get(p_).plotLASFile.getAbsolutePath()));
            }catch (Exception e){
                e.printStackTrace();
            }

            int itc_offset = this.allPlots.get(p_).ITC_offset;
            int tree_offset = this.allPlots.get(p_).treeOffset;

            for(long i = 0; i < tmpReader2.getNumberOfPointRecords(); i++) {

                try {
                    tmpReader2.readRecord(i, tempPoint);
                }catch (Exception e){
                    e.printStackTrace();
                }

                if (!augmentator.aR.inclusionRule.ask(tempPoint, i, true)) {
                    continue;
                }

                int itc_id = tempPoint.getExtraByteInt(tree_id) + itc_offset;
                int tree_id_ = tempPoint.getExtraByteInt(tree_unique_id) + tree_offset;

                //System.out.println(itc_id + " " + itc_offset + " " + pA.plots.get(p_).id);


                if (add.containsKey(itc_id)) {

                    //System.out.println("HERE!!");
                    //dones.add(itc_id);

                    wroteITCIds.add(itc_id);

                    if(tempPoint.z > augmentator.aR.z_cutoff) {


                        //tempPoint.x += (ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).geometricCenter[0] - tempPoint.x) +
                        //        (ps.augmentator.ITC_id_to_tree_id.get((itc_id)).geometricCenter[0] - tempPoint.x);
                        tempPoint.x += (augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).getTreeX_ITC() - tempPoint.x) +
                                (augmentator.ITC_id_to_tree_id.get((itc_id)).getTreeX_ITC()- tempPoint.x);
                        //tempPoint.y += (ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).geometricCenter[1] - tempPoint.y) +
                        //        (ps.augmentator.ITC_id_to_tree_id.get((itc_id)).geometricCenter[1] - tempPoint.y);
                        tempPoint.y += (augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).getTreeY_ITC() - tempPoint.y) +
                                (augmentator.ITC_id_to_tree_id.get((itc_id)).getTreeY_ITC() - tempPoint.y);

                        tempPoint.x += this.allPlots.get(currentPlotId).simulationOffsetX * this.allPlots.get(currentPlotId).simulationLocationCountX;
                        tempPoint.y -= this.allPlots.get(currentPlotId).simulationOffsetY * this.allPlots.get(currentPlotId).simulationLocationCountY;
                        //System.out.println(Arrays.toString((ps.augmentator.ITC_id_to_tree_id.get(itc_id).geometricCenter)));


                        frame.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, itc_id, tempPoint.z});


                    }

                }else if(keep_these_tree_unique_ids.contains(tree_id_)){

                    boolean belongsToITC = augmentator.trees.get(tree_id_).belongsToSomeITC;


                    //System.out.println("WHAT! " + belongsToITC);


                    if(belongsToITC) {
                        int itc_id_ = augmentator.trees.get(tree_id_).beneathCrownId;
                        boolean belongsToOptimization = add.containsKey(itc_id_);

                        //System.out.println("YES?!! " + belongsToOptimization + " " + itc_id_);
                        if (tempPoint.z > augmentator.aR.z_cutoff && belongsToOptimization) {


                            //System.out.println("HERE!!");
                            // System.exit(1);
                            //tempPoint.x += (ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).geometricCenter[0] - tempPoint.x) +
                            //        (ps.augmentator.ITC_id_to_tree_id.get((itc_id)).geometricCenter[0] - tempPoint.x);
                            tempPoint.x += (augmentator.ITC_id_to_tree_id.get(add.get(itc_id_)).getTreeX_ITC() - tempPoint.x) +
                                    (augmentator.ITC_id_to_tree_id.get((itc_id_)).getTreeX_ITC() - tempPoint.x);
                            //tempPoint.y += (ps.augmentator.ITC_id_to_tree_id.get(add.get(itc_id)).geometricCenter[1] - tempPoint.y) +
                            //        (ps.augmentator.ITC_id_to_tree_id.get((itc_id)).geometricCenter[1] - tempPoint.y);
                            tempPoint.y += (augmentator.ITC_id_to_tree_id.get(add.get(itc_id_)).getTreeY_ITC() - tempPoint.y) +
                                    (augmentator.ITC_id_to_tree_id.get((itc_id_)).getTreeY_ITC() - tempPoint.y);

                            tempPoint.x += this.allPlots.get(currentPlotId).simulationOffsetX * this.allPlots.get(currentPlotId).simulationLocationCountX;
                            tempPoint.y -= this.allPlots.get(currentPlotId).simulationOffsetY * this.allPlots.get(currentPlotId).simulationLocationCountY;
                            //System.out.println(Arrays.toString((ps.augmentator.ITC_id_to_tree_id.get(itc_id).geometricCenter)));

                            frame.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, itc_id_, tempPoint.z});



                        }
                    }

                }
            }

            try {
                tmpReader2.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        pointDataForAnimation.add(frame);
        System.out.println("Frame added with " + frame.size() + " points");

    }

    public static double[] normalize(double[] v) {
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / norm, v[1] / norm, v[2] / norm};
    }

    public static double[] crossProduct(double[] a, double[] b) {
        double[] c = new double[3];
        c[0] = a[1] * b[2] - a[2] * b[1];
        c[1] = a[2] * b[0] - a[0] * b[2];
        c[2] = a[0] * b[1] - a[1] * b[0];
        return c;
    }



    public void outputAnimation() throws IOException {

        int width = 1000;
        int height = 1000;


        // Create a new GIF file
        ImageOutputStream output = new FileImageOutputStream(new File("/home/koomikko/Documents/research/aba_conv_augmentation/animation.gif"));
        GifSequenceWriter writer = new GifSequenceWriter(output, BufferedImage.TYPE_INT_ARGB, 500, true);

        //Projection proj = new Projection();
        ProjectionP proj = new ProjectionP();

        // Create each frame and add it to the GIF

        int counter = 0;

        double minZ = Double.MAX_VALUE;
        double maxZ = Double.NEGATIVE_INFINITY;
        double minX = Double.MAX_VALUE;
        double maxX = Double.NEGATIVE_INFINITY;

        double minY = Double.MAX_VALUE;
        double maxY = Double.NEGATIVE_INFINITY;

        double rangeX = 0;
        double rangeY = 0;
        double rangeZ = 0;

        double resolution_x = 0;
        double resolution_y = 0;
        double scale = 1.5;
        double resolution = 0;


        for (List<double[]> pointList : pointDataForAnimation) {


        }

        double angle = 0;
        double angle2 = 0;

        Font font = new Font("Arial", Font.BOLD, 24);

        List<double[]> lastOneBackup = new ArrayList<>();
        for(int i = 0; i < pointDataForAnimation.get(pointDataForAnimation.size()-1).size(); i++){
            lastOneBackup.add(new double[]{pointDataForAnimation.get(pointDataForAnimation.size()-1).get(i)[0],
                    pointDataForAnimation.get(pointDataForAnimation.size()-1).get(i)[1],
                    pointDataForAnimation.get(pointDataForAnimation.size()-1).get(i)[2],
                    pointDataForAnimation.get(pointDataForAnimation.size()-1).get(i)[3],
                    pointDataForAnimation.get(pointDataForAnimation.size()-1).get(i)[4]});
        }

        for (List<double[]> pointList : pointDataForAnimation) {


            angle2 += 5;
            if(angle2 > 360)
                angle2 = 5;


            minZ = Double.MAX_VALUE;
            maxZ = Double.NEGATIVE_INFINITY;
            minX = Double.MAX_VALUE;
            maxX = Double.NEGATIVE_INFINITY;
            minY = Double.MAX_VALUE;
            maxY = Double.NEGATIVE_INFINITY;

            for (double[] point : pointList) {

                if(point[0] < minX) minX = point[0];
                if(point[0] > maxX) maxX = point[0];
                if(point[1] < minY) minY = point[1];
                if(point[1] > maxY) maxY = point[1];
                if(point[2] < minZ) minZ = point[2];
                if(point[2] > maxZ) maxZ = point[2];

            }

            rangeX = maxX - minX;
            rangeY = maxY - minY;
            rangeZ = maxZ - minZ;

            //System.out.println(minY + " " + maxY + " " + minX + " " + maxX);
            //System.out.println("rangex: " + rangeX + " rangey: " + rangeY);

            resolution_x = rangeX / width;
            resolution_y = rangeY / height;

            scale = 1.25;
            resolution = Math.max(resolution_x, resolution_y) * scale;

            // Create a new BufferedImage for the frame
            BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = frame.createGraphics();
            g2d.setBackground(Color.WHITE);

            g2d.clearRect(0, 0, width, height);

            //
            // Project each point onto the x-y plane and draw a circle



            for (double[] point : pointList) {

                //(int)Math.floor((tempPoint.x - minX) / step);

                double x = (int) ((point[0] - minX) / resolution);
                double y = (int) ((point[1] - minY) / resolution);
                double z = (int) ((point[2] - minZ) / resolution);


                point[0] = x - width / 2.0 * (1.0 / scale);
                point[1] = y - height / 2.0 * (1.0 / scale);
                point[2] = z - height / 2.0 * (1.0 / scale);


                //System.out.println(Arrays.toString(point));
            }

            for (double[] point : pointList) {

                //System.out.println("before: " + Arrays.toString(point));

                rotatePoint(point, Math.toRadians(70), Math.toRadians(0), Math.toRadians(angle2));

                //System.out.println("after: " + Arrays.toString(point));

            }

            pointList = sortListByThirdElement(pointList);


            g2d.setFont(font);
            g2d.setColor(Color.BLACK);

            String outString = counter + "\nPine: " + this.volumesForAnimation.get(counter)[0] + "\nSpruce: " + this.volumesForAnimation.get(counter)[1] + "\nBirch: " + this.volumesForAnimation.get(counter)[2] +
                    "\nTotal: " + this.volumesForAnimation.get(counter)[3];


            int currentY = 50;

            g2d.drawString(counter + " ", 10, currentY);
            currentY += g2d.getFontMetrics().getHeight();
            g2d.drawString("Optimized", 10 + 150, currentY);
            g2d.drawString("Target", 10 + 350, currentY);

            currentY += g2d.getFontMetrics().getHeight() + 5;
            g2d.drawString("Pine", 10, currentY);
            g2d.drawString(round(this.volumesForAnimation.get(counter)[0]) + " ----------> ", 10 + 150, currentY);
            g2d.drawString(round(targetVolume[0]) + "", 10 + 350, currentY);

            currentY += g2d.getFontMetrics().getHeight();
            g2d.drawString("Spruce", 10, currentY);
            g2d.drawString(round(this.volumesForAnimation.get(counter)[1]) + " ----------> ", 10 + 150, currentY);
            g2d.drawString(round(targetVolume[1]) + "", 10 + 350, currentY);

            currentY += g2d.getFontMetrics().getHeight();
            g2d.drawString("Birch", 10, currentY);
            g2d.drawString(round(this.volumesForAnimation.get(counter)[2]) + " ----------> ", 10 + 150, currentY);
            g2d.drawString(round(targetVolume[2]) + "", 10 + 350, currentY);

            currentY += g2d.getFontMetrics().getHeight();
            g2d.drawString("Total", 10, currentY);
            g2d.drawString(round(this.volumesForAnimation.get(counter)[3]) + " ----------> ", 10 + 150, currentY);
            g2d.drawString(round(sumOfArray(targetVolume)) + "", 10 + 350, currentY);


            int plot_x = 550;
            int plot_y = 40;
            int plotWidth = 400;
            int plotHeight = 200;

            /* THE PLOT */
            g2d.drawLine(plot_x, plot_y + plotHeight, plot_x + plotWidth, plot_y + plotHeight);

            g2d.drawLine(plot_x + plotWidth, plot_y + plotHeight, plot_x + plotWidth - 10, plot_y + plotHeight - 10);
            g2d.drawLine(plot_x + plotWidth, plot_y + plotHeight, plot_x + plotWidth - 10, plot_y + plotHeight + 10);


            g2d.drawLine(plot_x, plot_y, plot_x, plot_y + plotHeight); // Y-axis

            // Y-axis arrow
            g2d.drawLine(plot_x, plot_y, plot_x - 10, plot_y + 10);
            g2d.drawLine(plot_x, plot_y, plot_x + 10, plot_y + 10);

            g2d.drawString("Cost", plot_x - 80, plot_y + plotHeight / 2);

            g2d.drawString("Iteration", plot_x + plotWidth / 2 - 50, plot_y + plotHeight + 40);

            int numTicks = this.costsForAnimation.size();
            int tickSpacing = plotWidth / numTicks;
            double max_y_axis = costsForAnimation.get(0);
            int numTicks_y = 15;



            for(int i = 0; i < numTicks; i++) {
                int yTick = plot_y + plotHeight - (int) (costsForAnimation.get(i) / max_y_axis * plotHeight);

                g2d.drawString("o", plot_x + i * tickSpacing, plot_y + yTick);

                if(i >= counter)
                    break;

            }

            counter++;
            //System.exit(1);
            for (double[] point : pointList) {

                int x = (int) (point[0] + width / 2.0 + width * 0.1);
                int y = (int) (point[1] + height / 2.0 - width * 0.1);

                //x = (int) ((point[0] - minX) / rangeX * width);
                //y = (int) ((point[1] - minY) / rangeY * height);
                //int z = (int) (point[2]);

                //System.out.println(minX + " " + maxX + " " + minY + " " + maxY);
                //System.out.println(rangeX + " " + rangeY);
                //System.out.println("x: " + x +  " y: " + y + " z: ");
                int size = 12;
                //g2d.setColor(getColorFromInt((int)point[3]));
                g2d.setColor(getColorFromViridisGradient((int)point[3], 0.75, 10000));
                //System.out.println(((int)point[4]));
                g2d.fillOval((int) (x - size), (int)(y - size), (int) size, (int) size);


            }


            angle += 45;
            if(angle > 360)
                angle = 45;


            if(counter != pointDataForAnimation.size())
                drawQuestionMark(g2d, plot_x - plotWidth  , plot_y + plotHeight + 150, Math.toRadians(angle));
            else{
                drawExclamasionMark(g2d, plot_x - plotWidth  , plot_y + plotHeight + 150, Math.toRadians(0));

            }


            // Add the frame to the GIF

            writer.writeToSequence(frame);

            if(false)
            if(counter == pointDataForAnimation.size())
                for(int i = 0; i < 5; i++) {
                    writer.writeToSequence(frame);
                }

        }

        counter = pointDataForAnimation.size() - 1;

        for(int i_ = 0; i_ < 20; i_++) {

            List<double[]> pointList = new ArrayList<>();

            for(int i = 0; i < lastOneBackup.size(); i++) {
                pointList.add(new double[]{lastOneBackup.get(i)[0], lastOneBackup.get(i)[1], lastOneBackup.get(i)[2], lastOneBackup.get(i)[3], lastOneBackup.get(i)[4]});
            }

            angle2 += 15;
            if(angle2 > 360)
                angle2 = 15;


            minZ = Double.MAX_VALUE;
            maxZ = Double.NEGATIVE_INFINITY;
            minX = Double.MAX_VALUE;
            maxX = Double.NEGATIVE_INFINITY;
            minY = Double.MAX_VALUE;
            maxY = Double.NEGATIVE_INFINITY;

            for (double[] point : pointList) {

                if(point[0] < minX) minX = point[0];
                if(point[0] > maxX) maxX = point[0];
                if(point[1] < minY) minY = point[1];
                if(point[1] > maxY) maxY = point[1];
                if(point[2] < minZ) minZ = point[2];
                if(point[2] > maxZ) maxZ = point[2];

            }

            rangeX = maxX - minX;
            rangeY = maxY - minY;
            rangeZ = maxZ - minZ;

            //System.out.println(minY + " " + maxY + " " + minX + " " + maxX);
            //System.out.println("rangex: " + rangeX + " rangey: " + rangeY);

            resolution_x = rangeX / width;
            resolution_y = rangeY / height;

            scale = 1.25;
            resolution = Math.max(resolution_x, resolution_y) * scale;

            // Create a new BufferedImage for the frame
            BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = frame.createGraphics();
            g2d.setBackground(Color.WHITE);

            g2d.clearRect(0, 0, width, height);

            //
            // Project each point onto the x-y plane and draw a circle

            for (double[] point : pointList) {

                //(int)Math.floor((tempPoint.x - minX) / step);

                double x = (int) ((point[0] - minX) / resolution);
                double y = (int) ((point[1] - minY) / resolution);
                double z = (int) ((point[2] - minZ) / resolution);


                point[0] = x - width / 2.0 * (1.0 / scale);
                point[1] = y - height / 2.0 * (1.0 / scale);
                point[2] = z - height / 2.0 * (1.0 / scale);


                //System.out.println(Arrays.toString(point));
            }

            for (double[] point : pointList) {

                //System.out.println("before: " + Arrays.toString(point));

                rotatePoint(point, Math.toRadians(70), Math.toRadians(0), Math.toRadians(angle2));

                //System.out.println("after: " + Arrays.toString(point));

            }

            pointList = sortListByThirdElement(pointList);



            g2d.setFont(font);
            g2d.setColor(Color.BLACK);

            String outString = counter + "\nPine: " + this.volumesForAnimation.get(counter)[0] + "\nSpruce: " + this.volumesForAnimation.get(counter)[1] + "\nBirch: " + this.volumesForAnimation.get(counter)[2] +
                    "\nTotal: " + this.volumesForAnimation.get(counter)[3];


            int currentY = 50;

            g2d.drawString(counter + " ", 10, currentY);
            currentY += g2d.getFontMetrics().getHeight();
            g2d.drawString("Optimized", 10 + 150, currentY);
            g2d.drawString("Target", 10 + 350, currentY);

            currentY += g2d.getFontMetrics().getHeight() + 5;
            g2d.drawString("Pine", 10, currentY);
            g2d.drawString(round(this.volumesForAnimation.get(counter)[0]) + " ----------> ", 10 + 150, currentY);
            g2d.drawString(round(targetVolume[0]) + "", 10 + 350, currentY);

            currentY += g2d.getFontMetrics().getHeight();
            g2d.drawString("Spruce", 10, currentY);
            g2d.drawString(round(this.volumesForAnimation.get(counter)[1]) + " ----------> ", 10 + 150, currentY);
            g2d.drawString(round(targetVolume[1]) + "", 10 + 350, currentY);

            currentY += g2d.getFontMetrics().getHeight();
            g2d.drawString("Birch", 10, currentY);
            g2d.drawString(round(this.volumesForAnimation.get(counter)[2]) + " ----------> ", 10 + 150, currentY);
            g2d.drawString(round(targetVolume[2]) + "", 10 + 350, currentY);

            currentY += g2d.getFontMetrics().getHeight();
            g2d.drawString("Total", 10, currentY);
            g2d.drawString(round(this.volumesForAnimation.get(counter)[3]) + " ----------> ", 10 + 150, currentY);
            g2d.drawString(round(sumOfArray(targetVolume)) + "", 10 + 350, currentY);


            int plot_x = 550;
            int plot_y = 40;
            int plotWidth = 400;
            int plotHeight = 200;

            /* THE PLOT */
            g2d.drawLine(plot_x, plot_y + plotHeight, plot_x + plotWidth, plot_y + plotHeight);

            g2d.drawLine(plot_x + plotWidth, plot_y + plotHeight, plot_x + plotWidth - 10, plot_y + plotHeight - 10);
            g2d.drawLine(plot_x + plotWidth, plot_y + plotHeight, plot_x + plotWidth - 10, plot_y + plotHeight + 10);


            g2d.drawLine(plot_x, plot_y, plot_x, plot_y + plotHeight); // Y-axis

            // Y-axis arrow
            g2d.drawLine(plot_x, plot_y, plot_x - 10, plot_y + 10);
            g2d.drawLine(plot_x, plot_y, plot_x + 10, plot_y + 10);

            g2d.drawString("Cost", plot_x - 80, plot_y + plotHeight / 2);

            g2d.drawString("Iteration", plot_x + plotWidth / 2 - 50, plot_y + plotHeight + 40);

            int numTicks = this.costsForAnimation.size();
            int tickSpacing = plotWidth / numTicks;
            double max_y_axis = costsForAnimation.get(0);
            int numTicks_y = 15;



            for(int i = 0; i < numTicks; i++) {
                int yTick = plot_y + plotHeight - (int) (costsForAnimation.get(i) / max_y_axis * plotHeight);

                g2d.drawString("o", plot_x + i * tickSpacing, plot_y + yTick);

                if(i >= counter)
                    break;

            }

            //System.exit(1);
            for (double[] point : pointList) {

                int x = (int) (point[0] + width / 2.0 + width * 0.1);
                int y = (int) (point[1] + height / 2.0 - width * 0.1);

                //x = (int) ((point[0] - minX) / rangeX * width);
                //y = (int) ((point[1] - minY) / rangeY * height);
                //int z = (int) (point[2]);

                //System.out.println(minX + " " + maxX + " " + minY + " " + maxY);
                //System.out.println(rangeX + " " + rangeY);
                //System.out.println("x: " + x +  " y: " + y + " z: ");
                int size = 12;
                //g2d.setColor(getColorFromInt((int)point[3]));
                g2d.setColor(getColorFromViridisGradient((int)point[3], 0.75, 10000));
                //System.out.println(((int)point[4]));
                g2d.fillOval((int) (x - size), (int)(y - size), (int) size, (int) size);


            }


            angle += 45;
            if(angle > 360)
                angle = 45;

            drawExclamasionMark(g2d, plot_x - plotWidth  , plot_y + plotHeight + 150, Math.toRadians(0));




            // Add the frame to the GIF

            writer.writeToSequence(frame);

            if(false)
                if(counter == pointDataForAnimation.size())
                    for(int i = 0; i < 5; i++) {
                        writer.writeToSequence(frame);
                    }
        }
            // Close the GIF writer
            writer.close();
            output.close();

    }

    public List<double[]> sortListByThirdElement(List<double[]> list) {
        // Create a custom Comparator that compares the third element in each array
        Comparator<double[]> comparator = new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                // Compare the third element in each array
                return Double.compare(o1[2], o2[2]);
            }
        };

        // Sort the list using the custom Comparator
        Collections.sort(list, comparator);

        return list;
    }


    public static void drawExclamasionMark(Graphics2D g2d, double x, double y, double angleRadians) {
        Font font = new Font("Arial", Font.PLAIN, 200);
        g2d.setFont(font);

        String exclamationMark = "!";

        // Calculate the width and height of the exclamation mark
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(exclamationMark);
        int height = fm.getHeight();

        // Calculate the x and y coordinates for drawing the exclamation mark
        int drawX = (int) (x - (width / 2));
        int drawY = (int) (y + (height / 3));

        // Reset the AffineTransform to its default state
        g2d.setTransform(new AffineTransform());

        // Set the rotation
        g2d.rotate(angleRadians, x, y);

        // Draw the exclamation mark
        g2d.drawString(exclamationMark, drawX, drawY);
    }
    public static void drawQuestionMark(Graphics2D g2d, double x, double y, double angleRadians) {
        Font font = new Font("Arial", Font.PLAIN, 200);
        g2d.setFont(font);

        String questionMark = "?";

        // Calculate the width and height of the question mark
        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(questionMark);
        int height = fm.getHeight();

        // Calculate the x and y coordinates for drawing the question mark
        int drawX = (int) (x - (width / 2));
        int drawY = (int) (y + (height / 3));

        // Reset the AffineTransform to its default state
        g2d.setTransform(new AffineTransform());

        // Set the rotation
        g2d.rotate(angleRadians, x, y);

        // Draw the question mark
        g2d.drawString(questionMark, drawX, drawY);
    }


    public static Color getColorFromInt(int number) {
            int red = (number >> 16) & 0xFF;
            int green = (number >> 8) & 0xFF;
            int blue = number & 0xFF;
            return new Color(red, green, blue);
    }


    public static Color getColorFromViridisGradient(int value, double alpha, double maxValue) {
        // Normalize the value to a range between 0 and 1
        double normalizedValue = value / maxValue;

        normalizedValue = Math.min(1, normalizedValue);
        normalizedValue = Math.max(0, normalizedValue);

        // Define the viridis color map
        Color[] viridisColors = {
                new Color(68, 1, 84),
                new Color(72, 35, 116),
                new Color(64, 67, 135),
                new Color(52, 94, 141),
                new Color(41, 120, 142),
                new Color(32, 143, 140),
                new Color(35, 167, 132),
                new Color(62, 183, 118),
                new Color(101, 192, 99),
                new Color(142, 196, 76),
                new Color(182, 195, 54),
                new Color(220, 190, 39),
                new Color(253, 231, 37),
                new Color(255, 255, 109)
        };

        // Calculate the index of the viridis color based on the normalized value
        int colorIndex = (int) Math.floor(normalizedValue * (viridisColors.length - 1));

        colorIndex = Math.max(0, colorIndex);
        colorIndex = Math.min(viridisColors.length - 2, colorIndex);

        // Get the start and end colors for the current color segment
        Color startColor = viridisColors[colorIndex];
        Color endColor = viridisColors[colorIndex + 1];

        // Calculate the relative position within the current color segment
        double segmentPosition = (normalizedValue - (colorIndex / (double) (viridisColors.length - 1))) * (viridisColors.length - 1);

        // Interpolate between the start and end colors based on the relative position
        int red = (int) (startColor.getRed() + (endColor.getRed() - startColor.getRed()) * segmentPosition);
        int green = (int) (startColor.getGreen() + (endColor.getGreen() - startColor.getGreen()) * segmentPosition);
        int blue = (int) (startColor.getBlue() + (endColor.getBlue() - startColor.getBlue()) * segmentPosition);

        // Create and return the color
        return new Color(red, green, blue, (int) (alpha * 255));
    }
    public static Color getColorFromGradient(int value, double alpha) {
        // Normalize the value to a range between 0 and 1
        double normalizedValue = value / 40.0;

        // Define the gradient colors
        Color[] gradientColors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE};

        // Calculate the index of the gradient color based on the normalized value
        int colorIndex = (int) Math.floor(normalizedValue * (gradientColors.length - 1));

        // Get the start and end colors for the current color segment
        Color startColor = gradientColors[colorIndex];
        Color endColor = gradientColors[colorIndex + 1];

        // Calculate the relative position within the current color segment
        double segmentPosition = (normalizedValue - (colorIndex / (double) (gradientColors.length - 1))) * (gradientColors.length - 1);

        // Interpolate between the start and end colors based on the relative position
        int red = (int) (startColor.getRed() + (endColor.getRed() - startColor.getRed()) * segmentPosition);
        int green = (int) (startColor.getGreen() + (endColor.getGreen() - startColor.getGreen()) * segmentPosition);
        int blue = (int) (startColor.getBlue() + (endColor.getBlue() - startColor.getBlue()) * segmentPosition);

        // Create and return the color
        return new Color(red, green, blue, (int) (alpha * 255));
    }

    public static double[] multiplyMatrixVector(double[][] matrix, double[] vector) {
        int m = matrix.length;
        int n = vector.length;
        double[] result = new double[m];
        for (int i = 0; i < m; i++) {
            double sum = 0;
            for (int j = 0; j < n; j++) {
                sum += matrix[i][j] * vector[j];
            }
            result[i] = sum;
        }
        return result;
    }

    public static double[][] multiplyMatrices(double[][] matrix1, double[][] matrix2) {
        int m = matrix1.length;
        int n = matrix2[0].length;
        int o = matrix2.length;
        double[][] result = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                double sum = 0;
                for (int k = 0; k < o; k++) {
                    sum += matrix1[i][k] * matrix2[k][j];
                }
                result[i][j] = sum;
            }
        }
        return result;
    }


    public void rotatePoint(double[] point, double xRotation, double yRotation, double zRotation){

        double[] tmpPoint = new double[]{point[0], point[1], point[2]};

        double[][] xRotationMatrix = {
                { 1, 0, 0},
                { 0, Math.cos(xRotation), -Math.sin(xRotation)},
                { 0, Math.sin(xRotation), Math.cos(xRotation)},
        };

        double[][] yRotationMatrix = {
                { Math.cos(yRotation), 0, -Math.sin(yRotation)},
                { 0, 1, 0},
                { Math.sin(yRotation), 0, Math.cos(yRotation)},
        };

        double[][] zRotationMatrix = {
                { Math.cos(zRotation), -Math.sin(zRotation), 0},
                {  Math.sin(zRotation), Math.cos(zRotation), 0},
                {0, 0, 1},
        };

        double[][] multiplied = multiplyMatrices(multiplyMatrices(xRotationMatrix, yRotationMatrix), zRotationMatrix);

        double[] rotatedPoint = multiply(multiplied, tmpPoint);

        point[0] = rotatedPoint[0];
        point[1] = rotatedPoint[1];
        point[2] = rotatedPoint[2];

        //System.out.println(Arrays.toString(rotatedPoint));
    }

    public static double[] multiply(double[][] matrix, double[] vector) {
        return Arrays.stream(matrix)
                .mapToDouble(row -> IntStream.range(0, row.length)
                        .mapToDouble(col -> row[col] * vector[col])
                        .sum())
                .toArray();
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

    public void prepare(){

        HashSet<Integer> removeThese = new HashSet<Integer>();

        for(int i : searchSpace_map.keySet()){
            if(searchSpace_map.get(i).getPlotID() == currentPlotId){
                removeThese.add(i);
            }
        }

        for(int i : removeThese){
            searchSpace_map.remove(i);
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

    public void undoSwap(HashMap<Integer, forestTree> plotTrees, HashMap<Integer, forestTree> treeBank){

        for(int i : add_these){

            //System.out.println("remove");
            forestTree temp = plotTrees.get(i);

            if(!temp.hasCrown){
                throw new toolException("tree does not have crown! " + temp.getTreeID_unique() + " " + temp.getTreeITCid());
            }
            plotTrees.remove(i);
            //removed1++;

            treeBank.put(temp.getTreeID_unique(), temp);
            //added2++;

            for(int j = 0; j < temp.treesBeneath.size(); j++){

                if(!plotTrees.containsKey(temp.treesBeneath.get(j).getTreeID_unique())){
                    throw new toolException("tree not in plot! " + temp.treesBeneath.get(j));
                }

                //System.out.println("tree beneath remove!");
                plotTrees.remove(temp.treesBeneath.get(j).getTreeID_unique());
                if(temp.treesBeneath.get(j).hasCrown){
                    throw new toolException("tree has a crown when it should not have one!");
                }
                //emove_these.add(temp.treesBeneath.get(j).getTreeID_unique());
                //removed1++;
                //added2++;


                treeBank.put(temp.treesBeneath.get(j).getTreeID_unique(), temp.treesBeneath.get(j));
                //System.out.println(treeBank.size());
            }
            //
        }

        for(int i : remove_these){

            //System.out.println(i);
            forestTree temp = treeBank.get(i);

            if(!temp.hasCrown){
                throw new toolException("tree does not have crown! " + temp.getTreeID_unique() + " " + temp.getTreeITCid());
            }

            treeBank.remove(temp.getTreeID_unique());
            //removed2++;

            plotTrees.put(temp.getTreeID_unique(), temp);
            //added1++;

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
                //removed2++;
                //added1++;
                plotTrees.put(temp.treesBeneath.get(j).getTreeID_unique(), temp.treesBeneath.get(j));

            }
            //System.out.println("-----------------");

        }


    }
    public int swapStuff(HashMap<Integer, forestTree> plotTrees, HashMap<Integer, forestTree> treeBank, double switchProbability, int swap_this_many){

        //System.out.println( "treeBank.size: " + treeBank.size());
        //System.out.println( "plotTrees.size: " + plotTrees.size());
        int total_size = plotTrees.size() + treeBank.size();

        HashSet<Integer> alreadySwitched = new HashSet<Integer>();
        int switchIndex = 0;

        ArrayList<Integer> ind = new ArrayList<Integer>(plotTrees.keySet());
        Collections.shuffle(ind, r);
        ArrayList<forestTree> all_trees = new ArrayList<forestTree>(treeBank.values());

        remove_these = new ArrayList<>();
        add_these = new ArrayList<>();

        int n_switches = 0;
        //for(int i : plotTrees.keySet()){
        for(int i : ind){

            if(plotTrees.get(i).hasCrown){

                //System.out.println(plotTrees.get(i).maxDBH + " " + this.maxValue);


                //if(r.nextDouble() < switchProbability){
                    switchIndex = r.nextInt(all_trees.size());


                    int loops = 0;
                    boolean failed = false;

                    while(alreadySwitched.contains(switchIndex) || !all_trees.get(switchIndex).hasCrown){
                     //|| calculateRelativeDifference(all_trees.get(switchIndex).getTreeCrownArea(), plotTrees.get(i).treeCrownArea) > 0.1){
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
       // System.out.println("HERE!!");

        for(int i : remove_these){

            //System.out.println("remove");
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
                    throw new toolException("tree not in plot! " + temp.treesBeneath.get(j));
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
        if(true)
            if(size_after != total_size){

                throw new toolException("size after is not equal to size before!");
            }

        return n_switches;
    }

    public static double calculateRelativeDifference(double x, double y) {
        double max = Math.max(Math.abs(x), Math.abs(y));
        return max == 0.0 ? 0.0 : Math.abs(x - y) / max;
    }

    public boolean accept(double t, double oldCost, double newCost){

        double e = Math.exp(1);
        double random = r.nextDouble();
        double powi = ((oldCost - newCost) / t);
        double a = Math.pow(e, powi);

        //if(t > 0.5)
        System.out.println("acceptance probability: " + a + " oldCost: " + oldCost + " newCost: " + newCost + " t: " + t);
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
    public ArrayList<int[]> calculateSpeciesSpecificHeightDistributions(HashMap<Integer, forestTree> plot){

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
                throw new toolException("species not recognized!");
        }

        ArrayList<int[]> speciesSpecificDiameterDistributions = new ArrayList<>();
        speciesSpecificDiameterDistributions.add(diameterDistribution_pine);
        speciesSpecificDiameterDistributions.add(diameterDistribution_spruce);
        speciesSpecificDiameterDistributions.add(diameterDistribution_decid);

        return speciesSpecificDiameterDistributions;

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

    public double sumOfArray(double[] array){
        double sum = 0;
        for(int i = 0; i < array.length; i++){
            sum += array[i];
        }
        return sum;
    }

    public static double relativeDifference(double a, double b) {
        double diff = Math.abs(a - b);
        double avg = (Math.abs(a) + Math.abs(b)) / 2.0;
        return diff / avg;
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
    public double costWithEMD(ArrayList<int[]> histogram1, ArrayList<int[]> histogram2){

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


    public static double bhattacharyyaDistance(int[] h1, int[] h2) {



        double[] h1Normalized = normalize(h1);
        double[] h2Normalized = normalize(h2);
        double bCoeff = 0.0;
        for (int i = 0; i < h1Normalized.length; i++) {
            bCoeff += Math.sqrt(h1Normalized[i] * h2Normalized[i]);
        }

        if (bCoeff == 0.0) {
            return Double.MAX_VALUE;
        } else {
            return -Math.log(bCoeff);
        }

    }

    // Normalize a histogram so that its values sum up to 1
    public static double[] normalize(int[] histogram) {
        int sum = Arrays.stream(histogram).sum();
        double[] normalized = new double[histogram.length];
        for (int i = 0; i < histogram.length; i++) {
            normalized[i] = (double) histogram[i] / sum;
        }
        return normalized;
    }

    // Calculate the similarity between two histograms using the Bhattacharyya distance
    public static double bhattacharyyaSimilarity(ArrayList<int[]> histogram1, ArrayList<int[]> histogram2) {

        double costSum = 0;

        int[] h1 = new int[histogram1.get(0).length * histogram1.size()];
        int[] h2 = new int[histogram1.get(0).length * histogram1.size()];

        int counter = 0;

        for(int i = 0; i < histogram1.size(); i++){

            for(int j = 0; j < histogram1.get(i).length; j++){
                h1[counter] = histogram1.get(i)[j];
                h2[counter] = histogram2.get(i)[j];
                counter++;
            }

        }

        double bd = bhattacharyyaDistance(h1, h2);
        return Math.exp(-bd/2.0);
    }

    public double calculateCrownArea(HashMap<Integer, forestTree> plot){

        double area = 0;

        for(int i : plot.keySet()){
            area += plot.get(i).getTreeCrownArea();
        }

        return area;
    }

    public double[] calculateSpeciesSpecificVolumes(HashMap<Integer, forestTree> plot, double area){

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

    public List<List<Double>> outliersModifiedZScore(HashMap<Integer, forestTree> trees, double threshold) {

        List<Double> outliers = new ArrayList<>();

        if(trees.size() == 0)
            return null;

        ArrayList<Double> values_ = new ArrayList<>();
        for (int i : trees.keySet()) {
            values_.add(trees.get(i).getTreeVolume());
        }

        Collections.sort(values_, Collections.reverseOrder());

        int n_trees = (int)(values_.size() * 0.1);

        if(n_trees > 20)
            n_trees = 20;

        if(n_trees < 5)
            n_trees = 5;

        double[] values = new double[n_trees];

        int counter = 0;
        for (double d : values_) {
            values[counter++] = d;
            if(counter >= n_trees)
                break;
        }

        double[] modifiedZScores = calculateModifiedZScores(values);
        List<Double> z_scores = new ArrayList<>();

        for(int i = 0; i < modifiedZScores.length; i++) {
            if (modifiedZScores[i] > threshold){
                outliers.add(values[i]);
            }
            z_scores.add(modifiedZScores[i]);
        }

        //System.out.println(Arrays.toString(values));
        List<List<Double>> output = new ArrayList<>();
        output.add(outliers);

        output.add(z_scores);
        return output;

    }

    public static double[] calculateModifiedZScores(double[] data) {
        int n = data.length;
        double[] zScores = new double[n];

        // Calculate median absolute deviation (MAD)
        double median = calculateMedian(data);
        double[] absoluteDeviations = new double[n];
        for (int i = 0; i < n; i++) {
            absoluteDeviations[i] = Math.abs(data[i] - median);
        }
        double mad = calculateMedian(absoluteDeviations) * 1.4826; // Scaling factor for normal distribution

        // Calculate Modified Z-score for each data point
        for (int i = 0; i < n; i++) {
            double modifiedZScore = 0.6745 * (data[i] - median) / mad; // Scaling factor for normal distribution
            zScores[i] = Math.abs(modifiedZScore);
        }

        return zScores;
    }

    private static double calculateMedian(double[] data) {
        int n = data.length;
        Arrays.sort(data);
        if (n % 2 == 0) {
            return (data[n/2-1] + data[n/2]) / 2.0;
        } else {
            return data[n/2];
        }
    }

    public static List<Double> detectOutliersGrubbs(HashMap<Integer, forestTree> trees, double alpha) {
        List<Double> outliers = new ArrayList<>();

        ArrayList<Double> values_ = new ArrayList<>();
        for(int i : trees.keySet()){
            values_.add(trees.get(i).getTreeHeight());
        }
        Collections.sort(values_, Collections.reverseOrder());


        int n_trees = (int)(values_.size() * 0.2);

        double[] values = new double[n_trees];

        int n = values.length;
        double mean = 0;
        int counter = 0;

        for (double d : values_) {
            mean += d;
            values[counter++] = d;
            if(counter >= n_trees)
                break;
        }



        System.out.println(Arrays.toString(values));
        mean /= n;

        double stdDev = 0;
        for (double value : values) {
            stdDev += Math.pow(value - mean, 2);
        }

        stdDev = Math.sqrt(stdDev / (n - 1));

        double criticalValue = Math.sqrt((tDistribution(n - 2, alpha / (2 * n))) * (n - 1) / (n * tDistribution(n - 1, alpha / (2 * n))));

        double Gmax = 0;
        int idx = -1;
        for (int i = 0; i < n; i++) {
            double Gi = Math.abs(values[i] - mean) / stdDev;
            if (Gi > Gmax) {
                Gmax = Gi;
                idx = i;
            }
        }

        if (Gmax > criticalValue) {
            outliers.add(values[idx]);
        }

        return outliers;
    }


    public static List<Double> detectOutliersGrubbs(double[] values, double alpha) {
        List<Double> outliers = new ArrayList<>();

        int n = values.length;
        double mean = 0;
        for (double value : values) {
            mean += value;
        }
        mean /= n;

        double stdDev = 0;
        for (double value : values) {
            stdDev += Math.pow(value - mean, 2);
        }
        stdDev = Math.sqrt(stdDev / (n - 1));

        double criticalValue = Math.sqrt((tDistribution(n - 2, alpha / (2 * n))) * (n - 1) / (n * tDistribution(n - 1, alpha / (2 * n))));

        double Gmax = 0;
        int idx = -1;
        for (int i = 0; i < n; i++) {
            double Gi = Math.abs(values[i] - mean) / stdDev;
            if (Gi > Gmax) {
                Gmax = Gi;
                idx = i;
            }
        }

        if (Gmax > criticalValue) {
            outliers.add(values[idx]);
        }

        return outliers;
    }

    private static double tDistribution(int df, double alpha) {
        return Math.abs(new TDistribution(df).inverseCumulativeProbability(alpha));
    }

}


class Projection {
    private double[][] projectionMatrix; // the projection matrix

    public Projection() {
        double k0 = 0.9996; // scale factor
        double falseEasting = 500000; // false easting
        double falseNorthing = 0; // false northing

        // Define rotation angle in radians
        // Define rotation angles in radians
        double rotationAngleX = Math.PI / 4; // 45 degrees around x-axis
        double rotationAngleY = Math.PI / 4; // 45 degrees around y-axis
        double rotationAngleZ = Math.PI / 4;           // no rotation around z-axis

// Define rotation matrices
        double[][] rotationMatrixX = new double[][] {
                { 1, 0, 0 },
                { 0, Math.cos(rotationAngleX), -Math.sin(rotationAngleX) },
                { 0, Math.sin(rotationAngleX), Math.cos(rotationAngleX) }
        };
        double[][] rotationMatrixY = new double[][] {
                { Math.cos(rotationAngleY), 0, Math.sin(rotationAngleY) },
                { 0, 1, 0 },
                { -Math.sin(rotationAngleY), 0, Math.cos(rotationAngleY) }
        };
        double[][] rotationMatrixZ = new double[][] {
                { Math.cos(rotationAngleZ), -Math.sin(rotationAngleZ), 0 },
                { Math.sin(rotationAngleZ), Math.cos(rotationAngleZ), 0 },
                { 0, 0, 1 }
        };

// Combine rotation matrices
        double[][] rotationMatrix = multiplyMatrices(multiplyMatrices(rotationMatrixX, rotationMatrixY), rotationMatrixZ);

// Define rotation matrix


        projectionMatrix = new double[][] {
                { k0, 0, falseEasting },
                { 0, -k0, falseNorthing },
                { 0, 0, 1 }
        };


        // construct the projection matrix
        projectionMatrix = new double[][] {
                { k0 * rotationMatrix[0][0], k0 * rotationMatrix[0][1], falseEasting },
                { -k0 * rotationMatrix[1][0], -k0 * rotationMatrix[1][1], falseNorthing },
                { 0, 0, 1 }
        };

    }

    public List<double[]> project(List<double[]> vertices) {
        List<double[]> projectedVertices = new ArrayList<>();
        for (double[] vertex : vertices) {
            double[] projectedVertex = new double[2];
            projectedVertex[0] = projectionMatrix[0][0] * vertex[0] + projectionMatrix[0][1] * vertex[1] + projectionMatrix[0][2];
            projectedVertex[1] = projectionMatrix[1][0] * vertex[0] + projectionMatrix[1][1] * vertex[1] + projectionMatrix[1][2];

        }
        return projectedVertices;
    }
}


class ProjectionP {
    private double[][] projectionMatrix; // the projection matrix

    public ProjectionP() {

        double fov = Math.toRadians(60); // field of view in degrees
        double aspectRatio = 1.0; // aspect ratio of the projection plane
        double near = 0.1; // distance to the near clipping plane
        double far = 100.0; // distance to the far clipping plane

        double xRotation = Math.toRadians(45);
        double yRotation = Math.toRadians(45);


        double f = 1.0 / Math.tan(fov / 2.0);
        double zRange = far - near;

        projectionMatrix = new double[][] {
                { f / aspectRatio, 0, 0, 0 },
                { 0, f, 0, 0 },
                { 0, 0, -(far + near) / zRange, -(2 * far * near) / zRange },
                { 0, 0, -1, 0 }
        };

        double[][] xRotationMatrix = {
                { 1, 0, 0},
                { 0, Math.cos(xRotation), -Math.sin(xRotation)},
                { 0, Math.sin(xRotation), Math.cos(xRotation)},
        };

        double[][] yRotationMatrix = {
                { Math.cos(yRotation), 0, -Math.sin(yRotation)},
                { 0, 1, 0},
                { Math.sin(yRotation), 0, Math.cos(yRotation)},
        };

        double[][] zRotationMatrix = {
                { Math.cos(yRotation), -Math.sin(yRotation), 0},
                {  Math.sin(yRotation), Math.cos(yRotation), 0},
                {0, 0, 1},
        };


        // combine the rotation matrices
        double[][] rotationMatrix = multiplyMatrices(yRotationMatrix, xRotationMatrix);
/*
        projectionMatrix = multiplyMatrices(new double[][] {
                { f / aspectRatio, 0, 0, 0 },
                { 0, f, 0, 0 },
                { 0, 0, -(far + near) / zRange, -(2 * far * near) / zRange },
                { 0, 0, -1, 0 }
        }, rotationMatrix);
*/

    }

    public List<double[]> project(List<double[]> vertices) {
        List<double[]> projectedVertices = new ArrayList<>();
        for (double[] vertex : vertices) {

            System.out.println("pre: " + vertex[0] + " " + vertex[1] + " " + vertex[2]);
            double[] projectedVertex = new double[2];
            double w = projectionMatrix[3][0] * vertex[0] + projectionMatrix[3][1] * vertex[1] + projectionMatrix[3][2] * vertex[2] + projectionMatrix[3][3];
            projectedVertex[0] = (projectionMatrix[0][0] * vertex[0] + projectionMatrix[0][1] * vertex[1] + projectionMatrix[0][2] * vertex[2] + projectionMatrix[0][3]) / w;
            projectedVertex[1] = (projectionMatrix[1][0] * vertex[0] + projectionMatrix[1][1] * vertex[1] + projectionMatrix[1][2] * vertex[2] + projectionMatrix[1][3]) / w;
            projectedVertices.add(projectedVertex);
            System.out.println("after: " + Arrays.toString(projectedVertex));


        }
        return projectedVertices;
    }
}

