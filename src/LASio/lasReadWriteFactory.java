package LASio;

import utils.argumentReader;
import utils.pointWriterMultiThread;


import java.io.IOException;
import java.util.ArrayList;

public class lasReadWriteFactory {

    LasPoint tempPoint = new LasPoint();
    int n_threads = 0;
    argumentReader aR;

    ArrayList<LASReader> pointClouds = new ArrayList<>();
    ArrayList<pointWriterMultiThread> pw = new ArrayList<>();
    ArrayList<LasPointBufferCreator> buf = new ArrayList<>();


    public lasReadWriteFactory(argumentReader aR){
        this.aR = aR;
    }

    public synchronized int addReadThread(LASReader pointCloud){

        pointClouds.add(pointCloud);
        pw.add(null);
        buf.add(null);

        return n_threads++;

    }

    public synchronized void prepareBuffer(int thread, int i, int n) throws Exception{

        pointClouds.get(thread).readRecord_noRAF(i, tempPoint, n);

    }

    public synchronized void addWriteThread(int thread_n, pointWriterMultiThread pw, LasPointBufferCreator buf){

        this.pw.set(thread_n, pw);
        this.buf.set(thread_n, buf);

    }

    public void writePoint(LasPoint p, int index, int thread) throws Exception {

        buf.get(thread).writePoint(p, this.aR.inclusionRule, index);

    }

    public synchronized void closeThread(int thread){

        try {
            buf.get(thread).close();
            pw.get(thread).close();
        }catch (IOException e){
            e.printStackTrace();
        }

    }
}
