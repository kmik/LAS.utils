package LASio;

import err.lasFormatException;
import utils.pointWriterMultiThread;

import java.io.IOException;

public class LasPointBufferCreator {

    public int bufferSize;
    public byte[] allArray2;
    public int allArray2Index = 0;

    public Byte myByte = new Byte("00000000");
    public byte myBitti = myByte.byteValue();

    public byte[] intArray = new byte[4];
    public byte[] unsignedShortArray = new byte[2];
    public byte[] unsignedByteArray = new byte[1];
    public byte[] longArray = new byte[8];
    public byte[] doubleArray = new byte[8];
    public byte[] floatArray = new byte[4];

    //ArrayList<BlockingQueue<byte[]>> outputQue;
    //BlockingQueue<Integer> threadDone;
    public pointWriterMultiThread pwrite;

    int bufferId = 0;

    LASraf pointCloudOut;

    int pointLengthInBytes;

    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;

    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;

    double minZ = Double.POSITIVE_INFINITY;
    double maxZ = Double.NEGATIVE_INFINITY;

    long[] pointsByReturn = new long[5];
    long[] pointsByReturn_1_4 = new long[15];
    int pointDataRecordFormat = -1;

    long pointCount = 0;
    long pointCount_1_4 = 0;

    public LasPointBufferCreator(int bufferId, pointWriterMultiThread pwrite){

        this.pointLengthInBytes = pwrite.pointDataRecordLength;
        this.pointDataRecordFormat = pwrite.pointDataRecordFormat;

        this.bufferId = bufferId;

        this.bufferSize = this.pointLengthInBytes * 50000;
        allArray2 = new byte[bufferSize];
        this.pwrite = pwrite;

    }

    public void setOutputPointCloud(LASraf in){

        this.pointCloudOut = in;

    }



    public void output() throws IOException {

        pwrite.write(allArray2);

    }
    public void writeInt(int in) throws IOException {

        intArray[3] = (byte)(in >>> 24);
        intArray[2] = (byte)(in >>> 16);
        intArray[1] = (byte)(in >>> 8);
        intArray[0] = (byte)in;

        if(allArray2Index + intArray.length >= allArray2.length) {

            allArray2[allArray2Index] = intArray[0];
            allArray2Index++;
            allArray2[allArray2Index] = intArray[1];
            allArray2Index++;
            allArray2[allArray2Index] = intArray[2];
            allArray2Index++;
            allArray2[allArray2Index] = intArray[3];
            allArray2Index++;

            output();
            allArray2Index = 0;

            return;
        }

        allArray2[allArray2Index] = intArray[0];
        allArray2Index++;
        allArray2[allArray2Index] = intArray[1];
        allArray2Index++;
        allArray2[allArray2Index] = intArray[2];
        allArray2Index++;
        allArray2[allArray2Index] = intArray[3];
        allArray2Index++;

    }

