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

    public boolean outputRastrerCreated = false;
    float nanvalue = Float.NaN;
    int numBands = 0;
    boolean dontcareformemory = true;
    float[][] rasterArray = new float[1][1];
    boolean lock_1 = false;
    int id;
    int processingInProgress = 0;
    public ArrayList<String> metadatas = new ArrayList<String>();
    public ArrayList<String> metadatas_values = new ArrayList<String>();
    public Float[] nanValue;
    float[] value = new float[1];

    public String filename = null;

    public Dataset raster;
    public Dataset rasterMask;

    public Dataset additionalBands;

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

    public ArrayList<Band> newBands = new ArrayList<Band>();

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

        if(outputRastrerCreated)
            return;

        outputRastrerCreated = true;
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
        options.add("COMPRESS=LZW");
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

    public void compressAndReplaceDataset() {
        gdal.AllRegister();

        if (this.rasterMask == null) {
            System.err.println("Input dataset is null.");
        }

        // Get original filename
        String originalPath = this.rasterMask.GetDescription();
        if (originalPath == null || originalPath.isEmpty()) {
            throw new toolException("Dataset does not have a valid filename.");

        }

        // Create temp path
        String tempPath = originalPath + ".tmp_compressed.tif";

        // Get GTiff driver
        Driver driver = gdal.GetDriverByName("GTiff");

        // Define compression options
        Vector<String> compressOptions = new Vector<>();
        compressOptions.add("COMPRESS=LZW");
        compressOptions.add("TILED=YES");
        compressOptions.add("BLOCKXSIZE=256");
        compressOptions.add("BLOCKYSIZE=256");

        // Create compressed copy
        Dataset compressedDataset = driver.CreateCopy(tempPath, this.rasterMask, 0, compressOptions);
        if (compressedDataset == null) {
            throw new toolException("Compression failed.");
        }

        // Close both datasets before replacing file
        this.rasterMask.delete();
        compressedDataset.delete();

        // Replace original with compressed
        File originalFile = new File(originalPath);
        File tempFile = new File(tempPath);

        if (!originalFile.delete()) {
            System.err.println("Failed to delete original file: " + originalPath);

        }

        if (!tempFile.renameTo(originalFile)) {
            System.err.println("Failed to rename compressed file.");

        }

        // Reopen compressed dataset
        Dataset newDataset = gdal.Open(originalPath);
        if (newDataset == null) {
            System.err.println("Failed to reopen compressed dataset.");
        }


    }


    public synchronized void readRaster(String filename) {
        this.raster = (gdal.Open(filename, readOrWrite));
        this.nanValue = new Float[1];
        Double[] nanValueDouble = new Double[1];
        this.raster.GetRasterBand(1).GetNoDataValue(nanValueDouble);

        if(nanValueDouble[0] == null || nanValueDouble[0].isNaN() || nanValueDouble[0].isInfinite()){

        }
        else
            this.nanValue[0] = nanValueDouble[0].floatValue();

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

    public double[] realWorldCoordinateToPixelCoordinate(double x, double y){

        if(!this.isOpen)
            this.open();

        double[] pixelCoordinate = new double[2];

        pixelCoordinate[0] = (x - this.geoTransform[0]) / this.geoTransform[1];
        pixelCoordinate[1] = (y - this.geoTransform[3]) / this.geoTransform[5];

        //System.out.println(Arrays.toString(pixelCoordinate));

        return pixelCoordinate;

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

    public void retileRaster(String outputDirectory, int tileSizeX, int tileSizeY) {

        if (this.raster == null) {
            throw new toolException("Raster is not opened. Please open the raster before retile.");
        }

        int numTilesX = (int) Math.ceil((double) this.number_of_pix_x / tileSizeX);
        int numTilesY = (int) Math.ceil((double) this.number_of_pix_y / tileSizeY);

        for (int tileY = 0; tileY < numTilesY; tileY++) {
            for (int tileX = 0; tileX < numTilesX; tileX++) {

                int startX = tileX * tileSizeX;
                int startY = tileY * tileSizeY;
                int endX = Math.min(startX + tileSizeX, this.number_of_pix_x);
                int endY = Math.min(startY + tileSizeY, this.number_of_pix_y);

                String outputFilename = String.format("%s/tile_%d_%d.tif", outputDirectory, tileX, tileY);
                Driver driver = gdal.GetDriverByName("GTiff");
                Dataset outputDataset = driver.Create(outputFilename, endX - startX, endY - startY, 1, gdalconst.GDT_Float32);

                Band outputBand = outputDataset.GetRasterBand(1);
                float[] dataRow = new float[endX - startX];

                for (int y = startY; y < endY; y++) {
                    this.band.ReadRaster(startX, y, endX - startX, 1, dataRow);
                    outputBand.WriteRaster(0, y - startY, endX - startX, 1, dataRow);
                }

                outputBand.FlushCache();
                outputDataset.FlushCache();
                outputDataset.delete();
            }
        }

    }

    public void retileRasterWithGeotransform(String outputDirectory, int tileSizeX, int tileSizeY) {

        if (this.raster == null) {
            throw new toolException("Raster is not opened. Please open the raster before retile.");
        }

        int numTilesX = (int) Math.ceil((double) this.number_of_pix_x / tileSizeX);
        int numTilesY = (int) Math.ceil((double) this.number_of_pix_y / tileSizeY);

        for (int tileY = 0; tileY < numTilesY; tileY++) {
            for (int tileX = 0; tileX < numTilesX; tileX++) {

                int startX = tileX * tileSizeX;
                int startY = tileY * tileSizeY;
                int endX = Math.min(startX + tileSizeX, this.number_of_pix_x);
                int endY = Math.min(startY + tileSizeY, this.number_of_pix_y);

                String outputFilename = String.format("%s/tile_%d_%d.tif", outputDirectory, tileX, tileY);
                Driver driver = gdal.GetDriverByName("GTiff");
                Dataset outputDataset = driver.Create(outputFilename, endX - startX, endY - startY, 1, gdalconst.GDT_Float32);

                double[] geoTransform = new double[6];
                System.arraycopy(this.geoTransform, 0, geoTransform, 0, 6);
                geoTransform[0] += startX * geoTransform[1];
                geoTransform[3] += startY * geoTransform[5];

                outputDataset.SetGeoTransform(geoTransform);
                outputDataset.SetProjection(this.raster.GetProjection());

                Band outputBand = outputDataset.GetRasterBand(1);
                float[] dataRow = new float[endX - startX];

                for (int y = startY; y < endY; y++) {
                    this.band.ReadRaster(startX, y, endX - startX, 1, dataRow);
                    outputBand.WriteRaster(0, y - startY, endX - startX, 1, dataRow);
                }

                outputBand.FlushCache();
                outputDataset.FlushCache();
                outputDataset.delete();
            }
        }


    }

    public void retileRasterWithGeotransform(String outputDirectory, int tileSizeX, int tileSizeY, int overlap) {

        if (this.raster == null) {
            throw new toolException("Raster is not opened. Please open the raster before retile.");
        }

        if (overlap >= tileSizeX || overlap >= tileSizeY) {
            throw new IllegalArgumentException("Overlap must be smaller than tile size.");
        }

        int strideX = tileSizeX - overlap;
        int strideY = tileSizeY - overlap;

        // Calculate number of tiles needed using stride steps
        int numTilesX = (int) Math.ceil((double)(this.number_of_pix_x - overlap) / strideX);
        int numTilesY = (int) Math.ceil((double)(this.number_of_pix_y - overlap) / strideY);

        for (int tileY = 0; tileY < numTilesY; tileY++) {
            for (int tileX = 0; tileX < numTilesX; tileX++) {

                int startX = tileX * strideX;
                int startY = tileY * strideY;
                int endX = Math.min(startX + tileSizeX, this.number_of_pix_x);
                int endY = Math.min(startY + tileSizeY, this.number_of_pix_y);

                String outputFilename = String.format("%s/tile_%d_%d.tif", outputDirectory, tileX, tileY);
                Driver driver = gdal.GetDriverByName("GTiff");
                Dataset outputDataset = driver.Create(outputFilename, endX - startX, endY - startY, 1, gdalconst.GDT_Float32);

                double[] geoTransform = new double[6];
                System.arraycopy(this.geoTransform, 0, geoTransform, 0, 6);
                geoTransform[0] += startX * geoTransform[1];
                geoTransform[3] += startY * geoTransform[5];

                outputDataset.SetGeoTransform(geoTransform);
                outputDataset.SetProjection(this.raster.GetProjection());

                Band outputBand = outputDataset.GetRasterBand(1);
                float[] dataRow = new float[endX - startX];

                for (int y = startY; y < endY; y++) {
                    this.band.ReadRaster(startX, y, endX - startX, 1, dataRow);
                    outputBand.WriteRaster(0, y - startY, endX - startX, 1, dataRow);
                }

                outputBand.FlushCache();
                outputDataset.FlushCache();
                outputDataset.delete();
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

    public synchronized float[] readValue(int x, int y, int xSize, int ySize){

        if(!this.isOpen)
            this.open();

        float[] value = new float[xSize * ySize];

        band.ReadRaster(x, y, xSize, ySize, value);

        return value;

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

    public void addBands(int numAddedBands, ArrayList<Float> bandValues){

        if(!this.isOpen)
            this.open();

        // Create a new tif file (dataset) with numaddedbands + 1
        // bands and copy the data from the original raster to the new raster.
        if(this.raster == null){
            throw new toolException("Raster is not opened. Please open the raster before adding bands.");
        }
        if(this.raster.GetRasterBand(1) == null){
            throw new toolException("Raster band is not available. Please check the raster file.");
        }
        if(this.raster.GetRasterBand(1).GetDataset() == null){
            throw new toolException("Raster band dataset is not available. Please check the raster file.");
        }
        this.numBands = this.raster.GetRasterCount() + numAddedBands;
        Driver driver = gdal.GetDriverByName("GTiff");
        Vector<String> options = new Vector<>();
        options.add("COMPRESS=LZW");
        options.add("TILED=YES");
        //options.add("BLOCKXSIZE=256");
        //options.add("BLOCKYSIZE=256");
        this.additionalBands = driver.Create(
                this.filename,              // filename
                this.number_of_pix_x,        // width
                this.number_of_pix_y,        // height
                this.numBands,               // number of bands
                gdalconstConstants.GDT_Float32, // data type
                options                      // creation options
        );

        double[] rasterExtent = new double[6];
        this.raster.GetGeoTransform(rasterExtent);
        this.additionalBands.SetGeoTransform(rasterExtent);
        this.additionalBands.SetProjection(this.raster.GetProjection());
        //System.out.println(Arrays.toString(rasterExtent) + " " + this.number_of_pix_x + " " + this.number_of_pix_y + " " + this.filename);

        // First add the original raster values to the additionalBands raster
        Band originalBand = this.band;
        Band newBand = this.additionalBands.GetRasterBand(1); // writable band

        float[] dataRow = new float[this.number_of_pix_x];
        for (int y = 0; y < this.number_of_pix_y; y++) {
            originalBand.ReadRaster(0, y, this.number_of_pix_x, 1, dataRow);
            newBand.WriteRaster(0, y, this.number_of_pix_x, 1, dataRow);
        }

        for(int i = 1; i <= numAddedBands; i++){

            Band addedBand = this.additionalBands.GetRasterBand(i + 1); // writable band
            addedBand.SetNoDataValue(-9999f);
            addedBand.Fill(-9999f);

            // Write the values from bandValues to the new band
            for (int y = 0; y < this.number_of_pix_y; y++) {
                float[] dataRow2 = new float[this.number_of_pix_x];

                // Fill the datarow with bandValues.get(i)
                for (int x = 0; x < this.number_of_pix_x; x++) {
                    dataRow2[x] = bandValues.get(i-1); // Default value
                }

                //for (int x = 0; x < this.number_of_pix_x; x++) {
                //    float value = bandValues.get(i-1);
                    //if (value != -9999f) {
                addedBand.WriteRaster(0, y, this.number_of_pix_x, 1, dataRow2);
                    //}
                //}
            }
        }


        // Flush the cache to ensure all data is written

        this.additionalBands.FlushCache();

    }

    public synchronized void syncToDisk(){

        if(!this.isOpen)
            this.open();

        if(!this.outputRastrerCreated)
            return;
        //this.band.FlushCache();
        //this.raster.FlushCache();

        this.bandClip.FlushCache();
        this.rasterMask.FlushCache();




    }



    public synchronized void closeRasterMask(){

        if(this.rasterMask != null){
            this.rasterMask.delete();
            this.bandClip = null;
            this.bandClipFloat = null;
            this.rasterMask = null;
        }

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

    // This adds ALL values from raster2 to this raster that are within the bounds of this raster,
    // The other raster may or may not be of same resolution, so check based on the center pixel
    // coordinates of this raster the value from the other raster.
    public void addValuesFromAnotherRaster(gdalRaster raster2, int bandNumberToWriteOn){

        if(!this.isOpen)
            this.open();

        if(!raster2.isOpen)
            raster2.open();

        if(this.number_of_pix_x != raster2.number_of_pix_x || this.number_of_pix_y != raster2.number_of_pix_y){
            throw new toolException("The two rasters do not have the same dimensions");
        }

        Band bandToWrite = this.raster.GetRasterBand(bandNumberToWriteOn);

        float[] dataRow = new float[this.number_of_pix_x];

        for(int y = 0; y < this.number_of_pix_y; y++){
            for(int x = 0; x < this.number_of_pix_x;x++){

                float value = raster2.readValue(x, y);

                if(value != raster2.nanValue[0]){
                    dataRow[x] = value;
                }
                else{
                    dataRow[x] = -9999f;
                }

            }
            bandToWrite.WriteRaster(0, y, this.number_of_pix_x, 1, dataRow);
        }

        bandToWrite.FlushCache();
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
