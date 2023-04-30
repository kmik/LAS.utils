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

import java.io.File;
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

        for(int y = 0; y < number_of_pix_y; y++) {

            tifBand.ReadRaster(0, y, number_of_pix_x, 1, floatArray);

            for (int x = 0; x < number_of_pix_x; x++) {

                System.out.println("line " + y);
                float value = floatArray[x];

                if (value == 1.0f) {
                    mask[x][y] = true;

                }else{

                }
            }
        }

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


}
