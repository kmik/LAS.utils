import LASio.LASReader;
import err.toolException;
import org.gdal.ogr.ogr;
import tools.ToShp;
import utils.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;
import static utils.miscProcessing.printProcessingTime;

public class lasGridStats {


    public static void main(String[] args) throws IOException {

        ogr.RegisterAll();

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "lasGridStats");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        threadProgressbars prog = new threadProgressbars(aR.cores, aR.inputFiles.size());
        aR.setProgressBars(prog);

        if(aR.inputFiles.get(0).getName().contains(".tif")){

            aR.cores = aR.origCores;


            if(aR.configFile != null){


                KarttaLehtiJako klj = new KarttaLehtiJako();

                HashSet<String> mapSheetsToConsider = null;
                HashSet<String> mapSheetsInsideFinland = null;

                try {
                    klj.readFromFile(new File(""));
                    mapSheetsToConsider = readConfigFile(aR.configFile);
                    mapSheetsInsideFinland = readConfigFile(aR.configFile2);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                checkConfigFile(mapSheetsToConsider, klj, mapSheetsInsideFinland);

                try {
                    klj.readFromFile(new File(""), mapSheetsToConsider);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println(klj.configMapSheetExtents.size());

                ForkJoinPool customThreadPool = new ForkJoinPool(aR.cores);

                System.out.println("Starting lasGridStats with " + aR.cores + " cores.");

                //ArrayList<Pair<String, double[]>> mapSheetNamesAndExtents = new ArrayList<>();
                ArrayList<Integer> dat = new ArrayList<>();
                for(int i = 0; i < klj.configMapSheetExtents.size(); i++){
                    dat.add(i);
                    //mapSheetNamesAndExtents.add(new Pair<>(klj.configMapSheetNames.get(i), klj.configMapSheetExtents.get(i)));
                }

                dataDistributor dD = new dataDistributor(dat);

                if(aR.cores > 1){
                    threadTool2(aR, dD, klj);
                }
                else{
                    for(int i = 0; i < klj.configMapSheetExtents.size(); i++) {
                        System.out.println("Processing " + klj.configMapSheetNames.get(i) + " " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));


                        lasClipMetricOfile lCMO = new lasClipMetricOfile(aR);
                        double[] temp = klj.configMapSheetExtents.get(i);
                        String outputName = klj.configMapSheetNames.get(i) + ".txt";
                        try {
                            tools.lasGridStats lGS = new tools.lasGridStats(aR, 1, temp, outputName, lCMO);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if(false)
                try {

                    customThreadPool.submit(() -> IntStream.range(0, klj.configMapSheetExtents.size()).parallel().forEach(i -> {

                        System.out.println("Processing " + klj.configMapSheetNames.get(i) + " " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
                        //for(int i = 0; i < klj.configMapSheetExtents.size(); i++){

                        lasClipMetricOfile lCMO = new lasClipMetricOfile(aR);
                        double[] temp = klj.configMapSheetExtents.get(i);
                        String outputName = klj.configMapSheetNames.get(i) + ".txt";
                        try {
                            tools.lasGridStats lGS = new tools.lasGridStats(aR, 1, temp, outputName, lCMO);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })).get();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }else {

                try {
                    tools.lasGridStats lGS = new tools.lasGridStats(aR, 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }else {

            if (aR.cores > 1) {
                threadTool(aR, fD);
            } else {
                for (int i = 0; i < inputFiles.size(); i++) {
                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    try {
                        tools.lasGridStats lGS = new tools.lasGridStats(temp, aR, 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    aR.prog.fileDone();
                }
            }
        }

        printProcessingTime();


    }

    public static HashSet<String> readConfigFile(File configFile) throws Exception{

        HashSet<String> output = new HashSet<>();
        File config = configFile;

        // read the config file line by line

        try {
            BufferedReader br = new BufferedReader(new FileReader(config));
            String line = br.readLine();
            while (line != null) {

                output.add(line);

                if((line.matches(".*\\s+.*")) )
                    throw new toolException("Map sheet names cannot contain spaces. Maybe you do not have one map sheet name per line?");

                line = br.readLine();
            }

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    public static boolean checkConfigFile(HashSet<String> mapSheetsToConsider, KarttaLehtiJako klj, HashSet<String> mapsheetsinsideFinland){

        for(String s : mapSheetsToConsider){
            if(!klj.mapSheetNames.contains(s)){
                throw new toolException("Map sheet " + s + " is not valid.");
            }
            if(!mapsheetsinsideFinland.contains(s)){
                throw new toolException("Map sheet " + s + " is not within the borders of Finland.");
            }
        }

        return true;
    }

    private static void threadTool(argumentReader aR, fileDistributor fD) {

        proge.setEnd(aR.inputFiles.size());

        if (aR.cores > aR.inputFiles.size())
            aR.cores = aR.inputFiles.size();

        ArrayList<Thread> threadList = new ArrayList<Thread>();

        for (int ii = 1; ii <= aR.cores; ii++) {

            proge.addThread();
            Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
            threadList.add(temp);
            temp.start();

        }

        for (int i = 0; i < threadList.size(); i++) {

            try {
                threadList.get(i).join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static void threadTool2(argumentReader aR, dataDistributor fD, KarttaLehtiJako klj) {

        proge.setEnd(aR.inputFiles.size());

        if (aR.cores > fD.size())
            aR.cores = fD.size();

        ArrayList<Thread> threadList = new ArrayList<Thread>();

        for (int ii = 1; ii <= aR.cores; ii++) {

            proge.addThread();
            Thread temp = new Thread(new multiThreadTool2(aR, aR.cores, ii, fD, klj));
            threadList.add(temp);
            temp.start();

        }

        for (int i = 0; i < threadList.size(); i++) {

            try {
                threadList.get(i).join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Just a class to divide workers for multithreaded tools.
     */
    public static class multiThreadTool implements Runnable {

        argumentReader aR;
        int nCores;
        int nCore;
        fileDistributor fD;

        public multiThreadTool(argumentReader aR, int nCores, int nCore, fileDistributor fD) {

            this.aR = aR;
            this.nCores = nCores;
            this.nCore = nCore;
            this.fD = fD;

        }

        public void run() {

            while (true) {
                if (fD.isEmpty())
                    break;
                File f = fD.getFile();
                if (f == null)
                    continue;
                LASReader temp = null;
                try {

                    temp = new LASReader(f);
                    tools.lasGridStats lGS = new tools.lasGridStats(temp, aR, nCore);
                    temp.close();

                }catch (Exception e){
                    e.printStackTrace();
                }

                aR.prog.fileDone();

            }
        }
    }

    public static class multiThreadTool2 implements Runnable {

        argumentReader aR;
        int nCores;
        int nCore;
        dataDistributor fD;

        KarttaLehtiJako klj;

        public multiThreadTool2(argumentReader aR, int nCores, int nCore, dataDistributor fD, KarttaLehtiJako klj) {

            this.aR = aR;
            this.nCores = nCores;
            this.nCore = nCore;
            this.fD = fD;
            this.klj = klj;

        }

        public void run() {

            while (true) {
                if (fD.isEmpty())
                    break;
                int data = (int)fD.getData();


                System.out.println("Processing " + klj.configMapSheetNames.get(data) + " " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
                //for(int i = 0; i < klj.configMapSheetExtents.size(); i++){

                lasClipMetricOfile lCMO = new lasClipMetricOfile(aR);
                double[] temp = klj.configMapSheetExtents.get(data);
                String outputName = klj.configMapSheetNames.get(data) + ".txt";

                try {
                    tools.lasGridStats lGS = new tools.lasGridStats(aR, 1, temp, outputName, lCMO);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
