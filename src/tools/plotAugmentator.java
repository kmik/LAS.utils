package tools;

import err.toolException;
import org.gdal.ogr.*;
import utils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class plotAugmentator {

    public argumentReader aR;

    public HashMap<Integer, forestPlot> targets = new HashMap<>();
    public HashMap<Integer, forestPlot> plots = new HashMap<Integer, forestPlot>();
    public HashMap<Integer, forestTree> trees = new HashMap<Integer, forestTree>();
    public HashMap<Integer, forestTree> itc_with_tree = new HashMap<Integer, forestTree>();
    public HashMap<Integer, forestTree> itc_with_tree_unique_id = new HashMap<Integer, forestTree>();

    public HashMap<Integer, double[]> ITChasNoMatch = new HashMap<>();
    public HashMap<Integer, double[][]> ITChasNoMatchBoundaries = new HashMap<>();
    //HashMap<Integer, double[][]> treeBounds = new HashMap<>();
    public ArrayList<double[][]> treeBounds = new ArrayList<>();

    public HashMap<Integer, forestTree> ITC_id_to_tree_id = new HashMap<>();

    public ArrayList<double[]> plotTargets = new ArrayList<>();

    public plotAugmentator(argumentReader aR){

        this.aR = aR;

    }

    public void readMatchedTrees(File matchedTreesFile) throws IOException {

        BufferedReader sc = new BufferedReader( new FileReader(matchedTreesFile));
        sc.readLine();
        String line1;
        while((line1 = sc.readLine())!= null) {

            String[] line  = line1.split("\t");
            //System.out.println(Arrays.toString(line));

            int plot_id = (int)Double.parseDouble(line[line.length - 5]);

            int tree_id = (int)Double.parseDouble(line[line.length - 4]);
            int tree_id_ITC = (int)Double.parseDouble(line[line.length - 13]);


            if(plots.containsKey(plot_id))
            if(tree_id > 0 && plots.get(plot_id).trees.containsKey(tree_id)) {


                double tree_x = Double.parseDouble(line[line.length - 10]);
                double tree_y = Double.parseDouble(line[line.length - 9]);


                if(true)
                if(!plots.get(plot_id).pointInPlot(new double[]{tree_x, tree_y})){

                    /*
                    if(!trees.containsKey(tree_id_ITC)){
                        throw new toolException("Tree " + tree_id_ITC + " not found in trees list");
                    }
                    trees.remove(tree_id_ITC);
                    plots.get(plot_id).trees.remove(tree_id);

*/
                    //continue;
                }

/*
                if(tree_id_ITC == 6905){

                    System.out.println(tree_x + " " + tree_y);
                    System.exit(1);
                }


*/

                plots.get(plot_id).hasITCid(tree_id);
                itc_with_tree.put(tree_id_ITC, plots.get(plot_id).getTree(tree_id));
                itc_with_tree_unique_id.put(plots.get(plot_id).getTree(tree_id).getTreeID_unique(), plots.get(plot_id).getTree(tree_id));
                plots.get(plot_id).getTree(tree_id).setTreeITCid((int) Double.parseDouble(line[line.length - 13]));

                ITC_id_to_tree_id.put(plots.get(plot_id).getTree(tree_id).getTreeITCid(), plots.get(plot_id).getTree(tree_id));

                plots.get(plot_id).getTree(tree_id).setTreeHeight_ITC( Double.parseDouble(line[line.length - 8]));
                plots.get(plot_id).getTree(tree_id).setTreeX_ITC((Double.parseDouble(line[line.length - 10])));
                plots.get(plot_id).getTree(tree_id).setTreeY_ITC((Double.parseDouble(line[line.length - 9])));
                plots.get(plot_id).getTree(tree_id).setTreeCrownArea( Double.parseDouble(line[line.length - 12]));


            }else{

                //plots.get new double[]{(int) Double.parseDouble(line[line.length - 10]), (int) Double.parseDouble(line[line.length - 9])
                //TODO NOTHING DONE WITH THESE!?!?!?!!? Causes problems. major problems

                plots.get(plot_id).ITCHasNoMatch((int) Double.parseDouble(line[line.length - 13]));

                this.ITChasNoMatch.put((int) Double.parseDouble(line[line.length - 13]), new double[]{(int) Double.parseDouble(line[line.length - 10]), (int) Double.parseDouble(line[line.length - 9])});

                System.out.println("this.ITChasNoMatch.size() " + this.ITChasNoMatch.size());
            }
        }


        for(int plot : plots.keySet()){

        }

        //System.out.println(itc_with_tree_unique_id.size());
        //System.exit(1);

    }

    public void preparePlots(){

        // Remove half at random

        for(int plot : plots.keySet()){


            System.out.println("Preparing plot " + plot);
            plots.get(plot).preparePlot();
        }

        //System.exit(1);
    }

    public void replicatePlots(){

        int maxTreeUniqueId = 0;
        int maxITCId = 0;

        for(int i : this.trees.keySet()){

            if(this.trees.get(i).getTreeID_unique() > maxTreeUniqueId){
                maxTreeUniqueId = this.trees.get(i).getTreeID_unique();
            }

            if(this.trees.get(i).hasCrown){
                if(this.trees.get(i).getTreeITCid() > maxITCId){
                    maxITCId = this.trees.get(i).getTreeITCid();
                }
            }
        }

        int maxPlotId = 0;



        for(int i : this.plots.keySet()){

            if(this.plots.get(i).getPlotID() > maxPlotId){
                maxPlotId = this.plots.get(i).getPlotID();
            }

        }

        int counter = 1;

        int n_replicates = 3;

        ArrayList<forestPlot> replicatedPlots = new ArrayList<>();

        for(int n = 0; n < n_replicates; n++) {


            for (int plot : plots.keySet()) {

                forestPlot replicated_ = plots.get(plot).replicate(maxTreeUniqueId * counter, maxITCId * counter, maxPlotId * counter);

                for (int i : replicated_.trees.keySet()) {

                    this.trees.put(replicated_.trees.get(i).getTreeID_unique(), replicated_.trees.get(i));

                    if (replicated_.trees.get(i).hasCrown) {
                        this.ITC_id_to_tree_id.put(replicated_.trees.get(i).getTreeITCid(), replicated_.trees.get(i));
                        this.itc_with_tree.put(replicated_.trees.get(i).getTreeITCid(), replicated_.trees.get(i));
                        itc_with_tree_unique_id.put(replicated_.trees.get(i).getTreeID_unique(), replicated_.trees.get(i));
                    }
                }


                replicated_.setPlotAugmentator(this);

                replicatedPlots.add(replicated_);

            }
            counter++;
            //System.out.println("trees: " + this.trees.size());
            //System.out.println("ITC_id_to_tree_id: " + this.ITC_id_to_tree_id.size());
        }

        for (forestPlot p : replicatedPlots) {
            this.plots.put(p.getPlotID(), p);
        }

        for(int i : plots.keySet()){

            //System.out.println(plots.get(i).getPlotID() + " " + plots.get(i).trees.size() + " maxplotid " + maxPlotId);

        }

        //System.exit(1);

    }

    public void readMeasuredTrees(File measuredTreesFile) throws IOException {

        BufferedReader sc = new BufferedReader( new FileReader(measuredTreesFile));
        sc.readLine();
        String line1;

        while((line1 = sc.readLine())!= null) {

            String[] line  = line1.split(",");

            double tree_x = Double.parseDouble(line[3]);
            double tree_y = Double.parseDouble(line[4]);


            forestTree tmpTree = new forestTree();

            tmpTree.setTreeLineFromFieldData(line1);
            tmpTree.setTreeLineFromFieldData_delimited((String[])line.clone());
            tmpTree.setTreeID(Integer.parseInt(line[1]));
            tmpTree.setTreeID_unique(Integer.parseInt(line[0]));

            trees.put(tmpTree.getTreeID_unique(), tmpTree);
            //tmpTree.setTreeCrownBounds(treeBounds.get(tmpTree.getTreeID_unique()));

            //print2DArray(treeBounds.get(tmpTree.getTreeID_unique()));

            tmpTree.setTreeX(Double.parseDouble(line[3]));
            tmpTree.setTreeY(Double.parseDouble(line[4]));
            tmpTree.setTreeHeight(Double.parseDouble(line[11]));

            int species = Integer.parseInt(line[7]);

            species = Math.min(species, 3) - 1;

            tmpTree.setTreeSpecies(species);
            tmpTree.setTreePlotID(Integer.parseInt(line[1]));

            tmpTree.setTreeDBH(Double.parseDouble(line[9]));

            tmpTree.setTreeVolume(Double.parseDouble(line[42]));

            if(true)
            if(Integer.parseInt(line[2]) > 0){
                if(!plots.containsKey(Integer.parseInt(line[2]))) {
                    System.out.println("Plot " + line[2] + " not found in shapefile");
                    //throw new toolException("Plot " + line[2] + " not found in shapefile");
                }else
                    plots.get(Integer.parseInt(line[2])).addTree(tmpTree);

            }

            if(false)
            if(Integer.parseInt(line[2]) > 0){


                for(int plot : plots.keySet()){

                    if(plots.get(plot).pointInPlot(new double[]{tree_x, tree_y})){
                        plots.get(plot).addTree(tmpTree);
                    }

                }
            }
        }

    }

    public void readSmallerPolygon(String[] shapeFile){

        DataSource ds = ogr.Open( shapeFile[0] );

        if( ds == null ) {
            System.out.println( "Opening plot file failed." );
            System.exit( 1 );
        }

        Layer shapeFileLayer = ds.GetLayer(0);

        for(long i = 0; i < shapeFileLayer.GetFeatureCount(); i++ ) {

            Feature tempF = shapeFileLayer.GetFeature(i);
            Geometry tempG = tempF.GetGeometryRef();
            Geometry tempG2 = tempG.GetGeometryRef(0);

            if(tempG == null)
                continue;

            if(plots.containsKey(tempF.GetFieldAsInteger(0))) {
                plots.get(tempF.GetFieldAsInteger(0)).setSmallerBounds(tempG2.GetPoints());
                plots.get(tempF.GetFieldAsInteger(0)).setSmallerArea(tempG2.GetArea());
                System.out.println("Smaller area: " + tempG2.GetArea());
            }
            //System.exit(1);

        }

    //System.exit(1);

    }
    public void readShapeFiles(String shapeFile, String[] shapeFile2) throws IOException {

        DataSource ds = ogr.Open( shapeFile );
        //DataSource ds2 = ogr.Open( shapeFile2 );

        if( ds == null ) {
            System.out.println( "Opening plot file failed." );
            System.exit( 1 );
        }

        Layer shapeFileLayer = ds.GetLayer(0);

        long featureCount = shapeFileLayer.GetFeatureCount();
        List<Long> indices = new ArrayList<>();

        for (long i = 0; i < featureCount; i++) {
            indices.add(i);
        }

        Collections.shuffle(indices);  // Randomize order
        int limit = (int) (featureCount / 1);

        for (int j = 0; j < limit; j++) {
            long i = indices.get(j);
            Feature tempF = shapeFileLayer.GetFeature(i);
            Geometry tempG = tempF.GetGeometryRef();

            if (tempG == null)
                continue;

            Geometry tempG2 = tempG.GetGeometryRef(0);
            int plotId = tempF.GetFieldAsInteger(0);

            plots.put(plotId, new forestPlot(plotId, tempG2.GetPoints(), this));
            plots.get(plotId).setArea(tempG2.GetArea());
        }

        /*
        for(long i = 0; i < shapeFileLayer.GetFeatureCount(); i++ ) {

            // THIS IS HOW TO REDUCE THE NUMBER OF PLOTS
            //if( i % 2 == 0 || false) {
            //if (i % 4 < 3) {
            if (i % 2 == 0 ) {
                Feature tempF = shapeFileLayer.GetFeature(i);
                Geometry tempG = tempF.GetGeometryRef();
                Geometry tempG2 = tempG.GetGeometryRef(0);

                if (tempG == null)
                    continue;

                plots.put(tempF.GetFieldAsInteger(0), new forestPlot(tempF.GetFieldAsInteger(0), tempG2.GetPoints(), this));

                plots.get(tempF.GetFieldAsInteger(0)).setArea(tempG2.GetArea());
                //print2DArray(plots.get(tempF.GetFieldAsInteger(0)).getPlotBounds());
                //System.exit(1);
            }
        }
        */


        int numberOfSegments = 0;

        double[] searchThisPoint = new double[]{606073, 6927864};

        Set<Integer> keySet = plots.keySet();

        for(Integer key : keySet){
            if(plots.get(key).pointInPlot(searchThisPoint)){
                System.out.println("Found plot " + key);
            }
        }
    }

    public void readITCPolygons(String[] shapeFile2) throws IOException {

        DataSource ds;

        Layer shapeFileLayer;

        int numberOfSegments = 0;

        int matched_trees = 0;
        for(int i = 0; i < shapeFile2.length; i++){

            //System.out.println(shapeFile2[i]);
            ds = ogr.Open( shapeFile2[i] );


            if( ds == null ) {
                System.out.println( "Opening plot ITC shapefile failed." );
                System.exit( 1 );
            }

            shapeFileLayer = ds.GetLayer(0);


            for(long j = 0; j < shapeFileLayer.GetFeatureCount(); j++ ) {

                Feature tempF = shapeFileLayer.GetFeature(j);


                Geometry tempG = tempF.GetGeometryRef();
                Geometry tempG2 = tempG.GetGeometryRef(0);

                //treeBounds.put(tempF.GetFieldAsInteger(0), tempG2.GetPoints().clone());

                if(tempF.GetFieldAsInteger(0) == 6905){

                    System.out.println("FOUND: " + ITC_id_to_tree_id.containsKey(tempF.GetFieldAsInteger(0)));
                    //System.exit(1);
                }

                if(ITC_id_to_tree_id.containsKey(tempF.GetFieldAsInteger(0))) {
                    //System.out.println(tempF.GetFieldAsInteger(0));
                    ITC_id_to_tree_id.get(tempF.GetFieldAsInteger(0)).setTreeCrownBounds(tempG2.GetPoints());

                    plots.get(ITC_id_to_tree_id.get(tempF.GetFieldAsInteger(0)).getPlotID()).setPlotLASFile(
                            new File(shapeFile2[i].split("_TreeSegmentation.shp") [0] + "_ITD.las")
                    );
                    //trees.get(ITC_id_to_tree_id.get(tempF.GetFieldAsInteger(0))).setTreeCrownBounds(tempG2.GetPoints());
                    matched_trees++;
                }

                if(this.ITChasNoMatch.containsKey(tempF.GetFieldAsInteger(0))){

                    System.out.println("ITC " + tempF.GetFieldAsInteger(0) + " has no match");

                    ITChasNoMatchBoundaries.put(tempF.GetFieldAsInteger(0),  clone2DArray(tempG2.GetPoints()));

                }



            }


        }

        //System.out.println("Matched " + matched_trees + " trees");

        for(int tree : trees.keySet()){
            if(trees.get(tree).getTreeCrownBounds() != null){

                if(trees.get(tree).getTreeHeight_ITC() <= 0)
                    throw new toolException("Tree " + tree + " has ITC match, but has negative height " + trees.get(tree).getTreeHeight_ITC() + " " + trees.get(tree).getPlotID());
            }
        }


    }

    public static double[][] clone2DArray(double[][] original) {
        int rows = original.length;
        double[][] clone = new double[rows][];

        for (int i = 0; i < rows; i++) {
            int columns = original[i].length;
            clone[i] = new double[columns];
            System.arraycopy(original[i], 0, clone[i], 0, columns);
        }

        return clone;
    }
    public static void print2DArray(double[][] arr) {
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                System.out.print(arr[i][j] + " ");
            }
            System.out.println();
        }
    }

    public void readTargets(File in){

        int counter = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(in));
            String line = br.readLine();
            String[] split = line.split(aR.sep);

            while ((line = br.readLine()) != null) {
                split = line.split(aR.sep);

                int plotID = Integer.parseInt(split[9]);
                if(!targets.containsKey(plotID))
                    targets.put(plotID, new forestPlot(plotID));

                forestTree tmpTree = new forestTree();
                tmpTree.setTreeDBH(Double.parseDouble(split[2]));

                int treeSpecies = Integer.parseInt(split[3]);

                treeSpecies = Math.min(3, treeSpecies);
                treeSpecies -= 1;

                tmpTree.setTreeSpecies(treeSpecies);
                tmpTree.setTreeID(counter);
                tmpTree.setTreeID_unique(counter);
                tmpTree.setTreeVolume(Double.parseDouble(split[38]));
                targets.get(plotID).addTree(tmpTree);

                counter++;
                //System.out.println(targets.get(plotID).trees.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Read " + targets.size() + " target plots");
        System.out.println("Read " + counter + " target trees");
        ArrayList<Integer> removeSmall = new ArrayList<Integer>();

        for(int i : targets.keySet()){
            //System.out.println(targets.get(i).totalVolue * (10000 / targets.get(i).train_area) / 1000);
            if(targets.get(i).totalVolue * (10000 / targets.get(i).train_area) / 1000 < 50){
                removeSmall.add(i);
            }
        }
        for(int i : removeSmall){
            //targets.remove(i);
        }
        //System.exit(1);
    }


    public void readHistogram(File histogramFile, File speciesProportionsFile){

        // Read files line by line

        try {
            BufferedReader sc = new BufferedReader(new FileReader(histogramFile));
            //sc.readLine();
            String line1;

            BufferedReader sc2 = new BufferedReader(new FileReader(speciesProportionsFile));
            //sc.readLine();
            String line2;


            while ((line1 = sc.readLine()) != null) {

                line2 = sc2.readLine();
                String[] line = line1.split(" ");
                String[] line2split = line2.split(" ");


                System.out.println("Volume: " + Integer.parseInt(line[0]) + " " + Integer.parseInt(line[1]));


                if(Integer.parseInt(line[1]) == 0)
                    continue;

                double average_pine = Double.parseDouble(line2split[0]);
                double average_spruce = Double.parseDouble(line2split[1]);
                double average_birch = Double.parseDouble(line2split[2]);

                double sd_pine = Double.parseDouble(line2split[3]);
                double sd_spruce = Double.parseDouble(line2split[4]);
                double sd_birch = Double.parseDouble(line2split[5]);

                double volumeBinCenter = Double.parseDouble(line[0]) + 10;
                int countInBin = Integer.parseInt(line[1]);

                countInBin *= 10;

                for(int i_ = 0; i_ < 1; i_++)
                for(int i = 0; i < countInBin; i++){

                    double prop_pine = randomDouble(average_pine, sd_pine);
                    double prop_spruce = randomDouble(average_spruce, sd_spruce);
                    double prop_birch = randomDouble(average_birch, sd_birch);

                    prop_pine = Math.max(0, prop_pine);
                    prop_pine = Math.min(1, prop_pine);

                    prop_spruce = Math.max(0, prop_spruce);
                    prop_spruce = Math.min(1, prop_spruce);

                    prop_birch = Math.max(0, prop_birch);
                    prop_birch = Math.min(1, prop_birch);


                    double[] proportions = new double[]{prop_pine, prop_spruce, prop_birch};

                    proportions = modifyDoubles(prop_pine, prop_spruce, prop_birch);

                    double volume = getRandomDoubleInRange(volumeBinCenter - 9.9, volumeBinCenter + 9.9);

                    double[] volumes = new double[]{volume * proportions[0], volume * proportions[1], volume * proportions[2]};

                    plotTargets.add(new double[]{volumes[0], volumes[1], volumes[2]});

                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        for(int i = 0; i < plotTargets.size(); i++){
            System.out.println(plotTargets.get(i)[0] + " " + plotTargets.get(i)[1] + " " + plotTargets.get(i)[2]);
        }

        //System.exit(1);

    }

    public static double getRandomDoubleInRange(double min, double max) {
        Random r = new Random();
        return min + (max - min) * r.nextDouble();
    }

    public static double[] modifyDoubles(double d1, double d2, double d3) {
        double[] result = new double[3];
        double sum = d1 + d2 + d3;
        result[0] = d1 / sum;
        result[1] = d2 / sum;
        result[2] = d3 / sum;
        return result;
    }

    public static double randomDouble(double d, double s) {
        Random rand = new Random();
        return rand.nextGaussian() * s + d;
    }

    public void simulatePlots(){


        readHistogram(new File("/home/koomikko/Documents/research/aba_conv_augmentation/total_volumes.txt"),
                new File("/home/koomikko/Documents/research/aba_conv_augmentation/species_proportions.txt"));

        //readHistogram(new File("/home/koomikko/Documents/research/aba_conv_augmentation/total_volumes.txt"),
        //new File("/home/koomikko/Documents/research/aba_conv_augmentation/species_proportions.txt"));


        plotSimulator simulator = new plotSimulator(this);

        simulatedAnnealingForestSimulator costFunction = new simulatedAnnealingForestSimulator(this, 1);

        ArrayList<forestTree> listOfValues
                = itc_with_tree.values().stream().collect(
                Collectors.toCollection(ArrayList::new));

        costFunction.initialSolution(listOfValues);

        //int n_simulations = aR.num_iter;

        File originalOutputDirectory = new File(aR.odir);

        if(!originalOutputDirectory.isDirectory()){
            throw new toolException("Output directory " + aR.odir + " is not a directory");
        }

        List<Integer> targets = new ArrayList<Integer>(this.targets.keySet());

        final int n_simulations = this.plotTargets.size();
        final int n_simulations2 = 16;

        Random r = new Random();
        //r.setSeed(1234);

        Collections.shuffle(targets, r);


        int maxThreads = aR.cores; // set the maximum number of threads
        ForkJoinPool customThreadPool = new ForkJoinPool(maxThreads);

        try {

            customThreadPool.submit(() ->

                    IntStream.range(0, n_simulations).parallel().forEach(i -> {
                    //IntStream.range(0, 10).parallel().forEach(i -> {


                        //for( int i = 0; i < n_simulations; i++ ){

                        //aR.odir = originalOutputDirectory.getAbsolutePath() + "/sim_" + i;

                        String odir = originalOutputDirectory.getAbsolutePath() + "/sim_" + i;

                        File outputDirectory = new File(odir);
                        if (!outputDirectory.isDirectory()) {
                            outputDirectory.mkdir();
                        }

                /*
                simulator.simulatePlotDumbWay(i, true, false);
                simulator.simulatePlotDumbWay(i, false, false);
                simulator.simulatePlotDumbWay(i, false, true);
    */
                        //simulator.simulatePlotAnnealingWay(i, true, false, targets, odir);
                        simulator.simulatePlotAnnealingWay2(i, true, false, plotTargets, odir);
                        //simulator.simulatePlotAnnealingWay(i, false, false);
                        //simulator.simulatePlotAnnealingWay(i, false, true);
                        //System.exit(1);
                        //}
                    })).get();
        }catch (Exception e){
            e.printStackTrace();
        }

        simulator.writeSimulationRaport( new File(originalOutputDirectory.getAbsolutePath() + "/simulation_report.txt" ));
        //aR.odir = originalOutputDirectory.getAbsolutePath();
        this.writePlots(new File(originalOutputDirectory.getAbsolutePath() + "/plot_ids.txt" ));
    }

    public void writePlots(File in){

        try {
            in.createNewFile();

            BufferedWriter bw = new BufferedWriter(new FileWriter(in));

            for(int i : this.plots.keySet()){
                bw.write(this.plots.get(i).getPlotID() + "\n");
            }

            bw.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
