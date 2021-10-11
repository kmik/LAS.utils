package tools;

import LASio.LASReader;
import LASio.LasPoint;
import LASio.LasPointBufferCreator;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import utils.argumentReader;
import utils.pointWriterMultiThread;

import java.io.*;
import java.util.*;

public class classifyTrunks {

    ArrayList<double[][]> polygons;
    ArrayList<Double> plotIds;
    ArrayList<String> plotIdsString = new ArrayList<String>();

    argumentReader aR;
    LASReader pointCloud;
    HashMap<Integer, int[]> trunkMatches;

    HashMap<Integer, double[]> trunkFile = new HashMap<>();
    HashSet<Integer> upperStoreyTrunks;
    HashSet<Integer> underStoreyTrunks;
    HashMap<Integer, Integer> trunk_to_crown = new HashMap<>();


    public classifyTrunks(){}

    public classifyTrunks(LASReader pointCloud, argumentReader aR, HashMap<Integer, double[]> trunkFile,
                          HashSet<Integer> upperStoreyTrunks,
                          HashSet<Integer> underStoreyTrunks,
                          HashMap<Integer, Integer> trunk_to_crown) throws Exception{

        this.pointCloud = pointCloud;
        this.aR = aR;

        this.trunk_to_crown = trunk_to_crown;

        this.upperStoreyTrunks = upperStoreyTrunks;
        this.underStoreyTrunks = underStoreyTrunks;


        //this.readPolygon(new File(aR.poly));
        this.trunkFile = trunkFile;

        FileInputStream fis = new FileInputStream("trunks.ser");
        ObjectInputStream ois = new ObjectInputStream(fis);
        //noinspection unchecked
        this.trunkMatches = (HashMap<Integer, int[]>) ois.readObject();
        ois.close();

    }



