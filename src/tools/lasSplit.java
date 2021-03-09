package tools;

import LASio.LASReader;
import LASio.LASraf;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import utils.argumentReader;
import utils.pointWriterMultiThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class lasSplit {

    argumentReader aR;
    LASReader pointCloud;

    int splitBy = 0;

    HashMap<Integer, LASraf> tempFiles = new HashMap<Integer, LASraf>();
    HashMap<Integer, LasPointBufferCreator> tempFiles_buf = new HashMap<>();
    int coreNumber = 0;

    public lasSplit(LASReader pointCloud, argumentReader aR, int coreNumber) throws IOException {

        aR.p_update.lasSplit_splitCriterion = aR.splitBy;
        aR.p_update.updateProgressSplit();

        this.coreNumber = coreNumber;

        this.pointCloud = pointCloud;
        this.aR = aR;

        if(aR.splitBy.equals("gps")){
            splitBy = 1;
        }
        if(aR.splitBy.equals("classification")){
            splitBy = 2;
        }
        if(aR.splitBy.equals("return")){
            splitBy = 3;
        }
        if(aR.splitBy.equals("userData")){
            splitBy = 4;
        }
        if(aR.splitBy.equals("pointSourceId")){
            splitBy = 5;
        }
    }

    public void split() throws IOException{

        int maxi = 0;

        LasPoint tempPoint = new LasPoint();

        aR.p_update.threadFile[coreNumber-1] = pointCloud.getFile().getName();
        aR.p_update.threadEnd[coreNumber-1] = (int)pointCloud.getNumberOfPointRecords();
        aR.p_update.threadProgress[coreNumber-1] = 0;

        pointCloud.braf.raFile.seek(pointCloud.braf.raFile.length());

        if((splitBy == 1)){

            int flightLineId = 0;

            ArrayList<Double> bins = new ArrayList<>();
            ArrayList<Integer> bin_indexes = new ArrayList<>();

            for (int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

                maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

                try {
                    pointCloud.readRecord_noRAF(i, tempPoint, maxi);
                } catch (Exception e) {
                    e.printStackTrace();//pointCloud.braf.buffer.position(0);
                }

                for (int j = 0; j < maxi; j++) {
                    //Sstem.out.println(j);


                    pointCloud.readFromBuffer(tempPoint);

                    if((i+j) == 0){
                        flightLineId++;
                        declareOutputFile(flightLineId);
                        bins.add(tempPoint.gpsTime);
                        bin_indexes.add(0);
                    }

                    if(tempPoint.gpsTime < bins.get(bins.size()-1)){
                        aR.p_update.lasSplit_splitCriterion = "ERROR! LAS file not gps ordered!";
                        //System.out.println("ERROR!!");
                        return;
                    }

                    if(tempPoint.gpsTime - bins.get(bins.size()-1) > aR.interval){
                        flightLineId++;
                        declareOutputFile(flightLineId);
                        bins.add(tempPoint.gpsTime);
                        bin_indexes.add((i+j));
                    }



                    //System.out.println(flightLineId);
                    this.tempFiles_buf.get(flightLineId).writePoint(tempPoint, aR.getInclusionRule(), j+i);

                    aR.p_update.threadProgress[coreNumber - 1]++;

                    if (aR.p_update.threadProgress[coreNumber - 1] % 10000 == 0)
                        aR.p_update.updateProgressSplit();
                }
            }
            aR.p_update.updateProgressSplit();

        }

        else {
            for (int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

                maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

                try {
                    pointCloud.readRecord_noRAF(i, tempPoint, maxi);
                } catch (Exception e) {
                    e.printStackTrace();//pointCloud.braf.buffer.position(0);
                }

                for (int j = 0; j < maxi; j++) {
                    //Sstem.out.println(j);
                    pointCloud.readFromBuffer(tempPoint);

                    switch (splitBy) {

                        case 2:
                            if (!this.tempFiles_buf.containsKey(tempPoint.classification))
                                declareOutputFile(tempPoint.classification);

                            this.tempFiles_buf.get(tempPoint.classification).writePoint(tempPoint, aR.getInclusionRule(), j);

                            break;

                        case 3:
                            if (!this.tempFiles_buf.containsKey(tempPoint.returnNumber))
                                declareOutputFile(tempPoint.returnNumber);

                            this.tempFiles_buf.get(tempPoint.returnNumber).writePoint(tempPoint, aR.getInclusionRule(), j);

                            break;

                        case 4:
                            if (!this.tempFiles_buf.containsKey(tempPoint.userData))
                                declareOutputFile(tempPoint.userData);

                            this.tempFiles_buf.get(tempPoint.userData).writePoint(tempPoint, aR.getInclusionRule(), j);

                            break;

                        case 5:
                            if (!this.tempFiles_buf.containsKey((int) tempPoint.pointSourceId))
                                declareOutputFile(tempPoint.pointSourceId);

                            //System.out.println(this.tempFiles.get((int)tempPoint.pointSourceId));
                            this.tempFiles_buf.get((int) tempPoint.pointSourceId).writePoint(tempPoint, aR.getInclusionRule(), j);
                            break;
                    }

                    aR.p_update.threadProgress[coreNumber - 1]++;

                    if (aR.p_update.threadProgress[coreNumber - 1] % 10000 == 0)
                        aR.p_update.updateProgressSplit();
                }
            }
        }

        for(int i : tempFiles_buf.keySet()){
            /*
            tempFiles.get(i).writeBuffer2();
            tempFiles.get(i).updateHeader2();
            */
            tempFiles_buf.get(i).close();
            tempFiles_buf.get(i).pwrite.close();
        }

        aR.p_update.fileProgress++;
        aR.p_update.updateProgressSplit();


    }

    public void declareOutputFile(int split) throws IOException{

        LASraf tempRaf;
        pointWriterMultiThread pw;
        LasPointBufferCreator buf;
        switch (splitBy){

            case 1 :

                pw = new pointWriterMultiThread(aR.createOutputFileWithExtension(pointCloud, "_flightLine_" + split + ".las"), pointCloud, "lasSplit", aR);
                buf = new LasPointBufferCreator(pointCloud.pointDataRecordLength, 1, pw);
                this.tempFiles_buf.put(split, buf);
                break;

            case 2 :

                pw = new pointWriterMultiThread(aR.createOutputFileWithExtension(pointCloud, "_classification_" + split + ".las"), pointCloud, "lasSplit", aR);
                buf = new LasPointBufferCreator(pointCloud.pointDataRecordLength, 1, pw);
                this.tempFiles_buf.put(split, buf);

                /*
                tempRaf = new LASraf(aR.createOutputFileWithExtension(pointCloud, "_classification_" + split + ".las"));
                tempRaf.writeHeader("lasSplit", pointCloud.versionMajor, pointCloud.versionMinor, pointCloud.pointDataRecordFormat, pointCloud.pointDataRecordLength);
                this.tempFiles.put(split, tempRaf);
                 */
                break;

            case 3 :
                /*
                tempRaf = new LASraf(aR.createOutputFileWithExtension(pointCloud, "_return_" + split + ".las"));
                tempRaf.writeHeader("lasSplit", pointCloud.versionMajor, pointCloud.versionMinor, pointCloud.pointDataRecordFormat, pointCloud.pointDataRecordLength);
                this.tempFiles.put(split, tempRaf);

                 */
                pw = new pointWriterMultiThread(aR.createOutputFileWithExtension(pointCloud, "_return_" + split + ".las"), pointCloud, "lasSplit", aR);
                buf = new LasPointBufferCreator(pointCloud.pointDataRecordLength, 1, pw);
                this.tempFiles_buf.put(split, buf);
                break;

            case 4 :
                /*
                tempRaf = new LASraf(aR.createOutputFileWithExtension(pointCloud, "_userData_" + split + ".las"));
                tempRaf.writeHeader("lasSplit", pointCloud.versionMajor, pointCloud.versionMinor, pointCloud.pointDataRecordFormat, pointCloud.pointDataRecordLength);
                this.tempFiles.put(split, tempRaf);

                 */
                pw = new pointWriterMultiThread(aR.createOutputFileWithExtension(pointCloud, "_userData_" + split + ".las"), pointCloud, "lasSplit", aR);
                buf = new LasPointBufferCreator(pointCloud.pointDataRecordLength, 1, pw);
                this.tempFiles_buf.put(split, buf);
                break;

            case 5 :
                /*
                tempRaf = new LASraf(aR.createOutputFileWithExtension(pointCloud, "_pointSourceId_" + split + ".las"));
                tempRaf.writeHeader("lasSplit", pointCloud.versionMajor, pointCloud.versionMinor, pointCloud.pointDataRecordFormat, pointCloud.pointDataRecordLength);
                this.tempFiles.put(split, tempRaf);

                 */
                pw = new pointWriterMultiThread(aR.createOutputFileWithExtension(pointCloud, "_pointSourceId_" + split + ".las"), pointCloud, "lasSplit", aR);
                buf = new LasPointBufferCreator(pointCloud.pointDataRecordLength, 1, pw);
                this.tempFiles_buf.put(split, buf);
                break;
        }

    }
}
