package com.iise.shawn.algorithm.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import com.iise.shawn.util.IndexUtil;
import com.iise.shawn.util.PetriNetConversion;

import cern.colt.matrix.DoubleMatrix2D;


public class Test {
	private static AlignmentAlgorithm aA = new AlignmentAlgorithm();
	private static MyAlgorithm mA = new MyAlgorithm();
	
	private static Random ran = new Random(1);
	private static GenerateTrace gTrace = new GenerateTrace();
	
	private static String dataFileName;
	
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
	
	public static void initializeAlgorithm(PetriNet model, String modelName, String dataPath, int loopNum, AlgorithmType algoType) throws IOException{
//		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		DateFormat dateFormat = new SimpleDateFormat("MMdd-HH-mm");
		Date date = new Date();
		System.out.println(dateFormat.format(date));
		
		dataFileName = dataPath + modelName + "_" + dateFormat.format(date) + "_" + loopNum + ".txt";
		
		File output = new File(dataFileName);
		FileWriter fw = new FileWriter(output);
		BufferedWriter bw = new BufferedWriter(fw);
		
		System.out.println("============= init... ============= ");
		bw.write("============= init... ============= \n");
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
		System.out.println("getting CPU time consumed: " + (endTime - startTime));
		bw.write("CPU Time Consumed:\t" + (endTime - startTime) + "\n");
		
		SSD ssd = new SSD();
		ssd.initSSD(cpu);
		ArrayList<String> alOrder_cfp = new ArrayList<String>();
		System.out.println("===== start SSD Computation Status ===== ");
		startTime = System.nanoTime();
		DoubleMatrix2D ssdMatrix = ssd.computeSSD(cpu, alOrder_cfp);
		endTime = System.nanoTime();
		System.out.println("===== end SSD Computation Status ===== ");
		System.out.println("init SSD time consumed: " + (endTime - startTime));
		bw.write("SSD Time Consumed:\t" + (endTime - startTime) + "\n");
		mA.setModel(model);
		mA.setCPU(cpu);
		mA.setSSD(ssd);
		mA.setSSDMatrix(ssdMatrix);
		mA.setOrderCPU(alOrder_cfp);
		mA.initSSDMatrixNew();
		bw.write("Trace Number:\t" + (gTrace.traceList.size()) + "\n");
		System.out.println("============= init accomplished ============= ");
		bw.write("============= init accomplished ============= \n");
		bw.close();
		fw.close();
	}
	
	
	
