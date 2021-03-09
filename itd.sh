javac -cp ".:src/Tinfour-1.0.jar:src/:src/gdal.jar:src/commons-collections4-4.1.jar:src/jai_imageio-1.1.jar:src/opencv-320.jar:src/metadata-extractor-2.10.1.jar:src/xmpcore-5.1.2.jar:src/commons-math3-3.6.1.jar:src/quickhull3d.jar" src/runners.ITDtest.java
echo "Compiled OK!"
java -Xmx8g -Djava.library.path=./proj-4.8.0/gdal-2.2.3/swig/java -cp ".:src/Tinfour-1.0.jar:src/:src/gdal.jar:src/commons-collections4-4.1.jar:src/jai_imageio-1.1.jar:src/opencv-320.jar:src/metadata-extractor-2.10.1.jar:src/xmpcore-5.1.2.jar:src/commons-math3-3.6.1.jar:src/quickhull3d.jar" runners.ITDtest
