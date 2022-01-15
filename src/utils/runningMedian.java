package utils;

import java.util.Collections;
import java.util.PriorityQueue;

public class runningMedian {

    PriorityQueue<Float> smaller = new PriorityQueue<>
            (Collections.reverseOrder());
    PriorityQueue<Float> greater = new PriorityQueue<>();

    float median = -1;

    public runningMedian(){



    }

    public void add(float in){

        if(smaller.size() == 0 && greater.size() == 0){
            smaller.add(in);
            median = in;
            return;
        }

        if(smaller.size() > greater.size())
        {
            if(in < median)
            {
                greater.add(smaller.remove());
                smaller.add(in);
            }
            else
                greater.add(in);

            median = (smaller.peek() + greater.peek())/2.0f;
        }

        // case2(both heaps are balanced)
        else if(smaller.size() == greater.size())
        {
            if(in < median)
            {
                smaller.add(in);
                median = smaller.peek();
            }
            else
            {
                greater.add(in);
                median = greater.peek();
            }
        }
        else
        {
            if(in > median)
            {
                smaller.add(greater.remove());
                greater.add(in);
            }
            else
                smaller.add(in);
            median = (smaller.peek() + greater.peek())/2.0f;

        }


    }

    public float median(){
        return this.median;
    }
}
