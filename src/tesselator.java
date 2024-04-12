import LASio.LASReader;
import tools.process_las2las;
import utils.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;

public class tesselator {

    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "las2las");
        fileDistributor fD = null;

        if(aR.inputFiles.size() > 0)
            fD = new fileDistributor(aR.inputFiles);


        double proportionWithinOneAffiliaty = 0.5;
        HashSet<Integer> affiliatiesWithWholeMunicipalities = new HashSet<>();
        affiliatiesWithWholeMunicipalities.add(1);
        affiliatiesWithWholeMunicipalities.add(2);

        // SHAPEFILE PART

        ShapefileUtils sU_plots = new ShapefileUtils(aR);

        if(aR.poly_2 != null)
            sU_plots.readShapeFiles(aR.poly_2[0]);

        System.out.println(sU_plots.numPolygons());

        ShapefileUtils sU = new ShapefileUtils(aR);
        double[] boundingBox = new double[4];
        sU.readShapeFiles(aR.poly);
        boundingBox = sU.getBoundingBox();

        System.out.println("Bounding box: " + boundingBox[0] + " " + boundingBox[1] + " " + boundingBox[2] + " " + boundingBox[3]);

        short[][] grid = sU.shapeFileToGrid(aR.step, aR.step);
        byte [][] gridAffiliation = sU.gridToAffiliationGrid(grid);


        for(int i : sU.getBounds().keySet()){

            for(int j : sU_plots.getBounds().keySet()){

                double[][] bound1 = sU.getBounds(i);
                double[][] bound2 = sU_plots.getBounds(j);

                if(sU.boundsOverlap(bound1, bound2)) {
                    //System.out.println("Overlap: " + i + " " + j);

                    //System.out.println(sU.overlapPercentage(bound1, bound2));

                    sU_plots.addAffiliation(j, i);
                }

            }

        }


        //System.exit(1);
        ArrayList<double[]> points = sU.randomPointsInAllPolygons(aR.dist);
        ArrayList<Integer> pointToPolygonLink = new ArrayList<>();

        for(int i = 0; i < points.size(); i++){
            pointToPolygonLink.add(sU.pointInWhichPolygon(points.get(i)[0], points.get(i)[1]));
        }

        //System.out.println("Number of points: " + points.size() + " " + pointToPolygonLink.size());

        for( int i = 0; i < points.size(); i++){
            //System.out.println("Point: " + points.get(i)[0] + " " + points.get(i)[1] + " " + pointToPolygonLink.get(i));
        }



        GridUtils gU = new GridUtils(aR);

        gU.setGrid(grid);
        gU.setOrigin(boundingBox[0], boundingBox[3]);
        gU.setResolution(aR.step);

        int numPolys = sU_plots.numPolygons();

        System.out.println("Number of polygons: " + numPolys);

        HashMap<Integer, ArrayList<int[]>> featuresInMultipleCells = new HashMap<>();

        for(int i : sU_plots.getBounds().keySet()){
            double[] bbox = sU_plots.getBoundingBox(i);
            ArrayList<int[]> cells = gU.getCellsThatOverlapWithExtent(bbox);
            //System.out.println("Number of cells: " + cells.size());

            if(cells.size() > 1){
                //System.out.println("More than one cell for polygon: " + i);
                featuresInMultipleCells.put(i, new ArrayList<>());
                for(int j = 0; j < cells.size(); j++){
                    featuresInMultipleCells.get(i).add(cells.get(j));
                }
            }
            for(int j = 0; j < cells.size(); j++){
                gU.addValueToGridCell(cells.get(j)[0], cells.get(j)[1], i);
            }
        }

        System.out.println("Number of features in multiple cells: " + featuresInMultipleCells.size());

        for(int i : featuresInMultipleCells.keySet()){
            //System.out.println("Feature: " + i);
            for(int j = 0; j < featuresInMultipleCells.get(i).size(); j++){
                //System.out.println("Cell: " + featuresInMultipleCells.get(i).get(j)[0] + " " + featuresInMultipleCells.get(i).get(j)[1]);
            }
        }

        //System.exit(1);

        gU.setFeaturesInMultipleCells(featuresInMultipleCells);
        ArrayList<int[]> gridPoints = gU.coordinatesToGridCoordinates(points);

        Segmentation seg = new Segmentation();
        seg.setGrid(gU);
        seg.setGridAffiliation(gridAffiliation);

        // print gridAffiliation
        for(int i = 0; i < gridAffiliation.length; i++){
            for(int j = 0; j < gridAffiliation[0].length; j++){
                //System.out.print(gridAffiliation[i][j] + " ");
            }
            //System.out.println();
        }

        //System.exit(1);
        seg.setSeedPoints(gridPoints);
        seg.setSeedPointIds(pointToPolygonLink);


        for(int i = 0; i < gridPoints.size(); i++) {
            //System.out.println("Seed point: " + gridPoints.get(i)[0] + " " + gridPoints.get(i)[1]);
        }

        seg.setAlgorithm("regionGrowing");
        seg.addShapefile(sU_plots);
        seg.run();
        seg.validateNoFeaturesInMultipleCells();
        seg.affiliationPercentages();


        File ofile = aR._createOutputFile_("statistics.txt");

        seg.writeOutputFile(ofile, sU_plots);
        gU.writeRasterOutput("", gU.getGrid());
        gU.polygonize(gU.rasterFile.getAbsolutePath());
        aR.cleanup();

    }




}
