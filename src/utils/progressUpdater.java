package utils;

public class progressUpdater {

    ThreadProgressBar progeBar = new ThreadProgressBar();

    double[] valuesStatic = new double[10];

    argumentReader aR;

    /** Global variables */

    public volatile int fileProgress = 0;
    public volatile int totalFiles = 0;
    public volatile int totalOutputFiles = 0;
    public volatile int[] threadProgress;
    public volatile int[] threadEnd;
    public volatile int[] threadInt;
    public volatile double[] threadDouble;
    public volatile String[] threadFile;

    /** LASstrip variables */

    public volatile int lasstrip_filesRead = 0;
    public volatile int lasstrip_tinProgress = 0;
    public volatile int lasstrip_filesWritten = 0;
    public volatile double lasstrip_averageImprovement = 0.0;
    public volatile double lasstrip_averageDifference = 0.0;
    public volatile double lasstrip_averageImprovement_std = 0.0;
    public volatile double lasstrip_averageDifference_std = 0.0;

    public volatile boolean lasstrip_boreDisabled = false;
    public volatile boolean lasstrip_boreDone = false;
    public volatile boolean lasstrip_boreInProgress = false;
    public volatile int lasstrip_pairProgress = 0;

    /** LASground variables */

    public volatile int lasground_fileProgress = 0;
    public volatile int lasground_totalFiles = 0;
    public volatile int lasground_axelssonGridSize = 0;
    public volatile int lasground_seedPoints = 0;
    public volatile int lasground_doneIndexes = 0;
    public volatile double lasground_angleThreshold = 0.0;
    public volatile double lasground_distanceThreshold = 0.0;
    public volatile int lasground_vertices = 0;

    /** LASheight variables */

    public volatile int lasheight_totalPoints = 0;
    public volatile int lasheight_processedPoints = 0;
    public volatile int lasheight_pointsOutsideTIN = 0;
    public volatile int lasheight_groundClass = 0;

    /** LASnoise variables */

    public volatile int lasnoise_stepSize = 0;
    public volatile int lasnoise_few = 0;
    public volatile int lasnoise_removed = 0;

    /** LASborder variables */

    public volatile String lasborder_mode = "concave";
    public volatile String lasborder_concavity = "concave";

    /** LAStiler variables */

    public volatile int lastile_tileSize = 0;
    public volatile int lastile_bufferSize = 0;

    /** LAS2dsm variables */
    public volatile int las2dsm_resolution = 0;
    public volatile int las2dsm_gaussianKernel = 0;
    public volatile double las2dsm_gaussianTheta = 0;
    public volatile boolean las2dsm_print = true;

    /** LASthin variables */

    public volatile double lasthin_kernel = 0;
    public volatile double lasthin_random = 0;
    public volatile String lasthin_mode = "asdi";

    /** LASclip variables */
    public volatile int lasclip_clippedPoints = 0;
    public volatile double lasclip_empty = 0;

    /** LASITD variables */
    public volatile double lasITD_gaussianKernel = 0.0;
    public volatile double lasITD_gaussiantheta = 0.0;
    public volatile double lasITD_CHMresolution = 0.0;
    public volatile double lasITD_itdKernel = 0.0;

    /** lasSplit variables */
    public volatile String lasSplit_splitCriterion = "null";

    /** las2txt variables */
    public volatile String las2txt_oparse = "xyz";


    public progressUpdater(argumentReader aR){

        this.aR = aR;
        this.threadProgress = new int[aR.cores];
        this.threadEnd = new int[aR.cores];
        this.threadFile = new String[aR.cores];
        this.threadDouble = new double[aR.cores];
        this.threadInt = new int[aR.cores];

    }

