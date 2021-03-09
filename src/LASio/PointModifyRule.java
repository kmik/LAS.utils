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

import java.util.List;
//import org.tinfour.*;	

/**
 *	Used by pointincludionrule to modify LAS
 *	points.
 */


public class PointModifyRule{


	int setClassification = -999;
	int setUserData = -999;
	boolean synthetic = false;
	short setPointSourceId = -999;

	//int dropClassification =-999;

	boolean normalize = false;

	org.tinfour.standard.IncrementalTin tin;
	org.tinfour.interpolation.TriangularFacetInterpolator polator;
	org.tinfour.interpolation.VertexValuatorDefault valuator = new org.tinfour.interpolation.VertexValuatorDefault();

	public PointModifyRule(){


	}

	public void setClassification(int in){

		this.setClassification = in;

	}

	/*
	public void dropClassification(int in){

		this.dropClassification = in;

	}
	*/
	
	public void setUserData(int in){

		this.setUserData = in;

	}
	public void synthetic(){

		this.synthetic = true;

	}

	public void setPointSourceId(short in){

		this.setPointSourceId = in;

	}

	public void normalize(org.tinfour.standard.IncrementalTin tin1){

		this.normalize = true;
		this.tin = tin1;
		polator = new org.tinfour.interpolation.TriangularFacetInterpolator(tin);

	}


	/**
 	 *	Modify the input point according to rules.
 	 *
 	 *	@param tempPoint 		Input point
	 *
	 *	@return true if point is kept, false otherwise (deprecated)
 	 */

	public boolean modify(LasPoint tempPoint){

		//System.out.println("AGOY");



		if(setClassification != -999){
			if(tempPoint.classification != setClassification)
				tempPoint.classification = setClassification;
			//else
			//	tempPoint.classification = 0;

		}

		if(setUserData != -999)
			tempPoint.userData = setUserData;

		if(synthetic)
			tempPoint.synthetic = true;

		if(setPointSourceId != -999)
			tempPoint.pointSourceId = setPointSourceId;

		if(normalize){

			double interpolatedValue = polator.interpolate(tempPoint.x, tempPoint.y, valuator);
			
			if(!Double.isNaN(interpolatedValue)){

					tempPoint.z = tempPoint.z - interpolatedValue;
					//System.out.println("GOT HERE!");
			}
			else{

				List<org.tinfour.common.Vertex> closest = tin.getNeighborhoodPointsCollector().collectNeighboringVertices(tempPoint.x, tempPoint.y, 0, 0);

				tempPoint.z = tempPoint.z - closest.get(0).getZ();
				//System.out.println(closest.get(0).getZ());	

			}

		}
		/*
		if(dropClassification != -999)
			if(tempPoint.classification == dropClassification)
				return false;
		*/
		return true;

	}

}
