package utils;

import LASio.*;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import err.argumentException;
import err.lasFormatException;
import err.toolException;
import org.agrona.concurrent.SystemEpochClock;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

@SuppressWarnings("unchecked")
public class argumentReader {

    public double pp_x_offset = 0.0;
    public double pp_y_offset = 0.0;

    public String stringArgument1 = null;
    public boolean convo = false;
    public boolean subsetColumnNamesVMI = false;
    public boolean compress_output = false;
    public File configFile2 = null;
    public File configFile = null;
    public int origCores = 1;

    public String userString1 = null;

    public String path = null;
    public ArrayList<String> metadataitems = new ArrayList<>();
    public ArrayList<String> metadataitemsModNames = new ArrayList<>();
    public ArrayList<File> inputFilesSpectral = new ArrayList<>();
    public int nBands = 3;
    public boolean rasterizeColor = false;
    public boolean rasterizeIntensity = false;

    public float extraByteFloat = 0;
    public ArrayList<String> extraByteNames = new ArrayList<>();
    public double adjustKappa = 0.0;
    public boolean PREMOTO_ADAPTIVEDISTANCE = false;
    public boolean outputMask = false;
    public String metadatafile = null;
    public boolean mapSheetExtent = false;
    public boolean noLasUtilsInput = false;
    public File logFile = null;

    public String extraByteName = "asd";
    public File aux_file = null;
    public double radius = 0.0;
    public boolean turnHexagon = false;

    public boolean onlyConvolutionMetrics = false;

    public boolean clip_to_circle = false;

    public boolean thinToCenter = false;
    public int image_height = 0;

    public threadProgressbars prog = new threadProgressbars();
    public ArrayList<File> ref = new ArrayList<>();
    public String[] ref_;

    public String identifier = null;
    public ArrayList<File> tar = new ArrayList<>();
    public String[] tar_;
    public String[] gr;
    public String exclude = null;
    public String include = null;
    public HashMap<Integer, HashSet<Integer>> tree_belongs_to_this_plot = null;

    public ArrayList<Integer> create_extra_byte_vlr = new ArrayList<>();
    public ArrayList<String> create_extra_byte_vlr_name = new ArrayList<>();
    public ArrayList<String> create_extra_byte_vlr_description = new ArrayList<>();
    public ArrayList<Integer> create_extra_byte_vlr_n_bytes = new ArrayList<>();

    public File target = null;

    public boolean MML_klj = false;
    public boolean overWrite = false;
    public File amapVoxFile = null;

    public File ITC_metrics_file = null;

    public boolean eaba = false;

    public boolean save_to_p_id = false;

    public boolean use_p_source = false;

    public boolean ray_trace = false;

    public double min_edge_length = 0.5;

    public boolean convolution_metrics_train = false;
    public boolean convolution_metrics = true;

    public boolean noConvolution = true;
    public boolean output_only_itc_segments = false;

    public int thread_safe_id = 0;

    public File treeTops = null;

    public boolean noLas = false;

    public boolean skeleton_output = false;

    public boolean noModify = true;

    public ArrayList<File> grounds = new ArrayList<>();

    public boolean harmonized = false;

    public lasReadWriteFactory pfac;
    public double edges = 0.0;

    public lasClipMetricOfile lCMO = new lasClipMetricOfile(this);

    public File mergedPointCloud = null;

    public File debug_file = null;

    public boolean classify = false;

    public double std_threshold = 1.75;

    public int EPSG = 3067;

    public boolean input_in_radians = false;

    public int num_iter = 3;

    public double filter_intensity = 2.5;

    public boolean olas = false;

    public double altitude = 0.0;
    public String[] files;

    public int layers = 1;

    public File measured_trees = null;
    public File measured_trees_2 = null;

    public boolean mode_3d = false;

    public int field = 0;
    public String field_string = "";
    public int drop_scan_angle_below = -1;
    public int drop_scan_angle_above = -1;
    //public String pathSep = System.getProperty("file.separator");
    public String execDir;
    public File execDir_file;
    String firstDir = "null";

    public boolean highest = true;
    public boolean lowest = false;

    fileOperations fo = new fileOperations();
    public ArrayList<LASReader> pointClouds = new ArrayList<LASReader>();
    public ArrayList<File> inputFiles = new ArrayList<>();
    public ArrayList<File> outputFiles = new ArrayList<File>();

    public ArrayList<File> groundPointFiles = new ArrayList<>();

    public double decimate_tin = -1;

    public int tool = -1;
    public String tool_string = null;

    public int batch_size = 32;

    ArrayList<String> ORIGINAL_FILES = new ArrayList<String>();

    public int prepare_nn_input = 0;

    public double orig_x = -1;
    public double orig_y = -1;

    public double gdal_cache_gb = 7.5;

    public boolean interpolate = true;

    public boolean rasterizeInterpolate = false;
    public boolean lasrelate = false;

    public boolean dz_on_the_fly = false;

    public int label_index = 160;

    public String neural_mode = "merged";

    public boolean rule_when_reading = true;
    public boolean rule_when_writing = false;

    public boolean pitFree = false;

    public String[] args;
    public PointInclusionRule inclusionRule;
    public PointModifyRule modifyRule;

    public String iparse = "xyz";
    public String oparse = "xyz";
    public String output = "asd";
    public String input = "asd";

    public File train_2 = null;
    public File test_2 = null;
    public File validation_2 = null;

    public File train = null;
    public File validation = null;
    public File test = null;
    public int time = 1;


    public double buffer = 0;

    public double res = 15.0;

    public String odir = "asd";
    public String idir = "asd";

    public int few = 5;


    public double step = 15;

    public boolean dense = false;

    public int ground_class = 2;

    public String method;

    public String sep = "\t";

    public boolean lazInput = false;
    public String tmpDirectory = null;
    public boolean axelsson_mirror = false;

    public int cores = 1;

    public String otype = "las";

    public double numarg1;
    public double angle = -999;
    public double axgrid = 20;
    public String groundPoints = "-999";



    public boolean debug = false;



    public int set_seed = -1;

    public boolean by_gps_time = true;
    public boolean by_z_order = false;

    public String poly = "null";
    public String[] poly_2;
    public String[] poly_3 = new String[0];
    public double[] densities = new double[]{1.3, 2.5, 5.0, 7.5, 10.0, 15.0, 20.0, 25.0};
    public double percentiles = 0.05;

    public double z_cutoff = 1.3;
    public int br = 0;

    public boolean omet = false;

    public String pathSep;

    public boolean photogrammetry = false;

    public double translate_x;
    public double translate_y;
    public double translate_z;
    public double translate_i;

    public double scale_x;
    public double scale_y;
    public double scale_z;
    public double scale_i;


    public int set_point_source_id = -1;

    public boolean skip_global = false;

    public String trajectory;

    public boolean thin3d = false;

    public double prob = 0.5;
    public boolean output_statistics = false;

    public boolean split = false;

    public String splitBy = "asd";

    public boolean remove_buffer = false;
    public boolean remove_buffer_2 = false;

    public LASraf outputFile = null;

    public double dist = -999;

    public double theta = 1.0;

    public double learning_rate = 0.01;

    public int kernel = 3;

    public double delta = 5.0;
    public double lambda = 10.0;



    public LasBlock blokki;

    public progressUpdater p_update; // = new progressUpdater(this);

    public String save_file = "bestModel_graph.bin";

    public double concavity = 100.0;

    public double interval = 20.0;

    public boolean echoClass = false;

    public int keep_classification = -999;

    public int change_point_type = -999;
    public int change_version_minor = -999;

    public int convolution_option = 1;

    public boolean concave = false;

    public ArrayList<double[][]> polyBank = new ArrayList<>();

    public boolean setNegativeZero = false;

    Options options;

    public int min_points = 10;

    public boolean o_dz = false;

    public File model = null;

    public boolean mem_efficient = false;

    public String exterior;
    public String interior;

    HashSet<String> addedExtraByteNames = new HashSet<>();

    public int[] sequence = new int[]{0,1,2};

    public boolean output_only_stemAlignInput = false;

    public int globalId = 0;

    long startTimegc = 0L;
    long endTimegc = 10000L;

    boolean noEstimation = true;
    boolean simpleEstimation = false;
    boolean simpleEstimationWithProb = false;
    boolean estimationWithCHM = false;

    boolean estimationSpecialThinning = false;

    /**
     * A sort of a "thread-safe" gc. Avoid calling GC multiple
     * times from different threads too many times (here, all
     * threads share this argumentReader object).
     * @return
     */
    public boolean gc(){

        endTimegc = System.currentTimeMillis();

        if(endTimegc - startTimegc < 10000){

            System.gc();
            startTimegc = System.currentTimeMillis();
            return true;

        }else{
            return false;
        }

    }

    public void setProgressBars(threadProgressbars in){
        this.prog = in;
    }


    public argumentReader(){

    }

    public void setOutputFile(LASraf in){
        this.outputFile = in;
    }

    public void setExecDir(String dir){
        this.execDir = dir;
        this.execDir_file = new File(dir);

    }


    public synchronized int getGlobalId(){
        return this.globalId++;
    }

    public argumentReader(String[] args){
        this.args = args;

        pfac = new lasReadWriteFactory(this);
        //System.out.println(args.length);

        //

        this.modifyRule = new PointModifyRule();
        this.inclusionRule = new PointInclusionRule();
    }

