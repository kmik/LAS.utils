package LASio;/* --------------------------------------------------------------------
 * Copyright 2018 Mikko Kukkonen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---------------------------------------------------------------------
 */

 /*
 * -----------------------------------------------------------------------
 *
 * Revision History:
 * Date Name Description
 * ------ --------- -------------------------------------------------
 * 03/2018 Mikko Kukkonen
 *
 * Notes:
 *
 * -----------------------------------------------------------------------
 */

import java.util.HashMap;
import java.util.HashSet;
//import org.tinfour.*;

/**
 *	Can be used to queru whether or not
 *	to include a point based on a variety
 * 	of conditions.
 */

public class PointInclusionRule{


	boolean applyWhenReading = true;

	LasPoint tempPoint = new LasPoint();

	int keep_classification = -999;
	int drop_classification = -999;

	boolean have_drop_rule = false;
	boolean have_keep_rule = false;

	boolean drop_noise = false;

	boolean keep_only = false;
	boolean drop_only = false;

	double drop_z_below = -999;
	double drop_z_above = -999;

	double scaleFactor = -999;

	boolean first_only = false;
	boolean keep_first = false;
	boolean drop_first = false;

	boolean last_only = false;
	boolean keep_last = false;
	boolean keep_intermediate = false;

	boolean drop_last = false;
	boolean drop_intermediate = false;

	boolean drop_first_of_many = false;
	boolean drop_last_of_many = false;

	boolean keep_middle = false;
	boolean drop_middle = false;

	boolean keep_single = false;
	boolean drop_single = false;

	boolean keep_double = false;
	boolean drop_double = false;

	boolean keep_triple = false;
	boolean drop_triple = false;

	boolean keep_quadruple = false;
	boolean drop_quadruple = false;

	boolean keep_quintuple = false;
	boolean drop_quintuple = false;


	boolean drop_synthetic = false;
	int drop_user_data = -999;
	int keep_user_data = -999;

	double translate_x = -999;
	double translate_y = -999;
	double translate_z = -999;

    int translate_i = -999;
	// MODIFY POINT RULES:

	int set_classification = -999;
	int set_user_data = -999;
	int set_point_source_id = -999;

	HashSet<Integer> keepIndex = new HashSet<Integer>();

	HashSet<Integer> dropIndex = new HashSet<Integer>();

	HashSet<Integer> modifyIndex = new HashSet<Integer>();
	HashMap<Integer,Double> modifyWith = new HashMap<Integer, Double>();

	HashSet<Integer> ignoreIndex = new HashSet<Integer>();

	boolean modifyAll = false;
	PointModifyRule prule = new PointModifyRule();

	HashSet<Integer> keep_classificationSet = new HashSet<Integer>();

	boolean drop_not_in_ModifyIndex = false;

	boolean remove_buffer = false;

	int changePointFormat = -999;

	int drop_scan_angle_below = -999;
	int drop_scan_angle_above = -999;


	/*
	NEW ONES! THESE ARE NOT IN THE SCRIPT FILES!
	*/

	int drop_intensity_below = -999;
	int drop_intensity_above = -999;

	int set_point_source = -999;

	int drop_point_source_below = -999;
	int drop_point_source_above = -999;

	int drop_point_source = -999;
	int keep_point_source = -999;

	boolean thin3d = false;


	//ÁrrayList<Rule> ruleOrder = new ArrayList<Rule>();
	

	public PointInclusionRule(){

	}

	/**
	 *	Constructor:
	 *
	 *	@param in 	True for write, false for read.
	 *
	 */

	public PointInclusionRule(boolean in){
		
		applyWhenReading = in;

	}

	public void applyWhenReading(){

		this.applyWhenReading = true;

	}

	public void applyWhenWriting(){

		this.applyWhenReading = false;

	}

	public void keepClassification(int in){

		keep_classification = in;

		this.have_keep_rule = true;
	} 

	public void keepClassification(HashSet<Integer> in){

		keep_classificationSet = in;
		this.have_keep_rule = true;

	} 

	public void dropClassification(int in){

		drop_classification = in;
		this.have_drop_rule = true;

	} 

