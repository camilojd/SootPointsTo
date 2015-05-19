package dc.aap;

import java.io.File;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;


public class StandaloneMain {

	public static void main(String[] args) {		
		File someClassFile = new File("./bin/").getAbsoluteFile();
     	Scene.v().setSootClassPath(Scene.v().getSootClassPath() + File.pathSeparator + someClassFile);
		SootClass c = Scene.v().loadClassAndSupport("dc.aap.analyzed.SomeClass");
		c.setApplicationClass();
		SootMethod m = c.getMethodByName("entryPoint");
		soot.options.Options.v().set_keep_line_number(true);
		//soot.options.Options.v().print_tags_in_output();
		Scene.v().loadNecessaryClasses();
		
		Body b = m.retrieveActiveBody();
		System.out.println(b);
		UnitGraph g = new BriefUnitGraph(b);
		PtgForwardAnalysis analysis = new PtgForwardAnalysis(g);
//		for (Unit u : g) {
//			
//		}
	}	
}
