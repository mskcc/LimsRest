#!/bin/bash
currentUser=$(whoami)
if [[ $currentUser != "sapio-tomcat" ]];then
   echo "Must be run as sapio-tomcat"
   exit 1
fi
tomcatLocation=/srv/www/sapio/tomcat7
currentDir=$(pwd)
unset -v newestLims
for file in LimsRest*.war; do
  [[ $file -nt $newestLims ]] && newestLims=$file
done
cd $tomcatLocation
jar xvf $newestLims WEB-INF/classes/connect.txt
machine=$(hostname)
machineName=${machine/.*/}
machineMatch=$(grep $machineName WEB-INF/classes/connect.txt)
cd $currentDir
if [[ -z $machineMatch ]];then
   echo "It appears $newestLims is not built for machine $machineName. Not deploying."
else
   unset -v deployedLims
   for file in $tomcatLocation/webapps/LimsRest*.war; do
       deployedLims=$file
   done
   if [[ -f $deployedLims ]];then
       echo "Undeploying $deployedLims"
       mv $deployedLims $tomcatLocation 
       sleep 6 
   fi
   echo "Deploying $newestLims"  
   cp $newestLims $tomcatLocation/webapps
fi
exit
