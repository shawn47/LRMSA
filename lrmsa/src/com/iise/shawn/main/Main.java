package com.iise.shawn.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.plugin.ProvidedObject;
import org.processmining.importing.pnml.PnmlImport;

import com.iise.shawn.ssd.SSD;
import com.iise.shawn.util.PetriNetConversion;

import cern.colt.matrix.DoubleMatrix2D;

public class Main {
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
	
//	public static void modModels(String path) throws Exception {
//        PnmlImport pnmlImport = new PnmlImport();
//        File folder = new File(path);
//        File[] files = folder.listFiles();
//        for(File file : files) {
//        		if (!file.getName().startsWith(".")) {
//	            FileInputStream input = new FileInputStream(file);
//	            System.out.println(file.getAbsolutePath());
//	            PetriNet pn = pnmlImport.read(input);
//	            input.close();
//	            pn.setName(file.getName());
//	            for(Transition t : pn.getTransitions()) {
//	                t.setLogEvent(new LogEvent(t.getIdentifier(), "auto"));
//	            }
//	            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
//	            PnmlWriter.write(false, true, pn, writer);
//	            writer.close();
//        		}
//        }
//    }
	
	public static void lrmsaTest() throws Exception {
		// input model file
//		String modelFile = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/simple_xor_split.pnml";
//		String modelFile = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/simple_and.pnml";
//		String modelFile = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/simple_and_skip_task.pnml";
//		String modelFile = "/Users/shawn/Documents/LAB/开题/exp/myModels/simple_loop_prom.pnml";
//		String modelFile = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/double_loop_nested.pnml";
//		String modelFile = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/Double_Loop_XOR.pnml";
		String modelFile = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/test/Petrinet1.pnml";
		PnmlImport pnmlImport = new PnmlImport();
		PetriNet p1 = pnmlImport.read(new FileInputStream(new File(modelFile)));
		
		NetSystem ns = PetriNetConversion.convert(p1);
		CompletePrefixUnfolding cpu = new CompletePrefixUnfolding(ns);
		
		SSD ssd = new SSD();
		ssd.initSSD(cpu);
		ArrayList<String> alOrder_cfp = new ArrayList<String>();
		DoubleMatrix2D ssdMatrix = ssd.computeSSD(cpu, alOrder_cfp);
		
		// input multi-set of activities
		GenerateTrace gTrace = new GenerateTrace();
		gTrace.init();
		gTrace.generateTrace(modelFile, "file");
		gTrace.generateMisOrderTraceList();
		
		for (Map<String, Integer> multiset : gTrace.multiSetList) {
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
								// if 2 candidate nodes (node A and node B) can reach to each other, we play a test:
								if (ssdMatrix.get(localAncientNodeIndex, candiNodeIndex1) >= 0.0) {
									// if the ssd distance from A to B is larger than the ssd distance from B to A, then we choose B as the next node.
									if ((ssdMatrix.get(candiNodeIndex1, localAncientNodeIndex) >= 0.0) && (ssdMatrix.get(localAncientNodeIndex, candiNodeIndex1) > ssdMatrix.get(candiNodeIndex1, localAncientNodeIndex))) {
										localAncientNode = candiNode1;
										localAncientNodeIndex = candiNodeIndex1;
									}
									// else, we choose A.
									else {
										
									}
								}
								// if 2 candidate nodes are in sequence relation, we choose the local ancient one (which can reach to the other)
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
	}

	public static void main(String[] args) throws Exception {
//		String filePrefix = "/Users/shawn/Documents/LAB/开题/exp/myModels/simple_loop_prom";
//		lrmsaGraph(filePrefix);
//		modModels("/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/test");
		lrmsaTest();
	}
}
