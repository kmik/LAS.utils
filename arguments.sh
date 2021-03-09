#!/bin/bash

echo ' _       ___   _____         _    _  _      '  
echo '| |     / _ \ /  ___|       | |  (_)| |     '  
echo '| |    / /_\ \\ `--.  _   _ | |_  _ | | ___ ' 
echo '| |    |  _  | `--. \| | | || __|| || |/ __|'
echo '| |____| | | |/\__/ /| |_| || |_ | || |\__ \'
echo '\_____/\_| |_/\____/  \__,_| \__||_||_||___/'
echo '                                            '
echo '      LASutils (LASutils version 0.1)  '

cat << EOF

Arguments to use with LASutils scripts:


-iparse 		Input .txt file column order.
-oparse			Output .txt file column order.
	
		-iparse and -oparse are input as e.g. xyzi.

		x = x coordinate
		y = y -:-
		z = z -:-
		i = intensity
		c = classification
		n = number of returns of a given pulse
		r = return number of a given echo
		p = point source ID
		t = GPS time
		s = SKIP (i.e. in read ignore that column
		    and in write create a column of zeroes)

-i 			Input file.
-o			Output file.
-odir			Output directory. Used in example
			with lastile.

-drop_noise		Remove noise from output file.
-drop_classification	Remove points with a classification
			from output file.

-first_only		Keep only first echoes in the output.
-drop_first		Remove first echoes from the output.
-keep_first		Keep -......

-last_only		Keep only last echoes in the output.
-drop_last		Remove last echoes from the output.
-keep_last		Keep -......

-drop_first_of_many	Remove points that are the first echo
			of many echoes of a single pulse.
-drop_last_of_many	Remove points that are the last echo
			of many echoes of a single pulse.

-keep_middle		Keep points that are not the first and
			not the last echo of a pulse that has
			more than 2 echoes.
-drop_middle		Remove points that are not the first and
			not the last echo of a pulse that has
			more than 2 echoes.

-keep_single		Keep single echoes.
-drop_single		Remove single echoes.
-keep_double		...
-drop_double		...
-keep_triple		...
-drop_triple		...
-keep_quadruple		...
-drop_quadruple		...
-keep_quintuple		...
-drop_quintuple		...

-drop_synthetic		Remove points that are flagged synthetic.

-drop_user_data		Remove points that have the given user data.
-keep_user_data		Keep points that have the given user data.

-set_classification	Set the given classification to all points.
-set_user_data		Set the given user data to all points.

-drop_z_below		Remove points that have a z value below the 
			given threshold. ATTENTION! This works
			with lasheight!
-drop_z_above		Remove points that have a z value above the 
			given threshold. ATTENTION! This works
			with lasheight!

-translate_x		
-translate_y
-translate_z

-buffer			A buffer area given to tiles in lastiles.
			This can be reversed in any tool by using
			the -bb flag (basically ignores points flagged
			as synthetic).
-remove_buffer		Ignore points marked as buffer. Actually ignored
			points flagged as synthetic.

-poly			Input polygon.

-step			Used in various algorithms to define a certain
			perimeter / increment. 

-few			Used in thinning to define the threshold of minimum
			points per unit.			

-kernel			Side length of a square window. Used e.g. in 
			las2chm to define the kernel of gaussian smoothing

-theta			Theta parameter of gaussian smoothing.

-ground_class		Classification ID of ground points. By default this 
			is the ASPRS LAS standard (2).

-thin2d			Used to select whether thinning is done in 2D.
-thin3d			Used to select whether thinning is done in 3D (default).


EOF



