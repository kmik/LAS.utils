package runners;

import LASio.LASReader;
import LASio.LASraf;
import LASio.LASwrite;
import LASio.PointInclusionRule;
import err.toolException;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import org.jetbrains.annotations.NotNull;
import tools.*;
import utils.*;

import com.clust4j.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static runners.MKid4pointsLAS.clipPlots_singleLASfile;
import static runners.MKid4pointsLAS.readPolygonsFromWKT;

@SuppressWarnings("unused")
public class RunLASutils {

    public static fileOperations fo = new fileOperations();
    public static listOfFiles tiedostoLista = new listOfFiles();
    public static RunId4pointsLAS.ThreadProgressBar proge = new RunId4pointsLAS.ThreadProgressBar();

    public static void addIndexFiles(ArrayList<LASReader> in) throws Exception {

        File temppi;
        for (int i = 0; i < in.size(); i++) {
            temppi = fo.createNewFileWithNewExtension(in.get(i).getFile(), ".lasx");

            if (temppi.exists()) {

                FileInputStream fileIn = new FileInputStream(temppi);
                ObjectInputStream in2 = new ObjectInputStream(fileIn);
                HashMap<Integer, ArrayList<Long>> temppi2;
                temppi2 = (HashMap<Integer, ArrayList<Long>>) in2.readObject();

                in.get(i).setIndexMap(temppi2);
            }
        }

    }

