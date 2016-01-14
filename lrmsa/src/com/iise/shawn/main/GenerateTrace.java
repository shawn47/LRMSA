package com.iise.shawn.main;

import java.io.FileInputStream;
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
//	private ArrayList<ArrayList<String>> misOrderTraceList;
	public ArrayList<Map<String, Integer>> multiSetList;
	
	public void init() {
		this.traceList = new ArrayList<ArrayList<String>>();
//		this.misOrderTraceList = new ArrayList<ArrayList<String>>();
		this.multiSetList = new ArrayList<Map<String, Integer>>();
	}
	
	public void generateTrace(String modelFile, String fileName) throws Exception {
		System.out.println("============= start to generate logs =============");
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

		NewPtsSet nps2 = ttg.generatTTree(ctree, 2, fileName);
		
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
				trace.add(node.getTransition().getName());
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
}
