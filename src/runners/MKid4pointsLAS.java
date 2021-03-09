package runners;

import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Set;
import java.util.Iterator;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;

import java.util.concurrent.BlockingQueue;

import LASio.*;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TIntHashSet;
import tools.*;
import utils.argumentReader;
import utils.pointCloudMetrics;
import utils.pointWriterMultiThread;
import utils.*;
class MKid4pointsLAS{


    public static class MKid4pointsOutput{

        ArrayList<String> outputHila = new ArrayList<String>();
        ArrayList<String> outputPlot = new ArrayList<String>();
        int numberOfThreads = 0;

        String hilaName = "hila.txt";
        String plotName = "plot.txt";

        int donet = 0;

        FileWriter fwHila = null; // new FileWriter("output" + System.getProperty("file.separator") + hilaName, true);
        BufferedWriter bwHila = null;//  new BufferedWriter(fwHila);
        PrintWriter outHila = null;
        FileWriter fwPlot = null;//  new FileWriter("output" + System.getProperty("file.separator") + plotName, true);
        BufferedWriter bwPlot = null;//  = new BufferedWriter(fwPlot);
        PrintWriter outPlot = null;


        public MKid4pointsOutput(){

            try{

                FileWriter fwHila = new FileWriter("output" + System.getProperty("file.separator") + hilaName, true);
                BufferedWriter bwHila = new BufferedWriter(fwHila);
                PrintWriter outHila = new PrintWriter(bwHila);

                FileWriter fwPlot = new FileWriter("output" + System.getProperty("file.separator") + plotName, true);
                BufferedWriter bwPlot = new BufferedWriter(fwPlot);
                PrintWriter outPlot = new PrintWriter(bwPlot);

            }catch(IOException e){}


        }

        public MKid4pointsOutput(String plotName2, String hilaName2){

            try{

                FileWriter fwHila = new FileWriter("output" + System.getProperty("file.separator") + hilaName2, true);
                BufferedWriter bwHila = new BufferedWriter(fwHila);
                PrintWriter outHila = new PrintWriter(bwHila);

                BufferedWriter bwPlot = new BufferedWriter(fwPlot);
                PrintWriter outPlot = new PrintWriter(bwPlot);

            }catch(IOException e){}

        }

        public void resetThreads(){

            numberOfThreads = 0;

        }

        public void addDone(){

            donet++;

        }

        public int getDone(){

            return donet;

        }

        public void addThread(){

            numberOfThreads++;

        }

        public synchronized void addToHila(String in){

            outputHila.add(in);

        }

        public synchronized void addToPlot(String in){

            outputPlot.add(in);

        }


        public synchronized void writeLineHila(String line){

            outHila.println(line);

        }

        public synchronized void writeLinePlot(String line){

            System.out.println(line);
            this.outPlot.println(line);

        }


        public void close(){

            outPlot.close();
            outHila.close();

        }

    }


