package tools;

import LASio.*;
import de.lmu.ifi.dbs.utilities.Arrays2;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import org.tinfour.common.IQuadEdge;
import org.tinfour.interpolation.NaturalNeighborInterpolator;
import org.tinfour.interpolation.VertexValuatorDefault;
import org.tinfour.utils.Polyside;
import utils.GLCM;
import utils.KdTree;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;
import utils.argumentReader;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import static org.tinfour.utils.Polyside.isPointInPolygon;


public class ITDstatistics{



    Statistics statsConst = new Statistics();

    LASReader pointCloud;
    LASReader pointCloud2;
    LASReader pointCloudOut;

    LASraf oWriter;

    ArrayList<LasPoint> dataList;
    //ArrayList<Float> dataListF;

    /** Treetop -> x, y and z */
    double[] treeTop = new double[]{0.0,0.0, Double.NEGATIVE_INFINITY};

    double[] profileDistance;
    double[] profileDistanceMax;
    double[] profileSum;
    double[] profileThold;
    double[] profile;

    public boolean groundMeasuredOK = false;

    public double[][] treeBank;

    /** Includes 	[0] = distance to chosen ITD tree
     [1] = Index of the chosen ITD tree (in output list)
     [2] = x coordinate of the chosen ITD tree
     [3] = y coordinate of the chosen ITD tree
     */

    ArrayList<float[]> labelTree = new ArrayList<float[]>();

    ArrayList<String> output = new ArrayList<String>();
    ArrayList<String> output_underStorey = new ArrayList<>();

    public File outFile = null;

    FileWriter fw = null;
    BufferedWriter bw = null;//
    PrintWriter out = null;

    public File outFile_stem = null;

    FileWriter fw_stem = null;
    BufferedWriter bw_stem = null;//
    PrintWriter out_stem = null;

    public File outFile_stem2 = null;

    FileWriter fw_stem2 = null;
    BufferedWriter bw_stem2 = null;//
    PrintWriter out_stem2 = null;

    public File outFile_stem3 = null;

    FileWriter fw_stem3 = null;
    BufferedWriter bw_stem3 = null;//
    PrintWriter out_stem3 = null;

    String sep = "\t";

    int treeTopIndex = -1;
    double treeTopMeasuredHeight = 0.0;

    Statistics stat = new Statistics();

    double[] Rn;
    double[] Gn;
    double[] Bn;

    ArrayList<double[][]> polygons = new ArrayList<>();
    ArrayList<Double> plotIds;

    int plotIdIndex = 0;

    ArrayList<String> plotIdsString = new ArrayList<String>();

    double plotId;

    int pointCount2 = 0;

    public int detectedTrees = 0;
    public int correctlyDetectedTrees = 0;
    public int groundMeasuredTrees = 0;
    public HashSet<Integer> plots = new HashSet<>();

    double detected_over_20 = 0;
    double detected_over_15 = 0;
    double detected_over_10 = 0;
    double detected_over_5 = 0;

    double overDetected_over_20 = 0;
    double overDetected_over_15 = 0;
    double overDetected_over_10 = 0;
    double overDetected_over_5 = 0;

    double groundMeasured_over_20 = 0;
    double groundMeasured_over_15 = 0;
    double groundMeasured_over_10 = 0;
    double groundMeasured_over_5 = 0;

    argumentReader aR;

    public KdTree kd_tree1 = new KdTree();
    public KdTree kd_tree2 = new KdTree();



    public ITDstatistics(){

        KdTree.XYZPoint testPoint = new KdTree.XYZPoint(1,1,1);

    }
    public ITDstatistics(argumentReader aR){

        this.aR = aR;
        KdTree.XYZPoint testPoint = new KdTree.XYZPoint(1,1,1);

    }



    public void setData(ArrayList<LasPoint> in){

        this.dataList = in;
        //normalizeRGB();
    }

    public void setOutput(File in) throws IOException {

        this.outFile = in;

        if(outFile.exists())
            outFile.delete();

        outFile.createNewFile();

        try{

            fw = new FileWriter(outFile, true);
            bw = new BufferedWriter(fw, 8192);
            out = new PrintWriter(bw);


        }catch(IOException e) {
            e.printStackTrace();
        }

    }

    public void setStemOutput2(File in) throws IOException{

        this.outFile_stem2 = in;

        if(outFile_stem2 .exists())
            outFile_stem2 .delete();

        outFile_stem2 .createNewFile();

        try{

            fw_stem2  = new FileWriter(outFile_stem2 , true);
            bw_stem2  = new BufferedWriter(fw_stem2 );
            out_stem2  = new PrintWriter(bw_stem2 );


        }catch(IOException e) {
            e.printStackTrace();
        }

    }

    public void setStemOutput3(File in) throws IOException{

        this.outFile_stem3 = in;

        if(outFile_stem3 .exists())
            outFile_stem3 .delete();

        outFile_stem3 .createNewFile();

        try{

            fw_stem3  = new FileWriter(outFile_stem3 , true);
            bw_stem3  = new BufferedWriter(fw_stem3 );
            out_stem3  = new PrintWriter(bw_stem3 );


        }catch(IOException e) {
            e.printStackTrace();
        }

    }

    public void setStemOutput(File in) throws IOException{

        this.outFile_stem = in;

        if(outFile_stem .exists())
            outFile_stem .delete();

        outFile_stem .createNewFile();

        try{

            fw_stem  = new FileWriter(outFile_stem , true);
            bw_stem  = new BufferedWriter(fw_stem );
            out_stem  = new PrintWriter(bw_stem );


        }catch(IOException e) {
            e.printStackTrace();
        }

    }

    public void changeOutputName(String oname){



    }

    public void printLine(String line){

        out.println(line);

    }

    public void closeFile(){

        out.close();
        out_stem.close();
        out_stem2.close();
        out_stem3.close();

    }

    public void resetTrees(){

        this.detectedTrees = 0;
        this.detected_over_10 = 0;
        this.detected_over_15 = 0;
        this.detected_over_20 = 0;

        this.groundMeasured_over_10 = 0;
        this.groundMeasured_over_15 = 0;
        this.groundMeasured_over_20 = 0;

        this.correctlyDetectedTrees = 0;
        this.groundMeasuredTrees = 0;
        this.plots.clear();
        this.plots = new HashSet<>();

    }

    public void setTreeTop(double[] in){

        this.treeTop = in;

    }

    public void setPolygon(ArrayList<double[][]> in, ArrayList<Double> plotIds1){

        this.polygons = in;
        this.plotIds = plotIds1;

        for(int i = 0; i < plotIds.size(); i++){

            plotIdsString.add(String.valueOf(plotIds.get(i)));

        }

    }

    /** Order: 	[0] = x coordinate
     [1] = y coordinate
     [2] = height
     [3] = diameter
     [4] = species
     [5] = used (1) or not (0)
     */
    public void setTreeBank(double[][] in){

        this.groundMeasuredOK = true;

        this.treeBank = in.clone();

        //System.out.println("tree length " + treeBank.length);

        for(int i = 0; i < treeBank.length; i++){

            labelTree.add(new float[]{Float.POSITIVE_INFINITY,0,0,0});

        }

        //

    }

    public void normalizeRGB(){

        double[] R = new double[dataList.size()];
        double[] G = new double[dataList.size()];
        double[] B = new double[dataList.size()];

        Rn = new double[dataList.size()];
        Gn = new double[dataList.size()];
        Bn = new double[dataList.size()];

        for(int i = 0; i < dataList.size(); i++){

            R[i] = dataList.get(i).R;
            G[i] = dataList.get(i).G;
            B[i] = dataList.get(i).B;

        }

        stat.setData(R);

        double Rmean = stat.getMean();
        double Rstd = stat.getStdDev();

        stat.setData(G);

        double Gmean = stat.getMean();
        double Gstd = stat.getStdDev();

        stat.setData(B);

        double Bmean = stat.getMean();
        double Bstd = stat.getStdDev();

        for(int i = 0; i < dataList.size(); i++){

            //(features2.get(i)[j] - mean) / std;
            Rn[i] = (dataList.get(i).R - Rmean) / Rstd;
            Gn[i] = (dataList.get(i).G - Gmean) / Gstd;
            Bn[i] = (dataList.get(i).B - Bmean) / Bstd;

        }

    }

    public void setPointCloud(LASReader in) throws IOException{

			/*
			LasPoint tempPoint = new LasPoint();

			int label = 0;

			String[] s = null;
			int pointCountThis = 0;



			PointInclusionRule rule = new PointInclusionRule();

			if(output.size() > 0){

				int min = output.size() - pointCount2;
				int max = output.size();

				System.out.println(min + " " + max);

				for(int i = min; i < max; i++){

					tempPoint = dataList.get(i - min);

					s = output.get(i).split("\t");

					label = Integer.parseInt(s[s.length - 6]);

					tempPoint.pointSourceId = (short)label;

					if(LASwrite.writePoint(oWriter, tempPoint, rule, 0.01, 0.01, 0.01, 0, 0, 0, pointCloud.pointDataRecordFormat, i))
						pointCountThis++;
				}

				oWriter.writeBuffer2();

				LASwrite.updateHeader2(oWriter, pointCountThis);

			}



			pointCount2 = 0;
			*/
        this.pointCloud = in;
        this.pointCloud2 = new LASReader(in.getFile());
			/*
			String oname = in.getFile().getAbsolutePath().replaceFirst("[.][^.]+$", "") + "_class.las";;

			File oFile = new File(oname);

			if(oFile.exists())
				oFile.delete();

			oFile.createNewFile();

			oWriter = new LASraf(oFile);

			LASwrite.writeHeader(oWriter, "lasNoise", pointCloud.versionMajor, pointCloud.versionMinor, pointCloud.pointDataRecordFormat, pointCloud.pointDataRecordLength);
			*/
    }

