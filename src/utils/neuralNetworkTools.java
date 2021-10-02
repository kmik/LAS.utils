package utils;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import java.io.*;
import java.util.Arrays;

public class neuralNetworkTools {

    argumentReader aR = null;

    public neuralNetworkTools(argumentReader aR) throws IOException {

        this.aR = aR;

    }

    public void printNetwork(File inputNetwork) throws IOException{

        System.out.println("HEREHEREHER");

        MultiLayerNetwork model = MultiLayerNetwork.load(inputNetwork, true);
        //ComputationGraph model = ComputationGraph.load(inputNetwork, true);
        System.out.println(model.summary());
        System.out.println(model.getUpdater().toString());
        System.out.println(model.conf().toJson());
        System.out.println(model.conf().toString());
        //System.out.println(model.getConfiguration().toString());

    }

    public void modelToJson(File inputNetwork) throws IOException{

        MultiLayerNetwork model = MultiLayerNetwork.load(inputNetwork, true);



    }

    public void searchArbiterOutput() throws IOException{

        File file = new File(aR.idir);
        String[] directories = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });

        System.out.println(Arrays.toString(directories));

        double high_score = Double.NEGATIVE_INFINITY;
        int hight_score_index = -1;

        for(int i = 0; i < directories.length; i++){

            String directory = aR.idir + System.getProperty("file.separator") + directories[i] + System.getProperty("file.separator");

            String text = null;

            BufferedReader brTest = new BufferedReader(new FileReader(directory + "score.txt"));
            text = brTest .readLine();

            double score = Double.parseDouble(text);

            if(score > high_score){
                high_score = score;
                hight_score_index = i;
            }

            System.out.println(i + " / " + directories.length);
        }

        System.out.println("High score: " + high_score);

        String bestModelString = aR.idir + System.getProperty("file.separator") + directories[hight_score_index] + System.getProperty("file.separator");
        bestModelString += "model.bin";

        this.printNetwork(new File(bestModelString));
    }

}
