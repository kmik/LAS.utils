package utils;

import com.fasterxml.jackson.core.sym.NameN;
import err.toolException;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

public class gdalRaster {

    int numBands = 0;
    boolean dontcareformemory = true;
    float[][] rasterArray = new float[1][1];
    boolean lock_1 = false;
    int id;
    int processingInProgress = 0;
    public ArrayList<String> metadatas = new ArrayList<String>();
    public ArrayList<String> metadatas_values = new ArrayList<String>();
    Double[] nanValue;
    float[] value = new float[1];

    public String filename = null;

    Dataset raster;
    Dataset rasterMask;

    int readOrWrite = gdalconst.GA_ReadOnly;

    Band band;

    Band bandClip;
    Band bandClipFloat;

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

    public gdalRaster(String filename, int id, int readOrWrite2) {

        this.readOrWrite = readOrWrite2;
        this.filename = filename;
        this.readRaster(filename);
        this.id = id;
        this.rasterExtent();
        //this.openedForNQueries_ = new int[cores];
    }

    public void writeIdToRaster(String odir) {

        // Remove existing .tif extension if present and append _clip_mask.tif
        File outputDir = new File(odir);
        if (!outputDir.isDirectory()) {
            throw new RuntimeException("Output directory does not exist or is not a directory: " + odir);
        }

// Extract base name from the input file
        String baseName = new File(this.filename).getName();
        if (baseName.toLowerCase().endsWith(".tif")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }

        String outputFilename = new File(outputDir, baseName + "_clip_mask.tif").getAbsolutePath();

        Driver driver = gdal.GetDriverByName("GTiff");
        Vector<String> options = new Vector<>();
        options.add("COMPRESS=NONE");
        options.add("TILED=YES");
        options.add("BLOCKXSIZE=256");
        options.add("BLOCKYSIZE=256");

        this.rasterMask = driver.Create(
                outputFilename,              // filename
                this.number_of_pix_x,        // width
                this.number_of_pix_y,        // height
                2,                           // number of bands
                gdalconstConstants.GDT_Float32, // data type
                options                      // creation options
        );

        double[] rasterExtent = new double[6];
        this.raster.GetGeoTransform(rasterExtent);

        this.rasterMask.SetGeoTransform(rasterExtent);
        this.rasterMask.SetProjection(this.raster.GetProjection());
        //System.out.println(Arrays.toString(rasterExtent) + " " + this.number_of_pix_x + " " + this.number_of_pix_y + " " + outputFilename);
        this.bandClip = this.rasterMask.GetRasterBand(1); // writable band

        this.bandClip.SetNoDataValue(-9999f);

        this.bandClipFloat = this.rasterMask.GetRasterBand(2); // writable band

        this.bandClipFloat.SetNoDataValue(-9999f);

        int fillStatus = this.bandClip.Fill(-9999f);
        int fillStatus2 = this.bandClipFloat.Fill(-9999f);

        if (fillStatus != 0) {
            throw new RuntimeException("Fill failed with error code: " + fillStatus);
        }
        if (fillStatus2 != 0) {
            throw new RuntimeException("Fill failed with error code: " + fillStatus);
        }


    }

    public synchronized void readRaster(String filename) {
        this.raster = (gdal.Open(filename, readOrWrite));
        this.nanValue = new Double[1];
        this.raster.GetRasterBand(1).GetNoDataValue(this.nanValue);

        // Add a raster band
        if (this.raster == null) {
            throw new toolException("Raster " + filename + " could not be opened. Please check the file path and format.");
        }



    }