	public static void test(String modelFilePath, PetriNet model, String modelName, String dataPath,
			int loopNum, double errorPercent, AlgorithmType algoType) throws Exception{
		
		gTrace.init();
		gTrace.generateTrace(modelFilePath, "file", loopNum);
		
		initializeAlgorithm(model, modelName, dataPath, loopNum, algoType);
		
		long startTime = 0, endTime = 0, delta1 = 0, delta2 = 0;
		float rateSum = 0;
		LinkedList<Transition> tau = null;
		List<String> rtn = null;
		
		// record experiment data
		File output = new File(dataFileName);
		FileWriter fw = new FileWriter(output, true);
		BufferedWriter bw = new BufferedWriter(fw);

		if(algoType == AlgorithmType.alignment) {
			System.out.println("============= Alignment Status ============= ");
		}
		else if (algoType == AlgorithmType.myAlgorithm) {
			System.out.println("============= MyAlgorithm Status ============= ");
		}
		else if (algoType == AlgorithmType.debug) {
			System.out.println("============= Comparision Status ============= ");
			bw.write("============= Comparision Status ============= \n");
		}
		
		float[] timeRate = new float[gTrace.traceList.size()];
		int index = 0;
		for (ArrayList<String> eventLog : gTrace.traceList) {
//			Comparator cmp = Collections.reverseOrder(); 
//			Comparator cmp = Collections.shuffle();
			LinkedList<String> log = new LinkedList<String>();
			for (String item : eventLog) {
				log.add(item);
			}
			// log is now in misorder
			Collections.shuffle(log);
			String rawLogString = "";
			for (int ievent = 0; ievent < log.size() - 1; ievent++) {
				rawLogString += log.get(ievent) + ", ";
			}
			rawLogString += log.get(log.size() - 1);
			
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
				
				LinkedList<String> log2 = new LinkedList<String>();
				String input = "I, K, C, G, B, H, H, G, F, C, H, G, C, H, J, I, E, B, H, A, J, B, G, G, I, B";
				String[] inputArray = input.split(", ");
				for (String itm : inputArray) {
					log2.add(itm);
				}
				System.out.println("raw log: " + log2);
				startTime = System.nanoTime();
				tau = aA.repair(model, log2, count);
				endTime = System.nanoTime();

				System.out.println("result log: " + tau);
				System.out.println("time consumed: " + (endTime - startTime));
				System.out.println("backtrack num: " + count[0]);
			}
			else if (algoType == AlgorithmType.myAlgorithm) {
				System.out.println("raw log: " + multiset);
				
				startTime = System.nanoTime();
				rtn = mA.repair2(multiset);
				endTime = System.nanoTime();

				System.out.println("result log: " + rtn);
				System.out.println("time consumed: " + (endTime - startTime));
			}
			else if (algoType == AlgorithmType.debug) {
				long delta2f = Long.MAX_VALUE;
				for (int loop = 0; loop < 10; loop++) {
					Map<String, Integer> multisetNewData = new HashMap<String, Integer>();
					for (int i = 0; i < log.size(); i++) {if (!multisetNewData.containsKey(log.get(i))) {
							multisetNewData.put(log.get(i), 1);
						}
						else {
							int count1 = multisetNewData.get(log.get(i));
							multisetNewData.put(log.get(i), ++count1);
						}
					}
					if (loop == 0) {
						bw.write("raw multiset:\t" + multisetNewData + "\n");
						System.out.println("===== for MyAlgo ===== ");
						bw.write("===== for MyAlgo ===== \n");
					}
					startTime = System.nanoTime();
					rtn = mA.repair2(multisetNewData);
					endTime = System.nanoTime();
					delta2 = endTime - startTime;
					if (delta2 < delta2f) {
						delta2f = delta2;
					}
				}
				
				System.out.println("result log: " + rtn);
				System.out.println("time consumed for myAlgo: " + delta2f);
				bw.write("result log:\t" + rtn + "\n");
				bw.write("time consumed for myAlgo:\t" + delta2f + "\n");
				
				
				int[] count = new int[1];
				count[0] = 0;
				
				long delta1f = Long.MAX_VALUE;
				for (int loop = 0; loop < 10; loop++) {
					LinkedList<String> logNew = new LinkedList<String>();
					String[] inputArray = rawLogString.split(", ");
					for (String itm : inputArray) {
						logNew.add(itm);
					}
					if (loop == 0) {
						bw.write("raw log:\t" + logNew + "\n");
						System.out.println("===== for Alignment ===== ");
						bw.write("===== for Alignment ===== \n");
					}
					startTime = System.nanoTime();
					tau = aA.repair(model, logNew, count);
					endTime = System.nanoTime();
					delta1 = endTime - startTime;
					if (delta1 < delta1f) {
						delta1f = delta1;
					}
				}
				
				
				System.out.println("result log: " + tau);
				System.out.println("backtrack num: " + count[0]);
				System.out.println("time consumed for Alignment: " + delta1f);
				bw.write("result log:\t" + tau + "\n");
				bw.write("backtrack num:\t" + count[0] + "\n");
				bw.write("time consumed for Alignment:\t" + delta1f + "\n");
				
				bw.write("===== time rate ===== \n");
				bw.write(String.format("time consumed rate:\t%.2f\n", ((float)delta1f / delta2f)));
				if (rtn.size() != tau.size()) {
					bw.write("2 result is result? NO\n");
				}
				else {
					int checkIndex = 0;
					for (; checkIndex < tau.size(); checkIndex++) {
						if (!rtn.get(checkIndex).equalsIgnoreCase(tau.get(checkIndex).getIdentifier())) {
							bw.write("2 result is result? NO\n");
							break;
						}
					}
					if (checkIndex == tau.size()) {
						bw.write("2 result is result? Yes\n");
					}
				}
				
				bw.write("\n");
				timeRate[index] = ((float)delta1f / delta2f);
				index++;
			}
			else if (algoType == AlgorithmType.optimization) {
				System.out.println("raw log: " + log);
				System.out.println("raw multiset: " + multiset);
				System.out.println("===== for old algo ===== ");
				bw.write("raw log:\t" + log + "\n");
				bw.write("raw multiset:\t" + multiset + "\n");
				bw.write("===== for old algo ===== \n");
				
				Map<String, Integer> multisetOldData = new HashMap<String, Integer>();
				Map<String, Integer> multisetNewData = new HashMap<String, Integer>();
				for (int i = 0; i < log.size(); i++) {
					if (!multisetOldData.containsKey(log.get(i))) {
						multisetOldData.put(log.get(i), 1);
					}
					else {
						int count = multisetOldData.get(log.get(i));
						multisetOldData.put(log.get(i), ++count);
					}
					if (!multisetNewData.containsKey(log.get(i))) {
						multisetNewData.put(log.get(i), 1);
					}
					else {
						int count = multisetNewData.get(log.get(i));
						multisetNewData.put(log.get(i), ++count);
					}
				}				
				
				startTime = System.nanoTime();
				rtn = mA.repair1(multisetOldData);
				endTime = System.nanoTime();
				delta1 = endTime - startTime;
				
				System.out.println("result log: " + rtn);
				System.out.println("time consumed for old algo: " + delta2);
				bw.write("result log:\t" + rtn + "\n");
				bw.write("time consumed for old algo:\t" + delta2 + "\n");
				
				System.out.println("===== for new algo ===== ");
				bw.write("===== for new algo ===== \n");
				
				startTime = System.nanoTime();
				rtn = mA.repair2(multisetNewData);
				endTime = System.nanoTime();
				delta2 = endTime - startTime;
				
				System.out.println("result log: " + rtn);
				System.out.println("time consumed for new algo: " + delta2);
				bw.write("result log:\t" + rtn + "\n");
				bw.write("time consumed for new algo:\t" + delta2 + "\n");
				bw.write("===== time rate ===== \n");
				bw.write(String.format("time consumed rate:\t%.2f\n", ((float)delta1 / delta2)));
				timeRate[index] = ((float)delta1 / delta2);
				index++;
			}
		}
		bw.write("============= Comparision Accomplished ============= \n");
		
