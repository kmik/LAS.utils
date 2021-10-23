package utils;

import LASio.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class pointWriterMultiThread {

    public LASReader tempReader;

    public LASraf outputFile;

    int pointCount = 0;

    public boolean writing = false;

    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;

    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;

    double minZ = Double.POSITIVE_INFINITY;
    double maxZ = Double.NEGATIVE_INFINITY;

    long[] pointsByReturn = new long[5];
    long[] pointsByReturn_1_4 = new long[15];

    BlockingQueue<byte[]> spareInput = new ArrayBlockingQueue<byte[]>(5000);

    argumentReader aR;

    public int pointDataRecordFormat = 0;
    public int pointDataRecordLength = 0;

    public pointWriterMultiThread(File outFile2, LASReader tempReader1, String softwareName, argumentReader aR) throws IOException {

        this.aR = aR;
        this.tempReader = tempReader1;

        outputFile = new LASraf(outFile2);

        int orig_point_type = tempReader.pointDataRecordFormat;

        this.pointDataRecordFormat = tempReader.pointDataRecordFormat;

        if(aR.change_point_type != -999){
            this.pointDataRecordFormat = aR.change_point_type;
        }

        if(this.pointDataRecordFormat == 0){
            this.pointDataRecordLength = 20;
        }else if(this.pointDataRecordFormat == 1){
            this.pointDataRecordLength = 28;
        }else if(this.pointDataRecordFormat == 2){
            this.pointDataRecordLength = 26;
        }else if(this.pointDataRecordFormat == 3){
            this.pointDataRecordLength = 34;
        }else if(this.pointDataRecordFormat == 4){
            this.pointDataRecordLength = 57;
        }else if(this.pointDataRecordFormat == 5){
            this.pointDataRecordLength = 63;
        }else if(this.pointDataRecordFormat == 6){
            this.pointDataRecordLength = 30;
        }else if(this.pointDataRecordFormat == 7){
            this.pointDataRecordLength = 36;
        }else if(this.pointDataRecordFormat == 8){
            this.pointDataRecordLength = 38;
        }else if(this.pointDataRecordFormat == 9){
            this.pointDataRecordLength = 59;
        }else if(this.pointDataRecordFormat == 10){
            this.pointDataRecordLength = 67;
        }

        if(aR.change_version_minor != -999){

        }

        LASwrite.writeHeader(outputFile, softwareName, tempReader, aR);

    }

    public synchronized void write(byte[] in) throws IOException{

        outputFile.write(in, this.pointDataRecordLength);
        pointCount++;

    }

    public synchronized void addToQueue(byte[] in) {

        try {
            spareInput.put(in.clone());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeRemaining(byte[] in, int length) throws IOException{

        byte[] temp = Arrays.copyOfRange(in, 0, length);
        outputFile.write(temp, this.pointDataRecordLength);
    }

    public synchronized void setHeaderBlockData(double minX, double maxX, double minY, double maxY, double minZ, double maxZ, long[] pointsByReturn){

        if(minX < this.minX)
            this.minX = minX;
        if(maxX > this.maxX)
            this.maxX = maxX;

        if(minY < this.minY)
            this.minY = minY;
        if(maxY > this.maxY)
            this.maxY = maxY;

        if(minZ < this.minZ)
            this.minZ = minZ;
        if(maxZ > this.maxZ)
            this.maxZ = maxZ;

        for(int i = 0; i < pointsByReturn.length; i++)
            this.pointsByReturn[i] += pointsByReturn[i];

    }

    public void close(argumentReader aR) throws IOException{

        outputFile.updateHeader(this.minX, this.maxX, this.minY, this.maxY, this.minZ, this.maxZ, this.pointsByReturn, aR);
        outputFile.close();
    }
}