    /** THIS IS A WORKING BACKUP OF PROFILE FOR TRUNKDBH (JUST REMOVE ONE OR TWO COLUMNS AND IT SHOULD WORK */
    public void profile2(double[] treeTop, TFloatArrayList p2, int treeId) throws IOException{

        //System.out.println("HERE! " + treeTop.length + " " + p2.size());
        if(treeTop == null || p2.size() == 0){

            return;
        }

        org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();
        NaturalNeighborInterpolator nni = new NaturalNeighborInterpolator(tin);
        VertexValuatorDefault valuator = new VertexValuatorDefault();

        int label = -1;
        double steps = 10.0;

        boolean understoreyStemFound = false;
        float understoreyStemDiameter = -1.0f;

        HashSet<Float> stems = new HashSet<>();

        profileDistance = new double[(int)steps + 1];
        profileDistanceMax = new double[(int)steps + 1];
        profileSum = new double[(int)steps + 1];
        profileThold = new double[(int)steps + 1];

        double[] profile = new double[(int)steps + 1];

        LasPoint tempPoint = new LasPoint();

        double[] RGB = new double[]{0.0, 0.0, 0.0};

        TFloatArrayList R = new TFloatArrayList();

        //ArrayList<Double> R = new ArrayList<Double>();
        //ArrayList<Double> G = new ArrayList<Double>();
        TFloatArrayList G = new TFloatArrayList();
        //ArrayList<Double> B = new ArrayList<Double>();
        TFloatArrayList B = new TFloatArrayList();

        double distance = 0.0;

        int slot = 0;

        double maxPointZ = Double.NEGATIVE_INFINITY;
        double minPointZ = Double.POSITIVE_INFINITY;

        double prevZ = 0.0;

        boolean inSideAPolygon = false;

        int counter = 0;

        inSideAPolygon = pointInPolygons(new double[]{treeTop[0], treeTop[1]});

        double[][] poly = polygons.get(this.plotIdIndex);

        if(!inSideAPolygon)
            return;

        double maxZ = treeTop[2];

        double minZ = maxZ * 0.5;  //Math.min(maxPointZ - minPointZ, 2.0);

        double increment = (maxZ - minZ) / steps;

        for(int i = 1; i <= (int)steps; i++){

            profileThold[i - 1] = minZ + (double)i * increment;

        }

        double maxDistance = Double.NEGATIVE_INFINITY;

        int pointCount = 0;

        double[] hullPoints = new double[p2.size() * 3];

        int hullPointCounter = 0;

        double hullMinZ = Double.POSITIVE_INFINITY;

        double hullMaxZ = Double.NEGATIVE_INFINITY;

        PriorityQueue<Float> zetas = new PriorityQueue<>();


        double sum_z = 0.0;

        double std_z = 0.0;

        double euc_dist_z = 0.0;

        TDoubleArrayList euc_dist_z_list = new TDoubleArrayList();
        //ArrayList<Double> euc_dist_z_list = new ArrayList<>();

        TDoubleArrayList VARI = new TDoubleArrayList();
        //ArrayList<Double> VARI = new ArrayList<Double>();
        double sumVARI = 0.0;
        TDoubleArrayList TGI = new TDoubleArrayList();
        //ArrayList<Double>  TGI = new ArrayList<Double>();
        double sumTGI = 0.0;
        TDoubleArrayList GRVI = new TDoubleArrayList();
        //ArrayList<Double>  GRVI = new ArrayList<Double>();
        double sumGRVI = 0.0;

        double VARI_temp = 0.0;
        double TGI_temp = 0.0;
        double GRVI_temp = 0.0;
        int pointCounti = 0;

        float[] tempPointArray = new float[8];

        double spectralCount = 0.0;

        for(int i = 0; i < p2.size(); i += 8){

            //pointCloud2.readRecord(in.get(i), tempPoint);
            tempPointArray[0] = p2.getQuick(i);
            tempPointArray[1] = p2.getQuick(i+1);
            tempPointArray[2] = p2.getQuick(i+2);
            tempPointArray[3] = p2.getQuick(i+3);
            tempPointArray[4] = p2.getQuick(i+4);
            tempPointArray[5] = p2.getQuick(i+5);
            tempPointArray[6] = p2.getQuick(i+6);
            tempPointArray[7] = p2.getQuick(i+7);

            /* This means that the point is part of understorey stem */

            //System.out.println(tempPointArray[6]);
            /*
            if(tempPointArray[6] > 0)
            if(!stems.contains(tempPointArray[6]) && tempPointArray[7] > 0){

                stems.add(tempPointArray[6]);
                //understoreyStemFound = true;
                understoreyStemDiameter = tempPointArray[7];
                System.out.println("UNDERSTOREY!!! " + tempPointArray[6]);

                //stemX[tempPoint.classification-1].add(tempPoint.x);
                //stemY[tempPoint.classification-1].add(tempPoint.y);
                continue;
            }
*/
            //System.out.println("HERE! " + tempPoint.z + " mi: " + minZ + " ma: " + maxZ);

            //if(tempPoint.z > minZ && tempPoint.z < maxZ){ //  && goodIndexes.contains(i)
            if(tempPointArray[2] > minZ && tempPointArray[2] <= maxZ){ //  && goodIndexes.contains(i)

                //System.out.println("HERE!!");
                double euc = euclideanDistance(treeTop[0], treeTop[1], tempPointArray[0], tempPointArray[1]);

                euc_dist_z_list.add(euc);
                euc_dist_z += euc;

                sum_z += tempPointArray[2];
                pointCounti++;

                zetas.add(tempPointArray[2]);

                hullPoints[hullPointCounter * 3] = tempPointArray[0];
                hullPoints[hullPointCounter * 3 + 1] = tempPointArray[1];
                hullPoints[hullPointCounter * 3 + 2] = tempPointArray[2];

                if(tempPointArray[2] < hullMinZ)
                    hullMinZ = tempPointArray[2];
                if(tempPointArray[2] > hullMaxZ)
                    hullMaxZ = tempPointArray[2];

                hullPointCounter++;
                /* TODO: 	Make sure that the point is valid, i.e make sure the point
                 is the furthest from the mid point in that height.
                 */


                tin.add(new org.tinfour.common.Vertex(tempPointArray[0], tempPointArray[1], 0.0));

                double perse = tempPointArray[2] / treeTop[2];

                if(euc < 2.5 && perse >= 0.5) {

                    RGB[0] += (tempPointArray[3]);
                    RGB[1] += (tempPointArray[4]);
                    RGB[2] += (tempPointArray[5]);
                    spectralCount++;
                }

                VARI_temp = Double.POSITIVE_INFINITY;
                TGI_temp = Double.POSITIVE_INFINITY;
                GRVI_temp = Double.POSITIVE_INFINITY;

                if( !(tempPointArray[3] == 0 && (tempPointArray[4] == 0 || tempPointArray[5] == 0) ) ||
                        !(tempPointArray[4]== 0 && (tempPointArray[3] == 0 || tempPointArray[5] == 0) )  ||
                        !(tempPointArray[5] == 0 && (tempPointArray[3] == 0 || tempPointArray[4] == 0) ) ) {

                    VARI_temp = (tempPointArray[4] - tempPointArray[3]) / (tempPointArray[4] + tempPointArray[3]- tempPointArray[5]);
                    TGI_temp = tempPointArray[4] - 0.39 * tempPointArray[3] - 0.61 * tempPointArray[5];
                    GRVI_temp = ((tempPointArray[4] - tempPointArray[3]) / (tempPointArray[4] + tempPointArray[3]));

                }

                if(!Double.isNaN(VARI_temp) && VARI_temp != Double.POSITIVE_INFINITY)
                    VARI.add(VARI_temp);
                else
                    VARI.add(0.0);

                if(!Double.isNaN(TGI_temp) && TGI_temp != Double.POSITIVE_INFINITY)
                    TGI.add(TGI_temp);
                else
                    TGI.add(0.0);

                if(!Double.isNaN(GRVI_temp) && GRVI_temp != Double.POSITIVE_INFINITY)
                    GRVI.add(GRVI_temp);
                else
                    GRVI.add(0.0);

                sumVARI += VARI_temp;
                sumTGI += TGI_temp;
                sumGRVI += GRVI_temp;


                R.add(tempPointArray[3]);
                G.add(tempPointArray[4]);
                B.add(tempPointArray[5]);


					/*
					RGB[0] += Rn[i];
					RGB[1] += Gn[i];
					RGB[2] += Bn[i];

					R.add(Rn[i]);
					G.add(Gn[i]);
					B.add(Bn[i]);
					*/

                //distance = euclideanDistance(tempPointArray[0], tempPointArray[1], treeTop[0], treeTop[1]);

                distance = euc;

                if(distance > maxDistance)
                    maxDistance = distance;

                slot = (int)Math.floor( (tempPointArray[2] - minZ) / increment);

                if(distance > profileDistanceMax[slot])
                    profileDistanceMax[slot] = distance;

                profileDistance[slot] += distance;
                profileSum[slot]++;
                pointCount++;
            }
        }


        double mean_euc_dist_z = euc_dist_z / (double)zetas.size();
        double mean_z = sum_z / (double)zetas.size();
        double std_euc_dist_z_2d = 0.0;


        double[] red = new double[zetas.size()];
        double[] green = new double[zetas.size()];
        double[] blue = new double[zetas.size()];

        double[] redProp = new double[zetas.size()];
        double[] greenProp = new double[zetas.size()];
        double[] blueProp = new double[zetas.size()];

        double[] z = new double[zetas.size()];
        double[] z_euc_dist = new double[zetas.size()];

        double[] RG = new double[zetas.size()];
        double[] RB = new double[zetas.size()];
        double[] GB = new double[zetas.size()];

/*
        double VARI[] = new double[zetas.size()];
        double sumVARI = 0.0;
        double TGI[] = new double[zetas.size()];
        double sumTGI = 0.0;
        double GRVI[] = new double[zetas.size()];
        double sumGRVI = 0.0;
*/


        double sumRG = 0.0;
        double sumRB = 0.0;
        double sumGB = 0.0;

        //Iterator<Double> it = zetas.iterator();

        double[] zPercentiles = new double[4];
        double[] percentileDrops = new double[]{0.2, 0.4, 0.6, 0.8};
        int pIndex = 0;
        double[] zDensity = new double[4];

        double stdR = 0.0;
        double stdG = 0.0;
        double stdB = 0.0;

        double stdVARI = 0.0;
        double stdTGI = 0.0;
        double stdGRVI = 0.0;

        double meanR = RGB[0] / spectralCount;
        double meanG = RGB[1] / spectralCount;
        double meanB = RGB[2] / spectralCount;

        double meanVARI = sumVARI / (double)zetas.size();
        double meanTGI = sumTGI / (double)zetas.size();
        double meanGRVI = sumGRVI / (double)zetas.size();

        double[] VARIArray = new double[VARI.size()];
        double[] TGIArray = new double[TGI.size()];
        double[] GRVIArray = new double[GRVI.size()];



        for(int i = 0; i < z.length; i++){
            //System.out.println(zetas.size() + " == " + zetas.size());
            z[i] = zetas.poll();

            std_z += Math.pow(z[i] - mean_z, 2);

            z_euc_dist[i] = Math.pow(euc_dist_z_list.get(i) - mean_euc_dist_z, 2);
            std_euc_dist_z_2d += z_euc_dist[i];

            if(i > (z.length * percentileDrops[Math.min(pIndex, percentileDrops.length - 1)]) && pIndex < percentileDrops.length){
                //System.out.println((zetas.size() * percentileDrops[Math.min(pIndex, percentileDrops.length - 1)]));
                zPercentiles[pIndex] = z[i] ;/// treeTop[2];
                pIndex++;

            }

            red[i] = R.getQuick(i);
            green[i] = G.getQuick(i);
            blue[i] = B.getQuick(i);

            redProp[i] = R.getQuick(i) / (R.getQuick(i) + G.getQuick(i) + B.getQuick(i));
            greenProp[i] = G.getQuick(i) / (R.getQuick(i) + G.getQuick(i) + B.getQuick(i));
            blueProp[i] = B.getQuick(i) / (R.getQuick(i) + G.getQuick(i) + B.getQuick(i));

            RG[i] = ( G.getQuick(i) - R.getQuick(i) ) / (G.getQuick(i) + R.getQuick(i));
            RB[i] = R.getQuick(i) + B.getQuick(i) + G.getQuick(i);
            GB[i] = ( B.getQuick(i) - G.getQuick(i) ) / (B.getQuick(i) + G.getQuick(i));

            stdR += Math.pow(R.getQuick(i) - meanR, 2);
            stdG += Math.pow(G.getQuick(i) - meanG, 2);
            stdB += Math.pow(B.getQuick(i) - meanB, 2);

            stdVARI += Math.pow(VARI.getQuick(i) - meanVARI, 2);
            stdTGI += Math.pow(TGI.getQuick(i) - meanTGI, 2);
            stdGRVI += Math.pow(GRVI.getQuick(i) - meanGRVI, 2);

            GRVIArray[i] = GRVI.getQuick(i);
            TGIArray[i] = TGI.getQuick(i);
            VARIArray[i] = VARI.getQuick(i);
/*
            VARI_temp = (G.get(i) - R.get(i)) / (G.get(i) + R.get(i) - B.get(i));
            TGI_temp = G.get(i) - 0.39 * R.get(i) - 0.61 * B.get(i);
            GRVI_temp = ((G.get(i) - R.get(i)) / (G.get(i) + R.get(i)));

            if(!Double.isNaN(VARI_temp) && VARI_temp != Double.POSITIVE_INFINITY)
                VARI[i] = VARI_temp;
            else
                VARI[i] = 0;

            if(!Double.isNaN(TGI_temp) && TGI_temp != Double.POSITIVE_INFINITY)
                TGI[i] = TGI_temp;
            else
                TGI[i] = 0;

            if(!Double.isNaN(GRVI_temp) && GRVI_temp != Double.POSITIVE_INFINITY)
                GRVI[i] = GRVI_temp;
            else
                GRVI[i] = 0;

            sumVARI += VARI[i];
            sumTGI += TGI[i];
            sumGRVI += GRVI[i];
*/
            sumRG += RG[i];
            sumRB += RB[i];
            sumGB += GB[i];

        }

        stdR = Math.sqrt(stdR / ((double)z.length-1));
        stdG = Math.sqrt(stdG / ((double)z.length-1));
        stdB = Math.sqrt(stdB / ((double)z.length-1));

        stdVARI = Math.sqrt(stdVARI / ((double)z.length-1));
        stdTGI = Math.sqrt(stdTGI / ((double)z.length-1));
        stdGRVI = Math.sqrt(stdGRVI / ((double)z.length-1));


        std_z = Math.sqrt(std_z / ((double)z.length-1));
        std_euc_dist_z_2d = Math.sqrt(std_euc_dist_z_2d / ((double)z.length-1));

        double[] finalHullPoints = new double[ (hullPointCounter) * 3];

        for(int i = 0; i < finalHullPoints.length; i++)
            finalHullPoints[i] = hullPoints[i];


        if(finalHullPoints.length > 10){

            quickhull3d.QuickHull3D hull = null;
            try {
                hull = new quickhull3d.QuickHull3D(finalHullPoints);
            }catch (Exception e){
                return;
            }
            double[] volume2 =  calcVolume3d(hull);

            hull = null;

            double volume = volume2[0];
            double area = volume2[1];

            volume2 = null;

            double hullRatio = (area / volume) * ( (treeTop[2] - hullMinZ) / 2.0);

            if(Double.isNaN(hullRatio)){
                System.out.println(area + " " + volume + " " + tin.getVertices().size());
                return;
            }

            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;

            double min_mean = Double.POSITIVE_INFINITY;
            double max_mean = Double.NEGATIVE_INFINITY;

            for(int i = 0; i < (int)steps; i++){

                profile[i] = profileDistance[i] / profileSum[i];

                if(!Double.isNaN(profile[i])){

                    //System.out.println(profileSum[i]);

                    if(profile[i] < min_mean)
                        min_mean = profile[i];
                    if(profile[i] > max_mean)
                        max_mean = profile[i];

                }
                else
                    profile[i] = 0.0;

                if(!Double.isNaN(profileDistanceMax[i])){

                    //System.out.println(profileSum[i]);

                    if(profileDistanceMax[i] < min)
                        min = profileDistanceMax[i];
                    if(profileDistanceMax[i] > max)
                        max = profileDistanceMax[i];

                }
                else
                    profileDistanceMax[i] = 0.0;

            }

            for(int i = 0; i < (int)steps; i++){

                if(profile[i] != 0.0)
                    profile[i] = (profile[i] - min_mean) / (max_mean - min_mean);
                else
                    profileDistanceMax[i] = 0.0;

                if(profileDistanceMax[i] != 0.0)
                    profileDistanceMax[i] = (profileDistanceMax[i] - min) / (max - min);
                else
                    profileDistanceMax[i] = 0.0;

            }


            Geometry crown_geom = new Geometry(ogr.wkbLinearRing);
            Geometry plot_geom = new Geometry(ogr.wkbLinearRing);
            System.out.println(crown_geom.GetGeometryName());

            int counter2 = 0;

            for(IQuadEdge e : tin.getPerimeter()){

                if(counter2 == 0){
                    crown_geom.AddPoint_2D(e.getA().x, e.getA().y);
                }

                crown_geom.AddPoint_2D(e.getB().x, e.getB().y);

                counter2++;

            }

            Geometry crown_geom_poly = new Geometry(ogr.wkbPolygon);
            Geometry plot_geom_poly = new Geometry(ogr.wkbPolygon);
            crown_geom_poly.AddGeometry(crown_geom);
            double tinArea = tin.countTriangles().getAreaSum();

            int numPolyCorners = poly.length;
            int j = numPolyCorners - 1;
            //boolean isInside = false;

            for (int i = 0; i < numPolyCorners; i++) {


                plot_geom.AddPoint_2D(poly[i][0], poly[i][1]);
            }

            plot_geom_poly.AddGeometry(plot_geom);

            Geometry inter = plot_geom_poly.Intersection(crown_geom_poly);
    /*
            System.out.println(crown_geom_poly.Area());
            System.out.println(plot_geom_poly.Area());
            System.out.println(inter.Area());
            System.out.println(crown_geom_poly.GetGeometryName());

            System.out.println(tinArea);



            System.exit(1);
*/

            double areaInside = inter.Area();
            boolean isIsolated = true;
            String printti = "";

            if((isIsolated)){ // && label < 3){ //  && plotId_major == plotId_major2 && plotId_minor == plotId_minor2)

                printti = printti.concat(std_z + "\t");
                printti = printti.concat(mean_z + "\t");
                printti = printti.concat(maxZ + "\t");
                printti = printti.concat(std_euc_dist_z_2d + "\t");
                printti = printti.concat(hullRatio + "\t");
                printti = printti.concat(volume + "\t");
                printti = printti.concat(area + "\t");
                printti = printti.concat(statsConst.skewness(z, mean_z, std_z) + "\t");
                printti = printti.concat(statsConst.kurtosis(z, mean_z, std_z) + "\t");

                for(int i = 0; i < (int)steps; i++){

                    printti += profile[i]; //profileDistanceMax[i];
                    printti += "\t";

                }

                printti += "-999\t";

                for(double d : zPercentiles){

                    printti += d;
                    printti += "\t";

                }
                printti += "-999\t";
                //printti += mean(zPercentiles) + "\t";
                //printti += skewness(zPercentiles) + "\t";
                //printti += kurtosis(zPercentiles) + "\t";

                //printti += maxDistance / (hullMaxZ - hullMinZ) + "\t";

/*
                double meanR = RGB[0] / (double)pointCount;
                double meanG = RGB[1] / (double)pointCount;
                double meanB = RGB[2] / (double)pointCount;

                double stdR = 0.0;
                double stdG = 0.0;
                double stdB = 0.0;

                double stdVARI = 0.0;
                double stdTGI = 0.0;
                double stdGRVI = 0.0;

                double meanVARI = sumVARI / (double)pointCount;
                double meanTGI = sumTGI / (double)pointCount;
                double meanGRVI = sumGRVI / (double)pointCount;

                for(int i = 0; i < pointCount; i++){

                    stdR += Math.pow(R.get(i) - meanR, 2);
                    stdG += Math.pow(G.get(i) - meanG, 2);
                    stdB += Math.pow(B.get(i) - meanB, 2);

                    stdVARI += Math.pow(VARI[i] - meanVARI, 2);
                    stdTGI += Math.pow(TGI[i] - meanTGI, 2);
                    stdGRVI += Math.pow(GRVI[i] - meanGRVI, 2);


                }
*/
                stdR = Math.sqrt(stdR / ((double)z.length-1));
                stdG = Math.sqrt(stdG / ((double)z.length-1));
                stdB = Math.sqrt(stdB / ((double)z.length-1));

                stdVARI = Math.sqrt(stdVARI / ((double)z.length-1));
                stdTGI = Math.sqrt(stdTGI / ((double)z.length-1));
                stdGRVI = Math.sqrt(stdGRVI / ((double)z.length-1));

                //printti += ((maxZ - minZ) / maxDistance) + "\t";

                //printti += tinArea + "\t";

                //double RGd = meanR / (meanG + meanR);
                //double RBd = meanR / (meanB + meanR);
                //double BGd = meanB / (meanB + meanG);

                //meanR = meanR / (meanR + meanG + meanB);
                //meanG = meanG / (meanR + meanG + meanB);
                //meanB = meanB / (meanR + meanG + meanB);

                printti += meanR + "\t";
                printti += meanG + "\t";
                printti += meanB + "\t";


                System.out.println(meanR + " " + meanG + " " + meanB);

                printti += stdVARI + "\t";
                printti += stdTGI + "\t";
                printti += stdGRVI + "\t";

                printti += meanVARI + "\t";
                printti += meanTGI + "\t";
                printti += meanGRVI + "\t";

                printti += statsConst.skewness(VARIArray, meanVARI, stdVARI) + "\t";
                printti += statsConst.skewness(TGIArray, meanTGI, stdTGI) + "\t";
                printti += statsConst.skewness(GRVIArray, meanGRVI, stdGRVI) + "\t";

                printti += statsConst.kurtosis(VARIArray, meanVARI, stdVARI) + "\t";
                printti += statsConst.kurtosis(TGIArray, meanTGI, stdTGI) + "\t";
                printti += statsConst.kurtosis(GRVIArray, meanGRVI, stdGRVI) + "\t";

                printti += statsConst.skewness(red, meanR, stdR) + "\t";
                printti += statsConst.skewness(green, meanG, stdG) + "\t";
                printti += statsConst.skewness(blue, meanB, stdB) + "\t";

                printti += statsConst.kurtosis(red, meanR, stdR) + "\t";
                printti += statsConst.kurtosis(green, meanG, stdG) + "\t";
                printti += statsConst.kurtosis(blue, meanB, stdB) + "\t";
/*
                printti += mean(redProp) + "\t";
                printti += mean(greenProp) + "\t";
                printti += mean(blueProp) + "\t";

                printti += skewness(redProp) + "\t";
                printti += skewness(greenProp) + "\t";
                printti += skewness(blueProp) + "\t";

                printti += kurtosis(redProp) + "\t";
                printti += kurtosis(greenProp) + "\t";
                printti += kurtosis(blueProp) + "\t";
*/

                printti += stdR + "\t";
                printti += stdG + "\t";
                printti += stdB + "\t";


                //printti += RGd + "\t";
                //printti += RBd + "\t";
                //printti += BGd + "\t";

					/*
					printti += sumRG / (double)RG.length + "\t";
					printti += sumRB / (double)RG.length + "\t";
					printti += sumGB / (double)RG.length + "\t";
					*/
/*
                printti += skewness(RG) + "\t";
                printti += skewness(RB)+ "\t";
                printti += skewness(GB) + "\t";

                printti += kurtosis(RG) + "\t";
                printti += kurtosis(RB)+ "\t";
                printti += kurtosis(GB) + "\t";
*/


					/*
					printti += skewness(red) + "\t";
					printti += skewness(green)+ "\t";
					printti += skewness(blue) + "\t";

					printti += kurtosis(red) + "\t";
					printti += kurtosis(green)+ "\t";
					printti += kurtosis(blue) + "\t";
					*/
                /* VARI */
                //printti += ((meanG - meanR) / (meanG + meanR - meanB)) + "\t";

                /* TGI */
                //printti += (meanG - 0.39 * meanR - 0.61 * meanB) + "\t";

                /* GRVI */

                //printti += ((meanG - meanR) / (meanG + meanR));

					/*
					if(label == 2)
						label = 1;
					if(label >= 3)
						label = 2;
					*/

                //printti += treeTop[0] + "\t" + treeTop[1] + "\t";

                /* Minus one because neural net requires labels to start from 0! */
                //if(treeTopIndex != -1)
                //  printti += (label - 1) + "\t";
                //else
                //  printti += (label - 1) + "\t";
                printti += treeId + "\t";
                printti += tinArea + "\t";
                printti += areaInside + "\t";

/*
                printti += treeTopIndex + "\t";
                printti += treeTopMeasuredHeight + "\t";

 */
                printti += treeTop[0] + "\t";
                printti += treeTop[1] + "\t";
                printti += treeTop[2] + "\t";

                int isInside = inSideAPolygon ? 1 : 0;

                printti += isInside + "\t";
                printti += this.plotId + "\t";

                KdTree.XYZPoint tempTreePoint = new KdTree.XYZPoint(treeTop[0], treeTop[1], treeTop[2]);
                tempTreePoint.setIndex(output.size());
                kd_tree1.add(tempTreePoint);


                //if(treeTopIndex != -1){
                /* Plot ID */

                //  printti += this.plotId + "\t";

                /* Species */
                //printti += (treeBank[treeTopIndex][4] - 1) + "\t";
                //}
                //else{
                /* Plot ID */

                //  printti += this.plotId + "\t";
                //System.out.println(this.plotId);

                /* Species */
                //printti += "" + (-1) + "\t";
                //}
                //int myInt = isIsolated ? 1 : 0;

                //System.out.println(myInt);

                //if(treeTopIndex != -1){
                /* Flag (1) train, (0) validation */
                //  printti += "" + 1; //myInt
                //}
                //else{
                //  printti += "" + (-1);
                //}

                //printLine(printti);

                output.add(printti);
                //System.out.println(printti);

                pointCount2++;

                if(treeTopIndex != -1){
                    this.correctlyDetectedTrees++;
                }

                this.detectedTrees++;

            }
        }

        for(org.tinfour.common.Vertex v : tin.getVertices())
            v = null;

        for(org.tinfour.common.IQuadEdge e : tin.getEdges())
            e = null;

        tin.clear();
        tin.dispose();
        tin = null;

        profileDistance = null;
        profileDistanceMax = null;
        profileSum = null;
        profileThold = null;

        profile = null;

        tempPoint = null;

        RGB = null;

        R = null;
        G = null;
        B = null;

    }

