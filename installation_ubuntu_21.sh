#!/bin/bash

sudo apt-get install -y libatlas3-base
sudo cp ./gdal/java/libdap.so.25 /usr/local/lib
sudo cp ./gdal/java/libarmadillo.so.9 /usr/local/lib
