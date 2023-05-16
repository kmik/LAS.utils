package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LASwrite;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import utils.*;
public class las2txt{

    argumentReader aR;
    int coreNumber = 0;

    public las2txt(LASReader in, String odir, String oparse, argumentReader aR, int coreNumber) throws IOException {

        this.coreNumber = coreNumber;
        this.aR = aR;
        fileOperations fo = new fileOperations();
        long n = 0;

        boolean ordered = false;

        LasPoint tempPoint = new LasPoint();

        File tempFile = null;
        String tempPath = "";

        File tempOutFile = null;

        FileWriter fw = null;
        BufferedWriter bw = null;//
        PrintWriter out = null;


        aR.p_update.threadFile[coreNumber-1] = in.getFile().getName();
        aR.p_update.threadProgress[coreNumber-1] = 0;
        aR.p_update.threadEnd[coreNumber-1] = (int)in.getNumberOfPointRecords();
        aR.p_update.updateProgressLas2txt();

        if(!ordered){
            //for(int j = 0; j < in.size(); j++){

            tempOutFile = aR.createOutputFileWithExtension(in, ".txt");
            System.out.println(in.getFile().getAbsolutePath());
            //System.out.println(tempOutFile.getAbsolutePath());
            if(!aR.odir.equals("asd")){
                tempOutFile = fo.transferDirectories(tempOutFile, aR.odir);
            }

            if(tempOutFile.exists())
                tempOutFile.delete();

            tempOutFile.createNewFile();

            fw = new FileWriter(tempOutFile, true);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw);

            n = in.getNumberOfPointRecords();

            int maxi = 0;

            aR.p_update.threadFile[coreNumber-1] = in.getFile().getName();

            aR.p_update.threadEnd[coreNumber-1] = (int)in.getNumberOfPointRecords();
            aR.p_update.threadProgress[coreNumber-1] = 0;

            int x_index;
            int y_index;

            for(int i = 0; i < in.getNumberOfPointRecords(); i += 10000) {

                maxi = (int) Math.min(10000, Math.abs(in.getNumberOfPointRecords() - i));

                try {
                    in.readRecord_noRAF(i, tempPoint, maxi);
                } catch (Exception e) {
                    e.printStackTrace();//pointCloud.braf.buffer.position(0);
                }

                for (int j = 0; j < maxi; j++) {

                    in.readFromBuffer(tempPoint);

                    /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                    if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
                        continue;
                    }

                    //System.out.println("e: " + tempPoint.returnNumber + " n: " + tempPoint.numberOfReturns);

                    if(aR.echoClass){

                        tempPoint.returnNumber = 0;

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


                    out.println(LASwrite.LASpoint2String(tempPoint, aR.oparse, aR.sep));

                    aR.p_update.threadProgress[coreNumber-1]++;

                    if(aR.p_update.threadProgress[coreNumber-1] % 10000 == 0)
                        aR.p_update.updateProgressLas2txt();
                    //}
                }
            }
            out.close();

            //}
        }
        else{

            HashMap<Short, Integer> bank = new HashMap<Short, Integer>();

            HashMap<Short, ArrayList<LasPoint>> pointBank = new HashMap<Short, ArrayList<LasPoint>>();

            //for(int j = 0; j < in.size(); j++){

            n = in.getNumberOfPointRecords();

            tempFile = in.getFile();
            tempPath = fo.createNewFileWithNewExtension(tempFile, ".txt").getAbsolutePath();
            //tempFile.getAbsolutePath().replaceFirst("[.][^.]+$", "") + ".txt";

            tempOutFile = new File(tempPath);

            if(tempOutFile.exists())
                tempOutFile.delete();

            tempOutFile.createNewFile();

            fw = new FileWriter(tempOutFile, true);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw);

            for(long i = 0; i < n; i++){

                in.readRecord(i, tempPoint);

                if(!bank.containsKey(tempPoint.pointSourceId))
                    bank.put(tempPoint.pointSourceId, 1);
                else
                    bank.put(tempPoint.pointSourceId, bank.get(tempPoint.pointSourceId) + 1);

            }

            for(long i = 0; i < n; i++){

                in.readRecord(i, tempPoint);


                if(!pointBank.containsKey(tempPoint.pointSourceId)){
                    pointBank.put(tempPoint.pointSourceId, new ArrayList<LasPoint>());
                    pointBank.get(tempPoint.pointSourceId).add(new LasPoint(tempPoint));
                }
                else
                    pointBank.get(tempPoint.pointSourceId).add(new LasPoint(tempPoint));

                if(pointBank.get(tempPoint.pointSourceId).size() == bank.get(tempPoint.pointSourceId)){

                    for(int k = 0; k < pointBank.get(tempPoint.pointSourceId).size(); k++){

                        out.println(LASwrite.LASpoint2String(pointBank.get(tempPoint.pointSourceId).get(k), oparse, aR.sep));

                    }

                    pointBank.get(tempPoint.pointSourceId).clear();

                }


            }

            //}

        }

    }

}
