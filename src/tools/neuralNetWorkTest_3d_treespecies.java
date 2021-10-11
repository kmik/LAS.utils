package tools;

import org.apache.log4j.BasicConfigurator;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;

import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.saver.LocalFileModelSaver;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
import org.deeplearning4j.nn.conf.CacheMode;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;

import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.DropoutLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.EvaluativeListener;

import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import org.nd4j.evaluation.IEvaluation;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.argumentReader;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

import static org.deeplearning4j.optimize.api.InvocationType.EPOCH_END;

public class neuralNetWorkTest_3d_treespecies {

    private static Logger log = LoggerFactory.getLogger(neuralNetWorkTest_3d_treespecies.class);
    //private final BatchNormalization normalizer_batch;

    public neuralNetWorkTest_3d_treespecies(argumentReader aR) throws  Exception{

        int batchSize = (int)Math.pow(2,6);

        int n_epoch = 25;

        int n_in = 160;

        File outFile = new File(aR.output);

        if(!outFile.exists())
            outFile.createNewFile();

        if(aR.model != null){

            MultiLayerNetwork model = MultiLayerNetwork.load(aR.model, true);

            //System.out.println(model.conf().toString());
              //      System.exit(1);

            RecordReader recordReader2 = new CSVRecordReader(0,",");
            RecordReader recordReader2_test = new CSVRecordReader(0,",");
            recordReader2.initialize(new FileSplit(aR.train));
            recordReader2_test.initialize(new FileSplit(aR.test));

            int labelIndex2 = n_in;
            int batchSize2 =  batchSize;
            int numClasses2 = 3;

            DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2,batchSize2,labelIndex2,numClasses2);
            DataSetIterator testIter = new RecordReaderDataSetIterator(recordReader2_test,batchSize2,labelIndex2,numClasses2);

            DataNormalization normalizer = new NormalizerStandardize();
            //DataNormalization normalizer = new NormalizerMinMaxScaler(0,1);
            normalizer.fit(trainIter);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data

            trainIter.setPreProcessor(normalizer);
            testIter.setPreProcessor(normalizer);

            Evaluation  eval = new Evaluation(3);

            ArrayList<Integer> pred = new ArrayList<>();
            ArrayList<Integer> obs = new ArrayList<>();

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

            FileWriter fw = new FileWriter(outFile, true);
            String outLinePred = pred.get(0) + ",";
            String outLineObs = obs.get(0) + ",";

            System.out.println(Arrays.toString(pred.toArray()));


            for(int i = 1; i < pred.size(); i++){

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

        RecordReader recordReader = new CSVRecordReader(0,",");
        RecordReader recordReader2 = new CSVRecordReader(0,",");
        RecordReader recordReader2_test = new CSVRecordReader(0,",");

        //recordReader.initialize(new FileSplit(new File("/home/koomikko/Documents/iris.csv")));
        //recordReader2.initialize(new FileSplit(new File("/home/koomikko/Documents/species2.csv")));
        //recordReader2.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_train_s2.txt")));
        recordReader2.initialize(new FileSplit(aR.train));
        //recordReader2_test.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_test_s2.txt")));
        recordReader2_test.initialize(new FileSplit(aR.test));



        int labelIndex = 4;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
        int labelIndex2 = n_in;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
        int numClasses = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
        int numClasses2 = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
        //int batchSize = 150;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)
        int batchSize2 =  batchSize;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)

        DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader2,batchSize2,labelIndex2,numClasses2);


        DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2,batchSize2,labelIndex2,numClasses2);
        DataSetIterator testIter = new RecordReaderDataSetIterator(recordReader2_test,batchSize2,labelIndex2,numClasses2);

        DataNormalization normalizer = new NormalizerStandardize();
        //DataNormalization normalizer = new NormalizerMinMaxScaler(0,1);

        normalizer.fit(trainIter);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data

        trainIter.setPreProcessor(normalizer);
        testIter.setPreProcessor(normalizer);

        if(false){

            while(trainIter.hasNext()){
                //normalizer.transform(trainIter.next());
            }
            while(testIter.hasNext()){
                //normalizer.transform(testIter.next());
            }
        }
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

        /* THIS IS GOOD MODEL FOR 40 slices 50% cutoff (ALL FEATURES) */


