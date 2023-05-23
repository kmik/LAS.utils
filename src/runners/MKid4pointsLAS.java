package runners;

import java.io.*;
import java.util.*;


import java.awt.geom.Path2D;

import java.util.concurrent.BlockingQueue;

import LASio.*;
import err.toolException;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TIntHashSet;
import tools.*;

import utils.*;
public class MKid4pointsLAS{

    public static boolean isInside(double[] point, double[] extent, double buffer){

        return point[0] > (extent[0] + buffer) && point[0] < (extent[2] - buffer)
                && point[1] > (extent[1] + buffer) && point[1] < (extent[3] - buffer);

    }


    public static double[] getExtent(String file, String indexAll){

        String delim = " ";
        double[] output = new double[4];
        String pathSep = System.getProperty("file.separator");
        String line;
        try {
            BufferedReader sc = new BufferedReader( new FileReader(indexAll));

            while((line = sc.readLine())!= null){
                String[]tokens = line.split(delim);
                if(file.split(pathSep)[file.split(pathSep).length - 1].equals(tokens[0])){

                    output[0] = Double.parseDouble(tokens[1]);
                    output[1] = Double.parseDouble(tokens[2]);
                    output[2] = Double.parseDouble(tokens[3]);
                    output[3] = Double.parseDouble(tokens[4]);

                }
            }} catch (Exception e) {
            e.printStackTrace();
        }

        return output;

    }


    public static ArrayList<String> listFiles(String directory, String endsWithh){

        ArrayList<String> output = new ArrayList<String>();


        File[] files3 = new File(directory).listFiles();
        Arrays.sort(files3);
        int a3 = 0;
        for(File file : files3){   //READ THE POINT CLOUD FILEPATHS
            if(file.isFile()){
                if(file.getName().endsWith((endsWithh))){

                    output.add(a3,file.getAbsolutePath());
                    a3++;
                }
            }
        }



        return output;
    }

