package tools;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import tools.threadSafeWriteToFile;

/** One output consumer, bounded queues and reusable primitive computation buffers. */
final class SolarTextOutput {
    static final class Batch {
        final double[][] values,coordinates;
        int count;
        Batch(int records,int samples) {values=new double[records][samples];coordinates=new double[records][3];}
    }
    private final ArrayBlockingQueue<Batch> free,ready;
    private final Thread consumer;
    private volatile RuntimeException failure;
    private volatile boolean finishing;

    SolarTextOutput(final threadSafeWriteToFile writer,final SolarSchedule schedule,int workers) {
        long recordBytes=8L*schedule.samples+64;
        int batchSize=(int)Math.max(1,Math.min(8,(1L<<20)/recordBytes));
        int buffers=(int)Math.max(2,Math.min(2L*workers+1,(64L<<20)/(batchSize*recordBytes)));
        free=new ArrayBlockingQueue<Batch>(buffers);ready=new ArrayBlockingQueue<Batch>(buffers);
        for(int i=0;i<buffers;i++) free.add(new Batch(batchSize,schedule.samples));
        consumer=new Thread(new Runnable() {
            public void run() {
                try {
                    while(!finishing||!ready.isEmpty()) {
                        Batch batch=ready.poll(100,TimeUnit.MILLISECONDS);
                        if(batch==null) continue;
                        for(int r=0;r<batch.count;r++) {
                            // The existing project writer requires ArrayLists. Box only here,
                            // not in the compute loop. Fresh lists also tolerate a retaining writer.
                            ArrayList<Double> values=new ArrayList<Double>(schedule.samples);
                            for(double value:batch.values[r]) values.add(value);
                            writer.writeLineSpecial(values,schedule.copyDates(),batch.coordinates[r].clone());
                        }
                        batch.count=0;free.put(batch);
                    }
                } catch(Exception e) {failure=new IllegalStateException("Cannot write voxel time series",e);}
                finally {
                    try {writer.close();} catch(Exception e) {
                        if(failure==null) failure=new IllegalStateException("Cannot close voxel time series",e);
                        else failure.addSuppressed(e);
                    }
                }
            }
        },"solar-text-output");
        consumer.start();
    }
    private void check() {if(failure!=null) throw failure;}
    Batch acquire() throws InterruptedException {
        for(;;) {check();Batch batch=free.poll(100,TimeUnit.MILLISECONDS);if(batch!=null) return batch;}
    }
    void submit(Batch batch) throws InterruptedException {
        if(batch==null) return;
        if(batch.count==0) {free.put(batch);return;}
        for(;;) {check();if(ready.offer(batch,100,TimeUnit.MILLISECONDS)) return;}
    }
    void finish() {
        finishing=true;
        boolean interrupted=false;
        while(consumer.isAlive()) {
            try {consumer.join();} catch(InterruptedException e) {interrupted=true;}
        }
        if(interrupted) {Thread.currentThread().interrupt();throw new IllegalStateException("Interrupted waiting for text output");}
        check();
    }
}
