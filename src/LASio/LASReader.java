package LASio;

import err.lasFormatException;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;
import org.tinfour.utils.LinearUnits;
import tools.Cantor;
import utils.fileOperations;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;



@SuppressWarnings("PMD.UnusedPrivateField")
public class LASReader {

  int previousIndex = -99;


  public PointInclusionRule rule = new PointInclusionRule();

  private static final int BIT4 = 0x10;
  private static final int BIT1 = 0x01;

  TIntIntHashMap sanityCheckPointRecordLength = new TIntIntHashMap();
  /**
   * Provides definitions for the alternate methods for specifying
   * a coordinate reference system.
   */
  public enum CoordinateReferenceSystemOption {
    /**
     * The LAS file uses GeoTIFF tags to identify CRS
     */
    GeoTIFF,
    /**
     * The LAS file used Well-Known-Text to identify CRS
     */
    WKT
  }

  public boolean isIndexed = false;

  public TIntHashSet qIndex = new TIntHashSet();
  public HashMap<Integer, ArrayList<Long>> indexMap = new HashMap<>();
  public HashMap<Integer, ArrayList<int[]>> indexMap2 = new HashMap<>();
  public TLongArrayList queriedIndexes = new TLongArrayList();
  public ArrayList<int[]> queriedIndexes2 = new ArrayList<>();

  public HashSet<Integer> queried_set_of_point_indexes = new HashSet<>();

  int spacing;

  /**
   * these are variables related to reading form indexes
   */

  int index_u = 0;
  int index_minIndex = 0;
  public boolean index_read_terminated = false;
  int index_points_in_buffer = 0;
  //TIntHashSet doneIndexes = new TIntHashSet();
  TLongHashSet doneIndexes = new TLongHashSet();

  ArrayList<int[]> indexMinMax = new ArrayList<>();

  long index_p = 0;


  public String fileSignature;
  public int fileSourceID;
  public int globalEncoding;
  public int versionMajor;
  public int versionMinor;
  public String systemIdentifier;
  public String generatingSoftware;
  public int fileCreationDayOfYear;
  public int fileCreationYear;
  public Date fileCreationDate;
  public int headerSize;
  public long offsetToPointData;
  public long numberVariableLengthRecords;
  public int pointDataRecordFormat;
  public int pointDataRecordLength;
  public long legacyNumberOfPointRecords;
  public long[] legacyNumberOfPointsByReturn;
  public double xScaleFactor;
  public double yScaleFactor;
  public double zScaleFactor;
  public double xOffset;
  public double yOffset;
  public double zOffset;
  public double minX;
  public double maxX;
  public double minY;
  public double maxY;
  public double minZ;
  public double maxZ;
  public long startOfWaveformDataPacket;
  public long startOfWaveformDataPacketRec;
  public long startOfExtendedVarLenRec;
  public long numberExtendedVarLenRec;
  public long numberOfPointRecords;
  public long[] numberOfPointsByReturn;
  public CoordinateReferenceSystemOption crsOption;
  public LasGpsTimeType lasGpsTimeType;
  public LinearUnits lasLinearUnits = LinearUnits.UNKNOWN;
  public boolean isGeographicModelTypeKnown;
  public boolean usesGeographicModel;
  public GeoTiffData gtData;

  public LASraf braf;
  public boolean isClosed;

  public boolean containsExtraBytes = false;

  public int extraBytesInPoint2 = 0;

  public ArrayList<Integer> extra_byte_data_type = new ArrayList<>();
  public ArrayList<Integer> extraBytesInPoint = new ArrayList<>();
  public HashMap<String, Integer> extraBytes_names = new HashMap<>();

  public byte[] readExtra = new byte[1];
  public final List<LasVariableLengthRecord> vlrList;
  public final File path;

  public int pointsRead = 0;
  fileOperations fo = new fileOperations();

  public int nExtraBytes = 0;

  public boolean errorCode1 = false;

  public LASReader(File path) throws IOException {

    sanityCheckPointRecordLength.put(0, 20);
    sanityCheckPointRecordLength.put(1, 28);
    sanityCheckPointRecordLength.put(2, 26);
    sanityCheckPointRecordLength.put(3, 34);
    sanityCheckPointRecordLength.put(4, 57);
    sanityCheckPointRecordLength.put(5, 63);
    sanityCheckPointRecordLength.put(6, 30);
    sanityCheckPointRecordLength.put(7, 36);
    sanityCheckPointRecordLength.put(8, 38);
    sanityCheckPointRecordLength.put(9, 59);
    sanityCheckPointRecordLength.put(10, 67);

    this.path = path;
    braf = new LASraf(path);
    vlrList = new ArrayList<>();

    readHeader();

    braf.pointDataRecordLength = this.pointDataRecordLength;
    braf.offsetToPointData = this.offsetToPointData;

    this.braf.setUpMappedByteBuffer();

    try {
      getIndexMap();
    }catch (Exception e){
      e.printStackTrace();
    }


    if(this.pointDataRecordLength != sanityCheckPointRecordLength.get(this.pointDataRecordFormat)){

      //System.out.println("POINT DATA RECORD LENGTH IS NOT STANDARD. SOMEONE ADDED SOME BYTES TO THE POINTS!!");
      //System.out.println(this.pointDataRecordFormat + " " + this.pointDataRecordLength);
      //this.extraBytesInPoint2 = this.pointDataRecordLength - sanityCheckPointRecordLength.get(this.pointDataRecordFormat);
      //readExtra = new byte[this.extraBytesInPoint];
      //readExtra = new byte[this.extraBytesInPoint];
    }

  }

  public LASReader(File path, int noMBB) throws IOException {

    sanityCheckPointRecordLength.put(0, 20);
    sanityCheckPointRecordLength.put(1, 28);
    sanityCheckPointRecordLength.put(2, 26);
    sanityCheckPointRecordLength.put(3, 34);
    sanityCheckPointRecordLength.put(4, 57);
    sanityCheckPointRecordLength.put(5, 63);
    sanityCheckPointRecordLength.put(6, 30);
    sanityCheckPointRecordLength.put(7, 36);
    sanityCheckPointRecordLength.put(8, 38);
    sanityCheckPointRecordLength.put(9, 59);
    sanityCheckPointRecordLength.put(10, 67);

    this.path = path;
    braf = new LASraf(path);
    vlrList = new ArrayList<>();

    readHeader();

    braf.pointDataRecordLength = this.pointDataRecordLength;
    braf.offsetToPointData = this.offsetToPointData;

    this.braf.setUpMappedByteBuffer();

    try {
      getIndexMap();
    }catch (Exception e){
      e.printStackTrace();
    }


    if(this.pointDataRecordLength != sanityCheckPointRecordLength.get(this.pointDataRecordFormat)){

      //System.out.println("POINT DATA RECORD LENGTH IS NOT STANDARD. SOMEONE ADDED SOME BYTES TO THE POINTS!!");
      //System.out.println(this.pointDataRecordFormat + " " + this.pointDataRecordLength);
      //this.extraBytesInPoint2 = this.pointDataRecordLength - sanityCheckPointRecordLength.get(this.pointDataRecordFormat);
      //readExtra = new byte[this.extraBytesInPoint];
      //readExtra = new byte[this.extraBytesInPoint];
    }

  }
  public LASReader(File path, boolean in) throws IOException {
    this.path = path;
    braf = new LASraf(path);
    vlrList = new ArrayList<>();

  }

  public long fastReadFromQuery(LasPoint tempPoint){

    boolean lastNotRead = false;

    try {
      if (!doneIndexes.contains(index_p)) {

        //System.out.println("index does not contain");
        readFromBuffer(tempPoint);
        //readRecord(index_p, tempPoint);
        doneIndexes.add(index_p);
      } else {

        //System.out.println("index contains, skipping!");
        skipPointInBuffer();

        boolean terminatus = false;
        while (!terminatus) {

          if (!doneIndexes.contains(++index_p)) {

            //System.out.println("reading while not doneindex");
            readFromBuffer(tempPoint);
            //readRecord(index_p, tempPoint);

            doneIndexes.add(index_p);
            terminatus = true;

          }else
            skipPointInBuffer();

          //System.out.println("point found? " + terminatus);

          if(!terminatus)
            lastNotRead = true;

          if (index_p + 1 > indexMinMax.get(this.index_u)[1]) {
            terminatus = true;

          }

          //System.out.println("still? " + terminatus);

        }

      }
    }catch (Exception e){

      System.out.println(this.pointsRead);


      e.printStackTrace();
      System.exit(1);
    }

    if(index_p + 1 > indexMinMax.get(this.index_u)[1]){

      index_u++;

      if(this.index_u >= indexMinMax.size()){

        //System.out.println("index read terminated!");
        this.index_read_terminated = true;

        if(lastNotRead)
          return -999;
        else
          return index_p;

      }

      try {
        readRecord_noRAF(indexMinMax.get(index_u)[0], indexMinMax.get(index_u)[1] - indexMinMax.get(index_u)[0] + 1);
        //System.out.println("READING " + (indexMinMax.get(index_u)[1] - indexMinMax.get(index_u)[0] + 1) + " POINTS");
        //System.out.println("minmax222: " + indexMinMax.get(index_u)[0] + " " + (indexMinMax.get(index_u)[1] - indexMinMax.get(index_u)[0] + 1));

      }catch (Exception e){

        e.printStackTrace();
      }

      long output = this.index_p;

      this.index_p = indexMinMax.get(index_u)[0];

      if(lastNotRead)
        return -999L;
      else
        return output;

    }

    return index_p++;
  }

