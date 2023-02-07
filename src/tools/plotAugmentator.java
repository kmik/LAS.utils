package tools;

import LASio.LASReader;
import err.toolException;
import org.gdal.ogr.*;
import org.mapdb.HTreeMap;
import utils.argumentReader;
import utils.forestPlot;
import utils.forestTree;
import utils.plotSimulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class plotAugmentator {

    argumentReader aR;

    public HashMap<Integer, forestPlot> plots = new HashMap<Integer, forestPlot>();
    public HashMap<Integer, forestTree> trees = new HashMap<Integer, forestTree>();
    //HashMap<Integer, double[][]> treeBounds = new HashMap<>();
    public ArrayList<double[][]> treeBounds = new ArrayList<>();

    public HashMap<Integer, forestTree> ITC_id_to_tree_id = new HashMap<>();

    public plotAugmentator(){

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

            if(tree_id > 0) {
                plots.get(plot_id).hasITCid(tree_id);
                plots.get(plot_id).getTree(tree_id).setTreeITCid((int) Double.parseDouble(line[line.length - 13]));

                ITC_id_to_tree_id.put(plots.get(plot_id).getTree(tree_id).getTreeITCid(), plots.get(plot_id).getTree(tree_id));

                plots.get(plot_id).getTree(tree_id).setTreeHeight_ITC((int) Double.parseDouble(line[line.length - 8]));
                plots.get(plot_id).getTree(tree_id).setTreeX_ITC((int) Double.parseDouble(line[line.length - 10]));
                plots.get(plot_id).getTree(tree_id).setTree_Y_ITC((int) Double.parseDouble(line[line.length - 9]));
                plots.get(plot_id).getTree(tree_id).setTreeCrownArea((int) Double.parseDouble(line[line.length - 12]));
            }else{
                plots.get(plot_id).ITCHasNoMatch((int) Double.parseDouble(line[line.length - 13]));
            }
        }

        for(int plot : plots.keySet()){

        }

    }

    public void preparePlots(){
        for(int plot : plots.keySet()){
            plots.get(plot).preparePlot();
        }
    }

    public void readMeasuredTrees(File measuredTreesFile) throws IOException {

        BufferedReader sc = new BufferedReader( new FileReader(measuredTreesFile));
        sc.readLine();
        String line1;

        while((line1 = sc.readLine())!= null) {

            String[] line  = line1.split(",");

            forestTree tmpTree = new forestTree();

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

            if(Integer.parseInt(line[2]) > 0){
                if(!plots.containsKey(Integer.parseInt(line[2]))) {
                    throw new toolException("Plot " + line[2] + " not found in shapefile");
                }else
                    plots.get(Integer.parseInt(line[2])).addTree(tmpTree);

            }


        }

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
            //System.out.println(tempG2.GetPoints()[0].length);
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

                if(ITC_id_to_tree_id.containsKey(tempF.GetFieldAsInteger(0))) {
                    //System.out.println(tempF.GetFieldAsInteger(0));
                    ITC_id_to_tree_id.get(tempF.GetFieldAsInteger(0)).setTreeCrownBounds(tempG2.GetPoints());

                    plots.get(ITC_id_to_tree_id.get(tempF.GetFieldAsInteger(0)).getPlotID()).setPlotLASFile(
                            new File(shapeFile2[i].split("_TreeSegmentation.shp") [0] + "_ITD.las")
                    );
                    //trees.get(ITC_id_to_tree_id.get(tempF.GetFieldAsInteger(0))).setTreeCrownBounds(tempG2.GetPoints());
                    matched_trees++;
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
    public static void print2DArray(double[][] arr) {
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                System.out.print(arr[i][j] + " ");
            }
            System.out.println();
        }
    }

    public void simulatePlots(){

        plotSimulator simulator = new plotSimulator(this);


    }
}
