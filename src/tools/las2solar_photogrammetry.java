package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import err.toolException;
import ij.gui.NonBlockingGenericDialog;
import javafx.util.Pair;
import jdk.nashorn.internal.ir.Block;
import net.e175.klaus.solarpositioning.*;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.ogr.ogr;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import utils.*;


import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static net.e175.klaus.solarpositioning.PSA.calculateSolarPosition;

public class las2solar_photogrammetry {

    private static int solarradiation = 1366;

    Band chm_values;
    float[][] chm_values_f;
    byte[][][] chm_values_f_3d;
    float[][] chm_output_f;

    LASReader pointCloud;

    float rasterMaxValue = 0.0f;

    public las2solar_photogrammetry(String chm_name, argumentReader aR, LASReader pointCloud, boolean d3) throws Exception{

        this.pointCloud = pointCloud;

        System.out.println(this.pointCloud.getMaxZ());

        LasPoint tempPoint = new LasPoint();

        int thread_n = aR.pfac.addReadThread(pointCloud);

        int number_of_last_returns = 0;
        int number_of_all_returns = 0;

        int raster_z_size = (int)Math.ceil((pointCloud.getMaxZ() - pointCloud.getMinZ()) / aR.step);

        Dataset chm = gdal.Open(chm_name);

        this.chm_values_f_3d = new byte[chm.getRasterYSize()][chm.getRasterXSize()][raster_z_size];

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                int x = (int)((tempPoint.x - pointCloud.getMinX()) / aR.step);
                int y = (int)((pointCloud.getMaxY() - tempPoint.y) / aR.step);
                int z = (int)((tempPoint.z - pointCloud.getMinZ()) / aR.step);

                if(chm_values_f_3d[y][x][z] < 100)
                    chm_values_f_3d[y][x][z]++;

            }
        }

        System.out.println("ALL GOOD!");


        ogr.RegisterAll(); //Registering all the formats..

        gdal.AllRegister();

        System.out.println("max cache: " + gdal.GetCacheMax());
        //gdal.SetCacheMax(413375897 * 4);
        gdal.SetCacheMax((int)(aR.gdal_cache_gb * 1073741824));


        Dataset dataset = null;
        Driver driver = null;
        driver = gdal.GetDriverByName("GTiff");
        driver.Register();
        Band band2=null;

        dataset = driver.Create("testi.tif", chm.GetRasterXSize(), chm.GetRasterYSize(), 1, gdalconst.GDT_Float32);
        dataset.SetGeoTransform(chm.GetGeoTransform());
        dataset.SetProjection(chm.GetProjection());
        band2 = dataset.GetRasterBand(1);    // writable band


        int steppi = 100;

        int x_res = (int)Math.ceil((double)chm.getRasterXSize() / (double)steppi);
        int y_res = (int)Math.ceil((double)chm.getRasterYSize() / (double)steppi);

        int[] minutes = new int[]{0, 20, 40};        //float[][][][][][] precomputed = new float[x_res][y_res][12][32][24][2];
        float[][][][][][][] precomputed = new float[x_res][y_res][12][32][24][minutes.length][2];


        SpatialReference src = new SpatialReference();
        SpatialReference dst = new SpatialReference();

        dst.ImportFromProj4("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
        src.ImportFromProj4("++proj=utm +zone=35 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs ");

        CoordinateTransformation ct = new CoordinateTransformation(src, dst);

        double[] gt = chm.GetGeoTransform();

        //GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-7 * 60 * 60 * 1000, "LST"));
        GregorianCalendar time = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));

        System.out.println(time.getTimeZone());

        this.chm_values = chm.GetRasterBand(1);

        chm_values_f = chm_to_array(chm_values);
        int x_ = chm_values.getXSize();
        int y_ = chm_values.getYSize();
        chm_output_f = new float[1][1];



        int[] calendar_month = new int[]{Calendar.JUNE, Calendar.JULY, Calendar.AUGUST};

        int[] n_date_in_month = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30 , 31};


        int x_size = chm_values.getXSize();
        int y_size = chm_values.getYSize();

        float[] raster_read = new float[x_size];
        float value = 0;

        double[] point = new double[3];
        double[] point_transformed = new double[2];

        /*
        Prepare sunrise and sunsets
         */
        ArrayList<int[]> sunriseAndSunset = new ArrayList<>();
        for(int month : calendar_month){
            for(int day = 1; day <= n_date_in_month[month]; day++) {

                time.set(2020, month, day, 12, 00, 00); // 17 October 2003, 12:30:30 LST-07:00

                Calendar[] sunriseSunset = ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(time, point_transformed[1], point_transformed[0]);
                sunriseAndSunset.add(new int[]{sunriseSunset[0].getTime().getHours(), sunriseSunset[1].getTime().getHours()});

            }
        }

        double[] blockArray = new double[]{0.0,0.0};

        solar3dManipulator rM = new solar3dManipulator(x_size, y_size, raster_z_size, new float[y_size][x_size][raster_z_size]);

        Thread[] threads = new Thread[aR.cores];
        int n_funk_per_thread = (int)Math.ceil((double)y_size / (double)aR.cores);

        sunriseAndSunset.clear();

        for(int x = 0; x < x_res; x++){
            for(int y = 0; y < y_res; y++){

                int precompute_x = (int)((double)steppi * (double)x + (double)steppi / 2.0); // x_size / 2;
                int precompute_y = (int)((double)steppi * (double)y + (double)steppi / 2.0); // x_size / 2;

                point[0] = gt[0] + precompute_x * gt[1] + precompute_y * gt[2];
                point[1] = gt[3] + precompute_x * gt[4] + precompute_y * gt[5];
                point[2] = value;

                //System.out.println(Arrays.toString(point));
                point_transformed = ct.TransformPoint(point[0], point[1]);
                //System.out.println(Arrays.toString(point_transformed));

                //System.exit(1);
                for(int month : calendar_month) {
                    for (int day = 1; day <= n_date_in_month[month]; day += 1) {

                        int sunriseSunsetcounter = 0;

                        // int sunrise = sunriseAndSunset.get(sunriseSunsetcounter)[0];
                        // int sunset = sunriseAndSunset.get(sunriseSunsetcounter++)[1];

                        time.set(2020, month, day, 12, 00, 00);

                        //Calendar[] sunriseSunset = ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(time, point_transformed[1], point_transformed[0]);

                        // GregorianCalendar[] res = SPA.calculateSunriseTransitSet(
                        //      time,
                        //       70.978056, // latitude
                        //       25.974722, // longitude
                        //       68); // delta T
                        //System.out.println(sunrise + " ?? " + sunriseSunset[0].getTime().getHours());
                        //System.out.println(sunset + " ?? " + sunriseSunset[1].getTime().getHours());

                        /* TODO: These are not highly accurate, considere trying to just test all
                        hours and check which are < 90 degrees
                         */
                        //int sunrise = sunriseSunset[0].getTime().getHours();
                        //int sunset = sunriseSunset[1].getTime().getHours();

                        //sunriseAndSunset.add(new int[]{sunrise, sunset});

                        //for (int hour = sunrise; hour <= sunset; hour += 1) {
                        for (int hour = 0; hour <= 23; hour += 1) {

                            for(int minute = 0; minute < minutes.length; minute++) {

                                time.set(2020, month, day, hour, minutes[minute], 00);

                                AzimuthZenithAngle result = SPA.calculateSolarPosition(time, point_transformed[1], point_transformed[0], 127, DeltaT.estimate(time));

                                //System.out.println("127: " + result.getZenithAngle() + " " + result.getAzimuth());

                                //result = SPA.calculateSolarPosition(time, point_transformed[1], point_transformed[0], 0, DeltaT.estimate(time));

                                //System.out.println("0: " + result.getZenithAngle() + " " + result.getAzimuth());


                                precomputed[x][y][month][day][hour][minute][0] = (float) result.getZenithAngle();
                                precomputed[x][y][month][day][hour][minute][1] = (float) result.getAzimuth();
                            }

                        }
                    }
                }

            }
        }

        long start = System.currentTimeMillis();

        BlockingQueue<Pair<Integer, byte[][]>> provideRow = new LinkedBlockingQueue<>();

        //System.out.println(chm_values_f.length + " " + chm_values_f[0].length);
        //System.exit(1);
        for(int y = 0; y < chm_values_f_3d.length; y++)
            provideRow.add(new Pair<>(y,chm_values_f_3d[y]));


        for (int i = 0; i < aR.cores; i++) {

            int mini = i * n_funk_per_thread;
            int maxi = Math.min(y_size, mini + n_funk_per_thread);

            threads[i] = (new solarParallel_3d(mini, maxi, x_size,y_size, raster_z_size, rM, sunriseAndSunset, gt, ct, aR, chm_values_f, chm_output_f, precomputed, steppi, rasterMaxValue, provideRow, chm_values_f_3d,
                    pointCloud));
            threads[i].start();

        }

        for (int i = 0; i < threads.length; i++) {

            try {
                threads[i].join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        File outFile = aR.createOutputFile(pointCloud);

        pointWriterMultiThread pw = new pointWriterMultiThread(outFile, pointCloud, "las2las", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
                    continue;
                }

                int x = (int)((tempPoint.x - pointCloud.getMinX()) / aR.step);
                int y = (int)((pointCloud.getMaxY() - tempPoint.y) / aR.step);
                int z = (int)((tempPoint.z - pointCloud.getMinZ()) / aR.step);

                tempPoint.intensity =   (int)(rM.getValue(x, y, z) / solarradiation * 65535.0);

                try {

                    aR.pfac.writePoint(tempPoint, i + j, thread_n);

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        aR.pfac.closeThread(thread_n);

        System.out.println("processing took: " + (System.currentTimeMillis()-start) + " ms with " + aR.cores + " threads");

        dataset.FlushCache();
        chm.FlushCache();

    }

    public las2solar_photogrammetry(String chm_name, argumentReader aR, LASReader pointCloud){

        this.pointCloud = pointCloud;

        System.out.println(this.pointCloud.getMaxZ());

        ogr.RegisterAll(); //Registering all the formats..

        gdal.AllRegister();

        System.out.println("max cache: " + gdal.GetCacheMax());
        //gdal.SetCacheMax(413375897 * 4);
        gdal.SetCacheMax((int)(aR.gdal_cache_gb * 1073741824));


        Dataset dataset = null;
        Driver driver = null;
        driver = gdal.GetDriverByName("GTiff");
        driver.Register();
        Band band2=null;

        Dataset chm = gdal.Open(chm_name);

        dataset = driver.Create("testi.tif", chm.GetRasterXSize(), chm.GetRasterYSize(), 1, gdalconst.GDT_Float32);
        dataset.SetGeoTransform(chm.GetGeoTransform());
        dataset.SetProjection(chm.GetProjection());
        band2 = dataset.GetRasterBand(1);    // writable band


        int steppi = 100;

        int x_res = (int)Math.ceil((double)chm.getRasterXSize() / (double)steppi);
        int y_res = (int)Math.ceil((double)chm.getRasterYSize() / (double)steppi);

        int[] minutes = new int[]{0, 20, 40};        //float[][][][][][] precomputed = new float[x_res][y_res][12][32][24][2];
        float[][][][][][][] precomputed = new float[x_res][y_res][12][32][24][minutes.length][2];


        SpatialReference src = new SpatialReference();
        SpatialReference dst = new SpatialReference();

        dst.ImportFromProj4("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
        src.ImportFromProj4("++proj=utm +zone=35 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs ");

        CoordinateTransformation ct = new CoordinateTransformation(src, dst);

        double[] gt = chm.GetGeoTransform();

        //GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-7 * 60 * 60 * 1000, "LST"));
        GregorianCalendar time = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));

        System.out.println(time.getTimeZone());

        this.chm_values = chm.GetRasterBand(1);

        chm_values_f = chm_to_array(chm_values);
        int x_ = chm_values.getXSize();
        int y_ = chm_values.getYSize();
        chm_output_f = new float[1][1];



        int[] calendar_month = new int[]{Calendar.JUNE, Calendar.JULY, Calendar.AUGUST};

        int[] n_date_in_month = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30 , 31};


        int x_size = chm_values.getXSize();
        int y_size = chm_values.getYSize();

        float[] raster_read = new float[x_size];
        float value = 0;

        double[] point = new double[3];
        double[] point_transformed = new double[2];

        /*
        Prepare sunrise and sunsets
         */
        ArrayList<int[]> sunriseAndSunset = new ArrayList<>();
        for(int month : calendar_month){
            for(int day = 1; day <= n_date_in_month[month]; day++) {

                time.set(2020, month, day, 12, 00, 00); // 17 October 2003, 12:30:30 LST-07:00

                Calendar[] sunriseSunset = ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(time, point_transformed[1], point_transformed[0]);
                sunriseAndSunset.add(new int[]{sunriseSunset[0].getTime().getHours(), sunriseSunset[1].getTime().getHours()});

            }
        }

        double[] blockArray = new double[]{0.0,0.0};

        rasterManipulator rM = new rasterManipulator(band2);

        Thread[] threads = new Thread[aR.cores];
        int n_funk_per_thread = (int)Math.ceil((double)y_size / (double)aR.cores);

        sunriseAndSunset.clear();

        for(int x = 0; x < x_res; x++){
            for(int y = 0; y < y_res; y++){

                int precompute_x = (int)((double)steppi * (double)x + (double)steppi / 2.0); // x_size / 2;
                int precompute_y = (int)((double)steppi * (double)y + (double)steppi / 2.0); // x_size / 2;

                point[0] = gt[0] + precompute_x * gt[1] + precompute_y * gt[2];
                point[1] = gt[3] + precompute_x * gt[4] + precompute_y * gt[5];
                point[2] = value;

                //System.out.println(Arrays.toString(point));
                point_transformed = ct.TransformPoint(point[0], point[1]);
                //System.out.println(Arrays.toString(point_transformed));

                //System.exit(1);
                for(int month : calendar_month) {
                    for (int day = 1; day <= n_date_in_month[month]; day += 1) {

                        int sunriseSunsetcounter = 0;

                       // int sunrise = sunriseAndSunset.get(sunriseSunsetcounter)[0];
                       // int sunset = sunriseAndSunset.get(sunriseSunsetcounter++)[1];

                        time.set(2020, month, day, 12, 00, 00);

                        //Calendar[] sunriseSunset = ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(time, point_transformed[1], point_transformed[0]);

                       // GregorianCalendar[] res = SPA.calculateSunriseTransitSet(
                          //      time,
                         //       70.978056, // latitude
                         //       25.974722, // longitude
                         //       68); // delta T
                        //System.out.println(sunrise + " ?? " + sunriseSunset[0].getTime().getHours());
                        //System.out.println(sunset + " ?? " + sunriseSunset[1].getTime().getHours());

                        /* TODO: These are not highly accurate, considere trying to just test all
                        hours and check which are < 90 degrees
                         */
                        //int sunrise = sunriseSunset[0].getTime().getHours();
                        //int sunset = sunriseSunset[1].getTime().getHours();

                        //sunriseAndSunset.add(new int[]{sunrise, sunset});

                        //for (int hour = sunrise; hour <= sunset; hour += 1) {
                        for (int hour = 0; hour <= 23; hour += 1) {

                            for(int minute = 0; minute < minutes.length; minute++) {

                                time.set(2020, month, day, hour, minutes[minute], 00);

                                AzimuthZenithAngle result = SPA.calculateSolarPosition(time, point_transformed[1], point_transformed[0], 127, DeltaT.estimate(time));

                                //System.out.println("127: " + result.getZenithAngle() + " " + result.getAzimuth());

                                //result = SPA.calculateSolarPosition(time, point_transformed[1], point_transformed[0], 0, DeltaT.estimate(time));

                                //System.out.println("0: " + result.getZenithAngle() + " " + result.getAzimuth());


                                precomputed[x][y][month][day][hour][minute][0] = (float) result.getZenithAngle();
                                precomputed[x][y][month][day][hour][minute][1] = (float) result.getAzimuth();
                            }

                        }
                    }
                }

            }
        }

        long start = System.currentTimeMillis();

        BlockingQueue<Pair<Integer, float[]>> provideRow = new LinkedBlockingQueue<>();

        //System.out.println(chm_values_f.length + " " + chm_values_f[0].length);
        //System.exit(1);
        for(int y = 0; y < chm_values_f.length; y++)
            provideRow.add(new Pair<>(y,chm_values_f[y]));


        for (int i = 0; i < aR.cores; i++) {

            int mini = i * n_funk_per_thread;
            int maxi = Math.min(y_size, mini + n_funk_per_thread);

            threads[i] = (new solarParallel(mini, maxi, x_size,y_size, rM, sunriseAndSunset, gt, ct, aR, chm_values_f, chm_output_f, precomputed, steppi, rasterMaxValue, provideRow));
            threads[i].start();

        }

        for (int i = 0; i < threads.length; i++) {

            try {
                threads[i].join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("processing took: " + (System.currentTimeMillis()-start) + " ms with " + aR.cores + " threads");

        if(false)
        for(int y = 0; y < y_size; y++){

            start = System.currentTimeMillis();

            for(int x = 0; x < x_size; x++) {

                //value = raster_read[x];
                value = chm_values_f[x][y];

                if(value <= 0)
                    continue;

                point[0] = gt[0] + x * gt[1] + y * gt[2];
                point[1] = gt[3] + x * gt[4] + y * gt[5];
                point[2] = value;

                point_transformed = ct.TransformPoint(point[0], point[1]);

                int sunriseSunsetcounter = 0;

                double dailyAverageInsolation = 0;

                for(int month : calendar_month){

                    for(int day = 1; day <= n_date_in_month[month]; day += 2){

                        int sunrise = sunriseAndSunset.get(sunriseSunsetcounter)[0];
                        int sunset = sunriseAndSunset.get(sunriseSunsetcounter++)[1];

                        for(int hour = sunrise; hour <= sunset; hour += 1){

                            time.set(2020, month, day, hour, 00, 00);

                            AzimuthZenithAngle result = calculateSolarPosition(time, point_transformed[1], point_transformed[0]);

                            /*
                            Only account for when sun is visible, i.e. time between sunrise and sunset
                             */
                            if(result.getZenithAngle() > 90.0)
                                continue;

                            double insolation = (double)solarradiation * Math.cos(Math.toRadians(result.getZenithAngle()));

                            if(insolation < 0.0){
                                throw new toolException("Negative insolation is impossible!");
                            }

                            blockArray = isBlocked(x, y, value, aR.step,(result.getZenithAngle()), Math.tan(Math.toRadians(90.0-result.getZenithAngle())), (float) (result.getAzimuth()));

                            if(blockArray[0] > 0){

                                /* If the average obstructed depth is less than one meter */
                                if((blockArray[0] / blockArray[1]) < 1.0){

                                    dailyAverageInsolation = (dailyAverageInsolation + insolation) / 2.0;

                                }
                            }else{
                                dailyAverageInsolation = (dailyAverageInsolation + insolation) / 2.0;
                            }

                            /* This means there is nothing blocking the sunray */
                            if(blockArray[0] == 0){
                            }
                        }
                    }
                }

                raster_read[0] = (float)dailyAverageInsolation;
                band2.WriteRaster(x, y, 1, 1, raster_read);

            }

            System.out.println("one row of size: " + aR.step + "m took: " + (System.currentTimeMillis()-start) + " ms " + y + "/" + y_size);

        }

        dataset.FlushCache();
        chm.FlushCache();

    }

    /**
     * Check if the sun ray intersects with CHM. If it does then we return false.
     * If the ray travels outside of the image before hitting anything, we retrn true.
     * Also return true if the ray z reaches @param z + 30 (we assume that there are
     * no more natural obstacles at this point).
     * @param x
     * @param y
     * @param z
     * @param resolution
     * @param zenith_angle
     * @param slope
     * @param direction_angle
     * @return
     */

    public double[] isBlocked(int x, int y, float z, double resolution, double zenith_angle, double slope, float direction_angle){

        double[] output = new double[]{0.0d, 0.0d};
        double increment_in_meters = resolution * 0.5;
        double current_distance_from_point = 0;

        double center_of_pixel_x = (double)x + 0.5 * resolution;
        double center_of_pixel_y = (double)y - 0.5 * resolution;

        double x_prev = center_of_pixel_x;
        double y_prev = center_of_pixel_y;

        double x_ = center_of_pixel_x,y_ = center_of_pixel_y;

        double start_z = z;

        double sun_vector_z = z;

        //float[] readFromRaster = new float[1];

        //double sum_obstructed_depth = 0.0;
        //double sum_obstructed_length = 0.0;

        //while(sun_vector_z <= start_z + 30){
        while(sun_vector_z < this.pointCloud.getMaxZ()){

            current_distance_from_point += increment_in_meters;

            /** These are fast cos and fast sin. Not super accurate, but will do for now */
            x_ = x_ + current_distance_from_point * cos(direction_angle);
            y_ = y_ + current_distance_from_point * sin(direction_angle);

            if(x_ == x_prev && y_ == y_prev){
                continue;
            }

            double z_sun_ray = start_z + current_distance_from_point * slope;

            sun_vector_z = z_sun_ray;

            if(x_ >= chm_values.getXSize() || x_ < 0 || y_ < 0 || y_ >= chm_values.getYSize())
                break;

            double new_z = chm_values_f[(int)x_][(int)y_];

            if(new_z > z_sun_ray){

                output[0] += (new_z - z_sun_ray);
                output[1] += increment_in_meters;

            }

            x_prev = x_;
            y_prev = y_;

        }

        return output;

    }

    public float[][] chm_to_array(Band chm) {

        int x_ = chm.getXSize();
        int y_ = chm.getYSize();

        float[][] output = new float[y_][x_];

        float[] raster_read = new float[x_];

        for (int y = 0; y < y_; y++) {

            chm_values.ReadRaster(0, y, x_, 1, raster_read);
            long start = System.currentTimeMillis();

            for (int x = 0; x < x_; x++) {
                output[y][x] = raster_read[x];
                if(raster_read[x] > this.rasterMaxValue)
                    rasterMaxValue = raster_read[x];

            }

        }
        return output;
    }

    public void array_to_chm(float[][] array, Band chm) {

        int x_ = chm.getXSize();
        int y_ = chm.getYSize();

        //float[][] output = new float[x_][y_];

        float[] raster_read = new float[x_];

        for (int y = 0; y < y_; y++) {

            for (int x = 0; x <x_; x++) {
                raster_read[x] = array[x][y];
            }

            chm.WriteRaster(0, y, x_, 1, raster_read);

        }
    }

    static final int precision = 100; // gradations per degree, adjust to suit

    static final int modulus = 360*precision;
    static final float[] sin = new float[modulus]; // lookup table
    static {
        // a static initializer fills the table
        // in this implementation, units are in degrees
        for (int i = 0; i<sin.length; i++) {
            sin[i]=(float)Math.sin((i*Math.PI)/(precision*180));
        }
    }
    // Private function for table lookup
    private static float sinLookup(int a) {
        return a>=0 ? sin[a%(modulus)] : -sin[-a%(modulus)];
    }

    // These are your working functions:
    public static float sin(float a) {
        return sinLookup((int)(a * precision + 0.5f));
    }
    public static float cos(float a) {
        return sinLookup((int)((a+90f) * precision + 0.5f));
    }


    //HOUR TO HOUR ANGLE
    // given an hour (midnight = 0, 12 = midday), calculate the hour angle at 15 degrees per hour.
    // returns radians rotated at this hour

    public double HourToHourAngle(double hourFromMidnight){

        double hourAngle = Math.toRadians(15*(hourFromMidnight - 12));
        return hourAngle;

    }

    //DETERMINE SOLAR DECLILANATION
    // given a day of the year, determine the solar declination.

    //By Jack Williams, n5736153
    public double SolarDeclinaton(int dayOfYear){

        double declination = Math.toRadians(23.45) * (Math.sin((Math.toRadians(360)/365)*(dayOfYear)));
        return declination;

    }


}

class solarParallel extends Thread {

    float[][] chm;
    float[][] chm_output;

    int min, max, x_size, y_size;
    rasterManipulator rM;
    ArrayList<int[]> sunriseAndSunset;
    double[] gt;
    CoordinateTransformation ct;
    argumentReader aR;

    float[][][][][][][] precomputed;
    int precomputed_resolution;

    float rasterMaxValue;

    BlockingQueue<Pair<Integer, float[]>> providerRow;

    public solarParallel(int min, int max, int x_size, int y_size, rasterManipulator rM, ArrayList<int[]> sunriseAndSunset,
                         double[] gt, CoordinateTransformation ct, argumentReader aR, float[][] chm, float[][] chm_output,
                         float[][][][][][][] precomputed, int precomputed_resolution, float rasterMaxValue, BlockingQueue<Pair<Integer, float[]>> providerRow) {

        this.providerRow = providerRow;
        this.rasterMaxValue = rasterMaxValue;
        this.precomputed_resolution = precomputed_resolution;
        this.precomputed = precomputed;
        this.chm_output = chm_output;
        this.chm = chm;

        this.min = min;
        this.max = max;
        this.x_size = x_size;
        this.y_size = y_size;

        this.rM = rM;
        this.sunriseAndSunset = sunriseAndSunset;
        this.gt = gt;
        this.ct = ct;

        this.aR = aR;
    }

    @Override
    public void run() {

        float value;
        double[] point = new double[3];
        double[] point_transformed = new double[3];

        int[] calendar_month = new int[]{Calendar.JUNE, Calendar.JULY, Calendar.AUGUST};

        int[] n_date_in_month = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30 , 31};
        int[] minutes = new int[]{0, 20, 40};
        int solarradiation = 1366;

        boolean debug = false;

        //for (int y = min; y < this.max; y++) {
        while(true){

            if (providerRow.isEmpty())
                break;

            Pair<Integer, float[]> row = providerRow.poll();

            float[] averageInsol = new float[x_size];

            for (int x = 0; x < x_size; x++) {

                int precomputed_x = (int)Math.floor((double)x / (double)precomputed_resolution);
                int precomputed_y = (int)Math.floor((double)1 / (double)precomputed_resolution);

                //value = chm[x][y];
                value = row.getValue()[x];

                if(value <= 0)
                    continue;

                //point[0] = gt[0] + x * gt[1] + y * gt[2];
                //point[1] = gt[3] + x * gt[4] + y * gt[5];
                //point[2] = value;

                //point_transformed = ct.TransformPoint(point[0], point[1]);

                double stupid_irradiance_sum = 0.0;
                int sunriseSunsetcounter = 0;

                double dailyAverageInsolation = 0;
                double dailyAverageInsolation_ = 0;
                double hour_mean_insolation = 0;

                int n = 0;

                int minuteSum = 0;

                boolean day_switch = false;
                int n_ = 0;
                for(int month : calendar_month){

                    for(int day = 1; day <= n_date_in_month[month]; day += 5){

                        //int sunrise = sunriseAndSunset.get(sunriseSunsetcounter)[0];
                        //int sunset = sunriseAndSunset.get(sunriseSunsetcounter++)[1];
                        dailyAverageInsolation_ = 0.0;

                        minuteSum = 0;

                        //for(int hour = sunrise; hour <= sunset; hour += 1){
                        for(int hour = 0; hour <= 23; hour += 1) {

                            n_ = 0;

/*
                            int start = 0;

                            if(!day_switch){
                                start = 1;
                                day_switch = !day_switch;
                            }else
                                day_switch = !day_switch;
*/
                            hour_mean_insolation = 0;

                            for (int minute = 0; minute < minutes.length; minute += 1) {


                                //long start = System.nanoTime();
                                //time.set(2020, month, day, hour, 00, 00);

                                //AzimuthZenithAngle result = PSA.calculateSolarPosition(time, point_transformed[0], point_transformed[1]);
                                float[] precomp = precomputed[precomputed_x][precomputed_y][month][day][hour][minute];
                                //precomp[0] = (float)result.getZenithAngle();
                                //precomp[1] = (float)result.getAzimuth();

                                //System.out.println("hour: " + hour + " dir: " + precomp[1] + " angle: " + precomp[0]);

                            /*
                            Only account for when sun is visible, i.e. time between sunrise and sunset
                             */
                                //if(result.getZenithAngle() > 90.0)
                                if ((precomp[0] > 80.0f)) // || precomp[1] > 270.0f || precomp[1] < 90.0f)
                                    continue;

                                //double insolation = (double)solarradiation * Math.cos(Math.toRadians(result.getZenithAngle()));
                                //double insolation = (double)solarradiation * Math.cos(Math.toRadians(precomp[0]));
                                //double insolation = (double) solarradiation * Math.cos(Math.toRadians(precomp[0]));
                                double insolation = (double) solarradiation * cos((precomp[0]));

                                //System.out.println("insolation: " + insolation + " w/m2 " + month + " " + result.getZenithAngle());

                                //double[] blockArray = isBlocked(x, y, value, aR.step,(result.getZenithAngle()), Math.tan(Math.toRadians(90.0-result.getZenithAngle())), (float) (result.getAzimuth()));
                                minuteSum += (minutes[1]-minutes[0]);

                                double slope = Math.tan(Math.toRadians(90.0f-precomp[0]));

                                //System.out.println("angle: " + precomp[1] + " slope: " + precomp[0]);
                                double[] blockArray = isBlocked(x, row.getKey(), value, aR.step, (precomp[0]), slope, (float) (precomp[1]), hour);

                                //System.out.println("insolation: " + slope + " " + (90.0f - precomp[0])) ;
                                //if (debug)
                                //    if (x == 130 && y == 1640)
                                //        System.out.println("block: " + Arrays.toString(blockArray) + " hour: " + hour + " sunrise: " + sunrise + " set: " + sunset + " precom: " + precomp[0]);

                                if (blockArray[0] > 0) {

                                    /* If the average obstructed depth is less than one meter */
                                    if ((blockArray[0] / blockArray[1]) < 0.5 && false) {

                                        //dailyAverageInsolation = (dailyAverageInsolation + insolation) / 2.0;
                                        //dailyAverageInsolation = dailyAverageInsolation + (insolation - dailyAverageInsolation)/(double)++n;
                                        //dailyAverageInsolation_ += insolation;
                                        hour_mean_insolation += insolation;

                                    }

                                } else {
                                    //dailyAverageInsolation = dailyAverageInsolation + (insolation - dailyAverageInsolation)/(double)++n;
                                    //dailyAverageInsolation_ += insolation;
                                    hour_mean_insolation += insolation;
                                    //n_++;
                                }
                                //System.out.println("ave: " + dailyAverageInsolation);
                                /* This means there is nothing blocking the sunray */
                                if (blockArray[0] == 0) {

                                }
                            }

                            dailyAverageInsolation_ += hour_mean_insolation / (double)minutes.length;
                        }

                        //dailyAverageInsolation_ = dailyAverageInsolation_ / ((float)minuteSum / 60.0f);
                        //System.out.println(dailyAverageInsolation_ + " " + n_);
                        //dailyAverageInsolation_ /= (double)n_;


                        dailyAverageInsolation = dailyAverageInsolation + ((dailyAverageInsolation_ / 24.0) - dailyAverageInsolation)/(double)++n;
                    }
                }
                //chm_output[x][y] = (float)dailyAverageInsolation;

                averageInsol[x] = (float)dailyAverageInsolation;

                //if(debug)
               // if(x == 130 && y == 1640) {
                   // System.out.println("AVERAGE_DEBUF: " + dailyAverageInsolation + " val: " + value);
                    //System.exit(1);
                //}
            }

            rM.setValue(row.getKey(),averageInsol  );

            //System.out.println(y + " / " + this.max );
        }
    }

    public double[] isBlocked(int x, int y, float z, double resolution, double zenith_angle, double slope, float direction_angle, int hour){

        double tolerance = 0.0;

        double[] output = new double[]{0.0d, 0.0d};
        double increment_in_meters = resolution / 2.0;

        /* Start from 1 meter */
        double current_distance_from_point = resolution - increment_in_meters;
        /*
        double current_distance_from_point_plus5 = 1.0 - increment_in_meters;
        double current_distance_from_point_plus10 = 1.0 - increment_in_meters;
        double current_distance_from_point_plus15 = 1.0 - increment_in_meters;
        double current_distance_from_point_plus20 = 1.0 - increment_in_meters;
*/
        double center_of_pixel_x = (double)x + 0.5 * resolution;
        double center_of_pixel_y = (double)y + 0.5 * resolution;

        double x_prev = center_of_pixel_x;
        double y_prev = center_of_pixel_y;

        double x_ = center_of_pixel_x,y_ = center_of_pixel_y;
        /*
        double x_plus5 = center_of_pixel_x,y_plus5 = center_of_pixel_y;
        double x_plus10 = center_of_pixel_x,y_plus10 = center_of_pixel_y;
        double x_plus15 = center_of_pixel_x,y_plus15 = center_of_pixel_y;
        double x_plus20 = center_of_pixel_x,y_plus20 = center_of_pixel_y;
*/
        double start_z = z;

        double sun_vector_z = z;
        /*
        double sun_vector_z_plus5 = z;
        double sun_vector_z_plus10 = z;
        double sun_vector_z_plus15 = z;
        double sun_vector_z_plus20 = z;


         */
        //float[] readFromRaster = new float[1];

        //double sum_obstructed_depth = 0.0;
        //double sum_obstructed_length = 0.0;

        //System.out.println("DIRECTION: " + direction_angle + " hour: " + hour);


        //float cosAngle = (float)cos(direction_angle);
        //float cosAngle = (float)Math.cos((Math.toRadians(direction_angle)));
        float cosAngle = 0;
        //float sinAngle = (float)sin(direction_angle);
        //float sinAngle = (float)Math.sin(Math.toRadians(direction_angle));
        float sinAngle = 0;

        int quadrant = -1;

        if(direction_angle < 90.0f){
            quadrant = 1;
            //cosAngle = (float)Math.cos(Math.toRadians(90.0 - direction_angle));
            //sinAngle = (float)Math.sin(Math.toRadians(90.0 - direction_angle));

            cosAngle = cos((90.0f - direction_angle));
            sinAngle = sin((90.0f - direction_angle));


        }else if(direction_angle < 180.0f){
            quadrant = 2;
            //cosAngle = (float)Math.cos(Math.toRadians(180.0 - direction_angle));
            //sinAngle = (float)Math.sin(Math.toRadians(180.0 - direction_angle));
            cosAngle = cos((180.0f - direction_angle));
            sinAngle = sin((180.0f - direction_angle));


        }else if(direction_angle < 270.0f){
            quadrant = 3;
            //cosAngle = (float)Math.cos(Math.toRadians(270.0 - direction_angle));
            //sinAngle = (float)Math.sin(Math.toRadians(270.0 - direction_angle));

            cosAngle = cos((270.0f - direction_angle));
            sinAngle = sin((270.0f - direction_angle));

        }else if(direction_angle < 360.0f){
            quadrant = 4;
            //cosAngle = (float)Math.cos(Math.toRadians(360.0 - direction_angle));
            //sinAngle = (float)Math.sin(Math.toRadians(360.0 - direction_angle));

            cosAngle = cos((360.0f - direction_angle));
            sinAngle = sin((360.0f - direction_angle));

        }

        int counter = 0;
/*
        float new_z_plus5 = -1;
        float new_z_plus10 = -1;
        float new_z_plus15 = -1;
        float new_z_plus20 = -1;
*/
        //while(sun_vector_z <= start_z + 30){
        while(sun_vector_z < this.rasterMaxValue){

            current_distance_from_point += increment_in_meters;
            //System.out.println("x: " + (int)x + " y: " + (int)y + " dir: "  + direction_angle + " x_: " + (int)x_ + " y_: " + (int)y_ + " c: " + counter++ + " i: " + increment_in_meters + " dis: " + current_distance_from_point + " q: " + quadrant);

           /*
            current_distance_from_point_plus5 += increment_in_meters + 5.0;
            current_distance_from_point_plus10 += increment_in_meters + 10.0;
            current_distance_from_point_plus15 += increment_in_meters + 15.0;
            current_distance_from_point_plus20 += increment_in_meters + 20.0;
*/
            switch (quadrant){

                case 1:

                    y_ = y - sinAngle * current_distance_from_point;
                    x_ = x + cosAngle * current_distance_from_point;
/*
                    y_plus5 = y_plus5 - sinAngle * current_distance_from_point_plus5;
                    x_plus5 = x_plus5 + cosAngle * current_distance_from_point_plus5;

                    y_plus10 = y_plus10 - sinAngle * current_distance_from_point_plus10;
                    x_plus10 = x_plus10 + cosAngle * current_distance_from_point_plus10;

                    y_plus15 = y_plus15 - sinAngle * current_distance_from_point_plus15;
                    x_plus15 = x_plus15 + cosAngle * current_distance_from_point_plus15;

                    y_plus20 = y_plus20 - sinAngle * current_distance_from_point_plus20;
                    x_plus20 = x_plus20 + cosAngle * current_distance_from_point_plus20;
*/
                    break;

                case 2:
                    y_ = y + cosAngle * current_distance_from_point;
                    x_ = x + sinAngle * current_distance_from_point;
/*
                    y_plus5 = y_plus5 + sinAngle * current_distance_from_point_plus5;
                    x_plus5 = x_plus5 + cosAngle * current_distance_from_point_plus5;

                    y_plus10 = y_plus10 + sinAngle * current_distance_from_point_plus10;
                    x_plus10 = x_plus10 + cosAngle * current_distance_from_point_plus10;

                    y_plus15 = y_plus15 + sinAngle * current_distance_from_point_plus15;
                    x_plus15 = x_plus15 + cosAngle * current_distance_from_point_plus15;

                    y_plus20 = y_plus20 + sinAngle * current_distance_from_point_plus20;
                    x_plus20 = x_plus20 + cosAngle * current_distance_from_point_plus20;
                    */
                    break;

                case 3:
                    y_ = y + sinAngle * current_distance_from_point;
                    x_ = x - cosAngle * current_distance_from_point;
/*
                    y_plus5 = y_plus5 + sinAngle * current_distance_from_point_plus5;
                    x_plus5 = x_plus5 - cosAngle * current_distance_from_point_plus5;

                    y_plus10 = y_plus10 + sinAngle * current_distance_from_point_plus10;
                    x_plus10 = x_plus10 - cosAngle * current_distance_from_point_plus10;

                    y_plus15 = y_plus15 + sinAngle * current_distance_from_point_plus15;
                    x_plus15 = x_plus15 - cosAngle * current_distance_from_point_plus15;

                    y_plus20 = y_plus20 + sinAngle * current_distance_from_point_plus20;
                    x_plus20 = x_plus20 - cosAngle * current_distance_from_point_plus20;
*/
                    break;

                case 4:
                    y_ = y - cosAngle * current_distance_from_point;
                    x_ = x - sinAngle * current_distance_from_point;
/*
                    y_plus5 = y_plus5 - sinAngle * current_distance_from_point_plus5;
                    x_plus5 = x_plus5 - cosAngle * current_distance_from_point_plus5;

                    y_plus10 = y_plus10 - sinAngle * current_distance_from_point_plus10;
                    x_plus10 = x_plus10 - cosAngle * current_distance_from_point_plus10;

                    y_plus15 = y_plus15 - sinAngle * current_distance_from_point_plus15;
                    x_plus15 = x_plus15 - cosAngle * current_distance_from_point_plus15;

                    y_plus20 = y_plus20 - sinAngle * current_distance_from_point_plus20;
                    x_plus20 = x_plus20 - cosAngle * current_distance_from_point_plus20;
*/
                    break;

                default:
                    break;

            }

            if(((int)x_ == (int)x_prev && (int)y_ == (int)y_prev) || ((int)x_ == (int)x && (int)y_ == (int)y)){
                continue;
            }

            double z_sun_ray = start_z + current_distance_from_point * slope;
            /*
            double z_sun_ray_plus_5 = start_z + current_distance_from_point_plus5 * slope;
            double z_sun_ray_plus_10 = start_z + current_distance_from_point_plus10 * slope;
            double z_sun_ray_plus_15 = start_z + current_distance_from_point_plus15 * slope;
            double z_sun_ray_plus_20 = start_z + current_distance_from_point_plus20 * slope;


             */
            sun_vector_z = z_sun_ray;

            if(x_ >= this.x_size || x_ < 0 || y_ < 0 || y_ >= this.y_size) {
                break;
            }

            float new_z = chm[(int)y_][(int)x_];
/*
            if(x_plus5 < this.x_size && x_plus5 >= 0 && y_plus5 >= 0 && y_plus5 < this.y_size)
                new_z_plus5 = chm[(int)x_plus5][(int)y_plus5];

            if(x_plus10 < this.x_size && x_plus10 >= 0 && y_plus10 >= 0 && y_plus10 < this.y_size)
                new_z_plus10 = chm[(int)x_plus10][(int)y_plus10];

            if(x_plus15 < this.x_size && x_plus15 >= 0 && y_plus15 >= 0 && y_plus15 < this.y_size)
                new_z_plus15 = chm[(int)x_plus15][(int)y_plus15];

            if(x_plus20 < this.x_size && x_plus20 >= 0 && y_plus20 >= 0 && y_plus20 < this.y_size)
                new_z_plus20 = chm[(int)x_plus20][(int)y_plus20];


 */
            //if(x == 320 && y == 1700)
            //  System.out.println("sun: " + z_sun_ray + " new_z: " + new_z + " " + Arrays.toString(output) + " dist: " + current_distance_from_point + " ang: " + zenith_angle + " slope: " + slope);

            if(new_z > (z_sun_ray + tolerance)){

                //System.out.println(counter);
                output[0] += (new_z - z_sun_ray);
                output[1] += increment_in_meters;
                return output;

            }

            x_prev = x_;
            y_prev = y_;
            /*
            //x_ = x_ + current_distance_from_point * cos(direction_angle);
            //y_ = y_ + current_distance_from_point * sin(direction_angle);

            x_ = x_ + current_distance_from_point * cosAngle;
            y_ = y_ + current_distance_from_point * sinAngle;

            if((int)x_ == (int)x_prev && (int)y_ == (int)y_prev){
                continue;
            }

            double z_sun_ray = start_z + current_distance_from_point * slope;

            sun_vector_z = z_sun_ray;

            if(x_ >= this.x_size || x_ < 0 || y_ < 0 || y_ >= this.y_size)
                break;

            double new_z = chm[(int)x_][(int)y_];

            //if(x == 320 && y == 1700)
              //  System.out.println("sun: " + z_sun_ray + " new_z: " + new_z + " " + Arrays.toString(output) + " dist: " + current_distance_from_point + " ang: " + zenith_angle + " slope: " + slope);
            if(new_z > z_sun_ray){

                output[0] += (new_z - z_sun_ray);
                output[1] += increment_in_meters;

            }

            x_prev = x_;
            y_prev = y_;

             */

            //counter++;

        }
        //System.out.println(counter);


        return output;

    }


    static final int precision = 100; // gradations per degree, adjust to suit

    static final int modulus = 360*precision;
    static final float[] sin = new float[modulus]; // lookup table
    static final float[] cos = new float[modulus]; // lookup table
    static {
        // a static initializer fills the table
        // in this implementation, units are in degrees
        for (int i = 0; i<sin.length; i++) {
            sin[i]=(float)Math.sin((i*Math.PI)/(precision*180));
        }

        for (int i = 0; i<cos.length; i++) {
            cos[i]=(float)Math.cos((i*Math.PI)/(precision*180));
        }

    }
    // Private function for table lookup
    private static float sinLookup(int a) {
        return a>=0 ? sin[a%(modulus)] : -sin[-a%(modulus)];
    }
    // Private function for table lookup
    private static float cosLookup(int a) {
        return a>=0 ? cos[a%(modulus)] : -cos[-a%(modulus)];
    }

    // These are your working functions:
    public static float sin(float a) {
        return sinLookup((int)(a * precision + 0.5f));
    }
    public static float cos(float a) {
        return sinLookup((int)((a+90f) * precision + 0.5f));
    }


    //HOUR TO HOUR ANGLE
    // given an hour (midnight = 0, 12 = midday), calculate the hour angle at 15 degrees per hour.
    // returns radians rotated at this hour

    public double HourToHourAngle(double hourFromMidnight){

        double hourAngle = Math.toRadians(15*(hourFromMidnight - 12));
        return hourAngle;

    }

    //DETERMINE SOLAR DECLILANATION
    // given a day of the year, determine the solar declination.

    //By Jack Williams, n5736153
    public double SolarDeclinaton(int dayOfYear){

        double declination = Math.toRadians(23.45) * (Math.sin((Math.toRadians(360)/365)*(dayOfYear)));
        return declination;

    }
}


