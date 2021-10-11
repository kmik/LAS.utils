package tools;

import org.apache.log4j.BasicConfigurator;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;

import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.graph.MergeVertex;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.PerformanceListener;
import org.nd4j.jita.conf.CudaEnvironment;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import org.nd4j.linalg.factory.Nd4j;

import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.*;


import utils.argumentReader;

import java.io.*;
import java.util.*;

import static org.deeplearning4j.util.ModelSerializer.*;

//import static org.datavec.image.loader.BaseImageLoader.MultiPageMode.MINIBATCH;
//import static org.deeplearning4j.optimize.api.InvocationType.EPOCH_END;

@SuppressWarnings("ALL")
public class convolution {

    //private static Logger log = LoggerFactory.getLogger(convolution.class);
    //private final BatchNormalization normalizer_batch;

    public convolution(argumentReader aR) throws  Exception{

        if(Nd4j.backend.toString() != "org.nd4j.linalg.cpu.nativecpu.CpuBackend") {
            try {

                CudaEnvironment.getInstance().getConfiguration()
                        .setMaximumGridSize(512)
                        .setMaximumBlockSize(512);


                CudaEnvironment.getInstance().getConfiguration()
                        .setMaximumDeviceCacheableLength(1024 * 1024 * 1024L)
                        .setMaximumDeviceCache(32L * 1024 * 1024 * 1024L)
                        .setMaximumHostCacheableLength(1024 * 1024 * 1024L)
                        .setMaximumHostCache(32L * 1024 * 1024 * 1024L);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int batchSize = (int)Math.pow(2,7);

        batchSize = aR.batch_size;

        int n_epoch = 50;
/*
        int n_in = 4 * 4 * 10;

        int depth = 10;
        int _x = 4;
        int _y = 4;
*/

        int n_in = 4 * 4 * 10;

        int depth = 10;
        int _x = 4;
        int _y = 4;


        int n_in_tex = 40 * 40 * 40;

        n_in_tex = 160;

        int depth_tex = 40;
        int _x_tex = 40;
        int _y_tex = 40;

        /*
        int n_in = 40 * 40 * 40;

        int depth = 40;
        int _x = 40;
        int _y = 40;
*/
        if(aR.model != null && aR.prepare_nn_input == 0){

            File outFile = null;

            //if(aR.convolution_option < 4){
                outFile = new File(aR.output);
            //}else if(aR.convolution_option < 7){
              //  outFile = changeExtension(outFile, "_texture.txt");
            //}else{
              //  outFile = changeExtension(outFile, "_voxel.txt");
            //}

            //File outFile_tex = changeExtension(outFile, "_texture.txt");
            //File outFile_vox = changeExtension(outFile, "_voxel.txt");

            if(!outFile.exists())
                outFile.createNewFile();

            /*
            if(!outFile_tex.exists())
                outFile.createNewFile();

            if(!outFile_vox.exists())
                outFile_vox.createNewFile();
*/
            //File graph = changeExtension(aR.model, "_graph.bin");

            File graph = new File(aR.save_file);

            //File mod_320 = changeExtension(aR.model, "_320.bin");

            //MultiLayerNetwork model = MultiLayerNetwork.load(aR.model, false);

            ComputationGraph model_graph = ComputationGraph.load(graph, false);
            //MultiLayerNetwork model_320 = MultiLayerNetwork.load(mod_320, false);
            //System.out.println(model.conf().toString());
            //      System.exit(1);
/*
            RecordReader recordReader2 = new CSVRecordReader(0,",");
            RecordReader recordReader2_test = new CSVRecordReader(0,",");

            RecordReader rr_320 = new CSVRecordReader(0,",");
            RecordReader rr_320_test = new CSVRecordReader(0,",");

            rr_320.initialize(new FileSplit(aR.train_2));
            rr_320_test.initialize(new FileSplit(aR.test_2));

            recordReader2.initialize(new FileSplit(aR.train));
            recordReader2_test.initialize(new FileSplit(aR.test));

            int labelIndex2 = n_in;
            int batchSize2 =  batchSize;
            int numClasses2 = 3;

            DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2,batchSize2,labelIndex2,numClasses2);
            DataSetIterator testIter = new RecordReaderDataSetIterator(recordReader2_test,batchSize2,labelIndex2,numClasses2);

            DataSetIterator trainIter_2 = new RecordReaderDataSetIterator(rr_320,batchSize2,n_in_tex,numClasses2);
            DataSetIterator testIter_2 = new RecordReaderDataSetIterator(rr_320_test,batchSize2,n_in_tex,numClasses2);

            DataNormalization normalizer = new NormalizerStandardize();
            //DataNormalization normalizer = new NormalizerMinMaxScaler();
            DataNormalization normalizer_2 = new NormalizerStandardize();
            //DataNormalization normalizer_2 = new NormalizerMinMaxScaler();

            //DataNormalization normalizer = new NormalizerMinMaxScaler(0,1);
            normalizer.fit(trainIter);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data
            normalizer_2.fit(trainIter_2);

            trainIter.setPreProcessor(normalizer);
            testIter.setPreProcessor(normalizer);

            trainIter_2.setPreProcessor(normalizer_2);
            testIter_2.setPreProcessor(normalizer_2);
*/
            Evaluation eval = new Evaluation(3);
            Evaluation eval_320 = new Evaluation(3);
            Evaluation eval_graph = new Evaluation(3);

            ArrayList<Integer> pred = new ArrayList<>();
            ArrayList<Integer> pred_320 = new ArrayList<>();
            ArrayList<Integer> pred_graph = new ArrayList<>();
            ArrayList<Integer> obs = new ArrayList<>();


            ArrayList<INDArray> testSet = new ArrayList<>();
            ArrayList<INDArray> testSet_labels = new ArrayList<>();
            //ArrayList<INDArray> testSet_labels_rank2 = new ArrayList<>();

            //ArrayList<INDArray> trainingSet_2 = new ArrayList<>();
            //ArrayList<INDArray> trainingSet_2_labels = new ArrayList<>();
            ArrayList<INDArray> testSet_2 = new ArrayList<>();
            ArrayList<INDArray> testSet_2_labels = new ArrayList<>();


                System.out.println("READING NNIN!!!");
                System.out.println("READING NNIN!!!");
                System.out.println("READING NNIN!!!");
                try
                {
                    FileInputStream fis1 = new FileInputStream(aR.train);
                    FileInputStream fis2 = new FileInputStream(aR.train_2);
                    FileInputStream fis3 = new FileInputStream(aR.test);
                    FileInputStream fis4 = new FileInputStream(aR.test_2);
                    ObjectInputStream ois1 = new ObjectInputStream(fis1);
                    ObjectInputStream ois2 = new ObjectInputStream(fis2);
                    ObjectInputStream ois3 = new ObjectInputStream(fis3);
                    ObjectInputStream ois4 = new ObjectInputStream(fis4);

                    ArrayList<INDArray>[] train_in = null;
                    ArrayList<INDArray>[] train_in_2 = null;
                    ArrayList<INDArray>[] test_in = null;
                    ArrayList<INDArray>[] test_in_2 = null;

                    test_in = (ArrayList<INDArray>[]) ois3.readObject();
                    test_in_2 = (ArrayList<INDArray>[]) ois4.readObject();

                    ois1.close();
                    ois2.close();
                    ois3.close();
                    ois4.close();

                    fis1.close();
                    fis2.close();
                    fis3.close();
                    fis4.close();

                    testSet = test_in[0];
                    testSet_labels = test_in[1];

                    testSet_2 = test_in_2[0];
                    testSet_2_labels = test_in_2[1];

                }
                catch (IOException ioe)
                {
                    ioe.printStackTrace();
                    return;
                }

/*
            while(testIter.hasNext()){

                DataSet ds = testIter.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses2, 1, depth, _y, _x));
                testSet_labels.add(ds.getLabels());
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());

                testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses2, 1, depth, _y, _x));

            }

            while(testIter_2.hasNext()){

                DataSet ds = testIter_2.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());

                //testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                testSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses2, 1, depth_tex, _y_tex, _x_tex));
                //testSet_2.add(ds.getFeatures());
                testSet_2_labels.add(ds.getLabels());

            }

*/
            DataSet t = new DataSet();
            DataSet t2 = new DataSet();

            //for(int i_ = 0; i_ < trainingSet.size(); i_++){


            for(int i_ = 0; i_ < testSet.size(); i_++){

                t.setFeatures(testSet.get(i_));
                t2.setFeatures(testSet_2.get(i_));
                t.setLabels(testSet_labels.get(i_));
                t2.setLabels(testSet_2_labels.get(i_));


                //DataSet t = testIter.next();


                INDArray features = t.getFeatures();
                INDArray features_320 = t2.getFeatures();
                INDArray labels = t.getLabels();

                INDArray[] features_ = new INDArray[2];
                INDArray[] labels_ = new INDArray[1];

                features_[0] = t2.getFeatures();
                features_[1] = t.getFeatures();

                labels_[0] = t.getLabels();

                //labels[1] = ds.getLabels();

                MultiDataSet mds = new MultiDataSet(features, labels, null, null);

                //System.out.println(labels.columns());

                for(int i = 0; i < labels.rows(); i++){

                    int one = labels.getColumn(0).getInt(i);
                    int two = labels.getColumn(1).getInt(i);

                    if(one == 1){
                        obs.add(0);
                    }else if(two == 1){
                        obs.add(1);
                    }else{
                        obs.add(2);
                    }

                }


                //INDArray predicted = model.output(features,false);
                //INDArray predicted_320 = model_320.output(features_320,false);
                INDArray[] predicted_graph = null;

                //                = model_graph.output(false, features_, null, null);
                if(aR.convolution_option < 4){
                    predicted_graph = model_graph.output(false, features_, null, null);
                }
                else if(aR.convolution_option < 7){
                    predicted_graph = model_graph.output(features_320);
                }else{
                    predicted_graph = model_graph.output(features);
                }

/*
                //System.exit(1);
                for(int i = 0; i < predicted.rows(); i++){

                    double one = predicted.getColumn(0).getDouble(i);
                    double two = predicted.getColumn(1).getDouble(i);
                    double three = predicted.getColumn(2).getDouble(i);


                    if(one > two && one > three){
                        pred.add(0);

                    }else if(two > one && two > three){
                        pred.add(1);

                    }else{
                        pred.add(2);

                    }

                }

                eval.eval(labels, predicted);

                for(int i = 0; i < predicted_320.rows(); i++){

                    double one = predicted_320.getColumn(0).getDouble(i);
                    double two = predicted_320.getColumn(1).getDouble(i);
                    double three = predicted_320.getColumn(2).getDouble(i);


                    if(one > two && one > three){
                        pred_320.add(0);

                    }else if(two > one && two > three){
                        pred_320.add(1);

                    }else{
                        pred_320.add(2);

                    }

                }
                eval_320.eval(labels, predicted_320);
*/
                for(int i = 0; i < predicted_graph[0].rows(); i++){

                    double one = predicted_graph[0].getColumn(0).getDouble(i);
                    double two = predicted_graph[0].getColumn(1).getDouble(i);
                    double three = predicted_graph[0].getColumn(2).getDouble(i);


                    if(one > two && one > three){
                        pred_graph.add(0);

                    }else if(two > one && two > three){
                        pred_graph.add(1);

                    }else{
                        pred_graph.add(2);

                    }

                }
                eval_graph.eval(labels, predicted_graph[0]);

            }

            //System.out.println("PURE CONVO 4x4x10:");
            //System.out.println(eval.stats());

 //           System.out.println("FF 320");
   //         System.out.println(eval_320.stats());

            System.out.println("GRAPH");
            System.out.println(eval_graph.stats());

            /*
            while(testIter.hasNext()){


                DataSet t = testIter.next();


                INDArray features = t.getFeatures();
                INDArray labels = t.getLabels();
                System.out.println(labels.size(0));
                System.out.println(labels.size(1));
                System.out.println(labels.columns());

                for(int i = 0; i < labels.rows(); i++){

                    int one = labels.getColumn(0).getInt(i);
                    int two = labels.getColumn(1).getInt(i);

                    if(one == 1){
                        obs.add(0);
                    }else if(two == 1){
                        obs.add(1);
                    }else{
                        obs.add(2);
                    }

                }


                INDArray predicted = model.output(features,false);


                //System.exit(1);
                for(int i = 0; i < predicted.rows(); i++){

                    double one = predicted.getColumn(0).getDouble(i);
                    double two = predicted.getColumn(1).getDouble(i);
                    double three = predicted.getColumn(2).getDouble(i);


                    if(one > two && one > three){
                        pred.add(0);

                    }else if(two > one && two > three){
                        pred.add(1);

                    }else{
                        pred.add(2);

                    }

                }
                eval.eval(labels, predicted);
            }
*/


            //FileWriter fw = new FileWriter(outFile, true);
            //FileWriter fw_320 = new FileWriter(outFile_tex, true);
            FileWriter fw_graph = new FileWriter(outFile, true);

            //String outLinePred = Integer.toString(pred.get(0)) + ",";
            //String outLinePred_320 = Integer.toString(pred.get(0)) + ",";
            String outLinePred_graph = (pred_graph.get(0)) + ",";
            //String outLineObs = Integer.toString(obs.get(0)) + ",";
            //String outLineObs_320 = Integer.toString(obs.get(0)) + ",";
            String outLineObs_graph = (obs.get(0)) + ",";

            //System.out.println(Arrays.toString(pred.toArray()));
/*
            for(int i = 1; i < pred.size(); i++){

                outLinePred += Integer.toString(pred.get(i)) + ",";
                outLineObs += Integer.toString(obs.get(i)) + ",";

            }

            for(int i = 1; i < pred_320.size(); i++){

                outLinePred_320 += Integer.toString(pred_320.get(i)) + ",";
                outLineObs_320 += Integer.toString(obs.get(i)) + ",";

            }
*/
            for(int i = 1; i < pred_graph.size(); i++){

                outLinePred_graph += (pred_graph.get(i)) + ",";
                outLineObs_graph += (obs.get(i)) + ",";

            }


/*
            fw.write(outLinePred);
            fw.write("\n");
            fw.write(outLineObs);
            fw.write("\n");

            fw.close();

            fw_320.write(outLinePred_320);
            fw_320.write("\n");
            fw_320.write(outLineObs_320);
            fw_320.write("\n");

            fw_320.close();
*/
            fw_graph.write(outLinePred_graph);
            fw_graph.write("\n");
            fw_graph.write(outLineObs_graph);
            fw_graph.write("\n");

            fw_graph.close();

            //System.out.println(pred.size() + " == " + obs.size());
            //System.out.println(eval.stats());

            return;
        }

        int haha = 1;

        System.out.println(Nd4j.backend);
        System.out.println("ND4J Data Type Setting: " + Nd4j.dataType());


        //Nd4j.getMemoryManager().setAutoGcWindow(10000);             //Set to 10 seconds (10000ms) between System.gc() calls
        //Nd4j.getMemoryManager().togglePeriodicGc(false);            //Disable periodic GC calls

        //System.exit(1);

        BasicConfigurator.configure();

        RecordReader recordReader = new CSVRecordReader(0,",");
        RecordReader recordReader2 = new CSVRecordReader(0,",");
        RecordReader recordReader2_test = new CSVRecordReader(0,",");

        RecordReader rr_320 = new CSVRecordReader(0,",");
        RecordReader rr_320_test = new CSVRecordReader(0,",");

        ArrayList<INDArray> trainingSet = new ArrayList<>();
        ArrayList<INDArray> trainingSet_labels = new ArrayList<>();
        ArrayList<INDArray> testSet = new ArrayList<>();
        ArrayList<INDArray> testSet_labels = new ArrayList<>();

        ArrayList<INDArray> trainingSet_2 = new ArrayList<>();
        ArrayList<INDArray> trainingSet_2_labels = new ArrayList<>();
        ArrayList<INDArray> testSet_2 = new ArrayList<>();
        ArrayList<INDArray> testSet_2_labels = new ArrayList<>();

        if(aR.prepare_nn_input != 0) {

            //recordReader.initialize(new FileSplit(new File("/home/koomikko/Documents/iris.csv")));
            //recordReader2.initialize(new FileSplit(new File("/home/koomikko/Documents/species2.csv")));
            //recordReader2.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_train_s2.txt")));
            recordReader2.initialize(new FileSplit(aR.train));
            //recordReader2_test.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_test_s2.txt")));
            recordReader2_test.initialize(new FileSplit(aR.test));

            rr_320.initialize(new FileSplit(aR.train_2));
            rr_320_test.initialize(new FileSplit(aR.test_2));

            int labelIndex = 4;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
            int labelIndex2 = n_in;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
            int numClasses = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
            int numClasses2 = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
            //int batchSize = 150;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)
            int batchSize2 = batchSize;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)

            //batchSize2 = 1;

            //DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader2,batchSize2,labelIndex2,numClasses2);
            //DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2,batchSize2,labelIndex2,numClasses2);
            DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2, batchSize2, labelIndex2, numClasses2);
            DataSetIterator trainIter_2 = new RecordReaderDataSetIterator(rr_320, batchSize2, n_in_tex, numClasses2);
            DataSetIterator testIter = new RecordReaderDataSetIterator(recordReader2_test, batchSize2, labelIndex2, numClasses2);
            DataSetIterator testIter_2 = new RecordReaderDataSetIterator(rr_320_test, batchSize2, n_in_tex, numClasses2);

            //DataSetIterator ds = new INDArrayDataSetIterator()



            //INDArray train_features = Nd4j.create(1,1,1);

            //JointMultiDataSetIterator jointIter = new JointMultiDataSetIterator(trainIter, testIter);

            DataNormalization normalizer = new NormalizerStandardize();
            //DataNormalization normalizer = new NormalizerMinMaxScaler();
            DataNormalization normalizer_2 = new NormalizerStandardize();
            //DataNormalization normalizer_2 = new NormalizerMinMaxScaler();

            //DataNormalization normalizer = new NormalizerMinMaxScaler(0,1);

            normalizer.fit(trainIter);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data
            normalizer_2.fit(trainIter_2);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data


            trainIter.setPreProcessor(normalizer);
            trainIter_2.setPreProcessor(normalizer_2);
            testIter.setPreProcessor(normalizer);
            testIter_2.setPreProcessor(normalizer_2);

            int counti1 = 0;
            int counti2 = 0;


            while (trainIter.hasNext()) {
                DataSet ds = trainIter.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());


                //trainingSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //trainingSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                trainingSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth, _y, _x));
                //trainingSet.add(ds.getFeatures().reshape(1, 1, 2, _y, _x));


                //System.out.println(ds.getFeatures());
                //System.out.println(trainingSet.get(0));
                //System.exit(1);

                //counti1++;
                //System.out.println(trainingSet.get(trainingSet.size()-1).shapeInfoToString());

                //System.out.println(counti1);
                trainingSet_labels.add(ds.getLabels());

            }

            while (trainIter_2.hasNext()) {
                DataSet ds = trainIter_2.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());

                //trainingSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //trainingSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth_tex, _y_tex, _x_tex));
                //trainingSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth_tex, _y_tex, _x_tex));

                trainingSet_2.add(ds.getFeatures());

                //trainingSet_2.add(ds.getFeatures());
                trainingSet_2_labels.add(ds.getLabels());
                counti2++;
                //System.out.println(counti2);
            }


