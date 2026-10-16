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

    final int[] months_to_use;

    /** Full English month names, separated by semicolons; returns Calendar month indices. */
    public static int[] parseMonths(String value) {
        if (value == null || value.trim().isEmpty())
            return new int[]{Calendar.JUNE, Calendar.JULY, Calendar.AUGUST};
        String[] names={"january","february","march","april","may","june",
                "july","august","september","october","november","december"};
        LinkedHashSet<Integer> selected=new LinkedHashSet<Integer>();
        for (String token:value.split(";",-1)) {
            String name=token.trim().toLowerCase(Locale.ROOT);
            int month=-1;
            for (int i=0;i<names.length;i++) if (names[i].equals(name)) {month=i;break;}
            if (month<0) throw new IllegalArgumentException("Invalid month '"+token
                    +"' in aR.months. Use full English names separated by semicolons, "
                    +"for example: june;july;august");
            selected.add(month);
        }
        int[] result=new int[selected.size()];
        int i=0;
        for (int month:selected) result[i++]=month;
        return result;
    }

    int year = 2025;
    float rasterMaxValue = Float.NEGATIVE_INFINITY;

    public las2solar_photogrammetry(String chm_name, argumentReader aR, LASReader pointCloud, boolean d3) throws Exception{

        this.months_to_use = parseMonths(aR.months);
        this.year = aR.year;
        if (!(aR.step > 0) || !Double.isFinite(aR.step))
            throw new IllegalArgumentException("Voxel size must be positive and finite");
        final int groundClass = Integer.parseInt(System.getProperty("las2solar.groundClass", "2"));
        if (groundClass < 0 || groundClass > 255) throw new IllegalArgumentException("Invalid ground class");

        final int firstExtra = registerExtraBytes(aR);

        BufferedWriter outWriter = null;
        if (aR.outputMask) {
            File outFileTxt = aR.createOutputFileWithExtension(pointCloud.getFile(), ".txt");
            outWriter = new BufferedWriter(new FileWriter(outFileTxt), 1 << 16);
        }
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

        if (outWriter != null) outWriter.newLine();

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
        final int requestedCores = Math.max(1, aR.cores);
        final SolarSchedule schedule = new SolarSchedule(Math.multiplyExact(x_res,y_res),
                months_to_use,year,minutes,aR.step,Math.max(4,Math.multiplyExact(2,requestedCores)),aR.outputMask);

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

        int[] calendar_month = months_to_use;

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

        long preparationStart = System.nanoTime();
        sunriseAndSunset.clear();

        // Bound precomputation to the requested thread count; no nested common-pool parallelism.
        java.util.concurrent.ForkJoinPool sunPool = new java.util.concurrent.ForkJoinPool(requestedCores);
        try {
            sunPool.submit(() -> IntStream.range(0, x_res).parallel().forEach(x -> {
                GregorianCalendar time_ = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"));
                for (int y=0; y<y_res; y++) {
                    int precompute_x=(int)((double)steppi*x+(double)steppi/2.0);
                    int precompute_y=(int)((double)steppi*y+(double)steppi/2.0);
                    double px=this_min_x+Math.min(this_x_size-0.5,precompute_x)*aR.step;
                    double py=this_max_y-Math.min(this_y_size-0.5,precompute_y)*aR.step;
                    double[] transformed;
                    synchronized(ct) {transformed=ct.TransformPoint(px,py);}
                    for(int sample=0;sample<schedule.samples;sample++) {
                        time_.set(year,schedule.month[sample],schedule.day[sample],schedule.hour[sample],schedule.minute[sample],0);
                        AzimuthZenithAngle result=SPA.calculateSolarPosition(time_,transformed[1],transformed[0],0,DeltaT.estimate(time_));
                        schedule.set(x*y_res+y,sample,(float)result.getZenithAngle(),(float)result.getAzimuth());
                    }
                }
            })).get();
        } finally {sunPool.shutdown();}
        long sunPrepared = System.nanoTime();
        long start = System.currentTimeMillis();
        long cells=Math.multiplyExact(Math.multiplyExact((long)x_size,y_size),raster_z_size);
        int cores=(int)Math.max(1,Math.min(requestedCores,(cells+1023)/1024));
        Thread[] threads=new Thread[cores];
        threadSafeWriteToFile threadWriter=outWriter==null?null:new threadSafeWriteToFile(outWriter,
                Math.addExact(Math.multiplyExact(Math.multiplyExact(x_size,y_size),raster_z_size),1));
        // Material preparation can fail. Start the output thread only after it succeeds.
        solarParallel_3d.Work work=new solarParallel_3d.Work(x_size,y_size,raster_z_size,aR.step,rM,
                chm_values_f_3d,nonGround,hasGround,buildings,chm_values_mean_x,chm_values_mean_y,chm_values_mean_z,
                terrain,schedule,steppi,y_res,null,this_min_x,this_max_y,this_min_z,year);
        SolarTextOutput textOutput=threadWriter==null?null:new SolarTextOutput(threadWriter,schedule,cores);
        work.output=textOutput;
        long materialsPrepared=System.nanoTime();
        System.out.println("Solar CPU workers: "+cores+"; text output: "+(textOutput!=null)
                +"; attenuation: "+(work.grid.opticalDepth?"optical depth":"compatible arithmetic"));
        try {
            for(int i=0;i<cores;i++) {threads[i]=new solarParallel_3d(work,i);threads[i].start();}
            joinWorkers(threads);
        } finally {
            // Also join already-started workers before closing output after a startup failure.
            for(Thread worker:threads) if(worker!=null) joinWorkers(new Thread[]{worker});
            if(textOutput!=null) textOutput.finish();
        }
        if(work.failure!=null) throw work.failure;
        if(Boolean.getBoolean("las2solar.profile")) System.out.printf(Locale.US,
                "Solar timings: positions %.3f s; material/output setup %.3f s; tracing/output %.3f s%n",
                (sunPrepared-preparationStart)/1e9,(materialsPrepared-sunPrepared)/1e9,
                (System.nanoTime()-materialsPrepared)/1e9);

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

            } finally { if (outWriter != null) outWriter.close(); }
    }

    public las2solar_photogrammetry(String chm_name, argumentReader aR, LASReader pointCloud,
            boolean d3, boolean this_is_binary) throws Exception {
        this(chm_name, aR, pointCloud, d3);
    }

    public las2solar_photogrammetry(String chm_name, argumentReader aR, LASReader pointCloud){
        this.months_to_use = parseMonths(aR.months);
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

        int[] calendar_month = months_to_use;

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
    /** Shared immutable inputs plus an atomic cursor over small Z-contiguous chunks. */
    static final class Work {
        final int nx,ny,nz,sunStep,sunRows,year;
        final double minx,maxy,minz,resolution;
        final byte[][][] occupancy,nonGround;
        final boolean[][][] hasGround;
        final short[][][] meanX,meanY,meanZ;
        final solar3dManipulator model;
        final SolarRayTracer.Terrain terrain;
        final SolarCpuTracer.Grid grid;
        final SolarSchedule schedule;
        SolarTextOutput output;
        final long cells,tileCells;
        final java.util.concurrent.atomic.AtomicLong cursor=new java.util.concurrent.atomic.AtomicLong();
        volatile RuntimeException failure;
        Work(int nx,int ny,int nz,double resolution,solar3dManipulator model,
                byte[][][] occupied,byte[][][] nonGround,boolean[][][] hasGround,byte[][][] buildings,
                short[][][] mx,short[][][] my,short[][][] mz,SolarRayTracer.Terrain terrain,
                SolarSchedule schedule,int sunStep,int sunRows,SolarTextOutput output,
                double minx,double maxy,double minz,int year) {
            this.nx=nx;this.ny=ny;this.nz=nz;this.resolution=resolution;this.model=model;
            occupancy=occupied;this.nonGround=nonGround;this.hasGround=hasGround;
            meanX=mx;meanY=my;meanZ=mz;this.terrain=terrain;this.schedule=schedule;
            this.sunStep=sunStep;this.sunRows=sunRows;this.output=output;
            this.minx=minx;this.maxy=maxy;this.minz=minz;this.year=year;
            tileCells=Math.multiplyExact(Math.multiplyExact((long)sunStep,sunStep),nz);
            // Tile-major ordering keeps nearby chunks on the same cached solar directions.
            // Edge tiles are padded only in the cursor arithmetic, never in the voxel arrays.
            cells=Math.multiplyExact(Math.multiplyExact((long)((nx-1)/sunStep+1),sunRows),tileCells);
            double extinction=Double.parseDouble(System.getProperty("las2solar.extinctionPerMetre","0.33"));
            grid=new SolarCpuTracer.Grid(nx,ny,nz,resolution,extinction,terrain,
                    model.hasAmapVox?new SolarCpuTracer.Transmission() {
                        public double get(int x,int y,int z) {return model.aV.data[x][y][z];}
                    }:null,Boolean.getBoolean("las2solar.opticalDepth"),
                    !Boolean.getBoolean("las2solar.disableBlockSkipping"));
            if(!model.hasAmapVox) for(int y=0;y<ny;y++) for(int x=0;x<nx;x++) {
                boolean representedGround=terrain!=null&&terrain.covers(x,y);
                for(int z=0;z<nz;z++) {
                    int count=representedGround&&nonGround!=null?nonGround[y][x][z]:occupied[y][x][z];
                    byte material=SolarCpuTracer.CLEAR;
                    if(count!=0) {
                        boolean solid=buildings!=null&&buildings[y][x][z]>0;
                        if(!solid&&(hasGround==null||!hasGround[y][x][z])&&model.vox_!=null
                                &&model.vox_[x][y][z]!=null) solid=model.vox_[x][y][z].isSolid;
                        material=solid?SolarCpuTracer.OPAQUE:SolarCpuTracer.ATTENUATING;
                    }
                    grid.set(x,y,z,material);
                }
            }
        }
        synchronized void fail(RuntimeException e) {if(failure==null) failure=e;}
    }
    private final Work work;
    private SolarTextOutput.Batch batch;
    private int cachedTile=-1;
    private SolarCpuTracer.Sun[] sun;
    solarParallel_3d(Work work,int number) {super("solar-compute-"+number);this.work=work;}
    public RuntimeException getFailure() {return work.failure;}
    public void run() {
        try {
            final long chunk=1024;
            long start;
            while(work.failure==null&&(start=work.cursor.getAndAdd(chunk))<work.cells) {
                long end=Math.min(work.cells,start+chunk);
                for(long index=start;index<end&&work.failure==null;) {
                    int tile=(int)(index/work.tileCells);
                    long local=index%work.tileCells;
                    int column=(int)(local/work.nz),z=(int)(local%work.nz);
                    int x=(tile/work.sunRows)*work.sunStep+column%work.sunStep;
                    int y=(tile%work.sunRows)*work.sunStep+column/work.sunStep;
                    int length=(int)Math.min(work.nz-z,end-index);
                    if(x<work.nx&&y<work.ny) for(int stop=z+length;z<stop&&work.failure==null;z++) {
                        if(work.occupancy[y][x][z]>0) processVoxel(x,y,z);
                    }
                    index+=length;
                }
            }
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();work.fail(new IllegalStateException("Solar worker interrupted",e));
        } catch(RuntimeException e) {work.fail(e);}
        finally {
            if(work.output!=null&&batch!=null) try {work.output.submit(batch);batch=null;}
                catch(InterruptedException e) {Thread.currentThread().interrupt();work.fail(new IllegalStateException("Output interrupted",e));}
                catch(RuntimeException e) {work.fail(e);}
        }
    }
    private void processVoxel(int x,int y,int z) throws InterruptedException {
        int tile=(x/work.sunStep)*work.sunRows+y/work.sunStep;
        if(tile!=cachedTile) {sun=work.schedule.prepared(tile);cachedTile=tile;}
        double ox=x+offset(work.meanX[y][x][z]),oy=y+offset(work.meanY[y][x][z]),oz=z+offset(work.meanZ[y][x][z]);
        final double originalZ=oz;
        if(work.terrain!=null) {
            double ground=work.terrain.heightAt(ox,oy);
            boolean groundOnly=work.hasGround!=null&&work.hasGround[y][x][z]&&work.nonGround[y][x][z]==0;
            if(Double.isFinite(ground)&&(groundOnly||oz<=ground)) oz=Math.max(0,ground+1e-5);
        }
        SolarCpuTracer.Receiver receiver=new SolarCpuTracer.Receiver(ox,oy,oz,x,y,z);
        double[] values=null;
        if(work.output!=null) {
            if(batch==null) batch=work.output.acquire();
            values=batch.values[batch.count];
        }
        double total=0;
        int sample=0;
        try {
            for(int h=0;h<work.schedule.hours;h++) {
                double hourly=0;
                for(int m=0;m<work.schedule.samplesPerHour;m++,sample++) {
                    SolarCpuTracer.Sun direction=sun[sample];
                    double irradiance=direction==null?0:direction.irradiance*SolarCpuTracer.trace(receiver,direction,work.grid);
                    if(!Double.isFinite(irradiance)) throw new IllegalStateException("Non-finite irradiance");
                    // Preserve the previous per-hour grouping and sample division order.
                    hourly+=irradiance/work.schedule.samplesPerHour;
                    if(values!=null) values[sample]=Math.round(irradiance*100.0)/100.0;
                }
                total+=hourly;
            }
        } catch(RuntimeException e) {
            throw new IllegalStateException("Ray failed at voxel ("+x+","+y+","+z+") in "+work.year+", "
                    +work.schedule.describe(Math.min(sample,work.schedule.samples-1)),e);
        }
        work.model.setValue(x,y,z,(float)(total/(24.0*work.schedule.days)));
        if(values!=null) {
            double[] coordinates=batch.coordinates[batch.count];
            coordinates[0]=work.minx+ox*work.resolution;
            coordinates[1]=work.maxy-oy*work.resolution;
            coordinates[2]=work.minz+originalZ*work.resolution;
            if(++batch.count==batch.values.length) {work.output.submit(batch);batch=null;}
        }
    }
    private static double offset(short value) {return Math.max(0,Math.min(0.999999,value/1000.0));}
}
