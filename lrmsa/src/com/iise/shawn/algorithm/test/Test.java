package com.iise.shawn.algorithm.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jbpt.petri.NetSystem;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Transition;
import org.processmining.importing.pnml.PnmlImport;

import com.iise.shawn.algorithm.AlignmentAlgorithm;
import com.iise.shawn.algorithm.MyAlgorithm;
import com.iise.shawn.main.GenerateTrace;
import com.iise.shawn.ssd.SSD;
import com.iise.shawn.util.AlgorithmType;
import com.iise.shawn.util.FileReaderUtil;
import com.iise.shawn.util.IndexUtil;
import com.iise.shawn.util.PetriNetConversion;

import cern.colt.matrix.DoubleMatrix2D;


public class Test {
	private static AlignmentAlgorithm aA = new AlignmentAlgorithm();
	private static MyAlgorithm mA = new MyAlgorithm();
	
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
		System.out.println("============= init... ============= ");
		long startTime = 0, endTime = 0;
		HashMap<String,Transition> transMapModel = IndexUtil.getTransMap(model);
		aA = new AlignmentAlgorithm();
		aA.setNet(model);
		aA.setTransMap(transMapModel);
		
		mA = new MyAlgorithm();
		NetSystem ns = PetriNetConversion.convert(model);
		System.out.println("===== start Getting CPU Status ===== ");
		startTime = System.nanoTime();
		CompletePrefixUnfolding cpu = new CompletePrefixUnfolding(ns);
		endTime = System.nanoTime();
		System.out.println("===== end Getting CPU Status ===== ");
		
		SSD ssd = new SSD();
		ssd.initSSD(cpu);
		ArrayList<String> alOrder_cfp = new ArrayList<String>();
		System.out.println("===== start SSD Computation Status ===== ");
		startTime = System.nanoTime();
		DoubleMatrix2D ssdMatrix = ssd.computeSSD(cpu, alOrder_cfp);
		endTime = System.nanoTime();
		System.out.println("===== end SSD Computation Status ===== ");
		System.out.println("init SSD time consumed: " + (endTime - startTime));
		mA.setModel(model);
		mA.setCPU(cpu);
		mA.setSSD(ssd);
		mA.setSSDMatrix(ssdMatrix);
		mA.setOrderCPU(alOrder_cfp);
		System.out.println("============= init accomplished ============= ");
	}
	
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void test(PetriNet model, String logRoute,
			int randomTime, double errorPercent, AlgorithmType algoType) throws Exception{
		long startTime = 0, endTime = 0;
		LinkedList<Transition> tau = null;
		List<String> rtn = null;
		
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
		
//		gTrace.generateMisOrderTraceList();
		if(algoType == AlgorithmType.alignment) {
			System.out.println("============= Alignment Status ============= ");
		}
		else if (algoType == AlgorithmType.myAlgorithm) {
			System.out.println("============= MyAlgorithm Status ============= ");
		}
		else if (algoType == AlgorithmType.debug) {
			System.out.println("============= Comparision Status ============= ");
		}
		
		for (ArrayList<String> eventLog : gTrace.traceList) {
			Comparator cmp = Collections.reverseOrder();  
			LinkedList<String> log = new LinkedList<String>();
			for (String item : eventLog) {
				log.add(item);
			}
			// log is now in misorder
			Collections.sort(log, cmp);
			// change log into multiset
			Map<String, Integer> multiset = new HashMap<String, Integer>();
			for (int i = 0; i < log.size(); i++) {
				if (!multiset.containsKey(log.get(i))) {
					multiset.put(log.get(i), 1);
				}
				else {
					int count = multiset.get(log.get(i));
					multiset.put(log.get(i), ++count);
				}
			}
			
			if(algoType == AlgorithmType.alignment) {
				int[] count = new int[1];
				count[0] = 0;
				System.out.println("raw log: " + log);
				
				startTime = System.nanoTime();
				tau = aA.repair(model, log, count);
				endTime = System.nanoTime();

				System.out.println("result log: " + tau);
				System.out.println("time consumed: " + (endTime - startTime));
				System.out.println("backtrack num: " + count[0]);
			}
			else if (algoType == AlgorithmType.myAlgorithm) {
				System.out.println("raw log: " + multiset);
				
				startTime = System.nanoTime();
				rtn = mA.repair(multiset);
				endTime = System.nanoTime();
				

				System.out.println("result log: " + rtn);
				System.out.println("time consumed: " + (endTime - startTime));
			}
			else if (algoType == AlgorithmType.debug) {
				System.out.println("raw log: " + log);
				System.out.println("raw multiset: " + multiset);
				System.out.println("===== for Alignment ===== ");
				int[] count = new int[1];
				count[0] = 0;
				startTime = System.nanoTime();
				tau = aA.repair(model, log, count);
				endTime = System.nanoTime();
				
				System.out.println("result log: " + tau);
				System.out.println("backtrack num: " + count[0]);
				System.out.println("time consumed for Alignment: " + (endTime - startTime));
				
				System.out.println("===== for MyAlgo ===== ");
				startTime = System.nanoTime();
				rtn = mA.repair(multiset);
				endTime = System.nanoTime();
				System.out.println("result log: " + rtn);
				System.out.println("time consumed for myAlgo: " + (endTime - startTime));
			}
		}
	}
	
	public static void repair(String petriNetPath, String logPath) throws Exception	
	{
		PnmlImport pnmlImport = new PnmlImport();
		PetriNet model = pnmlImport.read(new FileInputStream(new File(petriNetPath)));
		initializeAlgorithm(model);
//		test(model, logPath, 1, 0, AlgorithmType.alignment);
//		test(model, logPath, 1, 0, AlgorithmType.myAlgorithm);
		test(model, logPath, 1, 0, AlgorithmType.debug);
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