    public static void main(String[] args) throws Exception {

        ogr.RegisterAll(); //Registering all the formats..
        gdal.AllRegister();


        long tStart = System.currentTimeMillis();
        argumentReader aR = new argumentReader(args);
        aR.setExecDir(System.getProperty("user.dir"));
        aR.parseArguents();
        String pathSep = System.getProperty("file.separator");
        if (!System.getProperty("os.name").equals("Linux"))
            pathSep = "\\" + pathSep;
        boolean lasFormat, txtFormat;
        String[] lasToken = new File(aR.files[0]).getName().split("\\.");
        lasFormat = lasToken[lasToken.length-1].equals("las");
        txtFormat = lasToken[lasToken.length-1].equals("txt");
        ArrayList<String> filesList = new ArrayList<String>();
        ArrayList<LASReader> pointClouds = new ArrayList<LASReader>();
        ArrayList<File> inputFiles = new ArrayList<>();

        filesList = getFileListAsString(aR, lasFormat, txtFormat, filesList, inputFiles);

        proge.reset();
        try {
            addIndexFiles(pointClouds);
        } catch (Exception e) {
            e.printStackTrace();
        }
        aR.setInputFiles(inputFiles);
        aR.p_update.totalFiles = aR.pointClouds.size();
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        if (aR.tool == 1) {
            Tiler tile = new Tiler(aR.inputFiles, aR);
        }


        if (aR.tool == 2) {

            Merger merge = new Merger(aR.inputFiles, aR.output, aR.getInclusionRule(), aR.odir, aR);

        }

        if (aR.tool == 3) {

            if (aR.cores > 1) {
                threadTool(aR, fD);

            } else {

                for (int i = 0; i < filesList.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    Noise nois = new Noise(temp, aR, 1);
                }
            }

        }

        if (aR.tool == 4) {

            if (aR.cores > 1) {
                threadTool(aR, fD);

            } else {
                for (int i = 0; i < filesList.size(); i++) {
                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    /* PARTLY REFACTORED */
                    GroundDetector det = new GroundDetector(temp, false, aR.output, aR.odir, aR.getInclusionRule(), aR.angle, aR.numarg1, aR.axgrid, aR, 1);

                    int[] asdi = det.detectSeedPoints();

                    det.detect();

                    det.wipe();
                    det = null;
                    System.gc();
                }
            }

        }

        if (aR.tool == 5) {

            if (aR.cores > 1) {
                threadTool(aR, fD);

            } else {
                for (int i = 0; i < filesList.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    GroundDetector det = new GroundDetector(temp, false, aR.output, aR.odir, aR.getInclusionRule(), aR.angle, aR.numarg1, aR.axgrid, aR, 1);

                    det.oparse(aR.oparse);

                    if (aR.mem_efficient) {
                        det.normalizeZ_mem_eff(aR.output, aR.getInclusionRule(), aR.otype);
                    }

                    if (aR.groundPoints.equals("-999")) {

                        if (aR.ground_class == -1)
                            det.normalizeZ(aR.output, aR.getInclusionRule(), aR.otype);
                        else
                            det.normalizeZ(aR.ground_class, aR.output, aR.getInclusionRule(), aR.otype, aR.groundPoints);

                    } else {

                        det.normalizeZ(aR.ground_class, aR.output, aR.getInclusionRule(), aR.otype, aR.groundPoints);

                    }

                    det.wipe();
                    det = null;
                    System.gc();
                }
            }

        }

        if (aR.tool == 6) {

            if (aR.cores > 1) {
                threadTool(aR, fD);

            } else {
                for (int i = 0; i < filesList.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    Thinner thi = new Thinner(temp, aR.step, aR, 1);

                }
            }

        }

        if (aR.tool == 7) {

            if (aR.cores > 1) {
                threadTool(aR, fD);

            } else {

                for (int i = 0; i < filesList.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    createCHM testi_c = new createCHM();

                    createCHM.chm testi = testi_c.new chm(temp, "y", 1, aR, 1);

                }
            }

        }

        if (aR.tool == 800) {

            for (int i = 0; i < filesList.size(); i++) {

                File fromFile = new File(filesList.get(i));

                File tempFile = new File(filesList.get(i).split(".txt")[0] + ".las");

                if (tempFile.exists())
                    tempFile.delete();


                LASraf asd2 = new LASraf(tempFile);
                LASwrite.txt2las(fromFile, asd2, aR.iparse, "txt2las", aR.sep, aR.getInclusionRule(), false, aR);
            }

        }

        if (aR.tool == 9) {

            if (aR.cores > 1) {
                threadTool(aR, fD);

            } else {
                for (int i = 0; i < filesList.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    Boundary bound = new Boundary(temp, aR.odir, aR.output, false, aR, 1);

                }
            }
        }

        if (aR.tool == 10) {

            if (aR.cores > 1) {
                threadTool(aR, fD);

            } else {
                for (int i = 0; i < filesList.size(); i++) {
                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    Boundary bound = new Boundary(temp, aR.odir, aR.output, false, aR, 1);

                }
            }

        }

        if (aR.tool == 11) {

            if (aR.cores > 1) {
                threadTool(aR, fD);

            } else {
                for (int i = 0; i < filesList.size(); i++) {
                    //System.out.println("Odir: " + odir);
                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    ToShp toshape = new ToShp(aR.output, temp, aR.getInclusionRule(), aR.odir, aR.oparse, aR);

                }
            }
        }

        if (aR.tool == 12) {

            aR.p_update.las2txt_oparse = aR.oparse;

            if (aR.cores > 1) {
                threadTool(aR, fD);

            } else {


                for (int i = 0; i < filesList.size(); i++) {
                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    las2txt ddd = new las2txt(temp, aR.odir, aR.oparse, aR, 1);

                }
            }
        }

        if (aR.tool == 13) {

            if (aR.cores > 1) {
                threadTool(aR, fD);

            } else {

                process_las2las tooli = new process_las2las(1);

                for (int i = 0; i < inputFiles.size(); i++) {
                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    try {

                        tooli.convert(temp, aR);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }

        if (aR.tool == 14) {

            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n");

            lasStrip tooli = new lasStrip(aR);
            tooli.align();

        }

        if (aR.tool == 15) {

            ogr.RegisterAll();
            ArrayList<Integer> plotID = new ArrayList<>();

            ArrayList<double[][]> polyBank = new ArrayList<double[][]>();


            if (!aR.poly.equals("null")) {

                int checkShp = 1;
                DataSource ds = ogr.Open(aR.poly);

                Layer layeri = ds.GetLayer(0);
                //System.out.println("Feature count: " + layeri.GetFeatureCount());

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
                //shapeType = 2;
            }

            try {
                polyBank = readPolygonsFromWKT(aR.poly, plotID);
            }
            catch (Exception e){

            }
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

                for (int i = 0; i < filesList.size(); i++) {

                    run_itc(aR, i);

                }
            }

        }

        if (aR.tool == 16) {

            if (aR.step == 2)
                aR.step = 50;

            for (int i = 0; i < aR.inputFiles.size(); i++) {

                try {
                    LASReader tempReader = new LASReader(aR.inputFiles.get(i));
                    tempReader.index((int) aR.step);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        if (aR.tool == 17) {

            if (aR.cores > 1) {
                threadTool(aR, fD);

            } else {
                for (int i = 0; i < aR.inputFiles.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    try {
                        lasSort sort = new lasSort(temp, aR, 1);


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        if (aR.tool == 18) {

            if (aR.cores > 1) {
                threadTool(aR, fD);

            } else {
                for (int i = 0; i < aR.inputFiles.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    try {
                        lasSplit split = new lasSplit(temp, aR, 1);
                        split.split();
                        //sort.sortByZCount();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        if (aR.tool == 19) {

            for (int i = 0; i < aR.inputFiles.size(); i++) {

                LASReader temp = new LASReader(aR.inputFiles.get(i));

                try {
                    lasCheck check = new lasCheck(temp, aR);
                    check.check();
                    //sort.sortByZCount();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                temp.close();
                System.gc();
            }


        }

        /* Compute ITD statistics */
        /* NOT YET REFACTORED NOT SWITCHED TO SINGLE FILE!!! */
        if (aR.tool == 20) {

            if(aR.cores <= 1) {

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

            }else{

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
                    pointDistributor pd = new pointDistributor(temp, threadWorkLoad, aR, files);

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
            }


        }

        if (aR.tool == 21) {

            for (int i = 0; i < aR.inputFiles.size(); i++) {
                LASReader temp = new LASReader(aR.inputFiles.get(i));
                lasLayer layer = new lasLayer(temp, aR, 1);
                layer.layer();
            }

        }

        if (aR.tool == 22) {

            System.out.println("NEURAL TEST");

            try {
                neuralNetWorkTest_3d_treespecies te = new neuralNetWorkTest_3d_treespecies(aR);
                //convolution te = new convolution(aR);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (aR.tool == 221) {

            neuralNetworkTools ntool = new neuralNetworkTools(aR);
            //ntool.printNetwork(aR.model);
            ntool.searchArbiterOutput();
        }

        if (aR.tool == 222) {

            System.out.println("NEURAL HYPERPARAMETER OPTIMIZATION");

            try {
                neuralNetworkHyperparameterOptimization_convolution_mix te = new neuralNetworkHyperparameterOptimization_convolution_mix(aR);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (aR.tool == 23) {

            System.out.println("stem align");

            try {
                for (int i = 0; i < aR.inputFiles.size(); i++) {
                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    stemAligner sA = new stemAligner(temp, aR);
                    sA.align();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (aR.tool == 24) {

            if (aR.inputFiles.size() != 2) {
                System.out.println("No 2 input files, exiting!");
                throw new toolException("Ground match requires two inputs!");
            }

            try {

                LASReader temp1 = new LASReader(aR.inputFiles.get(0));
                LASReader temp2 = new LASReader(aR.inputFiles.get(1));

                groundMatch gM = new groundMatch(temp1, temp2, aR, 1);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (aR.tool == 25) {
            try {

                for (int i = 0; i < aR.inputFiles.size(); i++) {
                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    lasGridStats lGS = new lasGridStats(temp, aR, 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /* stem detector for e.g. UAV LiDAR data (should be useful in terrestrial photogrammetry as well?) */
        if (aR.tool == 26) {
            try {

                for (int i = 0; i < aR.inputFiles.size(); i++) {
                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    stemDetector sd = new stemDetector(temp, 0.1, 0.5, 1, aR);

                    sd.setUpOutputFiles(temp);

                    sd.detect(false);

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (aR.tool == 27) {

            HashMap<Integer, int[]> trunkMatches = new HashMap<>();
            int[] n_trunks = new int[]{1};

            try {

                for (int i = 0; i < aR.inputFiles.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    trunkDBH t_DBH = new trunkDBH(temp, aR, trunkMatches, n_trunks);
                    t_DBH.process();

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            FileOutputStream fos = new FileOutputStream("trunks.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(trunkMatches);
            oos.close();

        }

        if (aR.tool == 28) {

            File shapeFile = new File(aR.poly);

            ogr.RegisterAll(); //Registering all the formats..
            gdal.AllRegister();

            int shapeType = -1;

            File fout = null;

            int checkShp = 0;


            try {


                Thread.sleep(0);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (shapeFile.getName().contains(".shp")) {

                checkShp = 1;
                DataSource ds = ogr.Open(aR.poly);
                //System.out.println("Layer count: " + ds.GetLayerCount());
                Layer layeri = ds.GetLayer(0);
                //System.out.println("Feature count: " + layeri.GetFeatureCount());

                try {
                    fout = new File("tempWKT.csv");

                    if (fout.exists())
                        fout.delete();

                    fout.createNewFile();

                    FileOutputStream fos = new FileOutputStream(fout);
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

                    bw.write("WKT,plot_id");
                    bw.newLine();

                    for (long i = 0; i < layeri.GetFeatureCount(); i++) {

                        Feature tempF = layeri.GetFeature(i);
                        //System.out.println(layeri.GetFeatureCount());
                        Geometry tempG = tempF.GetGeometryRef();

                        if (tempG == null)
                            continue;

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
                shapeType = 2;

            }

            try {

                for (int i = 0; i < aR.inputFiles.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    clipPlots_singleLASfile(aR.poly, aR, temp);

                    System.out.println("HERE");

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (aR.tool == 29) {

            HashMap<Integer, double[]> trunkFile = new HashMap<>();
            readTrunkFile(new File("trunk_dbh_out.txt"), trunkFile);
            HashSet<Integer> upperStoreyTrunks = new HashSet<>();
            HashSet<Integer> underStoreyTrunks = new HashSet<>();
            HashMap<Integer, Integer> trunk_to_crown = new HashMap<>();


            try {

                for (int i = 0; i < aR.inputFiles.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    classifyTrunks c_trunks = new classifyTrunks(temp, aR, trunkFile,
                            upperStoreyTrunks,
                            underStoreyTrunks,
                            trunk_to_crown);

                    c_trunks.classify();

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            File o_trunk_file = aR.createOutputFileWithExtension(new File("trunk_dbh_out.txt"), "_underOver.txt");


            BufferedWriter bw2 = new BufferedWriter(new FileWriter(o_trunk_file, true));

            for (int key : trunkFile.keySet()) {

                String o_string = key + "\t";

                for (int i = 0; i < trunkFile.get(key).length; i++) {
                    o_string += trunkFile.get(key)[i] + "\t";
                }

                if (upperStoreyTrunks.contains(key))
                    o_string += 1 + "\t";

                else if (underStoreyTrunks.contains(key))
                    o_string += 0 + "\t";
                else
                    o_string += 2 + "\t";

                if (trunk_to_crown.containsKey(key))
                    o_string += trunk_to_crown.get(key) + "\t";
                else
                    o_string += -1;

                bw2.write(o_string);
                bw2.newLine();

            }

            bw2.close();
        }

        if (aR.tool == 30){

            if(aR.mode_3d){
                for (int i = 0; i < aR.inputFiles.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    createCHM testi_c = new createCHM();

                    createCHM.chm testi = testi_c.new chm(temp, "y", 1, aR, 1);

                    las2solar_photogrammetry l2s = new las2solar_photogrammetry(testi.outputFileName, aR, temp, false);

                }

            }else {
                try {

                    for (int i = 0; i < aR.inputFiles.size(); i++) {


                        LASReader temp = new LASReader(aR.inputFiles.get(i));

                        createCHM testi_c = new createCHM();

                        createCHM.chm testi = testi_c.new chm(temp, "y", 1, aR, 1);
                        las2solar_photogrammetry l2s = new las2solar_photogrammetry(testi.outputFileName, aR, temp);

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (aR.tool == 31){

            double averageDensity_pulse = 0.0;
            double averageDensity_points = 0.0;

            try {

                for (int i = 0; i < aR.inputFiles.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    lasStat stat = new lasStat(temp, aR);
                    averageDensity_pulse += stat.getPulsePerUnit();
                    averageDensity_points += stat.getPointsPerUnit();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("Average pulse density across all .las files: " + (averageDensity_pulse / (double)aR.inputFiles.size()));
            System.out.println("Average point density across all .las files: " + (averageDensity_points / (double)aR.inputFiles.size()));

        }

        if (aR.tool == 32){

            double averageDensity_pulse = 0.0;
            double averageDensity_points = 0.0;

            try {

                for (int i = 0; i < aR.inputFiles.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    lasStandDelineator delineator = new lasStandDelineator(temp, aR);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        if (aR.tool == 33){

            double averageDensity_pulse = 0.0;
            double averageDensity_points = 0.0;

            try {

                for (int i = 0; i < aR.inputFiles.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    lasCC cc = new lasCC(temp, aR);

                    cc.canopy_cover_points();

                    createCHM testi_c = new createCHM();

                    createCHM.chm testi = testi_c.new chm(temp, "y", 1, aR, 1);

                    cc.canopy_cover_chm(testi.filtered);

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }


        if (aR.tool == 34){

            if (aR.cores > 1) {
                threadTool(aR, fD);

            } else {
                try {

                    for (int i = 0; i < aR.inputFiles.size(); i++) {

                        LASReader temp = new LASReader(aR.inputFiles.get(i));

                        ground2raster g2r = new ground2raster(temp, aR, 1);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        if (aR.tool == 35){

            if (aR.cores > 1) {
                threadTool(aR, fD);

            } else {


                gz_tools gz = new gz_tools(aR);

                for (int i = 0; i < aR.inputFiles.size(); i++) {

                    gz.process(aR.inputFiles.get(i));

                }

            }


        }

        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;

        long minutes = (tDelta / 1000)  / 60;
        int seconds = (int)((tDelta / 1000) % 60);

        System.out.println("Processing took: " + minutes + " min " + seconds + " sec");

    }

    private static void run_itc(argumentReader aR, int i) throws Exception {
        LASReader temp = new LASReader(aR.inputFiles.get(i));

        aR.p_update.threadFile[0] = "CHM";
        aR.p_update.updateProgressITD();
        createCHM testi_c = new createCHM();

        createCHM.chm testi = testi_c.new chm(temp, "y", 1, aR, 1);

        System.out.println("CHM DONE!");
        System.out.println("CHM DONE!");
        System.out.println("CHM DONE!");


        aR.p_update.threadFile[0] = "CHM - treeTops";
        aR.p_update.updateProgressITD();
        testi.detectTreeTops((int) aR.p_update.lasITD_itdKernel);

        System.out.println("treetops DONE!");
        System.out.println("treetops DONE!");
        System.out.println("treetops DONE!");

        aR.p_update.threadFile[0] = "waterShed";
        aR.p_update.updateProgressITD();


        createCHM.WaterShed fill = new createCHM.WaterShed(testi.treeTops, 0.2, testi.cehoam, testi, aR, 1);
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

    @NotNull
    public static ArrayList<String> getFileListAsString(argumentReader aR, boolean lasFormat, boolean txtFormat, ArrayList<String> filesList, ArrayList<File> inputFiles) {
        if (lasFormat && !aR.noLas) {


            filesList.addAll(Arrays.asList(aR.files));


            for (int i = 0; i < filesList.size(); i++) {

                inputFiles.add(new File(filesList.get(i)));

            }
        }
        else if (txtFormat && !aR.noLas) {

            System.out.println("converting txt to las");

            proge.setName("Converting .txt to .las ...");
            ArrayList<String> tempList = new ArrayList<String>();

            tempList.addAll(Arrays.asList(aR.files));

            proge.setEnd(tempList.size());

            if (aR.cores > tempList.size())
                aR.cores = tempList.size();


            ArrayList<Thread> lista11 = new ArrayList<Thread>();
            for (int ii = 1; ii <= aR.cores; ii++) {

                proge.addThread();
                Thread temp = new Thread(new multiTXT2LAS(tempList, aR.iparse, aR.cores, ii, aR.odir, aR.echoClass, aR));
                lista11.add(temp);
                temp.start();

            }

            for (int i = 0; i < lista11.size(); i++) {

                try {

                    lista11.get(i).join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            filesList = tiedostoLista.files;

            for (int i = 0; i < filesList.size(); i++) {

                inputFiles.add(new File(filesList.get(i)));

            }
        }else{

            filesList.addAll(Arrays.asList(aR.files));


            for (int i = 0; i < filesList.size(); i++) {

                inputFiles.add(new File(filesList.get(i)));

            }

        }
        return filesList;
    }

    public static void readTrunkFile(File trunkFile, HashMap<Integer, double[]> fil) throws Exception {

        try (BufferedReader br = new BufferedReader(new FileReader(trunkFile))) {

            String line;

            while ((line = br.readLine()) != null) {

                String[] tokens = line.split("\t");

                fil.put(Integer.parseInt(tokens[0]), new double[]{Double.parseDouble(tokens[1]),
                        Double.parseDouble(tokens[2]),
                        Double.parseDouble(tokens[3]),
                        Double.parseDouble(tokens[4])
                });

            }

        }
    }

    /**
     * This is just a class to track the output from multiTXT2LAS
     *
     * @author Kukkonen Mikko
     * @version 0.1
     * @since 06.03.2018
     */
    public static class listOfFiles {

        ArrayList<String> files = new ArrayList<String>();

        public listOfFiles() {


        }

        public synchronized void add(ArrayList<String> in) {

            files.addAll(in);

        }


    }

    /**
     * A class to perform txt -> las conversion with multiple
     * threads.
     *
     * @author Kukkonen Mikko
     * @version 0.1
     * @since 06.03.2018
     */

    static class multiTXT2LAS implements Runnable {

        private final String parse;
        ArrayList<String> files;
        ArrayList<String> returnList = new ArrayList<String>();
        int numberOfCores;
        int coreNumber;
        boolean echoClass = false;
        String odir;
        argumentReader aR;

        public multiTXT2LAS(ArrayList<String> tiedostot2, String parse2, int numberOfCores2, int coreNumber2, String odir2, boolean echoClass, argumentReader aR) {

            this.aR = aR;
            files = tiedostot2;
            parse = parse2;
            numberOfCores = numberOfCores2;
            coreNumber = coreNumber2;
            odir = odir2;
            this.echoClass = echoClass;

        }

        public ArrayList<String> getList() {

            return returnList;

        }

        public void run() {

            try {

                int pienin = 0;
                int suurin = 0;
                if (coreNumber != 0) {

                    int jako = (int) Math.ceil((double) files.size() / (double) numberOfCores);
                    if (coreNumber != numberOfCores) {

                        pienin = (coreNumber - 1) * jako;
                        suurin = coreNumber * jako;
                    } else {
                        pienin = (coreNumber - 1) * jako;
                        suurin = files.size();
                    }

                    files = new ArrayList<String>(files.subList(pienin, suurin));

                }

                ArrayList<File> from = new ArrayList<File>();
                ArrayList<LASraf> to = new ArrayList<LASraf>();
                ArrayList<String> outList = new ArrayList<String>();
                PointInclusionRule rule = new PointInclusionRule();

                for (int i = 0; i < files.size(); i++) {

                    File tempFile = new File(files.get(i));

                    File toFile = null;

                    toFile = aR.createOutputFileWithExtension(tempFile, ".las");

                    System.out.println(toFile.getAbsolutePath());
                    File fromFile = new File(files.get(i));

                    if (toFile.exists())
                        toFile.delete();

                    toFile.createNewFile();

                    from.add(fromFile);

                    //to.add(new LASraf(toFile));
                    LASraf tmp = new LASraf(toFile);
                    LASwrite.txt2las(from.get(i), tmp, parse, "txt2las", aR.sep, rule, echoClass, aR);

                    tmp.writeBuffer2();
                    tmp.close();

                    //System.exit(1);

                    proge.updateCurrent(1);
                    if (i % (proge.end / 20 + 1) == 0)
                        proge.print();

                    //outList.add(aR.createOutputFileWithExtension(tempFile, ".las").getAbsolutePath());

                }

                tiedostoLista.add(outList);

                if(false)
                for (int i = 0; i < files.size(); i++) {


                }

            } catch (Exception e) {
                System.out.println(e.getMessage());

            }
        }
    }

    /**
     * Just a class to divide workers for multithreaded tools.
     */
    static class multiThreadTool implements Runnable {

        tool tool;
        argumentReader aR;
        int nCores;
        int nCore;
        ArrayList<LASReader> pointClouds;
        ArrayList<File> inputFiles;
        fileDistributor fD;

        public multiThreadTool(argumentReader aR, int nCores, int nCore, fileDistributor fD) {

            this.aR = aR;
            this.nCores = nCores;
            this.nCore = nCore;
            this.fD = fD;

        }

        public void run() {

            try {

                if (aR.tool == 3) {

                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        LASReader temp = new LASReader(f);

                        Noise nois = new Noise(temp, aR, nCore);

                    }

                }

                if (aR.tool == 4) {

                    while (true) {

                        try {
                            if (fD.isEmpty())
                                break;

                            File f = fD.getFile();

                            if (f == null)
                                continue;

                            LASReader temp = new LASReader(f);

                            GroundDetector det = new GroundDetector(temp, false, aR.output, aR.odir, aR.getInclusionRule(), aR.angle, aR.numarg1, aR.axgrid, aR, nCore);
                            int[] asdi = det.detectSeedPoints();
                            det.detect();
                            det.wipe();
                            det = null;

                        } catch (Exception e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                }

                if (aR.tool == 5) {

                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        LASReader temp = new LASReader(f);
                        GroundDetector det = new GroundDetector(temp, false, aR.output, aR.odir, aR.getInclusionRule(), aR.angle, aR.numarg1, aR.axgrid, aR, nCore);

                        det.oparse(aR.oparse);

                        if (aR.mem_efficient) {
                            det.normalizeZ_mem_eff(aR.output, aR.getInclusionRule(), aR.otype);
                        }

                        /* This means that no external .las file was input as ground points */
                        if (aR.groundPoints.equals("-999")) {

                            if (aR.ground_class == -1)
                                /* This is never run? */
                                det.normalizeZ(aR.output, aR.getInclusionRule(), aR.otype);
                            else
                                det.normalizeZ(aR.ground_class, aR.output, aR.getInclusionRule(), aR.otype, aR.groundPoints);

                        } else {

                            det.normalizeZ(aR.ground_class, aR.output, aR.getInclusionRule(), aR.otype, aR.groundPoints);

                        }

                        det.wipe();
                        det = null;
                        System.gc();
                    }
                }

                if (aR.tool == 6) {

                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        LASReader temp = new LASReader(f);
                        Thinner thi = new Thinner(temp, aR.step, aR, nCore);
                    }

                }

                if (aR.tool == 7) {

                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        LASReader temp = new LASReader(f);
                        createCHM testi_c = new createCHM();

                        createCHM.chm testi = testi_c.new chm(temp, "y", 1, aR, nCore);


                    }

                }


                if (aR.tool == 9) {

                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        LASReader temp = new LASReader(f);
                        Boundary bound = new Boundary(temp, aR.odir, aR.output, false, aR, nCore);

                    }
                }

                if (aR.tool == 10) {


                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        LASReader temp = new LASReader(f);
                        Boundary bound = new Boundary(temp, aR.odir, aR.output, false, aR, nCore);

                    }
                }

                if (aR.tool == 11) {

                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        LASReader temp = new LASReader(f);
                        ToShp toshape = new ToShp(aR.output, temp, aR.getInclusionRule(), aR.odir, aR.oparse, aR);

                    }

                }

                if (aR.tool == 12) {

                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        LASReader temp = new LASReader(f);
                        las2txt ddd = new las2txt(temp, aR.odir, aR.oparse, aR, nCore);

                    }

                }

                /* DONE */
                if (aR.tool == 13 && false) {

                    process_las2las tooli = new process_las2las(nCore);

                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        LASReader temp = new LASReader(f);

                        try {
                            tooli.convert(temp, aR);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                }

                if (aR.tool == 15) {


                    boolean rem_buf = false;

                    if(aR.remove_buffer){
                        aR.remove_buffer_2 = true;
                        aR.remove_buffer = false;
                        aR.inclusionRule.undoRemoveBuffer();
                    }


                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        LASReader temp = new LASReader(f);
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

                    }
                }

                if (aR.tool == 17) {

                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        LASReader temp = new LASReader(f);
                        try {
                            lasSort sort = new lasSort(temp, aR, nCore);


                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }


                if (aR.tool == 18) {

                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        LASReader temp = new LASReader(f);
                        try {
                            lasSplit split = new lasSplit(temp, aR, nCore);
                            split.split();
                            //sort.sortByZCount();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                if(aR.tool == 34){

                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        LASReader temp = new LASReader(f);
                        try {
                            ground2raster g2r = new ground2raster(temp, aR, nCore);
                            //sort.sortByZCount();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }


                }

                if(aR.tool == 35){

                    gz_tools gz = new gz_tools(aR);

                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        gz.process(f);


                    }


                }




            } catch (Exception e) {
                e.printStackTrace();
            }
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
}

