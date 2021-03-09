
curDir=$(dirname $0)

java -Xmx2g -Djava.library.path=$curDir/gdal/java/ -cp ".:$curDir/lib/*:$curDir/target/" lasStripTest
