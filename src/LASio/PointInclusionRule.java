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


	/* 	Means if we want to apply
		the rule while reading (false) or 
		writing (true)
	*/
	boolean readOrWrite = true;


	LasPoint tempPoint = new LasPoint();

	int keep_classification = -999;
	int drop_classification = -999;

	boolean drop_noise = false;

	double drop_z_below = -999;
	double drop_z_above = -999;

	double scaleFactor = -999;

	boolean first_only = false;
	boolean keep_first = false;
	boolean drop_first = false;

	boolean last_only = false;
	boolean keep_last = false;
	boolean drop_last = false;

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

    double translate_i = -999;
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
		
		readOrWrite = in;

	}

	public void keepClassification(int in){

		keep_classification = in;

	} 

	public void keepClassification(HashSet<Integer> in){

		keep_classificationSet = in;

	} 

	public void dropClassification(int in){

		drop_classification = in;

	} 

	public void dropNoise(){

		drop_noise = true;
		
	}

	public void dropSynthetic(){

		drop_synthetic = true;
		
	}

	public void removeBuffer(){
		this.remove_buffer = true;
	}

	public void dropZBelow(double in){

		drop_z_below = in;
		
	}

	public void dropZAbove(double in){

		drop_z_above = in;
		
	}

	public void keepIndexes(HashSet<Integer> in){

		keepIndex = in;
		
	}

	public void modifyIndexes(HashSet<Integer> in, PointModifyRule rule, boolean dropOutside, boolean modAll){

		this.drop_not_in_ModifyIndex = dropOutside;
		modifyIndex = in;

		//if(in.size() == 0)
			//modifyAll = true;
		modifyAll = modAll;
		prule = rule;
	}

	public void IgnoreIndexes(HashSet<Integer> in){

		ignoreIndex = in;

	}

	public void dropIndexes(HashSet<Integer> in){

		dropIndex = in;
		
	}

	public void scaleFactor(double in){

		scaleFactor = in;

	}

	public void firstOnly(){

		first_only = true;

	}

	public void keepFirst(){

		keep_first = true;

	}

	public void dropFirst(){

		drop_first = true;

	}

	public void lastOnly(){

		last_only = true;

	}

	public void keepLast(){

		keep_last = true;

	}

	public void dropLast(){

		drop_last = true;

	}

	public void dropFirstOfMany(){

		drop_first_of_many = true;

	}

	public void dropLastOfMany(){

		drop_last_of_many = true;

	}

	public void keepMiddle(){

		keep_middle = true;

	}

	public void dropMiddle(){

		drop_middle = true;

	}

	public void keepSingle(){

		keep_single = true;

	}

	public void dropSingle(){

		drop_single = true;

	}

	public void keepDouble(){

		keep_double = true;

	}

	public void dropDouble(){

		drop_double = true;

	}

	public void keepTriple(){

		keep_triple = true;

	}

	public void dropTriple(){

		drop_triple = true;

	}

	public void keepQuadruple(){

		keep_quadruple = true;

	}

	public void dropQuadruple(){

		drop_quadruple = true;

	}

	public void keepQuintuple(){

		keep_quintuple = true;

	}

	public void dropQuintuple(){

		drop_quintuple = true;

	}

	public void dropUserData(int in){

		drop_user_data = in;

	}

	public void keepUserData(int in){

		keep_user_data = in;

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


	public void translate_x(double in){

		this.translate_x = in;

	}

	public void translate_y(double in){

		this.translate_y = in;

	}

	public void translate_z(double in){

		this.translate_z = in;

	}

    public void translate_i(double in){

        this.translate_i = in;

    }


	/**
	 *	Queries whether to include a point and
	 * 	how to modify it.
	 *
	 *	@param tempPoint	Input LAS point
	 *	@param i 			Point index in LAS file
	 * 	@param io 			True while writing, false reading
	 *	@return 			True if the point is kept
	 *						false if the point is discarded 			
	 */


	public boolean ask(LasPoint tempPoint, int i, boolean io){



		/** Here we do removes that are done regardless of read or write */

		if(io == false && remove_buffer) {
			if (tempPoint.synthetic) {
				return false;
			}
		}

		/** We also modify the point */

		if(set_point_source_id != -999){
			tempPoint.pointSourceId = (short)this.set_point_source_id;
		}



		/*
		Check if IO equals the intended use of this rule
		*/

		if(keep_classification != -999){
			if(tempPoint.classification == keep_classification)
				return true;
			else
				return false;
		}

		if(io == readOrWrite){

			boolean drop_z_belowT = true;
			boolean drop_z_aboveT = true;


			/*
			Innocent until proven guilty
			*/

			boolean output = true;

			/**
			 *
			 *	First we need to check if there is any reason to drop the
			 *	point. If a flag wants to drop it, no other reason to keep
			 *	it matters!
			 *
			 */


			if(drop_first == true)
				if(tempPoint.returnNumber == 1)
					return false;

			if(drop_z_below != -999.0)
				if(drop_z_below > tempPoint.z)
					return false;

			if(drop_z_above != -999.0)
				if(drop_z_above < tempPoint.z)
					return false;

			if(dropIndex.size() > 0)
				if(dropIndex.contains(i))
					return false;

			if(drop_user_data != -999)
				if(tempPoint.userData == drop_user_data)
					return false;

			if(drop_classification != -999)
				if(tempPoint.classification == drop_classification){
					
					return false;
				}



			if(drop_synthetic == true)
				if(tempPoint.synthetic == true)
					return false;

			if(drop_noise == true)
				if(tempPoint.classification == 7)
					return false;
			
			if(drop_last == true)
				if(tempPoint.returnNumber == tempPoint.numberOfReturns)
					return false;

			if(drop_first_of_many == true)
				if(tempPoint.returnNumber == 1 && tempPoint.numberOfReturns != 1)
					return false;

			if(drop_last_of_many == true)
				if(tempPoint.numberOfReturns != 1 && tempPoint.returnNumber == tempPoint.numberOfReturns)
					return false;

			if(drop_single == true)
				if(tempPoint.numberOfReturns == 1)
					return false;

			if(drop_double == true)
				if(tempPoint.numberOfReturns == 2)
					return false;	

			if(drop_triple == true)
				if(tempPoint.numberOfReturns == 3)
					return false;

			if(drop_quadruple == true)
				if(tempPoint.numberOfReturns == 4)
					return false;

			if(drop_quintuple == true)
				if(tempPoint.numberOfReturns == 5)
					return false;




			/**
			 *
			 *	Next we have to see if the point in index i
			 *	has to be modified somehow. It could also be
			 *	kept or discarded.
			 *
			 */

			if(keepIndex.size() > 0)
				if(keepIndex.contains(i))
					output = true;

			if(dropIndex.size() > 0)
				if(dropIndex.contains(i))
					return false;

			if(modifyIndex.size() > 0){
				if(modifyIndex.contains(i)){
					//System.out.println("!!!");
					if(!prule.modify(tempPoint))
						return false;
				}
				else if(drop_not_in_ModifyIndex)
					return false;
			}

			if(modifyAll)
				prule.modify(tempPoint);
				//else
					//return false;


			/**
			 *
			 *	This is a little confusing.
			 *
			 */


				
			if(first_only == true)
				if(tempPoint.returnNumber == 1)
					return true;
				else
					return false;

			if(last_only == true)
				if(tempPoint.returnNumber == tempPoint.numberOfReturns)
					return true;
				else
					return false;


			if(keep_first == true)
				if(tempPoint.returnNumber == 1)
					return true;
				else
					return false;

			if(keep_last == true)
				if(tempPoint.returnNumber == tempPoint.numberOfReturns)
					return true;
				else
					return false;

			if(keep_middle == true)
				if(tempPoint.numberOfReturns != 1 && tempPoint.returnNumber != 1 && tempPoint.returnNumber != tempPoint.numberOfReturns)
					return true;
				else
					return false;

			if(keep_double == true)
				if(tempPoint.numberOfReturns == 2)
					return true;
				else
					return false;

			if(keep_triple == true)
				if(tempPoint.numberOfReturns == 3)
					return true;
				else
					return false;

			if(keep_quadruple == true)
				if(tempPoint.numberOfReturns == 4)
					return true;
				else
					return false;

			if(keep_quintuple == true)
				if(tempPoint.numberOfReturns == 5)
					return true;
				else
					return false;

			if(keep_user_data != -999)
				if(tempPoint.userData == keep_user_data)
					return true;
				else
					return false;


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


			// MODIFY POINT DATA RULES!
				/*
			if(output == true){

				if(set_classification != -999)
					tempPoint.classification = set_classification;

				if(set_user_data != -999)
					tempPoint.userData = set_user_data;

			}
			*/
			//System.out.println(drop_classification);


			return output;
		}

		return true;

	}

	

	

}