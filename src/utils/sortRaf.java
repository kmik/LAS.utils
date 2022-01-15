package utils;

import tools.lasSort;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class sortRaf implements Closeable {

    public File file;
    public RandomAccessFile raFile;
    public int bufferSize = (int)Math.pow(2, 24);
    public byte[] allArray2 = new byte[bufferSize];
    public int allArray2Index = 0;

    public final ByteBuffer buffer;
    public long raFilePos;

    public FileChannel fileChannel;

    public boolean bufferContainsData;

    byte[] intArray = new byte[4];
    byte[] floatArray = new byte[4];
    byte[] doubleArray = new byte[8];

    public long raFileLength;


    /**
     * Closes the file resources used by the referenced instance.
     * No further file access will be possible.
     *
     * @throws IOException In the event of an unrecoverable I/O condition.
     */

    @Override
    public void close() throws IOException {


    }

    public sortRaf(File file) throws IOException{

        buffer = ByteBuffer.allocateDirect(bufferSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.file = file;
        raFile = new RandomAccessFile(file, "rw");

        this.raFileLength = raFile.length();

        raFilePos = 0;
        raFile.seek(0);

        fileChannel = raFile.getChannel();

        this.bufferContainsData = false;

    }


    public synchronized void writeInt(int in) throws IOException {

        intArray[3] = (byte)(in >>> 24);

        intArray[2] = (byte)(in >>> 16);
        intArray[1] = (byte)(in >>> 8);
        intArray[0] = (byte)in;


        if(allArray2Index + intArray.length >= allArray2.length) {
            //writeBuffer2();
            writeBuffer3(intArray);
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

    public synchronized void writeBuffer3(byte[] leftOvers) throws IOException{

        int count = 0;

        for(int i = allArray2Index; i < allArray2.length; i++) {
            allArray2[i] = leftOvers[count];
            //writeBuffer.put(leftOvers[count]);
            count++;
        }

        raFile.write(allArray2);

        allArray2Index = 0;

        for(int i = count; i < (leftOvers.length); i++) {
            allArray2[allArray2Index] = leftOvers[i];
            //writeBuffer.put(leftOvers[i]);
            allArray2Index++;
        }
        //this.raFileLength = raFile.length();

    }

    public synchronized void writeBuffer2() throws IOException{

        byte[] temp = new byte[allArray2Index];

        for(int i = 0; i < allArray2Index; i++)
            temp[i] = allArray2[i];

        raFile.seek(raFile.length());
        raFile.write(temp);

        allArray2 = new byte[bufferSize];

        allArray2Index = 0;

        //this.raFileLength = raFile.length();
    }

    public synchronized void writeFloat(float in) throws IOException {
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
            writeBuffer3(floatArray);
            return;

        }
        //for(int i = 0; i < floatArray.length; i++){
        //writeBuffer.put(floatArray);

        allArray2[allArray2Index] = floatArray[0];
        allArray2Index++;
        allArray2[allArray2Index] = floatArray[1];
        allArray2Index++;
        allArray2[allArray2Index] = floatArray[2];
        allArray2Index++;
        allArray2[allArray2Index] = floatArray[3];
        allArray2Index++;

        //}
        //floatArray = null;
    }


    public synchronized void read(int n_bytes) throws IOException{

        prepareBufferForRead(n_bytes);

    }

    private synchronized void prepareBufferForRead(int bytesToRead) throws IOException {

        int bytesNotRead = bytesToRead;
        if (raFile == null) {
            throw new IOException("Reading from a file that was closed");
        }

        if (raFilePos >= raFile.length()) {
            throw new EOFException("Something wrong with file: " + this.file.getAbsolutePath());
        }

        boolean readEnabled = true;
        if (bufferContainsData) {
            int remaining = buffer.remaining();
            if (remaining >= bytesNotRead) {
                readEnabled = false;
            } else {
                // remaining < nBytes
                readEnabled = true;
                if (remaining == 0) {
                    buffer.clear();
                } else {
                    buffer.compact();
                    // note that we have to tweak our bookkeeping here
                    // because we have a partial in the buffer... so we need
                    // to advance the rafPosition ahead so that we don't re-read
                    // what we've already pulled in.
                    raFilePos += remaining;
                    bytesNotRead -= remaining;
                }
            }
        }


        if (readEnabled) {
            raFile.seek(raFilePos);
            fileChannel.read(buffer);
            buffer.flip();
            bufferContainsData = true;
        }
        raFilePos += bytesNotRead;
    }

    public void readFromBuffer(lasSort.Pair_float in){

        in.setIndex(buffer.getInt());
        in.setValue(buffer.getDouble());

    }

    public void readFromBuffer2(lasSort.Pair_z in){

        in.setIndex(buffer.getInt());
        in.setZ(buffer.getInt());

    }

    /**
     * Sets the virtual file-pointer position measured from the
     * beginning of the file.
     *
     * @param position The file position, measured in bytes from the
     * beginning of the file at which to set the virtual file position.
     * @throws IOException In the event of an unrecoverable I/O condition.
     */
    public synchronized void seek(long position) throws IOException {

        if (bufferContainsData) {
            int bufferPosition = buffer.position();
            int bufferRemaining = buffer.remaining();
            long pos0 = raFilePos - bufferPosition;
            long pos1 = raFilePos + bufferRemaining - 1;
            if (pos0 <= position && position <= pos1) {
                raFilePos = position;
                long bufferPos = position - pos0;
                buffer.position((int) bufferPos);
                return;
            }
        }

        bufferContainsData = false;
        buffer.clear();
        raFilePos = position;
    }

    public synchronized void writeDouble(double in) throws IOException {
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

        if(allArray2Index + doubleArray.length >= allArray2.length) {
            //writeBuffer2();
            writeBuffer3(doubleArray);
            return;
        }
        //writeBuffer.put(doubleArray);
        //for(int i = 0; i < doubleArray.length; i++){

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

        //}

    }


}
