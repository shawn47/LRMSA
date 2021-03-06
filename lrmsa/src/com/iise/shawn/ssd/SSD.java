package com.iise.shawn.ssd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.jbpt.petri.unfolding.Condition;
import org.jbpt.petri.unfolding.Event;
import org.jbpt.petri.unfolding.IBPNode;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.ObjectFactory2D;
import cern.colt.matrix.ObjectMatrix2D;

public class SSD {
	public static final double SSD_EXCLUSIVE = -1.0;
	public static final double SSD_UNDEFINED = -2.0;
	public static final double SSD_INFINITY = -3.0;
	
	@SuppressWarnings("rawtypes")
	private Hashtable<String, HashSet<IBPNode>> htVertex;
	public Hashtable<String, HashSet<String>> hVertex;
	public ArrayList<String> alMatrix;
	public ArrayList<IBPNode> alVertex;
	public int n;
	public DoubleMatrix2D lcaMatrix2d;
	
	@SuppressWarnings("rawtypes")
	public void initSSD(CompletePrefixUnfolding cpu) {
		this.alMatrix = new ArrayList<String>();
		this.hVertex = new Hashtable<String, HashSet<String>>();
		//row and col of matrix -> identifier
		this.htVertex = new Hashtable<String, HashSet<IBPNode>>();
		
		this.alVertex = new ArrayList<IBPNode>();
		this.alVertex.addAll(cpu.getEvents());
		this.alVertex.addAll(cpu.getConditions());
		this.alMatrix.clear();
		this.htVertex.clear();
//		ArrayList<String> alMatrix = new ArrayList<String>();
		//row and col of matrix -> identifier
//		this.htVertex = new Hashtable<String, HashSet<IBPNode>>();
		for(IBPNode v : this.alVertex) {
			if(!this.alMatrix.contains(v.getName())) {
				this.hVertex.put(v.getPetriNetNode().getName(), new HashSet<String>());
				this.htVertex.put(v.getName(), new HashSet<IBPNode>());
				this.alMatrix.add(v.getName());
			}
			this.hVertex.get(v.getPetriNetNode().getName()).add(v.getName());
			this.htVertex.get(v.getName()).add(v);
		}
		ArrayList<String> alSkipTasks = getSkipInvisibleTasks(cpu, this.htVertex);
		
		
		//the row and col of the following matrix are the same with the key of alMatrix, 0...n-1
		this.n = this.htVertex.size();
		this.lcaMatrix2d = DoubleFactory2D.sparse.make(this.n, this.n, 0);
	}
	
	@SuppressWarnings("rawtypes")
	public DoubleMatrix2D getLCA(CompletePrefixUnfolding cpu) {
		ArrayList<IBPNode> alVertex = new ArrayList<IBPNode>();
		alVertex.addAll(cpu.getEvents());
		alVertex.addAll(cpu.getConditions());

		this.alMatrix.clear();

		for(IBPNode v : alVertex) {
			if(!this.alMatrix.contains(v.getName())) {
				this.hVertex.put(v.getPetriNetNode().getName(), new HashSet<String>());
				this.htVertex.put(v.getName(), new HashSet<IBPNode>());
				this.alMatrix.add(v.getName());
				
			}
			this.hVertex.get(v.getPetriNetNode().getName()).add(v.getName());
			this.htVertex.get(v.getName()).add(v);
		}
		
		//the row and col of the following matrix are the same with the key of alMatrix, 0...n-1
		int n = this.htVertex.size();
		
		DoubleMatrix2D anceMatrix2dONet = this.getReachMatrixCPU(cpu, n, this.alMatrix, htVertex);
		
		DoubleMatrix2D lcaMatrix2d = DoubleFactory2D.sparse.make(n, n, 0);
		ArrayList<Event> alVisibleTrans = getVisibleTasks(cpu);
		
		ArrayList<Event> alTrans = new ArrayList<Event>();
		alTrans.addAll(0, cpu.getEvents());
		ArrayList<String> visitedTrans = new ArrayList<String>();
		for(int i = 0; i < alTrans.size(); ++i) {
			IBPNode tI = alTrans.get(i);
			if(visitedTrans.contains(tI.getName())) {
				continue;
			}
			int tIIndex = this.alMatrix.indexOf(tI.getName());
			for(int j = i + 1; j < alTrans.size(); ++j) {
				IBPNode tJ = alTrans.get(j);
				if(visitedTrans.contains(tJ.getName())) {
					continue;
				}
				int tJIndex = this.alMatrix.indexOf(tJ.getName());
				int maxLCAIndex = tIIndex < tJIndex ? tIIndex : tJIndex;
				for(int pos = maxLCAIndex; pos > 0; --pos) {
					if(anceMatrix2dONet.get(pos, tIIndex) == 1.0 && anceMatrix2dONet.get(pos, tJIndex) == 1.0) {
						lcaMatrix2d.set(tIIndex, tJIndex, pos);
						lcaMatrix2d.set(tJIndex, tIIndex, pos);
						break;
					}
				}
			}
			visitedTrans.add(tI.getName());
		}
		return lcaMatrix2d;
	}
	

