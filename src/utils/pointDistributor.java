package utils;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.PriorityBlockingQueue;

public class pointDistributor {

    PriorityBlockingQueue<double[]> que = new PriorityBlockingQueue<>();

    LASReader pointCloud;
    ArrayList<HashSet<Integer>> to;
    argumentReader aR;

    public pointDistributor(){

    }
/*
    public pointDistributor(LASReader pointCloud, ArrayList<HashSet<Integer>> to, argumentReader aR) throws Exception {

        this.pointCloud = pointCloud;
        this.to = to;
        LasPoint tempPoint = new LasPoint();

        int thread_n = aR.pfac.addReadThread(pointCloud);

        int number_of_last_returns = 0;
        int number_of_all_returns = 0;

        int pointer = 0;

        HashSet<Integer> allClasses = new HashSet<>();


        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if(!allClasses.contains(tempPoint.pointSourceId)){

                    to.get(pointer).add((int)tempPoint.pointSourceId);
                    pointer++;

                    if(pointer == to.size())
                        pointer = 0;

                }

            }
        }

    }
*/
    public pointDistributor(LASReader pointCloud, ArrayList<HashSet<Integer>> to, argumentReader aR, ArrayList<File> output) throws Exception {

        this.aR = aR;
        this.pointCloud = pointCloud;
        this.to = to;
        LasPoint tempPoint = new LasPoint();

        int thread_n = aR.pfac.addReadThread(pointCloud);

        int pointer = 0;

        HashSet<Integer> allClasses = new HashSet<>();

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                /* For itd statistics, 0 means the point belongs to no segment */
                if(tempPoint.pointSourceId == 0)
                    continue;

                if(!allClasses.contains((int) tempPoint.pointSourceId)){

                    allClasses.add((int)tempPoint.pointSourceId);
                    to.get(pointer).add((int)tempPoint.pointSourceId);
                    pointer++;

                    if(pointer == to.size())
                        pointer = 0;

                }

            }
        }



        ArrayList<pointWriterMultiThread> pw = new ArrayList<>();
        ArrayList<LasPointBufferCreator> buf = new ArrayList<>();

        for(int i = 0; i < aR.cores; i++){

            pw.add(new pointWriterMultiThread(aR.createOutputFileWithExtension(pointCloud, "_tmp_" + i + ".las"), pointCloud, "lasSplit", aR));
            buf.add(new LasPointBufferCreator(1, pw.get(pw.size()-1)));

        }

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            aR.pfac.prepareBuffer(thread_n, i, 10000);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                for(int i_ = 0; i_ < aR.cores; i_++){
                    if(to.get(i_).contains((int)tempPoint.pointSourceId)){
                        buf.get(i_).writePoint(tempPoint, aR.getInclusionRule(), j);
                        break;
                    }
                }
            }
        }


        for(LasPointBufferCreator buff : buf){
            /*
            tempFiles.get(i).writeBuffer2();
            tempFiles.get(i).updateHeader2();
            */

            output.add(buff.pwrite.outputFile.file);
            //  System.out.println(buff.pwrite.outputFile.file.getAbsolutePath());
            buff.close();
            buff.pwrite.close(aR);
        }


    }



    public void createOutputFiles(){



    }

}
