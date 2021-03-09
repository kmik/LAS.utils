package LASio;
/**
 * A simple data-holder class for transferring data from
 * LAS file records.  This class is intended for efficient
 * use in applications that may process millions of records.
 * Therefore, it is designed so that instances
 * can be used and reused over and over again as temporary
 * containers for data.  Thus elements are exposed as public
 * without accessors methods or other protections.
 * <p>
 * There is, however, no restriction on creating instances of this class.
 * Depending on the requirements of the implementation, it may be completely
 * reasonable to do so. However, when millions of points are to be
 * processed, it will be advantageous to not create persistent instances.
 */
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
    /** The observation-category classification for the return */
    public int classification;
    /** Indicates that point was created by techniques other than LIDAR */
    public boolean synthetic;
    /** Indicate point is a model key point */
    public boolean keypoint;
    /** Indicates that point should not be included in processing */
    public boolean withheld;
    /** The GPS time (interpreted according to header GPS flag */
    public double gpsTime;

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

    public void setClassification(int in){

        this.classification = in;

    }
    /*
    
    */
    public LasPoint(){


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

    }

    public Object clone() throws CloneNotSupportedException{
        return super.clone();
    }

    @Override
    public String toString() {
        return  "x: " + this.x + "\n" +
                "y: " + this.y + "\n" +
                "z: " + this.z + "\n" +
                "i: " + this.intensity + "\n";
    }

    public double getGpsTime(){

        return this.gpsTime;

    }

}
