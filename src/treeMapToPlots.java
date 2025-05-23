
import err.toolException;
import org.gdal.gdal.gdal;
import org.gdal.ogr.Feature;
import org.gdal.ogr.ogr;
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

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = null;

        if(aR.inputFiles.size() > 0)
            fD = new fileDistributor(aR.inputFiles);

        ShapefileUtils plotBoundaries = new ShapefileUtils(aR);
        double[] boundingBox = new double[4];
        plotBoundaries.readShapeFiles(aR.poly);
        plotBoundaries.calculateAreas();
        HashMap<Integer, ArrayList<double[]>> randomPlots = plotBoundaries.randomCirclesWithinPolygons(9.0, 14, 1.5);



        ShapefileUtils trees = new ShapefileUtils(aR);
        trees.createKdTree();
        double[] boundingBoxTrees = new double[4];
        trees.readShapeFiles(aR.poly_2[0]);

        //System.exit(1);

        // Print randomPlots
        for (int key : randomPlots.keySet()) {
            //System.out.println("Key: " + key);
            for (double[] value : randomPlots.get(key)) {
                //System.out.println("Value: " + value[0] + " " + value[1]);
            }
        }

        ShapefileUtils outputPlots = new ShapefileUtils(aR);


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

        boolean volumeColChecked = false;
        int volumeCol = -1;

        for(int p : randomPlots.keySet()){

            outputPlots.declarePlotShapefile("poly_" + p + "_sim_0.shp");

            ArrayList<double[]> plotCenters = randomPlots.get(p);

            // Crate a new directory for the plot
            File plotDir = new File(aR.odir + aR.pathSep + "plot_" + p);

            if(!plotDir.exists()){

                plotDir.mkdirs();

            }

            int counter = 0;
            for(double[] plotCenter : plotCenters){

                String id = "poly_" + p + "_sim_" + counter++;

                if(aR.stringArgument1 != null){

                    id = aR.stringArgument1 + "_" + id;
                }
                List<Feature> treesInPlot = trees.kNearestPoints(plotCenter[0], plotCenter[1], 100, 9.0);

                double volume_tot = 0.0;
                double volume_pine = 0.0;
                double volume_spruce = 0.0;
                double volume_deciduous = 0.0;

                // Print the features
                for (Feature f : treesInPlot) {

                    if(!volumeColChecked){
                        for(int i = 0; i < f.GetFieldCount(); i++){
                            if(f.GetFieldDefnRef(i).GetName().equals("vol")){
                                volumeCol = i;
                                volumeColChecked = true;
                            }
                            if(f.GetFieldDefnRef(i).GetName().equals("vtot")){
                                volumeCol = i;
                                volumeColChecked = true;
                            }
                        }
                        if(volumeCol == -1){
                            throw new toolException("Volume column not found in the shapefile. Please check the shapefile and try again.");

                        }
                    }
                    volume_tot += f.GetFieldAsDouble(volumeCol);
                    int treeSpecies = f.GetFieldAsInteger("species");

                    if(treeSpecies == 0){
                        volume_pine += f.GetFieldAsDouble(volumeCol);

                    } else if(treeSpecies == 1){
                        volume_spruce += f.GetFieldAsDouble(volumeCol);

                    } else if(treeSpecies > 1){
                        volume_deciduous += f.GetFieldAsDouble(volumeCol);
                    }
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

                System.out.println("Plot: " + id + " - Volume Total: " + volume_tot + " - Volume Pine: " + volume_pine + " - Volume Spruce: " + volume_spruce + " - Volume Deciduous: " + volume_deciduous);
                double frac = 10000 / (Math.PI * Math.pow(9.0, 2));

                volume_tot *= frac;
                volume_tot /= 1000;

                volume_pine *= frac;
                volume_pine /= 1000;
                volume_spruce *= frac;
                volume_spruce /= 1000;
                volume_deciduous *= frac;
                volume_deciduous /= 1000;

                outputPlots.addCircleToShapefile(plotCenter[0], plotCenter[1], 9.0, id, frac, volume_tot, volume_pine, volume_spruce, volume_deciduous);

            }
            outputPlots.finalizeOutputLayer();

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
