package utils;

import java.util.*;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

public class pointCloudMetrics {

    public double percentile_step_orig = 0.05;

    public double[] densities = new double[]{1.3, 2.5, 5.0, 7.5, 10.0, 15.0, 20.0, 25.0};

    double cutoff = 0;
    double cutoff_n_points = 10;


    public pointCloudMetrics(argumentReader aR){
        this.densities = aR.densities;
        this.percentile_step_orig = aR.percentiles;
        this.cutoff = aR.z_cutoff;
        this.cutoff_n_points = aR.min_points;
    }

    /**
     * Currently way unoptimized, can do a lot better i'm sure.
     *
     * @param z
     * @param intensity
     * @param sum_z
     * @param sum_i
     * @param suffix
     * @param colnames
     * @return
     */
    public ArrayList<Double> calc(ArrayList<Double> z, ArrayList<Integer> intensity, double sum_z, double sum_i, String suffix, ArrayList<String> colnames){

        ArrayList<Double> output = new ArrayList<>();
        colnames.clear();

        /* Should we have insufficient number of points to calculate metrics,
          then we just output NaN for all metrics.
         */
        if(z.size() < cutoff_n_points){

            int p_size = (int)Math.ceil((0.95 / this.percentile_step_orig));

            colnames.add("max_z" + suffix);
            output.add(Double.NaN);
            colnames.add("min_z" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_z" + suffix);
            output.add(Double.NaN);
            colnames.add("median_z" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_z" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_z" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_z" + suffix);
            output.add(Double.NaN);
            colnames.add("max_i" + suffix);
            output.add(Double.NaN);
            colnames.add("min_i" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_i" + suffix);
            output.add(Double.NaN);
            colnames.add("median_i" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_i" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_i" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_i" + suffix);
            output.add(Double.NaN);
            int counter22 = 0;

            double[] percentiles_names = new double[p_size];
            double percentile_step_z2 = percentile_step_orig;
            for(int i = 0; i < p_size; i++){

                percentiles_names[i] = percentile_step_z2;
                percentile_step_z2 += percentile_step_orig;

            }

            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (double)(Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
                output.add(Double.NaN);

            }
            counter22 = 0;
            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (double)(Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
                output.add(Double.NaN);
            }
            for(int i = 0; i < densities.length; i++) {

                colnames.add("d_" + densities[i] + "_z" + suffix);
                output.add(Double.NaN);
            }
            return output;
        }
        //concurrentSort(z, z, intensity);

        //Collections.sort(z);
        //Collections.sort(intensity);


        ArrayList<Double> z_above_threshold = new ArrayList<>();
        ArrayList<Integer> i_above_threshold = new ArrayList<>();

        TreeSet<Integer> intensity_median_set = new TreeSet<>();

        double percentile_step_z = percentile_step_orig;
        double percentile_step_i = percentile_step_orig;

        double[] densitiesOutput = new double[densities.length];



        double z_val;
        int i_val;

        double z_sum_run = 0.0d, i_sum_run = 0.0d;
/*
        System.out.println(percentile_step_i);
        System.out.println((int)(1.0 / percentile_step_z));
*/

        double sd_z = 0;
        double sd_i = 0;

        double mean_z = sum_z / (double)z.size();
        double mean_i = sum_i / (double)intensity.size();

        sum_z = 0;
        sum_i = 0;

        double n_above_t_hold = 0;

        double max_z = Double.NEGATIVE_INFINITY;
        int max_i = Integer.MIN_VALUE;

        double min_z = Double.POSITIVE_INFINITY;
        int min_i = Integer.MAX_VALUE;

        for(int i = 0; i < z.size(); i++){

            z_val = z.get(i);
            i_val = intensity.get(i);

            for(int j = densities.length-1 ; j >= 0; j--){

                if(z_val <= densities[j]){
                    densitiesOutput[j]++;

                }

            }

            if(z_val <= cutoff) {
                continue;
            }

            sum_z += z_val;
            sum_i += i_val;

            z_above_threshold.add(z_val);
            i_above_threshold.add(i_val);

            n_above_t_hold++;

            if(z_val > max_z)
                max_z = z_val;
            if(z_val < min_z)
                min_z = z_val;

            if(i_val > max_i)
                max_i = i_val;
            if(i_val < min_i)
                min_i = i_val;

        }

        if(z_above_threshold.size() < cutoff_n_points){
            /* Should we have insufficient number of points to calculate metrics,
              then we just output NaN for all metrics. This check is done here
              again because we only just now check for point that are within
              the legal boundaries.
             */
            //if(z.size() < cutoff_n_points){

                int p_size = (int)Math.ceil((0.95 / this.percentile_step_orig));

                colnames.add("max_z" + suffix);
                output.add(Double.NaN);
                colnames.add("min_z" + suffix);
                output.add(Double.NaN);
                colnames.add("sd_z" + suffix);
                output.add(Double.NaN);
                colnames.add("median_z" + suffix);
                output.add(Double.NaN);
                colnames.add("mean_z" + suffix);
                output.add(Double.NaN);
                colnames.add("skewness_z" + suffix);
                output.add(Double.NaN);
                colnames.add("kurtosis_z" + suffix);
                output.add(Double.NaN);
                colnames.add("max_i" + suffix);
                output.add(Double.NaN);
                colnames.add("min_i" + suffix);
                output.add(Double.NaN);
                colnames.add("sd_i" + suffix);
                output.add(Double.NaN);
                colnames.add("median_i" + suffix);
                output.add(Double.NaN);
                colnames.add("mean_i" + suffix);
                output.add(Double.NaN);
                colnames.add("skewness_i" + suffix);
                output.add(Double.NaN);
                colnames.add("kurtosis_i" + suffix);
                output.add(Double.NaN);
                int counter22 = 0;

                double[] percentiles_names = new double[p_size];
                double percentile_step_z2 = percentile_step_orig;
                for(int i = 0; i < p_size; i++){

                    percentiles_names[i] = percentile_step_z2;
                    percentile_step_z2 += percentile_step_orig;

                }

                for(int i = 0 ; i < p_size; i++){

                    colnames.add("p_" + (double)(Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
                    output.add(Double.NaN);

                }
                counter22 = 0;
                for(int i = 0 ; i < p_size; i++){

                    colnames.add("p_" + (double)(Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
                    output.add(Double.NaN);
                }
                for(int i = 0; i < densities.length; i++) {

                    colnames.add("d_" + densities[i] + "_z" + suffix);
                    output.add(Double.NaN);
                }
                return output;
            //}
        }

        mean_z = sum_z / (double)z_above_threshold.size();
        mean_i = sum_i / (double)i_above_threshold.size();

        double median_z = -1, median_i = -1;

        double z_skewness_v = 0;
        double i_skewness_v = 0;

        double z_kurtosis_v = 0;
        double z_kurtosis_v_2 = 0;
        double i_kurtosis_v = 0;
        double i_kurtosis_v_2 = 0;

        int densities_counter = 0;


        double sum_z_above = 0.0;
        double sum_i_above = 0.0;

        for(int i = 0; i < z_above_threshold.size(); i++){

            z_val = z_above_threshold.get(i);
            i_val = i_above_threshold.get(i);

            z_sum_run += z_val;
            i_sum_run += (double)i_val;

            sd_z += ((z_val - mean_z) * (z_val - mean_z));
            sd_i += ((i_val - mean_i) * (i_val - mean_i));

            z_skewness_v += ((z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z) );
            i_skewness_v += ((i_val - mean_i) * (i_val - mean_i) * (i_val - mean_i) );

            z_kurtosis_v += ((z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z));
            z_kurtosis_v_2 += ((z_val - mean_z) * (z_val - mean_z));
            i_kurtosis_v += ((i_val - mean_i) * (i_val - mean_i) * (i_val - mean_i) *  (i_val - mean_i));
            i_kurtosis_v_2 += ((i_val - mean_i) * (i_val - mean_i));

            if(z_val < 0)
                z_val = 0;
            if(i_val < 0)
                i_val = 0;

        }

        sd_z = sd_z / (n_above_t_hold-1);
        sd_i = sd_i / (n_above_t_hold-1);

        sd_z = Math.sqrt(sd_z);
        sd_i = Math.sqrt(sd_i);

        double skewness_z = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * z_skewness_v / (sd_z*sd_z*sd_z);
        double skewness_i = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * i_skewness_v / (sd_i*sd_i*sd_i);


/*
        double kurtosis_z = z_kurtosis_v / (double)z.size() / (sd_z*sd_z*sd_z*sd_z) - 3.0;
        double kurtosis_i = i_kurtosis_v / (double)intensity.size() / (sd_i*sd_i*sd_i*sd_i) - 3.0;

 */
        double kurtosis_z = z_kurtosis_v / n_above_t_hold / ( ( z_kurtosis_v_2 / n_above_t_hold ) * ( z_kurtosis_v_2 / n_above_t_hold )) ;
        double kurtosis_i = i_kurtosis_v / n_above_t_hold / ( ( i_kurtosis_v_2 / n_above_t_hold ) * ( i_kurtosis_v_2 / n_above_t_hold ));


        Collections.sort(i_above_threshold);
        Collections.sort(z_above_threshold);

        if (z_above_threshold.size() % 2 == 0) {
            //System.out.println(z_above_threshold.size());
            median_z = ( ((double) z_above_threshold.get(z_above_threshold.size() / 2) + (double) z_above_threshold.get(z_above_threshold.size() / 2 - 1) ) / 2.0);

            median_i = ( ((double) i_above_threshold.get(i_above_threshold.size() / 2) + (double) i_above_threshold.get(i_above_threshold.size() / 2 - 1)) / 2.0);
        }
        else {
            median_z = (double) z_above_threshold.get(z_above_threshold.size() / 2);
            median_i = (double) i_above_threshold.get(i_above_threshold.size() / 2);
        }


        //skewness_z = 1.0 / (sd_z*sd_z*sd_z) * (z_skewness_v / (double)z.size());

        //percentiles_z[percentiles_z.length-1] = z.get(z.size()-1);
        //percentiles_i[percentiles_i.length-1] = intensity.get(intensity.size()-1);

        for(int j = 0 ; j < densities.length; j++){

            densitiesOutput[j] /= (double)z.size();

        }
/*
        System.out.println(Arrays.toString(percentiles_z) + " " + z.get(z.size()-1));
        System.out.println(Arrays.toString(densitiesOutput));
        System.out.println(sd_z + " " + sd_i + " " + skewness_z + " " + kurtosis_z);
*/
        colnames.clear();

        int counter = 0;

        double[] z_array = ArrayUtils.toPrimitive(z_above_threshold.toArray(new Double[z_above_threshold.size()]));
        double[] i_array = new double[i_above_threshold.size()];

        for(int i = 0; i < i_above_threshold.size(); i++){
            i_array[i] = (double)i_above_threshold.get(i);
        }


        Percentile p_z = new Percentile(this.percentile_step_orig);
        Percentile p_i = new Percentile(this.percentile_step_orig);

        p_z.setData(z_array);
        p_i.setData(i_array);

        int pers_size = (int)Math.ceil((0.95 / percentile_step_z));

        double[] percentiles_z = new double[pers_size];
        double[] percentiles_z_names = new double[pers_size];
        double[] percentiles_i = new double[pers_size];
        double[] percentiles_i_names = new double[pers_size];

        for(int i = 0; i < percentiles_z.length; i++){

            percentiles_z[i] = p_z.evaluate(percentile_step_z * 100.0);
            percentiles_z_names[i] = percentile_step_z;
            percentiles_i[i] = p_i.evaluate(percentile_step_z * 100.0);
            percentiles_i_names[i] = percentile_step_z;
            percentile_step_z += percentile_step_orig;

        }

        //System.out.println("end: ");



        colnames.add("max_z" + suffix);
        output.add(max_z);
        colnames.add("min_z" + suffix);
        output.add(min_z);
        colnames.add("sd_z" + suffix);
        output.add(sd_z);
        colnames.add("median_z" + suffix);
        output.add(median_z);
        colnames.add("mean_z" + suffix);
        output.add(mean_z);
        colnames.add("skewness_z" + suffix);
        output.add(skewness_z);
        colnames.add("kurtosis_z" + suffix);
        output.add(kurtosis_z);
        colnames.add("max_i" + suffix);
        output.add((double)max_i);
        colnames.add("min_i" + suffix);
        output.add((double)min_i);
        colnames.add("sd_i" + suffix);
        output.add(sd_i);
        colnames.add("median_i" + suffix);
        output.add((median_i));
        colnames.add("mean_i" + suffix);
        output.add(mean_i);
        colnames.add("skewness_i" + suffix);
        output.add(skewness_i);
        colnames.add("kurtosis_i" + suffix);
        output.add(kurtosis_i);
        counter = 0;
        for(int i = 0 ; i < percentiles_z.length; i++){

            colnames.add("p_" + (double)(Math.round(percentiles_z_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
            output.add(percentiles_z[counter++]);

        }
        counter = 0;
        for(int i = 0 ; i < percentiles_i.length; i++){

            colnames.add("p_" + (double)(Math.round(percentiles_i_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
            output.add(percentiles_i[counter++]);
        }
        for(int i = 0; i < densities.length; i++){

            colnames.add("d_" + densities[i] + "_z" + suffix);
            output.add(densitiesOutput[i]);

        }

/*
        System.out.println(Arrays.toString(colnames.toArray()));
        System.out.println(Arrays.toString(output.toArray()));
*/
        return output;
    }

    public ArrayList<Double> calc_with_RGB(ArrayList<Double> z, ArrayList<Integer> intensity, double sum_z, double sum_i, String suffix, ArrayList<String> colnames,
                                           ArrayList<int[]> RGB){

        ArrayList<Double> output = new ArrayList<>();
        colnames.clear();

        /* Should we have insufficient number of points to calculate metrics,
          then we just output NaN for all metrics.
         */
        if(z.size() < cutoff_n_points){

            int p_size = (int)Math.ceil((0.95 / this.percentile_step_orig));

            colnames.add("max_z" + suffix);
            output.add(Double.NaN);
            colnames.add("min_z" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_z" + suffix);
            output.add(Double.NaN);
            colnames.add("median_z" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_z" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_z" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_z" + suffix);
            output.add(Double.NaN);

            colnames.add("max_i" + suffix);
            output.add(Double.NaN);
            colnames.add("min_i" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_i" + suffix);
            output.add(Double.NaN);
            colnames.add("median_i" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_i" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_i" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_i" + suffix);
            output.add(Double.NaN);



            int counter22 = 0;

            double[] percentiles_names = new double[p_size];
            double percentile_step_z2 = percentile_step_orig;
            for(int i = 0; i < p_size; i++){

                percentiles_names[i] = percentile_step_z2;
                percentile_step_z2 += percentile_step_orig;

            }

            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (double)(Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
                output.add(Double.NaN);

            }
            counter22 = 0;
            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (double)(Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
                output.add(Double.NaN);
            }
            for(int i = 0; i < densities.length; i++) {

                colnames.add("d_" + densities[i] + "_z" + suffix);
                output.add(Double.NaN);
            }

            colnames.add("max_R" + suffix);
            output.add(Double.NaN);
            colnames.add("min_R" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_R" + suffix);
            output.add(Double.NaN);
            colnames.add("median_R" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_R" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_R" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_R" + suffix);
            output.add(Double.NaN);

            colnames.add("max_G" + suffix);
            output.add(Double.NaN);
            colnames.add("min_G" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_G" + suffix);
            output.add(Double.NaN);
            colnames.add("median_G" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_G" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_G" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_G" + suffix);
            output.add(Double.NaN);

            colnames.add("max_B" + suffix);
            output.add(Double.NaN);
            colnames.add("min_B" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_B" + suffix);
            output.add(Double.NaN);
            colnames.add("median_B" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_B" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_B" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_B" + suffix);
            output.add(Double.NaN);


            return output;
        }
        //concurrentSort(z, z, intensity);

        //Collections.sort(z);
        //Collections.sort(intensity);


        ArrayList<Double> z_above_threshold = new ArrayList<>();
        ArrayList<Double> R_above_threshold = new ArrayList<>();
        ArrayList<Double> G_above_threshold = new ArrayList<>();
        ArrayList<Double> B_above_threshold = new ArrayList<>();
        ArrayList<Integer> i_above_threshold = new ArrayList<>();

        TreeSet<Integer> intensity_median_set = new TreeSet<>();

        double percentile_step_z = percentile_step_orig;
        double percentile_step_i = percentile_step_orig;

        double[] densitiesOutput = new double[densities.length];



        double z_val;
        double R_val;
        double G_val;
        double B_val;

        int i_val;

        double z_sum_run = 0.0d, i_sum_run = 0.0d;
        double R_sum_run = 0.0d;
        double G_sum_run = 0.0d;
        double B_sum_run = 0.0d;
/*
        System.out.println(percentile_step_i);
        System.out.println((int)(1.0 / percentile_step_z));
*/

        double sd_z = 0;
        double sd_R = 0;
        double sd_G = 0;
        double sd_B = 0;

        double sd_i = 0;

        double mean_z = sum_z / (double)z.size();
        double mean_R = 0;
        double mean_G = 0;
        double mean_B = 0;

        double sum_R = 0;
        double sum_G = 0;
        double sum_B = 0;

        double mean_i = sum_i / (double)intensity.size();

        sum_z = 0;
        sum_i = 0;

        double n_above_t_hold = 0;

        double max_z = Double.NEGATIVE_INFINITY;

        double max_R = Double.NEGATIVE_INFINITY;
        double max_G = Double.NEGATIVE_INFINITY;
        double max_B = Double.NEGATIVE_INFINITY;

        int max_i = Integer.MIN_VALUE;

        double min_z = Double.POSITIVE_INFINITY;
        double min_R = Double.POSITIVE_INFINITY;
        double min_G = Double.POSITIVE_INFINITY;
        double min_B = Double.POSITIVE_INFINITY;

        int min_i = Integer.MAX_VALUE;

        for(int i = 0; i < z.size(); i++){

            z_val = z.get(i);

            R_val = RGB.get(i)[0];
            G_val = RGB.get(i)[1];
            B_val = RGB.get(i)[2];

            i_val = intensity.get(i);

            for(int j = densities.length-1 ; j >= 0; j--){

                if(z_val <= densities[j]){
                    densitiesOutput[j]++;

                }

            }

            if(z_val <= cutoff) {
                continue;
            }

            sum_z += z_val;
            sum_i += i_val;

            sum_R += R_val;
            sum_G += G_val;
            sum_B += B_val;

            z_above_threshold.add(z_val);
            R_above_threshold.add(R_val);
            G_above_threshold.add(G_val);
            B_above_threshold.add(B_val);
            i_above_threshold.add(i_val);

            n_above_t_hold++;

            if(z_val > max_z)
                max_z = z_val;
            if(z_val < min_z)
                min_z = z_val;

            if(R_val > max_R)
                max_R = R_val;
            if(R_val < min_R)
                min_R = R_val;

            if(G_val > max_G)
                max_G = G_val;
            if(G_val < min_G)
                min_G = G_val;

            if(B_val > max_B)
                max_B = B_val;
            if(B_val < min_B)
                min_B = B_val;


            if(i_val > max_i)
                max_i = i_val;
            if(i_val < min_i)
                min_i = i_val;

        }

        /* Should we have insufficient number of points to calculate metrics,
          then we just output NaN for all metrics.
         */
        if(z_above_threshold.size() < cutoff_n_points){

            int p_size = (int)Math.ceil((0.95 / this.percentile_step_orig));

            colnames.add("max_z" + suffix);
            output.add(Double.NaN);
            colnames.add("min_z" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_z" + suffix);
            output.add(Double.NaN);
            colnames.add("median_z" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_z" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_z" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_z" + suffix);
            output.add(Double.NaN);

            colnames.add("max_i" + suffix);
            output.add(Double.NaN);
            colnames.add("min_i" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_i" + suffix);
            output.add(Double.NaN);
            colnames.add("median_i" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_i" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_i" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_i" + suffix);
            output.add(Double.NaN);



            int counter22 = 0;

            double[] percentiles_names = new double[p_size];
            double percentile_step_z2 = percentile_step_orig;
            for(int i = 0; i < p_size; i++){

                percentiles_names[i] = percentile_step_z2;
                percentile_step_z2 += percentile_step_orig;

            }

            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (double)(Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
                output.add(Double.NaN);

            }
            counter22 = 0;
            for(int i = 0 ; i < p_size; i++){

                colnames.add("p_" + (double)(Math.round(percentiles_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
                output.add(Double.NaN);
            }
            for(int i = 0; i < densities.length; i++) {

                colnames.add("d_" + densities[i] + "_z" + suffix);
                output.add(Double.NaN);
            }

            colnames.add("max_R" + suffix);
            output.add(Double.NaN);
            colnames.add("min_R" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_R" + suffix);
            output.add(Double.NaN);
            colnames.add("median_R" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_R" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_R" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_R" + suffix);
            output.add(Double.NaN);

            colnames.add("max_G" + suffix);
            output.add(Double.NaN);
            colnames.add("min_G" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_G" + suffix);
            output.add(Double.NaN);
            colnames.add("median_G" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_G" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_G" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_G" + suffix);
            output.add(Double.NaN);

            colnames.add("max_B" + suffix);
            output.add(Double.NaN);
            colnames.add("min_B" + suffix);
            output.add(Double.NaN);
            colnames.add("sd_B" + suffix);
            output.add(Double.NaN);
            colnames.add("median_B" + suffix);
            output.add(Double.NaN);
            colnames.add("mean_B" + suffix);
            output.add(Double.NaN);
            colnames.add("skewness_B" + suffix);
            output.add(Double.NaN);
            colnames.add("kurtosis_B" + suffix);
            output.add(Double.NaN);


            return output;
        }

        mean_z = sum_z / (double)z_above_threshold.size();

        mean_R = sum_R / (double)z_above_threshold.size();
        mean_G = sum_G / (double)z_above_threshold.size();
        mean_B = sum_B / (double)z_above_threshold.size();

        mean_i = sum_i / (double)i_above_threshold.size();


        double median_z = -1, median_i = -1;
        double median_R = -1;
        double median_G = -1;
        double median_B = -1;

        double z_skewness_v = 0;
        double R_skewness_v = 0;
        double G_skewness_v = 0;
        double B_skewness_v = 0;
        double i_skewness_v = 0;

        double z_kurtosis_v = 0;
        double R_kurtosis_v = 0;
        double G_kurtosis_v = 0;
        double B_kurtosis_v = 0;
        double z_kurtosis_v_2 = 0;
        double R_kurtosis_v_2 = 0;
        double G_kurtosis_v_2 = 0;
        double B_kurtosis_v_2 = 0;

        double i_kurtosis_v = 0;
        double i_kurtosis_v_2 = 0;

        int densities_counter = 0;


        double sum_z_above = 0.0;
        double sum_i_above = 0.0;

        for(int i = 0; i < z_above_threshold.size(); i++){

            z_val = z_above_threshold.get(i);
            R_val = R_above_threshold.get(i);
            G_val = G_above_threshold.get(i);
            B_val = B_above_threshold.get(i);

            i_val = i_above_threshold.get(i);

            z_sum_run += z_val;
            R_sum_run += R_val;
            G_sum_run += G_val;
            B_sum_run += B_val;

            i_sum_run += (double)i_val;

            sd_z += ((z_val - mean_z) * (z_val - mean_z));
            sd_R += ((R_val - mean_R) * (R_val - mean_R));
            sd_G += ((G_val - mean_G) * (G_val - mean_G));
            sd_B += ((B_val - mean_B) * (B_val - mean_B));

            sd_i += ((i_val - mean_i) * (i_val - mean_i));

            z_skewness_v += ((z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z) );
            R_skewness_v += ((R_val - mean_R) * (R_val - mean_R) * (R_val - mean_R) );
            G_skewness_v += ((G_val - mean_G) * (G_val - mean_G) * (G_val - mean_G) );
            B_skewness_v += ((B_val - mean_B) * (B_val - mean_B) * (B_val - mean_B) );

            i_skewness_v += ((i_val - mean_i) * (i_val - mean_i) * (i_val - mean_i) );

            z_kurtosis_v += ((z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z) * (z_val - mean_z));
            R_kurtosis_v += ((R_val - mean_R) * (R_val - mean_R) * (R_val - mean_R) * (R_val - mean_R));
            G_kurtosis_v += ((G_val - mean_G) * (G_val - mean_G) * (G_val - mean_G) * (G_val - mean_G));
            B_kurtosis_v += ((B_val - mean_B) * (B_val - mean_B) * (B_val - mean_B) * (B_val - mean_B));

            z_kurtosis_v_2 += ((z_val - mean_z) * (z_val - mean_z));
            R_kurtosis_v_2 += ((R_val - mean_R) * (R_val - mean_R));
            G_kurtosis_v_2 += ((G_val - mean_G) * (G_val - mean_G));
            B_kurtosis_v_2 += ((B_val - mean_B) * (B_val - mean_B));

            i_kurtosis_v += ((i_val - mean_i) * (i_val - mean_i) * (i_val - mean_i) *  (i_val - mean_i));
            i_kurtosis_v_2 += ((i_val - mean_i) * (i_val - mean_i));

            if(z_val < 0)
                z_val = 0;
            if(i_val < 0)
                i_val = 0;

        }

        sd_z = sd_z / (n_above_t_hold-1);
        sd_R = sd_R / (n_above_t_hold-1);
        sd_G = sd_G / (n_above_t_hold-1);
        sd_B = sd_B / (n_above_t_hold-1);

        sd_i = sd_i / (n_above_t_hold-1);

        sd_z = Math.sqrt(sd_z);
        sd_R = Math.sqrt(sd_R);
        sd_G = Math.sqrt(sd_G);
        sd_B = Math.sqrt(sd_B);

        sd_i = Math.sqrt(sd_i);

        double skewness_z = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * z_skewness_v / (sd_z*sd_z*sd_z);
        double skewness_R = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * R_skewness_v / (sd_R*sd_R*sd_R);
        double skewness_G = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * G_skewness_v / (sd_G*sd_G*sd_G);
        double skewness_B = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * B_skewness_v / (sd_B*sd_B*sd_B);

        double skewness_i = (n_above_t_hold / ((n_above_t_hold -1.0) * (n_above_t_hold - 2.0)) ) * i_skewness_v / (sd_i*sd_i*sd_i);


/*
        double kurtosis_z = z_kurtosis_v / (double)z.size() / (sd_z*sd_z*sd_z*sd_z) - 3.0;
        double kurtosis_i = i_kurtosis_v / (double)intensity.size() / (sd_i*sd_i*sd_i*sd_i) - 3.0;

 */
        double kurtosis_z = z_kurtosis_v / n_above_t_hold / ( ( z_kurtosis_v_2 / n_above_t_hold ) * ( z_kurtosis_v_2 / n_above_t_hold )) ;
        double kurtosis_R = R_kurtosis_v / n_above_t_hold / ( ( R_kurtosis_v_2 / n_above_t_hold ) * ( R_kurtosis_v_2 / n_above_t_hold )) ;
        double kurtosis_G = G_kurtosis_v / n_above_t_hold / ( ( G_kurtosis_v_2 / n_above_t_hold ) * ( G_kurtosis_v_2 / n_above_t_hold )) ;
        double kurtosis_B = B_kurtosis_v / n_above_t_hold / ( ( B_kurtosis_v_2 / n_above_t_hold ) * ( B_kurtosis_v_2 / n_above_t_hold )) ;

        double kurtosis_i = i_kurtosis_v / n_above_t_hold / ( ( i_kurtosis_v_2 / n_above_t_hold ) * ( i_kurtosis_v_2 / n_above_t_hold ));


        Collections.sort(i_above_threshold);
        Collections.sort(z_above_threshold);
        Collections.sort(R_above_threshold);
        Collections.sort(G_above_threshold);
        Collections.sort(B_above_threshold);

        if (z_above_threshold.size() % 2 == 0) {
            median_z = ( ((double) z_above_threshold.get(z_above_threshold.size() / 2) + (double) z_above_threshold.get(z_above_threshold.size() / 2 - 1) ) / 2.0);
            median_R = ( ((double) R_above_threshold.get(R_above_threshold.size() / 2) + (double) R_above_threshold.get(R_above_threshold.size() / 2 - 1) ) / 2.0);
            median_G = ( ((double) G_above_threshold.get(G_above_threshold.size() / 2) + (double) G_above_threshold.get(G_above_threshold.size() / 2 - 1) ) / 2.0);
            median_B = ( ((double) B_above_threshold.get(B_above_threshold.size() / 2) + (double) B_above_threshold.get(B_above_threshold.size() / 2 - 1) ) / 2.0);

            median_i = ( ((double) i_above_threshold.get(i_above_threshold.size() / 2) + (double) i_above_threshold.get(i_above_threshold.size() / 2 - 1)) / 2.0);
        }
        else {
            median_z = (double) z_above_threshold.get(z_above_threshold.size() / 2);
            median_R = (double) R_above_threshold.get(R_above_threshold.size() / 2);
            median_G = (double) G_above_threshold.get(G_above_threshold.size() / 2);
            median_B = (double) B_above_threshold.get(B_above_threshold.size() / 2);

            median_i = (double) i_above_threshold.get(i_above_threshold.size() / 2);
        }


        //skewness_z = 1.0 / (sd_z*sd_z*sd_z) * (z_skewness_v / (double)z.size());

        //percentiles_z[percentiles_z.length-1] = z.get(z.size()-1);
        //percentiles_i[percentiles_i.length-1] = intensity.get(intensity.size()-1);

        for(int j = 0 ; j < densities.length; j++){

            densitiesOutput[j] /= (double)z.size();

        }
/*
        System.out.println(Arrays.toString(percentiles_z) + " " + z.get(z.size()-1));
        System.out.println(Arrays.toString(densitiesOutput));
        System.out.println(sd_z + " " + sd_i + " " + skewness_z + " " + kurtosis_z);
*/
        colnames.clear();

        int counter = 0;

        double[] z_array = ArrayUtils.toPrimitive(z_above_threshold.toArray(new Double[z_above_threshold.size()]));
        double[] i_array = new double[i_above_threshold.size()];

        for(int i = 0; i < i_above_threshold.size(); i++){
            i_array[i] = (double)i_above_threshold.get(i);
        }


        Percentile p_z = new Percentile(this.percentile_step_orig);
        Percentile p_i = new Percentile(this.percentile_step_orig);

        p_z.setData(z_array);
        p_i.setData(i_array);

        int pers_size = (int)Math.ceil((0.95 / percentile_step_z));

        double[] percentiles_z = new double[pers_size];
        double[] percentiles_z_names = new double[pers_size];
        double[] percentiles_i = new double[pers_size];
        double[] percentiles_i_names = new double[pers_size];

        for(int i = 0; i < percentiles_z.length; i++){

            percentiles_z[i] = p_z.evaluate(percentile_step_z * 100.0);
            percentiles_z_names[i] = percentile_step_z;
            percentiles_i[i] = p_i.evaluate(percentile_step_z * 100.0);
            percentiles_i_names[i] = percentile_step_z;
            percentile_step_z += percentile_step_orig;

        }

        //System.out.println("end: ");



        colnames.add("max_z" + suffix);
        output.add(max_z);
        colnames.add("min_z" + suffix);
        output.add(min_z);
        colnames.add("sd_z" + suffix);
        output.add(sd_z);
        colnames.add("median_z" + suffix);
        output.add(median_z);
        colnames.add("mean_z" + suffix);
        output.add(mean_z);
        colnames.add("skewness_z" + suffix);
        output.add(skewness_z);
        colnames.add("kurtosis_z" + suffix);
        output.add(kurtosis_z);
        colnames.add("max_i" + suffix);
        output.add((double)max_i);
        colnames.add("min_i" + suffix);
        output.add((double)min_i);
        colnames.add("sd_i" + suffix);
        output.add(sd_i);
        colnames.add("median_i" + suffix);
        output.add((median_i));
        colnames.add("mean_i" + suffix);
        output.add(mean_i);
        colnames.add("skewness_i" + suffix);
        output.add(skewness_i);
        colnames.add("kurtosis_i" + suffix);
        output.add(kurtosis_i);
        counter = 0;
        for(int i = 0 ; i < percentiles_z.length; i++){

            colnames.add("p_" + (double)(Math.round(percentiles_z_names[i] * 100.0d) / 100.0d) + "_z" + suffix);
            output.add(percentiles_z[counter++]);

        }
        counter = 0;
        for(int i = 0 ; i < percentiles_i.length; i++){

            colnames.add("p_" + (double)(Math.round(percentiles_i_names[i] * 100.0d) / 100.0d) + "_i" + suffix);
            output.add(percentiles_i[counter++]);
        }
        for(int i = 0; i < densities.length; i++){

            colnames.add("d_" + densities[i] + "_z" + suffix);
            output.add(densitiesOutput[i]);

        }

        colnames.add("max_R" + suffix);
        output.add(max_R);
        colnames.add("min_R" + suffix);
        output.add(min_R);
        colnames.add("sd_R" + suffix);
        output.add(sd_R);
        colnames.add("median_R" + suffix);
        output.add(median_R);
        colnames.add("mean_R" + suffix);
        output.add(mean_R);
        colnames.add("skewness_R" + suffix);
        output.add(skewness_R);
        colnames.add("kurtosis_R" + suffix);
        output.add(kurtosis_R);

        colnames.add("max_G" + suffix);
        output.add(max_G);
        colnames.add("min_G" + suffix);
        output.add(min_G);
        colnames.add("sd_G" + suffix);
        output.add(sd_G);
        colnames.add("median_G" + suffix);
        output.add(median_G);
        colnames.add("mean_G" + suffix);
        output.add(mean_G);
        colnames.add("skewness_G" + suffix);
        output.add(skewness_G);
        colnames.add("kurtosis_G" + suffix);
        output.add(kurtosis_G);

        colnames.add("max_B" + suffix);
        output.add(max_B);
        colnames.add("min_B" + suffix);
        output.add(min_B);
        colnames.add("sd_B" + suffix);
        output.add(sd_B);
        colnames.add("median_B" + suffix);
        output.add(median_B);
        colnames.add("mean_B" + suffix);
        output.add(mean_B);
        colnames.add("skewness_B" + suffix);
        output.add(skewness_B);
        colnames.add("kurtosis_B" + suffix);
        output.add(kurtosis_B);

/*
        System.out.println(Arrays.toString(colnames.toArray()));
        System.out.println(Arrays.toString(output.toArray()));
*/
        return output;
    }


    public static <T extends Comparable<T>> void concurrentSort(
            final List<T> key, List<?>... lists){
        // Create a List of indices
        List<Integer> indices = new ArrayList<Integer>();
        for(int i = 0; i < key.size(); i++)
            indices.add(i);

        // Sort the indices list based on the key
        Collections.sort(indices, new Comparator<Integer>(){
            @Override public int compare(Integer i, Integer j) {
                return key.get(i).compareTo(key.get(j));
            }
        });

        // Create a mapping that allows sorting of the List by N swaps.
        // Only swaps can be used since we do not know the type of the lists
        Map<Integer,Integer> swapMap = new HashMap<Integer, Integer>(indices.size());
        List<Integer> swapFrom = new ArrayList<Integer>(indices.size()),
                swapTo   = new ArrayList<Integer>(indices.size());
        for(int i = 0; i < key.size(); i++){
            int k = indices.get(i);
            while(i != k && swapMap.containsKey(k))
                k = swapMap.get(k);

            swapFrom.add(i);
            swapTo.add(k);
            swapMap.put(i, k);
        }

        // use the swap order to sort each list by swapping elements
        for(List<?> list : lists)
            for(int i = 0; i < list.size(); i++)
                Collections.swap(list, swapFrom.get(i), swapTo.get(i));
    }

}
