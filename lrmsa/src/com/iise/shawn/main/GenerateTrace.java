package com.iise.shawn.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.basicprocess.CTree;
import cn.edu.thss.iise.beehivez.server.basicprocess.CTreeGenerator;
import cn.edu.thss.iise.beehivez.server.basicprocess.NewPtsSequence;
import cn.edu.thss.iise.beehivez.server.basicprocess.NewPtsSet;
import cn.edu.thss.iise.beehivez.server.basicprocess.TTreeGenerator;
import cn.edu.thss.iise.beehivez.server.basicprocess.TTreeNode;
import cn.edu.thss.iise.beehivez.server.basicprocess.mymodel.MyPetriNet;

public class GenerateTrace {
	public ArrayList<ArrayList<String>> traceList;
	public ArrayList<ArrayList<String>> traceBatchList;
	public ArrayList<Map<String, Integer>> multiSetList;
	public ArrayList<Map<String, Integer>> multiSetBatchList;
	
	public void init() {
		this.traceList = new ArrayList<ArrayList<String>>();
		this.traceBatchList = new ArrayList<ArrayList<String>>();
		this.multiSetList = new ArrayList<Map<String, Integer>>();
		this.multiSetBatchList = new ArrayList<Map<String, Integer>>();
	}
	
	public void generateTrace(String modelFile, String fileName, int loopNum) throws Exception {
		PnmlImport pnmlimport = new PnmlImport();
		CTree ctree = null;
		PetriNet petrinet = null;
		FileInputStream pnml = null;
		pnml = new FileInputStream(modelFile);
		petrinet = pnmlimport.read(pnml);

		CTreeGenerator generator = new CTreeGenerator(
				MyPetriNet.PromPN2MyPN(petrinet));
		ctree = generator.generateCTree();
		TTreeGenerator ttg = new TTreeGenerator();

		NewPtsSet nps2 = ttg.generatTTree(ctree, loopNum, fileName);
		
		Iterator<NewPtsSequence> traceList = nps2.getNPSet().iterator();
		while (traceList.hasNext()) {
			NewPtsSequence seq = traceList.next();
			ArrayList<TTreeNode> list = seq.getNPSequence();	
			// Iterate in reverse.
			Collections.reverse(list);
			Iterator<TTreeNode> iTTreeNode = list.iterator();
			ArrayList<String> trace = new ArrayList<String>();
			while(iTTreeNode.hasNext()) {
				TTreeNode node = iTTreeNode.next();
				if (node.getTransition().getName().startsWith("INV_") || node.getTransition().getName().equals("")) {
					continue;
				}
				
				trace.add(node.getTransition().getName().replace(",", " and"));
//				System.out.print(node.getTransition().getName() + " ");
			}
			this.traceList.add(trace);
//			System.out.println("");
		}
//		System.out.println("============= complete generating logs =============");
	}
	
	public void generateMisOrderTraceList() {
		for (ArrayList<String> trace : this.traceList) {
			ArrayList<String> misOrderTrace = new ArrayList<String>();
			for (String node : trace) {
				misOrderTrace.add(node);
			}
			Collections.sort(misOrderTrace);
			Map<String, Integer> multiset = new HashMap<String, Integer>();
			for (int i = 0; i < misOrderTrace.size(); i++) {
				if (!multiset.containsKey(misOrderTrace.get(i))) {
					multiset.put(misOrderTrace.get(i), 1);
				}
				else {
					int count = multiset.get(misOrderTrace.get(i));
					multiset.put(misOrderTrace.get(i), ++count);
				}
			}
			this.multiSetList.add(multiset);
		}
	}

	public void generateTraceBatch(String dirPath, int loopNum) throws Exception {
		PnmlImport pnmlImport = new PnmlImport();
        File folder = new File(dirPath);
        File[] files = folder.listFiles();
        for(File file : files) {
            FileInputStream input = new FileInputStream(file);
            System.out.println(file.getAbsolutePath());
            PetriNet petrinet = pnmlImport.read(input);
            
            CTree ctree = null;

    		CTreeGenerator generator = new CTreeGenerator(
    				MyPetriNet.PromPN2MyPN(petrinet));
    		ctree = generator.generateCTree();
    		TTreeGenerator ttg = new TTreeGenerator();

    		NewPtsSet nps2 = ttg.generatTTree(ctree, loopNum, file.getName());
    		
    		Iterator<NewPtsSequence> traceList = nps2.getNPSet().iterator();
    		
    		if (!this.traceList.isEmpty()) {
    			this.traceList.clear();
    		}
    		while (traceList.hasNext()) {
    			NewPtsSequence seq = traceList.next();
    			ArrayList<TTreeNode> list = seq.getNPSequence();	
    			// Iterate in reverse.
    			Collections.reverse(list);
    			Iterator<TTreeNode> iTTreeNode = list.iterator();
    			ArrayList<String> trace = new ArrayList<String>();
    			while(iTTreeNode.hasNext()) {
    				TTreeNode node = iTTreeNode.next();
    				if (node.getTransition().getName().startsWith("INV_") || node.getTransition().getName().equals("")) {
    					continue;
    				}
    				trace.add(node.getTransition().getName());
//    				System.out.print(node.getTransition().getName() + " ");
    			}
    			this.traceList.add(trace);
//    			System.out.println("");
    		}
//    		int maxLen = 0;
//    		ArrayList<String> maxLenArray = new ArrayList<String>();
//    		for (int i = 0; i < this.traceList.size();i++) {
//    			if (this.traceList.get(i).size() > maxLen) {
//    				maxLenArray.clear();
//    				for (String item : this.traceList.get(i)) {
//    					maxLenArray.add(item);
//    				}
//    			}
//    		}
    		this.traceBatchList.add(this.traceList.get(this.traceList.size() - 1));
//    		this.traceBatchList.add(maxLenArray);
        }
	}

	public void generateMisOrderTraceBatchList() {
		for (ArrayList<String> trace : this.traceBatchList) {
			ArrayList<String> misOrderTrace = new ArrayList<String>();
			for (String node : trace) {
				misOrderTrace.add(node);
			}
			Collections.sort(misOrderTrace);
			Map<String, Integer> multiset = new HashMap<String, Integer>();
			for (int i = 0; i < misOrderTrace.size(); i++) {
				if (!multiset.containsKey(misOrderTrace.get(i))) {
					multiset.put(misOrderTrace.get(i), 1);
				}
				else {
					int count = multiset.get(misOrderTrace.get(i));
					multiset.put(misOrderTrace.get(i), ++count);
				}
			}
			this.multiSetBatchList.add(multiset);
		}
	}
}
