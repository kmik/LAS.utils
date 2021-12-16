package runners;

import LASio.LASReader;
import LASio.LASraf;
import LASio.LASwrite;
import LASio.PointInclusionRule;
import err.toolException;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import tools.*;
import utils.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;

import static runners.MKid4pointsLAS.clipPlots_singleLASfile;
import static runners.MKid4pointsLAS.readPolygonsFromWKT;

@SuppressWarnings("unused")
public class RunLASutils {

    public static fileOperations fo = new fileOperations();
    public static listOfFiles tiedostoLista = new listOfFiles();
    public static RunId4pointsLAS.ThreadProgressBar proge = new RunId4pointsLAS.ThreadProgressBar();

    /**
     * Method to erase the line and print a progress
     * bar based on the input.
     *
     * @param paatos Maximum value
     * @param proge  Current progress
     * @param nimi   Name of the process
     */

    public static void progebar(int paatos, int proge, String nimi) {
        System.out.print("\033[2K"); // Erase line content
        if (proge < 0.05 * paatos) System.out.print(nimi + "   |                    |\r");
        if (proge >= 0.05 * paatos && proge < 0.10 * paatos)
            System.out.print(nimi + "   |#                   |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.10 * paatos && proge < 0.15 * paatos)
            System.out.print(nimi + "   |##                  |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.15 * paatos && proge < 0.20 * paatos)
            System.out.print(nimi + "   |###                 |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.20 * paatos && proge < 0.25 * paatos)
            System.out.print(nimi + "   |####                |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.25 * paatos && proge < 0.30 * paatos)
            System.out.print(nimi + "   |#####               |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.30 * paatos && proge < 0.35 * paatos)
            System.out.print(nimi + "   |######              |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.35 * paatos && proge < 0.40 * paatos)
            System.out.print(nimi + "   |#######             |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.40 * paatos && proge < 0.45 * paatos)
            System.out.print(nimi + "   |########            |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.45 * paatos && proge < 0.50 * paatos)
            System.out.print(nimi + "   |#########           |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.50 * paatos && proge < 0.55 * paatos)
            System.out.print(nimi + "   |##########          |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.55 * paatos && proge < 0.60 * paatos)
            System.out.print(nimi + "   |###########         |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.60 * paatos && proge < 0.65 * paatos)
            System.out.print(nimi + "   |############        |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.65 * paatos && proge < 0.70 * paatos)
            System.out.print(nimi + "   |#############       |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.70 * paatos && proge < 0.75 * paatos)
            System.out.print(nimi + "   |##############      |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.75 * paatos && proge < 0.80 * paatos)
            System.out.print(nimi + "   |###############     |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.80 * paatos && proge < 0.85 * paatos)
            System.out.print(nimi + "   |################    |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.85 * paatos && proge < 0.90 * paatos)
            System.out.print(nimi + "   |#################   |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.90 * paatos && proge < 0.95 * paatos)
            System.out.print(nimi + "   |##################  |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.95 * paatos && proge < 0.97 * paatos)
            System.out.print(nimi + "   |################### |  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");
        if (proge >= 0.97 * paatos && proge <= 1 * paatos)
            System.out.print(nimi + "   |####################|  " + Math.round(((double) proge / (double) paatos) * 100) + "%\r");


    }

    public static int[] split(int x, int n) {

        int[] output = new int[n];
// If we cannot split the
// number into exactly 'N' parts

        if (x < n)
            System.out.print("-1 ");


            // If x % n == 0 then the minimum
            // difference is 0 and all
            // numbers are x / n
        else if (x % n == 0) {
            for (int i = 0; i < n; i++) {
                output[i] = x / n;

            }
            return output;
            //System.out.print((x/n)+" ");
        } else {

            // upto n-(x % n) the values
            // will be x / n
            // after that the values
            // will be x / n + 1
            int zp = n - (x % n);
            int pp = x / n;
            for (int i = 0; i < n; i++) {

                if (i >= zp)
                    output[i] = pp + 1;
                else
                    output[i] = pp;
            }
        }

        return output;
    }

    public static void addIndexFiles(ArrayList<LASReader> in) throws Exception {

        File temppi;
        for (int i = 0; i < in.size(); i++) {
            temppi = fo.createNewFileWithNewExtension(in.get(i).getFile(), ".lasx");
            // new File(in.get(i).path.getAbsolutePath().replaceFirst("[.][^.]+$", "") + ".lasx");

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

        long tStart = System.currentTimeMillis();

        argumentReader aR = new argumentReader(args);
        aR.setExecDir(System.getProperty("user.dir"));
        aR.parseArguents();

        String pathSep = System.getProperty("file.separator");

        if (!System.getProperty("os.name").equals("Linux"))
            pathSep = "\\" + pathSep;

        boolean lasFormat = false;
        boolean txtFormat = false;

        lasFormat = new File(aR.files[0]).getName().split("\\.")[1].equals("las");
        txtFormat = new File(aR.files[0]).getName().split("\\.")[1].equals("txt");


        ArrayList<String> filesList = new ArrayList<String>();

        ArrayList<LASReader> pointClouds = new ArrayList<LASReader>();
        ArrayList<File> inputFiles = new ArrayList<>();

        if (lasFormat) {


            filesList.addAll(Arrays.asList(aR.files));


            for (int i = 0; i < filesList.size(); i++) {

                inputFiles.add(new File(filesList.get(i)));

            }
        }


        if (txtFormat) {

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
                //System.out.println(filesList.get(i));
                //pointClouds.add(new LASReader(new File(filesList.get(i))));
                inputFiles.add(new File(filesList.get(i)));

            }
        }
        proge.reset();

        try {
            addIndexFiles(pointClouds);
        } catch (Exception e) {
            e.printStackTrace();
        }

        aR.setInputFiles(inputFiles);

        aR.p_update.totalFiles = aR.pointClouds.size();

        //System.out.println(Arrays.toString(aR.inputFiles.toArray()));

        fileDistributor fD = new fileDistributor(aR.inputFiles);

        if (aR.tool == 1) {
            Tiler tile = new Tiler(aR.inputFiles, aR);
        }

        if (aR.tool == 2) {

            Merger merge = new Merger(aR.inputFiles, aR.output, aR.getInclusionRule(), aR.odir, aR);

        }

        if (aR.tool == 3) {

            if (aR.cores > 1) {
                proge.setEnd(aR.inputFiles.size());

                if (aR.cores > aR.inputFiles.size())
                    aR.cores = aR.inputFiles.size();

                ArrayList<Thread> lista11 = new ArrayList<Thread>();

                for (int ii = 1; ii <= aR.cores; ii++) {

                    proge.addThread();
                    Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
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

            } else {

                for (int i = 0; i < filesList.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    Noise nois = new Noise(temp, aR, 1);
                }
            }

        }

        if (aR.tool == 4) {

            if (aR.cores > 1) {
                proge.setEnd(aR.inputFiles.size());

                if (aR.cores > aR.inputFiles.size())
                    aR.cores = aR.inputFiles.size();

                ArrayList<Thread> lista11 = new ArrayList<Thread>();

                for (int ii = 1; ii <= aR.cores; ii++) {

                    proge.addThread();
                    Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
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

            } else {
                for (int i = 0; i < filesList.size(); i++) {
                    LASReader temp = new LASReader(aR.inputFiles.get(i));


                    GroundDetector det = new GroundDetector(temp, false, aR.output, aR.odir, aR.getInclusionRule(), aR.angle, aR.numarg1, aR.axgrid, aR, 1);

                    int[] asdi = det.detectSeedPoints();

                    det.detect();
                }
            }

        }

        if (aR.tool == 5) {
            if (aR.cores > 1) {
                proge.setEnd(aR.inputFiles.size());

                if (aR.cores > aR.inputFiles.size())
                    aR.cores = aR.inputFiles.size();

                ArrayList<Thread> lista11 = new ArrayList<Thread>();

                for (int ii = 1; ii <= aR.cores; ii++) {

                    proge.addThread();
                    Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
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


                }
            }

        }

        if (aR.tool == 6) {

            if (aR.cores > 1) {
                proge.setEnd(aR.inputFiles.size());

                if (aR.cores > aR.inputFiles.size())
                    aR.cores = aR.inputFiles.size();

                ArrayList<Thread> lista11 = new ArrayList<Thread>();

                for (int ii = 1; ii <= aR.cores; ii++) {

                    proge.addThread();
                    Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
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

            } else {
                for (int i = 0; i < filesList.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    Thinner thi = new Thinner(temp, aR.step, aR, 1);

                }
            }

        }

        if (aR.tool == 7) {

            if (aR.cores > 1) {
                proge.setEnd(aR.inputFiles.size());

                if (aR.cores > aR.inputFiles.size())
                    aR.cores = aR.inputFiles.size();

                ArrayList<Thread> lista11 = new ArrayList<Thread>();

                for (int ii = 1; ii <= aR.cores; ii++) {

                    proge.addThread();
                    Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
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

            } else {
                for (int i = 0; i < filesList.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    createCHM.chm testi = new createCHM.chm(temp, "y", 1, aR, 1);

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
                proge.setEnd(aR.inputFiles.size());

                if (aR.cores > aR.inputFiles.size())
                    aR.cores = aR.inputFiles.size();

                ArrayList<Thread> lista11 = new ArrayList<Thread>();

                for (int ii = 1; ii <= aR.cores; ii++) {

                    proge.addThread();
                    Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
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

            } else {
                for (int i = 0; i < filesList.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    Boundary bound = new Boundary(temp, aR.odir, aR.output, false, aR, 1);

                }
            }
        }

        if (aR.tool == 10) {

            if (aR.cores > 1) {
                proge.setEnd(aR.inputFiles.size());

                if (aR.cores > aR.inputFiles.size())
                    aR.cores = aR.inputFiles.size();

                ArrayList<Thread> lista11 = new ArrayList<Thread>();

                for (int ii = 1; ii <= aR.cores; ii++) {

                    proge.addThread();
                    Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
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

            } else {
                for (int i = 0; i < filesList.size(); i++) {
                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    Boundary bound = new Boundary(temp, aR.odir, aR.output, false, aR, 1);

                }
            }

        }

        if (aR.tool == 11) {

            if (aR.cores > 1) {
                proge.setEnd(aR.inputFiles.size());

                if (aR.cores > aR.inputFiles.size())
                    aR.cores = aR.inputFiles.size();

                ArrayList<Thread> lista11 = new ArrayList<Thread>();

                for (int ii = 1; ii <= aR.cores; ii++) {

                    proge.addThread();
                    Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
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

            } else {
                for (int i = 0; i < filesList.size(); i++) {
                    //System.out.println("Odir: " + odir);
                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    ToShp toshape = new ToShp(aR.output, temp, aR.getInclusionRule(), aR.odir, aR.oparse, aR);
                    //LASutils.Boundary bound = new LASutils.Boundary(pointClouds.get(i), odir, output, false);

                }
            }
        }

        if (aR.tool == 12) {

            aR.p_update.las2txt_oparse = aR.oparse;

            if (aR.cores > 1) {
                proge.setEnd(aR.inputFiles.size());

                if (aR.cores > aR.inputFiles.size())
                    aR.cores = aR.inputFiles.size();

                ArrayList<Thread> lista11 = new ArrayList<Thread>();

                for (int ii = 1; ii <= aR.cores; ii++) {

                    proge.addThread();
                    Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
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

            } else {


                for (int i = 0; i < filesList.size(); i++) {
                    LASReader temp = new LASReader(aR.inputFiles.get(i));
                    las2txt ddd = new las2txt(temp, aR.odir, aR.oparse, aR, 1);

                }
            }
        }

        if (aR.tool == 13) {

            if (aR.cores > 1) {
                proge.setEnd(aR.inputFiles.size());

                if (aR.cores > aR.inputFiles.size())
                    aR.cores = aR.inputFiles.size();

                ArrayList<Thread> lista11 = new ArrayList<Thread>();

                for (int ii = 1; ii <= aR.cores; ii++) {

                    proge.addThread();
                    Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
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

            } else {

                las2las tooli = new las2las(1);

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
            polyBank = readPolygonsFromWKT(aR.poly, plotID);

            aR.setPolyBank(polyBank);

            aR.p_update.las2dsm_print = false;

            aR.p_update.lasITD_CHMresolution = aR.step;
            aR.p_update.lasITD_gaussianKernel = aR.kernel;
            aR.p_update.lasITD_gaussiantheta = aR.theta;
            aR.p_update.lasITD_itdKernel = Math.max(aR.dist, 1);


            if (aR.cores > 1) {
                proge.setEnd(aR.inputFiles.size());

                if (aR.cores > aR.inputFiles.size())
                    aR.cores = aR.inputFiles.size();

                ArrayList<Thread> lista11 = new ArrayList<Thread>();

                for (int ii = 1; ii <= aR.cores; ii++) {

                    proge.addThread();
                    Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
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

            } else {
                for (int i = 0; i < filesList.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    aR.p_update.threadFile[0] = "CHM";
                    aR.p_update.updateProgressITD();
                    createCHM.chm testi = new createCHM.chm(temp, "y", 1, aR, 1);

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

                    createCHM.WaterShed fill = new createCHM.WaterShed(testi.treeTops, 0.2, testi.filtered, testi, aR, 1);

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
                    //aR.pointClouds.get(i).index((int)aR.step);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        if (aR.tool == 17) {

            if (aR.cores > 1) {
                proge.setEnd(aR.inputFiles.size());

                if (aR.cores > aR.inputFiles.size())
                    aR.cores = aR.inputFiles.size();

                ArrayList<Thread> lista11 = new ArrayList<Thread>();

                for (int ii = 1; ii <= aR.cores; ii++) {

                    proge.addThread();
                    Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
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

            } else {
                for (int i = 0; i < aR.inputFiles.size(); i++) {

                    LASReader temp = new LASReader(aR.inputFiles.get(i));

                    try {
                        lasSort sort = new lasSort(temp, aR);


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        if (aR.tool == 18) {

            if (aR.cores > 1) {
                proge.setEnd(aR.inputFiles.size());

                if (aR.cores > aR.inputFiles.size())
                    aR.cores = aR.inputFiles.size();

                ArrayList<Thread> lista11 = new ArrayList<Thread>();

                for (int ii = 1; ii <= aR.cores; ii++) {

                    proge.addThread();
                    Thread temp = new Thread(new multiThreadTool(aR, aR.cores, ii, fD));
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
            }

        }

        /* Compute ITD statistics */
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
                //neuralNetworkHyperparameterOptimization te = new neuralNetworkHyperparameterOptimization(aR);
                //neuralNetworkHyperparameterOptimization_convolution te = new neuralNetworkHyperparameterOptimization_convolution(aR);
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

                    lasGridStats lGS = new lasGridStats(temp, aR);

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
                    createCHM.chm testi = new createCHM.chm(temp, "y", 1, aR, 1);

                    las2solar_photogrammetry l2s = new las2solar_photogrammetry(testi.outputFileName, aR, temp, false);
                }




            }else {
                try {

                    for (int i = 0; i < aR.inputFiles.size(); i++) {


                        LASReader temp = new LASReader(aR.inputFiles.get(i));

                        createCHM.chm testi = new createCHM.chm(temp, "y", 1, aR, 1);
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

                    createCHM.chm testi = new createCHM.chm(temp, "y", 1, aR, 1);

                    cc.canopy_cover_chm(testi.filtered);

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;

        long minutes = (tDelta / 1000)  / 60;
        int seconds = (int)((tDelta / 1000) % 60);

        System.out.println("Processing took: " + minutes + " min " + seconds + " sec");

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
     * A class to implement the method progebar() in
     * multiple threads.
     *
     * @author Kukkonen Mikko
     * @version 0.1
     * @since 06.03.2018
     */

    public static class ThreadProgressBar {

        int current = 0;
        int end = 0;
        String name = "give me name!";
        int numberOfThreads = 0;

        public ThreadProgressBar() {

        }

        /**
         * Set the maximum value of the iterator to:
         *
         * @param newEnd
         */
        public synchronized void setEnd(int newEnd) {
            end = newEnd;
        }

        /**
         * Increment the current progress by amount equal to:
         *
         * @param input
         */

        public synchronized void updateCurrent(int input) {

            current += input;

        }

        /**
         * Reset the class
         */

        public synchronized void reset() {

            current = 0;
            numberOfThreads = 0;
            end = 0;
            name = "give me name!";

        }

        /**
         * Name of the process being tracked
         */

        public void setName(String nimi) {
            //System.out.println("Setting name to");
            name = nimi;

        }

        /**
         * Add a thread
         */

        public void addThread() {

            numberOfThreads++;

        }

        public synchronized void print() {
            //System.out.println(end);
            progebar(end, current, " " + name);
            //System.out.println(end + " " + current);
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
        ArrayList<String> tiedostot;
        ArrayList<String> returnList = new ArrayList<String>();
        int numberOfCores;
        int coreNumber;
        boolean echoClass = false;
        String odir;
        argumentReader aR;

        public multiTXT2LAS(ArrayList<String> tiedostot2, String parse2, int numberOfCores2, int coreNumber2, String odir2, boolean echoClass, argumentReader aR) {

            this.aR = aR;
            tiedostot = tiedostot2;
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

                    int jako = (int) Math.ceil((double) tiedostot.size() / (double) numberOfCores);
                    //System.out.println(plotID1.size() / (double)cores);
                    if (coreNumber != numberOfCores) {

                        pienin = (coreNumber - 1) * jako;
                        suurin = coreNumber * jako;
                    } else {
                        pienin = (coreNumber - 1) * jako;
                        suurin = tiedostot.size();
                    }

                    tiedostot = new ArrayList<String>(tiedostot.subList(pienin, suurin));
                    //System.out.println(tiedostot);
                    //polyBank = new ArrayList<double[][]>(polyBank1.subList(pienin, suurin));

                } else {

                    //tiedostot = new ArrayList<Double>(tiedostot);
                    //polyBank = new ArrayList<double[][]>(polyBank1);
                }


                ArrayList<File> from = new ArrayList<File>();
                ArrayList<LASraf> to = new ArrayList<LASraf>();
                ArrayList<String> outList = new ArrayList<String>();

                for (int i = 0; i < tiedostot.size(); i++) {

                    File tempFile = new File(tiedostot.get(i));

                    File toFile = null;
/*
                    if (odir.equals("asd"))

                        toFile = fo.createNewFileWithNewExtension(tempFile, ".las");
                    //new File(tiedostot.get(i).replaceFirst("[.][^.]+$", "") + ".las");

                    if (!odir.equals("asd"))

                        toFile = fo.createNewFileWithNewExtension(tempFile, odir, ".las");

 */
                    toFile = aR.createOutputFile(tempFile);

                    //new File(odir + System.getProperty("file.separator") + tempFile.getName().replaceFirst("[.][^.]+$", "") + ".las");

                    //System.out.println(toFile);
                    File fromFile = new File(tiedostot.get(i));

                    //System.out.println(odir + System.getProperty("file.separator") + tempFile.getName().replaceFirst("[.][^.]+$", "") + ".las");

                    if (toFile.exists())
                        toFile.delete();

                    toFile.createNewFile();

                    //System.out.println(toFile);
                    from.add(fromFile);

                    to.add(new LASraf(toFile));
                    //System.exit(0);
                    outList.add(fo.createNewFileWithNewExtension(tempFile, ".las").getAbsolutePath());
                    //outList.add(tiedostot.get(i).replaceFirst("[.][^.]+$", "") + ".las");

                }

                PointInclusionRule rule = new PointInclusionRule();
                tiedostoLista.add(outList);

                for (int i = 0; i < tiedostot.size(); i++) {

                    LASwrite.txt2las(from.get(i), to.get(i), parse, "txt2las", "\t", rule, echoClass, aR);
                    to.get(i).writeBuffer2();
                    to.get(i).close();
                    //System.out.println("GOT HERE");
                    proge.updateCurrent(1);
                    if (i % (proge.end / 20 + 1) == 0)
                        proge.print();
                }
                //return 1;
            } catch (Exception e) {
                //System.out.println(e);
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
            int jako = -1;

            int[] n_per_thread = split(aR.inputFiles.size(), aR.cores);

            try {

                int pienin = 0;
                int suurin = 0;

                if (nCore != 0) {

                    jako = (int) Math.ceil((double) aR.inputFiles.size() / (double) nCores);

                    int howMany = -n_per_thread[0];
                    for (int i = 0; i < nCore; i++) {

                        howMany += n_per_thread[i];

                    }
                    pienin = howMany;
                    suurin = Math.min(howMany + n_per_thread[nCore - 1], aR.inputFiles.size());

                    this.inputFiles = new ArrayList<>(aR.inputFiles.subList(pienin, suurin));

                } else {

                }
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
                        createCHM.chm testi = new createCHM.chm(temp, "y", 1, aR, nCore);

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

                if (aR.tool == 13) {

                    las2las tooli = new las2las(nCore);

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

                    while (true) {

                        if (fD.isEmpty())
                            break;

                        File f = fD.getFile();

                        if (f == null)
                            continue;

                        LASReader temp = new LASReader(f);
                        aR.p_update.threadFile[nCore - 1] = "CHM";
                        aR.p_update.updateProgressITD();
                        createCHM.chm testi = new createCHM.chm(temp, "y", 1, aR, nCore);

                        aR.p_update.threadFile[nCore - 1] = "treeTops";
                        aR.p_update.updateProgressITD();
                        testi.detectTreeTops((int) aR.p_update.lasITD_itdKernel);

                        aR.p_update.threadFile[nCore - 1] = "waterShed";
                        aR.p_update.updateProgressITD();
                        createCHM.WaterShed fill = new createCHM.WaterShed(testi.treeTops, 0.2, testi.filtered, testi, aR, nCore);

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
                            lasSort sort = new lasSort(temp, aR);


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