	private HashSet<Condition> getPostConditionWithCut(CompletePrefixUnfolding cpu, Event e) {
		HashSet<Condition> postConditions = new HashSet<Condition>();
		if (cpu.getCutoffEvents().contains(e)) {
			for (Condition eSucc : e.getPostConditions()) {
				Iterator<Condition> iESuccMappedContions = eSucc.getMappingConditions().iterator();
				while(iESuccMappedContions.hasNext()) {
					Condition eSuccMappedContion = iESuccMappedContions.next();
					postConditions.add(eSuccMappedContion);
				}
			}
		}
		else {
			for (Condition eSucc: e.getPostConditions()) {
				postConditions.add(eSucc);
			}
		}
		return postConditions;
	}
	
	
	@SuppressWarnings("rawtypes")
	public DoubleMatrix2D computeSSD(CompletePrefixUnfolding cpu, ArrayList<String> alOrder) {
		
		
		DoubleMatrix2D anceMatrix2dONet = this.getReachMatrixCPU(cpu, this.n, this.alMatrix, this.htVertex);
//		DoubleMatrix2D anceMatrix2dONet = this.getReachMatrix2d(cpu, n, this.alMatrix, htVertex);
		
//		DoubleMatrix2D lcaMatrix2d = DoubleFactory2D.sparse.make(n, n, 0);
//		lcaMatrix2d = DoubleFactory2D.sparse.make(this.n, this.n, 0);
		ArrayList<Event> alVisibleTrans = new ArrayList<Event>();
		alVisibleTrans.addAll(getVisibleTasks(cpu));
//		pn.getVisibleTasks();
		
		ArrayList<Event> alTrans = new ArrayList<Event>();
		alTrans.addAll(0, cpu.getEvents());
		ArrayList<String> visitedTrans = new ArrayList<String>();
		for(int i = 0; i < alTrans.size(); ++i) {
			IBPNode tI = alTrans.get(i);
			if(visitedTrans.contains(tI.getName())) {
				continue;
			}
			int tIIndex = this.alMatrix.indexOf(tI.getName());
			for(int j = i + 1; j < alTrans.size(); ++j) {
				IBPNode tJ = alTrans.get(j);
				if(visitedTrans.contains(tJ.getName())) {
					continue;
				}
				int tJIndex = this.alMatrix.indexOf(tJ.getName());
				int maxLCAIndex = tIIndex < tJIndex ? tIIndex : tJIndex;
				for(int pos = maxLCAIndex; pos > 0; --pos) {
					if(anceMatrix2dONet.get(pos, tIIndex) == 1.0 && anceMatrix2dONet.get(pos, tJIndex) == 1.0) {
						this.lcaMatrix2d.set(tIIndex, tJIndex, pos);
						this.lcaMatrix2d.set(tJIndex, tIIndex, pos);
						break;
					}
				}
			}
			visitedTrans.add(tI.getName());
		}
		
		//initialize shortest synchronization distance matrix
		DoubleMatrix2D ssdtMatrix2d = DoubleFactory2D.sparse.make(n, n, -3.0);
		//trace matrix, used in computing parallel structure
		ObjectMatrix2D traceMatrix2d = ObjectFactory2D.sparse.make(n, n, new ArrayList<String>());
		//get all transitions
		//ArrayList<Transitions> alTrans, already defined above
		//to mark transitions which have been handled
		visitedTrans.clear();
		//for sequential relations, set distance to 1 (predecessor to successor)
		for(Event t : alTrans) {
			HashSet<String> hsSuccPlaceId = this.getTransitionSuccSet(cpu, t, this.htVertex);
			if(hsSuccPlaceId.size() > 1) {
				if (!cpu.getCutoffEvents().contains(t)) {
					//parallel structure, pass
					continue;
				}
			}
			String tId = t.getName();
			if(visitedTrans.contains(tId)) {
				continue;
			}
			int tIndex = this.alMatrix.indexOf(tId);
			Iterator<IBPNode> itT = this.htVertex.get(tId).iterator();
			while(itT.hasNext()) {
//				change ssd on cpu for the adjacent activities
//				Iterator<Condition> itTSucc = ((Event) itT.next()).getPostConditions().iterator();
				Iterator<Condition> itTSucc = this.getPostConditionWithCut(cpu, (Event) itT.next()).iterator();
				while(itTSucc.hasNext()) {
					Condition tSuccPlace = itTSucc.next();
					Iterator<IBPNode> itTSuccPlace = this.htVertex.get(tSuccPlace.getName()).iterator();
					while(itTSuccPlace.hasNext()) {
						Iterator<Event> itPSuccEvent = ((Condition) itTSuccPlace.next()).getPostE().iterator();
						while(itPSuccEvent.hasNext()) {
							Event tSuccEvent = itPSuccEvent.next();
							//set ssd from t to tSuccTran with 1.0
							String tSuccEventId = tSuccEvent.getName();
							int tSuccEventIndex = this.alMatrix.indexOf(tSuccEventId);
							ssdtMatrix2d.set(tIndex, tSuccEventIndex, 1.0);
							//set trace from t to tSuccTran
							ArrayList<String> trace = new ArrayList<String>();
							trace.add(tId);
							trace.add(tSuccEventId);
							traceMatrix2d.set(tIndex, tSuccEventIndex, trace);
						}
					}
				}
			}
			visitedTrans.add(t.getName());
		}
		ArrayList<String> alSkipTasks = getSkipInvisibleTasks(cpu, this.htVertex);
		//add invisible tasks of skip type into set of visible tasks
		for(String s : alSkipTasks) {
			Iterator<IBPNode> it = this.htVertex.get(s).iterator();
			while(it.hasNext()) {
				alVisibleTrans.add((Event) it.next());
			}
		}
		
		//compute ssdt between other pairs of transitions recursively
		ArrayList<String> alVisitedITrans = new ArrayList<String>();
		for(int i = 0; i < alTrans.size(); ++i) {
			Event tI = alTrans.get(i);
			String tIId = tI.getName();
			if(alVisitedITrans.contains(tIId)) {
				continue;
			}
			int tIIndex = this.alMatrix.indexOf(tIId);
			ArrayList<String> alVisitedJTrans = new ArrayList<String>();
			for(int j = 0; j < alTrans.size(); ++j) {
				Event tJ = alTrans.get(j);
				String tJId = tJ.getName();
				if(alVisitedJTrans.contains(tJId)) {
					continue;
				}
				int tJIndex = this.alMatrix.indexOf(tJId);
				ArrayList<String> visited = new ArrayList<String>();
				ArrayList<String> trace = new ArrayList<String>();
				int ssdt = this.computeRecur(cpu, tI, tJ, ssdtMatrix2d, trace, visited, this.alMatrix, this.htVertex, traceMatrix2d);
				if(ssdt > 0) {
					ssdtMatrix2d.set(tIIndex, tJIndex, ssdt);
					traceMatrix2d.set(tIIndex, tJIndex, trace);
				}
				alVisitedJTrans.add(tJId);
			}
			alVisitedITrans.add(tIId);
		}
		
		//for parallel transitions, set distance to 1 (both directions)
		//for exclusive transitions, set distance to -1 (relation "x")
		visitedTrans.clear();
		for(int i = 0; i < alTrans.size(); ++i) {
			Event tI = alTrans.get(i);
			String tIId = tI.getName();
			if(visitedTrans.contains(tIId)) {
				continue;
			}
			int tIIndex = this.alMatrix.indexOf(tIId);
			for(int j = i + 1; j < alTrans.size(); ++j) {
				Event tJ = alTrans.get(j);
				String tJId = tJ.getName();
				if(visitedTrans.contains(tJId)) {
					continue;
				}
				int tJIndex = this.alMatrix.indexOf(tJId);
				int lcaIndex = (int)this.lcaMatrix2d.get(tIIndex, tJIndex);
				if(lcaIndex != tIIndex && lcaIndex != tJIndex) {
					Iterator<IBPNode> itVertex = this.htVertex.get(this.alMatrix.get(lcaIndex)).iterator();
					if(itVertex.hasNext()) {
						IBPNode vLCA = itVertex.next();
						if(vLCA instanceof Event) {
							// parallel
							if(!(ssdtMatrix2d.get(tIIndex, tJIndex) > 0 || ssdtMatrix2d.get(tJIndex, tIIndex) > 0)) {
//								ssdtMatrix2d.set(tIIndex, tJIndex, 1.0);
								ssdtMatrix2d.set(tIIndex, tJIndex, 0.0);
								ArrayList<String> trace = new ArrayList<String>();
								trace.add(tIId);
								trace.add(tJId);
								traceMatrix2d.set(tIIndex, tJIndex, trace);
//								ssdtMatrix2d.set(tJIndex, tIIndex, 1.0);
								ssdtMatrix2d.set(tJIndex, tIIndex, 0.0);
								trace = new ArrayList<String>();
								trace.add(tJId);
								trace.add(tIId);
								traceMatrix2d.set(tJIndex, tIIndex, trace);
							}
						} else if(vLCA instanceof Condition) {
							// exclusive
							if(!(ssdtMatrix2d.get(tIIndex, tJIndex) > 0 || ssdtMatrix2d.get(tJIndex, tIIndex) > 0)) {
								ssdtMatrix2d.set(tIIndex, tJIndex, -1.0);
								ssdtMatrix2d.set(tJIndex, tIIndex, -1.0);
							}
						}
					}
				}
			}
			visitedTrans.add(tIId);
		}
		
		ArrayList<Integer> alOrderNum = new ArrayList<Integer>();
		for(Event tI : alTrans) {
			String tIId = tI.getName();
			int tIIndex = this.alMatrix.indexOf(tIId);
			if(alOrderNum.contains(tIIndex)) {
				continue;
			}
			alOrderNum.add(tIIndex);
		}
		DoubleMatrix2D ssdMatrix = DoubleFactory2D.sparse.make(alOrderNum.size(), alOrderNum.size(), 0);
		for(int i = 0; i < alOrderNum.size(); ++i) {
			for(int j = 0; j < alOrderNum.size(); ++j) {
				ssdMatrix.set(i, j, ssdtMatrix2d.get(alOrderNum.get(i), alOrderNum.get(j)));
			}
		}
		alOrder.clear();
		for(int i = 0; i < alOrderNum.size(); ++i) {
			alOrder.add(this.alMatrix.get(alOrderNum.get(i)));
		}
		
		return ssdMatrix;
	}
	

