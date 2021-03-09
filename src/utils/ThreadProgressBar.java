package utils;

/**
 * A class to implement the method progebar() in
 * multiple threads.
 *
 * @author  Kukkonen Mikko
 * @version 0.1
 * @since 06.03.2018
 */

public class ThreadProgressBar{

    int current = 0;
    int end = 0;
    String name = "give me name!";
    int numberOfThreads = 0;

    public ThreadProgressBar(){

    }

    /**
     * Set the maximum value of the iterator to:
     *
     * @param newEnd
     */
    public synchronized void setEnd(int newEnd){
        end = newEnd;
    }

    /**
     * Increment the current progress by amount equal to:
     *
     * @param input
     */

    public synchronized void updateCurrent(int input){

        current += input;

    }

    /**
     * Reset the class
     */

    public synchronized void reset(){

        current = 0;
        numberOfThreads = 0;
        end = 0;
        name = "give me name!";

    }

    /**
     * Name of the process being tracked
     *
     */

    public void setName(String nimi){
        //System.out.println("Setting name to");
        name = nimi;

    }

    /**
     * Add a thread
     *
     */

    public void addThread(){

        numberOfThreads++;

    }

    public synchronized void print(){
        //System.out.println(end);
        progebar(end, current, " " + name);
        //System.out.println(end + " " + current);
    }

    /**
     *
     * Method to erase the line and print a progress
     * bar based on the input.
     *
     * @param paatos     Maximum value
     * @param proge      Current progress
     * @param nimi       Name of the process
     */

    public void progebar(int paatos, int proge, String nimi) {
        System.out.print("\033[2K"); // Erase line content
        if(proge < 0.05*paatos)System.out.print(nimi + "   |                    |\r");
        if(proge >= 0.05*paatos && proge < 0.10*paatos)System.out.print(nimi + "   |>                   |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.10*paatos && proge < 0.15*paatos)System.out.print(nimi + "   |=>                  |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.15*paatos && proge < 0.20*paatos)System.out.print(nimi + "   |==>                 |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.20*paatos && proge < 0.25*paatos)System.out.print(nimi + "   |===>                |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.25*paatos && proge < 0.30*paatos)System.out.print(nimi + "   |====>               |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.30*paatos && proge < 0.35*paatos)System.out.print(nimi + "   |=====>              |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.35*paatos && proge < 0.40*paatos)System.out.print(nimi + "   |======>             |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.40*paatos && proge < 0.45*paatos)System.out.print(nimi + "   |=======>            |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.45*paatos && proge < 0.50*paatos)System.out.print(nimi + "   |========>           |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.50*paatos && proge < 0.55*paatos)System.out.print(nimi + "   |=========>          |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.55*paatos && proge < 0.60*paatos)System.out.print(nimi + "   |==========>         |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.60*paatos && proge < 0.65*paatos)System.out.print(nimi + "   |===========>        |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.65*paatos && proge < 0.70*paatos)System.out.print(nimi + "   |============>       |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.70*paatos && proge < 0.75*paatos)System.out.print(nimi + "   |=============>      |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.75*paatos && proge < 0.80*paatos)System.out.print(nimi + "   |==============>     |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.80*paatos && proge < 0.85*paatos)System.out.print(nimi + "   |===============>    |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.85*paatos && proge < 0.90*paatos)System.out.print(nimi + "   |================>   |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.90*paatos && proge < 0.95*paatos)System.out.print(nimi + "   |=================>  |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.95*paatos && proge < 0.97*paatos)System.out.print(nimi + "   |==================> |  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");
        if(proge >= 0.97*paatos && proge <= 1*paatos)System.out.print(nimi + "   |===================>|  " + Math.round(((double)proge/(double)paatos)*100) + "%\r");


    }

    public String getProgress(int paatos, int proge) {

        if(paatos == -1 && proge == -1)
            return "|????????????????????|";

        if(proge < 0.05*paatos)return ("|                    |");
        if(proge >= 0.05*paatos && proge < 0.10*paatos)return ("|>                   |");
        if(proge >= 0.10*paatos && proge < 0.15*paatos)return ("|=>                  |");
        if(proge >= 0.15*paatos && proge < 0.20*paatos)return ("|==>                 |");
        if(proge >= 0.20*paatos && proge < 0.25*paatos)return ("|===>                |");
        if(proge >= 0.25*paatos && proge < 0.30*paatos)return ("|====>               |");
        if(proge >= 0.30*paatos && proge < 0.35*paatos)return ("|=====>              |");
        if(proge >= 0.35*paatos && proge < 0.40*paatos)return ("|======>             |");
        if(proge >= 0.40*paatos && proge < 0.45*paatos)return ("|=======>            |");
        if(proge >= 0.45*paatos && proge < 0.50*paatos)return ("|========>           |");
        if(proge >= 0.50*paatos && proge < 0.55*paatos)return ("|=========>          |");
        if(proge >= 0.55*paatos && proge < 0.60*paatos)return ("|==========>         |");
        if(proge >= 0.60*paatos && proge < 0.65*paatos)return ("|===========>        |");
        if(proge >= 0.65*paatos && proge < 0.70*paatos)return ("|============>       |");
        if(proge >= 0.70*paatos && proge < 0.75*paatos)return ("|=============>      |");
        if(proge >= 0.75*paatos && proge < 0.80*paatos)return ("|==============>     |");
        if(proge >= 0.80*paatos && proge < 0.85*paatos)return ("|===============>    |");
        if(proge >= 0.85*paatos && proge < 0.90*paatos)return ("|================>   |");
        if(proge >= 0.90*paatos && proge < 0.95*paatos)return ("|=================>  |");
        if(proge >= 0.95*paatos && proge < 0.97*paatos)return ("|==================> |");
        if(proge >= 0.97*paatos && proge <= 1*paatos)return ("|===================>|");

        return "|====================|";
    }

}