        /*

        System.out.println(trainingSet_2.get(0));
        try
        {
            FileOutputStream fos = new FileOutputStream("testList.lis");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(trainingSet_2);
            oos.close();
            fos.close();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }

        trainingSet_2.clear();
        try
        {
            FileInputStream fis = new FileInputStream("testList.lis");
            ObjectInputStream ois = new ObjectInputStream(fis);

            trainingSet_2 = (ArrayList) ois.readObject();

            ois.close();
            fis.close();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
            return;
        }

        System.out.println(trainingSet_2.get(0));

        System.exit(1);
        //System.out.println(counti1 + " == " + counti2);
        //System.exit(1);
        */
            while (testIter.hasNext()) {

                DataSet ds = testIter.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());

                //testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth, _y, _x));
                testSet_labels.add(ds.getLabels());

            }

            while (testIter_2.hasNext()) {

                DataSet ds = testIter_2.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());

                //testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //testSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth_tex, _y_tex, _x_tex));

                //testSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth_tex, _y_tex, _x_tex));

                testSet_2.add(ds.getFeatures());
                //testSet_2.add(ds.getFeatures());
                testSet_2_labels.add(ds.getLabels());

            }

            String fileExtension = ".mix_nnIn" + batchSize;

            ArrayList<INDArray>[] test_out = new ArrayList[2];

            test_out[0] = testSet;
            test_out[1] = testSet_labels;

            ArrayList<INDArray>[] test_out2 = new ArrayList[2];

            test_out2[0] = testSet_2;
            test_out2[1] = testSet_2_labels;


            ArrayList<INDArray>[] train_out = new ArrayList[2];

            train_out[0] = trainingSet;
            train_out[1] = trainingSet_labels;

            ArrayList<INDArray>[] train_out2 = new ArrayList[2];

            train_out2[0] = trainingSet_2;
            train_out2[1] = trainingSet_2_labels;

            if (aR.prepare_nn_input == 1) {


                File train_out_f = changeExtension(aR.train, fileExtension);
                File train_out2_f = changeExtension(aR.train_2, fileExtension);
                File test_out_f = changeExtension(aR.test, fileExtension);
                File test_out2_f = changeExtension(aR.test_2, fileExtension);

                try {

                    FileOutputStream fos1 = new FileOutputStream(train_out_f);
                    FileOutputStream fos2 = new FileOutputStream(train_out2_f);
                    FileOutputStream fos3 = new FileOutputStream(test_out_f);
                    FileOutputStream fos4 = new FileOutputStream(test_out2_f);

                    ObjectOutputStream oos1 = new ObjectOutputStream(fos1);
                    ObjectOutputStream oos2 = new ObjectOutputStream(fos2);
                    ObjectOutputStream oos3 = new ObjectOutputStream(fos3);
                    ObjectOutputStream oos4 = new ObjectOutputStream(fos4);

                    oos1.writeObject(train_out);
                    oos1.close();
                    fos1.close();

                    oos2.writeObject(train_out2);
                    oos2.close();
                    fos2.close();

                    oos3.writeObject(test_out);
                    oos3.close();
                    fos3.close();

                    oos4.writeObject(test_out2);
                    oos4.close();
                    fos4.close();


                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

/*
            try
            {
                FileInputStream fis = new FileInputStream(train_out_f );
                ObjectInputStream ois = new ObjectInputStream(fis);

                ArrayList<INDArray> hahaa[] = null;

                hahaa = (ArrayList<INDArray>[]) ois.readObject();

                System.out.println(hahaa[1]);
                ois.close();
                fis.close();
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
                return;
            }
*/
                System.exit(1);

            }
            if (aR.prepare_nn_input == 2) {


                File train_out_f = changeExtension(aR.train, fileExtension);
                File train_out2_f = changeExtension(aR.train_2, fileExtension);
                File test_out_f = changeExtension(aR.test, fileExtension);
                File test_out2_f = changeExtension(aR.test_2, fileExtension);

                try {

                    FileOutputStream fos3 = new FileOutputStream(test_out_f);
                    FileOutputStream fos4 = new FileOutputStream(test_out2_f);

                    ObjectOutputStream oos3 = new ObjectOutputStream(fos3);
                    ObjectOutputStream oos4 = new ObjectOutputStream(fos4);


                    oos3.writeObject(test_out);
                    oos3.close();
                    fos3.close();

                    oos4.writeObject(test_out2);
                    oos4.close();
                    fos4.close();


                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

                System.exit(1);

            }


            trainIter.reset();
            testIter.reset();

            testIter_2.reset();
            trainIter_2.reset();

            if(false){

                while(trainIter.hasNext()){

                    //normalizer.transform(trainIter.next());
                }
                while(testIter.hasNext()){

                    //normalizer.transform(testIter.next());
                }
            }

        }
        else{

            System.out.println("READING NNIN!!!");
            System.out.println("READING NNIN!!!");
            System.out.println("READING NNIN!!!");
            try
            {
                FileInputStream fis1 = new FileInputStream(aR.train);
                FileInputStream fis2 = new FileInputStream(aR.train_2);
                FileInputStream fis3 = new FileInputStream(aR.test);
                FileInputStream fis4 = new FileInputStream(aR.test_2);
                ObjectInputStream ois1 = new ObjectInputStream(fis1);
                ObjectInputStream ois2 = new ObjectInputStream(fis2);
                ObjectInputStream ois3 = new ObjectInputStream(fis3);
                ObjectInputStream ois4 = new ObjectInputStream(fis4);

                ArrayList<INDArray>[] train_in = null;
                ArrayList<INDArray>[] train_in_2 = null;
                ArrayList<INDArray>[] test_in = null;
                ArrayList<INDArray>[] test_in_2 = null;

                train_in = (ArrayList<INDArray>[]) ois1.readObject();
                train_in_2 = (ArrayList<INDArray>[]) ois2.readObject();
                test_in = (ArrayList<INDArray>[]) ois3.readObject();
                test_in_2 = (ArrayList<INDArray>[]) ois4.readObject();

                ois1.close();
                ois2.close();
                ois3.close();
                ois4.close();

                fis1.close();
                fis2.close();
                fis3.close();
                fis4.close();

                trainingSet = train_in[0];
                trainingSet_labels = train_in[1];

                trainingSet_2 = train_in_2[0];
                trainingSet_2_labels = train_in_2[1];

                testSet = test_in[0];
                testSet_labels = test_in[1];

                testSet_2 = test_in_2[0];
                testSet_2_labels = test_in_2[1];

            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
                return;
            }

            System.out.println("DONE READING NNIN!!");
            System.out.println("DONE READING NNIN!!");
            System.out.println("DONE READING NNIN!!");

        }

        //FeedForwardToCnn3DPreProcessor prepros = new FeedForwardToCnn3DPreProcessor(1, 10, 4, 4, true);

        /* THIS SEEMS OK FOR 4x4x10 VOXELS!!  */
/*
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .updater(new Adam())
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .weightInit(WeightInit.XAVIER)
                .list()

                .layer(new Convolution3D.Builder().kernelSize(3).convolutionMode(ConvolutionMode.Same)
                        .nIn(1).nOut(1).name("zero").build())
                .layer(new DenseLayer.Builder().nIn(160).nOut(37).name("two").build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .name("output")
                        .nIn(37)
                        .nOut(3)
                        .activation(Activation.SOFTMAX).build())
                //.setInputType(InputType.convolutional3D(Convolution3D.DataFormat.NCDHW ,10,8,8, 10))

                .setInputType(InputType.feedForward(160))
                .inputPreProcessor(0, new FeedForwardToCnn3DPreProcessor(10, 4, 4, 1, true))
                .build();


*/

        /*
        MultiLayerConfiguration conf_320 = new NeuralNetConfiguration.Builder()
                .activation(Activation.SIGMOID)
                .weightInit(WeightInit.RELU)
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Sgd(aR.learning_rate))
                //.updater(new Nesterovs(aR.learning_rate))
                //.l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(160).nOut(25)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(25).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();

        MultiLayerConfiguration configuration2 = new NeuralNetConfiguration.Builder()
                .updater(new Adam(0.001))
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .activation(Activation.RELU)
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(new Convolution2D.Builder().kernelSize(3,3).stride(1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(depth).nOut(depth).build())
                //.layer(new DropoutLayer(0.5))
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .name("output")
                        .nIn(n_in)
                        .nOut(3)
                        .activation(Activation.SOFTMAX).build())
                //.setInputType(InpubtType.feedForward(n_in))

                .setInputType(InputType.convolutional(_x, _y, depth))
                //.setInputType(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, 1, _y, _x, depth))
                //.inputPreProcessor(1, new Cnn3DToFeedForwardPreProcessor(1, 10, 4, 4, false))
                //.inputPreProcessor(0, new FeedForwardToCnn3DPreProcessor(depth, _y, _x, 1, true))

                .build();
*/
        /* 32x32x10 */
/*
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .updater(new Adam(0.001))
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .activation(Activation.RELU)
                .weightInit(WeightInit.RELU)
                .list()
                .layer(new Convolution2D.Builder().kernelSize(7,7).stride(1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(depth).nOut(depth).build())
                .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.AVG).kernelSize(3,3).stride(3,3).build())
                .layer(new Convolution2D.Builder().kernelSize(5,5).stride(1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(depth).nOut(depth).build())
                .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.AVG).kernelSize(2,2).stride(2,2).build())
                //.layer(new DenseLayer.Builder().nIn(n_in).nOut(7).name("two").build())
                .layer(new Convolution2D.Builder().kernelSize(5,5).stride(1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(depth).nOut(depth).build())
                //.layer(new DropoutLayer(0.5))
                .layer(new DenseLayer.Builder().nIn(250).nOut(160).build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .name("output")
                        .nIn(160)
                        .nOut(3)
                        .activation(Activation.SOFTMAX).build())
                //.setInputType(InputType.feedForward(n_in))

                .setInputType(InputType.convolutional(_x, _y, depth))
                //.setInputType(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, 1, _y, _x, depth))
                //.inputPreProcessor(1, new Cnn3DToFeedForwardPreProcessor(1, 10, 4, 4, false))
                //.inputPreProcessor(0, new FeedForwardToCnn3DPreProcessor(depth, _y, _x, 1, true))

                .build();
*/
        /* 16x16x10 */
/*
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .updater(new Adam(0.0001))
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .activation(Activation.RELU)
                .weightInit(WeightInit.RELU)
                .list()
                .layer(new Convolution2D.Builder().kernelSize(5,5).stride(1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(depth).nOut(depth).build())
                .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.AVG).kernelSize(3,3).stride(3,3).build())
                .layer(new Convolution2D.Builder().kernelSize(3,3).stride(1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(depth).nOut(depth).build())
                //.layer(new DenseLayer.Builder().nIn(n_in).nOut(7).name("two").build())
                .layer(new Convolution2D.Builder().kernelSize(3,3).stride(1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(depth).nOut(depth).build())
                //.layer(new DropoutLayer(0.5))
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .name("output")
                        .nIn(250)
                        .nOut(3)
                        .activation(Activation.SOFTMAX).build())
                //.setInputType(InputType.feedForward(n_in))

                .setInputType(InputType.convolutional(_x, _y, depth))
                //.setInputType(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, 1, _y, _x, depth))
                //.inputPreProcessor(1, new Cnn3DToFeedForwardPreProcessor(1, 10, 4, 4, false))
                //.inputPreProcessor(0, new FeedForwardToCnn3DPreProcessor(depth, _y, _x, 1, true))

                .build();
*/

/*
        MultiLayerConfiguration configuration2 = new NeuralNetConfiguration.Builder()
                .updater(new Adam(0.00001))
                .optimizationAlgo(OptimizationAlgDenseLayer.Builder().nIn(3).nOut(4).build(), "input1")
                .addLayer( "L2",new Convolution3D.Builder().kernelSize(2,2,2).stride(1,1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(1).nOut(1).name("zero").build(),"input2")
                .addVertex("merge", new Mergeorithm.STOCHASTIC_GRADIENT_DESCENT)
                .weightInit(WeightInit.XAVIER)
                .list()

                .layer(new Convolution3D.Builder().kernelSize(11,11,11).stride(1,1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(1).nOut(1).name("zero").build())
                .layer(new Convolution3D.Builder().kernelSize(7,7,7).stride(1,1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(1).nOut(1).name("zero_1").build())
                .layer(new Convolution3D.Builder().kernelSize(5,5,5).stride(1,1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(1).nOut(1).name("zero_2").build())
                .layer(new Convolution3D.Builder().kernelSize(3,3,3).stride(1,1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(1).nOut(1).name("zero_2").build())

                .layer(new DenseLayer.Builder().nIn(n_in).nOut(66).name("two").build())

                //.layer(new DropoutLayer(0.5))
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .name("output")
                        .nIn(66)
                        .nOut(3)
                        .activation(Activation.SOFTMAX).build())
                //.setInputType(InputType.feedForward(n_in))
                .setInputType(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth, _y, _x, 1))
                //.inputPreProcessor(1, new Cnn3DToFeedForwardPreProcessor(1, 10, 4, 4, false))
                //.inputPreProcessor(0, new FeedForwardToCnn3DPreProcessor(depth, _y, _x, 1, true))

                .build();

*/


/*
        ComputationGraphConfiguration conf2 = new NeuralNetConfiguration.Builder()
                .updater(new Adam(0.0001))
                .activation(Activation.RELU)
                .weightInit(WeightInit.RELU)
                .graphBuilder()
                .addInputs("input1", "input2")
                // THESE ARE THE TEXTURES!!!
                .addLayer("L1", new DenseLayer.Builder().nIn(160).nOut(25).build(), "input1")
                .addLayer( "L2",new Convolution2D.Builder().kernelSize(3,3).stride(1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(depth).nOut(depth).name("zero").build(),"input2")
                //.addLayer("conv_dense", new DenseLayer.Builder().nIn(4*4*10).nOut(25).build(),"L222")
                //.addLayer("conv_dense", new DenseLayer.Builder().nIn(32*32*10).nOut(250).build(),"L222")
                //.addLayer("conv_dense", new DenseLayer.Builder().nIn(250).nOut(25).build(),"L222")
                .addLayer("conv_dense", new DenseLayer.Builder().nIn(n_in).nOut(25).build(),"L2")
                .addVertex("merge", new MergeVertex(), "L1", "conv_dense")
                .addLayer("out", new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE).nIn(25 + 25).nOut(3).activation(Activation.SOFTMAX).build(), "merge")
                .setInputTypes(InputType.feedForward(160), InputType.convolutional(_x, _y, depth))
                .setOutputs("out")
                .build();
*/
        /* 32x32x10 voxels!! */

/*
        ComputationGraphConfiguration conf123 = new NeuralNetConfiguration.Builder()
                .updater(new Adam(0.0001))
                .activation(Activation.RELU)
                .weightInit(WeightInit.RELU)
                .graphBuilder()
                .addInputs("input1", "input2")
                // THESE ARE THE TEXTURES!!!
                .addLayer("L1", new DenseLayer.Builder().nIn(160).nOut(25).build(), "input1")
                .addLayer( "L2",new Convolution2D.Builder().kernelSize(11,11).stride(1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(depth).nOut(depth).build(),"input2")
                .addLayer( "pool_1", new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.AVG).kernelSize(3,3).stride(3,3).build(), "L2" )
                .addLayer( "L22",new Convolution2D.Builder().kernelSize(5,5).stride(1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(depth).nOut(depth).build(),"pool_1")
                .addLayer( "pool_2", new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.AVG).kernelSize(2,2).stride(2,2).build(), "L22" )
                .addLayer( "L222",new Convolution2D.Builder().kernelSize(5,5).stride(1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(depth).nOut(depth).build(),"pool_2")
                //.addLayer("conv_dense", new DenseLayer.Builder().nIn(4*4*10).nOut(25).build(),"L222")
                //.addLayer("conv_dense", new DenseLayer.Builder().nIn(32*32*10).nOut(250).build(),"L222")
                //.addLayer("conv_dense", new DenseLayer.Builder().nIn(250).nOut(25).build(),"L222")
                .addLayer("conv_dense", new DenseLayer.Builder().nIn(250).nOut(25).build(),"L222")
                .addVertex("merge", new MergeVertex(), "L1", "conv_dense")
                .addLayer("out", new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE).nIn(25 + 25).nOut(3).activation(Activation.SOFTMAX).build(), "merge")
                .setInputTypes(InputType.feedForward(160), InputType.convolutional(_x, _y, depth))
                .setOutputs("out")
                .build();
*/
        ComputationGraphConfiguration conf_merge = null;
        ComputationGraphConfiguration conf_vox = null;
        ComputationGraphConfiguration conf_tex = null;

        boolean debuggi = false;

        //n_epoch = 25;

        /* Arbiter merged convolution! */

        if(aR.convolution_option == 1 || debuggi) {

            /*
            conf_merge = new NeuralNetConfiguration.Builder()
                    .updater(new Adam(0.01))
                    .activation(Activation.TANH)
                    .weightInit(WeightInit.XAVIER).cacheMode(CacheMode.DEVICE).dropOut(0.75)
                    //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .graphBuilder()
                    .addInputs("input1", "input2")
                    // THESE ARE THE TEXTURES!!!
                    .addLayer("texture_convolution_1", new Convolution3D.Builder().kernelSize(5, 3, 3).stride(2, 1, 1).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "input1")
                    .addLayer("texture_convolution_2", new Convolution3D.Builder().kernelSize(3, 3, 3).stride(2, 2, 2).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "texture_convolution_1")
                    .addLayer("texture_convolution_3", new Convolution3D.Builder().kernelSize(3, 3, 3).stride(1, 2, 2).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "texture_convolution_2")
                    .addLayer("texture_convolution_4", new Convolution3D.Builder().kernelSize(3, 1, 1).stride(1, 1, 1).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "texture_convolution_3")
                    //.addLayer("dropout1", new DropoutLayer.Builder().dropOut(0.85).build(), "texture_convolution_1")

                    .addLayer("voxel_convolution_1", new Convolution3D.Builder().kernelSize(1, 1, 1).stride(1, 1, 1).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "input2")
                    .addLayer("voxel_convolution_2", new Convolution3D.Builder().kernelSize(2, 1, 1).stride(2, 1, 1).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "voxel_convolution_1")


                    .addLayer("conv_dense_texture", new DenseLayer.Builder().hasBias(true).nOut(11).build(), "texture_convolution_4")
                    .addLayer("conv_dense_voxel", new DenseLayer.Builder().hasBias(true).nOut(7).build(), "voxel_convolution_2")
                    .addVertex("merge", new MergeVertex(), "conv_dense_texture", "conv_dense_voxel")
                    .addLayer("dense1", new DenseLayer.Builder().hasBias(true).nIn(18).nOut(9).build(), "merge")

                    .addLayer("out", new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).nIn(9).nOut(3).activation(Activation.SOFTMAX).build(), "dense1")
                    //.setInputTypes(InputType.convolutional(_x_tex, _y_tex, depth_tex), InputType.convolutional(_x, _y, depth))
                    .setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth_tex, _y_tex, _x_tex, 1),
                            InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth, _y, _x, 1))
                    .setOutputs("out")
                    .build();



             */


            /* OPTIMAL MERGED 2D CONVOLUTION */


            conf_merge = new NeuralNetConfiguration.Builder()
                    .activation(Activation.RELU)
                    .weightInit(WeightInit.RELU)
                    .updater(new Adam(0.02170786958290351))
                    .l2(0.00831709149403009)
                    .graphBuilder()
                    .addInputs("input1", "input2")
                    .addLayer( "convo1", new ConvolutionLayer.Builder().nIn(depth_tex).nOut(depth_tex).kernelSize(3,7).stride(3, 1).convolutionMode(ConvolutionMode.Same).build(), "input1")
                    .addLayer( "convo2", new ConvolutionLayer.Builder().nIn(depth_tex).nOut(depth_tex).kernelSize(5, 1).stride(2, 2).convolutionMode(ConvolutionMode.Same).build(), "convo1")
                    .addLayer("dense1", new DenseLayer.Builder().nOut(254).build(), "convo2")
                    .addLayer( "vox1", new ConvolutionLayer.Builder().nIn(depth).nOut(depth).kernelSize(1, 1).stride(1, 1).convolutionMode(ConvolutionMode.Same).build(), "input2")
                    .addLayer( "vox2", new ConvolutionLayer.Builder().nIn(depth).nOut(depth).kernelSize(3, 1).stride(1, 1).convolutionMode(ConvolutionMode.Same).build(), "vox1")
                    .addLayer("dense2", new DenseLayer.Builder().nOut(155).build(), "vox2")
                    .addVertex("merge", new MergeVertex(), "dense1", "dense2")
                    .addLayer("dense3", new DenseLayer.Builder().hasBias(true).nOut(189).build(), "merge")

                    .addLayer("out", new OutputLayer.Builder().nOut(3).activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).build(), "dense3")
                    //.setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth_tex, _y_tex, _x_tex, 1),
                    //      InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth, _y, _x, 1))
                    .setInputTypes(InputType.convolutional(_y_tex, _x_tex, depth_tex),
                            InputType.convolutional(_y, _x, depth))
                    .setOutputs("out")
                    .build();

        }

        /* MIXED NETWORK, optimized with arbiter */
        if(aR.convolution_option == 2 || debuggi) {

            conf_merge = new NeuralNetConfiguration.Builder()
                    .activation(Activation.RELU)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new Adam(7.302006829710073E-4))
                    .l2(0.0044177129597666245)
                    .graphBuilder()
                    .addInputs("input1", "input2")
                    .addLayer("de_1", new DenseLayer.Builder().nIn(160).nOut(48).build(), "input1")
                    .addLayer("de_2", new DenseLayer.Builder().nOut(207).build(), "de_1")
                    //.addLayer("dense1", new DenseLayerSpace.Builder().nOut(layerSizeHyperparam_texture_dense1).build(), "convo2")
                    .addLayer( "vox1", new ConvolutionLayer.Builder().nIn(depth).nOut(depth).kernelSize( 3, 1).stride(1,1).convolutionMode(ConvolutionMode.Same).build(), "input2")
                    .addLayer( "vox2", new ConvolutionLayer.Builder().nIn(depth).nOut(depth).kernelSize(1, 3 ).stride(1,1).convolutionMode(ConvolutionMode.Same).build(), "vox1")
                    .addLayer("dense2", new DenseLayer.Builder().nOut(113).build(), "vox2")
                    .addVertex("merge", new MergeVertex(), "de_2", "dense2")
                    .addLayer("dense3", new DenseLayer.Builder().hasBias(true).nOut(87).build(), "merge")

                    .addLayer("out", new OutputLayer.Builder().nOut(3).activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).build(), "dense3")
                    //.setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth_tex, _y_tex, _x_tex, 1),
                    //      InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth, _y, _x, 1))
                    .setInputTypes(InputType.feedForward(160),
                            InputType.convolutional(_y, _x, depth))
                    .setOutputs("out")
                    .build();
        }

        /* texture optimized with arbiter */
        if(aR.convolution_option == 3 || debuggi) {

            conf_merge = new NeuralNetConfiguration.Builder()
                    .activation(Activation.TANH)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new Adam(0.0014059330133638808))
                    .l2(0.004848207017291663)
                    .graphBuilder()
                    .addInputs("input1")
                    .addLayer( "convo1", new ConvolutionLayer.Builder().nIn(depth_tex).nOut(depth_tex).kernelSize( 3, 5 ).stride(2, 1 ).convolutionMode(ConvolutionMode.Same).build(), "input1")
                    .addLayer( "convo2", new ConvolutionLayer.Builder().nIn(depth_tex).nOut(depth_tex).kernelSize(5, 1 ).stride( 1, 1 ).convolutionMode(ConvolutionMode.Same).build(), "convo1")

                    .addLayer("out", new OutputLayer.Builder().nOut(3).activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).build(), "dense1")
                    //.setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth_tex, _y_tex, _x_tex, 1),
                    //      InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth, _y, _x, 1))
                    .setInputTypes(InputType.convolutional(_y_tex, _x_tex, depth_tex))
                    .setOutputs("out")
                    .build();

        }

        /* texture optimized with arbiter */

        if(aR.convolution_option == 4 || debuggi) {
/*
            conf_merge = new NeuralNetConfiguration.Builder()
                    .updater(new Adam(0.01))
                    .activation(Activation.TANH)
                    .weightInit(WeightInit.XAVIER).cacheMode(CacheMode.DEVICE).dropOut(0.75)
                    //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .graphBuilder()
                    .addInputs("input1")
                    // THESE ARE THE TEXTURES!!!
                    .addLayer("texture_convolution_1", new Convolution3D.Builder().kernelSize(4, 4, 4).stride(2, 2, 2).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "input1")
                    .addLayer("texture_convolution_2", new Convolution3D.Builder().kernelSize(3, 3, 3).stride(2, 2, 2).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "texture_convolution_1")
                    .addLayer("texture_convolution_3", new Convolution3D.Builder().kernelSize(3, 3, 3).stride(1, 2, 2).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "texture_convolution_2")
                    .addLayer("texture_convolution_4", new Convolution3D.Builder().kernelSize(2, 2, 2).stride(2, 2, 2).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "texture_convolution_3")
                    //.addLayer("dropout1", new DropoutLayer.Builder().dropOut(0.85).build(), "texture_convolution_1")

                    //.addLayer("merge", new DenseLayer.Builder().hasBias(true).nOut(11).build(), "texture_convolution_4")
                    .addLayer("dense1", new DenseLayer.Builder().hasBias(true).nOut(11).build(), "texture_convolution_4")

                    .addLayer("out", new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).nIn(11).nOut(3).activation(Activation.SOFTMAX).build(), "dense1")
                    //.setInputTypes(InputType.convolutional(_x_tex, _y_tex, depth_tex), InputType.convolutional(_x, _y, depth))
                    .setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth_tex, _y_tex, _x_tex, 1))
                    .setOutputs("out")
                    .build();
*/

            conf_merge = new NeuralNetConfiguration.Builder()
                    .activation(Activation.TANH)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new Adam(0.0014059330133638808))
                    .l2(0.004848207017291663)
                    .graphBuilder()
                    .addInputs("input1")
                    .addLayer( "convo1", new ConvolutionLayer.Builder().nIn(depth_tex).nOut(depth_tex).kernelSize( 3, 5 ).stride(2, 1 ).convolutionMode(ConvolutionMode.Same).build(), "input1")
                    .addLayer( "convo2", new ConvolutionLayer.Builder().nIn(depth_tex).nOut(depth_tex).kernelSize(5, 1 ).stride( 1, 1 ).convolutionMode(ConvolutionMode.Same).build(), "convo1")
                    .addLayer("dense1", new DenseLayer.Builder().hasBias(true).nOut(178).build(), "convo2")
                    .addLayer("out", new OutputLayer.Builder().nOut(3).activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).build(), "dense1")
                    //.setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth_tex, _y_tex, _x_tex, 1),
                    //      InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth, _y, _x, 1))
                    .setInputTypes(InputType.convolutional(_y_tex, _x_tex, depth_tex))
                    .setOutputs("out")
                    .build();


        }

        if(aR.convolution_option == 5 || debuggi) {

            conf_merge = new NeuralNetConfiguration.Builder()
                    .updater(new Adam(0.001))
                    .activation(Activation.TANH)
                    .weightInit(WeightInit.XAVIER).cacheMode(CacheMode.DEVICE).dropOut(0.9)
                    //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .graphBuilder()
                    .addInputs("input1")
                    // THESE ARE THE TEXTURES!!!
                    .addLayer("texture_convolution_1", new Convolution3D.Builder().kernelSize(5, 3, 3).stride(2, 2, 2).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "input1")
                    .addLayer("texture_convolution_2", new Convolution3D.Builder().kernelSize(3, 3, 3).stride(2, 2, 2).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "texture_convolution_1")
                    .addLayer("texture_convolution_3", new Convolution3D.Builder().kernelSize(3, 3, 3).stride(2, 2, 2).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "texture_convolution_2")

                    .addLayer("merge", new DenseLayer.Builder().hasBias(true).nIn(125).nOut(27).build(), "texture_convolution_3")
                    .addLayer("dense1", new DenseLayer.Builder().hasBias(true).nIn(27).nOut(9).activation(Activation.RELU).build(), "merge")

                    .addLayer("out", new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).nIn(9).nOut(3).activation(Activation.SOFTMAX).build(), "dense1")
                    //.setInputTypes(InputType.convolutional(_x_tex, _y_tex, depth_tex), InputType.convolutional(_x, _y, depth))
                    .setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth_tex, _y_tex, _x_tex, 1))
                    .setOutputs("out")
                    .build();

        }

        if(aR.convolution_option == 6 || debuggi) {

            conf_merge = new NeuralNetConfiguration.Builder()
                    .updater(new Adam(0.001))
                    .activation(Activation.TANH)
                    .weightInit(WeightInit.XAVIER).cacheMode(CacheMode.DEVICE).dropOut(0.9)
                    //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .graphBuilder()
                    .addInputs("input1")
                    // THESE ARE THE TEXTURES!!!
                    .addLayer("texture_convolution_1", new Convolution3D.Builder().kernelSize(15, 3, 3).stride(2, 2, 2).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "input1")
                    .addLayer("texture_convolution_2", new Convolution3D.Builder().kernelSize(7, 3, 3).stride(2, 2, 2).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "texture_convolution_1")
                    .addLayer("texture_convolution_3", new Convolution3D.Builder().kernelSize(3, 3, 3).stride(2, 2, 2).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "texture_convolution_2")

                    .addLayer("merge", new DenseLayer.Builder().hasBias(true).nIn(125).nOut(25).build(), "texture_convolution_3")
                    .addLayer("dense1", new DenseLayer.Builder().hasBias(true).nIn(25).nOut(9).activation(Activation.RELU).build(), "merge")

                    .addLayer("out", new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).nIn(9).nOut(3).activation(Activation.SOFTMAX).build(), "dense1")
                    //.setInputTypes(InputType.convolutional(_x_tex, _y_tex, depth_tex), InputType.convolutional(_x, _y, depth))
                    .setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth_tex, _y_tex, _x_tex, 1))
                    .setOutputs("out")
                    .build();

        }

        if(aR.convolution_option == 7 || debuggi) {
/*
            conf_merge = new NeuralNetConfiguration.Builder()
                    .updater(new Adam(0.01))
                    .activation(Activation.TANH)
                    .weightInit(WeightInit.XAVIER).cacheMode(CacheMode.DEVICE).dropOut(0.75)
                    //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .graphBuilder()
                    .addInputs("input1")
                    // THESE ARE THE TEXTURES!!!
                    .addLayer("voxel_convolution_1", new Convolution3D.Builder().kernelSize(1, 2, 2).stride(1, 1, 1).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "input1")
                    .addLayer("voxel_convolution_2", new Convolution3D.Builder().kernelSize(2, 2, 2).stride(2, 1, 1).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "voxel_convolution_1")

                    //.addLayer("merge", new DenseLayer.Builder().hasBias(true).nOut(13).build(), "voxel_convolution_2")
                    .addLayer("dense1", new DenseLayer.Builder().hasBias(true).nOut(7).build(), "voxel_convolution_2")

                    .addLayer("out", new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).nIn(7).nOut(3).activation(Activation.SOFTMAX).build(), "dense1")
                    //.setInputTypes(InputType.convolutional(_x_tex, _y_tex, depth_tex), InputType.convolutional(_x, _y, depth))
                    .setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth, _y, _x, 1))
                    .setOutputs("out")
                    .build();
*/
            conf_merge = new NeuralNetConfiguration.Builder()
                    .activation(Activation.TANH)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new Adam(0.009070895181695285))
                    .l2(0.008703516392653547)
                    .graphBuilder()
                    .addInputs("input2")
                    .addLayer( "vox1", new ConvolutionLayer.Builder().nIn(depth).nOut(depth).kernelSize(3, 1 ).stride(1, 1).convolutionMode(ConvolutionMode.Same).build(), "input2")
                    .addLayer( "vox2", new ConvolutionLayer.Builder().nIn(depth).nOut(depth).kernelSize(1, 1).stride(1, 1).convolutionMode(ConvolutionMode.Same).build(), "vox1")
                    .addLayer("dense2", new DenseLayer.Builder().nOut(111).build(), "vox2")
                    .addLayer("out", new OutputLayer.Builder().nOut(3).activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).build(), "dense2")
                    //.setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth_tex, _y_tex, _x_tex, 1),
                    //      InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth, _y, _x, 1))
                    .setInputTypes(
                            InputType.convolutional(_y, _x, depth))
                    .setOutputs("out")
                    .build();
        }

        if(aR.convolution_option == 8 || debuggi) {

            conf_merge = new NeuralNetConfiguration.Builder()
                    .updater(new Adam(0.001))
                    .activation(Activation.TANH)
                    .weightInit(WeightInit.XAVIER).cacheMode(CacheMode.DEVICE).dropOut(0.9)
                    //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .graphBuilder()
                    .addInputs("input1")
                    // THESE ARE THE TEXTURES!!!
                    .addLayer("voxel_convolution_1", new Convolution3D.Builder().kernelSize(1, 2, 2).stride(1, 1, 1).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "input1")
                    .addLayer("voxel_convolution_2", new Convolution3D.Builder().kernelSize(2, 2, 2).stride(2, 1, 1).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "voxel_convolution_1")

                    .addLayer("merge", new DenseLayer.Builder().hasBias(true).nIn(80).nOut(25).build(), "voxel_convolution_2")
                    .addLayer("dense1", new DenseLayer.Builder().hasBias(true).nIn(25).nOut(7).build(), "merge")

                    .addLayer("out", new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).nIn(7).nOut(3).activation(Activation.SOFTMAX).build(), "dense1")
                    //.setInputTypes(InputType.convolutional(_x_tex, _y_tex, depth_tex), InputType.convolutional(_x, _y, depth))
                    .setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth, _y, _x, 1))
                    .setOutputs("out")
                    .build();

        }

        if(aR.convolution_option == 9 || debuggi) {

            conf_merge = new NeuralNetConfiguration.Builder()
                    .updater(new Adam(0.001))
                    .activation(Activation.TANH)
                    .weightInit(WeightInit.XAVIER).cacheMode(CacheMode.DEVICE).dropOut(0.9)
                    //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .graphBuilder()
                    .addInputs("input1")
                    // THESE ARE THE TEXTURES!!!
                    .addLayer("voxel_convolution_1", new Convolution3D.Builder().kernelSize(1, 2, 2).stride(1, 1, 1).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "input1")
                    .addLayer("voxel_convolution_2", new Convolution3D.Builder().kernelSize(2, 2, 2).stride(2, 1, 1).convolutionMode(ConvolutionMode.Same)
                            .nIn(1).nOut(1).hasBias(true).build(), "voxel_convolution_1")

                    .addLayer("merge", new DenseLayer.Builder().hasBias(true).nIn(80).nOut(15).build(), "voxel_convolution_2")
                    .addLayer("dense1", new DenseLayer.Builder().hasBias(true).nIn(15).nOut(6).build(), "merge")

                    .addLayer("out", new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).nIn(6).nOut(3).activation(Activation.SOFTMAX).build(), "dense1")
                    //.setInputTypes(InputType.convolutional(_x_tex, _y_tex, depth_tex), InputType.convolutional(_x, _y, depth))
                    .setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth, _y, _x, 1))
                    .setOutputs("out")
                    .build();

        }


        /* 16x16x10 */