    /** TESTING CONVOLUTION INPUT */
    public void profile(double[] treeTop, TDoubleArrayList p2, int treeId) throws IOException{

        //System.out.println("START!");

        if(treeTop == null || p2.size() == 0){

            return;
        }


        //System.out.println("SUCCESS! " + Arrays.toString(glcm.features));
        //System.exit(1);

        org.tinfour.standard.IncrementalTin tin = new org.tinfour.standard.IncrementalTin();
        NaturalNeighborInterpolator nni = new NaturalNeighborInterpolator(tin);
        VertexValuatorDefault valuator = new VertexValuatorDefault();

        int label = -1;
        double steps = 10.0;

        boolean understoreyStemFound = false;
        float understoreyStemDiameter = -1.0f;

        HashSet<Float> stems = new HashSet<>();

        profileDistance = new double[(int)steps + 1];
        profileDistanceMax = new double[(int)steps + 1];
        profileSum = new double[(int)steps + 1];
        profileThold = new double[(int)steps + 1];

        double[] profile = new double[(int)steps + 1];

        LasPoint tempPoint = new LasPoint();

        double[] RGB = new double[]{0.0, 0.0, 0.0};

        TFloatArrayList R = new TFloatArrayList();

        //ArrayList<Double> R = new ArrayList<Double>();
        //ArrayList<Double> G = new ArrayList<Double>();
        TFloatArrayList G = new TFloatArrayList();
        //ArrayList<Double> B = new ArrayList<Double>();
        TFloatArrayList B = new TFloatArrayList();

        double distance = 0.0;

        int slot = 0;

        double maxPointZ = Double.NEGATIVE_INFINITY;
        double minPointZ = Double.POSITIVE_INFINITY;

        double prevZ = 0.0;

        boolean inSideAPolygon = false;

        int counter = 0;

        inSideAPolygon = pointInPolygons(new double[]{treeTop[0], treeTop[1]});


        /* We do not process trees outside the boundaries of the polygon.
          What if no polygon is input? We should continue anyway.
         */
        if(!inSideAPolygon && polygons.size() > 0)
            return;


        double[][] poly = null;

        if(polygons.size() > 0)
            poly = polygons.get(this.plotIdIndex);


        double maxZ = treeTop[2];

        int x_ = 8;
        int y_ = 8;
        int z_ = 10;

        /* FOR CONVOLUTION */
        int textureResolution_x = 40;
        int textureResolution_y = 40;
        int textureResolution_z = 40;

        /* FOR TEXTURE */
/*
        int textureResolution_x = 100;
        int textureResolution_y = 100;
        int textureResolution_z = 40;

 */
        //BufferedImage image =


        double[] densities = new double[x_ * y_ * z_];
        int[] densities_count = new int[x_ * y_ * z_];
        double[] mu = new double[x_ * y_ * z_];
        double[] sum = new double[x_ * y_ * z_];

        double minZ = maxZ * 0.25;  //Math.min(maxPointZ - minPointZ, 2.0);

        ArrayList<WritableRaster> textureLayers = new ArrayList<>();
        ColorModel cm = null;

        for(int i = 0 ; i < textureResolution_z; i++){
            BufferedImage tmp = new BufferedImage(textureResolution_x, textureResolution_y, BufferedImage.TYPE_BYTE_GRAY);
            ColorModel cm_ = tmp.getColorModel();

            if(i == 0){
                cm = tmp.getColorModel();
            }
            textureLayers.add(cm_.createCompatibleWritableRaster(textureResolution_x, textureResolution_y));
            //textureLayers.add(new BufferedImage(textureResolution_x, textureResolution_y, BufferedImage.TYPE_BYTE_GRAY).getRaster());
        }



        double increment = (maxZ - minZ) / steps;

        for(int i = 1; i <= (int)steps; i++){

            profileThold[i - 1] = minZ + (double)i * increment;

        }

        double maxDistance = Double.NEGATIVE_INFINITY;


        int pointCount = 0;

        double[] hullPoints = new double[p2.size() * 3];

        int hullPointCounter = 0;

        double hullMinZ = Double.POSITIVE_INFINITY;

        double hullMaxZ = Double.NEGATIVE_INFINITY;

        PriorityQueue<Double> zetas = new PriorityQueue<>();

        ArrayList<Double> x = new ArrayList<>();
        ArrayList<Double> y = new ArrayList<>();
        ArrayList<Double> ze = new ArrayList<>();
        ArrayList<Double> intensity = new ArrayList<>();

        double sum_z = 0.0;

        double std_z = 0.0;

        double euc_dist_z = 0.0;

        TDoubleArrayList euc_dist_z_list = new TDoubleArrayList();
        //ArrayList<Double> euc_dist_z_list = new ArrayList<>();

        TDoubleArrayList VARI = new TDoubleArrayList();
        //ArrayList<Double> VARI = new ArrayList<Double>();
        double sumVARI = 0.0;
        TDoubleArrayList TGI = new TDoubleArrayList();
        //ArrayList<Double>  TGI = new ArrayList<Double>();
        double sumTGI = 0.0;
        TDoubleArrayList GRVI = new TDoubleArrayList();
        //ArrayList<Double>  GRVI = new ArrayList<Double>();
        double sumGRVI = 0.0;

        double VARI_temp = 0.0;
        double TGI_temp = 0.0;
        double GRVI_temp = 0.0;
        int pointCounti = 0;

        double[] tempPointArray = new double[8];

        double spectralCount = 0.0;

        double minx = Double.POSITIVE_INFINITY;
        double miny = Double.POSITIVE_INFINITY;

        double maxx = Double.NEGATIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;

        for(int i = 0; i < p2.size(); i += 8){

            //pointCloud2.readRecord(in.get(i), tempPoint);
            tempPointArray[0] = p2.getQuick(i);
            tempPointArray[1] = p2.getQuick(i+1);
            tempPointArray[2] = p2.getQuick(i+2);
            tempPointArray[3] = p2.getQuick(i+3);
            tempPointArray[4] = p2.getQuick(i+4);
            tempPointArray[5] = p2.getQuick(i+5);
            tempPointArray[6] = p2.getQuick(i+6);
            tempPointArray[7] = p2.getQuick(i+7);

            //System.out.println(tempPointArray[0] + " " +  tempPointArray[1]);

            /* This means that the point is part of understorey stem */

           //System.out.println(tempPointArray[6]);
            /*
            if(tempPointArray[6] > 0)
            if(!stems.contains(tempPointArray[6]) && tempPointArray[7] > 0){

                stems.add(tempPointArray[6]);
                //understoreyStemFound = true;
                understoreyStemDiameter = tempPointArray[7];
                System.out.println("UNDERSTOREY!!! " + tempPointArray[6]);

                //stemX[tempPoint.classification-1].add(tempPoint.x);
                //stemY[tempPoint.classification-1].add(tempPoint.y);
                continue;
            }
*/
            //System.out.println("HERE! " + tempPoint.z + " mi: " + minZ + " ma: " + maxZ);

            //if(tempPoint.z > minZ && tempPoint.z < maxZ){ //  && goodIndexes.contains(i)
            //System.out.println(tempPointArray[2] + " " + maxZ + " " + minZ);

            if(tempPointArray[2] > minZ && tempPointArray[2] <= maxZ){ //  && goodIndexes.contains(i)

                if(tempPointArray[0] > maxx)
                    maxx = tempPointArray[0];
                if(tempPointArray[1] > maxy)
                    maxy = tempPointArray[1];
                if(tempPointArray[0] < minx)
                    minx = tempPointArray[0];
                if(tempPointArray[1] < miny)
                    miny = tempPointArray[1];

                //System.out.println("HERE!!");
                double euc = euclideanDistance(treeTop[0], treeTop[1], tempPointArray[0], tempPointArray[1]);

                euc_dist_z_list.add(euc);
                euc_dist_z += euc;

                sum_z += tempPointArray[2];
                pointCounti++;

                zetas.add(tempPointArray[2]);

                if(tempPointArray[7] == 1){
                //if(tempPointArray[7] != 1 && tempPointArray[7] == tempPointArray[5]){
                //if(true){
                    x.add(tempPointArray[0]);
                    y.add(tempPointArray[1]);
                    ze.add(tempPointArray[2]);
                    intensity.add(tempPointArray[6]);

                }


                hullPoints[hullPointCounter * 3] = tempPointArray[0];
                hullPoints[hullPointCounter * 3 + 1] = tempPointArray[1];
                hullPoints[hullPointCounter * 3 + 2] = tempPointArray[2];

                if(tempPointArray[2] < hullMinZ)
                    hullMinZ = tempPointArray[2];
                if(tempPointArray[2] > hullMaxZ)
                    hullMaxZ = tempPointArray[2];

                hullPointCounter++;

                /* TODO: 	Make sure that the point is valid, i.e make sure the point
                 is the furthest from the mid point in that height.
                 */


                /*
                if(!tin.isPointInsideTin(tempPointArray[0], tempPointArray[1])){


                }

                 */
                    tin.add(new org.tinfour.common.Vertex(tempPointArray[0], tempPointArray[1], 0.0));

                double perse = tempPointArray[2] / treeTop[2];

                if(euc < 2.5 && perse >= 0.5) {

                    RGB[0] += (tempPointArray[3]);
                    RGB[1] += (tempPointArray[4]);
                    RGB[2] += (tempPointArray[5]);
                    spectralCount++;
                }

                VARI_temp = Double.POSITIVE_INFINITY;
                TGI_temp = Double.POSITIVE_INFINITY;
                GRVI_temp = Double.POSITIVE_INFINITY;

                if( !(tempPointArray[3] == 0 && (tempPointArray[4] == 0 || tempPointArray[5] == 0) ) ||
                        !(tempPointArray[4]== 0 && (tempPointArray[3] == 0 || tempPointArray[5] == 0) )  ||
                        !(tempPointArray[5] == 0 && (tempPointArray[3] == 0 || tempPointArray[4] == 0) ) ) {

                    VARI_temp = (tempPointArray[4] - tempPointArray[3]) / (tempPointArray[4] + tempPointArray[3]- tempPointArray[5]);
                    TGI_temp = tempPointArray[4] - 0.39 * tempPointArray[3] - 0.61 * tempPointArray[5];
                    GRVI_temp = ((tempPointArray[4] - tempPointArray[3]) / (tempPointArray[4] + tempPointArray[3]));

                }

                if(!Double.isNaN(VARI_temp) && VARI_temp != Double.POSITIVE_INFINITY)
                    VARI.add(VARI_temp);
                else
                    VARI.add(0.0);

                if(!Double.isNaN(TGI_temp) && TGI_temp != Double.POSITIVE_INFINITY)
                    TGI.add(TGI_temp);
                else
                    TGI.add(0.0);

                if(!Double.isNaN(GRVI_temp) && GRVI_temp != Double.POSITIVE_INFINITY)
                    GRVI.add(GRVI_temp);
                else
                    GRVI.add(0.0);

                sumVARI += VARI_temp;
                sumTGI += TGI_temp;
                sumGRVI += GRVI_temp;


                    R.add((float)tempPointArray[3]);
                    G.add((float)tempPointArray[4]);
                    B.add((float)tempPointArray[5]);


					/*
					RGB[0] += Rn[i];
					RGB[1] += Gn[i];
					RGB[2] += Bn[i];

					R.add(Rn[i]);
					G.add(Gn[i]);
					B.add(Bn[i]);
					*/

                //distance = euclideanDistance(tempPointArray[0], tempPointArray[1], treeTop[0], treeTop[1]);

                distance = euc;

                if(distance > maxDistance)
                    maxDistance = distance;

                slot = (int)Math.floor( (tempPointArray[2] - minZ) / increment);

                if(distance > profileDistanceMax[slot])
                    profileDistanceMax[slot] = distance;

                profileDistance[slot] += distance;
                profileSum[slot]++;
                pointCount++;
            }
        }


        double minDis_x = Math.min(maxx - treeTop[0], treeTop[0] - minx);
        double minDis_y = Math.min(maxy - treeTop[1], treeTop[1] - miny);

        //double dimens = Math.min(minDis_x, minDis_y);

        //System.out.println("MINIT: " + minDis_x + " " + minDis_y);

        minx = treeTop[0] - minDis_x;
        maxx = treeTop[0] + minDis_x;

        miny = treeTop[1] - minDis_y;
        maxy = treeTop[1] + minDis_y;

        double texture_size_x = (maxx - minx) / textureResolution_x;
        double texture_size_y = (maxy - miny) / textureResolution_y;
        double texture_size_z = (maxZ - minZ) / textureResolution_z;


        double densities_size_x = (maxx - minx) / x_;
        double densities_size_y = (maxy - miny) / y_;
        double densities_size_z = (maxZ - minZ) / z_;



        double mean_euc_dist_z = euc_dist_z / (double)zetas.size();
        double mean_z = sum_z / (double)zetas.size();
        double std_euc_dist_z_2d = 0.0;


        double[] red = new double[zetas.size()];
        double[] green = new double[zetas.size()];
        double[] blue = new double[zetas.size()];

        double[] redProp = new double[zetas.size()];
        double[] greenProp = new double[zetas.size()];
        double[] blueProp = new double[zetas.size()];

        double[] z = new double[zetas.size()];
        double[] z_euc_dist = new double[zetas.size()];

        double[] RG = new double[zetas.size()];
        double[] RB = new double[zetas.size()];
        double[] GB = new double[zetas.size()];

/*
        double VARI[] = new double[zetas.size()];
        double sumVARI = 0.0;
        double TGI[] = new double[zetas.size()];
        double sumTGI = 0.0;
        double GRVI[] = new double[zetas.size()];
        double sumGRVI = 0.0;
*/


        double sumRG = 0.0;
        double sumRB = 0.0;
        double sumGB = 0.0;

        //Iterator<Double> it = zetas.iterator();

        double[] zPercentiles = new double[4];
        double[] percentileDrops = new double[]{0.2, 0.4, 0.6, 0.8};
        int pIndex = 0;
        double[] zDensity = new double[4];

        double stdR = 0.0;
        double stdG = 0.0;
        double stdB = 0.0;

        double stdVARI = 0.0;
        double stdTGI = 0.0;
        double stdGRVI = 0.0;

        double meanR = RGB[0] / spectralCount;
        double meanG = RGB[1] / spectralCount;
        double meanB = RGB[2] / spectralCount;

        double meanVARI = sumVARI / (double)zetas.size();
        double meanTGI = sumTGI / (double)zetas.size();
        double meanGRVI = sumGRVI / (double)zetas.size();

        double[] VARIArray = new double[VARI.size()];
        double[] TGIArray = new double[TGI.size()];
        double[] GRVIArray = new double[GRVI.size()];

        int xy_counter = 0;
        //double sum = 0.0d;

        for(int i = 0; i < z.length; i++){
            //System.out.println(zetas.size() + " == " + zetas.size());
            z[i] = zetas.poll();

            /* IMPORTANT, THIS IS NOT THE X Y VALUE OF THE Z ABOVE, THE Z IS SORTED; THIS IS NOT!!!! */

            if(xy_counter < x.size()) {

                double x_value = x.get(xy_counter);
                double y_value = y.get(xy_counter);
                double z_value = ze.get(xy_counter);
                double intens = intensity.get(xy_counter);

                double x_dist_to_center = (x_value - treeTop[0]);
                double y_dist_to_center = (y_value - treeTop[1]);
                double z_dist_to_center = (maxZ - z_value);


                xy_counter++;

                if(x_value < maxx && x_value > minx && y_value < maxy && y_value > miny) {

                    int x_coor = Math.min((int) Math.floor((x_value - minx) / densities_size_x), x_ - 1);
                    int y_coor = Math.min((int) Math.floor((maxy - y_value) / densities_size_y), y_ - 1);
                    int z_coor = Math.min((int) Math.floor((z_dist_to_center) / densities_size_z), z_ - 1);

                    int locatio = x_ * y_coor + x_coor + (y_ * x_ * z_coor);

                    int x_coor_tex = Math.min((int) Math.floor((x_value - minx) / texture_size_x), textureResolution_x - 1);
                    int y_coor_tex = Math.min((int) Math.floor((maxy - y_value) / texture_size_y), textureResolution_y - 1);
                    int z_coor_tex = Math.min((int) Math.floor((z_dist_to_center) / texture_size_z), textureResolution_z - 1);

                    int[] valu = new int[]{255};

                    textureLayers.get(z_coor_tex).setPixel(x_coor_tex, y_coor_tex, valu);

                    int[] getValu = new int[]{0};
                    //textureLayers.get(z_coor_tex).getPixel(x_coor_tex, y_coor_tex, getValu);
                    //System.out.println("Getting: " + getValu[0]);
                    //System.out.println(y_dist_to_center + " " + densities_size_y + " " + ((maxy - y_value) / densities_size_y));
                    //System.out.prin   tln(x_coor + " " + y_coor + " " + z_coor);

                    densities[locatio] += intens;
                    densities_count[locatio]++;

                    double delta = intens - mu[locatio];
                    mu[locatio] += delta / (double) densities_count[locatio];
                    sum[locatio] += ((double) densities_count[locatio] - 1) / (double) densities_count[locatio] * delta * delta;
                }
                //sum++;


            }

            std_z += Math.pow(z[i] - mean_z, 2);

            z_euc_dist[i] = Math.pow(euc_dist_z_list.get(i) - mean_euc_dist_z, 2);
            std_euc_dist_z_2d += z_euc_dist[i];

            if(i > (z.length * percentileDrops[Math.min(pIndex, percentileDrops.length - 1)]) && pIndex < percentileDrops.length){
                //System.out.println((zetas.size() * percentileDrops[Math.min(pIndex, percentileDrops.length - 1)]));
                zPercentiles[pIndex] = z[i] ;/// treeTop[2];
                pIndex++;

            }

            red[i] = R.getQuick(i);
            green[i] = G.getQuick(i);
            blue[i] = B.getQuick(i);

            redProp[i] = R.getQuick(i) / (R.getQuick(i) + G.getQuick(i) + B.getQuick(i));
            greenProp[i] = G.getQuick(i) / (R.getQuick(i) + G.getQuick(i) + B.getQuick(i));
            blueProp[i] = B.getQuick(i) / (R.getQuick(i) + G.getQuick(i) + B.getQuick(i));

            RG[i] = ( G.getQuick(i) - R.getQuick(i) ) / (G.getQuick(i) + R.getQuick(i));
            RB[i] = R.getQuick(i) + B.getQuick(i) + G.getQuick(i);
            GB[i] = ( B.getQuick(i) - G.getQuick(i) ) / (B.getQuick(i) + G.getQuick(i));

            stdR += Math.pow(R.getQuick(i) - meanR, 2);
            stdG += Math.pow(G.getQuick(i) - meanG, 2);
            stdB += Math.pow(B.getQuick(i) - meanB, 2);

            stdVARI += Math.pow(VARI.getQuick(i) - meanVARI, 2);
            stdTGI += Math.pow(TGI.getQuick(i) - meanTGI, 2);
            stdGRVI += Math.pow(GRVI.getQuick(i) - meanGRVI, 2);

            GRVIArray[i] = GRVI.getQuick(i);
            TGIArray[i] = TGI.getQuick(i);
            VARIArray[i] = VARI.getQuick(i);
/*
            VARI_temp = (G.get(i) - R.get(i)) / (G.get(i) + R.get(i) - B.get(i));
            TGI_temp = G.get(i) - 0.39 * R.get(i) - 0.61 * B.get(i);
            GRVI_temp = ((G.get(i) - R.get(i)) / (G.get(i) + R.get(i)));

            if(!Double.isNaN(VARI_temp) && VARI_temp != Double.POSITIVE_INFINITY)
                VARI[i] = VARI_temp;
            else
                VARI[i] = 0;

            if(!Double.isNaN(TGI_temp) && TGI_temp != Double.POSITIVE_INFINITY)
                TGI[i] = TGI_temp;
            else
                TGI[i] = 0;

            if(!Double.isNaN(GRVI_temp) && GRVI_temp != Double.POSITIVE_INFINITY)
                GRVI[i] = GRVI_temp;
            else
                GRVI[i] = 0;

            sumVARI += VARI[i];
            sumTGI += TGI[i];
            sumGRVI += GRVI[i];
*/
            sumRG += RG[i];
            sumRB += RB[i];
            sumGB += GB[i];

        }

        stdR = Math.sqrt(stdR / ((double)z.length-1));
        stdG = Math.sqrt(stdG / ((double)z.length-1));
        stdB = Math.sqrt(stdB / ((double)z.length-1));

        stdVARI = Math.sqrt(stdVARI / ((double)z.length-1));
        stdTGI = Math.sqrt(stdTGI / ((double)z.length-1));
        stdGRVI = Math.sqrt(stdGRVI / ((double)z.length-1));


        std_z = Math.sqrt(std_z / ((double)z.length-1));
        std_euc_dist_z_2d = Math.sqrt(std_euc_dist_z_2d / ((double)z.length-1));

        double[] finalHullPoints = new double[ (hullPointCounter) * 3];

        for(int i = 0; i < finalHullPoints.length; i++)
            finalHullPoints[i] = hullPoints[i];

        ArrayList<float[]> textureFeatures = new ArrayList<>();

        for(int i = 0; i < textureLayers.size(); i++){

            GLCM glcm_temp = new GLCM();
            glcm_temp.setHaralickDist(1);

            GLCM.imageArray = new byte[]{};
            GLCM.imageArray = ((DataBufferByte)(textureLayers.get(i).getDataBuffer())).getData();

            //System.out.println(Arrays.toString(glcm_temp.imageArray));

            glcm_temp.process(textureLayers.get(i));

            glcm_temp.data = new ArrayList<>(1);
            glcm_temp.addData(glcm_temp.features);
            List<double[]> featuresHar=glcm_temp.getFeatures();

            String featureString = "";

            for (double[] feature : featuresHar) {
                featureString = Arrays2.join(feature, ",", "%.5f");
            }

            String[] featureStr=featureString.split(Pattern.quote(","));
            float[] featureFlot = new float[featureStr.length];
            for (int i_=0;i_<featureStr.length;i_++){
                featureFlot[i_]=Float.parseFloat(featureStr[i_]);
            }
//featureFlot is array that contain all 14 haralick features
            //glcm_temp.process(textureLayers.get(i));
            textureFeatures.add(featureFlot);
            //System.out.println("LAYER: " + i + Arrays.toString(featureFlot));
        }

        double ratio = 1.0 / (densities_size_x * densities_size_y * densities_size_y);

        for(int i = 0; i < densities.length; i++){

            if(densities_count[i] > 1)
                densities[i] /= densities_count[i];
                //densities[i] /= Math.sqrt(sum[i] / ((double)densities_count[i] - 1));
                //densities[i] = (double)densities_count[i] * ratio;
            else
                densities[i] = 0;
        }

        //System.out.println("HERE!! " + finalHullPoints.length + "  " + p2.size());


        if(finalHullPoints.length > 10){


            quickhull3d.QuickHull3D hull = null;
            try {
                hull = new quickhull3d.QuickHull3D(finalHullPoints);
            }catch (Exception e){
                return;
            }





            double[] volume2 =  calcVolume3d(hull);

            hull = null;

            double volume = volume2[0];
            double area = volume2[1];

            volume2 = null;

            double hullRatio = (area / volume) * ( (treeTop[2] - hullMinZ) / 2.0);

            if(Double.isNaN(hullRatio)){
                //System.out.println(area + " " + volume + " " + tin.getVertices().size());
                return;
            }

            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;

            double min_mean = Double.POSITIVE_INFINITY;
            double max_mean = Double.NEGATIVE_INFINITY;

            for(int i = 0; i < (int)steps; i++){

                profile[i] = profileDistance[i] / profileSum[i];

                if(!Double.isNaN(profile[i])){

                    //System.out.println(profileSum[i]);

                    if(profile[i] < min_mean)
                        min_mean = profile[i];
                    if(profile[i] > max_mean)
                        max_mean = profile[i];

                }
                else
                    profile[i] = 0.0;

                if(!Double.isNaN(profileDistanceMax[i])){

                    //System.out.println(profileSum[i]);

                    if(profileDistanceMax[i] < min)
                        min = profileDistanceMax[i];
                    if(profileDistanceMax[i] > max)
                        max = profileDistanceMax[i];

                }
                else
                    profileDistanceMax[i] = 0.0;

            }

            for(int i = 0; i < (int)steps; i++){

                if(profile[i] != 0.0)
                    profile[i] = (profile[i] - min_mean) / (max_mean - min_mean);
                else
                    profileDistanceMax[i] = 0.0;

                if(profileDistanceMax[i] != 0.0)
                    profileDistanceMax[i] = (profileDistanceMax[i] - min) / (max - min);
                else
                    profileDistanceMax[i] = 0.0;

            }


            Geometry crown_geom = new Geometry(ogr.wkbLinearRing);
            Geometry plot_geom = new Geometry(ogr.wkbLinearRing);
            //System.out.println(crown_geom.GetGeometryName());

            int counter2 = 0;

            for(IQuadEdge e : tin.getPerimeter()){

                if(counter2 == 0){
                    crown_geom.AddPoint_2D(e.getA().x, e.getA().y);
                }

                crown_geom.AddPoint_2D(e.getB().x, e.getB().y);

                counter2++;

            }

            Geometry crown_geom_poly = new Geometry(ogr.wkbPolygon);
            Geometry plot_geom_poly = new Geometry(ogr.wkbPolygon);
            crown_geom_poly.AddGeometry(crown_geom);
            double tinArea = tin.countTriangles().getAreaSum();

            Geometry inter = null;

            if(polygons.size() > 0){
                int numPolyCorners = poly.length;
                int j = numPolyCorners - 1;
                //boolean isInside = false;

                for (int i = 0; i < numPolyCorners; i++) {
                    plot_geom.AddPoint_2D(poly[i][0], poly[i][1]);
                }

                plot_geom_poly.AddGeometry(plot_geom);

                inter = plot_geom_poly.Intersection(crown_geom_poly);
            }

    /*
            System.out.println(crown_geom_poly.Area());
            System.out.println(plot_geom_poly.Area());
            System.out.println(inter.Area());
            System.out.println(crown_geom_poly.GetGeometryName());

            System.out.println(tinArea);

            System.exit(1);
*/

            HashSet<Integer> obsoleteFeatures = new HashSet<>();

            /* Correlation of 1 */
            obsoleteFeatures.add(7);
            obsoleteFeatures.add(8);
            obsoleteFeatures.add(4);
            obsoleteFeatures.add(9);

            /* correlation of 0.99 */
            obsoleteFeatures.add(5);
            obsoleteFeatures.add(10);

            /* Useless based on LDA */
            obsoleteFeatures.add(3);

            /* Write image */

            ogr.RegisterAll(); //Registering all the formats..
            gdal.AllRegister();

            Dataset dataset = null;
            Driver driver = null;
            Band band = null;


            int METHOD_DBB = 1;
            int METHOD_JAVA_ARRAYS = 2;
            int method = 2;

            int xsize = 100;
            int ysize = 100;

            int nbIters = 1;
            int nbands = 40;

            driver = gdal.GetDriverByName("Gtiff");

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * xsize);
            byteBuffer.order(ByteOrder.nativeOrder());
            FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();

            int[] intArray = new int[xsize];
            float[] floatArray = new float[xsize];

            //int a = gdalconst.GDT_Float32 == 1 ? 1 : 2;
            //System.out.println(filename);
            String name = "/home/koomikko/Documents/research/3d_tree_species/convolution_data/" + output.size() + ".tif";
            //dataset = driver.Create(name, xsize, ysize, nbands, gdalconst.GDT_Byte);

            File outFile = new File(name);

            int[] pixVal = new int[1];

           cm.getColorSpace().toString();

            ImageWriter writer = ImageIO.getImageWritersByFormatName("TIFF").next();


                try (ImageOutputStream output = ImageIO.createImageOutputStream(outFile)) {

                    writer.setOutput(output);

                    ImageWriteParam params = writer.getDefaultWriteParam();
                    params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

                    // Compression: None, PackBits, ZLib, Deflate, LZW, JPEG and CCITT variants allowed
                    // (different plugins may use a different set of compression type names)
                    params.setCompressionType("Deflate");

                    writer.prepareWriteSequence(null);

                    for(int i = 0; i < textureLayers.size(); i++){

                        BufferedImage img = new BufferedImage(cm, textureLayers.get(i), false, null);
                        writer.writeToSequence(new IIOImage(img, null, null), params);

                    }

                    // We're done
                    writer.endWriteSequence();
                }

            double areaInside = 0.0;

            if(polygons.size() > 0)
                areaInside = inter.Area();

            boolean isIsolated = true;
            String printti = "";

            if((isIsolated)){ // && label < 3){ //  && plotId_major == plotId_major2 && plotId_minor == plotId_minor2)

                printti = printti.concat(std_z + "\t");
                printti = printti.concat(mean_z + "\t");
                printti = printti.concat(maxZ + "\t");
                printti = printti.concat(std_euc_dist_z_2d + "\t");
                printti = printti.concat(hullRatio + "\t");
                printti = printti.concat(volume + "\t");
                printti = printti.concat(area + "\t");
                printti = printti.concat(statsConst.skewness(z, mean_z, std_z) + "\t");
                printti = printti.concat(statsConst.kurtosis(z, mean_z, std_z) + "\t");

                for(int i = 0; i < (int)steps; i++){

                    printti += profile[i]; //profileDistanceMax[i];
                    printti += "\t";

                }

                printti += "-999\t";

                for(double d : zPercentiles){

                    printti += d;
                    printti += "\t";

                }
                printti += "-999\t";

                printti += "-666\t";

                for(double d : densities){
                    printti += d;
                    printti += "\t";
                }

                if(true)
                for(int i = 0; i < textureFeatures.size(); i++){
/*
                    BufferedImage newImg = new BufferedImage(textureLayers.get(i).getWidth(), textureLayers.get(i).getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                    newImg.setData(textureLayers.get(i));
                    String filename = "./haralick/" + this.plotId + "_" + treeId + "_slice_" + i + ".png";

                    System.out.println(textureLayers.get(i).getWidth() + " " + textureLayers.get(i).getHeight());
                    File outputfile = new File(filename);
                    ImageIO.write(newImg, "png", outputfile);

*/
                    for(int j_ = 0; j_ < textureFeatures.get(i).length; j_++){
                        if(j_ <= 10) {
                            if(!obsoleteFeatures.contains(j_)){
                                printti += textureFeatures.get(i)[j_];
                                printti += "\t";
                            }

                        }
                    }
                }

                int[] pixelValue = new int[]{0};
                int size = textureResolution_x*textureResolution_y;

                StringBuilder sb = new StringBuilder();

                if(false)
                    for(int i = 0; i < textureLayers.size(); i++) {

                        //BufferedImage img = new BufferedImage(cm, textureLayers.get(i), false, null);

                        //byte[] buffer = new byte[]{};
                        byte[] buffer = ((DataBufferByte)(textureLayers.get(i).getDataBuffer())).getData();
                        int counter_ = 0;

                        for (int pos = 0; pos < size; pos++) {

                            int gray = buffer[pos]&0xff;
                            //System.out.println(gray);
                                //;
                                //textureLayers.get(i).getPixel(x_, y_, pixelValue);
                                //printti = printti.concat(Integer.toString(gray));
                                //printti += "1";
                                //printti += "\t";
                                sb.append(gray);
                                sb.append("\t");



                            }

                        System.out.println("here " + i);

                    }
                printti += sb.toString();

                printti += "-666\t";

                //printti += mean(zPercentiles) + "\t";
                //printti += skewness(zPercentiles) + "\t";
                //printti += kurtosis(zPercentiles) + "\t";

                //printti += maxDistance / (hullMaxZ - hullMinZ) + "\t";

/*printti += "-999\t";
                double meanR = RGB[0] / (double)pointCount;
                double meanG = RGB[1] / (double)pointCount;
                double meanB = RGB[2] / (double)pointCount;

                double stdR = 0.0;
                double stdG = 0.0;
                double stdB = 0.0;

                double stdVARI = 0.0;
                double stdTGI = 0.0;
                double stdGRVI = 0.0;

                double meanVARI = sumVARI / (double)pointCount;
                double meanTGI = sumTGI / (double)pointCount;
                double meanGRVI = sumGRVI / (double)pointCount;

                for(int i = 0; i < pointCount; i++){

                    stdR += Math.pow(R.get(i) - meanR, 2);
                    stdG += Math.pow(G.get(i) - meanG, 2);
                    stdB += Math.pow(B.get(i) - meanB, 2);

                    stdVARI += Math.pow(VARI[i] - meanVARI, 2);
                    stdTGI += Math.pow(TGI[i] - meanTGI, 2);
                    stdGRVI += Math.pow(GRVI[i] - meanGRVI, 2);


                }
*/
                stdR = Math.sqrt(stdR / ((double)z.length-1));
                stdG = Math.sqrt(stdG / ((double)z.length-1));
                stdB = Math.sqrt(stdB / ((double)z.length-1));

                stdVARI = Math.sqrt(stdVARI / ((double)z.length-1));
                stdTGI = Math.sqrt(stdTGI / ((double)z.length-1));
                stdGRVI = Math.sqrt(stdGRVI / ((double)z.length-1));

                //printti += ((maxZ - minZ) / maxDistance) + "\t";

                //printti += tinArea + "\t";

                //double RGd = meanR / (meanG + meanR);
                //double RBd = meanR / (meanB + meanR);
                //double BGd = meanB / (meanB + meanG);

                //meanR = meanR / (meanR + meanG + meanB);
                //meanG = meanG / (meanR + meanG + meanB);
                //meanB = meanB / (meanR + meanG + meanB);

                printti += meanR + "\t";
                printti += meanG + "\t";
                printti += meanB + "\t";


                System.out.println(meanR + " " + meanG + " " + meanB);

                printti += stdVARI + "\t";
                printti += stdTGI + "\t";
                printti += stdGRVI + "\t";

                printti += meanVARI + "\t";
                printti += meanTGI + "\t";
                printti += meanGRVI + "\t";

                printti += statsConst.skewness(VARIArray, meanVARI, stdVARI) + "\t";
                printti += statsConst.skewness(TGIArray, meanTGI, stdTGI) + "\t";
                printti += statsConst.skewness(GRVIArray, meanGRVI, stdGRVI) + "\t";

                printti += statsConst.kurtosis(VARIArray, meanVARI, stdVARI) + "\t";
                printti += statsConst.kurtosis(TGIArray, meanTGI, stdTGI) + "\t";
                printti += statsConst.kurtosis(GRVIArray, meanGRVI, stdGRVI) + "\t";

                printti += statsConst.skewness(red, meanR, stdR) + "\t";
                printti += statsConst.skewness(green, meanG, stdG) + "\t";
                printti += statsConst.skewness(blue, meanB, stdB) + "\t";

                printti += statsConst.kurtosis(red, meanR, stdR) + "\t";
                printti += statsConst.kurtosis(green, meanG, stdG) + "\t";
                printti += statsConst.kurtosis(blue, meanB, stdB) + "\t";
/*
                printti += mean(redProp) + "\t";
                printti += mean(greenProp) + "\t";
                printti += mean(blueProp) + "\t";

                printti += skewness(redProp) + "\t";
                printti += skewness(greenProp) + "\t";
                printti += skewness(blueProp) + "\t";

                printti += kurtosis(redProp) + "\t";
                printti += kurtosis(greenProp) + "\t";
                printti += kurtosis(blueProp) + "\t";
*/

                printti += stdR + "\t";
                printti += stdG + "\t";
                printti += stdB + "\t";


                //printti += RGd + "\t";
                //printti += RBd + "\t";
                //printti += BGd + "\t";

					/*
					printti += sumRG / (double)RG.length + "\t";
					printti += sumRB / (double)RG.length + "\t";
					printti += sumGB / (double)RG.length + "\t";
					*/
/*
                printti += skewness(RG) + "\t";
                printti += skewness(RB)+ "\t";
                printti += skewness(GB) + "\t";

                printti += kurtosis(RG) + "\t";
                printti += kurtosis(RB)+ "\t";
                printti += kurtosis(GB) + "\t";
*/


					/*
					printti += skewness(red) + "\t";
					printti += skewness(green)+ "\t";
					printti += skewness(blue) + "\t";

					printti += kurtosis(red) + "\t";
					printti += kurtosis(green)+ "\t";
					printti += kurtosis(blue) + "\t";
					*/
                /* VARI */
                //printti += ((meanG - meanR) / (meanG + meanR - meanB)) + "\t";

                /* TGI */
                //printti += (meanG - 0.39 * meanR - 0.61 * meanB) + "\t";

                /* GRVI */

                //printti += ((meanG - meanR) / (meanG + meanR));

					/*
					if(label == 2)
						label = 1;
					if(label >= 3)
						label = 2;
					*/

                //printti += treeTop[0] + "\t" + treeTop[1] + "\t";

                /* Minus one because neural net requires labels to start from 0! */
                //if(treeTopIndex != -1)
                  //  printti += (label - 1) + "\t";
                //else
                  //  printti += (label - 1) + "\t";
                printti += treeId + "\t";
                printti += tinArea + "\t";
                printti += areaInside + "\t";

/*
                printti += treeTopIndex + "\t";
                printti += treeTopMeasuredHeight + "\t";

 */
                printti += treeTop[0] + "\t";
                printti += treeTop[1] + "\t";
                printti += treeTop[2] + "\t";

                int isInside = inSideAPolygon ? 1 : 0;

                printti += isInside + "\t";
                printti += this.plotId + "\t";

                KdTree.XYZPoint tempTreePoint = new KdTree.XYZPoint(treeTop[0], treeTop[1], treeTop[2]);
                tempTreePoint.setIndex(output.size());
                kd_tree1.add(tempTreePoint);


                //if(treeTopIndex != -1){
                /* Plot ID */

                  //  printti += this.plotId + "\t";

                /* Species */
                    //printti += (treeBank[treeTopIndex][4] - 1) + "\t";
                //}
                //else{
                /* Plot ID */

                  //  printti += this.plotId + "\t";
                    //System.out.println(this.plotId);

                /* Species */
                    //printti += "" + (-1) + "\t";
                //}
                //int myInt = isIsolated ? 1 : 0;

                //System.out.println(myInt);

                //if(treeTopIndex != -1){
                /* Flag (1) train, (0) validation */
                  //  printti += "" + 1; //myInt
                //}
                //else{
                  //  printti += "" + (-1);
                //}

                //printLine(printti);

                output.add(printti);
                //System.out.println(printti);

                pointCount2++;

                if(treeTopIndex != -1){
                    this.correctlyDetectedTrees++;
                }

                this.detectedTrees++;

            }
        }

