package com.iise.shawn.util;
import java.io.*;

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
        batch.modModels("D:\\实验室\\开题\\DG\\loop\\");
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
            for(Transition t : pn.getTransitions()) {
            	if (t.getLogEvent() == null) {
            		t.setLogEvent(new LogEvent("INV_" + t.getIdentifier().replace(",", " and"), "auto"));
            		t.setIdentifier("INV_" + t.getIdentifier().replace(",", " and"));
            	}
            	else if (t.getIdentifier().matches("T\\d+")) {
            		t.setLogEvent(new LogEvent("INV_" + t.getIdentifier().replace(",", " and"), "auto"));
            		t.setIdentifier("INV_" + t.getIdentifier().replace(",", " and"));
            	}
            	else {
            		t.setLogEvent(new LogEvent(t.getIdentifier(), "auto"));
            	}
            	t.setIdentifier(t.getIdentifier().replace(",", " and"));
                
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