package LASio;


import sun.misc.Cleaner;
import utils.argumentReader;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;


/**
 * Accesses a random access file using a ByteBuffer with little-endian
 * byte order in support of the LAS file format.
 */
public class LASraf implements Closeable {



  public long writtenPoints = 0;

  public Byte myByte = new Byte("00000000");
  public byte myBitti = myByte.byteValue();

  /**
   * The default size for the read (and eventually write) buffer
   */
  private static final int DEFAULT_BUFFER_SIZE = (int)Math.pow(2, 12);
  private static final int DEFAULT_BUFFER_SIZE2 = (int)Math.pow(2, 12);

  public boolean writeInProgress = false;

  public int bufferSize = (int)Math.pow(2, 22);
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
  public ByteBuffer buffer;
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
  public ArrayList<MappedByteBuffer> mbb = new ArrayList<>();
  public ArrayList<Long> mbb_starts = new ArrayList<>();
  public int whichMbb = -1;

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

  long step = 0;

  double xScaleFactor;
  double yScaleFactor;
  double zScaleFactor;
  double xOffset;
  double yOffset;
  double zOffset;
  public int pointDataRecordFormat;
  public int pointDataRecordLength;
  public long offsetToPointData;
  public ArrayList<Integer> extra_bytes = new ArrayList<>();

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
  public LASraf(File file) throws IOException {

    if (file == null) {
      System.out.println("FILE IS NULL");
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

    //System.out.println(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

  }

  public void setUpMappedByteBuffer() throws IOException{

    //fileChannel = raFile.getChannel();

    this.step = (pointDataRecordLength * 10000000);

    if(fileChannel.size() > Integer.MAX_VALUE){

      int parts = (int)Math.ceil(((double)fileChannel.size()-(double)offsetToPointData) / (double)step);

      //System.out.println("here " + parts + " " + fileChannel.size() + " " + this.step + " " + offsetToPointData + " " + pointDataRecordLength);

      for(long i = 0; i < parts; i++){

          mbb.add(fileChannel
                  .map(FileChannel.MapMode.READ_ONLY, i * step + this.offsetToPointData, Math.min(step, fileChannel.size() - i * step)));

          mbb.get(mbb.size()-1).order(ByteOrder.LITTLE_ENDIAN);

          mbb_starts.add(i * step + this.offsetToPointData);

      }

    }else{

      mbb.add(fileChannel
              .map(FileChannel.MapMode.READ_ONLY, this.offsetToPointData, fileChannel.size() - this.offsetToPointData));

      mbb.get(mbb.size()-1).order(ByteOrder.LITTLE_ENDIAN);

      mbb_starts.add(this.offsetToPointData);

    }


    //System.out.println(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

    //System.exit(1);


  }

  public void setMBB(long filePosition, int pointDataRecordLength) throws IOException{

    this.whichMbb = Math.min((int)Math.floor((filePosition-this.offsetToPointData) / step), this.mbb.size()-1);

    try {
      this.mbb.get(this.whichMbb).position((int) (filePosition - this.mbb_starts.get(this.whichMbb)));
    }catch (Exception e){

      System.out.println((filePosition-this.offsetToPointData) + " " + fileChannel.size());

    }

  }

  public MappedByteBuffer getMbb(){
    //System.out.println(whichMbb);
    return this.mbb.get(whichMbb);

  }

  public boolean check(){
    return raFileLen >= 250;
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
    buffer.clear();

    for(int i = 0; i < mbb.size(); i++){
      mbb.get(i).clear();
    }

    allArray2 = null;

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

  private synchronized void prepareBufferForRead_not_sequential(int bytesToRead) throws IOException {

    int bytesNotRead = bytesToRead;
    if (raFile == null) {
      throw new IOException("Reading from a file that was closed");
    }

    if (raFilePos >= raFileLen) {
      throw new EOFException("Something wrong with file: " + this.file.getAbsolutePath());
    }

    boolean readEnabled = true;

    if (false) {
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

    System.out.println("READ ENABLED: " + readEnabled);
    System.out.println(buffer.remaining());
    if (readEnabled) {
      raFile.seek(raFilePos);
      fileChannel.read(buffer);

      buffer.flip();

      bufferContainsData = true;
    }
    raFilePos += bytesNotRead;
  }


  public synchronized void read(int n_bytes) throws Exception{

    //System.out.println(n_bytes + " == " + buffer.capacity());

    prepareBufferForRead(n_bytes);

  }

  public synchronized void read_not_sequential(int n_bytes) throws Exception{

    //System.out.println(n_bytes + " == " + buffer.capacity());

    prepareBufferForRead_not_sequential(n_bytes);

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
    LASraf.this.readAscii(builder, maximumLength);
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

    int unsignedValue = in&0x000000ff;
    byte unsignedValueB = (byte) unsignedValue;

    unsignedByteArray[0] = unsignedValueB;

    if(allArray2Index + unsignedByteArray.length >= allArray2.length) {
      writeBuffer3(unsignedByteArray);
      return;
    }

      allArray2[allArray2Index] = unsignedByteArray[0];
      allArray2Index++;

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

    int unsignedValue = in&0xffff;

      intArray[3] = (byte)(unsignedValue >>> 24);

      intArray[2] = (byte)(unsignedValue >>> 16);
      intArray[1] = (byte)(unsignedValue >>> 8);
      intArray[0] = (byte)unsignedValue;

    if(allArray2Index + intArray.length >= allArray2.length) {

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

    unsignedShortArray[0] = (byte)(in & 0xff);
    unsignedShortArray[1] = (byte)((in >> 8) & 0xff);

    if(allArray2Index + unsignedShortArray.length >= allArray2.length) {
      writeBuffer3(unsignedShortArray);
      return;
    }

    allArray2[allArray2Index] = unsignedShortArray[0];
    allArray2Index++;
    allArray2[allArray2Index] = unsignedShortArray[1];
    allArray2Index++;


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

  public void writeByteArray_reord(byte[] in, int pointDataRecordLength_) throws IOException{

    if(allArray2Index + in.length >= allArray2.length) {

      for(int i = 0; i < in.length; i++) {

        //

        allArray2[allArray2Index] = in[i];
        allArray2Index++;
      }
      this.write(allArray2, pointDataRecordLength_);
      allArray2Index = 0;

      return;

    }



    for(int i = 0; i < in.length; i++) {

      //

      allArray2[allArray2Index] = in[i];
      allArray2Index++;

    }

  }

  public synchronized void writeInt(int in) throws IOException {

      intArray[3] = (byte)(in >>> 24);

      intArray[2] = (byte)(in >>> 16);
      intArray[1] = (byte)(in >>> 8);
      intArray[0] = (byte)in;

    if(allArray2Index + intArray.length >= allArray2.length) {
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
  public static synchronized byte[] intToByteArray(int value) {
    return new byte[] {
            (byte)(value),
            (byte)(value >>> 8),
            (byte)(value >>> 16),
            (byte)(value >>> 24)};
  }

  public static synchronized byte[] doubleToByteArray(double value) {

    return ByteBuffer.allocate(8).putDouble(value).array();
  }

  public static synchronized byte[] longToByteArray(long value) {
    return new byte[] {
            (byte)(value),
            (byte)(value >>> 8),
            (byte)(value >>> 16),
            (byte)(value >>> 24),
            (byte)(value >>> 32),
            (byte)(value >>> 40),
            (byte)(value >>> 48),
            (byte)(value >>> 56)
    };
  }

  public static synchronized byte[] longToByteArray2(long value) {
    return new byte[] {
            (byte)(value >>> 24),
            (byte)(value >>> 16),
            (byte)(value >>> 8),
            (byte)(value)
    };
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



  public synchronized void writeDouble(double in) throws IOException {

    long v = Double.doubleToLongBits(in);

    doubleArray[0] = (byte)(v);
    doubleArray[1] = (byte)(v>>>8);
    doubleArray[2] = (byte)(v>>>16);
    doubleArray[3] = (byte)(v>>>24);
    doubleArray[4] = (byte)(v>>>32);
    doubleArray[5] = (byte)(v>>>40);
    doubleArray[6] = (byte)(v>>>48);
    doubleArray[7] = (byte)(v>>>56);


    if(allArray2Index + doubleArray.length >= allArray2.length) {
      writeBuffer3(doubleArray);
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

    int bits = Float.floatToIntBits(in);
    //byte[] array = new byte[4];
    floatArray[0] = (byte)(bits & 0xff);
    floatArray[1] = (byte)((bits >> 8) & 0xff);
    floatArray[2] = (byte)((bits >> 16) & 0xff);
    floatArray[3] = (byte)((bits >> 24) & 0xff);


    if(allArray2Index + floatArray.length >= allArray2.length) {
      writeBuffer3(floatArray);
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

    shortArray[0] = (byte) in;
    shortArray[1] = (byte) (in >> 8);

    if(allArray2Index + shortArray.length >= allArray2.length) {
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

  public synchronized void writeBuffer2() throws IOException{

    byte[] temp = new byte[allArray2Index];

    for(int i = 0; i < allArray2Index; i++)
      temp[i] = allArray2[i];

    raFile.seek(raFile.length());
    raFile.write(temp);

    allArray2 = new byte[bufferSize];

    allArray2Index = 0;

  }

  public synchronized void writeBuffer3(byte[] leftOvers) throws IOException{

    int count = 0;

    for(int i = allArray2Index; i < allArray2.length; i++) {
      allArray2[i] = leftOvers[count];
      count++;
    }

    raFile.write(allArray2);

    allArray2Index = 0;

    for(int i = count; i < (leftOvers.length); i++) {
      allArray2[allArray2Index] = leftOvers[i];
      allArray2Index++;
    }

  }

  /**
   * The input should be FULL, not partial or incomplete
   * @param arr
   * @throws IOException
   */
  public synchronized void write(byte[] arr, int pointLength) throws IOException{

    //System.exit(1);
      //System.out.println(this.raFile.length());
    if (this.raFile.getFilePointer() != this.raFile.length()) {
      this.raFile.seek(this.raFile.length());
    }
      this.raFile.seek(this.raFile.length());
      this.raFile.write(arr);
      this.writtenPoints += arr.length / pointLength;

  }

  public synchronized void writePointCount(long in)throws IOException{

    //byte[] array = intToByteArray((int)in);
    byte[] array = longToByteArray(in);

    raFile.seek(107);
    raFile.write(array);

  }

  public void writeScaleFactors(double x_scale, double y_scale, double z_scale) throws IOException{

    if(true)
      return;

    int skip = 131;

    raFile.seek(skip);
    x_scale = 0.01;
    //System.out.println(x_scale);
    //System.exit(1);
    long v = Double.doubleToLongBits(x_scale);
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

    v = Double.doubleToLongBits(y_scale);
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

    raFile.seek(skip + 16);

    v = Double.doubleToLongBits(z_scale);
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

  public synchronized void write_n_points_1_4(long in)throws IOException{

    int skip = 247;

    raFile.seek(skip);
    byte[] array = longToByteArray(in);
    raFile.write(array);

  }

  public synchronized void writePByReturn_1_4(long[] in)throws IOException{

    int skip = 255;

    for(int i = 0; i < in.length; i++){

      raFile.seek(skip + 8 * i);
      byte[] array = longToByteArray(in[i]);
      raFile.write(array);

    }



  }

  public synchronized void writePByReturn(long[] in)throws IOException{

    int skip = 111;
    //int skip = 107;

    raFile.seek(skip);
    byte[] array = intToByteArray((int)in[0]);
    //byte[] array = longToByteArray(in[0]);

    raFile.write(array);
    //System.out.println(Arrays.toString(array));

    raFile.seek(skip + 4);
    array = intToByteArray((int)in[1]);
    //array = longToByteArray(in[1]);
    raFile.write(array);
    //System.out.println(Arrays.toString(array));
    raFile.seek(skip + 4 * 2);
    array = intToByteArray((int)in[2]);
    //array = longToByteArray(in[2]);
    raFile.write(array);
    //System.out.println(Arrays.toString(array));

    raFile.seek(skip + 4 * 3);
    array = intToByteArray((int)in[3]);
    //array = longToByteArray(in[3]);
    raFile.write(array);
    //System.out.println(Arrays.toString(array));

    raFile.seek(skip + 4 * 4);
    array = intToByteArray((int)in[4]);
    //array = longToByteArray(in[4]);
    raFile.write(array);
    //System.out.println(Arrays.toString(array));

  }

  public synchronized void write_x_scale_factor(double in)throws IOException{

    int skip = 131;

    raFile.seek(skip);
    long v = Double.doubleToLongBits(in);
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

  }

  public synchronized void write_y_scale_factor(double in)throws IOException{

    int skip = 139;

    raFile.seek(skip);
    long v = Double.doubleToLongBits(in);
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

  }

  public synchronized void write_z_scale_factor(double in)throws IOException{

    int skip = 147;

    raFile.seek(skip);
    long v = Double.doubleToLongBits(in);
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

  }

  public synchronized void write_x_offset(double in)throws IOException{

    int skip = 155;

    raFile.seek(skip);
    long v = Double.doubleToLongBits(in);
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

  }

  public synchronized void write_y_offset(double in)throws IOException{

    int skip = 163;

    raFile.seek(skip);
    long v = Double.doubleToLongBits(in);
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

  }

  public synchronized void write_z_offset(double in)throws IOException{

    int skip = 171;

    raFile.seek(skip);
    long v = Double.doubleToLongBits(in);
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

  }


  /**
   * Makes an attempt to advance the virtual file position by <code>n</code>
   * bytes.
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
    int year =  Calendar.getInstance().get(Calendar.YEAR); //now.getYear();
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

      for (int i = 0; i < 15; i++) {
        this.writeLong(0);
      }
    }

    this.writeBuffer2();

  }

  public void updateHeader(double minX, double maxX, double minY, double maxY, double minZ, double maxZ, long[] pointsByReturn,
                           argumentReader aR,
                           double x_offset, double y_offset, double z_offset,
                           double x_scale, double y_scale, double z_scale	) throws IOException{


    int lx = (int)((minX - x_offset) / x_scale);
    int ly = (int)((minY - y_offset) / y_scale);
    int lz = (int)((minZ - z_offset) / z_scale);

    double min_x = lx * x_scale + x_offset;
    double min_y = ly * y_scale + y_offset;
    double min_z = lz * z_scale + z_offset;


    lx = (int)((maxX - x_offset) / x_scale);
    ly = (int)((maxY - y_offset) / y_scale);
    lz = (int)((maxZ - z_offset) / z_scale);

    double max_x = lx * x_scale + x_offset;
    double max_y = ly * y_scale + y_offset;
    double max_z = lz * z_scale + z_offset;



    this.writePointCount(this.writtenPoints);

    this.writeMinMax(min_x, max_x, min_y, max_y, max_z, min_z);

    this.writeScaleFactors(x_scale, y_scale, z_scale);

    this.writePByReturn(pointsByReturn);

  }



  public void updateHeader_1_4_2(int pointCount, double minX, double maxX, double minY, double maxY, double minZ, double maxZ, long[] pointsByReturn, long[] pointsByReturn_1_4,
                               argumentReader aR,
                               double x_offset, double y_offset, double z_offset,
                               double x_scale, double y_scale, double z_scale	) throws IOException{

    int lx = (int)((minX - x_offset) / x_scale);
    int ly = (int)((minY - y_offset) / y_scale);
    int lz = (int)((minZ - z_offset) / z_scale);

    double min_x = lx * x_scale + x_offset;
    double min_y = ly * y_scale + y_offset;
    double min_z = lz * z_scale + z_offset;


    lx = (int)((maxX - x_offset) / x_scale);
    ly = (int)((maxY - y_offset) / y_scale);
    lz = (int)((maxZ - z_offset) / z_scale);

    double max_x = lx * x_scale + x_offset;
    double max_y = ly * y_scale + y_offset;
    double max_z = lz * z_scale + z_offset;

    //this.writeMinMax(min_x, max_x, min_y, max_y, max_z, min_z);
    this.writeMinMax(minX, maxX, minY, maxY, minZ, maxZ);
    this.writeScaleFactors(x_scale, y_scale, z_scale);

    //System.out.println(this.pointDataRecordFormat);
    if(this.pointDataRecordFormat >= 6){
      this.writePointCount(0);
      this.writePByReturn(new long[]{0,0,0,0,0});
    }
    else {

      //this.writePointCount(this.writtenPoints);
      this.writePointCount(pointCount);
      this.writePByReturn(pointsByReturn);

    }



    //this.write_n_points_1_4(this.writtenPoints);
    this.write_n_points_1_4(pointCount);
    this.writePByReturn_1_4(pointsByReturn_1_4);


    System.out.println(Arrays.toString(pointsByReturn_1_4));


  }


  public void updateHeader_1_4(double minX, double maxX, double minY, double maxY, double minZ, double maxZ, long[] pointsByReturn, long[] pointsByReturn_1_4,
                           argumentReader aR,
                               double x_offset, double y_offset, double z_offset,
                               double x_scale, double y_scale, double z_scale	) throws IOException{

    int lx = (int)((minX - x_offset) / x_scale);
    int ly = (int)((minY - y_offset) / y_scale);
    int lz = (int)((minZ - z_offset) / z_scale);

    double min_x = lx * x_scale + x_offset;
    double min_y = ly * y_scale + y_offset;
    double min_z = lz * z_scale + z_offset;


    lx = (int)((maxX - x_offset) / x_scale);
    ly = (int)((maxY - y_offset) / y_scale);
    lz = (int)((maxZ - z_offset) / z_scale);

    double max_x = lx * x_scale + x_offset;
    double max_y = ly * y_scale + y_offset;
    double max_z = lz * z_scale + z_offset;

    this.writeMinMax(min_x, max_x, min_y, max_y, max_z, min_z);
    this.writeScaleFactors(x_scale, y_scale, z_scale);

    if(this.pointDataRecordFormat >= 6){
      this.writePointCount(0);
      this.writePByReturn(new long[]{0,0,0,0,0});
    }
    else {

      this.writePointCount(this.writtenPoints);
      this.writePByReturn(pointsByReturn);

    }



    this.write_n_points_1_4(this.writtenPoints);
    this.writePByReturn_1_4(pointsByReturn_1_4);

    //System.out.println(Arrays.toString(pointsByReturn_1_4));


  }


  /**
   * Update LAS file header information after the file has been completely written
   * @throws IOException
   */
  public void updateHeader2() throws IOException{

    long n = this.writtenPoints;
    this.writePointCount(n);

    LASReader file = new LASReader(this.file);

    n = file.getNumberOfPointRecords();

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

    for(int i = 0; i < this.writtenPoints; i += 10000) {

      maxi = (int) Math.min(10000, Math.abs(file.getNumberOfPointRecords() - i));

      try {
        file.readRecord_noRAF(i, tempPoint, maxi);
      } catch (Exception e) {
        e.printStackTrace();
      }

      for (int j = 0; j < maxi; j++) {

        file.readFromBuffer(tempPoint);

        pointCount++;

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



    this.writeMinMax(minX, maxX, minY, maxY, maxZ, minZ);

    this.writePByReturn(pointsByReturn);

  }


  public synchronized boolean writePoint(LasPoint tempPoint, PointInclusionRule rule,
                                      double xScaleFactor, double yScaleFactor, double zScaleFactor,
                                      double xOffset, double yOffset, double zOffset, int pointDataRecordFormat, int i)throws IOException{

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

  public int data_type_to_n_bytes(int data_type){

    int returni = -1;

    switch (data_type){

      case 0:
        returni = -1;
        break;

      case 1:
        returni = 1;
        break;

      case 2:
        returni = 1;
        break;

      case 3:
        returni = 2;
        break;

      case 4:
        returni = 2;
        break;

      case 5:
        returni = 4;
        break;

      case 6:
        returni = 4;
        break;

      case 7:
        returni = 8;
        break;

      case 8:
        returni = 8;
        break;

      case 9:
        returni = 4;
        break;

      case 10:
        returni =  8;
        break;

      case 11:
        returni = 2;
        break;

      case 12:
        returni =  2;
        break;

      case 13:
        returni = 4;
        break;

      case 14:
        returni = 4;
        break;

      case 15:
        returni = 8;
        break;

      case 16:
        returni = 8;
        break;

      case 17:
        returni = 16;
        break;

      case 18:
        returni = 8;
        break;

      case 19:
        returni = 16;
        break;

      case 20:
        returni = 8;
        break;

      case 21:
        returni = 3;
        break;

      case 22:
        returni = 3;
        break;
      case 23:
        returni = 6;
        break;
      case 24:
        returni = 6;
        break;
      case 25:
        returni = 12;
        break;
      case 26:
        returni = 12;
        break;
      case 27:
        returni = 24;
        break;

      case 28:
        returni =  24;
        break;
      case 29:
        returni =  12;
        break;
      case 30:
        returni = 24;
        break;

    }
    return returni;

  }

}
