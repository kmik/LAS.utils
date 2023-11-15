package utils;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class gdalRaster {

    boolean dontcareformemory = true;
    float[][] rasterArray = new float[1][1];
    boolean lock_1 = false;
    int id;
    int processingInProgress = 0;
    public ArrayList<String> metadatas = new ArrayList<String>();
    public ArrayList<String> metadatas_values = new ArrayList<String>();
    Double[] nanValue;
    float[] value = new float[1];

    String filename = null;

    Dataset raster;

    Band band;

    boolean isOpen = false;

    public int openedForNQueries = 0;

    public int[] openedForNQueries_ = new int[1];
    public double[] rasterExtent = new double[1];

    public double[] geoTransform = new double[6];

    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;

    public int number_of_pix_x;
    public int number_of_pix_y;

    int cores = 1;
    double resolution;
    public gdalRaster() {

    }

    public gdalRaster(String filename) {
        this.filename = filename;
        this.readRaster(filename);
        this.rasterExtent();
    }

    public gdalRaster(String filename, int id) {
        this.filename = filename;
        this.readRaster(filename);
        this.id = id;
        this.rasterExtent();
        //this.openedForNQueries_ = new int[cores];
    }


    public synchronized void readRaster(String filename) {
        this.raster = (gdal.Open(filename, gdalconst.GA_ReadOnly));
        this.nanValue = new Double[1];
        this.raster.GetRasterBand(1).GetNoDataValue(this.nanValue);

    }

    public synchronized void addMetadataitem(String item){
        this.metadatas.add(item);

        this.metadatas_values.add(this.raster.GetMetadataItem(item));


        //System.out.println(this.metadatas_values.get(this.metadatas_values.size()-1));

    }


    public synchronized double[] rasterExtent() {

        if(this.rasterExtent.length != 1){
            return this.rasterExtent;
        }

        double[] rasterExtent = new double[6];
        this.raster.GetGeoTransform(rasterExtent);


        this.raster.GetGeoTransform(geoTransform);

        double[] rasterWidthHeight_ = new double[2];
        rasterWidthHeight_[0] = raster.GetRasterXSize();
        rasterWidthHeight_[1] = raster.GetRasterYSize();

        this.resolution = rasterExtent[1];

        this.number_of_pix_x = (int) rasterWidthHeight_[0];
        this.number_of_pix_y = (int) rasterWidthHeight_[1];



        this.rasterExtent = new double[]{rasterExtent[0], rasterExtent[3] + rasterExtent[5] * rasterWidthHeight_[1],rasterExtent[0] + rasterExtent[1] * rasterWidthHeight_[0],  rasterExtent[3]};


        return this.rasterExtent;

    }

    public boolean isOverlapping(double minx, double maxx, double miny, double maxy){

        //System.out.println("array: " + Arrays.toString(this.rasterExtent));
        if (minx > this.rasterExtent[2] || maxx < this.rasterExtent[0] || miny > this.rasterExtent[3] || maxy < this.rasterExtent[1]){
            return false;
        }
        else{
            return true;
        }

    }

    public synchronized void close(){
        this.raster.delete();
        this.band = null;
        this.isOpen = false;
        this.openedForNQueries = 0;
        this.rasterArray = new float[1][1];
    }

    public void flush(){
        this.raster.FlushCache();
    }

    public synchronized void open(){
        this.open(this.filename);
        this.isOpen = true;

        if(dontcareformemory) {
            rasterArray = this.rasterToArray();
            //System.out.println(this.rasterArray.length + " " + this.rasterArray[0].length);
        }
    }
    public synchronized void open(String filename){
        this.raster = (gdal.Open(filename, gdalconst.GA_Update));
        this.band = this.raster.GetRasterBand(1);
    }

    public double getMinX(){
        return this.rasterExtent[0];
    }

    public double getMaxX(){
        return this.rasterExtent[2];
    }

    public double getMinY(){
        return this.rasterExtent[1];
    }

    public double getMaxY(){
        return this.rasterExtent[3];
    }

    public double getResolution(){
        return this.resolution;
    }

    public void rasterToArray(float[][] array, double arrayMinx, double arrayMaxx, double arrayMiny, double arrayMaxy){

        Band tifBand = this.raster.GetRasterBand(1);
        int number_of_pix_x = this.raster.getRasterXSize();
        int number_of_pix_y = this.raster.getRasterYSize();

        float[] floatArray = new float[number_of_pix_x];

        System.out.println("Reading raster line by line");

        double diffx = this.getMinX() - arrayMinx;
        double diffy = this.getMaxY() - arrayMaxy;

        int offsetx = (int)(diffx / this.getResolution());
        int offsety = (int)(diffy / this.getResolution());


        long startTime = System.currentTimeMillis();

        for(int y = 0; y < number_of_pix_y; y++) {

            tifBand.ReadRaster(0, y, number_of_pix_x, 1, floatArray);

            for (int x = 0; x < number_of_pix_x; x++) {

                //System.out.println("line " + y);
                float value = floatArray[x];

                array[x + offsetx][y - offsety] = value;

            }
        }
    }

    public float[][] rasterToArray(){

        Band tifBand = this.raster.GetRasterBand(1);
        int number_of_pix_x = this.raster.getRasterXSize();
        int number_of_pix_y = this.raster.getRasterYSize();

        //System.out.println(number_of_pix_x + " " + number_of_pix_y);
        float[] floatArray = new float[number_of_pix_x];
        float[][] output = new float[number_of_pix_x][number_of_pix_y];

        //System.out.println("Reading raster line by line");


        for(int y = 0; y < number_of_pix_y; y++) {

            tifBand.ReadRaster(0, y, number_of_pix_x, 1, floatArray);

            for (int x = 0; x < number_of_pix_x; x++) {

                //System.out.println("line " + y);
                float value = floatArray[x];

                output[x][y] = value;

            }
        }

        return output;
    }

    public boolean containsPoint(double x, double y){
        if (x > this.getMinX() && x < this.getMaxX() && y > this.getMinY() && y < this.getMaxY()){
            return true;
        }
        else{
            return false;
        }
    }

    public synchronized void addQuery(){
        this.openedForNQueries++;
    }

    public void resetQueries(){
        this.openedForNQueries = 0;
    }

    public boolean isOpen(){
        return this.isOpen;
    }

    public int openedFor(){
        return this.openedForNQueries;
    }

    public synchronized float readValue(int x, int y){


        //if(!this.isOpen)
        //    this.open();

        if(dontcareformemory){
            return rasterArray[x][y];
        }
        else {

            band.ReadRaster(x, y, 1, 1, value);

            if (value[0] == this.nanValue[0])
                return Float.NaN;

            return value[0];
        }

    }

    public void readAreaToBuffer(){



    }

    public synchronized void setProcessingInProgress(boolean value){

        this.lock_1 = true;

        if(value)
            this.processingInProgress++;
        else
            this.processingInProgress--;

        this.lock_1 = false;
        //System.out.println("processingInProgress: " + this.processingInProgress);

    }

    //public synchronized void setProcessingFinished(){
    //    this.processingInProgress--;
    //}

    public synchronized boolean canClose(){

        if(lock_1)
            return false;

        if(this.processingInProgress == 0){
            return true;
        }
        else{
            return false;
        }
    }

}
