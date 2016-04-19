package com.iise.shawn.algorithm;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Place;
import org.processmining.framework.models.petrinet.Transition;

public class AlignmentAlgorithm{
	protected long startTime;
	protected HashMap<String,Transition> transMap;
	protected PetriNet net;
	
	public LinkedList<String> checkEnds(LinkedList<String> eventLog){
		HashSet<String> endsList = new HashSet<String>();
		for(Object o : net.getSink().getPredecessors()){
			Transition t = (Transition) o;
			endsList.add(t.getIdentifier());
		}
		if(!endsList.contains(eventLog.getLast())){
			if(endsList.size() == 1){
				eventLog.add(endsList.iterator().next());
			}
		}
		return eventLog;
	}
	
	public boolean isLabelCorrect(LinkedList<String> eventLog){
		for(String e : eventLog){
			if(transMap.get(e) == null){
				return false;
			}
		}
		return true;
	}
	
	public void setTransMap(HashMap<String, Transition> map){
		transMap = map;
	}
	
	public void setNet(PetriNet ptnet){
		net = ptnet;
	}
	
	@SuppressWarnings("unchecked")
	public LinkedList<Place> findMarking(LinkedList<Transition> trace){
		LinkedList<Place> marking = new LinkedList<Place>();
		marking.add((Place) net.getSource());
		
		for(Transition trans:trace){
			marking.removeAll(trans.getPredecessors());
			marking.addAll(trans.getSuccessors());
		}
		return marking;
	}
	
