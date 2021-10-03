package tools;

import LASio.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

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

        //this.pointClouds = pointClouds2;
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
     * Creates the merged file according to @param outputFileName.
     * i.e. creates a BufferedRandomAccesForLidarUEF class to write
     * the .las and creates a dummy header.
     */

    public void declareOutputFile() throws IOException{

        /*
        if(outputFileName.equals("asd"))
            outputFileName = "merged.las";

        if(!odir.equals("asd"))
            outputFileName = odir + pathSep + outputFileName;

        File temp = new File(outputFileName);
           */



        //System.out.println(this.outputfile.getAbsolutePath());
        /*
        if(temp.exists()){

            temp = fo.createNewFileWithNewExtension(temp, "_1.las");
            // new File(temp.getAbsolutePath().replaceFirst("[.][^.]+$", "") + "_1.las");
        }

        temp.createNewFile();

        raOutput = new LASraf(temp);
        LASwrite.writeHeader(raOutput, "lasmerge", pointClouds.get(0).versionMajor, pointClouds.get(0).versionMinor, pointClouds.get(0).pointDataRecordFormat, pointClouds.get(0).pointDataRecordLength);
        */
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
/*
        for(int i = 0; i < pointClouds.size(); i++){

            LASReader temp = pointClouds.get(i);
            long n = temp.getNumberOfPointRecords();
            if(n > 0)
                for(int j = 0; j < n; j++){

                    temp.readRecord(j, tempPoint);
                    //System.out.println(tempPoint.x);
                    //writePoint(outputFilesMatrix[x][y], tempPoint, rule, 0.01, 0.01, 0.01, 0, 0, 0, 1, j)
                    if(raOutput.writePoint( tempPoint, rule, 0.01, 0.01, 0.01, 0, 0, 0, temp.pointDataRecordFormat, j))
                        pointCount++;
                    //else
                    //	System.out.println("DROPPED");
                }
            raOutput.writeBuffer2();
            raOutput.updateHeader2();
        }
*/



        pointWriterMultiThread pw = new pointWriterMultiThread(this.outputfile, tempReader1, "lasmerge", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(tempReader1.pointDataRecordLength, 1, pw);


        double minX = Double.MAX_VALUE;
        int counter = 0;
        for(int p = 0; p < aR.inputFiles.size(); p++){


            tempReader = new LASReader(aR.inputFiles.get(p));
            System.out.println("FILE: " + aR.inputFiles.get(p).getAbsolutePath());
            System.out.println("FILE: " + aR.inputFiles.get(p).getAbsolutePath());
            System.out.println("FILE: " + aR.inputFiles.get(p).getAbsolutePath());
            System.out.println("FILE: " + aR.inputFiles.get(p).getAbsolutePath());
            System.out.println("FILE: " + aR.inputFiles.get(p).getAbsolutePath());
            System.out.println("FILE: " + aR.inputFiles.get(p).getAbsolutePath());
            System.out.println("FILE: " + aR.inputFiles.get(p).getAbsolutePath());
             //System.out.println("File minx: " + tempReader.minX);
            for(int i = 0; i < tempReader .getNumberOfPointRecords(); i += 10000){

                int maxi = (int)Math.min(10000, Math.abs(tempReader .getNumberOfPointRecords() - i));


                int count = 0;
                tempReader.readRecord_noRAF(i, tempPoint, 10000);
                //pointCloud.braf.buffer.position(0);

                for (int j = 0; j < maxi; j++) {
                    //Sstem.out.println(j);
                    tempReader.readFromBuffer(tempPoint);
                    /*
                    if(raOutput.writePoint( tempPoint, rule, 0.01, 0.01, 0.01, 0, 0, 0, pointClouds.get(p).pointDataRecordFormat, j))
                        pointCount++;
*/
                    if(buf.writePoint( tempPoint, rule, j+i))
                        pointCount++;

                    counter++;
                    //System.out.println(tempPoint.x);
                }
            }
            /*
            raOutput.writeBuffer2();
            raOutput.updateHeader2();
               */
            tempReader.close();
        }

        buf.close();
        pw.close();

    }





}
