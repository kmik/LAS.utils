package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import err.toolException;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
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
                                aR.pfac.prepareBuffer(thread_n, i, 20000);
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.exit(1);
                            }
                            for (int j_ = 0; j_ < maxi; j_++) {

                                temp.readFromBuffer(tempPoint);

                                int x = (int) Math.round((tempPoint.x - geoTransform[0]) / geoTransform[1]);
                                int y = (int) Math.round((tempPoint.y - geoTransform[3]) / geoTransform[5]);

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
                        aR.pfac.prepareBuffer(thread_n, i, 20000);
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
}
