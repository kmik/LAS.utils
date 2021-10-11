package tools;

import org.apache.log4j.BasicConfigurator;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.arbiter.ComputationGraphSpace;
import org.deeplearning4j.arbiter.MultiLayerSpace;
import org.deeplearning4j.arbiter.conf.updater.AdamSpace;
import org.deeplearning4j.arbiter.layers.ConvolutionLayerSpace;
import org.deeplearning4j.arbiter.layers.DenseLayerSpace;
import org.deeplearning4j.arbiter.layers.OutputLayerSpace;
import org.deeplearning4j.arbiter.optimize.api.CandidateGenerator;
import org.deeplearning4j.arbiter.optimize.api.ParameterSpace;
import org.deeplearning4j.arbiter.optimize.api.data.DataSource;
import org.deeplearning4j.arbiter.optimize.api.saving.ResultSaver;
import org.deeplearning4j.arbiter.optimize.api.score.ScoreFunction;
import org.deeplearning4j.arbiter.optimize.api.termination.MaxTimeCondition;
import org.deeplearning4j.arbiter.optimize.api.termination.TerminationCondition;
import org.deeplearning4j.arbiter.optimize.config.OptimizationConfiguration;
import org.deeplearning4j.arbiter.optimize.generator.RandomSearchGenerator;
import org.deeplearning4j.arbiter.optimize.parameter.continuous.ContinuousParameterSpace;
import org.deeplearning4j.arbiter.optimize.parameter.discrete.DiscreteParameterSpace;
import org.deeplearning4j.arbiter.optimize.parameter.integer.IntegerParameterSpace;
import org.deeplearning4j.arbiter.optimize.runner.IOptimizationRunner;
import org.deeplearning4j.arbiter.optimize.runner.LocalOptimizationRunner;
import org.deeplearning4j.arbiter.saver.local.FileModelSaver;
import org.deeplearning4j.arbiter.scoring.impl.EvaluationScoreFunction;
import org.deeplearning4j.arbiter.task.ComputationGraphTaskCreator;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.INDArrayDataSetIterator;
import org.deeplearning4j.datasets.iterator.JointMultiDataSetIterator;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.graph.MergeVertex;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.evaluation.classification.Evaluation.Metric;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.argumentReader;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static tools.convolution.changeExtension;

public class neuralNetworkHyperparameterOptimization_convolution_mix {

    public static MultiDataSetIterator train_iter_multi = null;
    public static MultiDataSetIterator test_iter_multi = null;

    public static DataSetIterator train_iter = null;
    public static DataSetIterator test_iter = null;

    public static DataSetIterator train_iter_2 = null;
    public static DataSetIterator test_iter_2 = null;


    private static Logger log = LoggerFactory.getLogger(neuralNetWorkTest_3d_treespecies.class);
    //private final BatchNormalization normalizer_batch;

    public neuralNetworkHyperparameterOptimization_convolution_mix(argumentReader aR) throws  Exception {

        int n_in = 4 * 4 * 10;

        int depth = 10;
        int _x = 4;
        int _y = 4;


        int n_in_tex = 40 * 40 * 40;

        int depth_tex = 40;
        int _x_tex = 40;
        int _y_tex = 40;


        int batchSize = (int) Math.pow(2, 5);
        batchSize = 64;

        int n_epoch = 65;

        File outFile = new File(aR.output);

        if (!outFile.exists())
            outFile.createNewFile();

        if (aR.model != null) {

            MultiLayerNetwork model = MultiLayerNetwork.load(aR.model, true);

            //System.out.println(model.conf().toString());
            //      System.exit(1);

            RecordReader recordReader2 = new CSVRecordReader(0, ",");
            RecordReader recordReader2_test = new CSVRecordReader(0, ",");
            recordReader2.initialize(new FileSplit(aR.train));
            recordReader2_test.initialize(new FileSplit(aR.test));

            int labelIndex2 = 320;
            int batchSize2 = batchSize;
            int numClasses2 = 3;

            DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2, batchSize2, labelIndex2, numClasses2);
            DataSetIterator testIter = new RecordReaderDataSetIterator(recordReader2_test, batchSize2, labelIndex2, numClasses2);

            DataNormalization normalizer = new NormalizerStandardize();
            //DataNormalization normalizer = new NormalizerMinMaxScaler(0,1);
            normalizer.fit(trainIter);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data

            trainIter.setPreProcessor(normalizer);
            testIter.setPreProcessor(normalizer);

            Evaluation eval = new Evaluation(3);

            ArrayList<Integer> pred = new ArrayList<>();
            ArrayList<Integer> obs = new ArrayList<>();

            while (testIter.hasNext()) {


                DataSet t = testIter.next();


                INDArray features = t.getFeatures();
                INDArray labels = t.getLabels();
                System.out.println(labels.size(0));
                System.out.println(labels.size(1));
                System.out.println(labels.columns());

                for (int i = 0; i < labels.rows(); i++) {

                    int one = labels.getColumn(0).getInt(i);
                    int two = labels.getColumn(1).getInt(i);

                    if (one == 1) {
                        obs.add(0);
                    } else if (two == 1) {
                        obs.add(1);
                    } else {
                        obs.add(2);
                    }

                }


                INDArray predicted = model.output(features, false);

                for (int i = 0; i < predicted.rows(); i++) {

                    double one = predicted.getColumn(0).getDouble(i);
                    double two = predicted.getColumn(1).getDouble(i);
                    double three = predicted.getColumn(2).getDouble(i);


                    if (one > two && one > three) {
                        pred.add(0);

                    } else if (two > one && two > three) {
                        pred.add(1);

                    } else {
                        pred.add(2);

                    }

                }
                eval.eval(labels, predicted);
            }

            FileWriter fw = new FileWriter(outFile, true);
            String outLinePred = pred.get(0) + ",";
            String outLineObs = obs.get(0) + ",";

            System.out.println(Arrays.toString(pred.toArray()));


            for (int i = 1; i < pred.size(); i++) {

                outLinePred += pred.get(i) + ",";
                outLineObs += obs.get(i) + ",";


            }

            fw.write(outLinePred);
            fw.write("\n");
            fw.write(outLineObs);
            fw.write("\n");

            fw.close();
            System.out.println(pred.size() + " == " + obs.size());


            System.out.println(eval.stats());
            return;
        }

        BasicConfigurator.configure();
        //LogManager.getRootLogger().setLevel(Level.OFF);

        String fileExtension = ".nnIn" + batchSize;

        System.out.println("STARTING!!");
        if(aR.prepare_nn_input > 0) {
            System.out.println("STARTED!");
            RecordReader recordReader = new CSVRecordReader(0, ",");
            RecordReader recordReader2 = new CSVRecordReader(0, ",");
            RecordReader recordReader2_test = new CSVRecordReader(0, ",");

            RecordReader rr_320 = new CSVRecordReader(0, ",");
            RecordReader rr_320_test = new CSVRecordReader(0, ",");

            ArrayList<INDArray> trainingSet = new ArrayList<>();
            ArrayList<INDArray> trainingSet_labels = new ArrayList<>();
            ArrayList<INDArray> testSet = new ArrayList<>();
            ArrayList<INDArray> testSet_labels = new ArrayList<>();

            ArrayList<INDArray> trainingSet_2 = new ArrayList<>();
            ArrayList<INDArray> trainingSet_2_labels = new ArrayList<>();
            ArrayList<INDArray> testSet_2 = new ArrayList<>();
            ArrayList<INDArray> testSet_2_labels = new ArrayList<>();


//recordReader.initialize(new FileSplit(new File("/home/koomikko/Documents/iris.csv")));
            //recordReader2.initialize(new FileSplit(new File("/home/koomikko/Documents/species2.csv")));
            //recordReader2.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_train_s2.txt")));

            try {
                recordReader2.initialize(new FileSplit(aR.train));
                //recordReader2_test.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_test_s2.txt")));
                recordReader2_test.initialize(new FileSplit(aR.test));

                rr_320.initialize(new FileSplit(aR.train_2));
                rr_320_test.initialize(new FileSplit(aR.test_2));

            } catch (Exception e) {
                e.printStackTrace();
            }
            int labelIndex = 4;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
            int labelIndex2 = n_in;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
            int numClasses = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
            int numClasses2 = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
            //int batchSize = 150;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)
            int batchSize2 = 64;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)

            //batchSize2 = 1;

