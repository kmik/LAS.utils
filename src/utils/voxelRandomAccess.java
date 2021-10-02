package utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import static LASio.LASraf.intToByteArray;

public class voxelRandomAccess implements Closeable {

    public int length;
    public int xDim;
    public int yDim;
    public int zDim;



    private static final int DEFAULT_BUFFER_SIZE = 4 * 1024;
    public int bufferSize = 4 * 1024;
    public byte[] allArray2 = new byte[bufferSize];
    public int allArray2Index = 0;

    File file;

    public int n;

    //long raFilePos;
    long filePosition = 0;


    /**
     * The byte buffer used for reading (and eventually writing) data
     */
    ByteBuffer buffer;

    /**
     * The instance of the random-access file.
     */
    RandomAccessFile raFile;

    /**
     * The file channel obtained from the random-access file
     */
    FileChannel fileChannel;
    /**
     * the length of the random-access file being read
     */
    long raFileLen;

    /**
     * the position in the virtual file (not in the actual file)
     */
    long raFilePos;

    /**
     * Indicates that at least some data has been read into the buffer.
     */
    boolean bufferContainsData;

    public voxelRandomAccess() throws IOException {

        buffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        file = new File("voxel_temp.vox");

        if(file.exists())
            file.delete();

        file.createNewFile();

        raFile = new RandomAccessFile(file, "rw");
        fileChannel = raFile.getChannel();
        raFileLen = raFile.length();
        raFilePos = 0;
        raFile.seek(0);
        bufferContainsData = false;
    }

    public void setDims(int x, int y, int z) throws IOException{

        this.xDim = x;
        this.yDim = y;
        this.zDim = z;

        create();

    }

    private void create() throws IOException{

        if(file.exists()){
            file.delete();
        }

        file.createNewFile();

        raFile = new RandomAccessFile(file, "rw");
        fileChannel = raFile.getChannel();
        raFileLen = raFile.length();
        raFilePos = 0;
        raFile.seek(0);
        bufferContainsData = false;

        this.n = this.xDim * this.yDim * this.zDim;
        int writeInt = -999;

        for(int i = 0; i < n; i++){
            this.write(i, -999);
        }

        this.writeBuffer2();
        this.raFile.seek(0);

        raFileLen = raFile.length();
        raFile.seek(0);

        for(int i = 0; i < n; i++){

            //System.out.println(this.read(i) + " " + i + "/" + n);
        }


        System.out.println("raFilelength: " + raFileLen );
    }

    public void write(int index, int value) throws IOException{

        //this.filePosition = index;
        this.raFilePos = index * 4;
        this.seek(raFilePos);

        this.writeInt(value);

    }

    public void writeDirect(int index, int value) throws IOException{

        //this.filePosition = index;
        this.raFilePos = index * 4;
        raFile.seek(raFilePos);

        raFile.write(intToByteArray(value));

    }

    public int read(int index) throws IOException{

        this.raFilePos = index * 4;
        //this.filePosition = index;
        raFile.seek(raFilePos);
        return this.readInt();

    }

    public void flush() throws IOException{
        this.writeBuffer2();
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

        fileChannel = null;
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
            throw new EOFException();
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

    public synchronized void writeBuffer2() throws IOException{

        byte[] temp = new byte[allArray2Index];

        for(int i = 0; i < allArray2Index; i++)
            temp[i] = allArray2[i];

        //fileChannel.write(buffer);
        //buffer.wrap(allArray);
        //System.out.println(temp.length);
        raFile.seek(raFile.length());
        raFile.write(temp);
        //fileChannel.write(ByteBuffer.wrap(temp));

        allArray2 = new byte[bufferSize];

        //allArray = new byte[0];
        allArray2Index = 0;

    }

    public synchronized int readInt() throws IOException {
        prepareBufferForRead(4);
        return buffer.getInt();
    }

    public synchronized void writeInt(int in) throws IOException {
        //buffer.clear();
        byte[] array = intToByteArray(in);
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

        if (allArray2Index + array.length >= allArray2.length)
            writeBuffer2();

        for (int i = 0; i < array.length; i++) {

            allArray2[allArray2Index] = array[i];
            allArray2Index++;

        }
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
}