	private boolean enabled(LinkedList<Place> marking, Transition trans){
		for(Object o : trans.getPredecessors()){
			Place p = (Place) o;
			if(!marking.contains(p)){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * return tau after sigmaK
	 * @param sigmaK
	 * @param trace
	 * @param k
	 * @param depth
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<Transition> branch(LinkedList<Transition> sigmaK, LinkedList<Transition> visit, LinkedList<String> trace, int k, int depth, int[] count, boolean[] flag){
		// System.out.println(sigmaK+" "+trace+" "+k+" "+depth);
//		long nowTime = System.currentTimeMillis();
		Comparator<Transition> cmp = new Comparator<Transition>() {  
            @Override  
            public int compare(Transition t1, Transition t2) {  
                if (t1.getIdentifier().startsWith("INV_") && t2.getIdentifier().startsWith("INV_")) {
                		return 0;
                }
                else if (t1.getIdentifier().startsWith("INV_") && !t2.getIdentifier().startsWith("INV_")) {
                		return -1;
                }
                else if (!t1.getIdentifier().startsWith("INV_") && t2.getIdentifier().startsWith("INV_")) { 
                		return 1;
                }
                else {
                		return t1.getIdentifier().compareTo(t2.getIdentifier());
                }  
            }
        }; 
		
		LinkedList<Transition> tauMin = new LinkedList<Transition>();
		boolean infiniteTauMin = true;
		int nodeIndex = -1;
		
		if(k == (trace.size() - 1)){
			// System.out.println("end:"+sigmaK);
			Transition lastTrans = transMap.get(trace.get(k));
			LinkedList<Place> lastMarking = findMarking(sigmaK);
			if(enabled(lastMarking, lastTrans)) {
				sigmaK.add(lastTrans);
				return sigmaK;
			}
			else {
				LinkedList<Transition> tmpPostTransList = new LinkedList<Transition>();
				for(Place p : lastMarking){
					tmpPostTransList.addAll(p.getSuccessors());
				}
				Collections.sort(tmpPostTransList, cmp);
				for (Transition t : tmpPostTransList) {
					if (t.getIdentifier().startsWith("INV_")) {
						LinkedList<Transition> tmpSigmaK = new LinkedList<Transition>();
						tmpSigmaK.addAll(sigmaK);
						tmpSigmaK.add(t);
						LinkedList<Place> tmpLastMarking = findMarking(tmpSigmaK);
						if(enabled(tmpLastMarking, lastTrans)) {
							sigmaK.add(lastTrans);
							return sigmaK;
						}
					}
				}
				return null;
			}
		}
//		if((nowTime-startTime)>20000){
//			// System.out.println("time out");
//			// time out
//			return null;
//		}
		if(depth > 1000){
			return null;
		}
		Transition trans = transMap.get(trace.get(k));
		
		LinkedList<Place> marking = findMarking(sigmaK);
//		HashSet<Transition> postTransList = new HashSet<Transition>();
		LinkedList<Transition> postTransList = new LinkedList<Transition>();
		for(Place p:marking){
			postTransList.addAll(p.getSuccessors());
		}
		if(enabled(marking,trans)){
			LinkedList<Transition> newVisit = new LinkedList<Transition>();
			newVisit.addAll(visit);
			LinkedList<Transition> tau = new LinkedList<Transition>();
			tau.addAll(sigmaK);
			tau.add(trans);
			newVisit.add(trans);
			LinkedList<Transition> tau2 = branch(tau, newVisit, trace, k + 1, 0, count, flag);
			if ((k + 1) == (trace.size() - 1)) {
				infiniteTauMin = false;
				flag[0] = true;
				return tau2;
			}
			else {
				count[0]++;
				if(!(tau2 == null || tau2.isEmpty())) {
					return tau2;
				}
			}
		}
		
        Collections.sort(postTransList, cmp);
        
		for(Transition t:postTransList){
			if (t.getIdentifier().equalsIgnoreCase(trans.getIdentifier())) {
				continue;
			}
			if(enabled(marking, t)){
//				if(visit.contains(t)){
//					continue;
//				}
				LinkedList<Transition> newVisit = new LinkedList<Transition>();
				newVisit.addAll(visit);
				if (t.getIdentifier().startsWith("INV_")) {
					newVisit.add(t);
					LinkedList<Transition> tau = new LinkedList<Transition>();
					tau.addAll(sigmaK);
					tau.add(t);
//					System.out.println("+" + t.getIdentifier());
					LinkedList<Transition> tau2 = branch(tau, newVisit, trace, k, depth + 1, count, flag);
					if(tau2 == null || tau2.isEmpty()){
						count[0]++;
//						System.out.println("-" + t.getIdentifier());
						continue;
					}else{
						if(infiniteTauMin){
							tauMin = tau2;
							infiniteTauMin = false;
						}else{
							if(tau2.size() < tauMin.size()){
								tauMin = tau2;
							}
						}
						break;
					}
				}
				else {
					nodeIndex = trace.lastIndexOf(t.getIdentifier());
					if (nodeIndex != -1) {
						trace.remove(nodeIndex);
					}
					else {
						continue;
					}
					newVisit.add(t);
					LinkedList<Transition> tau = new LinkedList<Transition>();
					tau.addAll(sigmaK);
					tau.add(t);
//					System.out.println("+" + t.getIdentifier());
					LinkedList<Transition> tau2 = branch(tau, newVisit, trace, k, depth + 1, count, flag);
					if(tau2 == null || tau2.isEmpty()){
						trace.add(nodeIndex, t.getIdentifier());
//						System.out.println("-" + t.getIdentifier());
						count[0]++;
						continue;
					}else{
						if(infiniteTauMin){
							tauMin = tau2;
							infiniteTauMin = false;
						}else{
							if(tau2.size() < tauMin.size()){
								tauMin = tau2;
							}
						}
						break;
					}
				}
			}
		}
 		return tauMin;
	}

	public int getSigmaKLength(LinkedList<Transition> sigmaK) {
		int ret = 0;
		for (Transition item : sigmaK) {
			if (item.getIdentifier().startsWith("INV_")) {
				continue;
			}
			ret++;
		}
		return ret;
	}
	
	/**
	 * return tau after sigmaK
	 * @param sigmaK
	 * @param trace
	 * @param k
	 * @param depth
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public LinkedList<Transition> branch2(LinkedList<LinkedList<Transition>> sigmaKSet, LinkedList<Transition> sigmaK, LinkedList<String> trace, int len, int k, int depth, int[] count, boolean[] flag){
//		long nowTime = System.currentTimeMillis();
//		if (getSigmaKLength(sigmaK) > len) {
//			return null;
//		}
		Comparator<Transition> cmp = new Comparator<Transition>() {  
            @Override  
            public int compare(Transition t1, Transition t2) {  
                if (t1.getIdentifier().startsWith("INV_") && t2.getIdentifier().startsWith("INV_")) {
                		return 0;
                }
                else if (t1.getIdentifier().startsWith("INV_") && !t2.getIdentifier().startsWith("INV_")) {
                		return -1;
                }
                else if (!t1.getIdentifier().startsWith("INV_") && t2.getIdentifier().startsWith("INV_")) { 
                		return 1;
                }
                else {
                		return t1.getIdentifier().compareTo(t2.getIdentifier());
                }  
            }
        }; 
		
		LinkedList<Transition> tauMin = new LinkedList<Transition>();
		boolean infiniteTauMin = true;
		int nodeIndex = -1;
		
		if(k == (trace.size() - 1)){
			Transition lastTrans = transMap.get(trace.get(k));
			LinkedList<Place> lastMarking = findMarking(sigmaK);
			if(enabled(lastMarking, lastTrans)) {
				LinkedList<Transition> tmpSeqLinkedList = new LinkedList<Transition>();
				tmpSeqLinkedList.addAll(sigmaK);
				tmpSeqLinkedList.add(lastTrans);
				
				sigmaKSet.add(tmpSeqLinkedList);
				if (getSigmaKLength(sigmaK) > len - 1) {
					return null;
				}
				else {
					sigmaK.add(lastTrans);
					return sigmaK;
				}
//				sigmaK.add(lastTrans);
//				return sigmaK;
			}
			else {
				LinkedList<Transition> tmpPostTransList = new LinkedList<Transition>();
				for(Place p : lastMarking){
					tmpPostTransList.addAll(p.getSuccessors());
				}
				Collections.sort(tmpPostTransList, cmp);
				for (Transition t : tmpPostTransList) {
					if (t.getIdentifier().startsWith("INV_")) {
						LinkedList<Transition> tmpSigmaK = new LinkedList<Transition>();
						tmpSigmaK.addAll(sigmaK);
						tmpSigmaK.add(t);
						LinkedList<Place> tmpLastMarking = findMarking(tmpSigmaK);
						if(enabled(tmpLastMarking, lastTrans)) {
							LinkedList<Transition> tmpSeqLinkedList = new LinkedList<Transition>();
							tmpSeqLinkedList.addAll(sigmaK);
							tmpSeqLinkedList.add(lastTrans);
							
							sigmaKSet.add(tmpSeqLinkedList);
							if (getSigmaKLength(sigmaK) > len - 1) {
								return null;
							}
							else {
								sigmaK.add(lastTrans);
								return sigmaK;
							}
//							sigmaK.add(lastTrans);
//							return sigmaK;
						}
					}
				}
				return null;
			}
		}
		
		if(depth > 20 && getSigmaKLength(sigmaK) > len - 1){
			return null;
		}
		Transition trans = transMap.get(trace.get(k));
		
		LinkedList<Place> marking = findMarking(sigmaK);
		LinkedList<Transition> postTransList = new LinkedList<Transition>();
		for(Place p:marking){
			postTransList.addAll(p.getSuccessors());
		}
		if(enabled(marking,trans)){
			LinkedList<Transition> tau = new LinkedList<Transition>();
			tau.addAll(sigmaK);
			tau.add(trans);
			LinkedList<Transition> tau2 = branch2(sigmaKSet, tau, trace, len, k + 1, 0, count, flag);
			if ((k + 1) == (trace.size() - 1)) {
				infiniteTauMin = false;
				flag[0] = true;
//				if (tau2 != null) {
//					if (tau2.size() > len) {
//						return null;
//					}
//				}
				return tau2;
			}
			else {
				count[0]++;
				if(!(tau2 == null || tau2.isEmpty())) {
					return tau2;
				}
			}
		}
		
        Collections.sort(postTransList, cmp);
        
		for(Transition t:postTransList){
			if (t.getIdentifier().equalsIgnoreCase(trans.getIdentifier())) {
				continue;
			}
			if(enabled(marking, t)){
//				if(visit.contains(t)){
//					continue;
//				}
				if (t.getIdentifier().startsWith("INV_")) {
					LinkedList<Transition> tau = new LinkedList<Transition>();
					tau.addAll(sigmaK);
					tau.add(t);
//					System.out.println("+" + t.getIdentifier());
					LinkedList<Transition> tau2 = branch2(sigmaKSet, tau, trace, len, k, depth + 1, count, flag);
					if(tau2 == null || tau2.isEmpty()){
						count[0]++;
//						System.out.println("-" + t.getIdentifier());
						continue;
					}else{
						if(infiniteTauMin){
							tauMin = tau2;
							infiniteTauMin = false;
						}else{
							if(tau2.size() < tauMin.size()){
								tauMin = tau2;
							}
						}
//						break;
					}
				}
				else {
					nodeIndex = trace.lastIndexOf(t.getIdentifier());
					if (nodeIndex != -1) {
						trace.remove(nodeIndex);
						LinkedList<Transition> tau = new LinkedList<Transition>();
						tau.addAll(sigmaK);
						tau.add(t);
						LinkedList<Transition> tau2 = branch2(sigmaKSet, tau, trace, len, k, depth + 1, count, flag);
						if(tau2 == null || tau2.isEmpty()){
							trace.add(nodeIndex, t.getIdentifier());
							count[0]++;
							continue;
						}else{
							trace.add(nodeIndex, t.getIdentifier());
							if(infiniteTauMin){
								tauMin = tau2;
								infiniteTauMin = false;
							}else{
								if(tau2.size() < tauMin.size()){
									tauMin = tau2;
								}
							}
//							break;
						}
					}
					else {
						// the previous method is get the event in raw trace
						// however we should visit every possible path for A* algorithm
//						continue;
//						if (visit.contains(t)) {
//							continue;
//						}
						LinkedList<Transition> tau = new LinkedList<Transition>();
						tau.addAll(sigmaK);
						tau.add(t);
						LinkedList<Transition> tau2 = branch2(sigmaKSet, tau, trace, len, k, depth + 1, count, flag);
						if(tau2 == null || tau2.isEmpty()){
							count[0]++;
							continue;
						}else{
							if(infiniteTauMin){
								tauMin = tau2;
								infiniteTauMin = false;
							}else{
								if(tau2.size() < tauMin.size()){
									tauMin = tau2;
								}
							}
//							break;
						}
					}
				}
			}
		}
 		return tauMin;
	}

	
	public LinkedList<Transition> repair(PetriNet net, LinkedList<String> eventLog, int[] count, int[] solutionSize) {
		startTime = System.currentTimeMillis();
		LinkedList<Transition> sigmaK = new LinkedList<Transition>();
		LinkedList<LinkedList<Transition>> sigmaKSet = new LinkedList<LinkedList<Transition>>();
		boolean[] flag = new boolean[1];
		flag[0] = false;
//		LinkedList<Transition> retOriginLog = branch(sigmaK, new LinkedList<Transition>(), eventLog, 0, 0, count, flag);
		int len = eventLog.size();
		LinkedList<Transition> retOriginLog = branch2(sigmaKSet, sigmaK, eventLog, len, 0, 0, count, flag);
		solutionSize[0] = sigmaKSet.size();
		LinkedList<Transition> leastCostSolution = new LinkedList<Transition>();
		for (LinkedList<Transition> seq : sigmaKSet) {
			if (getSigmaKLength(seq) == len) {
				leastCostSolution.clear();
				leastCostSolution.addAll(seq);
			}
		}
		LinkedList<Transition> ret = new LinkedList<Transition>();
		for (Transition node : leastCostSolution) {
			if (node.getIdentifier().startsWith("INV_")) {
				continue;
			}
			ret.add(node);
		}
		return ret;
	}
}
