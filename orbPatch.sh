#!/bin/bash
# Patches the REDHAWK IDE Jacorb to work with the newest versions of JAVA.
# This patch can be applied to all versions of REDHAWK
# To apply the patch copy this script into the same directory as the eclipse executable
# Ensure your current directory is in the same directory as the eclipse executable
# Execute the patch script

function error() {
	echo "Auto configure ORB REDHAWK IDE Failed!"
	exit 1
}

IDE_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "Updating Eclipse.ini file:"
if grep -q -e "-Djava.endorsed.dirs="  eclipse.ini ; then
	echo "Changing line -Djava.endorsed.dirs=${IDE_HOME}/jacorb/lib" 
	sed -i -e "s#^-Djava\.endorsed\.dirs.*#-Djava.endorsed.dirs=${IDE_HOME}/jacorb/lib#" eclipse.ini || error
else 
	echo "Appending line"
	echo "-Djava.endorsed.dirs=${IDE_HOME}/jacorb/lib" | tee -a eclipse.ini || error
fi

if [ ! -d jacorb ] ; then 
	echo "Creating Jacorb Lib directory..."
	JACORB_JAR=`find plugins/ -maxdepth 1 -name org.jacorb.system*` || error 
	mkdir jacorb || error
	if [ -d "$JACORB_JAR" ] ; then 
		echo "Copying Jacorb to Lib directory..."
		mkdir jacorb/lib || error
		cp -R $JACORB_JAR/jars/* jacorb/lib/. || error
	else 
		echo "Extracting Jacorb to Lib directory..."
		cd jacorb 
		jar xf ../$JACORB_JAR || error
	fi
fi

