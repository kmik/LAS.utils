
curDir=$(dirname $0)
curDir="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib:$curDir/gdal/java
export GDAL_DATA=$curDir/gdal/gdal_data
echo $LD_LIBRARY_PATH 

java -Xmx16g -Djava.library.path=$curDir/gdal/java/ -cp ".:$curDir/lib/*:$curDir/target/" stemDetectionTest /home/koomikko/Documents/processing_directory/riegl/144_merge_fl_ud_RE_NI_R_1_1.las

