package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class rasterCollection {

    public ArrayList<Integer> currentSelection = new ArrayList<Integer>();

    public double[] currentSelectionExtent = new double[4];
    public ArrayList<gdalRaster> rasters = new ArrayList<gdalRaster>();
    ArrayList<double[]> rasterExtents = new ArrayList<double[]>();

    HashSet<Integer> previousSelection = new HashSet<Integer>();

    int cores = 1;
    int keepRastersInBufferFor = 10000;
    int currentCount = 0;

    int[] _currentCount_ = new int[1];
    public rasterCollection(int cores) {
        this.cores = cores;
        this._currentCount_ = new int[cores];
    }

    public void addRaster(gdalRaster raster) {

        rasters.add(raster);

        rasterExtents.add(raster.rasterExtent());

        raster.close();

    }

    public void addRaster(gdalRaster raster, argumentReader aR) {

        rasters.add(raster);

        rasterExtents.add(raster.rasterExtent());

        if(aR.metadataitems.size() > 0){

            for(int i = 0; i < aR.metadataitems.size(); i++) {
                raster.addMetadataitem(aR.metadataitems.get(i));
            }

        }else{
            throw new IllegalArgumentException("No metadata items specified");
        }
        raster.close();

    }

    public ArrayList<Integer> findOverlappingRastersThreadSafe(double minx, double maxx, double miny, double maxy){

        ArrayList<Integer> overlappingRasters = new ArrayList<Integer>();

        HashSet<Integer> doNotOpenAgain = new HashSet<Integer>();
        HashSet<Integer> currentlyToBeOpened = new HashSet<Integer>();
        HashSet<Integer> currentlyToBeClosed = new HashSet<Integer>();

        for(int i = 0; i < rasterExtents.size(); i++){

            if (rasters.get(i).isOverlapping(minx, maxx, miny, maxy)){

                rasters.get(i).resetQueries();
                //System.out.println("overlaps with: " + Arrays.toString(rasterExtents.get(i)));

                if(!rasters.get(i).isOpen())
                    rasters.get(i).open();

                currentlyToBeOpened.add(i);

                overlappingRasters.add(i);

                /*
                overlappingRasters.add(i);
                this.currentSelection.add(i);
                rasters.get(i).open();

                 */
            }else{
                currentlyToBeClosed.add(i);
                if(rasters.get(i).isOpen())
                    rasters.get(i).addQuery();
            }

        }

        //System.out.println(this.currentSelection.size());
        //System.exit(1);

        for(int i : currentlyToBeClosed){

            if(rasters.get(i).openedFor() > keepRastersInBufferFor && rasters.get(i).canClose()){
                rasters.get(i).close();
            }
        }

        return overlappingRasters;

    }
    public synchronized ArrayList<Integer> findOverlappingRasters(double minx, double maxx, double miny, double maxy){

        ArrayList<Integer> overlappingRasters = new ArrayList<Integer>();
        this.currentSelection.clear();

        HashSet<Integer> doNotOpenAgain = new HashSet<Integer>();
        HashSet<Integer> currentlyToBeOpened = new HashSet<Integer>();
        HashSet<Integer> currentlyToBeClosed = new HashSet<Integer>();


        currentCount++;

        for(int i = 0; i < rasterExtents.size(); i++){

            if (rasters.get(i).isOverlapping(minx, maxx, miny, maxy)){

                //System.out.println("overlaps with: " + Arrays.toString(rasterExtents.get(i)));

                if(!rasters.get(i).isOpen())
                    rasters.get(i).open();

                currentlyToBeOpened.add(i);

                overlappingRasters.add(i);
                this.currentSelection.add(i);

                /*
                overlappingRasters.add(i);
                this.currentSelection.add(i);
                rasters.get(i).open();

                 */
            }else{
                currentlyToBeClosed.add(i);
                if(rasters.get(i).isOpen())
                    rasters.get(i).addQuery();
            }

        }

        //System.out.println(this.currentSelection.size());
        //System.exit(1);

        for(int i : currentlyToBeClosed){

            if(rasters.get(i).openedFor() > keepRastersInBufferFor && rasters.get(i).canClose()){
                rasters.get(i).close();
            }
        }

        return overlappingRasters;
    }

    public void closeCurrentSelection(){
        for(int i = 0; i < currentSelection.size(); i++)
            rasters.get(currentSelection.get(i)).close();

        currentSelection.clear();

    }

    public gdalRaster getRaster(int i){
        return rasters.get(i);
    }

    public int numRasters(){
        return rasters.size();
    }

    public int numRasterExtents(){
        return rasterExtents.size();
    }

    public float[][] currenSelectionToArray(){

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        double resolution = this.getRaster(0).getResolution();

        for(int i = 0 ; i < currentSelection.size(); i++){

            int rasterIndex = currentSelection.get(i);

            if(this.getRaster(rasterIndex).getMinX() < minX)
                minX = this.getRaster(rasterIndex).getMinX();

            if(this.getRaster(rasterIndex).getMinY() < minY)
                minY = this.getRaster(rasterIndex).getMinY();

            if(this.getRaster(rasterIndex).getMaxX() > maxX)
                maxX = this.getRaster(rasterIndex).getMaxX();

            if(this.getRaster(rasterIndex).getMaxY() > maxY)
                maxY = this.getRaster(rasterIndex).getMaxY();

        }

        double numPixelsX = (maxX - minX) / resolution;
        double numPixelsY = (maxY - minY) / resolution;

        float[][] output  = new float[(int)numPixelsX][(int)numPixelsY];

        for(int i = 0; i < currentSelection.size(); i++)
            this.rasters.get(currentSelection.get(i)).rasterToArray(output, minX, minY, maxX, maxY);

        this.currentSelectionExtent = new double[]{minX, minY, maxX, maxY};

        return output;
    }

    public void printCurrentSelectionFileNames(){
        for(int i = 0; i < currentSelection.size(); i++)
            System.out.println(this.getRaster(currentSelection.get(i)).filename);
    }

    public double getCurrentSelectionMinX(){
        return this.currentSelectionExtent[0];
    }

    public double getCurrentSelectionMinY(){
        return this.currentSelectionExtent[1];
    }

    public double getCurrentSelectionMaxX(){
        return this.currentSelectionExtent[2];
    }

    public double getCurrentSelectionMaxY(){
        return this.currentSelectionExtent[3];
    }

    public double getCurrentSelectionResolution(){
        return this.getRaster(0).getResolution();
    }

    public double getResolution(){
        return this.getRaster(0).getResolution();
    }

    public float readRaster(double x, double y){

        float[] readValue = new float[1];

        int whichRaster = this.whichSelectedRasterContainsPoint(x, y);

        if(whichRaster == -1)
            return Float.NaN;

        //this.getRaster(whichRaster).readRaster(x, y, readValue);

        return 0.0f;
    }

    public int whichSelectedRasterContainsPoint(double x, double y){

        for(int i = 0; i < currentSelection.size(); i++){

            if(this.getRaster(currentSelection.get(i)).containsPoint(x, y))
                return currentSelection.get(i);

        }

        return -1;

    }
}
