package utils;

import LASio.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

@SuppressWarnings("unchecked")
public class argumentReader {

    public lasReadWriteFactory pfac;
    public double edges = 0.0;

    public lasClipMetricOfile lCMO = new lasClipMetricOfile(this);

    public File mergedPointCloud = null;

    public File debug_file = null;


    public boolean olas = false;

    public double altitude = 0.0;
    public String[] files;

    public int layers = 1;

    public File measured_trees = null;

    public int field = 0;


    public String execDir;
    public File execDir_file;
    String firstDir = "null";

    fileOperations fo = new fileOperations();
    public ArrayList<LASReader> pointClouds = new ArrayList<LASReader>();
    public ArrayList<File> inputFiles = new ArrayList<>();
    public ArrayList<File> outputFiles = new ArrayList<File>();

    public int tool;

    public int batch_size = 32;


    public int prepare_nn_input = 0;

    public double orig_x = -1;
    public double orig_y = -1;

    public boolean interpolate = true;

    public boolean lasrelate = false;

    public boolean dz_on_the_fly = false;

    public int label_index = 160;

    public String neural_mode = "merged";

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

    public int cores = 1;

    public String otype = "las";

    public double numarg1;
    public double angle = 7.5;
    public double axgrid = 20;
    public String groundPoints = "-999";

    public boolean debug = false;



    public int set_seed = -1;

    public boolean by_gps_time = true;
    public boolean by_z_order = false;

    public String poly = "null";

    public double[] densities = new double[]{1.3, 2.5, 5.0, 7.5, 10.0, 15.0, 20.0, 25.0};
    public double percentiles = 0.05;

    public double z_cutoff = 1.3;
    public int br = 0;

    public boolean omet = false;

    String pathSep;

    public boolean photogrammetry = false;

    public double translate_x;
    public double translate_y;
    public double translate_z;
    public double translate_i;

    public int set_point_source_id = -1;

    public boolean skip_global = false;

    public String trajectory;

    public boolean thin3d = false;

    public boolean output_statistics = false;

    public boolean split = false;

    public String splitBy = "asd";

    public boolean remove_buffer = false;

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

    public double concavity = 50.0;

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

    public int[] sequence = new int[]{0,1,2};

    public boolean output_only_stemAlignInput = false;

    public argumentReader(){

    }

    public void setOutputFile(LASraf in){
        this.outputFile = in;
    }

    public void setExecDir(String dir){
        this.execDir = dir;
        this.execDir_file = new File(dir);

    }
    public argumentReader(String[] args){
        this.args = args;

        pfac = new lasReadWriteFactory(this);
        //System.out.println(args.length);

        //

        this.modifyRule = new PointModifyRule();
        this.inclusionRule = new PointInclusionRule(this.br == 0);
    }

