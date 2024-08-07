package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import ch.qos.logback.core.encoder.EchoEncoder;
import err.toolException;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NonBlockingGenericDialog;
import ij.plugin.ChannelSplitter;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
//import javafx.util.Pair;
import javafx.geometry.Point3D;
import jdk.nashorn.internal.ir.Block;
import net.e175.klaus.solarpositioning.*;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.util.FastMath;
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
import quickhull3d.Point3d;
import quickhull3d.Vector3d;
import utils.*;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.Pair;
import static ij.IJ.createImage;
import static net.e175.klaus.solarpositioning.PSA.calculateSolarPosition;
import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Plane;

public class las2solar_photogrammetry {

    amapVox aV;
    private static int solarradiation = 1366;

    private static double xsigma=0.5, ysigma=0.5, zsigma=0.5;

    Band chm_values;
    float[][] chm_values_f;
    byte[][][] chm_values_f_3d;
    float[][] chm_output_f;

    LASReader pointCloud;

    short[][][] chm_values_mean_x;
    short[][][] chm_values_mean_y;
    short[][][] chm_values_mean_z;

    float rasterMaxValue = 0.0f;

    public las2solar_photogrammetry(String chm_name, argumentReader aR, LASReader pointCloud, boolean d3) throws Exception{


        aR.add_extra_bytes(6, "normal", "Surface normal");
        aR.add_extra_bytes(6, "flatness", "Surface flatness");
        aR.add_extra_bytes(6, "insolation", "Solar insolation");


        boolean amapVox_mode = false;

        if(aR.amapVoxFile != null){

            aV = new amapVox(aR.amapVoxFile);
            aV.readHeader();
            aV.readVoxels();
            amapVox_mode = true;

        }

        this.pointCloud = pointCloud;

        //System.out.println(this.pointCloud.getMaxZ());

        LasPoint tempPoint = new LasPoint();

        int thread_n = aR.pfac.addReadThread(pointCloud);


        int number_of_last_returns = 0;
        int number_of_all_returns = 0;

        int raster_z_size = (int)Math.ceil((pointCloud.getMaxZ() - pointCloud.getMinZ()) / aR.step);

        if(amapVox_mode){
            raster_z_size = (int)Math.ceil((aV.max_z - aV.min_z) / aR.step);
        }
        Dataset chm = gdal.Open(chm_name);
        int[][] maxValueInChm;

        if(!amapVox_mode) {
            this.chm_values_f_3d = new byte[chm.getRasterYSize()][chm.getRasterXSize()][raster_z_size];
            this.chm_values_mean_y = new short[chm.getRasterYSize()][chm.getRasterXSize()][raster_z_size];
            this.chm_values_mean_x = new short[chm.getRasterYSize()][chm.getRasterXSize()][raster_z_size];
            this.chm_values_mean_z = new short[chm.getRasterYSize()][chm.getRasterXSize()][raster_z_size];
            maxValueInChm = new int[chm.getRasterYSize()][chm.getRasterXSize()];
        }else{
            this.chm_values_f_3d = new byte[aV.y_dim][aV.x_dim][raster_z_size];
            this.chm_values_mean_y = new short[aV.y_dim][aV.x_dim][raster_z_size];
            this.chm_values_mean_x = new short[aV.y_dim][aV.x_dim][raster_z_size];
            this.chm_values_mean_z = new short[aV.y_dim][aV.x_dim][raster_z_size];
            maxValueInChm = new int[aV.y_dim][aV.x_dim];
        }


        double this_min_x, this_min_y, this_min_z;
        double this_max_x, this_max_y, this_max_z;

        int this_x_size, this_y_size, this_z_size;

        if(!amapVox_mode){
            this_min_x = pointCloud.getMinX();
            this_max_x = pointCloud.getMaxX();

            this_min_y = pointCloud.getMinY();
            this_max_y = pointCloud.getMaxY();

            this_min_z = pointCloud.getMinZ();
            this_max_z = pointCloud.getMaxZ();

            this_x_size = chm.getRasterXSize();
            this_y_size = chm.getRasterYSize();
            this_z_size = raster_z_size;

        }else{

            this_min_x = aV.min_x;
            this_max_x = aV.max_x;

            this_min_y = aV.min_y;
            this_max_y = aV.max_y;

            this_min_z = aV.min_z;
            this_max_z = aV.max_z;

            this_x_size = aV.x_dim;
            this_y_size = aV.y_dim;
            this_z_size = aV.z_dim;


        }


        VoxelNeighborhood[][][] vox_ = new VoxelNeighborhood[this_x_size][this_y_size][this_z_size];

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 20000) {

            int maxi = (int) Math.min(20000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if(amapVox_mode){

                    if(!aV.checkPoint(tempPoint))
                        continue;

                }

                if(tempPoint.z > this.rasterMaxValue)
                    this.rasterMaxValue = (float)tempPoint.z;

                int x = Math.min((int)((tempPoint.x - this_min_x) / aR.step), this_x_size-1);
                int y = Math.min((int)((this_max_y - tempPoint.y) / aR.step), this_y_size-1);
                int z = Math.min((int)((tempPoint.z - this_min_z) / aR.step), raster_z_size-1);




                if(vox_[x][y][z] == null){
                    vox_[x][y][z] = new VoxelNeighborhood(x, y, z);
                }

                vox_[x][y][z].addObservable();


                if(false)
                for (int x_ = -1; x_ < 2; x_++)
                    for (int y_ = -1; y_ < 2; y_++)
                        for (int z_ = -1; z_ < 2; z_++) {


                            if( (x+x_) >= 0 && (x+x_) < this_x_size &&
                                    (y+y_) >= 0 && (y+y_) < this_y_size &&
                                    (z+z_) >= 0 && (z+z_) < this_z_size) {
                                if (vox_[x + x_][y + y_][z + z_] == null) {   // vox_[x + x_][y + y_][z + z_].capacity > 0 &&
                                    vox_[x + x_][y + y_][z + z_] = new VoxelNeighborhood(x + x_, y + y_, z + z_);
                                    vox_[x + x_][y + y_][z + z_].addObservable();
                                } else {
                                    //System.out.println("HERE!");
                                    vox_[x + x_][y + y_][z + z_].addObservable();
                                }
                            }

                        }

                //System.out.println(vox_[x][y][z].capacity);

                float fraction_x = (float)((tempPoint.x - this_min_x) / aR.step) - (float)x;
                float fraction_y = (float)((this_max_y - tempPoint.y) / aR.step) - (float)y;
                float fraction_z = (float)((tempPoint.z - this_min_z) / aR.step) - (float)z;

                short frac_x = (short)(int)((fraction_x / aR.step) * 1000);
                short frac_y = (short)(int)((1.0f - fraction_y / aR.step) * 1000);
                short frac_z = (short)(int)((fraction_z / aR.step) * 1000);

                //System.out.println(frac_x + " " + frac_y + " " + frac_z);
                //System.out.println(x + " " + frac_x + " " + tempPoint.x + " " + aR.step*x);

                if(z > maxValueInChm[y][x]){
                    maxValueInChm[y][x] = z;
                }

                if(chm_values_f_3d[y][x][z] < 127) {
                    chm_values_f_3d[y][x][z]++;
                    //dailyAverageInsolation = dailyAverageInsolation + ((dailyAverageInsolation_ / 24.0) - dailyAverageInsolation)/(double)++n;
                    /*New average = old average * (n-1)/n + new value /n */
                    //chm_values_mean_x[y][x][z] = (short)(chm_values_mean_x[y][x][z] * ((short)chm_values_f_3d[y][x][z]-1)/(short)chm_values_f_3d[y][x][z] + frac_x / (short)chm_values_f_3d[y][x][z]);
                    chm_values_mean_x[y][x][z] = (short)(chm_values_mean_x[y][x][z] + (frac_x - chm_values_mean_x[y][x][z]) / (double) chm_values_f_3d[y][x][z]);// ((short)chm_values_f_3d[y][x][z]-1)/(short)chm_values_f_3d[y][x][z] + frac_x / (short)chm_values_f_3d[y][x][z]);
                    //chm_values_mean_y[y][x][z] = (short)(chm_values_mean_y[y][x][z] * ((short)chm_values_f_3d[y][x][z]-1)/(short)chm_values_f_3d[y][x][z] + frac_y / (short)chm_values_f_3d[y][x][z]);
                    chm_values_mean_y[y][x][z] = (short)(chm_values_mean_y[y][x][z] + (frac_y - chm_values_mean_y[y][x][z]) / (double) chm_values_f_3d[y][x][z]);
                    //chm_values_mean_z[y][x][z] = (short)(chm_values_mean_z[y][x][z] * ((short)chm_values_f_3d[y][x][z]-1)/(short)chm_values_f_3d[y][x][z] + frac_z / (short)chm_values_f_3d[y][x][z]);
                    chm_values_mean_z[y][x][z] = (short)(chm_values_mean_z[y][x][z] + (frac_z - chm_values_mean_z[y][x][z]) / (double) chm_values_f_3d[y][x][z]);


                }

            }
        }

        //System.out.println("LOOP 1 COMPLETE!");

        if(true)
            for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

                int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

                aR.pfac.prepareBuffer(thread_n, i, maxi);

