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

        int[][] minIndex = new int[(int)numberOfPixelsX][(int)numberOfPixelsY];
        float[][] min_z = new float[(int)numberOfPixelsX][(int)numberOfPixelsY];

        for(int x = 0; x < numberOfPixelsX; x++){
            for(int y = 0; y < numberOfPixelsY; y++) {
                min_z[x][y] = Float.POSITIVE_INFINITY;
                minIndex[x][y] = -1;
            }
        }


        double tempx = minX;
        double tempy = maxY;

        long countx = 0;
        long county = 0;
        int count2 = 0;

        long n = pointCloud.getNumberOfPointRecords();

        LasPoint tempPoint = new LasPoint();

        int maxi = 0;

        aR.p_update.threadFile[coreNumber-1] = "initial pass";

        aR.p_update.threadEnd[coreNumber-1] = (int)pointCloud.getNumberOfPointRecords();
        aR.p_update.threadProgress[coreNumber-1] = 0;

        int x_index;
        int y_index;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000){

            maxi = (int)Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                pointCloud.readRecord_noRAF(i, tempPoint, maxi);
            }catch(Exception e){
                e.printStackTrace();
            }

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                x_index = (int)Math.floor((tempPoint.x - minX) / step);
                y_index = (int)Math.floor((maxY - tempPoint.y) / step);

                if(tempPoint.z < min_z[x_index][y_index]){
                    min_z[x_index][y_index] = (float)tempPoint.z;
                    minIndex[x_index][y_index] = i+j;
                }

                aR.p_update.threadProgress[coreNumber-1]++;

                if(aR.p_update.threadProgress[coreNumber-1] % 10000 == 0)
                    aR.p_update.updateProgressThin();

            }
        }

        aR.p_update.updateProgressThin();

        if(outputFile.exists())
            outputFile.delete();

        outputFile.createNewFile();

        LASraf br = new LASraf(outputFile);

        /*
        LASwrite.writeHeader(br, "lasthin", this.pointCloud.versionMajor, this.pointCloud.versionMinor,
                this.pointCloud.pointDataRecordFormat, this.pointCloud.pointDataRecordLength,
                pointCloud.headerSize, pointCloud.offsetToPointData, pointCloud.numberVariableLengthRecords,
                pointCloud.fileSourceID, pointCloud.globalEncoding,
                pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset);

         */

        LASwrite.writeHeader(br, "lasThin", this.pointCloud, aR);

        int pointCount = 0;

        aR.p_update.threadFile[coreNumber-1] = "outputting";
        aR.p_update.threadEnd[coreNumber-1] = (int)numberOfPixelsX * (int)numberOfPixelsY;
        aR.p_update.threadProgress[coreNumber-1] = 0;

        for(int x = 0; x < numberOfPixelsX; x++) {
            for (int y = 0; y < numberOfPixelsY; y++) {

                if(minIndex[x][y] != -1){

                    pointCloud.readRecord(minIndex[x][y], tempPoint);
                    if(br.writePoint( tempPoint, rule, pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                            pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset, pointCloud.pointDataRecordFormat, minIndex[x][y]))
                        pointCount++;

                }
                aR.p_update.threadEnd[coreNumber-1]++;

                if(aR.p_update.threadEnd[coreNumber-1] % 10000 == 0){
                    aR.p_update.updateProgressThin();
                }

            }
        }

        br.writeBuffer2();
        br.updateHeader2();

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
/*
        LASraf br = new LASraf(outputFile);

        LASwrite.writeHeader(br, "lasthin3d", this.pointCloud.versionMajor, this.pointCloud.versionMinor,
                this.pointCloud.pointDataRecordFormat, this.pointCloud.pointDataRecordLength,
                pointCloud.headerSize, pointCloud.offsetToPointData, pointCloud.numberVariableLengthRecords,
                pointCloud.fileSourceID, pointCloud.globalEncoding,
                pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset);

 */
        int pointCount = 0;

        int nParts = (int)Math.ceil((n * 4.0 / 1000000.0) / 1000.0);

        int pienin;
        int suurin;
        int jako = (int)Math.ceil((double)n / (double) nParts);

        TreeMap<Long, ArrayList<Integer>> hashmappi = new TreeMap<>();

/*
        for(int c = 0; c < nParts; c++) {

            //System.out.println(plotID1.size() / (double)cores);
            if(c != nParts){

                pienin = (nParts - 1) * jako;
                suurin = nParts * jako;
            }

            else{
                pienin = (c - 1) * jako;
                suurin = numberOfPixelsX;
            }

            n = suurin - pienin;
            */

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

        int[] vox = new int[1000000];

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
        int endIndex = n;

        parit = Arrays.copyOfRange(parit, 0, endIndex);

        long prevValue = parit[0].value;

        Arrays.sort(parit);
        ArrayList<Integer> takeRandomList = new ArrayList<>();

        int[] randomit = new int[this.n_ranodm];
        int randomIndex = 0;

        int writeIndex = 0;
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
            //System.out.println(parit[i].value + " ; " + parit[i].index);
        }


        aR.p_update.threadFile[coreNumber-1] = "third pass";
        //aR.p_update.threadEnd[coreNumber-1] = parit.length;
        aR.p_update.threadEnd[coreNumber-1] = hashmappi.size();
        aR.p_update.threadProgress[coreNumber-1] = 0;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                pointCloud.readRecord_noRAF(i, tempPoint, 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //pointCloud.braf.buffer.position(0);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if(includeOrNot[i+j]){

                    buf.writePoint(tempPoint, aR.inclusionRule, i+j);
                    pointCount++;

                }


            }



        }

        buf.close();
        pw.close(aR);


        //br.writeBuffer2();
        //br.updateHeader2();

        n = numberOfPixelsX * numberOfPixelsY * numberOfPixelsZ;
        //}

    }

    public int takeRandom(int[] arrayIn, int maxIndex){

        return(arrayIn[rand.nextInt(maxIndex - 1)]);

    }

    public void take_n_Random(ArrayList<Integer> arrayIn, int n, int[] outArray){

        Collections.shuffle(arrayIn);

        for(int i = 0; i < n; i++){

            outArray[i] = arrayIn.get(i);

        }
        //return(arrayIn[rand.nextInt(maxIndex - 1)]);

    }

    public static String concatString(LasPoint point, String oparse){

        char[] array = oparse.toCharArray();

        String output = "";

        if(oparse.equals("all")){

            output += " " + point.x;

            output += " " + point.y;

            output += " " + point.z;

            output += " " + point.intensity;

            output += " " + point.classification;

            output += " " + point.gpsTime;

            output += " " + point.numberOfReturns;

            output += " " + point.returnNumber;

        }

        for(int i = 0; i < array.length; i++){

            if(array[i] == ('x'))
                output += " " + point.x;

            if(array[i] == ('y'))
                output += " " + point.y;

            if(array[i] == ('z'))
                output += " " + point.z;

            if(array[i] == ('i'))
                output += " " + point.intensity;

            if(array[i] == ('c'))
                output += " " + point.classification;

            if(array[i] == ('t'))
                output += " " + point.gpsTime;

            if(array[i] == ('n'))
                output += " " + point.numberOfReturns;

            if(array[i] == ('r'))
                output += " " + point.returnNumber;

            if(array[i] == ('s'))
                output += " " + 0;
        }
        return output;

    }

}
