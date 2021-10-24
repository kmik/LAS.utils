package utils;

import LASio.*;
import err.lasFormatException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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

    public long[] pointsByReturn = new long[5];
    public long[] pointsByReturn_1_4 = new long[15];

    public argumentReader aR;

    public int pointDataRecordFormat = 0;
    public int pointDataRecordLength = 0;
    public int version_minor_source = 0;
    public int version_minor_destination = 0;


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
            this.version_minor_destination = aR.change_version_minor;
        }else
            this.version_minor_destination = tempReader.versionMinor;

        if(tempReader1 != null)
            this.version_minor_source = tempReader1.versionMinor;

        if(this.version_minor_destination < 4 && this.pointDataRecordFormat > 5){

            throw new lasFormatException("PointFormat and LAS version mismatch. PointFormat " + this.pointDataRecordFormat +
                    " cannot be used with LAS version 1." + this.version_minor_destination);

        }

        LASwrite.writeHeader(outputFile, softwareName, tempReader, aR);

    }

    public synchronized void write(byte[] in) throws IOException{

        outputFile.write(in, this.pointDataRecordLength);
        pointCount++;

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

        if(this.version_minor_destination >= 4) {
            outputFile.updateHeader_1_4(this.minX, this.maxX, this.minY, this.maxY, this.minZ, this.maxZ, this.pointsByReturn, this.pointsByReturn_1_4, aR);
        }
        else
            outputFile.updateHeader(this.minX, this.maxX, this.minY, this.maxY, this.minZ, this.maxZ, this.pointsByReturn, aR);
        outputFile.close();
    }
}