		for (float delta : timeRate) {
			rateSum += delta;
		}
		
		bw.write(String.format("average time consumed rate(Alignment / MyAlgo):\t%.2f\n", rateSum / gTrace.traceList.size()));
		bw.close();
		fw.close();
	}
	
	public static void repair(String dirPath, String modelName, String postfix, String dataPath) throws Exception	
	{
		PnmlImport pnmlImport = new PnmlImport();
		PetriNet model = pnmlImport.read(new FileInputStream(new File(dirPath + modelName + postfix)));
//		test(dirPath + modelName + postfix, model, modelName, dataPath, 1, 0, AlgorithmType.alignment);
//		test(dirPath + modelName + postfix, model, modelName, dataPath, 1, 0, AlgorithmType.myAlgorithm);
		test(dirPath + modelName + postfix, model, modelName, dataPath, 4, 0, AlgorithmType.debug);
//		test(dirPath + modelName + postfix, model, modelName, dataPath, 1, 0, AlgorithmType.optimization);
	}
	
	public static void main(String args[]) throws Exception
	{
//		String petriNetPath = "/Users/shawn/Documents/LAB/开题/exp/BeehiveZ+jBPT+PIPE/bpm/笑尘代码/data/causal/FI.403.pnml";
//		String petriNetPath = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/double_loop_nested.pnml";
//		String petriNetPath = "/Users/shawn/Documents/LAB/开题/exp/BeehiveZ+jBPT+PIPE/bpm/笑尘代码/data/all_Loop/FI.106.pnml";
//		String petriNetPath = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/Double_Loop_XOR.pnml";
//		String petriNetPath = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/XOR_SPLIT_AND_SPLIT.pnml";
//		String logPath = "/Users/shawn/Documents/LAB/开题/exp/BeehiveZ+jBPT+PIPE/bpm/笑尘代码/data/causal/log/FI.403.pnml.mxml";
		
		String dirPath = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/";
		String dataPath = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/data/";
		String modelName = "Tripple_Loop_XOR";
		String postfix = ".pnml";
		
		repair(dirPath, modelName, postfix, dataPath);
	}
}