        for(org.tinfour.common.Vertex v : tin.getVertices())
            v = null;

        for(org.tinfour.common.IQuadEdge e : tin.getEdges())
            e = null;

        tin.clear();
        tin.dispose();
        tin = null;

        profileDistance = null;
        profileDistanceMax = null;
        profileSum = null;
        profileThold = null;

        profile = null;

        tempPoint = null;

        RGB = null;

        R = null;
        G = null;
        B = null;

    }


    public void labelTrees(){

        float[] asdi;
        HashMap<Integer, float[]> matches = new HashMap<>();

        KdTree.XYZPoint searchPoint = new KdTree.XYZPoint(0,0,0);

        List<KdTree.XYZPoint> nearest;
        List<KdTree.XYZPoint> nearest2;

        double maxDistance = 2.0;
        double distance = 0.0;

        boolean terminate = true;

        int k = 0;
        Iterator it = kd_tree1.iterator();

        int index;

        /* Tree x*/
        //trees[treeCount][0] = Double.parseDouble(line.split(",")[3]);

        /* Tree y*/
        //trees[treeCount][1] = Double.parseDouble(line.split(",")[4]);

        /* Tree height*/
        //trees[treeCount][2] = Double.parseDouble(line.split(",")[11]);

        /* Tree diameter*/
        //trees[treeCount][3] = Double.parseDouble(line.split(",")[9]);

        /* Tree species*/
        //trees[treeCount][4] = Math.min(Double.parseDouble(line.split(",")[7]), 3.0);

        /* Tree WHAT?*/
        //trees[treeCount][5] = 0.0;

        /* Tree volume*/
        //trees[treeCount][6] = Double.parseDouble(line.split(",")[42]);

        /* Tree plot-id*/
        //trees[treeCount][7] = Double.parseDouble(line.split(",")[2])

        /* Tree id*/
        //trees[treeCount][8] = Double.parseDouble(line.split(",")[1]);

        while(it.hasNext()){


            KdTree.XYZPoint tem = (KdTree.XYZPoint)it.next();

            nearest = (List<KdTree.XYZPoint>)kd_tree2.nearestNeighbourSearch(1, tem);

            int indeksi = 0;

            if(nearest.size() > 0)
                //nearest2 = (List<KdTree.XYZPoint>)kd_tree1.nearestNeighbourSearch(1, nearest.get(0));
                indeksi = ((List<KdTree.XYZPoint>)kd_tree1.nearestNeighbourSearch(1, nearest.get(0))).get(0).getIndex();
            else{
                indeksi = tem.getIndex();
            }

            distance = tem.euclideanDistance(nearest.get(0));
            boolean isInside = false;

            /* This is just so that we can label trees without having a polygon input */
            if(polygons.size() > 0)
                isInside = Integer.parseInt(output.get(tem.getIndex()).split("\t")[output.get(tem.getIndex()).split("\t").length-2]) == 1;

            //if(tem.getIndex() == nearest2.get(0).getIndex()){
            if(tem.getIndex() == indeksi && nearest.size() > 0 && distance < maxDistance){

                index = nearest.get(0).getIndex();

                String temp = output.get(tem.getIndex());

                temp += treeBank[index][7] + "\t";
                temp += treeBank[index][8] + "\t";
                temp += treeBank[index][2] + "\t";
                temp += (treeBank[index][4]-1) + "\t";


                /* HERE WE MOVE THE FILE "index".tif to directory (treeBank[index][4]-1) */

                String fileToBeMoved = "/home/koomikko/Documents/research/3d_tree_species/convolution_data/" + indeksi + ".tif";
                String moveTo = "/home/koomikko/Documents/research/3d_tree_species/convolution_data/" + (int)(treeBank[index][4]-1) + "/" + indeksi + ".tif";

                try {
                    Files.move(Paths.get(fileToBeMoved), Paths.get(moveTo), StandardCopyOption.REPLACE_EXISTING);
                }catch (Exception e){
                    e.printStackTrace();
                }

                temp += 1 + "\t";

                output.set(tem.getIndex(), temp);

            }else if(!isInside && nearest.size() > 0){


                index = nearest.get(0).getIndex();


                String temp = output.get(tem.getIndex());

                temp += treeBank[index][7] + "\t";
                temp += "-1\t";
                temp += "-1\t";
                temp += "-1\t";
                temp += -1 + "\t";

                output.set(tem.getIndex(), temp);
            }else if(isInside && nearest.size() > 0){


                index = nearest.get(0).getIndex();


                String temp = output.get(tem.getIndex());

                temp += treeBank[index][7] + "\t";
                temp += "-1\t";
                temp += "-1\t";
                temp += "-1\t";
                temp += 1 + "\t";

                output.set(tem.getIndex(), temp);
            }else{ // if(nearest.size() == 0){

                String temp = output.get(tem.getIndex());

                temp += "-1\t";
                temp += "-1\t";
                temp += "-1\t";
                temp += "-1\t";
                temp += 1 + "\t";

                output.set(tem.getIndex(), temp);

            }

        }


        if(true)
            return;

        for(int i = 0; i < treeBank.length; i++) {

            searchPoint.setX(treeBank[i][0]);
            searchPoint.setY(treeBank[i][1]);
            searchPoint.setZ(treeBank[i][2]);

            nearest = (List<KdTree.XYZPoint>)kd_tree1.nearestNeighbourSearch(1, searchPoint);

            distance = nearest.get(0).euclideanDistance(searchPoint);

            if(distance < maxDistance){

                if(!matches.containsKey(nearest.get(0).getIndex())){

                    matches.put(nearest.get(0).getIndex(), new float[]{i, (float)distance});

                }
                /* This ITD tree has already been labeled! */
                else{

                    asdi = matches.get(nearest.get(0).getIndex());

                    /* But this measured tree is closer! What to do?! */
                    /* Continue searching closest neighbors of the ALREADY labeled measured tree */
                    if(asdi[1] < distance){

                        searchPoint.setX(treeBank[(int)asdi[0]][0]);
                        searchPoint.setY(treeBank[(int)asdi[0]][1]);
                        searchPoint.setZ(treeBank[(int)asdi[0]][2]);

                        terminate = true;
                        k = 2;

                        while(terminate){

                            nearest = (List<KdTree.XYZPoint>)kd_tree1.nearestNeighbourSearch(k, searchPoint);

                            for(int n = 0; n < nearest.size(); n++){

                                distance = nearest.get(n).euclideanDistance(searchPoint);

                                if(!matches.containsKey(nearest.get(0).getIndex()) && distance < maxDistance){

                                }
                                else{

                                }

                            }

                            k++;

                        }
                    }
                    /* But this measured tree is not closer! What to do?! */
                    /* Continue searching closest neighbors of THIS measured tree */
                    else{

                        terminate = true;
                        k = 2;

                        while(terminate){

                            nearest = (List<KdTree.XYZPoint>)kd_tree1.nearestNeighbourSearch(k, searchPoint);

                            for(int n = 0; n < nearest.size(); n++){



                            }

                            k++;

                        }
                    }

                }
            }


        }
    }

    public void showDetectionRate(int plotSize){

        FileWriter fileWriter = null;
        FileWriter fileWriter2 = null;

        try {
            fileWriter = new FileWriter("testi.txt");
            fileWriter2 = new FileWriter("testi2.txt");
            fileWriter.write("plot_id\tmean_crown_area\tstd_crown_area\tskew_crown_area\tkurt_crown_area\tmean_std_z\tstd_std_z\tskew_std_z\tkurt_std_z\tmean_std_euc_dist_z_2d\t" +
                            "std_std_euc_dist_z_2d\tskew_std_euc_dist_z_2d\tkurt_std_euc_dist_z_2d\tcrownPercentage\n");


        }catch (Exception e){
            e.printStackTrace();
        }


        /* Tree x*/
        //trees[treeCount][0] = Double.parseDouble(line.split(",")[3]);

        /* Tree y*/
        //trees[treeCount][1] = Double.parseDouble(line.split(",")[4]);

        /* Tree height*/
        //trees[treeCount][2] = Double.parseDouble(line.split(",")[11]);

        /* Tree diameter*/
        //trees[treeCount][3] = Double.parseDouble(line.split(",")[9]);

        /* Tree species*/
        //trees[treeCount][4] = Math.min(Double.parseDouble(line.split(",")[7]), 3.0);

        /* Tree WHAT?*/
        //trees[treeCount][5] = 0.0;

        /* Tree volume*/
        //trees[treeCount][6] = Double.parseDouble(line.split(",")[42]);

        /* Tree plot-id*/
        //trees[treeCount][7] = Double.parseDouble(line.split(",")[2])
        double maxHeight = Double.NEGATIVE_INFINITY;

        double height = 0.0;
        double height_pred = 0.0;

        boolean goodOrNot = false;

        boolean rightPlot = false;

        Iterator<Integer> it = plots.iterator();
        Statistics stats = new Statistics();
        while(it.hasNext()){

            int j = it.next();

            this.correctlyDetectedTrees = 0;
            this.detectedTrees = 0;

            this.detected_over_5 = 0;
            this.detected_over_10 = 0;
            this.detected_over_15 = 0;
            this.detected_over_20 = 0;

            this.overDetected_over_5 = 0;
            this.overDetected_over_10 = 0;
            this.overDetected_over_15 = 0;
            this.overDetected_over_20 = 0;

            this.groundMeasured_over_5 = 0;
            this.groundMeasured_over_10 = 0;
            this.groundMeasured_over_15 = 0;
            this.groundMeasured_over_20 = 0;

            this.groundMeasuredTrees = 0;

            ArrayList<Double> measured_08 = new ArrayList<>();
            ArrayList<Double> predicted_08 = new ArrayList<>();

            ArrayList<Double> measured_06 = new ArrayList<>();
            ArrayList<Double> predicted_06 = new ArrayList<>();

            ArrayList<Double> measured_04 = new ArrayList<>();
            ArrayList<Double> predicted_04 = new ArrayList<>();

            ArrayList<Double> measured_02 = new ArrayList<>();
            ArrayList<Double> predicted_02 = new ArrayList<>();

            maxHeight = Double.NEGATIVE_INFINITY;
            double maxHeight_detected = Double.NEGATIVE_INFINITY;

            for(int i = 0; i < treeBank.length; i++) {

                if((int)(treeBank[i][7]) == j){
                    this.groundMeasuredTrees++;
                    if(treeBank[i][2] > maxHeight)
                        maxHeight = treeBank[i][2];

                }
            }

            for(int i = 0; i < treeBank.length; i++) {

                if((int)(treeBank[i][7]) == j){
                    if(treeBank[i][2] > maxHeight * 0.8)
                        groundMeasured_over_20++;
                    if(treeBank[i][2] > maxHeight * 0.6)
                        groundMeasured_over_15++;
                    if(treeBank[i][2] > maxHeight * 0.4)
                        groundMeasured_over_10++;
                    if(treeBank[i][2] > maxHeight * 0.2)
                        groundMeasured_over_5++;
                }
            }

            ArrayList<Double> crownArea = new ArrayList<>();
            ArrayList<Double> std_z = new ArrayList<>();
            ArrayList<Double> std_euc_dist_z_2d = new ArrayList<>();

            ArrayList<Double> hullRatio = new ArrayList<>();
            ArrayList<Double> hullVolume = new ArrayList<>();
            ArrayList<Double> hullArea = new ArrayList<>();

            double crownAreaSum = 0.0;

            for (Iterator<String> it1 = output.iterator(); it1.hasNext();){

                String[] s = it1.next().split("\t");

                goodOrNot = (int)Double.parseDouble(s[s.length - 1]) == -1;

                int plotti = (int)(Double.parseDouble(s[s.length - 5]));

                rightPlot = (plotti == j);

                height = Double.parseDouble(s[s.length - 3]);
                height_pred = Double.parseDouble(s[s.length - 7]);

                if(!goodOrNot && rightPlot){

                    crownArea.add(Double.parseDouble(s[s.length - 10]));
                    std_z.add(Double.parseDouble(s[0]));
                    std_euc_dist_z_2d.add(Double.parseDouble(s[1]));
                    crownAreaSum += Double.parseDouble(s[s.length - 10]);

                    hullRatio.add(Double.parseDouble(s[2]));
                    hullVolume.add(Double.parseDouble(s[3]));
                    hullArea.add(Double.parseDouble(s[4]));

                    detectedTrees++;
                    correctlyDetectedTrees++;

                    if(height > maxHeight * 0.8) {
                        detected_over_20++;
                        measured_08.add(height);
                        predicted_08.add(height_pred);
                    }
                    if(height > maxHeight * 0.6) {
                        detected_over_15++;
                        measured_06.add(height);
                        predicted_06.add(height_pred);
                    }
                    if(height > maxHeight * 0.4) {
                        detected_over_10++;
                        measured_04.add(height);
                        predicted_04.add(height_pred);
                    }
                    if(height > maxHeight * 0.2) {
                        detected_over_5++;
                        measured_02.add(height);
                        predicted_02.add(height_pred);
                    }
                }

                if(goodOrNot && rightPlot){

                    detectedTrees++;

                    if(height_pred > maxHeight * 0.8) {
                        overDetected_over_20++;
                    }
                    if(height_pred > maxHeight * 0.6) {
                        overDetected_over_15++;

                    }
                    if(height_pred > maxHeight * 0.4) {
                        overDetected_over_10++;

                    }
                    if(height_pred > maxHeight * 0.2) {
                        overDetected_over_5++;

                    }

                }

            }

            stats.setData(crownArea);
            double mean_crown_area = stats.getMeanFromList();
            double std_crown_area = stats.getStdDevFromList(mean_crown_area);
            double skew_crown_area = stats.skewness(crownArea, mean_crown_area, std_crown_area);
            double kurt_crown_area = stats.kurtosis(crownArea, mean_crown_area, std_crown_area);

            stats.setData(std_z);
            double mean_std_z = stats.getMeanFromList();
            double std_std_z = stats.getStdDevFromList(mean_std_z);
            double skew_std_z = stats.skewness(std_z, mean_std_z, std_std_z);
            double kurt_std_z = stats.kurtosis(std_z, mean_std_z, std_std_z);

            stats.setData(std_euc_dist_z_2d);
            double mean_std_euc_dist_z_2d = stats.getMeanFromList();
            double std_std_euc_dist_z_2d = stats.getStdDevFromList(mean_std_euc_dist_z_2d);
            double skew_std_euc_dist_z_2d = stats.skewness(std_euc_dist_z_2d, mean_std_euc_dist_z_2d, std_std_euc_dist_z_2d);
            double kurt_std_euc_dist_z_2d = stats.kurtosis(std_euc_dist_z_2d, mean_std_euc_dist_z_2d, std_std_euc_dist_z_2d);




            double prop08 = (overDetected_over_20 / (overDetected_over_20 + detected_over_20))*100.0;
            double prop06 = (overDetected_over_15 / (overDetected_over_15 + detected_over_15))*100.0;
            double prop04 = (overDetected_over_10 / (overDetected_over_10 + detected_over_10))*100.0;
            double prop02 = (overDetected_over_5 / (overDetected_over_5 + detected_over_5))*100.0;

            double[] error_08 = calcRMSE(measured_08, predicted_08);
            double[] error_06 = calcRMSE(measured_06, predicted_06);
            double[] error_04 = calcRMSE(measured_04, predicted_04);
            double[] error_02 = calcRMSE(measured_02, predicted_02);

            System.out.println("\nPLOT ID: " + j);

            System.out.println("Total delineated trees: " + this.detectedTrees);
            System.out.println("Correctly delineated trees: " + this.correctlyDetectedTrees);
            System.out.println("Ground measured trees: " + this.groundMeasuredTrees);

            System.out.println("\nDetection rates:");
            System.out.printf("\n" + "%-8s" + "%-5s" + "%-5s" + "%-12s" + "%-12s" + "%-11s" + "%-11s" + "%-11s" + "%-11s", "p", "det", "obs", "DetRate(%)", "OverDet(%)", "h rmse(m)", "h rmse(%)", "h MD(m)", "h MD(%)");
            System.out.printf("\n" + "_____________________________________________________________________________________");

            System.out.printf("\n" + "%-8s" + "%-5.0f" + "%-5.0f" + "%-12.2f" + "%-12.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f", "> 0.8", detected_over_20, groundMeasured_over_20, (detected_over_20 / groundMeasured_over_20)*100.0, prop08, error_08[0], error_08[1], error_08[2], error_08[3]);
            System.out.printf("\n" + "%-8s" + "%-5.0f" + "%-5.0f" + "%-12.2f" + "%-12.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f", "> 0.6", detected_over_15, groundMeasured_over_15, (detected_over_15 / groundMeasured_over_15)*100.0, prop06, error_06[0], error_06[1], error_06[2], error_06[3]);
            System.out.printf("\n" + "%-8s" + "%-5.0f" + "%-5.0f" + "%-12.2f" + "%-12.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f", "> 0.4", detected_over_10, groundMeasured_over_10, (detected_over_10 / groundMeasured_over_10)*100.0, prop04, error_04[0], error_04[1], error_04[2], error_04[3]);
            System.out.printf("\n" + "%-8s" + "%-5.0f" + "%-5.0f" + "%-12.2f" + "%-12.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f", "> 0.2", detected_over_5, groundMeasured_over_5, (detected_over_5 / groundMeasured_over_5)*100.0, prop02, error_02[0], error_02[1], error_02[2], error_02[3]);

            System.out.printf("\np. = percentile (0.x * maxHeight)");
            System.out.printf("\ndet = delineated trees, obs = measured trees, OverDet = treesegments without ground match");
            System.out.printf("\nh = tree height, RMSE = root mean square error, MD = mean difference\n");

            try {
                fileWriter.write(j + "\t" + mean_crown_area + "\t" + std_crown_area + "\t" + skew_crown_area + "\t" + kurt_crown_area + "\t" +
                        mean_std_z + "\t" + std_std_z + "\t" + skew_std_z + "\t" + kurt_std_z + "\t" +
                        mean_std_euc_dist_z_2d + "\t" + std_std_euc_dist_z_2d + "\t" + skew_std_euc_dist_z_2d + "\t" + kurt_std_euc_dist_z_2d + "\t" +
                        (crownAreaSum / (plotSize)) + "\n");

                fileWriter2.write("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n");
                fileWriter2.write("PLOT ID: " + j + "\n");

                fileWriter2.write("Total delineated trees: " + this.detectedTrees + "\n");
                fileWriter2.write("Correctly delineated trees: " + this.correctlyDetectedTrees + "\n");
                fileWriter2.write("Ground measured trees: " + this.groundMeasuredTrees + "\n");

                fileWriter2.write("Detection rates:");
                fileWriter2.write(String.format("\n" + "%-8s" + "%-5s" + "%-5s" + "%-12s" + "%-12s" + "%-11s" + "%-11s" + "%-11s" + "%-11s", "p", "det", "obs", "DetRate(%)", "OverDet(%)", "h rmse(m)", "h rmse(%)", "h MD(m)", "h MD(%)"));
                fileWriter2.write(String.format("\n" + "_____________________________________________________________________________________"));

                fileWriter2.write(String.format("\n" + "%-8s" + "%-5.0f" + "%-5.0f" + "%-12.2f" + "%-12.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f", "> 0.8", detected_over_20, groundMeasured_over_20, (detected_over_20 / groundMeasured_over_20)*100.0, prop08, error_08[0], error_08[1], error_08[2], error_08[3]));
                fileWriter2.write(String.format("\n" + "%-8s" + "%-5.0f" + "%-5.0f" + "%-12.2f" + "%-12.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f", "> 0.6", detected_over_15, groundMeasured_over_15, (detected_over_15 / groundMeasured_over_15)*100.0, prop06, error_06[0], error_06[1], error_06[2], error_06[3]));
                fileWriter2.write(String.format("\n" + "%-8s" + "%-5.0f" + "%-5.0f" + "%-12.2f" + "%-12.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f", "> 0.4", detected_over_10, groundMeasured_over_10, (detected_over_10 / groundMeasured_over_10)*100.0, prop04, error_04[0], error_04[1], error_04[2], error_04[3]));
                fileWriter2.write(String.format("\n" + "%-8s" + "%-5.0f" + "%-5.0f" + "%-12.2f" + "%-12.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f" + "%-11.2f", "> 0.2", detected_over_5, groundMeasured_over_5, (detected_over_5 / groundMeasured_over_5)*100.0, prop02, error_02[0], error_02[1], error_02[2], error_02[3]));

                fileWriter2.write(String.format("\np. = percentile (0.x * maxHeight)"));
                fileWriter2.write(String.format("\ndet = delineated trees, obs = measured trees, OverDet = treesegments without ground match"));
                fileWriter2.write(String.format("\nh = tree height, RMSE = root mean square error, MD = mean difference\n"));

            }catch (Exception e){
                e.printStackTrace();
            }

        }

        try {
            fileWriter.close();
            fileWriter2.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public double[] calcRMSE(ArrayList<Double> measured, ArrayList<Double> predicted){

        double sum = 0.0;
        double squ = 0.0;

        double sum_obs = 0.0;
        double sum_pred = 0.0;

        double MD = 0.0;

        for(int i = 0; i < predicted.size(); i++){

            squ += ((predicted.get(i) - measured.get(i)) * (predicted.get(i) - measured.get(i)));
            sum += measured.get(i);

            sum_obs += measured.get(i);
            sum_pred += predicted.get(i);

            MD += (predicted.get(i) - measured.get(i));

        }



        return new double[]{((Math.sqrt(squ / (double)measured.size()))), ((Math.sqrt(squ / (double)measured.size())) / (sum / (double)measured.size())  * 100.0),
                (MD / (double)predicted.size()), (MD / (double)predicted.size()) / (sum_obs / (double)measured.size()) * 100.0 };
    }

    public void readMeasuredTrees(File measuredTreesFile) throws IOException{

        int treeCount = 0;

        int lineCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(measuredTreesFile))) {
            String line;
            while ((line = br.readLine()) != null) {

                if(lineCount != 0)
                    //if(Double.parseDouble(line.split(",")[5]) == -1.0)
                    treeCount++;

                lineCount++;

            }
        }

        double[][] trees = new double[treeCount][10];

        treeCount = 0;
        lineCount = 0;



        try (BufferedReader br = new BufferedReader(new FileReader(measuredTreesFile))) {

            String line;

            while ((line = br.readLine()) != null) {

                if(lineCount != 0){
                    //if(Double.parseDouble(line.split(",")[5]) == -1.0){

                    /* Tree x*/
                    trees[treeCount][0] = Double.parseDouble(line.split(",")[3]);

                    /* Tree y*/
                    trees[treeCount][1] = Double.parseDouble(line.split(",")[4]);

                    /* Tree height*/
                    trees[treeCount][2] = Double.parseDouble(line.split(",")[11]);

                    /* Tree diameter*/
                    trees[treeCount][3] = Double.parseDouble(line.split(",")[9]);

                    /* Tree species*/
                    trees[treeCount][4] = Math.min(Double.parseDouble(line.split(",")[7]), 3.0);

                    /* Tree WHAT?*/
                    trees[treeCount][5] = 0.0;

                    /* Tree volume*/
                    trees[treeCount][6] = Double.parseDouble(line.split(",")[42]);

                    /* Tree plot-id*/
                    trees[treeCount][7] = Double.parseDouble(line.split(",")[2]);

                    /* Tree id*/
                    trees[treeCount][8] = Double.parseDouble(line.split(",")[1]);

                    KdTree.XYZPoint tempTreePoint = new KdTree.XYZPoint(0,0,0);
                    tempTreePoint.setX(trees[treeCount][0]);
                    tempTreePoint.setY(trees[treeCount][1]);
                    tempTreePoint.setZ(trees[treeCount][2]);
                    tempTreePoint.setIndex(treeCount);

                    kd_tree2.add(tempTreePoint);

                    treeCount++;

                }

                lineCount++;
            }
        }

        this.setTreeBank(trees.clone());

    }

    public void readMeasuredTrees_2d(File measuredTreesFile) throws IOException{

        int treeCount = 0;

        int lineCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(measuredTreesFile))) {
            String line;
            while ((line = br.readLine()) != null) {

                if(lineCount != 0 || true)
                    //if(Double.parseDouble(line.split(",")[5]) == -1.0)
                    treeCount++;

                lineCount++;

            }
        }

        double[][] trees = new double[treeCount][10];

        treeCount = 0;
        lineCount = 0;



        double maxDiam = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(measuredTreesFile))) {

            String line;

            while ((line = br.readLine()) != null) {

                if(lineCount != 0 || true){
                    //if(Double.parseDouble(line.split(",")[5]) == -1.0){
                    //System.out.println(line);
                    //System.exit(1);
                    /* Tree x*/
                    trees[treeCount][0] = Double.parseDouble(line.split(",")[3]);

                    /* Tree y*/
                    trees[treeCount][1] = Double.parseDouble(line.split(",")[4]);

                    /* Tree height*/
                    trees[treeCount][2] = Double.parseDouble(line.split(",")[11]);

                    /* Tree diameter*/
                    trees[treeCount][3] = Double.parseDouble(line.split(",")[9]);

                    /* Tree species*/
                    trees[treeCount][4] = Math.min(Double.parseDouble(line.split(",")[7]), 3.0);

                    /* Tree WHAT?*/
                    trees[treeCount][5] = 0.0;

                    /* Tree volume*/
                    trees[treeCount][6] = Double.parseDouble(line.split(",")[42]);

                    /* Tree plot-id*/
                    trees[treeCount][7] = Double.parseDouble(line.split(",")[2]);

                    /* Tree id*/
                    trees[treeCount][8] = Double.parseDouble(line.split(",")[1]);

                    KdTree.XYZPoint tempTreePoint = new KdTree.XYZPoint(0,0,0);
                    tempTreePoint.setX(trees[treeCount][0]);
                    tempTreePoint.setY(trees[treeCount][1]);
                    tempTreePoint.setZ(0.0);
                    tempTreePoint.setIndex(treeCount);

                    kd_tree2.add(tempTreePoint);

                    treeCount++;

                }

                lineCount++;
            }
        }

        //System.out.println("max diam: " + maxDiam);
        //System.exit(1);

        this.setTreeBank(trees.clone());

    }

    public void readFieldPlots(File plots) throws IOException{

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        if(!plots.exists())
            return;

        DataSource ds = ogr.Open(plots.getAbsolutePath());
        System.out.println("Layer count: " + ds.GetLayerCount());
        Layer layeri = ds.GetLayer(0);
        System.out.println("Feature count: " + layeri.GetFeatureCount());


        File fout = new File("tempWKT.csv");

        if(fout.exists())
            fout.delete();

        fout.createNewFile();

        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        bw.write("WKT,plot_id");
        bw.newLine();

        //System.out.println("Feature count: " + layeri.GetFeatureCount());

        for(long i = 0; i < layeri.GetFeatureCount(); i++ ){

            Feature tempF = layeri.GetFeature(i);
            Geometry tempG = tempF.GetGeometryRef();
            //layeri.GetGeomType();
            String id = "";

            if(tempF.GetFieldCount() > 0)
                id = tempF.GetFieldAsString(this.aR.field);
            else
                id = String.valueOf(i);

            //System.out.println(tempG.ExportToWkt());
            String out = "\"" + tempG.ExportToWkt() + "\"," + id;

            //System.out.println();
            bw.write(out);
            bw.newLine();


        }

        bw.close();


        ArrayList<double[][]> polyBank1 = new ArrayList<double[][]>();
        ArrayList<Double> plotID1 = new ArrayList<Double>();
        //String tiedosto_coord = "plotsTEST30.csv";
        //String tiedosto_coord = "plotsTEST15.csv";
        String tiedosto_coord = "tempWKT.csv";
        String line1 = "";

        File tiedostoCoord = new File(tiedosto_coord);
        tiedostoCoord.setReadable(true);

        BufferedReader sc = new BufferedReader( new FileReader(tiedostoCoord));
        sc.readLine();

        while((line1 = sc.readLine())!= null){

            //System.out.println(line1);

            String[] tokens =  line1.split(",");
            System.out.println(Arrays.toString(tokens));

            if(Double.parseDouble(tokens[tokens.length - 1]) != -999){

                plotID1.add(Double.parseDouble(tokens[tokens.length - 1]));

                double[][] tempPoly = new double[tokens.length - 1][2];
                int n = (tokens.length) - 2;
                int counteri = 0;

                tempPoly[counteri][0] = Double.parseDouble(tokens[0].split("\\(\\(")[1].split(" ")[0]);
                tempPoly[counteri][1] = Double.parseDouble(tokens[0].split("\\(\\(")[1].split(" ")[1]);

                counteri++;

                boolean breikki = false;

                for(int i = 1; i < n; i++){
                    //System.out.println(tokens[i]);

                    if(tokens[i].split(" ")[0].contains(")") || tokens[i].split(" ")[1].contains(")")){
                        plotID1.remove(plotID1.size() - 1);
                        breikki = true;
                        break;
                    }

                    tempPoly[counteri][0] = Double.parseDouble(tokens[i].split(" ")[0]);
                    tempPoly[counteri][1] = Double.parseDouble(tokens[i].split(" ")[1]);
                    //System.out.println(Arrays.toString(tempPoly[counteri]));
                    counteri++;
                }

                tempPoly[counteri][0] = Double.parseDouble(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")[0]);
                tempPoly[counteri][1] = Double.parseDouble(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")[1]);

                if(!breikki)
                    polyBank1.add(tempPoly);
            }

        }

        this.setPolygon(polyBank1, plotID1);


    }

    public boolean processPointCloud(LASReader pointCloud) throws IOException{

        int treeId = 0;

        this.setPointCloud(pointCloud);

        LasPoint tempPoint = new LasPoint();

        THashMap<Integer, TDoubleArrayList>  mappi13 = new THashMap<>();
        THashMap<Integer, TDoubleArrayList>  mappi_smallerTrees = new THashMap<>();
        TIntIntHashMap  mappi2 = new TIntIntHashMap();
        TIntIntHashMap  mappi32 = new TIntIntHashMap();
        HashMap<Integer, double[]>  mappi3 = new HashMap<>();

        HashMap<Integer, double[]>  mappi_smallerTreesStats = new HashMap<>();

        HashMap<Integer, double[]>  mappi_allStemStats = new HashMap<>();
        HashMap<Integer, double[]>  mappi_onlyOver = new HashMap<>();

        double resolution = 1.0;

        int xDim = (int)Math.ceil((pointCloud.getMaxX() - pointCloud.getMinX()) / resolution ) + 1;
        int yDim = (int)Math.ceil((pointCloud.getMaxY() - pointCloud.getMinY()) / resolution ) + 1;
        int zDim = (int)Math.ceil((pointCloud.getMaxZ() - pointCloud.getMinZ()) / resolution ) + 1;

        float[][] raster = new float[xDim][yDim];

        int maxi = 0;

        int xCoord, yCoord;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                pointCloud.readRecord_noRAF(i, tempPoint, 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
                    continue;
                }

                treeId = tempPoint.pointSourceId;

                xCoord= (int) Math.floor((tempPoint.x - pointCloud.getMinX()) / resolution);
                yCoord = (int) Math.floor((pointCloud.getMaxY() - tempPoint.y) / resolution);

                if(tempPoint.z > raster[xCoord][yCoord])
                    raster[xCoord][yCoord] = (float)tempPoint.z;

                /* Means this point is part of a stem */
                if(tempPoint.classification == 5 || tempPoint.classification == 1){

                    if(mappi_allStemStats.containsKey(tempPoint.intensity)){
                        mappi_allStemStats.get(tempPoint.intensity)[5]++;
                        mappi_allStemStats.get(tempPoint.intensity)[0] += tempPoint.x;
                        mappi_allStemStats.get(tempPoint.intensity)[1] += tempPoint.y;
                        mappi_allStemStats.get(tempPoint.intensity)[2] += tempPoint.z;

                    }else if (!mappi_allStemStats.containsKey(tempPoint.intensity)){
                        mappi_allStemStats.put(tempPoint.intensity, new double[]{tempPoint.x, tempPoint.y, tempPoint.z, tempPoint.gpsTime,
                                tempPoint.userData, 1, tempPoint.pointSourceId});
                    }
                }

                /* Means this point is part of an ITD stem */
                if(tempPoint.classification == 1){

                    if(mappi_onlyOver.containsKey(tempPoint.intensity)){
                        mappi_onlyOver.get(tempPoint.intensity)[5]++;
                        mappi_onlyOver.get(tempPoint.intensity)[0] += tempPoint.x;
                        mappi_onlyOver.get(tempPoint.intensity)[1] += tempPoint.y;
                        mappi_onlyOver.get(tempPoint.intensity)[2] += tempPoint.z;

                    }else if (!mappi_onlyOver.containsKey(tempPoint.intensity)){
                        mappi_onlyOver.put(tempPoint.intensity, new double[]{tempPoint.x, tempPoint.y, tempPoint.z, tempPoint.gpsTime,
                                tempPoint.userData, 1, tempPoint.pointSourceId});
                    }
                }

                /* Means this point is part of an understorey stem */
                if(tempPoint.classification == 5){

                    if(mappi_smallerTreesStats.containsKey(tempPoint.intensity)){
                        mappi_smallerTreesStats.get(tempPoint.intensity)[5]++;
                        mappi_smallerTreesStats.get(tempPoint.intensity)[0] += tempPoint.x;
                        mappi_smallerTreesStats.get(tempPoint.intensity)[1] += tempPoint.y;
                        mappi_smallerTreesStats.get(tempPoint.intensity)[2] += tempPoint.z;

                    }else if (!mappi_smallerTreesStats.containsKey(tempPoint.intensity)){
                        mappi_smallerTreesStats.put(tempPoint.intensity, new double[]{tempPoint.x, tempPoint.y, tempPoint.z, tempPoint.gpsTime,
                                tempPoint.userData, 1, tempPoint.pointSourceId});
                    }

                    if(mappi32.containsKey(tempPoint.intensity)){
                        mappi32.put(tempPoint.intensity, mappi32.get(tempPoint.intensity) + 1);

                    }else{
                        mappi32.put(tempPoint.intensity, 0);
                    }

                    continue;
                }

                if(treeId <= 0)
                    continue;

                if(mappi2.containsKey(treeId)){
                    mappi2.put(treeId, mappi2.get(treeId) + 1);

                }else{
                    mappi2.put(treeId, 0);
                }

                /* Means the point is part of the treeTop x and y location */
                if(tempPoint.synthetic){

                    if(mappi3.containsKey(treeId) && tempPoint.z > mappi3.get(treeId)[2]){
                        mappi3.get(treeId)[0] = tempPoint.x;
                        mappi3.get(treeId)[1] = tempPoint.y;
                        mappi3.get(treeId)[2] = tempPoint.z;

                    }else if (!mappi3.containsKey(treeId)){
                        mappi3.put(treeId, new double[]{tempPoint.x, tempPoint.y, tempPoint.z});
                    }

                }

            }

        }

        System.out.println("DONE! " + mappi2.size());

        maxi = 0;
        int count = 0;

        long start = System.currentTimeMillis();
        int treeCount = 0;

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));

            try {
                pointCloud.readRecord_noRAF(i, tempPoint, 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                /* Reading, so ask if this point is ok, or if
                it should be modified.
                 */
                if(!aR.inclusionRule.ask(tempPoint, i+j, true)){
                    continue;
                }

                treeId = tempPoint.pointSourceId;

                /* Means this point is part of an understorey stem */
                if(tempPoint.classification == 8) {

                    if(mappi_smallerTrees.containsKey(treeId)){

                        mappi_smallerTrees.get(treeId).add(tempPoint.x);
                        mappi_smallerTrees.get(treeId).add(tempPoint.y);
                        mappi_smallerTrees.get(treeId).add(tempPoint.z);

                    }else{

                        mappi_smallerTrees.put(treeId, new TDoubleArrayList());
                        mappi_smallerTrees.get(treeId).add(tempPoint.x);
                        mappi_smallerTrees.get(treeId).add(tempPoint.y);
                        mappi_smallerTrees.get(treeId).add(tempPoint.z);

                    }
                }

                if(treeId > 0 && mappi2.containsKey(treeId)) {

                    mappi2.put(treeId, mappi2.get(treeId) - 1);

                    if(mappi13.containsKey(treeId)){
                        //mappi1.get(treeId).add(i + j);
                        //mappi12.get(treeId).add(new float[]{(float)tempPoint.x, (float)tempPoint.y, (float)tempPoint.z, tempPoint.R, tempPoint.G, tempPoint.B});
                        mappi13.get(treeId).add(tempPoint.x);
                        mappi13.get(treeId).add(tempPoint.y);
                        mappi13.get(treeId).add(tempPoint.z);
                        mappi13.get(treeId).add(tempPoint.R);
                        mappi13.get(treeId).add(tempPoint.G);
                        mappi13.get(treeId).add(tempPoint.B);
                        mappi13.get(treeId).add(tempPoint.intensity);
                        mappi13.get(treeId).add(tempPoint.returnNumber);
                    }else{

                        mappi13.put(treeId, new TDoubleArrayList());
                        mappi13.get(treeId).add(tempPoint.x);
                        mappi13.get(treeId).add(tempPoint.y);
                        mappi13.get(treeId).add(tempPoint.z);
                        mappi13.get(treeId).add(tempPoint.R);
                        mappi13.get(treeId).add(tempPoint.G);
                        mappi13.get(treeId).add(tempPoint.B);
                        mappi13.get(treeId).add(tempPoint.intensity);
                        mappi13.get(treeId).add(tempPoint.returnNumber);
                    }

                    if(mappi2.get(treeId) < 0){
                        start = System.nanoTime();

                        this.profile(mappi3.get(treeId), mappi13.get(treeId), treeId);

                        System.out.println((treeCount) + " / " + output.size() + " : " + treeId);

                        mappi13.get(treeId).clear();
                        mappi13.remove(treeId);
                        mappi2.remove(treeId);
                        mappi3.remove(treeId);

                        if(treeCount % 10 == 0){
                            System.gc();
                            System.gc();
                            System.gc();
                        }

                        mappi2.remove(treeId);
                        treeCount++;

                    }
                }
            }
        }

        System.out.println(mappi_smallerTreesStats.size() + " < " + mappi_allStemStats.size());

        for(int i : mappi_smallerTreesStats.keySet()){

            double xLocation = mappi_smallerTreesStats.get(i)[0] / mappi_smallerTreesStats.get(i)[5];
            double yLocation = mappi_smallerTreesStats.get(i)[1] / mappi_smallerTreesStats.get(i)[5];

            float plot_id = stemInPolygons(new double[]{xLocation, yLocation});

            if(plot_id != -99.9f){

                xCoord= (int) Math.floor((xLocation - pointCloud.getMinX()) / resolution);
                yCoord = (int) Math.floor((pointCloud.getMaxY() - yLocation) / resolution);

                double zet = raster [xCoord][yCoord];

                double dx = zet - mappi_smallerTreesStats.get(i)[4] / 10.0;
                double dy = mappi_smallerTreesStats.get(i)[3] / 2.0;
                double slope = dy / dx;

                //System.out.println(zet + " " + dx + " " + dy + " " + slope + " " + (mappi_smallerTreesStats.get(i)[4] / 10.0));
                double slopeValue_1_3m = slope * (zet - 1.3);

                double diameter_1_3m = slopeValue_1_3m * 2.0d;



                //System.out.println(diameter_1_3m + " > " + mappi_smallerTreesStats.get(i)[3]);
                String outString = plot_id + "\t" + (mappi_smallerTreesStats.get(i)[6]) + "\t" + xLocation  + "\t" + yLocation + "\t" + (mappi_smallerTreesStats.get(i)[3]) + "\t" + zet;
                out_stem.println(outString);
            }

        }

        for(int i : mappi_allStemStats.keySet()){

            double xLocation = mappi_allStemStats.get(i)[0] / mappi_allStemStats.get(i)[5];
            double yLocation = mappi_allStemStats.get(i)[1] / mappi_allStemStats.get(i)[5];

            float plot_id = stemInPolygons(new double[]{xLocation, yLocation});

            if(plot_id != -99.9f){

                xCoord= (int) Math.floor((xLocation - pointCloud.getMinX()) / resolution);
                yCoord = (int) Math.floor((pointCloud.getMaxY() - yLocation) / resolution);

                double zet = raster [xCoord][yCoord];

                double dx = zet - mappi_allStemStats.get(i)[4] / 10.0;
                double dy = mappi_allStemStats.get(i)[3] / 2.0;
                double slope = dy / dx;

                //System.out.println(zet + " " + dx + " " + dy + " " + slope + " " + (mappi_smallerTreesStats.get(i)[4] / 10.0));
                double slopeValue_1_3m = slope * (zet - 1.3);

                double diameter_1_3m = slopeValue_1_3m * 2.0d;


                //System.out.println(diameter_1_3m + " > " + mappi_smallerTreesStats.get(i)[3]);
                String outString = plot_id + "\t" + (mappi_allStemStats.get(i)[6]) + "\t" + xLocation + "\t" + yLocation + "\t" + (mappi_allStemStats.get(i)[3]) + "\t" + zet;
                out_stem2.println(outString);
            }

        }

        for(int i : mappi_onlyOver.keySet()){


            double xLocation = mappi_onlyOver.get(i)[0] / mappi_onlyOver.get(i)[5];
            double yLocation = mappi_onlyOver.get(i)[1] / mappi_onlyOver.get(i)[5];

            float plot_id = stemInPolygons(new double[]{xLocation, yLocation});

            if(plot_id != -99.9f){

                xCoord= (int) Math.floor((xLocation - pointCloud.getMinX()) / resolution);
                yCoord = (int) Math.floor((pointCloud.getMaxY() - yLocation) / resolution);

                double zet = raster [xCoord][yCoord];

                double dx = zet - mappi_onlyOver.get(i)[4] / 10.0;
                double dy = mappi_onlyOver.get(i)[3] / 2.0;
                double slope = dy / dx;

                //System.out.println(zet + " " + dx + " " + dy + " " + slope + " " + (mappi_smallerTreesStats.get(i)[4] / 10.0));
                double slopeValue_1_3m = slope * (zet - 1.3);

                double diameter_1_3m = slopeValue_1_3m * 2.0d;


                //System.out.println(diameter_1_3m + " > " + mappi_smallerTreesStats.get(i)[3]);
                String outString = plot_id + "\t" + (mappi_onlyOver.get(i)[6]) + "\t" + xLocation + "\t" + yLocation + "\t" + (mappi_onlyOver.get(i)[3]) + "\t" + zet;
                out_stem3.println(outString);
            }

        }


        System.out.println("POINT CLUOD DONE!");
        return true;
    }

    public boolean processStem(TFloatArrayList points){



        return true;

    }

    public float stemInPolygons(double[] point) {

        double[][] poly;

        for(int f = 0; f < polygons.size(); f++){

            poly = polygons.get(f);

            int numPolyCorners = poly.length;
            int j = numPolyCorners - 1;
            boolean isInside = false;

            for (int i = 0; i < numPolyCorners; i++) {
                if (poly[i][1] < point[1] && poly[j][1] >= point[1] || poly[j][1] < point[1] && poly[i][1] >= point[1]) {
                    if (poly[i][0] + (point[1] - poly[i][1]) / (poly[j][1] - poly[i][1]) * (poly[j][0] - poly[i][0]) < point[0]) {
                        isInside = !isInside;
                    }
                }
                j = i;
            }

            if(isInside){
                return plotIds.get(f).floatValue();
            }
        }
        return -99.9f;
    }

    public boolean pointInPolygons(double[] point) {

        double[][] poly;

        /* If no polygons are input with -poly, then just return false? Is this ok? */
        if(polygons.size() == 0){
            return false;
        }

        for(int f = 0; f < polygons.size(); f++){

            poly = polygons.get(f);

            int numPolyCorners = poly.length;
            int j = numPolyCorners - 1;
            boolean isInside = false;

            for (int i = 0; i < numPolyCorners; i++) {
                if (poly[i][1] < point[1] && poly[j][1] >= point[1] || poly[j][1] < point[1] && poly[i][1] >= point[1]) {
                    if (poly[i][0] + (point[1] - poly[i][1]) / (poly[j][1] - poly[i][1]) * (poly[j][0] - poly[i][0]) < point[0]) {
                        isInside = !isInside;
                    }
                }
                j = i;
            }

            if(isInside){
                this.plotId = plotIds.get(f);
                this.plotIdIndex = f;
                this.plots.add((int)this.plotId);
                return true;
            }
        }
        return false;
    }

    public double mean(double[] in){

        double mean = 0.0;
        for(double i : in)
            mean += i;

        return mean / (double)in.length;

    }

    public double normalize(double[] in){

        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;

        for(double i : in){

            if(i > max)
                max = i;
            if(i < min)
                min = i;

        }

        for(int i = 0; i < in.length; i++){

            in[i] = (in[i] - min) / (max - min);

        }

        return max;

    }

    public boolean isIsolated(double x, double y, double distance){

        double[] thispoint = new double[]{x,y};

        for(int i = 0; i < treeBank.length; i++){

            if(i != this.treeTopIndex && pointInCircle(thispoint, new double[]{treeBank[i][0], treeBank[i][1]}, distance))
                return false;

        }

        return true;
    }

    public static boolean pointInCircle(double[] point, double[] plotCenter,double radi){

        return Math.sqrt(Math.pow(Math.abs(point[1] - plotCenter[1]), 2.0) + Math.pow(Math.abs(point[0] - plotCenter[0]), 2.0)) <= radi;

    }

    public double kurtosis(double[] in){

        Kurtosis kurt = new Kurtosis();

        return kurt.evaluate(in, 0, in.length);
    }

    public double skewness(double[] in){

        Skewness skew = new Skewness();

        return skew.evaluate(in, 0, in.length);

    }

    public void printOutput(){
/*
        for (Iterator<String> it1 = output.iterator(); it1.hasNext();){

            String[] s = it1.next().split("\t");

            //if (Integer.parseInt(s[s.length - 6]) == -1){
               // it1.remove();
            //}
        }
*/
        int counter = 0;
        int end = output.size();

        for(String i : output) {
            printLine(i);

            if(counter % 10 == 0){
                System.out.println(counter + " / " + end);
            }
            counter++;
        }

        output.clear();
        //labelTree.clear();

        //for(int i = 0; i < treeBank.length; i++){

        //	labelTree.add(new double[]{Double.POSITIVE_INFINITY,0,0,0});

        //}

    }

    public int[] makeMeshFaces(int[][] in){

        int[] output = new int[in.length * 3 * 2];

        int count = 0;

        for(int i = 0; i < in.length; i++){

            output[count] = in[i][0];
            count++;
            output[count] = 0;
            count++;
            output[count] = in[i][1];
            count++;
            output[count] = 0;
            count++;
            output[count] = in[i][2];
            count++;
            output[count] = 0;
            count++;
        }

        return output;

    }

    public static float[] convertDoublesToFloats(double[] input){

        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = (float)input[i];
        }
        return output;
    }



    public double[] calcVolume3d(quickhull3d.QuickHull3D hull){

        double[] output = new double[]{0.0, 0.0};

        double[] centroid = centroid(hull);

        double volume = 0.0;

        double area = 0.0;

        double H = 0.0;

        double hypotenuse = 0.0;
        double side = 0.0;

        quickhull3d.Point3d[] point3d = hull.getVertices();

        int[][] faces = hull.getFaces();

        //System.out.println(faces.length + " " + point3d.length);

        for(int i = 0; i < faces.length; i++){

            area = triangleArea(point3d[faces[i][0]].x, point3d[faces[i][0]].y, point3d[faces[i][1]].x, point3d[faces[i][1]].y, point3d[faces[i][2]].x, point3d[faces[i][2]].y);

            hypotenuse = distance(centroid[0], centroid[1], centroid[2], point3d[faces[i][0]].x, point3d[faces[i][0]].y, point3d[faces[i][0]].z);
            side = distance(centroid[0], centroid[1], point3d[faces[i][0]].x, point3d[faces[i][0]].y);

            H = Math.sqrt(Math.pow(hypotenuse, 2) - Math.pow(side, 2));

            output[0] += ((1.0 / 3.0) * area * H);
            output[1] += area;

        }

        for(int i = 0; i < point3d.length; i++){
            point3d[i] = null;
        }
        point3d = null;
        faces = null;
        point3d = null;
        return output;
    }

    public double triangleArea(double x1, double y1, double x2, double y2, double x3, double y3){


        return Math.abs( (x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2)) / 2.0 );

    }

    public double distance(quickhull3d.Point3d p1, quickhull3d.Point3d p2){

        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2) + Math.pow(p1.z - p2.z, 2) );

    }

    public double distance(double[] p1, double[] p2){

        return Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1] - p2[1], 2) + Math.pow(p1[2] - p2[2], 2) );

    }

    public double distance(double x1, double y1, double z1, double x2, double y2, double z2){

        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) + Math.pow(z1 - z2, 2) );

    }

    public double distance(double x1, double y1, double x2, double y2){

        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));

    }


    public double[] centroid(quickhull3d.QuickHull3D hull){

        double[] output = new double[]{0.0, 0.0, 0.0};

        quickhull3d.Point3d[] point3d = hull.getVertices();

        for(int i = 0; i < point3d.length; i++){

            output[0] += point3d[i].x;
            output[1] += point3d[i].y;
            output[2] += point3d[i].z;

        }

        output[0] = output[0] / (double)point3d.length;
        output[1] = output[1] / (double)point3d.length;
        output[2] = output[2] / (double)point3d.length;

        for(int i = 0; i < point3d.length; i++){
            point3d[i] = null;
        }
        point3d = null;

        return output;

    }

    public static double euclideanDistance(double x1, double y1, double x2, double y2){


        return Math.sqrt( Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) );

    }

    public void correctLabel(double x, double y, int index){


        double minDistance = Double.POSITIVE_INFINITY;
        double dist = 0.0;

        double distThold = 1.5;

        treeTopIndex = -1;

        double maxHeight = Double.NEGATIVE_INFINITY;

        int lab = -1;


        for(int i = 0; i < treeBank.length; i++){

            dist = euclideanDistance(treeBank[i][0], treeBank[i][1], x, y);
            //System.out.println(treeBank[i][0] + " " + treeBank[i][1]);
            //System.out.println(treeBank[1000][0] + " " + treeBank[1000][1]);

            /* This means that a closer ITD tree was found in the vicinity of ground tree */
            if(dist < labelTree.get(i)[0] && treeBank[i][5] == 1.0){

                correctLabel(labelTree.get(i)[2], labelTree.get(i)[3], (int)labelTree.get(i)[1]);

                minDistance = dist;
                maxHeight = treeBank[i][2];
                lab = (int)treeBank[i][4];
                treeTopIndex = i;

            }

            if(dist < distThold && treeBank[i][2] > maxHeight && treeBank[i][5] == 0.0){ // && treeBank[i][6] == -1.0){

                minDistance = dist;
                maxHeight = treeBank[i][2];
                lab = (int)treeBank[i][4];
                treeTopIndex = i;

            }

        }


        //if(treeTopIndex != -1){

        String[] toModify = output.get(index).split("\t");

        String out = toModify[0];

        for(int i = 1; i < toModify.length; i++){

            out += "\t";

            if(i == toModify.length - 8)
                out += treeTopIndex;
            else if (i == toModify.length - 7)
                out += maxHeight;
            else
                out += toModify[i];

        }

        output.set(index, out);
        //}
        //else
        //System.out.println("GOT HERE!!");



        //return lab;
    }



    public int findLabel(double x, double y){

        double minDistance = Double.POSITIVE_INFINITY;
        double closerOneMinDistance = Double.POSITIVE_INFINITY;
        double dist = 0.0;

        double distThold = 2.0;

        treeTopIndex = -1;

        double maxHeight = Double.NEGATIVE_INFINITY;

        int lab = -1;

        int closerOne = -1;


        /* Tree x*/
        //trees[treeCount][0] = Double.parseDouble(line.split(",")[3]);

        /* Tree y*/
        //trees[treeCount][1] = Double.parseDouble(line.split(",")[4]);

        /* Tree height*/
        //trees[treeCount][2] = Double.parseDouble(line.split(",")[11]);

        /* Tree diameter*/
        //trees[treeCount][3] = Double.parseDouble(line.split(",")[9]);

        /* Tree species*/
        //trees[treeCount][4] = Math.min(Double.parseDouble(line.split(",")[7]), 3.0);

        /* Tree WHAT?*/
        //trees[treeCount][5] = 0.0;

        /* Tree volume*/
        //trees[treeCount][6] = Double.parseDouble(line.split(",")[42]);

        /* Tree plot-id*/
        //trees[treeCount][7] = Double.parseDouble(line.split(",")[2])

        for(int i = 0; i < treeBank.length; i++){

            dist = euclideanDistance(treeBank[i][0], treeBank[i][1], x, y);
            //System.out.println(treeBank[i][0] + " " + treeBank[i][1]);
            //System.out.println(treeBank[1000][0] + " " + treeBank[1000][1]);

            /* This means that a closer ITD tree was found in the vicinity of ground tree */

            if(dist < labelTree.get(i)[0] && treeBank[i][5] == 1.0){

                if(dist < closerOneMinDistance){

                    closerOne = i;
                    closerOneMinDistance = dist;
                    treeTopIndex = -1;
                    //break;
                    //minDistance = dist;
                    //maxHeight = treeBank[i][2];

                }

					/*
					correctLabel(labelTree.get(i)[1], labelTree.get(i)[2], (int)labelTree.get(i)[0]);

					minDistance = dist;
					maxHeight = treeBank[i][2];
					lab = (int)treeBank[i][4];
					treeTopIndex = i;
					*/


            }

            if(dist < distThold && treeBank[i][2] > maxHeight && treeBank[i][5] == 0.0){ // && treeBank[i][6] == -1.0){

                //System.out.println(dist);
                minDistance = dist;
                maxHeight = treeBank[i][2];
                lab = (int)treeBank[i][4];
                treeTopIndex = i;
                treeTopMeasuredHeight = maxHeight;
                //System.out.println(dist);

            }

        }

        if(treeTopIndex != -1){
            //System.out.println(minDistance);
            labelTree.get(treeTopIndex)[0] = (float)minDistance;
            labelTree.get(treeTopIndex)[1] = (float)output.size();

            labelTree.get(treeTopIndex)[2] = (float)x;
            labelTree.get(treeTopIndex)[3] = (float)y;

            treeBank[treeTopIndex][5] = 1.0;

        }
        else if(closerOne != -1){


            correctLabel(labelTree.get(closerOne)[2], labelTree.get(closerOne)[3], (int)labelTree.get(closerOne)[1]);

            minDistance = dist;
            maxHeight = treeBank[closerOne][2];
            lab = (int)treeBank[closerOne][4];
            treeTopIndex = closerOne;
            treeTopMeasuredHeight = maxHeight;

            labelTree.get(closerOne)[0] = (float)minDistance;
            labelTree.get(closerOne)[1] = (float)output.size();

            labelTree.get(closerOne)[2] = (float)x;
            labelTree.get(closerOne)[3] = (float)y;

            treeBank[closerOne][5] = 1.0;

        }

        return lab;
    }

}
