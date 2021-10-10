package runners;

import java.io.*;
import java.util.ArrayList;
import java.io.IOException;

import com.github.mreutegg.laszip4j.laslib.LASfilter;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Layer;
import org.gdal.ogr.Feature;
import org.gdal.ogr.Geometry;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import LASio.*;
import utils.argumentReader;
import utils.fileOperations;
import utils.pointWriterMultiThread;

public class RunId4pointsLAS{

    public static fileOperations fo = new fileOperations();
    public static boolean done = false;

    public static MKid4pointsLAS.MKid4pointsOutput output = new MKid4pointsLAS.MKid4pointsOutput();
    public static ThreadProgressBar proge = new ThreadProgressBar();

    public static listOfFiles tiedostoLista = new listOfFiles();

    public static class listOfFiles{

        ArrayList<String> files = new ArrayList<String>();


        public listOfFiles(){


        }

        public synchronized void add(ArrayList<String> in){

            files.addAll(in);

        }

    }


    public static class ThreadProgressBar{

        int current = 0;
        int end = 0;
        String name = "give me name!";
        int numberOfThreads = 0;

        public ThreadProgressBar(){

        }

        public synchronized void setEnd(int newEnd){
            end = newEnd;
        }

        public synchronized void updateCurrent(int input){

            current += input;

        }

        public synchronized void reset(){

            current = 0;
            numberOfThreads = 0;
            end = 0;
            name = "give me name!";

        }

        public void setName(String nimi){
            //System.out.println("Setting name to");
            name = nimi;

        }

        public void addThread(){

            numberOfThreads++;

        }

        public synchronized void print(){
            //System.out.println(end);
            progebar(end, current, " " + name);
            //System.out.println(end + " " + current);
        }

    }

