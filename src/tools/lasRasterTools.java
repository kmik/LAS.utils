package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import com.drew.metadata.Directory;
import err.toolException;
//import javafx.util.Pair;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.ogr.*;
import org.gdal.osr.SpatialReference;
import utils.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import static org.gdal.gdalconst.gdalconstConstants.GCI_GrayIndex;
import static tools.createCHM.fo;


public class lasRasterTools {

    boolean[][] mask = null;
    double[] geoTransform = null;

    ArrayList<MyPair<String, String>> metadata = new ArrayList<MyPair<String, String>>();
    argumentReader aR;
    public lasRasterTools(){

    }

    public lasRasterTools(argumentReader aR){
        this.aR = aR;
    }

    public void printTimeInMinutesSeconds(long timeInMilliseconds, String message){
        System.out.println(message + " -- Time taken: " + (timeInMilliseconds / 1000) / 60 + " minutes and " + (timeInMilliseconds / 1000) % 60 + " seconds.");
    }

    public void readMetadata(String metadatafile){

        File metadataFile = new File(metadatafile);

        if(!metadataFile.exists()){
            throw new toolException("Metadata file does not exist!");
        }


        try {
            BufferedReader br = new BufferedReader(new FileReader(metadataFile));
            String line = br.readLine();
            while (line != null) {
                String[] temp = line.split("=");
                metadata.add(new MyPair<String, String>(temp[0], temp[1]));
                //just made a change
                line = br.readLine();
            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    public ArrayList<Dataset> readMultipleRasters(argumentReader aR){

        ArrayList<Dataset> rasters = new ArrayList<Dataset>();

        for(File raster : aR.inputFiles){
            rasters.add(gdal.Open(raster.getAbsolutePath(), gdalconst.GA_ReadOnly));
        }

        return rasters;

    }

    public ArrayList<Dataset> readMultipleRasters(argumentReader aR, ArrayList<double[]> areasOfInterest, rasterCollection rasterBank){

        ArrayList<Dataset> rasters = new ArrayList<Dataset>();

        int counter = 0;

        for(File raster : aR.inputFiles){

            Dataset tmp = gdal.Open(raster.getAbsolutePath(), gdalconst.GA_ReadOnly);

            double[] rasterExtent = new double[6];
            tmp.GetGeoTransform(rasterExtent);


            //tmp.GetGeoTransform(geoTransform);

            double[] rasterWidthHeight_ = new double[2];
            rasterWidthHeight_[0] = tmp.GetRasterXSize();
            rasterWidthHeight_[1] = tmp.GetRasterYSize();

            double resolution = rasterExtent[1];

            int number_of_pix_x = (int) rasterWidthHeight_[0];
            int number_of_pix_y = (int) rasterWidthHeight_[1];



            rasterExtent = new double[]{rasterExtent[0],rasterExtent[0] + rasterExtent[1] * rasterWidthHeight_[0], rasterExtent[3] + rasterExtent[5] * rasterWidthHeight_[1],  rasterExtent[3]};


            for(int i = 0; i < areasOfInterest.size(); i++) {
                if (isOverlapping(areasOfInterest.get(i)[0], areasOfInterest.get(i)[1], areasOfInterest.get(i)[2], areasOfInterest.get(i)[3], rasterExtent)) {
                    rasters.add(tmp);
                    if(aR.metadataitems.size() == 0)
                        rasterBank.addRaster(new gdalRaster(tmp.GetDescription(), counter++));
                    else{
                        rasterBank.addRaster(new gdalRaster(tmp.GetDescription(), counter++), aR);
                        //n_metadataItems = aR.metadataitems.size();
                    }
                }
            }

        }


        return rasters;

    }

    public void readMultipleRasters(argumentReader aR, double[] areaOfInterest, rasterCollection rasterBank){

        //ArrayList<Dataset> rasters = new ArrayList<Dataset>();

        int counter = 0;

        for(File raster : aR.inputFiles){

            Dataset tmp = gdal.Open(raster.getAbsolutePath(), gdalconst.GA_ReadOnly);

            double[] rasterExtent = new double[6];
            tmp.GetGeoTransform(rasterExtent);


            //tmp.GetGeoTransform(geoTransform);

            double[] rasterWidthHeight_ = new double[2];
            rasterWidthHeight_[0] = tmp.GetRasterXSize();
            rasterWidthHeight_[1] = tmp.GetRasterYSize();

            double resolution = rasterExtent[1];

            int number_of_pix_x = (int) rasterWidthHeight_[0];
            int number_of_pix_y = (int) rasterWidthHeight_[1];



            rasterExtent = new double[]{rasterExtent[0],rasterExtent[0] + rasterExtent[1] * rasterWidthHeight_[0], rasterExtent[3] + rasterExtent[5] * rasterWidthHeight_[1],  rasterExtent[3]};


                if (isOverlapping(areaOfInterest[0], areaOfInterest[1], areaOfInterest[2], areaOfInterest[3], rasterExtent)) {
                    //rasters.add(tmp);
                    if(aR.metadataitems.size() == 0)
                        rasterBank.addRaster(new gdalRaster(tmp.GetDescription(), counter++));
                    else{
                        rasterBank.addRaster(new gdalRaster(tmp.GetDescription(), counter++), aR);
                        //n_metadataItems = aR.metadataitems.size();
                    }

            }

                tmp.delete();

        }

    }

    public class MyPair<K, V> {
        private final K key;
        private final V value;

        public MyPair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "(" + key + ", " + value + ")";
        }
    }

    public void readMultipleRasters(argumentReader aR, double[] areaOfInterest, rasterCollection rasterBank, rasterCollection rasterBank2){

        //ArrayList<Dataset> rasters = new ArrayList<Dataset>();

        int counter = 0;

        for(gdalRaster raster : rasterBank2.rasters){

            double[] rasterExtent = raster.rasterExtent;

            //System.out.println(Arrays.toString(rasterExtent));
            //System.out.println(Arrays.toString(areaOfInterest));

            //System.exit(1);
            if (isOverlapping(areaOfInterest[0], areaOfInterest[1], areaOfInterest[2], areaOfInterest[3], rasterExtent)) {
                //rasters.add(tmp);
                if(aR.metadataitems.size() == 0)
                    rasterBank.addRaster(new gdalRaster(raster.filename, counter++));
                else{
                    rasterBank.addRaster(new gdalRaster(raster.filename, counter++), aR);
                    //n_metadataItems = aR.metadataitems.size();
                }

            }

        }


        //return rasters;

    }

    public boolean isOverlapping(double minx, double maxx, double miny, double maxy, double[] rasterExtent){

        if (minx > rasterExtent[2] || maxx < rasterExtent[0] || miny > rasterExtent[3] || maxy < rasterExtent[1]){
            return false;
        }
        else{
            return true;
        }

    }

    public void readRasters(){

        gdal.AllRegister();

        if(aR.ref.size() > 1){
            throw new toolException("Only one reference raster can be used at a time!");
        }
        if(aR.ref.size() == 0){
            throw new toolException("No reference raster provided!");
        }

        Dataset tifDataset = gdal.Open(aR.ref.get(0).getAbsolutePath(), gdalconst.GA_ReadOnly);
        Band tifBand = tifDataset.GetRasterBand(1);
        int number_of_pix_x = tifDataset.getRasterXSize();
        int number_of_pix_y = tifDataset.getRasterYSize();

        this.geoTransform = tifDataset.GetGeoTransform();

        this.mask = new boolean[tifDataset.GetRasterXSize()][tifDataset.GetRasterYSize()];

        float[] floatArray = new float[number_of_pix_x];

        System.out.println("Reading raster line by line");

        long startTime = System.currentTimeMillis();

        for(int y = 0; y < number_of_pix_y; y++) {

            tifBand.ReadRaster(0, y, number_of_pix_x, 1, floatArray);

            for (int x = 0; x < number_of_pix_x; x++) {

                //System.out.println("line " + y);
                float value = floatArray[x];

                if (value == 1.0f) {
                    mask[x][y] = true;

                }else{

                }
            }
        }

        long endTime = System.currentTimeMillis();

        printTimeInMinutesSeconds(endTime - startTime, "Raster to 2d array");

    }

    public void readRasters2(){

        gdal.AllRegister();

        if(aR.ref.size() > 1){
            throw new toolException("Only one reference raster can be used at a time!");
        }
        if(aR.ref.size() == 0){
            throw new toolException("No reference raster provided!");
        }

        String fileName = aR.ref.get(0).getAbsolutePath();
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        int width = image.getWidth();
        int height = image.getHeight();

        this.mask = new boolean[width][height];
        long startTime = System.currentTimeMillis();


        System.out.println("Reading raster line by line");

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                if (rgb == 1) {
                    mask[x][y] = true;

                }else{

                }
            }
        }
        long endTime = System.currentTimeMillis();

        printTimeInMinutesSeconds(endTime - startTime, "Raster to 2d array");

    }