                for (int j = 0; j < maxi; j++) {

                    pointCloud.readFromBuffer(tempPoint);

                    if(amapVox_mode){

                        if(!aV.checkPoint(tempPoint))
                            continue;

                    }



                    int x = (int)((tempPoint.x - this_min_x) / aR.step);
                    int y = (int)((this_max_y - tempPoint.y) / aR.step);
                    int z = (int)((tempPoint.z - this_min_z) / aR.step);

                    if(x < 0 || x >= this_x_size || y < 0 || y >= this_y_size || z < 0 || z >= this_z_size)
                        continue;

                    vox_[x][y][z].addPoint(tempPoint.x, tempPoint.y, tempPoint.z, i+j);

                    if(tempPoint.classification == 4){

                        vox_[x][y][z].n_solid_points++;

                    }

                    if(false)
                    for (int x_ = -1; x_ < 2; x_++)
                        for (int y_ = -1; y_ < 2; y_++)
                            for (int z_ = -1; z_ < 2; z_++) {


                                if( (x+x_) >= 0 && (x+x_) < this_x_size &&
                                        (y+y_) >= 0 && (y+y_) < this_y_size &&
                                        (z+z_) >= 0 && (z+z_) < this_z_size) {
                                    if (vox_[x + x_][y + y_][z + z_] != null) {   // vox_[x + x_][y + y_][z + z_].capacity > 0 &&
                                        //vox_[x + x_][y + y_][z + z_] = new VoxelNeighborhood(x + x_, y + y_, z + z_);
                                        vox_[x + x_][y + y_][z + z_].addPoint(tempPoint.x, tempPoint.y, tempPoint.z, i+j);
                                    } else {
                                        //System.out.println("HERE!");
                                        //vox_[x + x_][y + y_][z + z_].addObservable();
                                    }
                                }

                            }

                    //if(chm_values_f_3d[y][x][z] < 100)
                    //    chm_values_f_3d[y][x][z]++;

                }
            }


        //System.out.println("ALL GOOD!");
        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();
        //System.out.println("max cache: " + gdal.GetCacheMax());
        //gdal.SetCacheMax(413375897 * 4);
        //gdal.SetCacheMax((int)(aR.gdal_cache_gb * 1073741824));


        //Dataset dataset = null;
        Driver driver = null;
        driver = gdal.GetDriverByName("GTiff");
        driver.Register();
        //Band band2=null;

        //dataset = driver.Create("testi.tif", chm.GetRasterXSize(), chm.GetRasterYSize(), 1, gdalconst.GDT_Float32);
        //dataset.SetGeoTransform(chm.GetGeoTransform());
        //dataset.SetProjection(chm.GetProjection());

        //System.out.println(chm.GetProjection());
        //System.exit(1);
        //band2 = dataset.GetRasterBand(1);    // writable band


        int steppi = 10;

        int x_res = (int)Math.ceil((double)this_x_size / (double)steppi);
        int y_res = (int)Math.ceil((double)this_y_size / (double)steppi);

        int[] minutes = new int[]{0};        //float[][][][][][] precomputed = new float[x_res][y_res][12][32][24][2];
        float[][][][][][][] precomputed = new float[x_res][y_res][12][32][24][minutes.length][2];

        SpatialReference src = new SpatialReference();
        SpatialReference dst = new SpatialReference();

        dst.ImportFromProj4("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
        src.ImportFromProj4("+proj=utm +zone=35 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs");

        CoordinateTransformation ct = new CoordinateTransformation(src, dst);

        double[] gt = chm.GetGeoTransform();

        //GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-7 * 60 * 60 * 1000, "LST"));
        GregorianCalendar time = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));

        //System.out.println(time.getTimeZone());

        this.chm_values = chm.GetRasterBand(1);


        chm_values_f = chm_to_array(chm_values);
        int x_ = this_x_size;
        int y_ = this_y_size;
        chm_output_f = new float[1][1];



        int[] calendar_month = new int[]{Calendar.JUNE, Calendar.JULY, Calendar.AUGUST};

        int[] n_date_in_month = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30 , 31};


        int x_size = this_x_size;
        int y_size = this_y_size;

        float[] raster_read = new float[x_size];
        float value = 0;

        double[] point = new double[3];
        //double[] point_transformed = new double[2];

        /*
        Prepare sunrise and sunsets
         */
        ArrayList<int[]> sunriseAndSunset = new ArrayList<>();


        double[] blockArray = new double[]{0.0,0.0};

        double angleThreshold = aR.angle;

        if(angleThreshold == -999)
            angleThreshold = 45;

        //System.out.println(angleThreshold);
        //System.exit(1);


        solar3dManipulator rM = new solar3dManipulator(this_x_size, this_y_size, raster_z_size, new float[this_y_size][this_x_size][raster_z_size], aR.step, maxValueInChm, angleThreshold);
        rM.setContains_points(chm_values_f_3d);

        rM.setVox_(vox_);

        if(aR.amapVoxFile != null){

            //rM.read_amapvox_1_6(aR.amapVoxFile);
            rM.setAmapVox(aV);

        }



        int n_funk_per_thread = (int)Math.ceil((double)y_size / (double)aR.cores);

        sunriseAndSunset.clear();
        //ForkJoinPool myPool = new ForkJoinPool(aR.cores);

        //myPool.submit(() ->
        IntStream.range(0, x_res).parallel().forEach(x -> {
            //for(int x = 0; x < x_res; x++){
            GregorianCalendar time_ = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));

            double[] point_ = new double[3];
            for(int y = 0; y < y_res; y++){

                int precompute_x = (int)((double)steppi * (double)x + (double)steppi / 2.0); // x_size / 2;
                int precompute_y = (int)((double)steppi * (double)y + (double)steppi / 2.0); // x_size / 2;

                point_[0] = gt[0] + precompute_x * gt[1] + precompute_y * gt[2];
                point_[1] = gt[3] + precompute_x * gt[4] + precompute_y * gt[5];
                point_[2] = value;

                //System.out.println(Arrays.toString(point));
                double[] point_transformed = ct.TransformPoint(point_[0], point_[1]);
                //System.out.println(Arrays.toString(point_transformed));

                //System.exit(1);
                for(int month : calendar_month) {
                    for (int day = 1; day <= n_date_in_month[month]; day += 1) {

                        for (int hour = 0; hour <= 23; hour += 1) {

                            for(int minute = 0; minute < minutes.length; minute++) {

                                time_.set(2020, month, day, hour, minutes[minute], 00);

                                AzimuthZenithAngle result = SPA.calculateSolarPosition(time_, point_transformed[1], point_transformed[0], 0, DeltaT.estimate(time));

                                precomputed[x][y][month][day][hour][minute][0] =  (float)result.getZenithAngle();
                                precomputed[x][y][month][day][hour][minute][1] =  (float)result.getAzimuth();

                            }
                        }
                    }
                }
            }
        });//);

        //System.out.println("PRECOMPUTATION COMPLETE!");


        long start = System.currentTimeMillis();

        BlockingQueue<Pair<Integer, byte[][]>> provideRow = new LinkedBlockingQueue<>();

        for(int y = 0; y < chm_values_f_3d.length; y++)
            provideRow.add(Pair.of(y,chm_values_f_3d[y]));


        int cores = 4;
        Thread[] threads = new Thread[cores];

        for (int i = 0; i < cores; i++) {

            int mini = i * n_funk_per_thread;
            int maxi = Math.min(y_size, mini + n_funk_per_thread);

            threads[i] = (new solarParallel_3d(mini, maxi, x_size,y_size, raster_z_size, rM, sunriseAndSunset, gt, ct, aR, chm_values_f, chm_output_f, precomputed, steppi, rasterMaxValue, provideRow, chm_values_f_3d,
                    pointCloud, chm_values_mean_x, chm_values_mean_y, chm_values_mean_z));
            threads[i].start();

            //ForkJoinPool customThreadPool = new ForkJoinPool(5);

            //customThreadPool.submit(() –> largeDataset.parallelStream().forEach(System.out::println));
            //customThreadPool.shutdownNow();
/*
            IntStream.range(0, 10).parallel().forEach(i_ -> {
// throw an exception if
// a[] == null, b[] = null
// i < 0, a.length <= i, b.length <= i

            });

 */
        }

        for (int i = 0; i < threads.length; i++) {

            try {
                threads[i].join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //System.out.println("LOOP 2 COMPLETE!");

        //rM.get_sky_prop();

        //System.out.println("SKY PROP COMPLETE!");
        //ImagePlus imp = IJ.createImage("KansasCityShuffle", "32-bit", chm.getRasterXSize(), chm.getRasterYSize(), raster_z_size);

        //System.out.println(imp.getImageStack().getSize());

        if(false)
        for(int z = 1; z <= raster_z_size; z++){

            //ImageProcessor pros = imp.getImageStack().getProcessor(z);
            for(int x = 0; x < x_size; x++) {
                for (int y = 0; y < y_size; y++) {

                    //pros.putPixelValue(x, y, rM.getValue(x, y, z-1));

                }
            }
        }


        //blur3D(imp, xsigma, ysigma, zsigma);



        if(false)
            for(int z = 1; z <= raster_z_size; z++){

                //ImageProcessor pros = imp.getImageStack().getProcessor(z);

                for(int x = 0; x < x_size; x++) {
                    for (int y = 0; y < y_size; y++) {


                        //rM.setValue(x, y, z-1, pros.getPixelValue(x, y));

                    }
                }
            }



        File outFile = aR.createOutputFile(pointCloud);

        pointWriterMultiThread pw = new pointWriterMultiThread(outFile, pointCloud, "las2las", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);

        //System.out.println("START WRITING!");

        float min_angle = Float.MAX_VALUE;
        float min_flatness = Float.MAX_VALUE;
        float max_angle = Float.MIN_VALUE;
        float max_flatness = Float.MIN_VALUE;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);


                if(amapVox_mode){

                    if(!aV.checkPoint(tempPoint))
                        continue;

                }

                if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
                    continue;
                }

                double x__ = Math.min(((tempPoint.x - this_min_x) / aR.step), this_x_size-1);
                double y__ = Math.min(((this_max_y - tempPoint.y) / aR.step), this_y_size-1);
                double z__ = Math.min(((tempPoint.z - this_min_z) / aR.step), raster_z_size-1);

                int x = (int)x__;
                int y = (int)y__;
                int z = (int)z__;

                float[] idw_value = rM.IDW(x__, y__, z__);

                //System.out.println("iterpolated: " + Arrays.toString(idw_value) + " orig: " + rM.getValue(x, y, z));
                //tempPoint.intensity =  (int)(rM.getValue(x, y, z) / solarradiation * 65535.0);
                //tempPoint.intensity =  (int)(idw_value / solarradiation * 65535.0);
                tempPoint.intensity =  (int)(idw_value[0] / solarradiation * 65535.0);

                //tempPoint.intensity = (int)(vox_[x][y][z].normal / 180.0f * 65535.0);
                //tempPoint.intensity = (int)(vox_[x][y][z].flatness / 1.0f * 65535.0);

                tempPoint.setExtraByteINT((int)(vox_[x][y][z].normal / 180.0f * 65535.0), aR.create_extra_byte_vlr_n_bytes.get(0), 0);
                //tempPoint.setExtraByteINT((int)(idw_value[2] / 180.0f * 65535.0), aR.create_extra_byte_vlr_n_bytes.get(0), 0);
                tempPoint.setExtraByteINT((int)(vox_[x][y][z].flatness / 1.0f * 65535.0), aR.create_extra_byte_vlr_n_bytes.get(0), 1);
                //tempPoint.setExtraByteINT((int)(idw_value[3] / 1.0f * 65535.0), aR.create_extra_byte_vlr_n_bytes.get(0), 1);
                tempPoint.setExtraByteINT((int)(idw_value[0] / solarradiation * 65535.0), aR.create_extra_byte_vlr_n_bytes.get(0), 2);



                // This is insolation
                tempPoint.pointSourceId =  (short) (idw_value[1]);

                if(vox_[x][y][z].normal < min_angle)
                    min_angle = vox_[x][y][z].normal;

                if(vox_[x][y][z].flatness < min_flatness)
                    min_flatness = vox_[x][y][z].flatness;

                if(vox_[x][y][z].normal > max_angle)
                    max_angle = vox_[x][y][z].normal;

                if(vox_[x][y][z].flatness > max_flatness)
                    max_flatness = vox_[x][y][z].flatness;

                // this is closure
                //tempPoint.pointSourceId =  (short) (idw_value[1]);

                //System.out.println(tempPoint.pointSourceId);

                try {

                    aR.pfac.writePoint(tempPoint, i + j, thread_n);

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        //System.out.println("FINISHED WRITING!");

        System.out.println("min_angle: " + min_angle + " max_angle: " + max_angle);
        System.out.println("min_flatness: " + min_flatness + " max_flatness: " + max_flatness);

        aR.pfac.closeThread(thread_n);

        System.out.println("processing took: " + (System.currentTimeMillis()-start) + " ms with " + aR.cores + " threads");

        //dataset.FlushCache();
        chm.FlushCache();

    }

    public las2solar_photogrammetry(String chm_name, argumentReader aR, LASReader pointCloud, boolean d3, boolean this_is_binary) throws Exception{

        this.pointCloud = pointCloud;
        System.out.println(this.pointCloud.getMaxZ());
        LasPoint tempPoint = new LasPoint();
        int thread_n = aR.pfac.addReadThread(pointCloud);

        int number_of_last_returns = 0;
        int number_of_all_returns = 0;

        int raster_z_size = (int)Math.ceil((pointCloud.getMaxZ() - pointCloud.getMinZ()) / aR.step);

        Dataset chm = gdal.Open(chm_name);

        this.chm_values_f_3d = new byte[chm.getRasterYSize()][chm.getRasterXSize()][raster_z_size];
        this.chm_values_mean_y = new short[chm.getRasterYSize()][chm.getRasterXSize()][raster_z_size];
        this.chm_values_mean_x = new short[chm.getRasterYSize()][chm.getRasterXSize()][raster_z_size];
        this.chm_values_mean_z = new short[chm.getRasterYSize()][chm.getRasterXSize()][raster_z_size];

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 20000) {

            int maxi = (int) Math.min(20000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                int x = Math.min((int)((tempPoint.x - pointCloud.getMinX()) / aR.step), chm.getRasterXSize()-1);
                int y = Math.min((int)((pointCloud.getMaxY() - tempPoint.y) / aR.step), chm.getRasterYSize()-1);
                int z = Math.min((int)((tempPoint.z - pointCloud.getMinZ()) / aR.step), raster_z_size-1);


                float fraction_x = (float)((tempPoint.x - pointCloud.getMinX()) / aR.step) - (float)x;
                float fraction_y = (float)((pointCloud.getMaxY() - tempPoint.y) / aR.step) - (float)y;
                float fraction_z = (float)((tempPoint.z - pointCloud.getMinZ()) / aR.step) - (float)z;

                short frac_x = (short)(int)((fraction_x / aR.step) * 1000);
                short frac_y = (short)(int)((1.0f - fraction_y / aR.step) * 1000);
                short frac_z = (short)(int)((fraction_z / aR.step) * 1000);

                if(chm_values_f_3d[y][x][z] < 127) {
                    chm_values_f_3d[y][x][z]++;
                    //dailyAverageInsolation = dailyAverageInsolation + ((dailyAverageInsolation_ / 24.0) - dailyAverageInsolation)/(double)++n;
                    /*New average = old average * (n-1)/n + new value /n */
                    //chm_values_mean_x[y][x][z] = (short)(chm_values_mean_x[y][x][z] * ((short)chm_values_f_3d[y][x][z]-1)/(short)chm_values_f_3d[y][x][z] + frac_x / (short)chm_values_f_3d[y][x][z]);
                    chm_values_mean_x[y][x][z] = (short)(chm_values_mean_x[y][x][z] + (frac_x - chm_values_mean_x[y][x][z]) / (double) chm_values_f_3d[y][x][z]);// ((short)chm_values_f_3d[y][x][z]-1)/(short)chm_values_f_3d[y][x][z] + frac_x / (short)chm_values_f_3d[y][x][z]);
                    //chm_values_mean_y[y][x][z] = (short)(chm_values_mean_y[y][x][z] * ((short)chm_values_f_3d[y][x][z]-1)/(short)chm_values_f_3d[y][x][z] + frac_y / (short)chm_values_f_3d[y][x][z]);
                    chm_values_mean_y[y][x][z] = (short)(chm_values_mean_y[y][x][z] + (frac_y - chm_values_mean_y[y][x][z]) / (double) chm_values_f_3d[y][x][z]);
                    //chm_values_mean_z[y][x][z] = (short)(chm_values_mean_z[y][x][z] * ((short)chm_values_f_3d[y][x][z]-1)/(short)chm_values_f_3d[y][x][z] + frac_z / (short)chm_values_f_3d[y][x][z]);
                    chm_values_mean_z[y][x][z] = (short)(chm_values_mean_z[y][x][z] + (frac_z - chm_values_mean_z[y][x][z]) / (double) chm_values_f_3d[y][x][z]);

                }

            }
        }


        if(false)
            for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

                int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

                aR.pfac.prepareBuffer(thread_n, i, maxi);

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
        //gdal.SetCacheMax((int)(aR.gdal_cache_gb * 1073741824));


        //Dataset dataset = null;
        Driver driver = null;
        driver = gdal.GetDriverByName("GTiff");
        driver.Register();
        //Band band2=null;

        //dataset = driver.Create("testi.tif", chm.GetRasterXSize(), chm.GetRasterYSize(), 1, gdalconst.GDT_Float32);
        //dataset.SetGeoTransform(chm.GetGeoTransform());
        //dataset.SetProjection(chm.GetProjection());

        System.out.println(chm.GetProjection());
        //System.exit(1);
        //band2 = dataset.GetRasterBand(1);    // writable band


        int steppi = 10;

        int x_res = (int)Math.ceil((double)chm.getRasterXSize() / (double)steppi);
        int y_res = (int)Math.ceil((double)chm.getRasterYSize() / (double)steppi);

        int[] minutes = new int[]{0};        //float[][][][][][] precomputed = new float[x_res][y_res][12][32][24][2];
        float[][][][][][][] precomputed = new float[x_res][y_res][12][32][24][minutes.length][2];

        SpatialReference src = new SpatialReference();
        SpatialReference dst = new SpatialReference();

        dst.ImportFromProj4("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
        src.ImportFromProj4("+proj=utm +zone=35 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs");

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
        //double[] point_transformed = new double[2];

        /*
        Prepare sunrise and sunsets
         */
        ArrayList<int[]> sunriseAndSunset = new ArrayList<>();


        double[] blockArray = new double[]{0.0,0.0};

        double angleThreshold = aR.angle;

        if(angleThreshold == -999)
            angleThreshold = 45;


        solar3dManipulator rM = new solar3dManipulator(x_size, y_size, raster_z_size, new float[y_size][x_size][raster_z_size], aR.step);

        Thread[] threads = new Thread[aR.cores];
        int n_funk_per_thread = (int)Math.ceil((double)y_size / (double)aR.cores);

        sunriseAndSunset.clear();
        //ForkJoinPool myPool = new ForkJoinPool(aR.cores);
        GregorianCalendar time__ = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));

        time__.set(2020, 6, 14, 0, 0, 00);

        System.out.println("Hour: "+time__.get(Calendar.HOUR_OF_DAY));
        System.out.println("Date: "+time__.get(Calendar.DATE));
        System.out.println("Month: "+time__.get(Calendar.MONTH));
        System.out.println("Year: "+time__.get(Calendar.YEAR));
        //System.out.println(time__);
        time__.add(Calendar.SECOND, 231143);
        System.out.println("Hour: "+time__.get(Calendar.HOUR_OF_DAY));
        System.out.println("Date: "+time__.get(Calendar.DATE));
        System.out.println("Month: "+time__.get(Calendar.MONTH));
        System.out.println("Year: "+time__.get(Calendar.YEAR));
        System.exit(1);

        //myPool.submit(() ->
        IntStream.range(0, x_res).parallel().forEach(x -> {
            //for(int x = 0; x < x_res; x++){
            GregorianCalendar time_ = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));

            time_.set(2020, 6, 20, 0, 0, 00);

            time_.add(Calendar.SECOND, 10000);


            double[] point_ = new double[3];
            for(int y = 0; y < y_res; y++){

                int precompute_x = (int)((double)steppi * (double)x + (double)steppi / 2.0); // x_size / 2;
                int precompute_y = (int)((double)steppi * (double)y + (double)steppi / 2.0); // x_size / 2;

                point_[0] = gt[0] + precompute_x * gt[1] + precompute_y * gt[2];
                point_[1] = gt[3] + precompute_x * gt[4] + precompute_y * gt[5];
                point_[2] = value;

                //System.out.println(Arrays.toString(point));
                double[] point_transformed = ct.TransformPoint(point_[0], point_[1]);
                //System.out.println(Arrays.toString(point_transformed));

                //System.exit(1);
                for(int month : calendar_month) {
                    for (int day = 1; day <= n_date_in_month[month]; day += 1) {

                        for (int hour = 0; hour <= 23; hour += 1) {

                            for(int minute = 0; minute < minutes.length; minute++) {

                                time_.set(2020, month, day, hour, minutes[minute], 00);

                                AzimuthZenithAngle result = SPA.calculateSolarPosition(time_, point_transformed[1], point_transformed[0], 0, DeltaT.estimate(time_));

                                precomputed[x][y][month][day][hour][minute][0] =  (float)result.getZenithAngle();
                                precomputed[x][y][month][day][hour][minute][1] =  (float)result.getAzimuth();

                            }
                        }
                    }
                }
            }
        });//);

        long start = System.currentTimeMillis();

        BlockingQueue<Pair<Integer, byte[][]>> provideRow = new LinkedBlockingQueue<>();

        for(int y = 0; y < chm_values_f_3d.length; y++)
            provideRow.add(Pair.of(y,chm_values_f_3d[y]));


        for (int i = 0; i < aR.cores; i++) {

            int mini = i * n_funk_per_thread;
            int maxi = Math.min(y_size, mini + n_funk_per_thread);

            threads[i] = (new solarParallel_3d(mini, maxi, x_size,y_size, raster_z_size, rM, sunriseAndSunset, gt, ct, aR, chm_values_f, chm_output_f, precomputed, steppi, rasterMaxValue, provideRow, chm_values_f_3d,
                    pointCloud, chm_values_mean_x, chm_values_mean_y, chm_values_mean_z));
            threads[i].start();

            //ForkJoinPool customThreadPool = new ForkJoinPool(5);

            //customThreadPool.submit(() –> largeDataset.parallelStream().forEach(System.out::println));
            //customThreadPool.shutdownNow();
/*
            IntStream.range(0, 10).parallel().forEach(i_ -> {
// throw an exception if
// a[] == null, b[] = null
// i < 0, a.length <= i, b.length <= i

            });

 */
        }

        for (int i = 0; i < threads.length; i++) {

            try {
                threads[i].join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ImagePlus imp = IJ.createImage("KansasCityShuffle", "32-bit", chm.getRasterXSize(), chm.getRasterYSize(), raster_z_size);
        System.out.println(imp.getImageStack().getSize());

        for(int z = 1; z <= raster_z_size; z++){

            ImageProcessor pros = imp.getImageStack().getProcessor(z);
            for(int x = 0; x < chm.getRasterXSize(); x++) {
                for (int y = 0; y < chm.getRasterYSize(); y++) {

                    pros.putPixelValue(x, y, rM.getValue(x, y, z-1));

                }
            }
        }

        blur3D(imp, xsigma, ysigma, zsigma);

        if(true)
            for(int z = 1; z <= raster_z_size; z++){

                ImageProcessor pros = imp.getImageStack().getProcessor(z);

                for(int x = 0; x < chm.getRasterXSize(); x++) {
                    for (int y = 0; y < chm.getRasterYSize(); y++) {


                        rM.setValue(x, y, z-1, pros.getPixelValue(x, y));

                    }
                }
            }

        File outFile = aR.createOutputFile(pointCloud);

        pointWriterMultiThread pw = new pointWriterMultiThread(outFile, pointCloud, "las2las", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
                    continue;
                }

                int x = Math.min((int)((tempPoint.x - pointCloud.getMinX()) / aR.step), chm.getRasterXSize()-1);
                int y = Math.min((int)((pointCloud.getMaxY() - tempPoint.y) / aR.step), chm.getRasterYSize()-1);
                int z = Math.min((int)((tempPoint.z - pointCloud.getMinZ()) / aR.step), raster_z_size-1);

                tempPoint.intensity =  (int)(rM.getValue(x, y, z) / solarradiation * 65535.0);

                try {

                    aR.pfac.writePoint(tempPoint, i + j, thread_n);

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        aR.pfac.closeThread(thread_n);

        System.out.println("processing took: " + (System.currentTimeMillis()-start) + " ms with " + aR.cores + " threads");

        //dataset.FlushCache();
        chm.FlushCache();

    }


    private void blur3D(ImagePlus imp, double sigmaX, double sigmaY, double sigmaZ) {

        imp.killRoi();
        ImageStack stack = imp.getStack();
        if (sigmaX==sigmaY) {
            if (sigmaX!=0.0)
                IJ.run(imp, "Gaussian Blur...", "sigma="+sigmaX+" stack");
        } else {
            GaussianBlur gb = new GaussianBlur();
            for (int i=1; i<=imp.getStackSize(); i++) {
                ImageProcessor ip = stack.getProcessor(i);
                double accuracy = (imp.getBitDepth()==8||imp.getBitDepth()==24)?0.002:0.0002;
                gb.blurGaussian(ip, sigmaX, sigmaY, accuracy);
            }
        }
        if (sigmaZ>0.0) {
            if (imp.isHyperStack())
                blurHyperStackZ(imp, zsigma);
            else
                blurZ(stack, sigmaZ);
            imp.updateAndDraw();
        }
    }

    private void blurZ(ImageStack stack, double sigmaZ) {
        GaussianBlur gb = new GaussianBlur();
        double accuracy = (stack.getBitDepth()==8||stack.getBitDepth()==24)?0.002:0.0002;
        int w=stack.getWidth(), h=stack.getHeight(), d=stack.getSize();
        float[] zpixels = null;
        FloatProcessor fp =null;
        IJ.showStatus("Z blurring");
        gb.showProgress(false);
        int channels = stack.getProcessor(1).getNChannels();
        for (int y=0; y<h; y++) {
            IJ.showProgress(y, h-1);
            for (int channel=0; channel<channels; channel++) {
                zpixels = stack.getVoxels(0, y, 0, w, 1, d, zpixels, channel);
                if (fp==null)
                    fp = new FloatProcessor(w, d, zpixels);
                //if (y==h/2) new ImagePlus("before-"+h/2, fp.duplicate()).show();
                gb.blur1Direction(fp, sigmaZ, accuracy, false, 0);
                stack.setVoxels(0, y, 0, w, 1, d, zpixels, channel);
            }
        }
        IJ.showStatus("");
    }

    private void blurHyperStackZ(ImagePlus imp, double zsigma) {
        ImagePlus[] images = ChannelSplitter.split(imp);
        for (int i=0; i<images.length; i++) {
            blurZ(images[i].getStack(), zsigma);
        }
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
            provideRow.add(Pair.of(y,chm_values_f[y]));


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

            rM.setValue(row.getKey(),averageInsol);

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

    short[][][] chm_values_mean_x;
    short[][][] chm_values_mean_y;
    short[][][] chm_values_mean_z;

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
                            byte[][][] struct_3d, LASReader pointCloud,
                            short[][][] chm_values_mean_x,
                            short[][][] chm_values_mean_y,
                            short[][][] chm_values_mean_z) {

        this.chm_values_mean_x = chm_values_mean_x;
        this.chm_values_mean_y = chm_values_mean_y;
        this.chm_values_mean_z = chm_values_mean_z;


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


        double[] point = new double[3];
        double[] point_transformed = new double[3];

        int[] calendar_month = new int[]{Calendar.JUNE, Calendar.JULY, Calendar.AUGUST};

        int[] n_date_in_month = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30 , 31};
        int[] minutes = new int[]{0};
        int solarradiation = 1366;

        boolean debug = false;

        //ForkJoinPool customThreadPool = new ForkJoinPool(8);
        //float chm_value = 0f;
        //for (int y = min; y < this.max; y++) {
        while(true) {

            if (providerRow.isEmpty())
                break;

            Pair<Integer, byte[][]> row = providerRow.poll();

            float[] averageInsol = new float[x_size];

            //for (int x = 0; x < x_size; x++) {
                //ForkJoinPool myPool = new ForkJoinPool(2);

                //myPool.submit(() ->


                IntStream.range(0, x_size).parallel().forEach(x -> {
                int blocked_n = 0;
                for (int z = 0; z < z_size; z++) {

                    int precomputed_x = (int) Math.floor((double) x / (double) precomputed_resolution);
                    int precomputed_y = (int) Math.floor((double) row.getKey() / (double) precomputed_resolution);

                    //value = chm[x][y];
                    byte value = row.getValue()[x][z];
                    float chm_value = chm[row.getKey()][x];

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
                                    //minuteSum += (minutes[1] - minutes[0]);

                                    double slope = FastMath.tan(FastMath.toRadians(precomp[0]));

                                    //System.out.println("angle: " + slope + " slope: " + precomp[0]);
                                    //System.out.println("chm:value: " + rasterMaxValue);
                                    //double[] blockArray = isBlocked(x, row.getKey(), z, value, aR.step, (precomp[0]), slope, (float) (precomp[1]), hour);
                                    boolean blockArray = true;
                                    double dampening = 0.0;
                                    double dampening_2 = 0.0;
                                    try {
                                        //blockArray = isBlocked_ray_trace(x, row.getKey(), z, value, aR.step, (precomp[0]), slope, (float) (precomp[1]), hour, rasterMaxValue);

                                        if (!rM.hasAmapVox)
                                            dampening = isBlocked_ray_trace_dampening(x, row.getKey(), z, value, aR.step, (precomp[0]), slope, (float) (precomp[1]), hour, rasterMaxValue, rM);
                                        else
                                            dampening = isBlocked_ray_trace_amapVox(x, row.getKey(), z, value, aR.step, (precomp[0]), slope, (float) (precomp[1]), hour, rasterMaxValue, rM);
                                    } catch (Exception e) {

                                    }


                                    //System.out.println("insolation: " + slope + " " + (90.0f - precomp[0])) ;
                                    //if (debug)
                                    //    if (x == 130 && y == 1640)
                                    //        System.out.println("block: " + Arrays.toString(blockArray) + " hour: " + hour + " sunrise: " + sunrise + " set: " + sunset + " precom: " + precomp[0]);

                                    if (!blockArray || true) {

                                        dampening = Math.max(0, dampening);
                                        //System.out.println("dampening: " + dampening);

                                        //dampening_2 = Math.max(0, dampening_2);

                                        //System.out.println(dampening + " " + dampening_2);
                                        /* If the average obstructed depth is less than one meter */
                                        // if ((blockArray[0] / blockArray[1]) < 0.5 && false) {

                                        //dailyAverageInsolation = (dailyAverageInsolation + insolation) / 2.0;
                                        //dailyAverageInsolation = dailyAverageInsolation + (insolation - dailyAverageInsolation)/(double)++n;
                                        //dailyAverageInsolation_ += insolation;


                                        //hour_mean_insolation += insolation;
                                        hour_mean_insolation += insolation * dampening;
                                        // }

                                    } else {
                                        hour_mean_insolation += 0;
                                        //dailyAverageInsolation = dailyAverageInsolation + (insolation - dailyAverageInsolation)/(double)++n;
                                        //dailyAverageInsolation_ += insolation;
                                        //hour_mean_insolation += insolation;
                                        //n_++;
                                        blocked_n++;

                                    }
                                    //System.out.println("ave: " + dailyAverageInsolation);
                                    /* This means there is nothing blocking the sunray */
                                    //if (blockArray[0] == 0) {

                                    //}
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
                    rM.setValue(x, row.getKey(), z, (float) dailyAverageInsolation);
                }
            });//);

            //System.out.println(row.getKey());
            //System.out.println(providerRow.size());
            //System.out.println("n_blocked: " + blocked_n);
            //System.out.println(y + " / " + this.max );
        }
    }

    public float euclideanDistance3d(float x1, float y1, float z1, float x2, float y2, float z2){


        return (float)Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2) + (z1 - z2)*(z1 - z2) );

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

    public boolean isBlocked_ray_trace(int x, int y, int z_vox, byte z, double resolution, double zenith_angle, double slope, float direction_angle, int hour, float chm_max){



        int penetration = 0;

        int minPenetration = 2;

        double distance_threshold = 100000;
        int current_x = x;
        int current_y = y;
        int current_z = z_vox;

        //System.out.println(y + " " + x + " " + z + " " + this.z_size );
        float x_offset = chm_values_mean_x[y][x][z_vox] / 1000.0f;
        float y_offset = chm_values_mean_y[y][x][z_vox] / 1000.0f;
        float z_offset = chm_values_mean_z[y][x][z_vox] / 1000.0f;

        Vector3d line_quick = new Vector3d(0,0,0);
        Vector3d point_quick = new Vector3d(0,0,0);

        float outside_x = 10000, outside_y = 10000, outside_z = 10000;

        int step_z;

        //double[] output = new double[]{0.0d, 0.0d};
        double increment_in_meters = resolution / 2.0;

        //float center_of_pixel_x = (float)x + 0.5f * (float)resolution;
        float center_of_pixel_x = (float)x + x_offset * (float)resolution;

        //float center_of_pixel_y = (float)y + 0.5f * (float)resolution;
        float center_of_pixel_y = (float)y + y_offset * (float)resolution;

        //float center_of_pixel_z = (float)z_vox + 0.5f * (float)resolution;
        float center_of_pixel_z = (float)z_vox + z_offset * (float)resolution;

        int right_x = (int)(center_of_pixel_x + 1);
        int left_x = (int)(center_of_pixel_x);

        int top_y = (int)(-center_of_pixel_y + 1);
        int bottom_y = (int)(center_of_pixel_x);

        //System.out.println(center_of_pixel_x + " " + center_of_pixel_y + " " + center_of_pixel_z);
        //System.out.println(x_offset + " " + y_offset + " " + z_offset);

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
        float cosAngle2 = 0;

        cosAngle2 = cos((FastMath.abs((float)zenith_angle)));
        sinAngle2 = sin((FastMath.abs((float)zenith_angle)));

        int step_x = -2, step_y = -2;
        byte n_points_in_voxel = 0;

        if(direction_angle < 90.0f){

            step_x = 1;
            step_y = -1;

            cosAngle = cos((90.0f - direction_angle));
            sinAngle = sin((90.0f - direction_angle));

            outside_y = -center_of_pixel_y + sinAngle * 10000;
            outside_x = center_of_pixel_x + cosAngle * 10000;

            line_quick.set(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);

            float t_d_x = (float) (resolution / cosAngle / sinAngle2);
            float t_d_y = (float) (resolution / sinAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);


            float[] y_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, center_of_pixel_x - 10000, (int)(-center_of_pixel_y), center_of_pixel_x + 10000, (int)(-center_of_pixel_y));
            float[] x_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x+1), -(float) center_of_pixel_y + 10000, (int)(center_of_pixel_x+1), -center_of_pixel_y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, x - 10000,  (int)(center_of_pixel_z+1), x + 10000,  (int)(center_of_pixel_z+1));
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, x - 10000,  (int)(center_of_pixel_z), x + 10000,  (int)(center_of_pixel_z));


            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;

            //System.out.println("DEG: " + FastMath.abs(center_of_pixel_z - z_intersect[1]));
            //System.out.println("t_max_x: " + t_max_x + " t_max_z: " + t_max_z + " t_max_y: " + t_max_y + " " + direction_angle + " " + zenith_angle);

            if(false)
                if(direction_angle > 85f){

                    System.out.println(t_max_x + " " + t_max_y + " " + t_max_z + " " + zenith_angle + " " + direction_angle);

                }

            boolean breakki = false;
            int counter = 0;
            /* RAY TRACE ITERATIONS */
            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }
                counter++;


                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    return false;
                }

                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    return false;


                if(current_z != z_vox)
                    n_points_in_voxel = chm_3d[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > 1)
                    penetration++;

                if(n_points_in_voxel > (1) && penetration >= minPenetration){

                    x_offset = (float)chm_values_mean_x[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    y_offset = (float)chm_values_mean_y[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    z_offset = (float)chm_values_mean_z[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;

                    //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                    //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                    // point.crossProduct(point);
                    //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                    //        (float)-current_y+y_offset-(-center_of_pixel_y),
                    //        (float)current_z+z_offset-center_of_pixel_z);

                    point_quick.set((float)current_x+x_offset-center_of_pixel_x,
                            (float)-current_y+y_offset-(-center_of_pixel_y),
                            (float)current_z+z_offset-center_of_pixel_z);

                    //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());

                    point_quick.cross(point_quick, line_quick);
                    float distance = (float)(point_quick.norm() / line_quick.norm());
                    //float distance = 0;
                    //System.out.println(distance + " == " + distance2);
                    //System.out.println(distance);

                        if(distance < distance_threshold){
                           // System.out.println("BLOCKED!");
                            return true;
                        }

                }
            }

        }

        else if(direction_angle < 180.0f){

           // System.out.println("HERE!!");
            step_x = 1;
            step_y = 1;

            cosAngle = cos((180.0f - direction_angle));
            sinAngle = sin((180.0f - direction_angle));


            outside_y = -center_of_pixel_y - cosAngle * 10000;
            outside_x = center_of_pixel_x + sinAngle * 10000;

            //Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);

            float t_d_x = (float) (resolution / sinAngle / sinAngle2);
            float t_d_y = (float) (resolution / cosAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            float[] y_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, center_of_pixel_x - 10000, (int)(-center_of_pixel_y-1), center_of_pixel_x + 10000, (int)(-center_of_pixel_y-1));
            float[] x_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x+1), -center_of_pixel_y + 10000, (int)(center_of_pixel_x+1), -center_of_pixel_y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect((float) center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z+1), center_of_pixel_x + 10000,  (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD BE HERE!");
            }else
                z_intersect = lineIntersect((float) center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z), center_of_pixel_x + 10000,  (int)(center_of_pixel_z));


            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;

            boolean breakki = false;

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }

                //System.out.println(this.rasterMaxValue + " " + (this.p_cloud_min_z + current_z * resolution));


                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    return false;
                }


                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    return false;

                if(current_z != z_vox)
                    n_points_in_voxel = chm_3d[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > 1)
                    penetration++;

                if(n_points_in_voxel > (1) && penetration >= minPenetration){



                    x_offset = (float)chm_values_mean_x[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    y_offset = (float)chm_values_mean_y[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    z_offset = (float)chm_values_mean_z[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;

                    //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                    //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                    // point.crossProduct(point);
                    //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                    // point.crossProduct(point);
                    //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                    //        (float)-current_y+y_offset-(-center_of_pixel_y),
                    //        (float)current_z+z_offset-center_of_pixel_z);

                    point_quick.set((float)current_x+x_offset-center_of_pixel_x,
                            (float)-current_y+y_offset-(-center_of_pixel_y),
                            (float)current_z+z_offset-center_of_pixel_z);

                    //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());

                    point_quick.cross(point_quick, line_quick);
                    float distance = (float)(point_quick.norm() / line_quick.norm());
                    //float distance = 0;
                    //System.out.println(distance);
                    //float distance = 0;
                    if(distance < distance_threshold){
                        //System.out.println("HERE2");

                       // System.out.println("BLOCKED!");
                        return true;
                    }

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

            float t_d_x = (float) (resolution / cosAngle / sinAngle2);
            float t_d_y = (float) (resolution / sinAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            //Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);

            float[] y_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, x - 10000, (int)(-center_of_pixel_y-1), x + 10000, (int)(-center_of_pixel_y-1));
            float[] x_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x), -center_of_pixel_y + 10000, (int)(center_of_pixel_x), -center_of_pixel_y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z+1), center_of_pixel_x + 10000,  (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD!");
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z), center_of_pixel_x + 10000,  (int)(center_of_pixel_z));


            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;

            boolean breakki = false;

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }

                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    return false;
                }

                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    return false;


                if(current_z != z_vox)
                    n_points_in_voxel = chm_3d[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > 1)
                    penetration++;

                if(n_points_in_voxel > (1) && penetration >= minPenetration){

                    x_offset = (float)chm_values_mean_x[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    y_offset = (float)chm_values_mean_y[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    z_offset = (float)chm_values_mean_z[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;

                    //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                    //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                    // point.crossProduct(point);
                    //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                    // point.crossProduct(point);
                    //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                    //        (float)-current_y+y_offset-(-center_of_pixel_y),
                    //        (float)current_z+z_offset-center_of_pixel_z);

                    point_quick.set((float)current_x+x_offset-center_of_pixel_x,
                            (float)-current_y+y_offset-(-center_of_pixel_y),
                            (float)current_z+z_offset-center_of_pixel_z);

                    //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());

                    point_quick.cross(point_quick, line_quick);
                    float distance = (float)(point_quick.norm() / line_quick.norm());
                    //float distance = 0;
                    //System.out.println(distance + " == " + distance2);

                    //float distance = 0;
                    if(distance < distance_threshold){
                        //System.out.println("BLOCKED!");
                        return true;
                    }

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


            float t_d_x = (float) (resolution / sinAngle / sinAngle2);
            float t_d_y = (float) (resolution / cosAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            // Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);


            float[] y_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, x - 10000, (int)(-center_of_pixel_y), x + 10000, (int)(-center_of_pixel_y));
            //float[] y_intersect_z = lineIntersect(-center_of_pixel_y, center_of_pixel_z, outside_y, outside_z, z - 10000, (int)(-center_of_pixel_y), x + 10000, (int)(-center_of_pixel_y));
            float[] x_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x), -y + 10000, (int)(center_of_pixel_x), -y - 10000);


            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, x - 10000,  (int)(center_of_pixel_z+1), x + 10000, (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD!");
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, x - 10000, (int)(center_of_pixel_z), x + 10000,  (int)(center_of_pixel_z));


            //System.out.println(Arrays.toString(y_intersect));
            //System.out.println(Arrays.toString(x_intersect));


            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;

            boolean breakki = false;

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {

                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }


                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    return false;
                }
                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    return false;


                if(current_z != z_vox)
                    n_points_in_voxel = chm_3d[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > 1)
                    penetration++;

                if(n_points_in_voxel > (1) && penetration >= minPenetration       ){

                    x_offset = (float)chm_values_mean_x[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    y_offset = (float)chm_values_mean_y[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    z_offset = (float)chm_values_mean_z[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;

                    //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                    //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                    // point.crossProduct(point);
                    //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                    // point.crossProduct(point);
                    //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                    //        (float)-current_y+y_offset-(-center_of_pixel_y),
                    //        (float)current_z+z_offset-center_of_pixel_z);

                    point_quick.set((float)current_x+x_offset-center_of_pixel_x,
                            (float)-current_y+y_offset-(-center_of_pixel_y),
                            (float)current_z+z_offset-center_of_pixel_z);

                    //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());

                    point_quick.cross(point_quick, line_quick);
                    float distance = (float)(point_quick.norm() / line_quick.norm());
                    //float distance = 0;
                    //System.out.println(distance + " == " + distance2);

                    //float distance = 0;

                    if(distance < distance_threshold){

                        return true;
                    }

                }

            }



        }

        return false;

    }

    public static Vector2D getIntersectionPoint(Line line, Plane plane) {
        Vector3D intersection = plane.intersection(line);
        return new Vector2D(intersection.getX(), intersection.getY());
    }

    public double isBlocked_ray_trace_dampening(int x, int y, int z_vox, byte z, double resolution, double zenith_angle, double slope,
                                                float direction_angle, int hour, float chm_max, solar3dManipulator rM){


        //Plane pla = new Plane();
        //direction_angle = 225;
        //zenith_angle = 35.264;
        //zenith_angle = 50;


        boolean debug = false;
        double dampening_factor_per_meter = 0.33;

        double dampening_factor_per_voxel = resolution * dampening_factor_per_meter;
        double dampening = 1.0;

        int penetration = 0;

        float[] point = new float[3];
        float[] lineStart = new float[3];
        float[] lineEnd = new float[3];


        int minPenetration = 1;

        double distance_threshold_solid = 100000;
        double distance_threshold = 10000;
        double length_threshold = 0.0;


        int current_x = x;
        int current_y = y;
        int current_z = z_vox;

        //System.out.println(y + " " + x + " " + z + " " + this.z_size );
        float x_offset = chm_values_mean_x[y][x][z_vox] / 1000.0f;
        float y_offset = chm_values_mean_y[y][x][z_vox] / 1000.0f;
        float z_offset = chm_values_mean_z[y][x][z_vox] / 1000.0f;

        //x_offset = 0.5f;
        //y_offset = 0.5f;
        //z_offset = 0.5f;

        Vector3d line_quick = new Vector3d(0,0,0);
        Vector3d point_quick = new Vector3d(0,0,0);

        float outside_x = 10000, outside_y = 10000, outside_z = 10000;

        int step_z;

        //double[] output = new double[]{0.0d, 0.0d};
        double increment_in_meters = resolution / 2.0;

        //float center_of_pixel_x = (float)x + 0.5f * (float)resolution;
        float center_of_pixel_x = (float)x + x_offset;

        //float center_of_pixel_y = (float)y + 0.5f * (float)resolution;
        float center_of_pixel_y = (float)y + y_offset;

        //float center_of_pixel_z = (float)z_vox + 0.5f * (float)resolution;
        float center_of_pixel_z = (float)z_vox + z_offset;

        int right_x = (int)(center_of_pixel_x + 1);
        int left_x = (int)(center_of_pixel_x);

        int top_y = (int)(-center_of_pixel_y + 1);
        int bottom_y = (int)(center_of_pixel_x);

        //System.out.println(center_of_pixel_x + " " + center_of_pixel_y + " " + center_of_pixel_z);
        //System.out.println(x_offset + " " + y_offset + " " + z_offset);

        if(slope > 0) {
            step_z = 1;
            outside_z = center_of_pixel_z + (float)Math.tan( Math.toRadians((float)zenith_angle)) * 10000;
            //System.out.println(outside_z);
            //System.out.println(center_of_pixel_z);
            //System.exit(1);
        }
        else {
            outside_z = center_of_pixel_z - cos(90.0f - (float)zenith_angle) * 10000;
            step_z = -1;
        }

        if(false) {
            System.out.println("......................................");
            System.out.println("......................................");
            System.out.println("......................................");
            System.out.println("STARTING: " + current_x + " " + current_y + " " + current_z);
        }


        float cosAngle = 0;
        float sinAngle = 0;
        float sinAngle2 = 0;
        float cosAngle2 = 0;
        float cosAngle2_2 = 0;

        //cosAngle2 = (float)Math.cos((FastMath.abs((float)zenith_angle)));
        cosAngle2 = (float)cos((FastMath.abs((float)zenith_angle)));
        cosAngle2_2 = (float)cos((FastMath.abs( 90.0f - (float)zenith_angle)));
        //sinAngle2 = (float)Math.sin((FastMath.abs((float)zenith_angle)));
        sinAngle2 = (float)sin((FastMath.abs((float)zenith_angle)));

        int step_x = -2, step_y = -2;
        byte n_points_in_voxel = 0;

        float delta_t_last = -1;


        // Point3D point, Point3D linePoint1, Point3D linePoint2

        Point3d point_ = new Point3d(0,0,0);
        Point3d linePoint1 = new Point3d(0,0,0);
        Point3d linePoint2 = new Point3d(0,0,0);


        if(direction_angle < 90.0f){

            step_x = 1;
            step_y = -1;

            cosAngle = cos((90.0f - direction_angle));
            sinAngle = sin((90.0f - direction_angle));

            outside_y = -center_of_pixel_y + sinAngle * 10000;
            outside_x = center_of_pixel_x + cosAngle * 10000;

            line_quick.set(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);

            float t_d_x = (float) (resolution / cosAngle / sinAngle2);
            float t_d_y = (float) (resolution / sinAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);


            t_d_x = (float) (resolution / cosAngle / cosAngle2);
            t_d_y = (float) (resolution / sinAngle / cosAngle2);
            t_d_z = (float) (resolution / sinAngle2);

            float[] y_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, center_of_pixel_x - 10000, (int)(-center_of_pixel_y), center_of_pixel_x + 10000, (int)(-center_of_pixel_y));
            float[] x_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x+1), -(float) center_of_pixel_y + 10000, (int)(center_of_pixel_x+1), -center_of_pixel_y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, x - 10000,  (int)(center_of_pixel_z+1), x + 10000,  (int)(center_of_pixel_z+1));
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, x - 10000,  (int)(center_of_pixel_z), x + 10000,  (int)(center_of_pixel_z));

