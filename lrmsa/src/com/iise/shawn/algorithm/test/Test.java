package com.iise.shawn.algorithm.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Transition;
import org.processmining.importing.pnml.PnmlImport;

import com.iise.shawn.alignment.AlignmentAlgorithm;
import com.iise.shawn.main.GenerateTrace;
import com.iise.shawn.util.AlgorithmType;
import com.iise.shawn.util.FileReaderUtil;
import com.iise.shawn.util.IndexUtil;


public class Test {
	private static AlignmentAlgorithm aA = new AlignmentAlgorithm();
	private static Random ran = new Random(1);
	private static GenerateTrace gTrace = new GenerateTrace();
	
	public static LinkedList<String> randomError(ArrayList<String> eventLog, double percent){
		LinkedList<String> log = new LinkedList<String>();
		log.addAll(eventLog);
		int errorNumber = (int) (log.size()*percent);
		
		if(errorNumber==0){
			errorNumber++;
		}
		if(errorNumber==eventLog.size()){
			errorNumber--;
		}
		
		//System.out.println(errorNumber);
		while(errorNumber!=0){
			//System.out.println(log);
			int index = ran.nextInt(log.size()-1);
			log.remove(index);
			errorNumber--;
		}
		
		return log;
	}
	
	public static void initializeAlgorithm(PetriNet model){
		HashMap<String,Transition> transMapModel = IndexUtil.getTransMap(model);
		aA = new AlignmentAlgorithm();
		aA.setNet(model);
		aA.setTransMap(transMapModel);
	}
	
	
	
	@SuppressWarnings("unchecked")
	public static void test(PetriNet model, String logRoute,
			int randomTime, double errorPercent, AlgorithmType algoType) throws Exception{
		
//		String modelName = logRoute.substring(logRoute.lastIndexOf("/")+1, logRoute.indexOf(".pnml.mxml"));
//		LinkedList<LinkedList<String>> eventLogs = FileReaderUtil.readMxmlLog(logRoute);
//		for(LinkedList<String> eventLog:eventLogs){
//			Comparator cmp = Collections.reverseOrder();  
//			LinkedList<String> log = new LinkedList<String>();
//			for (String item : eventLog) {
//				log.add(item);
//			}
//			// sort the list
//			Collections.sort(log, cmp); 
//			System.out.println("raw log: " + log);
//			long startTime = 0, endTime = 0;
//			int count = 0;
//			LinkedList<Transition> tau = null;
//			if(algoType == AlgorithmType.alignment){
//				startTime =  System.currentTimeMillis();
//				tau = aA.repair(model, log);
//				endTime =  System.currentTimeMillis();
//			}
//			System.out.println("result log: " + tau);
//			System.out.println("time consumed: " + (endTime - startTime));
//		}
		
		gTrace.generateMisOrderTraceList();
		
		for (ArrayList<String> eventLog : gTrace.traceList) {
			Comparator cmp = Collections.reverseOrder();  
			LinkedList<String> log = new LinkedList<String>();
			for (String item : eventLog) {
				log.add(item);
			}
			// sort the list
			Collections.sort(log, cmp);
			System.out.println("raw log: " + log);
			long startTime = 0, endTime = 0;
			int[] count = new int[1];
			count[0] = 0;
			LinkedList<Transition> tau = null;
			if(algoType == AlgorithmType.alignment){
				startTime =  System.currentTimeMillis();
				tau = aA.repair(model, log, count);
				endTime =  System.currentTimeMillis();
			}
			System.out.println("result log: " + tau);
			System.out.println("time consumed: " + (endTime - startTime));
			System.out.println("backtrack num: " + count[0]);
		}
	}
	
	public static void repair(String petriNetPath, String logPath) throws Exception	
	{
		PnmlImport pnmlImport = new PnmlImport();
		PetriNet model = pnmlImport.read(new FileInputStream(new File(petriNetPath)));
		initializeAlgorithm(model);
		test(model, logPath, 1, 0, AlgorithmType.alignment);
	}
	
	public static void main(String args[]) throws Exception
	{
//		String petriNetPath = "/Users/shawn/Documents/LAB/开题/exp/BeehiveZ+jBPT+PIPE/bpm/笑尘代码/data/causal/FI.403.pnml";
//		String petriNetPath = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/double_loop_nested.pnml";
//		String petriNetPath = "/Users/shawn/Documents/LAB/开题/exp/BeehiveZ+jBPT+PIPE/bpm/笑尘代码/data/all_Loop/FI.106.pnml";
		String petriNetPath = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/Double_Loop_XOR.pnml";
//		String logPath = "/Users/shawn/Documents/LAB/开题/exp/BeehiveZ+jBPT+PIPE/bpm/笑尘代码/data/causal/log/FI.403.pnml.mxml";
		
		String logPath = "/Users/shawn/Documents/LAB/开题/exp/BeehiveZ+jBPT+PIPE/bpm/笑尘代码/data/all_Loop/log/FI.106.pnml.mxml";
		
		gTrace.init();
		gTrace.generateTrace(petriNetPath, "file");
		
		repair(petriNetPath, logPath);
	}
}