    public void classify() throws Exception{

        System.out.println("HERE");

        File o_file = aR.createOutputFileWithExtension(pointCloud, "_trunkClas.las");


        int thread_n = aR.pfac.addReadThread(pointCloud);

        LasPoint tempPoint = new LasPoint();

        HashMap<Integer, TreeMap<Double, Integer>> itc_trunks = new HashMap<>();

        HashMap<Integer, TreeMap<Integer, Integer>> trunk_crown_match = new HashMap<>();

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));
            //in.readRecord_noRAF(i, tempPoint, 10000);
            aR.pfac.prepareBuffer(thread_n, i, 10000);

            //pointCloud.braf.buffer.position(0);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if(tempPoint.classification == 4){

                    if(trunk_crown_match.containsKey(tempPoint.intensity)){

                        if(trunk_crown_match.get(tempPoint.intensity).containsKey((int)tempPoint.pointSourceId)){
                            trunk_crown_match.get(tempPoint.intensity).put((int)tempPoint.pointSourceId, trunk_crown_match.get(tempPoint.intensity).get((int)tempPoint.pointSourceId)+1);
                        }else{
                            trunk_crown_match.get(tempPoint.intensity).put((int)tempPoint.pointSourceId, 1);
                        }
                    }else{
                        trunk_crown_match.put(tempPoint.intensity, new TreeMap<>());
                        trunk_crown_match.get(tempPoint.intensity).put((int)tempPoint.pointSourceId, 1);
                    }

                }
            }
        }



        for(int key : trunk_crown_match.keySet()){

            System.out.println("trunk " + key + " size: " + trunk_crown_match.get(key).size());
            int matchCrown = 0;
            int maxPoints = 0;

            for(int key2 : trunk_crown_match.get(key).keySet()){

                System.out.println(key2 + " n_: " + trunk_crown_match.get(key).get(key2));
                if(trunk_crown_match.get(key).get(key2) > maxPoints){
                    matchCrown = key2;
                    maxPoints = trunk_crown_match.get(key).get(key2);
                }


            }
            System.out.println("Match crown = " + matchCrown);
            System.out.println(Arrays.toString(trunkFile.get(key))) ;
            trunk_to_crown.put(key, matchCrown);

            if(itc_trunks.containsKey(matchCrown)){

                itc_trunks.get(matchCrown).put(trunkFile.get(key)[0], key);
            }else{

                itc_trunks.put(matchCrown, new TreeMap<>());
                itc_trunks.get(matchCrown).put(trunkFile.get(key)[0], key);

            }
        }

        //HashSet<Integer> upperStoreyTrunks = new HashSet<>();
        //HashSet<Integer> underStoreyTrunks = new HashSet<>();
        int total = 0;



        for(int key : itc_trunks.keySet()){

            for(double key2 : itc_trunks.get(key).keySet()){
                if(key2 != itc_trunks.get(key).lastKey()){
                    underStoreyTrunks.add(itc_trunks.get(key).get(key2 ));
                }
            }
            upperStoreyTrunks.add(itc_trunks.get(key).get(itc_trunks.get(key).lastKey() ));
            //System.out.println(itc_trunks.get(key).get(itc_trunks.get(key).lastKey() ));
            total += itc_trunks.get(key).size();
        }

        System.out.println("upper: " + upperStoreyTrunks.size() + " under: " + underStoreyTrunks.size() +  " total: " + total);

        //BufferedWriter bw = new BufferedWriter(new FileWriter(o_trunk_file, true));



        //System.exit(1);

        if(false)
        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));
            //in.readRecord_noRAF(i, tempPoint, 10000);
            aR.pfac.prepareBuffer(thread_n, i, 10000);

            //pointCloud.braf.buffer.position(0);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                /** Means we have a crown segment */
                if(tempPoint.pointSourceId > 0){

                    /** Means this is a trunk point */
                    if(tempPoint.classification == 4){

                        if(trunkMatches.containsKey(tempPoint.intensity)) {
                            if (itc_trunks.containsKey((int)tempPoint.pointSourceId)) {


                                if(!itc_trunks.get((int) tempPoint.pointSourceId).containsKey(trunkMatches.get(tempPoint.intensity)[2])) {

                                    System.out.println(tempPoint.gpsTime + " == " + trunkMatches.get(tempPoint.intensity)[2]);

                                    //itc_trunks.get((int) tempPoint.pointSourceId).put(trunkMatches.get(tempPoint.intensity)[2], tempPoint.intensity);
                                }

                            } else {

                                itc_trunks.put((int) tempPoint.pointSourceId, new TreeMap<>());
                                //itc_trunks.get((int) tempPoint.pointSourceId).put(trunkMatches.get(tempPoint.intensity)[2], tempPoint.intensity);
                            }
                        }
                    }
                }
            }
        }



        pointWriterMultiThread pw = new pointWriterMultiThread(o_file, pointCloud, "las2las", aR);

        LasPointBufferCreator buf = new LasPointBufferCreator(pointCloud.pointDataRecordLength, 1, pw);

        aR.pfac.addWriteThread(thread_n, pw, buf);

        for(int i = 0; i < pointCloud.getNumberOfPointRecords(); i += 10000) {

            int maxi = (int) Math.min(10000, Math.abs(pointCloud.getNumberOfPointRecords() - i));
            //in.readRecord_noRAF(i, tempPoint, 10000);
            aR.pfac.prepareBuffer(thread_n, i, 10000);

            //pointCloud.braf.buffer.position(0);

            for (int j = 0; j < maxi; j++) {

                pointCloud.readFromBuffer(tempPoint);

                if(tempPoint.classification == 4)
                    if(upperStoreyTrunks.contains(tempPoint.intensity))
                        tempPoint.classification = 1;
                    else
                        tempPoint.classification = 5;

                try {
                    aR.pfac.writePoint(tempPoint, i + j, thread_n);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        aR.pfac.closeThread(thread_n);
    }


    public void readPolygon(File plots) throws IOException {

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

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
            String id = "";

            if(tempF.GetFieldCount() > 0)
                id = tempF.GetFieldAsString(0);
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

    public void setPolygon(ArrayList<double[][]> in, ArrayList<Double> plotIds1){

        this.polygons = in;
        this.plotIds = plotIds1;

        for(int i = 0; i < plotIds.size(); i++){

            plotIdsString.add(String.valueOf(plotIds.get(i)));

        }

    }

    public boolean pointInPolygons(double[] point, double[] plotId) {

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
                plotId[0] = this.plotIds.get(f);
                return true;
            }
        }
        return false;
    }

}