    public synchronized void updateProgressLasStrip(){

        System.out.printf(((char) 0x1b) + "[16A\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "LASutils - LasStrip, version (0.1), (c) Mikko Kukkonen");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%d / %d", "* Total number of LAS files", lasstrip_filesRead, aR.pointClouds.size() );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%d / %d", "* LAS pairs processed", lasstrip_tinProgress, aR.blokki.order.size());
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%d / %d", "* LAS files written", lasstrip_filesWritten,aR. blokki.pointClouds.size() );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%.2f", "* Average improvement (cm) ", this.lasstrip_averageImprovement);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%.2f", "* Average difference (cm) ", this.lasstrip_averageDifference);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%.2f", "* Average improvement, std (cm) ", this.lasstrip_averageImprovement_std);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%.2f", "* Average difference, std (cm) ", this.lasstrip_averageDifference_std);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%.2f ; %.0f", "* Points (total ; avg per pair)", aR.blokki.totalPoints, aR.blokki.totalPoints / aR.blokki.differenceBefore_2_to_1.size());
        if(lasstrip_boreDisabled)
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%s", "* Global optimization", "Disabled");
        else if(lasstrip_boreDone)
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%s", "* Global optimization", "done");
        else if(lasstrip_boreInProgress)
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%s", "* Global optimization", "In progress...");
        else
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s" + "%s", "* Global optimization", "Waiting");

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-20s" + progeBar.getProgress(aR.blokki.order.size(),lasstrip_tinProgress), "Tin creation");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-20s" + progeBar.getProgress(aR.blokki.order.size(), lasstrip_pairProgress), "Local optimization");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-20s" + progeBar.getProgress(aR.blokki.pointClouds.size(),this.lasstrip_filesWritten), "Cloud output");

    }

    public synchronized void updateProgressGroundDetector(){

        if(false)
            return;

        int first = 14;

        first += aR.cores * 1;

        String beginning = "[" + first + "A\r";

        System.out.printf(((char) 0x1b) + beginning + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "LASutils - Lasground, version (0.1), (c) Mikko Kukkonen");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d | %d", "* File progress:", this.lasground_fileProgress, this.totalFiles);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d", "* Grid size:", this.lasground_axelssonGridSize );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %.2f", "* Initial angle:", this.lasground_angleThreshold );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %.2f", "* Distance threshold:", this.lasground_distanceThreshold );

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t\t\t---Thread progress---");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8s" + "%-25s" + "%-12s" + "%-12s" + "%-12s", "thread", "file", "ground p", "angle", "seed");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------------------");

        for(int i = 0; i < aR.cores; i++) {
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8d" + "%-25s" + "%-12d" + "%-12.2f" + "%-12d", i+1, threadFile[i], threadProgress[i], threadDouble[i], threadEnd[i]);
        }

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

    }

    public synchronized void updateProgressNormalize(){


        int first = 12;


        if(true)
            return;

        first += aR.cores;


        String beginning = "[" + first + "A\r";

        System.out.printf(((char) 0x1b) + beginning + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "LASutils - Lasheight, version (0.1), (c) Mikko Kukkonen");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d | %d", "* File progress:", this.lasground_fileProgress, this.totalFiles);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d", "* Ground class:", this.lasheight_groundClass );


        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t\t\t---Thread progress---");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8s" + "%-25s" + "%-25s" + "%-10s", "thread", "file", "progress", "outside TIN");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------------------");

        for(int i = 0; i < aR.cores; i++) {
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8d" + "%-25s" + "%-25s" + "%-10d", i+1, threadFile[i],  progeBar.getProgress(threadEnd[i],threadProgress[i]), threadInt[i]);
        }

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");


    }

    public synchronized void updateProgressNoise(){

        int first = 14;


        if(false)
            return;

        first += aR.cores;

        String beginning = "[" + first + "A\r";

        System.out.printf(((char) 0x1b) + beginning + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "LASutils - Lasnoise, version (0.1), (c) Mikko Kukkonen");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d | %d", "* File progress:", this.fileProgress, this.totalFiles);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d", "* Voxel size (m):", this.lasnoise_stepSize );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d", "* Min points in voxel:", this.lasnoise_few );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d", "* Total removed points:", this.lasnoise_removed );

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t\t\t---Thread progress---");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8s" + "%-25s" + "%-25s" + "%-10s", "thread", "file", "progress", "removed");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "____________________________________________________________________________");

        for(int i = 0; i < aR.cores; i++) {
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8d" + "%-25s" + "%-25s" + "%-10d", i+1, threadFile[i],  progeBar.getProgress(threadEnd[i],threadProgress[i]), threadInt[i]);
        }

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

    }

    public synchronized void updateProgressNBorder(){


        int first = 13;

        first += aR.cores;


        String beginning = "[" + first + "A\r";

        System.out.printf(((char) 0x1b) + beginning + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "LASutils - lasborder, version (0.1), (c) Mikko Kukkonen");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d | %d", "* File progress:", this.fileProgress, this.totalFiles);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %-25s", "* Mode:", this.lasborder_mode );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %-25s", "* Concavity:", this.aR.concavity );

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t\t\t---Thread progress---");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8s" + "%-25s" + "%-25s" + "%-10s", "thread", "file", "progress", "vertices");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "____________________________________________________________________________");

        for(int i = 0; i < aR.cores; i++) {
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8d" + "%-25s" + "%-25s" + "%-10d", i+1, threadFile[i],  progeBar.getProgress(threadEnd[i],threadProgress[i]), threadInt[i]);
        }

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

    }

    public synchronized void updateProgressTiler(){

        if(true)
            return;

        int first = 13;

        first += aR.cores;

        String beginning = "[" + first + "A\r";

        System.out.printf(((char) 0x1b) + beginning + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "LASutils - lastile, version (0.1), (c) Mikko Kukkonen");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d | %d", "* File progress:", this.fileProgress, this.totalFiles);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d", "* Tile size:", this.lastile_tileSize );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d", "* Buffer:", this.lastile_bufferSize );

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t\t\t---Thread progress---");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8s" + "%-25s" + "%-25s" + "%-10s", "thread", "file", "progress", "vertices");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "____________________________________________________________________________");

        for(int i = 0; i < aR.cores; i++) {
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8d" + "%-25s" + "%-25s" + "%-10d", i+1, threadFile[i],  progeBar.getProgress(threadEnd[i],threadProgress[i]), threadInt[i]);
        }

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

    }

    public synchronized void updateProgressDSM(){

        if(!las2dsm_print)
            return;

        if(true)
            return;

        int first = 14;

        first += aR.cores;

        String beginning = "[" + first + "A\r";

        System.out.printf(((char) 0x1b) + beginning + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "LASutils - las2dsm, version (0.1), (c) Mikko Kukkonen");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d | %d", "* File progress:", this.fileProgress, this.totalFiles);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d", "* Resolution:", this.las2dsm_resolution );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d", "* Gaussian kernel:", this.las2dsm_gaussianKernel );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %.2f", "* Gaussian theta:", this.las2dsm_gaussianTheta );

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t\t\t---Thread progress---");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8s" + "%-25s" + "%-25s" + "%-10s", "thread", "operation", "progress", "vertices");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "____________________________________________________________________________");

        for(int i = 0; i < aR.cores; i++) {
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8d" + "%-25s" + "%-25s" + "%-10d", i+1, threadFile[i],  progeBar.getProgress(threadEnd[i],threadProgress[i]), threadInt[i]);
        }

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

    }

    public synchronized void updateProgressThin(){

        if(false)
            return;
        int first = 13;

        first += aR.cores;

        String beginning = "[" + first + "A\r";

        System.out.printf(((char) 0x1b) + beginning + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "LASutils - lasthin, version (0.1), (c) Mikko Kukkonen");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d | %d", "* File progress:", this.fileProgress, this.totalFiles);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %-25s", "* Mode:", this.lasthin_mode );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %.2f", "* Kernel size:", this.lasthin_kernel );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %.2f", "* n_random in kernel:", this.lasthin_random );

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t\t\t---Thread progress---");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8s" + "%-25s" + "%-25s" + "%-10s", "thread", "operation", "progress", "vertices");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "____________________________________________________________________________");

        for(int i = 0; i < aR.cores; i++) {
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8d" + "%-25s" + "%-25s" + "%-10d", i+1, threadFile[i],  progeBar.getProgress(threadEnd[i],threadProgress[i]), threadInt[i]);
        }

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

    }

    public synchronized void updateProgressClip(){

        if((true))
            return;

        int first = 13;

        first += aR.cores;

        String beginning = "[" + first + "A\r";

        System.out.printf(((char) 0x1b) + beginning + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "LASutils - lasclip, version (0.1), (c) Mikko Kukkonen");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d | %d", "* Polygon progress:", this.fileProgress, this.totalFiles);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %-25s", "* Clipped points:", this.lasclip_clippedPoints );
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %.0f", "* Empty polygons:", this.lasclip_empty);

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t\t\t---Thread progress---");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8s" + "%-25s" + "%-25s" + "%-10s", "thread", "current polygon", "progress", "completed");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "____________________________________________________________________________");

        for(int i = 0; i < aR.cores; i++) {
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8d" + "%-25s" + "%-25s" + "%-10d", i+1, threadFile[i],  progeBar.getProgress(threadEnd[i],threadProgress[i]), threadInt[i]);
        }

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

    }

    public synchronized void updateProgressITD(){

        if(false)
            return;

        int first = 15;

        first += aR.cores;

        String beginning = "[" + first + "A\r";

        System.out.printf(((char) 0x1b) + beginning + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "LASutils - lasITD, version (0.1), (c) Mikko Kukkonen");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d | %d", "* File progress:", this.fileProgress, this.totalFiles);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %-25s", "* CHM resolution:", this.lasITD_CHMresolution);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %.2f", "* CHM gaussian kernel:", this.lasITD_gaussianKernel);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %.2f", "* CHM gaussian theta:", this.lasITD_gaussiantheta);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %.2f", "* ITD kernel:", this.lasITD_itdKernel);

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t\t\t---Thread progress---");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8s" + "%-25s" + "%-25s" + "%-10s", "thread", "operation", "progress", "trees found");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "____________________________________________________________________________");

        for(int i = 0; i < aR.cores; i++) {
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8d" + "%-25s" + "%-25s" + "%-10d", i+1, threadFile[i],  progeBar.getProgress(threadEnd[i],threadProgress[i]), threadInt[i]);
        }

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

    }

    public synchronized void updateProgressSplit(){

        int first = 12;

        first += aR.cores;

        String beginning = "[" + first + "A\r";

        System.out.printf(((char) 0x1b) + beginning + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "LASutils - lassplit, version (0.1), (c) Mikko Kukkonen");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d | %d", "* File progress:", this.fileProgress, this.totalFiles);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %-25s", "* Split criterion:", this.lasSplit_splitCriterion );

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t\t\t---Thread progress---");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8s" + "%-25s" + "%-25s" + "%-10s", "thread", "file", "progress", "-");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "____________________________________________________________________________");

        for(int i = 0; i < aR.cores; i++) {
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8d" + "%-25s" + "%-25s" + "%-10d", i+1, threadFile[i],  progeBar.getProgress(threadEnd[i],threadProgress[i]), threadInt[i]);
        }

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

    }

    public synchronized void updateProgressLas2Las(){

        if(true)
            return;

        int first = 11;

        first += aR.cores;

        String beginning = "[" + first + "A\r";

        System.out.printf(((char) 0x1b) + beginning + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "LASutils - las2las, version (0.1), (c) Mikko Kukkonen");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d | %d", "* File progress:", this.fileProgress, this.totalFiles);

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t\t\t---Thread progress---");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8s" + "%-25s" + "%-25s" + "%-10s", "thread", "file", "progress", "-");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "____________________________________________________________________________");

        for(int i = 0; i < aR.cores; i++) {
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8d" + "%-25s" + "%-25s" + "%-10d", i+1, threadFile[i],  progeBar.getProgress(threadEnd[i],threadProgress[i]), threadInt[i]);
        }

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

    }

    public synchronized void updateProgressLas2txt(){

        int first = 12;

        first += aR.cores;

        String beginning = "[" + first + "A\r";

        System.out.printf(((char) 0x1b) + beginning + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "LASutils - las2las, version (0.1), (c) Mikko Kukkonen");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "-------------------------------------------------------");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %d | %d", "* File progress:", this.fileProgress, this.totalFiles);
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t%-35s %-25s", "* oparse:", this.las2txt_oparse );

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "\t\t\t---Thread progress---");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8s" + "%-25s" + "%-25s" + "%-10s", "thread", "file", "progress", "-");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "____________________________________________________________________________");

        for(int i = 0; i < aR.cores; i++) {
            System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K" + "%-8d" + "%-25s" + "%-25s" + "%-10d", i+1, threadFile[i],  progeBar.getProgress(threadEnd[i],threadProgress[i]), threadInt[i]);
        }

        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");
        System.out.printf(((char) 0x1b) + "[1B\r" + "\033[2K");

    }

}