class solarParallel_3d extends Thread {

    LASReader pointCloud;

    float[][] chm;
    byte[][][] chm_3d;
    float[][] chm_output;

    int min, max, x_size, y_size, z_size;
    solar3dManipulator rM;
    ArrayList<int[]> sunriseAndSunset;
    double[] gt;
    CoordinateTransformation ct;
    argumentReader aR;

    float[][][][][][][] precomputed;
    int precomputed_resolution;

    float rasterMaxValue;

    double p_cloud_min_x, p_cloud_max_y, p_cloud_min_z;

    BlockingQueue<Pair<Integer, byte[][]>> providerRow;

    public solarParallel_3d(int min, int max, int x_size, int y_size, int z_size, solar3dManipulator rM, ArrayList<int[]> sunriseAndSunset,
                            double[] gt, CoordinateTransformation ct, argumentReader aR, float[][] chm, float[][] chm_output,
                            float[][][][][][][] precomputed, int precomputed_resolution, float rasterMaxValue, BlockingQueue<Pair<Integer, byte[][]>> providerRow,
                            byte[][][] struct_3d, LASReader pointCloud) {

        this.pointCloud = pointCloud;
        this.z_size = z_size;


        this.p_cloud_max_y = pointCloud.getMaxY();
        this.p_cloud_min_z = pointCloud.getMinZ();
        this.p_cloud_min_x = pointCloud.getMinX();


        this.chm_3d = struct_3d;
        this.providerRow = providerRow;
        this.rasterMaxValue = rasterMaxValue;
        this.precomputed_resolution = precomputed_resolution;
        this.precomputed = precomputed;
        this.chm_output = chm_output;
        this.chm = chm;

        this.min = min;
        this.max = max;
        this.x_size = x_size;
        this.y_size = y_size;

        this.rM = rM;
        this.sunriseAndSunset = sunriseAndSunset;
        this.gt = gt;
        this.ct = ct;

        this.aR = aR;
    }