    public synchronized void add_extra_bytes(int data_type, String name, String description){

        /* This is to enable multithreaded tools */
        if(!addedExtraByteNames.contains(name)){
            addedExtraByteNames.add(name);
        }
        else
            return;

        this.create_extra_byte_vlr_description.add(description);
        this.create_extra_byte_vlr_name.add(name);
        this.create_extra_byte_vlr.add(data_type);

        switch (data_type){

            case 0:
                this.create_extra_byte_vlr_n_bytes.add(-1);
                break;

            case 1:
                this.create_extra_byte_vlr_n_bytes.add(1);
                break;

            case 2:
                this.create_extra_byte_vlr_n_bytes.add(1);
                break;

            case 3:
                this.create_extra_byte_vlr_n_bytes.add(2);
                break;

            case 4:
                this.create_extra_byte_vlr_n_bytes.add(2);
                break;

            case 5:
                this.create_extra_byte_vlr_n_bytes.add(4);
                break;

            case 6:
                this.create_extra_byte_vlr_n_bytes.add(4);
                break;

            case 7:
                this.create_extra_byte_vlr_n_bytes.add(8);
                break;

            case 8:
                this.create_extra_byte_vlr_n_bytes.add(8);
                break;

            case 9:
                this.create_extra_byte_vlr_n_bytes.add(4);
                break;

            case 10:
                this.create_extra_byte_vlr_n_bytes.add(8);
                break;

            case 11:
                this.create_extra_byte_vlr_n_bytes.add(2);
                break;

            case 12:
                this.create_extra_byte_vlr_n_bytes.add(2);
                break;

            case 13:
                this.create_extra_byte_vlr_n_bytes.add(4);
                break;

            case 14:
                this.create_extra_byte_vlr_n_bytes.add(4);
                break;

            case 15:
                this.create_extra_byte_vlr_n_bytes.add(8);
                break;

            case 16:
                this.create_extra_byte_vlr_n_bytes.add(8);
                break;

            case 17:
                this.create_extra_byte_vlr_n_bytes.add(16);
                break;

            case 18:
                this.create_extra_byte_vlr_n_bytes.add(8);
                break;

            case 19:
                this.create_extra_byte_vlr_n_bytes.add(16);
                break;

            case 20:
                this.create_extra_byte_vlr_n_bytes.add(8);
                break;

            case 21:
                this.create_extra_byte_vlr_n_bytes.add(3);
                break;

            case 22:
                this.create_extra_byte_vlr_n_bytes.add(3);
                break;
            case 23:
                this.create_extra_byte_vlr_n_bytes.add(6);
                break;
            case 24:
                this.create_extra_byte_vlr_n_bytes.add(6);
                break;
            case 25:
                this.create_extra_byte_vlr_n_bytes.add(12);
                break;
            case 26:
                this.create_extra_byte_vlr_n_bytes.add(12);
                break;
            case 27:
                this.create_extra_byte_vlr_n_bytes.add(24);
                break;

            case 28:
                this.create_extra_byte_vlr_n_bytes.add(24);
                break;
            case 29:
                this.create_extra_byte_vlr_n_bytes.add(12);
                break;
            case 30:
                this.create_extra_byte_vlr_n_bytes.add(24);
                break;

        }

    }

