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
      "Name"                      : "GenericInterfaceTest",
      "Version"                   : "v1.0-r5.6",
      "BuildNumber"               : time.strftime("%y%m%d", time.localtime(time.time()))+"1",
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
# Events
#------------------------------------------------------------------------------#
config["Events"] = \
   {
      "EventResourceFile"    : ".\\resources\\events\\events.er",
   }


#------------------------------------------------------------------------------#
# Java    
#------------------------------------------------------------------------------#
config["Java"] = \
   {
      "BasePackage"          : "postilion.realtime.genericinterface",
       "ClassPaths"                : \
        [
           ".\\resources\\lib\\commonclasslibrary.jar",
		   ".\\resources\\lib\\json-simple-1.1",
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
				 "Depends"		: [("CommonClassInterface","0.1")]
			 }
	}

#------------------------------------------------------------------------------#
# Database																	   #
#------------------------------------------------------------------------------#
# config["Database"] = \
   # [
      # (
         # "realtime",
         # {	
			# "SourceDirs"	      : \
				# [
					# (".\\source\\sql\\version2.5", RealtimeAppBuilder.INCLUDE_NO_RECURSE),
				# ],
			# "UpgradeObjects" : \
				# [
					# ("SQL", "realtime",
						# [
                            # ("FILE", ".\\build\\sql\\intermediate\\realtime_cust_equivalent_response_code_upgrade_data.*"),
                            # ("FILE", ".\\build\\sql\\intermediate\\realtime_cust_tm_config_tran_1_upgrade_data.*"),
                            # ("FILE", ".\\build\\sql\\intermediate\\realtime_cust_tm_config_tran_2_upgrade_data.*"),
                            # ("FILE", ".\\build\\sql\\intermediate\\realtime_cust_tm_config_tran_3_upgrade_data.*"),
                            # ("FILE", ".\\build\\sql\\intermediate\\realtime_cust_tm_config_tran_4_upgrade_data.*"),
							# ("FILE", ".\\build\\sql\\intermediate\\realtime_cust_tm_config_tran_5_upgrade_data.*"),
						# ]
					# ),
				# ],
         # }
      # )
   # ]

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
