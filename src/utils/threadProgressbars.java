package utils;

import java.util.Arrays;

public class threadProgressbars {

    public int numberOfCalls = 0;

    volatile int[] progress;
    volatile long[] time;
    int[] end;

    int[] progress2;
    int[] end2;
    String[] lines;

    int totalFiles = 0;
    int fileProgress = 0;


    int numCores = 0;
    public threadProgressbars(){

    }

    public threadProgressbars(int cores, int totalFiles){

        progress = new int[cores];
        time = new long[cores];
        end = new int[cores];
        lines = new String[cores];
        this.numCores = cores;

        System.out.print("\033[H\033[2J");
        System.out.flush();

        this.totalFiles = totalFiles;

    }

    public synchronized void setEnd(int coreNumber, int end) {

        this.end[coreNumber] = end;
        this.progress[coreNumber] = 0;

        //System.out.println(Arrays.toString(this.end));
        //System.exit(1);

    }

    public synchronized void setEnd2(int coreNumber, int end) {

        this.end2[coreNumber] = end;
        this.progress2[coreNumber] = 0;

        //System.out.println(Arrays.toString(this.end));
        //System.exit(1);

    }

    public synchronized void fileDone(){
        fileProgress++;

    }

    public void addProgress(int coreNumber, int progress) {

        this.progress[coreNumber] += progress;

    }

    public void setProgress(int coreNumber, int progress) {

        this.progress[coreNumber] = progress;

    }

    public void setTime(int coreNumber, long time) {

        this.time[coreNumber] = time;

    }


    public synchronized void printProgressBar2() {
        for (int i = 0; i < progress.length; i++) {
            int percent = (int) Math.round((progress[i] / (double) end[i]) * 100);
            StringBuilder bar = new StringBuilder("[");
            int width = 20;
            int filled = (int) (width * (percent / 100.0));
            for (int j = 0; j < width; j++) {
                if (j < filled) {
                    bar.append("=");
                } else {
                    bar.append(" ");
                }
            }
            bar.append("] " + percent + "%");

            System.out.print("\033[F"); // move cursor to beginning of previous line
            System.out.println(bar.toString()); // print updated progress bar on previous line

        }
    }

    public synchronized void printProgressBar3() {

        for (int i = 0; i < numCores; i++) {
            int percent = (int) Math.round((progress[i] / (double) end[i]) * 100);
            StringBuilder bar = new StringBuilder("[");
            int width = 20;
            int filled = (int) (width * (percent / 100.0));
            for (int j = 0; j < width; j++) {
                if (j < filled) {
                    bar.append("=");
                } else {
                    bar.append(" ");
                }
            }
            bar.append("] " + percent + "%");
            lines[i] = "\033[" + (i + 1) + ";1f" + bar.toString(); // move cursor to ith line and 1st column, save the line to array
        }
        StringBuilder output = new StringBuilder();
        for (String line : lines) {
            output.append(line).append("\n"); // concatenate all lines with a newline character
        }
        System.out.print("\033[0;0f" + output.toString()); // move cursor to 1st line and 1st column, print concatenated lines
    }

    public synchronized void printProgressBar() {

        this.numberOfCalls++;

        for (int i = 0; i < numCores; i++) {
            int percent = (int) Math.round((progress[i] / (double) end[i]) * 100);
            StringBuilder bar = new StringBuilder("[");
            int width = 20;
            int filled = (int) (width * (percent / 100.0));
            for (int j = 0; j < width; j++) {
                if (j < filled) {
                    bar.append("=");
                } else {
                    bar.append(" ");
                }
            }

            long proge = Math.max(progress[i], 1);
            bar.append("] " + percent + "% " + (time[i]/proge) + "us/unit");
            bar.append("\nFiles: " + fileProgress + "/" + totalFiles); // print files done
            System.out.print("\033[" + (i + 1) + ";0f"); // move cursor to ith line and 0th column
            System.out.print("\033[2K"); // clear the entire line
            System.out.print(bar.toString()); // print the progress bar for the ith core/thread
        }
        System.out.print("\033[" + (numCores + 1) + ";0f"); // move cursor to next line after all progress bars
    }
}