    public void writeUnsignedShort(int in) throws IOException {   // POSSIBLY WRONG!!!!!
        //buffer.clear();
        //int unsignedValue = in&0xffff;
        //short unsignedValueS = (short)unsignedValue;
        //unsignedShortArray = new byte[2];//buffer.allocate(2).putShort(unsignedValueS).array();

        unsignedShortArray[0] = (byte)(in & 0xff);
        unsignedShortArray[1] = (byte)((in >> 8) & 0xff);
        //System.out.println(unsignedValueS);
    /*
    buffer.wrap(array);
    buffer.flip();
    fileChannel.write(buffer);
    */
        //fileChannel.position(fileChannel.size());
        //System.out.println(array.length);
        //fileChannel.write(ByteBuffer.wrap(array));
        //allArray = concatenateByteArrays(allArray, array);
    /*
    writeBuffer_unsignedShort.position(0);
    writeBuffer_unsignedShort.put(unsignedShortArray);
    writeBuffer_unsignedShort.position(0);
    raFile.seek(raFile.length());
    fileChannel.write(writeBuffer_unsignedShort);

    if(true)
      return;


     */


        if(allArray2Index + unsignedShortArray.length >= allArray2.length) {
            //writeBuffer2();


            allArray2[allArray2Index] = unsignedShortArray[0];
            allArray2Index++;
            allArray2[allArray2Index] = unsignedShortArray[1];
            allArray2Index++;

            //System.out.println("thread " + this.bufferId + " wants to write!");
            //System.out.println("HERE!!!" + allArray2Index + " == " + allArray2.length);
/*
            System.out.println("WRITING WITH THREAD: " + this.bufferId);
            System.out.println("WRITING WITH THREAD: " + this.bufferId);
            System.out.println("WRITING WITH THREAD: " + this.bufferId);
            System.out.println("WRITING WITH THREAD: " + this.bufferId);

            try {
                outputQue.put(allArray2.clone());
            }catch (Exception e){
                e.printStackTrace();
            }


 */
            output();
            allArray2Index = 0;

            return;
        }

        allArray2[allArray2Index] = unsignedShortArray[0];
        allArray2Index++;
        allArray2[allArray2Index] = unsignedShortArray[1];
        allArray2Index++;


        //for(int i = 0; i < unsignedShortArray.length; i++){

        //writeBuffer.put(unsignedShortArray);

        //}

    }

    public byte setUnsetBit(byte in, int bit, int set){

        if(set==1)
            return in |= 1 << bit;
        else
            return in &= ~(1 << bit);
    }

