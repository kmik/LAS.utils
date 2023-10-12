package utils;

import java.util.ArrayList;

public class rasterCollection {

    ArrayList<Integer> currentSelection = new ArrayList<Integer>();

    public double[] currentSelectionExtent = new double[4];
    ArrayList<gdalRaster> rasters = new ArrayList<gdalRaster>();
    ArrayList<double[]> rasterExtents = new ArrayList<double[]>();
    public rasterCollection() {

    }

    public void addRaster(gdalRaster raster) {

        rasters.add(raster);

        rasterExtents.add(raster.rasterExtent());

        raster.close();

    }

    public ArrayList<Integer> findOverlappingRasters(double minx, double maxx, double miny, double maxy){

        ArrayList<Integer> overlappingRasters = new ArrayList<Integer>();
        this.currentSelection.clear();

        for(int i = 0; i < rasterExtents.size(); i++){

            if (rasters.get(i).isOverlapping(minx, maxx, miny, maxy)){
                overlappingRasters.add(i);
                this.currentSelection.add(i);
                rasters.get(i).open();
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
}
