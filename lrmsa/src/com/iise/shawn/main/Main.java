package com.iise.shawn.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;

import org.jbpt.petri.NetSystem;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.processmining.exporting.DotPngExport;
import org.processmining.framework.models.petrinet.PNEdge;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.plugin.ProvidedObject;
import org.processmining.importing.pnml.PnmlImport;

import com.iise.shawn.ssd.SSD;
import com.iise.shawn.util.AddEventLog;
import com.iise.shawn.util.PetriNetConversion;

import att.grappa.Edge;
import att.grappa.Node;
import cern.colt.matrix.DoubleMatrix2D;

public class Main {
	public static void lrmsaTest() throws Exception {
		// input multi-set of activities
		ArrayList<String> multisetOfAct = new ArrayList<String>();
		multisetOfAct.add("T0");
		multisetOfAct.add("T5");
		multisetOfAct.add("T3");
		multisetOfAct.add("T2");
		multisetOfAct.add("T1");
		multisetOfAct.add("T4");
//		multisetOfAct.add("T6");
		// input model file
//		String modelFile = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/simple_xor_split.pnml";
//		String modelFile = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/simple_and.pnml";
		String modelFile = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/simple_and_skip_task.pnml";
		
		PnmlImport pnmlImport = new PnmlImport();
		PetriNet p1 = pnmlImport.read(new FileInputStream(new File(modelFile)));
		
		
		
		NetSystem ns = PetriNetConversion.convert(p1);
		CompletePrefixUnfolding cpu = new CompletePrefixUnfolding(ns);
		
		SSD ssd = new SSD();
		ssd.initSSD();
		DoubleMatrix2D lcaMatrixOriNet = ssd.getLCA(cpu);
		ArrayList<String> alOrder_cfp = new ArrayList<String>();
		DoubleMatrix2D ssdMatrix = ssd.computeSSD(cpu, alOrder_cfp);
		
		Map<String, Integer> multiset = new HashMap<String, Integer>();
		for (int i = 0; i < multisetOfAct.size(); i++) {
			if (!multiset.containsKey(multisetOfAct.get(i))) {
				multiset.put(multisetOfAct.get(i), 1);
			}
			else {
				int count = multiset.get(multisetOfAct.get(i));
				multiset.put(multisetOfAct.get(i), ++count);
			}
		}
		
		List<String> validTrace = new LinkedList<String>();
		Queue<String> queue = new LinkedList<String>();
		if (multiset.containsKey(ssd.alMatrix.get(1).split("-")[0])) {
			int count = multiset.get(ssd.alMatrix.get(1).split("-")[0]);
			multiset.put(ssd.alMatrix.get(1).split("-")[0], --count);
		}
		queue.add(ssd.alMatrix.get(1).split("-")[0]);
		
		
		while(!queue.isEmpty()) {
			String curAct = queue.remove();
			validTrace.add(curAct);
			Iterator<Entry<String, Integer>> iterCandidate = multiset.entrySet().iterator();
			Iterator<String> ihVertex = ssd.hVertex.get(curAct).iterator();
			String postAct = "";
			double distanceBetweenActivities = Double.MAX_VALUE;
			HashSet<String> candidateNodes = new HashSet<String>();
			while(ihVertex.hasNext()) {
				String curActT = ihVertex.next();
				int preActIndex = alOrder_cfp.indexOf(curActT);
				while(iterCandidate.hasNext()) {
					Map.Entry<String, Integer> entryCandidate = iterCandidate.next();
					if (entryCandidate.getValue() == 0) {
						continue;
					}
					Iterator<String> ipostHVertex = ssd.hVertex.get(entryCandidate.getKey()).iterator();
					while (ipostHVertex.hasNext()) {
						String postActT = ipostHVertex.next();
						int postActIndex = alOrder_cfp.indexOf(postActT);
						double tempDistance = ssdMatrix.get(preActIndex, postActIndex);
						if (tempDistance >= 0 && tempDistance < distanceBetweenActivities) {
							distanceBetweenActivities = tempDistance;
							postAct = entryCandidate.getKey();
							candidateNodes.clear();
							candidateNodes.add(postActT);
						}
						else if (tempDistance >= 0 && tempDistance == distanceBetweenActivities) {
							distanceBetweenActivities = tempDistance;
							postAct = entryCandidate.getKey();
							candidateNodes.add(postActT);
						}
					}
				}
			}
			if (candidateNodes.size() != 0) {
				if (candidateNodes.size() == 1) {
					Iterator<String> iCandiNodes = candidateNodes.iterator();
					while (iCandiNodes.hasNext()) {
						String candiNode = iCandiNodes.next();
						int count = multiset.get(candiNode.split("-")[0]);
						multiset.put(candiNode.split("-")[0], --count);
						queue.add(candiNode.split("-")[0]);
					}
				}
				else {
					String localAncientNode = "";
					int localAncientNodeIndex = 0;
					Iterator<String> iCandiNodes1 = candidateNodes.iterator();
					while (iCandiNodes1.hasNext()) {
						String candiNode1 = iCandiNodes1.next();
						int candiNodeIndex1 = alOrder_cfp.indexOf(candiNode1);
						if (localAncientNode.equalsIgnoreCase("")) {
							localAncientNode = candiNode1;
							localAncientNodeIndex = alOrder_cfp.indexOf(localAncientNode);
						}
						else {
							if (ssdMatrix.get(localAncientNodeIndex, candiNodeIndex1) >= 0.0) {
								
							}
							else {
								localAncientNode = candiNode1;
								localAncientNodeIndex = candiNodeIndex1;
							}
						}
					}	
					int count = multiset.get(localAncientNode.split("-")[0]);
					multiset.put(localAncientNode.split("-")[0], --count);
					queue.add(localAncientNode.split("-")[0]);
				}
			}
		}
		System.out.println(validTrace);
	}

