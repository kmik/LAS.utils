
curDir=$(dirname $0)

javac -cp ".:$curDir/src/Tinfour-1.0.jar:$curDir/src/:$curDir/src/gdal.jar:$curDir/src/commons-collections4-4.1.jar:$curDir/src/jai_imageio-1.1.jar:$curDir/src/opencv-320.jar:$curDir/src/metadata-extractor-2.10.1.jar:$curDir/src/xmpcore-5.1.2.jar" "$curDir"/src/gdalE.java

javac -cp ".:$curDir/src/Tinfour-1.0.jar:$curDir/src/:$curDir/src/gdal.jar:$curDir/src/commons-collections4-4.1.jar:$curDir/src/jai_imageio-1.1.jar:$curDir/src/opencv-320.jar:$curDir/src/metadata-extractor-2.10.1.jar:$curDir/src/xmpcore-5.1.2.jar" "$curDir"/src/createCHM.java

javac -cp ".:$curDir/src/Tinfour-1.0.jar:$curDir/src/:$curDir/src/gdal.jar:$curDir/src/commons-collections4-4.1.jar:$curDir/src/jai_imageio-1.1.jar:$curDir/src/opencv-320.jar:$curDir/src/metadata-extractor-2.10.1.jar:$curDir/src/xmpcore-5.1.2.jar" rgb.java

java -Djava.library.path=$curDir/proj-4.8.0/gdal-2.2.3/swig/java -cp ".:$curDir/src/Tinfour-1.0.jar:$curDir/src/:$curDir/src/gdal.jar:$curDir/src/commons-collections4-4.1.jar:$curDir/src/jai_imageio-1.1.jar:$curDir/src/opencv-320.jar:$curDir/src/metadata-extractor-2.10.1.jar:$curDir/src/xmpcore-5.1.2.jar" rgb
