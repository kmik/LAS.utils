
import org.gdal.ogr.Feature;
import utils.ShapefileUtils;
import utils.argumentReader;
import utils.fileDistributor;
import utils.Plot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static utils.miscProcessing.prepareData;

public class treeMapToPlots {

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = null;

        if(aR.inputFiles.size() > 0)
            fD = new fileDistributor(aR.inputFiles);



        ShapefileUtils plotBoundaries = new ShapefileUtils(aR);
        double[] boundingBox = new double[4];
        plotBoundaries.readShapeFiles(aR.poly);
        plotBoundaries.calculateAreas();
        HashMap<Integer, ArrayList<double[]>> randomPlots = plotBoundaries.randomCirclesWithinPolygons(9.0, 7.0, 1.5);



        ShapefileUtils trees = new ShapefileUtils(aR);
        trees.createKdTree();
        double[] boundingBoxTrees = new double[4];
        trees.readShapeFiles(aR.poly_2[0]);

        //System.exit(1);

        // Print randomPlots
        for (int key : randomPlots.keySet()) {
            System.out.println("Key: " + key);
            for (double[] value : randomPlots.get(key)) {
                System.out.println("Value: " + value[0] + " " + value[1]);
            }
        }

        ShapefileUtils outputPlots = new ShapefileUtils(aR);

        outputPlots.declarePlotShapefile("outputPlots.shp");

        File outputFile = aR._createOutputFile_("treeListSimulatedPlots" + ".txt");

        BufferedWriter writer = null;
        FileWriter fw = null;

        int numRows = 0;

        try {
            fw = new FileWriter(outputFile);
            writer = new BufferedWriter(fw);
        } catch (IOException e) {
            e.printStackTrace();

        }

        for(int p : randomPlots.keySet()){

            ArrayList<double[]> plotCenters = randomPlots.get(p);

            int counter = 0;
            for(double[] plotCenter : plotCenters){

                String id = "plot_" + p + "_sim_" + counter++;

                outputPlots.addCircleToShapefile(plotCenter[0], plotCenter[1], 9.0, id);
                List<Feature> treesInPlot = trees.kNearestPoints(plotCenter[0], plotCenter[1], 100, 9.0);

                // Print the features
                for (Feature f : treesInPlot) {

                    if(numRows++ == 0){
                        writer.write("ID\t");
                        for(int i = 0; i < f.GetFieldCount(); i++){

                            writer.write(f.GetFieldDefnRef(i).GetName() + "\t");
                        }

                        writer.newLine();
                    }

                    writer.write(id + "\t");
                    // Print the field name and value
                    for (int i = 0; i < f.GetFieldCount(); i++) {

                        writer.write(f.GetFieldAsString(i) + "\t");
                    }

                    writer.newLine();

                }

            }

        }

        writer.flush();
        writer.close();

        outputPlots.finalizeOutputLayer();
        aR.cleanup();

    }

    public double euclideanDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }
}
