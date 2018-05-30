#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Must specify path to swirlds sdk!"
    echo "e.g."
    echo "\`./run.bash /path/to/sdk\`"
    exit
fi


FN="SharedWorld.jar"


ln -sf $1 ../sdk

RESULT=`gradle jar`
if [[ ${RESULT} != *"BUILD SUCCESSFUL"* ]];then
  echo "gradle jar failed!"
  exit
fi


#Get the most up-to-date .jar:
###JARFILENAME=`ls -tr ./build/libs/*.jar | tail -n 1`
#Just filename...
###JARFILENAME=$(basename "$JARFILENAME")
#Copy to swirlds sdk directory:
###cp ./build/libs/$JARFILENAME $1/data/apps/
#Copy most recent jar file:
cp `ls -tr ./build/libs/*.jar | tail -n 1` $1/data/apps/$FN

echo "Built successfully."
echo ""



#Push to sdk directory!
pushd $1

#First comment out ALL apps from config.txt
sed -i '' '/^ *app./s/^/#/g' config.txt

#Add our app to official sdk:
#First get linenumnber of last occurance of "app,"
LINENUMBER=`cat config.txt | grep -n 'app,' | tail -n1 | cut -d: -f1`
#Add new line at LINENUMBER:
awk "NR==$LINENUMBER{print \"app,          $FN\"}1" config.txt > tmp && mv tmp config.txt
#sed $LINENUMBER"a\
#app,          $JARFILENAME" < config.txt

java -jar swirlds.jar

#Pop back to original directory
popd

# echo "***Now run with:***"
# echo "----->>>     java -jar $1/swirlds.jar"
# echo ""