    @Override
    public void run() {

        byte value;
        double[] point = new double[3];
        double[] point_transformed = new double[3];

        int[] calendar_month = new int[]{Calendar.JUNE, Calendar.JULY, Calendar.AUGUST};

        int[] n_date_in_month = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30 , 31};
        int[] minutes = new int[]{0, 20, 40};
        int solarradiation = 1366;

        boolean debug = false;
        float chm_value = 0f;
        //for (int y = min; y < this.max; y++) {
        while(true) {

            if (providerRow.isEmpty())
                break;

            Pair<Integer, byte[][]> row = providerRow.poll();

            float[] averageInsol = new float[x_size];

            for (int x = 0; x < x_size; x++) {
                for (int z = 0; z < z_size; z++) {

                    int precomputed_x = (int) Math.floor((double) x / (double) precomputed_resolution);
                    int precomputed_y = (int) Math.floor((double) 1 / (double) precomputed_resolution);

                    //value = chm[x][y];
                    value = row.getValue()[x][z];
                    chm_value = chm[row.getKey()][x];

                    if (value <= 0)
                        continue;

                    double stupid_irradiance_sum = 0.0;
                    int sunriseSunsetcounter = 0;

                    double dailyAverageInsolation = 0;
                    double dailyAverageInsolation_ = 0;
                    double hour_mean_insolation = 0;

                    int n = 0;

                    int minuteSum = 0;

                    boolean day_switch = false;
                    int n_ = 0;

                    for (int month : calendar_month) {

                        for (int day = 1; day <= n_date_in_month[month]; day += 5) {

                            dailyAverageInsolation_ = 0.0;

                            minuteSum = 0;

                            //for(int hour = sunrise; hour <= sunset; hour += 1){
                            for (int hour = 0; hour <= 23; hour += 1) {

                                n_ = 0;

                                hour_mean_insolation = 0;

                                for (int minute = 0; minute < minutes.length; minute += 1) {

                                    float[] precomp = precomputed[precomputed_x][precomputed_y][month][day][hour][minute];

                                    if ((precomp[0] > 90.0f)) // || precomp[1] > 270.0f || precomp[1] < 90.0f)
                                        continue;

                                    double insolation = (double) solarradiation * cos((precomp[0]));

                                    //System.out.println("insolation: " + insolation + " w/m2 " + month + " " + result.getZenithAngle());

                                    //double[] blockArray = isBlocked(x, y, value, aR.step,(result.getZenithAngle()), Math.tan(Math.toRadians(90.0-result.getZenithAngle())), (float) (result.getAzimuth()));
                                    minuteSum += (minutes[1] - minutes[0]);

                                    double slope = Math.tan(Math.toRadians(90.0f - precomp[0]));

                                    //System.out.println("angle: " + slope + " slope: " + precomp[0]);
                                    //System.out.println("chm:value: " + rasterMaxValue);
                                    //double[] blockArray = isBlocked(x, row.getKey(), z, value, aR.step, (precomp[0]), slope, (float) (precomp[1]), hour);
                                    double[] blockArray = isBlocked_ray_trace(x, row.getKey(), z, value, aR.step, (precomp[0]), slope, (float) (precomp[1]), hour, rasterMaxValue);

                                    //System.out.println("insolation: " + slope + " " + (90.0f - precomp[0])) ;
                                    //if (debug)
                                    //    if (x == 130 && y == 1640)
                                    //        System.out.println("block: " + Arrays.toString(blockArray) + " hour: " + hour + " sunrise: " + sunrise + " set: " + sunset + " precom: " + precomp[0]);

                                    if (blockArray[0] > 0) {

                                        /* If the average obstructed depth is less than one meter */
                                        if ((blockArray[0] / blockArray[1]) < 0.5 && false) {

                                            //dailyAverageInsolation = (dailyAverageInsolation + insolation) / 2.0;
                                            //dailyAverageInsolation = dailyAverageInsolation + (insolation - dailyAverageInsolation)/(double)++n;
                                            //dailyAverageInsolation_ += insolation;
                                            hour_mean_insolation += insolation;

                                        }

                                    } else {
                                        //dailyAverageInsolation = dailyAverageInsolation + (insolation - dailyAverageInsolation)/(double)++n;
                                        //dailyAverageInsolation_ += insolation;
                                        hour_mean_insolation += insolation;
                                        //n_++;
                                    }
                                    //System.out.println("ave: " + dailyAverageInsolation);
                                    /* This means there is nothing blocking the sunray */
                                    if (blockArray[0] == 0) {

                                    }
                                }

                                dailyAverageInsolation_ += hour_mean_insolation / (double) minutes.length;
                            }

                            //dailyAverageInsolation_ = dailyAverageInsolation_ / ((float)minuteSum / 60.0f);
                            //System.out.println(dailyAverageInsolation_ + " " + n_);
                            //dailyAverageInsolation_ /= (double)n_;


                            dailyAverageInsolation = dailyAverageInsolation + ((dailyAverageInsolation_ / 24.0) - dailyAverageInsolation) / (double) ++n;
                        }
                    }
                    //chm_output[x][y] = (float)dailyAverageInsolation;

                    averageInsol[x] = (float) dailyAverageInsolation;
                    //System.out.println(dailyAverageInsolation);
                    //if(debug)
                    // if(x == 130 && y == 1640) {
                    // System.out.println("AVERAGE_DEBUF: " + dailyAverageInsolation + " val: " + value);
                    //System.exit(1);
                    //}
                    rM.setValue(x, row.getKey(), z, (float) dailyAverageInsolation );
                }
            }

            //System.out.println(row.getKey());

            //System.out.println(y + " / " + this.max );
        }
    }

    public double[] isBlocked(int x, int y, int z_vox, byte z, double resolution, double zenith_angle, double slope, float direction_angle, int hour){
        //(x, row.getKey(), z, value, aR.step, (precomp[0]), slope, (float) (precomp[1]), hour




        double tolerance = 0.0;

        double[] output = new double[]{0.0d, 0.0d};
        double increment_in_meters = resolution / 2.0;

        /* Start from 1 meter */
        double current_distance_from_point = resolution - increment_in_meters;
        /*
        double current_distance_from_point_plus5 = 1.0 - increment_in_meters;
        double current_distance_from_point_plus10 = 1.0 - increment_in_meters;
        double current_distance_from_point_plus15 = 1.0 - increment_in_meters;
        double current_distance_from_point_plus20 = 1.0 - increment_in_meters;
*/
        double center_of_pixel_x = (double)x + 0.5 * resolution;
        double center_of_pixel_y = (double)y + 0.5 * resolution;
        double center_of_pixel_z = (double)z_vox + 0.5 * resolution;

        double outside_x = 10000, outside_y = 10000;

        double x_prev = center_of_pixel_x;
        double y_prev = center_of_pixel_y;

        double x_ = center_of_pixel_x,y_ = center_of_pixel_y, z_ = z_vox;
        /*
        double x_plus5 = center_of_pixel_x,y_plus5 = center_of_pixel_y;
        double x_plus10 = center_of_pixel_x,y_plus10 = center_of_pixel_y;
        double x_plus15 = center_of_pixel_x,y_plus15 = center_of_pixel_y;
        double x_plus20 = center_of_pixel_x,y_plus20 = center_of_pixel_y;
*/
        double start_z = z_vox;

        double sun_vector_z = z_vox;
        /*
        double sun_vector_z_plus5 = z;
        double sun_vector_z_plus10 = z;
        double sun_vector_z_plus15 = z;
        double sun_vector_z_plus20 = z;


         */
        //float[] readFromRaster = new float[1];

        //double sum_obstructed_depth = 0.0;
        //double sum_obstructed_length = 0.0;

        //System.out.println("DIRECTION: " + direction_angle + " hour: " + hour);


        //float cosAngle = (float)cos(direction_angle);
        //float cosAngle = (float)Math.cos((Math.toRadians(direction_angle)));
        float cosAngle = 0;
        //float sinAngle = (float)sin(direction_angle);
        //float sinAngle = (float)Math.sin(Math.toRadians(direction_angle));
        float sinAngle = 0;

        int quadrant = -1;

        int step_x = -2, step_y = -2;

        if(direction_angle < 90.0f){


            step_x = 1;
            step_y = -1;

            quadrant = 1;
            //cosAngle = (float)Math.cos(Math.toRadians(90.0 - direction_angle));
            //sinAngle = (float)Math.sin(Math.toRadians(90.0 - direction_angle));

            cosAngle = cos((90.0f - direction_angle));
            sinAngle = sin((90.0f - direction_angle));


            outside_y = -center_of_pixel_y + sinAngle * 10000;
            outside_x = center_of_pixel_x +cosAngle * 10000;



        }else if(direction_angle < 180.0f){

            step_x = 1;
            step_y = 1;


            quadrant = 2;
            //cosAngle = (float)Math.cos(Math.toRadians(180.0 - direction_angle));
            //sinAngle = (float)Math.sin(Math.toRadians(180.0 - direction_angle));
            cosAngle = cos((180.0f - direction_angle));
            sinAngle = sin((180.0f - direction_angle));


        }else if(direction_angle < 270.0f){

            step_x = -1;
            step_y = 1;

            quadrant = 3;
            //cosAngle = (float)Math.cos(Math.toRadians(270.0 - direction_angle));
            //sinAngle = (float)Math.sin(Math.toRadians(270.0 - direction_angle));

            cosAngle = cos((270.0f - direction_angle));
            sinAngle = sin((270.0f - direction_angle));

        }else if(direction_angle < 360.0f){

            step_x = -1;
            step_y = -1;

            quadrant = 4;
            //cosAngle = (float)Math.cos(Math.toRadians(360.0 - direction_angle));
            //sinAngle = (float)Math.sin(Math.toRadians(360.0 - direction_angle));

            cosAngle = cos((360.0f - direction_angle));
            sinAngle = sin((360.0f - direction_angle));

        }

        int counter = 0;
/*
        float new_z_plus5 = -1;
        float new_z_plus10 = -1;
        float new_z_plus15 = -1;
        float new_z_plus20 = -1;
*/
        //while(sun_vector_z <= start_z + 30){
        while(sun_vector_z < this.rasterMaxValue){

            current_distance_from_point += increment_in_meters;
            //System.out.println("x: " + (int)x + " y: " + (int)y + " dir: "  + direction_angle + " x_: " + (int)x_ + " y_: " + (int)y_ + " c: " + counter++ + " i: " + increment_in_meters + " dis: " + current_distance_from_point + " q: " + quadrant);

           /*
            current_distance_from_point_plus5 += increment_in_meters + 5.0;
            current_distance_from_point_plus10 += increment_in_meters + 10.0;
            current_distance_from_point_plus15 += increment_in_meters + 15.0;
            current_distance_from_point_plus20 += increment_in_meters + 20.0;
*/
            switch (quadrant){

                case 1:

                    y_ = center_of_pixel_y - sinAngle * current_distance_from_point;
                    x_ = center_of_pixel_x + cosAngle * current_distance_from_point;
/*
                    y_plus5 = y_plus5 - sinAngle * current_distance_from_point_plus5;
                    x_plus5 = x_plus5 + cosAngle * current_distance_from_point_plus5;

                    y_plus10 = y_plus10 - sinAngle * current_distance_from_point_plus10;
                    x_plus10 = x_plus10 + cosAngle * current_distance_from_point_plus10;

                    y_plus15 = y_plus15 - sinAngle * current_distance_from_point_plus15;
                    x_plus15 = x_plus15 + cosAngle * current_distance_from_point_plus15;

                    y_plus20 = y_plus20 - sinAngle * current_distance_from_point_plus20;
                    x_plus20 = x_plus20 + cosAngle * current_distance_from_point_plus20;
*/
                    break;

                case 2:
                    y_ = center_of_pixel_y + cosAngle * current_distance_from_point;
                    x_ = center_of_pixel_x + sinAngle * current_distance_from_point;
/*
                    y_plus5 = y_plus5 + sinAngle * current_distance_from_point_plus5;
                    x_plus5 = x_plus5 + cosAngle * current_distance_from_point_plus5;

                    y_plus10 = y_plus10 + sinAngle * current_distance_from_point_plus10;
                    x_plus10 = x_plus10 + cosAngle * current_distance_from_point_plus10;

                    y_plus15 = y_plus15 + sinAngle * current_distance_from_point_plus15;
                    x_plus15 = x_plus15 + cosAngle * current_distance_from_point_plus15;

                    y_plus20 = y_plus20 + sinAngle * current_distance_from_point_plus20;
                    x_plus20 = x_plus20 + cosAngle * current_distance_from_point_plus20;
                    */
                    break;

                case 3:
                    y_ = center_of_pixel_y + sinAngle * current_distance_from_point;
                    x_ = center_of_pixel_x - cosAngle * current_distance_from_point;
/*
                    y_plus5 = y_plus5 + sinAngle * current_distance_from_point_plus5;
                    x_plus5 = x_plus5 - cosAngle * current_distance_from_point_plus5;

                    y_plus10 = y_plus10 + sinAngle * current_distance_from_point_plus10;
                    x_plus10 = x_plus10 - cosAngle * current_distance_from_point_plus10;

                    y_plus15 = y_plus15 + sinAngle * current_distance_from_point_plus15;
                    x_plus15 = x_plus15 - cosAngle * current_distance_from_point_plus15;

                    y_plus20 = y_plus20 + sinAngle * current_distance_from_point_plus20;
                    x_plus20 = x_plus20 - cosAngle * current_distance_from_point_plus20;
*/
                    break;

                case 4:
                    y_ = center_of_pixel_y - cosAngle * current_distance_from_point;
                    x_ = center_of_pixel_x - sinAngle * current_distance_from_point;
/*
                    y_plus5 = y_plus5 - sinAngle * current_distance_from_point_plus5;
                    x_plus5 = x_plus5 - cosAngle * current_distance_from_point_plus5;

                    y_plus10 = y_plus10 - sinAngle * current_distance_from_point_plus10;
                    x_plus10 = x_plus10 - cosAngle * current_distance_from_point_plus10;

                    y_plus15 = y_plus15 - sinAngle * current_distance_from_point_plus15;
                    x_plus15 = x_plus15 - cosAngle * current_distance_from_point_plus15;

                    y_plus20 = y_plus20 - sinAngle * current_distance_from_point_plus20;
                    x_plus20 = x_plus20 - cosAngle * current_distance_from_point_plus20;
*/
                    break;

                default:
                    break;

            }


            z_ = center_of_pixel_z + current_distance_from_point * slope;

            if((int)z_ - (int)center_of_pixel_z <= 0)
                continue;

            if(((int)x_ == (int)x_prev && (int)y_ == (int)y_prev) || ((int)x_ == (int)x && (int)y_ == (int)y && (int)z_ == (int)z)  ){
                continue;
            }



            double z_sun_ray = start_z + current_distance_from_point * slope;


            /*
            double z_sun_ray_plus_5 = start_z + current_distance_from_point_plus5 * slope;
            double z_sun_ray_plus_10 = start_z + current_distance_from_point_plus10 * slope;
            double z_sun_ray_plus_15 = start_z + current_distance_from_point_plus15 * slope;
            double z_sun_ray_plus_20 = start_z + current_distance_from_point_plus20 * slope;
             */
            sun_vector_z = z_sun_ray;


            if(x_ >= this.x_size || x_ < 0 || y_ < 0 || y_ >= this.y_size || z_ < 0 || z_ >= this.z_size) {
                break;
            }

            byte n_points_in_voxel = chm_3d[(int)y_][(int)x_][(int)z_];
/*
            if(x_plus5 < this.x_size && x_plus5 >= 0 && y_plus5 >= 0 && y_plus5 < this.y_size)
                new_z_plus5 = chm[(int)x_plus5][(int)y_plus5];

            if(x_plus10 < this.x_size && x_plus10 >= 0 && y_plus10 >= 0 && y_plus10 < this.y_size)
                new_z_plus10 = chm[(int)x_plus10][(int)y_plus10];

            if(x_plus15 < this.x_size && x_plus15 >= 0 && y_plus15 >= 0 && y_plus15 < this.y_size)
                new_z_plus15 = chm[(int)x_plus15][(int)y_plus15];

            if(x_plus20 < this.x_size && x_plus20 >= 0 && y_plus20 >= 0 && y_plus20 < this.y_size)
                new_z_plus20 = chm[(int)x_plus20][(int)y_plus20];


 */
            //if(x == 320 && y == 1700)
            //  System.out.println("sun: " + z_sun_ray + " new_z: " + new_z + " " + Arrays.toString(output) + " dist: " + current_distance_from_point + " ang: " + zenith_angle + " slope: " + slope);

            if(n_points_in_voxel > (1)){

                //System.out.println(counter);
                output[0] += 20;
                output[1] += increment_in_meters;
                return output;

            }

            x_prev = x_;
            y_prev = y_;
            /*
            //x_ = x_ + current_distance_from_point * cos(direction_angle);
            //y_ = y_ + current_distance_from_point * sin(direction_angle);

            x_ = x_ + current_distance_from_point * cosAngle;
            y_ = y_ + current_distance_from_point * sinAngle;

            if((int)x_ == (int)x_prev && (int)y_ == (int)y_prev){
                continue;
            }

            double z_sun_ray = start_z + current_distance_from_point * slope;

            sun_vector_z = z_sun_ray;

            if(x_ >= this.x_size || x_ < 0 || y_ < 0 || y_ >= this.y_size)
                break;

            double new_z = chm[(int)x_][(int)y_];

            //if(x == 320 && y == 1700)
              //  System.out.println("sun: " + z_sun_ray + " new_z: " + new_z + " " + Arrays.toString(output) + " dist: " + current_distance_from_point + " ang: " + zenith_angle + " slope: " + slope);
            if(new_z > z_sun_ray){

                output[0] += (new_z - z_sun_ray);
                output[1] += increment_in_meters;

            }

            x_prev = x_;
            y_prev = y_;

             */

            //counter++;

        }
        //System.out.println(counter);


        return output;

    }

    public double[] isBlocked_ray_trace(int x, int y, int z_vox, byte z, double resolution, double zenith_angle, double slope, float direction_angle, int hour, float chm_max){

        int current_x = x;
        int current_y = y;
        int current_z = z_vox;

        double outside_x = 10000, outside_y = 10000, outside_z = 10000;

        int step_z;

        double[] output = new double[]{0.0d, 0.0d};
        double increment_in_meters = resolution / 2.0;

        double center_of_pixel_x = (double)x + 0.5 * resolution;
        double center_of_pixel_y = (double)y + 0.5 * resolution;
        double center_of_pixel_z = (double)z_vox + 0.5 * resolution;

        if(slope > 0) {
            step_z = 1;
            outside_z = center_of_pixel_z + sin(90.0f - (float)zenith_angle) * 10000;
        }
        else {
            outside_z = center_of_pixel_z - sin(90.0f - (float)zenith_angle) * 10000;
            step_z = -1;
        }

        float cosAngle = 0;
        float sinAngle = 0;
        float sinAngle2 = 0;

        sinAngle2 = sin((Math.abs((float)zenith_angle)));

        int step_x = -2, step_y = -2;
        byte n_points_in_voxel = 0;

        if(direction_angle < 90.0f){

            step_x = 1;
            step_y = -1;

            cosAngle = cos((90.0f - direction_angle));
            sinAngle = sin((90.0f - direction_angle));

            outside_y = -center_of_pixel_y + sinAngle * 10000;
            outside_x = center_of_pixel_x + cosAngle * 10000;

            float t_d_x = (float) (resolution / cosAngle);
            float t_d_y = (float) (resolution / sinAngle);

            float t_d_z = (float) (resolution / sinAngle2);

            float[] y_intersect = lineIntersect((float) center_of_pixel_x, -(float) center_of_pixel_y, (float) outside_x, (float) outside_y, (float) center_of_pixel_x - 10000, -(float) center_of_pixel_y + (float)resolution/2.0f, (float) center_of_pixel_x + 10000, -(float) center_of_pixel_y + (float)resolution/2.0f);
            float[] x_intersect = lineIntersect((float) center_of_pixel_x, -(float) center_of_pixel_y, (float) outside_x, (float) outside_y, (float) center_of_pixel_x + (float)resolution/2.0f, -(float) center_of_pixel_y + 10000, (float) center_of_pixel_x + (float)resolution/2.0f, -(float) center_of_pixel_y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect((float) center_of_pixel_x, (float) center_of_pixel_z, (float) outside_x, (float) outside_z, x - 10000,  (float) center_of_pixel_z + (float)resolution/2.0f, x + 10000,  (float) center_of_pixel_z + (float)resolution/2.0f);
            }else
                z_intersect = lineIntersect((float) center_of_pixel_x, (float) center_of_pixel_z, (float) outside_x, (float) outside_z, x - 10000,  (float) center_of_pixel_z - (float)resolution/2.0f, x + 10000,  (float) center_of_pixel_z - (float)resolution/2.0f);

            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], (float) center_of_pixel_x, -(float) center_of_pixel_y);
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], (float) center_of_pixel_x, -(float) center_of_pixel_y);
            float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], (float) center_of_pixel_x, (float) center_of_pixel_z);


            boolean breakki = false;

            /* RAY TRACE ITERATIONS */
            while (true) {

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }

                //System.out.println("c_x: " + current_x + " c_y: " + current_y + " c_z: " + current_z + " x: " + x + " y:" + y + " z: " + z_vox + " a_d: " + direction_angle + " a_z: " + zenith_angle + " chm: " + chm_value);
                //System.out.println("t_max_x: " + t_max_x + " t_max_y: " + t_max_y);

                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    //breakki = true;
                    break;

                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    break;
                }

                if(current_z != z_vox)
                    n_points_in_voxel = chm_3d[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > (1)){

                    output[0] += 20;
                    output[1] += increment_in_meters;
                    return output;

                }
            }

        }

        else if(direction_angle < 180.0f){

            step_x = 1;
            step_y = 1;

            cosAngle = cos((180.0f - direction_angle));
            sinAngle = sin((180.0f - direction_angle));


            outside_y = -center_of_pixel_y - cosAngle * 10000;
            outside_x = center_of_pixel_x + sinAngle * 10000;


            float t_d_x = (float) (resolution / sinAngle);
            float t_d_y = (float) (resolution / cosAngle);
            float t_d_z = (float) (resolution / sinAngle2);

            float[] y_intersect = lineIntersect((float) center_of_pixel_x, -(float) center_of_pixel_y, (float) outside_x, (float) outside_y, x - 10000, -y - (float)resolution/2.0f, x + 10000, -y - (float)resolution/2.0f);
            float[] x_intersect = lineIntersect((float) center_of_pixel_x, -(float) center_of_pixel_y, (float) outside_x, (float) outside_y, x + (float)resolution/2.0f, -y + 10000, x + (float)resolution/2.0f, -y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect((float) center_of_pixel_x, (float) center_of_pixel_z, (float) outside_x, (float) outside_z, x - 10000,  (float) center_of_pixel_z + (float)resolution/2.0f, x + 10000,  (float) center_of_pixel_z + (float)resolution/2.0f);
                //System.out.println("SHOULD BE HERE!");
            }else
                z_intersect = lineIntersect((float) center_of_pixel_x, (float) center_of_pixel_z, (float) outside_x, (float) outside_z, x - 10000,  (float) center_of_pixel_z - (float)resolution/2.0f, x + 10000,  (float) center_of_pixel_z - (float)resolution/2.0f);


            //System.out.println(Arrays.toString(y_intersect));
            //System.out.println(Arrays.toString(x_intersect));
            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], (float) center_of_pixel_x, -(float) center_of_pixel_y);
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], (float) center_of_pixel_x, -(float) center_of_pixel_y);
            float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], (float) center_of_pixel_x, (float) center_of_pixel_z);


            boolean breakki = false;

            while (true) {

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }

                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    //breakki = true;
                    break;

                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    break;
                }

                if(current_z != z_vox)
                    n_points_in_voxel = chm_3d[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > (1)){

                    //System.out.println(counter);
                    output[0] += 20;
                    output[1] += increment_in_meters;
                    return output;

                }

            }



        }

        else if(direction_angle < 270.0f){

            step_x = -1;
            step_y = 1;

            //cosAngle = (float)Math.cos(Math.toRadians(270.0 - direction_angle));
            //sinAngle = (float)Math.sin(Math.toRadians(270.0 - direction_angle));

            cosAngle = cos((270.0f - direction_angle));
            sinAngle = sin((270.0f - direction_angle));

            outside_y = -center_of_pixel_y - sinAngle * 10000;
            outside_x = center_of_pixel_x - cosAngle * 10000;

            float t_d_x = (float) (resolution / cosAngle);
            float t_d_y = (float) (resolution / sinAngle);
            float t_d_z = (float) (resolution / sinAngle2);

            float[] y_intersect = lineIntersect((float) center_of_pixel_x, -(float) center_of_pixel_y, (float) outside_x, (float) outside_y, x - 10000, -y - (float)resolution/2.0f, x + 10000, -y - (float)resolution/2.0f);
            float[] x_intersect = lineIntersect((float) center_of_pixel_x, -(float) center_of_pixel_y, (float) outside_x, (float) outside_y, x - (float)resolution/2.0f, -y + 10000, x - (float)resolution/2.0f, -y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect((float) center_of_pixel_x, (float) center_of_pixel_z, (float) outside_x, (float) outside_z, x - 10000,  (float) center_of_pixel_z + (float)resolution/2.0f, x + 10000,  (float) center_of_pixel_z + (float)resolution/2.0f);
                //System.out.println("SHOULD BE HERE!");
            }else
                z_intersect = lineIntersect((float) center_of_pixel_x, (float) center_of_pixel_z, (float) outside_x, (float) outside_z, x - 10000,  (float) center_of_pixel_z - (float)resolution/2.0f, x + 10000,  (float) center_of_pixel_z - (float)resolution/2.0f);


            //System.out.println(Arrays.toString(y_intersect));
            //System.out.println(Arrays.toString(x_intersect));
            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], (float) center_of_pixel_x, -(float) center_of_pixel_y);
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], (float) center_of_pixel_x, -(float) center_of_pixel_y);
            float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], (float) center_of_pixel_x, (float) center_of_pixel_z);


            boolean breakki = false;

            while (true) {

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }

                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    //breakki = true;
                    break;

                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    break;
                }

                if(current_z != z_vox)
                    n_points_in_voxel = chm_3d[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > (1)){

                    //System.out.println(counter);
                    output[0] += 20;
                    output[1] += increment_in_meters;
                    return output;

                }

            }



        }

        else if(direction_angle < 360.0f){

            step_x = -1;
            step_y = -1;

            //cosAngle = (float)Math.cos(Math.toRadians(360.0 - direction_angle));
            //sinAngle = (float)Math.sin(Math.toRadians(360.0 - direction_angle));

            cosAngle = cos((360.0f - direction_angle));
            sinAngle = sin((360.0f - direction_angle));

            outside_y = -center_of_pixel_y + cosAngle * 10000;
            outside_x = center_of_pixel_x - sinAngle * 10000;


            float t_d_x = (float) (resolution / sinAngle);
            float t_d_y = (float) (resolution / cosAngle);
            float t_d_z = (float) (resolution / sinAngle2);

            float[] y_intersect = lineIntersect((float) center_of_pixel_x, -(float) center_of_pixel_y, (float) outside_x, (float) outside_y, x - 10000, -y + (float)resolution/2.0f, x + 10000, -y +     (float)resolution/2.0f);
            float[] x_intersect = lineIntersect((float) center_of_pixel_x, -(float) center_of_pixel_y, (float) outside_x, (float) outside_y, x - (float)resolution/2.0f, -y + 10000, x - (float)resolution/2.0f, -y - 10000);


            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect((float) center_of_pixel_x, (float) center_of_pixel_z, (float) outside_x, (float) outside_z, x - 10000,  (float) center_of_pixel_z + (float)resolution/2.0f, x + 10000,  (float) center_of_pixel_z + (float)resolution/2.0f);
                //System.out.println("SHOULD BE HERE!");
            }else
                z_intersect = lineIntersect((float) center_of_pixel_x, (float) center_of_pixel_z, (float) outside_x, (float) outside_z, x - 10000,  (float) center_of_pixel_z - (float)resolution/2.0f, x + 10000,  (float) center_of_pixel_z - (float)resolution/2.0f);


            //System.out.println(Arrays.toString(y_intersect));
            //System.out.println(Arrays.toString(x_intersect));
            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], (float) center_of_pixel_x, -(float) center_of_pixel_y);
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], (float) center_of_pixel_x, -(float) center_of_pixel_y);
            float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], (float) center_of_pixel_x, (float) center_of_pixel_z);


            boolean breakki = false;

            while (true) {

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }

                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    //breakki = true;
                    break;

                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    break;
                }

                if(current_z != z_vox)
                    n_points_in_voxel = chm_3d[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > (1)){

                    //System.out.println(counter);
                    output[0] += 20;
                    output[1] += increment_in_meters;
                    return output;

                }

            }



        }

        return output;

    }


    public static float lineIntersect_x(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denom == 0.0) { // Lines are parallel.
            return Float.NaN;
        }
        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3))/denom;
        double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3))/denom;
        if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f) {
            // Get the intersection point.
            return (float) (x1 + ua*(x2 - x1));
        }

        return Float.NaN;
    }

    public float euclideanDistance(float x1, double y1, float x2, float y2){


        return (float)Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

    }


    public static float[] lineIntersect(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denom == 0.0) { // Lines are parallel.
            return null;
        }
        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3))/denom;
        double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3))/denom;
        if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f) {
            // Get the intersection point.
            return new float[]{(float) (x1 + ua*(x2 - x1)),  (float) (y1 + ua*(y2 - y1))};
        }

        return null;
    }


    static final int precision = 100; // gradations per degree, adjust to suit

    static final int modulus = 360*precision;
    static final float[] sin = new float[modulus]; // lookup table
    static final float[] cos = new float[modulus]; // lookup table
    static {
        // a static initializer fills the table
        // in this implementation, units are in degrees
        for (int i = 0; i<sin.length; i++) {
            sin[i]=(float)Math.sin((i*Math.PI)/(precision*180));
        }

        for (int i = 0; i<cos.length; i++) {
            cos[i]=(float)Math.cos((i*Math.PI)/(precision*180));
        }

    }
    // Private function for table lookup
    private static float sinLookup(int a) {
        return a>=0 ? sin[a%(modulus)] : -sin[-a%(modulus)];
    }
    // Private function for table lookup
    private static float cosLookup(int a) {
        return a>=0 ? cos[a%(modulus)] : -cos[-a%(modulus)];
    }

    // These are your working functions:
    public static float sin(float a) {
        return sinLookup((int)(a * precision + 0.5f));
    }
    public static float cos(float a) {
        return sinLookup((int)((a+90f) * precision + 0.5f));
    }


    //HOUR TO HOUR ANGLE
    // given an hour (midnight = 0, 12 = midday), calculate the hour angle at 15 degrees per hour.
    // returns radians rotated at this hour

    public double HourToHourAngle(double hourFromMidnight){

        double hourAngle = Math.toRadians(15*(hourFromMidnight - 12));
        return hourAngle;

    }

    //DETERMINE SOLAR DECLILANATION
    // given a day of the year, determine the solar declination.

    //By Jack Williams, n5736153
    public double SolarDeclinaton(int dayOfYear){

        double declination = Math.toRadians(23.45) * (Math.sin((Math.toRadians(360)/365)*(dayOfYear)));
        return declination;

    }
}