    public void clip(){

        ForkJoinPool customThreadPool = new ForkJoinPool(aR.cores);

        try {

            customThreadPool.submit(() ->

                    IntStream.range(0, aR.inputFiles.size()).parallel().forEach(i_ -> {
                        LASReader temp = null;

                        LasPoint tempPoint = new LasPoint();
                        try {
                            temp = new LASReader(aR.inputFiles.get(i_));
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                        int thread_n = aR.pfac.addReadThread(temp);

                        File outFile = null;
                        pointWriterMultiThread pw = null;

                        try {
                            outFile = aR.createOutputFile(temp);

                            pw = new pointWriterMultiThread(outFile, temp, "las2las", aR);
                        }catch (Exception e){
                            System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
                            return;
                        }
                        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);
                        aR.pfac.addWriteThread(thread_n, pw, buf);

                        float[] data = new float[1];

                        for (long i = 0; i < temp.getNumberOfPointRecords(); i += 20000) {

                            int maxi = (int) Math.min(20000, Math.abs(temp.getNumberOfPointRecords() - i));

                            try {
                                aR.pfac.prepareBuffer(thread_n, i, maxi);
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                            for (int j_ = 0; j_ < maxi; j_++) {

                                temp.readFromBuffer(tempPoint);

                                int x = (int) Math.round((tempPoint.x - geoTransform[0]) / geoTransform[1]);
                                int y = (int) Math.round((tempPoint.y - geoTransform[3]) / geoTransform[5]);

                                if(x < 0 || x >= mask.length || y < 0 || y >= mask[0].length){
                                    continue;
                                }
                                if(this.mask[x][y] == false){

                                    try {

                                        aR.pfac.writePoint(tempPoint, i+j_, thread_n);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        System.exit(1);
                                    }

                                }
                            }
                        }

                        aR.pfac.closeThread(thread_n);

                    })).get();
            }catch (Exception e){
                e.printStackTrace();
        }


    }

    public void clip2(){

        //ForkJoinPool customThreadPool = new ForkJoinPool(aR.cores);

        try {

            //customThreadPool.submit(() ->
            for(int i_ = 0; i_ < aR.inputFiles.size(); i_++) {
                //IntStream.range(0, aR.inputFiles.size()).parallel().forEach(i_ -> {
                LASReader temp = null;

                LasPoint tempPoint = new LasPoint();
                try {
                    temp = new LASReader(aR.inputFiles.get(i_));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                int thread_n = aR.pfac.addReadThread(temp);

                File outFile = null;
                pointWriterMultiThread pw = null;

                try {
                    outFile = aR.createOutputFile(temp);

                    pw = new pointWriterMultiThread(outFile, temp, "las2las", aR);
                } catch (Exception e) {
                    System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
                    return;
                }
                LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);
                aR.pfac.addWriteThread(thread_n, pw, buf);

                float[] data = new float[1];

                for (long i = 0; i < temp.getNumberOfPointRecords(); i += 20000) {

                    int maxi = (int) Math.min(20000, Math.abs(temp.getNumberOfPointRecords() - i));

                    try {
                        aR.pfac.prepareBuffer(thread_n, i, maxi);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    for (int j_ = 0; j_ < maxi; j_++) {

                        temp.readFromBuffer(tempPoint);

                        int x = (int) Math.round((tempPoint.x - geoTransform[0]) / geoTransform[1]);
                        int y = (int) Math.round((tempPoint.y - geoTransform[3]) / geoTransform[5]);

                        if (this.mask[x][y] == false) {

                            try {

                                aR.pfac.writePoint(tempPoint, i + j_, thread_n);

                            } catch (Exception e) {
                                e.printStackTrace();
                                System.exit(1);
                            }

                        }
                    }
                }

                aR.pfac.closeThread(thread_n);
            }
                    //})).get();
        }catch (Exception e){
            e.printStackTrace();
        }


    }



    public void rasterize(LASReader pointCloud, double resolution){

        String outputFileNameMask = "notAssigned";
        String outputFileNameColor = "notAssigned";


        String year = "0000";

        for(MyPair pair : this.metadata){
            //System.out.println(pair.getKey() + " " + pair.getValue());

            if(pair.getKey().equals("DATA_DATE")){


                year = pair.getValue().toString().substring(0, 4);

            }
        }

        String outputFileName = "";


        KarttaLehtiJako karttaLehtiJako = new KarttaLehtiJako();

        try {
            karttaLehtiJako.readFromFile(new File(""));
        }catch (Exception e){
            e.printStackTrace();
        }
        // point cloud geometric center
        double x__ = (pointCloud.getMinX() + pointCloud.getMaxX()) / 2.0;
        double y__ = (pointCloud.getMinY() + pointCloud.getMaxY()) / 2.0;

        String mapSheetName_ = karttaLehtiJako.getMapSheetNameByCoordinates(x__, y__);


        outputFileName = fo.createNewFileWithNewExtension(pointCloud.getFile(), "_raster.tif").getAbsolutePath();

        if(!year.equals("0000")){

            outputFileName = fo.createNewFileWithoutNewExtension(pointCloud.getFile(), mapSheetName_ + "_" + year + "_IPC.tif").getAbsolutePath();

            if(aR.outputMask){
                //outputFileNameMask = fo.createNewFileWithNewExtension(pointCloud.getFile(), "_raster_mask.tif").getAbsolutePath();
                outputFileNameMask = fo.createNewFileWithNewExtension(pointCloud.getFile(), mapSheetName_ + "_" + year + "_IPC_mask.tif").getAbsolutePath();
            }

            if(aR.rasterizeColor){
                outputFileNameColor = fo.createNewFileWithNewExtension(pointCloud.getFile(), "_raster_color.tif").getAbsolutePath();
            }

        }else {

            if (aR.outputMask) {
                outputFileNameMask = fo.createNewFileWithNewExtension(pointCloud.getFile(), "_raster_mask.tif").getAbsolutePath();
            }
            if (aR.rasterizeColor) {
                outputFileNameColor = fo.createNewFileWithNewExtension(pointCloud.getFile(), "_raster_color.tif").getAbsolutePath();
            }
        }

        double pointCloudMinX = pointCloud.getMinX() - aR.res/2.0;
        double pointCloudMinY = pointCloud.getMinY() - aR.res/2.0;
        double pointCloudMaxX = pointCloud.getMaxX() + aR.res/2.0;
        double pointCloudMaxY = pointCloud.getMaxY() + aR.res/2.0;

/*
        double pointCloudMinX = pointCloud.getMinX();
        double pointCloudMinY = pointCloud.getMinY();
        double pointCloudMaxX = pointCloud.getMaxX();
        double pointCloudMaxY = pointCloud.getMaxY();
*/


        int rasterWidth = (int) Math.ceil((pointCloudMaxX - pointCloudMinX) / resolution);
        int rasterHeight = (int) Math.ceil((pointCloudMaxY - pointCloudMinY) / resolution);

        double[] geoTransform = new double[6];
        geoTransform[0] = pointCloudMinX;
        geoTransform[1] = resolution;
        geoTransform[2] = 0;
        geoTransform[3] = pointCloudMaxY;
        geoTransform[4] = 0;
        geoTransform[5] = -resolution;

        LasPoint tempPoint = new LasPoint();

        org.gdal.gdal.Driver driver = null;
        driver = gdal.GetDriverByName("GTiff");
        driver.Register();

        Dataset cehoam;
        String compressionOptions = "COMPRESS=LZW";

        Dataset mask = null;
        Dataset color = null;
        try {
            cehoam = driver.Create(outputFileName, rasterWidth, rasterHeight, 1, gdalconst.GDT_Float32);
            //cehoam.SetMetadataItem("COMPRESSION", compressionOptions);

            if(aR.outputMask){
                mask = driver.Create(outputFileNameMask, rasterWidth, rasterHeight, 1, gdalconst.GDT_Byte);
                //mask.SetMetadataItem("COMPRESSION", compressionOptions);
            }

            if(aR.rasterizeColor){
                color = driver.Create(outputFileNameColor, rasterWidth, rasterHeight, aR.nBands, gdalconst.GDT_Byte);
                //color.SetMetadataItem("COMPRESSION", compressionOptions);
            }


            }catch (Exception e){
            System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
            return;
        }
        //cehoam.SetMetadataItem("test", "leaf-off");
        Band band = cehoam.GetRasterBand(1);

        Band maskBand = null;
        ArrayList<Band> colorBands = new ArrayList<Band>(aR.nBands);

        if(aR.outputMask){
            maskBand = mask.GetRasterBand(1);

        }

        if(aR.rasterizeColor) {
            for(int i = 0; i < aR.nBands; i++){
                colorBands.add(color.GetRasterBand(i+1));
            }
        }

        band.SetNoDataValue(Float.NaN);

        SpatialReference sr = new SpatialReference();

        sr.ImportFromEPSG(3067);

        cehoam.SetProjection(sr.ExportToWkt());
        cehoam.SetGeoTransform(geoTransform);

        if(aR.outputMask){
            mask.SetProjection(sr.ExportToWkt());
            mask.SetGeoTransform(geoTransform);
        }

        if(aR.rasterizeColor){
            color.SetProjection(sr.ExportToWkt());
            color.SetGeoTransform(geoTransform);
        }

        double[][] chm_array = new double[rasterWidth][rasterHeight];
        ArrayList<int[][]> color_array = new ArrayList<int[][]>(aR.nBands);
        boolean[][] mask_array = new boolean[rasterWidth][rasterHeight];

        double minx = pointCloud.getMinX();
        double miny = pointCloud.getMinY();
        double maxx = pointCloud.getMaxX();
        double maxy = pointCloud.getMaxY();

        long n = pointCloud.getNumberOfPointRecords();

        int thread_n = aR.pfac.addReadThread(pointCloud);

        byte[] colorValue = new byte[1];

        if(aR.rasterizeColor){
            for(int i = 0; i < aR.nBands; i++){
                color_array.add(new int[rasterWidth][rasterHeight]);
            }
        }


        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 20000) {

            int maxi = (int) Math.min(20000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                aR.pfac.prepareBuffer(thread_n, i, maxi);
            }catch (Exception e){
                e.printStackTrace();
                System.exit(1);
            }

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if(tempPoint.x < minx)
                    minx = tempPoint.x;
                if(tempPoint.y < miny)
                    miny = tempPoint.y;
                if(tempPoint.x > maxx)
                    maxx = tempPoint.x;

                int x = (int) Math.floor((tempPoint.x - geoTransform[0]) / geoTransform[1]);
                int y = (int) Math.floor((tempPoint.y - geoTransform[3]) / geoTransform[5]);

                if (x >= 0 && x < rasterWidth && y >= 0 && y < rasterHeight) {

                    if(Double.isNaN(chm_array[x][y]))
                        chm_array[x][y] = (double)tempPoint.z;
                    else if ( (float)tempPoint.z > chm_array[x][y])
                        chm_array[x][y] = (double)tempPoint.z;
                    else{

                    }

                    if(tempPoint.synthetic){
                        mask_array[x][y] = true;
                    }

                    if(aR.rasterizeColor){

                        if(aR.nBands == 3){

                            color_array.get(0)[x][y] = tempPoint.R;
                            color_array.get(1)[x][y] = tempPoint.G;
                            color_array.get(2)[x][y] = tempPoint.B;
                            //color_array[x][y][0] = tempPoint.R;

                            /*
                            // write tempPoint.R to raster
                            colorValue[0] = (byte)tempPoint.R;
                            colorBands.get(0).WriteRaster(x, y, 1, 1, colorValue);

                            // write tempPoint.G to raster
                            colorValue[0] = (byte)tempPoint.G;
                            colorBands.get(1).WriteRaster(x, y, 1, 1, colorValue);

                            // write tempPoint.B to raster
                            colorValue[0] = (byte)tempPoint.B;
                            colorBands.get(2).WriteRaster(x, y, 1, 1, colorValue);
*/


                        }
                        if(aR.nBands == 4){

                            color_array.get(0)[x][y] = tempPoint.R;
                            color_array.get(1)[x][y] = tempPoint.G;
                            color_array.get(2)[x][y] = tempPoint.B;
                            color_array.get(3)[x][y] = tempPoint.N;
                            /*
                            // write tempPoint.R to raster
                            colorValue[0] = (byte)tempPoint.R;
                            colorBands.get(0).WriteRaster(x, y, 1, 1, colorValue);

                            // write tempPoint.G to raster
                            colorValue[0] = (byte)tempPoint.G;
                            colorBands.get(1).WriteRaster(x, y, 1, 1, colorValue);

                            // write tempPoint.B to raster
                            colorValue[0] = (byte)tempPoint.B;
                            colorBands.get(2).WriteRaster(x, y, 1, 1, colorValue);

                            // write tempPoint.N to raster
                            colorValue[0] = (byte)tempPoint.N;
                            colorBands.get(3).WriteRaster(x, y, 1, 1, colorValue);


                             */
                        }

                    }
                }

            }
        }

        copyRasterContents(chm_array, band);

        if(aR.outputMask)
            copyRasterContents(mask_array, maskBand);

        if(aR.rasterizeColor) {

            if(aR.nBands == 4)
                colorBands.get(3).SetColorInterpretation(GCI_GrayIndex);
            for (int i = 0; i < aR.nBands; i++) {
                copyRasterContents(color_array.get(i), colorBands.get(i));
                //colorBands.get(i).SetColorInterpretation(GCI_GrayIndex);
            }
        }

        //String[] options = new String[]{"COMPRESS=LZW"};

        // Compression options
        String[] options = new String[]{
                "COMPRESS=DEFLATE",   // Use DEFLATE compression
                "PREDICTOR=1",        // Use predictor=1
                "TILED=YES",          // Enable tiling
                "BLOCKXSIZE=256",    // Tile width in pixels
                "BLOCKYSIZE=256"     // Tile height in pixels
        };

        cehoam.FlushCache();
        band.FlushCache();


        Dataset outputDataset = gdal.GetDriverByName("GTiff").CreateCopy(outputFileName, cehoam, 0, options);

        if(this.metadata.size() > 0){

            for(int i = 0; i < this.metadata.size(); i++){

                outputDataset.SetMetadataItem(this.metadata.get(i).getKey(), this.metadata.get(i).getValue());

            }

        }

        if(aR.outputMask){

            Dataset outputDatasetMask = gdal.GetDriverByName("GTiff").CreateCopy(outputFileNameMask, mask, 0, options);

            if(this.metadata.size() > 0){

                for(int i = 0; i < this.metadata.size(); i++){

                    outputDatasetMask.SetMetadataItem(this.metadata.get(i).getKey(), this.metadata.get(i).getValue());

                }

            }

            outputDatasetMask.FlushCache();
        }

        if(aR.rasterizeColor){


            Dataset outputDatasetColor = gdal.GetDriverByName("GTiff").CreateCopy(outputFileNameColor, color, 0, options);

            if(this.metadata.size() > 0){

                for(int i = 0; i < this.metadata.size(); i++){

                    outputDatasetColor.SetMetadataItem(this.metadata.get(i).getKey(), this.metadata.get(i).getValue());

                }

            }

            outputDatasetColor.FlushCache();
        }


        outputDataset.FlushCache();

    }

