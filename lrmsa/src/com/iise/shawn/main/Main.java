package com.iise.shawn.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;

import org.jbpt.petri.NetSystem;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.processmining.exporting.DotPngExport;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.plugin.ProvidedObject;
import org.processmining.importing.pnml.PnmlImport;

import com.iise.shawn.ssd.SSD;
import com.iise.shawn.util.PetriNetConversion;

import cern.colt.matrix.DoubleMatrix2D;

public class Main {
	public static void main(String[] args) throws Exception {
		String fileName = "/Users/shawn/Documents/LAB/开题/exp/myModels/misorder/5_choice_1_loop_2";
		String filePath1 = fileName + ".pnml";
		String filePath2 = fileName + ".png";
		String filePath3 = fileName + "-cfp.png";
				
		PnmlImport pnmlImport = new PnmlImport();
		PetriNet p1 = pnmlImport.read(new FileInputStream(new File(filePath1)));
		
		// ori
		
		ProvidedObject po1 = new ProvidedObject("petrinet", p1);
		
		DotPngExport dpe1 = new DotPngExport();
		OutputStream image1 = new FileOutputStream(filePath2);
		dpe1.export(po1, image1);
		

		NetSystem ns = PetriNetConversion.convert(p1);
		CompletePrefixUnfolding cpu = new CompletePrefixUnfolding(ns);
		
		// cfp
		
		PetriNet p2 = PetriNetConversion.convert(cpu);
		ProvidedObject po2 = new ProvidedObject("petrinet", p2);
		DotPngExport dpe2 = new DotPngExport();
		OutputStream image2 = new FileOutputStream(filePath3);
		dpe2.export(po2, image2);
	}
}
