package tools;

import LASio.*;
import utils.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class lasSort {

    public Byte myByte1 = new Byte("00000000000000000000000000000000");

    public Byte myByte2 = new Byte("00000000000000000000000000000000");

    File outputFile;

    argumentReader aR;
    LASReader pointCloud;

    ArrayList<sortRaf> tempFiles = new ArrayList<>();

    public lasSort(LASReader pointCloud, argumentReader aR){

        this.pointCloud = pointCloud;
        this.aR = aR;

        try {

            this.outputFile = aR.createOutputFile(pointCloud);

            if(aR.by_gps_time)
                this.sortByGPStime();
            if(aR.by_z_order)
                this.sortByZCount();

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Sorts the points in .las file by gps time in ascending order
     * @throws IOException
     */
    public void sortByGPStime() throws IOException {

        LasPoint tempPoint = new LasPoint();

        int parts = (int)Math.ceil((double)pointCloud.getNumberOfPointRecords() / 1000000.0);
        int jako = (int)Math.ceil((double)pointCloud.getNumberOfPointRecords() / (double) parts);

        int pienin;
        int suurin;
        int ero;

        int count;
        int totalCount = 0;
        declareTemporaryFiles(parts);

        int[] pointCountsPerFile = new int[parts];


        for(int i = 1; i <= parts; i++) {

            count = 0;

            if (i != parts) {

                pienin = (i-1) * jako;
                suurin = i * jako;
            } else {
                pienin = (i-1) * jako;
                suurin = (int) pointCloud.getNumberOfPointRecords();
            }

            Pair_float[] parit = new Pair_float[suurin-pienin];
            int maxi;

            pointCloud.braf.raFile.seek(pointCloud.braf.raFile.length());

            for(int s = pienin; s < suurin; s += 10000){

                maxi = Math.min(10000, Math.abs(suurin - s));
                try {
                    pointCloud.readRecord_noRAF(s, tempPoint, maxi);
                }catch (Exception e){
                    e.printStackTrace();
                }

                for (int j = 0; j < maxi; j++) {

                    pointCloud.readFromBuffer(tempPoint);

                    parit[count] = new Pair_float(s + j, tempPoint.gpsTime);
                    count++;
                }

            }

            totalCount += count;

            Arrays.sort(parit);
            tempFiles.get(i-1).seek(0);

            for (Pair_float pair_float : parit) {

                tempFiles.get(i - 1).writeInt(pair_float.index);
                tempFiles.get(i - 1).writeDouble(pair_float.value);

                pointCountsPerFile[i - 1]++;

            }

            tempFiles.get(i-1).writeBuffer2();

            System.gc();

        }

        int pointCount = 0;

        int counter = 0;

        int[] counts = new int[tempFiles.size()];

        for(int i = 0; i < tempFiles.size(); i++){
            counts[i] = 1000;
        }


        Pair_float[][] ehm = new Pair_float[tempFiles.size()][1000];

        for(int i = 0; i < ehm[0].length; i++){
            for(int j = 0; j < ehm.length; j++){
                ehm[j][i] = new Pair_float(0, 0, 0);
            }
        }

        PriorityQueue<Pair_float> prioque = new PriorityQueue<>();


        System.out.println(tempFiles.get(0).buffer.remaining() + " " + tempFiles.get(0).buffer.capacity());

        Pair_float tempPair = new Pair_float(0, 0);

        for(int j = 0; j < tempFiles.size(); j++) {


            tempFiles.get(j).seek(0);
            tempFiles.get(j).read((4 + 8) * 1000);

            for (int i = 0; i < 1000; i++) {

                tempFiles.get(j).readFromBuffer(tempPair);

                ehm[j][i].value = tempPair.value;
                ehm[j][i].index = tempPair.index;
                ehm[j][i].extra = j;

                prioque.add(ehm[j][i]);

            }
        }

        Pair_float tempPair2;
        int counteri = 0;
        pointWriterMultiThread pw = new pointWriterMultiThread(outputFile, pointCloud, "lasSort", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        while(prioque.size() > 0){

            counteri++;
            counteri++;

            tempPair2 = prioque.poll();

            pointCloud.readRecord(tempPair2.index, tempPoint);

            if(buf.writePoint( tempPoint, aR.getInclusionRule(), counter))
                pointCount++;

            counts[tempPair2.extra]--;
            pointCountsPerFile[tempPair2.extra]--;

            if(counts[tempPair2.extra] == 0){

                if(pointCountsPerFile[tempPair2.extra] > 0) {

                    tempFiles.get(tempPair2.extra).read((4 + 8) * Math.min(1000, pointCountsPerFile[tempPair2.extra]));

                    for (int i = 0; i < Math.min(1000, pointCountsPerFile[tempPair2.extra]); i++) {

                        tempFiles.get(tempPair2.extra).readFromBuffer(ehm[tempPair2.extra][i]);
                        ehm[tempPair2.extra][i].extra = tempPair2.extra;
                        prioque.add(ehm[tempPair2.extra][i]);

                    }

                    counts[tempPair2.extra] = Math.min(1000, pointCountsPerFile[tempPair2.extra]);
                }
            }

            if(counteri % 100000 == 0) {
                System.out.println(prioque.size());
                System.gc();
            }

            tempPair2 = null;
        }


        buf.close();
        pw.close(aR);

        deleteTemporaryFiles(parts);


    }

    /**
     * Sorts the points in .las file by z-count. This is very useful
     * to do BEFORE spatial indexing.
     * @throws IOException sdadas
     */
    public void sortByZCount() throws IOException {

        int parts = (int)Math.ceil((double)pointCloud.getNumberOfPointRecords() / 2000000.0);
        int jako = (int)Math.ceil((double)pointCloud.getNumberOfPointRecords() / (double) parts);

        int pienin;
        int suurin;
        int ero;

        int count;// = 0;
        int totalCount = 0;

        declareTemporaryFiles(parts);

        int[] pointCountsPerFile = new int[parts];

        HashSet<Integer> chekki = new HashSet<>();

        int morton = 0;
        UInt32 morton_uint;

        UInt32 x;

        LasPoint tempPoint = new LasPoint();

        for(int i = 1; i <= parts; i++) {

            count = 0;

            if (i != parts) {

                pienin = (i-1) * jako;
                suurin = i * jako;
            } else {
                pienin = (i-1) * jako;
                suurin = (int) pointCloud.getNumberOfPointRecords();
            }

            Pair_z[] parit = new Pair_z[suurin-pienin];

            int maxi;// = 0;

            pointCloud.braf.raFile.seek(pointCloud.braf.raFile.length());

            for(int s = pienin; s < suurin; s += 10000){

                maxi = Math.min(10000, Math.abs(suurin - s));

                try {
                    pointCloud.readRecord_noRAF(s, tempPoint, maxi);
                }catch (Exception e){
                    e.printStackTrace();
                }

                for (int j = 0; j < maxi; j++) {

                    pointCloud.readFromBuffer(tempPoint);
                    parit[count] = new Pair_z(s + j, calcZOrder((int) tempPoint.x * 10, (int) tempPoint.y * 10));
                    count++;

                }

            }

            totalCount += count;

            Arrays.sort(parit);
            tempFiles.get(i-1).seek(0);

            for (Pair_z pair_z : parit) {

                tempFiles.get(i - 1).writeInt(pair_z.index);
                tempFiles.get(i - 1).writeInt((int) pair_z.z_order);

                pointCountsPerFile[i - 1]++;

            }

            tempFiles.get(i-1).writeBuffer2();

            parit = null;

            System.gc();

        }

        LASraf br = new LASraf(outputFile);

        LASwrite.writeHeader(br, "lasSort", this.pointCloud, aR);

        int pointCount = 0;

        int counter = 0;

        int n_ = 100000;

        TreeMap<Integer, Integer> help = new TreeMap<>();

        int[] counts = new int[tempFiles.size()];

        for(int i = 0; i < tempFiles.size(); i++){
            counts[i] = n_;
        }


        Pair_z[][] ehm = new Pair_z[tempFiles.size()][n_];

        for(int i = 0; i < ehm[0].length; i++){
            for(int j = 0; j < ehm.length; j++){
                ehm[j][i] = new Pair_z(0, 0, 0);
            }
        }

        PriorityQueue<Pair_z> prioque = new PriorityQueue<>();

        TreeSet<Pair_z> setti = new TreeSet<>();

        Pair_z tempPair = new Pair_z(0, 0);

        double previous = -1.0;

        int increment = 1;

        for(int j = 0; j < tempFiles.size(); j++) {

            tempFiles.get(j).seek(0);
            tempFiles.get(j).read((4 + 4) * n_);

            for (int i = 0; i < n_; i++) {

                tempFiles.get(j).readFromBuffer2(tempPair);

                ehm[j][i].z_order = tempPair.z_order;
                ehm[j][i].index = tempPair.index;
                ehm[j][i].extra = j;

                if(ehm[j][i].z_order == previous){

                    ehm[j][i].z_order += 0.0000001 * increment;
                    increment++;
                    prioque.add(ehm[j][i]);
                }else{
                    previous = ehm[j][i].z_order;
                    prioque.add(ehm[j][i]);
                }
            }
        }

        Pair_z tempPair2;

        int counteri = 0;

        previous = -1.0;

        while(prioque.size() > 0){

            counteri++;

            tempPair2 = prioque.poll();

            pointCloud.readRecord(tempPair2.index, tempPoint);

            if(br.writePoint( tempPoint, aR.getInclusionRule(), pointCloud.xScaleFactor, pointCloud.yScaleFactor, pointCloud.zScaleFactor,
                    pointCloud.xOffset, pointCloud.yOffset, pointCloud.zOffset, pointCloud.pointDataRecordFormat, counter))
                pointCount++;

            counts[tempPair2.extra]--;
            pointCountsPerFile[tempPair2.extra]--;

            if(counts[tempPair2.extra] == 0){

                if(pointCountsPerFile[tempPair2.extra] > 0) {

                    tempFiles.get(tempPair2.extra).read((4 + 4) * Math.min(n_, pointCountsPerFile[tempPair2.extra]));

                    for (int i = 0; i < Math.min(n_, pointCountsPerFile[tempPair2.extra]); i++) {

                        tempFiles.get(tempPair2.extra).readFromBuffer2(ehm[tempPair2.extra][i]);
                        ehm[tempPair2.extra][i].extra = tempPair2.extra;
                        //help.put(tempPair.value, tempPair.index);

                        if(ehm[tempPair2.extra][i].z_order == previous){

                            ehm[tempPair2.extra][i].z_order += 0.0000001 * increment;
                            increment++;
                            //System.out.println(ehm[tempPair2.extra][i].z_order);
                            prioque.add(ehm[tempPair2.extra][i]);
                        }else{
                            previous = ehm[tempPair2.extra][i].z_order;
                            prioque.add(ehm[tempPair2.extra][i]);
                        }

                    }

                    counts[tempPair2.extra] = Math.min(n_, pointCountsPerFile[tempPair2.extra]);
                }
            }

            if(counteri % 100000 == 0) {
                System.out.println(Arrays.toString(pointCountsPerFile) + " " + tempPair2.extra);
                System.gc();
            }

            tempPair2 = null;
        }

        br.writeBuffer2();
        br.updateHeader2();
        deleteTemporaryFiles(parts);

    }

    public void sortByZCount_RAM() throws Exception {

        int parts = (int)Math.ceil((double)pointCloud.getNumberOfPointRecords() / 5000000.0);
        int jako = (int)Math.ceil((double)pointCloud.getNumberOfPointRecords() / (double) parts);

        int pienin;
        int suurin;
        int ero;

        int count = 0;// = 0;
        int totalCount = 0;

        //declareTemporaryFiles(parts);

        int[] pointCountsPerFile = new int[parts];

        HashSet<Integer> chekki = new HashSet<>();

        int morton = 0;
        UInt32 morton_uint;

        UInt32 x;

        LasPoint tempPoint = new LasPoint();
        Pair_z[] parit = new Pair_z[(int)pointCloud.getNumberOfPointRecords()];

        int thread_n = aR.pfac.addReadThread(pointCloud);

        pointCloud.braf.raFile.seek(pointCloud.braf.raFile.length());

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 200000) {

            int maxi = (int) Math.min(200000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 200000);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);
                    parit[count] = new Pair_z(i + j, calcZOrder((int) tempPoint.x * 10, (int) tempPoint.y * 10));
                    count++;

                }

            }


            totalCount += count;

            Arrays.sort(parit);

        pointWriterMultiThread pw = new pointWriterMultiThread(outputFile, pointCloud, "lasSort - z_order", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);

        count =0;

        for(Pair_z p_z : parit){

            //System.out.println(p_z.index);

            pointCloud.readRecord(p_z.index, tempPoint);

            aR.pfac.writePoint(tempPoint, p_z.index, thread_n);

        }

        aR.pfac.closeThread(thread_n);

    }

    public void declareTemporaryFiles(int nFiles) throws IOException{

        for(int i = 1; i <= nFiles; i++){

            File tempFile = new File(i + "_lasUtilsSort.tempo");

            if(tempFile.exists())
                tempFile.delete();

            tempFile.createNewFile();

            tempFiles.add(new sortRaf(tempFile));

        }

    }

    public void deleteTemporaryFiles(int nFiles) throws IOException{

        for(int i = 1; i <= nFiles; i++){

            File tempFile = new File(i + "_lasUtilsSort.tempo");
            tempFile.delete();

        }

    }

    public static class Pair_float implements Comparable<Pair_float> {

        public int index;
        public double value;

        public int extra;

        public UInt32 z_order;

        public Pair_float(int index, double value) {
            this.index = index;
            this.value = value;
        }

        public Pair_float(int index, double value, int extra) {
            this.index = index;
            this.value = value;
            this.extra = extra;
        }

        public Pair_float(int index, double value, int extra, UInt32 z_order) {
            this.index = index;
            this.value = value;
            this.extra = extra;
            this.z_order = z_order;
        }

        public Pair_float(int index, double value, UInt32 z_order) {
            this.index = index;
            this.value = value;
            this.z_order = z_order;
        }

        @Override
        public int compareTo(Pair_float other) {
            //multiplied to -1 as the author need descending sort order

            return 1 * Double.valueOf(this.value).compareTo(other.value);
        }

        public void setValue(double in){
            this.value = in;

        }
        public void setIndex(int in){
            this.index = in;
        }
    }

    long calcZOrder(int xPos, int yPos) {
        int[] MASKS = {0x55555555, 0x33333333, 0x0F0F0F0F, 0x00FF00FF};
        long[] SHIFTS = {1, 2, 4, 8};

        int x = xPos;  // Interleave lower 16 bits of x and y, so the bits of x
        int y = yPos;  // are in the even positions and bits from y in the odd;

        x = (x | (x << SHIFTS[3])) & MASKS[3];
        x = (x | (x << SHIFTS[2])) & MASKS[2];
        x = (x | (x << SHIFTS[1])) & MASKS[1];
        x = (x | (x << SHIFTS[0])) & MASKS[0];

        y = (y | (y << SHIFTS[3])) & MASKS[3];
        y = (y | (y << SHIFTS[2])) & MASKS[2];
        y = (y | (y << SHIFTS[1])) & MASKS[1];
        y = (y | (y << SHIFTS[0])) & MASKS[0];

        long result = x | (y << 1);
        return result;
    }

    public static class Pair_z implements Comparable<Pair_z> {

        public int index;

        public int extra;

        public long z_order;

        public Pair_z(int index, long z_order) {
            this.index = index;
            this.z_order = z_order;
        }

        public Pair_z(int index, long z_order, int extra) {
            this.index = index;
            this.z_order = z_order;
            this.extra = extra;
        }

        @Override
        public int compareTo(Pair_z other) {
            //multiplied to -1 as the author need descending sort order

            return Long.valueOf(this.z_order).compareTo(other.z_order);
        }

        public void setZ(long z_order){
            this.z_order = z_order;

        }
        public void setIndex(int in){
            this.index = in;
        }
    }



}
