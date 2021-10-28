package utils;

/*
    What we need to keep a track of??

    index is 0 - n , where n = number of grid cells

    In each index, we write all the statistics (a lot of doubles per grid cell).
    Also, the spatial extent of the points within the grid.

 */

import LASio.LASReader;
import LASio.LasPoint;
import LASio.PointInclusionRule;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Accesses a random access file using a ByteBuffer with little-endian
 * byte order in support of the LAS file format.
 */
public class gridRAF implements Closeable {

    public int writtenPoints = 0;

    public Byte myByte = new Byte("00000000");
    public byte myBitti = myByte.byteValue();

    /**
     * The default size for the read (and eventually write) buffer
     */
    private static final int DEFAULT_BUFFER_SIZE = (int)Math.pow(2, 12);
    private static final int DEFAULT_BUFFER_SIZE2 = (int)Math.pow(2, 12);

    public boolean writeInProgress = false;

    public int bufferSize = (int)Math.pow(2, 20);
    //public byte[] allArray = new byte[0];
    public byte[] allArray2 = new byte[bufferSize];
    public int allArray2Index = 0;
    /**
     * A reference to the file with which this instance is associated
     */
    public final File file;

    /**
     * The byte buffer used for reading (and eventually writing) data
     */
    public final ByteBuffer buffer;
    //public final ByteBuffer buffer2;
    public ByteBuffer writeBuffer;
    public ByteBuffer temp;
    /**
     * The instance of the random-access file.
     */
    public RandomAccessFile raFile;
    public long raFileIndex = 0;
    /**
     * The file channel obtained from the random-access file
     */
    public FileChannel fileChannel;
    /**
     * the length of the random-access file being read
     */
    public long raFileLen;

    /**
     * the position in the virtual file (not in the actual file)
     */
    public long raFilePos;

    /**
     * Indicates that at least some data has been read into the buffer.
     */
    public boolean bufferContainsData;

    public FileOutputStream fileOuputStream;

    byte[] booleanArray;
    byte[] unsignedByteArray = new byte[1];
    byte[] byteArray = new byte[1];
    byte[] intArray = new byte[4];
    byte[] unsignedShortArray = new byte[2];
    byte[] shortArray = new byte[2];
    byte[] longArray =  new byte[8];
    byte[] floatArray = new byte[4];
    byte[] doubleArray = new byte[8];
  /*
  public ByteBuffer writeBuffer_boolean;
  public ByteBuffer writeBuffer_unsignedByte;
  public ByteBuffer writeBuffer_byte;
  public ByteBuffer writeBuffer_int;
  public ByteBuffer writeBuffer_unsignedShort;
  public ByteBuffer writeBuffer_short;
  public ByteBuffer writeBuffer_long;
  public ByteBuffer writeBuffer_float;
  public ByteBuffer writeBuffer_double;
  public ByteBuffer writeBuffer_ascii_32;
  public ByteBuffer writeBuffer_ascii_8;
  */

    double xScaleFactor;
    double yScaleFactor;
    double zScaleFactor;
    double xOffset;
    double yOffset;
    double zOffset;
    int pointDataRecordFormat;

    /**
     * Opens the specified file for read-only, random-access.
     * A buffer is created to provide efficient input operations
     * across a series of sequential method calls.  In accordance with
     * the LAS standard, all data in the file is treated as being in
     * little-endian byte order.
     *
     * @param file A valid file object.
     * @throws IOException In the event of an unrecoverable IO condition
     * such as file not found, access denied, etc.
     */
    public gridRAF(File file) throws IOException {

        if (file == null) {
            throw new NullPointerException();
        }

        //fileOuputStream = new FileOutputStream(file.getName(), true);
        buffer = ByteBuffer.allocateDirect(bufferSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);


        //buffer2 = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE2);
        //buffer2.order(ByteOrder.LITTLE_ENDIAN);

        //writeBuffer = ByteBuffer.allocateDirect(bufferSize);
        //writeBuffer.order(ByteOrder.LITTLE_ENDIAN);

        /*             */

    /*
    writeBuffer_boolean = ByteBuffer.allocateDirect(1);
    writeBuffer_boolean.order(ByteOrder.LITTLE_ENDIAN);

    writeBuffer_unsignedByte = ByteBuffer.allocateDirect(1);
    writeBuffer_unsignedByte.order(ByteOrder.LITTLE_ENDIAN);

    writeBuffer_byte = ByteBuffer.allocateDirect(1);
    writeBuffer_byte.order(ByteOrder.LITTLE_ENDIAN);

    writeBuffer_int = ByteBuffer.allocateDirect(4);
    writeBuffer_int.order(ByteOrder.LITTLE_ENDIAN);

    writeBuffer_unsignedShort = ByteBuffer.allocateDirect(2);
    writeBuffer_unsignedShort.order(ByteOrder.LITTLE_ENDIAN);

    writeBuffer_short = ByteBuffer.allocateDirect(2);
    writeBuffer_short.order(ByteOrder.LITTLE_ENDIAN);

    writeBuffer_long = ByteBuffer.allocateDirect(8);
    writeBuffer_long.order(ByteOrder.LITTLE_ENDIAN);

    writeBuffer_float = ByteBuffer.allocateDirect(4);
    writeBuffer_float.order(ByteOrder.LITTLE_ENDIAN);

    writeBuffer_double = ByteBuffer.allocateDirect(8);
    writeBuffer_double.order(ByteOrder.LITTLE_ENDIAN);

    writeBuffer_ascii_32 = ByteBuffer.allocateDirect(32);
    writeBuffer_ascii_32.order(ByteOrder.LITTLE_ENDIAN);
    writeBuffer_ascii_8 = ByteBuffer.allocateDirect(8);
    writeBuffer_ascii_8.order(ByteOrder.LITTLE_ENDIAN);
    */
        this.file = file;
        raFile = new RandomAccessFile(file, "rw");
        fileChannel = raFile.getChannel();
        raFileLen = raFile.length();
        raFilePos = 0;
        raFile.seek(0);
        bufferContainsData = false;

    }

