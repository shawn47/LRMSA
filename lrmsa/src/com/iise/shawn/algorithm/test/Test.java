package com.iise.shawn.algorithm.test;

import java.awt.PageAttributes.OriginType;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import org.processmining.analysis.redesign.util.PowerSet;
import org.processmining.converting.PostProcessOfUML2SequenceChartImport;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Transition;
import org.processmining.importing.pnml.PnmlImport;

import com.iise.shawn.algorithm.AlignmentAlgorithm;
import com.iise.shawn.algorithm.MyAlgorithm;
import com.iise.shawn.algorithm.MyAlgorithmUpdate;
import com.iise.shawn.main.GenerateTrace;
import com.iise.shawn.ssd.SSD;
import com.iise.shawn.util.AlgorithmType;
import com.iise.shawn.util.IndexUtil;
import com.iise.shawn.util.PetriNetConversion;

import cern.colt.matrix.DoubleMatrix2D;


public class Test {
	private static AlignmentAlgorithm aA = new AlignmentAlgorithm();
	private static MyAlgorithm mA = new MyAlgorithm();
	private static MyAlgorithmUpdate myAU = new MyAlgorithmUpdate();
	
	private static Random ran = new Random(1);
	private static GenerateTrace gTrace = new GenerateTrace();
	
	private static String dataFileName, dataGraphFileName;
	
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
//		dataFileName = dataPath + modelName + "_" + dateFormat.format(date) + "_" + loopNum + ".txt";
		
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
		int i = 0, j = 0;
		for (i = 0; i < ssdMatrix.rows(); i++) {
			for (j = 0; j < ssdMatrix.columns(); j++) {
				if (ssdMatrix.get(i, j) != -3) {
					break;
				}
			}
			if (j == ssdMatrix.columns()) {
				break;
			}
		}
		mA.setEndNodeIndex(i);
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
		