    public synchronized boolean writePoint(LasPoint tempPoint, PointInclusionRule rule, int i)throws IOException {

        /* Written or not */
        boolean output = false;

        /* Write if rule says so */
        if(rule.ask(tempPoint, i, false)){

            /* We got here, so output true */
            output = true;

            if(pointDataRecordFormat <= 5) {

                double x = tempPoint.x;
                double y = tempPoint.y;
                double z = tempPoint.z;




                /* Scale x and apply xOffset */
                int lx = (int) ((x - this.pwrite.tempReader.xOffset) / this.pwrite.tempReader.xScaleFactor);
                /* Scale y and apply yOffset */
                int ly = (int) ((y - this.pwrite.tempReader.yOffset) / this.pwrite.tempReader.yScaleFactor);
                /* Scale z and apply zOffset */
                int lz = (int) ((z - this.pwrite.tempReader.zOffset) / this.pwrite.tempReader.zScaleFactor);

                if(lx > Integer.MAX_VALUE ||
                        lx < Integer.MIN_VALUE ){

                    throw new lasFormatException("X scale factor / offset out of range!");

                }

                if(lz > Integer.MAX_VALUE ||
                        lz < Integer.MIN_VALUE ){

                    throw new lasFormatException("Z scale factor / offset out of range!");


                }

                if(ly > Integer.MAX_VALUE ||
                        ly < Integer.MIN_VALUE ){

                    throw new lasFormatException("Y scale factor / offset out of range!");


                }

                /* Write scaled and offset x, y and z */
                this.writeInt(lx);
                this.writeInt(ly);
                this.writeInt(lz);

                /* Write intensity */
                this.writeUnsignedShort((short) tempPoint.intensity);// braf.readUnsignedShort()

			/*
				Return Number 3 bits 					(bits 0, 1, 2) 3 bits *
				Number of Returns (given pulse) 3 bits 	(bits 3, 4, 5) 3 bits *
				Scan Direction Flag 1 bit 				(bit 6) 1 bit *
				Edge of Flight Line 1 bit 				(bit 7)
			 */

                myBitti = setUnsetBit(myBitti, 7, tempPoint.edgeOfFlightLine ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 6, tempPoint.scanDirectionFlag);
                myBitti = setUnsetBit(myBitti, 5, ((byte) tempPoint.numberOfReturns & (1 << (2))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 4, ((byte) tempPoint.numberOfReturns & (1 << (1))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 3, ((byte) tempPoint.numberOfReturns & (1 << (0))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 2, ((byte) tempPoint.returnNumber & (1 << (2))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 1, ((byte) tempPoint.returnNumber & (1 << (1))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 0, ((byte) tempPoint.returnNumber & (1 << (0))) > 0 ? 1 : 0);

                /* Write byte */
                this.writeUnsignedByte(myBitti);

                /* Reset the byte */
                //myByte = 0;


			/*

			Bits 	Explanation

			0:4 	Classification Standard ASPRS classification as defined in the
					following classification table.
			5 		Synthetic If set then this point was created by a technique
					other than LIDAR collection such as digitized from
					a photogrammetric stereo model.
			6 		Key-point If set, this point is considered to be a model keypoint
					and thus generally should not be withheld in
					a thinning algorithm.
			7 		Withheld If set, this point should not be included in

			*/


                myBitti = setUnsetBit(myBitti, 7, (tempPoint.withheld) ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 6, (tempPoint.keypoint) ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 5, (tempPoint.synthetic) ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 4, ((byte) tempPoint.classification & (1 << (4))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 3, ((byte) tempPoint.classification & (1 << (3))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 2, ((byte) tempPoint.classification & (1 << (2))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 1, ((byte) tempPoint.classification & (1 << (1))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 0, ((byte) tempPoint.classification & (1 << (0))) > 0 ? 1 : 0);


                /* Write the byte */
                this.writeUnsignedByte(myBitti);

                /* Write scan angle */
                this.writeUnsignedByte((byte) tempPoint.scanAngleRank);

                /* Write user data */
                this.writeUnsignedByte((byte) tempPoint.userData);

                /* Write point source ID */
                this.writeUnsignedShort(tempPoint.pointSourceId);


            /* Previous stuff is pretty much standard for any point data type.
             How to do the extra stuff that is point data type specific? */

                /* RGB is included in both 2 and 3 record types in LAS 1.2 */

                if (this.pwrite.pointDataRecordFormat == 1 ||
                        this.pwrite.pointDataRecordFormat == 3 ||
                        this.pwrite.pointDataRecordFormat == 4 ||
                        this.pwrite.pointDataRecordFormat == 5) {

                    this.writeDouble(tempPoint.gpsTime);// = braf.readDouble();

                }

                if (this.pwrite.pointDataRecordFormat == 2 ||
                        this.pwrite.pointDataRecordFormat == 3 ||
                        this.pwrite.pointDataRecordFormat == 5) {

                    this.writeUnsignedShort(tempPoint.R);
                    this.writeUnsignedShort(tempPoint.G);
                    this.writeUnsignedShort(tempPoint.B);

                }

                if (this.pwrite.pointDataRecordFormat == 4 ||
                        this.pwrite.pointDataRecordFormat == 5) {

                    this.writeUnsignedByte((byte) tempPoint.WavePacketDescriptorIndex);
                    this.writeLong(tempPoint.ByteOffsetToWaveformData);
                    this.writeUnsignedInt(tempPoint.WaveformPacketSizeInBytes);
                    this.writeFloat(tempPoint.ReturnPointWaveformLocation);

                    this.writeFloat(tempPoint.x_t);
                    this.writeFloat(tempPoint.y_t);
                    this.writeFloat(tempPoint.z_t);
                }
            }else{

                double x = tempPoint.x;
                double y = tempPoint.y;
                double z = tempPoint.z;

                /* Scale x and apply xOffset */
                int lx = (int) ((x - this.pwrite.tempReader.xOffset) / this.pwrite.tempReader.xScaleFactor);
                /* Scale y and apply yOffset */
                int ly = (int) ((y - this.pwrite.tempReader.yOffset) / this.pwrite.tempReader.yScaleFactor);
                /* Scale z and apply zOffset */
                int lz = (int) ((z - this.pwrite.tempReader.zOffset) / this.pwrite.tempReader.zScaleFactor);

                /* Write scaled and offset x, y and z */
                this.writeInt(lx);
                this.writeInt(ly);
                this.writeInt(lz);

                this.writeUnsignedShort((short) tempPoint.intensity);// braf.readUnsignedShort()

                /*
				Return Number 3 bits 					(bits 0, 1, 2, 3) 4 bits *
				Number of Returns (given pulse) 3 bits 	(bits 4, 5, 6, 7) 4 bits *
			 */

                myBitti = setUnsetBit(myBitti, 7, ((byte) tempPoint.numberOfReturns & (1 << (3))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 6, ((byte) tempPoint.numberOfReturns & (1 << (2))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 5, ((byte) tempPoint.numberOfReturns & (1 << (1))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 4, ((byte) tempPoint.numberOfReturns & (1 << (0))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 3, ((byte) tempPoint.returnNumber & (1 << (3))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 2, ((byte) tempPoint.returnNumber & (1 << (2))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 1, ((byte) tempPoint.returnNumber & (1 << (1))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 0, ((byte) tempPoint.returnNumber & (1 << (0))) > 0 ? 1 : 0);

                /* Write byte */
                this.writeUnsignedByte(myBitti);

                /*

			Bits 	Explanation

			0 - 3 	Classification Flags: Classification flags are used to indicate special characteristics associated
                    with the point.
			4 - 5   Scanner Channel
            6       Scan Direction Flag
            7       Edge of Flight Line
			*/


                /* NOT TESTED!!!! */

                myBitti = setUnsetBit(myBitti, 7, (tempPoint.edgeOfFlightLine) ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 6, (tempPoint.scanDirectionFlag == 1) ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 5, ((byte) tempPoint.scannerCannel & (1 << (7))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 4, ((byte) tempPoint.scannerCannel & (1 << (6))) > 0 ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 3, (tempPoint.overlap) ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 2, (tempPoint.withheld) ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 1, (tempPoint.keypoint) ? 1 : 0);
                myBitti = setUnsetBit(myBitti, 0, (tempPoint.synthetic) ? 1 : 0);

                /* Write byte */
                this.writeUnsignedByte(myBitti);

                /* Write classification */
                this.writeUnsignedByte((byte) tempPoint.classification);

                /* Write user data */
                this.writeUnsignedByte((byte) tempPoint.userData);

                /* Write scan angle */
                this.writeUnsignedShort((byte) tempPoint.scanAngleRank);

                /* Write pointSourceId */
                this.writeUnsignedShort((byte) tempPoint.pointSourceId);

                /* Write gps time */
                this.writeDouble(tempPoint.gpsTime);


                if(pointDataRecordFormat >= 7 && pointDataRecordFormat != 9){

                    this.writeUnsignedShort(tempPoint.R);
                    this.writeUnsignedShort(tempPoint.G);
                    this.writeUnsignedShort(tempPoint.B);

                    if(pointDataRecordFormat >= 8){
                        this.writeUnsignedShort(tempPoint.N);
                    }
                }
                if(pointDataRecordFormat == 10 || pointDataRecordFormat == 9){

                    this.writeUnsignedByte((byte) tempPoint.WavePacketDescriptorIndex);
                    this.writeLong(tempPoint.ByteOffsetToWaveformData);
                    this.writeUnsignedInt(tempPoint.WaveformPacketSizeInBytes);
                    this.writeFloat(tempPoint.ReturnPointWaveformLocation);

                    this.writeFloat(tempPoint.x_t);
                    this.writeFloat(tempPoint.y_t);
                    this.writeFloat(tempPoint.z_t);

                }
            }

            this.pointCount++;

            if(tempPoint.z < this.minZ)
                this.minZ = tempPoint.z;
            if(tempPoint.x < this.minX) {
                this.minX = tempPoint.x;
            }
            if(tempPoint.y < this.minY)
                this.minY = tempPoint.y;

            if(tempPoint.z > this.maxZ)
                this.maxZ = tempPoint.z;
            if(tempPoint.x > this.maxX)
                this.maxX = tempPoint.x;
            if(tempPoint.y > this.maxY)
                this.maxY = tempPoint.y;

            if(tempPoint.returnNumber <= 5)
                pointsByReturn[tempPoint.returnNumber - 1]++;

            if(this.pwrite.version_minor_destination >= 4){


                if(tempPoint.returnNumber <= 15)
                    this.pwrite.pointsByReturn_1_4[tempPoint.returnNumber - 1]++;

                this.pointCount_1_4++;

            }

        }

        return output;
    }

    public void writeLong(long in) throws IOException {

        longArray[7] = (byte)(in >>> 56);
        longArray[6] = (byte)(in >>> 48);
        longArray[5] = (byte)(in >>> 40);
        longArray[4] = (byte)(in >>> 32);
        longArray[3] = (byte)(in >>> 24);
        longArray[2] = (byte)(in >>> 16);
        longArray[1] = (byte)(in >>>  8);
        longArray[0] = (byte)(in >>>  0);



        if(allArray2Index + longArray.length >= allArray2.length) {
            //writeBuffer2();

            allArray2[allArray2Index] = longArray[0];
            allArray2Index++;
            allArray2[allArray2Index] = longArray[1];
            allArray2Index++;
            allArray2[allArray2Index] = longArray[2];
            allArray2Index++;
            allArray2[allArray2Index] = longArray[3];
            allArray2Index++;
            allArray2[allArray2Index] = longArray[4];
            allArray2Index++;
            allArray2[allArray2Index] = longArray[5];
            allArray2Index++;
            allArray2[allArray2Index] = longArray[6];
            allArray2Index++;
            allArray2[allArray2Index] = longArray[7];
            allArray2Index++;

            output();
            allArray2Index = 0;

            return;
        }

        allArray2[allArray2Index] = longArray[0];
        allArray2Index++;
        allArray2[allArray2Index] = longArray[1];
        allArray2Index++;
        allArray2[allArray2Index] = longArray[2];
        allArray2Index++;
        allArray2[allArray2Index] = longArray[3];
        allArray2Index++;
        allArray2[allArray2Index] = longArray[4];
        allArray2Index++;
        allArray2[allArray2Index] = longArray[5];
        allArray2Index++;
        allArray2[allArray2Index] = longArray[6];
        allArray2Index++;
        allArray2[allArray2Index] = longArray[7];
        allArray2Index++;
/*
        if(allArray2Index + longArray.length >= allArray2.length) {
            //writeBuffer2();
            writeBuffer3(longArray);
            return;
        }

 */
        //writeBuffer.put(longArray);
        //for(int i = 0; i < longArray.length; i++){

        //}

        //return buffer.getLong();
    }

    public void writeDouble(double in) throws IOException {
        //buffer.clear();
        long v = Double.doubleToLongBits(in);
        //doubleArray = new byte[8];// buffer.allocate(8).putDouble(in).array();

        doubleArray[0] = (byte)(v);
        doubleArray[1] = (byte)(v>>>8);
        doubleArray[2] = (byte)(v>>>16);
        doubleArray[3] = (byte)(v>>>24);
        doubleArray[4] = (byte)(v>>>32);
        doubleArray[5] = (byte)(v>>>40);
        doubleArray[6] = (byte)(v>>>48);
        doubleArray[7] = (byte)(v>>>56);
    /*
    //allArray = concatenateByteArrays(allArray, array);
    writeBuffer_double.position(0);
    writeBuffer_double.put(doubleArray);
    writeBuffer_double.position(0);
    raFile.seek(raFile.length());
    fileChannel.write(writeBuffer_double);

    if(true)
      return;

     */
/*
        if(allArray2Index + doubleArray.length >= allArray2.length) {
            //writeBuffer2();
            writeBuffer3(doubleArray);
            return;
        }

 */


        if(allArray2Index + doubleArray.length >= allArray2.length) {
            //writeBuffer2();

            allArray2[allArray2Index] = doubleArray[0];
            allArray2Index++;
            allArray2[allArray2Index] = doubleArray[1];
            allArray2Index++;
            allArray2[allArray2Index] = doubleArray[2];
            allArray2Index++;
            allArray2[allArray2Index] = doubleArray[3];
            allArray2Index++;
            allArray2[allArray2Index] = doubleArray[4];
            allArray2Index++;
            allArray2[allArray2Index] = doubleArray[5];
            allArray2Index++;
            allArray2[allArray2Index] = doubleArray[6];
            allArray2Index++;
            allArray2[allArray2Index] = doubleArray[7];
            allArray2Index++;
            /*
            try {
                outputQue.put(allArray2.clone());
            }catch (Exception e){
                e.printStackTrace();
            }

            allArray2Index = 0;
            return;

             */
            output();
            allArray2Index = 0;

            return;
        }

        allArray2[allArray2Index] = doubleArray[0];
        allArray2Index++;
        allArray2[allArray2Index] = doubleArray[1];
        allArray2Index++;
        allArray2[allArray2Index] = doubleArray[2];
        allArray2Index++;
        allArray2[allArray2Index] = doubleArray[3];
        allArray2Index++;
        allArray2[allArray2Index] = doubleArray[4];
        allArray2Index++;
        allArray2[allArray2Index] = doubleArray[5];
        allArray2Index++;
        allArray2[allArray2Index] = doubleArray[6];
        allArray2Index++;
        allArray2[allArray2Index] = doubleArray[7];
        allArray2Index++;
        //writeBuffer.put(doubleArray);
        //for(int i = 0; i < doubleArray.length; i++){



        //}

    }
    public void writeFloat(float in) throws IOException {
        //buffer.clear();

        int bits = Float.floatToIntBits(in);
        //byte[] array = new byte[4];
        floatArray[0] = (byte)(bits & 0xff);
        floatArray[1] = (byte)((bits >> 8) & 0xff);
        floatArray[2] = (byte)((bits >> 16) & 0xff);
        floatArray[3] = (byte)((bits >> 24) & 0xff);

        //byte[] array = ByteBuffer.allocate(4).putFloat(in).array();
    /*
    buffer.wrap(array);
    buffer.flip();
    fileChannel.write(buffer);
    */
        //fileChannel.position(fileChannel.size());
        //fileChannel.write(ByteBuffer.wrap(array));
        //allArray = concatenateByteArrays(allArray, array);
        /*
    writeBuffer_float.position(0);
    writeBuffer_float.put(floatArray);
    writeBuffer_float.position(0);
    raFile.seek(raFile.length());
    fileChannel.write(writeBuffer_float);

    if(true)
      return;


         */

        if(allArray2Index + floatArray.length >= allArray2.length) {
            //writeBuffer2();
            allArray2[allArray2Index] = floatArray[0];
            allArray2Index++;
            allArray2[allArray2Index] = floatArray[1];
            allArray2Index++;
            allArray2[allArray2Index] = floatArray[2];
            allArray2Index++;
            allArray2[allArray2Index] = floatArray[3];
            allArray2Index++;
/*
            try {
                outputQue.put(allArray2.clone());
            }catch (Exception e){
                e.printStackTrace();
            }

            allArray2Index = 0;
            return;

 */
            output();
            allArray2Index = 0;

            return;
        }

        allArray2[allArray2Index] = floatArray[0];
        allArray2Index++;
        allArray2[allArray2Index] = floatArray[1];
        allArray2Index++;
        allArray2[allArray2Index] = floatArray[2];
        allArray2Index++;
        allArray2[allArray2Index] = floatArray[3];
        allArray2Index++;
/*
        if(allArray2Index + floatArray.length >= allArray2.length) {
            //writeBuffer2();
            writeBuffer3(floatArray);
            return;

        }

 */
        //for(int i = 0; i < floatArray.length; i++){
        //writeBuffer.put(floatArray);



        //}
        //floatArray = null;
    }

    public void writeUnsignedInt(int in) throws IOException {
        //buffer.clear();
        int unsignedValue = in&0xffff;

        intArray[3] = (byte)(unsignedValue >>> 24);

        intArray[2] = (byte)(unsignedValue >>> 16);
        intArray[1] = (byte)(unsignedValue >>> 8);
        intArray[0] = (byte)unsignedValue;
    /*
    writeBuffer_int.position(0);
    writeBuffer_int.put(intArray);
    writeBuffer_int.position(0);
    raFile.seek(raFile.length());
    fileChannel.write(writeBuffer_int);

    if(true)
      return;

     */
        //intArray = intToByteArray(in);
        //byte[] array = buffer.allocate(4).putInt(unsignedValue).array();
    /*
    buffer.wrap(array);
    buffer.flip();
    fileChannel.write(buffer);
    */
        //fileChannel.position(fileChannel.size());
        //fileChannel.write(ByteBuffer.wrap(array));
        //allArray = concatenateByteArrays(allArray, array);


        if(allArray2Index + intArray.length >= allArray2.length) {
            //writeBuffer2();

            allArray2[allArray2Index] = intArray[0];
            allArray2Index++;
            allArray2[allArray2Index] = intArray[1];
            allArray2Index++;
            allArray2[allArray2Index] = intArray[2];
            allArray2Index++;
            allArray2[allArray2Index] = intArray[3];
            allArray2Index++;
            /*
            try {
                outputQue.put(allArray2.clone());
            }catch (Exception e){
                e.printStackTrace();
            }

            allArray2Index = 0;
            return;

             */
            output();
            allArray2Index = 0;

            return;
        }

        allArray2[allArray2Index] = intArray[0];
        allArray2Index++;
        allArray2[allArray2Index] = intArray[1];
        allArray2Index++;
        allArray2[allArray2Index] = intArray[2];
        allArray2Index++;
        allArray2[allArray2Index] = intArray[3];
        allArray2Index++;
/*
        if(allArray2Index + intArray.length >= allArray2.length) {
            //writeBuffer2();
            writeBuffer3(intArray);
            return;
        }

 */
        //for(int i = 0; i < intArray.length; i++){
        //writeBuffer.put(intArray);


        //}
        //intArray = null;

    }

    public void writeUnsignedByte(byte in) throws IOException {
        //buffer.clear();

        int unsignedValue = in&0x000000ff;
        byte unsignedValueB = (byte) unsignedValue;
    /*
    //unsignedByteArray = buffer2.allocate(1).put(unsignedValueB).array();
    writeBuffer_unsignedByte.position(0);
    writeBuffer_unsignedByte.put(unsignedValueB);
    writeBuffer_unsignedByte.position(0);
    raFile.seek(raFile.length());
    fileChannel.write(writeBuffer_unsignedByte);
    //System.out.println();

    if(true)
      return;
    */
        unsignedByteArray[0] = unsignedValueB;
        //array[0] = unsignedValueB;
    /*
    buffer.wrap(array);
    buffer.flip();
    fileChannel.write(buffer);
    */

        //fileChannel.position(fileChannel.size());
        //  System.out.println(Arrays.toString(array));
        //fileChannel.write(ByteBuffer.wrap(array));

        //allArray = concatenateByteArrays(allArray, array);



        if(allArray2Index + unsignedByteArray.length >= allArray2.length) {
            //writeBuffer2();
            allArray2[allArray2Index] = unsignedByteArray[0];
            allArray2Index++;
            /*
            try {
                outputQue.put(allArray2.clone());
            }catch (Exception e){
                e.printStackTrace();
            }

            allArray2Index = 0;
            return;

             */
            output();
            allArray2Index = 0;

            return;
        }

        allArray2[allArray2Index] = unsignedByteArray[0];
        allArray2Index++;

/*
        if(allArray2Index + unsignedByteArray.length >= allArray2.length) {
            //writeBuffer2();
            writeBuffer3(unsignedByteArray);
            return;
        }

 */
        //for(int i = 0; i < unsignedByteArray.length; i++){
        //writeBuffer.put(unsignedByteArray);


        //}
        //unsignedByteArray = null;

    }

    public void close() throws IOException{

        pwrite.writeRemaining(allArray2, allArray2Index);
        pwrite.setHeaderBlockData(this.minX, this.maxX, this.minY, this.maxY, this.minZ, this.maxZ, this.pointsByReturn);

    }

}
