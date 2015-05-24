package dc.aap;

import java.io.File;

import dc.aap.analyzed.SomeClass.Class1;
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
		soot.options.Options.v().set_keep_line_number(true);
		Scene.v().setSootClassPath(Scene.v().getSootClassPath() + File.pathSeparator + someClassFile);
		SootClass c = Scene.v().loadClassAndSupport("dc.aap.analyzed.SomeClass");	
		c.setApplicationClass();
		Scene.v().loadNecessaryClasses();
		SootMethod m = c.getMethodByName("entryPoint");
		Body b = m.retrieveActiveBody();
		System.out.println(b);
		UnitGraph g = new BriefUnitGraph(b);
		PtgForwardAnalysis analysis = new PtgForwardAnalysis(g);
		analysis.getPointsToGraph().toDotFile();
		//System.out.println(analysis.getPointsToGraph());
	}	
}