    public void createOpts(){

        options = new Options();

        options.addOption(Option.builder()
                .longOpt("amapVoxFile")
                .hasArg(true)
                .desc("AmapVox file path")
                .required(false)
                .build());

        options.addOption( org.apache.commons.cli.Option.builder("i")
                .longOpt("input")
                .hasArg(true)
                .desc("Input data")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .required(false)
                .build());

        options.addOption( Option.builder()
                .longOpt("inputSpectral")
                .hasArg(true)
                .desc("Input data Spectral")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .required(false)
                .build());

        options.addOption( Option.builder()
                .longOpt("metadataItems")
                .hasArg(true)
                .desc("Metadata items to consider")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .required(false)
                .build());


        options.addOption( Option.builder()
                .longOpt("extraBytes")
                .hasArg(true)
                .desc("Extra byte names for txt2las")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .required(false)
                .build());

        options.addOption( Option.builder()
                .longOpt("ground")
                .hasArg(true)
                .desc("Ground las files")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .required(false)
                .build());

        options.addOption( Option.builder()
                .longOpt("name")
                .hasArg(true)
                .desc("naming convention")
                .required(false)
                .build());

        options.addOption( Option.builder()
                .longOpt("path")
                .hasArg(true)
                .desc("naming convention")
                .required(false)
                .build());

        options.addOption(Option.builder("o")
                .longOpt("output")
                .hasArg(true)
                .desc("Output")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_z_below")
                .hasArg(true)
                .desc("Drop z below threshold")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_scan_angle_below")
                .hasArg(true)
                .desc("Drop scan angle below threshold")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_scan_angle_above")
                .hasArg(true)
                .desc("Drop scan angle above threshold")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("image_height")
                .hasArg(true)
                .desc("")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("string1")
                .hasArg(true)
                .desc("")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("o_itc")
                .hasArg(false)
                .desc("Output only ITC segments in point cloud")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("overWrite")
                .hasArg(false)
                .desc("Overwrite output file")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("subsetColumns")
                .hasArg(false)
                .desc("VMI SPECIAL")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("mapSheetExtent")
                .hasArg(false)
                .desc("Use extent from MML map sheets (6km)")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_z_above")
                .hasArg(true)
                .desc("Drop z above threshold")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("ref")
                .hasArg(true)
                .desc("Reference point cloud")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("tar")
                .hasArg(true)
                .desc("Target point cloud")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .required(false)
                .build());



        options.addOption(Option.builder()
                .longOpt("raster")
                .hasArg(true)
                .desc("Target point cloud")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("drop_classification")
                .hasArg(true)
                .desc("Drop a point class")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("field_string")
                .hasArg(true)
                .desc("Field id as string")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("config")
                .hasArg(true)
                .desc("Configuration file (usage depends on the tool)")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("config2")
                .hasArg(true)
                .desc("Second configuration file (usage depends on the tool)")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("turn_hexagon")
                .hasArg(false)
                .desc("Rotate points from hexagon to upright pos")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("simpleEstimation")
                .hasArg(false)
                .desc("PREMOTO - simpleEstimation")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("simpleEstimationWithProb")
                .hasArg(false)
                .desc("PREMOTO - simpleEstimationWithProb")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("estimationWithCHM")
                .hasArg(false)
                .desc("PREMOTO - estimationWithCHM")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("estimationSpecialThinning")
                .hasArg(false)
                .desc("PREMOTO - estimationWithCHM")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("convo")
                .hasArg(false)
                .desc("PREMOTO - estimationWithCHM")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_noise")
                .hasArg(false)
                .desc("Drop noise (class 7)")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("compress_output")
                .hasArg(false)
                .desc("Compress output?")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("ray_trace")
                .hasArg(false)
                .desc("Exclude shadow points in computation of metrics")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("keep_classification")
                .hasArg(true)
                .desc("Keep a point class")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("itc_metrics")
                .hasArg(true)
                .desc("ITC_metrics output file")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("prob")
                .hasArg(true)
                .desc("Probability (check tool for more info)")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("externalData")
                .hasArg(false)
                .desc("Data is external to lasutils")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("exclude")
                .hasArg(true)
                .desc("Exclude these ids")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("include")
                .hasArg(true)
                .desc("Include these ids")
                .required(false)
                .build());



        options.addOption(Option.builder()
                .longOpt("pp_x_offset")
                .hasArg(true)
                .desc("Principal point X offset")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("pp_y_offset")
                .hasArg(true)
                .desc("Principal point Y offset")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("concavity")
                .hasArg(true)
                .desc("Concavity")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("aux_file")
                .hasArg(true)
                .desc("Auxiliary data file")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("radius")
                .hasArg(true)
                .desc("radius")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("concave")
                .hasArg(false)
                .desc("concave border")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("only_convo")
                .hasArg(false)
                .desc("asd")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("prep_nn_input")
                .hasArg(true)
                .desc("Reshape .txt input into .nnin")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("measured_trees")
                .hasArg(true)
                .desc("Field measured trees")
                .required(false)
                .build());
        options.addOption(Option.builder()
                .longOpt("measured_trees_2")
                .hasArg(true)
                .desc("Field measured trees_2")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("target")
                .hasArg(true)
                .desc("Target file")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("stringArg1")
                .hasArg(true)
                .desc("stringArg1 stringArg1")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("min_edge_length")
                .hasArg(true)
                .desc("TIN min edge length (for ground filtering)")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("save_to_pointSourceId")
                .hasArg(false)
                .desc("Save polygon id to pointSourceId")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("extra_byte")
                .hasArg(true)
                .desc("Add extra byte to all points")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("tree_tops")
                .hasArg(true)
                .desc("Tree tops shp file")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("only_circle")
                .hasArg(false)
                .desc("asd")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("learning_rate")
                .hasArg(true)
                .desc("neural network learning rate")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("layers")
                .hasArg(true)
                .desc("neural network layers")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("rasterizeInterpolate")
                .hasArg(false)
                .desc("neural network layers")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("identifier")
                .hasArg(true)
                .desc("LAS header identifier")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("neural_mode")
                .hasArg(true)
                .desc("neural network mode")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("seq")
                .hasArgs()
                .desc("Sequence")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("exterior")
                .hasArg(true)
                .desc("exter")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("train")
                .hasArg(true)
                .desc("neural network train set")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("poly2")
                .hasArg(true)
                .desc("Second polyon file")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("MML_klj")
                .hasArg(false)
                .desc("Use MML karttalehtijako")
                .required(false)
                .build());



        options.addOption(Option.builder()
                .longOpt("poly3")
                .hasArg(true)
                .desc("Third polyon file")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("test")
                .hasArg(true)
                .desc("neural network test set")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("validation")
                .hasArg(true)
                .desc("neural network validation set")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("eaba")
                .hasArg(false)
                .desc("Petteris eaba")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("thin_to_center")
                .hasArg(false)
                .desc("Move the thinned point to the center of the cell")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("decimate_tin")
                .hasArg(true)
                .desc("Use a decimated TIN")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("use_p_source")
                .hasArg(false)
                .desc("special use case")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("label_index")
                .hasArg(true)
                .desc("label index")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("train2")
                .hasArg(true)
                .desc("neural network train 2 set")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("test2")
                .hasArg(true)
                .desc("neural network test 2 set")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("validation2")
                .hasArg(true)
                .desc("neural network validation 2 set")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("convolution_metrics")
                .hasArg(false)
                .desc("do not compute convolution metrics")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("field")
                .hasArg(true)
                .desc("field")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("num_iter")
                .hasArg(true)
                .desc("number of iterations")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("interior")
                .hasArg(true)
                .desc("inter")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("std_threshold")
                .hasArg(true)
                .desc("inter")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("highest")
                .hasArg(false)
                .desc("Keep highest point in lasthin")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("axelsson_mirror")
                .hasArg(false)
                .desc("Use the mirroring strategy in ground filtering")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("lowest")
                .hasArg(false)
                .desc("Keep lowest point in lasthin")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("mode_3d")
                .hasArg(false)
                .desc("Use 3d solar")
                .required(false)
                .build());



        options.addOption(Option.builder()
                .longOpt("alt")
                .hasArg(true)
                .desc("flying altitude")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("model")
                .hasArg(true)
                .desc("neural network model")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("gdal_cache_gb")
                .hasArg(true)
                .desc("gdal cache in gb")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("input_in_radians")
                .hasArg(false)
                .desc("Input angles are in radians")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("set_point_source_id")
                .hasArg(true)
                .desc("set point source id to")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("o_dz")
                .hasArg(false)
                .desc("Output delta z")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("harmonized")
                .hasArg(false)
                .desc("Harmonized ground")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("sa")
                .hasArg(false)
                .desc("output only stemalign input")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("premoto_adaptiveDistance")
                .hasArg(false)
                .desc("PREMOTOSTUFF")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("set_seed")
                .hasArg(true)
                .desc("output only stemalign input")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("change_point_format")
                .hasArg(true)
                .desc("Change point type")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("change_version_minor")
                .hasArg(true)
                .desc("Change las version minor")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("dz_")
                .hasArg(false)
                .desc("dz on the fly")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("dense")
                .hasArg(false)
                .desc("output dense ground")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_noise")
                .hasArg(false)
                .desc("Remove points classified as noise")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("remove_buffer")
                .hasArg(false)
                .desc("Remove buffer points (synthetic)")
                .required(false)
                .build());

        options.addOption(Option.builder("h")
                .longOpt("help")
                .hasArg(false)
                .desc("Display help for this tool")
                .required(false)
                .build());



        options.addOption(Option.builder()
                .longOpt("dist")
                .hasArg(true)
                .desc("Dist")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("filter_intensity")
                .hasArg(true)
                .desc("Dist")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("save_file")
                .hasArg(true)
                .desc("name of the file to be saved")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("export_mask")
                .hasArg(false)
                .desc("name of the file to be saved")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("export_color")
                .hasArg(false)
                .desc("las2raster export color")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("export_intensity")
                .hasArg(false)
                .desc("las2raster export intensity")
                .required(false)
                .build());


        options.addOption(Option.builder("c")
                .longOpt("cores")
                .hasArg(true)
                .desc("Number of cores used")
                .numberOfArgs(1)
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("iparse")
                .hasArg(true)
                .desc("Column order of .txt point cloud file")
                .numberOfArgs(1)
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("EPSG")
                .hasArg(true)
                .desc("Coordinate reference system")
                .numberOfArgs(1)
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("metadata")
                .hasArg(true)
                .desc("Metadata .txt file")
                .numberOfArgs(1)
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("epsg")
                .hasArg(true)
                .desc("Coordinate reference system")
                .numberOfArgs(1)
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("buffer")
                .hasArg(true)
                .desc("Buffer size")
                .numberOfArgs(1)
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("classify")
                .hasArg(false)
                .desc("Classify noise points")
                .numberOfArgs(1)
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("debug_file")
                .hasArg(true)
                .desc("debug file")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("oparse")
                .hasArg(true)
                .desc("Column order of  output .txt point cloud file")
                .numberOfArgs(1)
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("read_rules")
                .hasArg(false)
                .desc("Apply point mod in read or write")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("write_rules")
                .hasArg(false)
                .desc("Apply point mod in read or write")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("by_gps_time")
                .hasArg(false)
                .desc("Sort by gps time")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("by_z_order")
                .hasArg(false)
                .desc("Sort by z order curve")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("first_only")
                .hasArg(false)
                .desc("Use only first or only echoes")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("olas")
                .hasArg(false)
                .desc("Output las format")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("keep_first")
                .hasArg(false)
                .desc("Keep first")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("debug")
                .hasArg(false)
                .desc("Debug mode")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_first")
                .hasArg(false)
                .desc("Drop first")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("last_only")
                .hasArg(false)
                .desc("Use only last echoes")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("keep_last")
                .hasArg(false)
                .desc("Keep last")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("keep_intermediate")
                .hasArg(false)
                .desc("Keep last")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_intermediate")
                .hasArg(false)
                .desc("Keep last")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_last")
                .hasArg(false)
                .desc("Drop last")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("keep_only")
                .hasArg(false)
                .desc("Drop last")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("drop_only")
                .hasArg(false)
                .desc("Drop last")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("interpolate")
                .hasArg(false)
                .desc("inter")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_last_of_many")
                .hasArg(false)
                .desc("Drop last of many")
                .required(false)
                .build());
        options.addOption(Option.builder()
                .longOpt("drop_first_of_many")
                .hasArg(false)
                .desc("Drop first of many")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("keep_middle")
                .hasArg(false)
                .desc("Keep _middle")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("pit_free")
                .hasArg(false)
                .desc("Pit-free CHM")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_middle")
                .hasArg(false)
                .desc("Drop _middle")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("keep_single")
                .hasArg(false)
                .desc("Keep _single")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_single")
                .hasArg(false)
                .desc("Drop single")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("keep_double")
                .hasArg(false)
                .desc("Keep double")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_double")
                .hasArg(false)
                .desc("Drop double")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("keep_triple")
                .hasArg(false)
                .desc("Keep triple")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_triple")
                .hasArg(false)
                .desc("Drop triple")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("echo_class")
                .hasArg(false)
                .desc("Output echo class")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("thin3d")
                .hasArg(false)
                .desc("Thin in 3d voxels, rather than in 2d grid")
                .required(false)
                .build());
        options.addOption(Option.builder()
                .longOpt("keep_quadruple")
                .hasArg(false)
                .desc("Keep quadruple")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_quadruple")
                .hasArg(false)
                .desc("Drop quadruple")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("keep_quintuple")
                .hasArg(false)
                .desc("Keep quintuple")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_quintuple")
                .hasArg(false)
                .desc("Drop quintuple")
                .required(false)
                .build());
        options.addOption(Option.builder()
                .longOpt("drop_synthetic")
                .hasArg(false)
                .desc("Drop synthetic points")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_user_data")
                .hasArg(true)
                .desc("Drop specified user data points")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("keep_user_data")
                .hasArg(true)
                .desc("Keep specified user data points")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("set_classification")
                .hasArg(true)
                .desc("Set specified classification tag to all points")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("set_user_data")
                .hasArg(true)
                .desc("Set specified user data tag to all points")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("convolution_option")
                .hasArg(true)
                .desc("Choose which convolution config to use")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("interval")
                .hasArg(true)
                .desc("Inteval?")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("odir")
                .hasArg(true)
                .desc("Output directory")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("idir")
                .hasArg(true)
                .desc("Input directory")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("adjustKappa")
                .hasArg(true)
                .desc("Add this value to kappa")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("few")
                .hasArg(true)
                .desc("Few argument")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("step")
                .hasArg(true)
                .desc("Step argument")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("theta")
                .hasArg(true)
                .desc("Theta argument")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("kernel")
                .hasArg(true)
                .desc("Kernel argument")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("ground_class")
                .hasArg(true)
                .desc("Class number of ground points")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("batch_size")
                .hasArg(true)
                .desc("b size")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("method")
                .hasArg(true)
                .desc("Method??")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("args")
                .hasArg(false)
                .desc("Print tool independent arguments and their explanations.")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("traj")
                .hasArg(true)
                .desc("Trajectory file")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("sep")
                .hasArg(true)
                .desc("separator")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("otype")
                .hasArg(true)
                .desc("Output file type. Las or txt")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("split")
                .hasArg(false)
                .desc("Split or not")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("splitBy")
                .hasArg(true)
                .desc("in lasSplit define what to split with")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("poly")
                .hasArg(true)
                .desc(".shp file location")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("angle")
                .hasArg(true)
                .desc("angle parameter")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("lambda")
                .hasArg(true)
                .desc("ilambda param")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("axGrid")
                .hasArg(true)
                .desc("axGrid")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("time")
                .hasArg(true)
                .desc("neural network train time")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("photogrammetry")
                .hasArg(false)
                .desc("axGrid")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("delta")
                .hasArg(true)
                .desc("delta param")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("translate_x")
                .hasArg(true)
                .desc("translate x")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("edges")
                .hasArg(true)
                .desc("ai2las edgegs")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("translate_y")
                .hasArg(true)
                .desc("translate y")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("translate_z")
                .hasArg(true)
                .desc("translate z")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("translate_i")
                .hasArg(true)
                .desc("translate i")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("scale_i")
                .hasArg(true)
                .desc("scale i")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("skip_global")
                .hasArg(true)
                .desc("skip boresight and leverarm")
                .required(false)
                .build());
        options.addOption(Option.builder()
                .longOpt("ground_points")
                .hasArg(true)
                .desc("File with ground points")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("orig_y")
                .hasArg(true)
                .desc("Origin y")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("orig_x")
                .hasArg(true)
                .desc("Origin x")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("o_stat")
                .hasArg(false)
                .desc("Output plot/gridd statistics")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("mem_eff")
                .hasArg(false)
                .desc("Use memory efficient methods")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("skeleton")
                .hasArg(false)
                .desc("Reduced output - see tool for more information")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("densities")
                .hasArg(true)
                .desc("Vector of densities")
                .required(false)
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .build());

        options.addOption(Option.builder()
                .longOpt("percentiles")
                .hasArg(true)
                .desc("Percentile step")
                .required(false)
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .build());

        options.addOption(Option.builder()
                .longOpt("z_cutoff")
                .hasArg(true)
                .desc("z_cutoff")
                .required(false)
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .build());

        options.addOption(Option.builder()
                .longOpt("res")
                .hasArg(true)
                .desc("Resolution")
                .required(false)
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .build());

        options.addOption(Option.builder()
                .longOpt("omet")
                .hasArg(false)
                .desc("Output metrics in clipping")
                .required(false)
                .build());

        options.addOption(Option.builder("h")
                .longOpt("help")
                .hasArg(false)
                .desc("Print help")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("uef_echoClass")
                .hasArg(false)
                .desc("uef echoclass")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("min_points")
                .hasArg(true)
                .desc("Minimum points in metric calculation")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("nBands")
                .hasArg(true)
                .desc("Number of output/input bands")
                .required(false)
                .build());

    }

    public void parseArguents(String tool) throws IOException{

        this.tool_string = tool;
        parseArguents();

    }

