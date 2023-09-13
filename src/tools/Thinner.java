package tools;
import LASio.*;
import err.toolException;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import utils.*;

import java.io.*;
import java.util.*;

/**
 *  Prunes the LAS points. Points are removed by keeping only
 *  either the highest or the lowest point within a square area.
 *
 *
 * @author  Kukkonen Mikko
 * @version 0.1
 * @since 06.03.2018
 */

public class Thinner{

    boolean[][] mask = null;
    double[] geoTransform = null;
    String oparse = "xyz";
    double step = 2.0;

    String outputName = "thin.txt";

    LASReader pointCloud = null;

    PointInclusionRule rule = new PointInclusionRule(true);

    boolean lowest = true;

    File outputFile;

    argumentReader aR;

    Random rand = new Random();

    int coreNumber = 0;

    int n_ranodm = 1;

    boolean thin3d = false;

    public Thinner(){


    }

    public Thinner(LASReader pointCloud2){

        this.pointCloud = pointCloud2;
    }


    /**
     *
     * @param pointCloud2
     * @param step2
     * @param aR
     * @param coreNumber
     * @throws IOException
     */
    public Thinner(LASReader pointCloud2, double step2, argumentReader aR, int coreNumber) throws IOException{

        if(aR.ref.size() > 0){
            this.readRasters();
        }

        this.coreNumber = coreNumber;

        this.step = step2;
        this.pointCloud = pointCloud2;
        this.aR = aR;

        this.n_ranodm = aR.few;

        outputFile = aR.createOutputFile(pointCloud2);

        aR.p_update.lasthin_kernel = this.step;
        aR.p_update.lasthin_random = this.n_ranodm;

        this.thin3d = aR.thin3d;

        //this.thin3d = true;

        if(thin3d) {
            aR.p_update.lasthin_mode = "3d";
            aR.p_update.updateProgressThin();
            thin3D();
        }
        else {
            aR.p_update.lasthin_mode = "2d";
            aR.p_update.updateProgressThin();
            thin();
        }

    }

    /**
     * Constructor
     *
     * @param pointCloud2 		Input point cloud
     * @param step2 				Side length of the square that is used
     *							in thinning
     * @param lowest2 			Keep lowest (true) or the highest (false)
     */

    public Thinner(LASReader pointCloud2, double step2, boolean lowest2){

        this.step = step2;
        this.pointCloud = pointCloud2;
        lowest = lowest2;
    }


    /**
     * Perform the thinning by keeping the lowest z point
     * inside "pixels" that are defined by aR.step
     */

    public void thin() throws IOException {

        double minX = pointCloud.getMinX();
        double maxX = pointCloud.getMaxX();
        double minY = pointCloud.getMinY();
        double maxY = pointCloud.getMaxY();

        double numberOfPixelsX = (int)Math.ceil((maxX - minX) / step) + 1;
        double numberOfPixelsY = (int)Math.ceil((maxY - minY) / step) + 1;

        long[][] minIndex = new long[(int)numberOfPixelsX][(int)numberOfPixelsY];
        float[][] min_z = new float[(int)numberOfPixelsX][(int)numberOfPixelsY];

        for(int x = 0; x < numberOfPixelsX; x++){
            for(int y = 0; y < numberOfPixelsY; y++) {

                if(aR.lowest)
                    min_z[x][y] = Float.POSITIVE_INFINITY;
                else
                    min_z[x][y] = Float.NEGATIVE_INFINITY;

                minIndex[x][y] = -1;
            }
        }


        long n = pointCloud.getNumberOfPointRecords();

        LasPoint tempPoint = new LasPoint();

        int maxi = 0;

        aR.p_update.threadFile[coreNumber-1] = "initial pass";

        aR.p_update.threadEnd[coreNumber-1] = (long)pointCloud.getNumberOfPointRecords();
        aR.p_update.threadProgress[coreNumber-1] = 0;

        int x_index;
        int y_index;

        //System.out.println("READING");
        int counter = 0;



        for(long i = 0; i < pointCloud.getNumberOfPointRecords(); i += 20000){
        //for(long i = 0; i < pointCloud.getNumberOfPointRecords(); i++) {
        //    int j = 0;
            maxi = (int)Math.min(20000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                pointCloud.readRecord_noRAF(i, tempPoint, maxi);
            }catch(Exception e){
                e.printStackTrace();
            }

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);
            //pointCloud.readRecord(i, tempPoint);
                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
                    continue;
                }

                double x_offset = ((tempPoint.x - minX) / step) % 1;
                double y_offset = ((maxY - tempPoint.y) / step) % 1;

