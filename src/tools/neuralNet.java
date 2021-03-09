package tools;

import org.apache.log4j.BasicConfigurator;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
//import org.deeplearning4j.examples.utils.DownloaderUtility;

import java.io.File;

public class neuralNet {

    public File dataAll;
    public File dataTrain;
    public File dataTest;

    int labelIndex = 4;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
    int numClasses = 3;     //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
    int batchSize = 150;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)


    public neuralNet(File allData, int labelIndex, int numClasses, int batchSize) throws Exception {

        BasicConfigurator.configure();

        this.dataAll = allData;

        RecordReader recordReader = new CSVRecordReader(0,",");
        DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader,batchSize,labelIndex,numClasses);
        DataSet allDataSet = iterator.next();


    }

    public neuralNet(File testData, File trainData, int labelIndex, int numClasses, int batchSize) throws Exception {



    }
}