  public long fastReadFromQuery_backup(LasPoint tempPoint){

    try {
      if (!doneIndexes.contains(index_p)) {

        System.out.println("index does not contain");
        readFromBuffer(tempPoint);
        //readRecord(index_p, tempPoint);
        doneIndexes.add(index_p);
      } else {

        System.out.println("index contains, skipping!");
        skipPointInBuffer();

        boolean terminatus = false;
        while (!terminatus) {

          if (!doneIndexes.contains(++index_p)) {

            System.out.println("reading while not doneindex");
            readFromBuffer(tempPoint);
            //readRecord(index_p, tempPoint);

            doneIndexes.add(index_p);
            terminatus = true;

          }else
            skipPointInBuffer();

          System.out.println("point found? " + terminatus);

          if (index_p + 1 > indexMinMax.get(this.index_u)[1])
            terminatus = true;

          System.out.println("still? " + terminatus);

        }

      }
    }catch (Exception e){

      System.out.println(this.pointsRead);


      e.printStackTrace();
      System.exit(1);
    }

    if(index_p + 1 > indexMinMax.get(this.index_u)[1]){

      index_u++;

      if(this.index_u >= indexMinMax.size()){

        System.out.println("index read terminated!");
        this.index_read_terminated = true;
        return index_p;

      }

      try {
        readRecord_noRAF(indexMinMax.get(index_u)[0], indexMinMax.get(index_u)[1] - indexMinMax.get(index_u)[0] + 1);
        //System.out.println("READING " + (indexMinMax.get(index_u)[1] - indexMinMax.get(index_u)[0] + 1) + " POINTS");
        //System.out.println("minmax222: " + indexMinMax.get(index_u)[0] + " " + (indexMinMax.get(index_u)[1] - indexMinMax.get(index_u)[0] + 1));

      }catch (Exception e){
        e.printStackTrace();
      }

      long output = this.index_p;

      this.index_p = indexMinMax.get(index_u)[0];



      return output;

    }

    return index_p++;
  }
  public void prepareBuffer() throws Exception{

    doneIndexes.clear();
    int pienin, suurin;
    this.indexMinMax.clear();
    this.index_read_terminated = false;

    for (int u = 0; u < this.queriedIndexes2.size(); u++) {

      long n1 = this.queriedIndexes2.get(u)[1] - this.queriedIndexes2.get(u)[0];
      long n2 = this.queriedIndexes2.get(u)[1];

      int parts = (int) Math.ceil((double) n1 / 10000.0);
      int jako = (int) Math.ceil((double) n1 / (double) parts);

      for (int c = 1; c <= parts; c++) {

        if (c != parts) {
          pienin = (c - 1) * jako;
          suurin = c * jako;
        } else {
          pienin = (c - 1) * jako;
          suurin = (int) n1;
        }

        pienin = pienin + this.queriedIndexes2.get(u)[0];
        suurin = suurin + this.queriedIndexes2.get(u)[0];

        indexMinMax.add(new int[]{pienin, suurin});
      }
    }


    /* IF NO MIN MAX FOUND */

    if(indexMinMax.size() == 0){
      return;
    }

    this.index_u = 0;
    try {
      readRecord_noRAF(indexMinMax.get(index_u)[0], indexMinMax.get(index_u)[1] - indexMinMax.get(index_u)[0] + 1);
      //System.out.println("READING " + (indexMinMax.get(index_u)[1] - indexMinMax.get(index_u)[0] + 1) + " POINTS");
      //System.out.println("minmax111: " + indexMinMax.get(index_u)[0] + " " + (indexMinMax.get(index_u)[1] - indexMinMax.get(index_u)[0] + 1));
    }catch (Exception e){
      System.out.println("HERE!");
      this.errorCode1 = true;
      e.printStackTrace();

    }

    //System.out.println(index_u);

    this.index_p = indexMinMax.get(index_u)[0];

  }


  public boolean indexContainsStuff(){
    return this.queriedIndexes2.size() > 0;
  }

  /**
   *
   * @param indexMap hjhjhk
   */
  public void setIndexMap2(HashMap<Integer, ArrayList<int[]>> indexMap){

    this.indexMap2 = indexMap;

  }

  public void setIndexMap(HashMap<Integer, ArrayList<Long>> indexMap){

    this.indexMap = indexMap;

  }

  /**
   * Tries to find a .lasx file correponding to the filename of this LASreader.
   * @throws Exception
   */
  public void getIndexMap() throws Exception{

    File temppi = fo.createNewFileWithNewExtension(this.getFile(), ".lasx");

    if(temppi.exists()) {
      FileInputStream fileIn = new FileInputStream(temppi);
      ObjectInputStream in2 = new ObjectInputStream(fileIn);
      HashMap<Integer, ArrayList<int[]>> temppi2;
      temppi2 = (HashMap<Integer, ArrayList<int[]>>) in2.readObject();

      this.isIndexed = true;

      this.setIndexMap2(temppi2);
    }else{
      this.isIndexed = false;
    }

  }

  public boolean isIndexed(){
    return this.indexMap2.size() > 0;
  }


  /**
   *	Indexes a .las file to enable faster spatial queries.
   *	Creates a .lasx file with the same filename as the input
   *    .las file.

   *	@param spacing 			The square size of the index grid.
   *							Optimal size depends on the point
   *							density of the data.
   */

  public void index(int spacing) throws Exception{

    int minYindex = 0;

    double minYi = Double.POSITIVE_INFINITY;

    String parent = this.getFile().getParent();

    if(parent == null) {
      parent = "";
    }else{
      parent = parent + File.separator;
    }

    String outputFileName = parent    + this.getFile().getName().split(".las")[0] + ".lasx";

    File oFile = new File(outputFileName);
    System.out.println(outputFileName);
    if(oFile.exists()){
      oFile.delete();
      oFile.createNewFile();
    }else{
      oFile.createNewFile();
    }

    int n_pixels_x = (int)Math.ceil((this.maxX - this.minX) / (double)spacing);
    int n_pixels_y = (int)Math.ceil((this.maxY - this.minY) / (double)spacing);

    HashMap<Integer, ArrayList<int[]>> save2 = new HashMap<>();

    LasPoint tempPoint = new LasPoint();

    long maxi;

    int counter = 0;
    long paritus ;

    int xPixel = 0;
    int yPixel = 0;

    int indeksi = 0;

    for(long i = 0; i < this.getNumberOfPointRecords(); i += 10000){

      maxi = (long)Math.min(10000, Math.abs(this.getNumberOfPointRecords() - i));

      this.readRecord_noRAF(i, tempPoint, 10000);

      for (int j = 0; j < maxi; j++) {

        this.readFromBuffer(tempPoint);

        if(tempPoint.y < minYi){
          minYindex = counter;
          minYi = tempPoint.y;
        }

        xPixel = Math.min((int)((tempPoint.x - this.minX) / (double)spacing), n_pixels_x);   //X INDEX
        yPixel = Math.min((int)((this.maxY - tempPoint.y) / (double)spacing), n_pixels_y);

        indeksi = yPixel * n_pixels_x + xPixel;

        /* If the current index square is not initialized */
        if(save2.get(indeksi) == null){
          save2.put(indeksi, new ArrayList<>());
          save2.get(indeksi).add(new int[]{counter, counter+1});
        }
        else if((counter - save2.get(indeksi).get(save2.get(indeksi).size()-1)[1]) > 1000) {
          save2.get(indeksi).add(new int[]{counter, counter+1});
        }else{
          save2.get(indeksi).get(save2.get(indeksi).size()-1)[1] = counter;
        }
        counter++;
      }
    }

    ArrayList<Long> temppiLista = new ArrayList<>();
    temppiLista.add(0L);
    temppiLista.add(0L);
    temppiLista.add(0L);
    temppiLista.add(0L);
    temppiLista.add((long)spacing);
    temppiLista.add((long)minYindex);

    save2.put(-1, new ArrayList<>());
    save2.get(-1).add(new int[]{spacing});

    try {
      FileOutputStream fileOut = new FileOutputStream(outputFileName);
      ObjectOutputStream out = new ObjectOutputStream(fileOut);
      out.writeObject(save2);
      out.close();
      fileOut.close();
    }catch(IOException i) {
      i.printStackTrace();
    }

    this.getIndexMap();

  }

  /**
   * NOT USED IN ANYWHERE IMPORTANT!!!
   *
   * @param minX1
   * @param maxX1
   * @param minY1
   * @param maxY1
   * @return
   */
  public boolean query(double minX1, double maxX1, double minY1, double maxY1){

    int n1 = 0;
    int n2 = 0;
    int n3 = 0;
    int n4 = 0;

    if(indexMap.size() == 0)
      return false;

    this.spacing = indexMap2.get(-1L).get(0)[0];

    this.qIndex.clear();

    long[] extentti = new long[4];
    extentti[0] = (int)Math.floor( (minX1 - indexMap.get(-1L).get(0)) / (double)spacing);
    extentti[1] = (int)Math.floor( (maxX1 - indexMap.get(-1L).get(0)) / (double)spacing);

    extentti[2] = (int)Math.floor( (indexMap.get(-1L).get(3) - minY1) / (double)spacing);
    extentti[3] = (int)Math.floor( (indexMap.get(-1L).get(3) - maxY1) / (double)spacing);

    n1 = (int)Math.min(extentti[0], extentti[1]);
    n2 = (int)Math.max(extentti[0], extentti[1]);
    n3 = (int)Math.min(extentti[2], extentti[3]);
    n4 = (int)Math.max(extentti[2], extentti[3]);

    long paritus;

    this.queriedIndexes.clear();
    long[] temp99 = new long[2];

    for(long k = n1; k <= n2; k++)
      for(long f = n3; f <= n4; f++){

        temp99[0] = k;
        temp99[1] = f;

        paritus = Cantor.pair(temp99[0], temp99[1]);
        if(indexMap2.containsKey(paritus) && paritus != -1L){

          for (int u = 0; u < indexMap.get(Cantor.pair(temp99[0], temp99[1])).size() - 1; u += 2) {

            for (long p = indexMap.get(Cantor.pair(temp99[0], temp99[1])).get(u); p < indexMap.get(Cantor.pair(temp99[0], temp99[1])).get(u + 1); p++) {
              qIndex.add((int)p);
            }
          }

          if(indexMap.get(Cantor.pair(temp99[0], temp99[1])).size() == 1) {
            System.out.println("HERE!!");
            qIndex.add(indexMap.get(Cantor.pair(temp99[0], temp99[1])).get(0).intValue());
          }
        }

      }

    return true;
  }

  public boolean queryPoly2(double minX1, double maxX1, double minY1, double maxY1) throws Exception {

    int n1 = 0;
    int n2 = 0;
    int n3 = 0;
    int n4 = 0;

    int pienin = -1;
    int suurin = -1;

    if (indexMap2.size() == 0)
      return false;

    this.spacing = indexMap2.get(-1).get(0)[0];

    queried_set_of_point_indexes.clear();

    int n_pixels_x = (int) Math.ceil((this.maxX - this.minX) / (double) spacing);

    int[] extentti = new int[4];

    extentti[0] = (int) Math.floor((minX1 - this.minX) / (double) spacing);
    extentti[1] = (int) Math.floor((maxX1 - this.minX) / (double) spacing);

    extentti[2] = (int) Math.floor((this.maxY - minY1) / (double) spacing);
    extentti[3] = (int) Math.floor((this.maxY - maxY1) / (double) spacing);

    n1 = Math.min(extentti[0], extentti[1]);
    n2 = Math.max(extentti[0], extentti[1]);
    n3 = Math.min(extentti[2], extentti[3]);
    n4 = Math.max(extentti[2], extentti[3]);

    this.queriedIndexes2.clear();

    int indeksi = 0;

    for (int k = n1; k <= n2; k++)
      for (int f = n3; f <= n4; f++) {

        if(k < 0 || f < 0)
          continue;

        indeksi = f * n_pixels_x + k;

        if (indexMap2.containsKey(indeksi) && indeksi >= 0)
          queriedIndexes2.addAll(indexMap2.get(indeksi));

      }

    this.prepareBuffer();

    if(this.errorCode1){
      this.errorCode1 = false;
      this.queriedIndexes2.clear();
      return false;
    }
    return true;

  }

