package tools;

import LASio.*;

import utils.argumentReader;
import utils.fileOperations;
import utils.pointWriterMultiThread;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 *  Tiles a potentially very large amount of LAS points from one
 *  or many files into square non-overlapping tiles of a specified
 *  size and save them into LAS format.
 *
 *
 * @author  Kukkonen Mikko
 * @version 0.1
 * @since 06.03.2018
 */

public class Tiler{

    double sideLength = 1000.0;

    ArrayList<LASReader> pointClouds = new ArrayList<LASReader>();
    LASraf[][] outputFilesMatrix;

    pointWriterMultiThread[][] outputFilesMatrix_pw;
    LasPointBufferCreator[][] outputFilesMatrix_buf;

    int[][] pointCounts;
    ArrayList<File> outputFiles = new ArrayList<File>();

    int outputFileCount = 0;

    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;

    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;

    int xMax = 0;
    int yMax = 0;

    double buffer = 10.0;

    argumentReader aR;

    PointInclusionRule rule = new PointInclusionRule(true);

    String odir;

    fileOperations fo = new fileOperations();

    /**
     * Constructor
     *
     * @param in					Input list of point clouds
     */

    public Tiler(ArrayList<File> in, argumentReader aR) throws IOException {

        this.aR = aR;

        this.buffer = aR.buffer;
        this.rule = aR.getInclusionRule();
        this.odir = aR.odir;
        this.sideLength = aR.step;
        this.makePointCloudList(in);
        //pointClouds = in;
        findExtent();

        outputFileCount = numberOfOutputFiles();
        declareOutputFiles();

        if(aR.cores >= in.size())
            aR.cores = in.size();

        aR.p_update.lastile_tileSize = (int)this.sideLength;
        aR.p_update.lastile_bufferSize = (int)this.buffer;

        aR.p_update.updateProgressTiler();

        make();



    }