            //DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader2,batchSize2,labelIndex2,numClasses2);
            //DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2,batchSize2,labelIndex2,numClasses2);
            DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2, batchSize2, labelIndex2, numClasses2);
            DataSetIterator trainIter_2 = new RecordReaderDataSetIterator(rr_320, batchSize2, n_in_tex, numClasses2);
            DataSetIterator testIter = new RecordReaderDataSetIterator(recordReader2_test, batchSize2, labelIndex2, numClasses2);
            DataSetIterator testIter_2 = new RecordReaderDataSetIterator(rr_320_test, batchSize2, n_in_tex, numClasses2);

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

            //System.out.println("HERE!!!");
            //System.exit(1);

            while (trainIter.hasNext()) {
                DataSet ds = trainIter.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());


                trainingSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth, _y, _x));
                //trainingSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));

                trainingSet_labels.add(ds.getLabels());

            }

            while (trainIter_2.hasNext()) {
                DataSet ds = trainIter_2.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());

                trainingSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth_tex, _y_tex, _x_tex));
                //trainingSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth_tex, _y_tex, _x_tex));
                //trainingSet_2.add(ds.getFeatures());
                trainingSet_2_labels.add(ds.getLabels());
                counti2++;
                //System.out.println(counti2);
            }

            while (testIter.hasNext()) {

                DataSet ds = testIter.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());

                testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth, _y, _x));
                //testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth, _y, _x, 1));
                testSet_labels.add(ds.getLabels());

            }

            while (testIter_2.hasNext()) {

                DataSet ds = testIter_2.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());

                testSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth_tex, _y_tex, _x_tex));
                //testSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth_tex, _y_tex, _x_tex));
                //testSet_2.add(ds.getFeatures());
                testSet_2_labels.add(ds.getLabels());

            }

            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> train_list_1 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> train_list_2 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> test_list_1 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> test_list_2 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();

            for (int i = 0; i < trainingSet.size(); i++) {

                train_list_1.add(new org.nd4j.common.primitives.Pair<>(trainingSet.get(i), trainingSet_labels.get(i)));
                train_list_2.add(new org.nd4j.common.primitives.Pair<>(trainingSet_2.get(i), trainingSet_2_labels.get(i)));

            }

            for (int i = 0; i < testSet.size(); i++) {

                test_list_1.add(new org.nd4j.common.primitives.Pair<>(testSet.get(i), testSet_labels.get(i)));
                test_list_2.add(new org.nd4j.common.primitives.Pair<>(testSet_2.get(i), testSet_2_labels.get(i)));

            }

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



            System.out.println("HERE!!");
            System.exit(1);
        }
        String baseSaveDirectory = aR.odir;
        File f = new File(baseSaveDirectory);
        if(f.exists()) f.delete();
        f.mkdir();
        ResultSaver modelSaver = new FileModelSaver(baseSaveDirectory);



        System.out.println("START READING BIN!");

        ArrayList<INDArray> trainingSet = new ArrayList<>();
        ArrayList<INDArray> trainingSet_labels = new ArrayList<>();
        ArrayList<INDArray> testSet = new ArrayList<>();
        ArrayList<INDArray> testSet_labels = new ArrayList<>();

        ArrayList<INDArray> trainingSet_2 = new ArrayList<>();
        ArrayList<INDArray> trainingSet_2_labels = new ArrayList<>();
        ArrayList<INDArray> testSet_2 = new ArrayList<>();
        ArrayList<INDArray> testSet_2_labels = new ArrayList<>();

        try {
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
        }catch (Exception e){
            e.printStackTrace();
        }
        List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> train_list_1 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
        List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> train_list_2 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
        List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> test_list_1 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
        List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> test_list_2 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();

        INDArray emptyArray = new NDArray();


        for(int i = 0; i < trainingSet.size(); i++){

            //System.out.println("HEREERERE " + trainingSet.size() + " " + trainingSet_labels.size());
            //trainingSet.get(i);
            //trainingSet_labels.get(i);
            train_list_1.add(new org.nd4j.common.primitives.Pair<>(trainingSet.get(i), trainingSet_labels.get(i)));

        }

        for(int i = 0; i < trainingSet_2.size(); i++){

            train_list_2.add(new org.nd4j.common.primitives.Pair<>(trainingSet_2.get(i), trainingSet_2_labels.get(i)));
            //train_list_2.add(new org.nd4j.common.primitives.Pair<>(trainingSet_2.get(i), emptyArray));

        }


        for(int i = 0; i < testSet.size(); i++){

            test_list_1.add(new org.nd4j.common.primitives.Pair<>(testSet.get(i), testSet_labels.get(i)));

        }

        for(int i = 0; i < testSet_2.size(); i++){

            test_list_2.add(new org.nd4j.common.primitives.Pair<>(testSet_2.get(i), testSet_2_labels.get(i)));
            //test_list_2.add(new org.nd4j.common.primitives.Pair<>(testSet_2.get(i), emptyArray));

        }

        //org.nd4j.common.primitives.Pair<INDArray, INDArray> haha = new org.nd4j.common.primitives.Pair<>(trainingSet.get(0), trainingSet_2_labels.get(0));

        //List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> haha_list = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
        //haha_list.add(haha);



        INDArrayDataSetIterator train_1_iterator = new INDArrayDataSetIterator(train_list_1, 1);
        INDArrayDataSetIterator train_2_iterator = new INDArrayDataSetIterator(train_list_2, 1);
        INDArrayDataSetIterator test_1_iterator = new INDArrayDataSetIterator(test_list_1, 1);
        INDArrayDataSetIterator test_2_iterator = new INDArrayDataSetIterator(test_list_2, 1);
        //MultiDataSetIterator multiDataSetIterator_train = new JointMultiDataSetIterator(train_1_iterator, train_2_iterator);
        MultiDataSetIterator multiDataSetIterator_train = new JointMultiDataSetIterator(1, train_2_iterator, train_1_iterator);
        //MultiDataSetIterator multiDataSetIterator_test = new JointMultiDataSetIterator(test_1_iterator, test_2_iterator);
        MultiDataSetIterator multiDataSetIterator_test = new JointMultiDataSetIterator(1, test_2_iterator, test_1_iterator);

        this.train_iter_multi = multiDataSetIterator_train;
        this.test_iter_multi = multiDataSetIterator_test;

        this.train_iter = train_1_iterator;
        this.train_iter_2 = train_2_iterator;

        this.test_iter = test_1_iterator;
        this.test_iter_2 = test_2_iterator;

        System.out.println("END READING BIN!");


        ParameterSpace<Double> learningRateHyperparam = new ContinuousParameterSpace(0.00001, 0.1);  //Values will be generated uniformly at random between 0.0001 and 0.1 (inclusive)
        ParameterSpace<Integer> layerSizeHyperparam = new IntegerParameterSpace(16,256);            //Integer values will be generated uniformly at random between 16 and 256 (inclusive)
        ParameterSpace<Integer> layerSizeHyperparam2 = new IntegerParameterSpace(16,256);            //Integer values will be generated uniformly at random between 16 and 256 (inclusive)
        ParameterSpace<Double> l2sizeHyperparam = new ContinuousParameterSpace(1e-9, 1e-2);

        //ParameterSpace<Integer> numEpochs = new IntegerParameterSpace(15);
        DiscreteParameterSpace<Activation> activationSpace =new DiscreteParameterSpace(new Activation[]{Activation.TANH,Activation.SIGMOID,Activation.RELU});
        DiscreteParameterSpace<WeightInit> weightInitSpace =new DiscreteParameterSpace(new WeightInit[]{WeightInit.NORMAL, WeightInit.XAVIER, WeightInit.RELU});

        DiscreteParameterSpace<Integer> texture_convolution_layer_1_depth = new DiscreteParameterSpace<Integer>(3,5,7,9,11,13,15);
        DiscreteParameterSpace<Integer> texture_convolution_layer_1_x = new DiscreteParameterSpace<Integer>(3,5);
        DiscreteParameterSpace<Integer> texture_convolution_layer_1_y = new DiscreteParameterSpace<Integer>(3,5);

        DiscreteParameterSpace<Integer> texture_convolution_layer_1_stride_depth = new DiscreteParameterSpace<Integer>(1,2,3);
        DiscreteParameterSpace<Integer> texture_convolution_layer_1_stride_x = new DiscreteParameterSpace<Integer>(1,2,3);
        DiscreteParameterSpace<Integer> texture_convolution_layer_1_stride_y = new DiscreteParameterSpace<Integer>(1,2,3);

        DiscreteParameterSpace<Integer> texture_convolution_layer_2_depth = new DiscreteParameterSpace<Integer>(3,5,7,9,11,13,15);
        DiscreteParameterSpace<Integer> texture_convolution_layer_2_x = new DiscreteParameterSpace<Integer>(3,5);
        DiscreteParameterSpace<Integer> texture_convolution_layer_2_y = new DiscreteParameterSpace<Integer>(3,5);

        DiscreteParameterSpace<Integer> texture_convolution_layer_2_stride_depth = new DiscreteParameterSpace<Integer>(1,2,3);
        DiscreteParameterSpace<Integer> texture_convolution_layer_2_stride_x = new DiscreteParameterSpace<Integer>(1,2,3);
        DiscreteParameterSpace<Integer> texture_convolution_layer_2_stride_y = new DiscreteParameterSpace<Integer>(1,2,3);

        DiscreteParameterSpace<Integer> voxel_convolution_layer_1_depth = new DiscreteParameterSpace<Integer>(3,5,7,9,11,13,15);
        DiscreteParameterSpace<Integer> voxel_convolution_layer_1_x = new DiscreteParameterSpace<Integer>(3,5);
        DiscreteParameterSpace<Integer> voxel_convolution_layer_1_y = new DiscreteParameterSpace<Integer>(3,5);

        DiscreteParameterSpace<Integer> voxel_convolution_layer_1_stride_depth = new DiscreteParameterSpace<Integer>(1,2,3);
        DiscreteParameterSpace<Integer> voxel_convolution_layer_1_stride_x = new DiscreteParameterSpace<Integer>(1,2,3);
        DiscreteParameterSpace<Integer> voxel_convolution_layer_1_stride_y = new DiscreteParameterSpace<Integer>(1,2,3);

        DiscreteParameterSpace<Integer> voxel_convolution_layer_2_depth = new DiscreteParameterSpace<Integer>(3,5,7,9,11,13,15);
        DiscreteParameterSpace<Integer> voxel_convolution_layer_2_x = new DiscreteParameterSpace<Integer>(3,5);
        DiscreteParameterSpace<Integer> voxel_convolution_layer_2_y = new DiscreteParameterSpace<Integer>(3,5);

        DiscreteParameterSpace<Integer> voxel_convolution_layer_2_stride_depth = new DiscreteParameterSpace<Integer>(1,2,3);
        DiscreteParameterSpace<Integer> voxel_convolution_layer_2_stride_x = new DiscreteParameterSpace<Integer>(1,2,3);
        DiscreteParameterSpace<Integer> voxel_convolution_layer_2_stride_y = new DiscreteParameterSpace<Integer>(1,2,3);

        /*

        DiscreteParameterSpace<Integer> texture_convolution_layer_3_depth = new DiscreteParameterSpace<Integer>(3,5,7,9,11,13,15);
        DiscreteParameterSpace<Integer> texture_convolution_layer_3_x = new DiscreteParameterSpace<Integer>(3,5);
        DiscreteParameterSpace<Integer> texture_convolution_layer_3_y = new DiscreteParameterSpace<Integer>(3,5);

        DiscreteParameterSpace<Integer> texture_convolution_layer_3_stride_depth = new DiscreteParameterSpace<Integer>(1,2,3);
        DiscreteParameterSpace<Integer> texture_convolution_layer_3_stride_x = new DiscreteParameterSpace<Integer>(1,2,3);
        DiscreteParameterSpace<Integer> texture_convolution_layer_3_stride_y = new DiscreteParameterSpace<Integer>(1,2,3);

        DiscreteParameterSpace<Integer> texture_convolution_layer_4_depth = new DiscreteParameterSpace<Integer>(3,5,7,9,11,13,15);
        DiscreteParameterSpace<Integer> texture_convolution_layer_4_x = new DiscreteParameterSpace<Integer>(3,5);
        DiscreteParameterSpace<Integer> texture_convolution_layer_4_y = new DiscreteParameterSpace<Integer>(3,5);

        DiscreteParameterSpace<Integer> texture_convolution_layer_4_stride_depth = new DiscreteParameterSpace<Integer>(1,2,3);
        DiscreteParameterSpace<Integer> texture_convolution_layer_4_stride_x = new DiscreteParameterSpace<Integer>(1,2,3);
        DiscreteParameterSpace<Integer> texture_convolution_layer_4_stride_y = new DiscreteParameterSpace<Integer>(1,2,3);
*/

        ParameterSpace<Integer> layerSizeHyperparam_texture_dense1 = new IntegerParameterSpace(16,256);            //Integer values will be generated uniformly at random between 16 and 256 (inclusive)
        ParameterSpace<Integer> layerSizeHyperparam_voxel_dense1 = new IntegerParameterSpace(16,256);
        ParameterSpace<Integer> layerSizeHyperparam_merge = new IntegerParameterSpace(16,256);
        ParameterSpace<Integer> layerSizeHyperparam_haralick1 = new IntegerParameterSpace(16,256);
        ParameterSpace<Integer> layerSizeHyperparam_haralick2 = new IntegerParameterSpace(16,256);


        //ParameterSpace<Integer> layerSizeHyperparam = new IntegerParameterSpace(16,256);            //Integer values will be generated uniformly at random between 16 and 256 (inclusive)
        //ParameterSpace<Integer> layerSizeHyperparam2 = new IntegerParameterSpace(16,256);

        DiscreteParameterSpace<int[]> voxel_1_k = new DiscreteParameterSpace<int[]>(new int[]{1,1}, new int[]{1,3}, new int[]{3,1}, new int[]{3,3});
        DiscreteParameterSpace<int[]> voxel_2_k = new DiscreteParameterSpace<int[]>(new int[]{1,1}, new int[]{1,3}, new int[]{3,1}, new int[]{3,3});
        DiscreteParameterSpace<int[]> voxel_3_k = new DiscreteParameterSpace<int[]>(new int[]{1,1}, new int[]{1,3}, new int[]{3,1}, new int[]{3,3});

        DiscreteParameterSpace<int[]> voxel_1_s = new DiscreteParameterSpace<int[]>(new int[]{1,1});
        DiscreteParameterSpace<int[]> voxel_2_s = new DiscreteParameterSpace<int[]>(new int[]{1,1});
        DiscreteParameterSpace<int[]> voxel_3_s = new DiscreteParameterSpace<int[]>(new int[]{1,1});


        DiscreteParameterSpace<int[]> texture_1_k = new DiscreteParameterSpace<int[]>(new int[]{1,1}, new int[]{1,3}, new int[]{3,1}, new int[]{3,3},
                                                                                        new int[]{1,5}, new int[]{1,5}, new int[]{5,1}, new int[]{5,3},
                                                                                    new int[]{1,7}, new int[]{1,7}, new int[]{7,1}, new int[]{7,3},
                new int[]{3,5}, new int[]{3,5}, new int[]{5,3}, new int[]{5,5},
                new int[]{3,7}, new int[]{3,7}, new int[]{7,3}, new int[]{7,5},

                new int[]{7,5}, new int[]{7,5}, new int[]{5,7}, new int[]{7,7},
                new int[]{5,7}, new int[]{3,7}, new int[]{7,3});
        DiscreteParameterSpace<int[]> texture_2_k = new DiscreteParameterSpace<int[]>(new int[]{1,1}, new int[]{1,3}, new int[]{3,1}, new int[]{3,3},
                new int[]{3,5}, new int[]{1,5}, new int[]{5,1}, new int[]{5,3});
        DiscreteParameterSpace<int[]> texture_3_k = new DiscreteParameterSpace<int[]>(new int[]{1,1}, new int[]{1,3}, new int[]{3,1}, new int[]{3,3});

        DiscreteParameterSpace<int[]> texture_1_s = new DiscreteParameterSpace<int[]>(new int[]{1,1}, new int[]{1,2}, new int[]{2,1}, new int[]{2,2},
                new int[]{3,3}, new int[]{3,2}, new int[]{2,3}, new int[]{1,3},
                new int[]{3,1}, new int[]{3,3});
        DiscreteParameterSpace<int[]> texture_2_s = new DiscreteParameterSpace<int[]>(new int[]{1,1}, new int[]{1,2}, new int[]{2,1}, new int[]{2,2});
        DiscreteParameterSpace<int[]> texture_3_s = new DiscreteParameterSpace<int[]>(new int[]{1,1}, new int[]{1,2}, new int[]{2,1});


        ParameterSpace<Double> dropoutParamSpace = new ContinuousParameterSpace(0.1, 1.0);


        if(aR.neural_mode.equals("merged")){

            System.out.println("MERGED!!");
            System.out.println("MERGED!!");
            System.out.println("MERGED!!");
            System.out.println("MERGED!!");
            System.out.println("MERGED!!");

            ComputationGraphSpace mls = new ComputationGraphSpace.Builder()
                    .activation(activationSpace)
                    .weightInit(weightInitSpace)
                    .numEpochs(120)
                    .updater(new AdamSpace(learningRateHyperparam))
                    .l2(l2sizeHyperparam)
                    .dropOut(dropoutParamSpace)
                    .addInputs("input1", "input2")
                    .addLayer("de_1", new DenseLayerSpace.Builder().nIn(160).nOut(layerSizeHyperparam_haralick1).build(), "input1")
                    .addLayer("de_2", new DenseLayerSpace.Builder().nOut(layerSizeHyperparam_haralick2).build(), "de_1")
                    //.addLayer("dense1", new DenseLayerSpace.Builder().nOut(layerSizeHyperparam_texture_dense1).build(), "convo2")
                    .addLayer( "vox1", new ConvolutionLayerSpace.Builder().nIn(depth).nOut(depth).kernelSize(voxel_1_k).stride(voxel_1_s).convolutionMode(ConvolutionMode.Same).build(), "input2")
                    .addLayer( "vox2", new ConvolutionLayerSpace.Builder().nIn(depth).nOut(depth).kernelSize(voxel_2_k).stride(voxel_2_s).convolutionMode(ConvolutionMode.Same).build(), "vox1")
                    .addLayer("dense2", new DenseLayerSpace.Builder().nOut(layerSizeHyperparam_voxel_dense1).build(), "vox2")
                    .addVertex("merge", new MergeVertex(), "de_2", "dense2")
                    .addLayer("dense3", new DenseLayerSpace.Builder().hasBias(true).nOut(layerSizeHyperparam_merge).build(), "merge")

                    .addLayer("out", new OutputLayerSpace.Builder().nOut(3).activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).build(), "dense3")
                    //.setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth_tex, _y_tex, _x_tex, 1),
                    //      InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth, _y, _x, 1))
                    .setInputTypes(InputType.feedForward(160),
                            InputType.convolutional(_y, _x, depth))
                    .setOutputs("out")
                    .build();

            ScoreFunction scoreFunction = new EvaluationScoreFunction(Metric.F1);

            CandidateGenerator candidateGenerator = new RandomSearchGenerator(mls);

            Properties dataSourceProperties = new Properties();

            ExampleDataSource_convolution_all ds = new ExampleDataSource_convolution_all();


            TerminationCondition[] terminationConditions ={
                    new MaxTimeCondition(48,TimeUnit.HOURS),
                    //new MaxCandidatesCondition(10)
            };

            OptimizationConfiguration configuration = new OptimizationConfiguration.Builder()
                    .candidateGenerator(candidateGenerator)
                    .dataSource(ds.getClass(), dataSourceProperties)
                    .modelSaver(modelSaver)
                    .terminationConditions(terminationConditions)
                    .scoreFunction(scoreFunction)
                    .build();


            IOptimizationRunner runner = new LocalOptimizationRunner(configuration, new ComputationGraphTaskCreator());
            //IOptimizationRunner runner = new LocalOptimizationRunner(configuration, new MultiLayerNetworkTaskCreator());

            runner.removeAllListeners();

            //Start the hyperparameter optimization
            runner.execute();

            System.out.println("SUCCESS!");
            System.exit(1);

        }else if(aR.neural_mode.equals("voxel")){

            System.out.println("VOXEL!!");
            System.out.println("VOXEL!!");
            System.out.println("VOXEL!!");
            System.out.println("VOXEL!!");
            ComputationGraphSpace mls = new ComputationGraphSpace.Builder()
                    .activation(activationSpace)
                    .weightInit(weightInitSpace)
                    .numEpochs(60)
                    .updater(new AdamSpace(learningRateHyperparam))
                    .l2(l2sizeHyperparam)
                    .addInputs("input2")
                    .addLayer( "vox1", new ConvolutionLayerSpace.Builder().nIn(depth).nOut(depth).kernelSize(voxel_1_k).stride(voxel_1_s).convolutionMode(ConvolutionMode.Same).build(), "input2")
                    .addLayer( "vox2", new ConvolutionLayerSpace.Builder().nIn(depth).nOut(depth).kernelSize(voxel_2_k).stride(voxel_2_s).convolutionMode(ConvolutionMode.Same).build(), "vox1")
                    .addLayer("dense2", new DenseLayerSpace.Builder().nOut(layerSizeHyperparam_voxel_dense1).build(), "vox2")
                    .addLayer("out", new OutputLayerSpace.Builder().nOut(3).activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).build(), "dense2")
                    //.setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth_tex, _y_tex, _x_tex, 1),
                    //      InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth, _y, _x, 1))
                    .setInputTypes(
                            InputType.convolutional(_y, _x, depth))
                    .setOutputs("out")
                    .build();

            ScoreFunction scoreFunction = new EvaluationScoreFunction(Metric.F1);

            CandidateGenerator candidateGenerator = new RandomSearchGenerator(mls);

            Properties dataSourceProperties = new Properties();

            ExampleDataSource_convolution_voxel ds = new ExampleDataSource_convolution_voxel();


            TerminationCondition[] terminationConditions ={
                    new MaxTimeCondition(30,TimeUnit.HOURS),
                    //new MaxCandidatesCondition(10)
            };

            OptimizationConfiguration configuration = new OptimizationConfiguration.Builder()
                    .candidateGenerator(candidateGenerator)
                    .dataSource(ds.getClass(), dataSourceProperties)
                    .modelSaver(modelSaver)
                    .terminationConditions(terminationConditions)
                    .scoreFunction(scoreFunction)
                    .build();



            IOptimizationRunner runner = new LocalOptimizationRunner(configuration, new ComputationGraphTaskCreator());
            //IOptimizationRunner runner = new LocalOptimizationRunner(configuration, new MultiLayerNetworkTaskCreator());

            runner.removeAllListeners();

            //Start the hyperparameter optimization
            runner.execute();

            System.out.println("SUCCESS!");
            System.exit(1);

        }else if(aR.neural_mode.equals("texture")){
            System.out.println("TEXTURE!!");
            System.out.println("TEXTURE!!");
            System.out.println("TEXTURE!!");
            System.out.println("TEXTURE!!");

            ComputationGraphSpace mls = new ComputationGraphSpace.Builder()
                    .activation(activationSpace)
                    .weightInit(weightInitSpace)
                    .numEpochs(60)
                    .updater(new AdamSpace(learningRateHyperparam))
                    .l2(l2sizeHyperparam)
                    .addInputs("input1")
                    .addLayer( "convo1", new ConvolutionLayerSpace.Builder().nIn(depth_tex).nOut(depth_tex).kernelSize(texture_1_k).stride(texture_1_s).convolutionMode(ConvolutionMode.Same).build(), "input1")
                    .addLayer( "convo2", new ConvolutionLayerSpace.Builder().nIn(depth_tex).nOut(depth_tex).kernelSize(texture_2_k).stride(texture_2_s).convolutionMode(ConvolutionMode.Same).build(), "convo1")
                    .addLayer("dense1", new DenseLayerSpace.Builder().nOut(layerSizeHyperparam_texture_dense1).build(), "convo2")

                    .addLayer("out", new OutputLayerSpace.Builder().nOut(3).activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).build(), "dense1")
                    //.setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth_tex, _y_tex, _x_tex, 1),
                    //      InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth, _y, _x, 1))
                    .setInputTypes(InputType.convolutional(_y_tex, _x_tex, depth_tex))
                    .setOutputs("out")
                    .build();

            ScoreFunction scoreFunction = new EvaluationScoreFunction(Metric.F1);

            CandidateGenerator candidateGenerator = new RandomSearchGenerator(mls);

            Properties dataSourceProperties = new Properties();

            ExampleDataSource_convolution_texture ds = new ExampleDataSource_convolution_texture();

            TerminationCondition[] terminationConditions ={
                    new MaxTimeCondition(48,TimeUnit.HOURS),
                    //new MaxCandidatesCondition(10)
            };

            OptimizationConfiguration configuration = new OptimizationConfiguration.Builder()
                    .candidateGenerator(candidateGenerator)
                    .dataSource(ds.getClass(), dataSourceProperties)
                    .modelSaver(modelSaver)
                    .terminationConditions(terminationConditions)
                    .scoreFunction(scoreFunction)
                    .build();



            IOptimizationRunner runner = new LocalOptimizationRunner(configuration, new ComputationGraphTaskCreator());
            //IOptimizationRunner runner = new LocalOptimizationRunner(configuration, new MultiLayerNetworkTaskCreator());

            runner.removeAllListeners();

            //Start the hyperparameter optimization
            runner.execute();

            System.out.println("SUCCESS!");
            System.exit(1);
        }

        MultiLayerSpace mls1 =
                new MultiLayerSpace.Builder()
                .activation(activationSpace)
                .weightInit(weightInitSpace)
                        .numEpochs(30)
                .updater(new AdamSpace(learningRateHyperparam))
                //.updater(new Nesterovs(aR.learning_rate))
                .l2(l2sizeHyperparam)
                .addLayer(new DenseLayerSpace.Builder().nIn(aR.label_index).nOut(layerSizeHyperparam)
                        .build())

                //.layer(new DropoutLayer(0.75))
                .addLayer(new DenseLayerSpace.Builder().nIn(layerSizeHyperparam).nOut(layerSizeHyperparam2)
                        .build())
                .addLayer( new OutputLayerSpace.Builder()
                        .lossFunction(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(layerSizeHyperparam2).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();


        MultiLayerSpace mls123 =
                new MultiLayerSpace.Builder()
                        .activation(activationSpace)
                        .weightInit(weightInitSpace)
                        .numEpochs(30)
                        .updater(new AdamSpace(learningRateHyperparam))
                        //.updater(new Nesterovs(aR.learning_rate))
                        .l2(l2sizeHyperparam)
                        .layer( new ConvolutionLayerSpace.Builder().nIn(depth).nOut(depth).kernelSize(2,2).stride(1,1).convolutionMode(ConvolutionMode.Same).build())
                        //.layer(new DropoutLayer(0.75))
                        .layer( new ConvolutionLayerSpace.Builder().nIn(depth).nOut(depth).kernelSize(2,2).stride(1,1).convolutionMode(ConvolutionMode.Same).build())

                        .addLayer( new OutputLayerSpace.Builder()
                                .lossFunction(LossFunctions.LossFunction.MCXENT)
                                .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                                //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                                .nIn(n_in).nOut(3).build())
                        //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                        //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //    .nIn(4).nOut(3).build())
                        .setInputType(InputType.convolutional(_y, _x, depth))
                        .build();




        ComputationGraphSpace mls22 =
                new ComputationGraphSpace.Builder()
                        .activation(activationSpace)
                        .weightInit(weightInitSpace)
                        .numEpochs(50)
                        .updater(new AdamSpace(learningRateHyperparam))
                        //.updater(new Nesterovs(aR.learning_rate))
                        .l2(l2sizeHyperparam)
                        .addInputs("input1")
                        .addLayer("convo1", new ConvolutionLayerSpace.Builder().nIn(depth).nOut(depth).kernelSize(voxel_1_k).stride(voxel_1_s).convolutionMode(ConvolutionMode.Same).build(), "input1")
                        //.layer(new DropoutLayer(0.75))
                        .addLayer("convo2", new ConvolutionLayerSpace.Builder().nIn(depth).nOut(depth).kernelSize(voxel_2_k).stride(voxel_2_s).convolutionMode(ConvolutionMode.Same).build(), "convo1")
                        .addLayer("convo3", new ConvolutionLayerSpace.Builder().nIn(depth).nOut(depth).kernelSize(voxel_3_k).stride(voxel_3_s).convolutionMode(ConvolutionMode.Same).build(), "convo2")

                        .addLayer("dense1", new DenseLayerSpace.Builder().nOut(layerSizeHyperparam).build(), "convo3")

                        .addLayer("out", new OutputLayerSpace.Builder()
                                .lossFunction(LossFunctions.LossFunction.MCXENT)
                                .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                                //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                                .nIn(layerSizeHyperparam).nOut(3).build(), "dense1")
                        //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                        //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //    .nIn(4).nOut(3).build())
                        .setInputTypes(InputType.convolutional(_y, _x, depth))
                        .setOutputs("out")
                        .build();
        //DiscreteParameterSpace<int[]> asdi = new DiscreteParameterSpace<int[]>(new int[]{2,2}, new int[]{3,3});


        ComputationGraphSpace mls = new ComputationGraphSpace.Builder()
                .activation(activationSpace)
                .weightInit(weightInitSpace)
                .numEpochs(25)
                .updater(new AdamSpace(learningRateHyperparam))
                .l2(l2sizeHyperparam)
                .addInputs("input1", "input2")
                .addLayer( "convo1", new ConvolutionLayerSpace.Builder().nIn(depth_tex).nOut(depth_tex).kernelSize(2,2).stride(1,1).convolutionMode(ConvolutionMode.Same).build(), "input1")
                .addLayer("dense1", new DenseLayerSpace.Builder().nOut(5).build(), "convo1")
                .addLayer( "vox1", new ConvolutionLayerSpace.Builder().nIn(depth).nOut(depth).kernelSize(2,2).stride(1,1).convolutionMode(ConvolutionMode.Same).build(), "input2")
                .addLayer("dense2", new DenseLayerSpace.Builder().nOut(5).build(), "vox1")
                .addVertex("merge", new MergeVertex(), "dense1", "dense2")
                .addLayer("dense3", new DenseLayerSpace.Builder().hasBias(true).nOut(9).build(), "merge")

                .addLayer("out", new OutputLayerSpace.Builder().nOut(3).activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).build(), "dense3")
                //.setInputTypes(InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth_tex, _y_tex, _x_tex, 1),
                  //      InputType.convolutional3D(Convolution3D.DataFormat.NDHWC, depth, _y, _x, 1))
                .setInputTypes(InputType.convolutional(_y_tex, _x_tex, depth_tex),
                                InputType.convolutional(_y, _x, depth))
                .setOutputs("out")
                .build();


        ScoreFunction scoreFunction = new EvaluationScoreFunction(Metric.F1);

        CandidateGenerator candidateGenerator = new RandomSearchGenerator(mls);

        //DataSet ds = new DataSet();

        //ExampleDataSource_convolution_all ds = new ExampleDataSource_convolution_all();
        //ExampleDataSource ds2 = new ExampleDataSource();
        //ExampleDataSource_convolution_voxel ds = new ExampleDataSource_convolution_voxel();
        ExampleDataSource_convolution_all ds = new ExampleDataSource_convolution_all();


        //ds.setTestData(testIter);
        //ds.setTrainData(trainIter);
        Properties dataSourceProperties = new Properties();

/*

        dataSourceProperties.setProperty("minibatchSize", Integer.toString(batchSize));
        dataSourceProperties.setProperty("label_index", Integer.toString(n_in));
        dataSourceProperties.setProperty("label_index_2", Integer.toString(n_in_tex));
        dataSourceProperties.setProperty("train_name", aR.train.getAbsolutePath());
        dataSourceProperties.setProperty("test_name", aR.test.getAbsolutePath());

        dataSourceProperties.setProperty("train_name_bin", aR.train.getAbsolutePath());
        dataSourceProperties.setProperty("test_name_bin", aR.test.getAbsolutePath());


        dataSourceProperties.setProperty("train_name_2", aR.train_2.getAbsolutePath());
        dataSourceProperties.setProperty("test_name_2", aR.test_2.getAbsolutePath());

        dataSourceProperties.setProperty("train_name_2_bin", aR.train_2.getAbsolutePath());
        dataSourceProperties.setProperty("test_name_2_bin", aR.test_2.getAbsolutePath());

        dataSourceProperties.setProperty("depth", Integer.toString(depth));
        dataSourceProperties.setProperty("_x", Integer.toString(_x));
        dataSourceProperties.setProperty("_y", Integer.toString(_y));

        dataSourceProperties.setProperty("depth_tex", Integer.toString(depth_tex));
        dataSourceProperties.setProperty("_x_tex", Integer.toString(_x_tex));
        dataSourceProperties.setProperty("_y_tex", Integer.toString(_y_tex));
*/
        TerminationCondition[] terminationConditions ={
                new MaxTimeCondition(30,TimeUnit.HOURS),
                //new MaxCandidatesCondition(10)
        };

        OptimizationConfiguration configuration = new OptimizationConfiguration.Builder()
                .candidateGenerator(candidateGenerator)
                .dataSource(ds.getClass(), dataSourceProperties)
                .modelSaver(modelSaver)
                .terminationConditions(terminationConditions)
                .scoreFunction(scoreFunction)
                .build();



        IOptimizationRunner runner = new LocalOptimizationRunner(configuration, new ComputationGraphTaskCreator());
        //IOptimizationRunner runner = new LocalOptimizationRunner(configuration, new MultiLayerNetworkTaskCreator());

        runner.removeAllListeners();

        //Start the hyperparameter optimization
        runner.execute();

        System.out.println("SUCCESS!");
        System.exit(1);

        /* Learning rate */
        double[] learningRates = new double[]{0.1, 0.01, 0.001};
        /* Neurons per layer */
        int[] neurons = new int[]{5, 10, 15, 25, 30, 35, 40, 75, 125, 200};
        /* Batch size */
        int[] batchSizes = new int[]{(int) Math.pow(2, 4), (int) Math.pow(2, 5), (int) Math.pow(2, 6), (int) Math.pow(2, 7), (int) Math.pow(2, 8), (int) Math.pow(2, 9)};
        /* Epoch count */
        int[] n_epochs = new int[]{25, 50, 100};
        /* Activation function */
        Activation[] activations = new Activation[]{Activation.SIGMOID, Activation.TANH, Activation.RELU};
        /* Weight initialization */
        WeightInit[] initializations = new WeightInit[]{WeightInit.XAVIER, WeightInit.RELU, WeightInit.NORMAL};

/*

        for (int l_rate = 0; l_rate < learningRates.length; l_rate++) {
            for (int neur = 0; neur < neurons.length; neur++) {
                for (int b_siz = 0; b_siz < batchSizes.length; b_siz++) {
                    for (int epch = 0; epch < n_epochs.length; epch++) {
                        for (int act = 0; act < activations.length; act++) {
                            for (int init = 0; init < initializations.length; init++) {

                                double l_r = learningRates[l_rate];
                                int n_n = neurons[neur];
                                int b_s = batchSizes[b_siz];
                                int ep = n_epochs[epch];
                                Activation acti = activations[act];
                                WeightInit ini = initializations[init];

                                RecordReader recordReader = new CSVRecordReader(0, ",");
                                RecordReader recordReader2 = new CSVRecordReader(0, ",");
                                RecordReader recordReader2_test = new CSVRecordReader(0, ",");


                                //recordReader.initialize(new FileSplit(new File("/home/koomikko/Documents/iris.csv")));
                                //recordReader2.initialize(new FileSplit(new File("/home/koomikko/Documents/species2.csv")));
                                //recordReader2.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_train_s2.txt")));
                                recordReader2.initialize(new FileSplit(aR.train));
                                //recordReader2_test.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_test_s2.txt")));
                                recordReader2_test.initialize(new FileSplit(aR.test));

                                int labelIndex = 4;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
                                int labelIndex2 = 320;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
                                int numClasses = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
                                int numClasses2 = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
                                //int batchSize = 150;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)
                                int batchSize2 = batchSize;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)


                                //DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader2, b_s, labelIndex2, numClasses2);

                                DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2, b_s, labelIndex2, numClasses2);
                                DataSetIterator testIter = new RecordReaderDataSetIterator(recordReader2_test, b_s, labelIndex2, numClasses2);

                                DataNormalization normalizer = new NormalizerStandardize();
                                //DataNormalization normalizer = new NormalizerMinMaxScaler(0,1);

                                normalizer.fit(trainIter);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data

                                trainIter.setPreProcessor(normalizer);
                                testIter.setPreProcessor(normalizer);

                                MultiLayerConfiguration conf = null;

                                if(aR.layers == 1) {
                                    conf = new NeuralNetConfiguration.Builder()
                                            .activation(acti)
                                            .weightInit(ini)
                                            //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                                            .updater(new Sgd(l_r))
                                            //.updater(new Nesterovs(aR.learning_rate))
                                            //.l2(1e-4)
                                            .list()
                                            .layer(new DenseLayer.Builder().nIn(320).nOut(n_n)
                                                    .build())
                                            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                                                    .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                                                    //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                                                    .nIn(n_n).nOut(3).build())
                                            //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                                            //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                                            //    .nIn(4).nOut(3).build())
                                            .build();
                                }

                                if(aR.layers == 2) {
                                    conf = new NeuralNetConfiguration.Builder()
                                            .activation(acti)
                                            .weightInit(ini)
                                            //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                                            .updater(new Sgd(l_r))
                                            //.updater(new Nesterovs(aR.learning_rate))
                                            //.l2(1e-4)
                                            .list()
                                            .layer(new DenseLayer.Builder().nIn(320).nOut(n_n)
                                                    .build())
                                            .layer(new DenseLayer.Builder().nIn(n_n).nOut(n_n)
                                                    .build())
                                            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                                                    .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                                                    //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                                                    .nIn(n_n).nOut(3).build())
                                            //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                                            //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                                            //    .nIn(4).nOut(3).build())
                                            .build();
                                }
                                if(aR.layers == 3){
                                    conf = new NeuralNetConfiguration.Builder()
                                            .activation(acti)
                                            .weightInit(ini)
                                            //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                                            .updater(new Sgd(l_r))
                                            //.updater(new Nesterovs(aR.learning_rate))
                                            //.l2(1e-4)
                                            .list()
                                            .layer(new DenseLayer.Builder().nIn(320).nOut(n_n)
                                                    .build())
                                            .layer(new DenseLayer.Builder().nIn(n_n).nOut(n_n)
                                                    .build())
                                            .layer(new DenseLayer.Builder().nIn(n_n).nOut(n_n)
                                                    .build())
                                            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                                                    .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                                                    //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                                                    .nIn(n_n).nOut(3).build())
                                            //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                                            //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                                            //    .nIn(4).nOut(3).build())
                                            .build();
                                }
                                if(aR.odir.compareTo("asd") == 0)
                                    aR.odir = "";

                                EarlyStoppingConfiguration esConf = new EarlyStoppingConfiguration.Builder()
                                        .iterationTerminationConditions(new MaxTimeIterationTerminationCondition(aR.time, TimeUnit.MINUTES))
                                        .epochTerminationConditions(new MaxEpochsTerminationCondition(ep))
                                        .scoreCalculator(new DataSetLossCalculator(testIter, true))
                                        //.scoreCalculator(new DataSetLossCalculator(trainIter, true))
                                        //.scoreCalculator(new DataSetLossCalculator(trainIter, true))
                                        .evaluateEveryNEpochs(1)
                                        .modelSaver(new LocalFileModelSaver(aR.odir))
                                        .build();

                                MultiLayerNetwork model = new MultiLayerNetwork(conf);


                                EarlyStoppingTrainer trainer = new EarlyStoppingTrainer(esConf, model, trainIter);
                                EarlyStoppingResult result = trainer.fit();


                                //System.exit(1);

                                try {
                                    MultiLayerNetwork bestModel = (MultiLayerNetwork) result.getBestModel();

                                    Evaluation eval = new Evaluation(3);
                                    testIter.reset();
                                    while (testIter.hasNext()) {
                                        DataSet t = testIter.next();

                                        INDArray features = t.getFeatures();
                                        INDArray labels = t.getLabels();
                                        INDArray predicted = bestModel.output(features, false);
                                        eval.eval(labels, predicted);
                                    }

                                    double valid_accuracy = eval.accuracy();

                                    eval = new Evaluation(3);
                                    trainIter.reset();
                                    while (trainIter.hasNext()) {
                                        DataSet t = trainIter.next();

                                        INDArray features = t.getFeatures();
                                        INDArray labels = t.getLabels();
                                        INDArray predicted = bestModel.output(features, false);
                                        eval.eval(labels, predicted);
                                    }

                                    double train_accuracy = eval.accuracy();


                                    FileWriter fw = new FileWriter(outFile, true);

                                    String outLine = train_accuracy + "\t";
                                    outLine += valid_accuracy + "\t";
                                    outLine += l_r + "\t";
                                    outLine += n_n + "\t";
                                    outLine += b_s + "\t";
                                    outLine += ep + "\t";
                                    outLine += acti.name() + "\t";
                                    outLine += ini.name() + "\t";


                                    fw.write(outLine);
                                    fw.write("\n");
                                    fw.close();

                                }catch (Exception e){

                                }


                            }
                        }
                    }
                }
            }
        }

 */
    }

    public static class ExampleDataSource implements DataSource {

        private int minibatchSize;

        private DataSetIterator testIterator = null;
        private DataSetIterator trainIterator = null;


        public ExampleDataSource() {

        }

        @Override
        public void configure(Properties properties)  {
            /*
            this.minibatchSize = Integer.parseInt(properties.getProperty("minibatchSize", "16"));
            String train_name = properties.getProperty("train_name", "16");
            String test_name = properties.getProperty("test_name", "16");
            int label_index = Integer.parseInt(properties.getProperty("label_index", "16"));

            RecordReader recordReader = new CSVRecordReader(0, ",");
            RecordReader recordReader2 = new CSVRecordReader(0, ",");
            RecordReader recordReader2_test = new CSVRecordReader(0, ",");

            try {
                recordReader2.initialize(new FileSplit(new File(train_name)));
                //recordReader2_test.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_test_s2.txt")));
                recordReader2_test.initialize(new FileSplit(new File(test_name)));
            }catch (Exception e){
                e.printStackTrace();
            }
            int labelIndex = 4;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
            int labelIndex2 = label_index;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
            int numClasses = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
            int numClasses2 = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
            //int batchSize = 150;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)
            int batchSize2 = minibatchSize;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)


            //DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader2, b_s, labelIndex2, numClasses2);

            DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2, batchSize2, labelIndex2, numClasses2);
            DataSetIterator testIter = new RecordReaderDataSetIterator(recordReader2_test, batchSize2, labelIndex2, numClasses2);

            DataNormalization normalizer = new NormalizerStandardize();
            //DataNormalization normalizer = new NormalizerMinMaxScaler(0,1);

            normalizer.fit(trainIter);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data

            trainIter.setPreProcessor(normalizer);
            testIter.setPreProcessor(normalizer);

            this.trainIterator = trainIter;
            this.testIterator = testIter;
            */

        }

        @Override
        public Object trainData() {
            try {
                return this.trainIterator;// new MnistDataSetIterator(minibatchSize, true, 12345);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object testData() {
            try {
                return this.testIterator;//MnistDataSetIterator(minibatchSize, false, 12345);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void setTestData(DataSetIterator test){

            this.testIterator = test;

        }

        public void setTrainData(DataSetIterator train){

            this.trainIterator = train;

        }

        @Override
        public Class<?> getDataType() {
            return DataSetIterator.class;
        }
    }
    @SuppressWarnings("FieldCanBeLocal")
    public static class ExampleDataSource_convolution_all implements DataSource {

        private int minibatchSize;

        private MultiDataSetIterator testIterator = null;
        private MultiDataSetIterator trainIterator = null;

        public ExampleDataSource_convolution_all() {

        }

        @Override
        public void configure(Properties properties)  {

            /*
            System.out.println("CONFIGURING!!");
            System.out.println("CONFIGURING!!");
            System.out.println("CONFIGURING!!");
            System.out.println("CONFIGURING!!");
            System.out.println("CONFIGURING!!");
            System.out.println("CONFIGURING!!");
            System.out.println("CONFIGURING!!");
            this.minibatchSize = Integer.parseInt(properties.getProperty("minibatchSize", "16"));
            String train_name = properties.getProperty("train_name", "16");
            String train_name_2 = properties.getProperty("train_name_2", "16");
            String test_name = properties.getProperty("test_name", "16");
            String test_name_2 = properties.getProperty("test_name_2", "16");
            int depth = Integer.parseInt(properties.getProperty("depth", "16"));
            int _x = Integer.parseInt(properties.getProperty("_x", "16"));
            int _y = Integer.parseInt(properties.getProperty("_y", "16"));

            int depth_tex = Integer.parseInt(properties.getProperty("depth_tex", "16"));
            int _x_tex = Integer.parseInt(properties.getProperty("_x_tex", "16"));
            int _y_tex = Integer.parseInt(properties.getProperty("_y_tex", "16"));

            int label_index_voxel = Integer.parseInt(properties.getProperty("label_index", "16"));
            int label_index_texture = Integer.parseInt(properties.getProperty("label_index_2", "16"));

            ArrayList<INDArray> trainingSet = new ArrayList<>();
            ArrayList<INDArray> trainingSet_labels = new ArrayList<>();
            ArrayList<INDArray> testSet = new ArrayList<>();
            ArrayList<INDArray> testSet_labels = new ArrayList<>();

            ArrayList<INDArray> trainingSet_2 = new ArrayList<>();
            ArrayList<INDArray> trainingSet_2_labels = new ArrayList<>();
            ArrayList<INDArray> testSet_2 = new ArrayList<>();
            ArrayList<INDArray> testSet_2_labels = new ArrayList<>();

            try {
                FileInputStream fis1 = new FileInputStream(train_name);
                FileInputStream fis2 = new FileInputStream(train_name_2);
                FileInputStream fis3 = new FileInputStream(test_name);
                FileInputStream fis4 = new FileInputStream(test_name_2);
                ObjectInputStream ois1 = new ObjectInputStream(fis1);
                ObjectInputStream ois2 = new ObjectInputStream(fis2);
                ObjectInputStream ois3 = new ObjectInputStream(fis3);
                ObjectInputStream ois4 = new ObjectInputStream(fis4);

                ArrayList<INDArray> train_in[] = null;
                ArrayList<INDArray> train_in_2[] = null;
                ArrayList<INDArray> test_in[] = null;
                ArrayList<INDArray> test_in_2[] = null;

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
            }catch (Exception e){
                e.printStackTrace();
            }
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> train_list_1 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> train_list_2 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> test_list_1 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> test_list_2 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();

            INDArray emptyArray = new NDArray();


            for(int i = 0; i < trainingSet.size(); i++){

                //System.out.println("HEREERERE " + trainingSet.size() + " " + trainingSet_labels.size());
                //trainingSet.get(i);
                //trainingSet_labels.get(i);
                train_list_1.add(new org.nd4j.common.primitives.Pair<>(trainingSet.get(i), trainingSet_labels.get(i)));

            }

            for(int i = 0; i < trainingSet_2.size(); i++){

                train_list_2.add(new org.nd4j.common.primitives.Pair<>(trainingSet_2.get(i), trainingSet_2_labels.get(i)));
                //train_list_2.add(new org.nd4j.common.primitives.Pair<>(trainingSet_2.get(i), emptyArray));

            }


            for(int i = 0; i < testSet.size(); i++){

                test_list_1.add(new org.nd4j.common.primitives.Pair<>(testSet.get(i), testSet_labels.get(i)));

            }

            for(int i = 0; i < testSet_2.size(); i++){

                test_list_2.add(new org.nd4j.common.primitives.Pair<>(testSet_2.get(i), testSet_2_labels.get(i)));
                //test_list_2.add(new org.nd4j.common.primitives.Pair<>(testSet_2.get(i), emptyArray));

            }

            //org.nd4j.common.primitives.Pair<INDArray, INDArray> haha = new org.nd4j.common.primitives.Pair<>(trainingSet.get(0), trainingSet_2_labels.get(0));

            //List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> haha_list = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            //haha_list.add(haha);



            INDArrayDataSetIterator train_1_iterator = new INDArrayDataSetIterator(train_list_1, 1);
            INDArrayDataSetIterator train_2_iterator = new INDArrayDataSetIterator(train_list_2, 1);
            INDArrayDataSetIterator test_1_iterator = new INDArrayDataSetIterator(test_list_1, 1);
            INDArrayDataSetIterator test_2_iterator = new INDArrayDataSetIterator(test_list_2, 1);
            //MultiDataSetIterator multiDataSetIterator_train = new JointMultiDataSetIterator(train_1_iterator, train_2_iterator);
            MultiDataSetIterator multiDataSetIterator_train = new JointMultiDataSetIterator(1, train_2_iterator, train_1_iterator);
            //MultiDataSetIterator multiDataSetIterator_test = new JointMultiDataSetIterator(test_1_iterator, test_2_iterator);
            MultiDataSetIterator multiDataSetIterator_test = new JointMultiDataSetIterator(1, test_2_iterator, test_1_iterator);

            this.trainIterator = multiDataSetIterator_train;
            this.testIterator = multiDataSetIterator_test;

*/

        }

        @Override
        public Object trainData() {
            try {
                //return this.trainIterator;// new MnistDataSetIterator(minibatchSize, true, 12345);
                return neuralNetworkHyperparameterOptimization_convolution_mix.train_iter_multi;// new MnistDataSetIterator(minibatchSize, true, 12345);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object testData() {
            try {

                //return this.testIterator;//MnistDataSetIterator(minibatchSize, false, 12345);
                return neuralNetworkHyperparameterOptimization_convolution_mix.test_iter_multi;//MnistDataSetIterator(minibatchSize, false, 12345);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void setTestData(MultiDataSetIterator test){

            this.testIterator = test;

        }

        public void setTrainData(MultiDataSetIterator train){

            this.trainIterator = train;

        }

        @Override
        public Class<?> getDataType() {
            return MultiDataSetIterator.class;
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    public static class ExampleDataSource_convolution_voxel implements DataSource {

        private int minibatchSize;

        private DataSetIterator testIterator = null;
        private DataSetIterator trainIterator = null;

        public ExampleDataSource_convolution_voxel() {

        }

        @Override
        public void configure(Properties properties)  {

/*
            this.minibatchSize = Integer.parseInt(properties.getProperty("minibatchSize", "16"));
            String train_name = properties.getProperty("train_name", "16");
            String train_name_2 = properties.getProperty("train_name_2", "16");
            String test_name = properties.getProperty("test_name", "16");
            String test_name_2 = properties.getProperty("test_name_2", "16");
            int depth = Integer.parseInt(properties.getProperty("depth", "16"));
            int _x = Integer.parseInt(properties.getProperty("_x", "16"));
            int _y = Integer.parseInt(properties.getProperty("_y", "16"));

            int depth_tex = Integer.parseInt(properties.getProperty("depth_tex", "16"));
            int _x_tex = Integer.parseInt(properties.getProperty("_x_tex", "16"));
            int _y_tex = Integer.parseInt(properties.getProperty("_y_tex", "16"));

            int label_index_voxel = Integer.parseInt(properties.getProperty("label_index", "16"));
            int label_index_texture = Integer.parseInt(properties.getProperty("label_index_2", "16"));

            ArrayList<INDArray> trainingSet = new ArrayList<>();
            ArrayList<INDArray> trainingSet_labels = new ArrayList<>();
            ArrayList<INDArray> testSet = new ArrayList<>();
            ArrayList<INDArray> testSet_labels = new ArrayList<>();

            ArrayList<INDArray> trainingSet_2 = new ArrayList<>();
            ArrayList<INDArray> trainingSet_2_labels = new ArrayList<>();
            ArrayList<INDArray> testSet_2 = new ArrayList<>();
            ArrayList<INDArray> testSet_2_labels = new ArrayList<>();

            try {
                FileInputStream fis1 = new FileInputStream(train_name);
                FileInputStream fis2 = new FileInputStream(train_name_2);
                FileInputStream fis3 = new FileInputStream(test_name);
                FileInputStream fis4 = new FileInputStream(test_name_2);
                ObjectInputStream ois1 = new ObjectInputStream(fis1);
                ObjectInputStream ois2 = new ObjectInputStream(fis2);
                ObjectInputStream ois3 = new ObjectInputStream(fis3);
                ObjectInputStream ois4 = new ObjectInputStream(fis4);

                ArrayList<INDArray> train_in[] = null;
                ArrayList<INDArray> train_in_2[] = null;
                ArrayList<INDArray> test_in[] = null;
                ArrayList<INDArray> test_in_2[] = null;

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
            }catch (Exception e){
                e.printStackTrace();
            }
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> train_list_1 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> train_list_2 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> test_list_1 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> test_list_2 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();

            for(int i = 0; i < trainingSet.size(); i++){

                //System.out.println("HEREERERE " + trainingSet.size() + " " + trainingSet_labels.size());
                //trainingSet.get(i);
                //trainingSet_labels.get(i);
                train_list_1.add(new org.nd4j.common.primitives.Pair<>(trainingSet.get(i), trainingSet_labels.get(i)));

            }

            for(int i = 0; i < trainingSet.size(); i++){

                train_list_2.add(new org.nd4j.common.primitives.Pair<>(trainingSet_2.get(i), trainingSet_2_labels.get(i)));

            }


            for(int i = 0; i < testSet.size(); i++){

                test_list_1.add(new org.nd4j.common.primitives.Pair<>(testSet.get(i), testSet_labels.get(i)));

            }

            for(int i = 0; i < testSet.size(); i++){

                test_list_2.add(new org.nd4j.common.primitives.Pair<>(testSet_2.get(i), testSet_2_labels.get(i)));

            }



            //org.nd4j.common.primitives.Pair<INDArray, INDArray> haha = new org.nd4j.common.primitives.Pair<>(trainingSet.get(0), trainingSet_2_labels.get(0));

            //List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> haha_list = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            //haha_list.add(haha);

            INDArrayDataSetIterator train_1_iterator = new INDArrayDataSetIterator(train_list_1, 1);
            INDArrayDataSetIterator train_2_iterator = new INDArrayDataSetIterator(train_list_2, 1);
            INDArrayDataSetIterator test_1_iterator = new INDArrayDataSetIterator(test_list_1, 1);
            INDArrayDataSetIterator test_2_iterator = new INDArrayDataSetIterator(test_list_2, 1);

            MultiDataSetIterator multiDataSetIterator_train = new JointMultiDataSetIterator(train_1_iterator, train_2_iterator);
            MultiDataSetIterator multiDataSetIterator_test = new JointMultiDataSetIterator(test_1_iterator, test_2_iterator);


            this.trainIterator = train_1_iterator;
            this.testIterator = test_1_iterator;
            /*
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


//recordReader.initialize(new FileSplit(new File("/home/koomikko/Documents/iris.csv")));
            //recordReader2.initialize(new FileSplit(new File("/home/koomikko/Documents/species2.csv")));
            //recordReader2.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_train_s2.txt")));

            try {
                recordReader2.initialize(new FileSplit(new File(train_name)));
                //recordReader2_test.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_test_s2.txt")));
                recordReader2_test.initialize(new FileSplit(new File(test_name)));

                rr_320.initialize(new FileSplit(new File(train_name_2)));
                rr_320_test.initialize(new FileSplit(new File(test_name_2)));

            }catch (Exception e){
                e.printStackTrace();
            }
            int labelIndex = 4;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
            int labelIndex2 = label_index_voxel;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
            int numClasses = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
            int numClasses2 = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
            //int batchSize = 150;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)
            int batchSize2 = minibatchSize;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)

            //batchSize2 = 1;

            //DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader2,batchSize2,labelIndex2,numClasses2);
            //DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2,batchSize2,labelIndex2,numClasses2);
            DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2, batchSize2, labelIndex2, numClasses2);
            DataSetIterator trainIter_2 = new RecordReaderDataSetIterator(rr_320, batchSize2, label_index_texture, numClasses2);
            DataSetIterator testIter = new RecordReaderDataSetIterator(recordReader2_test, batchSize2, labelIndex2, numClasses2);
            DataSetIterator testIter_2 = new RecordReaderDataSetIterator(rr_320_test, batchSize2, label_index_texture, numClasses2);

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


                trainingSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth, _y, _x));
                //trainingSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));

                trainingSet_labels.add(ds.getLabels());

            }

            while (trainIter_2.hasNext()) {
                DataSet ds = trainIter_2.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());

                trainingSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth_tex, _y_tex, _x_tex));
                //trainingSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth_tex, _y_tex, _x_tex));
                //trainingSet_2.add(ds.getFeatures());
                trainingSet_2_labels.add(ds.getLabels());
                counti2++;
                //System.out.println(counti2);
            }

            while (testIter.hasNext()) {

                DataSet ds = testIter.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());

                testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth, _y, _x));
                //testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth, _y, _x, 1));
                testSet_labels.add(ds.getLabels());

            }

            while (testIter_2.hasNext()) {

                DataSet ds = testIter_2.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());

                testSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth_tex, _y_tex, _x_tex));
                //testSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth_tex, _y_tex, _x_tex));
                //testSet_2.add(ds.getFeatures());
                testSet_2_labels.add(ds.getLabels());

            }

            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> train_list_1 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> train_list_2 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> test_list_1 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> test_list_2 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();

            for(int i = 0; i < trainingSet.size(); i++){

                //System.out.println("HEREERERE " + trainingSet.size() + " " + trainingSet_labels.size());
                //trainingSet.get(i);
                //trainingSet_labels.get(i);
                train_list_1.add(new org.nd4j.common.primitives.Pair<>(trainingSet.get(i), trainingSet_labels.get(i)));

            }

            for(int i = 0; i < trainingSet.size(); i++){

                train_list_2.add(new org.nd4j.common.primitives.Pair<>(trainingSet_2.get(i), trainingSet_2_labels.get(i)));

            }


            for(int i = 0; i < testSet.size(); i++){

                test_list_1.add(new org.nd4j.common.primitives.Pair<>(testSet.get(i), testSet_labels.get(i)));

            }

            for(int i = 0; i < testSet.size(); i++){

                test_list_2.add(new org.nd4j.common.primitives.Pair<>(testSet_2.get(i), testSet_2_labels.get(i)));

            }



            //org.nd4j.common.primitives.Pair<INDArray, INDArray> haha = new org.nd4j.common.primitives.Pair<>(trainingSet.get(0), trainingSet_2_labels.get(0));

            //List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> haha_list = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            //haha_list.add(haha);

            INDArrayDataSetIterator train_1_iterator = new INDArrayDataSetIterator(train_list_1, 1);
            INDArrayDataSetIterator train_2_iterator = new INDArrayDataSetIterator(train_list_2, 1);
            INDArrayDataSetIterator test_1_iterator = new INDArrayDataSetIterator(test_list_1, 1);
            INDArrayDataSetIterator test_2_iterator = new INDArrayDataSetIterator(test_list_2, 1);

            MultiDataSetIterator multiDataSetIterator_train = new JointMultiDataSetIterator(train_1_iterator, train_2_iterator);
            MultiDataSetIterator multiDataSetIterator_test = new JointMultiDataSetIterator(test_1_iterator, test_2_iterator);


            this.trainIterator = train_1_iterator;
            this.testIterator = test_1_iterator;

            */

        }

        @Override
        public Object trainData() {
            try {
                //return this.trainIterator;// new MnistDataSetIterator(minibatchSize, true, 12345);
                return neuralNetworkHyperparameterOptimization_convolution_mix.train_iter;// new MnistDataSetIterator(minibatchSize, true, 12345);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object testData() {
            try {
                //return this.testIterator;//MnistDataSetIterator(minibatchSize, false, 12345);
                return neuralNetworkHyperparameterOptimization_convolution_mix.test_iter;//MnistDataSetIterator(minibatchSize, false, 12345);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void setTestData(DataSetIterator test){

            this.testIterator = test;

        }

        public void setTrainData(DataSetIterator train){

            this.trainIterator = train;

        }

        @Override
        public Class<?> getDataType() {
            return DataSetIterator.class;
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    public static class ExampleDataSource_convolution_texture implements DataSource {

        private int minibatchSize;

        private DataSetIterator testIterator = null;
        private DataSetIterator trainIterator = null;

        public ExampleDataSource_convolution_texture() {

        }

        @Override
        public void configure(Properties properties)  {

/*
            this.minibatchSize = Integer.parseInt(properties.getProperty("minibatchSize", "16"));
            String train_name = properties.getProperty("train_name", "16");
            String train_name_2 = properties.getProperty("train_name_2", "16");
            String test_name = properties.getProperty("test_name", "16");
            String test_name_2 = properties.getProperty("test_name_2", "16");
            int depth = Integer.parseInt(properties.getProperty("depth", "16"));
            int _x = Integer.parseInt(properties.getProperty("_x", "16"));
            int _y = Integer.parseInt(properties.getProperty("_y", "16"));

            int depth_tex = Integer.parseInt(properties.getProperty("depth_tex", "16"));
            int _x_tex = Integer.parseInt(properties.getProperty("_x_tex", "16"));
            int _y_tex = Integer.parseInt(properties.getProperty("_y_tex", "16"));

            int label_index_voxel = Integer.parseInt(properties.getProperty("label_index", "16"));
            int label_index_texture = Integer.parseInt(properties.getProperty("label_index_2", "16"));

            ArrayList<INDArray> trainingSet = new ArrayList<>();
            ArrayList<INDArray> trainingSet_labels = new ArrayList<>();
            ArrayList<INDArray> testSet = new ArrayList<>();
            ArrayList<INDArray> testSet_labels = new ArrayList<>();

            ArrayList<INDArray> trainingSet_2 = new ArrayList<>();
            ArrayList<INDArray> trainingSet_2_labels = new ArrayList<>();
            ArrayList<INDArray> testSet_2 = new ArrayList<>();
            ArrayList<INDArray> testSet_2_labels = new ArrayList<>();

            try {
                FileInputStream fis1 = new FileInputStream(train_name);
                FileInputStream fis2 = new FileInputStream(train_name_2);
                FileInputStream fis3 = new FileInputStream(test_name);
                FileInputStream fis4 = new FileInputStream(test_name_2);
                ObjectInputStream ois1 = new ObjectInputStream(fis1);
                ObjectInputStream ois2 = new ObjectInputStream(fis2);
                ObjectInputStream ois3 = new ObjectInputStream(fis3);
                ObjectInputStream ois4 = new ObjectInputStream(fis4);

                ArrayList<INDArray> train_in[] = null;
                ArrayList<INDArray> train_in_2[] = null;
                ArrayList<INDArray> test_in[] = null;
                ArrayList<INDArray> test_in_2[] = null;

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
            }catch (Exception e){
                e.printStackTrace();
            }
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> train_list_1 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> train_list_2 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> test_list_1 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> test_list_2 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();

            for(int i = 0; i < trainingSet.size(); i++){

                //System.out.println("HEREERERE " + trainingSet.size() + " " + trainingSet_labels.size());
                //trainingSet.get(i);
                //trainingSet_labels.get(i);
                train_list_1.add(new org.nd4j.common.primitives.Pair<>(trainingSet.get(i), trainingSet_labels.get(i)));

            }

            for(int i = 0; i < trainingSet.size(); i++){

                train_list_2.add(new org.nd4j.common.primitives.Pair<>(trainingSet_2.get(i), trainingSet_2_labels.get(i)));

            }


            for(int i = 0; i < testSet.size(); i++){

                test_list_1.add(new org.nd4j.common.primitives.Pair<>(testSet.get(i), testSet_labels.get(i)));

            }

            for(int i = 0; i < testSet.size(); i++){

                test_list_2.add(new org.nd4j.common.primitives.Pair<>(testSet_2.get(i), testSet_2_labels.get(i)));

            }



            //org.nd4j.common.primitives.Pair<INDArray, INDArray> haha = new org.nd4j.common.primitives.Pair<>(trainingSet.get(0), trainingSet_2_labels.get(0));

            //List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> haha_list = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            //haha_list.add(haha);

            INDArrayDataSetIterator train_1_iterator = new INDArrayDataSetIterator(train_list_1, 1);
            INDArrayDataSetIterator train_2_iterator = new INDArrayDataSetIterator(train_list_2, 1);
            INDArrayDataSetIterator test_1_iterator = new INDArrayDataSetIterator(test_list_1, 1);
            INDArrayDataSetIterator test_2_iterator = new INDArrayDataSetIterator(test_list_2, 1);

            MultiDataSetIterator multiDataSetIterator_train = new JointMultiDataSetIterator(train_1_iterator, train_2_iterator);
            MultiDataSetIterator multiDataSetIterator_test = new JointMultiDataSetIterator(test_1_iterator, test_2_iterator);


            this.trainIterator = train_1_iterator;
            this.testIterator = test_1_iterator;
            /*
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


//recordReader.initialize(new FileSplit(new File("/home/koomikko/Documents/iris.csv")));
            //recordReader2.initialize(new FileSplit(new File("/home/koomikko/Documents/species2.csv")));
            //recordReader2.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_train_s2.txt")));

            try {
                recordReader2.initialize(new FileSplit(new File(train_name)));
                //recordReader2_test.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_test_s2.txt")));
                recordReader2_test.initialize(new FileSplit(new File(test_name)));

                rr_320.initialize(new FileSplit(new File(train_name_2)));
                rr_320_test.initialize(new FileSplit(new File(test_name_2)));

            }catch (Exception e){
                e.printStackTrace();
            }
            int labelIndex = 4;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
            int labelIndex2 = label_index_voxel;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
            int numClasses = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
            int numClasses2 = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
            //int batchSize = 150;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)
            int batchSize2 = minibatchSize;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)

            //batchSize2 = 1;

            //DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader2,batchSize2,labelIndex2,numClasses2);
            //DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2,batchSize2,labelIndex2,numClasses2);
            DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2, batchSize2, labelIndex2, numClasses2);
            DataSetIterator trainIter_2 = new RecordReaderDataSetIterator(rr_320, batchSize2, label_index_texture, numClasses2);
            DataSetIterator testIter = new RecordReaderDataSetIterator(recordReader2_test, batchSize2, labelIndex2, numClasses2);
            DataSetIterator testIter_2 = new RecordReaderDataSetIterator(rr_320_test, batchSize2, label_index_texture, numClasses2);

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


                trainingSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth, _y, _x));
                //trainingSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));

                trainingSet_labels.add(ds.getLabels());

            }

            while (trainIter_2.hasNext()) {
                DataSet ds = trainIter_2.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());

                trainingSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth_tex, _y_tex, _x_tex));
                //trainingSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth_tex, _y_tex, _x_tex));
                //trainingSet_2.add(ds.getFeatures());
                trainingSet_2_labels.add(ds.getLabels());
                counti2++;
                //System.out.println(counti2);
            }

            while (testIter.hasNext()) {

                DataSet ds = testIter.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());

                testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth, _y, _x));
                //testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //testSet.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth, _y, _x, 1));
                testSet_labels.add(ds.getLabels());

            }

            while (testIter_2.hasNext()) {

                DataSet ds = testIter_2.next();
                //ds.setFeatures(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth, _y, _x));
                //System.out.println("RANK!!!!  " + ds.getFeatures().rank());

                testSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, depth_tex, _y_tex, _x_tex));
                //testSet_2.add(ds.getFeatures().reshape(ds.getLabels().length() / numClasses, 1, depth_tex, _y_tex, _x_tex));
                //testSet_2.add(ds.getFeatures());
                testSet_2_labels.add(ds.getLabels());

            }

            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> train_list_1 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> train_list_2 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> test_list_1 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> test_list_2 = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();

            for(int i = 0; i < trainingSet.size(); i++){

                //System.out.println("HEREERERE " + trainingSet.size() + " " + trainingSet_labels.size());
                //trainingSet.get(i);
                //trainingSet_labels.get(i);
                train_list_1.add(new org.nd4j.common.primitives.Pair<>(trainingSet.get(i), trainingSet_labels.get(i)));

            }

            for(int i = 0; i < trainingSet.size(); i++){

                train_list_2.add(new org.nd4j.common.primitives.Pair<>(trainingSet_2.get(i), trainingSet_2_labels.get(i)));

            }


            for(int i = 0; i < testSet.size(); i++){

                test_list_1.add(new org.nd4j.common.primitives.Pair<>(testSet.get(i), testSet_labels.get(i)));

            }

            for(int i = 0; i < testSet.size(); i++){

                test_list_2.add(new org.nd4j.common.primitives.Pair<>(testSet_2.get(i), testSet_2_labels.get(i)));

            }



            //org.nd4j.common.primitives.Pair<INDArray, INDArray> haha = new org.nd4j.common.primitives.Pair<>(trainingSet.get(0), trainingSet_2_labels.get(0));

            //List<org.nd4j.common.primitives.Pair<INDArray, INDArray>> haha_list = new ArrayList<org.nd4j.common.primitives.Pair<INDArray, INDArray>>();
            //haha_list.add(haha);

            INDArrayDataSetIterator train_1_iterator = new INDArrayDataSetIterator(train_list_1, 1);
            INDArrayDataSetIterator train_2_iterator = new INDArrayDataSetIterator(train_list_2, 1);
            INDArrayDataSetIterator test_1_iterator = new INDArrayDataSetIterator(test_list_1, 1);
            INDArrayDataSetIterator test_2_iterator = new INDArrayDataSetIterator(test_list_2, 1);

            MultiDataSetIterator multiDataSetIterator_train = new JointMultiDataSetIterator(train_1_iterator, train_2_iterator);
            MultiDataSetIterator multiDataSetIterator_test = new JointMultiDataSetIterator(test_1_iterator, test_2_iterator);


            this.trainIterator = train_1_iterator;
            this.testIterator = test_1_iterator;

            */

        }

        @Override
        public Object trainData() {
            try {
                //return this.trainIterator;// new MnistDataSetIterator(minibatchSize, true, 12345);
                return neuralNetworkHyperparameterOptimization_convolution_mix.train_iter_2;// new MnistDataSetIterator(minibatchSize, true, 12345);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object testData() {
            try {
                //return this.testIterator;//MnistDataSetIterator(minibatchSize, false, 12345);
                return neuralNetworkHyperparameterOptimization_convolution_mix.test_iter_2;//MnistDataSetIterator(minibatchSize, false, 12345);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void setTestData(DataSetIterator test){

            this.testIterator = test;

        }

        public void setTrainData(DataSetIterator train){

            this.trainIterator = train;

        }

        @Override
        public Class<?> getDataType() {
            return DataSetIterator.class;
        }
    }


}


