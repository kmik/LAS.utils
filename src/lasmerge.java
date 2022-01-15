import tools.Merger;
import tools.Tiler;
import utils.argumentReader;
import utils.fileDistributor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static utils.miscProcessing.prepareData;

public class lasmerge {


    public static void main(String[] args) throws IOException {

        argumentReader aR = new argumentReader(args);
        ArrayList<File> inputFiles = prepareData(aR, "lasmerge");
        fileDistributor fD = new fileDistributor(aR.inputFiles);

        try {
            Merger merge = new Merger(aR.inputFiles, aR.output, aR.getInclusionRule(), aR.odir, aR);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

