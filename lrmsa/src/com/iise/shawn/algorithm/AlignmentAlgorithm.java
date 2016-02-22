package com.iise.shawn.algorithm;

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
		LinkedList<Transition> tauMin = new LinkedList<Transition>();
		boolean infiniteTauMin = true;
		int nodeIndex = -1;
		
		if(k == (trace.size() - 1)){
			// 已检查到最后
			// System.out.println("end:"+sigmaK);
			Transition lastTrans = transMap.get(trace.get(k));
			sigmaK.add(lastTrans);
			return sigmaK;
		}
//		if((nowTime-startTime)>20000){
//			// System.out.println("time out");
//			// time out
//			return null;
//		}
		if(depth > 1000){
			// 深度超过标准
			return null;
		}
		Transition trans = transMap.get(trace.get(k));
		
		LinkedList<Place> marking = findMarking(sigmaK);
		HashSet<Transition> postTransList = new HashSet<Transition>();
		for(Place p:marking){
			postTransList.addAll(p.getSuccessors());
		}
		if(enabled(marking,trans)){
			LinkedList<Transition> tau = new LinkedList<Transition>();
			tau.addAll(sigmaK);
			tau.add(trans);
			//System.out.println("tau:"+tau);
			LinkedList<Transition> tau2 = branch(tau, new LinkedList<Transition>(), trace, k + 1, 0, count, flag);
			//System.out.println("tau2:"+tau2);
			if ((k + 1) == (trace.size() - 1)) {
				infiniteTauMin = false;
				flag[0] = true;
				return tau2;
			}
//			else if (tau2.size() == len) {
//				infiniteTauMin = false;
//				return tau2; 
//			}
			else {
				count[0]++;
				if (flag[0]) {
					return tau2;
				}
//				System.out.println("backtrack!");
			}
		}
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
				nodeIndex = trace.lastIndexOf(t.getIdentifier());
//				nodeIndex = trace.indexOf(t.getIdentifier());
				if (nodeIndex != -1) {
					trace.remove(nodeIndex);
//					trace.remove(t.getIdentifier());
				}
				else {
//					System.out.println("error");
					continue;
				}
				newVisit.add(t);
				LinkedList<Transition> tau = new LinkedList<Transition>();
				tau.addAll(sigmaK);
				tau.add(t);
				LinkedList<Transition> tau2 = branch(tau, newVisit, trace, k, depth + 1, count, flag);
				//System.out.println("branch:"+tau2);
				if(tau2 == null || tau2.isEmpty()){
					trace.add(nodeIndex, t.getIdentifier());
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
//		if(infiniteTauMin){
//			return null;
//		}
 		return tauMin;
	}
	
	public LinkedList<Transition> repair(PetriNet net, LinkedList<String> eventLog, int[] count) {
		startTime = System.currentTimeMillis();
		LinkedList<Transition> sigmaK = new LinkedList<Transition>();
		boolean[] flag = new boolean[1];
		flag[0] = false;
		return branch(sigmaK, new LinkedList<Transition>(), eventLog, 0, 0, count, flag);
	}
}