    boolean duplicate(Collection<String> input, String dup){

        return input.contains(dup);

    }


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
                //System.out.println(file.split(pathSep)[file.split(pathSep).length - 1]);
                //System.out.println(tokens[0]);
                //System.out.println("----------------------");
                if(file.split(pathSep)[file.split(pathSep).length - 1].equals(tokens[0])){

                    output[0] = Double.parseDouble(tokens[1]);
                    output[1] = Double.parseDouble(tokens[2]);
                    output[2] = Double.parseDouble(tokens[3]);
                    output[3] = Double.parseDouble(tokens[4]);

                }


            }} catch (Exception e) {}

        return output;

    }


    public static ArrayList<String> listFiles(String directory, String endsWithh){

        ArrayList<String> output = new ArrayList<String>();


        File[] files3 = new File(directory).listFiles();        //Haetaan tekstitiedostojen polut
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

        RunId4pointsLAS.output.resetThreads();

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


//homma.pair(countx, county)
                        long code1 = homma.pair(temp[0], temp[1]);
                        long code2 = homma.pair(temp2[0], temp2[1]);

                        //combined = "" + ex + ey;
                        //combined2 = "" + mapx + mapy;
                        //System.out.println(ex);
                        String tempString = " " + LASwrite.LASpoint2String(tempPoint, oparse);;
                        outt.println(code2 + tempString + " " + code1);


                    }
                }

                sc.close();
                outt.close();
            } catch( IOException ioException ){}

            //System.out.print("HilaFile " + (i+1) + " out of " + pilvi1.size() + "\r");

        }
    }

    public static int countLines_no_output(String filename) throws IOException {

        InputStream is = new BufferedInputStream(new FileInputStream(filename));

        try {

            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean empty = true;

            while ((readChars = is.read(c)) != -1) {
                empty = false;

                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            return (count == 0 && !empty) ? 1 : count;
        } finally {

            is.close();
        }
    }

    public static void sort(String inFile,String outputti){


        if(System.getProperty("os.name").equals("Linux")){

            String a = "sort --parallel=6 -k1 ";
            String b = " -o ";
            String c = outputti;
            String asd = a+inFile+b+c;

            try{
                Runtime rt = Runtime.getRuntime();
                Process proc = rt.exec(asd);
                proc.waitFor();
            }catch( Exception ex ) {}


        }
        else{
            //System.out.println(outputti);

            String g = "sort /M 100240 ";
            String k = inFile;
            String h = " /o ";
            String l = outputti;
            String conc = g + k + h + l;

            try{
                Process p = Runtime.getRuntime().exec(conc);
                p.waitFor();
                //Process proc = rt.exec(conc);

            }catch(Exception ex){}

        }

    }

    public static void indexing(String inFile, String outFile, long linet, String delim) {

        String enter = "\n";
        //System.out.println(" ");
        //System.out.println("Tiedosto: " + inFile + " linecount: " + linet);
        //System.out.println(" ");
        long lineCount = linet;
        double previous = 0.0;
        double nexti = 0.0;
        String tiedosto = inFile;

        Path file = Paths.get(tiedosto);
        String prev_x ="";
        int hj=0;
        long avain1;
        int jaotus;
        String[] tokens;
        double min_y = 999999999;
        double max_y = 0;
        String line;
        if(linet % 500 != 0)
            jaotus=500;
        else
            jaotus = 501;
        double increment=Math.round(lineCount/jaotus);
        double increment_orig=Math.round(lineCount/jaotus);

        Double[] cutoff = new Double[jaotus];
        int cutoff_ind = 0;
        long chars = 0;
        long prev_char = 0;
        ArrayList<String> indeksit = new ArrayList<String>();
        boolean tsekki = false;

        try {

            BufferedReader sc = new BufferedReader( new FileReader(inFile));



            while((line = sc.readLine())!= null){

                //chars += (long)line.getBytes().length + 1;
                chars += ((long)line.length() + enter.length());
                tokens = line.split(delim);
                prev_x = tokens[0];
                tsekki = true;
                if(Double.parseDouble(tokens[1]) > max_y) max_y = Double.parseDouble(tokens[1]);
                if(Double.parseDouble(tokens[1]) < min_y) min_y = Double.parseDouble(tokens[1]);

                if((hj >= increment || hj == 0) && cutoff_ind < 500){




                    if(hj != 0){
                        hj++;
                        increment = increment + increment_orig;


                        nexti = Double.parseDouble(tokens[0]);
                        previous = nexti;

                        while(previous == nexti){


                            line = sc.readLine();
                            hj++;
                            prev_char = ((long)line.length() + enter.length());
                            chars += ((long)line.length() + enter.length());
                            previous = nexti;
                            nexti = Double.parseDouble(line.split(delim)[0]);

                        }

                    }

                    if(hj == 0){

                        indeksit.add(cutoff_ind + " " + hj + " " + Double.parseDouble(tokens[0]) + " " + 0);
                        hj++;
                    }
                    else{
                        indeksit.add(cutoff_ind + " " + hj + " " + nexti + " " + (chars - prev_char));
                    }
                    cutoff_ind++;

                } else {
                    hj++;

                }




                //System.out.println((long)line.getBytes().length) + 1;

                tokens = null;
                line = null;

            }

            sc.close();


        } catch (Exception e) {}

        System.out.println(inFile);
        System.out.println(" ");

        if(indeksit.size() < 500){

            int aloitus = indeksit.size() - 1;
            //System.out.println(" ");
            //System.out.println(Integer.toString(aloitus).length());
            //System.out.println(" ");
            String substringi = indeksit.get(indeksit.size() - 1).substring((Integer.toString(aloitus).length() + 1), (indeksit.get(indeksit.size() - 1).length()));
            //System.out.println(substringi);

            while(indeksit.size() < 500){

                indeksit.add((aloitus + 1) + delim + substringi);
                aloitus++;

            }

        }
        indeksit.add(500 + " " + hj + " " + Double.parseDouble(prev_x) + " " + (-9999999));
        indeksit.add(min_y + " " + max_y + " " + -1 + " " + -1);

        if(!tsekki) System.out.println("Reading failed!!!!!!! " + "hj: " + hj + " linet: " + linet + " linecount: " + lineCount);

        outFile=outFile;
        int size=indeksit.size();
        String line2;
        try{
            FileOutputStream fos = new FileOutputStream(outFile);

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            for (int g = 0; g < size; g++) {
                line2=indeksit.get(g);
                bw.write(line2);
                bw.newLine();
            }

            bw.close();
        }
        catch( IOException ioException ) {}

    }

    public static void indexingTHEindexes(String pathi, ArrayList<String> tiedostot_indeksi, int indexCount){

        String line;
        int tiedostojen_maara_indeksi = tiedostot_indeksi.size();
        String all_index = "" + pathi + "/sorted/all.in_dex";
        try{
            FileOutputStream fos = new FileOutputStream(all_index);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

            for(int ok = 0; ok <= tiedostojen_maara_indeksi-1; ok++){    // LOOP EACH INDEX FILE AND PRINT X Y EXTENT ALONGSIDE THE FILENAME
                double min_y = 999999999;
                double max_y = 0;

                int laskuuri = 0;
                String all_index_input = "";

                try {

                    BufferedReader sc = new BufferedReader( new FileReader(tiedostot_indeksi.get(ok)));
                    //System.out.println(tiedostot_indeksi.get(ok));
                    all_index_input += tiedostot_indeksi.get(ok).split("sorted")[1].split(".index")[0] + ".txt";
                    all_index_input = all_index_input.substring(1);
                    //System.out.println(all_index_input);
                    while((line = sc.readLine())!= null){

                        String[] tokens = line.split(" ");
                        if(laskuuri == 0){
                            all_index_input += " " + tokens[2];
                        }

                        if(laskuuri == indexCount){
                            all_index_input += " " + tokens[2];
                        }

                        if(laskuuri == (indexCount+1)){
                            all_index_input += " " + tokens[0] + " " + tokens[1];
                        }

                        laskuuri++;
                    }
                    bw.write(all_index_input);
                    bw.newLine();
                    sc.close();
                } catch( IOException ioException ){}


                //System.out.println("ADSASDASDASD");
            }
            bw.close();
        } catch( IOException ioException ){}


    }

    public static double[] percentile(ArrayList<String> points){

        double cutoff = 2.0;
        ArrayList<Double> zets = new ArrayList<Double>();
        ArrayList<Double> intensity = new ArrayList<Double>();
        double[] peet = new double[38];
        Iterator<String> it = points.iterator();
        double sum = 0;
        String temp;
        while(it.hasNext()){
            temp=it.next();
            if(Double.parseDouble(temp.split(" ")[2]) >= cutoff){

                sum = sum + Double.parseDouble(temp.split(" ")[2]);
                zets.add(Double.parseDouble(temp.split(" ")[2]));
                intensity.add(Double.parseDouble(temp.split(" ")[3]));
            }
        }

        if(zets.size() != 0){
            Collections.sort(zets);
            Collections.sort(intensity);
            double min = zets.get(0);
            double max = zets.get(zets.size()-1);
            int quantile = 5;
            int quantile_int = 5;
            double size = zets.size();
            double value = 0.0;
            double sum2 = 0.0;
            int peelaskuri = 0;
            int peelaskuri_int = 19;

            for(int j = 0; j < size - 1; j++){

                if(j > size * (double)(quantile/100.0) && quantile != 100){
                    peet[peelaskuri] = zets.get(j);
                    peelaskuri++;
                    quantile += 5;
                }
                if(j > size * (double)(quantile_int/100.0) && quantile_int != 100){
                    peet[peelaskuri_int] = intensity.get(j);
                    peelaskuri_int++;
                    quantile_int += 5;
                }

            }
        }

        //peet[0] = sum;                                

        return peet;


    }

    public static double[] density(ArrayList<String> points){

        double cutoff = 2.0;
        ArrayList<Double> zets = new ArrayList<Double>();
        double[] peet = new double[6];
        peet[0] = 1;peet[1] = 1;peet[2] = 1;peet[3] = 1;peet[4] = 1;peet[5] = 1;
        Iterator<String> it = points.iterator();
        double sum = 0;
        String temp;
        while(it.hasNext()){
            temp=it.next();
            if(Double.parseDouble(temp.split(" ")[2]) >= cutoff){
                sum = sum + Double.parseDouble(temp.split(" ")[2]);
                // System.out.println(temp.split(" ")[2]);
                zets.add(Double.parseDouble(temp.split(" ")[2]));
            }
        }
        Collections.sort(zets);
        if(zets.size() > 0){
            double min = zets.get(0);
            double max = zets.get(zets.size()-1);
            double[] asd = new double[8];
            //asd[0] = 0.5;
            //asd[1] = 1.0;
            asd[0] = 2.5;
            asd[1] = 5.0;
            asd[2] = 10.0;
            asd[3] = 15.0;
            asd[4] = 20.0;
            asd[5] = 25.0;
            double size = zets.size();
            double value = 0.0;
            double sum2 = 0.0;
            int peelaskuri = 0;
            int j = 0;

            while(peelaskuri < 6){
                if(zets.get(j) >= asd[peelaskuri] || j == size-1){
                    //if(j==0){
                    peet[peelaskuri] =j / (size - 1);
                    peelaskuri++;
                    // }
                    //if(j > 0){
                    //        peet[peelaskuri] = (j-1) / (size - 1);
                    //   peelaskuri++;
                    // }

                }
                if(j < size-1)j++;
            }


        }
        //peet[0] = sum;                                

        return peet;


    }

    public static boolean pointInCircle(double[] point, double[] plotCenter,double radi){

        if(Math.sqrt(Math.pow(Math.abs(point[1]-plotCenter[1]),2.0)+Math.pow(Math.abs(point[0]-plotCenter[0]),2.0)) <= radi)
            return true;
        else{
            return false; }

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

    public static double[] statistics(ArrayList<String> points, double[] plotCoords, boolean treelist){


        double cutoff = 2.0;
        double cutoff_cover = 5.0;
        double sum_intensity = 0.0;
        ArrayList<Double> intensity = new ArrayList<Double>();
        ArrayList<Double> zets = new ArrayList<Double>();
        //ArrayList<Float> return_number = new ArrayList<Float>();
        ArrayList<String> trees = new ArrayList<String>();
        //ArrayList<String> interleaved = new ArrayList<String>();
        TreeMap<Long, Double> zetsMap = new TreeMap<Long, Double>();
        TreeMap<Long, Double> xetsMap = new TreeMap<Long, Double>();
        TreeMap<Long, Double> yetsMap = new TreeMap<Long, Double>();
        double tieto;
        double[] peet = new double[15];
        Iterator<String> it = points.iterator();
        Set<Long> avaimet;

        int cover_above = 0;
        int cover_all = 0;
        int intermediate = 0;
        String temp;
        Long avain;
        int puuluku = 0;
        double sum = 0;
        while(it.hasNext()){
            temp=it.next();
            //System.out.println(temp);
            tieto = Double.parseDouble(temp.split(" ")[2]);
            //return_number.add(Float.parseFloat(temp.split(" ")[5]));
            if(tieto >= cutoff){
                sum = sum + tieto;
                zets.add(tieto);

                intensity.add(Double.parseDouble(temp.split(" ")[3]));
                sum_intensity += Double.parseDouble(temp.split(" ")[3]);
            }

            if( tieto > cutoff_cover && Float.parseFloat(temp.split(" ")[5]) == 1 )
                cover_above++;

            if( Float.parseFloat(temp.split(" ")[5]) == 1 )
                cover_all++;

            if( Float.parseFloat(temp.split(" ")[4]) - Float.parseFloat(temp.split(" ")[5]) > 1 )
                intermediate++;

            if(treelist){
                avain= Long.parseLong(interleave(Integer.toBinaryString((int)Double.parseDouble(temp.split(" ")[1])),Integer.toBinaryString((int)Double.parseDouble(temp.split(" ")[0]))), 2);
                zetsMap.put(avain, Double.parseDouble(temp.split(" ")[2]));
                xetsMap.put(avain, Double.parseDouble(temp.split(" ")[0]));
                yetsMap.put(avain, Double.parseDouble(temp.split(" ")[1]));
            }
        }

        //canopy cover
        peet[7] = (double)cover_above / (double)cover_all;


        if(zets.size() != 0){

            //intermediate
            peet[8] = intermediate / (double)zets.size();


            Collections.sort(zets);
            Collections.sort(intensity);
            double min = zets.get(0);
            double max = zets.get(zets.size()-1);

            double size = zets.size();
            peet[6] = (double) puuluku;

            peet[0] = sum/size;       //mean
            double mean = peet[0];

            double temp2 = 0;
            for(double a :zets)
                temp2 += (a-mean)*(a-mean);
            peet[1] = temp2/size;                 //VARIANCE

            peet[2] = Math.sqrt(peet[1]);              //STD

            peet[9] = sum_intensity / (double)intensity.size(); //intensity mean

            double temp3 = 0;
            int count1 = 0;
            for(double a2 :intensity){
                temp3 += (a2-peet[9])*(a2-peet[9]);
                count1++;
            }

            peet[10] = temp3/count1;                 //VARIANCE

            peet[11] = Math.sqrt(peet[10]);  //intensity std



            peet[12] = intensity.get(0);			//intensity min
            peet[13] = intensity.get(intensity.size()-1);	//intensity max

            peet[14] = max;

            if (size % 2 == 0)
            {
                peet[3] = (zets.get(((int)size / 2) - 1) + zets.get((int)size / 2)) / 2.0;  //MEDIAN
            }
            else
            {
                peet[3] = zets.get((int)size / 2);   //MEDIAN
            }

            peet[4] = 3.0*(peet[0] - peet[3])/(double)zets.size();   // Skewness
            double summa = 0.0;

            int above = 0;


            for(int i = 0; i < zets.size() - 1; i++){
                summa += Math.pow(zets.get(i)-peet[0],4);

                if(zets.get(i) > cutoff_cover)
                    above++;
            }
            peet[5] = summa / Math.pow(peet[2],4) / (double)zets.size();    // kurtosis




            peet[6]	= above / (double)zets.size();				// canopy density


            //if(sum_intensity / (double)intensity.size() >= 10 && peet[3] > 10)peet[9] = 1;
            //	else{ peet[9] = 0; }

        }
        return peet;
    }

    public static String interleave(String one, String two){
        int pituus=one.length();
        int pituus2=two.length();
        String storage="";
        String binny;
        for(int t=0;t < pituus;t++){
            binny=one.substring(0+t, 1+t);
            storage += (String.valueOf(binny));
            if(t < pituus2){binny=two.substring(0+t, 1+t);
                storage += (String.valueOf(binny));}
        }
        return storage;

    }


    public static boolean testIntersection(Shape shapeA, Shape shapeB) {
        Area areaA = new Area(shapeA);
        areaA.intersect(new Area(shapeB));
        return !areaA.isEmpty();
    }

    public static ArrayList<double[][]> readPolygonsFromWKT(String fileName, ArrayList<Integer> plotID1){

        ArrayList<double[][]> output = new ArrayList<>();
        String line1;

        try{
            BufferedReader sc = new BufferedReader( new FileReader(new File(fileName)));
            sc.readLine();
            while((line1 = sc.readLine())!= null){

                //System.out.println(line1);

                String[] tokens =  line1.split(",");

                if(Double.parseDouble(tokens[tokens.length - 1]) != -999){

                    plotID1.add(Integer.parseInt(tokens[tokens.length - 1]));

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
                }

            }


        }catch( IOException ioException ) {}

        return output;
    }

    public void clipPlots(String coords, int shapeType, Double dimension, ArrayList<String> pilvi,String outieFile,
                                 ArrayList<String> indeksitiedosto, String delim, String outShape, int part, int cores, boolean statCalc, String output_txt_points,
                                 String all_index, double buffer, String plotOutName, String oparse, String output, boolean split, String otype,
                                 RunId4pointsLAS.FileOutput foutti, BlockingQueue<String> queue, BlockingQueue<LasPoint> queueLAS, argumentReader aR, BlockingQueue<byte[]> que,
                          LASraf mergeOutput, ArrayList<BlockingQueue<byte[]>> qu, BlockingQueue<Integer> threadWrote, pointWriterMultiThread pwrite,
                          ArrayList<pointWriterMultiThread> outputFiles)throws Exception{



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
        LasPointBufferCreator pointBuffer = null;

        if(aR.split){
            for(int i = 0; i < outputFiles.size(); i++)
                outputBuffers.add(new LasPointBufferCreator(templateCloud.pointDataRecordLength, part, outputFiles.get(i)));
        }
        else
            pointBuffer = new LasPointBufferCreator(templateCloud.pointDataRecordLength, part, pwrite);

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


            }catch( IOException ioException ) {}
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
                dimension = plot_radi1.get(j);

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
                                            String tempString = " " + LASwrite.LASpoint2String(tempPoint, oparse);

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
                        System.out.println("");
                    }
                    catch( InterruptedException e ){
                        System.out.println(e.getMessage());
                        System.out.println("");
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

        int debugCounter = 0;
        /* Shapefile */
        if(shapeType == 2) {

            ArrayList<Integer> plotID;

            ArrayList<double[][]> polyBank1 = new ArrayList<double[][]>();
            ArrayList<double[][]> polyBank = new ArrayList<double[][]>();
            ArrayList<double[][]> polyBank_for_indexing = new ArrayList<double[][]>();

            polyBank1 = readPolygonsFromWKT(coords, plotID1);
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

                aR.p_update.threadFile[part-1] = plotID.get(j).toString();

                //System.gc();

                TIntHashSet doneIndexes = new TIntHashSet();
                //HashSet<Integer> doneIndexes2 = new HashSet<>();

                //HashSet<String> pisteMappi = new HashSet<String>();

                double[][] tempPolygon = polyBank.get(j);

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

                double[] minmaxXY = findMinMax(tempPolygon);

                ArrayList<Integer> valinta = new ArrayList<Integer>();

                for (int th = 0; th < pilvi.size(); th++) {

                    double[] extentti2 = new double[4];
                    extentti2[0] = all_min_x.get(th);
                    extentti2[1] = all_max_x.get(th);
                    extentti2[2] = all_min_y.get(th);
                    extentti2[3] = all_max_y.get(th);

                    if (buffer == 0.0) {

                        if (isWithin(extentti2, minmaxXY[0], minmaxXY[3]) || isWithin(extentti2, minmaxXY[0], minmaxXY[2]) ||
                                isWithin(extentti2, minmaxXY[1], minmaxXY[3]) || isWithin(extentti2, minmaxXY[1], minmaxXY[2]))
                            valinta.add(th);

                    } else {

                        if (minmaxXY[1] >= (all_min_x.get(th) + buffer) && minmaxXY[0] <= (all_max_x.get(th) - buffer) && minmaxXY[3] >= (all_min_y.get(th) + buffer) && minmaxXY[2] <= (all_max_y.get(th) - buffer))
                            valinta.add(th);

                    }

                }
                if (valinta.size() == 0) {

                }
                npoints = 0;



                for (int va = 0; va < valinta.size(); va++) {


                    doneIndexes.clear();
                    LASReader asd = new LASReader(aR.inputFiles.get(valinta.get(va))); //pointClouds.get(valinta.get(va));

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
                                            readPoints++;
                                            doneIndexes.add(p);
                                            //asd.readRecord(p, tempPoint);

                                            haku[0] = tempPoint.x;
                                            haku[1] = tempPoint.y;

                                            if (pointInPolygon(haku, tempPolygon)) {
                                                //if (true) {

                                                if (otype.equals("las")) {

                                                    tempPoint.pointSourceId = plotID.get(j).shortValue();


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


                                                    if(!aR.split)
                                                        pointBuffer.writePoint(tempPoint, aR.getInclusionRule(), (int)p);
                                                    else
                                                        outputBuffers.get(valinta.get(va)).writePoint(tempPoint, aR.getInclusionRule(), (int)p);

                                                    aR.p_update.lasclip_clippedPoints++;

                                                    if(aR.p_update.lasclip_clippedPoints % 10000 == 0)
                                                        aR.p_update.updateProgressClip();

                                                } else if (otype.equals("txt")) {
                                                    String tempString = " " + LASwrite.LASpoint2String(tempPoint, oparse);
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

                                    //System.out.println((p+s) + " " + va);





                                        haku[0] = tempPoint.x;
                                        haku[1] = tempPoint.y;

                                        if (pointInPolygon(haku, tempPolygon)) {

                                            doneIndexes.add( (p + s));

                                            if (otype.equals("las")) {

                                                tempPoint.pointSourceId = plotID.get(j).shortValue();
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
                                        gridPoints_i_f.add(tempPoint.intensity);

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

                                                if(!aR.split)
                                                    pointBuffer.writePoint(tempPoint, aR.getInclusionRule(), (int) p);
                                                else
                                                    outputBuffers.get(valinta.get(va)).writePoint(tempPoint, aR.getInclusionRule(), (int)p);
                                                //LasPoint clonePoint = (LasPoint) tempPoint.clone();


                                                aR.p_update.lasclip_clippedPoints++;

                                                if(aR.p_update.lasclip_clippedPoints % 10000 == 0)
                                                    aR.p_update.updateProgressClip();


                                            } else if (otype.equals("txt")) {
                                                String tempString = " " + LASwrite.LASpoint2String(tempPoint, oparse);
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
                }


                /** if we want to output metrics per polyon */
                if(aR.omet){

                    ArrayList<Double> metrics_a = pCM.calc(gridPoints_z_a, gridPoints_i_a, sum_z_a, sum_i_a, "_a", colnames_a);
                    ArrayList<Double> metrics_f = pCM.calc_with_RGB(gridPoints_z_f, gridPoints_i_f, sum_z_f, sum_i_f, "_f", colnames_f, gridPoints_RGB_f);
                    ArrayList<Double> metrics_l = pCM.calc(gridPoints_z_l, gridPoints_i_l, sum_z_l, sum_i_l, "_l", colnames_l);
                    ArrayList<Double> metrics_i = pCM.calc(gridPoints_z_i, gridPoints_i_i, sum_z_i, sum_i_i, "_i", colnames_i);
                    aR.lCMO.writeLine(metrics_a, metrics_f, metrics_l, metrics_i, colnames_a, colnames_f, colnames_l, colnames_i, plotID.get(j).shortValue());

                }

                if (npoints != 0) {
                    aR.p_update.threadInt[part-1]++;
                } else {

                    aR.p_update.lasclip_empty++;
                }

                if (npoints == 0) {
                    //nopointplots++;
                }
               // RunId4pointsLAS.proge.updateCurrent(1);
               // RunId4pointsLAS.proge.print();
               // RunId4pointsLAS.output.addDone();
                //pisteet.clear();

                System.gc();
                System.gc();
                System.gc();
                System.gc();
                System.gc();

                aR.p_update.threadProgress[part-1]++;
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


    public static String concatString(LasPoint point, String oparse){

        char[] array = oparse.toCharArray();

        String output = "";

        if(oparse.equals("all")){

            output += " " + point.x;

            output += " " + point.y;

            output += " " + point.z;

            output += " " + point.intensity;

            output += " " + point.classification;

            output += " " + (double)point.gpsTime;

            output += " " + point.numberOfReturns;

            output += " " + point.returnNumber;

        }

        for(int i = 0; i < array.length; i++){

            if(array[i] == ('x'))
                output += " " + point.x;

            if(array[i] == ('y'))
                output += " " + point.y;

            if(array[i] == ('z'))
                output += " " + point.z;

            if(array[i] == ('i'))
                output += " " + point.intensity;

            if(array[i] == ('c'))
                output += " " + point.classification;

            if(array[i] == ('t'))
                output += " " + (double)point.gpsTime;

            if(array[i] == ('n'))
                output += " " + point.numberOfReturns;

            if(array[i] == ('r'))
                output += " " + point.returnNumber;

            if(array[i] == ('s'))
                output += " " + 0;
        }
        return output;

    }

    public static double[] findMinMax(double[][] in){

        double minX = Double.POSITIVE_INFINITY;
        double maxX = 0.0;

        double minY = Double.POSITIVE_INFINITY;
        double maxY = 0.0;
        //System.out.println(in.length);
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

        if(x >= extent[0] && x <= extent[1] && y <= extent[3] && y >= extent[2])
            return true;

        return false;

    }

    public static void sortData(String directory){

        String pathSep2 = System.getProperty("file.separator");

        ArrayList<String> tiedostot_pilvi = new ArrayList<String>();
        File[] files = new File(directory).listFiles(); 		//Haetaan tekstitiedostojen polut
        File[] files_sorted = new File("" + directory + pathSep2 + "sorted").listFiles(); 		//Haetaan tekstitiedostojen polut
        int a = 0;
        for(File file : files){   //READ THE POINT CLOUD FILEPATHS
            if(file.isFile()){
                if(file.getName().endsWith((".txt"))){
                    tiedostot_pilvi.add(a,file.getAbsolutePath());
                    a++;
                }
            }
        }

        for(File file2: files_sorted)
            if (!file2.isDirectory())
                file2.delete();
        int tiedostojen_maara = tiedostot_pilvi.size();

        for(int k = 0; k <= tiedostojen_maara-1; k++){
            File uusi = new File(tiedostot_pilvi.get(k));
            String direct = uusi.getParentFile().getName();
            //System.out.println(direct);
            int one = 10;
            int two = 0;
            System.out.print("Sorting files, progress: " + (k+1) + pathSep2 + tiedostojen_maara + "\r");
            String inputti = tiedostot_pilvi.get(k);
            String vali = "" + direct + pathSep2 + "sorted" + pathSep2;
            String outputti = tiedostot_pilvi.get(k).split(direct)[0] + vali + tiedostot_pilvi.get(k).split(direct)[1];
            //System.out.println(outputti);

            sort(inputti, outputti);

            try{
                Thread.sleep(200);
                while(one != two){

                    one = countLines_no_output(inputti);
                    two = countLines_no_output(outputti);

                }


                two = countLines_no_output(outputti);

            } catch (Exception e) {}

            indexing(outputti,(outputti.split("sorted")[0] + pathSep2 + "sorted" + pathSep2 + outputti.split("sorted")[1].split(".txt")[0] + ".index"),two, " ");

        }





    }

}