/*
        ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder()
                .updater(new Adam(0.0001))
                .activation(Activation.RELU)
                .weightInit(WeightInit.RELU)
                .graphBuilder()
                .addInputs("input1", "input2")
                // THESE ARE THE TEXTURES!!!
                .addLayer("L1", new DenseLayer.Builder().nIn(160).nOut(25).build(), "input1")
                .addLayer( "L2",new Convolution2D.Builder().kernelSize(5,5).stride(1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(depth).nOut(depth).name("zero").build(),"input2")
                .addLayer( "pool_1", new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.AVG).kernelSize(3,3).stride(3,3).build(), "L2" )
                .addLayer( "L22",new Convolution2D.Builder().kernelSize(3,3).stride(1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(depth).nOut(depth).name("zero").build(),"pool_1")
                .addLayer( "L222",new Convolution2D.Builder().kernelSize(3,3).stride(1,1).convolutionMode(ConvolutionMode.Same)
                        .nIn(depth).nOut(depth).name("zero").build(),"L22")
                //.addLayer("conv_dense", new DenseLayer.Builder().nIn(4*4*10).nOut(25).build(),"L222")
                //.addLayer("conv_dense", new DenseLayer.Builder().nIn(32*32*10).nOut(250).build(),"L222")
                //.addLayer("conv_dense", new DenseLayer.Builder().nIn(250).nOut(25).build(),"L222")
                .addLayer("conv_dense", new DenseLayer.Builder().nIn(250).nOut(25).build(),"L222")
                .addVertex("merge", new MergeVertex(), "L1", "conv_dense")
                .addLayer("out", new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE).nIn(25 + 25).nOut(3).activation(Activation.SOFTMAX).build(), "merge")
                .setInputTypes(InputType.feedForward(160), InputType.convolutional(_x, _y, depth))
                .setOutputs("out")
                .build();
*/

        //MultiLayerNetwork model = new MultiLayerNetwork(configuration);
        //MultiLayerNetwork model_320 = new MultiLayerNetwork(conf_320);

        ComputationGraph model_graph = new ComputationGraph(conf_merge);
        //ComputationGraph model_graph_vox = new ComputationGraph(conf_vox);
        //ComputationGraph model_graph_tex = new ComputationGraph(conf_tex);

        //model.setListeners(new PerformanceListener(1));       //Logs ETL and iteration speed on each iteration
        int listenerFrequency = 5;
        boolean reportScore = true;
        boolean reportGC = false;
        //model.setListeners(new PerformanceListener(listenerFrequency, reportScore, reportGC));
        model_graph.setListeners(new PerformanceListener(listenerFrequency, reportScore, reportGC));
        //model.setListeners(new ScoreIterationListener(100));
        //ComputationGraph graph = new ComputationGraph(conf);

        if(aR.odir.compareTo("asd") == 0)
            aR.odir = "";
