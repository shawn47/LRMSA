package com.iise.shawn.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;

import org.processmining.framework.log.AuditTrailEntry;
import org.processmining.framework.log.LogFile;
import org.processmining.framework.log.ProcessInstance;
import org.processmining.framework.log.rfb.LogData;

public class FileReaderUtil {
	
	public static LinkedList<LinkedList<String>> readTextLog(String logFolderRoute) throws Exception{
		LinkedList<LinkedList<String>> eventLogs = new LinkedList<LinkedList<String>>();
		
		File file = new File(logFolderRoute);
		for(File logFile:file.listFiles()){
			if(!logFile.getName().endsWith("txt"))
				continue;
			FileReader fr = new FileReader(logFile);
			BufferedReader br = new BufferedReader(fr);
			
			String log = br.readLine();
			if(log==null){
				continue;
			}
			String[] events = log.split(",");
			LinkedList<String> eventLog = new LinkedList<String>();
			for(String event:events){
				eventLog.add(event);
			}
			eventLogs.add(eventLog);
			
			br.close();
			fr.close();
		}
		
		return eventLogs;
	}
	
	public static LinkedList<LinkedList<String>> readMxmlLog(String logFileRoute) throws Exception{
		LinkedList<LinkedList<String>> eventLogs = new LinkedList<LinkedList<String>>();
		
		LogFile logFile = LogFile.getInstance(logFileRoute);
		LogData data = LogData.createInstance(logFile);
		
		for(ProcessInstance process:data.instances()){
			LinkedList<String> eventLog = new LinkedList<String>();
			for(AuditTrailEntry ate:process.getListOfATEs()){
				eventLog.add(ate.getName());
			}
			eventLogs.add(eventLog);
		}
		
		return eventLogs;
	}
}
