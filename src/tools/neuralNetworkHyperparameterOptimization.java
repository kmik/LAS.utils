package tools;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;

import org.deeplearning4j.arbiter.ComputationGraphSpace;
import org.deeplearning4j.arbiter.MultiLayerSpace;
import org.deeplearning4j.arbiter.conf.updater.AdamSpace;
import org.deeplearning4j.arbiter.layers.ConvolutionLayerSpace;
import org.deeplearning4j.arbiter.layers.DenseLayerSpace;
import org.deeplearning4j.arbiter.layers.OutputLayerSpace;
import org.deeplearning4j.arbiter.optimize.api.data.DataSource;
import org.deeplearning4j.arbiter.optimize.parameter.discrete.DiscreteParameterSpace;
import org.deeplearning4j.arbiter.scoring.impl.EvaluationScoreFunction;
import org.deeplearning4j.arbiter.task.MultiLayerNetworkTaskCreator;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;

import org.deeplearning4j.arbiter.optimize.api.CandidateGenerator;

import org.deeplearning4j.arbiter.optimize.api.ParameterSpace;

import org.deeplearning4j.arbiter.optimize.api.saving.ResultSaver;
import org.deeplearning4j.arbiter.optimize.api.score.ScoreFunction;
import org.deeplearning4j.arbiter.optimize.api.termination.MaxTimeCondition;
import org.deeplearning4j.arbiter.optimize.api.termination.TerminationCondition;
import org.deeplearning4j.arbiter.optimize.config.OptimizationConfiguration;
import org.deeplearning4j.arbiter.optimize.generator.RandomSearchGenerator;
import org.deeplearning4j.arbiter.optimize.parameter.continuous.ContinuousParameterSpace;
import org.deeplearning4j.arbiter.optimize.parameter.integer.IntegerParameterSpace;
import org.deeplearning4j.arbiter.optimize.runner.IOptimizationRunner;
import org.deeplearning4j.arbiter.optimize.runner.LocalOptimizationRunner;
import org.deeplearning4j.arbiter.saver.local.FileModelSaver;

import org.nd4j.evaluation.classification.Evaluation.Metric;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import org.nd4j.evaluation.classification.Evaluation;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;