/*
            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;


 */
            float x_ = (float)Math.ceil(center_of_pixel_x) - center_of_pixel_x;
            float x_hypo = x_ / cosAngle;
            float x_length = x_hypo / cosAngle2;

            float y_ =  center_of_pixel_y - (float)Math.floor(center_of_pixel_y);
            float y_hypo = y_ / sinAngle;
            float y_length = y_hypo / cosAngle2;


            float z_;
            float z_hypo;
            float z_length;

            if(slope > 0) {
                z_ = (float) Math.ceil(center_of_pixel_z) - center_of_pixel_z;
                z_hypo = z_ / cosAngle2_2;
                z_length = z_hypo;
            }else{
                z_ = center_of_pixel_z - (float) Math.floor(center_of_pixel_z);
                z_hypo = z_ / cosAngle2_2;
                z_length = z_hypo;
            }



            //float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / cosAngle2;
            //float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / cosAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            //float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;
            float t_max_z = euclideanDistance(center_of_pixel_x, center_of_pixel_z, z_intersect[0], z_intersect[1]) / cosAngle;
            //System.out.println("DEG: " + FastMath.abs(center_of_pixel_z - z_intersect[1]));
            //System.out.println("t_max_x: " + t_max_x + " t_max_z: " + t_max_z + " t_max_y: " + t_max_y + " " + direction_angle + " " + zenith_angle);

            t_max_x = x_length;
            t_max_y = y_length;
            t_max_z = z_length;

            lineStart[0] = center_of_pixel_x;lineEnd[0] = outside_x;
            lineStart[1] = -center_of_pixel_y; lineEnd[1] = outside_y;
            lineStart[2] = center_of_pixel_z; lineEnd[2] = outside_z;



            if(false)
                if(direction_angle > 85f){

                    System.out.println(t_max_x + " " + t_max_y + " " + t_max_z + " " + zenith_angle + " " + direction_angle);

                }

            boolean breakki = false;
            int counter = 0;
            /* RAY TRACE ITERATIONS */
            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }
                counter++;


                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    return dampening;
                }

                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    return dampening;


                if(current_z != z_vox)
                    n_points_in_voxel = chm_3d[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > 1)
                    penetration++;

                if(n_points_in_voxel > (1) && penetration >= minPenetration){

                    double length = getLengthInCurrentVoxel(t_max_x, t_max_y, t_max_z, t_d_x, t_d_y, t_d_z, t_current, current_x, current_y, current_z, step_x,
                            step_y, step_z, delta_t_last);

                    if(length > length_threshold) {

                        length *= resolution;

                        x_offset = (float) chm_values_mean_x[(int) current_y][(int) current_x][(int) current_z] / 1000.0f;
                        y_offset = (float) chm_values_mean_y[(int) current_y][(int) current_x][(int) current_z] / 1000.0f;
                        z_offset = (float) chm_values_mean_z[(int) current_y][(int) current_x][(int) current_z] / 1000.0f;

                        //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                        //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                        // point.crossProduct(point);
                        //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                        //        (float)-current_y+y_offset-(-center_of_pixel_y),
                        //        (float)current_z+z_offset-center_of_pixel_z);

                        point_quick.set((float) current_x + x_offset - center_of_pixel_x,
                                (float) -current_y + y_offset - (-center_of_pixel_y),
                                (float) current_z + z_offset - center_of_pixel_z);

                        //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());


                        point_quick.cross(point_quick, line_quick);


                        float distance = (float) (point_quick.norm() / line_quick.norm());


                        point[0] = (float) current_x + x_offset;
                        point[1] = (float) -current_y + y_offset;
                        point[2] = (float) current_z + z_offset;

                        distance = betweenPointAndLine(point, lineStart, lineEnd);


                        //System.out.println("dist_90: " + distance);
                        //float distance = 0;
                        //System.out.println(distance + " == " + distance2);
                        //System.out.println(distance);

                        //System.out.println("length: " + length + " " + (length / resolution));

                        //System.exit(1);


                        if (distance < distance_threshold) {

                            if (rM.vox_[current_x][current_y][current_z].isSolid)
                                return 0.0;

                            // System.out.println("BLOCKED!");

                            //if(rM.vox_[current_x][current_y][current_z].flatness < 0.20){
                            //    return 0.0;
                            //}

                            float neg = 1.0f - rM.vox_[current_x][current_y][current_z].flatness;

                            //dampening -= dampening_factor_per_voxel; // * neg;
                            dampening -= dampening_factor_per_meter * length; // * neg;
                        }
                    }
                }

                delta_t_last = t_current;

                if(dampening <= 0.0)
                    return dampening;
            }

        }

        else if(direction_angle < 180.0f){

            if(false){
                if(direction_angle < 130 || direction_angle > 140)
                    return 0.0;
            }
            // System.out.println("HERE!!");
            step_x = 1;
            step_y = 1;

            //cosAngle = cos((180.0f - direction_angle));
            cosAngle = cos((180.0f - direction_angle));
            //sinAngle = sin((180.0f - direction_angle));
            sinAngle = sin(180.0f - direction_angle);


            outside_y = -center_of_pixel_y - cosAngle * 10000;
            outside_x = center_of_pixel_x + sinAngle * 10000;

            //Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y + center_of_pixel_y, outside_z-center_of_pixel_z);
            //line_quick.set(outside_x, outside_y, outside_z);

            float t_d_x = (float) (resolution / sinAngle / sinAngle2);
            float t_d_y = (float) (resolution / cosAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            //System.out.println(t_d_x);
            //System.out.println(t_d_y);
            //System.out.println(t_d_z);

            t_d_x = (float) (resolution / sinAngle / cosAngle2);
            t_d_y = (float) (resolution / cosAngle / cosAngle2);
            t_d_z = (float) (resolution / sinAngle2);

            //System.out.println("new");
            //System.out.println(t_d_x);
            //System.out.println(t_d_y);
            //System.out.println(t_d_z);


            float[] x_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, center_of_pixel_x - 10000, (int)(-center_of_pixel_y-1), center_of_pixel_x + 10000, (int)(-center_of_pixel_y-1));
            float[] y_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x+1), -center_of_pixel_y + 10000, (int)(center_of_pixel_x+1), -center_of_pixel_y - 10000);

            float[] z_intersect;
            float[] z_intersect_2 = null;

            if(step_z > 0){
                z_intersect = lineIntersect((float) center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z+1), center_of_pixel_x + 10000,  (int)(center_of_pixel_z+1));
                z_intersect_2 = lineIntersect((float) -center_of_pixel_y, center_of_pixel_z, outside_y, outside_z, -center_of_pixel_y - 10000,  (int)(center_of_pixel_z+1), -center_of_pixel_y + 10000,  (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD BE HERE!");
            }else
                z_intersect = lineIntersect((float) center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z), center_of_pixel_x + 10000,  (int)(center_of_pixel_z));


            float x_ = (float)Math.ceil(center_of_pixel_x) - center_of_pixel_x;
            float x_hypo = x_ / sinAngle;
            float x_length = x_hypo / cosAngle2;

            float y_ =  center_of_pixel_y - (float)Math.floor(center_of_pixel_y);
            float y_hypo = y_ / cosAngle;
            float y_length = y_hypo / cosAngle2;

            float z_;
            float z_hypo;
            float z_length;



            if(slope > 0) {
                z_ = (float) Math.ceil(center_of_pixel_z) - center_of_pixel_z;
                z_hypo = z_ / cosAngle2_2;
                z_length = z_hypo;
            }else{
                z_ = center_of_pixel_z - (float) Math.floor(center_of_pixel_z);
                z_hypo = z_ / cosAngle2_2;
                z_length = z_hypo;
            }

            //System.out.println(x_length);
            //System.out.println(y_length);
            //System.out.println(z_length);
            //System.exit(1);