	/**
	 * compute ssd between other pairs of transitions recursively
	 * @param tI - source transition
	 * @param tJ - target transition
	 * @param ssdtMatrix2d - ssdt Matrix
	 * @param trace - shortest trace from tI to tJ
	 * @param visited - store visited transitions
	 * @param alMatrix - all the ids of vertices
	 * @param htVertex - the map of label->list(vertex)
	 * @param traceMatrix2d - shortest trace Matrix
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private int computeRecur(CompletePrefixUnfolding cpu, Event tI, Event tJ, DoubleMatrix2D ssdtMatrix2d, ArrayList<String> trace, 
				ArrayList<String> visited, ArrayList<String> alMatrix, 
				Hashtable<String, HashSet<IBPNode>> htVertex, 
				ObjectMatrix2D traceMatrix2d) {
		String tIId = tI.getName();
		if(visited.contains(tIId)) {
			//has already been visited, return infinity
			return -3;
		}
		String tJId = tJ.getName();
		if(tIId.equals(tJId) && !visited.isEmpty()) {
			//arrive at tJ, return 0
			trace.add(tJId);
			return 0;
		}
		int tIIndex = alMatrix.indexOf(tIId);
		int tJIndex = alMatrix.indexOf(tJId);
		if(ssdtMatrix2d.get(tIIndex, tJIndex) > 0.0) {
			trace.addAll((Collection<? extends String>) traceMatrix2d.get(tIIndex, tJIndex));
			return (int) ssdtMatrix2d.get(tIIndex, tJIndex);
		}
		//start, add tI to visited
		visited.add(tIId);
		//compute ssd recursively (sequential, exclusive, parallel)
		HashSet<String> hsTISuccPlace = this.getTransitionSuccSet(cpu, tI, htVertex);
//		HashSet<String> hsTISuccPlace = this.getPostConditionWithCut(cpu, tI);
		int nTISuccPlace = hsTISuccPlace.size();
		if(nTISuccPlace == 1) {
			//sequential or exclusive, tI has only one output place
			Iterator<IBPNode> itTI = htVertex.get(tIId).iterator();
			Condition pTISuccPlace = null;
			while(itTI.hasNext() && pTISuccPlace == null) {
				Iterator<Condition> itTISuccPlace = ((Event) itTI.next()).getPostConditions().iterator();
				while(itTISuccPlace.hasNext()) {
					pTISuccPlace = itTISuccPlace.next();
					break;
				}
			}
			HashSet<String> hsPSuccTran = this.getPlaceSuccSet(pTISuccPlace, htVertex, true);
			int nPSuccTran = hsPSuccTran.size();
			if(nPSuccTran == 0) {
				//cannot reach tj, return infinity
				return -3;
			} else if(nPSuccTran == 1) {
				//sequential
				Iterator<IBPNode> itPlace = htVertex.get(pTISuccPlace.getName()).iterator();
				Event tPSuccTran = null;
				while(itPlace.hasNext() && tPSuccTran == null) {
					Iterator<Event> itTSucc = ((Condition) itPlace.next()).getPostE().iterator();
					while(itTSucc.hasNext()) {
						tPSuccTran = itTSucc.next();
						break;
					}
				}
				ArrayList<String> succTrace = new ArrayList<String>();
				int succSSDT = this.computeRecur(cpu, tPSuccTran, tJ, ssdtMatrix2d, succTrace, visited, alMatrix, htVertex, traceMatrix2d);
//				if(succSSDT == -3) {
//					return -3;
//				} else if(!tPSuccTran.isInvisibleTask()/* || newTi.getAttribute("skip") != null*/) {
//					trace.add(tIId);
//					trace.addAll(succTrace);
//					return 1 + succSSDT;
//				} else {
//					trace.add(tIId);
//					trace.addAll(succTrace);
//					return succSSDT;
//				}
				/**
				 * 
				 
				if(succSSDT == -3) {
					return -3;
				} else {
					trace.add(tIId);
					trace.addAll(succTrace);
					return 1 + succSSDT;
				}
				*/
				if(succSSDT == -3) {
					return -3;
				} else if (!tPSuccTran.getName().startsWith("INV_")) {
					trace.add(tIId);
					trace.addAll(succTrace);
					return 1 + succSSDT;
				} else {
					trace.add(tIId);
					trace.addAll(succTrace);
					return 1 + succSSDT;
				}
				
			} else {
				//exclusive
				int minSuccSSDT = -3;
				ArrayList<String> tmpTrace = new ArrayList<String>();
				ArrayList<String> visitedCopy = new ArrayList<String>();
				visitedCopy.addAll(visited);
				for(String pSuccTranId : hsPSuccTran) {
					Iterator<IBPNode> itPSuccTran = htVertex.get(pSuccTranId).iterator();
					if(itPSuccTran.hasNext()) {
						Event tPSuccTran = (Event) itPSuccTran.next();
						ArrayList<String> succTrace = new ArrayList<String>();
						ArrayList<String> tmpVisited = new ArrayList<String>();
						tmpVisited.addAll(visitedCopy);
						int succSSDT = this.computeRecur(cpu, tPSuccTran, tJ, ssdtMatrix2d, succTrace, tmpVisited, alMatrix, htVertex, traceMatrix2d);
						if(succSSDT != -3) {
							if(!tPSuccTran.getName().startsWith("INV_")/* || newTi.getAttribute("skip") != null*/) {
								++succSSDT;
							}
							/**
							 * 
							 
							++succSSDT;
							*/
							if(minSuccSSDT == -3 || succSSDT < minSuccSSDT) {
								tmpTrace.clear();
								tmpTrace.addAll(succTrace);
								minSuccSSDT = succSSDT;
							}
						}
						for(String s : tmpVisited) {
							if(!(visited.contains(s))) {
								visited.add(s);
							}
						}
					}
				}
				if(minSuccSSDT != -3) {
					trace.add(tIId);
					trace.addAll(tmpTrace);
				}
				return minSuccSSDT;
			}
		} else if(nTISuccPlace > 1) {
			// for cut off events, tI has 2 corresponding places
			if (cpu.getCutoffEvents().contains(tI)) {
				//sequential or exclusive, tI has only one output place
				Iterator<IBPNode> itTI = htVertex.get(tIId).iterator();
				Condition pTISuccPlace = null;
				while(itTI.hasNext()) {
					Event e = (Event) itTI.next();
					for (Condition eSucc : e.getPostConditions()) {
						Iterator<Condition> iESuccMappedContions = eSucc.getMappingConditions().iterator();
						while(iESuccMappedContions.hasNext()) {
							Condition eSuccMappedContion = iESuccMappedContions.next();
							if (!eSuccMappedContion.getName().equalsIgnoreCase(eSucc.getName()) && !eSuccMappedContion.getPostE().isEmpty()) {
								pTISuccPlace = eSuccMappedContion;
							}
							
						}
					}
				}
				
//				Condition pTISuccPlace = null;
//				while(itTI.hasNext() && pTISuccPlace == null) {
//					Iterator<Condition> itTISuccPlace = ((Event) itTI.next()).getPostConditions().iterator();
//					while(itTISuccPlace.hasNext()) {
//						pTISuccPlace = itTISuccPlace.next();
//						break;
//					}
//				}
				HashSet<String> hsPSuccTran = this.getPlaceSuccSet(pTISuccPlace, htVertex, true);
				int nPSuccTran = hsPSuccTran.size();
				if(nPSuccTran == 0) {
					//cannot reach tj, return infinity
					return -3;
				} else if(nPSuccTran == 1) {
					//sequential
					Iterator<IBPNode> itPlace = htVertex.get(pTISuccPlace.getName()).iterator();
					Event tPSuccTran = null;
					while(itPlace.hasNext() && tPSuccTran == null) {
						Iterator<Event> itTSucc = ((Condition) itPlace.next()).getPostE().iterator();
						while(itTSucc.hasNext()) {
							tPSuccTran = itTSucc.next();
							break;
						}
					}
					ArrayList<String> succTrace = new ArrayList<String>();
					int succSSDT = this.computeRecur(cpu, tPSuccTran, tJ, ssdtMatrix2d, succTrace, visited, alMatrix, htVertex, traceMatrix2d);
					if(succSSDT == -3) {
						return -3;
					} else {
						trace.add(tIId);
						trace.addAll(succTrace);
						return 1 + succSSDT;
					}
				} else {
					//exclusive
					int minSuccSSDT = -3;
					ArrayList<String> tmpTrace = new ArrayList<String>();
					ArrayList<String> visitedCopy = new ArrayList<String>();
					visitedCopy.addAll(visited);
					for(String pSuccTranId : hsPSuccTran) {
						Iterator<IBPNode> itPSuccTran = htVertex.get(pSuccTranId).iterator();
						if(itPSuccTran.hasNext()) {
							Event tPSuccTran = (Event) itPSuccTran.next();
							ArrayList<String> succTrace = new ArrayList<String>();
							ArrayList<String> tmpVisited = new ArrayList<String>();
							tmpVisited.addAll(visitedCopy);
							int succSSDT = this.computeRecur(cpu, tPSuccTran, tJ, ssdtMatrix2d, succTrace, tmpVisited, alMatrix, htVertex, traceMatrix2d);
							if(succSSDT != -3) {
								if(!tPSuccTran.getName().startsWith("INV_")/* || newTi.getAttribute("skip") != null*/) {
									++succSSDT;
								}
								/**
								 * 
								 
								++succSSDT;
								*/
								if(minSuccSSDT == -3 || succSSDT < minSuccSSDT) {
									tmpTrace.clear();
									tmpTrace.addAll(succTrace);
									minSuccSSDT = succSSDT;
								}
							}
							for(String s : tmpVisited) {
								if(!(visited.contains(s))) {
									visited.add(s);
								}
							}
						}
					}
					if(minSuccSSDT != -3) {
						trace.add(tIId);
						trace.addAll(tmpTrace);
					}
					return minSuccSSDT;
				}
			}
			else {
				//parallel, tI has more than one output place
				Iterator<String> itTISuccPlaceId = hsTISuccPlace.iterator();
				int nTotalSSDT = -3;
				int nParallel = 0;
				ArrayList<ArrayList<String>> alSuccTrace = new ArrayList<ArrayList<String>>();
				ArrayList<String> visitedCopy = new ArrayList<String>();
				visitedCopy.addAll(visited);
				while(itTISuccPlaceId.hasNext()) {
					Iterator<IBPNode> itTISuccPlace = htVertex.get(itTISuccPlaceId.next()).iterator();
					Condition pTISuccPlace = (Condition) itTISuccPlace.next();
					HashSet<String> hsPSuccTran = this.getPlaceSuccSet(pTISuccPlace, htVertex, true);
					int nPSuccTran = hsPSuccTran.size();
					ArrayList<String> tmpVisited = new ArrayList<String>();
					tmpVisited.addAll(visitedCopy);
					if(nPSuccTran == 0) {
						//cannot reach tj, no action
					} else if(nPSuccTran == 1) {
						//sequential
						Iterator<String> itPSuccTranId = hsPSuccTran.iterator();
						Event tPSuccTran = (Event) htVertex.get(itPSuccTranId.next()).iterator().next();
						ArrayList<String> succTrace = new ArrayList<String>();
						int succSSDT = this.computeRecur(cpu, tPSuccTran, tJ, ssdtMatrix2d, succTrace, tmpVisited, alMatrix, htVertex, traceMatrix2d);
						if(succSSDT != -3) {
							nTotalSSDT += (1 + succSSDT);
							++nParallel;
							if(tPSuccTran.getName().startsWith("INV_")/* && newTi.getAttribute("skip") == null*/) {
								--nTotalSSDT;
							}
							alSuccTrace.add(succTrace);
						}
						for(String s : tmpVisited) {
							if(!visited.contains(s)) {
								visited.add(s);
							}
						}
					} else {
						//exclusive
						Iterator<String> itPSuccTranId = hsPSuccTran.iterator();
						int minSuccSSDT = -3;
						ArrayList<String> tmpTrace = new ArrayList<String>();
						while(itPSuccTranId.hasNext()) {
							Iterator<IBPNode> itPSuccTran = htVertex.get(itPSuccTranId.next()).iterator();
							if(itPSuccTran.hasNext()) {
								Event tPSuccTran = (Event) itPSuccTran.next();
								ArrayList<String> succTrace = new ArrayList<String>();
								tmpVisited.clear();
								tmpVisited.addAll(visited);
								int succSSDT = this.computeRecur(cpu, tPSuccTran, tJ, ssdtMatrix2d, succTrace, tmpVisited, alMatrix, htVertex, traceMatrix2d);
								if(succSSDT != -3) {
									if(!tPSuccTran.getName().startsWith("INV_")/* || newTi.getAttribute("skip") != null*/) {
										++succSSDT;
									}
									/**
									 * 
									 
									++succSSDT;
									*/
									if(minSuccSSDT == -3 || succSSDT < minSuccSSDT) {
										tmpTrace.clear();
										tmpTrace.addAll(succTrace);
										minSuccSSDT = succSSDT;
									}
								}
								for(String s : tmpVisited) {
									if(!visited.contains(s)) {
										visited.add(s);
									}
								}
							}
						}
						if(minSuccSSDT != -3) {
							alSuccTrace.add(tmpTrace);
							nTotalSSDT += minSuccSSDT;
							++nParallel;	
						}
					}
				}
				if(nParallel == 0) {
					return nTotalSSDT;
				} else {
					//get merge trace
					ArrayList<String> parallelSuccTrace = new ArrayList<String>();
					for(ArrayList<String> succTrace : alSuccTrace) {
						for(String s : succTrace) {
							if(parallelSuccTrace.isEmpty() || !parallelSuccTrace.contains(s)) {
								parallelSuccTrace.add(s);
							}
						}
					}
					trace.add(tIId);
					trace.addAll(parallelSuccTrace);
					int tracelength = trace.size() - 1;
					return tracelength;
				}
			}
		} else {
			//nSucc = 0, cannot reach tj
			return -3;
		}
	}
	
	/**
	 * get all the successor places of a transition
	 * @param place
	 * @param htVertex
	 * @param hasInv - whether the result should contain invisible tasks
	 * @return the set of successor places
	 */
	@SuppressWarnings("rawtypes")
	private HashSet<String> getPlaceSuccSet(Condition place, Hashtable<String, HashSet<IBPNode>> htVertex, boolean hasInv) {
		HashSet<String> succId = new HashSet<String>();
		if (place != null) {
			Iterator<IBPNode> itPlace = htVertex.get(place.getName()).iterator();
			while(itPlace.hasNext()) {
				Iterator<Event> itSucc = ((Condition) itPlace.next()).getPostE().iterator();
				while(itSucc.hasNext()) {
					Event pSucc = itSucc.next();
					String pSuccId = pSucc.getName();
					if (hasInv == false && pSucc.getName().startsWith("INV_")) {
						continue;
					}
					succId.add(pSuccId);
				}
			}
		}
		return succId;
	}
	
	/**
	 * get all the predecessor transitions of a place
	 * @param transition
	 * @param htVertex
	 * @return the set of predecessor transitions
	 */
	private HashSet<String> getTransitionPredSet(CompletePrefixUnfolding cpu, Event event, Hashtable<String, HashSet<IBPNode>> htVertex) {
		String tId = event.getName();
		Iterator<IBPNode> itTran = htVertex.get(tId).iterator();
		HashSet<String> predId = new HashSet<String>();
		while(itTran.hasNext()) {
			@SuppressWarnings("unchecked")
			IBPNode iTran = itTran.next();
			if (iTran instanceof Event) {
				Iterator<Condition> itVPred = ((Event) iTran).getPreConditions().iterator();
				while(itVPred.hasNext()) {
					Condition vPred = itVPred.next();
					String vPredId = vPred.getName();
					predId.add(vPredId);
				}
			}
		}
		return predId;
	}
	
	/**
	 * get all the successor event of a place
	 * @param event
	 * @param htVertex
	 * @return the set of successor transitions
	 */
	@SuppressWarnings("rawtypes")
	private HashSet<String> getTransitionSuccSet(CompletePrefixUnfolding cpu, Event event, Hashtable<String, HashSet<IBPNode>> htVertex) {
//		String tId = event.getName();
//		Iterator<IBPNode> itTran = htVertex.get(tId).iterator();
//		HashSet<String> succId = new HashSet<String>();
//		while(itTran.hasNext()) {
//			Iterator<Condition> itSucc = ((Event) itTran.next()).getPostConditions().iterator();
//			while(itSucc.hasNext()) {
//				succId.add(itSucc.next().getName());
//			}
//		}
//		return succId;
		
		HashSet<String> postConditions = new HashSet<String>();
		if (cpu.getCutoffEvents().contains(event)) {
			for (Condition eSucc : event.getPostConditions()) {
				Iterator<Condition> iESuccMappedContions = eSucc.getMappingConditions().iterator();
				while(iESuccMappedContions.hasNext()) {
					Condition eSuccMappedContion = iESuccMappedContions.next();
					postConditions.add(eSuccMappedContion.getName());
				}
			}
		}
		else {
			for (Condition eSucc: event.getPostConditions()) {
				postConditions.add(eSucc.getName());
			}
		}
		return postConditions;
	}
	
	/**
	 * Preprocess descendants table of any vertex, thus, generate the reachable matrix of a cpu
	 * @param cpu
	 * @param n - number of vertices
	 * @param alMatrix - store the visiting order of vertices
	 * @param htVertex - store the map of label->list(vertex)
	 * @return the reachable matrix
	 */
	@SuppressWarnings({ "rawtypes" })
	private DoubleMatrix2D getReachMatrixCPU(CompletePrefixUnfolding cpu, int n, ArrayList<String> alMatrix, 
			Hashtable<String, HashSet<IBPNode>> htVertex) {
		DoubleMatrix2D reachMatrix2d = DoubleFactory2D.sparse.make(n, n, 0);
		alMatrix.clear();
		HashSet<IBPNode> startVertex = new HashSet<IBPNode>();
		
		startVertex.addAll(cpu.getInitialCut());
		
		int order = 0;
		while(startVertex.size() > 0) {
			Iterator<IBPNode> iStartVertex = startVertex.iterator();
			while(iStartVertex.hasNext()) {
				IBPNode v = iStartVertex.next();
				String vId = v.getName();
				if(alMatrix.contains(vId)) {
					continue;
				}
				alMatrix.add(vId);
				reachMatrix2d.set(order, order, 1.0);
				Iterator<IBPNode> itV = htVertex.get(vId).iterator();
				HashSet<String> hsVPredId = new HashSet<String>();
				while(itV.hasNext()) {
					IBPNode vt = itV.next();
					if (vt instanceof Condition) {
						if (((Condition) vt).getPreEvent() != null) {
							hsVPredId.add(((Condition) vt).getPreEvent().getName());
						}
					}
					else if (vt instanceof Event) {
						Iterator<Condition> itVPred = ((Event) vt).getPreConditions().iterator();
						while(itVPred.hasNext()) {
							Condition vPred = itVPred.next();
							String vPredId = vPred.getName();
							hsVPredId.add(vPredId);
						}
					}
				}
				Iterator<String> itVPredId = hsVPredId.iterator();
				while(itVPredId.hasNext()) {
					String VPredId = itVPredId.next();
					int vPredOrder = alMatrix.indexOf(VPredId);
					if(vPredOrder == -1) {
						continue;
					}
					reachMatrix2d.set(vPredOrder, order, 1.0);
					for(int i = 0; i <= vPredOrder; ++i) {
						if(reachMatrix2d.get(i, vPredOrder) != 0.0) {
							reachMatrix2d.set(i, order, 1.0);
						}
					}
				}
				++order;
			}
			
			//move forward one level
			iStartVertex = startVertex.iterator();
			HashSet<IBPNode> descVertex = new HashSet<IBPNode>();
			while(iStartVertex.hasNext()) {
				IBPNode v = iStartVertex.next();
 				String vId = v.getName();
 				if(alMatrix.contains(vId)) {
 					if (v instanceof Condition) {
 						descVertex.addAll(((Condition) v).getPostE());
					}
					else if (v instanceof Event) {
						descVertex.addAll(((Event) v).getPostConditions());
					}
				}
			}
			startVertex.clear();
			startVertex.addAll(descVertex);
		}
		
		Hashtable<String, HashSet<IBPNode>> reachVertex = new Hashtable<String, HashSet<IBPNode>>();
		for(int irow = 0; irow < alMatrix.size(); irow++){
			reachVertex.put(alMatrix.get(irow), new HashSet<IBPNode>());
			for(int jcol = 0; jcol < alMatrix.size(); jcol++) {
				if (reachMatrix2d.get(irow, jcol) != 0.0 && irow != jcol) {
					reachVertex.get(alMatrix.get(irow)).addAll(this.htVertex.get(alMatrix.get(jcol)));
				}
			}
		}
		
		Iterator<Event> iCutOffEvents = cpu.getCutoffEvents().iterator();
		while(iCutOffEvents.hasNext()) {
			Event e = iCutOffEvents.next();
			int eIndex = alMatrix.indexOf(e.getName());
			
//			Event eCor = cpu.getCorrespondingEvent(e);
//			int eCorIndex = alMatrix.indexOf(eCor.getName());

//			ICoSet<BPNode, Condition, Event, Flow, Node, Place, Transition, Marking> ePostConditions = e.getPostConditions();
			for (Condition eSucc : e.getPostConditions()) {
				int eCondiIndex = alMatrix.indexOf(eSucc.getName());
				Iterator<Condition> iESuccMappedContions = eSucc.getMappingConditions().iterator();
				while(iESuccMappedContions.hasNext()) {
					Condition eSuccMappedContion = iESuccMappedContions.next();
					if (eSuccMappedContion.getName().equalsIgnoreCase(eSucc.getName())) {
						continue;
					}
					Event eCorAccordingToCut = eSuccMappedContion.getPreEvent();
					Iterator<IBPNode> iEventCorReachList = reachVertex.get(eCorAccordingToCut.getName()).iterator();
					while(iEventCorReachList.hasNext()) {
						IBPNode v = iEventCorReachList.next();
						int reachableIndex = alMatrix.indexOf(v.getName());
						reachMatrix2d.set(eIndex, reachableIndex, 1.0);
						for (int i = 0; i <= eIndex; i++) {
							if(reachMatrix2d.get(i, eIndex) != 0.0) {
								reachMatrix2d.set(i, reachableIndex, 1.0);
							}
						}
					}
					
//					Iterator<IBPNode> iContiCorReachList = reachVertex.get(eSuccMappedContion.getName()).iterator();
//					while (iContiCorReachList.hasNext()) {
//						IBPNode v = iContiCorReachList.next();
//						int reachableIndex = alMatrix.indexOf(v.getName());
//						reachMatrix2d.set(eCondiIndex, reachableIndex, 1.0);
//						for (int i = 0; i <= eCondiIndex; i++) {
//							if(reachMatrix2d.get(i, eCondiIndex) != 0.0) {
//								reachMatrix2d.set(i, reachableIndex, 1.0);
//							}
//						}
//					}
//					int reachableIndex = alMatrix.indexOf(eSuccMappedContion.getName());
//					reachMatrix2d.set(eIndex, reachableIndex, 1.0);
				}
			}
			
//			for (Condition eCorSucc : eCor.getPostConditions()) {
//				Iterator<Condition> iECorSuccMappedContions = eCorSucc.getMappingConditions().iterator();
//				while(iECorSuccMappedContions.hasNext()) {
//					Condition eCorSuccMappedContion = iECorSuccMappedContions.next();
//					if (eCorSuccMappedContion.getName().equalsIgnoreCase(eCorSucc.getName())) {
//						continue;
//					}
//					int reachableIndex = alMatrix.indexOf(eCorSuccMappedContion.getName());
//					reachMatrix2d.set(eCorIndex, reachableIndex, 1.0);
//				}
//			}
			
			
//			// for e
//			Iterator<IBPNode> iEventReachList = reachVertex.get(e.getName()).iterator();
//			while(iEventReachList.hasNext()) {
//				IBPNode v = iEventReachList.next();
//				int reachableIndex = alMatrix.indexOf(v.getName());
//				reachMatrix2d.set(eCorIndex, reachableIndex, 1.0);
//			}
//			// for corresponding event
//			Iterator<IBPNode> iEventCorReachList = reachVertex.get(eCor.getName()).iterator();
//			while(iEventCorReachList.hasNext()) {
//				IBPNode v = iEventCorReachList.next();
//				int reachableIndex = alMatrix.indexOf(v.getName());
//				reachMatrix2d.set(eIndex, reachableIndex, 1.0);
//			}
		}
		
		return reachMatrix2d;
	}
	
	
	/**
	 * Preprocess descendants table of any vertex, thus, generate the reachable matrix of a cpu
	 * @param cpu
	 * @param n - number of vertices
	 * @param alMatrix - store the visiting order of vertices
	 * @param htVertex - store the map of label->list(vertex)
	 * @return the reachable matrix
	 */
	@SuppressWarnings({ "rawtypes" })
	private DoubleMatrix2D getReachMatrix2d(CompletePrefixUnfolding cpu, int n, ArrayList<String> alMatrix, 
			Hashtable<String, HashSet<IBPNode>> htVertex) {
		DoubleMatrix2D reachMatrix2d = DoubleFactory2D.sparse.make(n, n, 0);
		alMatrix.clear();
		HashSet<IBPNode> startVertex = new HashSet<IBPNode>();
		
		startVertex.addAll(cpu.getInitialCut());
		
		int order = 0;
		while(startVertex.size() > 0) {
			Iterator<IBPNode> iStartVertex = startVertex.iterator();
			while(iStartVertex.hasNext()) {
				IBPNode v = iStartVertex.next();
				String vId = v.getName();
				if(alMatrix.contains(vId)) {
					continue;
				}
				alMatrix.add(vId);
				reachMatrix2d.set(order, order, 1.0);
				Iterator<IBPNode> itV = htVertex.get(vId).iterator();
				HashSet<String> hsVPredId = new HashSet<String>();
				while(itV.hasNext()) {
					IBPNode vt = itV.next();
					if (vt instanceof Condition) {
						if (((Condition) vt).getPreEvent() != null) {
							hsVPredId.add(((Condition) vt).getPreEvent().getName());
						}
					}
					else if (vt instanceof Event) {
						Iterator<Condition> itVPred = ((Event) vt).getPreConditions().iterator();
						while(itVPred.hasNext()) {
							Condition vPred = itVPred.next();
							String vPredId = vPred.getName();
							hsVPredId.add(vPredId);
						}
					}
				}
				Iterator<String> itVPredId = hsVPredId.iterator();
				while(itVPredId.hasNext()) {
					String VPredId = itVPredId.next();
					int vPredOrder = alMatrix.indexOf(VPredId);
					if(vPredOrder == -1) {
						continue;
					}
					reachMatrix2d.set(vPredOrder, order, 1.0);
					for(int i = 0; i <= vPredOrder; ++i) {
						if(reachMatrix2d.get(i, vPredOrder) != 0.0) {
							reachMatrix2d.set(i, order, 1.0);
						}
					}
				}
				++order;
			}
			
			//move forward one level
			iStartVertex = startVertex.iterator();
			HashSet<IBPNode> descVertex = new HashSet<IBPNode>();
			while(iStartVertex.hasNext()) {
				IBPNode v = iStartVertex.next();
 				String vId = v.getName();
 				if(alMatrix.contains(vId)) {
 					if (v instanceof Condition) {
 						descVertex.addAll(((Condition) v).getPostE());
					}
					else if (v instanceof Event) {
						descVertex.addAll(((Event) v).getPostConditions());
					}
				}
			}
			startVertex.clear();
			startVertex.addAll(descVertex);
		}
		return reachMatrix2d;
	}
	
	public ArrayList<Event> getInvisibleTasks(CompletePrefixUnfolding cpu) {
		ArrayList<Event> result = new ArrayList<Event>();
		Iterator allTransitions = cpu.getEvents().iterator();
		while (allTransitions.hasNext()) {
			Event currentTransition = (Event) allTransitions.next();
			if (currentTransition.getName().startsWith("INV_")) {
				result.add(currentTransition);
			}
//			if (currentTransition.isInvisibleTask() == true) {
//				result.add(currentTransition);
//			}
		}
		return result;
	}

	/**
	 * Retrieves the visible tasks contained in this net.
	 * 
	 * @return the list of those transitions that resemble visible tasks. The
	 *         list may be empty if the model contains no visible tasks
	 */
	public ArrayList<Event> getVisibleTasks(CompletePrefixUnfolding cpu) {
		ArrayList<Event> result = new ArrayList<Event>();
		Iterator allTransitions = cpu.getEvents().iterator();
		while (allTransitions.hasNext()) {
			Event currentTransition = (Event) allTransitions.next();
			if (!currentTransition.getName().startsWith("INV_")) {
				result.add(currentTransition);
			}
//			if (currentTransition.isInvisibleTask() == false) {
//				result.add(currentTransition);
//			}
		}
		return result;
	}
	
	/** 
	 * Get a list of invisible tasks of skip type in a cfp
	 * @param cfp
	 * @param htVertex - the map of label->list(vertex)
	 * @return list of invisible tasks of skip type
	 */
	private ArrayList<String> getSkipInvisibleTasks(CompletePrefixUnfolding cpu, Hashtable<String, HashSet<IBPNode>> htVertex) {
		ArrayList<Event> alInvTasks = getInvisibleTasks(cpu);
		ArrayList<String> visitedTasks = new ArrayList<String>();
		ArrayList<String> alSkipTasks = new ArrayList<String>();
		for(Event t : alInvTasks) {
			if(visitedTasks.contains(t.getName())) {
				continue;
			}
			visitedTasks.add(t.getName());
			HashSet<String> tPredPlaceId = getTransitionPredSet(cpu, t, htVertex);
			HashSet<String> tSuccPlaceId = getTransitionSuccSet(cpu, t, htVertex);
			//if there is a trace from pred place to succ place other than the invisible task itself
			//this invisible task is of skip type
			boolean canReach = false;
			for(String source : tPredPlaceId) {
				for(String target : tSuccPlaceId) {
					ArrayList<String> visited = new ArrayList<String>();
					visited.add(t.getName());
					if(this.canReach(source, target, htVertex, visited) == true) {
						canReach = true;
						break;
					}
				}
				if(canReach == true) {
					break;
				}
			}
			if(canReach == true) {
				Iterator<IBPNode> itInvTask = htVertex.get(t.getName()).iterator();
				while(itInvTask.hasNext()) {
					IBPNode v = itInvTask.next();
//					v.setAttribute("skip", "skip");
				}
				alSkipTasks.add(t.getName());
			}
		}
		return alSkipTasks;
	}
	
	/**
	 * Judge if there is a trace from a vertex to another
	 * @param source - source vertex
	 * @param target - target vertex
	 * @param htVertex - the map of label->list(vertex)
	 * @param visited - list of vertices which have been visited
	 * @return a boolean type of value
	 */
	@SuppressWarnings("unchecked")
	private boolean canReach(String source, String target, Hashtable<String, HashSet<IBPNode>> htVertex,
			ArrayList<String> visited) {
		HashSet<IBPNode> startNodes = htVertex.get(source);
		while(startNodes.size() > 0) {
			Iterator<IBPNode> iStart = startNodes.iterator();
			ArrayList<String> exclude = new ArrayList<String>();
			while(iStart.hasNext()) {
				IBPNode v = iStart.next();
				String vId = v.getName();
				if(vId.split("-")[0].equals(target.split("-")[0])) {
					return true;
//				if(vId.equals(target)) {
//					return true;
				} else if(visited.contains(vId)) {
					exclude.add(vId);
					continue;
				}
				visited.add(vId);
			}
			//move forward one level
			iStart = startNodes.iterator();
			HashSet<IBPNode> descVertex = new HashSet<IBPNode>();
			while(iStart.hasNext()) {
				IBPNode v = iStart.next();
				String vId = v.getName();
				if(exclude.contains(vId)) {
					continue;
				}
				if(visited.contains(vId)) {
					if (v instanceof Event) {
						descVertex.addAll(((Event) v).getPostConditions());
					}
					else if (v instanceof Condition) {
						descVertex.addAll(((Condition) v).getPostE());
					}
					
				}
			}
			startNodes = new HashSet<IBPNode>();
			startNodes.addAll(descVertex);
		}
		return false;
	}
}

