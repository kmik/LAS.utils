import LASio.LASReader;
import org.gdal.ogr.*;
import org.jetbrains.annotations.NotNull;
import tools.ToShp;
import tools.createCHM;
import utils.argumentReader;
import utils.fileDistributor;

import java.io.*;
import java.util.ArrayList;

import static runners.MKid4pointsLAS.readPolygonsFromWKT;
import static runners.RunLASutils.proge;
import static utils.miscProcessing.prepareData;
import static utils.miscProcessing.printProcessingTime;

public class lasITC {


    public static void main(String[] args) throws IOException {

        ogr.RegisterAll();

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "lasITC");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        ArrayList<double[][]> polyBank = getPolygons(aR);

        aR.setPolyBank(polyBank);

        aR.p_update.las2dsm_print = false;

        aR.p_update.lasITD_CHMresolution = aR.step;
        aR.p_update.lasITD_gaussianKernel = aR.kernel;
        aR.p_update.lasITD_gaussiantheta = aR.theta;
        aR.p_update.lasITD_itdKernel = Math.max(aR.dist, 1);


        if (aR.cores > 1) {
            threadTool(aR, fD);

        } else {

            boolean rem_buf = false;

            if(aR.remove_buffer){
                aR.remove_buffer_2 = true;
                aR.remove_buffer = false;
                aR.inclusionRule.undoRemoveBuffer();
            }

            for (int i = 0; i <  aR.inputFiles.size(); i++) {

                try {
                    run_itc(aR, i);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        printProcessingTime();

        aR.cleanup();
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
                    fill.releaseMemory();

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
        fill.releaseMemory();
        fill = null;
        testi = null;
        temp.close();
        temp = null;


    }

}
