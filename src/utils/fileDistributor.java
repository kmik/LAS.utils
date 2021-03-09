package utils;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;

public class fileDistributor {

    PriorityBlockingQueue<File> que = new PriorityBlockingQueue<>();

    public fileDistributor(){

    }

    public fileDistributor(ArrayList<File> files){

        que = new PriorityBlockingQueue<>(files.size());

        for(int i = 0; i < files.size(); i++)
            que.offer(files.get(i));

    }

    public synchronized File getFile(){

            return que.poll();

    }

    public synchronized boolean isEmpty(){

        return que.isEmpty();

    }

    public PriorityBlockingQueue<File> getQue(){
        return this.que;
    }
}
