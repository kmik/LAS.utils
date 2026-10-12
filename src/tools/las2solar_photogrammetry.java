package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;

import net.e175.klaus.solarpositioning.*;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.ogr.ogr;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

import utils.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.util.*;
import java.util.concurrent.BlockingQueue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.Pair;

/** Solar irradiation workflow. Requires the existing LAS.utils project dependencies.
 * The 3D traversal and classified-ground surface are implemented in SolarRayTracer.
 */
public class las2solar_photogrammetry {

    private static final Map<argumentReader,Integer> EXTRA_BYTES = new WeakHashMap<argumentReader,Integer>();
    private static synchronized int registerExtraBytes(argumentReader args) {
        Integer previous = EXTRA_BYTES.get(args);
        if (previous != null) return previous;
        int first = args.create_extra_byte_vlr_n_bytes.size();
        args.add_extra_bytes(6, "normal", "Surface normal");
        args.add_extra_bytes(6, "flatness", "Surface flatness");
        args.add_extra_bytes(6, "insolation", "Solar insolation");
        EXTRA_BYTES.put(args, first);
        return first;
    }
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

    final int[] months_to_use2 = new int[]{
            Calendar.JANUARY,
            Calendar.FEBRUARY,
            Calendar.MARCH,
            Calendar.APRIL,
            Calendar.MAY,
            Calendar.JUNE,
            Calendar.JULY,
            Calendar.AUGUST,
            Calendar.SEPTEMBER,
            Calendar.OCTOBER,
            Calendar.NOVEMBER,
            Calendar.DECEMBER
    };

    final int[] months_to_use = new int[]{

            Calendar.JUNE,
            Calendar.JULY,
            Calendar.AUGUST,

    };

    int year = 2025;
    float rasterMaxValue = Float.NEGATIVE_INFINITY;

    public las2solar_photogrammetry(String chm_name, argumentReader aR, LASReader pointCloud, boolean d3) throws Exception{

        this.year = aR.year;
        if (!(aR.step > 0) || !Double.isFinite(aR.step))
            throw new IllegalArgumentException("Voxel size must be positive and finite");
        final int groundClass = Integer.parseInt(System.getProperty("las2solar.groundClass", "2"));
        if (groundClass < 0 || groundClass > 255) throw new IllegalArgumentException("Invalid ground class");

        final int firstExtra = registerExtraBytes(aR);

        File outFileTxt = aR.createOutputFileWithExtension(pointCloud.getFile(), ".txt");

        BufferedWriter outWriter = new BufferedWriter(new FileWriter(outFileTxt), 1 << 16);
        try { 

        boolean amapVox_mode = false;

        if(aR.amapVoxFile != null){

            aV = new amapVox(aR.amapVoxFile);
            aV.readHeader();
            aV.readVoxels();
            amapVox_mode = true;

        }

        this.pointCloud = pointCloud;

        LasPoint tempPoint = new LasPoint();

        int thread_n = aR.pfac.addReadThread(pointCloud);

        int number_of_last_returns = 0;
        int number_of_all_returns = 0;

        int raster_z_size = Math.max(1, (int)Math.floor((pointCloud.getMaxZ() - pointCloud.getMinZ()) / aR.step) + 1);

        if(amapVox_mode){
            raster_z_size = aV.z_dim;
            checkVoxelSize(aV.max_x-aV.min_x, aV.x_dim, aR.step);
            checkVoxelSize(aV.max_y-aV.min_y, aV.y_dim, aR.step);
            checkVoxelSize(aV.max_z-aV.min_z, aV.z_dim, aR.step);
        }
        Dataset chm = gdal.Open(chm_name);
        if (chm == null) throw new IllegalArgumentException("Cannot open CHM: " + chm_name);
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
        byte[][][] nonGround = new byte[this_y_size][this_x_size][this_z_size];
        byte[][][] buildings = new byte[this_y_size][this_x_size][this_z_size];
        boolean[][][] hasGround = new boolean[this_y_size][this_x_size][this_z_size];
        SolarRayTracer.TerrainBuilder terrainBuilder = new SolarRayTracer.TerrainBuilder(this_x_size, this_y_size);

        outWriter.newLine();

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

                int x = gridIndex((tempPoint.x - this_min_x) / aR.step, this_x_size);
                int y = gridIndex((this_max_y - tempPoint.y) / aR.step, this_y_size);
                int z = gridIndex((tempPoint.z - this_min_z) / aR.step, this_z_size);

                if(vox_[x][y][z] == null){
                    vox_[x][y][z] = new VoxelNeighborhood(x, y, z);
                }

                vox_[x][y][z].addObservable();

                double fraction_x = ((tempPoint.x - this_min_x) / aR.step) - x;
                double fraction_y = ((this_max_y - tempPoint.y) / aR.step) - y;
                double fraction_z = ((tempPoint.z - this_min_z) / aR.step) - z;

                short frac_x = encodeFraction(fraction_x);
                short frac_y = encodeFraction(fraction_y);
                short frac_z = encodeFraction(fraction_z);
                if (tempPoint.classification == groundClass) {
                    hasGround[y][x][z] = true;
                    terrainBuilder.add(x, y, (tempPoint.x-this_min_x)/aR.step,
                            (this_max_y-tempPoint.y)/aR.step, (tempPoint.z-this_min_z)/aR.step);
                } else if (nonGround[y][x][z] < 127) nonGround[y][x][z]++;
                if (tempPoint.classification == 6 && buildings[y][x][z] < 127) buildings[y][x][z]++;

                if(z > maxValueInChm[y][x]){
                    maxValueInChm[y][x] = z;
                }

                if(chm_values_f_3d[y][x][z] < 127) {

                    chm_values_f_3d[y][x][z]++;

                    chm_values_mean_x[y][x][z] = (short)Math.round(chm_values_mean_x[y][x][z] + (frac_x - chm_values_mean_x[y][x][z]) / (double) chm_values_f_3d[y][x][z]);

                    chm_values_mean_y[y][x][z] = (short)Math.round(chm_values_mean_y[y][x][z] + (frac_y - chm_values_mean_y[y][x][z]) / (double) chm_values_f_3d[y][x][z]);

                    chm_values_mean_z[y][x][z] = (short)Math.round(chm_values_mean_z[y][x][z] + (frac_z - chm_values_mean_z[y][x][z]) / (double) chm_values_f_3d[y][x][z]);

                }
            }
        }

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