/*
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .activation(Activation.RELU)
                .weightInit(WeightInit.RELU)
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Sgd(aR.learning_rate))
                //.updater(new Nesterovs(aR.learning_rate))
                //.l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(320).nOut(29)
                        .build())
                .layer(new DenseLayer.Builder().nIn(29).nOut(29)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(29).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();

        */


        /* "OPTIMAL (OR AT LEAST VERY NICE!!!) FOR 320 FEATURES */

        MultiLayerConfiguration conf22 = new NeuralNetConfiguration.Builder()
                .activation(Activation.RELU)
                .weightInit(WeightInit.NORMAL)
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(0.001)).cacheMode(CacheMode.DEVICE)
                //.updater(new Nesterovs(aR.learning_rate))
                //.l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(n_in).nOut(25)
                        .build())
                .layer(new DropoutLayer(0.75))
                .layer(new DenseLayer.Builder().nIn(25).nOut(9)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(9).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();

        MultiLayerConfiguration conf = null;

        /* Arbiter optimized 320 features, 64 batch size */
        MultiLayerConfiguration conf_merged_arbiter = new NeuralNetConfiguration.Builder()
                .activation(Activation.RELU)
                .weightInit(WeightInit.RELU)
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(.0016085763024211916)).cacheMode(CacheMode.DEVICE)
                //.updater(new Nesterovs(aR.learning_rate))
                .l2(1.9127220880647207E-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(n_in).nOut(247)
                        .build())
                //.layer(new DropoutLayer(0.75))
                .layer(new DenseLayer.Builder().nIn(247).nOut(62)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(62).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();

        /* Arbiter optimized 320 features, 128 batch size */
        MultiLayerConfiguration conf123456 = new NeuralNetConfiguration.Builder()
                .activation(Activation.TANH)
                .weightInit(WeightInit.RELU)
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(0.009142468203873783)).cacheMode(CacheMode.DEVICE)
                //.updater(new Nesterovs(aR.learning_rate))
                .l2(1.8885916174400034E-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(n_in).nOut(204)
                        .build())
                //.layer(new DropoutLayer(0.75))
                .layer(new DenseLayer.Builder().nIn(204).nOut(78)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(78).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();

        /* GOOD FOR 160 VOXELS */

        MultiLayerConfiguration conf111 = new NeuralNetConfiguration.Builder()
                .activation(Activation.RELU)
                .weightInit(WeightInit.NORMAL)
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(0.001)).cacheMode(CacheMode.DEVICE)
                //.updater(new Nesterovs(aR.learning_rate))
                //.l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(n_in).nOut(20)
                        .build())
                .layer(new DropoutLayer(0.75))
                .layer(new DenseLayer.Builder().nIn(20).nOut(7)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(7).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();

        MultiLayerConfiguration conf_voxel_arbiter = new NeuralNetConfiguration.Builder()
                .activation(Activation.SIGMOID)
                .weightInit(WeightInit.RELU)
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(0.00771704227396059)).cacheMode(CacheMode.DEVICE)
                //.updater(new Nesterovs(aR.learning_rate))
                .l2(6.76041810669577E-5)
                .list()
                .layer(new DenseLayer.Builder().nIn(n_in).nOut(176)
                        .build())
                //.layer(new DropoutLayer(0.75))
                .layer(new DenseLayer.Builder().nOut(95)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(95).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();

        MultiLayerConfiguration conf_texture_arbiter = new NeuralNetConfiguration.Builder()
                .activation(Activation.RELU)
                .weightInit(WeightInit.RELU)
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(0.0036360488949355244)).cacheMode(CacheMode.DEVICE)
                //.updater(new Nesterovs(aR.learning_rate))
                .l2(0.004094652439635985)
                .list()
                .layer(new DenseLayer.Builder().nIn(n_in).nOut(245)
                        .build())
                //.layer(new DropoutLayer(0.75))
                .layer(new DenseLayer.Builder().nOut(129)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(129).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();

        conf = conf_voxel_arbiter;

        /* GOOD FOR 160 texture */

        MultiLayerConfiguration conf123 = new NeuralNetConfiguration.Builder()
                .activation(Activation.RELU)
                .weightInit(WeightInit.NORMAL)
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(0.001)).cacheMode(CacheMode.DEVICE)
                //.updater(new Nesterovs(aR.learning_rate))
                //.l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(n_in).nOut(29)
                        .build())
                .layer(new DropoutLayer(0.75))
                .layer(new DenseLayer.Builder().nIn(29).nOut(6)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(6).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();
        /*
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .activation(Activation.TANH)
                .weightInit(WeightInit.XAVIER)
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Sgd(aR.learning_rate))
                //.updater(new Nesterovs(aR.learning_rate))
                //.l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(320).nOut(5)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(5).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();
*/
/*
        ContinuousParameterSpace learningRateHyperparam  = new ContinuousParameterSpace(0.0001, 0.1);
        IntegerParameterSpace layerSizeHyperparam  = new IntegerParameterSpace(16,256);

        MultiLayerSpace hyperparameterSpace  = new MultiLayerSpace.Builder()
                //These next few options: fixed values for all models
                .weightInit(WeightInit.XAVIER)
                //.l2(0.0001)
                //Learning rate hyperparameter: search over different values, applied to all models
                .updater(new SgdSpace(learningRateHyperparam))
                .addLayer( new DenseLayerSpace.Builder()
                        //Fixed values for this layer:
                        .nIn(320)  //Fixed input: 28x28=784 pixels for MNIST
                        .activation(Activation.LEAKYRELU)
                        //One hyperparameter to infer: layer size
                        .nOut(layerSizeHyperparam)
                        .build())
                .addLayer( new OutputLayerSpace.Builder()
                        .nOut(3)
                        .activation(Activation.SOFTMAX)
                        .lossFunction(LossFunctions.LossFunction.MCXENT)
                        .build())
                .build();

        RandomSearchGenerator candidateGenerator = new RandomSearchGenerator(hyperparameterSpace, null);
        EvaluationScoreFunction scoreFunction = new EvaluationScoreFunction(Metric.ACCURACY);
        MaxTimeCondition terminationConditions =  new MaxTimeCondition(15, TimeUnit.MINUTES);


        FileModelSaver modelSaver = new FileModelSaver("/home/koomikko/Documents/codes/silvai/nn_out/");




        OptimizationConfiguration configuration = new OptimizationConfiguration.Builder()
                .candidateGenerator(candidateGenerator)
                //.dataProvider(dataProvider)
                .dataSource(neuralNetWorkTest_3d_treespecies.ExampleDataSource.class)
                .modelSaver(modelSaver)
                .scoreFunction(scoreFunction)
                .terminationConditions(terminationConditions)
                .build();

        LocalOptimizationRunner runner = new LocalOptimizationRunner(configuration, new MultiLayerNetworkTaskCreator());

//Start the hyperparameter optimization

        runner.execute();

 */

/*
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .activation(Activation.SIGMOID)
                .weightInit(WeightInit.XAVIER_LEGACY)
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Sgd(aR.learning_rate))
                //.updater(new Nesterovs(aR.learning_rate))
                //.l2(1e-5)
                .list()
                .layer(new DenseLayer.Builder().nIn(320).nOut(11)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(11).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();
*/


/*
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .activation(Activation.SIGMOID)
                .weightInit(WeightInit.XAVIER_LEGACY)
                //.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Sgd(aR.learning_rate))
                //.updater(new Nesterovs(aR.learning_rate))
                //.l2(1e-5)
                .list()
                .layer(new DenseLayer.Builder().nIn(489).nOut(17)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(17).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();
*/
        /* THE BOTTOM ONE WORKS WELL WITH ITC!!! */
/*
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .activation(Activation.SIGMOID)
                .weightInit(WeightInit.XAVIER_UNIFORM)
                .updater(new Sgd(aR.learning_rate))
                //.updater(new Nesterovs(aR.learning_rate))
                .l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(5).nOut(15)
                        .build())
                .layer(new DenseLayer.Builder().nIn(15).nOut(9)
                        .build())
                .layer(new DenseLayer.Builder().nIn(9).nOut(5)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(5).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();
*/
/*
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .activation(Activation.SWISH)
                .weightInit(WeightInit.LECUN_UNIFORM)
                .updater(new Sgd(aR.learning_rate))
                //.updater(new Nesterovs(aR.learning_rate))
                .l2(1e-6)
                .list()
                .layer(new DenseLayer.Builder().nIn(6).nOut(9)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(9).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();
*/
        System.out.println("GOT THROUGHT!");

        //Initialize the user interface backend
        //UIServer uiServer = UIServer.getInstance();

        //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
        //StatsStorage statsStorage = new InMemoryStatsStorage();         //Alternative: new FileStatsStorage(File), for saving and loading later

        //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
        //uiServer.attach(statsStorage);

        //Then add the StatsListener to collect this information from the network, as it trains

        MultiLayerNetwork model = new MultiLayerNetwork(conf);

        //model.setListeners(new StatsListener(statsStorage));

        //model.setInputMiniBatchSize(124);
        //System.out.println("BATCH SIZE: " + model.batchSize());

        if(aR.odir.compareTo("asd") == 0)
            aR.odir = "";

        EarlyStoppingConfiguration esConf = new EarlyStoppingConfiguration.Builder()
                //.iterationTerminationConditions(new MaxTimeIterationTerminationCondition(aR.time, TimeUnit.MINUTES))
                .epochTerminationConditions(new MaxEpochsTerminationCondition(n_epoch))
                .scoreCalculator(new DataSetLossCalculator(testIter, true))
                //.scoreCalculator(new DataSetLossCalculator(trainIter, true))
                //.scoreCalculator(new DataSetLossCalculator(trainIter, true))
                .evaluateEveryNEpochs(1)
                .modelSaver(new LocalFileModelSaver(aR.odir))
                .build();




        EarlyStoppingTrainer trainer = new EarlyStoppingTrainer(esConf,model,trainIter);

        EvaluativeListener eva_lis_train = new EvaluativeListener(trainIter, 1, EPOCH_END);
        EvaluativeListener eva_lis_test = new EvaluativeListener(testIter, 1, EPOCH_END);


        //trainer.setListener(new StatsListener(statsStorage));
//Conduct early stopping training:

        System.out.println("EARLY STOPPING!!");
        /*
        EarlyStoppingResult result = trainer.fit();

        //System.exit(1);
        MultiLayerNetwork bestModel = (MultiLayerNetwork) result.getBestModel();

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


*/
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


        }

        FileWriter fw = new FileWriter(outFile, true);

        fw.write(out_train);
        fw.write("\n");
        fw.write(out_test);
        fw.write("\n");

        fw.close();


        System.exit(1);
/*
        int[] testi = model.predict(testData.getFeatures());

        System.out.println(Arrays.toString(testi));

        System.out.println(model.summary());

        Evaluation eval = new Evaluation(3);


        INDArray output = model.output(testData.getFeatures());
        eval.eval(testData.getLabels(), output);
        log.info(eval.stats());

*/

        Evaluation eval2 = new Evaluation(3);


        //INDArray output = model.output(testData.getFeatures());
        //eval.eval(testData.getLabels(), output);
        log.info(eval2.stats());
        //Evaluation  eval = new Evaluation(3);
        while(testIter.hasNext()){
            DataSet t = testIter.next();

            INDArray features = t.getFeatures();
            INDArray labels = t.getLabels();
            INDArray predicted = model.output(features,false);
            eval2.eval(labels, predicted);
        }

        System.out.println(eval2.stats());

        //model.save(new File("smk_s2_net.net"));

    }


}