    public boolean check(){
        return raFileLen >= 250;
    }

    public void refresh() throws IOException{

        this.raFileLen = raFile.length();

    }

    /**
     * Closes the file resources used by the referenced instance.
     * No further file access will be possible.
     *
     * @throws IOException In the event of an unrecoverable I/O condition.
     */

    @Override
    public void close() throws IOException {
        if (raFile != null) {
            raFile.close();
            raFile = null;
        }
        //fileOuputStream.close();
        fileChannel = null;
    }

    /**
     * Gets a File object referring to the currently open file (if any).
     *
     * @return A valid object if defined; otherwise, null.
     */
    public File getFileReference() {
        return file;
    }

    /**
     * Gets the current size of the file in bytes.
     *
     * @return A long integer giving file size in bytes.
     */
    public long getFileSize() {
        return raFileLen;
    }

    /**
     * Provides the current position within the random-access file.
     *
     * @return a long integer value giving offset in bytes from beginning of file.
     */
    public long getFilePosition() {
        return raFilePos;
    }

    /**
     * Prepares the read buffer to access the specified number of
     * bytes. Adjusts internal elements accordingly. If the seek method
     * was previously called, an actual read may not yet occurred but
     * will be executed by this method.
     * at least the required number of bytes.
     *
     * @param bytesToRead Number of bytes to be read, must not exceed
     * the size of the buffer.
     * @throws IOException In the event of an unrecoverable I/O condition.
     */
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

    public synchronized void read(int n_bytes) throws Exception{

        prepareBufferForRead(n_bytes);

    }

    /**
     * Reads a C/C++ style null-terminated string of a specified maximum
     * length from the from data file. The source data is treated as specifying
     * a string as one byte per character following the ISO-8859-1 standard.
     * If a zero byte is encountered in the sequence, the string is terminated.
     * Otherwise, it is extended out to the maximum length. Regardless of
     * how many characters are read, the file position is always adjusted
     * forward by the maximum length.
     *
     * @param maximumLength Maximum number of bytes to be read from file.
     * @return A valid, potentially empty string.
     * @throws IOException In the event of an unrecoverable I/O condition.
     */
    public synchronized String readAscii(int maximumLength) throws IOException {
        if (maximumLength <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(maximumLength);
        utils.gridRAF.this.readAscii(builder, maximumLength);
        return builder.toString();
    }

    public synchronized void writeAscii(int maximumLength, String in) throws IOException {

        //buffer.clear();
        byte[] array = in.getBytes();

        if(array.length < maximumLength)
            array = Arrays.copyOf(array, maximumLength);

        //buffer.allocateDirect(in.length());
    /*
    buffer.wrap(array);
    System.out.println(buffer);
    buffer.flip();
    buffer.compact();
    System.out.println("File size: " + raFile.length());
    */
        /*
    if(array.length > 10) {
      writeBuffer_ascii_32.position(0);
      writeBuffer_ascii_32.put(array);
      writeBuffer_ascii_32.position(0);
      raFile.seek(raFile.length());
      fileChannel.write(writeBuffer_ascii_32);
    }
    else{
      writeBuffer_ascii_8.position(0);
      writeBuffer_ascii_8.put(array);
      writeBuffer_ascii_8.position(0);
      raFile.seek(raFile.length());
      fileChannel.write(writeBuffer_ascii_8);
    }

      if(true)
          return;

         */
        //fileChannel.position(fileChannel.size());
        //fileChannel.write(ByteBuffer.wrap(array));

        //allArray = concatenateByteArrays(allArray, array);

        if(allArray2Index + array.length >= allArray2.length){
            writeBuffer2();
            System.out.println("HEHEHFKAJFASDF");
        }

        //writeBuffer.put(array);

        for(int i = 0; i < array.length; i++){

            allArray2[allArray2Index] = array[i];
            allArray2Index++;

        }

        //buffer.put(array);
        //System.out.println(fileChannel.write(buffer));

        //for(int i = 0; i < in.length(); i++)
        //fileChannel.write((byte)in.charAt(i));
    }

    byte[] concatenateByteArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }



    /**
     * Reads a C/C++ style null-terminated string of a specified maximum
     * length from the from data file. The source data is treated as specifying
     * a string as one byte per character following the ISO-8859-1 standard.
     * If a zero byte is encountered in the sequence, the string is terminated.
     * Otherwise, it is extended out to the maximum length. Regardless of
     * how many characters are read, the file position is always adjusted
     * forward by the maximum length.
     *
     * @param builder The StringBuilder to which data is appended; if
     * the builder already contains text, then it will not be clear out
     * before the data is written.
     * @param maximumLength Maximum number of bytes to be read from file.
     * @return Number of valid characters extracted from the file before
     * a null was encountered.
     * @throws IOException In the event of an unrecoverable I/O condition.
     */
    public synchronized int readAscii(final StringBuilder builder, final int maximumLength)
            throws IOException {
        if (maximumLength <= 0) {
            return 0;
        }
        int k = 0;
        while (k < maximumLength) {
            k++;
            int b = this.readByte();
            if (b == 0) {
                if (k < maximumLength) {
                    skipBytes(maximumLength - k);
                }
                return k;
            }
            builder.append((char) (b & 0xff));
        }
        return maximumLength;
    }


