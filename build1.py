import os
import sys
import time
import RealtimeAppBuilder

#==============================================================================#
# Realtime Framework Builder Configuration
#==============================================================================#

config = {}

#------------------------------------------------------------------------------#
# Project   
#------------------------------------------------------------------------------#
config["Project"] = \
{
      "Name"                      : "GenericInterface1",
      "Version"                   : "v1.0-r5.6",
      "BuildNumber"               : time.strftime("%y%m%d", time.localtime(time.time()))+"1"
}

#------------------------------------------------------------------------------#
# BuildEnvironment   
#------------------------------------------------------------------------------#
config["BuildEnvironment"] = \
   {
      "JDKHome"                   : "C:\\Program Files\\Postilion\\realtime\\jdk",
      "OutputDir"                 : ".\\build"
   }


#------------------------------------------------------------------------------#
# Tasks   
#------------------------------------------------------------------------------#
config["Tasks"] = \
{
      "GenericInterface1"    :
         {
            "TaskType"            : RealtimeAppBuilder.TASK_TYPE_INTERCHANGE,
            "Service"             : True,
            "Description"         : "GenericInterface",
            "MainClass"           : "postilion.realtime.sdk.env.App",
            "ClassArguments"      : 
               [
                  "GenericInterface1",
                  0,
                  "postilion.realtime.sdk.node.InterchangeProcessor",
                  "postilion.realtime.sdk.node.Interchange",
                  "postilion.realtime.genericinterface.GenericInterface"
               ]
         }
}

#------------------------------------------------------------------------------#
# Events
#------------------------------------------------------------------------------#
#config["Events"] = \
#  {
#      "EventResourceFile"    : ".\\resources\\events\\events.er",
#   }


#------------------------------------------------------------------------------#
# Java    
#------------------------------------------------------------------------------#
config["Java"] = \
   {
      "BasePackage"          : "postilion.realtime.genericinterface",
       "ClassPaths"                : \
        [
           ".\\resources\\lib\\commonclasslibrary.jar"
        ],
      "SourceDirs"           : \
         [
            (".\\source\\java", RealtimeAppBuilder.INCLUDE_RECURSE)
         ],
   }


#------------------------------------------------------------------------------#
# Documentation																					 #
#------------------------------------------------------------------------------#
config["Documentation"] = \
	{
		 "userguide" 			: 
            {                 	
				 "Name"			: "User Guide",
				 "Type"			: "CHM",
				 "SourceDir"	: "doc\\userguide",
				 "Project"		: "ug_GenericInterface",
				 "Replacements"	        :   
					 {           	
						 "Files"	: ["Title.htm"]
					 }
			 },
		 "releasenotes" 		: 
			 {                 	
				 "Name"			: "Release Notes",
				 "Type"			: "CHM",
				 "SourceDir"	: "doc\\releasenotes",
				 "Project"		: "rn_GenericInterface",
			 }
	}


#------------------------------------------------------------------------------#
# Release									 #
#------------------------------------------------------------------------------#

config["Release"] = \
	{
		"Packaging"				: \
			[	
				(RealtimeAppBuilder.WINDOWS_ONLY,"build\\install\\standard_edition\\setup.exe", "setup.exe"),
			]
	}
	
#==============================================================================#
# Realtime Framework Builder										          #
#==============================================================================#

# Get the target and any target arguments that are present
target = RealtimeAppBuilder.getTargetName(sys.argv)
target_args = RealtimeAppBuilder.getTargetArguments(sys.argv)
   
# Build the target project.
RealtimeAppBuilder.RealtimeAppBuilder(config).buildProject(build_target=target, build_target_args=target_args)
