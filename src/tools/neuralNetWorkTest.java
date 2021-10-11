package tools;

import org.apache.log4j.BasicConfigurator;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
import org.deeplearning4j.earlystopping.EarlyStoppingResult;
import org.deeplearning4j.earlystopping.saver.LocalFileModelSaver;
import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
import org.deeplearning4j.earlystopping.termination.MaxTimeIterationTerminationCondition;
import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.argumentReader;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class neuralNetWorkTest {

    private static Logger log = LoggerFactory.getLogger(neuralNetWorkTest.class);
    //private final BatchNormalization normalizer_batch;

    public neuralNetWorkTest(argumentReader aR) throws  Exception{

        int batchSize = (int)Math.pow(2,6);

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

            int labelIndex2 = 5;
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
        int labelIndex2 = 5;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
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

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .activation(Activation.SIGMOID)
                .weightInit(WeightInit.XAVIER_UNIFORM)
                .updater(new Sgd(aR.learning_rate))
                //.updater(new Nesterovs(aR.learning_rate))
                .l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder().nIn(5).nOut(15)
                        .build())
                .layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                        .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                        //.activation(Activation.SOFTSIGN) //Override the global TANH activation with softmax for this layer
                        .nIn(15).nOut(3).build())
                //.layer( new OutputLayer.Builder(LossFunctions.LossFunction.KL_DIVERGENCE)
                //      .activation(Activation.SOFTMAX) //Override the global TANH activation with softmax for this layer
                //    .nIn(4).nOut(3).build())
                .build();


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

        MultiLayerNetwork model = new MultiLayerNetwork(conf);

        //model.setInputMiniBatchSize(124);
        //System.out.println("BATCH SIZE: " + model.batchSize());

        if(aR.odir.compareTo("asd") == 0)
            aR.odir = "";

        EarlyStoppingConfiguration esConf = new EarlyStoppingConfiguration.Builder()
                .iterationTerminationConditions(new MaxTimeIterationTerminationCondition(aR.time, TimeUnit.MINUTES))
                .epochTerminationConditions(new MaxEpochsTerminationCondition(100000))
                .scoreCalculator(new DataSetLossCalculator(testIter, true))
                //.scoreCalculator(new DataSetLossCalculator(trainIter, true))
                .evaluateEveryNEpochs(1)
                .modelSaver(new LocalFileModelSaver(aR.odir))
                .build();

        EarlyStoppingTrainer trainer = new EarlyStoppingTrainer(esConf,model,trainIter);

//Conduct early stopping training:
        EarlyStoppingResult result = trainer.fit();

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

        model.init();
        //record score once every 100 iterations
        model.setListeners(new ScoreIterationListener(100));

        for(int i = 0; i < 10000; i++ ) {
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
        //Evaluation  eval = new Evaluation(3);
        while(testIter.hasNext()){
            DataSet t = testIter.next();

            INDArray features = t.getFeatures();
            INDArray labels = t.getLabels();
            INDArray predicted = model.output(features,false);
            eval.eval(labels, predicted);
        }

        System.out.println(eval.stats());

        //model.save(new File("smk_s2_net.net"));

    }

}