    /**
     * Reads one input byte and returns <code>true</code> if that byte is nonzero,
     * <code>false</code> if that byte is zero. This method may be used to
     * read the byte written by the writeBoolean method of interface DataOutput.
     *
     * @return The boolean value read from the file.
     * @throws IOException In the event of an unrecoverable I/O condition.
     */

    public synchronized boolean readBoolean() throws IOException {
        prepareBufferForRead(1);
        byte test = buffer.get();

        return test != 0;
    }

    public synchronized void writeBoolean(int in) throws IOException {
        //.clear();
        //byte[] array //buffer.allocate(1).putInt(in).array();

        booleanArray[0] = (byte)in;


        if(allArray2Index + booleanArray.length >= allArray2.length) {
            //writeBuffer2();
            writeBuffer3(booleanArray);
            return;
        }
        //writeBuffer.put(booleanArray);
        //for(int i = 0; i < booleanArray.length; i++){

        allArray2[allArray2Index] = booleanArray[0];
        allArray2Index++;

        //}

    }

    /**
     * Reads and returns one input byte. The byte is treated as a signed
     * value in the range -128 through 127, inclusive. This method may be used
     * to read the byte written by the writeByte method of interface DataOutput.
     *
     * @return The 8-bit value read.
     * @throws IOException In the event of an unrecoverable I/O condition.
     */

    public synchronized byte readByte() throws IOException {
        prepareBufferForRead(1);
        //return raFile.readByte();

        return buffer.get();
    }
/*
  public synchronized void writeByte(byte in) throws IOException {
    //buffer.clear();
    byteArray = buffer2.allocate(1).put(in).array();



    //array[0] = in;
    /*
    buffer.wrap(array);
    buffer.flip();
    fileChannel.write(buffer);

    //fileChannel.position(fileChannel.size());
    //fileChannel.write(ByteBuffer.wrap(array));

    //allArray = concatenateByteArrays(allArray, array);




    if(allArray2Index + byteArray.length >= allArray2.length) {
      //writeBuffer2();
      writeBuffer3(byteArray);
      return;
    }
    //for(int i = 0; i < byteArray.length; i++){
    //writeBuffer.put(byteArray);
      allArray2[allArray2Index] = byteArray[0];
      allArray2Index++;

    //}

    byteArray = null;

  }
*/

    /**
     * Reads one input byte and returns an integer value
     * in the range 0 through 255.
     *
     * @return An integer primitive based on the unsigned value of a byte
     * read from the source file.
     * @throws IOException In the event of an unrecoverable I/O condition.
     */

    public synchronized int readUnsignedByte() throws IOException {
        prepareBufferForRead(1);

        return (int) (buffer.get()) & 0x000000ff;
    }

    public synchronized void writeUnsignedByte(byte in) throws IOException {
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
            writeBuffer3(unsignedByteArray);
            return;
        }
        //for(int i = 0; i < unsignedByteArray.length; i++){
        //writeBuffer.put(unsignedByteArray);
        allArray2[allArray2Index] = unsignedByteArray[0];
        allArray2Index++;

