#!/bin/bash
# 04-14-2020 Joe Troy script to create WAR file - to be called by capistrano

# Capistrano should change directory to where app is deployed to
echo "From create_vireo_war_file.sh, the current folder is" `pwd`

# now run maven
mvn clean package -Dproduction -Dassets.uri=file:/home/vireo/vireo4_data/assets



