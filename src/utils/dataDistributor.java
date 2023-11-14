package utils;

import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;

public class dataDistributor<T> {

    private PriorityBlockingQueue<T> queue;

    public dataDistributor() {
        queue = new PriorityBlockingQueue<>();
    }

    public dataDistributor(ArrayList<T> items) {
        queue = new PriorityBlockingQueue<>(items.size());
        queue.addAll(items);
    }

    public synchronized T getData() {
        return queue.poll();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public PriorityBlockingQueue<T> getQueue() {
        return queue;
    }

    public int size(){
        return queue.size();
    }
}