                x_index = (int)Math.floor((tempPoint.x - minX) / step);
                y_index = (int)Math.floor((maxY - tempPoint.y) / step);

                x_index = Math.max(0, x_index);
                y_index = Math.max(0, y_index);

                if(aR.lowest) {
                    if (tempPoint.z < min_z[x_index][y_index]) {
                        min_z[x_index][y_index] = (float) tempPoint.z;
                        minIndex[x_index][y_index] = i + j;
                    }
                }
                else {
                    if (tempPoint.z > min_z[x_index][y_index]) {
                        min_z[x_index][y_index] = (float) tempPoint.z;
                        minIndex[x_index][y_index] = i + j;
                    }
                }
                aR.p_update.threadProgress[coreNumber-1]++;
                //counter++;

                if(aR.p_update.threadProgress[coreNumber-1] % 1000000 == 0) {
                    aR.p_update.updateProgressThin();
                    //System.out.println(counter + " / " + pointCloud.getNumberOfPointRecords());
                }

            }
        }

        aR.p_update.updateProgressThin();

        if(outputFile.exists())
            outputFile.delete();

        outputFile.createNewFile();

        //LASraf br = new LASraf(outputFile);
        //LASwrite.writeHeader(br, "lasThin", this.pointCloud, aR);

        int thread_n = aR.pfac.addReadThread(pointCloud);

        pointWriterMultiThread pw = new pointWriterMultiThread(outputFile, pointCloud, "lasthin", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);

        int pointCount = 0;

        aR.p_update.threadFile[coreNumber-1] = "outputting";
        aR.p_update.threadEnd[coreNumber-1] = (int)numberOfPixelsX * (int)numberOfPixelsY;
        aR.p_update.threadProgress[coreNumber-1] = 0;

        reset2dArray(min_z, -1.0f);

        LasPoint genericPoint = new LasPoint();


        for(int x = 0; x < numberOfPixelsX; x++) {
            for (int y = 0; y < numberOfPixelsY; y++) {

                if(minIndex[x][y] != -1){

                    pointCloud.readRecord(minIndex[x][y], tempPoint);

                    if(x == 0 && y == 0)
                        try {
                            genericPoint = (LasPoint) tempPoint.clone();
                            genericPoint.synthetic = true;
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    //if(br.writePoint( tempPoint, rule, pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                    //        pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset, pointCloud.pointDataRecordFormat, minIndex[x][y]))
                    //    pointCount++;

                    try {

                        if(aR.thinToCenter){

                            tempPoint.x = (float)(minX + x * step + step / 2.0);
                            tempPoint.y = (float)(maxY - y * step - step / 2.0);

                        }

                        aR.pfac.writePoint(tempPoint, minIndex[x][y], thread_n);
                        min_z[x][y] = (float)tempPoint.z;

                    }catch (Exception e){
                        e.printStackTrace();
                        System.exit(1);
                    }


                }
                aR.p_update.threadEnd[coreNumber-1]++;

                if(aR.p_update.threadEnd[coreNumber-1] % 10000 == 0){
                    aR.p_update.updateProgressThin();
                }

            }
        }

        if(aR.thinToCenter && aR.interpolate) {
            int countNanBefore = countNanvalues(min_z, -1.0f);

            boolean[][] interpolated = interpolate2dArrayMedian(min_z, -1.0f);

            int countNanAfter = countNanvalues(min_z, -1.0f);

            System.out.println("CountNanBefore: " + countNanBefore + " CountNanAfter: " + countNanAfter);
            System.out.println("CountNanBefore: " + countNanBefore + " CountNanAfter: " + countNanAfter);
            System.out.println("CountNanBefore: " + countNanBefore + " CountNanAfter: " + countNanAfter);
            System.out.println("CountNanBefore: " + countNanBefore + " CountNanAfter: " + countNanAfter);
            System.out.println("CountNanBefore: " + countNanBefore + " CountNanAfter: " + countNanAfter);
            System.out.println("CountNanBefore: " + countNanBefore + " CountNanAfter: " + countNanAfter);

            for (int x = 0; x < numberOfPixelsX; x++) {
                for (int y = 0; y < numberOfPixelsY; y++) {

                    if (interpolated[x][y]) {

                        try {

                            if (aR.thinToCenter) {

                                genericPoint.x = (float) (minX + x * step + step / 2.0);
                                genericPoint.y = (float) (maxY - y * step - step / 2.0);


                            }

                            if(aR.ref.size() > 0){
                                int x_ = (int) Math.round((genericPoint.x - geoTransform[0]) / geoTransform[1]);
                                int y_ = (int) Math.round((genericPoint.y - geoTransform[3]) / geoTransform[5]);


                                if(x < 0 || x >= mask.length || y < 0 || y >= mask[0].length){
                                    genericPoint.z = min_z[x][y];
                                    aR.pfac.writePoint(genericPoint, -1, thread_n);
                                }
                                else if(this.mask[x][y] != false){
                                    genericPoint.z = 0;
                                }else{
                                    genericPoint.z = min_z[x][y];
                                }

                                aR.pfac.writePoint(genericPoint, -1, thread_n);

                            }else {

                                genericPoint.z = min_z[x][y];
                                aR.pfac.writePoint(genericPoint, -1, thread_n);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            System.exit(1);
                        }

                    }

                }

            }
        }
        //System.exit(1);

        //br.writeBuffer2();
        //br.updateHeader2();

        aR.pfac.closeThread(thread_n);

        aR.p_update.updateProgressThin();

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

        //printTimeInMinutesSeconds(endTime - startTime, "Raster to 2d array");

    }

    public int countNanvalues(float[][] array, float nanvalue){

            int count = 0;

            int ncols = array.length;
            int nrows = array[0].length;

            for(int i = 0; i < ncols; i++){
                for(int j = 0; j < nrows; j++){
                    if(array[i][j] == nanvalue)
                        count++;
                }
            }

            return count;

    }

    public static float[][] clone2DArray(float[][] original) {
        int rows = original.length;
        float[][] clone = new float[rows][];

        for (int i = 0; i < rows; i++) {
            int columns = original[i].length;
            clone[i] = new float[columns];
            System.arraycopy(original[i], 0, clone[i], 0, columns);
        }

        return clone;
    }

    public boolean[][] interpolate2dArrayMedian(float[][] array, float nanvalue){

        float[][] tmpArray = clone2DArray(array);

        int ncols = array.length;
        int nrows = array[0].length;

        boolean[][] changed = new boolean[ncols][nrows];


        int countDone = 0;
        List<Long> nanvalues = new ArrayList<>();
        //List<Long> nanvalues2 = new LinkedList<>();

        for(int i = 0; i < ncols; i++){
            for(int j = 0; j < nrows; j++){

                if(array[i][j] == nanvalue){
                    nanvalues.add((long)j * ncols + i);
                }
            }
        }

        //boolean switch_ = true;

        int size = nanvalues.size();
        int currentIndex = 0;

        HashMap<Long, Float> map = new HashMap<>();



        while(countDone < nanvalues.size()){


                Long index = nanvalues.get(currentIndex);

                if(nanvalues.get(currentIndex) == -99L){
                    currentIndex++;

                    if(currentIndex >= nanvalues.size()) {

                        System.out.println("RESETTING ITERATOR");
                        for (Long key : map.keySet()) {
                            int x_ = (int) (key % ncols);
                            int y_ = (int) (key / ncols);
                            array[x_][y_] = map.get(key);
                            tmpArray[x_][y_] = map.get(key);
                        }
                        map.clear();
                        currentIndex = 0;

                    }

                    continue;
                }

                int x = (int)(index % ncols);
                int y = (int)(index / ncols);

                //float mean = getMeanFromNeighbors(tmpArray, x, y, nanvalue);
                float mean = getMedianFromNeighbors(tmpArray, x, y, nanvalue);

                if(mean != nanvalue){
                    //array[x][y] = mean;
                    map.put(index, mean);
                    changed[x][y] = true;
                    nanvalues.set(currentIndex, -99L);
                    countDone++;
                }

                currentIndex++;

                if(currentIndex >= nanvalues.size()) {

                    System.out.println("RESETTING ITERATOR");
                    for (Long key : map.keySet()) {
                        int x_ = (int) (key % ncols);
                        int y_ = (int) (key / ncols);
                        array[x_][y_] = map.get(key);
                        tmpArray[x_][y_] = map.get(key);
                    }
                    map.clear();
                    currentIndex = 0;

                }

            if(countDone % 1000 == 0)
                System.out.println("countDone: " + countDone + " / " + size);
            //System.out.println("nanvalues: " + nanvalues.size() + " nanvalues2: " + nanvalues2.size() + " sum: " + (nanvalues.size() + nanvalues2.size()));

        }

        if(map.size() > 0){
            for (Long key : map.keySet()) {
                int x_ = (int) (key % ncols);
                int y_ = (int) (key / ncols);
                array[x_][y_] = map.get(key);
                tmpArray[x_][y_] = map.get(key);
            }
        }

        for(int i = 0; i < nanvalues.size(); i++){
            if(nanvalues.get(i) != -99L)
                System.out.println("ERROR: nanvalues.get(i) != -99L");
        }

        return changed;

    }

    public float getMeanFromNeighbors(float[][] array, int x, int y, float nanvalue){

        int numRows = array.length;
        int numCols = array[0].length;

        float sum = 0;
        int count = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int newX = x + dx;
                int newY = y + dy;

                // Check if the neighboring cell is within bounds
                if (newX >= 0 && newX < numRows && newY >= 0 && newY < numCols) {
                    float neighborValue = array[newX][newY];

                    // Ignore neighbors with nanvalue
                    if (neighborValue != nanvalue) {
                        sum += neighborValue;
                        count++;
                    }
                }
            }
        }

        //System.out.println("count: " + count + " sum: " + sum);

        // Calculate the mean (average) value
        if (count > 0) {
            return sum / count;
        } else {
            // No valid neighbors found, return a special value (e.g., NaN or -1) to indicate that
            return nanvalue;
        }

    }

    public float getMedianFromNeighbors(float[][] array, int x, int y, float nanvalue) {
        int numRows = array.length;
        int numCols = array[0].length;

        List<Float> neighbors = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int newX = x + dx;
                int newY = y + dy;

                // Check if the neighboring cell is within bounds
                if (newX >= 0 && newX < numRows && newY >= 0 && newY < numCols) {
                    float neighborValue = array[newX][newY];

                    // Ignore neighbors with nanvalue
                    if (neighborValue != nanvalue) {
                        neighbors.add(neighborValue);
                    }
                }
            }
        }

        // Sort the list of valid neighboring values
        Float[] sortedNeighbors = neighbors.toArray(new Float[0]);
        Arrays.sort(sortedNeighbors);

        // Calculate the median value
        int size = sortedNeighbors.length;
        if (size < 2) {
            // No valid neighbors found, return a special value (e.g., NaN or -1) to indicate that
            return nanvalue;
        } else if (size % 2 == 0) {
            // If there is an even number of values, return the average of the two middle values
            int middle = size / 2;
            float median = (sortedNeighbors[middle - 1] + sortedNeighbors[middle]) / 2.0f;
            return median;
        } else {
            // If there is an odd number of values, return the middle value
            int middle = size / 2;
            return sortedNeighbors[middle];
        }
    }
    public void reset2dArray(float[][] array, float value){
        for(int i = 0; i < array.length; i++){
            for(int j = 0; j < array[0].length; j++){
                array[i][j] = value;
            }
        }
    }



    public static class Pair implements Comparable<Pair> {
        public final int index;
        public final long value;

        public Pair(int index, long value) {
            this.index = index;
            this.value = value;
        }

        @Override
        public int compareTo(Pair other) {
            //multiplied to -1 as the author need descending sort order

            return 1 * Long.valueOf(this.value).compareTo(other.value);
        }
    }


    public void thin3D() throws IOException{


        Random random = new Random();

        double minX = pointCloud.getMinX();
        double maxX = pointCloud.getMaxX();
        double minY = pointCloud.getMinY();
        double maxY = pointCloud.getMaxY();
        double minZ = pointCloud.getMinZ();
        double maxZ = pointCloud.getMaxZ();

        int numberOfPixelsX = (int)Math.ceil((maxX - minX) / step) + 1;
        int numberOfPixelsY = (int)Math.ceil((maxY - minY) / step) + 1;
        int numberOfPixelsZ = (int)Math.ceil((maxY - minY) / step) + 1;

        int n = (int)pointCloud.getNumberOfPointRecords();

        if(outputFile.exists())
            outputFile.delete();

        outputFile.createNewFile();

        int pointCount = 0;

        int nParts = (int)Math.ceil((n * 4.0 / 1000000.0) / 1000.0);

        int pienin;
        int suurin;
        int jako = (int)Math.ceil((double)n / (double) nParts);

        TreeMap<Long, ArrayList<Integer>> hashmappi = new TreeMap<>();

        Pair[] parit = new Pair[n];

        LasPoint tempPoint = new LasPoint();

        int xCoord;
        int yCoord;
        int zCoord;

        int voxelNumber;
        long voxelNumber_long;

        long current;
        long replace;

        long maxValue = 0;
        int indeksi = 0;

        long voxelCount = numberOfPixelsX * numberOfPixelsY * numberOfPixelsZ;

        //int[] vox = new int[1000000];

        int maxi = 0;


        aR.p_update.threadFile[coreNumber-1] = "first pass";
        aR.p_update.threadEnd[coreNumber-1] = (int)pointCloud.getNumberOfPointRecords();
        aR.p_update.threadProgress[coreNumber-1] = 0;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000){

            maxi = (int)Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                pointCloud.readRecord_noRAF(i, tempPoint, maxi);
            }catch(Exception e){
                e.printStackTrace();
            }

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
                    continue;
                }

                xCoord = (int) ((tempPoint.x - minX) / step);
                yCoord = (int) ((maxY - tempPoint.y) / step);
                zCoord = (int) ((tempPoint.z - minZ) / step);

                voxelNumber_long = yCoord * numberOfPixelsX + xCoord + zCoord * (numberOfPixelsX * numberOfPixelsY);

                parit[indeksi] = new Pair(i+j, voxelNumber_long);
                indeksi++;

                if (voxelNumber_long > maxValue)
                    maxValue = voxelNumber_long;


                aR.p_update.threadProgress[coreNumber-1]++;

                if(aR.p_update.threadProgress[coreNumber-1] % 10000 == 0){
                    aR.p_update.updateProgressThin();
                }

            }

        }

        aR.p_update.updateProgressThin();

        parit = Arrays.copyOfRange(parit, 0, n);

        long prevValue = parit[0].value;

        Arrays.sort(parit);
        ArrayList<Integer> takeRandomList = new ArrayList<>();

        int[] randomit = new int[this.n_ranodm];
        aR.p_update.threadFile[coreNumber-1] = "second pass";
        aR.p_update.threadEnd[coreNumber-1] = parit.length;
        //aR.p_update.threadEnd[coreNumber-1] = hashmappi.size();
        aR.p_update.threadProgress[coreNumber-1] = 0;
        ArrayList<Integer> a;

        pointWriterMultiThread pw = new pointWriterMultiThread(outputFile,pointCloud, "lasthin", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        boolean[] includeOrNot = new boolean[(int)pointCloud.getNumberOfPointRecords()];

        if(false)
            for (Map.Entry<Long, ArrayList<Integer>> entry : hashmappi.entrySet())
            {
                a = entry.getValue();

                if(a.size() > this.n_ranodm) {

                    take_n_Random(a, this.n_ranodm, randomit);

                    for (int kkk = 0; kkk < this.n_ranodm; kkk++) {

                        includeOrNot[randomit[kkk]] = true;

                    }
                }

                else{

                    for(int kkk = 0; kkk < a.size(); kkk++){

                        includeOrNot[a.get(kkk)] = true;

                    }
                }


                aR.p_update.threadProgress[coreNumber-1]++;

                if(aR.p_update.threadProgress[coreNumber-1] % 1000 == 0){
                    aR.p_update.updateProgressThin();
                }
            }



        for(int i = 0; i < parit.length; i++){

            if(parit[i].value == prevValue){
                takeRandomList.add(parit[i].index);
            }
            else{

                if(takeRandomList.size() > this.n_ranodm) {

                    take_n_Random(takeRandomList, this.n_ranodm, randomit);

                    //System.out.println(randomit[0] + " " + randomit[1]);

                    for (int kkk = 0; kkk < this.n_ranodm; kkk++) {
                        includeOrNot[randomit[kkk]] = true;
                    }
                }

                else{
                    //writeIndex = takeRandom[0];
                    for(int kkk = 0; kkk < takeRandomList.size(); kkk++){
                        includeOrNot[takeRandomList.get(kkk)] = true;
                    }
                }

                takeRandomList.clear();
                takeRandomList.add(parit[i].index);
            }


            prevValue = parit[i].value;

            aR.p_update.threadProgress[coreNumber-1]++;

            if(aR.p_update.threadProgress[coreNumber-1] % 1000 == 0){
                aR.p_update.updateProgressThin();
            }
        }

        aR.p_update.threadFile[coreNumber-1] = "third pass";
        aR.p_update.threadEnd[coreNumber-1] = hashmappi.size();
        aR.p_update.threadProgress[coreNumber-1] = 0;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                pointCloud.readRecord_noRAF(i, tempPoint, 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
                    continue;
                }

                if(includeOrNot[i+j]){
                    buf.writePoint(tempPoint, aR.inclusionRule, i+j);
                    pointCount++;

                }
            }
        }

        buf.close();
        pw.close(aR);

    }

    public void take_n_Random(ArrayList<Integer> arrayIn, int n, int[] outArray){

        Collections.shuffle(arrayIn);

        for(int i = 0; i < n; i++){

            outArray[i] = arrayIn.get(i);

        }

    }

}