/*
            System.out.println("x_le: " + x_length);
            System.out.println("y_le: " + y_length);
            System.out.println("z_le: " + z_length);


            System.out.println(center_of_pixel_x + " " + center_of_pixel_y + " " + center_of_pixel_z);
            System.out.println(cosAngle2);
            System.out.println(Arrays.toString(x_intersect));
            System.out.println(Arrays.toString(y_intersect));
            System.out.println(Arrays.toString(z_intersect));
            System.out.println(Arrays.toString(z_intersect_2));

            //float x_distance = euclideanDistance3d(x_intersect[0], x_intersect[1], z_intersect)

            //outside_y = y_intersect[1] - cosAngle * 10000;
            //outside_x = x_intersect[0] + sinAngle * 10000;

            System.out.println(euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y));
*/
            //float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / cosAngle2;
            //float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / cosAngle2;
            //float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / cosAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            //float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;
            float t_max_z = euclideanDistance(center_of_pixel_x, center_of_pixel_z, z_intersect[0], z_intersect[1]) / cosAngle;


            t_max_x = x_length;
            t_max_y = y_length;
            t_max_z = z_length;

/*
            System.out.println("slope: " + slope);
            System.out.println(t_max_x + " x ");
            System.out.println(t_max_y + " y ");
            System.out.println(t_max_z + " z ");
            System.out.println(zenith_angle);

            //System.exit(1);
*/

            boolean breakki = false;

            lineStart[0] = center_of_pixel_x;lineEnd[0] = outside_x;
            lineStart[1] = -center_of_pixel_y; lineEnd[1] = outside_y;
            lineStart[2] = center_of_pixel_z; lineEnd[2] = outside_z;

            linePoint1.set(center_of_pixel_x, -center_of_pixel_y, center_of_pixel_z);
            linePoint1.set(center_of_pixel_x, -center_of_pixel_y, center_of_pixel_z);
            //linePoint1.set(x_intersect[0], y_intersect[1], z_intersect[1]);
            linePoint2.set(outside_x, outside_y, outside_z);

            point[0] = center_of_pixel_x;
            point[1] = -center_of_pixel_y;
            point[2] = center_of_pixel_z;

            float distance_test = betweenPointAndLine(point, lineStart, lineEnd);

            ArrayList<Integer> sides = new ArrayList<>();
            ArrayList<Float> distances = new ArrayList<>();
            ArrayList<Float> correction_plus = new ArrayList<>();
            ArrayList<Integer> x_s = new ArrayList<>();
            ArrayList<Integer> y_s = new ArrayList<>();
            ArrayList<Float> x_coor = new ArrayList<>();
            ArrayList<Float> y_coor = new ArrayList<>();
            ArrayList<Float> lengths = new ArrayList<>();


            //System.out.println("TEST DISTANCE: " + distance_test);
            //sSystem.exit(1);

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                        sides.add(1);
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                        sides.add(2);
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }

                //System.out.println(x + " " + y + " " + z_vox);
                //System.out.println("debug: " + current_x + "\t" + current_y + "\t" + current_z + " " + t_current + " " + delta_t_last + " " + (t_current - delta_t_last));
                //System.exit(1);
                //System.out.println(this.rasterMaxValue + " " + (this.p_cloud_min_z + current_z * resolution));


                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){

                    if(debug) {
                        System.out.println(Arrays.toString(sides.toArray()));
                        System.out.println(Arrays.toString(distances.toArray()));
                        System.out.println(Arrays.toString(correction_plus.toArray()));

                        System.out.println(Arrays.toString(x_s.toArray()));
                        System.out.println(Arrays.toString(y_s.toArray()));
                        System.out.println(Arrays.toString(x_coor.toArray()));
                        System.out.println(Arrays.toString(y_coor.toArray()));
                        System.out.println(Arrays.toString(lengths.toArray()));
                        System.exit(1);
                    }

                    return dampening;
                }


                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size) {

                    if(debug) {
                        System.out.println(Arrays.toString(sides.toArray()));
                        System.out.println(Arrays.toString(distances.toArray()));
                        System.out.println(Arrays.toString(correction_plus.toArray()));
                        System.out.println(Arrays.toString(x_s.toArray()));
                        System.out.println(Arrays.toString(y_s.toArray()));
                        System.out.println(Arrays.toString(x_coor.toArray()));
                        System.out.println(Arrays.toString(y_coor.toArray()));
                        System.out.println(Arrays.toString(lengths.toArray()));
                        System.exit(1);
                    }

                    return dampening;
                }

                if(current_z != z_vox)
                    n_points_in_voxel = chm_3d[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > 1)
                    penetration++;



                if(debug){

                    x_s.add(current_x);
                    y_s.add(current_y);

                    double length = getLengthInCurrentVoxel(t_max_x, t_max_y, t_max_z, t_d_x, t_d_y, t_d_z, t_current, current_x, current_y, current_z, step_x,
                            step_y, step_z, delta_t_last);

                    lengths.add((float)length);
                    System.out.println("orig: " + x + " " + y + " " + z_vox);
                    System.out.println("CENTER: " + center_of_pixel_x + " " + center_of_pixel_y + " " + center_of_pixel_z);
                    System.out.println("intersect: " + x_intersect[0] + " " + y_intersect[1] + " " + z_intersect[1]);
                    System.out.println(Arrays.toString(z_intersect));
                    System.out.println("Zenith: " + zenith_angle + " dir: " + direction_angle);

                    x_offset = (float)chm_values_mean_x[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    y_offset = (float)chm_values_mean_y[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    z_offset = (float)chm_values_mean_z[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;


                    //x_offset = 0.5f;
                    //y_offset = 0.5f;
                    //z_offset = 0.5f;

                    point_quick.set((float)current_x+x_offset,
                            (float)-current_y-y_offset,
                            (float)current_z+z_offset);

                    //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm())

                    //point_quick.cross(point_quick, line_quick);
                    point_quick.cross(line_quick, point_quick);

                    float distance = (float)(point_quick.norm() / line_quick.norm());
                    System.out.println("dist: " + distance + " " + current_x + " " + current_y + " " + current_z);
                    System.out.println("canter: " + center_of_pixel_x + " " + center_of_pixel_y + " " + center_of_pixel_z);
                    System.out.println("current: " + current_x + " " + current_y + " " + current_z);
                    System.out.println("Tcurrent: " + t_current);
                    System.out.println("outside: " + outside_x + " " + outside_y + " " + outside_z);
                    System.out.println("offs: " + x_offset  +  " " + y_offset + " " + z_offset );

                    point[0] = (float)current_x+x_offset;
                    point[1] = (float)-current_y+y_offset;
                    point[2] = (float)current_z+z_offset;

                    point_.set((float)current_x+x_offset, (float)current_y+y_offset, (float)current_z+z_offset);

                    System.out.println("point: " + point[0] + " " + point[1] + " " + point[2] );

                    distance = betweenPointAndLine(point, lineStart, lineEnd);
                    distances.add(distance);
                    point[2] -= distance;
                    distance = betweenPointAndLine(point, lineStart, lineEnd);
                    correction_plus.add(distance);
                    System.out.println("dist2: " + distance + " " + t_current   );
                    x_coor.add(point[0]);
                    y_coor.add(point[1]);
                    distance = (float)distancePointLine(point_, linePoint1, linePoint2);


                    System.out.println("dist3: " + distance  );
                    //System.out.println("dist3: " + point_quick.cross(point_quick, line_quick) + " " + t_current   );

                    //distance = (float)distancePointLine(point_, linePoint1, linePoint2);

                    //System.out.println("dist3: " + distance);
                    System.out.println(point[0] + " " + point[1] + " " + point[2]);
                    System.out.println("-----------------------------------");

                }

                if(!debug)
                    if(n_points_in_voxel > (1) && penetration >= minPenetration){

                        double length = getLengthInCurrentVoxel(t_max_x, t_max_y, t_max_z, t_d_x, t_d_y, t_d_z, t_current, current_x, current_y, current_z, step_x,
                                step_y, step_z, delta_t_last);

                        if(length > length_threshold) {

                            length *= resolution;

                            x_offset = (float) chm_values_mean_x[(int) current_y][(int) current_x][(int) current_z] / 1000.0f;
                            y_offset = (float) chm_values_mean_y[(int) current_y][(int) current_x][(int) current_z] / 1000.0f;
                            z_offset = (float) chm_values_mean_z[(int) current_y][(int) current_x][(int) current_z] / 1000.0f;

                            //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                            //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                            // point.crossProduct(point);
                            //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                            //        (float)-current_y+y_offset-(-center_of_pixel_y),
                            //        (float)current_z+z_offset-center_of_pixel_z);

                            point_quick.set((float) current_x + x_offset - center_of_pixel_x,
                                    (float) -current_y + y_offset - (-center_of_pixel_y),
                                    (float) current_z + z_offset - center_of_pixel_z);

                            //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());


                            point_quick.cross(point_quick, line_quick);


                            float distance = (float) (point_quick.norm() / line_quick.norm());


                            point[0] = (float) current_x + x_offset;
                            point[1] = (float) -current_y + y_offset;
                            point[2] = (float) current_z + z_offset;

                            distance = betweenPointAndLine(point, lineStart, lineEnd);


                            //System.out.println("dist_90: " + distance);
                            //float distance = 0;
                            //System.out.println(distance + " == " + distance2);
                            //System.out.println(distance);

                            //System.out.println("length: " + length + " " + (length / resolution));

                            //System.exit(1);


                            if (distance < distance_threshold) {

                                if (rM.vox_[current_x][current_y][current_z].isSolid)
                                    return 0.0;

                                // System.out.println("BLOCKED!");

                                //if(rM.vox_[current_x][current_y][current_z].flatness < 0.20){
                                //    return 0.0;
                                //}

                                float neg = 1.0f - rM.vox_[current_x][current_y][current_z].flatness;

                                //dampening -= dampening_factor_per_voxel; // * neg;
                                dampening -= dampening_factor_per_meter * length; // * neg;
                            }
                        }
                    }

                delta_t_last = t_current;

                if(dampening <= 0.0)
                    return dampening;

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

            float t_d_x = (float) (resolution / cosAngle / sinAngle2);
            float t_d_y = (float) (resolution / sinAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);


            t_d_x = (float) (resolution / cosAngle / cosAngle2);
            t_d_y = (float) (resolution / sinAngle / cosAngle2);
            t_d_z = (float) (resolution / sinAngle2);



            //Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);

            float[] y_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, x - 10000, (int)(-center_of_pixel_y-1), x + 10000, (int)(-center_of_pixel_y-1));
            float[] x_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x), -center_of_pixel_y + 10000, (int)(center_of_pixel_x), -center_of_pixel_y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z+1), center_of_pixel_x + 10000,  (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD!");
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z), center_of_pixel_x + 10000,  (int)(center_of_pixel_z));


            /*
            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;


             */