    public void clearPointClouds(){

        for(int i = 0; i < this.pointClouds.size(); i++){

            try {
                this.pointClouds.get(i).close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    public void populatePointClouds(){

        for(int i = 0; i < this.inputFiles.size(); i++){

                try {
                    this.pointClouds.add(new LASReader(this.inputFiles.get(i)));

                }catch (Exception e){
                    e.printStackTrace();
                }
        }
    }

    public void parseArguents() throws IOException {

        this.pathSep = System.getProperty("file.separator");

        if(this.tool_string == null)
            this.tool = Integer.parseInt(args[0]);

        if(args.length <= 1){
            printHelp pH;
            if(this.tool_string == null)
                pH = new printHelp(this.tool);
            else
                pH = new printHelp(this.tool_string);
            System.exit(1);

        }

        createOpts();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        String[] inputFilesArray = new String[1];

        try {
            cmd = parser.parse(options, args);


            if (cmd.hasOption("help")) {

                printHelp pH = new printHelp(this.tool);
                System.exit(1);

            }

            if( cmd.hasOption("extraBytes")){

                String extraBytes = cmd.getOptionValue("extraBytes");

                if(extraBytes.contains(";")) {

                    String[] extraBytesArray = extraBytes.split(";");

                    for (int i = 0; i < extraBytesArray.length; i++) {

                        this.extraByteNames.add(extraBytesArray[i]);

                    }
                }else{

                        this.extraByteNames.add(extraBytes);

                }

            }

            if(cmd.hasOption("ref")){

                ref_ = cmd.getOptionValues("ref");

                if(ref_[0].split(";").length > 1){
                    ref_ = ref_[0].split(";");
                }
                ArrayList<String> temp = new ArrayList<>();

                for(String s : ref_){

                    if(new File(s).getName().contains("las")) {
                        if (new File(s).getName().split("\\.")[1].equals("las")) {

                            LASReader tempReader = new LASReader(new File(s));

                            if (tempReader.getNumberOfPointRecords() < 10) {

                                tempReader.close();
                                continue;
                            }
                            tempReader.close();
                        }
                    }
                    //System.out.println(s);

                    this.ref.add(new File(s));
                }

                System.out.println("SETTING REF!! " + this.ref.size());
                System.out.println(cmd.getOptionValues("ref"));

            }

            if(cmd.hasOption("raster")){

                ref_ = cmd.getOptionValues("raster");

                if(ref_[0].split(";").length > 1){
                    ref_ = ref_[0].split(";");
                }
                ArrayList<String> temp = new ArrayList<>();

                for(String s : ref_){

                    //System.out.println(s);

                    this.ref.add(new File(s));
                }

                System.out.println("SETTING REF!! " + this.ref.size());
                System.out.println(cmd.getOptionValues("ref"));

            }

            if(cmd.hasOption("inputSpectral")){

                ref_ = cmd.getOptionValues("inputSpectral");

                if(ref_[0].split(";").length > 1){
                    ref_ = ref_[0].split(";");
                }

                for(String s : ref_){

                    this.inputFilesSpectral.add(new File(s));

                }
            }


            if(cmd.hasOption("metadataItems")){
                ref_ = cmd.getOptionValues("metadataItems");

                if(ref_[0].split(";").length > 0){
                    ref_ = ref_[0].split(";");
                }

                for(String s : ref_){

                    if(s.equals("DATA_DATE"))
                        this.metadataitemsModNames.add("DATE");
                    else
                        this.metadataitemsModNames.add(s);

                    this.metadataitems.add(s);
                }

            }

            if(cmd.hasOption("tar")){

                tar_ = cmd.getOptionValues("tar");
                if(tar_[0].split(";").length > 1){
                    tar_ = tar_[0].split(";");
                }
                ArrayList<String> temp = new ArrayList<>();

                for(String s : tar_){

                    //System.out.println(s);
                    LASReader tempReader = new LASReader(new File(s));

                    if(tempReader.getNumberOfPointRecords() < 10){

                        tempReader.close();
                        continue;
                    }
                    tempReader.close();

                    this.tar.add(new File(s));
                }

            }

            if(cmd.hasOption("ground")){

                gr = cmd.getOptionValues("ground");
                if(gr[0].split(";").length > 1){
                    gr = gr[0].split(";");
                }
                ArrayList<String> temp = new ArrayList<>();

                for(String s : gr){

                    //System.out.println(s);

                    this.grounds.add(new File(s));
                }

            }

            if (cmd.hasOption("i")) {

                files = cmd.getOptionValues("i");
                System.out.println("FILES: " + files.length + " " + files[0]);

                ArrayList<String> files_ = new ArrayList<>();

                // get the filename without path of files[0]


                if(files.length == 1){

                    String filenameWithoutPath = files[0].substring(files[0].lastIndexOf(this.pathSep) + 1);

                    if(filenameWithoutPath.equals("files.txt")) {

                        //files = Files.readAllLines(Paths.get(files[0])).toArray(new String[0]);

                        // read the file line by line

                        BufferedReader br = new BufferedReader(new FileReader(files[0]));
                        String line;

                        while ((line = br.readLine()) != null) {
                            //System.out.println(line);
                            files_.add(line);
                        }

                        files = new String[files_.size()];

                        for (int i = 0; i < files_.size(); i++) {
                            files[i] = files_.get(i);
                        }

                        for (int i = 0; i < files.length; i++) {
                            //System.out.println(files[i]);
                        }
                    }
                }

                if(files[0].split(";").length > 1){
                    files = files[0].split(";");
                }

                ArrayList<String> temp = new ArrayList<>();
                for(String s : files) {
                    ORIGINAL_FILES.add(s);
                }

                for(String s : files) {

                    if(new File(s).getName().contains("laz")){
                        this.convertLasToLaz();
                    }

                }

                for(String s : files){

                    //System.out.println(s);
                    if(new File(s).getName().contains("las")) {
                        if (new File(s).getName().split("\\.")[1].equals("las")) {

                            LASReader tempReader = null;

                            try {
                                tempReader = new LASReader(new File(s));

                                long numberOfPointRecords = tempReader.getNumberOfPointRecords();

                                tempReader.close();
                                tempReader = null;

                                if(numberOfPointRecords < 10){

                                    continue;
                                }
                                temp.add(s);
                                this.inputFiles.add(new File(s));

                            } catch (Exception e) {
                            }

                        } else {
                            temp.add(s);
                            this.inputFiles.add(new File(s));
                        }
                    }else{
                        temp.add(s);
                        this.inputFiles.add(new File(s));
                    }
                }

                System.gc();

                files = temp.toArray(new String[0]);


            }

            if (cmd.hasOption("poly2")) {

                poly_2 = cmd.getOptionValues("poly2");
                if(poly_2[0].split(";").length > 1){
                    poly_2 = poly_2[0].split(";");
                }

            }

            if(cmd.hasOption("string1")){
                this.userString1 = cmd.getOptionValue("string1");
            }

            if (cmd.hasOption("poly3")) {

                poly_3 = cmd.getOptionValues("poly3");
                if(poly_3[0].split(";").length > 1){
                    poly_3 = poly_3[0].split(";");
                }

            }


            if (cmd.hasOption("seq")) {

                String sequ = cmd.getOptionValue("seq");

                String[] seqit = sequ.split(",");

                this.sequence = new int[seqit.length];

                for(int i = 0; i < seqit.length; i++){

                    sequence[i] = Integer.parseInt(seqit[i]) - 1;

                }

            }

            if (cmd.hasOption("rasterizeInterpolate")){
                this.rasterizeInterpolate = true;
            }

            if (cmd.hasOption("c")) {

                this.cores = Integer.parseInt(cmd.getOptionValue("c"));
                this.origCores = this.cores;
            }

            if (cmd.hasOption("set_seed")) {

                this.set_seed = Integer.parseInt(cmd.getOptionValue("set_seed"));
            }

            if(cmd.hasOption("change_point_format")){

                this.noModify = false;

                this.change_point_type = Integer.parseInt(cmd.getOptionValue("change_point_format"));

                this.inclusionRule.changePointFormat(this.change_point_type);

                if(this.change_point_type < 0 || this.change_point_type > 10)
                    throw new argumentException("Nonsense point format (-change_point_format).");
            }

            if(cmd.hasOption("change_version_minor")){

                this.change_version_minor = Integer.parseInt(cmd.getOptionValue("change_version_minor"));

                if(change_version_minor > 4 || change_version_minor < 1)
                    throw new argumentException("Incomprehensible version minor (-change_version_minor). Great Scott!!");

            }

            if( cmd.hasOption("path")){

                this.path = cmd.getOptionValue("path");

            }

            if (cmd.hasOption("label_index")) {

                this.label_index = Integer.parseInt(cmd.getOptionValue("label_index"));
            }

            if (cmd.hasOption("drop_classification")) {
                this.noModify = false;
                this.inclusionRule.dropClassification(Integer.parseInt(cmd.getOptionValue("drop_classification")));

            }

            if (cmd.hasOption("drop_noise")) {
                this.noModify = false;
                this.inclusionRule.dropNoise();

            }

            if (cmd.hasOption("drop_user_data")) {
                this.noModify = false;
                this.inclusionRule.dropUserData(Integer.parseInt(cmd.getOptionValue("drop_user_data")));

            }

            if (cmd.hasOption("keep_user_data")) {
                this.noModify = false;
                this.inclusionRule.keepUserData(Integer.parseInt(cmd.getOptionValue("keep_user_data")));

            }

            if (cmd.hasOption("compress_output")) {

                this.compress_output = true;

            }

            if (cmd.hasOption("learning_rate")) {

                this.learning_rate = (Double.parseDouble(cmd.getOptionValue("learning_rate")));
            }

            if (cmd.hasOption("layers")) {

                this.layers = (Integer.parseInt(cmd.getOptionValue("layers")));

            }

            if (cmd.hasOption("eaba")) {

                this.eaba = true;

            }

            if(cmd.hasOption("convo")){
                this.convo = true;
            }

            if (cmd.hasOption("iparse")) {

                this.iparse = cmd.getOptionValue("iparse");

            }

            if (cmd.hasOption("field_string")) {

                this.field_string = (cmd.getOptionValue("field_string"));
            }

            if (cmd.hasOption("EPSG")) {

                this.EPSG = Integer.parseInt(cmd.getOptionValue("EPSG"));
            }

            if (cmd.hasOption("epsg")) {

                this.EPSG = Integer.parseInt(cmd.getOptionValue("epsg"));
            }



            if (cmd.hasOption("oparse")) {

                this.oparse = cmd.getOptionValue("oparse");
            }

            if (cmd.hasOption("debug_file")) {

                this.debug_file = new File(cmd.getOptionValue("debug_file"));

                if(!this.debug_file.exists())
                    throw new argumentException("-debug_file does not exist!");
            }

            if(cmd.hasOption("turn_hexagon")){

                this.turnHexagon = true;

            }

            if(cmd.hasOption("radius")){

                this.radius = Double.parseDouble(cmd.getOptionValue("radius"));

            }

            if (cmd.hasOption("o")) {

                this.output = cmd.getOptionValue("o");
            }

            if (cmd.hasOption("step")) {

                this.step = Double.parseDouble(cmd.getOptionValue("step"));

            }
            if (cmd.hasOption("std_threshold")) {

                this.std_threshold = Double.parseDouble(cmd.getOptionValue("std_threshold"));

            }
            if (cmd.hasOption("alt")) {

                this.altitude = Double.parseDouble(cmd.getOptionValue("alt"));

                if(this.altitude < 0)
                    throw new argumentException("Negative altitude is impossible.");
            }

            if(cmd.hasOption("decimate_tin")){

                this.decimate_tin = Double.parseDouble(cmd.getOptionValue("decimate_tin"));
            }

            if(cmd.hasOption("args")){
                printHelp pH = new printHelp(9999);
            }

            if(cmd.hasOption("only_circle")){

                this.clip_to_circle = true;

            }


            if (cmd.hasOption("prep_nn_input")) {

                this.prepare_nn_input = Integer.parseInt(cmd.getOptionValue("prep_nn_input"));

            }

            if( cmd.hasOption("overWrite") ){
                this.overWrite = true;
            }

            if (cmd.hasOption("amapVoxFile")) {

                this.amapVoxFile = new File(cmd.getOptionValue("amapVoxFile"));

            }


            if (cmd.hasOption("few")) {

                this.few = Integer.parseInt(cmd.getOptionValue("few"));

            }

            if (cmd.hasOption("uef_echoClass")) {

                this.echoClass = true;

            }

            if (cmd.hasOption("o_itc")) {

                this.output_only_itc_segments = true;

            }

            if (cmd.hasOption("input_in_radians")) {

                this.input_in_radians = true;

            }

            if (cmd.hasOption("tree_tops")) {

                this.treeTops = new File(cmd.getOptionValue("tree_tops"));

            }


            if (cmd.hasOption("export_mask")) {

                this.outputMask = true;

            }

            if (cmd.hasOption("export_color")) {

                this.rasterizeColor = true;

            }

            if (cmd.hasOption("export_intensity")) {

                this.rasterizeIntensity = true;

            }

            if (cmd.hasOption("extra_byte")) {

                //this.create_extra_byte_vlr = Integer.parseInt(cmd.getOptionValue("extra_byte"));

            }

            if( cmd.hasOption("config")) {
                this.configFile = new File(cmd.getOptionValue("config"));

                if(!this.configFile.exists())
                    throw new argumentException("-config does not exist!");
            }

            if( cmd.hasOption("config2")) {
                this.configFile2 = new File(cmd.getOptionValue("config2"));

                if(!this.configFile.exists())
                    throw new argumentException("-config does not exist!");
            }

            if(cmd.hasOption("exclude")){
                this.exclude = cmd.getOptionValue("exclude");
            }

            if(cmd.hasOption("include")){
                this.include = cmd.getOptionValue("include");
            }

            if(cmd.hasOption("stringArg1")){
                this.stringArgument1 = cmd.getOptionValue("stringArg1");
            }


            if (cmd.hasOption("MML_klj")) {

                this.MML_klj = true;

            }

            if (cmd.hasOption("sa")) {

                this.output_only_stemAlignInput = true;

            }

            if (cmd.hasOption("save_to_pointSourceId")) {

                this.save_to_p_id = true;

            }

            if (cmd.hasOption("convolution_metrics")) {

                this.noConvolution = false;

            }

            if (cmd.hasOption("poly")) {

                this.poly = cmd.getOptionValue("poly");

                if(!new File(this.poly).exists())
                    throw new argumentException("-poly does not exist!");


            }

            if (cmd.hasOption("aux_file")) {

                this.aux_file = new File(cmd.getOptionValue("aux_file"));

                if(!this.aux_file.exists())
                    throw new argumentException("-aux_file does not exist!");

            }

            if (cmd.hasOption("simpleEstimation")) {

                this.noEstimation = false;
                this.simpleEstimation = true;
            }

            if (cmd.hasOption("simpleEstimationWithProb")) {

                this.noEstimation = false;
                this.simpleEstimationWithProb = true;
            }

            if (cmd.hasOption("estimationSpecialThinning")){
                this.noEstimation = false;
                this.estimationSpecialThinning = true;
            }

            if( cmd.hasOption("mapSheetExtent")){
                this.mapSheetExtent = true;
            }

            if (cmd.hasOption("estimationWithCHM")) {

                this.noEstimation = false;
                this.estimationWithCHM = true;
            }

            if (cmd.hasOption(("premoto_adaptiveDistance"))){
                this.PREMOTO_ADAPTIVEDISTANCE = true;
            }


            if (cmd.hasOption("convolution_option")) {

                this.convolution_option = Integer.parseInt(cmd.getOptionValue("convolution_option"));

            }

            if (cmd.hasOption("image_height")) {

                this.image_height = Integer.parseInt(cmd.getOptionValue("image_height"));

            }

            if (cmd.hasOption("num_iter")) {

                this.num_iter = Integer.parseInt(cmd.getOptionValue("num_iter"));

            }

            if(cmd.hasOption("name")){
                this.extraByteName = cmd.getOptionValue("name");
            }

            if (cmd.hasOption("measured_trees")) {

                this.measured_trees = new File(cmd.getOptionValue("measured_trees"));

                if(!this.measured_trees.exists())
                    throw new argumentException("-measured_trees does not exist!");

            }

            if (cmd.hasOption("target")){
                this.target = new File(cmd.getOptionValue("target"));

                if(!this.target.exists())
                    throw new argumentException("-target does not exist!");
            }

            if (cmd.hasOption("measured_trees_2")) {

                this.measured_trees_2 = new File(cmd.getOptionValue("measured_trees_2"));

                if(!this.measured_trees_2.exists())
                    throw new argumentException("-measured_trees does not exist!");

            }
            if (cmd.hasOption("only_convo")) {

                this.onlyConvolutionMetrics = true;

            }


            if( cmd.hasOption("itc_metrics")){
                this.ITC_metrics_file = new File(cmd.getOptionValue("itc_metrics"));

            }
            if (cmd.hasOption("dist")) {

                this.dist = Double.parseDouble(cmd.getOptionValue("dist"));

            }

            if (cmd.hasOption("pp_x_offset")){
                this.pp_x_offset = Double.parseDouble(cmd.getOptionValue("pp_x_offset"));
            }

            if (cmd.hasOption("pp_y_offset")){
                this.pp_y_offset = Double.parseDouble(cmd.getOptionValue("pp_y_offset"));
            }

            if (cmd.hasOption("prob")) {

                this.prob = Double.parseDouble(cmd.getOptionValue("prob"));

            }

            if(cmd.hasOption("identifier")){
                this.identifier = cmd.getOptionValue("identifier");
            }

            if (cmd.hasOption("filter_intensity")) {

                this.filter_intensity = Double.parseDouble(cmd.getOptionValue("filter_intensity"));

            }

            if (cmd.hasOption("delta")) {

                this.delta = Double.parseDouble(cmd.getOptionValue("delta"));

            }

            if (cmd.hasOption("gdal_cache_gb")) {

                this.gdal_cache_gb = Double.parseDouble(cmd.getOptionValue("gdal_cache_gb"));

            }

            if (cmd.hasOption("set_classification")) {
                this.noModify = false;
                this.inclusionRule.setClassification(Integer.parseInt(cmd.getOptionValue("set_classification")));

            }
            if (cmd.hasOption("field")) {

                this.field = Integer.parseInt(cmd.getOptionValue("field"));

            }

            if(cmd.hasOption("skip_global")){
                this.skip_global = true;
            }

            if(cmd.hasOption("metadata")){
                File metadata = new File(cmd.getOptionValue("metadata"));

                if(!metadata.exists())
                    throw new argumentException("-metadata does not exist!");

                this.metadatafile = cmd.getOptionValue("metadata");
            }


            if(cmd.hasOption("use_p_source")){
                this.use_p_source = true;
            }

            if (cmd.hasOption("drop_noise")) {
                this.noModify = false;
                this.inclusionRule.dropNoise();

            }

            if (cmd.hasOption("thin3d")) {

                this.thin3d = true;

            }

            if (cmd.hasOption("pit_free")) {

                this.pitFree = true;

            }

            if(cmd.hasOption("externalData")){
                this.noLasUtilsInput = true;
            }

            if (cmd.hasOption("ray_trace")) {

                this.ray_trace = true;

            }
            if (cmd.hasOption("classify")) {

                this.classify = true;

            }

            if (cmd.hasOption("photogrammetry")) {

                this.photogrammetry = true;

            }

            if (cmd.hasOption("olas")) {

                this.olas = true;

            }
            if (cmd.hasOption("axelsson_mirror")) {

                this.axelsson_mirror = true;

            }
            if (cmd.hasOption("highest")) {

                this.highest = true;
                this.lowest = false;
            }

            if (cmd.hasOption("lowest")) {

                this.lowest = true;
                this.highest = false;
            }

            if(this.highest && this.lowest){
                throw new argumentException("Can't have it both -highest and -lowest");
            }

            if (cmd.hasOption("splitBy")) {

                this.splitBy = cmd.getOptionValue("splitBy");

            }

            if( cmd.hasOption("subsetColumns"))
                this.subsetColumnNamesVMI = true;

            if (cmd.hasOption("set_point_source_id")){
                this.noModify = false;
                this.inclusionRule.setPointSourceId(Integer.parseInt(cmd.getOptionValue("set_point_source_id")));

            }

            if (cmd.hasOption("train")) {

                this.train = new File(cmd.getOptionValue("train"));

                if(!this.train.exists())
                    throw new argumentException("-train does not exist!");

            }

            if (cmd.hasOption("test")) {

                this.test = new File(cmd.getOptionValue("test"));

                if(!this.test.exists())
                    throw new argumentException("-test does not exist!");

            }

            if (cmd.hasOption("validation")) {

                this.validation = new File(cmd.getOptionValue("validation"));

                if(!this.validation.exists())
                    throw new argumentException("-validation does not exist!");

            }

            if (cmd.hasOption("train2")) {

                this.train_2 = new File(cmd.getOptionValue("train2"));

                if(!this.train_2.exists())
                    throw new argumentException("-train2 does not exist!");

            }

            if (cmd.hasOption("test2")) {

                this.test_2 = new File(cmd.getOptionValue("test2"));

                if(!this.test_2.exists())
                    throw new argumentException("-test2 does not exist!");

            }

            if (cmd.hasOption("read_rules")){
                this.rule_when_reading = true;
                this.inclusionRule.applyWhenReading();

            }

            if (cmd.hasOption("mode_3d")){

                this.mode_3d = true;

            }



            if (cmd.hasOption("write_rules")){

                this.rule_when_writing = true;
                this.inclusionRule.applyWhenWriting();

            }


            if (cmd.hasOption("validation2")) {

                this.validation_2 = new File(cmd.getOptionValue("validation2"));

                if(!this.validation_2.exists())
                    throw new argumentException("-validation2 does not exist!");

            }

            if (cmd.hasOption("time")) {

                this.time = Integer.parseInt(cmd.getOptionValue("time"));

            }

            if (cmd.hasOption("dz_")) {

                this.dz_on_the_fly = true;

            }

            if (cmd.hasOption("interior")) {

                this.interior = cmd.getOptionValue("interior");

                if(!new File(this.interior).exists())
                    throw new argumentException("-interior does not exist!");

            }

            if (cmd.hasOption("save_file")) {

                this.save_file = cmd.getOptionValue("save_file");

            }

            if (cmd.hasOption("harmonized")) {

                this.harmonized = true;

            }

            if (cmd.hasOption("axGrid")) {

                this.axgrid = Double.parseDouble(cmd.getOptionValue("axGrid"));
            }

            if (cmd.hasOption("kernel")) {

                this.kernel = Integer.parseInt(cmd.getOptionValue("kernel"));

            }

            if (cmd.hasOption("thin_to_center")) {

                this.thinToCenter = true;

            }



            if (cmd.hasOption("set_user_data")) {
                this.noModify = false;
                this.inclusionRule.setUserData(Integer.parseInt(cmd.getOptionValue("set_user_data")));

                if(Integer.parseInt(cmd.getOptionValue("set_user_data")) > 255){
                    throw new lasFormatException("-set_user_data exceeds unsigned byte range 0 - 255");
                }
            }

            if (cmd.hasOption("drop_z_below")){
                this.noModify = false;
                this.inclusionRule.dropZBelow(Double.parseDouble(cmd.getOptionValue("drop_z_below")));

            }
            if (cmd.hasOption("drop_z_above")){
                this.noModify = false;
                this.inclusionRule.dropZAbove(Double.parseDouble(cmd.getOptionValue("drop_z_above")));

            }

            if (cmd.hasOption("drop_scan_angle_below")){
                this.noModify = false;
                this.inclusionRule.drop_scan_angle_below(Integer.parseInt(cmd.getOptionValue("drop_scan_angle_below")));

            }
            if (cmd.hasOption("drop_scan_angle_above")){
                this.noModify = false;
                this.inclusionRule.drop_scan_angle_above(Integer.parseInt(cmd.getOptionValue("drop_scan_angle_above")));

            }

            if (cmd.hasOption("theta")) {

                this.theta = Double.parseDouble(cmd.getOptionValue("theta"));

            }

            if (cmd.hasOption("min_edge_length")) {

                this.min_edge_length = Double.parseDouble(cmd.getOptionValue("min_edge_length"));

            }
            if (cmd.hasOption("buffer")) {

                this.buffer = Double.parseDouble(cmd.getOptionValue("buffer"));

            }

            if (cmd.hasOption("orig_x")) {

                this.orig_x = Double.parseDouble(cmd.getOptionValue("orig_x"));

            }

            if (cmd.hasOption("orig_y")) {

                this.orig_y = Double.parseDouble(cmd.getOptionValue("orig_y"));

            }

            if (cmd.hasOption("edges")) {

                this.edges = Double.parseDouble(cmd.getOptionValue("edges"));

            }

            if (cmd.hasOption("model")) {

                this.model = new File(cmd.getOptionValue("model"));

            }


            if (cmd.hasOption("exterior")) {

                this.exterior = cmd.getOptionValue("exterior");

                if(!new File(this.exterior).exists())
                    throw new argumentException("-exterior does not exist!");

            }

            if (cmd.hasOption("otype")) {

                this.otype = cmd.getOptionValue("otype");

                if(!this.otype.equals("las") && !this.otype.equals("txt")){
                    throw new argumentException("-otype " + this.otype + " not recognized!");
                }
            }

            if (cmd.hasOption("o_dz")) {

                this.o_dz = true;

            }

            if (cmd.hasOption("skeleton")) {

                this.skeleton_output = true;

            }

            if (cmd.hasOption("sep")) {

                this.sep = cmd.getOptionValue("sep");

                if(sep.equals("\\s"))
                    this.sep = " ";
                if(sep.equals("\\t"))
                    this.sep = "\t";
                if(sep.equals(";"))
                    this.sep = ";";
                if(sep.equals(","))
                    this.sep = ",";

                //System.out.println("sep: " + this.sep);

            }

            if (cmd.hasOption("neural_mode")) {


                this.neural_mode = cmd.getOptionValue("neural_mode");

            }

            if (cmd.hasOption("method")) {

                this.method = cmd.getOptionValue("method");

            }

            if (cmd.hasOption("keep_first")) {
                this.noModify = false;
                this.inclusionRule.keepFirst();

            }

            if (cmd.hasOption("first_only")) {
                this.noModify = false;
                this.inclusionRule.firstOnly();
            }

            if (cmd.hasOption("keep_last")) {
                this.noModify = false;
                this.inclusionRule.keepLast();

            }

            if (cmd.hasOption("keep_only")) {
                this.noModify = false;
                this.inclusionRule.keepOnly();

            }

            if (cmd.hasOption("drop_only")) {
                this.noModify = false;
                this.inclusionRule.dropOnly();

            }

            if (cmd.hasOption("keep_intermediate")) {
                this.noModify = false;
                this.inclusionRule.keepIntermediate();

            }

            if (cmd.hasOption("drop_intermediate")) {
                this.noModify = false;
                this.inclusionRule.dropIntermediate();

            }

            if (cmd.hasOption("drop_synthetic")) {
                this.noModify = false;
                this.inclusionRule.dropSynthetic();

            }
            if (cmd.hasOption("nBands")) {

                this.nBands = Integer.parseInt(cmd.getOptionValue("nBands"));

            }

            if (cmd.hasOption("traj")) {

                this.trajectory = cmd.getOptionValue("traj");

                if(!new File(this.trajectory).exists())
                    throw new argumentException("-traj does not exist!");

            }


            if (cmd.hasOption("idir")) {

                this.idir = cmd.getOptionValue("idir");
                File odr = new File(idir);

                if (!odr.isDirectory()) {

                    throw new argumentException("-idir not a directory!");

                }

                this.idir = odr.getCanonicalPath() + System.getProperty("file.separator");

            }

                if (cmd.hasOption("odir")) {

                this.odir = cmd.getOptionValue("odir");
                File odr = new File(odir);

                if(!odr.exists()){

                    odr.mkdirs();
                }
                if(!odr.isDirectory()){

                    throw new argumentException("-odir is not a directory!");

                }

                this.odir = odr.getCanonicalPath() + System.getProperty("file.separator");


                String odirFirst = "asd";

            }

            if (cmd.hasOption("split")) {

                this.split = true;

            }

            if (cmd.hasOption("debug")) {

                this.debug = true;

            }

            if (cmd.hasOption("res")) {

                this.res = Double.parseDouble(cmd.getOptionValue("res"));

            }

            if (cmd.hasOption("min_points")) {

                this.min_points = Integer.parseInt(cmd.getOptionValue("min_points"));

            }

            if (cmd.hasOption("batch_size")) {

                this.batch_size = Integer.parseInt(cmd.getOptionValue("batch_size"));

            }

            if (cmd.hasOption("remove_buffer")) {

                this.remove_buffer = true;
                this.inclusionRule.removeBuffer();

            }

            if (cmd.hasOption("angle")) {

                this.angle = Double.parseDouble(cmd.getOptionValue("angle"));

            }

            if (cmd.hasOption("translate_x")) {
                this.noModify = false;
                this.inclusionRule.translate_x(Double.parseDouble(cmd.getOptionValue("translate_x")));

            }
            if (cmd.hasOption("translate_y")) {
                this.noModify = false;
                this.inclusionRule.translate_y(Double.parseDouble(cmd.getOptionValue("translate_y")));

            }

            if (cmd.hasOption("translate_z")) {
                this.noModify = false;
                this.inclusionRule.translate_z(Double.parseDouble(cmd.getOptionValue("translate_z")));

            }

            if (cmd.hasOption("translate_i")) {
                this.noModify = false;
                this.inclusionRule.translate_i(Integer.parseInt(cmd.getOptionValue("translate_i")));

            }
            if (cmd.hasOption("adjustKappa")) {
                this.adjustKappa = Double.parseDouble(cmd.getOptionValue("adjustKappa"));

            }


            if (cmd.hasOption("scale_i")) {
                this.noModify = false;
                this.inclusionRule.scale_i(Double.parseDouble(cmd.getOptionValue("scale_i")));

            }




            if (cmd.hasOption("ground_points")) {

                this.groundPoints = cmd.getOptionValue("ground_points");

            }

            if (cmd.hasOption("o_stat")) {

                this.output_statistics = true;

            }

            if (cmd.hasOption("interpolate")) {

                this.interpolate = true;

            }

            if (cmd.hasOption("mem_eff")) {

                this.mem_efficient = true;

            }

            if (cmd.hasOption("densities")) {

                //System.out.println(cmd.getOptionValue("densities"));

                this.densities = Stream.of(cmd.getOptionValue("densities").split(","))
                        .mapToDouble (Double::parseDouble)
                        .toArray();

            }

            if (cmd.hasOption("percentiles")) {

                this.percentiles = Double.parseDouble(cmd.getOptionValue("percentiles"));

            }

            if (cmd.hasOption("interval")) {

                this.interval = Double.parseDouble(cmd.getOptionValue("interval"));

            }

            if (cmd.hasOption("dense")) {

                this.dense = true;

            }

            if (cmd.hasOption("z_cutoff")) {

                this.z_cutoff = Double.parseDouble(cmd.getOptionValue("z_cutoff"));

            }

            if (cmd.hasOption("lambda")) {

                this.lambda = Double.parseDouble(cmd.getOptionValue("lambda"));

            }

            if (cmd.hasOption("concavity")) {

                this.concavity = Double.parseDouble(cmd.getOptionValue("concavity"));

            }

            if (cmd.hasOption("concave")) {

                this.concave = true;

            }

            if (cmd.hasOption("keep_classification")) {
                this.noModify = false;
                this.keep_classification = Integer.parseInt(cmd.getOptionValue("keep_classification"));
                this.inclusionRule.keepClassification(this.keep_classification);
            }

            if (cmd.hasOption("omet")) {

                this.omet = true;

            }

            if (cmd.hasOption("by_z_order")) {

                this.by_gps_time = false;
                this.by_z_order = true;

            }

            if (cmd.hasOption("by_gps_time")) {

                this.by_z_order = false;
                this.by_gps_time = true;

            }
        }catch (Exception pe) {
            System.out.println("Error parsing command-line arguments!");
            System.out.println("Please, follow the instructions below:");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "All arguments:", options );

            pe.printStackTrace();

            //helpPrinter.printHelp(1);

            System.exit(1);
        }

        p_update = new progressUpdater(this);

        if(true)
            return;

        this.iparse = args[1];

        this.oparse = args[2];

        this.output = args[3];

        this.input = args[4];

        files = input.split(";");
        //System.out.println(Arrays.toString(files));

        ArrayList<String> temp = new ArrayList<>();

        for(String s : files){
            if(new LASraf(new File(s)).check()){
                temp.add(s);
            }
        }

        files = temp.toArray(new String[0]);


        if(Integer.parseInt(args[5]) != 0)
            this.inclusionRule.dropNoise();

        if(Integer.parseInt(args[6]) != -999)
            this.inclusionRule.dropClassification(Integer.parseInt(args[6]));

        if(Integer.parseInt(args[7]) != 0)
            this.inclusionRule.firstOnly();
        if(Integer.parseInt(args[8]) != 0)
            this.inclusionRule.keepFirst();
        if(Integer.parseInt(args[9]) != 0)
            this.inclusionRule.dropFirst();

        if(Integer.parseInt(args[10]) != 0)
            this.inclusionRule.lastOnly();
        if(Integer.parseInt(args[11]) != 0)
            this.inclusionRule.keepLast();
        if(Integer.parseInt(args[12]) != 0)
            this.inclusionRule.dropLast();


        if(Integer.parseInt(args[13]) != 0)
            this.inclusionRule.dropFirstOfMany();
        if(Integer.parseInt(args[14]) != 0)
            this.inclusionRule.dropLastOfMany();


        if(Integer.parseInt(args[15]) != 0)
            this.inclusionRule.keepMiddle();
        if(Integer.parseInt(args[16]) != 0)
            this.inclusionRule.dropMiddle();

        if(Integer.parseInt(args[17]) != 0)
            this.inclusionRule.keepSingle();
        if(Integer.parseInt(args[18]) != 0)
            this.inclusionRule.dropSingle();

        if(Integer.parseInt(args[19]) != 0)
            this.inclusionRule.keepDouble();
        if(Integer.parseInt(args[20]) != 0)
            this.inclusionRule.dropDouble();

        if(Integer.parseInt(args[21]) != 0)
            this.inclusionRule.keepTriple();
        if(Integer.parseInt(args[22]) != 0)
            this.inclusionRule.dropTriple();

        if(Integer.parseInt(args[23]) != 0)
            this.inclusionRule.keepQuadruple();
        if(Integer.parseInt(args[24]) != 0)
            this.inclusionRule.dropQuadruple();

        if(Integer.parseInt(args[25]) != 0)
            this.inclusionRule.keepQuintuple();
        if(Integer.parseInt(args[26]) != 0)
            this.inclusionRule.dropQuintuple();

        if(Integer.parseInt(args[27]) != 0)
            this.inclusionRule.dropSynthetic();

        if(Integer.parseInt(args[28]) != -999)
            this.inclusionRule.dropUserData(Integer.parseInt(args[28]));
        if(Integer.parseInt(args[29]) != -999)
            this.inclusionRule.keepUserData(Integer.parseInt(args[29]));

        if(Integer.parseInt(args[30]) != -999)
            this.inclusionRule.setClassification(Integer.parseInt(args[30]));

        if(Integer.parseInt(args[31]) != -999)
            this.inclusionRule.setUserData(Integer.parseInt(args[31]));

        if(Integer.parseInt(args[32]) != -999)
            this.inclusionRule.dropZBelow(Integer.parseInt(args[32]));
        if(Integer.parseInt(args[33]) != -999)
            this.inclusionRule.dropZAbove(Integer.parseInt(args[33]));


        this.buffer = Double.parseDouble(args[34]);

        this.odir = args[35];

        if(odir.charAt(0) == '/' || odir.charAt(0) == '\\'){
            odir = odir.substring(1);
        }

        String odirFirst = "asd";

        if(odir.split(System.getProperty("file.separator")).length > 1)
            odirFirst = odir.split(System.getProperty("file.separator"))[0];

        execDir = System.getProperty("user.dir");

        File curDir = new File(execDir);

        if(!odirFirst.equals("asd"))
        for(File f : curDir.listFiles()){

            if(f.getName().equals(odirFirst )) {

                odir = curDir.getAbsolutePath() + System.getProperty("file.separator") + odir + System.getProperty("file.separator");
            }
        }

        this.few = Integer.parseInt(args[36]);

        this.step = Double.parseDouble(args[37]);

        this.ground_class = Integer.parseInt(args[38]);

        this.method = args[39];

        this.sep = args[40];

        this.cores = Integer.parseInt(args[41]);

        this.otype = args[42];

        //this.numarg1 = Double.parseDouble(args[43]);
        this.split = Integer.parseInt(args[43]) == 1;
        this.angle = Double.parseDouble(args[44]);
        this.axgrid = Double.parseDouble(args[45]);
        this.groundPoints = args[46]; // NUMARG4

        this.br = Integer.parseInt(args[47]);

        this.poly = args[48]; //MOVE TO LAST

        if(Double.parseDouble(args[49]) != -999)
            this.inclusionRule.translate_x(Double.parseDouble(args[49]));
        if(Double.parseDouble(args[50]) != -999)
            this.inclusionRule.translate_y(Double.parseDouble(args[50]));
        if(Double.parseDouble(args[51]) != -999)
            this.inclusionRule.translate_z(Double.parseDouble(args[51]));

        this.skip_global = Integer.parseInt(args[52]) == 1;

        this.trajectory = args[53];

        this.translate_i = Double.parseDouble(args[54]);

        if(Integer.parseInt(args[55]) != 0){
            this.thin3d = true;
        }

        this.splitBy = args[56];

        if(Integer.parseInt(args[57]) != 0) {
            this.remove_buffer = true;
            this.inclusionRule.removeBuffer();
        }

        this.dist = Double.parseDouble(args[58]);

        this.theta = Double.parseDouble(args[59]);

        this.kernel = Integer.parseInt(args[60]);

        this.lambda = Double.parseDouble(args[61]);
        this.delta = Double.parseDouble(args[62]);

        this.concavity = Double.parseDouble(args[63]);

        this.interval = Double.parseDouble(args[64]);

        this.echoClass = Integer.parseInt(args[65]) == 1;

        this.keep_classification = Integer.parseInt(args[66]);


        this.inclusionRule.keepClassification(this.keep_classification);

        this.change_point_type = Integer.parseInt(args[67]);



        System.gc();

        //System.out.println("skip global:  " + skip_global);

    }

    public void createOutputFiles(){

        for(int i = 0; i < pointClouds.size(); i++){

            File tempFile;
            String tempPath = this.output;

            if(this.output.equals("asd"))
                tempPath = pointClouds.get(i).getFile().getName();

            if(!odir.equals("asd"))
                tempPath = odir + pathSep + tempPath;

            //System.out.println(tempPath);
            tempFile = new File(tempPath);

            if(tempFile.exists())
                tempFile.delete();

            try {
                tempFile.createNewFile();
            }catch (Exception e) {
                e.printStackTrace();
            }
            this.outputFiles.add(tempFile);
        }
    }

    public void setPolyBank(ArrayList<double[][]> polyBank){

        this.polyBank = (ArrayList<double[][]>)polyBank.clone();

    }

    public File createOutputFile(File in) throws IOException {

        File tempFile = null;
        String tempPath = this.output;

        if(this.output.equals("asd"))
            tempFile = in;
        else
            tempFile = new File(this.output);

        if(!odir.equals("asd")) {

            File diri = new File(odir);

            tempFile = fo.transferDirectories(tempFile, diri.getAbsolutePath());
        }

        String extensionHere = tempFile.getName().substring(tempFile.getName().lastIndexOf("."));

        if(tempFile.exists()){
            tempFile = fo.createNewFileWithNewExtension(tempFile, "_1" + extensionHere);
        }

        if(tempFile.exists())
            tempFile.delete();


        tempFile.createNewFile();

        return tempFile;

    }

    public File createOutputFile(File in, String extension) throws IOException {

        File tempFile = new File(in.getAbsolutePath().substring(0, in.getAbsolutePath().lastIndexOf(".")) + extension);

        if(!odir.equals("asd")) {

            File diri = new File(odir);

            tempFile = fo.transferDirectories(tempFile, diri.getAbsolutePath());
        }

        String extensionHere = tempFile.getName().substring(tempFile.getName().lastIndexOf("."));

        if(tempFile.exists()){
            tempFile = fo.createNewFileWithNewExtension(tempFile, "_1" + extensionHere);
        }

        if(tempFile.exists())
            tempFile.delete();


        tempFile.createNewFile();

        return tempFile;

    }

    public File _createOutputFile_(String fileName){

        File outputFile = null;

        if(this.inputFiles.size() == 0 && this.odir.equals("asd")){
            throw new IllegalArgumentException("No input files");
        }

        File tempFile = null;

        if(this.odir.equals("asd")) {
            outputFile = new File(inputFiles.get(0).getParent() + pathSep + fileName);
        }else{
            outputFile = new File(this.odir + pathSep + fileName);
        }

        return outputFile;
    }

    public File createOutputFileReplace(File in, String fullName) throws IOException {

        File tempFile = new File(in.getParent() + pathSep + fullName);

        if(!odir.equals("asd")) {

            File diri = new File(odir);

            tempFile = fo.transferDirectories(tempFile, diri.getAbsolutePath());
        }

        String extensionHere = tempFile.getName().substring(tempFile.getName().lastIndexOf("."));

        if(tempFile.exists()){
            tempFile = fo.createNewFileWithNewExtension(tempFile, "_1" + extensionHere);
        }

        if(tempFile.exists())
            tempFile.delete();


        tempFile.createNewFile();

        return tempFile;

    }

    public File createOutputFile(LASReader in) throws IOException {


        File tempFile = null;
        String tempPath = this.output;

        if(this.output.equals("asd"))
            tempFile = in.getFile();
        else
            tempFile = new File(this.output);

        if(!odir.equals("asd")) {

            File diri = new File(odir);

            tempFile = fo.transferDirectories(tempFile, diri.getAbsolutePath());

        }

        String extensionHere = tempFile.getName().substring(tempFile.getName().lastIndexOf("."));

        if(tempFile.exists()){
            if(!this.overWrite)
                tempFile = fo.createNewFileWithNewExtension(tempFile, "_1" + extensionHere);

        }

        if(tempFile.getAbsolutePath().equals(in.getFile().getAbsolutePath()))
            throw new toolException("Input and output files are the same.  Please change the output file name.");

        if(tempFile.exists())
            tempFile.delete();


        tempFile.createNewFile();

        return tempFile;

    }

    public File createOutputFile_extension(LASReader in, String extension) throws IOException {

        File tempFile = null;
        String tempPath = this.output;

        if(this.output.equals("asd"))
            tempFile =  fo.createNewFileWithNewExtension(in.getFile(), extension);
        else
            tempFile = new File(this.output);

        if(!odir.equals("asd")) {

            File diri = new File(odir);

            tempFile = fo.transferDirectories(tempFile, diri.getAbsolutePath());
        }


        if(tempFile.exists()){
            tempFile = fo.createNewFileWithNewExtension(tempFile, "_1" + extension);
        }

        if(tempFile.exists())
            tempFile.delete();


        tempFile.createNewFile();

        return tempFile;

    }

    public File createOutputFile_txt(LASReader in) throws IOException {

        File tempFile = null;
        String tempPath = this.output;

        if(this.output.equals("asd"))
            tempFile = in.getFile();
        else
            tempFile = new File(this.output);

        if(!odir.equals("asd")) {

            File diri = new File(odir);

            tempFile = fo.transferDirectories(tempFile, diri.getAbsolutePath());
        }

        String extensionHere = ".txt";
        if(tempFile.exists()){
            tempFile = fo.createNewFileWithNewExtension(tempFile, "_1" + extensionHere);
        }

        if(tempFile.exists())
            tempFile.delete();


        tempFile.createNewFile();

        return tempFile;

    }

    public File createOutputFileWithExtension(String in, String extension) throws IOException {


        return createOutputFileWithExtension(new File(in), extension);

    }

    public File createOutputFileWithExtension(LASReader in, String extension) throws IOException {


        return createOutputFileWithExtension(in.getFile(), extension);

    }

    public synchronized File createOutputFileWithExtension(File in, String extension) throws IOException {

        File tempFile = null;
        String tempPath = this.output;

        if(this.output.equals("asd"))
            tempFile = in;
        else
            tempFile = new File(this.output);

        if(!odir.equals("asd")) {

            File diri = new File(odir);
            tempFile = fo.transferDirectories(tempFile, diri.getAbsolutePath());

        }

        //if(tempFile.exists()){

        tempFile = fo.createNewFileWithNewExtension(tempFile, extension);

        //}

        if(tempFile.getAbsolutePath().compareTo(in.getAbsolutePath()) == 0)
            throw new toolException("Attempting to delete the original file. NO NO NO NO! ");


        if(tempFile.exists())
            tempFile.delete();


        tempFile.createNewFile();

        return tempFile;
    }

    public synchronized File createFile(String extension) throws IOException {

        File tempFile = new File(extension);
        String tempPath = this.output;


        if(!odir.equals("asd")) {

            File diri = new File(odir);
            tempFile = fo.transferDirectories(tempFile, diri.getAbsolutePath());

        }else{


                tempFile = fo.transferDirectories(tempFile, this.inputFiles.get(0).getAbsoluteFile().getParent());
        }

        if(tempFile.exists())
            tempFile.delete();


        tempFile.createNewFile();

        return tempFile;
    }
    public synchronized File createOutputFileWithExtension(File in, String extension, String odir_in) throws IOException {

        File tempFile = null;
        String tempPath = this.output;

        if(this.output.equals("asd"))
            tempFile = in;
        else
            tempFile = new File(this.output);

        if(!odir_in.equals("asd")) {

            File diri = new File(odir_in);
            tempFile = fo.transferDirectories(tempFile, diri.getAbsolutePath());

        }


        //if(tempFile.exists()){

        tempFile = fo.createNewFileWithNewExtension(tempFile, extension);

        //}

        if(tempFile.getAbsolutePath().compareTo(in.getAbsolutePath()) == 0)
            throw new toolException("Attempting to delete the original file. NO NO NO NO! ");


        if(tempFile.exists())
            tempFile.delete();


        tempFile.createNewFile();



        return tempFile;
    }
    public File createOutputFileWithExtension2(File in, String extension) throws IOException {


        File tempFile = null;
        String tempPath = this.output;

        if(this.output.equals("asd"))
            tempFile = in;
        else
            tempFile = new File(this.output);

        System.out.println(tempFile.getAbsolutePath() + " " + this.output.equals("asd") + " " + this.output);
        if(!odir.equals("asd")) {

            File diri = new File(odir);

            tempFile = fo.transferDirectories(tempFile, diri.getAbsolutePath());
        }

        if(tempFile.exists()){

            tempFile = fo.createNewFileWithNewExtension(tempFile, "_1" + extension);
        }

        if(tempFile.exists())
            tempFile.delete();

        tempFile.createNewFile();

        return tempFile;

    }




    public static void compressFileToGzip(String filePath) {
        try (
                FileInputStream fis = new FileInputStream(filePath);
                GZIPOutputStream gzipOS = new GZIPOutputStream(new FileOutputStream(filePath + ".gz"))
        ) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                gzipOS.write(buffer, 0, bytesRead);
            }
            System.out.println("File compressed successfully.");

            // Delete the original file after successful compression
            File originalFile = new File(filePath);
            if (originalFile.delete()) {
                System.out.println("Original file deleted successfully.");
            } else {
                System.out.println("Failed to delete the original file.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void compressFileToGzip(File file) {
        try (
                FileInputStream fis = new FileInputStream(file);
                GZIPOutputStream gzipOS = new GZIPOutputStream(new FileOutputStream(file.getAbsolutePath() + ".gz"))
        ) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                gzipOS.write(buffer, 0, bytesRead);
            }
            System.out.println("File compressed successfully.");

            // Delete the original file after successful compression
            if (file.delete()) {
                System.out.println("Original file deleted successfully.");
            } else {
                System.out.println("Failed to delete the original file.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    public void convertLasToLaz() throws IOException{

        this.lazInput = true;

        if(this.odir.equals("asd")) {
            File tmpDirectory = new File("tmpLAS");
            this.tmpDirectory = tmpDirectory.getAbsolutePath();
            tmpDirectory.mkdirs();
            this.odir = new File(tmpDirectory.getAbsolutePath()).getParent();
        }
        else{
            File tmpDirectory = new File(this.odir + this.pathSep + "tmpLAS");
            this.tmpDirectory = tmpDirectory.getAbsolutePath();
            tmpDirectory.mkdirs();
        }


        Path path = Paths.get(this.getPath());
        String libs = path.getParent().getParent().getParent().toString() + this.pathSep + "lib" + this.pathSep + "laszip4j-0.15.jar";
        //System.out.println(libs);

        ForkJoinPool customThreadPool = new ForkJoinPool(4);

        try {

            customThreadPool.submit(() ->

                    IntStream.range(0, this.files.length).parallel().forEach(i -> {
        //for(int i = 0; i < this.files.length; i++){

            String jarFilePath = libs;
            String inputLazFile = files[i];

            File tmpFile = new File(inputLazFile);
            String outputLasFile = tmpFile.getName().replace(".laz", ".las");
            outputLasFile = "tmpLAS" + this.pathSep + outputLasFile;

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "java", "-jar", jarFilePath, "-i", inputLazFile, "-o", outputLasFile
            );

            //ProcessBuilder processBuilder = new ProcessBuilder("ls");

            // Optionally, set the working directory
            // processBuilder.directory(new File("/path/to/working/directory"));

            // Start the process
            Process process = null;
            try {
               process = processBuilder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Read the output (if any)
            java.io.InputStream inputStream = process.getInputStream();
            java.util.Scanner scanner = new java.util.Scanner(inputStream).useDelimiter("\\A");
            String output = scanner.hasNext() ? scanner.next() : "";
            // Wait for the process to complete
            int exitCode = -1;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

                        this.files[i] = outputLasFile;

            try {
                LASReader tmpLas = new LASReader(new File(this.files[i]));
                tmpLas.index(10);
            }catch (Exception e){
                System.out.println("Error while reading file: " + this.files[i]);
                System.exit(1);
            }
        //}
        })).get();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

    public void cleanup(){

            if(this.lazInput){

                for(int i = 0; i < this.files.length; i++){
                    File tmpFile = new File(this.files[i]);
                    boolean do_not_delete = false;
                    for(int i_ = 0; i_ < this.ORIGINAL_FILES.size(); i_++){
                        if(this.ORIGINAL_FILES.get(i_).equals(tmpFile.getAbsolutePath()))
                            do_not_delete = true;

                    }
                    if(!do_not_delete)
                        tmpFile.delete();
                }
            }

    }


    public String getPath(){
            Class<?> currentClass = argumentReader.class;
            String className = currentClass.getSimpleName() + ".class";
            URL classUrl = currentClass.getResource(className);

            if (classUrl != null) {
                String classPath = classUrl.toString();

                if (classPath.startsWith("jar:file:") || classPath.startsWith("file:")) {
                    try {
                        String classFilePath = classPath.substring(classPath.indexOf(':') + 1);

                        if (classFilePath.startsWith("file:")) {
                            classFilePath = classFilePath.substring(5); // Remove "file:"
                        }

                        // Remove the class file path from the end to get the directory
                        String classDirectory = classFilePath.substring(0, classFilePath.length() - className.length());

                        // Replace .class with .java to get the source file name
                        String sourceFileName = currentClass.getSimpleName() + ".java";

                        // Construct the full path to the .java source file
                        String sourceFilePath = classDirectory + sourceFileName;

                        //System.out.println("Path to the .java source file: " + sourceFilePath);
                        return sourceFilePath;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Unable to determine the .java source file path.");
                }
            } else {
                System.out.println("Unable to determine the .java source file path.");
            }

            return "null";
        }

    public boolean checkIfTree(int tree_id, int plot_id){

        if(!tree_belongs_to_this_plot.containsKey(plot_id))
            return false;

        if(tree_belongs_to_this_plot.get(plot_id).contains(tree_id))
            return true;

        return false;

    }

    @SuppressWarnings("unchecked")
    public void addIndexFiles() throws Exception{

        File temppi;
        for(int i = 0; i < pointClouds.size(); i++){
            temppi = fo.createNewFileWithNewExtension(pointClouds.get(i).getFile(), ".lasx");

            if(temppi.exists()){

                FileInputStream fileIn = new FileInputStream(temppi);
                ObjectInputStream in2 = new ObjectInputStream(fileIn);
                HashMap<Integer, ArrayList<Long>> temppi2;
                temppi2 = (HashMap<Integer, ArrayList<Long>>) in2.readObject();

                pointClouds.get(i).isIndexed = true;

                pointClouds.get(i).setIndexMap(temppi2);
            }
        }

    }

    public synchronized int get_thread_safe_id(){

        return thread_safe_id++;

    }

    public void setArgs(String[] args){
        this.args = args;
    }

    public void setInclusionRule(PointInclusionRule in){
        this.inclusionRule = in;
    }

    public void setModifyRule(PointModifyRule in){
        this.modifyRule = in;
    }

    public PointInclusionRule getInclusionRule(){
        return this.inclusionRule;
    }
    public PointModifyRule getModifyRule(){
        return this.modifyRule;
    }

    public void setPointClouds(ArrayList<LASReader> in){
        this.pointClouds = in;
    }

    public void setInputFiles(ArrayList<File> in){
        this.inputFiles = in;
    }


    public void changePointType(int newPointType, LASReader pointCloud){

        pointCloud.pointDataRecordFormat = newPointType;

        if(newPointType == 0){
            pointCloud.pointDataRecordLength = 20;
        }else if(newPointType == 1){
            pointCloud.pointDataRecordLength = 28;
        }else if(newPointType == 2){
            pointCloud.pointDataRecordLength = 26;
        }else if(newPointType == 3){
            pointCloud.pointDataRecordLength = 34;
        }else if(newPointType == 4){
            pointCloud.pointDataRecordLength = 57;
        }else if(newPointType == 5){
            pointCloud.pointDataRecordLength = 63;
        }


    }

    public synchronized void writeLineToLogFile(String line){

        try{
            FileWriter fstream = new FileWriter(logFile, true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(line + "\n");
            out.close();
        }catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }

    }


}


