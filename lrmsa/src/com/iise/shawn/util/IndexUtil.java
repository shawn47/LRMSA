package com.iise.shawn.util;

import java.util.HashMap;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Transition;

public class IndexUtil {
	public static HashMap<String, Transition> getTransMap(PetriNet net) {
		HashMap<String, Transition> transMap = new HashMap<String, Transition>();
		for (Transition trans : net.getTransitions()) {
			transMap.put(trans.getIdentifier(), trans);
		}

		return transMap;
	}
}