		myAU = new MyAlgorithmUpdate();
		myAU.setEndNodeIndex(i);
		endTime = System.nanoTime();
		System.out.println("===== end SSD Computation Status ===== ");
		System.out.println("init SSD time consumed: " + (endTime - startTime));
		bw.write("SSD Time Consumed:\t" + (endTime - startTime) + "\n");
		myAU.setModel(model);
		myAU.setCPU(cpu);
		myAU.setSSD(ssd);
		myAU.setSSDMatrix(ssdMatrix);
		myAU.setOrderCPU(alOrder_cfp);
		myAU.initSSDMatrixNew();
		
		
		if (algoType == AlgorithmType.batch) {
			bw.write("Trace Number:\t" + (gTrace.traceBatchList.size()) + "\n");
		}
		else {
			bw.write("Trace Number:\t" + (gTrace.traceList.size()) + "\n");
		}
		System.out.println("============= init accomplished ============= ");
		bw.write("============= init accomplished ============= \n");
		bw.close();
		fw.close();
	}
	
	
	
	public static void test(String modelFilePath, PetriNet model, String modelName, String dataPath,
			int loopNum, double errorPercent, AlgorithmType algoType) throws Exception{
		
		gTrace.init();
		gTrace.generateTrace(modelFilePath, "file", loopNum);
		
		System.out.println("total trace number:\t" + gTrace.traceList.size());
		
		initializeAlgorithm(model, modelName, dataPath, loopNum, algoType);
		
		long startTime = 0, endTime = 0;
		float delta1 = 0, delta2 = 0;
		float rateSum = 0;
		LinkedList<Transition> tau = null;
		List<String> rtn = null;
		
		// record experiment data
		File output = new File(dataFileName);
		FileWriter fw = new FileWriter(output, true);
		BufferedWriter bw = new BufferedWriter(fw);
		
		File outputGraph = new File(dataGraphFileName);
		FileWriter fwGraph = new FileWriter(outputGraph, true);
		BufferedWriter bwGraph = new BufferedWriter(fwGraph);

		if(algoType == AlgorithmType.alignment) {
//			System.out.println("============= Alignment Status ============= ");
		}
		else if (algoType == AlgorithmType.myAlgorithm) {
//			System.out.println("============= MyAlgorithm Status ============= ");
		}
		else if (algoType == AlgorithmType.debug) {
//			System.out.println("============= Comparision Status ============= ");
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
			String originLogString = "";
			for (int ievent = 0; ievent < log.size() - 1; ievent++) {
				originLogString += log.get(ievent) + ", ";
			}
			originLogString += log.get(log.size() - 1);
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
				String input = "Start, Maintenance of accounts submitted for, Business leader and general ledger accounting and auditing reasonable, General ledger accounting changes and to freeze and delete subjects , Treasurer Check , End";
				String[] inputArray = input.split(", ");
				for (String itm : inputArray) {
					log2.add(itm);
				}
				Collections.shuffle(log2);
				System.out.println("raw log: " + log2);
				int[] alignmentSolutionSize = new int[1];
				startTime = System.nanoTime();
				tau = aA.repair(model, log2, count, alignmentSolutionSize);
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
				System.out.println((index + 1) + "th trace(" + gTrace.traceList.size() + ") complete " + ((double)(index + 1) / gTrace.traceList.size()));
				long delta2f = 0;
				for (int loop = 0; loop < 6; loop++) {
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
//						bw.write("raw multiset:\t" + multisetNewData + "\n");
						System.out.println("===== for MyAlgo ===== ");
						bw.write("===== for MyAlgo ===== \n");
					}
					startTime = System.nanoTime();
					rtn = mA.repair2(multisetNewData);
					endTime = System.nanoTime();
					if (loop != 0) {
						delta2f += endTime - startTime;
					}
				}
				delta2 = (float)delta2f / 5;
				
				System.out.println("result log: " + rtn);
				System.out.println("time consumed for myAlgo: " + delta2);
				bw.write("time consumed for myAlgo:\t" + delta2 + "\n");
				
				
				int[] count = new int[1];
				count[0] = 0;
				
				long delta1f = 0;
				int[] alignmentSolutionSize = new int[1];
				for (int loop = 0; loop < 6; loop++) {
					LinkedList<String> logNew = new LinkedList<String>();
					String[] inputArray = rawLogString.split(", ");
					for (String itm : inputArray) {
						logNew.add(itm);
					}
					if (loop == 0) {
//						bw.write("raw log:\t" + logNew + "\n");
						System.out.println("===== for Alignment ===== ");
						bw.write("===== for Alignment ===== \n");
					}
					startTime = System.nanoTime();
					alignmentSolutionSize[0] = 0;
					tau = aA.repair(model, logNew, count, alignmentSolutionSize);
					endTime = System.nanoTime();
					if (loop != 0) {
						delta1f += endTime - startTime;
					}
//					delta1 = endTime - startTime;
//					if (delta1 < delta1f) {
//						delta1f = delta1;
//					}
				}
				delta1 = (float)delta1f / 5;
				
				
				System.out.println("result log: " + tau);
				System.out.println("backtrack num: " + count[0]);
				System.out.println("time consumed for Alignment: " + delta1);
//				bw.write("result log:\t" + tau + "\n");
				bw.write("backtrack num:\t" + count[0] + "\n");
				bw.write("solutionset size for Alignment:\t" + alignmentSolutionSize[0] + "\n");
				bw.write("time consumed for Alignment:\t" + delta1 + "\n");
				
				bw.write("===== time rate ===== \n");
				bw.write(String.format("time consumed rate:\t%.2f\n", ((float)delta1 / delta2)));

				// for trace accuracy
				// 1 : match perfect
				// 0 : diff between original trace and repaired trace
				String[] originTrace = originLogString.split(", ");
				int errorCountRtn = 0, errorCountTau = 0;
				int minLength = Integer.MAX_VALUE, lenIndex = 0;
				minLength = Math.min(Math.min(originTrace.length, rtn.size()), tau.size());
				double traceAccuracyRtn = 0.0, traceAccuracyTau = 0.0;
				for (; lenIndex < minLength; lenIndex++) {
					if (!rtn.get(lenIndex).equalsIgnoreCase(originTrace[lenIndex])) {
						errorCountRtn++;
					}
				}
				errorCountRtn += Math.abs((originTrace.length - lenIndex));
				for (lenIndex = 0; lenIndex < minLength; lenIndex++) {
					if (!tau.get(lenIndex).getIdentifier().replace(",", " and").equalsIgnoreCase(originTrace[lenIndex])) {
						errorCountTau++;
					}
				}
				errorCountTau += Math.abs((originTrace.length - lenIndex));
				
				if (errorCountRtn == 0) {
					traceAccuracyRtn = 1.0;
				}
				if (errorCountTau == 0) {
					traceAccuracyTau = 1.0;
				}
				
				// for event accuracy
				double eventAccuracyRtn = 0.0, eventAccuracyTau = 0.0;
				if (errorCountRtn != 0) {
					eventAccuracyRtn = 1 - ((double)errorCountRtn) / originTrace.length;
				}
				if (errorCountTau != 0) {
					eventAccuracyTau = 1 - ((double)errorCountTau) / originTrace.length;
				}
				
				// 
				double accuracyRtn = 0.0, accuracyTau = 0.0;
				accuracyRtn = traceAccuracyRtn + (1 - traceAccuracyRtn) * eventAccuracyRtn;
				accuracyTau = traceAccuracyTau + (1 - traceAccuracyTau) * eventAccuracyTau;
				bw.write("myAlgo rtn length:\t" + rtn.size() + "\n");
				bw.write("alignment tau length:\t" + tau.size() + "\n");
				bw.write("model transitions number:\t" + model.getTransitions().size() + "\n");
				bw.write("accuracy for myAlgo:\t" + accuracyRtn + "\n");
				bw.write("accuracy for Alignment:\t" + accuracyTau + "\n");
				bw.write("\n");
				timeRate[index] = ((float)delta1 / delta2);
				// timeForMyAlgo,traceSize,accuracy,timeForAlignment,traceSize,accuracy,modelSize,modelName
				bwGraph.write(delta2 + "," + rtn.size() + "," + accuracyRtn + "," + delta1 + "," + tau.size() + "," + accuracyTau + "," + model.getTransitions().size() + "," + modelName + "\n");
				
				index++;
			}
			else if (algoType == AlgorithmType.debug2) {
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
				rtn = myAU.repair(multisetOldData);
				endTime = System.nanoTime();
				delta1 = endTime - startTime;
				
//				System.out.println("result log: " + rtn);
//				System.out.println("time consumed for old algo: " + delta2);
				bw.write("result log:\t" + rtn + "\n");
				bw.write("time consumed for old algo:\t" + delta2 + "\n");
				
//				System.out.println("===== for new algo ===== ");
				bw.write("===== for new algo ===== \n");
				
				startTime = System.nanoTime();
				rtn = mA.repair2(multisetNewData);
				endTime = System.nanoTime();
				delta2 = endTime - startTime;
				
//				System.out.println("result log: " + rtn);
//				System.out.println("time consumed for new algo: " + delta2);
				bw.write("result log:\t" + rtn + "\n");
				bw.write("time consumed for new algo:\t" + delta2 + "\n");
				bw.write("===== time rate ===== \n");
				bw.write(String.format("time consumed rate:\t%.2f\n", ((float)delta1 / delta2)));
				timeRate[index] = ((float)delta1 / delta2);
				index++;
			}
			else if (algoType == AlgorithmType.optimization) {
//				System.out.println("raw log: " + log);
//				System.out.println("raw multiset: " + multiset);
//				System.out.println("===== for old algo ===== ");
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
				
//				System.out.println("result log: " + rtn);
//				System.out.println("time consumed for old algo: " + delta2);
				bw.write("result log:\t" + rtn + "\n");
				bw.write("time consumed for old algo:\t" + delta2 + "\n");
				
//				System.out.println("===== for new algo ===== ");
				bw.write("===== for new algo ===== \n");
				
				startTime = System.nanoTime();
				rtn = mA.repair2(multisetNewData);
				endTime = System.nanoTime();
				delta2 = endTime - startTime;
				
//				System.out.println("result log: " + rtn);
//				System.out.println("time consumed for new algo: " + delta2);
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
		bwGraph.close();
		fwGraph.close();
	}
	
	public static void testBatch(String dirPath, String dataPath, int loopNum) throws Exception {
		gTrace.init();
		gTrace.generateTraceBatch(dirPath, loopNum);
		
		System.out.println("total trace number:\t" + gTrace.traceBatchList.size());
		
		PnmlImport pnmlImport = new PnmlImport();
        File folder = new File(dirPath);
        File[] files = folder.listFiles();
        
        // record experiment data
		File output = new File(dataFileName);
		FileWriter fw = new FileWriter(output, true);
		BufferedWriter bw = new BufferedWriter(fw);
		
		File outputGraph = new File(dataGraphFileName);
		FileWriter fwGraph = new FileWriter(outputGraph, true);
		BufferedWriter bwGraph = new BufferedWriter(fwGraph);
		
		long startTime = 0, endTime = 0;
		float delta1 = 0, delta2 = 0;
		int index = 0;
		float[] timeRate = new float[gTrace.traceBatchList.size()];
		float rateSum = 0;
		
		
        for(File file : files) {
        	bw.write("model name:\t" + file.getName() + "\n");
        	System.out.println("model name:\t" + file.getName());
            FileInputStream input = new FileInputStream(file);
            System.out.println(file.getAbsolutePath());
            PetriNet model = pnmlImport.read(input);
            
            initializeAlgorithm(model, file.getName(), dataPath, loopNum, AlgorithmType.batch);
            
            LinkedList<Transition> tau = null;
    		List<String> rtn = null;
    		
    		LinkedList<String> log = new LinkedList<String>();
			for (String item : gTrace.traceBatchList.get(index)) {
				log.add(item);
			}
			String originLogString = "";
			for (int ievent = 0; ievent < log.size() - 1; ievent++) {
				originLogString += log.get(ievent) + ", ";
			}
			originLogString += log.get(log.size() - 1);
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
			System.out.println((index + 1) + "th trace(" + gTrace.traceBatchList.size() + ") complete " + ((double)(index + 1) / gTrace.traceList.size()));
//			long delta2f = Long.MAX_VALUE;
			long delta2f = 0;
			for (int loop = 0; loop < 6; loop++) {
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
//					bw.write("raw multiset:\t" + multisetNewData + "\n");
					System.out.println("===== for MyAlgo ===== ");
					bw.write("===== for MyAlgo ===== \n");
				}
				startTime = System.nanoTime();
//				rtn = mA.repair2(multisetNewData);
				rtn = myAU.repair(multisetNewData);
				endTime = System.nanoTime();
				if (loop != 0) {
					delta2f += endTime - startTime;
				}
//				delta2 = endTime - startTime;
//				if (delta2 < delta2f) {
//					delta2f = delta2;
//				}
			}
			delta2 = (float)delta2f / 5;
			
			System.out.println("result log: " + rtn);
			System.out.println("time consumed for myAlgo: " + delta2);
//			bw.write("result log:\t" + rtn + "\n");
			bw.write("time consumed for myAlgo:\t" + delta2 + "\n");
			
			
			int[] count = new int[1];
			count[0] = 0;
			
//			long delta1f = Long.MAX_VALUE;
			long delta1f = 0;
			int[] alignmentSolutionSize = new int[1];
			for (int loop = 0; loop < 6; loop++) {
				LinkedList<String> logNew = new LinkedList<String>();
				String[] inputArray = rawLogString.split(", ");
				for (String itm : inputArray) {
					logNew.add(itm);
				}
				if (loop == 0) {
//					bw.write("raw log:\t" + logNew + "\n");
					System.out.println("===== for Alignment ===== ");
					bw.write("===== for Alignment ===== \n");
				}
				startTime = System.nanoTime();
				alignmentSolutionSize[0] = 0;
				tau = aA.repair(model, logNew, count, alignmentSolutionSize);
				endTime = System.nanoTime();
				if (loop != 0) {
					delta1f += endTime - startTime;
				}
//				delta1 = endTime - startTime;
//				if (delta1 < delta1f) {
//					delta1f = delta1;
//				}
			}
			delta1 = (float)delta1f / 5;
			
			System.out.println("result log: " + tau);
			System.out.println("backtrack num: " + count[0]);
			System.out.println("time consumed for Alignment: " + delta1);
//			bw.write("result log:\t" + tau + "\n");
//			bw.write("backtrack num:\t" + count[0] + "\n");
			bw.write("time consumed for Alignment:\t" + delta1 + "\n");
			bw.write("solutionset size for Alignment:\t" + alignmentSolutionSize[0] + "\n");
			
			bw.write("===== time rate ===== \n");
			bw.write(String.format("time consumed rate:\t%.2f\n", ((float)delta1 / delta2)));
			
			// for trace accuracy
			// 1 : match perfect
			// 0 : diff between original trace and repaired trace
			String[] originTrace = originLogString.split(", ");
			int errorCountRtn = 0, errorCountTau = 0;
			int minLength = Integer.MAX_VALUE, lenIndex = 0;
			minLength = Math.min(Math.min(originTrace.length, rtn.size()), tau.size());
			double traceAccuracyRtn = 0.0, traceAccuracyTau = 0.0;
			for (; lenIndex < minLength; lenIndex++) {
				if (!rtn.get(lenIndex).equalsIgnoreCase(originTrace[lenIndex])) {
					errorCountRtn++;
				}
			}
			errorCountRtn += Math.abs((originTrace.length - lenIndex));
			for (lenIndex = 0; lenIndex < minLength; lenIndex++) {
				if (!tau.get(lenIndex).getIdentifier().replace(",", " and").equalsIgnoreCase(originTrace[lenIndex])) {
					errorCountTau++;
				}
			}
			errorCountTau += Math.abs((originTrace.length - lenIndex));
			
			if (errorCountRtn == 0) {
				traceAccuracyRtn = 1.0;
			}
			if (errorCountTau == 0) {
				traceAccuracyTau = 1.0;
			}
			
			// for event accuracy
			double eventAccuracyRtn = 0.0, eventAccuracyTau = 0.0;
			if (errorCountRtn != 0) {
				eventAccuracyRtn = 1 - ((double)errorCountRtn) / originTrace.length;
			}
			if (errorCountTau != 0) {
				eventAccuracyTau = 1 - ((double)errorCountTau) / originTrace.length;
			}
			
			// 
			double accuracyRtn = 0.0, accuracyTau = 0.0;
			accuracyRtn = traceAccuracyRtn + (1 - traceAccuracyRtn) * eventAccuracyRtn;
			accuracyTau = traceAccuracyTau + (1 - traceAccuracyTau) * eventAccuracyTau;
			bw.write("myAlgo rtn length:\t" + rtn.size() + "\n");
			bw.write("alignment tau length:\t" + tau.size() + "\n");
			bw.write("model transitions number:\t" + model.getTransitions().size() + "\n");
			bw.write("accuracy for myAlgo:\t" + accuracyRtn + "\n");
			bw.write("accuracy for Alignment:\t" + accuracyTau + "\n");
			bw.write("\n");
			timeRate[index] = ((float)delta1 / delta2);
			// timeForMyAlgo,traceSize,accuracy,timeForAlignment,traceSize,accuracy,modelSize,modelName
			bwGraph.write(delta2 + "," + rtn.size() + "," + accuracyRtn + "," + delta1 + "," + tau.size() + "," + accuracyTau + "," + model.getTransitions().size() + "," + file.getName() + "\n");
			index++;
        }
        bw.write("============= Comparision Accomplished ============= \n");
		
		for (float delta : timeRate) {
			rateSum += delta;
		}
		
		bw.write(String.format("average time consumed rate(Alignment / MyAlgo):\t%.2f\n", rateSum / gTrace.traceBatchList.size()));
		bw.close();
		fw.close();
		bwGraph.close();
		fwGraph.close();
	}
	
	public static void repair(String dirPath, String modelName, String postfix, String dataPath, int loopNum) throws Exception	
	{
		DateFormat dateFormat = new SimpleDateFormat("MMdd_HH_mm_ss");
		Date date = new Date();
		System.out.println(dateFormat.format(date));
		dataFileName = dataPath + modelName + "_" + dateFormat.format(date) + "_" + loopNum + ".txt";
		dataGraphFileName = dataPath + "graph\\" + modelName + "_" + dateFormat.format(date) + "_" + loopNum + ".txt";
		
		PnmlImport pnmlImport = new PnmlImport();
		PetriNet model = pnmlImport.read(new FileInputStream(new File(dirPath + modelName + postfix)));
//		test(dirPath + modelName + postfix, model, modelName, dataPath, loopNum, 0, AlgorithmType.alignment);
//		test(dirPath + modelName + postfix, model, modelName, dataPath, loopNum, 0, AlgorithmType.myAlgorithm);
//		test(dirPath + modelName + postfix, model, modelName, dataPath, loopNum, 0, AlgorithmType.debug);
		test(dirPath + modelName + postfix, model, modelName, dataPath, loopNum, 0, AlgorithmType.debug2);
//		test(dirPath + modelName + postfix, model, modelName, dataPath, loopNum, 0, AlgorithmType.optimization);
	}
	
	public static void repairBatch(String dirPath, String dataPath, int loopNum) throws Exception {
		DateFormat dateFormat = new SimpleDateFormat("MMdd_HH_mm_ss");
		Date date = new Date();
		System.out.println(dateFormat.format(date));
		dataFileName = dataPath + dataPath.substring(dataPath.lastIndexOf("\\"), dataPath.length()) + "_" + dateFormat.format(date) + "_" + loopNum + ".txt";
		dataGraphFileName = dataPath + "graph\\"  + dataPath.substring(dataPath.lastIndexOf("\\"), dataPath.length()) + "_" + dateFormat.format(date) + "_" + loopNum + ".txt";
		
		testBatch(dirPath, dataPath, loopNum);
	}
	
	public static void main(String args[]) throws Exception
	{
//		String dirPath = "D:\\实验室\\开题\\efficiency\\loop\\";
		String dirPath = "D:\\实验室\\开题\\efficiency\\loop";
		String dataPath = "D:\\实验室\\日志\\data\\";
		String modelName = "TC_MM.300";
		String postfix = ".pnml";
		
		
//		repair(dirPath, modelName, postfix, dataPath, 3);
		repairBatch(dirPath, dataPath, 3);
	}
}
