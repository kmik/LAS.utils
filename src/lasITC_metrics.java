import LASio.LASReader;
import org.gdal.ogr.*;
import org.jetbrains.annotations.NotNull;
import runners.RunLASutils;
import tools.ITDstatistics;
import tools.createCHM;
import utils.argumentReader;
import utils.fileDistributor;
import utils.pointDistributor;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

import static runners.MKid4pointsLAS.readPolygonsFromWKT;
import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;

public class lasITC_metrics {

    public static void main(String[] args) throws IOException {

        ogr.RegisterAll();

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "lasITC");
        fileDistributor fD = new fileDistributor(aR.inputFiles);


        if (aR.cores > 1) {
            File trees = aR.measured_trees;

            int plotSize = (int) aR.step;

            ITDstatistics stats_base = new ITDstatistics(aR);

            if (aR.measured_trees != null)
                stats_base.readMeasuredTrees(trees);

            File o_file = null;

            if (aR.output.equals("asd")) {

                aR.output = "default_lasITD_output.txt";

            }

            o_file = new File(aR.output);

            if (o_file.exists())
                o_file.createNewFile();

            stats_base.setOutput(new File(aR.output));

            int index = stats_base.outFile.getName().lastIndexOf(".");

            String ext = stats_base.outFile.getName().substring(0, index);

            String parent = stats_base.outFile.getParent() == null ? "" : stats_base.outFile.getParent() + "/";

            /* These output files are only for UAV LiDAR data that has been trunk segmented with lasStem.sh */
            stats_base.setStemOutput(new File(parent + ext + "_stem.txt"));
            stats_base.setStemOutput2(new File(parent + ext + "_stem_2.txt"));
            stats_base.setStemOutput3(new File(parent + ext + "_stem_3.txt"));

            /* Not required. Will only segment trees within the polygon boundaries */
            stats_base.readFieldPlots(new File(aR.poly));

            threadOutputs to = new threadOutputs(aR.cores);

            for (int i = 0; i < aR.inputFiles.size(); i++) {

                ArrayList<String> output_all = new ArrayList<>();

                LASReader temp = new LASReader(aR.inputFiles.get(i));

                ArrayList<HashSet<Integer>> threadWorkLoad = new ArrayList<>();

                for (int c = 0; c < aR.cores; c++)
                    threadWorkLoad.add(new HashSet<>());

                ArrayList<File> files = new ArrayList<>();

                try {
                    pointDistributor pd = new pointDistributor(temp, threadWorkLoad, aR, files);
                }catch (Exception e){
                    e.printStackTrace();
                }
                ArrayList<Thread> lista11 = new ArrayList<Thread>();


                for (int c = 0; c < aR.cores; c++) {

                    //LASReader temp_2 = new LASReader(files.get(c));
                    LASReader temp_2 = new LASReader(aR.inputFiles.get(i));

                    System.out.println("FILE: " + temp_2.getFile().getAbsolutePath());

                    ITDstatistics stats_tmp = new ITDstatistics(aR);

                    stats_tmp.pointSourceSubset(threadWorkLoad.get(c));

                    if (aR.measured_trees != null)
                        stats_tmp.readMeasuredTrees(trees);

                    Thread thr = new Thread(new multiThreadITDstats(stats_tmp, to, temp_2));
                    lista11.add(thr);
                    thr.start();

                }
                for (int i_ = 0; i_ < lista11.size(); i_++) {

                    try {

                        lista11.get(i_).join();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                for(i = 0; i < to.outputs.size(); i++)
                    output_all.addAll(to.outputs.get(i));


                stats_base.output.addAll(output_all);

                /* Delete temporary files */
                for(File f : files){
                    f.delete();
                }
            }

            stats_base.printOutput();
            stats_base.closeFile();

        } else {

            File trees = aR.measured_trees;

            int plotSize = (int) aR.step;

            ITDstatistics stats = new ITDstatistics(aR);

            if (aR.measured_trees != null)
                stats.readMeasuredTrees(trees);

            File o_file = null;

            if (aR.output.equals("asd")) {

                aR.output = "default_lasITD_output.txt";

            }

            o_file = new File(aR.output);

            if (o_file.exists())
                o_file.createNewFile();

            stats.setOutput(new File(aR.output));

            int index = stats.outFile.getName().lastIndexOf(".");

            String ext = stats.outFile.getName().substring(0, index);

            String parent = stats.outFile.getParent() == null ? "" : stats.outFile.getParent() + "/";

            /* These output files are only for UAV LiDAR data that has been trunk segmented with lasStem.sh */
            stats.setStemOutput(new File(parent + ext + "_stem.txt"));
            stats.setStemOutput2(new File(parent + ext + "_stem_2.txt"));
            stats.setStemOutput3(new File(parent + ext + "_stem_3.txt"));

            /* Not required. Will only segment trees within the polygon boundaries */
            stats.readFieldPlots(new File(aR.poly));

            for (int i = 0; i < aR.inputFiles.size(); i++) {

                LASReader temp = new LASReader(aR.inputFiles.get(i));

                stats.setPointCloud(temp);
                stats.processPointCloud(temp);

            }

            if (aR.measured_trees != null)
                stats.labelTrees();



            stats.printOutput();

            stats.closeFile();

        }

    }

    @NotNull
    private static ArrayList<double[][]> getPolygons(argumentReader aR) {
        ArrayList<Integer> plotID = new ArrayList<>();
        ArrayList<double[][]> polyBank = new ArrayList<double[][]>();
        if (!aR.poly.equals("null")) {

            DataSource ds = ogr.Open(aR.poly);
            Layer layeri = ds.GetLayer(0);

            try {
                File fout = new File("tempWKT.csv");

                if (fout.exists())
                    fout.delete();

                fout.createNewFile();

                FileOutputStream fos = new FileOutputStream(fout);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

                bw.write("WKT,plot_id");
                bw.newLine();

                for (long i = 0; i < layeri.GetFeatureCount(); i++) {

                    Feature tempF = layeri.GetFeature(i);
                    Geometry tempG = tempF.GetGeometryRef();
                    String id = "";
                    if (tempF.GetFieldCount() > 0)
                        id = tempF.GetFieldAsString(0);
                    else
                        id = String.valueOf(i);
                    String out = "\"" + tempG.ExportToWkt() + "\"," + id;
                    bw.write(out);
                    bw.newLine();
                }

                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            aR.poly = "tempWKT.csv";
        }

        try {
            polyBank = readPolygonsFromWKT(aR.poly, plotID);
        }
        catch (Exception e){

        }
        return polyBank;
    }

    private static void threadTool(argumentReader aR, fileDistributor fD) {

        proge.setEnd(aR.inputFiles.size());

        if (aR.cores > aR.inputFiles.size())
            aR.cores = aR.inputFiles.size();

        ArrayList<Thread> threadList = new ArrayList<Thread>();

        for (int ii = 1; ii <= aR.cores; ii++) {

            proge.addThread();
            Thread temp = new Thread(new lasITC.multiThreadTool(aR, aR.cores, ii, fD));
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
    static class multiThreadTool implements Runnable {

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
                    aR.p_update.threadFile[nCore - 1] = "CHM";
                    aR.p_update.updateProgressITD();
                    createCHM testi_c = new createCHM();

                    createCHM.chm testi = testi_c.new chm(temp, "y", 1, aR, nCore);

                    aR.p_update.threadFile[nCore - 1] = "treeTops";
                    aR.p_update.updateProgressITD();
                    testi.detectTreeTops((int) aR.p_update.lasITD_itdKernel);

                    aR.p_update.threadFile[nCore - 1] = "waterShed";
                    aR.p_update.updateProgressITD();
                    createCHM.WaterShed fill = new createCHM.WaterShed(testi.treeTops, 0.2, testi.cehoam, testi, aR, nCore);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    private static void run_itc(argumentReader aR, int i) throws Exception {
        LASReader temp = new LASReader(aR.inputFiles.get(i));

        aR.p_update.threadFile[0] = "CHM";
        aR.p_update.updateProgressITD();
        createCHM testi_c = new createCHM();

        createCHM.chm testi = testi_c.new chm(temp, "y", 1, aR, 1);

        aR.p_update.threadFile[0] = "CHM - treeTops";
        aR.p_update.updateProgressITD();
        testi.detectTreeTops((int) aR.p_update.lasITD_itdKernel);

        aR.p_update.threadFile[0] = "waterShed";
        aR.p_update.updateProgressITD();


        createCHM.WaterShed fill = new createCHM.WaterShed(testi.treeTops, 0.2, testi.cehoam, testi, aR, 1);
    }

    static class threadOutputs{

        int n_threads;

        ArrayList<ArrayList<String>> outputs = new ArrayList<>();

        int adds = 0;
        int removes = 0;

        public threadOutputs(int n_threads){

            this.n_threads = n_threads;

        }

        public synchronized void addData(ArrayList<String> in){

            outputs.add(in);
            adds++;

        }

    }

    static class multiThreadITDstats implements Runnable {

        ITDstatistics stats;
        threadOutputs to;
        LASReader pointCloud;

        public multiThreadITDstats(ITDstatistics stats, threadOutputs to, LASReader pointCloud){

            this.stats = stats;
            this.to = to;
            this.pointCloud = pointCloud;
        }

        public void run() {

            try {

                stats.setPointCloud(pointCloud);
                stats.processPointCloud(pointCloud);

            }catch (Exception e){

                e.printStackTrace();

            }

            to.addData(stats.output);

        }

    }

}
