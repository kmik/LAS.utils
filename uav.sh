
export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib
curDir=$(dirname $0)
FILE1=$1
FILE2=$2

FILE3=$3
FILE4=$4

#javac -cp ".:src/JavaMI.jar:$curDir/src/Tinfour-1.0.jar:$curDir/src/:$curDir/src/gdal.jar:$curDir/src/commons-collections4-4.1.jar:$curDir/src/jai_imageio-1.1.jar:$curDir/src/opencv-320.jar:$curDir/src/metadata-extractor-2.10.1.jar:$curDir/src/xmpcore-5.1.2.jar" $curDir/src/gdalE.java 

#java -Djava.library.path=$curDir/gdal/java/ -cp ".:$curDir/lib/*:$curDir/target/" -Xmx8g tools.uav "$FILE1" "$FILE2" "$FILE3" "$FILE4"
java -Djava.library.path=$curDir/gdal/java/ -cp ".:$curDir/lib/*:$curDir/target/" -Xmx8g tools.uav "$FILE1" "$FILE2"
