package tools;

import LASio.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import utils.*;
/**
 * Merger merges multiple .las files into one.
 *
 *
 * @author  Kukkonen Mikko
 * @version 0.1
 * @since 06.03.2018
 */
public class Merger{

    fileOperations fo = new fileOperations();
    ArrayList<LASReader> pointClouds = new ArrayList<LASReader>();
    LASraf raOutput;
    PointInclusionRule rule = new PointInclusionRule();
    String outputFileName = "";
    String odir = "";
    String pathSep = System.getProperty("file.separator");

    File outputfile;

    argumentReader aR;
    public Merger(){


    }

    /**
     *
     * This tool merges multiple pointClouds in the arraylist pointClouds2 to one
     * pointcloud.
     *
     * @param pointClouds2
     * @param output
     * @param rule2
     * @param odir2
     * @param aR
     * @throws IOException
     */
    public Merger(ArrayList<File> pointClouds2, String output, PointInclusionRule rule2, String odir2, argumentReader aR) throws IOException {

        this.aR = aR;
        this.odir = odir2;

        this.rule = rule2;
        this.outputFileName = output;

        this.outputfile = aR.createOutputFile(new LASReader(aR.inputFiles.get(0)));

        try {
            make();
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    /**
     * Reads the points from each input point cloud file
     * and writes them according to @param rule to the
     * merged file.
     */

    public void make() throws Exception{

        LasPoint tempPoint = new LasPoint();
        int pointCount = 0;

        LASReader tempReader1 = new LASReader(aR.inputFiles.get(0));
        LASReader tempReader = new LASReader(aR.inputFiles.get(0));

        int thread_n = aR.pfac.addReadThread(tempReader1);

        pointWriterMultiThread pw = new pointWriterMultiThread(this.outputfile, tempReader1, "lasmerge", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);

        double minX = Double.MAX_VALUE;
        int counter = 0;
        for(int p = 0; p < aR.inputFiles.size(); p++){


            tempReader = new LASReader(aR.inputFiles.get(p));

            for(long i = 0; i < tempReader .getNumberOfPointRecords(); i += 10000){

                int maxi = (int)Math.min(10000, Math.abs(tempReader .getNumberOfPointRecords() - i));

                int count = 0;
                tempReader.readRecord_noRAF(i, tempPoint, 10000);

                for (int j = 0; j < maxi; j++) {
                    tempReader.readFromBuffer(tempPoint);

                    /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                    if(!rule.ask(tempPoint, i+j, true)){
                        continue;
                    }

                    aR.pfac.writePoint(tempPoint, i, thread_n);


                    counter++;
                }
            }

            tempReader.close();
        }

        buf.close();
        pw.close(aR);

    }
}
