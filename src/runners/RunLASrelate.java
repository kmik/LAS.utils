package runners;/* --------------------------------------------------------------------
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
import LASio.*;
import java.io.*;
import java.util.ArrayList;


import tools.*;

class RunLASrelate{

	public static void main(String[] args) throws IOException {

		ArrayList<String> filesListPointCloud = new ArrayList<String>();
		ArrayList<String> filesListReference = new ArrayList<String>();

		ArrayList<LASReader> pointClouds = new ArrayList<LASReader>();
		ArrayList<LASReader> references = new ArrayList<LASReader>();

		filesListPointCloud = MKid4pointsLAS.listFiles("/media/koomikko/B8C80A93C80A4FD41/id4points/LASutils/lasrelate_xy_test/all_point_clouds/clipped_done/relT/", ".las");
		filesListReference = MKid4pointsLAS.listFiles("/media/koomikko/B8C80A93C80A4FD41/id4points/LASutils/lasrelate_xy_test/titan/relT/", ".las");


		for(String i : filesListPointCloud){

			pointClouds.add(new LASReader(new File(i)));

		}

		for(String i : filesListReference){

			references.add(new LASReader(new File(i)));

		}

		int axGrid = 10;

		LasRelate rel = new LasRelate(pointClouds, references, axGrid);

	}
}