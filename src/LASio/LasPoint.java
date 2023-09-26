package LASio;

import org.apache.commons.math3.fitting.GaussianCurveFitter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

public class LasPoint implements Cloneable {
    /** The position within the file at which the record is stored */
    public long filePosition;
    /** The X coordinate from the record, always populated */
    public double x;
    /** The Y coordinate from the record, always populated */
    public double y;
    /** The Z coordinate from the record, always populated */
    public double z;
    /** The intensity of the return at the detected point,
     * by convention normalized to the range 0 to 65535
     */
    public int intensity;
    /** The return number for the point */
    public int returnNumber;
    /** The number of returns for the pulse for which the
     * point was detected.
     */
    public int numberOfReturns;
    /** The one bit scan direction flag */
    public int scanDirectionFlag;
    /** Indicates whether the detection was at the edge of a flight line */
    public boolean edgeOfFlightLine;
    /** The classification for the return */
    public int classification;
    /** Indicates that point was created by techniques other than LIDAR */
    public boolean synthetic;
    /** Indicate point is a model key point */
    public boolean keypoint;
    /** Indicates that point should not be included in processing */
    public boolean withheld;
    /** If set, this point is within the overlap region of
     two or more swaths or takes. */
    public boolean overlap;
    /** The GPS time (interpreted according to header GPS flag */
    public double gpsTime;

    /** Scanner Channel is used to indicate the channel (scanner head) of a multichannel system. Channel 0 is used for single scanner systems. Up to four channels are
     supported (0-3). */
    public int scannerCannel;

    public int userData;

    public int scanAngleRank;

    public short pointSourceId;

    /** Red band DN value */
    public int R;
    
    /** Green band DN value */
    public int G;
    
    /** Blue band DN value */
    public int B;

    /** Near-infrared band DN value */
    public int N;

    public int WavePacketDescriptorIndex;

    public long ByteOffsetToWaveformData;

    public int WaveformPacketSizeInBytes;

    public float ReturnPointWaveformLocation;

    public float x_t;

    public float y_t;

    public float z_t;

    public double custom_double;
    public double custom_int;

    /* We should only define the extra bytes that we are actually using

     */
    ByteBuffer buffer = ByteBuffer.allocateDirect(4);


    /* If we don't care for the extra bytes */
    public ArrayList<byte[]> extra_bytes = new ArrayList<>();
    public ArrayList<byte[]> extra_bytes_custom = new ArrayList<>();

    public void setClassification(int in){

        this.classification = in;

    }

    public LasPoint(){

        //buffer.order(ByteOrder.BIG_ENDIAN);
        this.N = 0;
    }

    public LasPoint(ArrayList<Integer> extra_bytes_length){

        for(int i : extra_bytes_length)
            this.extra_bytes.add(new byte[i]);

    }

    public void addExraByte(int n){

        this.extra_bytes.add(new byte[n]);

    }

    public void setExtraByteINT(int in, int bytes, int whichOne){

        if(extra_bytes_custom.size() <= whichOne){
            while(extra_bytes_custom.size() <= whichOne)
                extra_bytes_custom.add(new byte[0]);
        }

        //System.out.println(extra_bytes_custom.size());

        extra_bytes_custom.set(whichOne,ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(in).array());
        //System.out.println(ByteBuffer.wrap(extra_bytes_custom.get(0)).getInt());

    }



    public void setExtraByteSHORT(short in, int bytes, int whichOne){

        if(extra_bytes_custom.size() <= whichOne){
            while(extra_bytes_custom.size() <= whichOne)
                extra_bytes_custom.add(new byte[0]);
        }

        //System.out.println(extra_bytes_custom.size());

        extra_bytes_custom.set(whichOne,buffer.allocate(bytes).putShort(in).array());

        //System.out.println(ByteBuffer.wrap(extra_bytes_custom.get(0)).getInt());

    }

    public void setExtraByteLONG(long in, int bytes){

        if(extra_bytes_custom.size() == 0){
            extra_bytes_custom.add(new byte[0]);
        }

        extra_bytes_custom.set(0,buffer.allocate(bytes).putLong(in).array());
        //System.out.println(ByteBuffer.wrap(extra_bytes_custom.get(0)).getInt());

    }

    public void setExtraByteDOUBLE(double in, int bytes){

        if(extra_bytes_custom.size() == 0){
            extra_bytes_custom.add(new byte[0]);
        }

        extra_bytes_custom.set(0,buffer.allocate(bytes).putDouble(in).array());
        //System.out.println(ByteBuffer.wrap(extra_bytes_custom.get(0)).getInt());

    }

    public void setExtraByteFLOAT(float in, int bytes){

        if(extra_bytes_custom.size() == 0){
            extra_bytes_custom.add(new byte[0]);
        }

        extra_bytes_custom.set(0,buffer.allocate(bytes).putFloat(in).array());
        //System.out.println(ByteBuffer.wrap(extra_bytes_custom.get(0)).getInt());

    }

    public int getExtraByteInt(int whichOne){

        return ByteBuffer.wrap(extra_bytes.get(whichOne)).order(ByteOrder.LITTLE_ENDIAN).getInt();

    }

    public int getExtraByteInt_custom(int whichOne){

        return ByteBuffer.wrap(extra_bytes_custom.get(whichOne)).order(ByteOrder.BIG_ENDIAN).getInt();

    }

    public short getExtraByteShort(int whichOne){

        return ByteBuffer.wrap(extra_bytes.get(whichOne)).order(ByteOrder.LITTLE_ENDIAN).getShort();

    }

    public float getExtraByteFloat(int whichOne){

        return ByteBuffer.wrap(extra_bytes.get(whichOne)).order(ByteOrder.LITTLE_ENDIAN).getFloat();

    }


    
    public LasPoint(LasPoint another) {

        this.filePosition = another.filePosition; 
        this.z = another.z;
        this.y = another.y;
        this.x = another.x;
        this.intensity = another.intensity;
        this.returnNumber = another.returnNumber;
        this.numberOfReturns = another.numberOfReturns;
        this.scanDirectionFlag = another.scanDirectionFlag;
        this.edgeOfFlightLine = another.edgeOfFlightLine;
        this.classification = another.classification;

        this.synthetic = another.synthetic;
        this.keypoint = another.keypoint;
        this.withheld = another.withheld;
        this.gpsTime = another.gpsTime;
        this.userData = another.userData;
        this.scanAngleRank = another.scanAngleRank;
        this.pointSourceId = another.pointSourceId;

        this.R = another.R;
        this.G = another.G;
        this.B = another.B;
        this.N = another.N;

        this.extra_bytes = another.extra_bytes;
        this.extra_bytes_custom = another.extra_bytes_custom;



    }

    public Object clone() throws CloneNotSupportedException{
        return super.clone();
    }

    @Override
    public String toString() {
        return  "x: " + this.x + "\n" +
                "y: " + this.y + "\n" +
                "z: " + this.z + "\n" +
                "i: " + this.intensity + "\n" +
                "u: " + this.userData + "\n" +
                "n: " + this.numberOfReturns + "\n" +
                "r: " + this.returnNumber + "\n";
    }

    public String getRGB(){
        return "R: " + this.R + "\n" +
                "G: " + this.G + "\n" +
                "B: " + this.B + "\n" +
                "N: " + this.N + "\n";
    }

    public double getGpsTime(){

        return this.gpsTime;

    }





}
