package utils;

import LASio.LASReader;
//import org.bytedeco.libfreenect._freenect_context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class lasClipMetricOfile {

    ArrayList<File> echo_class_files = new ArrayList<>();
    ArrayList<FileWriter> echo_class_FileWriter = new ArrayList<>();
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

        }catch (IOException e){
            e.printStackTrace();
        }

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

    public void closeFiles(){

        System.out.println("Closing output metric files");
        try {
            echo_class_FileWriter.get(0).close();
            echo_class_FileWriter.get(1).close();
            echo_class_FileWriter.get(2).close();
            echo_class_FileWriter.get(3).close();

        }catch (Exception e){
            e.printStackTrace();
        }

    }

}
