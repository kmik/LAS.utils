
curDir=$(dirname $0)

java -Djava.library.path=$curDir/gdal/java/ -cp ".:$curDir/lib/*:$curDir/target/" testClass
