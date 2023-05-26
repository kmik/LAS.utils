package tools;
import LASio.*;
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

        //for(long i = 0; i < pointCloud.getNumberOfPointRecords(); i += 20000){
        for(long i = 0; i < pointCloud.getNumberOfPointRecords(); i++) {
            int j = 0;
        //    maxi = (int)Math.min(20000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

       //     try {
       //         pointCloud.readRecord_noRAF(i, tempPoint, maxi);
       //     }catch(Exception e){
        //        e.printStackTrace();
        //    }

       //     for (int j = 0; j < maxi; j++) {

                //pointCloud.readFromBuffer(tempPoint);
            pointCloud.readRecord(i, tempPoint);
                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
                    continue;
                }

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

            //}
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

        for(int x = 0; x < numberOfPixelsX; x++) {
            for (int y = 0; y < numberOfPixelsY; y++) {

                if(minIndex[x][y] != -1){

                    pointCloud.readRecord(minIndex[x][y], tempPoint);
                    //if(br.writePoint( tempPoint, rule, pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                    //        pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset, pointCloud.pointDataRecordFormat, minIndex[x][y]))
                    //    pointCount++;

                    try {

                        aR.pfac.writePoint(tempPoint, minIndex[x][y], thread_n);

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

        //br.writeBuffer2();
        //br.updateHeader2();

        aR.pfac.closeThread(thread_n);

        aR.p_update.updateProgressThin();

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