	public void dropNoise(){

		drop_noise = true;
		this.have_drop_rule = true;
	}

	public void dropSynthetic(){

		drop_synthetic = true;
		this.have_drop_rule = true;
	}

	public void removeBuffer(){

		this.remove_buffer = true;
		this.have_drop_rule = true;
	}

	public void undoRemoveBuffer(){

		this.remove_buffer = false;
		this.have_drop_rule = false;

	}

	public void dropZBelow(double in){

		drop_z_below = in;
		this.have_drop_rule = true;
	}

	public void dropZAbove(double in){

		drop_z_above = in;
		this.have_drop_rule = true;
	}

	public void drop_scan_angle_below(int in){

		drop_scan_angle_below = in;
		this.have_drop_rule = true;
	}

	public void drop_scan_angle_above(int in){

		drop_scan_angle_above = in;
		this.have_drop_rule = true;
	}

	public void scaleFactor(double in){

		scaleFactor = in;

	}

	public void firstOnly(){

		first_only = true;
		this.have_keep_rule = true;

	}

	public void keepFirst(){

		keep_first = true;
		this.have_keep_rule = true;

	}

	public void dropFirst(){

		drop_first = true;
		this.have_drop_rule = true;
	}

	public void lastOnly(){

		last_only = true;
		this.have_keep_rule = true;

	}

	public void keepLast(){

		keep_last = true;
		this.have_keep_rule = true;

	}

	public void keepIntermediate(){

		keep_intermediate = true;
		this.have_keep_rule = true;

	}

	public void dropIntermediate(){

		drop_intermediate = true;
		this.have_drop_rule = true;
	}

	public void dropLast(){

		drop_last = true;
		this.have_drop_rule = true;
	}

	public void keepOnly(){

		keep_only = true;
		this.have_keep_rule = true;

	}

	public void dropOnly(){

		drop_only = true;
		this.have_drop_rule = true;
	}

	public void dropFirstOfMany(){

		drop_first_of_many = true;
		this.have_drop_rule = true;
	}

	public void dropLastOfMany(){

		drop_last_of_many = true;
		this.have_drop_rule = true;
	}

	public void keepMiddle(){

		keep_middle = true;
		this.have_keep_rule = true;

	}

	public void dropMiddle(){

		drop_middle = true;
		this.have_drop_rule = true;
	}

	public void keepSingle(){

		keep_single = true;
		this.have_keep_rule = true;

	}

	public void dropSingle(){

		drop_single = true;
		this.have_drop_rule = true;
	}

	public void keepDouble(){

		keep_double = true;
		this.have_keep_rule = true;

	}

	public void dropDouble(){

		drop_double = true;
		this.have_drop_rule = true;
	}

	public void keepTriple(){

		keep_triple = true;
		this.have_keep_rule = true;

	}

	public void dropTriple(){

		drop_triple = true;
		this.have_drop_rule = true;
	}

	public void keepQuadruple(){

		keep_quadruple = true;
		this.have_keep_rule = true;

	}

	public void dropQuadruple(){

		drop_quadruple = true;
		this.have_drop_rule = true;
	}

	public void keepQuintuple(){

		keep_quintuple = true;
		this.have_keep_rule = true;

	}

	public void dropQuintuple(){

		drop_quintuple = true;
		this.have_drop_rule = true;
	}

	public void dropUserData(int in){

		drop_user_data = in;
		this.have_drop_rule = true;
	}

	public void keepUserData(int in){

		keep_user_data = in;
		this.have_keep_rule = true;

	}

	public void setClassification(int in){

		set_classification = in;

	}

	public void setUserData(int in){

		set_user_data = in;
	}

	public void setPointSourceId(int in){

		set_point_source_id = in;
	}

	public void changePointFormat(int toFormat){

		this.changePointFormat = toFormat;

	}

	public void translate_x(double in){

		this.translate_x = in;

	}

	public void translate_y(double in){

		this.translate_y = in;

	}

	public void translate_z(double in){

		this.translate_z = in;

	}

    public void translate_i(int in){

        this.translate_i = in;

    }


