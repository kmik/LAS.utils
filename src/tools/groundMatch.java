package tools;
import LASio.*;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import utils.argumentReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class groundMatch {

    LASReader pointCloud1;
    LASReader pointCloud2;
    argumentReader aR;

    double resolution = 0.5;

    org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();

    org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();

    public groundMatch(LASReader pointCloud1, LASReader pointCloud2, argumentReader aR, int coreNumber) throws IOException {

        this.pointCloud1 = pointCloud1;
        this.pointCloud2 = pointCloud2;



        this.aR = aR;

        this.aR.createOutputFileWithExtension(pointCloud1, "_z_cor.txt");

        this.match();
    }

    public void match(){


        File logFile = new File("lokkeri.txt");

        FileWriter fw = null;

        try {
            if (!logFile.exists())
                logFile.createNewFile();


            fw = new FileWriter(logFile, true);

        }catch (Exception e){
            e.printStackTrace();
        }


        double min_x = Math.min(pointCloud1.minX, pointCloud2.minX);
        double min_y = Math.min(pointCloud1.minY, pointCloud2.minY);
        double min_z = Math.min(pointCloud1.minZ, pointCloud2.minZ);

        double max_x = Math.max(pointCloud1.maxX, pointCloud2.maxX);
        double max_y = Math.max(pointCloud1.maxY, pointCloud2.maxY);
        double max_z = Math.max(pointCloud1.maxZ, pointCloud2.maxZ);

        int n_x = (int)Math.ceil((max_x - min_x) / resolution);
        int n_y = (int)Math.ceil((max_y - min_y) / resolution);
        int n_z = (int)Math.ceil((max_z - min_z) / resolution);

        double[][][] raster1 = new double[n_x][n_y][5];
        double[][][] raster2 = new double[n_x][n_y][5];

        int maxi = 0;
        LasPoint tempPoint = new LasPoint();

        double M = 0, oldM = 0;

        for (int p = 0; p < pointCloud1.getNumberOfPointRecords(); p += 10000) {
            //for(int i = 0; i < n; i++){

            maxi = (int) Math.min(10000, Math.abs(pointCloud1.getNumberOfPointRecords() - (p)));

            try {
                pointCloud1.readRecord_noRAF(p, tempPoint, maxi);
            } catch (Exception e) {
                e.printStackTrace();
            }


            for (int j = 0; j < maxi; j++) {

                //if((j+p) > 1600000)

                //System.out.println((j) + " " + maxi + " " + pointCloud.getNumberOfPointRecords());
                pointCloud1.readFromBuffer(tempPoint);

                if(tempPoint.classification == 2){

                    int slot_x = (int)Math.floor( (tempPoint.x - min_x) / resolution );
                    int slot_y = (int)Math.floor( (max_y - tempPoint.y) / resolution );

                    raster1[slot_x][slot_y][0]++;
                    raster1[slot_x][slot_y][1] += tempPoint.z;

                    oldM = raster1[slot_x][slot_y][2];

                    M = raster1[slot_x][slot_y][2];
                    raster1[slot_x][slot_y][2] = M + (tempPoint.z - M) / raster1[slot_x][slot_y][0];

                    raster1[slot_x][slot_y][3] = raster1[slot_x][slot_y][3] + (tempPoint.z - raster1[slot_x][slot_y][2]) * (tempPoint.z - oldM);

                    raster1[slot_x][slot_y][4] = Math.sqrt(raster1[slot_x][slot_y][3] / (raster1[slot_x][slot_y][0] - 1.0d));


                }

            }
        }

        for (int p = 0; p < pointCloud2.getNumberOfPointRecords(); p += 10000) {
            //for(int i = 0; i < n; i++){

            maxi = (int) Math.min(10000, Math.abs(pointCloud2.getNumberOfPointRecords() - (p)));

            try {
                pointCloud2.readRecord_noRAF(p, tempPoint, maxi);
            } catch (Exception e) {
                e.printStackTrace();
            }


            for (int j = 0; j < maxi; j++) {

                //if((j+p) > 1600000)

                //System.out.println((j) + " " + maxi + " " + pointCloud.getNumberOfPointRecords());
                pointCloud2.readFromBuffer(tempPoint);

                if(tempPoint.classification == 2){

                    int slot_x = (int)Math.floor( (tempPoint.x - min_x) / resolution );
                    int slot_y = (int)Math.floor( (max_y - tempPoint.y) / resolution );

                    raster2[slot_x][slot_y][0]++;
                    raster2[slot_x][slot_y][1] += tempPoint.z;

                    oldM = raster2[slot_x][slot_y][2];

                    M = raster2[slot_x][slot_y][2];
                    raster2[slot_x][slot_y][2] = M + (tempPoint.z - M) / raster2[slot_x][slot_y][0];

                    raster2[slot_x][slot_y][3] = raster2[slot_x][slot_y][3] + (tempPoint.z - raster2[slot_x][slot_y][2]) * (tempPoint.z - oldM);

                    raster2[slot_x][slot_y][4] = Math.sqrt(raster2[slot_x][slot_y][3] / (raster2[slot_x][slot_y][0] - 1.0d));

                }

            }

        }

        ArrayList<Double> differences = new ArrayList<>();

        for(int x = 0; x < n_x; x++){
            for(int y = 0; y < n_y; y++) {

                double mean1 = raster1[x][y][1] / raster1[x][y][0];
                double mean2 = raster2[x][y][1] / raster2[x][y][0];

                if(raster1[x][y][0] > 10 && raster2[x][y][0] > 10 && raster1[x][y][4] < 0.033 && raster2[x][y][4] < 0.033 ){

                    double difference = mean2 - mean1;

                    org.tinfour.common.Vertex tempVertex = new org.tinfour.common.Vertex(x + resolution / 2.0, y + resolution / 2.0, difference);

                    tin.add(tempVertex);
                    differences.add(difference);
                }

            }
        }


        Dataset temppi = gdalE.hei("corrections.tif", n_y, n_x, Float.NaN);// driver.Create("filtered.tif", input.getRasterXSize(), input.getRasterYSize(), 1, gdalconst.GDT_Float32);

        org.tinfour.interpolation.TriangularFacetInterpolator polator = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);
        Band input_band = temppi.GetRasterBand(1);
        float[] outArray = new float[1];

        for(int x = 0; x < n_x; x++){
            for(int y = 0; y < n_y; y++) {

                double interpolatedZ = polator.interpolate(x + resolution / 2.0, y + resolution / 2.0, valuator);
                outArray[0] = (float)interpolatedZ;
                input_band.WriteRaster(x, y, 1, 1, outArray);
                //System.out.println(outArray[0]);
            }
        }

        int kernel = 7;
        double sigma = ((double)kernel-1.0)/6.0;

        System.out.println("SIGMA: " + sigma);

        GaussianSmooth.smooth_tif(temppi, temppi, temppi.getRasterXSize(), temppi.getRasterYSize(), kernel, sigma);  // THIS IS GOOD!!!! :)

        temppi.FlushCache();
        input_band.FlushCache();

        Double average = differences.stream().mapToDouble(val -> val).average().orElse(0.0);

        System.out.println("Average difference: " + average);
        Statistics stat = new Statistics();

        stat.setData(differences);

        System.out.println("Standard deviation: " + stat.getStdDevFromList(average));
        try {

            fw.write(pointCloud1.getFile().getName() + "\t");
            fw.write(average + "\t" + stat.getStdDevFromList(average) + "\n");
            fw.close();
        }catch (Exception e){
            e.printStackTrace();
        }


    }


}