  public boolean querySquare(double minX1, double maxX1, double minY1, double maxY1){

    int n1 = 0;
    int n2 = 0;
    int n3 = 0;
    int n4 = 0;

    if(indexMap2.size() == 0)
      return false;

    this.spacing = indexMap2.get(-1).get(0)[0];

    int n_pixels_x = (int)Math.ceil((this.maxX - this.minX) / (double)spacing);

    int[] extentti = new int[4];

    extentti[0] = (int)Math.floor( (minX1 - this.minX) / (double)spacing);
    extentti[1] = (int)Math.floor( (maxX1 - this.minX) / (double)spacing);

    extentti[2] = (int)Math.floor( (this.maxY - minY1) / (double)spacing);
    extentti[3] = (int)Math.floor( (this.maxY - maxY1) / (double)spacing);

    n1 = Math.min(extentti[0], extentti[1]);
    n2 = Math.max(extentti[0], extentti[1]);
    n3 = Math.min(extentti[2], extentti[3]);
    n4 = Math.max(extentti[2], extentti[3]);

    this.queriedIndexes2.clear();

    int indeksi = 0;

    for(int k = n1; k <= n2; k++)
      for(int f = n3; f <= n4; f++){

        indeksi = f * n_pixels_x + k;

        if(indexMap2.containsKey(indeksi) && indeksi >= 0)
          queriedIndexes2.addAll(indexMap2.get(indeksi));

      }

    return true;

  }

    public boolean query2(double minX1, double maxX1, double minY1, double maxY1){

        int n1 = 0;
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;

        if(indexMap2.size() == 0) {
          return false;
        }

      int n_pixels_x = (int)Math.ceil((this.maxX - this.minX) / (double)spacing);

      //this.spacing = indexMap.get(-1L).get(4).intValue();
        this.spacing = indexMap2.get(-1).get(0)[0];

        //this.qIndex.clear();

        long[] extentti = new long[4];
        extentti[0] = (int)Math.floor( (minX1 - this.minX) / (double)spacing);
        extentti[1] = (int)Math.floor( (maxX1 - this.minX) / (double)spacing);

        extentti[2] = (int)Math.floor( (this.maxY - minY1) / (double)spacing);
        extentti[3] = (int)Math.floor( (this.maxY - maxY1) / (double)spacing);

        n1 = (int)Math.min(extentti[0], extentti[1]);
        n2 = (int)Math.max(extentti[0], extentti[1]);
        n3 = (int)Math.min(extentti[2], extentti[3]);
        n4 = (int)Math.max(extentti[2], extentti[3]);

        long paritus;

        this.queriedIndexes2.clear();

        long[] temp99 = new long[2];
        int indeksi = 0;
        for(int k = n1; k <= n2; k++)
            for(int f = n3; f <= n4; f++){

                indeksi = f * n_pixels_x + k;

                temp99[0] = k;
                temp99[1] = f;

                //paritus = Cantor.pair(temp99[0], temp99[1]);

                if(indexMap2.containsKey(indeksi) && indeksi != -1){
                    queriedIndexes2.addAll((indexMap2.get(indeksi)));
                }

            }

        return true;
    }


  public boolean query(double X, double Y){

    int n1 = 0;
    int n2 = 0;
    int n3 = 0;
    int n4 = 0;

    if(indexMap.size() == 0)
      return false;

    this.spacing = indexMap.get(-1L).get(4).intValue();


    long[] extentti = new long[4];
    int xCoord = (int)Math.floor( (X - indexMap.get(-1L).get(0)) / (double)spacing);

    int yCoord = (int)Math.floor( (indexMap.get(-1L).get(3) - Y) / (double)spacing);

    this.queriedIndexes.clear();
    long[] temp99 = new long[]{xCoord, yCoord};

    queriedIndexes.addAll(indexMap.get(Cantor.pair(temp99[0], temp99[1])));

    return true;
  }

  public boolean queryOffset(double X, double Y, int offsetX, int offsetY){

    int n1 = 0;
    int n2 = 0;
    int n3 = 0;
    int n4 = 0;

    if(indexMap.size() == 0)
      return false;

    this.spacing = indexMap.get(-1L).get(4).intValue();


    long[] extentti = new long[4];
    int xCoord = (int)Math.floor( (X - indexMap.get(-1L).get(0)) / (double)spacing) + offsetX;

    int yCoord = (int)Math.floor( (indexMap.get(-1L).get(3) - Y) / (double)spacing) + offsetY;

    this.queriedIndexes.clear();
    long[] temp99 = new long[]{xCoord, yCoord};

    queriedIndexes.addAll(indexMap.get(Cantor.pair(temp99[0], temp99[1])));

    return true;
  }

  public boolean querySquare(double X, double Y, int dim){

    int n1 = 0;
    int n2 = 0;
    int n3 = 0;
    int n4 = 0;

    double min = indexMap.get(-1L).get(0);
    double min2 = indexMap.get(-1L).get(3);

    if(indexMap.size() == 0)
      return false;

    this.spacing = indexMap.get(-1L).get(4).intValue();

    this.queriedIndexes.clear();

    long[] temp99 = new long[2];

    int xCoord;
    int yCoord;

    long paritus;

    xCoord = (int)Math.floor( (X - min) / (double)spacing) - dim;
    yCoord = (int)Math.floor( (min2 - Y) / (double)spacing) - dim;
    //System.out.println(yCoord);
    temp99[0] = xCoord;
    temp99[1] = yCoord;
    paritus = Cantor.pair(temp99[0], temp99[1]);
    //System.out.println(Cantor.pair(temp99[0], temp99[1]));
    if(indexMap.containsKey(paritus) && paritus != -1L)
      queriedIndexes.addAll(indexMap.get(Cantor.pair(temp99[0], temp99[1])));

    xCoord = (int)Math.floor( (X - min) / (double)spacing) - 0;
    yCoord = (int)Math.floor( (min2 - Y) / (double)spacing) - dim;
    temp99[0] = xCoord;
    temp99[1] = yCoord;
    paritus = Cantor.pair(temp99[0], temp99[1]);
    //System.out.println(Cantor.pair(temp99[0], temp99[1]));
    if(indexMap.containsKey(paritus) && paritus != -1L)
      queriedIndexes.addAll(indexMap.get(Cantor.pair(temp99[0], temp99[1])));

    xCoord = (int)Math.floor( (X - min) / (double)spacing) + dim;
    yCoord = (int)Math.floor( (min2 - Y) / (double)spacing) - dim;
    temp99[0] = xCoord;
    temp99[1] = yCoord;
    paritus = Cantor.pair(temp99[0], temp99[1]);
    //System.out.println(Cantor.pair(temp99[0], temp99[1]));
    if(indexMap.containsKey(paritus) && paritus != -1L)
      queriedIndexes.addAll(indexMap.get(Cantor.pair(temp99[0], temp99[1])));

    xCoord = (int)Math.floor( (X - min) / (double)spacing) - dim;
    yCoord = (int)Math.floor( (min2 - Y) / (double)spacing) + dim;
    temp99[0] = xCoord;
    temp99[1] = yCoord;
    paritus = Cantor.pair(temp99[0], temp99[1]);
    //System.out.println(Cantor.pair(temp99[0], temp99[1]));
    if(indexMap.containsKey(paritus) && paritus != -1L)
      queriedIndexes.addAll(indexMap.get(Cantor.pair(temp99[0], temp99[1])));

    xCoord = (int)Math.floor( (X - min) / (double)spacing) - 0;
    yCoord = (int)Math.floor( (min2 - Y) / (double)spacing) + dim;
    temp99[0] = xCoord;
    temp99[1] = yCoord;
    paritus = Cantor.pair(temp99[0], temp99[1]);
    //System.out.println(Cantor.pair(temp99[0], temp99[1]));
    if(indexMap.containsKey(paritus) && paritus != -1L)
      queriedIndexes.addAll(indexMap.get(Cantor.pair(temp99[0], temp99[1])));

    xCoord = (int)Math.floor( (X - min) / (double)spacing) + dim;
    yCoord = (int)Math.floor( (min2 - Y) / (double)spacing) + dim;
    temp99[0] = xCoord;
    temp99[1] = yCoord;
    paritus = Cantor.pair(temp99[0], temp99[1]);
    //System.out.println(Cantor.pair(temp99[0], temp99[1]));
    if(indexMap.containsKey(paritus) && paritus != -1L)
      queriedIndexes.addAll(indexMap.get(Cantor.pair(temp99[0], temp99[1])));

    xCoord = (int)Math.floor( (X - min) / (double)spacing) - dim;
    yCoord = (int)Math.floor( (min2 - Y) / (double)spacing) + 0;
    temp99[0] = xCoord;
    temp99[1] = yCoord;
    paritus = Cantor.pair(temp99[0], temp99[1]);
    //System.out.println(Cantor.pair(temp99[0], temp99[1]));
    if(indexMap.containsKey(paritus) && paritus != -1L)
      queriedIndexes.addAll(indexMap.get(Cantor.pair(temp99[0], temp99[1])));

    xCoord = (int)Math.floor( (X - min) / (double)spacing) + dim;
    yCoord = (int)Math.floor( (min2 - Y) / (double)spacing) + 0;
    temp99[0] = xCoord;
    temp99[1] = yCoord;
    paritus = Cantor.pair(temp99[0], temp99[1]);
    //System.out.println(Cantor.pair(temp99[0], temp99[1]));
    if(indexMap.containsKey(paritus) && paritus != -1L)
      queriedIndexes.addAll(indexMap.get(Cantor.pair(temp99[0], temp99[1])));
/*
    xCoord = (int)Math.floor( (X - min) / (double)spacing) + 0;
    yCoord = (int)Math.floor( (min2 - Y) / (double)spacing) + 0;
    temp99[0] = xCoord;
    temp99[1] = yCoord;
    paritus = Cantor.pair(temp99[0], temp99[1]);
    //System.out.println(Cantor.pair(temp99[0], temp99[1]));
    if(indexMap.containsKey(paritus) && paritus != -1L)
      queriedIndexes.addAll(indexMap.get(Cantor.pair(temp99[0], temp99[1])));
*/
    return true;
  }