    public void createOpts(){

        options = new Options();

        options.addOption( org.apache.commons.cli.Option.builder("i")
                .longOpt("input")
                .hasArg(true)
                .desc("Input data")
                .numberOfArgs(Option.UNLIMITED_VALUES)
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
                .longOpt("drop_z_above")
                .hasArg(true)
                .desc("Drop z above threshold")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("drop_classification")
                .hasArg(true)
                .desc("Drop a point class")
                .required(false)
                .build());
        options.addOption(Option.builder()
                .longOpt("keep_classification")
                .hasArg(true)
                .desc("Keep a point class")
                .required(false)
                .build());


        options.addOption(Option.builder()
                .longOpt("concavity")
                .hasArg(true)
                .desc("Concavity")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("concave")
                .hasArg(false)
                .desc("concave border")
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
                .longOpt("field")
                .hasArg(true)
                .desc("field")
                .required(false)
                .build());

        options.addOption(Option.builder()
                .longOpt("interior")
                .hasArg(true)
                .desc("inter")
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
                .longOpt("sa")
                .hasArg(false)
                .desc("output only stemalign input")
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
                .longOpt("save_file")
                .hasArg(true)
                .desc("name of the file to be saved")
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
                .longOpt("buffer")
                .hasArg(true)
                .desc("Buffer size")
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
                .longOpt("br")
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
                .longOpt("drop_last")
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


    }


    public void parseArguents() throws IOException {

        this.pathSep = System.getProperty("file.separator");

        this.tool = Integer.parseInt(args[0]);

        if(args.length <= 1){

            printHelp pH = new printHelp(this.tool);
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

            if (cmd.hasOption("i")) {

                files = cmd.getOptionValues("i");

                System.out.println(Arrays.toString(files));

                if(files[0].split(";").length > 1){
                    files = files[0].split(";");
                }



                //this.input = cmd.getOptionValue("i");


                ArrayList<String> temp = new ArrayList<>();

                for(String s : files){
                    if(new File(s).getName().split("\\.")[1].equals("las")){
                        temp.add(s);
                        this.inputFiles.add(new File(s));
                    }else{
                        temp.add(s);
                        this.inputFiles.add(new File(s));
                        System.out.println(s + " is not a LAS file, terminating");
                    }
                    //}
                }

                files = temp.toArray(new String[0]);

                System.out.println(Arrays.toString(files));
            }

            if (cmd.hasOption("seq")) {

                String sequ = cmd.getOptionValue("seq");
                //System.out.println(Arrays.toString(sequ));

                String[] seqit = sequ.split(",");

                this.sequence = new int[seqit.length];

                for(int i = 0; i < seqit.length; i++){

                    sequence[i] = Integer.parseInt(seqit[i]) - 1;

                }

            }

            if (cmd.hasOption("c")) {

                this.cores = Integer.parseInt(cmd.getOptionValue("c"));
            }

            if (cmd.hasOption("set_seed")) {

                this.set_seed = Integer.parseInt(cmd.getOptionValue("set_seed"));
            }

            if(cmd.hasOption("change_point_format")){

                this.change_point_type = Integer.parseInt(cmd.getOptionValue("change_point_format"));

                this.inclusionRule.changePointFormat(this.change_point_type);
            }

            if(cmd.hasOption("change_version_minor")){

                this.change_version_minor = Integer.parseInt(cmd.getOptionValue("change_version_minor"));

            }

            if (cmd.hasOption("label_index")) {

                this.label_index = Integer.parseInt(cmd.getOptionValue("label_index"));
            }

            if (cmd.hasOption("drop_classification")) {

                this.inclusionRule.dropClassification(Integer.parseInt(cmd.getOptionValue("drop_classification")));
            }



            if (cmd.hasOption("learning_rate")) {

                this.learning_rate = (Double.parseDouble(cmd.getOptionValue("learning_rate")));
            }

            if (cmd.hasOption("layers")) {

                this.layers = (Integer.parseInt(cmd.getOptionValue("layers")));
            }



            if (cmd.hasOption("iparse")) {

                this.iparse = cmd.getOptionValue("iparse");
            }

            if (cmd.hasOption("oparse")) {

                this.oparse = cmd.getOptionValue("oparse");
            }

            if (cmd.hasOption("debug_file")) {

                this.debug_file = new File(cmd.getOptionValue("debug_file"));
            }

            if (cmd.hasOption("o")) {

                this.output = cmd.getOptionValue("o");
            }

            if (cmd.hasOption("step")) {

                this.step = Double.parseDouble(cmd.getOptionValue("step"));

            }

            if (cmd.hasOption("alt")) {

                this.altitude = Double.parseDouble(cmd.getOptionValue("alt"));

            }

            if (cmd.hasOption("prep_nn_input")) {

                this.prepare_nn_input = Integer.parseInt(cmd.getOptionValue("prep_nn_input"));

            }

            if (cmd.hasOption("few")) {

                this.few = Integer.parseInt(cmd.getOptionValue("few"));

            }

            if (cmd.hasOption("echoClass")) {

                this.echoClass = true;

            }

            if (cmd.hasOption("sa")) {

                this.output_only_stemAlignInput = true;

            }

            if (cmd.hasOption("poly")) {

                this.poly = cmd.getOptionValue("poly");

            }

            if (cmd.hasOption("convolution_option")) {

                this.convolution_option = Integer.parseInt(cmd.getOptionValue("convolution_option"));

            }



            if (cmd.hasOption("measured_trees")) {

                this.measured_trees = new File(cmd.getOptionValue("measured_trees"));

            }

            if (cmd.hasOption("dist")) {

                this.dist = Double.parseDouble(cmd.getOptionValue("dist"));

            }

            if (cmd.hasOption("delta")) {

                this.delta = Double.parseDouble(cmd.getOptionValue("delta"));

            }


            if (cmd.hasOption("set_classification")) {

                this.inclusionRule.setClassification(Integer.parseInt(cmd.getOptionValue("set_classification")));

            }
            if (cmd.hasOption("field")) {

                this.field = Integer.parseInt(cmd.getOptionValue("field"));

            }

            if(cmd.hasOption("skip_global")){
                this.skip_global = true;
            }

            if (cmd.hasOption("drop_noise")) {

                this.inclusionRule.dropNoise();

            }

            if (cmd.hasOption("thin3d")) {

                this.thin3d = true;

            }

            if (cmd.hasOption("photogrammetry")) {

                this.photogrammetry = true;

            }

            if (cmd.hasOption("olas")) {

                this.olas = true;

            }

            if (cmd.hasOption("splitBy")) {

                this.splitBy = cmd.getOptionValue("splitBy");

            }

            if (cmd.hasOption("set_point_source_id")){

                this.inclusionRule.setPointSourceId(Integer.parseInt(cmd.getOptionValue("set_point_source_id")));

            }

            if (cmd.hasOption("train")) {

                this.train = new File(cmd.getOptionValue("train"));

            }

            if (cmd.hasOption("test")) {

                this.test = new File(cmd.getOptionValue("test"));

            }

            if (cmd.hasOption("validation")) {

                this.validation = new File(cmd.getOptionValue("validation"));

            }

            if (cmd.hasOption("train2")) {

                this.train_2 = new File(cmd.getOptionValue("train2"));

            }

            if (cmd.hasOption("test2")) {

                this.test_2 = new File(cmd.getOptionValue("test2"));

            }

            if (cmd.hasOption("validation2")) {

                this.validation_2 = new File(cmd.getOptionValue("validation2"));

            }

            if (cmd.hasOption("time")) {

                this.time = Integer.parseInt(cmd.getOptionValue("time"));

            }

            if (cmd.hasOption("dz_")) {

                this.dz_on_the_fly = true;

            }

            if (cmd.hasOption("interior")) {

                this.interior = cmd.getOptionValue("interior");

            }

            if (cmd.hasOption("save_file")) {

                this.save_file = cmd.getOptionValue("save_file");

            }



            if (cmd.hasOption("axGrid")) {

                this.axgrid = Double.parseDouble(cmd.getOptionValue("axGrid"));
                //System.out.println("SET AXGRID: " + Double.parseDouble(cmd.getOptionValue("axGrid")));
            }

            if (cmd.hasOption("kernel")) {

                this.kernel = Integer.parseInt(cmd.getOptionValue("kernel"));

            }

            if (cmd.hasOption("set_user_data")) {

                this.inclusionRule.setUserData(Integer.parseInt(cmd.getOptionValue("set_user_data")));

            }

            if (cmd.hasOption("theta")) {

                this.theta = Double.parseDouble(cmd.getOptionValue("theta"));

            }

            if (cmd.hasOption("buffer")) {

                this.buffer = Double.parseDouble(cmd.getOptionValue("buffer"));

            }

            if (cmd.hasOption("orig_x")) {

                this.orig_x = Double.parseDouble(cmd.getOptionValue("orig_x"));

            }if (cmd.hasOption("orig_y")) {

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

            }

            if (cmd.hasOption("otype")) {

                this.otype = cmd.getOptionValue("otype");

            }

            if (cmd.hasOption("o_dz")) {

                this.o_dz = true;

            }

            if (cmd.hasOption("sep")) {

                this.sep = cmd.getOptionValue("sep");

            }

            if (cmd.hasOption("neural_mode")) {


                this.neural_mode = cmd.getOptionValue("neural_mode");

            }

            if (cmd.hasOption("method")) {

                this.method = cmd.getOptionValue("method");

            }options.addOption(Option.builder()
                .longOpt("dz_")
                .hasArg(false)
                .desc("dz on the fly")
                .required(false)
                .build());

            if (cmd.hasOption("traj")) {

                this.trajectory = cmd.getOptionValue("traj");

            }

            if (cmd.hasOption("idir")) {

                this.idir = cmd.getOptionValue("idir");
                File odr = new File(idir);

                if (!odr.isDirectory()) {

                    odr.mkdir();
                    System.out.println(odr.getAbsolutePath());
                    System.out.println("IDIR IS NOT DIRECTORY, exiting");
                    System.exit(1);
                }

                this.idir = odr.getCanonicalPath() + System.getProperty("file.separator");

            }

                if (cmd.hasOption("odir")) {

                this.odir = cmd.getOptionValue("odir");
                File odr = new File(odir);

                if(!odr.isDirectory()){

                    odr.mkdir();
                    System.out.println(odr.getAbsolutePath());
                    System.out.println("ODIR IS NOT DIRECTORY, exiting");
                    System.exit(1);
                }

                this.odir = odr.getCanonicalPath() + System.getProperty("file.separator");

                //


                if(false)
                if(odir.charAt(0) == '/' || odir.charAt(0) == '\\'){
                    odir = odir.substring(1);
                }

                String odirFirst = "asd";


    /*
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

*/
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

            helpPrinter.printHelp(1);

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

            System.out.println(tempPath);
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
            tempFile = fo.createNewFileWithNewExtension(tempFile, "_1" + extensionHere);
        }

        if(tempFile.exists())
            tempFile.delete();


        tempFile.createNewFile();

        return tempFile;

    }

    public File createOutputFileWithExtension(LASReader in, String extension) throws IOException {

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

        String extensionHere = extension;
        if(tempFile.exists()){

            tempFile = fo.createNewFileWithNewExtension(tempFile, extensionHere);
        }

        if(tempFile.exists())
            tempFile.delete();


        tempFile.createNewFile();

        return tempFile;
    }

    public File createOutputFileWithExtension(File in, String extension) throws IOException {


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

        String extensionHere = extension;
        if(tempFile.exists()){

            tempFile = fo.createNewFileWithNewExtension(tempFile, extensionHere);
        }

        if(tempFile.exists())
            tempFile.delete();

        tempFile.createNewFile();

        return tempFile;

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
}


