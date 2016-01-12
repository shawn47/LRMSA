package com.iise.shawn.alignment;

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
	public LinkedList<Transition> branch(LinkedList<Transition> sigmaK, LinkedList<Transition> visit, LinkedList<String> trace, int k, int depth){
		// System.out.println(sigmaK+" "+trace+" "+k+" "+depth);
//		long nowTime = System.currentTimeMillis();
		LinkedList<Transition> tauMin = new LinkedList<Transition>();
		boolean infiniteTauMin = true;
		int nodeIndex = -1;
		
		if(k == trace.size()){
			// 已检查到最后
			// System.out.println("end:"+sigmaK);
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
			LinkedList<Transition> tau2 = branch(tau, new LinkedList<Transition>(), trace, k + 1, 0);
			//System.out.println("tau2:"+tau2);
			if ((k + 1) == trace.size()) {
				return tau2;
			}
			else {
				System.out.println("backtrack!");
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
				nodeIndex = trace.indexOf(t.getIdentifier());
				if (nodeIndex != -1) {
					trace.remove(t.getIdentifier());
				}
				else {
//					System.out.println("error");
					continue;
				}
				newVisit.add(t);
				LinkedList<Transition> tau = new LinkedList<Transition>();
				tau.addAll(sigmaK);
				tau.add(t);
				LinkedList<Transition> tau2 = branch(tau, newVisit, trace, k, depth + 1);
				//System.out.println("branch:"+tau2);
				if(tau2 == null){
					trace.add(nodeIndex, t.getIdentifier());
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
				}
			}
		}
		if(infiniteTauMin){
			return null;
		}
 		return tauMin;
	}
	
	public LinkedList<Transition> repair(PetriNet net,LinkedList<String> eventLog){
		startTime = System.currentTimeMillis();
		LinkedList<Transition> sigmaK = new LinkedList<Transition>();
		return branch(sigmaK, new LinkedList<Transition>(), eventLog, 0, 0);
	}
}
