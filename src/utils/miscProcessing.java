package utils;

import LASio.LASReader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static runners.RunLASutils.getFileListAsString;

public class miscProcessing {



    static long tStart = System.currentTimeMillis();
    static long tEnd = System.currentTimeMillis();
    static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy 'at' hh:mm a");
    static LocalDateTime now = LocalDateTime.now();



    @NotNull
    public static ArrayList<File> prepareData(argumentReader aR, String toolName) throws IOException {

        tStart = System.currentTimeMillis();
        //dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy 'at' hh:mm a");
        now = LocalDateTime.now();


        aR.setExecDir(System.getProperty("user.dir"));
        aR.parseArguents(toolName);
        String pathSep = System.getProperty("file.separator");
        if (!System.getProperty("os.name").equals("Linux"))
            pathSep = "\\" + pathSep;
        boolean lasFormat, txtFormat;

        ArrayList<File> inputFiles = new ArrayList<>();

        if(aR.files != null)
        if(aR.files.length != 0) {


            String[] lasToken = new File(aR.files[0]).getName().split("\\.");
            lasFormat = lasToken[lasToken.length - 1].equals("las");
            txtFormat = lasToken[lasToken.length - 1].equals("txt");
            ArrayList<String> filesList = new ArrayList<String>();
            ArrayList<LASReader> pointClouds = new ArrayList<LASReader>();
            filesList = getFileListAsString(aR, lasFormat, txtFormat, filesList, inputFiles);

            aR.setInputFiles(inputFiles);
            aR.p_update.totalFiles = aR.pointClouds.size();

            if (aR.cores > aR.inputFiles.size())
                aR.cores = aR.inputFiles.size();

        }
        return inputFiles;

    }

    public static void printProcessingTime(){

        tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;

        int hours = (int) ((tDelta / (1000*60*60)) % 24);
        long minutes = (tDelta / 1000)  / 60;
        int seconds = (int)((tDelta / 1000) % 60);

        System.out.println("-------------------------------------");
        System.out.println("Start time: " + dtf.format(now));
        System.out.println("Processing took: " + hours + " h " + minutes + " min " + seconds + " sec");
        System.out.println("-------------------------------------");

    }


}