    public static void progebar(int paatos, int proge, String nimi) {
        System.out.print("\033[2K"); // Erase line content
        if(proge < 0.05*paatos)System.out.print(nimi + "   |                    |\r");
        if(proge >= 0.05*paatos && proge < 0.10*paatos)System.out.print(nimi + "   |#                   |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.10*paatos && proge < 0.15*paatos)System.out.print(nimi + "   |##                  |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.15*paatos && proge < 0.20*paatos)System.out.print(nimi + "   |###                 |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.20*paatos && proge < 0.25*paatos)System.out.print(nimi + "   |####                |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.25*paatos && proge < 0.30*paatos)System.out.print(nimi + "   |#####               |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.30*paatos && proge < 0.35*paatos)System.out.print(nimi + "   |######              |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.35*paatos && proge < 0.40*paatos)System.out.print(nimi + "   |#######             |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.40*paatos && proge < 0.45*paatos)System.out.print(nimi + "   |########            |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.45*paatos && proge < 0.50*paatos)System.out.print(nimi + "   |#########           |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.50*paatos && proge < 0.55*paatos)System.out.print(nimi + "   |##########          |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.55*paatos && proge < 0.60*paatos)System.out.print(nimi + "   |###########         |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.60*paatos && proge < 0.65*paatos)System.out.print(nimi + "   |############        |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.65*paatos && proge < 0.70*paatos)System.out.print(nimi + "   |#############       |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.70*paatos && proge < 0.75*paatos)System.out.print(nimi + "   |##############      |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.75*paatos && proge < 0.80*paatos)System.out.print(nimi + "   |###############     |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.80*paatos && proge < 0.85*paatos)System.out.print(nimi + "   |################    |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.85*paatos && proge < 0.90*paatos)System.out.print(nimi + "   |#################   |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.90*paatos && proge < 0.95*paatos)System.out.print(nimi + "   |##################  |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.95*paatos && proge < 0.97*paatos)System.out.print(nimi + "   |################### |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.97*paatos && proge <= 1*paatos)System.out.print(nimi + "   |####################|  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");


    }

    public static void mergeFiles(File[] files, File mergedFile) {

        FileWriter fstream = null;
        BufferedWriter out = null;

        try {
            fstream = new FileWriter(mergedFile, true);
            out = new BufferedWriter(fstream);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        for (File f : files) {
            //System.out.println("merging: " + f.getName());
            FileInputStream fis;
            try {
                fis = new FileInputStream(f);
                BufferedReader in = new BufferedReader(new InputStreamReader(fis));

                String aLine;
                while ((aLine = in.readLine()) != null) {
                    out.write(aLine);
                    out.newLine();
                }

                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    static class MoniSaieAjo_HilaLaskenta implements Runnable{

        private final int j;
        private final int k;
        private final ArrayList<String> tiedostot;
        private final double[] topleft;
        private final double cellsize;
        private final String outname;
        private final int partionsize;
        private final double buffer_size;
        private final String index_all;
        private final String oparse;
        private final String output;

        public MoniSaieAjo_HilaLaskenta (int threadnumber, int corenumber, ArrayList<String> tiedostot2, double[] topleft2,
                                         double cellsize2, String outname2, int partionsize2 , double buffer_size2, String index_all2, String oparse2, String output2)
        {
            j = threadnumber;
            k = corenumber;
            tiedostot = tiedostot2;
            topleft = topleft2;
            cellsize = cellsize2;
            outname = outname2;
            partionsize = partionsize2;
            buffer_size = buffer_size2;
            index_all = index_all2;

            oparse = oparse2;
            output = output2;
        }

        public void run() {

            try {
                MKid4pointsLAS homma = new MKid4pointsLAS();
                String delimiter = " ";
                //System.out.println(outname);
                homma.hilaJako(tiedostot, index_all, topleft, cellsize, outname, partionsize,j, k, buffer_size, oparse, output);

            } catch (Exception e) {
                System.out.println(e);
                //System.out.println(e.getMessage());

            }

        }

    }

    public static class WriterThreadLAS extends Thread{



        protected BlockingQueue<LasPoint> blockingQueue = null;
        private File outFile;

        private LASraf raOutput;
        private PointInclusionRule rule;

        private LASReader tempReader;
        BlockingQueue<byte[]> que;

        ArrayList<BlockingQueue<byte[]>> thread_ques;
        BlockingQueue<Integer> threadWrote;
        boolean end = false;
        int takeInt;

        public WriterThreadLAS(BlockingQueue<LasPoint> blockingQueue, File outFile2, PointInclusionRule ruleIn, LASReader tempReader1, BlockingQueue<byte[]> que, ArrayList<BlockingQueue<byte[]>> thread_ques,
                               BlockingQueue<Integer> threadWrote){

            this.blockingQueue = blockingQueue;
            this.outFile = outFile2;
            this.rule = ruleIn;
            this.tempReader = tempReader1;
            this.que = que;
            this.thread_ques = thread_ques;
            this.threadWrote = threadWrote;

        }

        @Override
        public void run() {


            //long start = System.currentTimeMillis();
            //long end = System.currentTimeMillis();

            long averageTime = 0L;
            PrintWriter writer = null;
            int pointCount = 0;

            byte[] take;

            try {

                raOutput = new LASraf(outFile);
                LASwrite.writeHeader(raOutput, "lasclip", tempReader.versionMajor, tempReader.versionMinor,
                        tempReader.pointDataRecordFormat, tempReader.pointDataRecordLength,
                        tempReader.headerSize, tempReader.offsetToPointData, tempReader.numberVariableLengthRecords,
                        tempReader.fileSourceID, tempReader.globalEncoding,
                        tempReader.xScaleFactor, tempReader.yScaleFactor, tempReader.zScaleFactor,
                        tempReader.xOffset, tempReader.yOffset, tempReader.zOffset);

                while(!this.end){

                    System.out.println("LOOPPI!! " + threadWrote.size());
                    //LasPoint buffer = blockingQueue.take();
                    //take = que.take();

                    takeInt = threadWrote.take();
/*
                    if(take.length == 1){
                        break;
                    }
*/

                    if(takeInt == -99)
                        break;

                    raOutput.write(thread_ques.get(takeInt).take(), tempReader.pointDataRecordLength);

                    pointCount++;
                    take = null;
/*
                    //Check whether end of file has been reached
                    if(buffer.z == -987654321){
                        break;
                    }

                    if(raOutput.writePoint( buffer, rule, 0.01, 0.01, 0.01, 0, 0, 0, tempReader.pointDataRecordFormat, pointCount)){
                        //System.out.println("POINT WROTE");
                        pointCount++;
                    }

                    buffer = null;
                    */
                    //if(pointCount % 10 == 0) {
                        //System.out.println(blockingQueue.size());
                        //System.gc();
                    //}
                }


            } catch (Exception e) {

                e.printStackTrace();
            } finally{

                try{
                    //raOutput.writeBuffer2();
                    raOutput.updateHeader2();
                    raOutput.close();
                }catch(Exception e){

                    e.printStackTrace();

                }

            }

            //System.out.println()


        }

        public void end(){
            this.end = true;
        }

    }

    public static class WriterThread implements Runnable{

        protected BlockingQueue<String> blockingQueue = null;
        private File outFile;

        public WriterThread(BlockingQueue<String> blockingQueue, File outFile2){

            this.blockingQueue = blockingQueue;
            this. outFile = outFile2;

        }

        @Override
        public void run() {
            PrintWriter writer = null;

            try {
                writer = new PrintWriter(new File(outFile.getAbsolutePath()));

                while(true){
                    String buffer = blockingQueue.take();
                    //Check whether end of file has been reached
                    if(buffer.equals("EOF")){
                        break;
                    }
                    writer.println(buffer);
                }


            } catch (FileNotFoundException e) {

                e.printStackTrace();
            } catch(InterruptedException e){

            }finally{
                writer.close();
            }

        }

    }

    public static class FileOutput{

        File outFile = null;

        FileWriter fw = null;
        BufferedWriter bw = null;//
        PrintWriter out = null;

        int threads = 0;


        public FileOutput(){

        }

        public FileOutput(String outName){

            this.outFile = new File(outName);

            if(outFile.exists()){

                outFile = fo.createNewFileWithNewExtension(outFile, "_1.txt");
                // new File(outFile.getAbsolutePath().replaceFirst("[.][^.]+$", "") + "_1.txt");
                //temp.delete();
            }

            try{

                FileWriter fw = new FileWriter(outName, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw);


            }catch(IOException e){}

        }

        public synchronized void addThread(){

            threads++;

        }

        public int numberOfThreads(){

            return threads;

        }

        public synchronized void writeLine(String line){

            out.println(line);

        }

        public void close(){

            out.close();

        }

    }


    static class MoniSaieAjo_clipPlots extends Thread{

        private final int j;
        private final int k;

        private final String koealat;
        private final ArrayList<String> tiedostot_sorted;
        private final ArrayList<String> tiedostot_indeksi;
        private final String delimiter;
        private final String indeksi_pathi_all;
        private final double buffer_size;
        private final String nimi1;
        private final String plotOutName;

        private final int shape;
        private final String oparse;
        private final String output;
        private final boolean split;

        private final String otype;

        private final FileOutput foutti;
        private final BlockingQueue<String> queue;
        private final BlockingQueue<LasPoint> queueLAS;
        private final argumentReader aR;
        private BlockingQueue<byte[]> que;
        LASraf mergeOutput;

        ArrayList<pointWriterMultiThread> outputFiles;
        ArrayList<BlockingQueue<byte[]>> qu;
        BlockingQueue<Integer> threadWrote;
        pointWriterMultiThread pwrite;

        public MoniSaieAjo_clipPlots (int threadnumber, int corenumber, String koealatiedosto, ArrayList<String> sorted_files,
                                      ArrayList<String> indeksi_tiedosto, String erotin, String all_index, double bufferi, String nimi, String plotname, int shape2, String oparse2, String output2,
                                      boolean split2, String otype2, FileOutput foutti2, BlockingQueue<String> queue2, BlockingQueue<LasPoint> queueLAS2, argumentReader aR, BlockingQueue que,
                                      LASraf mergeOutput, ArrayList<BlockingQueue<byte[]>> qu, BlockingQueue<Integer> threadWrote, pointWriterMultiThread pwrite, ArrayList<pointWriterMultiThread> outputFiles){
            j = threadnumber;
            k = corenumber;
            koealat = koealatiedosto;
            tiedostot_sorted = sorted_files;
            tiedostot_indeksi = indeksi_tiedosto;
            delimiter = erotin;
            indeksi_pathi_all = all_index;
            buffer_size = bufferi;
            nimi1 = nimi;
            plotOutName = plotname;

            shape = shape2;
            oparse = oparse2;
            output = output2;

            split = split2;
            otype = otype2;
            foutti = foutti2;
            queue = queue2;
            queueLAS = queueLAS2;
            this.aR = aR;
            this.que = que;
            this.mergeOutput = mergeOutput;

            this.qu = qu;
            this.threadWrote = threadWrote;
            this.pwrite = pwrite;
            this.outputFiles = outputFiles;
        }

        public void run() {

            try {
                // int lineCount = countLines("test.txt");
                MKid4pointsLAS homma = new MKid4pointsLAS();
                String delimiter = " ";
                boolean statCalc = false;
                //System.out.println(indeksi_pathi_all);
                homma.clipPlots(koealat, shape , 9.0 , tiedostot_sorted, "outti.txt" , tiedostot_indeksi,delimiter, "plots.shp",j,k,
                        false,"testi_clipatut.txt",indeksi_pathi_all, buffer_size, plotOutName, oparse, output, split, otype, foutti, queue,
                        queueLAS, aR, que, mergeOutput, qu, threadWrote, pwrite, outputFiles);


            } catch (Exception e) {

                e.printStackTrace(System.out);

            }

        }

    }

    public static int detectShapeType(String in){

        File tiedostoCoord = new File(in);

        tiedostoCoord.setReadable(true);

        String line1;

        if(tiedostoCoord.getName().contains(".shp"))
            return 2;

        try{
            BufferedReader sc = new BufferedReader( new FileReader(tiedostoCoord));
            sc.readLine();
            while((line1 = sc.readLine())!= null){

                //System.out.println(line1);
                if(line1.contains("POLYGON (("))
                    return 2;
                if(line1.split(" ").length == 4)
                    return 1;

            }
        }catch( IOException ioException ) {}
        return -99;
    }

    static class multiTXT2LAS implements Runnable{

        private final String parse;
        ArrayList<String> tiedostot;
        ArrayList<String> returnList = new ArrayList<String>();
        int numberOfCores;
        int coreNumber;

        public multiTXT2LAS (ArrayList<String> tiedostot2, String parse2, int numberOfCores2, int coreNumber2)
        {

            tiedostot = tiedostot2;
            parse = parse2;
            numberOfCores = numberOfCores2;
            coreNumber = coreNumber2;

        }

        public ArrayList<String> getList(){

            return returnList;

        }

        public void run() {



            try {

                int pienin = 0;
                int suurin = 0;
                if(coreNumber != 0){

                    int jako = (int)Math.ceil((double)tiedostot.size() / (double) numberOfCores);
                    //System.out.println(plotID1.size() / (double)cores);
                    if(coreNumber != numberOfCores){

                        pienin = (coreNumber - 1) * jako;
                        suurin = coreNumber * jako;
                    }

                    else{
                        pienin = (coreNumber - 1) * jako;
                        suurin = tiedostot.size();
                    }

                    tiedostot = new ArrayList<String>(tiedostot.subList(pienin, suurin));
                    //System.out.println(tiedostot);
                    //polyBank = new ArrayList<double[][]>(polyBank1.subList(pienin, suurin));

                }
                else{

                    //tiedostot = new ArrayList<Double>(tiedostot);
                    //polyBank = new ArrayList<double[][]>(polyBank1);
                }



                ArrayList<File> from = new ArrayList<File>();
                ArrayList<LASraf> to = new ArrayList<LASraf>();
                ArrayList<String> outList = new ArrayList<String>();

                for(int i = 0; i < tiedostot.size(); i++){

                    File toFile = fo.createNewFileWithNewExtension(tiedostot.get(i), ".las");
                    // new File(tiedostot.get(i).replaceFirst("[.][^.]+$", "") + ".las");
                    //System.out.println(toFile);
                    File fromFile = new File(tiedostot.get(i));

                    if(toFile.exists())
                        toFile.delete();

                    toFile.createNewFile();

                    //System.out.println(toFile);
                    from.add(fromFile);

                    to.add(new LASraf(toFile));
                    //System.exit(0);
                    outList.add(toFile.getAbsolutePath());
                    //outList.add(tiedostot.get(i).replaceFirst("[.][^.]+$", "") + ".las");

                }
                PointInclusionRule rule = new PointInclusionRule();
                tiedostoLista.add(outList);

                for(int i = 0; i < tiedostot.size(); i++){

                    //System.out.println("thread: " + coreNumber + " " + i);
                    LASwrite.txt2las(from.get(i), to.get(i), parse, "txt2las", " ", rule, false);
                    to.get(i).writeBuffer2();
                    //System.out.println(to.get(i).getFileReference().getName());

                }
                //return 1;
            } catch (Exception e) {
                //System.out.println(e);
                System.out.println(e.getMessage());

            }

        }



    }

    public static ArrayList<String> convertTxt2Las(ArrayList<String> in, String parse){

        ArrayList<String> output = new ArrayList<String>();

        for(int i = 0; i < in.size(); i++){
            String fileName = in.get(i);
            output.add(fileName.split(".txt")[0] + ".las");
            String a = "txt2las --parse " + parse + " -i " + fileName + " -o " +  (fileName.split(".txt")[0] + ".las");
            //System.out.println(a);
            //String b = "gdalwarp -t_srs EPSG:3067 test2.tif final.tif";

            final String command = a;
            Process p = null;
            try {
                p = Runtime.getRuntime().exec(command);
            } catch (final IOException e) {
                e.printStackTrace();
            }

            //Wait to get exit value
            try {
                p.waitFor();
                final int exitValue = p.waitFor();
                if (exitValue == 0){
                    //System.out.println("Successfully executed the command: " + command);
                }
                else {
                    System.out.println("Failed to execute the following command: " + command + " due to the following error(s):");
                    try (final BufferedReader b = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                        String line;
                        if ((line = b.readLine()) != null)
                            System.out.println(line);
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            proge.print();
            proge.updateCurrent(1);
        }
        return output;
    }


    public static void main(String[] args) throws IOException {



        utils.Timer time = new utils.Timer();

        time.start();




        argumentReader aR = new argumentReader(args);

        aR.setExecDir( System.getProperty("user.dir"));


        aR.parseArguents();




        double[] origo = new double[2];
        int sortORnot = 0;//Integer.parseInt(args[2]);

        int hilaORnot = 0;
        origo[0] = 0.0;
        origo[1] = 0.0;
        String cellSize =  "";//args[7];
        String karttaLehtiKoko = "10";// args[8];

        int plotsORnot = Integer.parseInt(args[0]);

        String pathSep = System.getProperty("file.separator");

        //if(!System.getProperty("os.name").equals("Linux"))
          //  pathSep = "\\" + pathSep;

        //System.loadLibrary("opencv_java320");

        //if(!System.getProperty("os.name").equals("Linux"))
          //  System.loadLibrary("gdal202");


        //System.out.println(pathSep);
        String delimiter = " ";



        /*
        String pathi = args[4];


        int tool = Integer.parseInt(args[0]);



        String iparse = args[1];
        String oparse = args[2];

        String output = args[3];
        String input = args[4];
        //System.out.println(input);
        boolean drop_noise = Integer.parseInt(args[5]) != 0;
        int drop_classification = Integer.parseInt(args[6]);

        boolean first_only = Integer.parseInt(args[7]) != 0;
        boolean keep_first = Integer.parseInt(args[8]) != 0;
        boolean drop_first = Integer.parseInt(args[9]) != 0;

        boolean last_only = Integer.parseInt(args[10]) != 0;
        boolean keep_last = Integer.parseInt(args[11]) != 0;
        boolean drop_last = Integer.parseInt(args[12]) != 0;

        boolean drop_first_of_many = Integer.parseInt(args[13]) != 0;
        boolean drop_last_of_many = Integer.parseInt(args[14]) != 0;

        boolean keep_middle = Integer.parseInt(args[15]) != 0;
        boolean drop_middle = Integer.parseInt(args[16]) != 0;

        boolean keep_single = Integer.parseInt(args[17]) != 0;
        boolean drop_single = Integer.parseInt(args[18]) != 0;
        boolean keep_double = Integer.parseInt(args[19]) != 0;
        boolean drop_double = Integer.parseInt(args[20]) != 0;
        boolean keep_triple = Integer.parseInt(args[21]) != 0;
        boolean drop_triple = Integer.parseInt(args[22]) != 0;
        boolean keep_quadruple = Integer.parseInt(args[23]) != 0;
        boolean drop_quadruple = Integer.parseInt(args[24]) != 0;
        boolean keep_quintuple = Integer.parseInt(args[25]) != 0;
        boolean drop_quintuple = Integer.parseInt(args[26]) != 0;

        boolean drop_synthetic = Integer.parseInt(args[27]) != 0;
        int drop_user_data = Integer.parseInt(args[28]);
        int keep_user_data = Integer.parseInt(args[29]);

        int set_classification = Integer.parseInt(args[30]);
        int set_user_data = Integer.parseInt(args[31]);
        double drop_z_below = Double.parseDouble(args[32]);
        double drop_z_above = Double.parseDouble(args[33]);

        double buffer = Double.parseDouble(args[34]);

        String odir = args[35];

        int few = Integer.parseInt(args[36]);

        double step = Double.parseDouble(args[37]);

        cellSize = args[37];

        int ground_class = Integer.parseInt(args[38]);

        String method = args[39];

        String sep = args[40];

        int numberOfCores = Integer.parseInt(args[41]);

        String otype = args[42];

        int splitInt = Integer.parseInt(args[43]);

        double numarg2 = Double.parseDouble(args[44]);
        double numarg3 = Double.parseDouble(args[45]);
        String groundPoints = args[46]; // NUMARG4


        int br = Integer.parseInt(args[47]);



        String koealat = args[48];


         */

        String koealat = aR.poly;

        boolean reading = (aR.br == 0);
        PointInclusionRule rule = aR.getInclusionRule();

        File shapeFile = new File(koealat);

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();

        int shapeType = -1;

        File fout = null;

        int checkShp = 0;



        try{


            Thread.sleep(0);

        }catch(InterruptedException e){}

        if(shapeFile.getName().contains(".shp")){

            checkShp = 1;
            DataSource ds = ogr.Open( koealat );
            //System.out.println("Layer count: " + ds.GetLayerCount());
            Layer layeri = ds.GetLayer(0);
            //System.out.println("Feature count: " + layeri.GetFeatureCount());

            try{
                fout = new File("tempWKT.csv");

                if(fout.exists())
                    fout.delete();

                fout.createNewFile();

                FileOutputStream fos = new FileOutputStream(fout);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

                bw.write("WKT,plot_id");
                bw.newLine();

                for(long i = 0; i < layeri.GetFeatureCount(); i++ ){

                    Feature tempF = layeri.GetFeature(i);
                    //System.out.println(layeri.GetFeatureCount());
                    Geometry tempG = tempF.GetGeometryRef();

                    if(tempG == null)
                        continue;

                    String id = "";

                    if(tempF.GetFieldCount() > 0)
                        id = tempF.GetFieldAsString(aR.field);
                    else
                        id = String.valueOf(i);

                    String out = "\"" + tempG.ExportToWkt() + "\"," + id;

                    bw.write(out);
                    bw.newLine();


                }

                bw.close();
            }catch(IOException e){

            }

            koealat = "tempWKT.csv";
            shapeType = 2;

        }

        else
            shapeType = detectShapeType(koealat);


        /*
        if(drop_noise)
            rule.dropNoise();
        if(drop_classification != -999)
            rule.dropClassification(drop_classification);
        if(drop_classification != -999)
            rule.dropClassification(drop_classification);
        if(first_only)
            rule.firstOnly();
        if(keep_first)
            rule.keepFirst();
        if(drop_first)
            rule.dropFirst();
        if(last_only)
            rule.lastOnly();
        if(keep_last)
            rule.keepLast();
        if(drop_last)
            rule.dropLast();
        if(drop_first_of_many)
            rule.dropFirstOfMany();
        if(drop_last_of_many)
            rule.dropLastOfMany();
        if(keep_middle)
            rule.keepMiddle();
        if(drop_middle)
            rule.dropMiddle();
        if(keep_single)
            rule.keepSingle();
        if(drop_single)
            rule.dropSingle();
        if(keep_double)
            rule.keepDouble();
        if(drop_double)
            rule.dropDouble();
        if(keep_triple)
            rule.keepTriple();
        if(drop_triple)
            rule.dropTriple();
        if(keep_quadruple)
            rule.keepQuadruple();
        if(drop_quadruple)
            rule.dropQuadruple();
        if(keep_quintuple)
            rule.keepQuintuple();
        if(drop_quintuple)
            rule.dropQuintuple();
        if(drop_synthetic)
            rule.dropSynthetic();
        if(drop_user_data != -999)
            rule.dropUserData(drop_user_data);
        if(keep_user_data != -999)
            rule.keepUserData(keep_user_data);
        if(set_classification != -999)
            rule.setClassification(set_classification);
        if(set_user_data != -999)
            rule.setUserData(set_user_data);
        if(drop_z_below != -999)
            rule.dropZBelow(drop_z_below);
        if(drop_z_above != -999)
            rule.dropZAbove(drop_z_above);

        */
        // cellSize = step;

        //System.out.println(pathi.split(pathSep)[(pathi.split(pathSep)).length - 1].split("\\.")[1]);

        //boolean lasFormat = pathi.split(pathSep)[(pathi.split(pathSep)).length - 1].split("\\.")[1].equals("las");

        //boolean txtFormat = pathi.split(pathSep)[(pathi.split( pathSep)).length - 1].split("\\.")[1].equals("txt");

        //boolean wildCard = pathi.split(pathSep)[(pathi.split(pathSep)).length - 1].split("\\.")[0].equals("*");

        boolean lasFormat;// = aR.files[0].split("\\.")[1].equals("las");
        boolean txtFormat;// = aR.files[0].split("\\.")[1].equals("txt");

        lasFormat = new File(aR.files[0]).getName().split("\\.")[1].equals("las");
        //lasFormat = aR.files[0].split("\\.")[1].equals("las");
        txtFormat = new File(aR.files[0]).getName().split("\\.")[1].equals("txt");

        //System.out.println(pathi.split(pathSep)[(pathi.split(pathSep)).length - 1]);
        //System.out.println(wildCard + " " + txtFormat);

        MKid4pointsLAS homma = new MKid4pointsLAS();


        ArrayList<String> tiedostot_indeksi = new ArrayList<String>();
        ArrayList<String> tiedostot_sorted = new ArrayList<String>();

        ArrayList<LASReader> pointClouds = new ArrayList<>();
        ArrayList<File> inputFiles = new ArrayList<>();

        if(txtFormat){

            proge.setName("Converting .txt to .las ...");

            ArrayList<String> tempList = new ArrayList<String>();

            /*
            if(wildCard){
                //System.out.println(pathi.split("\\*")[0]);
                tempList = homma.listFiles(pathi.split("\\*")[0], ".txt");
                //System.out.println(tempList);
            }
            *
             */
            for(String f : aR.files)
                tempList.add(f);

            //tempList.add(pathi);


            proge.setEnd(tempList.size());

            if(aR.cores > tempList.size())
                aR.cores = tempList.size();

            //System.out.println("Number of cores: " + numberOfCores);

            ArrayList<Thread> lista11 = new ArrayList<Thread>();

            for(int ii = 1; ii <= aR.cores; ii++){

                proge.addThread();
                Thread temp = new Thread(new multiTXT2LAS(tempList, aR.iparse, aR.cores, ii));
                lista11.add(temp);
                temp.start();
            }

            for(int i = 0; i < lista11.size(); i++){

                try{

                    lista11.get(i).join();
                }catch(Exception e){}
            }
          

          /*
          ArrayList<File> from = new ArrayList<File>();
          ArrayList<LASraf> to = new ArrayList<LASraf>();

          for(int i = 0; i < tempList.size(); i++){

            File toFile = new File(tempList.get(i).replaceFirst("[.][^.]+$", "") + ".las");
            //System.out.println(toFile);
            File fromFile = new File(tempList.get(i));

            if(toFile.exists())
              toFile.delete();

            toFile.createNewFile();

            //System.out.println(toFile);
            from.add(fromFile);

            to.add(new LASraf(toFile));
            //System.exit(0);
            tiedostot_sorted.add(tempList.get(i).replaceFirst("[.][^.]+$", "") + ".las");
            
          }

          // omp parallel for
          for (int i = 0; i < 10; i++) {
            System.out.print(i);
          }
          System.out.println("");

          // omp parallel for
          for(int i = 0; i < tempList.size(); i++){
            System.out.println("thread: " + i);
            LASwrite.txt2las(from.get(i), to.get(i), iparse, "txt2las", " ", rule);
            to.get(i).writeBuffer2();
            //System.out.println(to.get(i).getFileReference().getName());
          
          }
          */

            for(int h = 0; h < tiedostoLista.files.size(); h++)
                pointClouds.add(new LASReader(new File(tiedostoLista.files.get(h))));

            tiedostot_sorted = tiedostoLista.files;


            //tiedostot_sorted = convertTxt2Las(tempList, iparse);

        }



        if(lasFormat){
/*
            if(wildCard){
                tiedostot_indeksi = homma.listFiles(pathi.split("\\*")[0], ".lasx");
                tiedostot_sorted = homma.listFiles(pathi.split("\\*")[0], ".las");
            }
*/
            //else{



            for(String f : aR.files) {
                inputFiles.add(new File(f));
                //pointClouds.add(new LASReader(new File(f)));

                tiedostot_sorted.add(f);
                File indeksi = fo.createNewFileWithNewExtension(f, ".lasx");
                if(indeksi.exists())
                    tiedostot_indeksi.add( indeksi.getAbsolutePath());
            }
                //tiedostot_sorted.add(pathi);
        /*
                File indeksi = fo.createNewFileWithNewExtension(pathi, ".lasx");
                // new File(pathi.replaceFirst("[.][^.]+$", "") + ".lasx");

                if(indeksi.exists())
                    tiedostot_indeksi.add( fo.createNewFileWithNewExtension(pathi, ".lasx").getAbsolutePath());

         */
                    //pathi.replaceFirst("[.][^.]+$", "") + ".lasx");
                //System.out.println(tiedostot_sorted);
            //}
        }


        aR.setPointClouds(pointClouds);
        aR.setInputFiles(inputFiles);
        proge.reset();

        /*
        File dir = new File("" + pathi + pathSep + "sorted");

        dir.delete();
        dir.mkdir();

        File dir2 = new File("output");
        dir2.delete();
        dir2.mkdir();


        File[] outputFiles = new File("output").listFiles();

        if(outputFiles.length > 0)
            for(File file2: outputFiles)
                if (!file2.isDirectory()) {
                    if(plotsORnot == 1 && (file2.getName().equals("plotFile.txt")))
                        file2.delete();
                    if(hilaORnot == 1 && (file2.getName().equals("hilaFile.txt")))
                        file2.delete();
                    if(aR.cores > 0 && plotsORnot == 1 && (file2.getName().equals("plotFile_moniajo.txt")))
                        file2.delete();
                }



        if(sortORnot == 1)
            homma.sortData(pathi);

         */

        String indeksi_pathi_all = "";//pathi.split(pathSep)[pathi.split(pathSep).length - 2] + pathSep + "all.lasxALL";
        //String[] tokens = aR.files[0].split(pathSep);

        indeksi_pathi_all = new File(aR.files[0]).getParent();

        //for(int i = 0; i < tokens.length - 1; i++)
           // indeksi_pathi_all += (tokens[i] + pathSep);

        String workingDir = indeksi_pathi_all;

        indeksi_pathi_all += pathSep + "all.lasxALL";
        //System.out.println(indeksi_pathi_all);
        File allFile = new File(indeksi_pathi_all);

        allFile.delete();
        //if(!allFile.exists())
        allFile.createNewFile();

        /*
        if(wildCard && lasFormat)
            origo = LASindex.indexAll3(tiedostot_sorted, indeksi_pathi_all);

        if(!wildCard && lasFormat)
            origo = LASindex.indexAll3(tiedostot_sorted, indeksi_pathi_all);

        if(txtFormat)
        */

        origo = LASindex.indexAll3(aR.inputFiles, indeksi_pathi_all);


        boolean split = (aR.numarg1 == 1);

        split = aR.split;
        proge.setName("Clipping plots...");
        proge.setEnd(100);

        File merged = null;

        File tempFile = new File(tiedostot_sorted.get(0));

        LASReader tempReader = new LASReader(new File(tiedostot_sorted.get(0)));
        pointWriterMultiThread pwrite = null;

        if(!aR.split) {

            merged = aR.createOutputFile(tempReader);

            if (merged.exists()) {
                merged.delete();
                merged.createNewFile();
            }

           pwrite = new pointWriterMultiThread(merged, tempReader, "lasClip", aR);

            aR.mergedPointCloud = merged;
        }


        ArrayList<pointWriterMultiThread> outputFiles = new ArrayList<>();

        if(aR.split){

            System.out.println(pointClouds.size());

            for(int i = 0; i < aR.inputFiles.size(); i++){

                LASReader tempFile11 = new LASReader(aR.inputFiles.get(i));
                File tempFile1 = aR.createOutputFile(tempFile11);


                LASraf tempraf = new LASraf(tempFile1);

                pointWriterMultiThread pwriteTemp = new pointWriterMultiThread(tempFile1, tempFile11, "lasClip", aR);
                outputFiles.add(pwriteTemp);

                System.out.println(tempFile1.getAbsolutePath());
            }

        }

        ArrayList<BlockingQueue<LasPoint>> quesLas = new ArrayList<>();



        FileOutput foutti = null;// new FileOutput(merged.getAbsolutePath());
        BlockingQueue<String> queue = null;
        BlockingQueue<LasPoint> queueLAS = null;

        BlockingQueue<byte[]> que = new ArrayBlockingQueue<>(10000);

        WriterThread writer = null;
        WriterThreadLAS writerLAS = null;

        Thread writerT = null;

        LASraf mergeOutput = null;

        if(!aR.split)
            mergeOutput = new LASraf(merged);

        ArrayList<BlockingQueue<byte[]>> qu = new ArrayList<>();

        for(int i = 0; i < aR.cores; i++){
            qu.add(new LinkedBlockingQueue<byte[]>());
        }

        BlockingQueue<Integer> threadWrote = new LinkedBlockingQueue<>();

        if(aR.otype.equals("txt")){

            System.out.println("OUTPUT IS TEXT!");
            queue = new LinkedBlockingQueue<String>(aR.cores * 100000 * 10);
            writer = new WriterThread(queue, merged);
            writerT = new Thread(writer);
            writerT.start();

        }
        else if(aR.otype.equals("las")){

            //queueLAS = new LinkedBlockingQueue<LasPoint>(aR.cores * 100000 * 10);
            //writerLAS = new WriterThreadLAS(queueLAS, merged, rule, tempReader, que, qu, threadWrote);

            //writerLAS.start();
            //writerT =  new Thread(writerLAS);
            //writerT.start();

        }
        else{
            System.out.println("Otype not recognized! : " + aR.otype);
            System.exit(0);
        }

        //System.exit(1);
        /*
        File outputtiFileTesti = new File("testia.las");

        if(outputtiFileTesti.exists())
            outputtiFileTesti.delete();

        outputtiFileTesti.createNewFile();

        try{
            Thread.sleep(1000);
        }catch(Exception e){
            e.printStackTrace();
        }
        LASraf asdi = new LASraf(outputtiFileTesti);

        asdi.writeHeader("testiajo", 1, 2, tempReader.pointDataRecordFormat, tempReader.pointDataRecordLength);

        aR.setOutputFile(asdi);
        */

        aR.p_update.lasclip_empty = aR.buffer;
        aR.p_update.lasclip_clippedPoints = 0;


        if(plotsORnot == 1){

            long tStart = System.currentTimeMillis();

            long tEnd = System.currentTimeMillis();
            long tDelta = tEnd - tStart;

            if(aR.omet){
                aR.lCMO.prep(aR.mergedPointCloud);
            }

            if(aR.cores > 0){

                ArrayList<Thread> lista = new ArrayList<Thread>();

                for(int ii = 1; ii <= aR.cores; ii++){

                    proge.addThread();

                    Thread temp = new MoniSaieAjo_clipPlots(ii, aR.cores, koealat, tiedostot_sorted,
                            tiedostot_indeksi, delimiter, indeksi_pathi_all, aR.buffer, ("Thread # " + ii), (ii + "temp.txt"), shapeType, aR.oparse, "output", split, aR.otype,
                            foutti, queue, queueLAS, aR, que, mergeOutput, qu, threadWrote, pwrite, outputFiles);

                    lista.add(temp);
                    temp.start();

                }

                for(int i = 0; i < lista.size(); i++){

                    try{

                        lista.get(i).join();
                    }catch(Exception e){}

                }


                if(aR.omet) {

                    aR.lCMO.closeFiles();

                }

                //try{
                    //LasPoint POISON_PILL = new LasPoint();
                    //POISON_PILL.z = -987654321;

                    //if(aR.otype.equals("txt"))
                      //  queue.put("EOF");

                    //if(aR.otype.equals("las"))
                      //  queueLAS.put(POISON_PILL);

                    //writerLAS.end();
                    //que.put(new byte[]{0});
                    //threadWrote.put(-99);
                    //writerLAS.join();

                //}catch(InterruptedException e){}
            }

            //mergeOutput.writeBuffer2();
            //mergeOutput.updateHeader2();
            //aR.outputFile.writeBuffer2();
            //aR.outputFile.updateHeader2();

        }

        if(aR.split){
            for(int i = 0; i < outputFiles.size(); i++){
                //outputFiles.get(i).close();
                outputFiles.get(i).close();
            }
        }

        if(!aR.split)
            pwrite.close();

        String outName = "hilaFile.txt";

        ArrayList<String> tiedostot_UNsorted = null;//homma.listFiles(("" + pathi), ".txt");

        if(plotsORnot == 0){
            indeksi_pathi_all = tiedostot_sorted.get(0).split("sorted")[0] + pathSep + "sorted" + pathSep + "all.in_dex";
            if(aR.cores > 1){

                ArrayList<Thread> lista = new ArrayList<Thread>();

                for(int ii = 1; ii <= aR.cores; ii++){

                    Thread temp = new Thread(new MoniSaieAjo_HilaLaskenta(ii, aR.cores, tiedostot_sorted, origo, Double.parseDouble(cellSize), (aR.output + pathSep + ii + "hila.txt"), Integer.parseInt(karttaLehtiKoko), aR.buffer, indeksi_pathi_all, aR.oparse, aR.output));

                    lista.add(temp);
                    temp.start();
                }

                for(int i = 0; i < lista.size(); i++){

                    try{

                        lista.get(i).join();
                    }catch(Exception e){}
                }

                File dirr = new File(aR.output);

                if(dirr.exists())
                    dirr.delete();

                dirr.createNewFile();

                File[] outFiles = dirr.listFiles(new FilenameFilter() {
                    public boolean accept(File dirr, String name) {
                        return name.toLowerCase().endsWith("hila.txt");
                    }
                });


                merged = new File((output + pathSep + outName));


                mergeFiles(outFiles, merged);

                if(outFiles.length > 0)
                    for(File file3: outFiles)
                        if (!file3.isDirectory()) {
                            for(int is = 1; is <= aR.cores; is++)
                                if(file3.getName().equals( (is + "hila.txt")))
                                    file3.delete();
                        }

            }

            else{

                homma.hilaJako(tiedostot_sorted, indeksi_pathi_all, origo, Double.parseDouble(cellSize), (aR.output + pathSep + outName), Integer.parseInt(karttaLehtiKoko), 0,0, aR.buffer, aR.oparse, aR.output);
            }


        }



        //System.out.println("");
        //System.out.println("Processing complete");

        //System.out.println(time.elapsed());

        System.exit(1);
    }


}