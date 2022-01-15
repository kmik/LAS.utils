package utils;

import LASio.LASReader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static runners.RunLASutils.getFileListAsString;

public class miscProcessing {



    @NotNull
    public static ArrayList<File> prepareData(argumentReader aR, String toolName) throws IOException {


        aR.setExecDir(System.getProperty("user.dir"));
        aR.parseArguents(toolName);
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

        aR.setInputFiles(inputFiles);
        aR.p_update.totalFiles = aR.pointClouds.size();
        return inputFiles;

    }
}