/*
        EarlyStoppingConfiguration esConf = new EarlyStoppingConfiguration.Builder()
                //.iterationTerminationConditions(new MaxTimeIterationTerminationCondition(aR.time, TimeUnit.MINUTES))
                .epochTerminationConditions(new MaxEpochsTerminationCondition(n_epoch))
                .scoreCalculator(new DataSetLossCalculator(testIter, true))
                //.scoreCalculator(new DataSetLossCalculator(trainIter, true))
                //.scoreCalculator(new DataSetLossCalculator(trainIter, true))
                .evaluateEveryNEpochs(1)
                .modelSaver(new LocalFileModelSaver(aR.odir))
                .build();
*/
        //EarlyStoppingTrainer trainer = new EarlyStoppingTrainer(esConf,model,trainIter);

        //EvaluativeListener eva_lis_train = new EvaluativeListener(trainIter, 1, org.deeplearning4j.optimize.api.InvocationType.EPOCH_END);
        //EvaluativeListener eva_lis_test = new EvaluativeListener(testIter, 1, org.deeplearning4j.optimize.api.InvocationType.EPOCH_END);


        String out_train = "";
        String out_test = "";

        Evaluation  eval_vox = new Evaluation(3);
        Evaluation  eval_graph = new Evaluation(3);
        Evaluation  eval_graph_train = new Evaluation(3);
        Evaluation  eval_tex = new Evaluation(3);
        //ComputationGraph bestModel_vox = null;
        //ComputationGraph bestModel_tex = null;
        ComputationGraph bestModel_graph = null;


        //model_graph_tex.init();
        model_graph.init();
        //model_graph_vox.init();

        double bestAccuracy = Double.NEGATIVE_INFINITY;
        double bestAccuracy_graph = Double.NEGATIVE_INFINITY;
        double bestAccuracy_320 = Double.NEGATIVE_INFINITY;
        //MultiLayerNetwork bestModel = null;

        //testIter.reset();
        //testIter_2.reset();

        /*
        ParallelWrapper pw = new ParallelWrapper.Builder(model_graph)
                .prefetchBuffer(24)
                .workers(1)
                .averagingFrequency(3)
                .reportScoreAfterAveraging(true)

                .build();

        */

        double train_accuracy = 0.0;
        double valid_accuracy = 0.0;

        File outFile = new File(aR.output);

        if(!outFile.exists())
            outFile.createNewFile();

        FileWriter fw_graph = new FileWriter(outFile, true);

        String string_out_train = "";
        String string_out_valid = "";


        for(int i = 0; i < n_epoch; i++){


            MultiDataSet mds = new MultiDataSet();
            eval_graph.reset();
            eval_graph_train.reset();
            eval_vox.reset();
            eval_tex.reset();

            train_accuracy = 0.0;
            valid_accuracy = 0.0;


            for(int i_ = 0; i_ < trainingSet.size(); i_++){

                DataSet ds = new DataSet();
                DataSet ds2 = new DataSet();

                INDArray[] features = new INDArray[2];
                INDArray[] labels = new INDArray[1];

                ds2.setFeatures(trainingSet_2.get(i_));
                ds2.setLabels(trainingSet_2_labels.get(i_));

                ds.setFeatures(trainingSet.get(i_));
                ds.setLabels(trainingSet_labels.get(i_));

                features[0] = ds2.getFeatures();
                features[1] = ds.getFeatures();

                labels[0] = ds.getLabels();

                INDArray lablstwo = ds.getLabels();
                //labels[1] = ds.getLabels();

                mds = new MultiDataSet(features, labels, null, null);

                //System.out.println("CONVO:");
                //model.fit(ds);
                //model_320.fit(ds2);
                //System.out.println("GRAPH:");

                if(aR.convolution_option < 4){
                    model_graph.fit(mds);
                }
                else if(aR.convolution_option < 7){
                    model_graph.fit(ds2);
                }else{
                    model_graph.fit(ds);
                }

                INDArray[] predicted_ = null;
                INDArray predicted = null;

                if(aR.convolution_option < 4){
                    predicted_ = model_graph.output(false, features, null, null);
                }
                else if(aR.convolution_option < 7){
                    predicted_ = model_graph.output(trainingSet_2.get(i_));
                }else{
                    predicted_ = model_graph.output(trainingSet.get(i_));

                }

                eval_graph_train.eval(labels[0], predicted_[0]);

                train_accuracy += eval_graph_train.accuracy();

            }

            train_accuracy /= (double)trainingSet.size();

            for(int i_ = 0; i_ < testSet.size(); i_++){

                INDArray features = testSet.get(i_);
                INDArray labels = testSet_labels.get(i_);

                INDArray features_graph = testSet_2.get(i_);
                INDArray labels_graph = testSet_2_labels.get(i_);

                INDArray[] features_ = new INDArray[2];
                INDArray[] labels_ = new INDArray[1];

                features_[0] = features_graph;
                features_[1] = features;

                labels_[0] = labels;
                mds = new MultiDataSet(features, labels, null, null);


                //INDArray predicted = model.output(features,false);
                //INDArray predicted_320 = model_320.output(features_graph,false);

                //System.out.println("EVAL convo: ");
                //System.out.println("i_: " + i_);
                //System.out.println(" TEST SET SIZE: " + testSet.size());
                //System.out.println(features_[0].shapeInfoToString() + " " + features_[1].shapeInfoToString());
                INDArray[] predicted_ = null;
                INDArray predicted = null;

                if(aR.convolution_option < 4){
                   predicted_ = model_graph.output(false, features_, null, null);
                }
                else if(aR.convolution_option < 7){
                    predicted_ = model_graph.output(features_graph);
                }else{
                    predicted_ = model_graph.output(features);

                }


                //try {
                    //System.out.println("EVAL GRAPH:");
                //    eval.eval(labels, predicted);

                eval_graph.eval(labels_[0], predicted_[0]);
                    //eval_320.eval(labels, predicted_320);

                valid_accuracy += eval_graph.accuracy();


                //}catch (Exception e){
                    //e.printStackTrace();
                //}
            }

            valid_accuracy /= (double)testSet.size();

            //fw_graph.write(train_accuracy + " " + valid_accuracy + "\n");
            string_out_train += train_accuracy + "\t";
            string_out_valid += valid_accuracy + "\t";
            /*
            while(trainIter.hasNext()){
                DataSet ds = trainIter.next();
                ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1 ,10,4,4));
                System.out.println("RANK!!!!  " + ds.getFeatures().rank());
                model.fit(ds);
            }

            while(testIter.hasNext()){
                DataSet t = testIter.next();
                t.setFeatures(t.getFeatures().reshape(t.getLabels().length() / numClasses, 1 ,10,4,4));

                INDArray features = t.getFeatures();
                INDArray labels = t.getLabels();
                INDArray predicted = model.output(features,false);
                eval.eval(labels, predicted);

            }

             */
            long seed = System.nanoTime();
            Collections.shuffle(trainingSet, new Random(seed));
            Collections.shuffle(trainingSet_2, new Random(seed));
            Collections.shuffle(trainingSet_labels, new Random(seed));
            Collections.shuffle(trainingSet_2_labels, new Random(seed));

            try {


                if (eval_graph.accuracy() > bestAccuracy_graph) {
                    bestAccuracy_graph = eval_graph.accuracy();
                    bestModel_graph = model_graph.clone();
                }

                /*
                if (eval.accuracy() > bestAccuracy) {
                    bestAccuracy = eval.accuracy();
                    bestModel = model.clone();
                }
                */
/*
                if (eval_320.accuracy() > bestAccuracy_320) {
                    bestAccuracy_320 = eval_320.accuracy();
                    bestModel_320 = model_320.clone();
                }
*/
            }catch (Exception e) {
                e.printStackTrace();
            }

            //trainIter.reset();
        }

        fw_graph.write(string_out_train + "\n");
        fw_graph.write(string_out_valid + "\n");
        fw_graph.close();

        System.out.println("DONE TRAINING!!");

        //bestModel.save(new File("bestModel.bin"), true);
        bestModel_graph.save(new File(aR.save_file), true);
        //bestModel_320.save(new File("bestModel_320.bin"), true);

        eval_graph.reset();
        eval_tex.reset();
        eval_vox.reset();

        //bestModel = restoreMultiLayerNetwork(new File("bestModel.bin"));
        bestModel_graph = restoreComputationGraph(new File(aR.save_file));
        //bestModel_320 = restoreMultiLayerNetwork(new File("bestModel_320.bin"));


        eval_vox = new Evaluation(3);
        //testIter.reset();



        for(int i_ = 0; i_ < testSet.size(); i_++){

            INDArray features = testSet.get(i_);
            INDArray labels = testSet_labels.get(i_);
            //INDArray predicted = bestModel.output(features,false);
            //eval.eval(labels, predicted);

            INDArray features_graph = testSet_2.get(i_);
            INDArray labels_graph = testSet_2_labels.get(i_);

            INDArray[] features_ = new INDArray[2];
            INDArray[] labels_ = new INDArray[1];

            features_[0] = features_graph;
            features_[1] = features;

            labels_[0] = labels;

            MultiDataSet mds = new MultiDataSet();


            mds = new MultiDataSet(features, labels, null, null);


            //System.out.println("EVAL convo: ");
            //eval.eval(labels, predicted);

            //System.out.println("i_: " + i_);
            //System.out.println(" TEST SET SIZE: " + testSet.size());
            //System.out.println(features_[0].shapeInfoToString() + " " + features_[1].shapeInfoToString());
            INDArray[] predicted_ = null; //bestModel_graph.output(false, features_, null, null);

            if(aR.convolution_option < 4){
                predicted_ = model_graph.output(false, features_, null, null);
            }
            else if(aR.convolution_option < 7){
                predicted_ = model_graph.output(features_graph);
            }else{
                predicted_ = model_graph.output(features);

            }

            //INDArray predicted_320 = bestModel_320.output(features_graph,false);

            //System.out.println("EVAL GRAPH:");
            //eval_320.eval(labels, predicted_320);
            eval_graph.eval(labels_[0], predicted_[0]);
        }

        //System.out.print(eval_320.stats());
        System.out.println(eval_graph.stats());
        //System.out.println(eval.stats());
        System.exit(1);

        //trainer.setListener(new StatsListener(statsStorage));
