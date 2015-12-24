package com.iise.shawn.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.processmining.framework.log.LogEvent;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Transition;
import org.processmining.framework.models.petrinet.algorithms.PnmlWriter;
import org.processmining.importing.pnml.PnmlImport;

public class AddEventLog {
	public String modModels(String path) throws Exception {
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
                t.setLogEvent(new LogEvent(t.getIdentifier(), "auto"));
            }
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            PnmlWriter.write(false, true, pn, writer);
            writer.close();
        }
        return path;
    }
}
