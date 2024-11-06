package utils;

import err.toolException;
import org.cts.op.transformation.grids.Grid;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import org.tinfour.interpolation.TriangularFacetInterpolator;
import org.tinfour.interpolation.VertexValuatorDefault;
import org.tinfour.standard.IncrementalTin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class Segmentation {

    ArrayList<ShapefileUtils> sU_plots = new ArrayList<>();
    GridUtils gU;
    argumentReader aR;

    HashMap<Integer, Segment> segments = new HashMap<>();

    // Generic 2d grid
    short[][] grid;
    short[][] interpolatedGrid;
    int[][] gridAffiliation = null;

    String algorithm;

    int counter = 0;

    ArrayList<int[]> seedPoints = new ArrayList<>();
    ArrayList<Integer> seedPointIds = new ArrayList<>();
    public void Watershed() {
        // Watershed algorithm
    }

    public void Watershed(argumentReader aR) {
        this.aR = aR;
    }

    public void setGrid(GridUtils gU) {

        this.gU = gU;
        this.grid = gU.getGrid();

    }

    public void setGridAffiliation(int[][] gridAffiliation) {
        this.gridAffiliation = gridAffiliation;
    }

    public int[][] getGridAffiliation() {
        return this.gridAffiliation;
    }

    public void setSeedPoints(ArrayList<int[]> seedPoints) {
        this.seedPoints = seedPoints;
    }

    public void setSeedPointIds(ArrayList<Integer> seedPointIds) {
        this.seedPointIds = seedPointIds;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void run() {
        // Run the watershed algorithm

        if(this.algorithm.equals("regionGrowing")) {
            this.regionGrowing();
        }
    }

    public void export(String outputName) {

    }

    public void addShapefile(ShapefileUtils sU) {
        this.sU_plots.add(sU);
    }

    public void regionGrowing() {
        // Region growing algorithm

        this.segments = new HashMap<>();

        rolling_stats rS = new rolling_stats();

        double averageArea = 0;
        // Initialize segments

        for(int i = 1; i <= this.seedPoints.size(); i++) {


            Segment segment = new Segment(i);
            segment.setResolution(this.gU.getResolution());
            int[] point = {(int) this.seedPoints.get(i-1)[0], (int) this.seedPoints.get(i-1)[1]};
            grid[point[0]][point[1]] = (short)i;
            segment.addPoint(point);
            segment.setCenter(point[0], point[1]);
            this.addNeighborsToQueue(point[0], point[1], segment);

            if(gU.cellHasValue(point[0], point[1])) {
                ArrayList<Integer> values = gU.getCellValues(point[0], point[1]);
                for(int j = 0; j < values.size(); j++) {

                    segment.addFeature(values.get(j));
                    segment.addFeature(sU_plots.get(0).getPolyIdAsString(values.get(j)));

                    if(gU.featureInMultipleCells(values.get(j))) {

                        //System.out.println("Feature " + values.get(j) + " is in multiple cells, what to do?");

                        for(int o = 0; o < gU.featuresInMultipleCells.get(values.get(j)).size(); o++){

                            int x = gU.featuresInMultipleCells.get(values.get(j)).get(o)[0];
                            int y = gU.featuresInMultipleCells.get(values.get(j)).get(o)[1];

                            grid[x][y] = (short)i;
                            segment.addPoint(new int[]{x, y});

                        }
                    }
                }
            }

            if(this.seedPointIds.size() > 0) {
                segment.setAffiliation(this.seedPointIds.get(i-1));
            }

            //segment.printQueueSize();
            segments.put(segment.id, segment);

        }

        boolean done = false;

        int numberOfCellsTotal = this.grid.length * this.grid[0].length;
        int progress = 0;

        while(!done) {
            done = true;
            for(int i : segments.keySet()) {

                Segment segment = segments.get(i);

                if(segment.queue.size() > 0) {

                    done = false;
                    Segment.QueueItem qI = segment.getFromQueue();

                    int x = qI.point[0];
                    int y = qI.point[1];

                    if(grid[x][y] != 0) {

                        qI.dispose();
                        qI = null;
                        continue;

                    }

                    if(false)
                    if(gridAffiliation != null){
                        if(gridAffiliation[x][y] != segment.affiliation) {
                            grid[x][y] = 0;
                            qI.dispose();
                            qI = null;
                            continue;
                        }
                    }

                    segment.setAreaLimit(averageArea);

                    boolean accepted = segment.addPoint(qI.point);

                    if(!accepted){
                        grid[x][y] = 0;
                        qI.dispose();
                        qI = null;
                        continue;

                    }

                    rS.add(segment.getArea());

                    grid[x][y] = (short)segment.id;

                    if(gU.cellHasValue(x, y)) {

                        ArrayList<Integer> values = gU.getCellValues(x, y);

                        for(int j = 0; j < values.size(); j++) {

                            segment.addFeature(values.get(j));
                            segment.addFeature(sU_plots.get(0).getPolyIdAsString(values.get(j)));

                            if(gU.featureInMultipleCells(values.get(j))) {

                                //System.out.println("Feature " + values.get(j) + " is in multiple cells, what to do?");

                                for(int o = 0; o < gU.featuresInMultipleCells.get(values.get(j)).size(); o++){

                                    int x_ = gU.featuresInMultipleCells.get(values.get(j)).get(o)[0];
                                    int y_ = gU.featuresInMultipleCells.get(values.get(j)).get(o)[1];

                                    grid[x_][y_] = (short)segment.id;;
                                    segment.addPoint(new int[]{x_, y_});
                                }
                            }
                        }
                    }
                    this.addNeighborsToQueue(x, y, segment);

                    qI.dispose();
                    qI = null;

                    progress++;

                    if(progress % 10000 == 0) {

                        for(int seg : segments.keySet()) {
                            System.out.println("Segment " + seg + " has " + segments.get(seg).points.size() + " points and a queue of size: " + segments.get(seg).queue.size());
                            System.out.println(progress + " / " + numberOfCellsTotal + " cells processed (" + Math.round(((float)progress / (float)numberOfCellsTotal)*100.0f) + "%)");
                        }
                        System.gc();
                    }

                }
            }

            // Clear the line
            //System.out.print("\033[H\033[2J");
            // Do this print by replacing the row below
            //System.out.println(progress + " / " + numberOfCellsTotal + " cells processed (" + Math.round(((float)progress / (float)numberOfCellsTotal)*100.0f) + "%)");
            // Move cursor back to the beginning of the line
            //System.out.print("\r");

            //double averageArea = rS.average_rolling_stats;

            averageArea = 0;

            for(int s : segments.keySet()) {
                averageArea += segments.get(s).getArea();
            }

            averageArea /= segments.size();

            //System.out.println("Average area: " + averageArea + " " + averageArea2);

            //if(averageArea2 == 0)
            //    rS.reset();
            //else
            //    System.exit(1);

        }

        System.out.println("Number of segments: " + segments.size());

        //System.exit(1);
        this.interpolatedGrid = gU.interpolate("haha", (short)0, gridAffiliation, -1);
        updateSegmentation(interpolatedGrid, grid);
        this.mergeSmallSegments();
        //

        gU.setGridByCloning(interpolatedGrid);

        this.segments = segments;

        if(false){
            IncrementalTin tin = new IncrementalTin();
            TriangularFacetInterpolator polator = new TriangularFacetInterpolator(tin);
            VertexValuatorDefault valuator = new VertexValuatorDefault();
            // Merge the tins of the segments
            for (int i = 0; i < segments.size(); i++) {

                segments.get(i).addTinVertices(tin);

            }
            polator.resetForChangeToTin();
            // Iterate the grid

            if (true)
                for (int x = 0; x < grid.length; x++) {
                    for (int y = 0; y < grid[0].length; y++) {

                        // This means we must interpolate the value based on the tin
                        if (grid[x][y] == 0) {
                            //System.out.println("HERE! " + (short) (Math.ceil(polator.interpolate(x, y, valuator))));

                            grid[x][y] = (short) (Math.ceil(polator.interpolate(x, y, valuator)));
                        }

                    }
                }
        }

    }

    public void affiliationPercentages(){

        for(int i : this.segments.keySet()){
            Segment segment = this.segments.get(i);
            TreeMap<Integer, Integer> affiliation = new TreeMap<>();

            double sumPercentages = 0;

            for(int j = 0; j < segment.points.size(); j++){
                int x = segment.points.get(j)[0];
                int y = segment.points.get(j)[1];
                if(gridAffiliation != null){
                    if(gridAffiliation[x][y] != 0){
                        if(!affiliation.containsKey((int)gridAffiliation[x][y])){
                            affiliation.put((int)gridAffiliation[x][y], 1);
                        }else{
                            affiliation.put((int)gridAffiliation[x][y], affiliation.get((int)gridAffiliation[x][y]) + 1);
                        }
                    }
                }
            }
            for(int j : affiliation.keySet()){
                //System.out.println("Segment " + i + " has " + affiliation.get(j) + " points affiliated with " + j + " " + (float)affiliation.get(j) / (float)segment.points.size() * 100.0f + "%");

                sumPercentages += (float)affiliation.get(j) / (float)segment.points.size() * 100.0f;

            }
            //System.out.println("This should be 100%: " + sumPercentages);
            segment.setAffiliationPercentages(affiliation);
        }

    }

    public void updateSegmentation(short[][] interpolated, short[][] original){
        // Update the segmentation based on the interpolated grid
        for(int x = 0; x < interpolated.length; x++) {
            for(int y = 0; y < interpolated[0].length; y++) {

                if(interpolated[x][y] != original[x][y]) {
                    // Find the segment that has the original value
                    for(int i : this.segments.keySet()) {
                        if(this.segments.get(i).id == interpolated[x][y]) {
                            this.segments.get(i).addPoint(new int[]{x, y});
                            if(gU.cellHasValue(x, y)) {
                                ArrayList<Integer> values = gU.getCellValues(x, y);
                                for(int j = 0; j < values.size(); j++) {

                                    this.segments.get(i).addFeature(values.get(j));
                                    this.segments.get(i).addFeature(this.sU_plots.get(0).getPolyIdAsString(values.get(j)));

                                }
                            }
                            break;
                        }
                    }

                }
            }
        }
    }

    public void validateNoFeaturesInMultipleCells() {

        System.out.println("Validating no features in multiple cells");
        System.out.println(gU.featuresInMultipleCells.size());
        for(int i = 0; i < grid.length; i++) {
            for(int j = 0; j < grid[0].length; j++) {
                if(grid[i][j] != 0) {
                    if(gU.cellHasValue(i, j)) {

                        ArrayList<Integer> values = gU.getCellValues(i, j);
                        //System.out.println("Cell: " + i + " " + j + " has " + values.size() + " values");
                        for(int k = 0; k < values.size(); k++) {

                            if(gU.featureInMultipleCells(values.get(k))) {

                                //System.out.println("Feature " + values.get(k) + " is in multiple cells, what to do?");

                                int x = gU.featuresInMultipleCells.get(values.get(k)).get(0)[0];
                                int y = gU.featuresInMultipleCells.get(values.get(k)).get(0)[1];
                                int firstValue = grid[x][y];

                                for(int o = 0; o < gU.featuresInMultipleCells.get(values.get(k)).size(); o++){

                                    x = gU.featuresInMultipleCells.get(values.get(k)).get(o)[0];
                                    y = gU.featuresInMultipleCells.get(values.get(k)).get(o)[1];

                                    if(grid[x][y] != firstValue) {
                                        throw new toolException("Feature " + values.get(k) + " is in multiple cells, what to do?");

                                    }

                                }

                            }
                        }
                    }
                }
            }
        }
    }


    public void addNeighborsToQueue(int x, int y, Segment segment) {

        // Add neighbors to queue
        if(false)
        for(int i = -1; i < 2; i++) {
            for(int j = -1; j < 2; j++) {

                if(i == 0 && j == 0) {
                    continue;
                }

                int x1 = x + i;
                int y1 = y + j;

                if(x1 < 0 || x1 >= grid.length || y1 < 0 || y1 >= grid[0].length) {
                    continue;
                }

                if(grid[x1][y1] != 0) {
                    continue;
                }else{
                    int[] point = {x1, y1};
                    segment.addToQueue(point);
                }
            }
        }

        // Do it again, but this time exclude diagonal neighbors

        for(int i = -1; i < 2; i++) {
            for(int j = -1; j < 2; j++) {

                if(i == 0 && j == 0) {
                    continue;
                }

                if(i != 0 && j != 0) {
                    continue;
                }

                int x1 = x + i;
                int y1 = y + j;

                if(x1 < 0 || x1 >= grid.length || y1 < 0 || y1 >= grid[0].length) {
                    continue;
                }
                if(gridAffiliation[x1][y1] == -1) {
                    //System.out.println("HERE!");
                    continue;
                }

                if(grid[x1][y1] != 0) {
                    continue;
                }else{
                    int[] point = {x1, y1};
                    segment.addToQueue(point);
                }
            }
        }
    }

    public void mergeSmallSegments(){
        // Merge small segments

        ArrayList<Integer> segmentsToRemove = new ArrayList<>();

        for(int i : this.segments.keySet()) {

            Segment segment = this.segments.get(i);


            if(segment.getArea() < 500) {

                int segmentId = segment.id;

                //System.out.println("Segment " + segment.id + " has " + segment.getArea() + " points");

                HashMap<Short, Integer> neighbors = this.segmentNeighbors(segmentId);

                // Print the map
                int highestNumberOfBorder = 0;
                int highestBorderSegment = 0;

                for(Short key : neighbors.keySet()) {

                    if(neighbors.get(key) > highestNumberOfBorder) {
                        highestNumberOfBorder = neighbors.get(key);
                        highestBorderSegment = key;
                    }

                    //System.out.println("Segment " + key + " has " + neighbors.get(key) + " neighbors");
                }

                //System.out.println("Segment " + segmentId + " has " + highestNumberOfBorder + " neighbors with segment " + highestBorderSegment);

                Segment highestBorderSegmentObject = this.segments.get(highestBorderSegment);
                highestBorderSegmentObject.mergeSegmentToThisOne(segment);

                for(int j = 0; j < segment.points.size(); j++) {
                    int x = segment.points.get(j)[0];
                    int y = segment.points.get(j)[1];
                    interpolatedGrid[x][y] = (short)highestBorderSegment;
                }

                segmentsToRemove.add(segmentId);
            }
        }

        // Remove the segments that are too small from segments
        for(int i = 0; i < segmentsToRemove.size(); i++) {
            for(int j : this.segments.keySet()) {
                if(this.segments.get(j).id == segmentsToRemove.get(i)) {
                    this.segments.remove(j);
                    break;
                }
            }
        }
    }

    public HashMap<Short, Integer> segmentNeighbors(int segmentId) {
        // Get the neighbors of a segment
        HashMap<Short, Integer> neighbors = new HashMap<>();

        Segment segment = this.segments.get(segmentId);

        for(int i = 0; i < segment.points.size(); i++) {

            int x = segment.points.get(i)[0];
            int y = segment.points.get(i)[1];

            for(int j = -1; j < 2; j++) {
                for(int k = -1; k < 2; k++) {

                    if(j == 0 && k == 0) {
                        continue;
                    }

                    int x1 = x + j;
                    int y1 = y + k;

                    if(x1 < 0 || x1 >= grid.length || y1 < 0 || y1 >= grid[0].length) {
                        continue;
                    }

                    if(grid[x1][y1] == 0) {
                        continue;
                    }

                    if(grid[x1][y1] == segmentId) {
                        continue;
                    }

                    if(!neighbors.containsKey(grid[x1][y1])) {
                        neighbors.put(grid[x1][y1], 1);
                    }else{
                        neighbors.put(grid[x1][y1], neighbors.get(grid[x1][y1]) + 1);
                    }
                }
            }
        }

        return neighbors;
    }




    public void writeOutputFile(File file, ShapefileUtils sU_plots) {

        BufferedWriter writer = null;

        FileWriter fw = null;

        // Write lines to file

        try {
            fw = new FileWriter(file);
            writer = new BufferedWriter(fw);
            writer.write("id\tarea_sq_km\tn_features\tfeature_ids\tfeature_affis\tn_segment_affi\tsegment_affi_ids\taffiProp\n");

            for(int i : this.segments.keySet()) {
                Segment segment = this.segments.get(i);
                writer.write(segment.id + "\t" + segment.getArea() + "\t" + segment.numFeatrues());

                writer.write("\t");
                for(int j = 0; j < segment.features_s.size(); j++){
                    writer.write(segment.features_s.get(j) + "");
                    if(j < segment.features_s.size() - 1){
                        writer.write(";");
                    }
                }
                if(segment.numFeatrues() == 0)
                    writer.write("null");

                writer.write("\t");
                for(int j = 0; j < segment.features_s.size(); j++){
                    writer.write(sU_plots.getAffiliation(segment.features.get(j)) + "");
                    if(j < segment.features_s.size() - 1){
                        writer.write(";");
                    }
                }
                if(segment.numFeatrues() == 0)
                    writer.write("null");


                writer.write("\t" + segment.affiliationPercentages.size() + "\t");

                int counter = 0;
                for(int j : segment.affiliationPercentages.keySet()){
                    writer.write(j + "");
                    if(counter++ < segment.affiliationPercentages.size() - 1){
                        writer.write(";");
                    }
                }

                counter = 0;
                writer.write("\t");

                for(int j : segment.affiliationPercentages.keySet()){
                    writer.write((roundDouble((double)segment.affiliationPercentages.get(j) / (double)segment.points.size(), 2)) + "");
                    if(counter++ < segment.affiliationPercentages.size() - 1){
                        writer.write(";");
                    }
                }

                writer.write("\n");

            }

            writer.close();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public double roundDouble(double value, int places) {
        // Round a double value to a certain number of places
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }


}