  /**
   *
   * @param minX1
   * @param maxX1
   * @param minY1
   * @param maxY1
   * @param polyg
   * @return
   */
  public boolean queryPoly(double minX1, double maxX1, double minY1, double maxY1, Path2D polyg){

    int n1 = 0;
    int n2 = 0;
    int n3 = 0;
    int n4 = 0;

    if(indexMap.size() == 0)
      return false;

    this.spacing = indexMap.get(-1L).get(4).intValue();


    long[] extentti = new long[4];
    extentti[0] = (int)Math.floor( (minX1 - indexMap.get(-1L).get(0)) / (double)spacing);
    extentti[1] = (int)Math.floor( (maxX1 - indexMap.get(-1L).get(0)) / (double)spacing);

    extentti[2] = (int)Math.floor( (indexMap.get(-1L).get(3) - minY1) / (double)spacing);
    extentti[3] = (int)Math.floor( (indexMap.get(-1L).get(3) - maxY1) / (double)spacing);

    n1 = (int)Math.min(extentti[0], extentti[1]);
    n2 = (int)Math.max(extentti[0], extentti[1]);
    n3 = (int)Math.min(extentti[2], extentti[3]);
    n4 = (int)Math.max(extentti[2], extentti[3]);

    this.queriedIndexes.clear();
    long[] temp99 = new long[2];

    for(long k = n1; k <= n2; k++)
      for(long f = n3; f <= n4; f++){

        temp99[0] = k;
        temp99[1] = f;

        double[] tempCorner1 = new double[2];
        tempCorner1[0] = k * (double)spacing + indexMap.get(-1L).get(0);
        tempCorner1[1] = indexMap.get(-1L).get(3) - f * (double)spacing;

        double[] tempCorner2 = new double[2];
        tempCorner2[0] = k * (double)spacing  + indexMap.get(-1L).get(0) + (double)spacing;
        tempCorner2[1] = indexMap.get(-1L).get(3) - f * (double)spacing;

        double[] tempCorner3 = new double[2];
        tempCorner3[0] = k * (double)spacing + indexMap.get(-1L).get(0);
        tempCorner3[1] = indexMap.get(-1L).get(3) - f * (double)spacing - (double)spacing;

        double[] tempCorner4 = new double[2];
        tempCorner4[0] = k * (double)spacing + indexMap.get(-1L).get(0) + (double)spacing;
        tempCorner4[1] = indexMap.get(-1L).get(3) - f * (double)spacing - (double)spacing;

        Path2D polygTemp = new Path2D.Double();

        polygTemp.moveTo(tempCorner1[0], tempCorner1[1]);
        polygTemp.lineTo(tempCorner2[0], tempCorner2[1]);
        polygTemp.lineTo(tempCorner4[0], tempCorner4[1]);
        polygTemp.lineTo(tempCorner3[0], tempCorner3[1]);
        polygTemp.closePath();

        if(testIntersection(polygTemp, polyg)){
          queriedIndexes.addAll(indexMap.get(Cantor.pair(temp99[0], temp99[1])));
        }
      }


    return true;

  }

  /**
   *
   * @param shapeA
   * @param shapeB
   * @return
   */
  public static boolean testIntersection(Shape shapeA, Shape shapeB) {
    Area areaA = new Area(shapeA);
    areaA.intersect(new Area(shapeB));
    return !areaA.isEmpty();
  }

  /**
   * Get the source file for the reader.
   *
   * @return a valid file instance.
   */
  public File getFile() {
    return this.path;
  }