    public synchronized void addMetadataitem(String item){
        this.metadatas.add(item);

        this.metadatas_values.add(this.raster.GetMetadataItem(item));

        if(this.raster.GetMetadataItem(item) == null){
            throw new toolException("Metadata item " + item + " not found in raster " + this.filename + " metadata");
        }

        System.out.println(this.metadatas_values.get(this.metadatas_values.size()-1));

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
        this.raster = (gdal.Open(filename, readOrWrite));
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

                //System.out.println("line " + y + "  " + this.filename);
                float value = floatArray[x];

                output[x][y] = value;
                //System.out.println(value);

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
            //System.out.println("here");
            return rasterArray[x][y];
        }
        else {

            band.ReadRaster(x, y, 1, 1, value);

            if (value[0] == this.nanValue[0])
                return Float.NaN;

            return value[0];
        }

    }

    public synchronized void setValue(int x, int y, int value){

        if(!this.isOpen)
            this.open();

        this.bandClip.WriteRaster(x, y, 1, 1, new int[]{value});


    }



    public synchronized void setValue2(int x, int y, float value, float value2){

        if(!this.isOpen)
            this.open();

        this.bandClip.WriteRaster(x, y, 1, 1, new float[]{value});
        this.bandClipFloat.WriteRaster(x, y, 1, 1, new float[]{value2});


    }

    public synchronized void setValue(int x, int y, int xSize, int ySize, float[] value1, float[] value2){

        if(!this.isOpen)
            this.open();

        try {
            this.bandClip.WriteRaster(x, y, xSize, ySize, value1);
            this.bandClipFloat.WriteRaster(x, y, xSize, ySize, value2);
        } catch (Exception e) {
            System.err.println("Error writing raster at (" + x + ", " + y + "): " + e.getMessage());
            e.printStackTrace();
            // Optionally: handle error, retry, or abort
        }
    }

    public synchronized void syncToDisk(){

        if(!this.isOpen)
            this.open();

        //this.band.FlushCache();
        //this.raster.FlushCache();

        this.bandClip.FlushCache();
        this.rasterMask.FlushCache();

    }
    public void readAreaToBuffer(){



    }


    /**
     * Synchronize two rasters with each other. If the other raster has a value that is below the threshold, the value of this raster is set that of the other raster.
     * @param chm
     * @param threshold
     */

    public void syncWithAnotherChm(gdalRaster chm, float threshold, String filename){

        if(this.number_of_pix_x != chm.number_of_pix_x || this.number_of_pix_y != chm.number_of_pix_y){
            throw new toolException("The two rasters do not have the same dimensions");
        }


        if(!chm.isOpen)
            chm.open();

        File f = new File(filename);

        if(f.exists()){
            f.delete();
        }

        try {
            f.createNewFile();
        }catch (Exception e){
            e.printStackTrace();
        }

        Dataset dataset = null;
        Driver driver = null;
        driver = gdal.GetDriverByName("GTiff");
        driver.Register();

        dataset = driver.Create(filename, this.number_of_pix_x, this.number_of_pix_y, 1, gdalconst.GDT_Float32);

        Band band2 = dataset.GetRasterBand(1);    // writable band

        rasterManipulator rM = new rasterManipulator(band2);
        float[] dataRow = new float[this.number_of_pix_x];

        for(int y = 0; y < this.number_of_pix_y; y++){
            for(int x = 0; x < this.number_of_pix_x;x++){

                float value = chm.readValue(x, y);

                if(value <= threshold)
                    dataRow[x] = value;
                else
                    dataRow[x] = this.readValue(x, y);

            }

            rM.setValue(y, dataRow);
            //System.out.println(Arrays.toString(dataRow));
        }

        //System.exit(1);
        band2.FlushCache();
        dataset.FlushCache();

        band2.delete();
        dataset.delete();


    }

    public void toTxt() {

        File outputFile = this.declareTxtFile();

        if(outputFile.exists())
            outputFile.delete();

        try {
            outputFile.createNewFile();
        }catch (Exception e){
            e.printStackTrace();
        }

        if(!this.isOpen)
            this.open();

        //System.out.println(Arrays.toString(this.geoTransform));
        //System.out.println("HERE");
        //System.exit(1);
        // The first two columns are x and y coordinates, followed by the value of the raster.

        try {
            java.io.FileWriter fw = new java.io.FileWriter(outputFile);
            java.io.BufferedWriter bw = new java.io.BufferedWriter(fw);

            for (int y = 0; y < this.number_of_pix_y; y++) {
                for (int x = 0; x < this.number_of_pix_x; x++) {

                    float value = this.readValue(x, y);

                    double realCoordinateX = this.geoTransform[0] + x * this.geoTransform[1] + y * this.geoTransform[2];
                    double realCoordinateY = this.geoTransform[3] + x * this.geoTransform[4] + y * this.geoTransform[5];

                    bw.write(realCoordinateX + " " + realCoordinateY + " " + value + "\n");

                }
            }

            bw.close();
            fw.close();

        }catch (Exception e){
            e.printStackTrace();
        }



    }

    /**
     * Declare a txt file for writing the raster to. The directory is the directory the raster is in and the filename is the name of the raster with .txt extension.
     */
    public File declareTxtFile(){

        File outputFileName = new File(this.filename);
        String outputFileNameString = outputFileName.getName();
        outputFileNameString = outputFileNameString.substring(0, outputFileNameString.length() - 4);
        outputFileNameString = outputFileNameString + ".txt";

        File f = new File(outputFileName.getParent() + "/" + outputFileNameString);

        return f;
    }

    public void copyRasterToFile(String filename){

        File f = new File(filename);

        if(f.exists()){
            f.delete();
        }

        try {
            f.createNewFile();
        }catch (Exception e){
            e.printStackTrace();
        }

        Dataset dataset = null;
        Driver driver = null;
        driver = gdal.GetDriverByName("GTiff");
        driver.Register();

        dataset = driver.Create(filename, this.number_of_pix_x, this.number_of_pix_y, 1, gdalconst.GDT_Float32);

        Band band2 = dataset.GetRasterBand(1);    // writable band


        rasterManipulator rM = new rasterManipulator(band2);
        float[] dataRow = new float[this.number_of_pix_x];

        for(int y = 0; y < this.number_of_pix_y; y++){
            for(int x = 0; x < this.number_of_pix_x;x++){

                dataRow[x] = this.readValue(x, y);

            }
            rM.setValue(y, dataRow);
        }
        band2.FlushCache();
        dataset.FlushCache();


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