        //}
        //unsignedByteArray = null;

    }



    /**
     * Reads 4 bytes given in little-endian order and and returns
     * a Java long primitive given values in the range 0 through 4294967295.
     *
     * @throws IOException In the event of an unrecoverable I/O condition.
     * @return a Java long correctly interpreted from the unsigned integer
     * (4-byte) value stored in the data file.
     */
    public synchronized long readUnsignedInt() throws IOException {
        prepareBufferForRead(4);
        return  (long)(buffer.getInt())&0xffffffffL;
    }

    public synchronized void writeUnsignedInt(int in) throws IOException {
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
            writeBuffer3(intArray);
            return;
        }
        //for(int i = 0; i < intArray.length; i++){
        //writeBuffer.put(intArray);
        allArray2[allArray2Index] = intArray[0];
        allArray2Index++;
        allArray2[allArray2Index] = intArray[1];
        allArray2Index++;
        allArray2[allArray2Index] = intArray[2];
        allArray2Index++;
        allArray2[allArray2Index] = intArray[3];
        allArray2Index++;

        //}
        //intArray = null;

    }

    /**
     * Read two bytes  and returns a
     * Java int primitive.
     *
     * @return A Java integer primitive in the range 0 to 65535.
     * @throws IOException In the event of an unrecoverable I/O condition.
     */
    public synchronized int readUnsignedShort() throws IOException {
        prepareBufferForRead(2);
        return (int)(buffer.getShort())&0xffff;
    }

    public synchronized void writeUnsignedShort(int in) throws IOException {   // POSSIBLY WRONG!!!!!
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
            writeBuffer3(unsignedShortArray);
            return;
        }
        //for(int i = 0; i < unsignedShortArray.length; i++){

        //writeBuffer.put(unsignedShortArray);
        allArray2[allArray2Index] = unsignedShortArray[0];
        allArray2Index++;
        allArray2[allArray2Index] = unsignedShortArray[1];
        allArray2Index++;
        //}

    }

    /**
     * Read 4 bytes and return Java integer.
     *
     * @return A Java integer primitive.
     * @throws IOException In the event of an unrecoverable I/O condition.
     */
    public synchronized int readInt() throws IOException {
        prepareBufferForRead(4);
        return buffer.getInt();
    }

    public synchronized void writeInt(int in) throws IOException {
        //buffer.clear();

        //intArray = intToByteArray(in);
        intArray[3] = (byte)(in >>> 24);

        intArray[2] = (byte)(in >>> 16);
        intArray[1] = (byte)(in >>> 8);
        intArray[0] = (byte)in;
        // byte[] array = buffer.allocate(4).putInt(in).array();
        //buffer.allocate(4);
        // buffer.wrap(array);
        //buffer.flip();
    /*
    buffer.wrap(array);
    buffer.flip();
    fileChannel.write(buffer);
    */
        //fileChannel.position(fileChannel.size());
        //fileChannel.write(buffer);
        //System.out.println(Arrays.toString(array));
        //System.out.println(fileOuputStream)
        //fileChannel.write(ByteBuffer.wrap(array));

        //allArray = concatenateByteArrays(allArray, array);
        /*
    writeBuffer_int.position(0);
    writeBuffer_int.put(intArray);
    writeBuffer_int.position(0);
    raFile.seek(raFile.length());
    fileChannel.write(writeBuffer_int);

    if(true)
      return;


         */

        if(allArray2Index + intArray.length >= allArray2.length) {
            //writeBuffer2();
            writeBuffer3(intArray);
            return;
        }

        //System.out.println("bif: " + bufferSize + " " + allArray2Index + " " + intArray.length + " " + writeBuffer.position() + " " + allArray2Index);
        //writeBuffer.put(intArray);
        //for(int i = 0; i < intArray.length; i++){

        allArray2[allArray2Index] = intArray[0];
        allArray2Index++;
        allArray2[allArray2Index] = intArray[1];
        allArray2Index++;
        allArray2[allArray2Index] = intArray[2];
        allArray2Index++;
        allArray2[allArray2Index] = intArray[3];
        allArray2Index++;

        // }
        /*


         */
        //intArray = null;
        //fileOuputStream.write(array);
    }
    public static synchronized byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value),
                (byte)(value >>> 8),
                (byte)(value >>> 16),
                (byte)(value >>> 24)};
    }
    /**
     * Read 4 bytes and return Java integer.
     *
     * @return A Java integer primitive.
     * @throws IOException In the event of an unrecoverable I/O condition.
     */
    public synchronized int readIntBigEndian() throws IOException {
        prepareBufferForRead(4);
        int i = buffer.getInt();
        return (i>>>24)|((i>>8)&0x0000ff00)|((i<<8)&0x00ff0000)|(i<<24);
    }



    /**
     * Read 8 bytes from the file and returns a Java double.
     *
     * @return A Java double primitive.
     * @throws IOException In the event of an unrecoverable I/O condition.
     *
     */
    public synchronized double readDoubleBigEndian() throws IOException {
        prepareBufferForRead(8);
        long r = buffer.getLong();
        long b
                = (r >>> 56)
                | ((r >> 48) & 0x000000000000ff00L)
                | ((r >> 40) & 0x0000000000ff0000L)
                | ((r >> 32) & 0x00000000ff000000L)
                | ((r << 32) & 0x000000ff00000000L)
                | ((r << 40) & 0x0000ff0000000000L)
                | ((r << 48) & 0x00ff000000000000L)
                | (r << 56);
        return Double.longBitsToDouble(b);
    }
    /**
     * Read 8 bytes from the file and returns a Java double.
     *
     * @return A Java double primitive.
     * @throws IOException In the event of an unrecoverable I/O condition.
     *
     */
    public synchronized double readDouble() throws IOException {
        prepareBufferForRead(8);
        return buffer.getDouble();
    }

    public synchronized void readLine(int n_bytes, int skip) throws IOException {
        seek(skip);
        prepareBufferForRead(n_bytes);
        //return buffer.getDouble();
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

    /**
     * Reads 4 bytes from the file and returns a Java float.
     *
     * @return A Java float primitive.
     * @throws IOException In the event of an unrecoverable I/O condition.
     */
    public synchronized float readFloat() throws IOException {
        prepareBufferForRead(4);
        return buffer.getFloat();
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

    /**
     * Read 8 bytes from the file and returns a java long
     *
     * @return AJava long primitive
     * @throws IOException In the event of an unrecoverable I/O condition.
     */
    public synchronized long readLong() throws IOException {
        prepareBufferForRead(8);
        return buffer.getLong();
    }

    public synchronized void writeLong(long in) throws IOException {
        //prepareBufferForRead(8);
        //buffer.clear();
        //byte[] array =  new byte[8];// ByteBuffer.allocate(8).putLong(in).array();
        longArray[7] = (byte)(in >>> 56);
        longArray[6] = (byte)(in >>> 48);
        longArray[5] = (byte)(in >>> 40);
        longArray[4] = (byte)(in >>> 32);
        longArray[3] = (byte)(in >>> 24);
        longArray[2] = (byte)(in >>> 16);
        longArray[1] = (byte)(in >>>  8);
        longArray[0] = (byte)(in >>>  0);


        if(allArray2Index + longArray.length >= allArray2.length) {
            writeBuffer3(longArray);
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

        //}

        //return buffer.getLong();
    }

    /**
     * Reads two bytes from the file treating them as being in little-endian order
     * and returns a short.
     *
     * @return A Java short primitive in the range -32768 to 32767.
     * @throws IOException In the event of an unrecoverable I/O condition.
     */
    public synchronized short readShort() throws IOException {
        prepareBufferForRead(2);
        return buffer.getShort();
    }

    public synchronized void writeShort(short in) throws IOException {
        //prepareBufferForRead(2);
        //buffer.clear();
        //byte[] array = new byte[2];
        shortArray[0] = (byte) in;
        shortArray[1] = (byte) (in >> 8);

        if(allArray2Index + shortArray.length >= allArray2.length) {
            //writeBuffer2();
            writeBuffer3(shortArray);
            return;
        }

        allArray2[allArray2Index] = shortArray[0];
        allArray2Index++;
        allArray2[allArray2Index] = shortArray[1];
        allArray2Index++;

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

    public synchronized void seekB(long position) throws IOException {

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
    /*
    public synchronized void writeBuffer() throws IOException{


      //System.out.println(buffer);
      //byte[] arr = buffer.array();
      //fileChannel.write(buffer);
      //buffer.wrap(allArray);
      fileChannel.write(ByteBuffer.wrap(allArray));
      allArray = new byte[0];

    }
     */
    public synchronized void writeBuffer2() throws IOException{

        //System.out.println("AHOY!");
        //System.out.println(buffer);
        //byte[] arr = buffer.array();

        //System.out.println("HKFJAFKLSDF");

        byte[] temp = new byte[allArray2Index];

        for(int i = 0; i < allArray2Index; i++)
            temp[i] = allArray2[i];

        //fileChannel.write(buffer);
        //buffer.wrap(allArray);
        //System.out.println(temp.length);

        raFile.seek(raFile.length());
        raFile.write(temp);

        //fileChannel.write(writeBuffer);
        //writeBuffer = ByteBuffer.allocate(bufferSize);
        //writeBuffer.order(ByteOrder.LITTLE_ENDIAN);

        //fileChannel.write(ByteBuffer.wrap(temp));

        allArray2 = new byte[bufferSize];

        //allArray = new byte[0];
        allArray2Index = 0;

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

    /**
     * The input should be FULL, not partial or incomplete
     * @param arr
     * @throws IOException
     */
    public synchronized void write(byte[] arr, int pointLength) throws IOException{

        this.raFile.seek(this.raFile.length());
        this.raFile.write(arr);
        this.writtenPoints += arr.length / pointLength;

    }

    public synchronized void writePointCount(long in)throws IOException{

        //buffer.position(107);

        byte[] array = intToByteArray((int)in);

        raFile.seek(107);
        raFile.write(array);

    }

    public synchronized void writeMinMax(double minX, double maxX, double minY, double maxY
            , double maxZ, double minZ)throws IOException{

        int skip = 179;

        raFile.seek(skip);

        long v = Double.doubleToLongBits(maxX);
        byte [] array = new byte[8];// buffer.allocate(8).putDouble(in).array();
        array[0] = (byte)(v);
        array[1] = (byte)(v>>>8);
        array[2] = (byte)(v>>>16);
        array[3] = (byte)(v>>>24);
        array[4] = (byte)(v>>>32);
        array[5] = (byte)(v>>>40);
        array[6] = (byte)(v>>>48);
        array[7]   = (byte)(v>>>56);
        raFile.write(array);

        raFile.seek(skip + 8);
        v = Double.doubleToLongBits(minX);
        array = new byte[8];// buffer.allocate(8).putDouble(in).array();
        array[0] = (byte)(v);
        array[1] = (byte)(v>>>8);
        array[2] = (byte)(v>>>16);
        array[3] = (byte)(v>>>24);
        array[4] = (byte)(v>>>32);
        array[5] = (byte)(v>>>40);
        array[6] = (byte)(v>>>48);
        array[7]   = (byte)(v>>>56);
        raFile.write(array);

        raFile.seek(skip + 8 * 2);
        v = Double.doubleToLongBits(maxY);
        array = new byte[8];// buffer.allocate(8).putDouble(in).array();
        array[0] = (byte)(v);
        array[1] = (byte)(v>>>8);
        array[2] = (byte)(v>>>16);
        array[3] = (byte)(v>>>24);
        array[4] = (byte)(v>>>32);
        array[5] = (byte)(v>>>40);
        array[6] = (byte)(v>>>48);
        array[7]   = (byte)(v>>>56);
        raFile.write(array);

        raFile.seek(skip + 8 * 3);
        v = Double.doubleToLongBits(minY);
        array = new byte[8];// buffer.allocate(8).putDouble(in).array();
        array[0] = (byte)(v);
        array[1] = (byte)(v>>>8);
        array[2] = (byte)(v>>>16);
        array[3] = (byte)(v>>>24);
        array[4] = (byte)(v>>>32);
        array[5] = (byte)(v>>>40);
        array[6] = (byte)(v>>>48);
        array[7]   = (byte)(v>>>56);
        raFile.write(array);

        raFile.seek(skip + 8 * 4);
        v = Double.doubleToLongBits(maxZ);
        array = new byte[8];// buffer.allocate(8).putDouble(in).array();
        array[0] = (byte)(v);
        array[1] = (byte)(v>>>8);
        array[2] = (byte)(v>>>16);
        array[3] = (byte)(v>>>24);
        array[4] = (byte)(v>>>32);
        array[5] = (byte)(v>>>40);
        array[6] = (byte)(v>>>48);
        array[7]   = (byte)(v>>>56);
        raFile.write(array);

        raFile.seek(skip + 8 * 5);
        v = Double.doubleToLongBits(minZ);
        array = new byte[8];// buffer.allocate(8).putDouble(in).array();
        array[0] = (byte)(v);
        array[1] = (byte)(v>>>8);
        array[2] = (byte)(v>>>16);
        array[3] = (byte)(v>>>24);
        array[4] = (byte)(v>>>32);
        array[5] = (byte)(v>>>40);
        array[6] = (byte)(v>>>48);
        array[7]   = (byte)(v>>>56);
        raFile.write(array);

    }

    public synchronized void writePByReturn(long[] in)throws IOException{

        int skip = 111;

        raFile.seek(skip);
        byte[] array = intToByteArray((int)in[0]);
        raFile.write(array);

        raFile.seek(skip + 4);
        array = intToByteArray((int)in[1]);
        raFile.write(array);

        raFile.seek(skip + 4 * 2);
        array = intToByteArray((int)in[2]);
        raFile.write(array);

        raFile.seek(skip + 4 * 3);
        array = intToByteArray((int)in[3]);
        raFile.write(array);

        raFile.seek(skip + 4 * 4);
        array = intToByteArray((int)in[4]);
        raFile.write(array);

    }



    /**
     * Makes an attempt to advance the virtual file position by <code>n</code>
     * bytes in order to match the functionality of the DataInput interface.
     *
     * @param n The number of bytes byte which to advance the file position
     * @return the number of bytes skipped.
     * @throws IOException In the event of an unrecoverable I/O condition.
     */

    public synchronized int skipBytes(int n) throws IOException {

        raFilePos += n;
        if (bufferContainsData) {
            int remaining = buffer.remaining();
            if (n < remaining) {
                int position = buffer.position();
                buffer.position(position + n);
            } else {
                buffer.clear();
                bufferContainsData = false;
            }
        }
        return n;
    }

    public synchronized void writeHeader(String softwareName, int major, int minor, int pointDataType, int pointDataRecordLength) throws IOException{

        //System.out.println("HERE WE GO!");

        this.writeAscii(4, "LASF");

        /* File source ID */
        this.writeUnsignedShort((short)2); // = braf.readUnsignedShort();


        /* Global encoding */
        this.writeUnsignedShort((short)1); // = braf.readUnsignedShort();

        /* ID */
        this.writeLong(0);

        /* GUID */
        this.writeAscii(8, "");


        /* Version major */
        this.writeUnsignedByte((byte)1);// = braf.readUnsignedByte();

        /* Version minor */
        this.writeUnsignedByte((byte)2);// = braf.readUnsignedByte();

        /* System identified */
        this.writeAscii(32, "LASutils (c) by Mikko Kukkonen");// systemIdentifier = braf.readAscii(32);

        /* Generating software */
        this.writeAscii(32, (softwareName + " version 0.1"));// generatingSoftware = braf.readAscii(32);

        //System.out.println(from.generatingSoftware);
        //Date now = new Date();     // Gets the current date and time
        int year = Calendar.getInstance().get(Calendar.YEAR);

        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

        /* File creation date */
        this.writeUnsignedShort((short)dayOfYear);// = braf.readUnsignedShort();

        /* File creation year */
        this.writeUnsignedShort((short)(year));// = braf.readUnsignedShort();

        /* Header size */
        this.writeUnsignedShort((short)227);// = braf.readUnsignedShort();

        /* Offset to point data */
        this.writeUnsignedInt(227);

        /* #Variable length records*/
        this.writeUnsignedInt(0);// = braf.readUnsignedInt();

        /* Point data format */
        this.writeUnsignedByte((byte)pointDataType);// = braf.readUnsignedByte();

        /* Point data record length */
        this.writeUnsignedShort((short)pointDataRecordLength);// = braf.readUnsignedShort();

        /* Number of point records */
        this.writeUnsignedInt(0);// = braf.readUnsignedInt();

        /* Number of points by return 0,1 ... 4, 5 */
        this.writeUnsignedInt(0);
        this.writeUnsignedInt(0);
        this.writeUnsignedInt(0);
        this.writeUnsignedInt(0);
        this.writeUnsignedInt(0);


        /* X scale factor */
        this.writeDouble(0.01);// = braf.readDouble();

        /* Y scale factor */
        this.writeDouble(0.01);// = braf.readDouble();

        /* Z scale factor */
        this.writeDouble(0.01);// = braf.readDouble();

        /* X offset */
        this.writeDouble(0);// = braf.readDouble();

        /* Y offset */
        this.writeDouble(0);// = braf.readDouble();

        /* Z offset */
        this.writeDouble(0);// = braf.readDouble();

        /* Max X */
        this.writeDouble(0);// = braf.readDouble();
        /* Min X */
        this.writeDouble(0);// = braf.readDouble();

        /* Max Y */
        this.writeDouble(0);// = braf.readDouble();
        /* Min Y */
        this.writeDouble(0);// = braf.readDouble();

        /* Max Z */
        this.writeDouble(0);// = braf.readDouble();
        /* Min Z */
        this.writeDouble(0);// = braf.readDouble();

        if(minor == 3){
            this.writeLong(0);
        }

        if(minor == 4){
            this.writeLong(0);
            this.writeLong(0);
            this.writeUnsignedInt(0);
            this.writeLong(0);

            //numberOfPointsByReturn = new long[15];
            for (int i = 0; i < 15; i++) {
                this.writeLong(0);
            }
        }

        this.writeBuffer2();

    }

    public void updateHeader(double minX, double maxX, double minY, double maxY, double minZ, double maxZ, long[] pointsByReturn) throws IOException{

        this.writePointCount(this.writtenPoints);

        this.writeMinMax(minX, maxX, minY, maxY, maxZ, minZ);
        this.writePByReturn(pointsByReturn);

    }

    /**
     * Update LAS file header information after the file has been completely written
     * @throws IOException
     */
    public void updateHeader2() throws IOException{

        long n = this.writtenPoints;
        this.writePointCount(n);

        LASReader file = new LASReader(this.file);
        //System.out.println(file.getNumberOfPointRecords());

        n = file.getNumberOfPointRecords();

        //

        LasPoint tempPoint = new LasPoint();
        long[] pointsByReturn = new long[5];
        long pointCount = 0;

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;

        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        file.braf.raFile.seek(file.braf.raFile.length());

        int maxi = 0;

        for(int i = 0; i < file.getNumberOfPointRecords(); i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(file.getNumberOfPointRecords() - i));

            try {
                file.readRecord_noRAF(i, tempPoint, maxi);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //pointCloud.braf.buffer.position(0);

            for (int j = 0; j < maxi; j++) {

                file.readFromBuffer(tempPoint);

                pointCount++;

                //System.out.println(tempPoint.x + " " + tempPoint.y + " " + tempPoint.z);

                int returnNumberi = tempPoint.returnNumber;

                if(returnNumberi < 1)
                    returnNumberi = 1;

                if(returnNumberi <= 5)
                    pointsByReturn[returnNumberi - 1]++;

                double x = tempPoint.x;
                double y = tempPoint.y;
                double z = tempPoint.z;

                if(z < minZ)
                    minZ = z;
                if(x < minX)
                    minX = x;
                if(y < minY)
                    minY = y;

                if(z > maxZ)
                    maxZ = z;
                if(x > maxX)
                    maxX = x;
                if(y > maxY)
                    maxY = y;
            }
        }
/*
    {
      for (long i = 0; i < n; i++) {

        file.readRecord(i, tempPoint);
        pointCount++;

        //System.out.println(tempPoint.x + " " + tempPoint.y + " " + tempPoint.z);

        int returnNumberi = tempPoint.returnNumber;

        if (returnNumberi < 1)
          returnNumberi = 1;

        pointsByReturn[returnNumberi - 1]++;

        double x = tempPoint.x;
        double y = tempPoint.y;
        double z = tempPoint.z;

        if (z < minZ)
          minZ = z;
        if (x < minX)
          minX = x;
        if (y < minY)
          minY = y;

        if (z > maxZ)
          maxZ = z;
        if (x > maxX)
          maxX = x;
        if (y > maxY)
          maxY = y;
        //System.out.println((int)((x - from.xOffset) / from.xScaleFactor));

      }

    }

 */
        this.writeMinMax(minX, maxX, minY, maxY, maxZ, minZ);
        this.writePByReturn(pointsByReturn);

    }

    /**
     * @deprecated
     *
     * @param tempPoint
     * @param rule

     * @param i
     * @return
     * @throws IOException
     */
    public synchronized boolean writePoint(LasPoint tempPoint, PointInclusionRule rule,
                                           int i)throws IOException{

        //xScaleFactor = 0.001;
        //yScaleFactor = 0.001;
        //zScaleFactor = 0.001;

        /* Written or not */
        boolean output = false;

        //Byte myByte = new Byte("00000000");
        //byte myBitti = myByte.byteValue();

        /* Write if rule says so */
        if(rule.ask(tempPoint, i, false)){

            /* We got here, so output true */
            output = true;

            double x = tempPoint.x;
            double y = tempPoint.y;
            double z = tempPoint.z;

            /* Scale x and apply xOffset */
            int lx = (int)((x - this.xOffset) / this.xScaleFactor);
            /* Scale y and apply yOffset */
            int ly = (int)((y - this.yOffset) / this.yScaleFactor);
            /* Scale z and apply zOffset */
            int lz = (int)((z - this.zOffset) / this.zScaleFactor);

            /* Write scaled and offset x, y and z */
            this.writeInt(lx);
            this.writeInt(ly);
            this.writeInt(lz);

            /* Write intensity */
            this.writeUnsignedShort((short)tempPoint.intensity);// braf.readUnsignedShort()

			/*
				Return Number 3 bits 					(bits 0, 1, 2) 3 bits *
				Number of Returns (given pulse) 3 bits 	(bits 3, 4, 5) 3 bits *
				Scan Direction Flag 1 bit 				(bit 6) 1 bit *
				Edge of Flight Line 1 bit 				(bit 7)
			 */
/*
			myByte = setBit(myByte, 7, (tempPoint.edgeOfFlightLine) ? 1 : 0);
			myByte = setBit(myByte, 6, tempPoint.scanDirectionFlag);
			myByte = setBit(myByte, 5, getBit((byte)tempPoint.numberOfReturns, 2));
			myByte = setBit(myByte, 4, getBit((byte)tempPoint.numberOfReturns, 1));
			myByte = setBit(myByte, 3, getBit((byte)tempPoint.numberOfReturns, 0));
			myByte = setBit(myByte, 2, getBit((byte)tempPoint.returnNumber, 2));
			myByte = setBit(myByte, 1, getBit((byte)tempPoint.returnNumber, 1));
			myByte = setBit(myByte, 0, getBit((byte)tempPoint.returnNumber, 0));

 */


            myBitti = setUnsetBit(myBitti, 7, tempPoint.edgeOfFlightLine ? 1 : 0);
            myBitti = setUnsetBit(myBitti, 6, tempPoint.scanDirectionFlag);
            myBitti = setUnsetBit(myBitti, 5, ((byte)tempPoint.numberOfReturns & (1 << (2))) > 0 ? 1 : 0);
            myBitti = setUnsetBit(myBitti, 4, ((byte)tempPoint.numberOfReturns & (1 << (1))) > 0 ? 1 : 0);
            myBitti = setUnsetBit(myBitti, 3, ((byte)tempPoint.numberOfReturns & (1 << (0))) > 0 ? 1 : 0);
            myBitti = setUnsetBit(myBitti, 2, ((byte)tempPoint.returnNumber & (1 << (2))) > 0 ? 1 : 0);
            myBitti = setUnsetBit(myBitti, 1, ((byte)tempPoint.returnNumber & (1 << (1))) > 0 ? 1 : 0);
            myBitti = setUnsetBit(myBitti, 0, ((byte)tempPoint.returnNumber & (1 << (0))) > 0 ? 1 : 0);

			/*
			myByte = setBit(myByte, 7, (tempPoint.edgeOfFlightLine) ? 1 : 0);
			myByte = setBit(myByte, 6, tempPoint.scanDirectionFlag);
			myByte = setBit(myByte, 5, (byte)tempPoint.numberOfReturns >> 2);
			myByte = setBit(myByte, 4, (byte)tempPoint.numberOfReturns >> 1);
			myByte = setBit(myByte, 3, (byte)tempPoint.numberOfReturns >> 0);
			myByte = setBit(myByte, 2, (byte)tempPoint.returnNumber >> 2);
			myByte = setBit(myByte, 1, (byte)tempPoint.returnNumber >> 1);
			myByte = setBit(myByte, 0, (byte)tempPoint.returnNumber >> 0);
			*/
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
			/*
			myByte = setBit(myByte, 7, (tempPoint.withheld) ? 1 : 0);
			myByte = setBit(myByte, 6, (tempPoint.keypoint) ? 1 : 0);
			myByte = setBit(myByte, 5, (tempPoint.synthetic) ? 1 : 0);
			myByte = setBit(myByte, 4, getBit((byte)tempPoint.classification,4));
			myByte = setBit(myByte, 3, getBit((byte)tempPoint.classification,3));
			myByte = setBit(myByte, 2, getBit((byte)tempPoint.classification,2));
			myByte = setBit(myByte, 1, getBit((byte)tempPoint.classification,1));
			myByte = setBit(myByte, 0, getBit((byte)tempPoint.classification,0));

			 */

            myBitti = setUnsetBit(myBitti, 7, (tempPoint.withheld) ? 1 : 0);
            myBitti = setUnsetBit(myBitti, 6, (tempPoint.keypoint) ? 1 : 0);
            myBitti = setUnsetBit(myBitti, 5, (tempPoint.synthetic) ? 1 : 0);
            myBitti = setUnsetBit(myBitti, 4, ((byte)tempPoint.classification & (1 << (4))) > 0 ? 1 : 0);
            myBitti = setUnsetBit(myBitti, 3, ((byte)tempPoint.classification & (1 << (3))) > 0 ? 1 : 0);
            myBitti = setUnsetBit(myBitti, 2, ((byte)tempPoint.classification & (1 << (2))) > 0 ? 1 : 0);
            myBitti = setUnsetBit(myBitti, 1, ((byte)tempPoint.classification & (1 << (1))) > 0 ? 1 : 0);
            myBitti = setUnsetBit(myBitti, 0, ((byte)tempPoint.classification & (1 << (0))) > 0 ? 1 : 0);

			/*
            myByte = setBit(myByte, 7, (tempPoint.withheld) ? 1 : 0);
            myByte = setBit(myByte, 6, (tempPoint.keypoint) ? 1 : 0);
            myByte = setBit(myByte, 5, (tempPoint.synthetic) ? 1 : 0);
            myByte = setBit(myByte, 4, (byte)tempPoint.classification >> 4);
            myByte = setBit(myByte, 3, (byte)tempPoint.classification >> 3);
            myByte = setBit(myByte, 2, (byte)tempPoint.classification >> 2);
            myByte = setBit(myByte, 1, (byte)tempPoint.classification >> 1);
            myByte = setBit(myByte, 0, (byte)tempPoint.classification >> 0);
			*/

            /* Write the byte */
            this.writeUnsignedByte(myBitti);

            /* Write scan angle */
            this.writeUnsignedByte((byte)tempPoint.scanAngleRank);

            /* Write user data */
            this.writeUnsignedByte((byte)tempPoint.userData);

            /* Write point source ID */
            this.writeUnsignedShort(tempPoint.pointSourceId);


            /* Previous stuff is pretty much standard for any point data type.
             How to do the extra stuff that is point data type specific? */

            /* RGB is included in both 2 and 3 record types in LAS 1.2 */

            if (this.pointDataRecordFormat == 1 || this.pointDataRecordFormat == 3 || this.pointDataRecordFormat == 4 || this.pointDataRecordFormat == 5) {

                this.writeDouble(tempPoint.gpsTime);// = braf.readDouble();

            }

            if(this.pointDataRecordFormat == 2 || this.pointDataRecordFormat == 3 || this.pointDataRecordFormat == 5){

                this.writeUnsignedShort(tempPoint.R);
                this.writeUnsignedShort(tempPoint.G);
                this.writeUnsignedShort(tempPoint.B);

            }

            if(this.pointDataRecordFormat == 4 || this.pointDataRecordFormat == 5){

                this.writeUnsignedByte((byte)tempPoint.WavePacketDescriptorIndex);
                this.writeLong(tempPoint.ByteOffsetToWaveformData);
                this.writeUnsignedInt(tempPoint.WaveformPacketSizeInBytes);
                this.writeFloat(tempPoint.ReturnPointWaveformLocation);

                this.writeFloat(tempPoint.x_t);
                this.writeFloat(tempPoint.y_t);
                this.writeFloat(tempPoint.z_t);
            }

        }

        if(output)
            this.writtenPoints++;

        return output;
    }



    public byte setUnsetBit(byte in, int bit, int set){

        if(set==1)
            return in |= 1 << bit;
        else
            return in &= ~(1 << bit);
    }

}