  /**
   * Reads the header information.
   *
   * @throws IOException in the event of an non recoverable I/O error
   * or LAS format violation
   */
  private void readHeader() throws IOException {

    fileSignature = braf.readAscii(4);
    if (!"LASF".equals(fileSignature)) {
      throw new IOException("File is not in recognizable LAS format");
    }
    fileSourceID = braf.readUnsignedShort();


    globalEncoding = braf.readUnsignedShort();

    // skip the GUID for now
    braf.skipBytes(16);
    versionMajor = braf.readUnsignedByte();
    versionMinor = braf.readUnsignedByte();

    systemIdentifier = braf.readAscii(32);
    generatingSoftware = braf.readAscii(32);
    fileCreationDayOfYear = braf.readUnsignedShort();

    fileCreationYear = braf.readUnsignedShort();
    GregorianCalendar cal = new GregorianCalendar();
    cal.set(Calendar.YEAR, fileCreationYear);
    cal.set(Calendar.DAY_OF_YEAR, fileCreationDayOfYear);
    cal.set(Calendar.HOUR, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    fileCreationDate = cal.getTime();
    headerSize = braf.readUnsignedShort();
    offsetToPointData = braf.readUnsignedInt();

    numberVariableLengthRecords = braf.readUnsignedInt();
    pointDataRecordFormat = braf.readUnsignedByte();
    pointDataRecordLength = braf.readUnsignedShort();

    legacyNumberOfPointRecords = braf.readUnsignedInt();

    legacyNumberOfPointsByReturn = new long[5];

    for (int i = 0; i < 5; i++) {
      legacyNumberOfPointsByReturn[i]
              = braf.readUnsignedInt();
      // System.out.println(legacyNumberOfPointsByReturn[i]);
    }

    xScaleFactor = braf.readDouble();
    yScaleFactor = braf.readDouble();
    zScaleFactor = braf.readDouble();
    xOffset = braf.readDouble();
    yOffset = braf.readDouble();
    zOffset = braf.readDouble();
    maxX = braf.readDouble();
    minX = braf.readDouble();
    maxY = braf.readDouble();
    minY = braf.readDouble();
    maxZ = braf.readDouble();
    minZ = braf.readDouble();

    // the following fields were not provided
    // in LAS format 1.2 and earlier (1.3 and earlier?).
    // Use the file size  to avoid reading them if they are not there.
    long pos = braf.getFilePosition();

    if (versionMinor <= 2) {
      numberOfPointRecords = this.legacyNumberOfPointRecords;
      numberOfPointsByReturn = this.legacyNumberOfPointsByReturn;
      /*
      System.arraycopy(
        numberOfPointsByReturn, 0,
        legacyNumberOfPointsByReturn, 0,
        5);
        */
    } else if(versionMinor == 3) {

      numberOfPointRecords = this.legacyNumberOfPointRecords;
      numberOfPointsByReturn = this.legacyNumberOfPointsByReturn;
      startOfWaveformDataPacketRec = braf.readLong();

    }else if(versionMinor == 4){
      startOfWaveformDataPacketRec = braf.readLong();

      startOfExtendedVarLenRec = braf.readLong();
      numberExtendedVarLenRec = braf.readUnsignedInt();
      numberOfPointRecords = braf.readLong();
      numberOfPointsByReturn = new long[15];
      for (int i = 0; i < 15; i++) {
        numberOfPointsByReturn[i] = braf.readLong();
      }
    }
    //System.out.println(Arrays.toString(legacyNumberOfPointsByReturn));
    if ((globalEncoding & BIT4) == 0) {

      //System.out.println("HERE!!");
      crsOption = CoordinateReferenceSystemOption.GeoTIFF;
    } else {

      crsOption = CoordinateReferenceSystemOption.WKT;
    }

    if ((globalEncoding & BIT1) == 0) {

      lasGpsTimeType = LasGpsTimeType.WeekTime;
    } else {

      lasGpsTimeType = LasGpsTimeType.SatelliteTime;
    }


    /* Right now, we don't really need this information */
    for (int i = 0; i < this.numberVariableLengthRecords; i++) {
      //System.out.println("got here");
      LasVariableLengthRecord vlrHeader = readVlrHeader();


      /* EXTRA BYTES */
      if(vlrHeader.getUserId().compareTo("LASF_Spec") == 0 && vlrHeader.getRecordId() == 4){

        int numberOfExtraRecords = vlrHeader.getRecordLength() / 192;

        this.containsExtraBytes = true;

        for(int i_ = 0; i_ < numberOfExtraRecords; i_++){

          braf.readByte();
          braf.readByte();

          //this.extra_byte_data_type = braf.readUnsignedByte();
          this.extra_byte_data_type.add(braf.readUnsignedByte());

          switch (this.extra_byte_data_type.get(this.extra_byte_data_type.size()-1)){

            case 0:
              this.extraBytesInPoint.add(-1);
              break;

            case 1:
              this.extraBytesInPoint.add(1);
              break;

            case 2:
              this.extraBytesInPoint.add(1);
              break;

            case 3:
              this.extraBytesInPoint.add(2);
              break;

            case 4:
              this.extraBytesInPoint.add(2);
              break;

            case 5:
              this.extraBytesInPoint.add(4);
              break;

            case 6:
              this.extraBytesInPoint.add(4);
              break;

            case 7:
              this.extraBytesInPoint.add(8);
              break;

            case 8:
              this.extraBytesInPoint.add(8);
              break;

            case 9:
              this.extraBytesInPoint.add(4);
              break;

            case 10:
              this.extraBytesInPoint.add(8);
              break;

            case 11:
              this.extraBytesInPoint.add(2);
              break;

            case 12:
              this.extraBytesInPoint.add(2);
              break;

            case 13:
              this.extraBytesInPoint.add(4);
              break;

            case 14:
              this.extraBytesInPoint.add(4);
              break;

            case 15:
              this.extraBytesInPoint.add(8);
              break;

            case 16:
              this.extraBytesInPoint.add(8);
              break;

            case 17:
              this.extraBytesInPoint.add(16);
              break;

            case 18:
              this.extraBytesInPoint.add(8);
              break;

            case 19:
              this.extraBytesInPoint.add(16);
              break;

            case 20:
              this.extraBytesInPoint.add(8);
              break;

            case 21:
              this.extraBytesInPoint.add(3);
              break;

            case 22:
              this.extraBytesInPoint.add(3);
              break;
            case 23:
              this.extraBytesInPoint.add(6);
              break;
            case 24:
              this.extraBytesInPoint.add(6);
              break;
            case 25:
              this.extraBytesInPoint.add(12);
              break;
            case 26:
              this.extraBytesInPoint.add(12);
              break;
            case 27:
              this.extraBytesInPoint.add(24);
              break;
            case 28:
              this.extraBytesInPoint.add(24);
              break;
            case 29:
              this.extraBytesInPoint.add(12);
              break;
            case 30:
              this.extraBytesInPoint.add(24);
              break;

          }

          byte options = (byte)braf.readUnsignedByte();

          String name = braf.readAscii(32);
          this.extraBytes_names.put(name, i_);

          braf.readInt();

          braf.readDouble();
          braf.readDouble();
          braf.readDouble();

          braf.readDouble();
          braf.readDouble();
          braf.readDouble();

          braf.readDouble();
          braf.readDouble();
          braf.readDouble();

          braf.readDouble();
          braf.readDouble();
          braf.readDouble();

          braf.readDouble();
          braf.readDouble();
          braf.readDouble();

          String description = braf.readAscii(32);

        }



      }else{

        /* We don't care because we know nothing about the contents... presumably */
        braf.skipBytes(vlrHeader.recordLength);

      }

      vlrList.add(vlrHeader);

    }

    if (crsOption == CoordinateReferenceSystemOption.GeoTIFF) {

      loadGeoTiffSpecification();  //NOPMD

    }

    if(this.extraBytesInPoint.size() > 0){
      for(int i = 0; i < this.extraBytesInPoint.size(); i++){
        this.nExtraBytes += this.extraBytesInPoint.get(i);
      }
    }

  }

  public double[] readHeaderOnlyExtent() throws IOException {

    fileSignature = braf.readAscii(4);
    if (!"LASF".equals(fileSignature)) {
      throw new IOException("File is not in recognizable LAS format");
    }

    fileSourceID = braf.readUnsignedShort();
    globalEncoding = braf.readUnsignedShort();
    // skip the GUID for now
    braf.skipBytes(16);
    versionMajor = braf.readUnsignedByte();
    versionMinor = braf.readUnsignedByte();
    systemIdentifier = braf.readAscii(32);
    generatingSoftware = braf.readAscii(32);
    fileCreationDayOfYear = braf.readUnsignedShort();
    fileCreationYear = braf.readUnsignedShort();
    GregorianCalendar cal = new GregorianCalendar();
    cal.set(Calendar.YEAR, fileCreationYear);
    cal.set(Calendar.DAY_OF_YEAR, fileCreationDayOfYear);
    cal.set(Calendar.HOUR, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    fileCreationDate = cal.getTime();
    headerSize = braf.readUnsignedShort();
    offsetToPointData = braf.readUnsignedInt();
    numberVariableLengthRecords = braf.readUnsignedInt();
    pointDataRecordFormat = braf.readUnsignedByte();
    pointDataRecordLength = braf.readUnsignedShort();

    legacyNumberOfPointRecords = braf.readUnsignedInt();

    legacyNumberOfPointsByReturn = new long[5];

    for (int i = 0; i < 5; i++) {
      legacyNumberOfPointsByReturn[i]
              = braf.readUnsignedInt();
    }

    xScaleFactor = braf.readDouble();
    yScaleFactor = braf.readDouble();
    zScaleFactor = braf.readDouble();
    xOffset = braf.readDouble();
    yOffset = braf.readDouble();
    zOffset = braf.readDouble();
    maxX = braf.readDouble();
    minX = braf.readDouble();
    maxY = braf.readDouble();
    minY = braf.readDouble();
    maxZ = braf.readDouble();
    minZ = braf.readDouble();



    return new double[]{minX, maxX, minY, maxY, minZ, maxZ};

  }

  private LasVariableLengthRecord readVlrHeader() throws IOException {

    braf.skipBytes(2); // reserved
    String userID = braf.readAscii(16);
    int recordID = braf.readUnsignedShort();
    int recordLength = braf.readUnsignedShort();
    String description = braf.readAscii(32);
    long offset = braf.getFilePosition();
    return new LasVariableLengthRecord(
            offset, userID, recordID, recordLength, description);
  }

  public void seekVlr(long filePos) throws IOException{

    braf.seek(filePos);

  }

  public byte readByte() throws IOException{

    return braf.readByte();

  }

  /**
   * Read a record from the LAS file. The LasPoint object is used
   * as a container to hold the results of the read. Since
   * this method may be called literally millions of times, it is
   * advantageous to reuse the point object rather than creating a
   * large number of instances.
   * <p>
   * Note that depending on the record type used in the LAS file,
   * not all elements may be populated.
   *
   * @param recordIndex the index of the record (0 to numberOfPointRecords-1)
   * @param p a valid instance to receive data
   * @throws IOException In the event of an unrecoverable IOException
   */
  public void readRecord(long recordIndex, LasPoint p) throws IOException {

    if (recordIndex < 0 || recordIndex >= this.numberOfPointRecords) {
      throw new IOException(
              "Record index "
                      + recordIndex
                      + " out of bounds ["
                      + 0 + ".." + numberOfPointRecords + "]");
    }
    if (isClosed) {
      throw new IOException("File is closed");
    }

    long filePos = this.offsetToPointData
            + recordIndex * this.pointDataRecordLength;

    if(previousIndex != (recordIndex - 1) && false) {

      try {
        braf.read(this.pointDataRecordLength);
      }catch (Exception e){
        e.printStackTrace();
      }
    }


    //braf.mbb.position((int) filePos);

    braf.setMBB(filePos, pointDataRecordLength);

/*
    if(pointDataRecordFormat <= 5) {

      int lx = braf.readInt_direct();
      int ly = braf.readInt_direct();
      int lz = braf.readInt_direct();

      p.x = lx * xScaleFactor + xOffset;
      p.y = ly * yScaleFactor + yOffset;
      p.z = lz * zScaleFactor + zOffset;
      p.intensity = braf.readUnsignedShort_direct();
      int mask = braf.readUnsignedByte_direct();

      p.returnNumber = mask & 0x07;
      p.numberOfReturns = (mask >> 3) & 0x7;
      p.scanDirectionFlag = (mask >> 5) & 0x01;
      p.edgeOfFlightLine = (mask & 0x80) != 0;

      mask = braf.readUnsignedByte_direct();
      p.classification = mask & 0x1f; // bits 0:4, values 0 to 32
      p.synthetic = (mask & 0x20) != 0;
      p.keypoint = (mask & 0x40) != 0;
      p.withheld = (mask & 0x80) != 0;

      p.scanAngleRank = braf.readByte_direct();
      p.userData = braf.readByte_direct();
      p.pointSourceId = braf.readShort_direct();


      if (pointDataRecordFormat == 1 || pointDataRecordFormat == 3 || pointDataRecordFormat == 4 || pointDataRecordFormat == 5) {
        p.gpsTime = braf.readDouble_direct();
      }

      if (pointDataRecordFormat == 2 || pointDataRecordFormat == 3 || pointDataRecordFormat == 5) {

        p.R = braf.readUnsignedShort_direct();
        p.G = braf.readShort_direct();
        p.B = braf.readShort_direct();

      }

      if (pointDataRecordFormat == 4 || pointDataRecordFormat == 5) {
        p.WavePacketDescriptorIndex = braf.readUnsignedByte_direct();
        p.ByteOffsetToWaveformData = braf.readLong_direct();
        p.WaveformPacketSizeInBytes = (int) braf.readUnsignedInt_direct();
        p.ReturnPointWaveformLocation = braf.readFloat_direct();

        p.x_t = braf.readFloat_direct();
        p.y_t = braf.readFloat_direct();
        p.z_t = braf.readFloat_direct();
      }
    }
    else{

      int lx = braf.readInt_direct();
      int ly = braf.readInt_direct();
      int lz = braf.readInt_direct();

      p.x = lx * xScaleFactor + xOffset;
      p.y = ly * yScaleFactor + yOffset;
      p.z = lz * zScaleFactor + zOffset;
      p.intensity = braf.readUnsignedShort_direct();

      int mask = braf.readUnsignedByte_direct();

      p.returnNumber = mask>>4;

      p.numberOfReturns = mask&15;

      mask = braf.readUnsignedByte_direct();

      int classificationFlag = mask>>4;

      p.synthetic =   ((classificationFlag >> 0) & 1) != 0;
      p.keypoint  =   ((classificationFlag >> 1) & 1) != 0;
      p.withheld  =   ((classificationFlag >> 2) & 1) != 0;
      p.overlap   =   ((classificationFlag >> 3) & 1) != 0;

      int lastPart = mask&15;

      p.scannerCannel = lastPart&0b11;

      p.scanDirectionFlag = ((lastPart >> 2) & 1);
      p.edgeOfFlightLine = ((lastPart >> 3) & 1) != 0;

      p.classification = braf.readUnsignedByte_direct();
      p.userData = braf.readUnsignedByte_direct();
      p.scanAngleRank = braf.readUnsignedShort_direct();
      p.pointSourceId = braf.readShort_direct();
      p.gpsTime = braf.readDouble_direct();

      if(pointDataRecordFormat >= 7 && pointDataRecordFormat != 9){

        p.R = (braf.readShort_direct()) & 0xffff;
        p.G = (braf.readShort_direct()) & 0xffff;
        p.B = (braf.readShort_direct()) & 0xffff;

        if(pointDataRecordFormat >= 8){
          p.N = (braf.readShort_direct()) & 0xffff;
        }
      }
      if(pointDataRecordFormat == 10 || pointDataRecordFormat == 9){

        p.WavePacketDescriptorIndex = braf.readUnsignedByte_direct();
        p.ByteOffsetToWaveformData = braf.readLong_direct();
        p.WaveformPacketSizeInBytes = (int)braf.readUnsignedInt_direct();
        p.ReturnPointWaveformLocation = braf.readFloat_direct();

        p.x_t = braf.readFloat_direct();
        p.y_t = braf.readFloat_direct();
        p.z_t = braf.readFloat_direct();

      }
    }

 */


    if(pointDataRecordFormat <= 5) {

      int lx = braf.getMbb().getInt();
      int ly = braf.getMbb().getInt();
      int lz = braf.getMbb().getInt();

      p.x = lx * xScaleFactor + xOffset;
      p.y = ly * yScaleFactor + yOffset;
      p.z = lz * zScaleFactor + zOffset;

      p.intensity = Short.toUnsignedInt(braf.getMbb().getShort());

      int mask = braf.getMbb().get();

      p.returnNumber = mask & 0x07;
      p.numberOfReturns = (mask >> 3) & 0x7;
      p.scanDirectionFlag = (mask >> 5) & 0x01;
      p.edgeOfFlightLine = (mask & 0x80) != 0;

      mask = braf.getMbb().get();
      p.classification = mask & 0x1f; // bits 0:4, values 0 to 32
      p.synthetic = (mask & 0x20) != 0;
      p.keypoint = (mask & 0x40) != 0;
      p.withheld = (mask & 0x80) != 0;

      p.scanAngleRank = braf.getMbb().get();
      p.userData = braf.getMbb().get();
      p.pointSourceId = braf.getMbb().getShort();

      switch(pointDataRecordFormat){

        case 1:
          p.gpsTime = braf.getMbb().getDouble();
          break;

        case 2:
          p.R = (braf.getMbb().getChar()) & 0xffff;
          p.G = (braf.getMbb().getChar()) & 0xffff;
          p.B = (braf.getMbb().getChar()) & 0xffff;
          break;

        case 3:
          p.gpsTime = braf.getMbb().getDouble();
          p.R = (braf.getMbb().getChar()) & 0xffff;
          p.G = (braf.getMbb().getChar()) & 0xffff;
          p.B = (braf.getMbb().getChar()) & 0xffff;
          break;

        case 4:
          p.gpsTime = braf.getMbb().getDouble();
          p.WavePacketDescriptorIndex = braf.getMbb().get();
          p.ByteOffsetToWaveformData = braf.getMbb().getLong();
          p.WaveformPacketSizeInBytes = braf.getMbb().getInt();
          p.ReturnPointWaveformLocation = braf.getMbb().getFloat();

          p.x_t = braf.getMbb().getFloat();
          p.y_t = braf.getMbb().getFloat();
          p.z_t = braf.getMbb().getFloat();
          break;

        case 5:
          p.gpsTime = braf.getMbb().getDouble();
          p.R = (braf.getMbb().getChar()) & 0xffff;
          p.G = (braf.getMbb().getChar()) & 0xffff;
          p.B = (braf.getMbb().getChar()) & 0xffff;
          p.WavePacketDescriptorIndex = braf.getMbb().get();
          p.ByteOffsetToWaveformData = braf.getMbb().getLong();
          p.WaveformPacketSizeInBytes = braf.getMbb().getInt();
          p.ReturnPointWaveformLocation = braf.getMbb().getFloat();

          p.x_t = braf.getMbb().getFloat();
          p.y_t = braf.getMbb().getFloat();
          p.z_t = braf.getMbb().getFloat();
          break;

        default:
          break;
      }
    }
    else{

      int lx = braf.getMbb().getInt();
      int ly = braf.getMbb().getInt();
      int lz = braf.getMbb().getInt();

      p.x = lx * xScaleFactor + xOffset;
      p.y = ly * yScaleFactor + yOffset;
      p.z = lz * zScaleFactor + zOffset;
      p.intensity = Short.toUnsignedInt(braf.getMbb().getShort());

      int mask = braf.getMbb().get();

      p.numberOfReturns = mask>>4;

      p.returnNumber = mask&15;

      mask = braf.getMbb().get();

      int classificationFlag = mask>>4;

      /* NOT TESTED! */
      p.synthetic =   ((classificationFlag >> 0) & 1) != 0;
      p.keypoint  =   ((classificationFlag >> 1) & 1) != 0;
      p.withheld  =   ((classificationFlag >> 2) & 1) != 0;
      p.overlap   =   ((classificationFlag >> 3) & 1) != 0;

      int lastPart = mask&15;

      p.scannerCannel = lastPart&0b11;

      p.scanDirectionFlag = ((lastPart >> 2) & 1);
      p.edgeOfFlightLine = ((lastPart >> 3) & 1) != 0;

      p.classification = braf.getMbb().get();
      p.userData = braf.getMbb().get();
      p.scanAngleRank = braf.getMbb().getShort();
      p.pointSourceId = braf.getMbb().getShort();
      p.gpsTime = braf.getMbb().getDouble();

      if(pointDataRecordFormat >= 7 && pointDataRecordFormat != 9){

        p.R = (braf.getMbb().getChar()) & 0xffff;
        p.G = (braf.getMbb().getChar()) & 0xffff;
        p.B = (braf.getMbb().getChar()) & 0xffff;

        if(pointDataRecordFormat >= 8){
          p.N = (braf.getMbb().getChar()) & 0xffff;
        }
      }
      if(pointDataRecordFormat == 10 || pointDataRecordFormat == 9){

        p.WavePacketDescriptorIndex = braf.getMbb().get();
        p.ByteOffsetToWaveformData = braf.getMbb().getLong();
        p.WaveformPacketSizeInBytes = braf.getMbb().getInt();
        p.ReturnPointWaveformLocation = braf.getMbb().getFloat();

        p.x_t = braf.getMbb().getFloat();
        p.y_t = braf.getMbb().getFloat();
        p.z_t = braf.getMbb().getFloat();


      }
    }

    //System.out.println("EXTRA: " + this.extraBytesInPoint + " " + p.x + " " + p.y);

    if (this.extraBytesInPoint.size() > 0) {

      //System.out.println("whAT " + this.extraBytesInPoint.size());
      /* IF WE DON'T CARE ABOUT THE CONTENTS */
      for(int i = 0; i < extraBytesInPoint.size(); i++){

        //System.out.println(braf.buffer.remaining());
        try {
          braf.getMbb().get(p.extra_bytes.get(i));
        }catch (Exception e){

          p.addExraByte(extraBytesInPoint.get(i));

          try{
            braf.getMbb().get(p.extra_bytes.get(i));
          }catch (Exception e2){
            e2.printStackTrace();
            System.exit(1);
          }

        }
      }

    }
  }

  public void readVLR(){


  }

  public synchronized double[] readFromBuffer_concurrent(){

    double[] output = new double[5];

    if(pointDataRecordFormat <= 5) {

      int lx = braf.buffer.getInt();
      int ly = braf.buffer.getInt();
      int lz = braf.buffer.getInt();


      output[0] = lx * xScaleFactor + xOffset;
      output[1] = ly * yScaleFactor + yOffset;
      output[2] = lz * zScaleFactor + zOffset;
      Short.toUnsignedInt(braf.buffer.getShort());

      int mask = braf.buffer.get();

      output[3] = mask & 0x07;
      output[4] = (mask >> 3) & 0x7;

      // for record types 0 to 5, the classification
      // is packed in with some other bit-values, see Table 8
      braf.buffer.get();

      braf.buffer.get();
      braf.buffer.get();
      braf.buffer.getShort();
      // we currently skip
      //   scan angle rank  1 byte
      //   user data        1 byte
      //   point source ID  2 bytes
      //braf.skipBytes(4); // scan angle rank

      if (pointDataRecordFormat == 1 || pointDataRecordFormat == 3 || pointDataRecordFormat == 4 || pointDataRecordFormat == 5) {
        braf.buffer.getDouble();
        // Depending on the gpsTimeType element, the GPS time can be
        // in one of two formats:
        //    GPS Week Time  seconds since 12:00 a.m. Sunday
        //    GPS Satellite Time   seconds since 12 a.m. Jan 6, 1980
        //                         minus an offset 1.0e+9
        //    The mapping to a Java time requires information about
        //    the GPS time type

      }

      if (pointDataRecordFormat == 2 || pointDataRecordFormat == 3 || pointDataRecordFormat == 5) {

        braf.buffer.getChar();
        braf.buffer.getChar();
        braf.buffer.getChar();

      }

      if (pointDataRecordFormat == 4 || pointDataRecordFormat == 5) {
        braf.buffer.get();
        braf.buffer.getLong();
        braf.buffer.getInt();
        braf.buffer.getFloat();

        braf.buffer.getFloat();
        braf.buffer.getFloat();
        braf.buffer.getFloat();

      }

      //If someone has decided they want to have their OWN point data format... this is just dumb.
      //if (this.extraBytesInPoint > 0) {
      //  braf.buffer.get(readExtra);
      //}
    }else{

      braf.buffer.getInt();
      braf.buffer.getInt();
      braf.buffer.getInt();

      Short.toUnsignedInt(braf.buffer.getShort());

      int mask = braf.buffer.get();


      mask = braf.buffer.get();

      int classificationFlag = mask>>4;

      int lastPart = mask&15;

      braf.buffer.get();
      braf.buffer.get();
      braf.buffer.getShort();
      braf.buffer.getShort();
      braf.buffer.getDouble();

      if(pointDataRecordFormat >= 7 && pointDataRecordFormat != 9){

        braf.buffer.getChar();
        braf.buffer.getChar();
        braf.buffer.getChar();

        if(pointDataRecordFormat >= 8){
          braf.buffer.getChar();
        }
      }
      if(pointDataRecordFormat == 10 || pointDataRecordFormat == 9){

        braf.buffer.get();
        braf.buffer.getLong();
        braf.buffer.getInt();
        braf.buffer.getFloat();

        braf.buffer.getFloat();
        braf.buffer.getFloat();
        braf.buffer.getFloat();


      }
    }

    return output;
  }

  public synchronized void readFromBuffer_fast(LasPoint p){

      int lx = braf.buffer.getInt();
      int ly = braf.buffer.getInt();
      int lz = braf.buffer.getInt();

      p.x = lx * xScaleFactor + xOffset;
      p.y = ly * yScaleFactor + yOffset;
      p.z = lz * zScaleFactor + zOffset;
      p.intensity = Short.toUnsignedInt(braf.buffer.getShort());

      braf.buffer.position(braf.buffer.position() + this.pointDataRecordLength - 14);


  }

  public void resetReadPoints(){
    pointsRead = 0;
  }

  public void readFromBuffer(LasPoint p){

    //here
    pointsRead++;

    if(pointDataRecordFormat <= 5) {

      int lx = braf.buffer.getInt();
      int ly = braf.buffer.getInt();
      int lz = braf.buffer.getInt();

      p.x = lx * xScaleFactor + xOffset;
      p.y = ly * yScaleFactor + yOffset;
      p.z = lz * zScaleFactor + zOffset;

      //System.out.println(lz + " " + zScaleFactor + " " + zOffset + " " + p.z);
      p.intensity = Short.toUnsignedInt(braf.buffer.getShort());

      int mask = braf.buffer.get();

      p.returnNumber = mask & 0x07;
      p.numberOfReturns = (mask >> 3) & 0x7;
      p.scanDirectionFlag = (mask >> 5) & 0x01;
      p.edgeOfFlightLine = (mask & 0x80) != 0;

      mask = braf.buffer.get();
      p.classification = mask & 0x1f; // bits 0:4, values 0 to 32
      p.synthetic = (mask & 0x20) != 0;
      p.keypoint = (mask & 0x40) != 0;
      p.withheld = (mask & 0x80) != 0;

      p.scanAngleRank = braf.buffer.get();
      p.userData = braf.buffer.get();
      p.pointSourceId = braf.buffer.getShort();

      switch(pointDataRecordFormat){

        case 1:
          p.gpsTime = braf.buffer.getDouble();
          break;

        case 2:
          p.R = (braf.buffer.getChar()) & 0xffff;
          p.G = (braf.buffer.getChar()) & 0xffff;
          p.B = (braf.buffer.getChar()) & 0xffff;
          break;

        case 3:
          p.gpsTime = braf.buffer.getDouble();
          p.R = (braf.buffer.getChar()) & 0xffff;
          p.G = (braf.buffer.getChar()) & 0xffff;
          p.B = (braf.buffer.getChar()) & 0xffff;
          break;

        case 4:
          p.gpsTime = braf.buffer.getDouble();
          p.WavePacketDescriptorIndex = braf.buffer.get();
          p.ByteOffsetToWaveformData = braf.buffer.getLong();
          p.WaveformPacketSizeInBytes = braf.buffer.getInt();
          p.ReturnPointWaveformLocation = braf.buffer.getFloat();

          p.x_t = braf.buffer.getFloat();
          p.y_t = braf.buffer.getFloat();
          p.z_t = braf.buffer.getFloat();
          break;

        case 5:
          p.gpsTime = braf.buffer.getDouble();
          p.R = (braf.buffer.getChar()) & 0xffff;
          p.G = (braf.buffer.getChar()) & 0xffff;
          p.B = (braf.buffer.getChar()) & 0xffff;
          p.WavePacketDescriptorIndex = braf.buffer.get();
          p.ByteOffsetToWaveformData = braf.buffer.getLong();
          p.WaveformPacketSizeInBytes = braf.buffer.getInt();
          p.ReturnPointWaveformLocation = braf.buffer.getFloat();

          p.x_t = braf.buffer.getFloat();
          p.y_t = braf.buffer.getFloat();
          p.z_t = braf.buffer.getFloat();
          break;

        default:
          break;
      }
    }
    else{

      int lx = braf.buffer.getInt();
      int ly = braf.buffer.getInt();
      int lz = braf.buffer.getInt();

      p.x = lx * xScaleFactor + xOffset;
      p.y = ly * yScaleFactor + yOffset;
      p.z = lz * zScaleFactor + zOffset;
      p.intensity = Short.toUnsignedInt(braf.buffer.getShort());

      int mask = braf.buffer.get();

      p.numberOfReturns = mask>>4;

      p.returnNumber = mask&15;

      mask = braf.buffer.get();

      int classificationFlag = mask>>4;

      /* NOT TESTED! */
      p.synthetic =   ((classificationFlag >> 0) & 1) != 0;
      p.keypoint  =   ((classificationFlag >> 1) & 1) != 0;
      p.withheld  =   ((classificationFlag >> 2) & 1) != 0;
      p.overlap   =   ((classificationFlag >> 3) & 1) != 0;

      int lastPart = mask&15;

      p.scannerCannel = lastPart&0b11;

      p.scanDirectionFlag = ((lastPart >> 2) & 1);
      p.edgeOfFlightLine = ((lastPart >> 3) & 1) != 0;

      p.classification = braf.buffer.get();
      p.userData = braf.buffer.get();
      p.scanAngleRank = braf.buffer.getShort();
      p.pointSourceId = braf.buffer.getShort();
      p.gpsTime = braf.buffer.getDouble();

      if(pointDataRecordFormat >= 7 && pointDataRecordFormat != 9){

        p.R = (braf.buffer.getChar()) & 0xffff;
        p.G = (braf.buffer.getChar()) & 0xffff;
        p.B = (braf.buffer.getChar()) & 0xffff;

        if(pointDataRecordFormat >= 8){
          p.N = (braf.buffer.getChar()) & 0xffff;
        }
      }
      if(pointDataRecordFormat == 10 || pointDataRecordFormat == 9){

        p.WavePacketDescriptorIndex = braf.buffer.get();
        p.ByteOffsetToWaveformData = braf.buffer.getLong();
        p.WaveformPacketSizeInBytes = braf.buffer.getInt();
        p.ReturnPointWaveformLocation = braf.buffer.getFloat();

        p.x_t = braf.buffer.getFloat();
        p.y_t = braf.buffer.getFloat();
        p.z_t = braf.buffer.getFloat();


      }



    }

    //System.out.println("EXTRA: " + this.extraBytesInPoint + " " + p.x + " " + p.y);

    if (this.extraBytesInPoint.size() > 0) {

      //System.out.println("whAT " + this.extraBytesInPoint.size());
      /* IF WE DON'T CARE ABOUT THE CONTENTS */
      for(int i = 0; i < extraBytesInPoint.size(); i++){

        //System.out.println(braf.buffer.remaining());
        try {
          braf.buffer.get(p.extra_bytes.get(i));
        }catch (Exception e){

          p.addExraByte(extraBytesInPoint.get(i));

          try{
            braf.buffer.get(p.extra_bytes.get(i));
          }catch (Exception e2){
            e2.printStackTrace();
            System.exit(1);
          }

        }
      }
      //System.exit(1);
      //braf.buffer.get(readExtra);
      //System.out.println("here");

      //braf.buffer.get()

    }

  }

  public synchronized void readFromBuffer_only_x_y_z(LasPoint p){


    if(pointDataRecordFormat <= 5) {

      int lx = braf.buffer.getInt();
      int ly = braf.buffer.getInt();
      int lz = braf.buffer.getInt();

      p.x = lx * xScaleFactor + xOffset;
      p.y = ly * yScaleFactor + yOffset;
      p.z = lz * zScaleFactor + zOffset;

      if(true)
        return;

      p.intensity = Short.toUnsignedInt(braf.buffer.getShort());

      int mask = braf.buffer.get();

      p.returnNumber = mask & 0x07;
      p.numberOfReturns = (mask >> 3) & 0x7;
      p.scanDirectionFlag = (mask >> 5) & 0x01;
      p.edgeOfFlightLine = (mask & 0x80) != 0;

      mask = braf.buffer.get();
      p.classification = mask & 0x1f; // bits 0:4, values 0 to 32
      p.synthetic = (mask & 0x20) != 0;
      p.keypoint = (mask & 0x40) != 0;
      p.withheld = (mask & 0x80) != 0;

      p.scanAngleRank = braf.buffer.get();
      p.userData = braf.buffer.get();
      p.pointSourceId = braf.buffer.getShort();

      switch(pointDataRecordFormat){

        case 1:
          p.gpsTime = braf.buffer.getDouble();
          break;

        case 2:
          p.R = (braf.buffer.getChar()) & 0xffff;
          p.G = (braf.buffer.getChar()) & 0xffff;
          p.B = (braf.buffer.getChar()) & 0xffff;
          break;

        case 3:
          p.gpsTime = braf.buffer.getDouble();
          p.R = (braf.buffer.getChar()) & 0xffff;
          p.G = (braf.buffer.getChar()) & 0xffff;
          p.B = (braf.buffer.getChar()) & 0xffff;
          break;

        case 4:
          p.gpsTime = braf.buffer.getDouble();
          p.WavePacketDescriptorIndex = braf.buffer.get();
          p.ByteOffsetToWaveformData = braf.buffer.getLong();
          p.WaveformPacketSizeInBytes = braf.buffer.getInt();
          p.ReturnPointWaveformLocation = braf.buffer.getFloat();

          p.x_t = braf.buffer.getFloat();
          p.y_t = braf.buffer.getFloat();
          p.z_t = braf.buffer.getFloat();
          break;

        case 5:
          p.gpsTime = braf.buffer.getDouble();
          p.R = (braf.buffer.getChar()) & 0xffff;
          p.G = (braf.buffer.getChar()) & 0xffff;
          p.B = (braf.buffer.getChar()) & 0xffff;
          p.WavePacketDescriptorIndex = braf.buffer.get();
          p.ByteOffsetToWaveformData = braf.buffer.getLong();
          p.WaveformPacketSizeInBytes = braf.buffer.getInt();
          p.ReturnPointWaveformLocation = braf.buffer.getFloat();

          p.x_t = braf.buffer.getFloat();
          p.y_t = braf.buffer.getFloat();
          p.z_t = braf.buffer.getFloat();
          break;

        default:
          break;
      }
    }
    else{

      int lx = braf.buffer.getInt();
      int ly = braf.buffer.getInt();
      int lz = braf.buffer.getInt();

      p.x = lx * xScaleFactor + xOffset;
      p.y = ly * yScaleFactor + yOffset;
      p.z = lz * zScaleFactor + zOffset;

      if(true)
        return;

      p.intensity = Short.toUnsignedInt(braf.buffer.getShort());

      int mask = braf.buffer.get();

      p.numberOfReturns = mask>>4;

      p.returnNumber = mask&15;

      mask = braf.buffer.get();

      int classificationFlag = mask>>4;

      /* NOT TESTED! */
      p.synthetic =   ((classificationFlag >> 0) & 1) != 0;
      p.keypoint  =   ((classificationFlag >> 1) & 1) != 0;
      p.withheld  =   ((classificationFlag >> 2) & 1) != 0;
      p.overlap   =   ((classificationFlag >> 3) & 1) != 0;

      int lastPart = mask&15;

      p.scannerCannel = lastPart&0b11;

      p.scanDirectionFlag = ((lastPart >> 2) & 1);
      p.edgeOfFlightLine = ((lastPart >> 3) & 1) != 0;

      p.classification = braf.buffer.get();
      p.userData = braf.buffer.get();
      p.scanAngleRank = braf.buffer.getShort();
      p.pointSourceId = braf.buffer.getShort();
      p.gpsTime = braf.buffer.getDouble();

      if(pointDataRecordFormat >= 7 && pointDataRecordFormat != 9){

        p.R = (braf.buffer.getChar()) & 0xffff;
        p.G = (braf.buffer.getChar()) & 0xffff;
        p.B = (braf.buffer.getChar()) & 0xffff;

        if(pointDataRecordFormat >= 8){
          p.N = (braf.buffer.getChar()) & 0xffff;
        }
      }
      if(pointDataRecordFormat == 10 || pointDataRecordFormat == 9){

        p.WavePacketDescriptorIndex = braf.buffer.get();
        p.ByteOffsetToWaveformData = braf.buffer.getLong();
        p.WaveformPacketSizeInBytes = braf.buffer.getInt();
        p.ReturnPointWaveformLocation = braf.buffer.getFloat();

        p.x_t = braf.buffer.getFloat();
        p.y_t = braf.buffer.getFloat();
        p.z_t = braf.buffer.getFloat();


      }



    }

    //System.out.println("EXTRA: " + this.extraBytesInPoint + " " + p.x + " " + p.y);

    if (this.extraBytesInPoint.size() > 0) {

      //System.out.println("whAT " + this.extraBytesInPoint.size());
      /* IF WE DON'T CARE ABOUT THE CONTENTS */
      for(int i = 0; i < extraBytesInPoint.size(); i++){

        //System.out.println(braf.buffer.remaining());
        try {
          braf.buffer.get(p.extra_bytes.get(i));
        }catch (Exception e){

          p.addExraByte(extraBytesInPoint.get(i));

          try{
            braf.buffer.get(p.extra_bytes.get(i));
          }catch (Exception e2){
            e2.printStackTrace();
            System.exit(1);
          }

        }
      }
      //System.exit(1);
      //braf.buffer.get(readExtra);
      //System.out.println("here");

      //braf.buffer.get()

    }

  }


  public void skipPointInBuffer() throws IOException{
    braf.buffer.position( braf.buffer.position() + this.pointDataRecordLength); //   skipBytes(this.pointDataRecordLength);
  }


  /**
   * Read @param n amount of points to a buffer. It is common that .las processing
   * tools require the traversal of the entire .las file. This speeds up the reading
   * substantially (about 50%) compared to using readRecord() sequentially.
   * @param recordIndex
   * @param p
   * @param n
   * @throws Exception
   */
  public synchronized void readRecord_noRAF(long recordIndex, LasPoint p, int n) throws Exception {

    if (recordIndex < 0 || recordIndex >= this.numberOfPointRecords) {
      throw new lasFormatException(
              "Record index "
                      + recordIndex
                      + " out of bounds ["
                      + 0 + ".." + numberOfPointRecords + "]");
    }
    if (isClosed) {
      throw new lasFormatException("File is closed");
    }

    long filePos = this.offsetToPointData
            + recordIndex * this.pointDataRecordLength;

    braf.seek(filePos);

    braf.read(n * this.pointDataRecordLength);

  }

  /**
   * Read @param n amount of points to a buffer. It is common that .las processing
   * tools require the traversal of the entire .las file. This speeds up the reading
   * substantially (about 50%) compared to using @method readRecord() sequentially.
   * @param recordIndex
   * @param n
   * @throws Exception
   */
  public synchronized void readRecord_noRAF(long recordIndex, int n) throws Exception {

    if (recordIndex < 0 || recordIndex >= this.numberOfPointRecords) {
      throw new IOException(
              "Record index "
                      + recordIndex
                      + " out of bounds ["
                      + 0 + ".." + numberOfPointRecords + "]");
    }
    if (isClosed) {
      throw new IOException("File is closed");
    }

    long filePos = this.offsetToPointData
            + recordIndex * this.pointDataRecordLength;

    braf.seek(filePos);

    braf.read(n * this.pointDataRecordLength);

  }

  /**
   * Get the record format specified in the LAS file. The meaning
   * of this value is described in the LAS specification. Generally, the
   * record format is more useful than the actual LAS format version of
   * the file.
   *
   * @return a positive integer value.
   */
  public int getPointDataRecordFormat() {
    return this.pointDataRecordFormat;
  }

  @Override
  public String toString() {
    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
    String yearString = sdf.format(this.fileCreationDate);
    return String.format("LAS vers %d.%d, created %s, nrecs %d",
            versionMajor, versionMinor, yearString, this.legacyNumberOfPointRecords);
  }

  /**
   * Get the number of point records in file.
   *
   * @return a positive long integer.
   */
  public long getNumberOfPointRecords() {

    //System.out.println("Returning: " + this.legacyNumberOfPointRecords);
    return Math.max(this.legacyNumberOfPointRecords, this.numberOfPointRecords);

  }

  /**
   * Closes all internal data files.
   *
   * @throws IOException in the event of an non-recoverable IO condition.
   */
  public void close() throws IOException {

    braf.close();
    braf = null;

  }

  /**
   * @return the fileSignature
   */
  public String getFileSignature() {
    return fileSignature;
  }

  /**
   * Gets the minimum value for the x coordinates of the points in the LAS
   * file as specified in the LAS-standard file header.
   *
   * @return the minimum value for the x coordinates in the file
   */
  public double getMinX() {
    return minX;
  }

  /**
   * Gets the maximum value for the x coordinates of the points in the LAS
   * file as specified in the LAS-standard file header.
   *
   * @return the maximum value for the x coordinates in the file
   */
  public double getMaxX() {
    return maxX;
  }

  /**
   * Gets the minimum value for the y coordinates of the points in the LAS
   * file as specified in the LAS-standard file header.
   *
   * @return the minimum value for the y coordinates in the file
   */
  public double getMinY() {
    return minY;
  }

  /**
   * Gets the maximum value for the y coordinates of the points in the LAS
   * file as specified in the LAS-standard file header.
   *
   * @return the maximum value for the y coordinates in the file.
   */
  public double getMaxY() {
    return maxY;
  }

  /**
   * Gets the minimum value for the z coordinates of the points in the LAS
   * file as specified in the LAS-standard file header.
   *
   * @return the minimum value for the z coordinates in the file.
   */
  public double getMinZ() {
    return minZ;
  }

  /**
   * Gets the maximum value for the z coordinates of the points in the LAS
   * file as specified in the LAS-standard file header.
   *
   * @return the maximum value for the z coordinates in the file.
   */
  public double getMaxZ() {
    return maxZ;
  }

  /**
   * Gets the option that was used for storing the coordinate reference
   * system in the LAS file. This information will indicate the appropriate
   * interpretation of the associated variable-length record instance.
   *
   * @return a valid enumeration instance.
   */
  public CoordinateReferenceSystemOption getCoordinateReferenceSystemOption() {
    return this.crsOption;
  }

  /**
   * Gets a new instance of a list containing all variable length records.
   *
   * @return a valid list.
   */
  public List<LasVariableLengthRecord> getVariableLengthRecordList() {
    List<LasVariableLengthRecord> list = new ArrayList<>();
    list.addAll(vlrList);
    return list;
  }

  /**
   * Gets the variable-length record with the specified recordId
   *
   * @param recordId a valid record ID in agreement with the LAS specification
   * @return if found, a valid instance; otherwise, a null
   */
  public LasVariableLengthRecord getVariableLengthRecordByRecordId(int recordId) {
    for (LasVariableLengthRecord vlr : vlrList) {
      if (vlr.getRecordId() == recordId) {
        return vlr;
      }
    }
    return null;
  }

  private boolean inLonRange(double x) {
    return -180 <= x && x <= 360;
  }

  private boolean inLatRange(double y) {
    return -90 <= y && y <= 90;
  }

  /**
   * Provides an incomplete and weak implementation of a method that determines
   * if the LAS file contains a geographic coordinate system. At present,
   * this result is obtained by inspecting the range of the x and y coordinates.
   * However, a valid implementation would involve looking at the
   * projection-related information in the Variable Length Records as
   * defined by the LAS specification.
   *
   * @return true if the LAS file uses geographic coordinates,
   * otherwise false.
   */
  public boolean usesGeographicCoordinates() {

    // while GeoTiff specification is implemented,
    // support for WKT is not.  So the class may or may not
    // know the geographic model type
    if (isGeographicModelTypeKnown) {
      return usesGeographicModel;
    }

    // apply some rules of thumb
    if (inLonRange(minX) && inLonRange(maxX)) {
      if (maxX - minX > 10) {
        // a lidar sample would not contain a 10-degree range
        return false;
      }
      if (inLatRange(minY) && inLatRange(maxY)) {
        return maxY - minY <= 10;
      }
    }
    return false;
  }

  /**
   * Gets the representation of time that is assigned to
   * the sample point GPS time values. This option
   * is arbitrarily assigned by the agency that collected and distributed
   * the LAS file. It is necessary to use this value in order to interpret
   * the GPS time of the samples.
   *
   * @return an enumeration giving the time recording format used for the LAS
   * file
   */
  public LasGpsTimeType getLasGpsTimeType() {
    return lasGpsTimeType;
  }

  private void loadGeoTiffSpecification() throws IOException {
    // get the projection keys
    LasVariableLengthRecord vlr = getVariableLengthRecordByRecordId(34735);
    if (vlr == null) {
      return;  // this is actually a file-format error
    }

    // technically, we should check to make sure that the
    // thing that follows is the GeoTiff header.
    // but we just assume that it is correct and skip it.
    braf.seek(vlr.getFilePosition() + 6);
    int nR = braf.readUnsignedShort();
    List<GeoTiffKey> keyList = new ArrayList<>();
    for (int i = 0; i < nR; i++) {
      int keyCode = braf.readUnsignedShort();
      int location = braf.readUnsignedShort();
      int count = braf.readUnsignedShort();
      int valueOrOffset = braf.readUnsignedShort();
      GeoTiffKey key = new GeoTiffKey(keyCode, location, count, valueOrOffset); //NOPMD
      keyList.add(key);
    }

    vlr = getVariableLengthRecordByRecordId(34736);
    double[] doubleData = null;
    if (vlr != null) {
      braf.seek(vlr.getFilePosition());
      int nD = vlr.recordLength / 8;
      doubleData = new double[nD];
      for (int i = 0; i < nD; i++) {
        doubleData[i] = braf.readDouble();
      }
    }

    vlr = getVariableLengthRecordByRecordId(34737);
    char[] asciiData = null;
    if (vlr != null) {
      braf.seek(vlr.getFilePosition());
      asciiData = new char[vlr.recordLength];
      for (int i = 0; i < vlr.recordLength; i++) {
        asciiData[i] = (char) braf.readUnsignedByte();
      }
    }

    int[] temp = new int[]{GeoTiffData.GtModelTypeGeoKey, GeoTiffData.ProjectedCSTypeGeoKey};

    gtData = new GeoTiffData(keyList, doubleData, asciiData);

    // see if the data is projected or geographic
    int gtModelType = gtData.getInteger(temp);

    if (gtModelType == 1) {
      // it's projected
      this.isGeographicModelTypeKnown = true;
      this.usesGeographicModel = false;
    } else if (gtModelType == 2) {
      // its geographic
      this.isGeographicModelTypeKnown = true;
      this.usesGeographicModel = true;
    }

    int unitsCode = -1;
    if (gtData.containsKey(GeoTiffData.VerticalUnitsGeoKey)) {
      unitsCode = gtData.getInteger(GeoTiffData.VerticalUnitsGeoKey);
    } else if (gtData.containsKey(GeoTiffData.ProjLinearUnitsGeoKey)) {
      unitsCode = gtData.getInteger(GeoTiffData.ProjLinearUnitsGeoKey);
    }

    // The following values are from GeoTIFF spec paragraph 6.3.1.3
    if (unitsCode == 9001) {
      lasLinearUnits = LinearUnits.METERS;
    } else if (9002 <= unitsCode && unitsCode <= 9006) {
      lasLinearUnits = LinearUnits.FEET;
    } else if (unitsCode == 9014) {
      // fathoms are probably not used, but could be supplied
      // in bathymetric lidar applications
      lasLinearUnits = LinearUnits.FATHOMS;
    }

    //if(gtData.isKeyDefined(GeoTiffData.PCSCitationGeoKey)){
    //    String s = gtData.getString(GeoTiffData.PCSCitationGeoKey);
    //    System.out.println(s);
    //}
    //
    //  if(gtData.isKeyDefined(GeoTiffData.GeoCitationGeoKey)){
    //    String s = gtData.getString(GeoTiffData.GeoCitationGeoKey);
    //    System.out.println(s);
    //}
  }

  /**
   * Gets a copy of the GeoTiffData associated with the LAS file, if any.
   * For those LAS files which use Well-Known Text (WKT) specifications,
   * the return value from this method will be null.
   *
   * @return if available, a valid instance; otherwise a null.
   */
  public GeoTiffData getGeoTiffData() {
    return gtData;
  }

  /**
   * Get the linear units specified by the LAS file. This method
   * assumes that the vertical and horizontal data are in the same
   * system (unless Geographic coordinates are used). In the future
   * this assumption may be revised, requiring a change to the API.
   *
   * @return a valid instance of the enumeration.
   */
  public LinearUnits getLinearUnits() {
    return lasLinearUnits;
  }

  /**
   * Gets the horizontal and vertical scale and offset factors
   * that were read from the LAS file header. Normally, these factors
   * are applied to coordinates when they are read using the readRecord()
   * method and are not needed by applications. But for those applications
   * that have other requirements, this API exposes the header data elements.
   *
   * @return a valid instance.
   */
  public LasScaleAndOffset getScaleAndOffset() {
    return new LasScaleAndOffset(xScaleFactor,
            yScaleFactor,
            zScaleFactor,
            xOffset,
            yOffset,
            zOffset
    );
  }
//   A sample main for debugging and diagnostics.
//    public static void main(String[] args) throws IOException
//    {
//        LasPoint p = new LasPoint();
//        File file = new File(args[0]);
//        LasFileReader lf = new LasFileReader(file);
//
//        PrintStream ps = System.out;
//        ps.format("Number of records:   %8d\n", lf.getNumberOfPointRecords());
//        ps.format("X Min:               %10.2f\n", lf.getMinX());
//        ps.format("X Max:               %10.2f\n", lf.getMaxX());
//        ps.format("Y Min:               %10.2f\n", lf.getMinY());
//        ps.format("Y Max:               %10.2f\n", lf.getMaxY());
//        ps.format("Z Min:               %10.2f\n", lf.getMinZ());
//        ps.format("Z Max:               %10.2f\n", lf.getMaxZ());
//
//        lf.readRecord(0, p);
//        lf.readRecord(1, p);
//    }

}
