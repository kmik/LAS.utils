
package tools;

import LASio.*;
import utils.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class process_las2las {

    int coreNumber = 0;

    public process_las2las(int coreNumber){

        this.coreNumber = coreNumber;

    }

    public boolean convert(LASReader in, argumentReader aR) throws Exception {

        LasPoint tempPoint = new LasPoint();

        File outFile = aR.createOutputFile(in);

        int maxi;

        int thread_n = aR.pfac.addReadThread(in);

        //aR.add_extra_bytes(6, "ITC_id", "ID for an ITC segment");

        //aR.add_extra_bytes(6, "z_order", "just a running id for points");
        //aR.add_extra_bytes(6, "point_id_2", "just a running id for points_2");
        //aR.add_extra_bytes(6, "tree_id", "ITC id");
        //aR.add_extra_bytes(4);
        //tempPoint.addExraByte(aR.create_extra_byte_vlr_n_bytes);
        pointWriterMultiThread pw = new pointWriterMultiThread(outFile, in, "las2las", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);

        /* Define the variables that we need */
/*
        int tree_id = -1, point_id = -1;
        try {
            tree_id = in.extraBytes_names.get("tree_id");
            point_id = in.extraBytes_names.get("point_id");
        }catch (Exception e){

        }
*/
/*
        int tree_id = -1, point_id = -1;
        try {
            //tree_id = in.extraBytes_names.get("point_id");
            point_id = in.extraBytes_names.get("point_id_2");
        }catch (Exception e){

        }
*/

        int counter = 0;

        //for(int i = 0; i < in.getNumberOfPointRecords(); i += 200000) {
        for(int i = 0; i < in.getNumberOfPointRecords(); i++) {

            //maxi = (int) Math.min(200000, Math.abs(in.getNumberOfPointRecords() - i));

            //aR.pfac.prepareBuffer(thread_n, i, 200000);

            //for (int j = 0; j < maxi; j++) {

                // in.readFromBuffer(tempPoint);

                //double x1 = tempPoint.x;

                //in.readRecord(i+j, tempPoint);
                in.readRecord(i, tempPoint);

                System.out.println(tempPoint);

                counter++;
                //double x2 = tempPoint.x;

                //System.out.println(x1 + " == " + x2);
                //System.out.println(tempPoint.x + " " + tempPoint.y);
                if(!aR.inclusionRule.ask(tempPoint, i, true)){
                    continue;
                }

                if(aR.echoClass){

                    /*
					#   0 = only

					#   1 = first of many
					#   2 = intermediate

   					#   3 = last of many
							*/
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

                //tempPoint.setExtraByteINT(i, aR.create_extra_byte_vlr_n_bytes.get(0), 0);
                //tempPoint.setExtraByteINT(tempPoint.scanAngleRank, aR.create_extra_byte_vlr_n_bytes.get(1), 1);

                if(aR.setNegativeZero){

                    if(tempPoint.z < 0.0)
                        tempPoint.z = 0;

                }

                try {

                        aR.pfac.writePoint(tempPoint, i, thread_n);

                }catch (Exception e){
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            //System.out.println(counter + " == " + in.numberOfPointRecords);
            aR.pfac.closeThread(thread_n);

        return true;
    }
}