    public static void copyRasterContents(int[][] from, Band to){

        int x = to.getXSize();
        int y = to.getYSize();

        float[] read = new float[x*y];

        int counter = 0;

        for(int y_ = 0; y_ < from[0].length; y_++){
            for(int x_ = 0; x_ < from.length; x_++){

                read[counter++] = (float)from[x_][y_];

            }
        }

        to.WriteRaster(0, 0, x, y, read);

    }

    public static void copyRasterContents(double[][] from, Band to){

        int x = to.getXSize();
        int y = to.getYSize();

        float[] read = new float[x*y];

        int counter = 0;

        for(int y_ = 0; y_ < from[0].length; y_++){
            for(int x_ = 0; x_ < from.length; x_++){

                read[counter++] = (float)from[x_][y_];

            }
        }

        to.WriteRaster(0, 0, x, y, read);

    }


    public static void copyRasterContents(boolean[][] from, Band to){

        int x = to.getXSize();
        int y = to.getYSize();

        float[] read = new float[x*y];

        int counter = 0;

        for(int y_ = 0; y_ < from[0].length; y_++){
            for(int x_ = 0; x_ < from.length; x_++){

                if(from[x_][y_])
                    read[counter++] = 1;
                else
                    read[counter++] = 0;

            }
        }

        to.WriteRaster(0, 0, x, y, read);

    }

    public void zonalStatistics(argumentReader aR) throws Exception{

        rasterCollection rasterBank = new rasterCollection(aR.cores);


        this.aR = aR;
        String polyWKT = readShapefile(aR.poly);
        ArrayList<Integer> polyIds = new ArrayList<>();

        int nBands = 0;

        ArrayList<double[][]> polygons = readPolygonsFromWKT(polyWKT, polyIds);
        HashMap<Integer, ArrayList<double[][]>> polygonHoles = readPolygonHolesFromWKT(polyWKT, polyIds);

        ArrayList<Dataset> rasters = new ArrayList<Dataset>();
        ArrayList<Dataset> rastersSpectral = new ArrayList<Dataset>();

        for(File raster : aR.inputFiles){
            rasters.add(gdal.Open(raster.getAbsolutePath(), gdalconst.GA_ReadOnly));
        }

        ArrayList<double[]> rasterExtents = new ArrayList<double[]>();
        ArrayList<double[]> rasterWidthHeight = new ArrayList<double[]>();
        ArrayList<double[]> rasterBoundingBoxes = new ArrayList<double[]>();
        ArrayList<Band> rasterBands = new ArrayList<Band>();

        ArrayList<double[]> rasterExtents_spectral = new ArrayList<double[]>();
        ArrayList<double[]> rasterWidthHeight_spectral = new ArrayList<double[]>();
        ArrayList<double[]> rasterBoundingBoxes_spectral = new ArrayList<double[]>();
        ArrayList<ArrayList<Band>> rasterBands_spectral = new ArrayList<>();

        aR.lCMO.prepZonal(aR.inputFiles.get(0));

        int n_metadataItems = 0;
        int counter = 0;

        for(Dataset raster : rasters){

            double[] rasterExtent = new double[6];
            raster.GetGeoTransform(rasterExtent);
            //System.out.println(Arrays.toString(rasterExtent));
            rasterExtents.add(rasterExtent);

            double[] rasterWidthHeight_ = new double[2];
            rasterWidthHeight_[0] = raster.GetRasterXSize();
            rasterWidthHeight_[1] = raster.GetRasterYSize();

            rasterWidthHeight.add(rasterWidthHeight_);

            rasterBoundingBoxes.add(new double[]{rasterExtent[0], rasterExtent[3] + rasterExtent[5] * rasterWidthHeight_[1],rasterExtent[0] + rasterExtent[1] * rasterWidthHeight_[0],  rasterExtent[3]});

            if(aR.metadataitems.size() == 0)
                rasterBank.addRaster(new gdalRaster(raster.GetDescription(), counter++));
            else{
                rasterBank.addRaster(new gdalRaster(raster.GetDescription(), counter++), aR);
                n_metadataItems = aR.metadataitems.size();
            }

            rasterBands.add(raster.GetRasterBand(1));

        }

        if(aR.inputFilesSpectral.size() > 0){

            for(File raster : aR.inputFilesSpectral){
                rastersSpectral.add(gdal.Open(raster.getAbsolutePath(), gdalconst.GA_ReadOnly));
            }

            for(Dataset raster : rastersSpectral){

                double[] rasterExtent = new double[6];
                raster.GetGeoTransform(rasterExtent);

                rasterExtents_spectral.add(rasterExtent);

                double[] rasterWidthHeight_ = new double[2];
                rasterWidthHeight_[0] = raster.GetRasterXSize();
                rasterWidthHeight_[1] = raster.GetRasterYSize();

                rasterWidthHeight_spectral.add(rasterWidthHeight_);

                rasterBoundingBoxes_spectral.add(new double[]{rasterExtent[0], rasterExtent[3] + rasterExtent[5] * rasterWidthHeight_[1],rasterExtent[0] + rasterExtent[1] * rasterWidthHeight_[0],  rasterExtent[3]});

                nBands = raster.GetRasterCount();

                ArrayList<Band> bands = new ArrayList<>();

                for(int i = 0; i < nBands; i++){

                    bands.add(raster.GetRasterBand(i + 1));

                }

                rasterBands_spectral.add(bands);

            }


        }

        pointCloudMetrics pCM = new pointCloudMetrics(aR);



        for(int i = 0; i < polygons.size(); i++){

            float[] readValue = new float[1];

            ArrayList<Double> gridPoints_z_a = new ArrayList<>();
            ArrayList<int[]> gridPoints_RGB_f = new ArrayList<>();
            double sum_z_a = 0.0;
            ArrayList<String> colnames_a = new ArrayList<>();
            double[] polygonExtent = getPolygonExtent(polygons.get(i));

            /*
            extent[0] = Double.MAX_VALUE;
        extent[1] = Double.MAX_VALUE;
        extent[2] = Double.MIN_VALUE;
        extent[3] = Double.MIN_VALUE;

             */

            /*
            double minx, double maxx, double miny, double maxy
             */
            ArrayList<Integer> selection = rasterBank.findOverlappingRastersThreadSafe(polygonExtent[0], polygonExtent[2], polygonExtent[1], polygonExtent[3]);

            System.out.println(selection.size());

            if(true)
                continue;


            for(int j = 0; j < rasterExtents.size(); j++) {

                HashSet<Integer> usedCells = new HashSet<Integer>();

                //System.out.println(Arrays.toString(polygonExtent));
                //System.out.println(Arrays.toString(rasterExtents.get(j)));
                //System.out.println(Arrays.toString(rasterBoundingBoxes.get(j)));
                //Check if polygon overlaps with raster

                if (polygonExtent[0] > rasterBoundingBoxes.get(j)[2] || polygonExtent[2] < rasterBoundingBoxes.get(j)[0] || polygonExtent[1] > rasterBoundingBoxes.get(j)[3] || polygonExtent[3] < rasterBoundingBoxes.get(j)[1]) {
                    //System.out.println("Polygon does not overlap with raster");
                    continue;
                }

                Band band = rasterBands.get(j);

                double[][] polygon = polygons.get(i);

                ArrayList<double[][]> polygonHoles_ = polygonHoles.get(polyIds.get(i));

                int[] extentInPixelCoordinates = new int[4];

                extentInPixelCoordinates[0] = (int)Math.floor((polygonExtent[0] - rasterBoundingBoxes.get(j)[0]) / rasterExtents.get(j)[1]);
                extentInPixelCoordinates[3] = (int)Math.floor((rasterBoundingBoxes.get(j)[3] - polygonExtent[1] ) / rasterExtents.get(j)[1]);
                extentInPixelCoordinates[2] = (int)Math.floor((polygonExtent[2] - rasterBoundingBoxes.get(j)[0]) / rasterExtents.get(j)[1]);
                extentInPixelCoordinates[1] = (int)Math.floor((rasterBoundingBoxes.get(j)[3] - polygonExtent[3] ) / rasterExtents.get(j)[1]);

                //System.out.println("extentinpixelcoordinates: " + Arrays.toString(extentInPixelCoordinates));

                for(int x = extentInPixelCoordinates[0]; x <= extentInPixelCoordinates[2]; x++){
                    for(int y = extentInPixelCoordinates[1]; y <= extentInPixelCoordinates[3]; y++){

                        if(x < 0 || y < 0 || x >= rasterWidthHeight.get(j)[0] || y >= rasterWidthHeight.get(j)[1]){
                            continue;
                        }

                        double[] realCoordinates = new double[2];
                        realCoordinates[0] = rasterExtents.get(j)[0] + x * rasterExtents.get(j)[1] + rasterExtents.get(j)[1] / 2;
                        realCoordinates[1] = rasterExtents.get(j)[3] + y * rasterExtents.get(j)[5] + rasterExtents.get(j)[5] / 2;

                        boolean pointInPolygon = pointInPolygon(realCoordinates, polygon, polygonHoles, polyIds.get(i));

                        if(pointInPolygon) {
                            band.ReadRaster(x, y, 1, 1, readValue);


                            gridPoints_z_a.add((double)readValue[0]);
                            sum_z_a += readValue[0];
                        }
                    }

                }
            }

            if(nBands > 0){

                for(int j = 0; j < rasterExtents_spectral.size(); j++) {

                    HashSet<Integer> usedCells = new HashSet<Integer>();

                    //System.out.println(Arrays.toString(polygonExtent));
                    //System.out.println(Arrays.toString(rasterExtents.get(j)));
                    //System.out.println(Arrays.toString(rasterBoundingBoxes.get(j)));
                    //Check if polygon overlaps with raster

                    if (polygonExtent[0] > rasterBoundingBoxes_spectral.get(j)[2] || polygonExtent[2] < rasterBoundingBoxes_spectral.get(j)[0] || polygonExtent[1] > rasterBoundingBoxes_spectral.get(j)[3] || polygonExtent[3] < rasterBoundingBoxes_spectral.get(j)[1]) {
                        //System.out.println("Polygon does not overlap with raster");
                        continue;
                    }

                    ArrayList<Band> band = rasterBands_spectral.get(j);

                    double[][] polygon = polygons.get(i);

                    ArrayList<double[][]> polygonHoles_ = polygonHoles.get(polyIds.get(i));

                    int[] extentInPixelCoordinates = new int[4];

                    extentInPixelCoordinates[0] = (int)Math.floor((polygonExtent[0] - rasterBoundingBoxes_spectral.get(j)[0]) / rasterExtents_spectral.get(j)[1]);
                    extentInPixelCoordinates[3] = (int)Math.floor((rasterBoundingBoxes_spectral.get(j)[3] - polygonExtent[1] ) / rasterExtents_spectral.get(j)[1]);
                    extentInPixelCoordinates[2] = (int)Math.floor((polygonExtent[2] - rasterBoundingBoxes_spectral.get(j)[0]) / rasterExtents_spectral.get(j)[1]);
                    extentInPixelCoordinates[1] = (int)Math.floor((rasterBoundingBoxes_spectral.get(j)[3] - polygonExtent[3] ) / rasterExtents_spectral.get(j)[1]);

                    //System.out.println("extentinpixelcoordinates: " + Arrays.toString(extentInPixelCoordinates));

                    for(int x = extentInPixelCoordinates[0]; x <= extentInPixelCoordinates[2]; x++){
                        for(int y = extentInPixelCoordinates[1]; y <= extentInPixelCoordinates[3]; y++){

                            if(x < 0 || y < 0 || x >= rasterWidthHeight_spectral.get(j)[0] || y >= rasterWidthHeight_spectral.get(j)[1]){
                                continue;
                            }

                            double[] realCoordinates = new double[2];
                            realCoordinates[0] = rasterExtents_spectral.get(j)[0] + x * rasterExtents_spectral.get(j)[1] + rasterExtents_spectral.get(j)[1] / 2;
                            realCoordinates[1] = rasterExtents_spectral.get(j)[3] + y * rasterExtents_spectral.get(j)[5] + rasterExtents_spectral.get(j)[5] / 2;

                            boolean pointInPolygon = pointInPolygon(realCoordinates, polygon, polygonHoles, polyIds.get(i));

                            if(pointInPolygon) {

                                int[] value = new int[nBands];

                                for(int i_ = 0; i_ < nBands; i_++) {
                                    rasterBands_spectral.get(j).get(i_).ReadRaster(x, y, 1, 1, readValue);
                                    value[i_] = (int)readValue[0];
                                }

                                gridPoints_RGB_f.add(value);

                            }
                        }

                    }
                }

            }


            ArrayList<Double> metrics_a = new ArrayList<>();

            if(nBands == 0)
                metrics_a = pCM.calcZonal(gridPoints_z_a, sum_z_a, "_a", colnames_a);
            else{
                metrics_a = pCM.calc_with_RGB_zonal(gridPoints_z_a, sum_z_a, "_a", colnames_a, gridPoints_RGB_f);
            }


            //System.out.println(Arrays.toString(gridPoints_z_a.toArray()));
            //System.out.println("HERE");
            aR.lCMO.writeLineZonal(metrics_a, colnames_a, polyIds.get(i));

            // print progress
            if(i % 1000 == 0){
                System.out.println("Progress: " + i + " / " + polygons.size());
            }

        }

        aR.lCMO.closeFilesZonal();
        new File(polyWKT).delete();
    }

