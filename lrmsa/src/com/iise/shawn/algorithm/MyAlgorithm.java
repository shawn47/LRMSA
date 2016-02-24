package com.iise.shawn.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;

import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.processmining.framework.models.petrinet.PetriNet;

import com.iise.shawn.ssd.Pair;
import com.iise.shawn.ssd.SSD;

import cern.colt.matrix.DoubleMatrix2D;

public class MyAlgorithm {
	protected PetriNet model;
	protected CompletePrefixUnfolding cpu;
	protected SSD ssd;
	protected ArrayList<String> alOrder_cfp;
	protected DoubleMatrix2D ssdMatrix;
	protected Map<String, ArrayList<Pair<ArrayList<String>, Double>>> ssdMatrixNew;
	
	public void setModel(PetriNet model) {
		this.model = model;
	}
	
	public void setCPU(CompletePrefixUnfolding cpu) {
		this.cpu = cpu;
	}
	
	public void setSSD(SSD ssd) {
		this.ssd = ssd;
	}
	
	public void setOrderCPU(ArrayList<String> alOrder_cfp) {
		this.alOrder_cfp = alOrder_cfp;
	}
	
	public void setSSDMatrix(DoubleMatrix2D ssdMatrix) {
		this.ssdMatrix = ssdMatrix;
	}
	
	public void initSSDMatrixNew() {
		ssdMatrixNew = new HashMap<String, ArrayList<Pair<ArrayList<String>, Double>>>();
		for (int i = 0; i < alOrder_cfp.size(); i++) {
			String nodeName = alOrder_cfp.get(i);
			ArrayList<Pair<ArrayList<String>, Double>> disList = new ArrayList<Pair<ArrayList<String>, Double>>();
			for (int j = 0; j < alOrder_cfp.size(); j++) {
				String pairNodeName = alOrder_cfp.get(j);
				double pairDistance = ssdMatrix.get(i, j);
				
				if (disList.isEmpty()) {
					ArrayList<String> newPair = new ArrayList<String>();
					newPair.add(pairNodeName);
					disList.add(new Pair<ArrayList<String>, Double>(newPair, pairDistance));
				}
				else {
					int k = 0;
					for (; k < disList.size(); k++) {
						if (disList.get(k).value == pairDistance) {
							disList.get(k).key.add(pairNodeName);
							break;
						}
					}
					if (k == disList.size()) {
						ArrayList<String> newPair = new ArrayList<String>();
						newPair.add(pairNodeName);
						disList.add(new Pair<ArrayList<String>, Double>(newPair, pairDistance));
					}
				}
			}
			Collections.sort(disList);
			ssdMatrixNew.put(nodeName, disList);
		}
		System.out.println("finish init ssdMatrixNew");
	}
	
	public List<String> repaireTrace2(Map<String, Integer> multiset) {
		List<String> validTrace = new LinkedList<String>();
		
		int leftNum = 0;
		Iterator<Entry<String, Integer>> iterCandidate = multiset.entrySet().iterator();
		while (iterCandidate.hasNext()) {
			Map.Entry<String, Integer> entryCandidate = iterCandidate.next();
			leftNum += entryCandidate.getValue();
		}
		
		if (multiset.containsKey(ssd.alMatrix.get(1).split("-")[0])) {
			int count = multiset.get(ssd.alMatrix.get(1).split("-")[0]);
			multiset.put(ssd.alMatrix.get(1).split("-")[0], --count);
		}
		String startNode = ssd.alMatrix.get(1);
		validTrace.add(startNode.split("-")[0]);
		leftNum--;
		while (leftNum > 0) {
			HashSet<String> candidateNodes = new HashSet<String>();
			for (int rowIndex = 0; rowIndex < ssdMatrixNew.get(startNode).size(); rowIndex++) {
				if (ssdMatrixNew.get(startNode).get(rowIndex).value >= 0) {/* && multiset.containsKey(ssdMatrixNew.get(startNode).get(rowIndex).key)) {*/
					for (int colIndex = 0; colIndex < ssdMatrixNew.get(startNode).get(rowIndex).key.size(); colIndex++) {
						if (multiset.containsKey(ssdMatrixNew.get(startNode).get(rowIndex).key.get(colIndex).split("-")[0])) {
							if (multiset.get(ssdMatrixNew.get(startNode).get(rowIndex).key.get(colIndex).split("-")[0]) > 0) {
								candidateNodes.add(ssdMatrixNew.get(startNode).get(rowIndex).key.get(colIndex));
							}
						}
					}
					if (candidateNodes.size() != 0) {
						break;
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
						validTrace.add(candiNode.split("-")[0]);
						startNode = candiNode;
						leftNum--;
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
					validTrace.add(localAncientNode.split("-")[0]);
					startNode = localAncientNode;
					leftNum--;
				}
			}
		}
		return validTrace;
	}
	
	public List<String> repaireTrace(Map<String, Integer> multiset) {
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
//			String postAct = "";
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
//							postAct = entryCandidate.getKey();
							candidateNodes.clear();
							candidateNodes.add(postActT);
						}
						else if (tempDistance >= 0 && tempDistance == distanceBetweenActivities) {
							distanceBetweenActivities = tempDistance;
//							postAct = entryCandidate.getKey();
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
//		System.out.println(validTrace);
		return validTrace;
	}
	
	
	public List<String> repair2(Map<String, Integer> multiset) {
		return repaireTrace2(multiset);
	}
	
	public List<String> repair1(Map<String, Integer> multiset) {
		return repaireTrace(multiset);
	}
}
