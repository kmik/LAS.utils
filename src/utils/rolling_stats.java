package utils;

public class rolling_stats {

    public double count_rolling_stats, average_rolling_stats,
            pwrSumAvg_rolling_stats, stdDev_rolling_stats,
            max_rolling_stats, min_rolling_stats;

    public rolling_stats(){

        this.reset();

    }

    public void add(double val){


        if(val > max_rolling_stats)
            max_rolling_stats = val;

        if(val < min_rolling_stats)
            min_rolling_stats = val;

        count_rolling_stats++;
        average_rolling_stats += (val - average_rolling_stats) / count_rolling_stats;
        pwrSumAvg_rolling_stats += (val * val - pwrSumAvg_rolling_stats) / count_rolling_stats;
        stdDev_rolling_stats = Math.sqrt((pwrSumAvg_rolling_stats * count_rolling_stats - count_rolling_stats * average_rolling_stats * average_rolling_stats) / (count_rolling_stats - 1));

    }

    public void remove(double val){

        count_rolling_stats--;
        //average_rolling_stats = ((average_rolling_stats * count_rolling_stats) - val) / (count_rolling_stats - 1);
        average_rolling_stats -= (val - average_rolling_stats) / count_rolling_stats;
        pwrSumAvg_rolling_stats -= (val * val - pwrSumAvg_rolling_stats) / count_rolling_stats;
        stdDev_rolling_stats = Math.sqrt((pwrSumAvg_rolling_stats * count_rolling_stats - count_rolling_stats * average_rolling_stats * average_rolling_stats) / (count_rolling_stats - 1));


    }


    public void reset(){

        this.count_rolling_stats = 0;
        this.average_rolling_stats = 0;
        this.pwrSumAvg_rolling_stats = 0;
        this.stdDev_rolling_stats = 0;
        this.max_rolling_stats = Double.NEGATIVE_INFINITY;
        this.min_rolling_stats = Double.POSITIVE_INFINITY;

    }

    public boolean reject_as_outlier_topSide(double val, double threshold){

        val = Math.abs(val);

        if(val < this.average_rolling_stats)
            return false;

        if( Math.abs(val - average_rolling_stats) > (this.stdDev_rolling_stats * threshold) )
            return true;

        return false;

    }

    public boolean reject_as_outlier_bottomSide(double val, double threshold){

        val = Math.abs(val);

        if(val > this.average_rolling_stats)
            return false;

        if( Math.abs(val - average_rolling_stats) > (this.stdDev_rolling_stats * threshold) )
            return true;

        return false;

    }

    public boolean reject_as_outlier(double val, double threshold){

        val = Math.abs(val);

        if( Math.abs(val - average_rolling_stats) > (this.stdDev_rolling_stats * threshold) )
            return true;

        return false;

    }


}