/*
            float x_ =  center_of_pixel_x - (float)Math.floor(center_of_pixel_x);
            float x_hypo = x_ / cosAngle2;
            float x_length = x_hypo / cosAngle;

            float y_ =  center_of_pixel_y - (float)Math.floor(center_of_pixel_y);
            float y_hypo = y_ / cosAngle2;
            float y_length = y_hypo / cosAngle;

*/
            float x_ = center_of_pixel_x - (float)Math.floor(center_of_pixel_x);
            float x_hypo = x_ / sinAngle;
            float x_length = x_hypo / cosAngle2;

            float y_ =  center_of_pixel_y - (float)Math.floor(center_of_pixel_y);
            float y_hypo = y_ / cosAngle;
            float y_length = y_hypo / cosAngle2;


            float z_;
            float z_hypo;
            float z_length;

            if(slope > 0) {
                z_ = (float) Math.ceil(center_of_pixel_z) - center_of_pixel_z;
                z_hypo = z_ / cosAngle2_2;
                z_length = z_hypo;
            }else{
                z_ = center_of_pixel_z - (float) Math.floor(center_of_pixel_z);
                z_hypo = z_ / cosAngle2_2;
                z_length = z_hypo;
            }




            //float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / cosAngle2;
            //float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / cosAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            //float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;
            float t_max_z = euclideanDistance(center_of_pixel_x, center_of_pixel_z, z_intersect[0], z_intersect[1]) / cosAngle;

            lineStart[0] = center_of_pixel_x;lineEnd[0] = outside_x;
            lineStart[1] = -center_of_pixel_y; lineEnd[1] = outside_y;
            lineStart[2] = center_of_pixel_z; lineEnd[2] = outside_z;

            t_max_x = x_length;
            t_max_y = y_length;
            t_max_z = z_length;


            //System.out.println(t_max_x);
            //System.out.println(t_max_y);
            //System.out.println(t_max_z);

            //System.exit(1);



            boolean breakki = false;

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }

                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    return dampening;
                }

                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    return dampening;


                if(current_z != z_vox)
                    n_points_in_voxel = chm_3d[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > 1)
                    penetration++;

                if(n_points_in_voxel > (1) && penetration >= minPenetration){

                    double length = getLengthInCurrentVoxel(t_max_x, t_max_y, t_max_z, t_d_x, t_d_y, t_d_z, t_current, current_x, current_y, current_z, step_x,
                            step_y, step_z, delta_t_last);

                    if(length > length_threshold) {

                        length *= resolution;

                        x_offset = (float) chm_values_mean_x[(int) current_y][(int) current_x][(int) current_z] / 1000.0f;
                        y_offset = (float) chm_values_mean_y[(int) current_y][(int) current_x][(int) current_z] / 1000.0f;
                        z_offset = (float) chm_values_mean_z[(int) current_y][(int) current_x][(int) current_z] / 1000.0f;

                        //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                        //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                        // point.crossProduct(point);
                        //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                        //        (float)-current_y+y_offset-(-center_of_pixel_y),
                        //        (float)current_z+z_offset-center_of_pixel_z);

                        point_quick.set((float) current_x + x_offset - center_of_pixel_x,
                                (float) -current_y + y_offset - (-center_of_pixel_y),
                                (float) current_z + z_offset - center_of_pixel_z);

                        //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());


                        point_quick.cross(point_quick, line_quick);


                        float distance = (float) (point_quick.norm() / line_quick.norm());


                        point[0] = (float) current_x + x_offset;
                        point[1] = (float) -current_y + y_offset;
                        point[2] = (float) current_z + z_offset;

                        distance = betweenPointAndLine(point, lineStart, lineEnd);


                        //System.out.println("dist_90: " + distance);
                        //float distance = 0;
                        //System.out.println(distance + " == " + distance2);
                        //System.out.println(distance);

                        //System.out.println("length: " + length + " " + (length / resolution));

                        //System.exit(1);


                        if (distance < distance_threshold) {

                            if (rM.vox_[current_x][current_y][current_z].isSolid)
                                return 0.0;

                            // System.out.println("BLOCKED!");

                            //if(rM.vox_[current_x][current_y][current_z].flatness < 0.20){
                            //    return 0.0;
                            //}

                            float neg = 1.0f - rM.vox_[current_x][current_y][current_z].flatness;

                            //dampening -= dampening_factor_per_voxel; // * neg;
                            dampening -= dampening_factor_per_meter * length; // * neg;
                        }
                    }
                }

                delta_t_last = t_current;

                if(dampening <= 0.0)
                    return dampening;

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


            float t_d_x = (float) (resolution / sinAngle / sinAngle2);
            float t_d_y = (float) (resolution / cosAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            t_d_x = (float) (resolution / sinAngle / cosAngle2);
            t_d_y = (float) (resolution / cosAngle / cosAngle2);
            t_d_z = (float) (resolution / sinAngle2);

            // Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);


            float[] y_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, x - 10000, (int)(-center_of_pixel_y), x + 10000, (int)(-center_of_pixel_y));
            //float[] y_intersect_z = lineIntersect(-center_of_pixel_y, center_of_pixel_z, outside_y, outside_z, z - 10000, (int)(-center_of_pixel_y), x + 10000, (int)(-center_of_pixel_y));
            float[] x_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x-1), -y + 10000, (int)(center_of_pixel_x-1), -y - 10000);


            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, x - 10000,  (int)(center_of_pixel_z+1), x + 10000, (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD!");
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, x - 10000, (int)(center_of_pixel_z), x + 10000,  (int)(center_of_pixel_z));


            //System.out.println(Arrays.toString(y_intersect));
            //System.out.println(Arrays.toString(x_intersect));

