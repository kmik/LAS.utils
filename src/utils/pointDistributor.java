package utils;

import LASio.LasPoint;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;

public class pointDistributor {

    PriorityBlockingQueue<double[]> que = new PriorityBlockingQueue<>();

    public pointDistributor(){

    }

    public pointDistributor(ArrayList<LasPoint> points){

        /* Initialize with an arbitary value, let's test it out */
        que = new PriorityBlockingQueue<>(1000);


    }

    public synchronized double[] getPoint(){

        return que.poll();

    }

    public synchronized boolean isEmpty(){

        return que.isEmpty();

    }

    public PriorityBlockingQueue<double[]> getQue(){
        return this.que;
    }
}