	/**
	 *	Queries whether to include a point and
	 * 	how to modify it.
	 *
	 *	@param tempPoint	Input LAS point
	 *	@param i 			Point index in LAS file
	 * 	@param areYouReading 			True while writing, false reading
	 *	@return 			True if the point is kept
	 *						false if the point is discarded 			
	 */


	public boolean ask(LasPoint tempPoint, int i, boolean areYouReading){


		if(areYouReading == applyWhenReading){

			/* Here we remove points that do not care for modifications */
			if(remove_buffer) {
				if (tempPoint.synthetic) {
					return false;
				}
			}

			/*

			 	First we need to check if there is any reason to drop the
			 	point. If a flag wants to drop it, no other reason to keep
			 	it matters!

			 */

			if(drop_scan_angle_above != -999)
				if(Math.abs(tempPoint.scanAngleRank) > drop_scan_angle_above)
					return false;

			if(drop_scan_angle_below != -999)
				if(Math.abs(tempPoint.scanAngleRank) < drop_scan_angle_below)
					return false;

			if(drop_first)
				if(tempPoint.returnNumber == 1)
					return false;


			if(drop_last)
				if(tempPoint.returnNumber == tempPoint.numberOfReturns)
					return false;

			if(drop_z_below != -999.0)
				if(drop_z_below > tempPoint.z)
					return false;

			if(drop_z_above != -999.0)
				if(drop_z_above < tempPoint.z)
					return false;

			if(drop_user_data != -999)
				if(tempPoint.userData == drop_user_data)
					return false;

			if(drop_classification != -999) {
				if (tempPoint.classification == drop_classification) {
					return false;
				}
			}

			if(keep_classification != -999){
				if(tempPoint.classification != keep_classification)
					return false;
			}


			if(drop_synthetic)
				if(tempPoint.synthetic)
					return false;

			if(drop_noise)
				if(tempPoint.classification == 7) {
					//System.out.println("DROP NOISE!");
					//return false;
				}


			if(drop_single)
				if(tempPoint.numberOfReturns == 1)
					return false;

			if(drop_intermediate)
				if(tempPoint.returnNumber > 1 && tempPoint.returnNumber < tempPoint.numberOfReturns)
					return false;


			/*

			 	Next we have to see if the point in index i
			 	has to be modified somehow. It could also be
			 	kept or discarded.

			 */

			/* No keep rules, so we don't need to check them */
			if(!have_keep_rule) {

				modifyPoint(tempPoint);
				return true;

			}


			/* Since we got here, we are fairly sure we want
			to keep some points.
			 */

			byte n_keeps = 0;
			byte n_no_keeps = 0;

			if(keep_intermediate)
				if(tempPoint.returnNumber > 1 && tempPoint.returnNumber < tempPoint.numberOfReturns)
					n_keeps++;
				else
					n_no_keeps++;


			if(keep_first)
				if(tempPoint.returnNumber == 1)
					n_keeps++;
				else
					n_no_keeps++;


			if(keep_last)
				if(tempPoint.returnNumber == tempPoint.numberOfReturns)
					n_keeps++;
				else
					n_no_keeps++;

			if(keep_user_data != -999)
				if(tempPoint.userData == keep_user_data)
					n_keeps++;
				else
					n_no_keeps++;


			/* Now we check the "keeps". */
			if(keep_classification != -999)
				if(tempPoint.classification == keep_classification)
					n_keeps++;
				else
					n_no_keeps++;



			if(n_keeps > 0){
				modifyPoint(tempPoint);
				return true;
			}else if(n_no_keeps == 0){
				modifyPoint(tempPoint);
				return true;
			}else
				return false;

		}

		return true;

	}

	
	public void modifyPoint(LasPoint tempPoint){

		/* We also modify the point */
		if(set_point_source_id != -999){
			tempPoint.pointSourceId = (short)this.set_point_source_id;
		}
		if(this.translate_x != -999)
			tempPoint.x += this.translate_x;
		if(this.translate_y != -999)
			tempPoint.y += this.translate_y;
		if(this.translate_z != -999)
			tempPoint.z += this.translate_z;

		if(this.set_classification != -999)
			tempPoint.classification = this.set_classification;

		if(this.set_user_data != -999)
			tempPoint.userData = this.set_user_data;


	}
	

}