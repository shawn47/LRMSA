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
	protected int endNodeIndex;
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
	
	public void setEndNodeIndex(int index) {
		this.endNodeIndex = index;
	}
	
	public void initSSDMatrixNew() {
		ssdMatrixNew = new HashMap<String, ArrayList<Pair<ArrayList<String>, Double>>>();
		for (int i = 0; i < alOrder_cfp.size(); i++) {
			String nodeName = alOrder_cfp.get(i).replace(",", " and");
			ArrayList<Pair<ArrayList<String>, Double>> disList = new ArrayList<Pair<ArrayList<String>, Double>>();
			for (int j = 0; j < alOrder_cfp.size(); j++) {
				String pairNodeName = alOrder_cfp.get(j).replace(",", " and");
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
			ssdMatrixNew.put(nodeName.replace(",", " and"), disList);
		}
		System.out.println("finish init ssdMatrixNew");
	}
	// optimization method
	public List<String> repaireTrace2(Map<String, Integer> multiset) {
		List<String> validTrace = new LinkedList<String>();
		
		int leftNum = 0;
		Iterator<Entry<String, Integer>> iterCandidate = multiset.entrySet().iterator();
		while (iterCandidate.hasNext()) {
			Map.Entry<String, Integer> entryCandidate = iterCandidate.next();
			leftNum += entryCandidate.getValue();
		}
		String startNode = null;
		for (int i = 1; i < ssd.alMatrix.size();i++) {
			if (/*ssd.alMatrix.get(i).split("-")[0].startsWith("P") || */ssd.alMatrix.get(i).substring(0, ssd.alMatrix.get(i).lastIndexOf("-")).startsWith("p")) {
				continue;
			}
			else if (ssd.alMatrix.get(i).substring(0, ssd.alMatrix.get(i).lastIndexOf("-")).startsWith("INV_")) {
				continue;
			}
			else {
				if (multiset.containsKey(ssd.alMatrix.get(i).substring(0, ssd.alMatrix.get(i).lastIndexOf("-")))) {
					int count = multiset.get(ssd.alMatrix.get(i).substring(0, ssd.alMatrix.get(i).lastIndexOf("-")));
					multiset.put(ssd.alMatrix.get(i).substring(0, ssd.alMatrix.get(i).lastIndexOf("-")), --count);
					startNode = ssd.alMatrix.get(i);
					break;
				}
				else {
					continue;
				}
			}
		}
//		if (multiset.containsKey(ssd.alMatrix.get(1).split("-")[0])) {
//			int count = multiset.get(ssd.alMatrix.get(1).split("-")[0]);
//			multiset.put(ssd.alMatrix.get(1).split("-")[0], --count);
//		}
		int startNodeIndex = alOrder_cfp.indexOf(startNode);
		validTrace.add(startNode.substring(0, startNode.lastIndexOf("-")));
		leftNum--;
		while (leftNum > 0) {
			HashSet<String> candidateNodes = new HashSet<String>();
			for (int rowIndex = 0; rowIndex < ssdMatrixNew.get(startNode).size(); rowIndex++) {
				if (ssdMatrixNew.get(startNode).get(rowIndex).value >= 0) {/* && multiset.containsKey(ssdMatrixNew.get(startNode).get(rowIndex).key)) {*/
					for (int colIndex = 0; colIndex < ssdMatrixNew.get(startNode).get(rowIndex).key.size(); colIndex++) {
						//ssdMatrixNew.get(startNode).get(rowIndex).key.get(colIndex).split("-")[0]
						//
						if (multiset.containsKey(ssdMatrixNew.get(startNode).get(rowIndex).key.get(colIndex).substring(0, ssdMatrixNew.get(startNode).get(rowIndex).key.get(colIndex).lastIndexOf("-")))) {
							if (multiset.get(ssdMatrixNew.get(startNode).get(rowIndex).key.get(colIndex).substring(0, ssdMatrixNew.get(startNode).get(rowIndex).key.get(colIndex).lastIndexOf("-"))) > 0) {
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
						int count = multiset.get(candiNode.substring(0, candiNode.lastIndexOf("-")));
						multiset.put(candiNode.substring(0, candiNode.lastIndexOf("-")), --count);
						validTrace.add(candiNode.substring(0, candiNode.lastIndexOf("-")));
						startNode = candiNode;
						leftNum--;
					}
				}
				else {
					String localAncientNode = "";
					int localAncientNodeIndex = 0;
					int leftTaskNum = 0;
					Iterator<String> iCandiNodes1 = candidateNodes.iterator();
					while (iCandiNodes1.hasNext()) {
						String candiNode1 = iCandiNodes1.next();
						int candiNodeIndex1 = alOrder_cfp.indexOf(candiNode1);
						if (localAncientNode.equalsIgnoreCase("")) {
							leftTaskNum = multiset.get(candiNode1.substring(0, candiNode1.lastIndexOf("-")));
							localAncientNode = candiNode1;
							localAncientNodeIndex = alOrder_cfp.indexOf(localAncientNode);
						}
						else {
							if ((ssdMatrix.get(localAncientNodeIndex, candiNodeIndex1)) == 0.0 && (ssdMatrix.get(candiNodeIndex1, localAncientNodeIndex)) == 0.0) {
								continue;
							}
							// if 2 candidate nodes (node A and node B) can reach to each other, we play a test:
							if ((ssdMatrix.get(localAncientNodeIndex, candiNodeIndex1)) > 0.0 && (ssdMatrix.get(candiNodeIndex1, localAncientNodeIndex)) > 0.0) {
//								// if the ssd distance from A to B is larger than the ssd distance from B to A, then we choose B as the next node.
//								if ((ssdMatrix.get(candiNodeIndex1, localAncientNodeIndex) >= 0.0) && (ssdMatrix.get(localAncientNodeIndex, candiNodeIndex1) > ssdMatrix.get(candiNodeIndex1, localAncientNodeIndex))) {
//									localAncientNode = candiNode1;
//									localAncientNodeIndex = candiNodeIndex1;
//								}
//								// else, we choose A.
//								else {
//									
//								}
								// when the distance between (current node and A) and (current and B) is the same
								// we choose the A because the number of left A is more than the number of left B
								if (multiset.get(candiNode1.substring(0, candiNode1.lastIndexOf("-"))) > leftTaskNum) {
									leftTaskNum = multiset.get(candiNode1.substring(0, candiNode1.lastIndexOf("-")));
									localAncientNode = candiNode1;
									localAncientNodeIndex = candiNodeIndex1;
								}
								// if the number of left A is equal to the number of left B
								else if (multiset.get(candiNode1.substring(0, candiNode1.lastIndexOf("-"))) == multiset.get(localAncientNode.substring(0, localAncientNode.lastIndexOf("-")))) {
									// when the distance between (current node and A) and (current and B) is the same
									// A is looped by an invisible task, so A is before B in local, we should choose A first
									if ((ssdMatrix.get(startNodeIndex, localAncientNodeIndex)) > (ssdMatrix.get(startNodeIndex, candiNodeIndex1))) {
										localAncientNode = candiNode1;
										localAncientNodeIndex = candiNodeIndex1;
									}
									else if ((ssdMatrix.get(startNodeIndex, localAncientNodeIndex)) == (ssdMatrix.get(startNodeIndex, candiNodeIndex1))) {
										// when the distance between (current node and A) and (current and B) is the same
										// A is in a loop inside the loop of B (the loop of B is the outer one, the loop of A is the inner one)
										// And A is not in the intersection part of the outer loop and the inner loop
										// we should choose A
										if ((ssdMatrix.get(endNodeIndex, localAncientNodeIndex)) < (ssdMatrix.get(endNodeIndex, candiNodeIndex1))) {
											localAncientNode = candiNode1;
											localAncientNodeIndex = candiNodeIndex1;
										}
										else {
											continue;
										}
									}
									else {
										continue;
									}
								}
								else {
									continue;
								}
							}
							// if 2 candidate nodes are in sequence relation, we choose the local ancient one (which can reach to the other)
							else if ((ssdMatrix.get(localAncientNodeIndex, candiNodeIndex1)) >= 0.0) {
								continue;
							}
							else {
								localAncientNode = candiNode1;
								localAncientNodeIndex = candiNodeIndex1;
							}
						}
					}	
					int count = multiset.get(localAncientNode.substring(0, localAncientNode.lastIndexOf("-")));
					multiset.put(localAncientNode.substring(0, localAncientNode.lastIndexOf("-")), --count);
					validTrace.add(localAncientNode.substring(0, localAncientNode.lastIndexOf("-")));
					startNode = localAncientNode;
					leftNum--;
				}
			}
		}
		return validTrace;
	}
	
	// old naive method
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