                    int x = gridIndex((tempPoint.x - this_min_x) / aR.step, this_x_size);
                    int y = gridIndex((this_max_y - tempPoint.y) / aR.step, this_y_size);
                    int z = gridIndex((tempPoint.z - this_min_z) / aR.step, this_z_size);

                    if(x < 0 || x >= this_x_size || y < 0 || y >= this_y_size || z < 0 || z >= this_z_size)
                        continue;

                    vox_[x][y][z].addPoint(tempPoint.x, tempPoint.y, tempPoint.z, i+j);

                    if(tempPoint.classification == 6){

                        vox_[x][y][z].n_solid_points++;

                    }

                }
            }

        ogr.RegisterAll(); 
        gdal.AllRegister();

        Driver driver = null;
        driver = gdal.GetDriverByName("GTiff");
        driver.Register();

        SolarRayTracer.Terrain terrain = terrainBuilder.pointCount() == 0 ? null : terrainBuilder.build();
        if (terrain == null && !amapVox_mode)
            System.err.println("No classified ground points found. Terrain self-shadow correction is unavailable; "
                    + "classify ground or set -Dlas2solar.groundClass=<class>.");
        terrainBuilder = null;
        int steppi = 10;

        int x_res = (int)Math.ceil((double)this_x_size / (double)steppi);
        int y_res = (int)Math.ceil((double)this_y_size / (double)steppi);

        int[] minutes = new int[]{0};        
        float[][][][][][][] precomputed = new float[x_res][y_res][12][32][24][minutes.length][2];

        SpatialReference src = new SpatialReference();
        SpatialReference dst = new SpatialReference();

        dst.ImportFromProj4("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
        src.ImportFromProj4("+proj=utm +zone=35 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs");

        CoordinateTransformation ct = new CoordinateTransformation(src, dst);

        double[] gt = chm.GetGeoTransform();

        GregorianCalendar time = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));

        this.chm_values = chm.GetRasterBand(1);

        chm_values_f = chm_to_array(chm_values);
        int x_ = this_x_size;
        int y_ = this_y_size;
        chm_output_f = new float[1][1];

        int[] calendar_month = new int[]{Calendar.JUNE, Calendar.JULY, Calendar.AUGUST};

        calendar_month = months_to_use;

        int[] n_date_in_month = las2solar_photogrammetry.daysInMonths(aR.year);

        int x_size = this_x_size;
        int y_size = this_y_size;

        float[] raster_read = new float[x_size];
        float value = 0;

        double[] point = new double[3];

        ArrayList<int[]> sunriseAndSunset = new ArrayList<>();

        double[] blockArray = new double[]{0.0,0.0};

        double angleThreshold = aR.angle;

        if(angleThreshold == -999)
            angleThreshold = 45;

        solar3dManipulator rM = new solar3dManipulator(this_x_size, this_y_size, raster_z_size, new float[this_y_size][this_x_size][raster_z_size], aR.step, maxValueInChm, angleThreshold);
        rM.setContains_points(chm_values_f_3d);

        rM.setVox_(vox_);

        if(aR.amapVoxFile != null){

            rM.setAmapVox(aV);

        }

        int n_funk_per_thread = (int)Math.ceil((double)y_size / (double)aR.cores);

        sunriseAndSunset.clear();

        IntStream.range(0, x_res).parallel().forEach(x -> {

            GregorianCalendar time_ = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));

            double[] point_ = new double[3];
            for(int y = 0; y < y_res; y++){

                int precompute_x = (int)((double)steppi * (double)x + (double)steppi / 2.0); 
                int precompute_y = (int)((double)steppi * (double)y + (double)steppi / 2.0); 

                point_[0] = this_min_x + Math.min(this_x_size - 0.5, precompute_x) * aR.step;
                point_[1] = this_max_y - Math.min(this_y_size - 0.5, precompute_y) * aR.step;
                point_[2] = value;

                double[] point_transformed;
                synchronized (ct) { point_transformed = ct.TransformPoint(point_[0], point_[1]); }

                for(int month : months_to_use) {
                    for (int day = 1; day <= n_date_in_month[month]; day += 1) {

                        for (int hour = 0; hour <= 23; hour += 1) {

                            for(int minute = 0; minute < minutes.length; minute++) {

                                time_.set(year, month, day, hour, minutes[minute], 00);

                                AzimuthZenithAngle result = SPA.calculateSolarPosition(time_, point_transformed[1], point_transformed[0], 0, DeltaT.estimate(time_));

                                precomputed[x][y][month][day][hour][minute][0] =  (float)result.getZenithAngle();
                                precomputed[x][y][month][day][hour][minute][1] =  (float)result.getAzimuth();

                            }
                        }
                    }
                }
            }
        });

        long start = System.currentTimeMillis();

        BlockingQueue<Pair<Integer, byte[][]>> provideRow = new LinkedBlockingQueue<>();

        for(int y = 0; y < chm_values_f_3d.length; y++)
            provideRow.add(Pair.of(y,chm_values_f_3d[y]));

        int cores = Math.max(1, Math.min(aR.cores, y_size));
        Thread[] threads = new Thread[cores];

        threadSafeWriteToFile threadWriter = new threadSafeWriteToFile(outWriter, Math.addExact(Math.multiplyExact(Math.multiplyExact(x_size, y_size), raster_z_size), 1));

        if(!aR.output_statistics)
            threadWriter = null;

        for (int i = 0; i < cores; i++) {

            int mini = i * n_funk_per_thread;
            int maxi = Math.min(y_size, mini + n_funk_per_thread);

            threads[i] = (new solarParallel_3d(mini, maxi, x_size,y_size, raster_z_size, rM, sunriseAndSunset, gt, ct, aR, chm_values_f, chm_output_f, precomputed, steppi, rasterMaxValue, provideRow, chm_values_f_3d,
                    pointCloud, chm_values_mean_x, chm_values_mean_y, chm_values_mean_z, threadWriter,
                    this_min_x, this_max_y, this_min_z, months_to_use, year));
            ((solarParallel_3d)threads[i]).setTerrain(terrain, nonGround, hasGround, buildings);
            threads[i].start();

        }

        joinWorkers(threads);

        if(threadWriter != null)
            threadWriter.close();
        for (Thread worker : threads) {
            RuntimeException failure = ((solarParallel_3d)worker).getFailure();
            if (failure != null) throw failure;
        }

        File outFile = aR.createOutputFile(pointCloud);

        pointWriterMultiThread pw = new pointWriterMultiThread(outFile, pointCloud, "las2las", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);

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

                float[] idw_value = new float[]{(float)rM.getValue(x, y, z), tempPoint.pointSourceId};

                tempPoint.intensity =  (int)(idw_value[0] / solarradiation * 65535.0);

                tempPoint.setExtraByteINT((int)(vox_[x][y][z].normal / 180.0f * 65535.0), aR.create_extra_byte_vlr_n_bytes.get(firstExtra + 0), firstExtra + 0);

                tempPoint.setExtraByteINT((int)(vox_[x][y][z].flatness / 1.0f * 65535.0), aR.create_extra_byte_vlr_n_bytes.get(firstExtra + 1), firstExtra + 1);

                tempPoint.setExtraByteINT((int)(idw_value[0] / solarradiation * 65535.0), aR.create_extra_byte_vlr_n_bytes.get(firstExtra + 2), firstExtra + 2);

                tempPoint.pointSourceId =  (short) (idw_value[1]);

                if(vox_[x][y][z].normal < min_angle)
                    min_angle = vox_[x][y][z].normal;

                if(vox_[x][y][z].flatness < min_flatness)
                    min_flatness = vox_[x][y][z].flatness;

                if(vox_[x][y][z].normal > max_angle)
                    max_angle = vox_[x][y][z].normal;

                if(vox_[x][y][z].flatness > max_flatness)
                    max_flatness = vox_[x][y][z].flatness;

                try {

                    aR.pfac.writePoint(tempPoint, i + j, thread_n);

                }catch (Exception e){ throw new IllegalStateException("Failed to write LAS point", e); }
            }
        }

        System.out.println("min_angle: " + min_angle + " max_angle: " + max_angle);
        System.out.println("min_flatness: " + min_flatness + " max_flatness: " + max_flatness);

        aR.pfac.closeThread(thread_n);

        long elapsed = System.currentTimeMillis() - start;

        long seconds = elapsed / 1000;
        long hours = seconds / 3600;
        long minutes_ = (seconds % 3600) / 60;
        long secs = seconds % 60;

        System.out.printf(
                "processing took: %02d:%02d:%02d with %d threads%n",
                hours, minutes_, secs, aR.cores
        );

        chm.FlushCache();

            } finally { outWriter.close(); }
    }

    public las2solar_photogrammetry(String chm_name, argumentReader aR, LASReader pointCloud,
            boolean d3, boolean this_is_binary) throws Exception {
        this(chm_name, aR, pointCloud, d3);
    }

    public las2solar_photogrammetry(String chm_name, argumentReader aR, LASReader pointCloud){
        this.year = aR.year;

        this.pointCloud = pointCloud;

        System.out.println(this.pointCloud.getMaxZ());

        ogr.RegisterAll(); 

        gdal.AllRegister();

        System.out.println("max cache: " + gdal.GetCacheMax());

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
        band2 = dataset.GetRasterBand(1);    

        int steppi = 100;

        int x_res = (int)Math.ceil((double)chm.getRasterXSize() / (double)steppi);
        int y_res = (int)Math.ceil((double)chm.getRasterYSize() / (double)steppi);

        int[] minutes = new int[]{0, 20, 40};        
        float[][][][][][][] precomputed = new float[x_res][y_res][12][32][24][minutes.length][2];

        SpatialReference src = new SpatialReference();
        SpatialReference dst = new SpatialReference();

        dst.ImportFromProj4("+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs");
        src.ImportFromProj4("+proj=utm +zone=35 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs ");

        CoordinateTransformation ct = new CoordinateTransformation(src, dst);

        double[] gt = chm.GetGeoTransform();

        GregorianCalendar time = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));

        System.out.println(time.getTimeZone());

        this.chm_values = chm.GetRasterBand(1);

        chm_values_f = chm_to_array(chm_values);
        int x_ = chm_values.getXSize();
        int y_ = chm_values.getYSize();
        chm_output_f = new float[1][1];

        int[] calendar_month = new int[]{Calendar.JUNE, Calendar.JULY, Calendar.AUGUST};
        calendar_month = months_to_use;

        int[] n_date_in_month = las2solar_photogrammetry.daysInMonths(aR.year);

        int x_size = chm_values.getXSize();
        int y_size = chm_values.getYSize();

        float[] raster_read = new float[x_size];
        float value = 0;

        double[] point = new double[3];
        double[] point_transformed = new double[2];

        ArrayList<int[]> sunriseAndSunset = new ArrayList<>();
        double[] blockArray = new double[]{0.0,0.0};

        rasterManipulator rM = new rasterManipulator(band2);

        Thread[] threads = new Thread[aR.cores];
        int n_funk_per_thread = (int)Math.ceil((double)y_size / (double)aR.cores);

        sunriseAndSunset.clear();

        for(int x = 0; x < x_res; x++){
            for(int y = 0; y < y_res; y++){

                int precompute_x = (int)((double)steppi * (double)x + (double)steppi / 2.0); 
                int precompute_y = (int)((double)steppi * (double)y + (double)steppi / 2.0); 

                point[0] = gt[0] + precompute_x * gt[1] + precompute_y * gt[2];
                point[1] = gt[3] + precompute_x * gt[4] + precompute_y * gt[5];
                point[2] = value;

                point_transformed = ct.TransformPoint(point[0], point[1]);

                for(int month : calendar_month) {
                    for (int day = 1; day <= n_date_in_month[month]; day += 1) {

                        int sunriseSunsetcounter = 0;

                        time.set(year, month, day, 12, 00, 00);

                        for (int hour = 0; hour <= 23; hour += 1) {

                            for(int minute = 0; minute < minutes.length; minute++) {

                                time.set(year, month, day, hour, minutes[minute], 00);

                                AzimuthZenithAngle result = SPA.calculateSolarPosition(time, point_transformed[1], point_transformed[0], 127, DeltaT.estimate(time));

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

        for(int y = 0; y < chm_values_f.length; y++)
            provideRow.add(Pair.of(y,chm_values_f[y]));

        for (int i = 0; i < aR.cores; i++) {

            int mini = i * n_funk_per_thread;
            int maxi = Math.min(y_size, mini + n_funk_per_thread);

            threads[i] = (new solarParallel(mini, maxi, x_size,y_size, rM, sunriseAndSunset, gt, ct, aR,
                    chm_values_f, chm_output_f, precomputed, steppi, rasterMaxValue, provideRow, months_to_use));
            threads[i].start();

        }

        joinWorkers(threads);
        for (Thread worker : threads) {
            RuntimeException failure = ((solarParallel)worker).getFailure();
            if (failure != null) throw failure;
        }

        long elapsed = System.currentTimeMillis() - start;

        long hours = elapsed / 3_600_000;
        long minutes_ = (elapsed % 3_600_000) / 60_000;
        long seconds = (elapsed % 60_000) / 1_000;
        long ms = elapsed % 1_000;

        System.out.printf(
                "processing took: %02d:%02d:%02d.%03d with %d threads%n",
                hours, minutes_, seconds, ms, aR.cores
        );

        dataset.FlushCache();
        chm.FlushCache();

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

        while(sun_vector_z < this.pointCloud.getMaxZ()){

            current_distance_from_point += increment_in_meters;

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

    private static void joinWorkers(Thread[] workers) {
        boolean interrupted=false;
        for (Thread worker:workers) {
            while (worker.isAlive()) {
                try { worker.join(); } catch (InterruptedException e) { interrupted=true; }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for solar calculations");
        }
    }

    private static int gridIndex(double coordinate, int size) {
        if (!Double.isFinite(coordinate) || coordinate < -1e-8 || coordinate > size+1e-8)
            throw new IllegalArgumentException("Point outside voxel grid: " + coordinate);
        return Math.max(0, Math.min(size-1, (int)Math.floor(coordinate)));
    }
    private static short encodeFraction(double fraction) {
        return (short)Math.round(Math.max(0,Math.min(0.999,fraction))*1000.0);
    }
    private static void checkVoxelSize(double extent, int count, double step) {
        if (count<1 || Math.abs(extent/count-step)>1e-6*Math.max(1,step))
            throw new IllegalArgumentException("AMAPVox grid spacing does not match the requested voxel size");
    }
    public static int[] daysInMonths(int year) {
        int[] days={31,28,31,30,31,30,31,31,30,31,30,31};
        if (new GregorianCalendar().isLeapYear(year)) days[1]=29;
        return days;
    }

    public float[][] chm_to_array(Band chm) {

        int x_ = chm.getXSize();
        int y_ = chm.getYSize();

        float[][] output = new float[y_][x_];

        float[] raster_read = new float[x_];

        for (int y = 0; y < y_; y++) {

            chm.ReadRaster(0, y, x_, 1, raster_read);
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

        float[] raster_read = new float[x_];

        for (int y = 0; y < y_; y++) {

            for (int x = 0; x <x_; x++) {
                raster_read[x] = array[y][x];
            }

            chm.WriteRaster(0, y, x_, 1, raster_read);

        }
    }

    static final int precision = 100; 

    static final int modulus = 360*precision;
    static final float[] sin = new float[modulus]; 
    static {

        for (int i = 0; i<sin.length; i++) {
            sin[i]=(float)Math.sin((i*Math.PI)/(precision*180));
        }
    }

    private static float sinLookup(int a) {
        return a>=0 ? sin[a%(modulus)] : -sin[-a%(modulus)];
    }

    public static float sin(float a) {
        return sinLookup((int)(a * precision + 0.5f));
    }
    public static float cos(float a) {
        return sinLookup((int)((a+90f) * precision + 0.5f));
    }

    public double HourToHourAngle(double hourFromMidnight){

        double hourAngle = Math.toRadians(15*(hourFromMidnight - 12));
        return hourAngle;

    }

    public double SolarDeclinaton(int dayOfYear){

        double declination = Math.toRadians(23.45) * (Math.sin((Math.toRadians(360)/365)*(dayOfYear)));
        return declination;

    }

}

class solarParallel extends Thread {
    private volatile RuntimeException failure;
    public RuntimeException getFailure() { return failure; }

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

    int[] months_to_use;
    BlockingQueue<Pair<Integer, float[]>> providerRow;

    public solarParallel(int min, int max, int x_size, int y_size, rasterManipulator rM, ArrayList<int[]> sunriseAndSunset,
                         double[] gt, CoordinateTransformation ct, argumentReader aR, float[][] chm, float[][] chm_output,
                         float[][][][][][][] precomputed, int precomputed_resolution, float rasterMaxValue, BlockingQueue<Pair<Integer, float[]>> providerRow,
                         int[] months_to_use) {

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
        this.months_to_use = months_to_use;

    }

    @Override
    public void run() {
        try {

        float value;
        double[] point = new double[3];
        double[] point_transformed = new double[3];

        int[] calendar_month = months_to_use;
        int[] n_date_in_month = las2solar_photogrammetry.daysInMonths(aR.year);
        int[] minutes = new int[]{0, 20, 40};
        int solarradiation = 1366;

        boolean debug = false;

        while(true){

            Pair<Integer, float[]> row = providerRow.poll();
            if (row == null) break;

            float[] averageInsol = new float[x_size];

            for (int x = 0; x < x_size; x++) {

                int precomputed_x = (int)Math.floor((double)x / (double)precomputed_resolution);
                int precomputed_y = (int)Math.floor((double)row.getKey() / (double)precomputed_resolution);

                value = row.getValue()[x];

                if(value <= 0)
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
                for(int month : calendar_month){

                    for(int day = 1; day <= n_date_in_month[month]; day += 5){

                        dailyAverageInsolation_ = 0.0;

                        minuteSum = 0;

                        for(int hour = 0; hour <= 23; hour += 1) {

                            n_ = 0;

                            hour_mean_insolation = 0;

                            for (int minute = 0; minute < minutes.length; minute += 1) {

                                float[] precomp = precomputed[precomputed_x][precomputed_y][month][day][hour][minute];

                                if ((precomp[0] > 80.0f)) 
                                    continue;

                                double insolation = (double) solarradiation * cos((precomp[0]));

                                minuteSum += (minutes[1]-minutes[0]);

                                double slope = Math.tan(Math.toRadians(90.0f-precomp[0]));

                                double[] blockArray = isBlocked(x, row.getKey(), value, aR.step, (precomp[0]), slope, precomp[1], hour);

                                if (blockArray[0] > 0) {

                                    if ((blockArray[0] / blockArray[1]) < 0.5 && false) {

                                        hour_mean_insolation += insolation;

                                    }

                                } else {

                                    hour_mean_insolation += insolation;

                                }

                                if (blockArray[0] == 0) {

                                }
                            }

                            dailyAverageInsolation_ += hour_mean_insolation / (double)minutes.length;
                        }

                        dailyAverageInsolation = dailyAverageInsolation + ((dailyAverageInsolation_ / 24.0) - dailyAverageInsolation)/(double)++n;
                    }
                }

                averageInsol[x] = (float)dailyAverageInsolation;

            }

            rM.setValue(row.getKey(),averageInsol);

        }
            } catch (RuntimeException e) { failure=e; providerRow.clear(); }
    }

    public double[] isBlocked(int x, int y, float z, double resolution, double zenith_angle, double slope, float direction_angle, int hour){

        double tolerance = 0.0;

        double[] output = new double[]{0.0d, 0.0d};
        double increment_in_meters = resolution / 2.0;

        double current_distance_from_point = resolution - increment_in_meters;

        double center_of_pixel_x = (double)x + 0.5 * resolution;
        double center_of_pixel_y = (double)y + 0.5 * resolution;

        double x_prev = center_of_pixel_x;
        double y_prev = center_of_pixel_y;

        double x_ = center_of_pixel_x,y_ = center_of_pixel_y;

        double start_z = z;

        double sun_vector_z = z;

        float cosAngle = 0;

        float sinAngle = 0;

        int quadrant = -1;

        if(direction_angle < 90.0f){
            quadrant = 1;

            cosAngle = cos((90.0f - direction_angle));
            sinAngle = sin((90.0f - direction_angle));

        }else if(direction_angle < 180.0f){
            quadrant = 2;

            cosAngle = cos((180.0f - direction_angle));
            sinAngle = sin((180.0f - direction_angle));

        }else if(direction_angle < 270.0f){
            quadrant = 3;

            cosAngle = cos((270.0f - direction_angle));
            sinAngle = sin((270.0f - direction_angle));

        }else if(direction_angle < 360.0f){
            quadrant = 4;

            cosAngle = cos((360.0f - direction_angle));
            sinAngle = sin((360.0f - direction_angle));

        }

        int counter = 0;

        while(sun_vector_z < this.rasterMaxValue){

            current_distance_from_point += increment_in_meters;

            switch (quadrant){

                case 1:

                    y_ = y - sinAngle * current_distance_from_point;
                    x_ = x + cosAngle * current_distance_from_point;

                    break;

                case 2:
                    y_ = y + cosAngle * current_distance_from_point;
                    x_ = x + sinAngle * current_distance_from_point;

                    break;

                case 3:
                    y_ = y + sinAngle * current_distance_from_point;
                    x_ = x - cosAngle * current_distance_from_point;

                    break;

                case 4:
                    y_ = y - cosAngle * current_distance_from_point;
                    x_ = x - sinAngle * current_distance_from_point;

                    break;

                default:
                    break;

            }

            if(((int)x_ == (int)x_prev && (int)y_ == (int)y_prev) || ((int)x_ == x && (int)y_ == y)){
                continue;
            }

            double z_sun_ray = start_z + current_distance_from_point * slope;

            sun_vector_z = z_sun_ray;

            if(x_ >= this.x_size || x_ < 0 || y_ < 0 || y_ >= this.y_size) {
                break;
            }

            float new_z = chm[(int)y_][(int)x_];

            if(new_z > (z_sun_ray + tolerance)){

                output[0] += (new_z - z_sun_ray);
                output[1] += increment_in_meters;
                return output;

            }

            x_prev = x_;
            y_prev = y_;

        }

        return output;

    }

    static final int precision = 100; 

    static final int modulus = 360*precision;
    static final float[] sin = new float[modulus]; 
    static final float[] cos = new float[modulus]; 
    static {

        for (int i = 0; i<sin.length; i++) {
            sin[i]=(float)Math.sin((i*Math.PI)/(precision*180));
        }

        for (int i = 0; i<cos.length; i++) {
            cos[i]=(float)Math.cos((i*Math.PI)/(precision*180));
        }

    }

    private static float sinLookup(int a) {
        return a>=0 ? sin[a%(modulus)] : -sin[-a%(modulus)];
    }

    private static float cosLookup(int a) {
        return a>=0 ? cos[a%(modulus)] : -cos[-a%(modulus)];
    }

    public static float sin(float a) {
        return sinLookup((int)(a * precision + 0.5f));
    }
    public static float cos(float a) {
        return sinLookup((int)((a+90f) * precision + 0.5f));
    }

    public double HourToHourAngle(double hourFromMidnight){

        double hourAngle = Math.toRadians(15*(hourFromMidnight - 12));
        return hourAngle;

    }

    public double SolarDeclinaton(int dayOfYear){

        double declination = Math.toRadians(23.45) * (Math.sin((Math.toRadians(360)/365)*(dayOfYear)));
        return declination;

    }
}

class solarParallel_3d extends Thread {
    private final int x_size, y_size, z_size, year, precomputed_resolution;
    private final solar3dManipulator rM;
    private final argumentReader aR;
    private final byte[][][] occupancy;
    private final short[][][] meanX, meanY, meanZ;
    private final float[][][][][][][] precomputed;
    private final BlockingQueue<Pair<Integer, byte[][]>> rows;
    private final threadSafeWriteToFile writer;
    private final double minx, maxy, minz;
    private final int[] months;
    private byte[][][] nonGround, building;
    private boolean[][][] hasGround;
    private SolarRayTracer.Terrain terrain;
    private volatile RuntimeException failure;
    private final double extinction;

    public solarParallel_3d(int min, int max, int xSize, int ySize, int zSize,
            solar3dManipulator manipulator, ArrayList<int[]> sunriseAndSunset,
            double[] gt, CoordinateTransformation ct, argumentReader args, float[][] chm,
            float[][] output, float[][][][][][][] sun, int sunStep, float rasterMax,
            BlockingQueue<Pair<Integer, byte[][]>> provider, byte[][][] occupied,
            LASReader cloud, short[][][] mx, short[][][] my, short[][][] mz,
            threadSafeWriteToFile textWriter, double minX, double maxY, double minZ,
            int[] selectedMonths, int selectedYear) {
        x_size=xSize; y_size=ySize; z_size=zSize; rM=manipulator; aR=args;
        precomputed=sun; precomputed_resolution=sunStep; rows=provider;
        occupancy=occupied; meanX=mx; meanY=my; meanZ=mz; writer=textWriter;
        minx=minX; maxy=maxY; minz=minZ; months=selectedMonths; year=selectedYear;
        extinction=Double.parseDouble(System.getProperty("las2solar.extinctionPerMetre", "0.33"));
        if (!Double.isFinite(extinction) || extinction < 0)
            throw new IllegalArgumentException("Extinction must be finite and nonnegative");
    }

    public void setTerrain(SolarRayTracer.Terrain ground, byte[][][] vegetation,
            boolean[][][] groundMask, byte[][][] buildings) {
        terrain=ground; nonGround=vegetation; hasGround=groundMask; building=buildings;
    }
    public RuntimeException getFailure() { return failure; }

    @Override public void run() {
        try {
            Pair<Integer, byte[][]> row;
            while ((row=rows.poll()) != null) {
                int y=row.getKey();
                for (int x=0; x<x_size; x++) for (int z=0; z<z_size; z++) {
                    if (occupancy[y][x][z] <= 0) continue;
                    processVoxel(x,y,z);
                }
            }
        } catch (RuntimeException e) {
            failure=e;
            rows.clear();
        }
    }

    private void processVoxel(int x, int y, int z) {
        double total=0;
        int days=0;
        ArrayList<Double> values=new ArrayList<Double>();
        ArrayList<String> dates=new ArrayList<String>();
        GregorianCalendar calendar=new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));
        for (int month:months) {
            calendar.clear(); calendar.set(year,month,1);
            int nDays=calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int day=1; day<=nDays; day++) {
                days++;
                for (int hour=0; hour<24; hour++) {
                    float[][] samples=precomputed[x/precomputed_resolution][y/precomputed_resolution][month][day][hour];
                    double hourly=0;
                    for (int m=0; m<samples.length; m++) {
                        double zenith=samples[m][0], azimuth=samples[m][1];
                        double irradiance;
                        try {
                            double transmission=trace(x,y,z,zenith,azimuth,rM.hasAmapVox);
                            irradiance=1366.0*Math.max(0,Math.cos(Math.toRadians(zenith)))*transmission;
                            if (!Double.isFinite(irradiance)) throw new IllegalStateException("Non-finite irradiance");
                        } catch (RuntimeException e) {
                            throw new IllegalStateException("Ray failed at voxel ("+x+","+y+","+z+") on "
                                    +year+"-"+(month+1)+"-"+day+" hour "+hour
                                    +"; zenith="+zenith+", azimuth="+azimuth,e);
                        }
                        hourly+=irradiance/samples.length;
                        if (writer!=null) {
                            dates.add(String.format(Locale.US,"%04d%02d%02dT%02d%02d",year,month+1,day,hour,
                                    m*60/samples.length));
                            values.add(Math.round(irradiance*100.0)/100.0);
                        }
                    }
                    total+=hourly;
                }
            }
        }
        rM.setValue(x,y,z,(float)(total/(24.0*days)));
        if (writer!=null) {
            double[] coordinates={minx+(x+offset(meanX[y][x][z]))*aR.step,
                    maxy-(y+offset(meanY[y][x][z]))*aR.step,
                    minz+(z+offset(meanZ[y][x][z]))*aR.step};
            try {
                synchronized (writer) { writer.writeLineSpecial(values,dates,coordinates); }
            } catch (Exception e) { throw new IllegalStateException("Cannot write voxel time series",e); }
        }
    }

    private static double offset(short value) {
        return Math.max(0,Math.min(0.999999,value/1000.0));
    }

    private double trace(final int x, final int y, final int z, double zenith,
            double azimuth, final boolean amap) {
        double ox=x+offset(meanX[y][x][z]);
        double oy=y+offset(meanY[y][x][z]);
        double oz=z+offset(meanZ[y][x][z]);
        if (terrain!=null) {
            double ground=terrain.heightAt(ox,oy);
            boolean groundOnly=hasGround!=null && hasGround[y][x][z]
                    && nonGround[y][x][z]==0;
            // Ground receivers lie on the reconstructed surface, not inside its voxel.
            if (Double.isFinite(ground) && (groundOnly || oz<=ground)) oz=Math.max(0,ground+1e-5);
        }
        return SolarRayTracer.trace(ox,oy,oz,x,y,z,x_size,y_size,z_size,aR.step,
                zenith,azimuth,terrain,new SolarRayTracer.Optics() {
            @Override public double transmission(int ix, int iy, int iz, double length) {
                if (amap) {
                    // AMAPVox input is assumed to store transmission for one voxel-side path.
                    double t=rM.aV.data[ix][iy][iz];
                    if (!Double.isFinite(t) || t<0 || t>1)
                        throw new IllegalStateException("AMAPVox transmission must be in [0,1] at "
                                +ix+","+iy+","+iz+": "+t);
                    return Math.pow(t,length/aR.step);
                }
                boolean representedGround=terrain!=null && terrain.covers(ix,iy);
                int count=representedGround && nonGround!=null
                        ? nonGround[iy][ix][iz] : occupancy[iy][ix][iz];
                if (count==0) return 1;
                if (building!=null && building[iy][ix][iz]>0) return 0;
                // A mixed ground/vegetation voxel must not become opaque merely due to ground.
                if ((hasGround==null || !hasGround[iy][ix][iz])
                        && rM.vox_!=null && rM.vox_[ix][iy][iz]!=null
                        && rM.vox_[ix][iy][iz].isSolid) return 0;
                return Math.exp(-extinction*length);
            }
        });
    }

    // Keep the original entry-point signatures for callers within the package.
    public double isBlocked_ray_trace_dampening(int x,int y,int z,byte count,double resolution,
            double zenith,double slope,float azimuth,int hour,float max,solar3dManipulator manipulator) {
        return trace(x,y,z,zenith,azimuth,false);
    }
    public double isBlocked_ray_trace_amapVox(int x,int y,int z,byte count,double resolution,
            double zenith,double slope,float azimuth,int hour,float max,solar3dManipulator manipulator) {
        return trace(x,y,z,zenith,azimuth,true);
    }
}