    /**
     * Simply creates a list of LASReaders to pointClouds arraylist.
     * @param in
     */
    public void makePointCloudList(ArrayList<File> in){

        try {
            for (File f : in) {

                this.pointClouds.add(new LASReader(f));

            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public Tiler(double sideLength2){

        this.sideLength = sideLength2;

    }

    /**
     * Finds the square extent of the input
     * LAS files. This is used to calculate
     * determine the output files.
     */

    public void findExtent(){

        for(int i = 0; i < pointClouds.size(); i++){

            if(pointClouds.get(i).minX < minX)
                minX = pointClouds.get(i).minX;

            if(pointClouds.get(i).maxX > maxX)
                maxX = pointClouds.get(i).maxX;

            if(pointClouds.get(i).minY < minY)
                minY = pointClouds.get(i).minY;

            if(pointClouds.get(i).maxY > maxY)
                maxY = pointClouds.get(i).maxY;

        }

    }

    /**
     * Counts the number of tiles according to point cloud
     * extent and @param step
     *
     * @Return number of tiles
     */

    public int numberOfOutputFiles(){

        this.xMax = (int)Math.ceil((maxX - minX) / sideLength);
        this.yMax = (int)Math.ceil((maxY - minY) / sideLength);

        System.out.println(sideLength + " " + maxX + " " + minX + " " + maxY + " " + minY);

        outputFilesMatrix = new LASraf[(int)Math.ceil((maxX - minX) / sideLength)][(int)Math.ceil((maxY - minY) / sideLength)];

        outputFilesMatrix_buf = new LasPointBufferCreator[(int)Math.ceil((maxX - minX) / sideLength)][(int)Math.ceil((maxY - minY) / sideLength)];
        outputFilesMatrix_pw = new pointWriterMultiThread[(int)Math.ceil((maxX - minX) / sideLength)][(int)Math.ceil((maxY - minY) / sideLength)];

        pointCounts = new int[(int)Math.ceil((maxX - minX) / sideLength)][(int)Math.ceil((maxY - minY) / sideLength)];

        return (int)(Math.ceil((maxX - minX) / sideLength) * Math.ceil((maxY - minY) / sideLength));

    }

    /**
     * Creates the output files according to extents from findExtent().
     * i.e. creates a BufferedRandomAccesForLidarUEF class to write
     * the .las and creates a dummy header.
     */

    public void declareOutputFiles() throws IOException{


        String pathSep = System.getProperty("file.separator");

        for(double i = minX; i < maxX; i += sideLength)
            for(double j = minY; j < maxY; j += sideLength){

                File temp = null;
                if(aR.buffer > 0)
                    temp = aR.createOutputFileWithExtension(pointClouds.get(0), "_tile_" + (int)i + "_" + (int)j + "_" + (int)aR.step + "m_buf_" + (int)aR.buffer + ".las");
                else
                    temp = aR.createOutputFileWithExtension(pointClouds.get(0), "_tile_" + (int)i + "_" + (int)j + "_" + (int)aR.step + "m.las");

                //String fileName = odir + pathSep + "tile_" + (int)i + "_" + (int)j + ".las";

                //System.out.println("HERE!! " + temp.getAbsolutePath() + " " + temp.exists());
                //System.out.println(fileName);
                //File temp = new File(fileName);

                //if(temp.exists())
                  //  temp.delete();

                //temp.createNewFile();

                //LASraf raTemp = new LASraf(temp);
                //LASwrite.writeHeader(raTemp, "lasTile", pointClouds.get(0).versionMajor, pointClouds.get(0).versionMinor, pointClouds.get(0).pointDataRecordFormat, pointClouds.get(0).pointDataRecordLength);
                //outputFilesMatrix[ (int)((maxX - i) / sideLength) ][(int)((maxY - j) / sideLength) ] = raTemp;

                outputFilesMatrix_pw[ (int)((maxX - i) / sideLength) ][(int)((maxY - j) / sideLength) ] = new pointWriterMultiThread(temp, pointClouds.get(0), "lasTile", aR);
                outputFilesMatrix_buf[ (int)((maxX - i) / sideLength) ][(int)((maxY - j) / sideLength) ] = new LasPointBufferCreator(1, outputFilesMatrix_pw[ (int)((maxX - i) / sideLength) ][(int)((maxY - j) / sideLength) ]);
                //System.out.println(fileName);
            }


    }

    /**
     * Creates the output files according to extents from findExtent().
     * i.e. creates a BufferedRandomAccesForLidarUEF class to write
     * the .las and creates a dummy header.
     */

    public void make() throws IOException{

        LasPoint tempPoint = new LasPoint();
        if(false)
        if(aR.cores >= 1){

            ArrayList<Thread> lista11 = new ArrayList<Thread>();

            for(int ii = 1; ii <= aR.cores; ii++){
                //proge.addThread();
                Thread temp = new Thread(new multiTile(ii, aR.cores, aR.pointClouds, this));
                lista11.add(temp);
                temp.start();

            }

            for(int i = 0; i < lista11.size(); i++){

                try{

                    lista11.get(i).join();
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }

            this.finalize_();

            return;
        }

        for(int i = 0; i < aR.inputFiles.size(); i++) {

            LASReader temp = new LASReader(aR.inputFiles.get(i));
            long n = temp.getNumberOfPointRecords();

            int maxi = 0;

            for (int p = 0; p < temp.getNumberOfPointRecords(); p += 10000) {
                //for(int i = 0; i < n; i++){

                maxi = (int) Math.min(10000, Math.abs(temp.getNumberOfPointRecords() - (p)));

                try {
                    temp.readRecord_noRAF(p, tempPoint, maxi);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                for (int j = 0; j < maxi; j++) {

                    //if((j+p) > 1600000)

                    //System.out.println((j) + " " + maxi + " " + pointCloud.getNumberOfPointRecords());
                    temp.readFromBuffer(tempPoint);



                    if (rule.ask(tempPoint, j, true)) {

                        int x = (int) ((tempPoint.x - this.minX) / sideLength);
                        int y = (int) ((this.maxY - tempPoint.y) / sideLength);

                        /* Here we write the point to the correct tile */
                        //if(LASwrite.writePoint(outputFilesMatrix[x][y], tempPoint, rule, 0.01, 0.01, 0.01, 0, 0, 0, temp.pointDataRecordFormat, j))
                        if (outputFilesMatrix_buf[x][y].writePoint(tempPoint, rule, j))
                            pointCounts[x][y]++;

                        /* Next we have to check whether the point is in a buffer */
                        if (buffer > 0.0) {

                            tempPoint.synthetic = true;

                            //tempPoint.classification = 5;

                            double xd = ((tempPoint.x - this.minX) / sideLength);
                            xd = xd - (double) (int) xd;
                            double yd = ((this.maxY - tempPoint.y) / sideLength);
                            yd = yd - (double) (int) yd;


                            double distanceToRight = (1.0 - xd) * sideLength;
                            double distanceToLeft = (xd) * sideLength;

                            double distanceToBottom = (1.0 - yd) * sideLength;
                            double distanceToTop = (yd) * sideLength;

                            if (distanceToRight < buffer)
                                if (x + 1 < xMax)
                                    if (outputFilesMatrix_buf[x + 1][y].writePoint(tempPoint, rule, j))
                                        pointCounts[x + 1][y]++;

                            if (distanceToLeft < buffer)
                                if (x - 1 >= 0)
                                    if (outputFilesMatrix_buf[x - 1][y].writePoint(tempPoint, rule, j))
                                        pointCounts[x - 1][y]++;

                            if (distanceToTop < buffer)
                                if (y - 1 >= 0)
                                    if (outputFilesMatrix_buf[x][y - 1].writePoint(tempPoint, rule, j))
                                        pointCounts[x][y - 1]++;

                            if (distanceToBottom < buffer)
                                if (y + 1 < yMax)
                                    if (outputFilesMatrix_buf[x][y + 1].writePoint(tempPoint, rule, j))
                                        pointCounts[x][y + 1]++;


                            /* Top right */
                            if (distanceToTop < buffer && distanceToRight < buffer)
                                if (y - 1 >= 0 && x + 1 < xMax)
                                    if (outputFilesMatrix_buf[x + 1][y - 1].writePoint(tempPoint, rule, j))
                                        pointCounts[x + 1][y - 1]++;

                            /* Top left */
                            if (distanceToTop < buffer && distanceToLeft < buffer)
                                if (y - 1 >= 0 && x - 1 >= 0)
                                    if (outputFilesMatrix_buf[x - 1][y - 1].writePoint(tempPoint, rule, j))
                                        pointCounts[x - 1][y - 1]++;

                            /* Bottom left */
                            if (distanceToBottom < buffer && distanceToLeft < buffer)
                                if (y + 1 < yMax && x - 1 >= 0)
                                    if (outputFilesMatrix_buf[x - 1][y + 1].writePoint(tempPoint, rule, j))
                                        pointCounts[x - 1][y + 1]++;

                            /* Bottom right */
                            if (distanceToBottom < buffer && distanceToRight < buffer)
                                if (y + 1 < yMax && x + 1 < xMax)
                                    if (outputFilesMatrix_buf[x + 1][y + 1].writePoint(tempPoint, rule, j))
                                        pointCounts[x + 1][y + 1]++;


                        }
                    }
                }
            }


        }
        int count = 0;
        //System.out.println(pointCounts[0][0]);
        for(int i = 0; i < outputFilesMatrix_buf.length; i++){
            for(int j = 0; j < outputFilesMatrix_buf[0].length; j++){

                //System.out.println("count: " + count++);
                if(pointCounts[i][j] > 0){
                    /*
                    outputFilesMatrix[i][j].writeBuffer2();
                    outputFilesMatrix[i][j].updateHeader2();

                     */
                    outputFilesMatrix_buf[i][j].close();
                    outputFilesMatrix_pw[i][j].close(aR);

                }
                else
                    outputFilesMatrix_pw[i][j].outputFile.file.delete();
            }

        }

    }

    protected void finalize_() throws IOException{
        int count = 0;
        //System.out.println(pointCounts[0][0]);
        for(int i = 0; i < outputFilesMatrix_buf.length; i++){
            for(int j = 0; j < outputFilesMatrix_buf[0].length; j++){

                //System.out.println("count: " + count++);
                if(pointCounts[i][j] > 0){
                    /*
                    outputFilesMatrix[i][j].writeBuffer2();
                    LASwrite.updateHeader2(outputFilesMatrix[i][j], pointCounts[i][j]);

                     */
                    outputFilesMatrix_buf[i][j].close();
                    outputFilesMatrix_pw[i][j].close(aR);
                }
                else
                    outputFilesMatrix_pw[i][j].outputFile.file.delete();
            }

        }
    }



    public void make(ArrayList<LASReader> pClouds, int coreNumber) throws IOException{

        aR.p_update.updateProgressTiler();

        LasPoint tempPoint = new LasPoint();

        for(int i = 0; i < aR.inputFiles.size(); i++){

            //System.out.println("thread: " + coreNumber + " files: " + pClouds.size());

            LASReader temp = new LASReader(aR.inputFiles.get(i));
            long n = temp.getNumberOfPointRecords();

            aR.p_update.threadProgress[coreNumber-1] = 0;
            aR.p_update.threadEnd[coreNumber-1] = (int)n;

            aR.p_update.threadFile[coreNumber-1] = temp.getFile().getName();


            int maxi = 0;

            for (int p = 0; p < temp.getNumberOfPointRecords(); p += 10000) {
                //for(int i = 0; i < n; i++){

                maxi = (int) Math.min(10000, Math.abs(temp.getNumberOfPointRecords() - (p)));

                try {
                    temp.readRecord_noRAF(p, tempPoint, maxi);
                } catch (Exception e) {
                    e.printStackTrace();
                }



                for (int j = 0; j < maxi; j++) {

                    //if((j+p) > 1600000)

                    //System.out.println((j) + " " + maxi + " " + pointCloud.getNumberOfPointRecords());
                    temp.readFromBuffer(tempPoint);


                    //for(int j = 0; j < n; j++){

                    //temp.readRecord(j, tempPoint);

                    //System.out.println("HERE!!");
                    if (rule.ask(tempPoint, j+p, true)) {

                        int x = (int) ((tempPoint.x - this.minX) / sideLength);
                        int y = (int) ((this.maxY - tempPoint.y) / sideLength);

                        /* Here we write the point to the correct tile */
                        //if(LASwrite.writePoint(outputFilesMatrix[x][y], tempPoint, rule, 0.01, 0.01, 0.01, 0, 0, 0, temp.pointDataRecordFormat, j))
                        if (outputFilesMatrix_buf[x][y].writePoint(tempPoint, rule, j+p))
                            pointCounts[x][y]++;

                        /* Next we have to check whether the point is in a buffer */
                        if (buffer > 0.0) {

                            tempPoint.synthetic = true;

                            //tempPoint.classification = 5;

                            double xd = ((tempPoint.x - this.minX) / sideLength);
                            xd = xd - (double) (int) xd;
                            double yd = ((this.maxY - tempPoint.y) / sideLength);
                            yd = yd - (double) (int) yd;


                            double distanceToRight = (1.0 - xd) * sideLength;
                            double distanceToLeft = (xd) * sideLength;

                            double distanceToBottom = (1.0 - yd) * sideLength;
                            double distanceToTop = (yd) * sideLength;

                            if (distanceToRight < buffer)
                                if (x + 1 < xMax)
                                    if (outputFilesMatrix_buf[x + 1][y].writePoint(tempPoint, rule, j+p))
                                        pointCounts[x + 1][y]++;

                            if (distanceToLeft < buffer)
                                if (x - 1 >= 0)
                                    if (outputFilesMatrix_buf[x - 1][y].writePoint(tempPoint, rule, j+p))
                                        pointCounts[x - 1][y]++;

                            if (distanceToTop < buffer)
                                if (y - 1 >= 0)
                                    if (outputFilesMatrix_buf[x][y - 1].writePoint(tempPoint, rule, j+p))
                                        pointCounts[x][y - 1]++;

                            if (distanceToBottom < buffer)
                                if (y + 1 < yMax)
                                    if (outputFilesMatrix_buf[x][y + 1].writePoint(tempPoint, rule, j+p))
                                        pointCounts[x][y + 1]++;


                            /* Top right */
                            if (distanceToTop < buffer && distanceToRight < buffer)
                                if (y - 1 >= 0 && x + 1 < xMax)
                                    if (outputFilesMatrix_buf[x + 1][y - 1].writePoint(tempPoint, rule, j+p))
                                        pointCounts[x + 1][y - 1]++;

                            /* Top left */
                            if (distanceToTop < buffer && distanceToLeft < buffer)
                                if (y - 1 >= 0 && x - 1 >= 0)
                                    if (outputFilesMatrix_buf[x - 1][y - 1].writePoint(tempPoint, rule, j+p))
                                        pointCounts[x - 1][y - 1]++;

                            /* Bottom left */
                            if (distanceToBottom < buffer && distanceToLeft < buffer)
                                if (y + 1 < yMax && x - 1 >= 0)
                                    if (outputFilesMatrix_buf[x - 1][y + 1].writePoint(tempPoint, rule, j+p))
                                        pointCounts[x - 1][y + 1]++;

                            /* Bottom right */
                            if (distanceToBottom < buffer && distanceToRight < buffer)
                                if (y + 1 < yMax && x + 1 < xMax)
                                    if (outputFilesMatrix_buf[x + 1][y + 1].writePoint(tempPoint, rule, j+p))
                                        pointCounts[x + 1][y + 1]++;


                        }
                    }
                    aR.p_update.threadProgress[coreNumber - 1]++;
                    if (aR.p_update.threadProgress[coreNumber - 1] % 10000 == 0)
                        aR.p_update.updateProgressTiler();
                }
            }

        }

    }

    public static class multiTile implements Runnable{

        int coreNumber;
        int numberOfCores;
        ArrayList<LASReader> tiedostot;

        Tiler tiler;

        public multiTile(int coreNumber, int numberOfCores, ArrayList<LASReader> tiedostot, Tiler tiler){

            this.coreNumber = coreNumber;
            this.numberOfCores = numberOfCores;
            this.tiedostot = tiedostot;
            this.tiler = tiler;

        }

        public void run() {

            int pienin = 0;
            int suurin = 0;
            if(coreNumber != 0){

                int jako = (int)Math.ceil((double)tiedostot.size() / (double) numberOfCores);
                //System.out.println(plotID1.size() / (double)cores);
                if(coreNumber != numberOfCores){

                    pienin = (coreNumber - 1) * jako;
                    suurin = coreNumber * jako;
                }

                else{
                    pienin = (coreNumber - 1) * jako;
                    suurin = tiedostot.size();
                }

                //int[] testi = get_pienin_suurin(tiedostot.size(), numberOfCores, coreNumber);
                //pienin = testi[0];
                //suurin = testi[1];
                //System.out.println("pienin: " + pienin + " suurin: " + suurin + " jako: " + jako);

                tiedostot = new ArrayList<LASReader>(tiedostot.subList(pienin, suurin));

            }
            else{

            }

            try {
                tiler.make(this.tiedostot, coreNumber);
            }catch(IOException e) {
                e.printStackTrace();
            }
        }

    }

    public int[] get_pienin_suurin(int m, int n, int part){

        int jako = (int)Math.ceil((double)m / (double) n);

        boolean correct = n % m != 0;

        //System.out.println("correct: " + correct);

        int[] output = new int[2];

        if(n != part){
            if(correct && part == n - 1){
                output[0] = (part - 1) * jako;
                output[1] = part * jako - 1;
            }else{
                output[0] = (part - 1) * jako;
                output[1] = part * jako;
            }
        }else{
            if(correct){
                output[0] = (part - 1) * jako - 1;
                output[1] = m;
            }
        }

        return output;
    }
}
