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

    public Tiler(ArrayList<File> in, argumentReader aR) throws Exception {

        this.aR = aR;

        this.buffer = aR.buffer;
        this.rule = aR.getInclusionRule();
        this.odir = aR.odir;
        this.sideLength = aR.step;
        this.makePointCloudList(in);
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

        int x_ = 0, y_ = 0;

        for(double i = minX; i < maxX; i += sideLength){
            y_ = yMax-1;
            for(double j = minY; j < maxY; j += sideLength){

                int x = Math.min((int)((i - minX) / sideLength), xMax-1);
                int y = Math.min((int)((maxY - j) / sideLength), yMax-1);

                File temp = null;
                if(aR.buffer > 0) {
                    temp = aR.createOutputFileWithExtension(pointClouds.get(0), "_tile_" + (int) i + "_" + (int) j + "_" + (int) aR.step + "m_buf_" + (int) aR.buffer + ".las");
                }else {
                    temp = aR.createOutputFileWithExtension(pointClouds.get(0), "_tile_" + (int) i + "_" + (int) j + "_" + (int) aR.step + "m.las");
                }

                outputFilesMatrix_pw[ x_ ][ y_ ] = new pointWriterMultiThread(temp, pointClouds.get(0), "lasTile", aR);
                outputFilesMatrix_buf[ x_ ][ y_  ] = new LasPointBufferCreator(1, outputFilesMatrix_pw[ x_ ][ y_ ]);

                y_--;
            }
            x_++;

        }


    }

    /**
     * Creates the output files according to extents from findExtent().
     * i.e. creates a BufferedRandomAccesForLidarUEF class to write
     * the .las and creates a dummy header.
     */

    public void make() throws Exception{

        LasPoint tempPoint = new LasPoint();
        if(false)
            if(aR.cores >= 1){

                ArrayList<Thread> lista11 = new ArrayList<Thread>();

                for(int ii = 1; ii <= aR.cores; ii++){
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


        for(int i_ = 0; i_ < aR.inputFiles.size(); i_++) {

            LASReader temp = new LASReader(aR.inputFiles.get(i_));

            int thread_n = aR.pfac.addReadThread(temp);

            for(long i = 0; i < temp.getNumberOfPointRecords(); i += 200000) {

                int maxi2 = (int) Math.min(200000, Math.abs(temp.getNumberOfPointRecords() - i));

                aR.pfac.prepareBuffer(thread_n, i, 200000);

                for (int j = 0; j < maxi2; j++) {

                    temp.readFromBuffer(tempPoint);

                    if (rule.ask(tempPoint, i+j, true)) {

                        int x = Math.min((int) ((tempPoint.x - this.minX) / sideLength), xMax-1);
                        int y = Math.min((int) ((this.maxY - tempPoint.y) / sideLength), yMax-1);


                        /* Here we write the point to the correct tile */
                        if (outputFilesMatrix_buf[x][y].writePoint(tempPoint, rule, i+j))
                            pointCounts[x][y]++;

                        /* Next we have to check whether the point is in a buffer */
                        if (buffer > 0.0) {

                            check_point_in_buffer(tempPoint, j, x, y);
                            
                        }
                    }
                }
            }

        }

        for(int i = 0; i < outputFilesMatrix_buf.length; i++){
            for(int j = 0; j < outputFilesMatrix_buf[0].length; j++){

                if(pointCounts[i][j] > 0){

                    outputFilesMatrix_buf[i][j].close();
                    outputFilesMatrix_pw[i][j].close(aR);

                }
                else
                    outputFilesMatrix_pw[i][j].outputFile.file.delete();
            }

        }




    }

    private void check_point_in_buffer(LasPoint tempPoint, int j, int x, int y) throws IOException {
        tempPoint.synthetic = true;

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

    protected void finalize_() throws IOException{
        int count = 0;

        for(int i = 0; i < outputFilesMatrix_buf.length; i++){
            for(int j = 0; j < outputFilesMatrix_buf[0].length; j++){

                if(pointCounts[i][j] > 0){

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

            LASReader temp = new LASReader(aR.inputFiles.get(i));
            long n = temp.getNumberOfPointRecords();

            aR.p_update.threadProgress[coreNumber-1] = 0;
            aR.p_update.threadEnd[coreNumber-1] = (int)n;
            aR.p_update.threadFile[coreNumber-1] = temp.getFile().getName();
            
            int maxi = 0;

            for (long p = 0; p < temp.getNumberOfPointRecords(); p += 10000) {

                maxi = (int) Math.min(10000, Math.abs(temp.getNumberOfPointRecords() - (p)));

                try {
                    temp.readRecord_noRAF(p, tempPoint, maxi);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (int j = 0; j < maxi; j++) {

                    temp.readFromBuffer(tempPoint);

                    if (rule.ask(tempPoint, j+p, true)) {

                        int x = (int) ((tempPoint.x - this.minX) / sideLength);
                        int y = (int) ((this.maxY - tempPoint.y) / sideLength);

                        /* Here we write the point to the correct tile */
                        if (outputFilesMatrix_buf[x][y].writePoint(tempPoint, rule, j+p))
                            pointCounts[x][y]++;

                        /* Next we have to check whether the point is in a buffer */
                        if (buffer > 0.0) {

                            check_point_in_buffer(tempPoint, j, x, y);

                        }
                    }
                    aR.p_update.threadProgress[coreNumber - 1]++;
                    if (aR.p_update.threadProgress[coreNumber - 1] % 10000 == 0)
                        aR.p_update.updateProgressTiler();
                }
            }

        }

    }

    private void check_point_in_buffer_2(LasPoint tempPoint, int p, int j, int x, int y) throws IOException {
        tempPoint.synthetic = true;

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
                if (outputFilesMatrix_buf[x + 1][y].writePoint(tempPoint, rule, j + p))
                    pointCounts[x + 1][y]++;

        if (distanceToLeft < buffer)
            if (x - 1 >= 0)
                if (outputFilesMatrix_buf[x - 1][y].writePoint(tempPoint, rule, j + p))
                    pointCounts[x - 1][y]++;

        if (distanceToTop < buffer)
            if (y - 1 >= 0)
                if (outputFilesMatrix_buf[x][y - 1].writePoint(tempPoint, rule, j + p))
                    pointCounts[x][y - 1]++;

        if (distanceToBottom < buffer)
            if (y + 1 < yMax)
                if (outputFilesMatrix_buf[x][y + 1].writePoint(tempPoint, rule, j + p))
                    pointCounts[x][y + 1]++;


        /* Top right */
        if (distanceToTop < buffer && distanceToRight < buffer)
            if (y - 1 >= 0 && x + 1 < xMax)
                if (outputFilesMatrix_buf[x + 1][y - 1].writePoint(tempPoint, rule, j + p))
                    pointCounts[x + 1][y - 1]++;

        /* Top left */
        if (distanceToTop < buffer && distanceToLeft < buffer)
            if (y - 1 >= 0 && x - 1 >= 0)
                if (outputFilesMatrix_buf[x - 1][y - 1].writePoint(tempPoint, rule, j + p))
                    pointCounts[x - 1][y - 1]++;

        /* Bottom left */
        if (distanceToBottom < buffer && distanceToLeft < buffer)
            if (y + 1 < yMax && x - 1 >= 0)
                if (outputFilesMatrix_buf[x - 1][y + 1].writePoint(tempPoint, rule, j + p))
                    pointCounts[x - 1][y + 1]++;

        /* Bottom right */
        if (distanceToBottom < buffer && distanceToRight < buffer)
            if (y + 1 < yMax && x + 1 < xMax)
                if (outputFilesMatrix_buf[x + 1][y + 1].writePoint(tempPoint, rule, j + p))
                    pointCounts[x + 1][y + 1]++;
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
                if(coreNumber != numberOfCores){

                    pienin = (coreNumber - 1) * jako;
                    suurin = coreNumber * jako;
                }

                else{
                    pienin = (coreNumber - 1) * jako;
                    suurin = tiedostot.size();
                }

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

}