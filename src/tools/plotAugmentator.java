package tools;

import err.toolException;
import org.gdal.ogr.*;
import utils.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

        System.out.println(itc_with_tree_unique_id.size());
        //System.exit(1);

    }

    public void preparePlots(){
        for(int plot : plots.keySet()){
            plots.get(plot).preparePlot();
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
                    throw new toolException("Plot " + line[2] + " not found in shapefile");
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

            plots.get(tempF.GetFieldAsInteger(0)).setSmallerBounds(tempG2.GetPoints());
            plots.get(tempF.GetFieldAsInteger(0)).setSmallerArea(tempG2.GetArea());
            System.out.println("Smaller area: " + tempG2.GetArea());
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

        for(long i = 0; i < shapeFileLayer.GetFeatureCount(); i++ ) {

            Feature tempF = shapeFileLayer.GetFeature(i);
            Geometry tempG = tempF.GetGeometryRef();
            Geometry tempG2 = tempG.GetGeometryRef(0);

            if(tempG == null)
                continue;

            plots.put(tempF.GetFieldAsInteger(0), new forestPlot(tempF.GetFieldAsInteger(0), tempG2.GetPoints(), this));

            plots.get(tempF.GetFieldAsInteger(0)).setArea(tempG2.GetArea());
            //print2DArray(plots.get(tempF.GetFieldAsInteger(0)).getPlotBounds());
            //System.exit(1);
        }

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
                System.out.println(targets.get(plotID).trees.size());
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
            targets.remove(i);
        }
        //System.exit(1);
    }


    public void simulatePlots(){

        plotSimulator simulator = new plotSimulator(this);

        simulatedAnnealingForestSimulator costFunction = new simulatedAnnealingForestSimulator();

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

        final int n_simulations = targets.size();


        int maxThreads = aR.cores; // set the maximum number of threads
        ForkJoinPool customThreadPool = new ForkJoinPool(maxThreads);

        try {

            customThreadPool.submit(() ->

                    IntStream.range(0, n_simulations).parallel().forEach(i -> {


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
                        simulator.simulatePlotAnnealingWay(i, true, false, targets, odir);
                        //simulator.simulatePlotAnnealingWay(i, false, false);
                        //simulator.simulatePlotAnnealingWay(i, false, true);
                        //System.exit(1);
                        //}
                    })).get();
        }catch (Exception e){
            e.printStackTrace();
        }

        //aR.odir = originalOutputDirectory.getAbsolutePath();
    }
}