    public void zonalStatistics2(argumentReader aR, lasClipMetricOfile lCMO ) throws Exception{

        KarttaLehtiJako klj = new KarttaLehtiJako();
        klj.readFromFile(new File(""));

        rasterCollection rasterBank = new rasterCollection(aR.cores);


        this.aR = aR;
        String polyWKT = readShapefile(aR.poly);
        ArrayList<String> polyIds = new ArrayList<>();

        int nBands = 0;

        ArrayList<double[][]> polygons = readPolygonsFromWKT_string(polyWKT, polyIds);

        HashMap<String, ArrayList<double[][]>> polygonHoles = readPolygonHolesFromWKT_string(polyWKT, polyIds);


        ArrayList<Dataset> rasters = new ArrayList<Dataset>();
        ArrayList<Dataset> rastersSpectral = new ArrayList<Dataset>();

        //for(File raster : aR.inputFiles){
        //    rasters.add(gdal.Open(raster.getAbsolutePath(), gdalconst.GA_ReadOnly));
       // }

        /*
        ArrayList<double[]> rasterExtents = new ArrayList<double[]>();
        ArrayList<double[]> rasterWidthHeight = new ArrayList<double[]>();
        ArrayList<double[]> rasterBoundingBoxes = new ArrayList<double[]>();
        ArrayList<Band> rasterBands = new ArrayList<Band>();

        ArrayList<double[]> rasterExtents_spectral = new ArrayList<double[]>();
        ArrayList<double[]> rasterWidthHeight_spectral = new ArrayList<double[]>();
        ArrayList<double[]> rasterBoundingBoxes_spectral = new ArrayList<double[]>();
        ArrayList<ArrayList<Band>> rasterBands_spectral = new ArrayList<>();
*/
        aR.lCMO.prepZonal(aR.inputFiles.get(0));

        int n_metadataItems = 0;
        int counter = 0;
        int counter_ = 0;

        // current time
        long start = System.currentTimeMillis();

        //for(Dataset raster : rasters){
        for(File file : aR.inputFiles){

            Dataset raster = gdal.Open(file.getAbsolutePath(), gdalconst.GA_ReadOnly);

            double[] rasterExtent = new double[6];
            raster.GetGeoTransform(rasterExtent);

            //System.out.println(Arrays.toString(rasterExtent));
            //rasterExtents.add(rasterExtent);

            double[] rasterWidthHeight_ = new double[2];
            rasterWidthHeight_[0] = raster.GetRasterXSize();
            rasterWidthHeight_[1] = raster.GetRasterYSize();

            //rasterWidthHeight.add(rasterWidthHeight_);

            //rasterBoundingBoxes.add(new double[]{rasterExtent[0], rasterExtent[3] + rasterExtent[5] * rasterWidthHeight_[1],rasterExtent[0] + rasterExtent[1] * rasterWidthHeight_[0],  rasterExtent[3]});

            if(aR.metadataitems.size() == 0)
                rasterBank.addRaster(new gdalRaster(raster.GetDescription(), counter++));
            else{
                rasterBank.addRaster(new gdalRaster(raster.GetDescription(), counter++), aR);
                n_metadataItems = aR.metadataitems.size();

            }
            raster.delete();
            //System.out.println(counter_++);
            //rasterBands.add(raster.GetRasterBand(1));

        }

        // end time
        long end = System.currentTimeMillis();

        // elapsed time in minutes and seconds
        long elapsedTime = end - start;
        double elapsedTimeMin = elapsedTime / (60.0 * 1000.0);
        double elapsedTimeSec = (elapsedTimeMin - Math.floor(elapsedTimeMin)) * 60.0;

        // print elapsed time
        System.out.println("Elapsed time for opening and closing rasters: " + (int) elapsedTimeMin + " min " + (int) elapsedTimeSec + " sec");
/*
        if(aR.inputFilesSpectral.size() > 0){

            for(File raster : aR.inputFilesSpectral){
                rastersSpectral.add(gdal.Open(raster.getAbsolutePath(), gdalconst.GA_ReadOnly));
            }

            for(Dataset raster : rastersSpectral){

                double[] rasterExtent = new double[6];
                raster.GetGeoTransform(rasterExtent);

                rasterExtents_spectral.add(rasterExtent);

                double[] rasterWidthHeight_ = new double[2];
                rasterWidthHeight_[0] = raster.GetRasterXSize();
                rasterWidthHeight_[1] = raster.GetRasterYSize();

                rasterWidthHeight_spectral.add(rasterWidthHeight_);

                rasterBoundingBoxes_spectral.add(new double[]{rasterExtent[0], rasterExtent[3] + rasterExtent[5] * rasterWidthHeight_[1],rasterExtent[0] + rasterExtent[1] * rasterWidthHeight_[0],  rasterExtent[3]});

                nBands = raster.GetRasterCount();

                ArrayList<Band> bands = new ArrayList<>();

                for(int i = 0; i < nBands; i++){

                    bands.add(raster.GetRasterBand(i + 1));

                }

                rasterBands_spectral.add(bands);

            }


        }


 */
        pointCloudMetrics pCM = new pointCloudMetrics(aR);

        lCMO.prepZonal(aR.inputFiles.get(0));

        for(int i = 0; i < polygons.size(); i++){

            int nNoData = 0;
            int nValid = 0;

            ArrayList<String[]> metadataItems = new ArrayList<>();


            float[] readValue = new float[1];

            ArrayList<double[]> gridPoints_xyz_a = new ArrayList<>();
            ArrayList<Double> gridPoints_z_a = new ArrayList<>();
            ArrayList<int[]> gridPoints_RGB_f = new ArrayList<>();
            double sum_z_a = 0.0;
            ArrayList<String> colnames_a = new ArrayList<>();
            double[] polygonExtent = getPolygonExtent(polygons.get(i));

            /*
            extent[0] = Double.MAX_VALUE;
        extent[1] = Double.MAX_VALUE;
        extent[2] = Double.MIN_VALUE;
        extent[3] = Double.MIN_VALUE;

             */

            /*
            double minx, double maxx, double miny, double maxy
             */

            //System.out.println(Arrays.toString(polygonExtent));
            String mapSheetName = klj.getMapSheetNameByCoordinates((polygonExtent[2] + polygonExtent[0]) / 2.0, (polygonExtent[3] + polygonExtent[1]) / 2.0);

            ArrayList<Integer> selection = rasterBank.findOverlappingRastersThreadSafe(polygonExtent[0], polygonExtent[2], polygonExtent[1], polygonExtent[3]);
            int[] numberOfPixelsPerSelection = new int[selection.size()];

            if(selection.size() == 0){

                //System.out.println("No raster found for polygon " + polyIds.get(i) + " with extent " + Arrays.toString(polygonExtent));
                //continue;

            }

            int[] nPixelsPerSelection = new int[selection.size()];

            //for(int j = 0; j < rasterExtents.size(); j++) {
            if(selection.size() != 0)
            for(int j = 0; j < selection.size(); j++) {

                HashSet<Integer> usedCells = new HashSet<Integer>();
                gdalRaster ras = rasterBank.getRaster(selection.get(j));


                ras.setProcessingInProgress(true);
                int[] extentInPixelCoordinates = new int[4];
                double[] bbox = ras.rasterExtent;
                double resolution = ras.getResolution();
                double[] geotransform = ras.geoTransform;

                if (polygonExtent[0] > bbox[2] || polygonExtent[2] < bbox[0] || polygonExtent[1] > bbox[3] || polygonExtent[3] < bbox[1]) {
                    //System.out.println("Polygon does not overlap with raster");
                    ras.setProcessingInProgress(false);
                    continue;
                }

                if (aR.metadataitems.size() > 0) {
                    for (int i_ = 0; i_ < ras.metadatas.size(); i_++) {
                        metadataItems.add(new String[]{ras.metadatas.get(i_), ras.metadatas_values.get(i_)});
                    }
                }


                double[][] polygon = polygons.get(i);

                ArrayList<double[][]> polygonHoles_ = polygonHoles.get(polyIds.get(i));

                /*
                extentInPixelCoordinates[0] = (int) Math.floor((cellMinX - bbox[0]) / geotransform[1]);
                            extentInPixelCoordinates[3] = (int) Math.floor((bbox[3] - cellMinY) / geotransform[1]);
                            extentInPixelCoordinates[2] = (int) Math.floor((cellMaxX - bbox[0]) / geotransform[1]);
                            extentInPixelCoordinates[1] = (int) Math.floor((bbox[3] - cellMaxY) / geotransform[1]);
                 */
                extentInPixelCoordinates[0] = (int) Math.floor((polygonExtent[0] - bbox[0]) / geotransform[1]);
                extentInPixelCoordinates[3] = (int) Math.floor((bbox[3] - polygonExtent[1]) / geotransform[1]);
                extentInPixelCoordinates[2] = (int) Math.floor((polygonExtent[2] - bbox[0]) / geotransform[1]);
                extentInPixelCoordinates[1] = (int) Math.floor((bbox[3] - polygonExtent[3]) / geotransform[1]);

                for(int x = extentInPixelCoordinates[0]; x <= extentInPixelCoordinates[2]; x++){
                    for(int y = extentInPixelCoordinates[1]; y <= extentInPixelCoordinates[3]; y++){

                        if (x < 0 || y < 0 || x >= ras.number_of_pix_x || y >= ras.number_of_pix_y) {
                            continue;
                        }

                        numberOfPixelsPerSelection[j]++;

                        double[] realCoordinates = new double[2];
                        realCoordinates[0] = geotransform[0] + x * geotransform[1] + geotransform[1] / 2;
                        realCoordinates[1] = geotransform[3] + y * geotransform[5] + geotransform[5] / 2;

                        boolean pointInPolygon = pointInPolygon_(realCoordinates, polygon, polygonHoles, polyIds.get(i));

                        if(pointInPolygon) {


                            float value = ras.readValue(x, y);


                            if (value == Float.NaN) {
                                nNoData++;
                                continue;
                            }

                            // This is a no data value
                            if (value < -5000f) {
                                nNoData++;
                                continue;
                            }

                            nValid++;

                            gridPoints_z_a.add((double) value);
                            gridPoints_xyz_a.add(new double[]{realCoordinates[0], realCoordinates[1], value});

                            sum_z_a += value;
                            nPixelsPerSelection[j]++;
                        }
                    }

                }

                ras.setProcessingInProgress(false);
            }else{
                nNoData = 1;
                nValid = 1;
            }
/*
            if(nBands > 0){

                for(int j = 0; j < rasterExtents_spectral.size(); j++) {

                    HashSet<Integer> usedCells = new HashSet<Integer>();

                    //System.out.println(Arrays.toString(polygonExtent));
                    //System.out.println(Arrays.toString(rasterExtents.get(j)));
                    //System.out.println(Arrays.toString(rasterBoundingBoxes.get(j)));
                    //Check if polygon overlaps with raster

                    if (polygonExtent[0] > rasterBoundingBoxes_spectral.get(j)[2] || polygonExtent[2] < rasterBoundingBoxes_spectral.get(j)[0] || polygonExtent[1] > rasterBoundingBoxes_spectral.get(j)[3] || polygonExtent[3] < rasterBoundingBoxes_spectral.get(j)[1]) {
                        //System.out.println("Polygon does not overlap with raster");
                        continue;
                    }

                    ArrayList<Band> band = rasterBands_spectral.get(j);

                    double[][] polygon = polygons.get(i);

                    ArrayList<double[][]> polygonHoles_ = polygonHoles.get(polyIds.get(i));

                    int[] extentInPixelCoordinates = new int[4];

                    extentInPixelCoordinates[0] = (int)Math.floor((polygonExtent[0] - rasterBoundingBoxes_spectral.get(j)[0]) / rasterExtents_spectral.get(j)[1]);
                    extentInPixelCoordinates[3] = (int)Math.floor((rasterBoundingBoxes_spectral.get(j)[3] - polygonExtent[1] ) / rasterExtents_spectral.get(j)[1]);
                    extentInPixelCoordinates[2] = (int)Math.floor((polygonExtent[2] - rasterBoundingBoxes_spectral.get(j)[0]) / rasterExtents_spectral.get(j)[1]);
                    extentInPixelCoordinates[1] = (int)Math.floor((rasterBoundingBoxes_spectral.get(j)[3] - polygonExtent[3] ) / rasterExtents_spectral.get(j)[1]);

                    //System.out.println("extentinpixelcoordinates: " + Arrays.toString(extentInPixelCoordinates));

                    for(int x = extentInPixelCoordinates[0]; x <= extentInPixelCoordinates[2]; x++){
                        for(int y = extentInPixelCoordinates[1]; y <= extentInPixelCoordinates[3]; y++){

                            if(x < 0 || y < 0 || x >= rasterWidthHeight_spectral.get(j)[0] || y >= rasterWidthHeight_spectral.get(j)[1]){
                                continue;
                            }

                            double[] realCoordinates = new double[2];
                            realCoordinates[0] = rasterExtents_spectral.get(j)[0] + x * rasterExtents_spectral.get(j)[1] + rasterExtents_spectral.get(j)[1] / 2;
                            realCoordinates[1] = rasterExtents_spectral.get(j)[3] + y * rasterExtents_spectral.get(j)[5] + rasterExtents_spectral.get(j)[5] / 2;

                            boolean pointInPolygon = pointInPolygon(realCoordinates, polygon, polygonHoles, polyIds.get(i));

                            if(pointInPolygon) {

                                int[] value = new int[nBands];

                                for(int i_ = 0; i_ < nBands; i_++) {
                                    rasterBands_spectral.get(j).get(i_).ReadRaster(x, y, 1, 1, readValue);
                                    value[i_] = (int)readValue[0];
                                }

                                gridPoints_RGB_f.add(value);

                            }
                        }

                    }
                }

            }
*/
            ArrayList<Double> metrics_a = new ArrayList<>();
            ArrayList<ArrayList<Double>> metrics_convo = new ArrayList<>();

            ArrayList<String> colnames_convo = new ArrayList<>();



            if(nBands == 0) {
                metrics_a = pCM.calcZonal(gridPoints_z_a, sum_z_a, "_a", colnames_a);
                /*
                double top_left_x, double top_left_y,
                                                                     double bottom_right_x, double bottom_right_y
                 */
                if(aR.convo)
                    metrics_convo = pCM.calc_nn_input_train_raster(gridPoints_xyz_a, "_a", colnames_convo, polygonExtent[0], polygonExtent[3],
                        polygonExtent[2], polygonExtent[1]);
            }
            else{
                metrics_a = pCM.calc_with_RGB_zonal(gridPoints_z_a, sum_z_a, "_a", colnames_a, gridPoints_RGB_f);
            }


            double proportionNoData = (double) nNoData / (double) (nNoData + nValid);

            metrics_a.add(0, proportionNoData);
            colnames_a.add(0,"proportionNoData");


            int mostPixels = 0;

            if(selection.size() != 0)
                mostPixels = indexOfHighestValue(nPixelsPerSelection);
            else
                mostPixels = 0;

            if(aR.metadataitems.size() == 0) {
                lCMO.writeLineZonal(metrics_a, colnames_a, polyIds.get(i));
            }
            else if(aR.convo) {
                lCMO.writeLine_convo_raster(metrics_convo, colnames_convo, polyIds.get(i));
            }else {
                lCMO.writeLineZonal(metrics_a, colnames_a, polyIds.get(i), metadataItems, aR.metadataitems.size(), mostPixels, mapSheetName);

                if(aR.convo)
                    lCMO.writeLine_convo_raster(metrics_convo, colnames_convo, polyIds.get(i));

            }

            // print progress
            if(i % 1000 == 0){
                System.out.println("Progress: " + i + " / " + polygons.size());
            }

        }

        HashSet<String> ignoreTheseColumnNames = new HashSet<>();
        ignoreTheseColumnNames.add("p_0.05_z_a");
        ignoreTheseColumnNames.add("p_0.15_z_a");
        ignoreTheseColumnNames.add("p_0.2_z_a");
        ignoreTheseColumnNames.add("p_0.25_z_a");
        ignoreTheseColumnNames.add("p_0.35_z_a");
        ignoreTheseColumnNames.add("p_0.4_z_a");
        ignoreTheseColumnNames.add("p_0.45_z_a");
        ignoreTheseColumnNames.add("p_0.55_z_a");
        ignoreTheseColumnNames.add("p_0.65_z_a");

        ignoreTheseColumnNames.add("d_2.5_z_a");
        ignoreTheseColumnNames.add("d_7.5_z_a");

        lCMO.closeFilesZonal2();

        if(aR.subsetColumnNamesVMI)
            lCMO.deleteColumnsFromFile(lCMO.echo_class_files.get(0), ignoreTheseColumnNames);

        lCMO.closeFilesZonal();
        new File(polyWKT).delete();
    }

