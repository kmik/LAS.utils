package tools;

import LASio.LASReader;
import err.toolException;
import net.e175.klaus.solarpositioning.AzimuthZenithAngle;
import net.e175.klaus.solarpositioning.PSA;
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
import utils.LevenbergMarquardt;
import utils.argumentReader;
import utils.rasterManipulator;


import java.util.*;

import static net.e175.klaus.solarpositioning.PSA.calculateSolarPosition;

public class las2solar_photogrammetry {

    private static int solarradiation = 1366;

    Band chm_values;
    float[][] chm_values_f;
    float[][] chm_output_f;


    public las2solar_photogrammetry(String chm_name, argumentReader aR){

        ogr.RegisterAll(); //Registering all the formats..

        gdal.AllRegister();


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

        float[][][][][][] precomputed = new float[x_res][y_res][12][32][24][2];


        SpatialReference src = new SpatialReference();
        SpatialReference dst = new SpatialReference();

        dst.ImportFromProj4("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
        src.ImportFromProj4("++proj=utm +zone=35 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs ");

        CoordinateTransformation ct = new CoordinateTransformation(src, dst);

        double[] gt = chm.GetGeoTransform();

        GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-7 * 60 * 60 * 1000, "LST"));

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

                time.set(2018, month, day, 12, 00, 00); // 17 October 2003, 12:30:30 LST-07:00
                Calendar[] sunriseSunset = ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(time, point_transformed[0], point_transformed[1]);
                sunriseAndSunset.add(new int[]{sunriseSunset[0].getTime().getHours(), sunriseSunset[1].getTime().getHours()});

            }
        }

        double[] blockArray = new double[]{0.0,0.0};

        rasterManipulator rM = new rasterManipulator(band2);

        Thread[] threads = new Thread[aR.cores];
        int n_funk_per_thread = (int)Math.ceil((double)y_size / (double)aR.cores);




        for(int x = 0; x < x_res; x++){
            for(int y = 0; y < y_res; y++){

                int precompute_x = (int)((double)steppi * (double)x + (double)steppi / 2.0); // x_size / 2;
                int precompute_y = (int)((double)steppi * (double)y + (double)steppi / 2.0); // x_size / 2;

                point[0] = gt[0] + precompute_x * gt[1] + precompute_y * gt[2];
                point[1] = gt[3] + precompute_x * gt[4] + precompute_y * gt[5];
                point[2] = value;
                point_transformed = ct.TransformPoint(point[0], point[1]);


                for(int month : calendar_month) {
                    for (int day = 0; day <= n_date_in_month[month]; day += 1) {

                        int sunriseSunsetcounter = 0;

                        int sunrise = sunriseAndSunset.get(sunriseSunsetcounter)[0];
                        int sunset = sunriseAndSunset.get(sunriseSunsetcounter++)[1];

                        for (int hour = sunrise; hour <= sunset; hour += 1) {

                            time.set(2020, month, day, hour, 00, 00);

                            AzimuthZenithAngle result = calculateSolarPosition(time, point_transformed[0], point_transformed[1]);

                            precomputed[x][y][month][day][hour][0] = (float)result.getZenithAngle();
                            precomputed[x][y][month][day][hour][1] = (float)result.getAzimuth();

                        }
                    }
                }

            }
        }

        long start = System.currentTimeMillis();



        for (int i = 0; i < aR.cores; i++) {

            int mini = i * n_funk_per_thread;
            int maxi = Math.min(y_size, mini + n_funk_per_thread);

            threads[i] = (new solarParallel(mini, maxi, x_size,y_size, rM, sunriseAndSunset, gt, ct, aR, chm_values_f, chm_output_f, precomputed, steppi));
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

                            AzimuthZenithAngle result = calculateSolarPosition(time, point_transformed[0], point_transformed[1]);

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

        while(sun_vector_z <= start_z + 30){

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

        float[][] output = new float[x_][y_];

        float[] raster_read = new float[x_];

        for (int y = 0; y < y_; y++) {

            chm_values.ReadRaster(0, y, x_, 1, raster_read);
            long start = System.currentTimeMillis();

            for (int x = 0; x <x_; x++) {
                output[x][y] = raster_read[x];
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

    float[][][][][][] precomputed;
    int precomputed_resolution;

    public solarParallel(int min, int max, int x_size, int y_size, rasterManipulator rM, ArrayList<int[]> sunriseAndSunset,
                         double[] gt, CoordinateTransformation ct, argumentReader aR, float[][] chm, float[][] chm_output,
                         float[][][][][][] precomputed, int precomputed_resolution) {

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

        int solarradiation = 1366;

        for (int y = min; y < this.max; y++) {

            for (int x = 0; x < x_size; x++) {

                int precomputed_x = (int)Math.floor((double)x / (double)precomputed_resolution);
                int precomputed_y = (int)Math.floor((double)y / (double)precomputed_resolution);

                value = chm[x][y];

                if(value <= 0)
                    continue;

                point[0] = gt[0] + x * gt[1] + y * gt[2];
                point[1] = gt[3] + x * gt[4] + y * gt[5];
                point[2] = value;

                point_transformed = ct.TransformPoint(point[0], point[1]);

                double stupid_irradiance_sum = 0.0;
                int sunriseSunsetcounter = 0;

                double dailyAverageInsolation = 0;

                for(int month : calendar_month){

                    for(int day = 1; day <= n_date_in_month[month]; day += 1){

                        int sunrise = sunriseAndSunset.get(sunriseSunsetcounter)[0];
                        int sunset = sunriseAndSunset.get(sunriseSunsetcounter++)[1];

                        for(int hour = sunrise; hour <= sunset; hour += 1){

                            //long start = System.nanoTime();
                            //time.set(2020, month, day, hour, 00, 00);

                            //AzimuthZenithAngle result = PSA.calculateSolarPosition(time, point_transformed[0], point_transformed[1]);
                            float[] precomp = precomputed[precomputed_x][precomputed_y][month][day][hour];

                            //precomp[0] = (float)result.getZenithAngle();
                            //precomp[1] = (float)result.getAzimuth();

                            /*
                            Only account for when sun is visible, i.e. time between sunrise and sunset
                             */
                            //if(result.getZenithAngle() > 90.0)
                            if(precomp[0] > 90.0f)
                                continue;

                            //double insolation = (double)solarradiation * Math.cos(Math.toRadians(result.getZenithAngle()));
                            //double insolation = (double)solarradiation * Math.cos(Math.toRadians(precomp[0]));
                            double insolation = (double)solarradiation * Math.cos(Math.toRadians(precomp[0]));

                            //System.out.println("insolation: " + insolation + " w/m2 " + month + " " + result.getZenithAngle());

                            //double[] blockArray = isBlocked(x, y, value, aR.step,(result.getZenithAngle()), Math.tan(Math.toRadians(90.0-result.getZenithAngle())), (float) (result.getAzimuth()));
                            double[] blockArray = isBlocked(x, y, value, aR.step,(precomp[0]), Math.tan(Math.toRadians(90.0-precomp[0])), (float) (precomp[1]));

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
                //chm_output[x][y] = (float)dailyAverageInsolation;
                rM.setValue(x,y,(float)dailyAverageInsolation);

            }
        }
    }

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

        float cosAngle = (float)cos(direction_angle);
        float sinAngle = (float)sin(direction_angle);

        while(sun_vector_z <= start_z + 30){

            current_distance_from_point += increment_in_meters;

            /** These are fast cos and fast sin. Not super accurate, but will do for now */
            //x_ = x_ + current_distance_from_point * cos(direction_angle);
            //y_ = y_ + current_distance_from_point * sin(direction_angle);

            x_ = x_ + current_distance_from_point * cosAngle;
            y_ = y_ + current_distance_from_point * sinAngle;

            if(x_ == x_prev && y_ == y_prev){
                continue;
            }

            double z_sun_ray = start_z + current_distance_from_point * slope;

            sun_vector_z = z_sun_ray;

            if(x_ >= this.x_size || x_ < 0 || y_ < 0 || y_ >= this.y_size)
                break;

            double new_z = chm[(int)x_][(int)y_];

            if(new_z > z_sun_ray){

                output[0] += (new_z - z_sun_ray);
                output[1] += increment_in_meters;

            }

            x_prev = x_;
            y_prev = y_;

        }

        return output;

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