import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.argumentReader;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class neuralNetworkHyperparameterOptimization {

    private static Logger log = LoggerFactory.getLogger(neuralNetWorkTest_3d_treespecies.class);

    public neuralNetworkHyperparameterOptimization(argumentReader aR) throws  Exception {


        int batchSize = (int) Math.pow(2, 5);
        batchSize = 128;

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
            String outLinePred = Integer.toString(pred.get(0)) + ",";
            String outLineObs = Integer.toString(obs.get(0)) + ",";

            System.out.println(Arrays.toString(pred.toArray()));


            for (int i = 1; i < pred.size(); i++) {

                outLinePred += Integer.toString(pred.get(i)) + ",";
                outLineObs += Integer.toString(obs.get(i)) + ",";


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
        LogManager.getRootLogger().setLevel(Level.OFF);


/*
        DataSet allData = iterator.next();
        System.out.println("got here");
        allData.shuffle();
        SplitTestAndTrain testAndTrain = allData.splitTestAndTrain(0.75);  //Use 65% of data for training

        DataSet trainingData = testAndTrain.getTrain();
        DataSet testData = testAndTrain.getTest();

        DataNormalization normalizer = new NormalizerStandardize();
        normalizer.fit(trainingData);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data
        normalizer.transform(trainingData);     //Apply normalization to the training data
        normalizer.transform(testData);         //Apply normalization to the test data. This is using statistics calculated from the *training* set


        DataNormalization normalizer2 = new NormalizerStandardize();
        normalizer2.fit(trainIter);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data
        //normalizer2.transform(trainIter);     //Apply normalization to the training data
        //normalizer2.transform(testIter);         //Apply normalization to the test data. This is using statistics calculated from the *training* set
*/

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
        int labelIndex2 = aR.label_index;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
        int numClasses = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
        int numClasses2 = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
        //int batchSize = 150;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)
        int batchSize2 = batchSize;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)


        //DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader2, b_s, labelIndex2, numClasses2);

        DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2, batchSize2, labelIndex2, numClasses2);
        DataSetIterator testIter = new RecordReaderDataSetIterator(recordReader2_test, batchSize2, labelIndex2, numClasses2);

        DataNormalization normalizer = new NormalizerStandardize();
        //DataNormalization normalizer = new NormalizerMinMaxScaler(0,1);

        normalizer.fit(trainIter);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data

        trainIter.setPreProcessor(normalizer);
        testIter.setPreProcessor(normalizer);

        MultiLayerConfiguration conf = null;


        String baseSaveDirectory = aR.odir;
        File f = new File(baseSaveDirectory);
        if(f.exists()) f.delete();
        f.mkdir();
        ResultSaver modelSaver = new FileModelSaver(baseSaveDirectory);

        ParameterSpace<Double> learningRateHyperparam = new ContinuousParameterSpace(0.00001, 0.1);  //Values will be generated uniformly at random between 0.0001 and 0.1 (inclusive)
        ParameterSpace<Integer> layerSizeHyperparam = new IntegerParameterSpace(16,256);            //Integer values will be generated uniformly at random between 16 and 256 (inclusive)
        ParameterSpace<Integer> layerSizeHyperparam2 = new IntegerParameterSpace(16,256);            //Integer values will be generated uniformly at random between 16 and 256 (inclusive)
        ParameterSpace<Double> l2sizeHyperparam = new ContinuousParameterSpace(1e-9, 1e-2);

        //ParameterSpace<Integer> numEpochs = new IntegerParameterSpace(15);
        DiscreteParameterSpace<Activation> activationSpace = new DiscreteParameterSpace(new Activation[]{Activation.TANH,Activation.SIGMOID,Activation.RELU});
        DiscreteParameterSpace<WeightInit> weightInitSpace = new DiscreteParameterSpace(new WeightInit[]{WeightInit.NORMAL, WeightInit.XAVIER, WeightInit.RELU});

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

        MultiLayerSpace mls =
                new MultiLayerSpace.Builder()
                .activation(activationSpace)
                .weightInit(weightInitSpace)
                        .numEpochs(30)
                .updater(new AdamSpace(learningRateHyperparam))
                //.updater(new Nesterovs(aR.learning_rate))
                .l2(l2sizeHyperparam)
                .layer(new DenseLayerSpace.Builder().nIn(aR.label_index).nOut(layerSizeHyperparam)
                        .build())
                //.layer(new DropoutLayer(0.75))
                .layer(new DenseLayerSpace.Builder().nIn(layerSizeHyperparam).nOut(layerSizeHyperparam2)
                        .build())
                .layer( new OutputLayerSpace.Builder()
                        .lossFunction(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(layerSizeHyperparam2).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();

        ComputationGraphSpace cgs = new ComputationGraphSpace.Builder()
                .activation(activationSpace)
                .weightInit(weightInitSpace)
                .numEpochs(50)
                .updater(new AdamSpace(learningRateHyperparam))
                .l2(l2sizeHyperparam)
                .layer( "convo1", new ConvolutionLayerSpace.Builder().kernelSize(3,3,3).stride(1,1,1).build())
                .layer("dense1", new DenseLayerSpace.Builder().nOut(5).build(), "convo1")
                .layer("out", new OutputLayerSpace.Builder().nIn(5).nOut(3).activation(Activation.SOFTMAX).lossFunction(LossFunctions.LossFunction.MCXENT).build(), "dense1")
                .build();

        ScoreFunction scoreFunction = new EvaluationScoreFunction(Metric.F1);

        CandidateGenerator candidateGenerator =new RandomSearchGenerator(mls);

        //DataSet ds = new DataSet();

        ExampleDataSource ds = new ExampleDataSource();

        ds.setTestData(testIter);
        ds.setTrainData(trainIter);

        Properties dataSourceProperties = new Properties();
        dataSourceProperties.setProperty("minibatchSize", Integer.toString(batchSize));
        dataSourceProperties.setProperty("label_index", Integer.toString(aR.label_index));
        dataSourceProperties.setProperty("train_name", aR.train.getAbsolutePath());
        dataSourceProperties.setProperty("test_name", aR.test.getAbsolutePath());



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


        IOptimizationRunner runner =new LocalOptimizationRunner(configuration,new MultiLayerNetworkTaskCreator());



        runner.removeAllListeners();

        //Start the hyperparameter optimization
        runner.execute();

        System.out.println("SUCCESS!");
        System.exit(1);

        /** Learning rate */
        double[] learningRates = new double[]{0.1, 0.01, 0.001};
        /** Neurons per layer */
        int[] neurons = new int[]{5, 10, 15, 25, 30, 35, 40, 75, 125, 200};
        /** Batch size */
        int[] batchSizes = new int[]{(int) Math.pow(2, 4), (int) Math.pow(2, 5), (int) Math.pow(2, 6), (int) Math.pow(2, 7), (int) Math.pow(2, 8), (int) Math.pow(2, 9)};
        /** Epoch count */
        int[] n_epochs = new int[]{25, 50, 100};
        /** Activation function */
        Activation[] activations = new Activation[]{Activation.SIGMOID, Activation.TANH, Activation.RELU};
        /** Weight initialization */
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

    @SuppressWarnings("FieldCanBeLocal")
    public static class ExampleDataSource implements DataSource {
        private int minibatchSize;

        private DataSetIterator testIterator = null;
        private DataSetIterator trainIterator = null;


        public ExampleDataSource() {

        }

        @Override
        public void configure(Properties properties)  {
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
}