	public static void lrmsaGraph(String fileName) throws Exception {
//		String filePrefix = "/Users/shawn/Documents/LAB/开题/exp/Models/NFC-01";
//		String filePrefix = "/Users/shawn/Documents/LAB/开题/exp/myModels/simple_xor_split";
//		String filePrefix = "/Users/shawn/Documents/LAB/开题/exp/myModels/simple_loop_prom";
//		String filePrefix = "/Users/shawn/Documents/LAB/开题/exp/myModels/XOR_SPLIT_AND_SPLIT";
//		String filePrefix = "/Users/shawn/Documents/LAB/开题/exp/myModels/2_cutoff_events_prom";
//		String filePrefix = "/Users/shawn/Documents/LAB/开题/exp/myModels/nfc_simple_prom";
//		String filePrefix = "/Users/shawn/Documents/LAB/开题/exp/myModels/cut1142";
		String filePath1 = fileName + ".pnml";
		String filePath2 = fileName + ".png";
		String filePath3 = fileName + "-cfp.png";
				
		
//		PNMLSerializer pnmlSerializer = new PNMLSerializer();
//		String filePath = "/Users/shawn/Documents/LAB/开题/exp/myModels/cut1142.pnml";
//		NetSystem ns = pnmlSerializer.parse(filePath);
		
		PnmlImport pnmlImport = new PnmlImport();
		PetriNet p1 = pnmlImport.read(new FileInputStream(new File(filePath1)));
		
		// ori
		
		ProvidedObject po1 = new ProvidedObject("petrinet", p1);
		
		DotPngExport dpe1 = new DotPngExport();
		OutputStream image1 = new FileOutputStream(filePath2);
		dpe1.export(po1, image1);
		

		NetSystem ns = PetriNetConversion.convert(p1);
		CompletePrefixUnfolding cpu = new CompletePrefixUnfolding(ns);
		
		// cfp
		
		PetriNet p2 = PetriNetConversion.convert(cpu);
		ProvidedObject po2 = new ProvidedObject("petrinet", p2);
		DotPngExport dpe2 = new DotPngExport();
		OutputStream image2 = new FileOutputStream(filePath3);
		dpe2.export(po2, image2);
		
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
//		String filePrefix = "/Users/shawn/Documents/LAB/开题/exp/myModels/simple_xor_split";
//		String fileName = "/Users/shawn/Documents/LAB/开题/exp/myModels/XOR_SPLIT_AND_SPLIT";
//		lrmsaGraph(filePrefix);
//		AddEventLog adl = new AddEventLog();
//		String path = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/";
//		String fileName = "simple_and.pnml";
//		adl.modModels(path);
		lrmsaTest();
	}

}
