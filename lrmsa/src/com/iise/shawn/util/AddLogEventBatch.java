package com.iise.shawn.util;
import java.io.*;
import java.util.Random;

import org.processmining.framework.log.LogEvent;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Place;
import org.processmining.framework.models.petrinet.Transition;
import org.processmining.framework.models.petrinet.algorithms.PnmlWriter;
import org.processmining.importing.pnml.PnmlImport;

public class AddLogEventBatch {
    public static void main(String[] args) throws Exception {
        // TODO Auto-generated method stub
        AddLogEventBatch batch = new AddLogEventBatch();
        batch.modModels("D:\\实验室\\开题\\IBM\\safe\\");
        System.exit(0);
    }

    public void modModels(String path) throws Exception {
        PnmlImport pnmlImport = new PnmlImport();
        File folder = new File(path);
        File[] files = folder.listFiles();
        for(File file : files) {
            FileInputStream input = new FileInputStream(file);
            System.out.println(file.getAbsolutePath());
            PetriNet pn = pnmlImport.read(input);
            input.close();
            pn.setName(file.getName());
            int size = pn.getTransitions().size();
            Random random = new Random();
            random.setSeed(555L);
            int numInvisibleTask = (int) Math.round(size * 0.2);
            int[] randomIndex = new int[numInvisibleTask];
            for (int i = 0; i < numInvisibleTask; i++) {
            	randomIndex[i] = random.nextInt(size);
            }
            int randomIndexPointer = 0, index = 0;
            for(Transition t : pn.getTransitions()) {
            	if (index == randomIndex[randomIndexPointer]) {
            		if (t.getLogEvent() == null) {
                		t.setLogEvent(new LogEvent("INV_" + t.getIdentifier().replace(",", " and").toUpperCase(), "auto"));
                		t.setIdentifier("INV_" + t.getIdentifier().replace(",", " and").toUpperCase());
                	}
                	else if (t.getIdentifier().matches("T\\d+")) {
                		t.setLogEvent(new LogEvent("INV_" + t.getIdentifier().replace(",", " and").toUpperCase(), "auto"));
                		t.setIdentifier("INV_" + t.getIdentifier().replace(",", " and").toUpperCase());
                	}
                	else {
                		t.setLogEvent(new LogEvent(t.getIdentifier().toUpperCase(), "auto"));
                	}
                	t.setIdentifier(t.getIdentifier().replace(",", " and").toUpperCase());
                	randomIndexPointer++;
                	if (randomIndexPointer > numInvisibleTask - 1) {
                		randomIndexPointer = numInvisibleTask - 1;
                	}
            	}
            	else {
            		if (t.getLogEvent() == null) {
//                		t.setLogEvent(new LogEvent("INV_" + t.getIdentifier().replace(",", " and"), "auto"));
//                		t.setIdentifier("INV_" + t.getIdentifier().replace(",", " and"));
                		t.setLogEvent(new LogEvent(t.getIdentifier().replace(",", " and").toUpperCase(), "auto"));
                		t.setIdentifier(t.getIdentifier().replace(",", " and").toUpperCase());
                	}
                	else if (t.getIdentifier().matches("T\\d+")) {
                		t.setLogEvent(new LogEvent("INV_" + t.getIdentifier().replace(",", " and").toUpperCase(), "auto"));
                		t.setIdentifier("INV_" + t.getIdentifier().replace(",", " and").toUpperCase());
                	}
                	else {
                		t.setLogEvent(new LogEvent(t.getIdentifier().toUpperCase(), "auto"));
                	}
                	t.setIdentifier(t.getIdentifier().replace(",", " and").toUpperCase());
            	}
                index++;
            }
            for(Place p : pn.getPlaces()) {
            	p.setIdentifier(p.getIdentifier().toLowerCase());
            }
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            PnmlWriter.write(false, true, pn, writer);
            writer.close();
        }
    }
}