    public void zonalStatistics_exportSurface(argumentReader aR, lasClipMetricOfile lCMO ) throws Exception{

        KarttaLehtiJako klj = new KarttaLehtiJako();
        klj.readFromFile(new File(""));

        rasterCollection rasterBank = new rasterCollection(aR.cores);

        this.aR = aR;
        System.out.println(aR.poly);
        String polyWKT = readShapefile(aR.poly);
        ArrayList<String> polyIds = new ArrayList<>();

        int nBands = 0;

        ArrayList<double[][]> polygons = readPolygonsFromWKT_string(polyWKT, polyIds);

        HashMap<String, ArrayList<double[][]>> polygonHoles = readPolygonHolesFromWKT_string(polyWKT, polyIds);


        ArrayList<Dataset> rasters = new ArrayList<Dataset>();
        ArrayList<Dataset> rastersSpectral = new ArrayList<Dataset>();

        //for(File raster : aR.inputFiles){
        //    rasters.add(gdal.Open(raster.getAbsolutePath(), gdalconst.GA_ReadOnly));
        // }

        /*
        ArrayList<double[]> rasterExtents = new ArrayList<double[]>();
        ArrayList<double[]> rasterWidthHeight = new ArrayList<double[]>();
        ArrayList<double[]> rasterBoundingBoxes = new ArrayList<double[]>();
        ArrayList<Band> rasterBands = new ArrayList<Band>();

        ArrayList<double[]> rasterExtents_spectral = new ArrayList<double[]>();
        ArrayList<double[]> rasterWidthHeight_spectral = new ArrayList<double[]>();
        ArrayList<double[]> rasterBoundingBoxes_spectral = new ArrayList<double[]>();
        ArrayList<ArrayList<Band>> rasterBands_spectral = new ArrayList<>();
*/
        aR.lCMO.prepZonal(aR.inputFiles.get(0));

        int n_metadataItems = 0;
        int counter = 0;
        int counter_ = 0;

        // current time
        long start = System.currentTimeMillis();

        //for(Dataset raster : rasters){
        for(File file : aR.inputFiles){

            Dataset raster = gdal.Open(file.getAbsolutePath(), gdalconst.GA_ReadOnly);

            double[] rasterExtent = new double[6];
            raster.GetGeoTransform(rasterExtent);

            //System.out.println(Arrays.toString(rasterExtent));
            //rasterExtents.add(rasterExtent);

            double[] rasterWidthHeight_ = new double[2];
            rasterWidthHeight_[0] = raster.GetRasterXSize();
            rasterWidthHeight_[1] = raster.GetRasterYSize();

            //rasterWidthHeight.add(rasterWidthHeight_);

            //rasterBoundingBoxes.add(new double[]{rasterExtent[0], rasterExtent[3] + rasterExtent[5] * rasterWidthHeight_[1],rasterExtent[0] + rasterExtent[1] * rasterWidthHeight_[0],  rasterExtent[3]});

            if(aR.metadataitems.size() == 0)
                rasterBank.addRaster(new gdalRaster(raster.GetDescription(), counter++));
            else{
                rasterBank.addRaster(new gdalRaster(raster.GetDescription(), counter++), aR);
                n_metadataItems = aR.metadataitems.size();

            }
            raster.delete();
            //System.out.println(counter_++);
            //rasterBands.add(raster.GetRasterBand(1));

        }

        // end time
        long end = System.currentTimeMillis();

        // elapsed time in minutes and seconds
        long elapsedTime = end - start;
        double elapsedTimeMin = elapsedTime / (60.0 * 1000.0);
        double elapsedTimeSec = (elapsedTimeMin - Math.floor(elapsedTimeMin)) * 60.0;

        // print elapsed time
        System.out.println("Elapsed time for opening and closing rasters: " + (int) elapsedTimeMin + " min " + (int) elapsedTimeSec + " sec");
/*
        if(aR.inputFilesSpectral.size() > 0){

            for(File raster : aR.inputFilesSpectral){
                rastersSpectral.add(gdal.Open(raster.getAbsolutePath(), gdalconst.GA_ReadOnly));
            }

            for(Dataset raster : rastersSpectral){

                double[] rasterExtent = new double[6];
                raster.GetGeoTransform(rasterExtent);

                rasterExtents_spectral.add(rasterExtent);

                double[] rasterWidthHeight_ = new double[2];
                rasterWidthHeight_[0] = raster.GetRasterXSize();
                rasterWidthHeight_[1] = raster.GetRasterYSize();

                rasterWidthHeight_spectral.add(rasterWidthHeight_);

                rasterBoundingBoxes_spectral.add(new double[]{rasterExtent[0], rasterExtent[3] + rasterExtent[5] * rasterWidthHeight_[1],rasterExtent[0] + rasterExtent[1] * rasterWidthHeight_[0],  rasterExtent[3]});

                nBands = raster.GetRasterCount();

                ArrayList<Band> bands = new ArrayList<>();

                for(int i = 0; i < nBands; i++){

                    bands.add(raster.GetRasterBand(i + 1));

                }

                rasterBands_spectral.add(bands);

            }


        }


 */
        pointCloudMetrics pCM = new pointCloudMetrics(aR);

        lCMO.prepZonal(aR.inputFiles.get(0));

        double res_ = aR.res;

        for(int i = 0; i < polygons.size(); i++){

            String dir = "";

            if(!aR.odir.equals("asd")){
                dir = aR.odir;
            }

            File outDirectory = new File(dir + aR.pathSep + polyIds.get(i));

            if(!outDirectory.exists())
                outDirectory.mkdirs();

            System.out.println(outDirectory.getAbsolutePath());
            System.out.println(dir);
            System.out.println(File.pathSeparator);

            if(aR.userString1 != null){
                outDirectory = new File(outDirectory.getAbsolutePath() + aR.pathSep + aR.userString1);
                if(!outDirectory.exists())
                    outDirectory.mkdirs();
            }
            //System.exit(1);

            int nNoData = 0;
            int nValid = 0;

            ArrayList<String[]> metadataItems = new ArrayList<>();


            float[] readValue = new float[1];

            ArrayList<double[]> gridPoints_xyz_a = new ArrayList<>();
            ArrayList<Double> gridPoints_z_a = new ArrayList<>();
            ArrayList<int[]> gridPoints_RGB_f = new ArrayList<>();
            double sum_z_a = 0.0;
            ArrayList<String> colnames_a = new ArrayList<>();
            double[] polygonExtent = getPolygonExtent(polygons.get(i));

            System.out.println(Arrays.toString(polygonExtent));

            double min_x = polygonExtent[0];
            double max_y = polygonExtent[3];

            double xLength = polygonExtent[2] - polygonExtent[0];
            double yLength = polygonExtent[3] - polygonExtent[1];

            int numPixelsX = (int)Math.ceil(xLength / res_);
            int numPixelsY = (int)Math.ceil(xLength / res_);

            double[][] surface = new double[numPixelsX][numPixelsY];
           // double[][] output = new double[xLength][yLength];


            /*
            extent[0] = Double.MAX_VALUE;
        extent[1] = Double.MAX_VALUE;
        extent[2] = Double.MIN_VALUE;
        extent[3] = Double.MIN_VALUE;

             */

            /*
            double minx, double maxx, double miny, double maxy
             */

            //System.out.println(Arrays.toString(polygonExtent));
            String mapSheetName = klj.getMapSheetNameByCoordinates((polygonExtent[2] + polygonExtent[0]) / 2.0, (polygonExtent[3] + polygonExtent[1]) / 2.0);

            ArrayList<Integer> selection = rasterBank.findOverlappingRastersThreadSafe(polygonExtent[0], polygonExtent[2], polygonExtent[1], polygonExtent[3]);
            int[] numberOfPixelsPerSelection = new int[selection.size()];

            if(selection.size() == 0){

                //System.out.println("No raster found for polygon " + polyIds.get(i) + " with extent " + Arrays.toString(polygonExtent));
                //continue;

            }

            int[] nPixelsPerSelection = new int[selection.size()];

            //for(int j = 0; j < rasterExtents.size(); j++) {
            if(selection.size() != 0)
                for(int j = 0; j < selection.size(); j++) {

                    HashSet<Integer> usedCells = new HashSet<Integer>();
                    gdalRaster ras = rasterBank.getRaster(selection.get(j));


                    ras.setProcessingInProgress(true);
                    int[] extentInPixelCoordinates = new int[4];
                    double[] bbox = ras.rasterExtent;
                    double resolution = ras.getResolution();
                    double[] geotransform = ras.geoTransform;

                    if (polygonExtent[0] > bbox[2] || polygonExtent[2] < bbox[0] || polygonExtent[1] > bbox[3] || polygonExtent[3] < bbox[1]) {
                        //System.out.println("Polygon does not overlap with raster");
                        ras.setProcessingInProgress(false);
                        continue;
                    }

                    if (aR.metadataitems.size() > 0) {
                        for (int i_ = 0; i_ < ras.metadatas.size(); i_++) {
                            metadataItems.add(new String[]{ras.metadatas.get(i_), ras.metadatas_values.get(i_)});
                        }
                    }


                    double[][] polygon = polygons.get(i);

                    ArrayList<double[][]> polygonHoles_ = polygonHoles.get(polyIds.get(i));

                /*
                extentInPixelCoordinates[0] = (int) Math.floor((cellMinX - bbox[0]) / geotransform[1]);
                            extentInPixelCoordinates[3] = (int) Math.floor((bbox[3] - cellMinY) / geotransform[1]);
                            extentInPixelCoordinates[2] = (int) Math.floor((cellMaxX - bbox[0]) / geotransform[1]);
                            extentInPixelCoordinates[1] = (int) Math.floor((bbox[3] - cellMaxY) / geotransform[1]);
                 */
                    extentInPixelCoordinates[0] = (int) Math.floor((polygonExtent[0] - bbox[0]) / geotransform[1]);
                    extentInPixelCoordinates[3] = (int) Math.floor((bbox[3] - polygonExtent[1]) / geotransform[1]);
                    extentInPixelCoordinates[2] = (int) Math.floor((polygonExtent[2] - bbox[0]) / geotransform[1]);
                    extentInPixelCoordinates[1] = (int) Math.floor((bbox[3] - polygonExtent[3]) / geotransform[1]);

                    for(int x = extentInPixelCoordinates[0]; x <= extentInPixelCoordinates[2]; x++){
                        for(int y = extentInPixelCoordinates[1]; y <= extentInPixelCoordinates[3]; y++){

                            if (x < 0 || y < 0 || x >= ras.number_of_pix_x || y >= ras.number_of_pix_y) {
                                continue;
                            }

                            numberOfPixelsPerSelection[j]++;

                            double[] realCoordinates = new double[2];
                            realCoordinates[0] = geotransform[0] + x * geotransform[1] + geotransform[1] / 2;
                            realCoordinates[1] = geotransform[3] + y * geotransform[5] + geotransform[5] / 2;

                            int surfaceCoordinateX = (int)((realCoordinates[0] - polygonExtent[0]) / res_);
                            int surfaceCoordinateY = (int)((polygonExtent[3] - realCoordinates[1]) / res_);

                            if(surfaceCoordinateX >= 0 && surfaceCoordinateX < numPixelsX && surfaceCoordinateY >= 0 && surfaceCoordinateY < numPixelsY){

                                surface[surfaceCoordinateX][surfaceCoordinateY] = ras.readValue(x, y);
                                System.out.println("HERE!!");

                            }

                            boolean pointInPolygon = pointInPolygon_(realCoordinates, polygon, polygonHoles, polyIds.get(i));

                            if(false)
                            if(pointInPolygon) {


                                float value = ras.readValue(x, y);


                                if (value == Float.NaN) {
                                    nNoData++;
                                    continue;
                                }

                                // This is a no data value
                                if (value < -5000f) {
                                    nNoData++;
                                    continue;
                                }

                                nValid++;

                                gridPoints_z_a.add((double) value);
                                gridPoints_xyz_a.add(new double[]{realCoordinates[0], realCoordinates[1], value});

                                sum_z_a += value;
                                nPixelsPerSelection[j]++;
                            }
                        }

                    }

                    ras.setProcessingInProgress(false);
                }else{
                nNoData = 1;
                nValid = 1;
            }
/*
            if(nBands > 0){

                for(int j = 0; j < rasterExtents_spectral.size(); j++) {

                    HashSet<Integer> usedCells = new HashSet<Integer>();

                    //System.out.println(Arrays.toString(polygonExtent));
                    //System.out.println(Arrays.toString(rasterExtents.get(j)));
                    //System.out.println(Arrays.toString(rasterBoundingBoxes.get(j)));
                    //Check if polygon overlaps with raster

                    if (polygonExtent[0] > rasterBoundingBoxes_spectral.get(j)[2] || polygonExtent[2] < rasterBoundingBoxes_spectral.get(j)[0] || polygonExtent[1] > rasterBoundingBoxes_spectral.get(j)[3] || polygonExtent[3] < rasterBoundingBoxes_spectral.get(j)[1]) {
                        //System.out.println("Polygon does not overlap with raster");
                        continue;
                    }

                    ArrayList<Band> band = rasterBands_spectral.get(j);

                    double[][] polygon = polygons.get(i);

                    ArrayList<double[][]> polygonHoles_ = polygonHoles.get(polyIds.get(i));

                    int[] extentInPixelCoordinates = new int[4];

                    extentInPixelCoordinates[0] = (int)Math.floor((polygonExtent[0] - rasterBoundingBoxes_spectral.get(j)[0]) / rasterExtents_spectral.get(j)[1]);
                    extentInPixelCoordinates[3] = (int)Math.floor((rasterBoundingBoxes_spectral.get(j)[3] - polygonExtent[1] ) / rasterExtents_spectral.get(j)[1]);
                    extentInPixelCoordinates[2] = (int)Math.floor((polygonExtent[2] - rasterBoundingBoxes_spectral.get(j)[0]) / rasterExtents_spectral.get(j)[1]);
                    extentInPixelCoordinates[1] = (int)Math.floor((rasterBoundingBoxes_spectral.get(j)[3] - polygonExtent[3] ) / rasterExtents_spectral.get(j)[1]);

                    //System.out.println("extentinpixelcoordinates: " + Arrays.toString(extentInPixelCoordinates));

                    for(int x = extentInPixelCoordinates[0]; x <= extentInPixelCoordinates[2]; x++){
                        for(int y = extentInPixelCoordinates[1]; y <= extentInPixelCoordinates[3]; y++){

                            if(x < 0 || y < 0 || x >= rasterWidthHeight_spectral.get(j)[0] || y >= rasterWidthHeight_spectral.get(j)[1]){
                                continue;
                            }

                            double[] realCoordinates = new double[2];
                            realCoordinates[0] = rasterExtents_spectral.get(j)[0] + x * rasterExtents_spectral.get(j)[1] + rasterExtents_spectral.get(j)[1] / 2;
                            realCoordinates[1] = rasterExtents_spectral.get(j)[3] + y * rasterExtents_spectral.get(j)[5] + rasterExtents_spectral.get(j)[5] / 2;

                            boolean pointInPolygon = pointInPolygon(realCoordinates, polygon, polygonHoles, polyIds.get(i));

                            if(pointInPolygon) {

                                int[] value = new int[nBands];

                                for(int i_ = 0; i_ < nBands; i_++) {
                                    rasterBands_spectral.get(j).get(i_).ReadRaster(x, y, 1, 1, readValue);
                                    value[i_] = (int)readValue[0];
                                }

                                gridPoints_RGB_f.add(value);

                            }
                        }

                    }
                }

            }
*/


            File outputFile = new File(outDirectory + aR.pathSep + "surface.txt");

            if(outputFile.exists()){
                outputFile.delete();
                outputFile.createNewFile();
            }else{
                outputFile.createNewFile();
            }

            FileWriter fw = null;
            BufferedWriter bw = null;//
            PrintWriter out = null;

            fw = new FileWriter(outputFile, false);
            bw = new BufferedWriter(fw);

            for(int y = 0; y < numPixelsY; y++){
                for(int x = 0; x < numPixelsX; x++){

                    double real_x = min_x + res_ * x + res_ / 2.0;
                    double real_y = max_y - res_ * y - res_ / 2.0;

                    bw.write(String.valueOf(real_x) + "\t" + String.valueOf(real_y) + "\t" + String.valueOf(surface[x][y]) + "\n");

                    //if(x != (numPixelsX-1))
                    //    bw.write("\t");

                }
                //if(y != (numPixelsY-1))
                //    bw.write("\n");
            }


            bw.close();
           if(false){

                ArrayList<Double> metrics_a = new ArrayList<>();
                ArrayList<ArrayList<Double>> metrics_convo = new ArrayList<>();

                ArrayList<String> colnames_convo = new ArrayList<>();


                if (nBands == 0) {
                    metrics_a = pCM.calcZonal(gridPoints_z_a, sum_z_a, "_a", colnames_a);
                /*
                double top_left_x, double top_left_y,
                                                                     double bottom_right_x, double bottom_right_y
                 */
                    if (aR.convo)
                        metrics_convo = pCM.calc_nn_input_train_raster(gridPoints_xyz_a, "_a", colnames_convo, polygonExtent[0], polygonExtent[3],
                                polygonExtent[2], polygonExtent[1]);
                } else {
                    metrics_a = pCM.calc_with_RGB_zonal(gridPoints_z_a, sum_z_a, "_a", colnames_a, gridPoints_RGB_f);
                }


                double proportionNoData = (double) nNoData / (double) (nNoData + nValid);

                metrics_a.add(0, proportionNoData);
                colnames_a.add(0, "proportionNoData");


                int mostPixels = 0;

                if (selection.size() != 0)
                    mostPixels = indexOfHighestValue(nPixelsPerSelection);
                else
                    mostPixels = 0;

                if (aR.metadataitems.size() == 0)
                    lCMO.writeLineZonal(metrics_a, colnames_a, polyIds.get(i));
                if (aR.convo)
                    lCMO.writeLine_convo_raster(metrics_convo, colnames_convo, polyIds.get(i));
                else {
                    lCMO.writeLineZonal(metrics_a, colnames_a, polyIds.get(i), metadataItems, aR.metadataitems.size(), mostPixels, mapSheetName);

                    if (aR.convo)
                        lCMO.writeLine_convo_raster(metrics_convo, colnames_convo, polyIds.get(i));

                }

            }

            // print progress
            if(i % 1000 == 0){
                System.out.println("Progress: " + i + " / " + polygons.size());
            }

        }

        HashSet<String> ignoreTheseColumnNames = new HashSet<>();
        ignoreTheseColumnNames.add("p_0.05_z_a");
        ignoreTheseColumnNames.add("p_0.15_z_a");
        ignoreTheseColumnNames.add("p_0.2_z_a");
        ignoreTheseColumnNames.add("p_0.25_z_a");
        ignoreTheseColumnNames.add("p_0.35_z_a");
        ignoreTheseColumnNames.add("p_0.4_z_a");
        ignoreTheseColumnNames.add("p_0.45_z_a");
        ignoreTheseColumnNames.add("p_0.55_z_a");
        ignoreTheseColumnNames.add("p_0.65_z_a");

        ignoreTheseColumnNames.add("d_2.5_z_a");
        ignoreTheseColumnNames.add("d_7.5_z_a");

        lCMO.closeFilesZonal2();

        if(aR.subsetColumnNamesVMI)
            lCMO.deleteColumnsFromFile(lCMO.echo_class_files.get(0), ignoreTheseColumnNames);

        lCMO.closeFilesZonal();
        new File(polyWKT).delete();
    }
    public static int highestValueInArray(int[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array must not be null or empty");
        }

        int highestValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > highestValue) {
                highestValue = array[i];
            }
        }
        return highestValue;
    }

    public static int indexOfHighestValue(int[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array must not be null or empty");
        }

        int highestValueIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[highestValueIndex]) {
                highestValueIndex = i;
            }
        }
        return highestValueIndex;
    }
    public static boolean pointInPolygon(double[] point, double[][] poly, HashMap<Integer, ArrayList<double[][]>> holes, int poly_id) {

        int numPolyCorners = poly.length;
        int j = numPolyCorners - 1;
        boolean isInside = false;

        for (int i = 0; i < numPolyCorners; i++) {
            if (poly[i][1] < point[1] && poly[j][1] >= point[1] || poly[j][1] < point[1] && poly[i][1] >= point[1]) {
                if (poly[i][0] + (point[1] - poly[i][1]) / (poly[j][1] - poly[i][1]) * (poly[j][0] - poly[i][0]) < point[0]) {
                    isInside = !isInside;
                }
            }
            j = i;
        }

        if(holes.containsKey(poly_id)){

            ArrayList<double[][]> holet = holes.get(poly_id);
            double[][] polyHole = null;

            for(int i_ = 0; i_ < holet.size(); i_++){

                numPolyCorners = holet.get(i_).length;

                //System.out.println(holet.get(i_)[0][0] + " == " + holet.get(i_)[holet.get(i_).length-1][0]);
                //System.out.println(holet.get(i_)[0][1] + " == " + holet.get(i_)[holet.get(i_).length-1][1]);
                j = numPolyCorners - 1;
                boolean is_inside_hole = false;
                polyHole = holet.get(i_);

                for (int i = 0; i < numPolyCorners; i++) {

                    //System.out.println(polyHole[i][0] + " " + polyHole[i][1]);

                    if (polyHole[i][1] < point[1] && polyHole[j][1] >= point[1] || polyHole[j][1] < point[1] && polyHole[i][1] >= point[1]) {
                        if (polyHole[i][0] + (point[1] - polyHole[i][1]) / (polyHole[j][1] - polyHole[i][1]) * (polyHole[j][0] - polyHole[i][0]) < point[0]) {
                            is_inside_hole = !is_inside_hole;
                        }
                    }
                    j = i;
                }

                //System.out.println("-------------------------");
                if(is_inside_hole) {

                    //System.out.println("YES YES YES!");
                    return false;
                }
            }
        }

        return isInside;
    }

    public static boolean pointInPolygon_(double[] point, double[][] poly, HashMap<String, ArrayList<double[][]>> holes, String poly_id) {

        int numPolyCorners = poly.length;
        int j = numPolyCorners - 1;
        boolean isInside = false;

        for (int i = 0; i < numPolyCorners; i++) {
            if (poly[i][1] < point[1] && poly[j][1] >= point[1] || poly[j][1] < point[1] && poly[i][1] >= point[1]) {
                if (poly[i][0] + (point[1] - poly[i][1]) / (poly[j][1] - poly[i][1]) * (poly[j][0] - poly[i][0]) < point[0]) {
                    isInside = !isInside;
                }
            }
            j = i;
        }

        if(holes.containsKey(poly_id)){

            ArrayList<double[][]> holet = holes.get(poly_id);
            double[][] polyHole = null;

            for(int i_ = 0; i_ < holet.size(); i_++){

                numPolyCorners = holet.get(i_).length;

                //System.out.println(holet.get(i_)[0][0] + " == " + holet.get(i_)[holet.get(i_).length-1][0]);
                //System.out.println(holet.get(i_)[0][1] + " == " + holet.get(i_)[holet.get(i_).length-1][1]);
                j = numPolyCorners - 1;
                boolean is_inside_hole = false;
                polyHole = holet.get(i_);

                for (int i = 0; i < numPolyCorners; i++) {

                    //System.out.println(polyHole[i][0] + " " + polyHole[i][1]);

                    if (polyHole[i][1] < point[1] && polyHole[j][1] >= point[1] || polyHole[j][1] < point[1] && polyHole[i][1] >= point[1]) {
                        if (polyHole[i][0] + (point[1] - polyHole[i][1]) / (polyHole[j][1] - polyHole[i][1]) * (polyHole[j][0] - polyHole[i][0]) < point[0]) {
                            is_inside_hole = !is_inside_hole;
                        }
                    }
                    j = i;
                }

                //System.out.println("-------------------------");
                if(is_inside_hole) {

                    //System.out.println("YES YES YES!");
                    return false;
                }
            }
        }

        return isInside;
    }
    public double[] getPolygonExtent(double[][] polygon){

        double[] extent = new double[4];

        extent[0] = Double.MAX_VALUE;
        extent[1] = Double.MAX_VALUE;
        extent[2] = Double.MIN_VALUE;
        extent[3] = Double.MIN_VALUE;

        for(int i = 0; i < polygon.length; i++){

            if(polygon[i][0] < extent[0])
                extent[0] = polygon[i][0];
            if(polygon[i][1] < extent[1])
                extent[1] = polygon[i][1];
            if(polygon[i][0] > extent[2])
                extent[2] = polygon[i][0];
            if(polygon[i][1] > extent[3])
                extent[3] = polygon[i][1];

        }

        return extent;

    }

    public static ArrayList<double[][]> readPolygonsFromWKT(String fileName, ArrayList<Integer> plotID1) throws Exception{

        ArrayList<double[][]> output = new ArrayList<>();
        String line1;
        int id_number = 1;

        try{
            BufferedReader sc = new BufferedReader( new FileReader(new File(fileName)));
            sc.readLine();
            while((line1 = sc.readLine())!= null){

                String[] tokens =  line1.split(",");
                String[] tokens2 =  line1.split("\\),\\(");
                //System.out.println(Arrays.toString(tokens));
                //if(tokens[tokens.length - 1] != -999){
                boolean holes = false;

                if(tokens2.length > 1){
                    holes = true;
                }

                if(holes) {
                    tokens = line1.split(",\\(");
                    tokens[0] += ")";
                    tokens = tokens[0].split(",");
                    //System.out.println(tokens[0]);
                    //System.exit(1);
                }

                String[] iidee_ = line1.split("\",");
                String id = iidee_[iidee_.length-1];

                //System.out.println(Integer.parseInt(tokens[tokens.length - 1]) + " " + tokens2.length);

                try {
                    //plotID1.add(Integer.parseInt(tokens[tokens.length - 1]));
                    plotID1.add(Integer.parseInt(id));

                }catch (NumberFormatException e){

                    //Not directly comvertable to a number

                    String str = id.replaceAll("\\D+","");

                    try {
                        plotID1.add(Integer.parseInt(str));
                    }catch (Exception ee){

                        //Still no numbers, let's make our own then I guess

                        plotID1.add(id_number++);

                    }

                }

                double[][] tempPoly = new double[tokens.length - 1][2];
                int n = (tokens.length) - 2;
                int counteri = 0;

                tempPoly[counteri][0] = Double.parseDouble(tokens[0].split("\\(\\(")[1].split(" ")[0]);
                tempPoly[counteri][1] = Double.parseDouble(tokens[0].split("\\(\\(")[1].split(" ")[1]);

                counteri++;

                boolean breikki = false;

                for(int i = 1; i < n; i++){

                    if(tokens[i].split(" ")[0].contains(")") || tokens[i].split(" ")[1].contains(")")){
                        plotID1.remove(plotID1.size() - 1);
                        breikki = true;
                        break;
                    }

                    tempPoly[counteri][0] = Double.parseDouble(tokens[i].split(" ")[0]);
                    tempPoly[counteri][1] = Double.parseDouble(tokens[i].split(" ")[1]);
                    counteri++;
                }

                //System.out.println(Arrays.toString(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")));
                tempPoly[counteri][0] = Double.parseDouble(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")[0]);
                tempPoly[counteri][1] = Double.parseDouble(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")[1]);

                if(!breikki)
                    output.add(tempPoly);
                //}

            }


        }catch( IOException ioException ) {
            //ioException.printStackTrace();
        }

        return output;
    }

    public static ArrayList<double[][]> readPolygonsFromWKT_string(String fileName, ArrayList<String> plotID1) throws Exception{

        ArrayList<double[][]> output = new ArrayList<>();
        String line1;
        int id_number = 1;

        try{
            BufferedReader sc = new BufferedReader( new FileReader(new File(fileName)));
            sc.readLine();
            while((line1 = sc.readLine())!= null){

                String[] tokens =  line1.split(",");
                String[] tokens2 =  line1.split("\\),\\(");
                //System.out.println(Arrays.toString(tokens));
                //if(tokens[tokens.length - 1] != -999){
                boolean holes = false;

                if(tokens2.length > 1){
                    holes = true;
                }

                if(holes) {
                    tokens = line1.split(",\\(");
                    tokens[0] += ")";
                    tokens = tokens[0].split(",");
                    //System.out.println(tokens[0]);
                    //System.exit(1);
                }

                String[] iidee_ = line1.split("\",");
                String id = iidee_[iidee_.length-1];

                //System.out.println(Integer.parseInt(tokens[tokens.length - 1]) + " " + tokens2.length);

                try {
                    //plotID1.add(Integer.parseInt(tokens[tokens.length - 1]));
                    plotID1.add((id));

                }catch (NumberFormatException e){

                    //Not directly comvertable to a number

                    String str = id.replaceAll("\\D+","");

                    try {
                        plotID1.add((str));
                    }catch (Exception ee){

                        //Still no numbers, let's make our own then I guess

                        //plotID1.add(id_number++);

                    }

                }

                double[][] tempPoly = new double[tokens.length - 1][2];
                int n = (tokens.length) - 2;
                int counteri = 0;

                tempPoly[counteri][0] = Double.parseDouble(tokens[0].split("\\(\\(")[1].split(" ")[0]);
                tempPoly[counteri][1] = Double.parseDouble(tokens[0].split("\\(\\(")[1].split(" ")[1]);

                counteri++;

                boolean breikki = false;

                for(int i = 1; i < n; i++){

                    if(tokens[i].split(" ")[0].contains(")") || tokens[i].split(" ")[1].contains(")")){
                        plotID1.remove(plotID1.size() - 1);
                        breikki = true;
                        break;
                    }

                    tempPoly[counteri][0] = Double.parseDouble(tokens[i].split(" ")[0]);
                    tempPoly[counteri][1] = Double.parseDouble(tokens[i].split(" ")[1]);
                    counteri++;
                }

                //System.out.println(Arrays.toString(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")));
                tempPoly[counteri][0] = Double.parseDouble(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")[0]);
                tempPoly[counteri][1] = Double.parseDouble(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")[1]);

                if(!breikki)
                    output.add(tempPoly);
                //}

            }


        }catch( IOException ioException ) {
            //ioException.printStackTrace();
        }

        return output;
    }

    public static HashMap<Integer, ArrayList<double[][]>> readPolygonHolesFromWKT(String fileName, ArrayList<Integer> plotID1) throws Exception{

        HashMap<Integer, ArrayList<double[][]>> output = new HashMap<Integer, ArrayList<double[][]>>();

        String line1;
        int id_number = 1;

        try{
            BufferedReader sc = new BufferedReader( new FileReader(new File(fileName)));
            sc.readLine();
            while((line1 = sc.readLine())!= null){

                String[] tokens =  line1.split(",");
                String[] tokens2 =  line1.split("\\),\\(");

                if(tokens2.length <= 1){
                    continue;
                }

                int plot_id = -1;

                try {
                    plot_id = Integer.parseInt(tokens[tokens.length - 1]);
                }catch (NumberFormatException e){

                    //Not directly comvertable to a number

                    String str = tokens[tokens.length - 1].replaceAll("\\D+","");

                    try {
                        plot_id = Integer.parseInt(str);
                    }catch (Exception ee){

                        //Still no numbers, let's make our own then I guess
                        plot_id = id_number++;

                    }

                }

                //System.out.println(tokens2[0]);

                String string_here = null;

                output.put(plot_id, new ArrayList<>());


                for(int i = 1; i < tokens2.length; i++){

                    string_here = tokens2[i];

                    if(i == tokens2.length-1){

                        String[] tokens_here = string_here.split("\\)\\)");
                        //String[] coords = tokens_here[0].split()
                        //System.out.println(tokens_here[0]);
                        string_here = tokens_here[0];

                    }else{
                        string_here = tokens2[i];
                    }

                    String[] toks = string_here.split(",");

                    double[][] tempPoly = new double[toks.length][2];
                    int n = toks.length;
                    int counteri = 0;

                    for(int i_ = 0; i_ < n; i_++){

                        tempPoly[counteri][0] = Double.parseDouble(toks[i_].split(" ")[0]);
                        tempPoly[counteri][1] = Double.parseDouble(toks[i_].split(" ")[1]);
                        counteri++;
                    }

                    output.get(plot_id).add(tempPoly);
                }

            }


        }catch( IOException ioException ) {
            //ioException.printStackTrace();
        }

        return output;
    }

    public static HashMap<String, ArrayList<double[][]>> readPolygonHolesFromWKT_string(String fileName, ArrayList<String> plotID1) throws Exception{

        HashMap<String, ArrayList<double[][]>> output = new HashMap<String, ArrayList<double[][]>>();

        String line1;
        int id_number = 1;

        try{
            BufferedReader sc = new BufferedReader( new FileReader(new File(fileName)));
            sc.readLine();
            while((line1 = sc.readLine())!= null){

                String[] tokens =  line1.split(",");
                String[] tokens2 =  line1.split("\\),\\(");

                if(tokens2.length <= 1){
                    continue;
                }

                String plot_id = "";

                try {
                    plot_id = (tokens[tokens.length - 1]);
                }catch (NumberFormatException e){

                    //Not directly comvertable to a number

                    String str = tokens[tokens.length - 1].replaceAll("\\D+","");

                    try {
                        plot_id = (str);
                    }catch (Exception ee){

                        //Still no numbers, let's make our own then I guess
                        //plot_id = id_number++;

                    }

                }

                //System.out.println(tokens2[0]);

                String string_here = null;

                output.put(plot_id, new ArrayList<>());


                for(int i = 1; i < tokens2.length; i++){

                    string_here = tokens2[i];

                    if(i == tokens2.length-1){

                        String[] tokens_here = string_here.split("\\)\\)");
                        //String[] coords = tokens_here[0].split()
                        //System.out.println(tokens_here[0]);
                        string_here = tokens_here[0];

                    }else{
                        string_here = tokens2[i];
                    }

                    String[] toks = string_here.split(",");

                    double[][] tempPoly = new double[toks.length][2];
                    int n = toks.length;
                    int counteri = 0;

                    for(int i_ = 0; i_ < n; i_++){

                        tempPoly[counteri][0] = Double.parseDouble(toks[i_].split(" ")[0]);
                        tempPoly[counteri][1] = Double.parseDouble(toks[i_].split(" ")[1]);
                        counteri++;
                    }

                    output.get(plot_id).add(tempPoly);
                }

            }


        }catch( IOException ioException ) {
            //ioException.printStackTrace();
        }

        return output;
    }

    public String readShapefile(String polyPath){

        File shapeFile = new File(polyPath);
        File fout = null;
        String koealat = null;

        if(shapeFile.getName().contains(".shp")){

            int checkShp = 1;
            DataSource ds = ogr.Open( polyPath);

            Layer layeri = ds.GetLayer(0);
            File checkFile = new File("tempWKT_" + System.currentTimeMillis() + ".csv");
            try {

                if (!checkFile.exists()) {


                    fout = checkFile;

                    if (fout.exists())
                        fout.delete();

                    fout.createNewFile();

                    FileOutputStream fos = new FileOutputStream(fout);
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

                    bw.write("WKT,plot_id");
                    bw.newLine();

                    int backUpId = 1;

                    for (long i = 0; i < layeri.GetFeatureCount(); i++) {

                        Feature tempF = layeri.GetFeature(i);
                        Geometry tempG = tempF.GetGeometryRef();

                        if (tempG == null)
                            continue;

                        String id = "";

                        if (tempF.GetFieldCount() > 0)
                            id = tempF.GetFieldAsString(aR.field);
                        else
                            id = String.valueOf(i);

                        if(id.equals(""))
                            id = "lasutilsID_" + backUpId++;

                        String out = "\"" + tempG.ExportToWkt() + "\"," + id;

                        bw.write(out);
                        bw.newLine();


                    }
                    bw.close();
                }
            }catch(IOException e){
                e.printStackTrace();
            }


            koealat = checkFile.getAbsolutePath();

        }
        return koealat;
    }
}
