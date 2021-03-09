package utils;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class ImageRaf {

    int recordLength = 0;

    public int bufferSize = (int)Math.pow(2, 20);
    //public byte[] allArray = new byte[0];
    public byte[] allArray2 = new byte[bufferSize];

    public final File file;

    public final ByteBuffer buffer;

    public FileChannel fileChannel;

    public RandomAccessFile raFile;

    public long raFileLen;

    byte[] floatArray = new byte[4];

    public int allArray2Index = 0;

    /**
     * the position in the virtual file (not in the actual file)
     */
    public long raFilePos;

    public boolean bufferContainsData;

    int xDim;
    int yDim;


    public ImageRaf(File file, int x, int y, float val) throws IOException {

        this.xDim = x;
        this.yDim = y;

        if (file == null) {
            throw new NullPointerException();
        }

        buffer = ByteBuffer.allocateDirect(bufferSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        this.file = file;
        raFile = new RandomAccessFile(file, "rw");
        fileChannel = raFile.getChannel();
        raFileLen = raFile.length();
        raFilePos = 0;
        raFile.seek(0);
        bufferContainsData = false;

        establish(x, y, val);


    }

    public void establish(int x, int y, float value) throws IOException{

        for(int i = 0; i < x*y; i++){
            this.writeFloat(0.0f);
        }

    }

    public void writePixel(int x, int y, float value) throws IOException{

        int index = y * xDim + x;

        this.write(index, value);
    }

    public void write(int index, float val) throws IOException{

        this.raFile.seek(index * this.recordLength);
        this.writeFloat(val);

    }

    public float read(int x, int y) throws IOException{

        int index = y * xDim + x;

        raFile.seek(index * recordLength);

        return this.readFloat();
    }

    public synchronized float readFloat() throws IOException {
        prepareBufferForRead(4);
        return buffer.getFloat();
    }

    private synchronized void prepareBufferForRead(int bytesToRead) throws IOException {
        int bytesNotRead = bytesToRead;
        if (raFile == null) {
            throw new IOException("Reading from a file that was closed");
        }

        if (raFilePos >= raFileLen) {
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

    }

}
