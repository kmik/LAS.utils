package utils;

import LASio.LASReader;
//import org.bytedeco.libfreenect._freenect_context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class lasClipMetricOfile {

    ArrayList<File> echo_class_files = new ArrayList<>();
    ArrayList<File> convolution_files = new ArrayList<>();
    ArrayList<FileWriter> echo_class_FileWriter = new ArrayList<>();
    ArrayList<FileWriter> convolution_FileWriter = new ArrayList<>();
    argumentReader aR;
    int closeCommands = 0;

    boolean colnamesWritten = false;

    public lasClipMetricOfile(argumentReader aR){
        this.aR = aR;
    }

    public void prep(){

        try {

            echo_class_files.add(aR.createOutputFileWithExtension(new LASReader(aR.inputFiles.get(0)), "_metrics_a.txt"));
            echo_class_FileWriter.add(new FileWriter(echo_class_files.get(echo_class_files.size()-1)));

            echo_class_files.add(aR.createOutputFileWithExtension(new LASReader(aR.inputFiles.get(0)), "_metrics_f.txt"));
            echo_class_FileWriter.add(new FileWriter(echo_class_files.get(echo_class_files.size()-1)));

            echo_class_files.add(aR.createOutputFileWithExtension(new LASReader(aR.inputFiles.get(0)), "_metrics_l.txt"));
            echo_class_FileWriter.add(new FileWriter(echo_class_files.get(echo_class_files.size()-1)));

            echo_class_files.add(aR.createOutputFileWithExtension(new LASReader(aR.inputFiles.get(0)), "_metrics_i.txt"));
            echo_class_FileWriter.add(new FileWriter(echo_class_files.get(echo_class_files.size()-1)));

            convolution_files.add(aR.createOutputFileWithExtension(new LASReader(aR.inputFiles.get(0)), "_convo_a.txt"));
            convolution_FileWriter.add(new FileWriter(echo_class_files.get(echo_class_files.size()-1)));


        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public void prep(File in){



        try {

            echo_class_files.add(aR.fo.createNewFileWithNewExtension(in, "_metrics_a.txt"));
            echo_class_FileWriter.add(new FileWriter(echo_class_files.get(echo_class_files.size()-1)));

            echo_class_files.add(aR.fo.createNewFileWithNewExtension(in, "_metrics_f.txt"));
            echo_class_FileWriter.add(new FileWriter(echo_class_files.get(echo_class_files.size()-1)));

            echo_class_files.add(aR.fo.createNewFileWithNewExtension(in, "_metrics_l.txt"));
            echo_class_FileWriter.add(new FileWriter(echo_class_files.get(echo_class_files.size()-1)));

            echo_class_files.add(aR.fo.createNewFileWithNewExtension(in, "_metrics_i.txt"));
            echo_class_FileWriter.add(new FileWriter(echo_class_files.get(echo_class_files.size()-1)));

            convolution_files.add(aR.createOutputFileWithExtension(in, "_convo_a.txt"));
            convolution_FileWriter.add(new FileWriter(convolution_files.get(convolution_files.size()-1)));


        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public void prepZonal(File in){



        try {

            //echo_class_files.add(aR.fo.createNewFileWithNewExtension(in, "_metrics_a.txt"));
            echo_class_files.add(aR.createOutputFile(in, "_metrics_a.txt"));

            echo_class_FileWriter.add(new FileWriter(echo_class_files.get(echo_class_files.size()-1)));


        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public void prepZonal(File in, String ext){



        try {

            //echo_class_files.add(aR.fo.createNewFileWithNewExtension(in, ext));
            echo_class_files.add(aR.createOutputFile(in, ext));



            echo_class_FileWriter.add(new FileWriter(echo_class_files.get(echo_class_files.size()-1)));


        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public void prepZonal_(String outputname){

        try {

            //echo_class_files.add(aR.fo.createNewFileWithNewExtension(in, ext));
            echo_class_files.add(aR._createOutputFile_(outputname));

            //System.out.println("output file: " + echo_class_files.get(echo_class_files.size()-1).getAbsolutePath());

            echo_class_FileWriter.add(new FileWriter(echo_class_files.get(echo_class_files.size()-1)));

        }catch (IOException e){
            e.printStackTrace();
        }

    }
    public synchronized void writeColumnNames_convo(ArrayList<String> colnames_convo){

        if(colnamesWritten)
            return;

        try {

            convolution_FileWriter.get(0).write("poly_id\t");

            for (int i = 0; i < colnames_convo.size(); i++) {

                convolution_FileWriter.get(0).write(colnames_convo.get(i) + "\t");

            }

            convolution_FileWriter.get(0).write("\n");


        }catch (Exception e){
            e.printStackTrace();
        }

        this.colnamesWritten = true;

    }


    public synchronized void writeColumnNames(ArrayList<String> colnames_a, ArrayList<String> colnames_f, ArrayList<String> colnames_l, ArrayList<String> colnames_i){

        if(colnamesWritten)
            return;

        try {

            echo_class_FileWriter.get(0).write("poly_id\t");
            echo_class_FileWriter.get(1).write("poly_id\t");
            echo_class_FileWriter.get(2).write("poly_id\t");
            echo_class_FileWriter.get(3).write("poly_id\t");

            for (int i = 0; i < colnames_a.size(); i++) {

                echo_class_FileWriter.get(0).write(colnames_a.get(i) + "\t");
                echo_class_FileWriter.get(2).write(colnames_l.get(i) + "\t");
                echo_class_FileWriter.get(3).write(colnames_i.get(i) + "\t");

            }

            for (int i = 0; i < colnames_f.size(); i++) {

                echo_class_FileWriter.get(1).write(colnames_f.get(i) + "\t");

            }

            echo_class_FileWriter.get(0).write("\n");
            echo_class_FileWriter.get(1).write("\n");
            echo_class_FileWriter.get(2).write("\n");
            echo_class_FileWriter.get(3).write("\n");

        }catch (Exception e){
            e.printStackTrace();
        }

        this.colnamesWritten = true;

    }

    public synchronized void writeColumnNamesZonal(ArrayList<String> colnames_a){

        if(colnamesWritten)
            return;

        try {

            echo_class_FileWriter.get(0).write("poly_id\t");


            for (int i = 0; i < colnames_a.size(); i++) {

                echo_class_FileWriter.get(0).write(colnames_a.get(i) + "\t");


            }

            echo_class_FileWriter.get(0).write("\n");


        }catch (Exception e){
            e.printStackTrace();
        }

        this.colnamesWritten = true;

    }
    public synchronized void writeColumnNamesZonalGrid(ArrayList<String> colnames_a){

        if(colnamesWritten)
            return;

        try {

            echo_class_FileWriter.get(0).write("poly_id\tx\ty\t");


            for (int i = 0; i < colnames_a.size(); i++) {

                echo_class_FileWriter.get(0).write(colnames_a.get(i) + "\t");


            }

            echo_class_FileWriter.get(0).write("\n");


        }catch (Exception e){
            e.printStackTrace();
        }

        this.colnamesWritten = true;

    }
    public synchronized void writeLine_convo(ArrayList<ArrayList<Double>> metrics, ArrayList<String> colnames, double poly_id){

        if(!colnamesWritten) {


            this.writeColumnNames_convo(colnames);

        }

        try {

            for(int i_ = 0; i_ < metrics.size(); i_++) {

                convolution_FileWriter.get(0).write(poly_id + "\t");

                for (int i = 0; i < metrics.get(i_).size(); i++) {

                    convolution_FileWriter.get(0).write(metrics.get(i_).get(i) + "\t");


                }


                convolution_FileWriter.get(0).write("\n");
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public synchronized void writeLine_convo_test(ArrayList<Double> metrics, ArrayList<String> colnames, double poly_id){

        if(!colnamesWritten) {


            this.writeColumnNames_convo(colnames);

        }

        try {

            //for(int i_ = 0; i_ < metrics.size(); i_++) {

                convolution_FileWriter.get(0).write(poly_id + "\t");

                for (int i = 0; i < metrics.size(); i++) {

                    convolution_FileWriter.get(0).write(metrics.get(i) + "\t");

                }
                convolution_FileWriter.get(0).write("\n");
            //}
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    public synchronized void writeLine(ArrayList<Double> metrics_a, ArrayList<Double> metrics_f, ArrayList<Double> metrics_l, ArrayList<Double> metrics_i,
                                       ArrayList<String> colnames_a, ArrayList<String> colnames_f, ArrayList<String> colnames_l, ArrayList<String> colnames_i, double poly_id){

        if(!colnamesWritten) {


            this.writeColumnNames(colnames_a, colnames_f, colnames_l, colnames_i);

        }

        try {

            echo_class_FileWriter.get(0).write(poly_id + "\t");
            echo_class_FileWriter.get(1).write(poly_id + "\t");
            echo_class_FileWriter.get(2).write(poly_id + "\t");
            echo_class_FileWriter.get(3).write(poly_id + "\t");

            for (int i = 0; i < metrics_a.size(); i++) {

                echo_class_FileWriter.get(0).write(metrics_a.get(i) + "\t");

                echo_class_FileWriter.get(2).write(metrics_l.get(i) + "\t");
                echo_class_FileWriter.get(3).write(metrics_i.get(i) + "\t");

            }
            for(int i = 0; i < metrics_f.size(); i++){
                echo_class_FileWriter.get(1).write(metrics_f.get(i) + "\t");
            }

            echo_class_FileWriter.get(0).write("\n");
            echo_class_FileWriter.get(1).write("\n");
            echo_class_FileWriter.get(2).write("\n");
            echo_class_FileWriter.get(3).write("\n");

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public synchronized <T> void writeLineZonal(ArrayList<Double> metrics_a,
                                       ArrayList<String> colnames_a, T poly_id){

        if(!colnamesWritten) {

            this.writeColumnNamesZonal(colnames_a);

        }

        try {

            echo_class_FileWriter.get(0).write(poly_id + "\t");

            for (int i = 0; i < metrics_a.size(); i++) {

                echo_class_FileWriter.get(0).write(metrics_a.get(i) + "\t");

            }

            echo_class_FileWriter.get(0).write("\n");

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public synchronized void writeLineZonal(ArrayList<Double> metrics_a,
                                            ArrayList<String> colnames_a, double poly_id, ArrayList<String[]> metadata, int nMetadata){


        for(int i = 0; i < nMetadata; i++)
            colnames_a.add(metadata.get(i)[0]);

        if(!colnamesWritten) {

            this.writeColumnNamesZonal(colnames_a);

        }

        try {

            echo_class_FileWriter.get(0).write(poly_id + "\t");

            for (int i = 0; i < metrics_a.size(); i++) {

                echo_class_FileWriter.get(0).write(metrics_a.get(i) + "\t");

            }

            if(metadata.size() == nMetadata)
                for(int i = 0; i < metadata.size(); i++)
                    echo_class_FileWriter.get(0).write(metadata.get(i)[1] + "\t");
            else{

                int howMany = metadata.size() / nMetadata;

                for(int k = 0; k < nMetadata; k++) {

                    for (int i = k; i < metadata.size(); i += nMetadata) {

                        echo_class_FileWriter.get(0).write(metadata.get(i)[1]);
                        if (i + nMetadata < metadata.size())
                            echo_class_FileWriter.get(0).write(";");

                    }

                    echo_class_FileWriter.get(0).write("\t");
                }

                // remove the last ;



            }

            echo_class_FileWriter.get(0).write("\n");

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public synchronized <T> void writeLineZonal(ArrayList<Double> metrics_a,
                                            ArrayList<String> colnames_a, T poly_id, ArrayList<String[]> metadata, int nMetadata, int mostPixels,
                                                String mapSheetName){

        colnames_a.add("MapSheetName");

        for(int i = 0; i < nMetadata; i++)
            colnames_a.add(aR.metadataitems.get(i));

        if(!colnamesWritten) {

            this.writeColumnNamesZonal(colnames_a);

        }

        try {

            echo_class_FileWriter.get(0).write(poly_id + "\t");

            for (int i = 0; i < metrics_a.size(); i++) {

                echo_class_FileWriter.get(0).write(metrics_a.get(i) + "\t");

            }

            echo_class_FileWriter.get(0).write(mapSheetName + "\t");



            //System.out.println(mostPixels);

            if(metadata.size() == nMetadata)
                for(int i = 0; i < metadata.size(); i++)
                    echo_class_FileWriter.get(0).write(metadata.get(i)[1] + "\t");
            else{

                int howMany = metadata.size() / nMetadata;

                if(metadata.size() != 0) {
                    for (int k = 0; k < nMetadata; k++) {
                        int counter = 0;
                        for (int i = k; i < metadata.size(); i += nMetadata) {

                            if (counter++ == mostPixels) {

                                if(metadata.size() > i){
                                    echo_class_FileWriter.get(0).write(metadata.get(i)[1]);
                                }else{
                                    echo_class_FileWriter.get(0).write("null");
                                }
                                //if (i + nMetadata < metadata.size())
                                //    echo_class_FileWriter.get(0).write(";");
                            }

                        }

                        echo_class_FileWriter.get(0).write("\t");

                    }
                }else{

                        for (int k = 0; k < nMetadata; k++) {

                            echo_class_FileWriter.get(0).write("null\t");

                        }
                }

            }

            echo_class_FileWriter.get(0).write("\n");

        }catch (Exception e){
            e.printStackTrace();
        }

    }


    public synchronized void writeLineZonalGrid(ArrayList<Double> metrics_a,
                                            ArrayList<String> colnames_a, double poly_id, double x_coord, double y_coord){

        if(!colnamesWritten) {

            this.writeColumnNamesZonalGrid(colnames_a);

        }

        try {

            echo_class_FileWriter.get(0).write(poly_id + "\t");
            echo_class_FileWriter.get(0).write(x_coord + "\t");
            echo_class_FileWriter.get(0).write(y_coord + "\t");

            for (int i = 0; i < metrics_a.size(); i++) {

                echo_class_FileWriter.get(0).write(metrics_a.get(i) + "\t");

            }

            echo_class_FileWriter.get(0).write("\n");

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public synchronized void writeLineZonalGrid(ArrayList<Double> metrics_a,
                                                ArrayList<String> colnames_a, double poly_id, double x_coord, double y_coord, ArrayList<String[]> metadata, int nMetadata){



        for(int i = 0; i < nMetadata; i++)
            colnames_a.add(metadata.get(i)[0]);

        if(!colnamesWritten) {

            this.writeColumnNamesZonalGrid(colnames_a);

        }

        try {

            echo_class_FileWriter.get(0).write(poly_id + "\t");
            echo_class_FileWriter.get(0).write(x_coord + "\t");
            echo_class_FileWriter.get(0).write(y_coord + "\t");

            for (int i = 0; i < metrics_a.size(); i++) {

                echo_class_FileWriter.get(0).write(metrics_a.get(i) + "\t");

            }

            if(metadata.size() == nMetadata)
                for(int i = 0; i < metadata.size(); i++)
                    echo_class_FileWriter.get(0).write(metadata.get(i)[1] + "\t");
            else{

                int howMany = metadata.size() / nMetadata;

                //System.out.println("howMany: " + howMany);
                //System.exit(1);

                for(int k = 0; k < nMetadata; k++) {

                    for (int i = k; i < metadata.size(); i += nMetadata) {

                        echo_class_FileWriter.get(0).write(metadata.get(i)[1]);
                        if (i + nMetadata < metadata.size())
                            echo_class_FileWriter.get(0).write(";");

                    }

                    echo_class_FileWriter.get(0).write("\t");
                }

                // remove the last ;



            }

            echo_class_FileWriter.get(0).write("\n");

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public synchronized void writeLineZonalGrid(ArrayList<Double> metrics_a,
                                                ArrayList<String> colnames_a, double poly_id, double x_coord, double y_coord, ArrayList<String[]> metadata, int nMetadata, int mostPixels,
                                                String mapSheetName){

        colnames_a.add("MapSheetName");


        for(int i = 0; i < nMetadata; i++)
            //colnames_a.add(metadata.get(i)[0]);
            colnames_a.add(aR.metadataitems.get(i));

        if(!colnamesWritten) {

            this.writeColumnNamesZonalGrid(colnames_a);

        }

        try {

            echo_class_FileWriter.get(0).write(poly_id + "\t");
            echo_class_FileWriter.get(0).write(x_coord + "\t");
            echo_class_FileWriter.get(0).write(y_coord + "\t");

            for (int i = 0; i < metrics_a.size(); i++) {

                echo_class_FileWriter.get(0).write(metrics_a.get(i) + "\t");

            }

            echo_class_FileWriter.get(0).write(mapSheetName + "\t");

            if(metadata.size() == nMetadata)
                for(int i = 0; i < metadata.size(); i++)
                    if(metadata.get(i)[1] != null)
                        echo_class_FileWriter.get(0).write(metadata.get(i)[1] + "\t");
                    else
                        echo_class_FileWriter.get(0).write("null\t");
            else{

                int howMany = metadata.size() / nMetadata;

                //System.out.println("howMany: " + howMany);
                //System.exit(1);

                if(metadata.size() != 0) {
                    for (int k = 0; k < nMetadata; k++) {
                        int counter = 0;
                        for (int i = k; i < metadata.size(); i += nMetadata) {

                            if (counter++ == mostPixels) {
                                if(metadata.get(i)[1] != null){
                                    //String justTest = metadata.get(i)[1];
                                    try {
                                        echo_class_FileWriter.get(0).write(metadata.get(i)[1]);
                                    }catch (Exception e){
                                        System.out.println(metadata.get(i)[0]);
                                        System.out.println(metadata.get(i)[1]);
                                        e.printStackTrace();
                                        System.exit(1);
                                    }
                                }else{
                                    echo_class_FileWriter.get(0).write("null");
                                }

                            }
                        }
                        echo_class_FileWriter.get(0).write("\t");
                    }
                }
                else{
                    for (int k = 0; k < nMetadata; k++) {
                        echo_class_FileWriter.get(0).write("null" + "\t");
                    }
                }


                // remove the last ;



            }

            echo_class_FileWriter.get(0).write("\n");

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void closeFiles(){

        System.out.println("Closing output metric files");
        try {
            echo_class_FileWriter.get(0).close();
            echo_class_FileWriter.get(1).close();
            echo_class_FileWriter.get(2).close();
            echo_class_FileWriter.get(3).close();
            convolution_FileWriter.get(0).close();

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void closeFilesZonal(){

        System.out.println("Closing output metric files");
        try {
            echo_class_FileWriter.get(0).close();

        }catch (Exception e){
            e.printStackTrace();
        }

    }

}
