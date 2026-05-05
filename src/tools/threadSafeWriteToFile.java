package tools;

import it.unimi.dsi.fastutil.Hash;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class threadSafeWriteToFile {

    private final BufferedWriter writer;
    private final Lock lock = new ReentrantLock(true); // fair locking

    HashMap<Integer, double[]> mappi = new HashMap<>();
    HashMap<Integer, Integer> nWrittenToMap = new HashMap<>();

    boolean alreadyWrittenHeader = false;
    int targetNumberOfValuesToWrite = 0;

    public threadSafeWriteToFile(BufferedWriter writer, int targetNumberOfValuesToWrite){
        this.writer = writer;
        this.targetNumberOfValuesToWrite = targetNumberOfValuesToWrite;

    }

    public void addDataToMap(int id, double[] data, int xIndexToWriteFrom, int numberOfValuesToWrite, String date) {

        // lock first
        lock.lock();

        // If nwrittentomap already contains the id, then add the numberfValuesToWrite to the existing value, otherwise put the numberOfValuesToWrite as the value for the id
        try {
            if (nWrittenToMap.containsKey(id)) {
                nWrittenToMap.put(id, nWrittenToMap.get(id) + numberOfValuesToWrite);
            } else {
                nWrittenToMap.put(id, numberOfValuesToWrite);
            }

            // If mappi already contains the id, then add the new data to the existing data, otherwise put the new data as the value for the id
            if (mappi.containsKey(id)) {
                double[] existingData = mappi.get(id);
                for (int i = 0; i < numberOfValuesToWrite; i++) {
                    existingData[xIndexToWriteFrom + i] += data[i];
                }
            } else {
                mappi.put(id, data);
            }
        } finally {

            if(nWrittenToMap.get(id) >= targetNumberOfValuesToWrite) {

                //Write data to outputfile. First the date and then the rest of the values tab delimited
                try {
                    writer.write(date + "\t");
                    for (int i = 0; i < mappi.get(id).length; i++) {
                        writer.write(mappi.get(id)[i] + "\t");
                    }
                    writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            lock.unlock();
        }

    }

    public void writeLine(String text) throws IOException {
        lock.lock();
        try {
            writer.write(text);
            writer.newLine();
        } finally {
            lock.unlock();
        }
    }

    public void writeLineSpecial(ArrayList<Double> values, ArrayList<String> header, double[] voxelCoordinates) throws IOException {
        lock.lock();
        if(!alreadyWrittenHeader) {
            try {
                // Write header first
                writer.write("x\ty\tz\t");
                for (String h : header) {

                    writer.write(h + "\t");
                }

                writer.newLine();
                alreadyWrittenHeader = true;

                // Write values, first is the voxel coordinates (separated by ;) and then the rest of the values tab delimited

                // Write the voxelcoordinates
                for (double v : voxelCoordinates) {
                    writer.write(v + "\t");
                }
                //writer.write("\t");

                // Print values for debug

                //System.out.println("Writing values: " + values.toString() + " for voxel coordinates: " + voxelCoordinates[0] + ", " + voxelCoordinates[1] + ", " + voxelCoordinates[2]);
                //System.exit(1);

                for (double v : values) {
                    writer.write(v + "\t");
                }
                writer.newLine();
            } finally {
                lock.unlock();
            }
        }
        else{
            try {
                // Write values

                for (double v : voxelCoordinates) {
                    writer.write(v + "\t");
                }


                for (double v : values) {
                    writer.write(v + "\t");
                }
                writer.newLine();
            } finally {
                lock.unlock();
            }
        }
    }

    public void flush() throws IOException {
        lock.lock();
        try {
            writer.flush();
        } finally {
            lock.unlock();
        }
    }

    public void close() throws IOException {
        lock.lock();
        try {
            writer.close();
        } finally {
            lock.unlock();
        }
    }
}