//Conduct early stopping training:

        //model.init();
        //model.fit(trainIter);

        //System.exit(1);
/*

        model.init();
        //record score once every 100 iterations
        //model.setListeners(new ScoreIterationListener(100));
        model.setListeners(eva_lis_train, eva_lis_test);

        double[] cost_train = new double[n_epoch];
        double[] cost_validation = new double[n_epoch];

        String out_train = "";
        String out_test = "";


        for(int i = 0; i < n_epoch; i++ ) {

            model.fit(trainIter);
            IEvaluation[] evals_test = eva_lis_test.getEvaluations();
            IEvaluation[] evals_train = eva_lis_train.getEvaluations();

            for(int i_ = 0; i_ < evals_test.length; i_++){

                out_test += evals_test[i_].getValue(Evaluation.Metric.ACCURACY) + "\t";
                out_train += evals_train[i_].getValue(Evaluation.Metric.ACCURACY) + "\t";

                System.out.println(evals_test[i_].getValue(Evaluation.Metric.ACCURACY) + " " + evals_train[i_].getValue(Evaluation.Metric.ACCURACY) + "\n");

            }

            bestModel = model.clone();

        }

        bestModel.save(new File("haha.bin"), true);


        bestModel = restoreMultiLayerNetwork(new File("haha.bin"));

        //bestModel = (MultiLayerNetwork) result.getBestModel();

        Evaluation  eval = new Evaluation(3);
        testIter.reset();

        while(testIter.hasNext()){
            DataSet t = testIter.next();

            INDArray features = t.getFeatures();
            INDArray labels = t.getLabels();
            INDArray predicted = bestModel.output(features,false);
            eval.eval(labels, predicted);

        }

        System.out.println(eval.stats());
        System.exit(1);




/*
        EarlyStoppingResult result = trainer.fit();


        //System.exit(1);
        bestModel = (MultiLayerNetwork) result.getBestModel();


        eval = new Evaluation(3);
        testIter.reset();

        while(testIter.hasNext()){
            DataSet t = testIter.next();

            INDArray features = t.getFeatures();
            INDArray labels = t.getLabels();
            INDArray predicted = bestModel.output(features,false);
            eval.eval(labels, predicted);

        }

        System.out.println(eval.stats());
        System.exit(1);





        model.init();
        //record score once every 100 iterations
        //model.setListeners(new ScoreIterationListener(100));
        model.setListeners(eva_lis_train, eva_lis_test);

        //double[] cost_train = new double[n_epoch];
        //double[] cost_validation = new double[n_epoch];

        out_train = "";
        out_test = "";

        for(int i = 0; i < n_epoch; i++ ) {

            model.fit(trainIter);

            LayerHelper h = model.getLayer(2).getHelper();    //Index 0: assume layer 0 is a ConvolutionLayer in this example

            System.out.println(model.getLayer(2).toString());

            System.out.println("Layer helper: " + (h == null ? null : h.getClass().getName()));


            System.exit(1);
            IEvaluation[] evals_test = eva_lis_test.getEvaluations();
            IEvaluation[] evals_train = eva_lis_train.getEvaluations();

            for(int i_ = 0; i_ < evals_test.length; i_++){

                out_test += evals_test[i_].getValue(Evaluation.Metric.ACCURACY) + "\t";
                out_train += evals_train[i_].getValue(Evaluation.Metric.ACCURACY) + "\t";

                System.out.println(evals_test[i_].getValue(Evaluation.Metric.ACCURACY) + " " + evals_train[i_].getValue(Evaluation.Metric.ACCURACY) + "\n");

            }


        }



/*
        String folderPath = "/home/koomikko/Documents/research/3d_tree_species/convolution_data/";

        File folder = new File(folderPath);
        File[] digitFolders = folder.listFiles();

        NativeImageLoader nil = new NativeImageLoader(100, 100, 1, BaseImageLoader.MultiPageMode.MINIBATCH);
        ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(0,1);

        INDArray input = Nd4j.create(new int[]{ 1634, 100*100 * 40 });
        INDArray output = Nd4j.create(new int[]{ 1634, 3 });

        int n = 0;
//scan all 0..9 digit subfolders
        for (File digitFolder : digitFolders) {

            int labelDigit = Integer.parseInt(digitFolder.getName());
            //scan all the images of the digit in processing
            File[] imageFiles = digitFolder.listFiles();

            for (File imageFile : imageFiles) {
                //read the image as a one dimensional array of 0..255 values
                INDArray img = nil.asRowVector(imageFile);
                //scale the 0..255 integer values into a 0..1 floating range
                //Note that the transform() method returns void, since it updates its input array
                scaler.transform(img);
                //copy the img array into the input matrix, in the next row
                input.putRow( n, img );
                //in the same row of the output matrix, fire (set to 1 value) the column correspondent to the label
                output.put( n, labelDigit, 1.0 );
                //row counter increment
                n++;

                System.out.println(n);
            }


        }


        int numLabels = 3;
        int batchSize = 50;
        int numExamples = 1634;
        Random rng = new Random(1234);

        ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();


        File mainPath = new File("/home/koomikko/Documents/research/3d_tree_species/convolution_data");
        FileSplit fileSplit = new FileSplit(mainPath, NativeImageLoader.ALLOWED_FORMATS, rng);
        BalancedPathFilter pathFilter = new BalancedPathFilter(rng, labelMaker, numExamples, numLabels, batchSize);

        double splitTrainTest = 0.8;

        InputSplit[] inputSplit = fileSplit.sample(pathFilter, splitTrainTest, 1 - splitTrainTest);
        InputSplit trainData = inputSplit[0];
        InputSplit testData = inputSplit[1];


        int height = 100;
        int width = 100;
        int channels = 1;

        ImageRecordReader recordReader = new ImageRecordReader(height, width, channels, labelMaker);
        DataSetIterator dataIter_train;

        recordReader.initialize(trainData);
        dataIter_train = new RecordReaderDataSetIterator(recordReader, batchSize, 1, numLabels);

        DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
        scaler.fit(dataIter_train);
        dataIter_train.setPreProcessor(scaler);

        while(dataIter_train.hasNext()){
            DataSet t = dataIter_train.next();

            INDArray features = t.getFeatures();
            INDArray labels = t.getLabels();

            System.out.println(features.toIntVector());
            //INDArray predicted = bestModel.output(features,false);
            //eval.eval(labels, predicted);
        }

        ImageRecordReader recordReader2 = new ImageRecordReader(height, width, channels, labelMaker);
        DataSetIterator dataIter_validation;


        recordReader2.initialize(testData);
        dataIter_validation = new RecordReaderDataSetIterator(recordReader2, batchSize, 1, numLabels);

        DataNormalization scaler2 = new ImagePreProcessingScaler(0, 1);
        scaler2.fit(dataIter_validation);
        dataIter_validation.setPreProcessor(scaler2);



        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .activation(Activation.RELU)
                .updater(new Sgd(aR.learning_rate))
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(0, convInit("cnn1", channels, 50 ,  new int[]{5, 5}, new int[]{1, 1}, new int[]{0, 0}, 0))
                .layer(1, maxPool("maxpool1", new int[]{2,2}))
                .layer(2, conv5x5("cnn2", 100, new int[]{5, 5}, new int[]{1, 1}, 0))
                .layer(3, maxPool("maxool2", new int[]{2,2}))
                .layer(4, new DenseLayer.Builder().nOut(500).build())
                .layer(5, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nOut(numLabels)
                        .activation(Activation.SOFTMAX)
                        .build())
                .setInputType(InputType.convolutional(height, width, channels))
                .build();


        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        network.init();

        network.fit(dataIter_train);
          */
    }

    public static File changeExtension(File f, String newExtension) {
        int i = f.getName().lastIndexOf('.');
        String name = f.getName().substring(0,i);
        return new File(f.getParent(), name + newExtension);
    }

    private ConvolutionLayer convInit(String name, int in, int out, int[] kernel, int[] stride, int[] pad, double bias) {
        return new ConvolutionLayer.Builder(kernel, stride, pad).name(name).nIn(in).nOut(out).biasInit(bias).build();
    }

    private ConvolutionLayer conv5x5(String name, int out, int[] stride, int[] pad, double bias) {
        return new ConvolutionLayer.Builder(new int[]{5,5}, stride, pad).name(name).nOut(out).biasInit(bias).build();
    }

    private SubsamplingLayer maxPool(String name, int[] kernel) {
        return new SubsamplingLayer.Builder(kernel, new int[]{2,2}).name(name).build();
    }

}


