@echo off
set curDir=%~dp0
SET PATH=%PATH%;%curDir%lib;%curDir%usr\lib;%curDir%usr\local\lib;%curDir%gdal\java
SET GDAL_DATA=$curDir/gdal/gdal_data	

java -Xmx16g -Djava.library.path=%curDir%gdal\java -classpath %curDir%lib\*;%curDir%target runners.RunId4pointsLAS 1 %*

set +f