/*
            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;


 */


            float x_ =  center_of_pixel_x - (float)Math.floor(center_of_pixel_x);
            float x_hypo = x_ / sinAngle;
            float x_length = x_hypo / cosAngle;

            float y_ =  (float)Math.floor(center_of_pixel_y);
            float y_hypo = y_ / cosAngle;
            float y_length = y_hypo / cosAngle;


            float z_;
            float z_hypo;
            float z_length;

            if(slope > 0) {
                z_ = (float) Math.ceil(center_of_pixel_z) - center_of_pixel_z;
                z_hypo = z_ / cosAngle2_2;
                z_length = z_hypo;
            }else{
                z_ = center_of_pixel_z - (float) Math.floor(center_of_pixel_z);
                z_hypo = z_ / cosAngle2_2;
                z_length = z_hypo;
            }


            //float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / cosAngle2;
            //float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / cosAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            //float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;
            float t_max_z = euclideanDistance(center_of_pixel_x, center_of_pixel_z, z_intersect[0], z_intersect[1]) / cosAngle;

            lineStart[0] = center_of_pixel_x;lineEnd[0] = outside_x;
            lineStart[1] = -center_of_pixel_y; lineEnd[1] = outside_y;
            lineStart[2] = center_of_pixel_z; lineEnd[2] = outside_z;


            t_max_x = x_length;
            t_max_y = y_length;
            t_max_z = z_length;

            boolean breakki = false;

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {

                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }


                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    return dampening;
                }
                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    return dampening;


                if(current_z != z_vox)
                    n_points_in_voxel = chm_3d[(int)current_y][(int)current_x][(int)current_z];

                if(n_points_in_voxel > 1)
                    penetration++;

                if(n_points_in_voxel > (1) && penetration >= minPenetration){

                    double length = getLengthInCurrentVoxel(t_max_x, t_max_y, t_max_z, t_d_x, t_d_y, t_d_z, t_current, current_x, current_y, current_z, step_x,
                            step_y, step_z, delta_t_last);

                    if(length > length_threshold) {

                        length *= resolution;

                        x_offset = (float) chm_values_mean_x[(int) current_y][(int) current_x][(int) current_z] / 1000.0f;
                        y_offset = (float) chm_values_mean_y[(int) current_y][(int) current_x][(int) current_z] / 1000.0f;
                        z_offset = (float) chm_values_mean_z[(int) current_y][(int) current_x][(int) current_z] / 1000.0f;

                        //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                        //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                        // point.crossProduct(point);
                        //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                        //        (float)-current_y+y_offset-(-center_of_pixel_y),
                        //        (float)current_z+z_offset-center_of_pixel_z);

                        point_quick.set((float) current_x + x_offset - center_of_pixel_x,
                                (float) -current_y + y_offset - (-center_of_pixel_y),
                                (float) current_z + z_offset - center_of_pixel_z);

                        //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());


                        point_quick.cross(point_quick, line_quick);


                        float distance = (float) (point_quick.norm() / line_quick.norm());


                        point[0] = (float) current_x + x_offset;
                        point[1] = (float) -current_y + y_offset;
                        point[2] = (float) current_z + z_offset;

                        distance = betweenPointAndLine(point, lineStart, lineEnd);


                        //System.out.println("dist_90: " + distance);
                        //float distance = 0;
                        //System.out.println(distance + " == " + distance2);
                        //System.out.println(distance);

                        //System.out.println("length: " + length + " " + (length / resolution));

                        //System.exit(1);


                        if (distance < distance_threshold) {

                            if (rM.vox_[current_x][current_y][current_z].isSolid)
                                return 0.0;

                            // System.out.println("BLOCKED!");

                            //if(rM.vox_[current_x][current_y][current_z].flatness < 0.20){
                            //    return 0.0;
                            //}

                            float neg = 1.0f - rM.vox_[current_x][current_y][current_z].flatness;

                            //dampening -= dampening_factor_per_voxel; // * neg;
                            dampening -= dampening_factor_per_meter * length; // * neg;
                        }
                    }
                }

                delta_t_last = t_current;

                if(dampening <= 0.0)
                    return dampening;
            }



        }

        return dampening;

    }

    public double getLengthInCurrentVoxel(float t_max_x, float t_max_y, float t_max_z,
                                          float t_d_x, float t_d_y, float t_d_z,
                                          float t_current,
                                          float current_x, float current_y, float current_z,
                                          float step_x, float step_y, float step_z,
                                          float delta_t_last){

        delta_t_last = t_current;

        if (t_max_x < t_max_y) {
            if (t_max_x < t_max_z) {
                t_current = t_max_x;
                t_max_x = t_max_x + t_d_x;
                current_x += step_x;
            }else{
                t_current = t_max_z;
                t_max_z = t_max_z + t_d_z;
                current_z += step_z;
            }

        } else {
            if (t_max_y < t_max_z) {
                t_current = t_max_y;
                t_max_y = t_max_y + t_d_y;
                current_y += step_y;
            } else {
                t_current = t_max_z;
                t_max_z = t_max_z + t_d_z;
                current_z += step_z;
            }
        }

            return t_current - delta_t_last;
    }

    public static double distancePointLine(Point3d point, Point3d linePoint1, Point3d linePoint2) {
        // Vector from linePoint1 to linePoint2
        Vector3D lineVector = new Vector3D(linePoint2.x - linePoint1.x, linePoint2.y - linePoint1.y, linePoint2.z - linePoint1.z);
        // Vector from linePoint1 to point
        Vector3D pointVector = new Vector3D(point.x - linePoint1.x, point.y - linePoint1.y, point.z - linePoint1.z);

        // Find the projection of the pointVector onto the lineVector
        double projection = pointVector.dotProduct(lineVector) / lineVector.dotProduct(lineVector);

        // Find the point on the line that is closest to the point
        Point3d closestPoint;
        if (projection < 0) {
            // Closest point is linePoint1
            closestPoint = linePoint1;
        } else if (projection > 1) {
            // Closest point is linePoint2
            closestPoint = linePoint2;
        } else {
            // Closest point is on the line segment between linePoint1 and linePoint2
            double x = linePoint1.x + projection * lineVector.getX();
            double y = linePoint1.y + projection * lineVector.getY();
            double z = linePoint1.z + projection * lineVector.getZ();
            closestPoint = new Point3d(x, y, z);
        }

        // Calculate the distance between the point and the closest point on the line
        double dx = point.x - closestPoint.x;
        double dy = point.y - closestPoint.y;
        double dz = point.z - closestPoint.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        return distance;
    }

    public static float betweenPointAndLine(float[] point, float[] lineStart, float[] lineEnd){
        float[] PointThing = new float[3];
        float[] TotalThing = new float[3];
        PointThing[0] = lineStart[0] - point[0];
        PointThing[1] = lineStart[1] - point[1];
        PointThing[2] = lineStart[2] - point[2];

        TotalThing[0] = (PointThing[1]*lineEnd[2] - PointThing[2]*lineEnd[1]);
        TotalThing[1] = -(PointThing[0]*lineEnd[2] - PointThing[2]*lineEnd[0]);
        TotalThing[2] = (PointThing[0]*lineEnd[1] - PointThing[1]*lineEnd[0]);

        float distance = (float) (Math.sqrt(TotalThing[0]*TotalThing[0] + TotalThing[1]*TotalThing[1] + TotalThing[2]*TotalThing[2]) /
                Math.sqrt(lineEnd[0] * lineEnd[0] + lineEnd[1] * lineEnd[1] + lineEnd[2] * lineEnd[2] ));


        return distance;
    }

    public static double distance(double x1, double y1, double z1,
                                  double x2, double y2, double z2,
                                  double x3, double y3, double z3) {
        double b = Math.sqrt(Math.pow((x2 - x3), 2)
                + Math.pow((y2 - y3), 2)
                + Math.pow((z2 - z3), 2));

        double S = Math.sqrt(Math.pow((y2 - y1) * (z3 - z1) - (z2 - z1) * (y3 - y1), 2) +
                Math.pow((z2 - z1) * (x3 - x1) - (x2 - x1) * (z3 - z1), 2) +
                Math.pow((x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1), 2)) / 2;

        return 2 * S / b;
    }


    public double isBlocked_ray_trace_amapVox(int x, int y, int z_vox, byte z, double resolution, double zenith_angle, double slope,
                                                float direction_angle, int hour, float chm_max, solar3dManipulator rM){

        double dampening_factor_per_meter = 1.1;

        double dampening_factor_per_voxel = resolution * dampening_factor_per_meter;
        double dampening = 1.0;

        float transmittance = 1.0f;

        int penetration = 0;

        int minPenetration = 1;

        double distance_threshold = 100000;
        int current_x = x;
        int current_y = y;
        int current_z = z_vox;

        //System.out.println(y + " " + x + " " + z + " " + this.z_size );
        float x_offset = chm_values_mean_x[y][x][z_vox] / 1000.0f;
        float y_offset = chm_values_mean_y[y][x][z_vox] / 1000.0f;
        float z_offset = chm_values_mean_z[y][x][z_vox] / 1000.0f;

        Vector3d line_quick = new Vector3d(0,0,0);
        Vector3d point_quick = new Vector3d(0,0,0);

        float outside_x = 10000, outside_y = 10000, outside_z = 10000;

        int step_z;

        //double[] output = new double[]{0.0d, 0.0d};
        double increment_in_meters = resolution / 2.0;

        //float center_of_pixel_x = (float)x + 0.5f * (float)resolution;
        float center_of_pixel_x = (float)x + x_offset * (float)resolution;

        //float center_of_pixel_y = (float)y + 0.5f * (float)resolution;
        float center_of_pixel_y = (float)y + y_offset * (float)resolution;

        //float center_of_pixel_z = (float)z_vox + 0.5f * (float)resolution;
        float center_of_pixel_z = (float)z_vox + z_offset * (float)resolution;

        int right_x = (int)(center_of_pixel_x + 1);
        int left_x = (int)(center_of_pixel_x);

        int top_y = (int)(-center_of_pixel_y + 1);
        int bottom_y = (int)(center_of_pixel_x);

        //System.out.println(center_of_pixel_x + " " + center_of_pixel_y + " " + center_of_pixel_z);
        //System.out.println(x_offset + " " + y_offset + " " + z_offset);

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
        float cosAngle2 = 0;

        cosAngle2 = cos((FastMath.abs((float)zenith_angle)));
        sinAngle2 = sin((FastMath.abs((float)zenith_angle)));

        int step_x = -2, step_y = -2;
        byte n_points_in_voxel = 0;

        if(direction_angle < 90.0f){

            step_x = 1;
            step_y = -1;

            cosAngle = cos((90.0f - direction_angle));
            sinAngle = sin((90.0f - direction_angle));

            outside_y = -center_of_pixel_y + sinAngle * 10000;
            outside_x = center_of_pixel_x + cosAngle * 10000;

            line_quick.set(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);

            float t_d_x = (float) (resolution / cosAngle / sinAngle2);
            float t_d_y = (float) (resolution / sinAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);


            float[] y_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, center_of_pixel_x - 10000, (int)(-center_of_pixel_y), center_of_pixel_x + 10000, (int)(-center_of_pixel_y));
            float[] x_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x+1), -(float) center_of_pixel_y + 10000, (int)(center_of_pixel_x+1), -center_of_pixel_y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, x - 10000,  (int)(center_of_pixel_z+1), x + 10000,  (int)(center_of_pixel_z+1));
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, x - 10000,  (int)(center_of_pixel_z), x + 10000,  (int)(center_of_pixel_z));


            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;

            //System.out.println("DEG: " + FastMath.abs(center_of_pixel_z - z_intersect[1]));
            //System.out.println("t_max_x: " + t_max_x + " t_max_z: " + t_max_z + " t_max_y: " + t_max_y + " " + direction_angle + " " + zenith_angle);

            if(false)
                if(direction_angle > 85f){

                    System.out.println(t_max_x + " " + t_max_y + " " + t_max_z + " " + zenith_angle + " " + direction_angle);

                }

            boolean breakki = false;
            int counter = 0;
            /* RAY TRACE ITERATIONS */
            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }
                counter++;


                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    return transmittance;
                }

                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    return transmittance;


                if(current_z != z_vox) {
                    n_points_in_voxel = chm_3d[(int) current_y][(int) current_x][(int) current_z];
                    transmittance -= (1.0f - rM.aV.data[(int) current_x][(int) current_y][(int) current_z]);

                }
                if(n_points_in_voxel > 1)
                    penetration++;

                if(transmittance <= 0.0f){

                    return 0.0;

                }

                if(false)
                if(n_points_in_voxel > (1) && penetration >= minPenetration){

                    x_offset = (float)chm_values_mean_x[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    y_offset = (float)chm_values_mean_y[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                    z_offset = (float)chm_values_mean_z[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;

                    //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                    //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                    // point.crossProduct(point);
                    //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                    //        (float)-current_y+y_offset-(-center_of_pixel_y),
                    //        (float)current_z+z_offset-center_of_pixel_z);

                    point_quick.set((float)current_x+x_offset-center_of_pixel_x,
                            (float)-current_y+y_offset-(-center_of_pixel_y),
                            (float)current_z+z_offset-center_of_pixel_z);

                    //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());

                    point_quick.cross(point_quick, line_quick);
                    float distance = (float)(point_quick.norm() / line_quick.norm());
                    //float distance = 0;
                    //System.out.println(distance + " == " + distance2);
                    //System.out.println(distance);

                    if(distance < distance_threshold){
                        // System.out.println("BLOCKED!");
                        dampening -= dampening_factor_per_voxel;
                    }

                }
            }

        }

        else if(direction_angle < 180.0f){

            // System.out.println("HERE!!");
            step_x = 1;
            step_y = 1;

            cosAngle = cos((180.0f - direction_angle));
            sinAngle = sin((180.0f - direction_angle));


            outside_y = -center_of_pixel_y - cosAngle * 10000;
            outside_x = center_of_pixel_x + sinAngle * 10000;

            //Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);

            float t_d_x = (float) (resolution / sinAngle / sinAngle2);
            float t_d_y = (float) (resolution / cosAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            float[] y_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, center_of_pixel_x - 10000, (int)(-center_of_pixel_y-1), center_of_pixel_x + 10000, (int)(-center_of_pixel_y-1));
            float[] x_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x+1), -center_of_pixel_y + 10000, (int)(center_of_pixel_x+1), -center_of_pixel_y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect((float) center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z+1), center_of_pixel_x + 10000,  (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD BE HERE!");
            }else
                z_intersect = lineIntersect((float) center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z), center_of_pixel_x + 10000,  (int)(center_of_pixel_z));


            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;

            boolean breakki = false;

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }

                //System.out.println(this.rasterMaxValue + " " + (this.p_cloud_min_z + current_z * resolution));


                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    return transmittance;
                }

                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    return transmittance;


                if(current_z != z_vox) {
                    n_points_in_voxel = chm_3d[(int) current_y][(int) current_x][(int) current_z];
                    transmittance -= (1.0f - rM.aV.data[(int) current_x][(int) current_y][(int) current_z]);

                }
                if(n_points_in_voxel > 1)
                    penetration++;

                if(transmittance <= 0.0f){

                    return 0.0;

                }

                if(false)
                    if(n_points_in_voxel > (1) && penetration >= minPenetration){

                        x_offset = (float)chm_values_mean_x[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                        y_offset = (float)chm_values_mean_y[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                        z_offset = (float)chm_values_mean_z[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;

                        //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                        //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                        // point.crossProduct(point);
                        //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                        //        (float)-current_y+y_offset-(-center_of_pixel_y),
                        //        (float)current_z+z_offset-center_of_pixel_z);

                        point_quick.set((float)current_x+x_offset-center_of_pixel_x,
                                (float)-current_y+y_offset-(-center_of_pixel_y),
                                (float)current_z+z_offset-center_of_pixel_z);

                        //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());

                        point_quick.cross(point_quick, line_quick);
                        float distance = (float)(point_quick.norm() / line_quick.norm());
                        //float distance = 0;
                        //System.out.println(distance + " == " + distance2);
                        //System.out.println(distance);

                        if(distance < distance_threshold){
                            // System.out.println("BLOCKED!");
                            dampening -= dampening_factor_per_voxel;
                        }

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

            float t_d_x = (float) (resolution / cosAngle / sinAngle2);
            float t_d_y = (float) (resolution / sinAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            //Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);

            float[] y_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, x - 10000, (int)(-center_of_pixel_y-1), x + 10000, (int)(-center_of_pixel_y-1));
            float[] x_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x), -center_of_pixel_y + 10000, (int)(center_of_pixel_x), -center_of_pixel_y - 10000);

            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z+1), center_of_pixel_x + 10000,  (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD!");
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, center_of_pixel_x - 10000,  (int)(center_of_pixel_z), center_of_pixel_x + 10000,  (int)(center_of_pixel_z));


            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;

            boolean breakki = false;

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {
                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }

                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    return transmittance;
                }

                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    return transmittance;


                if(current_z != z_vox) {
                    n_points_in_voxel = chm_3d[(int) current_y][(int) current_x][(int) current_z];
                    transmittance -= (1.0f - rM.aV.data[(int) current_x][(int) current_y][(int) current_z]);

                }
                if(n_points_in_voxel > 1)
                    penetration++;

                if(transmittance <= 0.0f){

                    return 0.0;

                }

                if(false)
                    if(n_points_in_voxel > (1) && penetration >= minPenetration){

                        x_offset = (float)chm_values_mean_x[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                        y_offset = (float)chm_values_mean_y[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                        z_offset = (float)chm_values_mean_z[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;

                        //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                        //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                        // point.crossProduct(point);
                        //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                        //        (float)-current_y+y_offset-(-center_of_pixel_y),
                        //        (float)current_z+z_offset-center_of_pixel_z);

                        point_quick.set((float)current_x+x_offset-center_of_pixel_x,
                                (float)-current_y+y_offset-(-center_of_pixel_y),
                                (float)current_z+z_offset-center_of_pixel_z);

                        //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());

                        point_quick.cross(point_quick, line_quick);
                        float distance = (float)(point_quick.norm() / line_quick.norm());
                        //float distance = 0;
                        //System.out.println(distance + " == " + distance2);
                        //System.out.println(distance);

                        if(distance < distance_threshold){
                            // System.out.println("BLOCKED!");
                            dampening -= dampening_factor_per_voxel;
                        }

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


            float t_d_x = (float) (resolution / sinAngle / sinAngle2);
            float t_d_y = (float) (resolution / cosAngle / sinAngle2);
            float t_d_z = (float) (resolution / cosAngle2);

            // Vector3D line = new Vector3D(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);
            line_quick.set(outside_x-center_of_pixel_x, outside_y-(-center_of_pixel_y), outside_z-center_of_pixel_z);


            float[] y_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, x - 10000, (int)(-center_of_pixel_y), x + 10000, (int)(-center_of_pixel_y));
            //float[] y_intersect_z = lineIntersect(-center_of_pixel_y, center_of_pixel_z, outside_y, outside_z, z - 10000, (int)(-center_of_pixel_y), x + 10000, (int)(-center_of_pixel_y));
            float[] x_intersect = lineIntersect(center_of_pixel_x, -center_of_pixel_y, outside_x, outside_y, (int)(center_of_pixel_x-1), -y + 10000, (int)(center_of_pixel_x-1), -y - 10000);


            float[] z_intersect;

            if(step_z > 0){
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, x - 10000,  (int)(center_of_pixel_z+1), x + 10000, (int)(center_of_pixel_z+1));
                //System.out.println("SHOULD!");
            }else
                z_intersect = lineIntersect(center_of_pixel_x, center_of_pixel_z, outside_x, outside_z, x - 10000, (int)(center_of_pixel_z), x + 10000,  (int)(center_of_pixel_z));


            //System.out.println(Arrays.toString(y_intersect));
            //System.out.println(Arrays.toString(x_intersect));


            float t_max_x = euclideanDistance(x_intersect[0], x_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            float t_max_y = euclideanDistance(y_intersect[0], y_intersect[1], center_of_pixel_x, -center_of_pixel_y) / sinAngle2;
            //float t_max_z = euclideanDistance(z_intersect[0], z_intersect[1], center_of_pixel_x, center_of_pixel_z) / sinAngle2;
            float t_max_z = FastMath.abs(center_of_pixel_z - z_intersect[1]) / cosAngle2;

            boolean breakki = false;

            while (true) {

                float t_current = -1;

                if (t_max_x < t_max_y) {

                    if (t_max_x < t_max_z) {
                        t_current = t_max_x;
                        t_max_x = t_max_x + t_d_x;
                        current_x += step_x;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                } else {
                    if (t_max_y < t_max_z) {
                        t_current = t_max_y;
                        t_max_y = t_max_y + t_d_y;
                        current_y += step_y;
                    }else{
                        t_current = t_max_z;
                        t_max_z = t_max_z + t_d_z;
                        current_z += step_z;
                    }

                }


                if(this.p_cloud_min_z + current_z * resolution > this.rasterMaxValue){
                    return transmittance;
                }

                if (current_x >= this.x_size || current_y >= this.y_size || current_x < 0 || current_y < 0 || current_z < 0 || current_z >= this.z_size)
                    return transmittance;


                if(current_z != z_vox) {
                    n_points_in_voxel = chm_3d[(int) current_y][(int) current_x][(int) current_z];
                    transmittance -= (1.0f - rM.aV.data[(int) current_x][(int) current_y][(int) current_z]);

                }
                if(n_points_in_voxel > 1)
                    penetration++;

                if(transmittance <= 0.0f){

                    return 0.0;

                }

                if(false)
                    if(n_points_in_voxel > (1) && penetration >= minPenetration){

                        x_offset = (float)chm_values_mean_x[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                        y_offset = (float)chm_values_mean_y[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;
                        z_offset = (float)chm_values_mean_z[(int)current_y][(int)current_x][(int)current_z] / 1000.0f;

                        //System.out.println(x_offset + " " + y_offset + " " + z_offset);

                        //Vector3D point = new Vector3D((float)current_x + x_offset,(float)current_y + y_offset,(float)current_z + z_offset);
                        // point.crossProduct(point);
                        //Vector3D point = new Vector3D((float)current_x+x_offset-center_of_pixel_x,
                        //        (float)-current_y+y_offset-(-center_of_pixel_y),
                        //        (float)current_z+z_offset-center_of_pixel_z);

                        point_quick.set((float)current_x+x_offset-center_of_pixel_x,
                                (float)-current_y+y_offset-(-center_of_pixel_y),
                                (float)current_z+z_offset-center_of_pixel_z);

                        //float distance = (float)(Vector3D.crossProduct(point, line).getNorm() / line.getNorm());

                        point_quick.cross(point_quick, line_quick);
                        float distance = (float)(point_quick.norm() / line_quick.norm());
                        //float distance = 0;
                        //System.out.println(distance + " == " + distance2);
                        //System.out.println(distance);

                        if(distance < distance_threshold){
                            // System.out.println("BLOCKED!");
                            dampening -= dampening_factor_per_voxel;
                        }

                    }

            }



        }

        return transmittance;

    }
    public double skyLight(){


        return 1.0;
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

    public float euclideanDistance(float x1, float y1, float x2, float y2){


        return (float)Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2) );

    }

    static float squareRoot(float n)
    {

        /*We are using n itself as
        initial approximation This
        can definitely be improved */
        float x = n;
        float y = 1;

        // e decides the accuracy level
        double e = 0.001;
        while (x - y > e) {
            x = (x + y) / 2;
            y = n / x;
        }
        return x;
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