    public static void hilaJako(ArrayList<String> pilvi,String all_index, double[] topLeft, double cellSize,
                                String outtieName, int partionSize, int part, int cores, double buffer, String oparse, String output){

        //RunId4pointsLAS.output.resetThreads();

        String delim = " ";
        ArrayList<String> pilvi1;
        int pienin = 0;
        //System.out.println(pilvi);
        int suurin = pilvi.size();

        double[] extent = new double[4];
        double[] point = new double[2];

        ArrayList<Double> all_min_y = new ArrayList<Double>();
        ArrayList<Double> all_max_y = new ArrayList<Double>();
        ArrayList<Double> all_min_x = new ArrayList<Double>();
        ArrayList<Double> all_max_x = new ArrayList<Double>();
        ArrayList<String> all_filename = new ArrayList<String>();

        Cantor homma = new Cantor();

        if(part != 0){

            int jako = Math.round(pilvi.size()/cores);
            if(part != cores){
                pienin = (part - 1) * jako;

                suurin = part * jako;
            }

            else{
                pienin = (part-1) * jako;
                suurin = pilvi.size();
            }

            pilvi1 = new ArrayList<String>(pilvi.subList(pienin, suurin));
        }
        else{
            pilvi1 = pilvi;
        }






        //System.out.println(pilvi1.size());

        File temp6 = new File(outtieName);

        if(temp6.exists())
            temp6.delete();

        double mapLeaf = cellSize * partionSize;
        LasPoint tempPoint = new LasPoint();

        for(int i = 0; i < pilvi1.size(); i++){

            try {
                if(buffer > 0.0)
                    extent = getExtent(pilvi1.get(i), all_index);
                //System.out.println(Arrays.toString(extent));
                //System.out.println(pilvi1.get(i));
                FileWriter fw = new FileWriter(outtieName, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter outt = new PrintWriter(bw);

                BufferedReader sc = new BufferedReader( new FileReader(pilvi1.get(i)));

                File file1 = new File(pilvi1.get(i));
                LASReader asd = new LASReader(file1);
                long n = asd.getNumberOfPointRecords();



                long ex = 0;
                long ey = 0;

                long mapx = 0;
                long mapy = 0;
                //System.out.println(topLeft[0]);
                String line1 = "";
                String combined;
                String combined2;
                for(long ii = 0; ii < n; ii++){
                    asd.readRecord(ii, tempPoint);
                    String[] tokens = line1.split(" ");
                    point[0] = tempPoint.x;
                    point[1] = tempPoint.y;
                    //System.out.println(tokens[0]);
                    if( (point[0] - topLeft[0]) > 0 && ( topLeft[1] - point[1] > 0 &&
                            (buffer == 0.0 || isInside(point,extent,buffer) ) )){

                        ex = (long)Math.ceil((Math.abs( point[0] - topLeft[0])) / cellSize);
                        ey = (long)Math.ceil((Math.abs( point[1] - topLeft[1])) / cellSize);

                        mapx = (long)Math.ceil((Math.abs( point[0] - topLeft[0])) / mapLeaf);
                        mapy = (long)Math.ceil((Math.abs( point[1] - topLeft[1])) / mapLeaf);

                        long[] temp = new long[2];
                        temp[0] = ex;
                        temp[1] = ey;

                        long[] temp2 = new long[2];
                        temp2[0] = mapx;
                        temp2[1] = mapy;

                        long code1 = Cantor.pair(temp[0], temp[1]);
                        long code2 = Cantor.pair(temp2[0], temp2[1]);

                        String tempString = " " + LASwrite.LASpoint2String(tempPoint, oparse, "\t");
                        outt.println(code2 + tempString + " " + code1);

                    }
                }

                sc.close();
                outt.close();
            } catch( IOException ioException ) {
                ioException.printStackTrace();
            }

        }
    }

    public static boolean pointInCircle(double[] point, double[] plotCenter,double radi){

        return Math.sqrt(Math.pow(Math.abs(point[1] - plotCenter[1]), 2.0) + Math.pow(Math.abs(point[0] - plotCenter[0]), 2.0)) <= radi;

    }

    public static boolean pointInPolygon(double[] point, double[][] poly) {


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
        return isInside;
    }

    public static boolean pointInPolygon(double[] point, double[][] poly, HashMap<Integer, ArrayList<double[][]>> holes, int poly_id) {

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

        if(holes.containsKey(poly_id)){

            ArrayList<double[][]> holet = holes.get(poly_id);
            double[][] polyHole = null;

            for(int i_ = 0; i_ < holet.size(); i_++){

                numPolyCorners = holet.get(i_).length;

                //System.out.println(holet.get(i_)[0][0] + " == " + holet.get(i_)[holet.get(i_).length-1][0]);
                //System.out.println(holet.get(i_)[0][1] + " == " + holet.get(i_)[holet.get(i_).length-1][1]);
                j = numPolyCorners - 1;
                boolean is_inside_hole = false;
                polyHole = holet.get(i_);

                for (int i = 0; i < numPolyCorners; i++) {

                    //System.out.println(polyHole[i][0] + " " + polyHole[i][1]);

                    if (polyHole[i][1] < point[1] && polyHole[j][1] >= point[1] || polyHole[j][1] < point[1] && polyHole[i][1] >= point[1]) {
                        if (polyHole[i][0] + (point[1] - polyHole[i][1]) / (polyHole[j][1] - polyHole[i][1]) * (polyHole[j][0] - polyHole[i][0]) < point[0]) {
                            is_inside_hole = !is_inside_hole;
                        }
                    }
                    j = i;
                }

                //System.out.println("-------------------------");
                if(is_inside_hole) {

                    //System.out.println("YES YES YES!");
                    return false;
                }
            }
        }

        return isInside;
    }


    public static ArrayList<double[][]> readPolygonsFromWKT(String fileName, ArrayList<Integer> plotID1) throws Exception{

        ArrayList<double[][]> output = new ArrayList<>();
        String line1;
        int id_number = 1;

        try{
            BufferedReader sc = new BufferedReader( new FileReader(new File(fileName)));
            sc.readLine();
            while((line1 = sc.readLine())!= null){

                String[] tokens =  line1.split(",");
                String[] tokens2 =  line1.split("\\),\\(");
                //System.out.println(Arrays.toString(tokens));
                //if(tokens[tokens.length - 1] != -999){
                boolean holes = false;

                if(tokens2.length > 1){
                    holes = true;
                }

                if(holes) {
                    tokens = line1.split(",\\(");
                    tokens[0] += ")";
                    tokens = tokens[0].split(",");
                    //System.out.println(tokens[0]);
                    //System.exit(1);
                }

                String[] iidee_ = line1.split("\",");
                String id = iidee_[iidee_.length-1];

                //System.out.println(Integer.parseInt(tokens[tokens.length - 1]) + " " + tokens2.length);

                    try {
                        //plotID1.add(Integer.parseInt(tokens[tokens.length - 1]));
                        plotID1.add(Integer.parseInt(id));

                    }catch (NumberFormatException e){

                        //Not directly comvertable to a number

                        String str = id.replaceAll("\\D+","");

                        try {
                            plotID1.add(Integer.parseInt(str));
                        }catch (Exception ee){

                            //Still no numbers, let's make our own then I guess

                            plotID1.add(id_number++);

                        }

                    }

                    //System.out.println(plotID1.get(plotID1.size()-1));


                    double[][] tempPoly = new double[tokens.length - 1][2];
                    int n = (tokens.length) - 2;
                    int counteri = 0;

                    tempPoly[counteri][0] = Double.parseDouble(tokens[0].split("\\(\\(")[1].split(" ")[0]);
                    tempPoly[counteri][1] = Double.parseDouble(tokens[0].split("\\(\\(")[1].split(" ")[1]);

                    counteri++;

                    boolean breikki = false;

                    for(int i = 1; i < n; i++){

                        if(tokens[i].split(" ")[0].contains(")") || tokens[i].split(" ")[1].contains(")")){
                            plotID1.remove(plotID1.size() - 1);
                            breikki = true;
                            break;
                        }

                        tempPoly[counteri][0] = Double.parseDouble(tokens[i].split(" ")[0]);
                        tempPoly[counteri][1] = Double.parseDouble(tokens[i].split(" ")[1]);
                        counteri++;
                    }

                    //System.out.println(Arrays.toString(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")));
                    tempPoly[counteri][0] = Double.parseDouble(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")[0]);
                    tempPoly[counteri][1] = Double.parseDouble(tokens[tokens.length - 2].split("\\)\\)")[0].split(" ")[1]);

                    if(!breikki)
                        output.add(tempPoly);
                //}

            }


        }catch( IOException ioException ) {
            //ioException.printStackTrace();
        }

        return output;
    }

    public static HashMap<Integer, ArrayList<double[][]>> readPolygonHolesFromWKT(String fileName, ArrayList<Integer> plotID1) throws Exception{

        HashMap<Integer, ArrayList<double[][]>> output = new HashMap<Integer, ArrayList<double[][]>>();

        String line1;
        int id_number = 1;

        try{
            BufferedReader sc = new BufferedReader( new FileReader(new File(fileName)));
            sc.readLine();
            while((line1 = sc.readLine())!= null){

                String[] tokens =  line1.split(",");
                String[] tokens2 =  line1.split("\\),\\(");

                if(tokens2.length <= 1){
                    continue;
                }

                int plot_id = -1;

                try {
                    plot_id = Integer.parseInt(tokens[tokens.length - 1]);
                }catch (NumberFormatException e){

                    //Not directly comvertable to a number

                    String str = tokens[tokens.length - 1].replaceAll("\\D+","");

                    try {
                        plot_id = Integer.parseInt(str);
                    }catch (Exception ee){

                        //Still no numbers, let's make our own then I guess
                        plot_id = id_number++;

                    }

                }

                //System.out.println(tokens2[0]);

                String string_here = null;

                output.put(plot_id, new ArrayList<>());


                for(int i = 1; i < tokens2.length; i++){

                    string_here = tokens2[i];

                    if(i == tokens2.length-1){

                        String[] tokens_here = string_here.split("\\)\\)");
                        //String[] coords = tokens_here[0].split()
                        //System.out.println(tokens_here[0]);
                        string_here = tokens_here[0];

                    }else{
                        string_here = tokens2[i];
                    }

                    String[] toks = string_here.split(",");

                    double[][] tempPoly = new double[toks.length][2];
                    int n = toks.length;
                    int counteri = 0;

                    for(int i_ = 0; i_ < n; i_++){

                        tempPoly[counteri][0] = Double.parseDouble(toks[i_].split(" ")[0]);
                        tempPoly[counteri][1] = Double.parseDouble(toks[i_].split(" ")[1]);
                        counteri++;
                    }

                    output.get(plot_id).add(tempPoly);
                }

            }


        }catch( IOException ioException ) {
            //ioException.printStackTrace();
        }

        return output;
    }

    // Returns true if two rectangles (l1, r1) and (l2, r2) overlap
    boolean doOverlap(Point l1, Point r1, Point l2, Point r2)
    {
        // If one rectangle is on left side of other
        if (l1.x > r2.x || l2.x > r1.x)
            return false;

        // If one rectangle is above other
        if (l1.y < r2.y || l2.y < r1.y)
            return false;

        return true;
    }

    public void clipPlots(String coords, int shapeType, ArrayList<String> pilvi,String outieFile,
                                 ArrayList<String> indeksitiedosto, String delim, String outShape, int part, int cores, boolean statCalc, String output_txt_points,
                                 String all_index, double buffer, String plotOutName, String oparse, String output, boolean split, String otype,
                                 RunId4pointsLAS.FileOutput foutti, BlockingQueue<String> queue, BlockingQueue<LasPoint> queueLAS, argumentReader aR, BlockingQueue<byte[]> que,
                          LASraf mergeOutput, ArrayList<BlockingQueue<byte[]>> qu, BlockingQueue<Integer> threadWrote, pointWriterMultiThread pwrite,
                          ArrayList<pointWriterMultiThread> outputFiles)throws Exception{


        boolean clip_out = true;

        pointCloudMetrics pCM = new pointCloudMetrics(aR);

        double sum_z_a = 0.0;
        double sum_i_a = 0.0;
        double sum_z_f = 0.0;
        double sum_i_f = 0.0;
        double sum_z_l = 0.0;
        double sum_i_l = 0.0;
        double sum_z_i = 0.0;
        double sum_i_i = 0.0;

        ArrayList<String> colnames_a = new ArrayList<>(), colnames_f = new ArrayList<>(), colnames_l = new ArrayList<>(), colnames_i = new ArrayList<>();

        ArrayList<String> colnames_convo = new ArrayList<>();

        ArrayList<Double> gridPoints_z_a = new ArrayList<>();
        ArrayList<Integer> gridPoints_i_a = new ArrayList<>();

        ArrayList<Double> gridPoints_z_f = new ArrayList<>();
        ArrayList<Integer> gridPoints_i_f = new ArrayList<>();
        ArrayList<int[]> gridPoints_RGB_f = new ArrayList<>();

        ArrayList<Double> gridPoints_z_l = new ArrayList<>();
        ArrayList<Integer> gridPoints_i_l = new ArrayList<>();

        ArrayList<Double> gridPoints_z_i = new ArrayList<>();
        ArrayList<Integer> gridPoints_i_i = new ArrayList<>();

        ArrayList<double[]> gridPoints_xyz_a = new ArrayList<>();
        ArrayList<double[]> gridPoints_xyz_f = new ArrayList<>();
        ArrayList<double[]> gridPoints_xyz_l = new ArrayList<>();

        aR.p_update.updateProgressClip();

        ArrayList<LASReader> pointClouds = aR.pointClouds;

        LASReader templateCloud = new LASReader(aR.inputFiles.get(0));

        ArrayList<LasPointBufferCreator> outputBuffers = new ArrayList<>();

        LasPointBufferCreator pointBuffer = null;

        if(aR.split){
            for(int i = 0; i < outputFiles.size(); i++)
                outputBuffers.add(new LasPointBufferCreator(part, outputFiles.get(i)));
        }
        else
            pointBuffer = new LasPointBufferCreator(part, pwrite);

        File tietue = new File("output" + System.getProperty("file.separator") + plotOutName);

        if(tietue.exists())
            tietue.delete();

        ArrayList<String> pisteet = new ArrayList<String>();   //TULOSTEPISTELISTA

        ArrayList<Double> coords1_x1 = new ArrayList<Double>();   //TULOSTEPISTELISTA
        ArrayList<Double> coords1_y1 = new ArrayList<Double>();   //TULOSTEPISTELISTA
        ArrayList<Double> plot_radi = new ArrayList<Double>();   //TULOSTEPISTELISTA
        ArrayList<Integer> plotID1 = new ArrayList<Integer>();   //TULOSTEPISTELISTA

        ArrayList<Double> all_min_y = new ArrayList<Double>();
        ArrayList<Double> all_max_y = new ArrayList<Double>();
        ArrayList<Double> all_min_x = new ArrayList<Double>();
        ArrayList<Double> all_max_x = new ArrayList<Double>();

        ArrayList<String> all_filename = new ArrayList<String>();
        String all_index_line;

        File tiedostoIndex = new File(all_index);
        tiedostoIndex.setReadable(true);
        BufferedReader sc1 = new BufferedReader( new FileReader(tiedostoIndex));   //INDEKSITIEDOSTON LUKEMINEN

        while((all_index_line = sc1.readLine())!= null){

            String[]tokens_i2=all_index_line.split(delim);

            if(tokens_i2.length > 3){

                all_filename.add(tokens_i2[0]);
                all_min_x.add(Double.parseDouble(tokens_i2[1]));
                all_min_y.add(Double.parseDouble(tokens_i2[3]));
                all_max_x.add(Double.parseDouble(tokens_i2[2]));
                all_max_y.add(Double.parseDouble(tokens_i2[4]));
            }

            else{

                all_filename.add(" ");
                all_min_x.add(-1.0);
                all_min_y.add(-1.0);
                all_max_x.add(-1.0);
                all_max_y.add(-1.0);
            }
        }
        sc1.close();

        ArrayList <String> indeksitiedosto1=indeksitiedosto;

        String line1;

        Cantor homma = new Cantor();

        ArrayList<Double> z = new ArrayList<>();
        ArrayList<Double> intensity = new ArrayList<>();


        /* Circular plot */
        /* THIS IS NOT USED ANYWHERE!!! */
        if(shapeType == 1){

            ArrayList<Integer> plotID;
            ArrayList<Double> coords1_x;
            ArrayList<Double> coords1_y;
            ArrayList<Double> plot_radi1;
            String tiedosto_coord = coords;
            File tiedostoCoord = new File(tiedosto_coord);
            tiedostoCoord.setReadable(true);

            try{
                BufferedReader sc = new BufferedReader( new FileReader(tiedostoCoord));

                while((line1 = sc.readLine())!= null){
                    //System.out.println(line1);
                    if(!line1.equals("")){
                        String[] tokens = line1.split(delim);
                        plotID1.add(Integer.parseInt(tokens[0]));
                        coords1_x1.add(Double.parseDouble(tokens[1]));
                        coords1_y1.add(Double.parseDouble(tokens[2]));
                        plot_radi.add(Double.parseDouble(tokens[3]));
                    }
                }


            }catch( IOException ioException ) {
                ioException.printStackTrace();
            }
            //RunId4pointsLAS.proge.setEnd(plotID1.size());
            int pienin;
            int suurin;

            if(part != 0){

                int jako = (int)Math.ceil(plotID1.size() / (double) cores);

                if(part != cores){

                    pienin = (part - 1) * jako;
                    suurin = part * jako;
                }

                else{
                    pienin = (part-1) * jako;
                    suurin = plotID1.size();
                }

                plotID = new ArrayList<Integer>(plotID1.subList(pienin, suurin));
                coords1_x = new ArrayList<Double>(coords1_x1.subList(pienin, suurin));
                coords1_y = new ArrayList<Double>(coords1_y1.subList(pienin, suurin));
                plot_radi1 = new ArrayList<Double>(plot_radi.subList(pienin, suurin));

            }
            else{

                plotID = new ArrayList<Integer>(plotID1);
                coords1_x = new ArrayList<Double>(coords1_x1);
                coords1_y = new ArrayList<Double>(coords1_y1);
                plot_radi1 = new ArrayList<Double>(plot_radi);
            }



            int kountti = coords1_y.size();

            String[] tokens;
            long mini1=0;   // Rivi josta luettavat pisteet alkaa
            long maxi1=0;    // Rivi johon luku paattyy

            int g = 0;
            int i=0;
            double[] plotC = new double[2];
            double[] haku = new double[2];

            int prev_i = 0;


            int npoints = 0;


            //// LOOPPI ALKAA ---------------------------------------------------------------------------------------------------------
            ArrayList<Integer> plot_id_poisto = new ArrayList<>();

            aR.p_update.threadEnd[part-1] = kountti;
            aR.p_update.threadProgress[part-1] = 0;

            aR.p_update.updateProgressClip();

            for(int j = 0; j < kountti; j++){

                aR.p_update.threadFile[part-1] = plotID.get(j).toString();

                HashSet<String> pisteMappi = new HashSet<String>();
                //System.out.println(plotID.get(j));
                plotC[0] = coords1_x.get(j);
                plotC[1] = coords1_y.get(j);

                double[] minmaxXY = new double[4];

                minmaxXY[0] = plotC[0] - plot_radi1.get(j);
                minmaxXY[1] = plotC[0] + plot_radi1.get(j);

                minmaxXY[2] = plotC[1] - plot_radi1.get(j);
                minmaxXY[3] = plotC[1] + plot_radi1.get(j);

                ArrayList<Integer> valinta = new ArrayList<Integer>();
                double dimension = plot_radi1.get(j);

                for(int th = 0; th < all_filename.size(); th++){

                    if(buffer == 0.0){

                        if(plotC[0] >= all_min_x.get(th) - dimension && plotC[0] <= all_max_x.get(th) + dimension && plotC[1] >= all_min_y.get(th) - dimension && plotC[1] <= all_max_y.get(th) + dimension ){

                            valinta.add(th);

                        }
                    }
                    else{

                        if(plotC[0] >= (all_min_x.get(th) + buffer) && plotC[0] <= (all_max_x.get(th) - buffer) && plotC[1] >= (all_min_y.get(th) + buffer) && plotC[1] <= (all_max_y.get(th) - buffer) ){

                            valinta.add(th);
                            //break;
                        }

                    }

                }
                if(valinta.size() == 0){

                }
                npoints = 0;

                for(int va = 0; va < valinta.size(); va++){
                    //System.out.println("Valinta: " + valinta.size());

                    File file1 = new File(all_filename.get(valinta.get(va)));
                    LASReader asd = new LASReader(file1);

                    ArrayList<Long> nelio = new ArrayList<Long>();


                    //int first = 0;
                    mini1 = 0;
                    maxi1 = 0;
                    g = 0;
                    long n1 = 0;
                    long n2 = 0;
                    long n3 = 0;
                    long n4 = 0;
                    HashMap<Integer, ArrayList<Long>> temp = null;
                    //System.out.println(indeksitiedosto.get(valinta.get(va)));
                    TLongArrayList diipa = new TLongArrayList();

                    File tiedosto = new File(all_filename.get(valinta.get(va)).split("\\.las")[0] + ".lasx");

                    if(tiedosto.exists()){ //if(indeksitiedosto.size() > 0){

                        try {

                            FileInputStream fileIn = new FileInputStream(tiedosto);
                            ObjectInputStream in2 = new ObjectInputStream(fileIn);
                            temp = (HashMap<Integer, ArrayList<Long>>) in2.readObject();
                            in2.close();
                            fileIn.close();
                        }catch(ClassNotFoundException c) {
                            c.printStackTrace();
                        }

                        asd.setIndexMap(temp);

                        asd.query(minmaxXY[0], minmaxXY[1], minmaxXY[2], minmaxXY[3]);

                        diipa = asd.queriedIndexes;
                        /*

                        try {

                            File tiedosto = new File(all_filename.get(valinta.get(va)).split("\\.las")[0] + ".lasx");

                            //System.out.println(tiedosto);
                            try {

                                FileInputStream fileIn = new FileInputStream(tiedosto);
                                ObjectInputStream in2 = new ObjectInputStream(fileIn);
                                temp = (HashMap<Integer, ArrayList<Long>>) in2.readObject();
                                in2.close();
                                fileIn.close();
                            }catch(IOException e) {
                                e.printStackTrace();

                            }catch(ClassNotFoundException c) {
                                c.printStackTrace();

                            }

                        } catch( Exception ioException ) {}

                        int koko = nelio.size();

                        long spacing = temp.get(-1L).get(4);


                        long[] extentti = new long[4];
                        extentti[0] = (long)Math.floor( (minmaxXY[0] - temp.get(-1L).get(0)) / spacing);
                        extentti[1] = (long)Math.floor( (minmaxXY[1] - temp.get(-1L).get(0)) / spacing);

                        extentti[2] = (long)Math.floor( (temp.get(-1L).get(3) - minmaxXY[2]) / spacing);
                        extentti[3] = (long)Math.floor( (temp.get(-1L).get(3) - minmaxXY[3]) / spacing);

                        n1 = Math.min(extentti[0], extentti[1]);
                        n2 = Math.max(extentti[0], extentti[1]);
                        n3 = Math.min(extentti[2], extentti[3]);
                        n4 = Math.max(extentti[2], extentti[3]);

                        for(long k = n1; k <= n2; k++)
                            for(long f = n3; f <= n4; f++){
                                long[] temp99 = new long[2];
                                temp99[0] = k;
                                temp99[1] = f;
                                diipa.addAll(temp.get(homma.pair(temp99[0], temp99[1])));
                            }

                         */
                    }else{

                        diipa.add(0L);
                    }

                    LasPoint tempPoint = new LasPoint();

                    try {
                        FileWriter fw = null;

                        if(indeksitiedosto.size() < pilvi.size())
                            diipa.add(asd.getNumberOfPointRecords());

                        for(int u = 0; u < diipa.size() - 1; u += 2){  //&& g <= maxi1
                            for(long p = diipa.get(u); p < diipa.get(u + 1); p++){
                                //countti++;
                                asd.readRecord(p, tempPoint);
                                haku[0] = tempPoint.x;
                                haku[1] = tempPoint.y;
                                String stringi = tempPoint.x + " " + tempPoint.y + " " + tempPoint.z;

                                if(pointInCircle(haku,plotC,plot_radi1.get(j))){

                                    //if(!pisteMappi.contains(stringi)){
                                    if(true){

                                        if(otype.equals("txt")) {
                                            String tempString = " " + LASwrite.LASpoint2String(tempPoint, oparse, aR.sep);

                                            String outLine;

                                            if(!split)
                                                outLine = plotID.get(j).intValue() + tempString;
                                            else
                                                outLine = tempString.trim();
                                            queue.put(outLine);
                                        }
                                        if(otype.equals("las")){

                                            tempPoint.pointSourceId = plotID.get(j).shortValue();
                                            queueLAS.put(tempPoint);

                                        }

                                        //pisteet.add(stringi);
                                        npoints++;
                                        i++;
                                    }
                                }

                                g++;

                            }
                        }
                    } catch( IOException ioException ){
                        System.out.println(ioException.getMessage());
                        System.out.println();
                    }
                    catch( InterruptedException e ){
                        System.out.println(e.getMessage());
                        System.out.println();
                    }
                    //}
                }


                if(npoints != 0){

                    prev_i = i + 1;

                    //pers++;
                }
                else{
                    plot_id_poisto.add(plotID.get(j));
                }

                if(npoints == 0){
                    //nopointplots++;
                    aR.p_update.threadInt[part-1]++;

                }

                pisteet.clear();

                aR.p_update.threadProgress[part-1]++;

                aR.p_update.updateProgressClip();

            }

            String outFile=outieFile;
            // if(statCalc){
            Iterator<Integer> iter = plotID.iterator();

            while(iter.hasNext())
                if(plot_id_poisto.contains(iter.next()))
                    iter.remove();


        }

        /* Shapefile */
        if(shapeType == 2) {


            ArrayList<Integer> plotID;

            ArrayList<double[][]> polyBank1 = new ArrayList<double[][]>();
            ArrayList<double[][]> polyBank = new ArrayList<double[][]>();
            ArrayList<double[][]> polyBank_for_indexing = new ArrayList<double[][]>();
            HashMap<Integer, ArrayList<double[][]>> holes = new HashMap<>();

            polyBank1 = readPolygonsFromWKT(coords, plotID1);
            holes = readPolygonHolesFromWKT(coords, plotID1);


            aR.p_update.totalFiles = plotID1.size();

            int pienin = -1;
            int suurin = -1;

            if (part != 0) {

                int jako = (int) Math.ceil((double) plotID1.size() / (double) cores);

                if (part != cores) {

                    pienin = (part - 1) * jako;
                    suurin = part * jako;
                } else {
                    pienin = (part - 1) * jako;
                    suurin = plotID1.size();
                }

                plotID = new ArrayList<Integer>(plotID1.subList(pienin, suurin));

                polyBank = new ArrayList<double[][]>(polyBank1.subList(pienin, suurin));

            } else {

                plotID = new ArrayList<Integer>(plotID1);
                polyBank = new ArrayList<double[][]>(polyBank1);

            }

            double[][] polyBank_index = new double[polyBank.size()][4];

            for(int i = 0; i < polyBank_index.length; i++){
                polyBank_index[i][0] = Double.MAX_VALUE;
                polyBank_index[i][1] = Double.MIN_VALUE;
                polyBank_index[i][2] = Double.MAX_VALUE;
                polyBank_index[i][3] = Double.MIN_VALUE;
            }

            for(int i = 0; i < polyBank.size(); i++){

                //System.out.println(polyBank.get(i)[0].length);

                for(int j = 0; j < polyBank.get(i).length; j++){

                    if(polyBank.get(i)[j][0] < polyBank_index[i][0])
                        polyBank_index[i][0] = polyBank.get(i)[j][0];
                    if(polyBank.get(i)[j][0] > polyBank_index[i][1])
                        polyBank_index[i][1] = polyBank.get(i)[j][0];

                    if(polyBank.get(i)[j][1] < polyBank_index[i][2])
                        polyBank_index[i][2] = polyBank.get(i)[j][1];
                    if(polyBank.get(i)[j][1] > polyBank_index[i][3])
                        polyBank_index[i][3] = polyBank.get(i)[j][1];
                }
            }

            int kountti = polyBank.size();

            int i = 0;
            double[] plotC = new double[2];
            double[] haku = new double[2];

            int npoints = 0;

            aR.p_update.threadEnd[part-1] = kountti;
            aR.p_update.threadProgress[part-1] = 0;

            aR.p_update.updateProgressClip();


            for (int j = 0; j < kountti; j++) {


                sum_z_a = 0.0;sum_i_a = 0.0;sum_z_f = 0.0;sum_i_f = 0.0;sum_z_l = 0.0;
                sum_i_l = 0.0;sum_z_i = 0.0;sum_i_i = 0.0;

                colnames_convo.clear();gridPoints_xyz_a.clear();gridPoints_xyz_f.clear(); gridPoints_xyz_l.clear();
                colnames_a.clear();colnames_f.clear();colnames_l.clear();colnames_i.clear();

                gridPoints_z_a.clear();gridPoints_i_a.clear();gridPoints_z_f.clear();gridPoints_i_f.clear();

                gridPoints_RGB_f.clear();gridPoints_z_l.clear();gridPoints_i_l.clear();
                gridPoints_z_i.clear();gridPoints_i_i.clear();

                aR.p_update.threadFile[part-1] = plotID.get(j).toString();

                //System.gc();

                TIntHashSet doneIndexes = new TIntHashSet();
                //HashSet<Integer> doneIndexes2 = new HashSet<>();

                //HashSet<String> pisteMappi = new HashSet<String>();

                double[][] tempPolygon = polyBank.get(j);

                //in[i][0] > maxX
                /*
                Path2D polyg = new Path2D.Double();
                Path2D polyg_for_index = new Path2D.Double();


                polyg.moveTo(tempPolygon[0][0], tempPolygon[0][1]);

                for (int o = 1; o < tempPolygon.length; o++) {
                    //System.out.println("pol: " + Arrays.toString(tempPolygon[o]));
                    polyg.lineTo(tempPolygon[o][0], tempPolygon[o][1]);
                }

                polyg_for_index.moveTo(polyBank_index[j][1], polyBank_index[j][3]);
                polyg_for_index.lineTo(polyBank_index[j][1], polyBank_index[j][2]);

                polyg_for_index.lineTo(polyBank_index[j][0], polyBank_index[j][2]);
                polyg_for_index.lineTo(polyBank_index[j][0], polyBank_index[j][3]);
                polyg_for_index.closePath();
                polyg = polyg_for_index;

                 */
                //System.out.println(Arrays.toString(polyBank_index[j]));

                //polyg.closePath();



                double[] minmaxXY = findMinMax(tempPolygon);

                //Point poly_p1 = new Point(minmaxXY[0], minmaxXY[3]);

                ArrayList<Integer> valinta = new ArrayList<Integer>();



                for (int th = 0; th < pilvi.size(); th++) {

                    if(aR.eaba)
                        buffer = 5.0;

                    double[] extentti2 = new double[4];

                    extentti2[0] = all_min_x.get(th);
                    extentti2[1] = all_max_x.get(th);
                    extentti2[2] = all_min_y.get(th);
                    extentti2[3] = all_max_y.get(th);

                    if(buffer > 0){

                        extentti2[0] -= buffer;
                        extentti2[1] += buffer;
                        extentti2[2] -= buffer;
                        extentti2[3] += buffer;


                /*
                output[0] = minX;
                output[1] = maxX;
                output[2] = minY;
                output[3] = maxY;
                 */
                        minmaxXY[0] -= buffer;
                        minmaxXY[1] += buffer;
                        minmaxXY[2] -= buffer;
                        minmaxXY[3] += buffer;

                    }
                    /*
                     If one rectangle is on left side of other
                    if (l1.x > r2.x || l2.x > r1.x)
                        return false;

                     If one rectangle is above other
                    if (l1.y < r2.y || l2.y < r1.y)
                        return false;

                    */



                    if (buffer == 0.0) {

                        boolean accept = true;

                        if(all_min_x.get(th) > minmaxXY[1] || minmaxXY[0] > all_max_x.get(th))
                            accept = false;
                        if(all_min_y.get(th) > minmaxXY[3] || minmaxXY[2] > all_max_y.get(th))
                            accept = false;

                        if(false)
                        if (isWithin(extentti2, minmaxXY[0], minmaxXY[3]) || isWithin(extentti2, minmaxXY[0], minmaxXY[2]) ||
                                isWithin(extentti2, minmaxXY[1], minmaxXY[3]) || isWithin(extentti2, minmaxXY[1], minmaxXY[2]))
                            valinta.add(th);

                        if(accept)
                            valinta.add(th);


                    } else {

                        // TODO FIX THIS UP
                        boolean accept = true;

                        if(all_min_x.get(th) > minmaxXY[1] || minmaxXY[0] > all_max_x.get(th))
                            accept = false;
                        if(all_min_y.get(th) > minmaxXY[3] || minmaxXY[2] > all_max_y.get(th))
                            accept = false;

                        if(false)
                            if (isWithin(extentti2, minmaxXY[0], minmaxXY[3]) || isWithin(extentti2, minmaxXY[0], minmaxXY[2]) ||
                                    isWithin(extentti2, minmaxXY[1], minmaxXY[3]) || isWithin(extentti2, minmaxXY[1], minmaxXY[2]))
                                valinta.add(th);

                        if(accept)
                            valinta.add(th);

                        //if (minmaxXY[1] >= (all_min_x.get(th) + buffer) && minmaxXY[0] <= (all_max_x.get(th) - buffer) && minmaxXY[3] >= (all_min_y.get(th) + buffer) && minmaxXY[2] <= (all_max_y.get(th) - buffer))
                          //  valinta.add(th);

                    }

                }


                if (valinta.size() == 0) {

                }
                npoints = 0;

                int debugCounter = 0;


                //System.out.println("hERE " + pilvi.size());




                for (int va = 0; va < valinta.size(); va++) {

                    doneIndexes.clear();
                    LASReader asd = new LASReader(aR.inputFiles.get(valinta.get(va))); //pointClouds.get(valinta.get(va));

                    /* Define the variables that we need */
                    int tree_id = -1, treeId = -1;

                    boolean tree_id_found_ = false;
                    boolean eaba_but_ground = false;
                    boolean point_inside_polygon = false;


                    if(aR.eaba) {

                        try {
                            tree_id = asd.extraBytes_names.get("ITC_id");
                        } catch (Exception e) {
                            throw new toolException("Cannot find ITC_id extra byte VLR. Maybe you don't want eaba?");
                        }
                    }

                    if (asd.isIndexed) {

                        // TODO ADD BUFFER HERE IF aR.eaba

                        asd.queryPoly2(minmaxXY[0], minmaxXY[1], minmaxXY[2], minmaxXY[3]);

                        LasPoint tempPoint = new LasPoint();

                        try {
                            FileWriter fw = null;

                            if(asd.indexContainsStuff()) {

                                        int p = 0;

                                        while(!asd.index_read_terminated){

                                            p = asd.fastReadFromQuery(tempPoint);

                                            if(aR.eaba){
                                                treeId = tempPoint.getExtraByteInt(tree_id);
                                            }
                                            //if(tempPoint.R == 0 || tempPoint.G == 0 || tempPoint.B == 0 || tempPoint.N == 0)
                                            //    System.out.println("ZERO SPECTRAL POINT! SHOULD NOT HAPPEN!");
                                                haku[0] = tempPoint.x;
                                                haku[1] = tempPoint.y;
                                            //System.out.println(tempPoint);

                                            if(tempPoint.x <= minmaxXY[1] && tempPoint.x >= minmaxXY[0] &&
                                                    tempPoint.y <= minmaxXY[3] && tempPoint.y >= minmaxXY[2])

                                                eaba_but_ground = false;
                                                tree_id_found_ = false;

                                                if(aR.eaba && treeId > 0){

                                                    tree_id_found_ = aR.checkIfTree(treeId, plotID.get(j));

                                                }else if(aR.eaba && treeId == 0){

                                                    eaba_but_ground = true;

                                                }

                                                point_inside_polygon = pointInPolygon(haku, tempPolygon, holes, plotID.get(j));

                                                if ( (aR.eaba && eaba_but_ground && point_inside_polygon) ||    // ground
                                                        (aR.eaba && tree_id_found_) ||                          // tree within extending outside
                                                        (!aR.eaba && point_inside_polygon)) {                   // Basic without eaba

                                                //if (pointInPolygon(haku, tempPolygon)) {

                                                    if (otype.equals("las")) {

                                                        if(aR.save_to_p_id)
                                                            tempPoint.pointSourceId = (short)plotID.get(j).intValue();

                                                        if(tempPoint.pointSourceId < 0)
                                                            throw new toolException("PointSourceId is negative: " + tempPoint.pointSourceId);

                                                        //if(plotID.get(j) == 149)
                                                        //    System.out.println("149 " + tempPoint.pointSourceId);

                                                        tempPoint.setExtraByteINT(plotID.get(j), aR.create_extra_byte_vlr_n_bytes.get(0), 0);

                                                        //if(plotID.get(j) == 111)
                                                        //    System.out.println(debugCounter++);
                                                        if (aR.omet) {


                                                            gridPoints_z_a.add(tempPoint.z);
                                                            gridPoints_i_a.add(tempPoint.intensity);
                                                            gridPoints_xyz_a.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, tempPoint.R, tempPoint.G, tempPoint.B, tempPoint.N});

                                                            sum_z_a += tempPoint.z;
                                                            sum_i_a += tempPoint.intensity;

                                                            //System.out.println("intensity" + tempPoint.intensity);

                                                            if (tempPoint.returnNumber == 1) {
                                        /*
                                        sum_z_f += tempPoint.z;
                                        sum_i_f += tempPoint.intensity;

                                        gridPoints_z_f.add(tempPoint.z);
                                        gridPoints_i_f.add(tempPoint.intensity);

                                         */

                                                                gridPoints_z_f.add(tempPoint.z);
                                                                gridPoints_i_f.add(tempPoint.intensity);
                                                                gridPoints_xyz_f.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, tempPoint.R, tempPoint.G, tempPoint.B, tempPoint.N});

                                                                if(aR.ray_trace) {
                                                                    if(tempPoint.pointSourceId == 2 && tempPoint.z > aR.z_cutoff) {
                                                                            //System.out.println(tempPoint.R + " " + tempPoint.G + " " + tempPoint.B + " " + tempPoint.pointSourceId);
                                                                        gridPoints_RGB_f.add(new int[]{tempPoint.R, tempPoint.G, tempPoint.B, tempPoint.N});
                                                                    }
                                                                }else
                                                                    gridPoints_RGB_f.add(new int[]{tempPoint.R, tempPoint.G, tempPoint.B, tempPoint.N});


                                                                //System.out.println(tempPoint.getRGB());
                                                                //System.out.println(Arrays.toString(gridPoints_RGB_f.get(gridPoints_RGB_f.size()-1)));
                                                                sum_z_f += tempPoint.z;
                                                                sum_i_f += tempPoint.intensity;
                                                            }
                                                            if (tempPoint.returnNumber == tempPoint.numberOfReturns) {
/*
                                        sum_z_l += tempPoint.z;
                                        sum_i_l += tempPoint.intensity;

                                        gridPoints_z_l.add(tempPoint.z);
                                        gridPoints_i_l.add(tempPoint.intensity);

 */
                                                                gridPoints_z_l.add(tempPoint.z);
                                                                gridPoints_i_l.add(tempPoint.intensity);
                                                                gridPoints_xyz_l.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, tempPoint.R, tempPoint.G, tempPoint.B, tempPoint.N});

                                                                sum_z_l += tempPoint.z;
                                                                sum_i_l += tempPoint.intensity;

                                                            }
                                                            if (tempPoint.returnNumber > 1 && tempPoint.returnNumber != tempPoint.numberOfReturns) {
/*
                                        sum_z_i += tempPoint.z;
                                        sum_i_i += tempPoint.intensity;
                                        gridPoints_z_i.add(tempPoint.z);
                                        gridPoints_i_i.add(tempPoint.intensity);
 */
                                                                gridPoints_z_i.add(tempPoint.z);
                                                                gridPoints_i_i.add(tempPoint.intensity);

                                                                sum_z_i += tempPoint.z;
                                                                sum_i_i += tempPoint.intensity;

                                                            }
                                                        }
                                                        if (!aR.split) {
                                                                try {
                                                                    pointBuffer.writePoint(tempPoint, aR.getInclusionRule(), p);
                                                                }catch (Exception e){
                                                                    e.printStackTrace();
                                                                    System.out.println("pointBuffer.writePoint(tempPoint, aR.getInclusionRule(), p);");
                                                                }
                                                        }else
                                                            outputBuffers.get(valinta.get(va)).writePoint(tempPoint, aR.getInclusionRule(), p);

                                                        aR.p_update.lasclip_clippedPoints++;

                                                        if (aR.p_update.lasclip_clippedPoints % 10000 == 0)
                                                            aR.p_update.updateProgressClip();

                                                    } else if (otype.equals("txt")) {
                                                        String tempString = " " + LASwrite.LASpoint2String(tempPoint, oparse, aR.sep);
                                                        String outLine = "";
                                                        if (!split)
                                                            outLine = plotID.get(j).intValue() + tempString;
                                                        else
                                                            outLine = tempString.trim();
                                                        queue.put(outLine);
                                                    }

                                                    npoints++;
                                                    i++;
                                                    //}
                                                }
                                        }
                                    }
                                //}
                            //}
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // NOT INDEXED
                    else{


                        LasPoint tempPoint = new LasPoint();


                        try {

                            int maxi = 0;

                            int counter = 0;

                            //asd.braf.raFile.seek(asd.braf.raFile.length());

                            for(long p = 0; p < asd.getNumberOfPointRecords(); p += 10000){

                                maxi = (int)Math.min(10000, Math.abs(asd.getNumberOfPointRecords() - p));


                                int count = 0;
                                asd.readRecord_noRAF(p, tempPoint, maxi);
                                //pointCloud.braf.buffer.position(0);

                                for (int s = 0; s < maxi; s++) {
                                    //Sstem.out.println(j);

                                    //if(!doneIndexes.contains(p+s)) {

                                        asd.readFromBuffer(tempPoint);

                                        if(aR.eaba){

                                            treeId = tempPoint.getExtraByteInt(tree_id);

                                        }

                                        if(!aR.inclusionRule.ask(tempPoint, p+s, true)){
                                            continue;
                                        }

                                        haku[0] = tempPoint.x;
                                        haku[1] = tempPoint.y;

                                        if(tempPoint.x <= minmaxXY[1] && tempPoint.x >= minmaxXY[0] &&
                                                tempPoint.y <= minmaxXY[3] && tempPoint.y >= minmaxXY[2])

                                            eaba_but_ground = false;
                                        tree_id_found_ = false;

                                        if(aR.eaba && treeId > 0){

                                            tree_id_found_ = aR.checkIfTree(treeId, plotID.get(j));

                                        }else if(aR.eaba && treeId == 0){

                                            eaba_but_ground = true;

                                        }

                                        point_inside_polygon = pointInPolygon(haku, tempPolygon, holes, plotID.get(j));


                                        if ( (aR.eaba && eaba_but_ground && point_inside_polygon) ||    // ground
                                                (aR.eaba && tree_id_found_) ||                          // tree within extending outside
                                                (!aR.eaba && point_inside_polygon)) {                   // Basic without eaba
                                        //if (pointInPolygon(haku, tempPolygon)) {

                                            if (otype.equals("las")) {

                                                if(aR.save_to_p_id)
                                                    tempPoint.pointSourceId = (short)plotID.get(j).intValue();

                                                if(tempPoint.pointSourceId < 0)
                                                    throw new toolException("PointSourceId is negative: " + tempPoint.pointSourceId);
                                                //tempPoint.gpsTime = plotID.get(j).doubleValue();
                                                tempPoint.setExtraByteINT(plotID.get(j), aR.create_extra_byte_vlr_n_bytes.get(0), 0);
                                                //System.out.println(tempPoint.gpsTime);
                                                //LasPoint clonePoint = (LasPoint) tempPoint.clone();
                                                //queueLAS.put(clonePoint);
                                                if(aR.omet){

                                                    gridPoints_z_a.add(tempPoint.z);
                                                    gridPoints_i_a.add(tempPoint.intensity);

                                                    //gridPoints_xyz_a.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z});
                                                    gridPoints_xyz_a.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z, tempPoint.R, tempPoint.G, tempPoint.B, tempPoint.N});

                                                    sum_z_a += tempPoint.z;
                                                    sum_i_a += tempPoint.intensity;

                                                    //System.out.println("intensity" + tempPoint.intensity);

                                                    if (tempPoint.returnNumber == 1) {
                                        /*
                                        sum_z_f += tempPoint.z;
                                        sum_i_f += tempPoint.intensity;

                                        gridPoints_z_f.add(tempPoint.z);
                                        gridPointsclip_i_f.add(tempPoint.intensity);

                                         */
                                                        gridPoints_z_f.add(tempPoint.z);
                                                        gridPoints_i_f.add(tempPoint.intensity);
                                                        gridPoints_xyz_f.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z});

                                                        if(aR.ray_trace) {
                                                            if(tempPoint.pointSourceId != 3 && tempPoint.z > aR.z_cutoff)
                                                                gridPoints_RGB_f.add(new int[]{tempPoint.R, tempPoint.G, tempPoint.B, tempPoint.N});

                                                        }else
                                                            gridPoints_RGB_f.add(new int[]{tempPoint.R, tempPoint.G, tempPoint.B, tempPoint.N});

                                                        sum_z_f += tempPoint.z;
                                                        sum_i_f += tempPoint.intensity;
                                                    }
                                                    if (tempPoint.returnNumber == tempPoint.numberOfReturns) {
/*
                                        sum_z_l += tempPoint.z;
                                        sum_i_l += tempPoint.intensity;

                                        gridPoints_z_l.add(tempPoint.z);
                                        gridPoints_i_l.add(tempPoint.intensity);

 */
                                                        gridPoints_z_l.add(tempPoint.z);
                                                        gridPoints_i_l.add(tempPoint.intensity);
                                                        gridPoints_xyz_l.add(new double[]{tempPoint.x, tempPoint.y, tempPoint.z});

                                                        sum_z_l += tempPoint.z;
                                                        sum_i_l += tempPoint.intensity;

                                                    }
                                                    if (tempPoint.returnNumber > 1 && tempPoint.returnNumber != tempPoint.numberOfReturns) {
/*
                                        sum_z_i += tempPoint.z;
                                        sum_i_i += tempPoint.intensity;
                                        gridPoints_z_i.add(tempPoint.z);
                                        gridPoints_i_i.add(tempPoint.intensity);
 */
                                                        gridPoints_z_i.add(tempPoint.z);
                                                        gridPoints_i_i.add(tempPoint.intensity);

                                                        sum_z_i += tempPoint.z;
                                                        sum_i_i+=  tempPoint.intensity;

                                                    }

                                                }

                                                if(!aR.split)
                                                    pointBuffer.writePoint(tempPoint, aR.getInclusionRule(), p);
                                                else
                                                    outputBuffers.get(valinta.get(va)).writePoint(tempPoint, aR.getInclusionRule(), p);
                                                //LasPoint clonePoint = (LasPoint) tempPoint.clone();


                                                aR.p_update.lasclip_clippedPoints++;

                                                if(aR.p_update.lasclip_clippedPoints % 10000 == 0)
                                                    aR.p_update.updateProgressClip();


                                            } else if (otype.equals("txt")) {
                                                String tempString = " " + LASwrite.LASpoint2String(tempPoint, oparse, aR.sep);
                                                String outLine = "";

                                                if (!split)
                                                    outLine = plotID.get(j).intValue() + tempString;
                                                else
                                                    outLine = tempString.trim();
                                                queue.put(outLine);
                                            }

                                            npoints++;
                                            i++;
                                            //}
                                        }
                                   // }else{
                                     //   asd.skipPointInBuffer();
                                    //}
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    asd.close();
                    asd = null;
                    //System.gc();
                }


                /* if we want to output metrics per polyon */
                if(aR.omet){

                    boolean train = aR.convolution_metrics_train;

                    if(aR.convolution_metrics_train || aR.convolution_metrics) {

                        if (aR.convolution_metrics_train) {
                            ArrayList<ArrayList<Double>> metrics_convolution = pCM.calc_nn_input_train_spectral(gridPoints_xyz_a, "_convo_f", colnames_convo, minmaxXY[0], minmaxXY[3],
                                    minmaxXY[1], minmaxXY[2]);
                            aR.lCMO.writeLine_convo(metrics_convolution, colnames_convo, plotID.get(j));
                            //System.out.println("HERE!!");
                        } else if (aR.convolution_metrics) {
                            ArrayList<Double> metrics_convolution = pCM.calc_nn_input_test_spectral(gridPoints_xyz_a, "_convo_f", colnames_convo, minmaxXY[0], minmaxXY[3],
                                    minmaxXY[1], minmaxXY[2]);
                            aR.lCMO.writeLine_convo_test(metrics_convolution, colnames_convo, plotID.get(j));
                            //System.out.println("HERE_test_data!!");
                        }
                    }
                    //else {
                        ArrayList<Double> metrics_a = pCM.calc(gridPoints_z_a, gridPoints_i_a, sum_z_a, sum_i_a, "_a", colnames_a);
                        ArrayList<Double> metrics_f = null;

                        if(!aR.ray_trace)
                            metrics_f = pCM.calc_with_RGB(gridPoints_z_f, gridPoints_i_f, sum_z_f, sum_i_f, "_f", colnames_f, gridPoints_RGB_f);
                        else
                            metrics_f = pCM.calc_with_RGB_only_sunny(gridPoints_z_f, gridPoints_i_f, sum_z_f, sum_i_f, "_f", colnames_f, gridPoints_RGB_f);

                        ArrayList<Double> metrics_l = pCM.calc(gridPoints_z_l, gridPoints_i_l, sum_z_l, sum_i_l, "_l", colnames_l);
                        ArrayList<Double> metrics_i = pCM.calc(gridPoints_z_i, gridPoints_i_i, sum_z_i, sum_i_i, "_i", colnames_i);

                        //for(int co = 0; co < 11; co++) {
                            aR.lCMO.writeLine(metrics_a, metrics_f, metrics_l, metrics_i, colnames_a, colnames_f, colnames_l, colnames_i, plotID.get(j));
                       // }
                    //}
                }

                if (npoints != 0) {
                    aR.p_update.threadInt[part-1]++;
                } else {

                    aR.p_update.lasclip_empty++;
                }

                if (npoints == 0) {

                }

                //System.gc();

                aR.p_update.threadProgress[part-1]++;
                aR.p_update.updateProgressClip();
                aR.p_update.fileProgress++;

                System.out.println(j + " / " + kountti);
            }
        }

        aR.p_update.updateProgressClip();

        if(!aR.split) {

            pointBuffer.close();
            //pointBuffer.pwrite.close(aR);

        }
        else{
            for(int i = 0; i < outputBuffers.size(); i++)
                outputBuffers.get(i).close();
        }

    }

    public void clipPlots_new(String coords, int shapeType, ArrayList<String> pilvi,String outieFile,
                          ArrayList<String> indeksitiedosto, String delim, String outShape, int part, int cores, boolean statCalc, String output_txt_points,
                          String all_index, double buffer, String plotOutName, String oparse, String output, boolean split, String otype,
                          RunId4pointsLAS.FileOutput foutti, BlockingQueue<String> queue, BlockingQueue<LasPoint> queueLAS, argumentReader aR, BlockingQueue<byte[]> que,
                          LASraf mergeOutput, ArrayList<BlockingQueue<byte[]>> qu, BlockingQueue<Integer> threadWrote, pointWriterMultiThread pwrite,
                          ArrayList<pointWriterMultiThread> outputFiles)throws Exception{


        polygonIndex[][] polygon_index;

    }

    class polygonIndex {
        public ArrayList<Integer> polygonIds_inside;
        public ArrayList<Integer> polygonIds_border;
        public String LastName;
        public int    BirthYear;


    };


    public static void clipPlots_singleLASfile(String coords,

                            argumentReader aR,
                                               LASReader pointCloud)throws Exception{



        pointCloudMetrics pCM = new pointCloudMetrics(aR);

        double sum_z_a = 0.0;
        double sum_i_a = 0.0;
        double sum_z_f = 0.0;
        double sum_i_f = 0.0;
        double sum_z_l = 0.0;
        double sum_i_l = 0.0;
        double sum_z_i = 0.0;
        double sum_i_i = 0.0;

        ArrayList<String> colnames_a = new ArrayList<>(), colnames_f = new ArrayList<>(), colnames_l = new ArrayList<>(), colnames_i = new ArrayList<>();

        ArrayList<Double> gridPoints_z_a = new ArrayList<>();
        ArrayList<Integer> gridPoints_i_a = new ArrayList<>();

        ArrayList<Double> gridPoints_z_f = new ArrayList<>();
        ArrayList<Integer> gridPoints_i_f = new ArrayList<>();
        ArrayList<int[]> gridPoints_RGB_f = new ArrayList<>();

        ArrayList<Double> gridPoints_z_l = new ArrayList<>();
        ArrayList<Integer> gridPoints_i_l = new ArrayList<>();

        ArrayList<Double> gridPoints_z_i = new ArrayList<>();
        ArrayList<Integer> gridPoints_i_i = new ArrayList<>();

        aR.p_update.updateProgressClip();

        ArrayList<LASReader> pointClouds = aR.pointClouds;

        LASReader templateCloud = new LASReader(aR.inputFiles.get(0));

        ArrayList<LasPointBufferCreator> outputBuffers = new ArrayList<>();
/*
        for(int i = 0; i < aR.pointClouds.size(); i++){
            pointClouds.add(new LASReader(new File(aR.pointClouds.get(i).getFile().getAbsolutePath())));
        }
*/

        File outputFile = aR.createOutputFile(pointCloud);

        pointWriterMultiThread pw = new pointWriterMultiThread(outputFile, pointCloud, "lasclip", aR);

        LasPointBufferCreator pointBuffer = null;


        pointBuffer = new LasPointBufferCreator(1, pw);


        LasPointBufferCreator buf = new LasPointBufferCreator(1, pw);


        ArrayList<String> pisteet = new ArrayList<String>();   //TULOSTEPISTELISTA

        ArrayList<Double> coords1_x1 = new ArrayList<Double>();   //TULOSTEPISTELISTA
        ArrayList<Double> coords1_y1 = new ArrayList<Double>();   //TULOSTEPISTELISTA
        ArrayList<Double> plot_radi = new ArrayList<Double>();   //TULOSTEPISTELISTA
        ArrayList<Integer> plotID1 = new ArrayList<Integer>();   //TULOSTEPISTELISTA

        ArrayList<Double> all_min_y = new ArrayList<Double>();
        ArrayList<Double> all_max_y = new ArrayList<Double>();
        ArrayList<Double> all_min_x = new ArrayList<Double>();
        ArrayList<Double> all_max_x = new ArrayList<Double>();

        ArrayList<String> all_filename = new ArrayList<String>();
        String all_index_line;



        String line1;

        Cantor homma = new Cantor();

        ArrayList<Double> z = new ArrayList<>();
        ArrayList<Double> intensity = new ArrayList<>();

        int debugCounter = 0;
        /* Shapefile */
        if(true) {

            ArrayList<Integer> plotID;

            ArrayList<double[][]> polyBank1 = new ArrayList<double[][]>();
            ArrayList<double[][]> polyBank = new ArrayList<double[][]>();
            ArrayList<double[][]> polyBank_for_indexing = new ArrayList<double[][]>();

            polyBank1 = readPolygonsFromWKT(coords, plotID1);
            aR.p_update.totalFiles = plotID1.size();

            int pienin = -1;
            int suurin = -1;


                plotID = new ArrayList<Integer>(plotID1);
                polyBank = new ArrayList<double[][]>(polyBank1);



            double[][] polyBank_index = new double[polyBank.size()][4];

            for(int i = 0; i < polyBank_index.length; i++){
                polyBank_index[i][0] = Double.MAX_VALUE;
                polyBank_index[i][1] = Double.MIN_VALUE;
                polyBank_index[i][2] = Double.MAX_VALUE;
                polyBank_index[i][3] = Double.MIN_VALUE;
            }

            for(int i = 0; i < polyBank.size(); i++){

                //System.out.println(polyBank.get(i)[0].length);

                for(int j = 0; j < polyBank.get(i).length; j++){

                    if(polyBank.get(i)[j][0] < polyBank_index[i][0])
                        polyBank_index[i][0] = polyBank.get(i)[j][0];
                    if(polyBank.get(i)[j][0] > polyBank_index[i][1])
                        polyBank_index[i][1] = polyBank.get(i)[j][0];

                    if(polyBank.get(i)[j][1] < polyBank_index[i][2])
                        polyBank_index[i][2] = polyBank.get(i)[j][1];
                    if(polyBank.get(i)[j][1] > polyBank_index[i][3])
                        polyBank_index[i][3] = polyBank.get(i)[j][1];
                }
            }

            int kountti = polyBank.size();

            int i = 0;
            double[] plotC = new double[2];
            double[] haku = new double[2];

            int npoints = 0;

            aR.p_update.threadEnd[1-1] = kountti;
            aR.p_update.threadProgress[1-1] = 0;

            aR.p_update.updateProgressClip();

            double[] extentti2 = new double[4];
            extentti2[0] = pointCloud.minX;
            extentti2[1] = pointCloud.maxX;
            extentti2[2] = pointCloud.minY;
            extentti2[3] = pointCloud.maxY;

            for (int j = 0; j < kountti; j++) {

                double[][] tempPolygon = polyBank.get(j);


                double[] minmaxXY = findMinMax(tempPolygon);

                    if (isWithin(extentti2, minmaxXY[0], minmaxXY[3]) || isWithin(extentti2, minmaxXY[0], minmaxXY[2]) ||
                            isWithin(extentti2, minmaxXY[1], minmaxXY[3]) || isWithin(extentti2, minmaxXY[1], minmaxXY[2])){

                    }else{
                        continue;
                    }

                sum_z_a = 0.0;
                sum_i_a = 0.0;
                sum_z_f = 0.0;
                sum_i_f = 0.0;
                sum_z_l = 0.0;
                sum_i_l = 0.0;
                sum_z_i = 0.0;
                sum_i_i = 0.0;

                colnames_a.clear();
                colnames_f.clear();
                colnames_l.clear();
                colnames_i.clear();

                gridPoints_z_a.clear();
                gridPoints_i_a.clear();

                gridPoints_z_f.clear();
                gridPoints_i_f.clear();
                gridPoints_RGB_f.clear();



                gridPoints_z_l.clear();
                gridPoints_i_l.clear();

                gridPoints_z_i.clear();
                gridPoints_i_i.clear();

                aR.p_update.threadFile[1-1] = plotID.get(j).toString();

                //System.gc();

                TIntHashSet doneIndexes = new TIntHashSet();
                //HashSet<Integer> doneIndexes2 = new HashSet<>();

                //HashSet<String> pisteMappi = new HashSet<String>();

                //double[][] tempPolygon = polyBank.get(j);

                //in[i][0] > maxX
                Path2D polyg = new Path2D.Double();
                Path2D polyg_for_index = new Path2D.Double();


                polyg.moveTo(tempPolygon[0][0], tempPolygon[0][1]);

                for (int o = 1; o < tempPolygon.length; o++) {
                    //System.out.println("pol: " + Arrays.toString(tempPolygon[o]));
                    polyg.lineTo(tempPolygon[o][0], tempPolygon[o][1]);
                }

                polyg_for_index.moveTo(polyBank_index[j][1], polyBank_index[j][3]);
                polyg_for_index.lineTo(polyBank_index[j][1], polyBank_index[j][2]);

                polyg_for_index.lineTo(polyBank_index[j][0], polyBank_index[j][2]);
                polyg_for_index.lineTo(polyBank_index[j][0], polyBank_index[j][3]);
                polyg_for_index.closePath();
                polyg = polyg_for_index;
                //System.out.println(Arrays.toString(polyBank_index[j]));

                //polyg.closePath();

                //double[] minmaxXY = findMinMax(tempPolygon);

                npoints = 0;



                //for (int va = 0; va < valinta.size(); va++) {


                    doneIndexes.clear();
                    LASReader asd = pointCloud; //pointClouds.get(valinta.get(va));

                    int readPoints = 0;

                    if (asd.isIndexed) {

                        asd.queryPoly2(minmaxXY[0], minmaxXY[1], minmaxXY[2], minmaxXY[3]);

                        LasPoint tempPoint = new LasPoint();

                        try {
                            FileWriter fw = null;

                            if(asd.queriedIndexes2.size() > 0)

                                for (int u = 0; u < asd.queriedIndexes2.size(); u++) {

                                    long n1 = asd.queriedIndexes2.get(u)[1] - asd.queriedIndexes2.get(u)[0];
                                    long n2 = asd.queriedIndexes2.get(u)[1];

                                    int parts = (int) Math.ceil((double) n1 / 20000.0);
                                    int jako = (int) Math.ceil((double) n1 / (double) parts);

                                    int ero;

                                    // System.out.println("Start: " + asd.queriedIndexes2.get(u)[0] + " end: " + asd.queriedIndexes2.get(u)[1]);
                                    // System.out.println("n1: " + n1 + " n2: " + n2 + " parts: " + parts + " jako: " + jako);

                                    if(parts > 1){
                                        debugCounter++;
                                    }

                                    for (int c = 1; c <= parts; c++) {

                                        if (c != parts) {
                                            pienin = (c - 1) * jako;
                                            suurin = c * jako;
                                        } else {
                                            pienin = (c - 1) * jako;
                                            suurin = (int) n1;
                                        }

                                        pienin = pienin + asd.queriedIndexes2.get(u)[0];
                                        suurin = suurin + asd.queriedIndexes2.get(u)[0];


                                        ero = suurin - pienin + 1;

                                        //System.out.println("pienin: " + pienin + " suurin: " + suurin + " ero: " + ero);
                                        //System.out.println(ero + " " + c + " / " + parts);
                                        //System.out.println();

                                        //try {

                                        asd.readRecord_noRAF(pienin, tempPoint, ero);

                                        //} catch (Exception e) {
                                        //  e.printStackTrace();
                                        //}

                                        int count1 = 0;
                                        debugCounter = 0;

                                        for (int p = pienin; p <= suurin; p++) {
                                            debugCounter++;

                                            if (!doneIndexes.contains(p)) {

                                                asd.readFromBuffer(tempPoint);

                                                if(!aR.inclusionRule.ask(tempPoint, p, true)){
                                                    continue;
                                                }

                                                readPoints++;
                                                doneIndexes.add(p);
                                                //asd.readRecord(p, tempPoint);

                                                haku[0] = tempPoint.x;
                                                haku[1] = tempPoint.y;

                                                if (pointInPolygon(haku, tempPolygon)) {
                                                    //if (true) {

                                                    //if (otype.equals("las")) {

                                                        //tempPoint.pointSourceId = plotID.get(j).shortValue();


                                                        if(aR.omet){

                                                            gridPoints_z_a.add(tempPoint.z);
                                                            gridPoints_i_a.add(tempPoint.intensity);

                                                            sum_z_a += tempPoint.z;
                                                            sum_i_a += tempPoint.intensity;

                                                            //System.out.println("intensity" + tempPoint.intensity);

                                                            if (tempPoint.returnNumber == 1) {
                                        /*
                                        sum_z_f += tempPoint.z;
                                        sum_i_f += tempPoint.intensity;

                                        gridPoints_z_f.add(tempPoint.z);
                                        gridPoints_i_f.add(tempPoint.intensity);

                                         */
                                                                gridPoints_z_f.add(tempPoint.z);
                                                                gridPoints_i_f.add(tempPoint.intensity);

                                                                gridPoints_RGB_f.add(new int[]{tempPoint.R,tempPoint.G,tempPoint.B});
                                                                //System.out.println(Arrays.toString(gridPoints_RGB_f.get(gridPoints_RGB_f.size()-1)));
                                                                sum_z_f += tempPoint.z;
                                                                sum_i_f += tempPoint.intensity;
                                                            }
                                                            if (tempPoint.returnNumber == tempPoint.numberOfReturns) {
/*
                                        sum_z_l += tempPoint.z;
                                        sum_i_l += tempPoint.intensity;

                                        gridPoints_z_l.add(tempPoint.z);
                                        gridPoints_i_l.add(tempPoint.intensity);

 */
                                                                gridPoints_z_l.add(tempPoint.z);
                                                                gridPoints_i_l.add(tempPoint.intensity);

                                                                sum_z_l += tempPoint.z;
                                                                sum_i_l += tempPoint.intensity;

                                                            }
                                                            if (tempPoint.returnNumber > 1 && tempPoint.returnNumber != tempPoint.numberOfReturns) {
/*
                                        sum_z_i += tempPoint.z;
                                        sum_i_i += tempPoint.intensity;
                                        gridPoints_z_i.add(tempPoint.z);
                                        gridPoints_i_i.add(tempPoint.intensity);
 */
                                                                gridPoints_z_i.add(tempPoint.z);
                                                                gridPoints_i_i.add(tempPoint.intensity);

                                                                sum_z_i += tempPoint.z;
                                                                sum_i_i+=  tempPoint.intensity;

                                                            }

                                                        }


                                                            pointBuffer.writePoint(tempPoint, aR.getInclusionRule(), p);

                                                        aR.p_update.lasclip_clippedPoints++;

                                                        if(aR.p_update.lasclip_clippedPoints % 10000 == 0)
                                                            aR.p_update.updateProgressClip();

                                                    //}

                                                    npoints++;
                                                    i++;
                                                    //}
                                                }
                                            }else{
                                                asd.skipPointInBuffer();
                                            }
                                        }

                                        //System.out.println(debugCounter + " ?==? " + ero);
                                    }
                                }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // NOT INDEXED
                    else{
                        LasPoint tempPoint = new LasPoint();

                        try {

                            int maxi = 0;

                            int counter = 0;

                            //asd.braf.raFile.seek(asd.braf.raFile.length());

                            for(int p = 0; p < asd.getNumberOfPointRecords(); p += 10000){

                                maxi = (int)Math.min(10000, Math.abs(asd.getNumberOfPointRecords() - p));


                                int count = 0;
                                asd.readRecord_noRAF(p, tempPoint, maxi);
                                //pointCloud.braf.buffer.position(0);

                                for (int s = 0; s < maxi; s++) {
                                    //Sstem.out.println(j);

                                    if(!doneIndexes.contains(p+s)) {

                                        asd.readFromBuffer(tempPoint);

                                        if(!aR.inclusionRule.ask(tempPoint, p+s, true)){
                                            continue;
                                        }

                                        //System.out.println((p+s) + " " + va);





                                        haku[0] = tempPoint.x;
                                        haku[1] = tempPoint.y;

                                        if (pointInPolygon(haku, tempPolygon)) {

                                            doneIndexes.add( (p + s));

                                            //if (otype.equals("las")) {

                                                //tempPoint.pointSourceId = plotID.get(j).shortValue();
                                                //tempPoint.gpsTime = plotID.get(j).doubleValue();

                                                //System.out.println(tempPoint.gpsTime);
                                                //LasPoint clonePoint = (LasPoint) tempPoint.clone();
                                                //queueLAS.put(clonePoint);


                                                if(aR.omet){

                                                    gridPoints_z_a.add(tempPoint.z);
                                                    gridPoints_i_a.add(tempPoint.intensity);

                                                    sum_z_a += tempPoint.z;
                                                    sum_i_a += tempPoint.intensity;

                                                    //System.out.println("intensity" + tempPoint.intensity);

                                                    if (tempPoint.returnNumber == 1) {
                                        /*
                                        sum_z_f += tempPoint.z;
                                        sum_i_f += tempPoint.intensity;

                                        gridPoints_z_f.add(tempPoint.z);
                                        gridPointsclip_i_f.add(tempPoint.intensity);

                                         */
                                                        gridPoints_z_f.add(tempPoint.z);
                                                        gridPoints_i_f.add(tempPoint.intensity);

                                                        gridPoints_RGB_f.add(new int[]{tempPoint.R,tempPoint.G,tempPoint.B});

                                                        sum_z_f += tempPoint.z;
                                                        sum_i_f += tempPoint.intensity;
                                                    }
                                                    if (tempPoint.returnNumber == tempPoint.numberOfReturns) {
/*
                                        sum_z_l += tempPoint.z;
                                        sum_i_l += tempPoint.intensity;

                                        gridPoints_z_l.add(tempPoint.z);
                                        gridPoints_i_l.add(tempPoint.intensity);

 */
                                                        gridPoints_z_l.add(tempPoint.z);
                                                        gridPoints_i_l.add(tempPoint.intensity);

                                                        sum_z_l += tempPoint.z;
                                                        sum_i_l += tempPoint.intensity;

                                                    }
                                                    if (tempPoint.returnNumber > 1 && tempPoint.returnNumber != tempPoint.numberOfReturns) {
/*
                                        sum_z_i += tempPoint.z;
                                        sum_i_i += tempPoint.intensity;
                                        gridPoints_z_i.add(tempPoint.z);
                                        gridPoints_i_i.add(tempPoint.intensity);
 */
                                                        gridPoints_z_i.add(tempPoint.z);
                                                        gridPoints_i_i.add(tempPoint.intensity);

                                                        sum_z_i += tempPoint.z;
                                                        sum_i_i+=  tempPoint.intensity;

                                                    }

                                                }


                                                    pointBuffer.writePoint(tempPoint, aR.getInclusionRule(), p);

                                                //LasPoint clonePoint = (LasPoint) tempPoint.clone();


                                                aR.p_update.lasclip_clippedPoints++;

                                                if(aR.p_update.lasclip_clippedPoints % 10000 == 0)
                                                    aR.p_update.updateProgressClip();


                                            //}

                                            npoints++;
                                            i++;
                                            //}
                                        }
                                    }else{
                                        asd.skipPointInBuffer();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    asd = null;
                    System.gc();
                //}


                /* if we want to output metrics per polyon */
                if(aR.omet){

                    ArrayList<Double> metrics_a = pCM.calc(gridPoints_z_a, gridPoints_i_a, sum_z_a, sum_i_a, "_a", colnames_a);
                    ArrayList<Double> metrics_f = pCM.calc_with_RGB(gridPoints_z_f, gridPoints_i_f, sum_z_f, sum_i_f, "_f", colnames_f, gridPoints_RGB_f);
                    ArrayList<Double> metrics_l = pCM.calc(gridPoints_z_l, gridPoints_i_l, sum_z_l, sum_i_l, "_l", colnames_l);
                    ArrayList<Double> metrics_i = pCM.calc(gridPoints_z_i, gridPoints_i_i, sum_z_i, sum_i_i, "_i", colnames_i);
                    aR.lCMO.writeLine(metrics_a, metrics_f, metrics_l, metrics_i, colnames_a, colnames_f, colnames_l, colnames_i, plotID.get(j).shortValue());

                }

                if (npoints != 0) {
                    aR.p_update.threadInt[1-1]++;
                } else {

                    aR.p_update.lasclip_empty++;
                }

                if (npoints == 0) {
                    //nopointplots++;
                }

                System.gc();
                System.gc();
                System.gc();
                System.gc();
                System.gc();

                aR.p_update.threadProgress[1-1]++;
                aR.p_update.updateProgressClip();
                aR.p_update.fileProgress++;
            }
        }

        aR.p_update.updateProgressClip();

        if(!aR.split)
            pointBuffer.close();
        else{
            for(int i = 0; i < outputBuffers.size(); i++)
                outputBuffers.get(i).close();
        }

    }

    public static double[] findMinMax(double[][] in){

        double minX = Double.POSITIVE_INFINITY;
        double maxX = 0.0;

        double minY = Double.POSITIVE_INFINITY;
        double maxY = 0.0;
        for(int i = 0; i < in.length; i++){

            if(in[i][0] > maxX)
                maxX = in[i][0];

            if(in[i][1] > maxY)
                maxY = in[i][1];

            if(in[i][0] < minX)
                minX = in[i][0];

            if(in[i][1] < minY)
                minY = in[i][1];

        }

        double[] output = new double[4];

        output[0] = minX;
        output[1] = maxX;
        output[2] = minY;
        output[3] = maxY;

        return output;

    }

    public static boolean isWithin(double[] extent, double x, double y){

        return x >= extent[0] && x <= extent[1] && y <= extent[3] && y >= extent[2];

    }

}
