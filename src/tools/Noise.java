package tools;

import LASio.*;
import utils.argumentReader;
import utils.pointWriterMultiThread;

import java.io.File;
import java.util.HashSet;

/**
 * Classifies and possibly removes points
 * classified as noise. Currently only classifies
 * AND removes, TODO: flag to not remove, only classify
 *
 *
 * @author  Kukkonen Mikko
 * @version 0.1
 * @since 06.03.2018
 */

public class Noise{

    double step = 2.0;
    LASReader pointCloud = null;

    int few = 6;

    PointInclusionRule rule = new PointInclusionRule();

    short[][][] cellMatrix;
    String odir = "";
    String outputFile = "";

    File outWriteFile = null;

    String pathSep = System.getProperty("file.separator");

    argumentReader aR;

    String stage = "first pass";

    int coreNumber = 1;

    public Noise(){


    }

    public Noise(double step2){

        this.step = step2;
    }

    public Noise(LASReader pointCloud2, argumentReader aR, int coreNumber) throws Exception {

        this.coreNumber = coreNumber;
        this.aR = aR;

        this.step = aR.step;
        this.pointCloud = pointCloud2;
        //this.odir = odir2;
        this.few = aR.few;
        this.rule = aR.getInclusionRule();

        aR.p_update.lasnoise_stepSize = (int)this.step;
        aR.p_update.lasnoise_few = this.few;

        outWriteFile = aR.createOutputFile(pointCloud2);

        aR.p_update.updateProgressNoise();

        removeNoise();
    }

    /**
     * Classfies solitary points as noise according to
     *
     */

    public void removeNoise() throws Exception{

        double minX = pointCloud.getMinX();
        double maxX = pointCloud.getMaxX();
        double minY = pointCloud.getMinY();
        double maxY = pointCloud.getMaxY();
        double minZ = pointCloud.getMinZ();
        double maxZ = pointCloud.getMaxZ();

        int numberOfPixelsX = (int)Math.ceil((maxX - minX) / step) + 1;
        int numberOfPixelsY = (int)Math.ceil((maxY - minY) / step) + 1;
        int numberOfPixelsZ = (int)Math.ceil((maxZ - minZ) / step) + 1;

        this.cellMatrix = new short[numberOfPixelsX][numberOfPixelsY][numberOfPixelsZ];

        long n = pointCloud.getNumberOfPointRecords();

        HashSet<Integer> noisePointsList = new HashSet<Integer>();
        HashSet<Integer> noisePoints = new HashSet<Integer>();

        LasPoint tempPoint = new LasPoint();


        aR.p_update.threadFile[coreNumber-1] = "first pass";

        int maxi = 0;

        aR.p_update.threadProgress[coreNumber-1] = 0;
        aR.p_update.threadEnd[coreNumber-1] = (int)pointCloud.getNumberOfPointRecords();
        int thread_n = aR.pfac.addReadThread(pointCloud);

        for (int p = 0; p < pointCloud.getNumberOfPointRecords(); p += 20000) {
            //for(int i = 0; i < n; i++){

            maxi = (int) Math.min(20000, Math.abs(pointCloud.getNumberOfPointRecords() - (p)));

            aR.pfac.prepareBuffer(thread_n, p, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                if(!aR.inclusionRule.ask(tempPoint, p+j, true)){
                    continue;
                }


                cellMatrix[(int)((tempPoint.x - minX) / step)][(int)((tempPoint.y - minY) / step)][(int)((tempPoint.z - minZ) / step)]++;
                aR.p_update.threadProgress[coreNumber-1]++;

                if((j+p) % 10000 == 0){
                    aR.p_update.updateProgressNoise();
                }
            }



        }

        int noiseCount = 0;
        int counter = 0;

        for(int x = 0; x < numberOfPixelsX; x++)
            for(int y = 0; y < numberOfPixelsY; y++)
                for(int z = 0; z < numberOfPixelsZ; z++){

                    int minXindex = x - 1;
                    int maxXindex = x + 1;

                    int minYindex = y - 1;
                    int maxYindex = y + 1;

                    int minZindex = z - 1;
                    int maxZindex = z + 1;

                    if(minXindex < 0)
                        minXindex = 0;
                    if(minYindex < 0)
                        minYindex = 0;
                    if(minZindex < 0)
                        minZindex = 0;

                    if(maxXindex > (numberOfPixelsX - 1) )
                        maxXindex = numberOfPixelsX - 1;
                    if(maxYindex > (numberOfPixelsY - 1) )
                        maxYindex = numberOfPixelsY - 1;
                    if(maxZindex > (numberOfPixelsZ - 1) )
                        maxZindex = numberOfPixelsZ - 1;

                    int count = 0;

                    for(int i = minXindex; i <= maxXindex; i++)
                        for(int j = minYindex; j <= maxYindex; j++)
                            for(int a = minZindex; a <= maxZindex; a++){
                                count += cellMatrix[i][j][a];

                            }


                    if(cellMatrix[x][y][z] > 0 && count < (1 + few)){	//cellMatrix[x][y][z] == 1 &&

                        noisePoints.add(combine(x, y, z));

                    }

                }

        aR.p_update.threadFile[coreNumber-1] = "second pass";

        maxi = 0;

        aR.p_update.threadProgress[coreNumber-1] = 0;
        aR.p_update.threadEnd[coreNumber-1] = (int)pointCloud.getNumberOfPointRecords();

        pointWriterMultiThread pw = new pointWriterMultiThread(outWriteFile, pointCloud, "las2las", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);
        aR.pfac.addWriteThread(thread_n, pw, buf);

        int noisePointCount = 0;

        int pointCount = 0;

        for (long p = 0; p < pointCloud.getNumberOfPointRecords(); p += 10000) {

            maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - (p)));

            pointCloud.readRecord_noRAF(p, tempPoint, maxi);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                if(!aR.inclusionRule.ask(tempPoint, p+j, true)){
                    continue;
                }

                int x = (int)((tempPoint.x - minX) / step);
                int y = (int)((tempPoint.y - minY) / step);
                int z = (int)((tempPoint.z - minZ) / step);

                if(aR.classify){

                    if(noisePoints.contains(combine(x, y, z))){
                        tempPoint.classification = 7;
                    }

                    pointCount++;
                    aR.pfac.writePoint(tempPoint, p + j, thread_n);

                }else{
                    if(!noisePoints.contains(combine(x, y, z))){

                        pointCount++;
                        aR.pfac.writePoint(tempPoint, p + j, thread_n);

                    }else{
                        aR.p_update.lasnoise_removed++;
                        noisePointCount++;
                        aR.p_update.threadInt[coreNumber-1] = noisePointCount;
                    }

                }


                aR.p_update.threadProgress[coreNumber-1]++;

                if((p+j) % 10000 == 0){

                    aR.p_update.updateProgressNoise();
                }
            }
        }

        aR.pfac.closeThread(thread_n);

        aR.p_update.fileProgress++;

        aR.p_update.updateProgressNoise();
    }

    public int combine(int a, int b, int c){

        return (a << 20) | (b << 10) | c;

    }

}
