package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import err.toolException;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.ogr.Geometry;
import org.gdal.osr.SpatialReference;
import utils.argumentReader;
import utils.pointWriterMultiThread;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import static tools.createCHM.fo;


public class lasRasterTools {

    boolean[][] mask = null;
    double[] geoTransform = null;

    argumentReader aR;
    public lasRasterTools(){

    }

    public lasRasterTools(argumentReader aR){
        this.aR = aR;
    }

    public void printTimeInMinutesSeconds(long timeInMilliseconds, String message){
        System.out.println(message + " -- Time taken: " + (timeInMilliseconds / 1000) / 60 + " minutes and " + (timeInMilliseconds / 1000) % 60 + " seconds.");
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

        String outputFileName = fo.createNewFileWithNewExtension(pointCloud.getFile(), "_raster.tif").getAbsolutePath();
        /*
        double pointCloudMinX = pointCloud.getMinX() - aR.res/2.0;
        double pointCloudMinY = pointCloud.getMinY() - aR.res/2.0;
        double pointCloudMaxX = pointCloud.getMaxX() + aR.res/2.0;
        double pointCloudMaxY = pointCloud.getMaxY() + aR.res/2.0;
*/

        double pointCloudMinX = pointCloud.getMinX();
        double pointCloudMinY = pointCloud.getMinY();
        double pointCloudMaxX = pointCloud.getMaxX();
        double pointCloudMaxY = pointCloud.getMaxY();



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

        try {
            cehoam = driver.Create(outputFileName, rasterWidth, rasterHeight, 1, gdalconst.GDT_Float32);

        }catch (Exception e){
            System.out.println("Not enough points! Are you using remove_buffer and ALL points in this .las file are part of the buffer?");
            return;
        }

        Band band = cehoam.GetRasterBand(1);

        band.SetNoDataValue(Float.NaN);

        SpatialReference sr = new SpatialReference();

        sr.ImportFromEPSG(3067);

        cehoam.SetProjection(sr.ExportToWkt());
        cehoam.SetGeoTransform(geoTransform);

        double[][] chm_array = new double[rasterWidth][rasterHeight];

        double minx = pointCloud.getMinX();
        double miny = pointCloud.getMinY();
        double maxx = pointCloud.getMaxX();
        double maxy = pointCloud.getMaxY();

        long n = pointCloud.getNumberOfPointRecords();

        int thread_n = aR.pfac.addReadThread(pointCloud);

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

                int x = (int) Math.round((tempPoint.x - geoTransform[0]) / geoTransform[1]);
                int y = (int) Math.round((tempPoint.y - geoTransform[3]) / geoTransform[5]);

                if (x >= 0 && x < rasterWidth && y >= 0 && y < rasterHeight) {

                    if(Double.isNaN(chm_array[x][y]))
                        chm_array[x][y] = (double)tempPoint.z;
                    else if ( (float)tempPoint.z > chm_array[x][y])
                        chm_array[x][y] = (double)tempPoint.z;
                    else{

                    }
                }

            }




        }

        System.out.println("minx: " + minx + " pointCloudMinX: " + pointCloudMinX);
        System.out.println("miny: " + miny + " pointCloudMinY: " + pointCloudMinY);
        System.out.println("maxx: " + maxx + " pointCloudMaxX: " + pointCloudMaxX);
        System.out.println("maxy: " + maxy + " pointCloudMaxY: " + pointCloudMaxY);

        copyRasterContents(chm_array, band);

        cehoam.FlushCache();
        band.FlushCache();

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
}
