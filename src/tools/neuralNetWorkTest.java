package tools;

import org.apache.log4j.BasicConfigurator;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
//import org.deeplearning4j.examples.utils.DownloaderUtility;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class neuralNetWorkTest {

    private static Logger log = LoggerFactory.getLogger(neuralNetWorkTest.class);
    //private final BatchNormalization normalizer_batch;

    public neuralNetWorkTest() throws  Exception{

        BasicConfigurator.configure();

        RecordReader recordReader = new CSVRecordReader(0,",");
        RecordReader recordReader2 = new CSVRecordReader(0,",");
        RecordReader recordReader2_test = new CSVRecordReader(0,",");


        recordReader.initialize(new FileSplit(new File("/home/koomikko/Documents/iris.csv")));
        //recordReader2.initialize(new FileSplit(new File("/home/koomikko/Documents/species2.csv")));
        recordReader2.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_train.txt")));
        recordReader2_test.initialize(new FileSplit(new File("/home/koomikko/Downloads/L8_reflectance_and_FFC_plot_data/smk_test.txt")));

        int labelIndex = 4;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
        int labelIndex2 = 4;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
        int numClasses = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
        int numClasses2 = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
        int batchSize = 150;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)
        int batchSize2 =  128;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)

        DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader2,batchSize2,labelIndex2,numClasses2);

        DataSetIterator trainIter = new RecordReaderDataSetIterator(recordReader2,batchSize2,labelIndex2,numClasses2);
        DataSetIterator testIter = new RecordReaderDataSetIterator(recordReader2_test,batchSize2,labelIndex2,numClasses2);

        DataNormalization normalizer = new NormalizerStandardize();
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
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .activation(Activation.RELU )
                .weightInit(WeightInit.RELU)
                .updater(new Sgd(0.1))
                .l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(4).nOut(7)
                        .build())
                .layer(new DenseLayer.Builder().nIn(7).nOut(5)
                        .build())

                .layer(new DenseLayer.Builder().nIn(5).nOut(3)
                        .build())

                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        .nIn(3).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                  //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                    //    .nIn(4).nOut(3).build())
                .build();

        System.out.println("GOT THROUGHT!");

        MultiLayerNetwork model = new MultiLayerNetwork(conf);

        //model.setInputMiniBatchSize(124);
        //System.out.println("BATCH SIZE: " + model.batchSize());

        model.init();
        //record score once every 100 iterations
        model.setListeners(new ScoreIterationListener(100));

        for(int i = 0; i < 1000; i++ ) {
            model.fit(trainIter);
        }

/*
        int[] testi = model.predict(testData.getFeatures());

        System.out.println(Arrays.toString(testi));

        System.out.println(model.summary());

        Evaluation eval = new Evaluation(3);


        INDArray output = model.output(testData.getFeatures());
        eval.eval(testData.getLabels(), output);
        log.info(eval.stats());

*/
        Evaluation  eval = new Evaluation(3);
        while(testIter.hasNext()){
            DataSet t = testIter.next();

            INDArray features = t.getFeatures();
            INDArray labels = t.getLabels();
            INDArray predicted = model.output(features,false);
            eval.eval(labels, predicted);
        }

        System.out.println(eval.stats());


    }

}
