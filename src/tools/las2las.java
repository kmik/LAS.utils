
package tools;

import LASio.*;
import utils.*;

import java.io.*;
import java.util.concurrent.ExecutionException;

public class las2las{

    int coreNumber = 0;

    public las2las(int coreNumber){

        this.coreNumber = coreNumber;

    }

    public boolean convert(LASReader in, argumentReader aR) throws Exception {
        long n = in.getNumberOfPointRecords();

        if(aR.change_point_type != -999) {
            //aR.changePointType(aR.change_point_type, in);
        }

        boolean ordered = true;

        int pointCount = 0;
        LasPoint tempPoint = new LasPoint();

        File outFile = aR.createOutputFile(in);
        //System.out.println(outFile.getAbsolutePath());
        //LASraf outPointCloud = new LASraf(outFile);

        //LASwrite.writeHeader(outPointCloud, "las2las", in.versionMajor, in.versionMinor, in.pointDataRecordFormat, in.pointDataRecordLength);

        int maxi;

        int counter = 0;
        long paritus ;

        int thread_n = aR.pfac.addReadThread(in);


        pointWriterMultiThread pw = new pointWriterMultiThread(outFile, in, "las2las", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(in.pointDataRecordLength, 1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);
/*
        aR.p_update.threadFile[coreNumber-1] = in.getFile().getName();
        aR.p_update.threadEnd[coreNumber-1] = (int)in.getNumberOfPointRecords();

        aR.p_update.threadProgress[coreNumber-1] = 0;

        aR.p_update.updateProgressLas2Las();
*/
        for(int i = 0; i < in.getNumberOfPointRecords(); i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(in.getNumberOfPointRecords() - i));


                //in.readRecord_noRAF(i, tempPoint, 10000);
                aR.pfac.prepareBuffer(thread_n, i, 10000);

            //pointCloud.braf.buffer.position(0);

            for (int j = 0; j < maxi; j++) {

                in.readFromBuffer(tempPoint);

                if(aR.echoClass){

                    if(tempPoint.numberOfReturns == 1){
                        tempPoint.numberOfReturns = 0;
                    }else if(tempPoint.returnNumber == tempPoint.numberOfReturns){
                        tempPoint.numberOfReturns = 3;
                    }else if(tempPoint.returnNumber == 1){
                        tempPoint.numberOfReturns = 1;
                    }else{
                        tempPoint.numberOfReturns = 2;
                    }

                }

                if(aR.setNegativeZero){

                    if(tempPoint.z < 0.0)
                        tempPoint.z = 0;

                }

                //buf.writePoint(tempPoint, aR.inclusionRule, i+j);
                try {



                    aR.pfac.writePoint(tempPoint, i + j, thread_n);
                }catch (Exception e){
                    e.printStackTrace();
                }

                //aR.p_update.threadProgress[coreNumber-1]++;

                //if(aR.p_update.threadProgress[coreNumber-1] % 10000 == 0)
                   // aR.p_update.updateProgressLas2Las();
            }
        }

        //buf.close();
        //pw.close();

            aR.pfac.closeThread(thread_n);

        return true;
    }
}