package dc.aap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import soot.*;
import soot.tagkit.*;
import soot.JastAddJ.Literal;
import soot.jimple.*;
import soot.jimple.internal.InvokeExprBox;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class PtgForwardAnalysis extends ForwardFlowAnalysis {

	private PointsToGraph pointsToGraph;
	
	public PtgForwardAnalysis(DirectedGraph<Unit> graph) {
		super(graph);
		doAnalysis();
	}

	@Override
	protected Object newInitialFlow() {
		pointsToGraph = new PointsToGraph();
		return pointsToGraph;
	}

	@Override
	protected void copy(Object source, Object dest) {
		((PointsToGraph)source).copy((PointsToGraph)dest);
	}

	@Override
	protected void merge(Object in1, Object in2, Object out) {
		// Merge se usa con los if, que no hay en nuestro lenguaje, no ?
		PointsToGraph in1_flow = (PointsToGraph) in1;
		PointsToGraph in2_flow = (PointsToGraph) in2;
		PointsToGraph out_flow = (PointsToGraph) out;
		
		in1_flow.merge(out_flow);
		in2_flow.merge(out_flow);
		
		out_flow.merge(pointsToGraph);
	}
	
	@Override
	protected void flowThrough(Object in, Object u, Object out) {
		Unit d = (Unit)u;
		PointsToGraph out_flow = (PointsToGraph) out;
		PointsToGraph in_flow = (PointsToGraph) in;
		in_flow.copy(out_flow);
		
		if (d instanceof IdentityStmt) {
			processParameterDeclarationStmt(d, out_flow);
		} else if (d instanceof InvokeStmt) {
			InvokeExpr invoke = ((InvokeStmt) d).getInvokeExpr();
			if (invoke.getMethod().getName().equals("<init>")) {
				processNotHandleStmt(d, out_flow);
			} else {
				processInvocationStmt(invoke, out_flow);
			}
		} else if (d instanceof AssignStmt) {
			processAssignmentStmt(d, out_flow);
		} else if (d instanceof ReturnStmt){
			processReturnStmt(d, out_flow);
		} else {
			processNotHandleStmt(d, out_flow);
		}
			
		out_flow.mergeNodes();
		out_flow.merge(pointsToGraph);
	}

	private void processReturnStmt(Unit d, PointsToGraph out_flow) {
		Set<String> ls = out_flow.locals.get(d.getUseBoxes().get(0).getValue().toString());
		out_flow.returnNodes = ls;
	}

	private Set<String> processInvocationStmt(InvokeExpr invoke, PointsToGraph out_flow) {
		Body b = invoke.getMethod().retrieveActiveBody();
		UnitGraph g = new BriefUnitGraph(b);
		PtgForwardAnalysis analysis = new PtgForwardAnalysis(g);
		
		// Saco primero los parametros formales y luego hago el merge.
		int id = IdGenerator.GenerateId();
		PointsToGraph internalPtg = analysis.pointsToGraph.getPtgWithoutLocalsWithPrefix(id);
		internalPtg.merge(out_flow);
		
		// Se reemplazan los parametros de la invocacion
		List<Value> args = invoke.getArgs();
		if (args.size() > 0) {
			for (int i = 1; i < internalPtg.p_nodes.size(); i++) {
				String formalParamNode = internalPtg.p_nodes.get(i);
				String localArg = args.get(i-1).toString();
				if (!out_flow.locals.containsKey(localArg)) {
					// Debe ser un literal, no se hace nada.
					continue;
				}
				Set<String> sl = out_flow.locals.get(localArg);
				for (String localParamNode : sl) {
					out_flow.replaceNode(formalParamNode, localParamNode);
				}
			}
		}
		
		// Si es static no tiene this.
		if (!invoke.getMethodRef().isStatic()) {
			// Se reemplaza el this.
			//Quizas hay una forma mas facil de obtenerlo :-P
			String local_this = invoke.getUseBoxes().get(invoke.getArgCount()).getValue().toString();
			String invocationThisNode =  internalPtg.p_nodes.get(0);
			for (String localThisNode : out_flow.locals.get(local_this)) {
				out_flow.replaceNode(invocationThisNode, localThisNode);
			}
		}

		return internalPtg.returnNodes;
	}

	private void processParameterDeclarationStmt(Unit d, PointsToGraph out_flow) {
		// i0 := @parameter0: int;
		// Cuando le da nombre a los parametros de la funcion.
		// Def: JimpleLocalBox(i0), Use: IdentityRefBox(@parameter0: int)
		// Creo que solo interesa el Def.
		String formalParam = d.getDefBoxes().get(0).getValue().toString();
		String paramNode = "parameter_" + formalParam;
		out_flow.p_nodes.add(paramNode);
		Set<String> s = new HashSet<String>();
		s.add(paramNode);
		out_flow.locals.put(formalParam, s);
	}

	private void processAssignmentStmt(Unit d, PointsToGraph out_flow) {
		// Si no tenemos lado iz o der, lo ignoramos
		if (d.getUseBoxes().isEmpty() || d.getDefBoxes().isEmpty())
			return;
		
		boolean rightField = false;
		boolean leftField = false;
		
		for (ValueBox i: d.getUseBoxes()) {
			if (i.getValue() instanceof FieldRef) {
				rightField = true;
			}
		}

		for (ValueBox i: d.getDefBoxes()) {
			if (i.getValue() instanceof FieldRef) {
				leftField = true;
			}
		}

		Value expr = d.getUseBoxes().get(0).getValue();
		if (expr instanceof AnyNewExpr) {
			processNewAssignment(d, out_flow);
		} else if (expr instanceof InvokeExpr) {
			processInvocationAssignment(d, (InvokeExpr)expr, out_flow);
		} else if (leftField && rightField) {
			processReferenceToReferenceAssignment(d, out_flow);
		} else if (leftField) {
			processFieldToReferenceOrLiteralAssignment(d, out_flow);
		} else if (rightField) {
			processFieldToReferenceAssignment(d, out_flow);
		} else if (!leftField && !rightField) {
			processReferenceToReferenceAssignment(d, out_flow);
		}
	}

	private void processNotHandleStmt(Unit d, PointsToGraph out) {
		out.wrongs.add(d.toString());
	}

	private void processNewAssignment(Unit d, PointsToGraph out) {
		Value right = d.getUseBoxes().get(0).getValue();
		Value left = d.getDefBoxes().get(0).getValue();
		
		// El nombre del nodo es de la forma: A_p
		String vName = right.getType() + "_" + d.getJavaSourceStartLineNumber();		
		out.nodes.add(vName);
		
		// La instruccion es del tipo:
		//   p: x = new A;
		// Entonces ponemos en el map de locals:
		//   x --> {A_p}
		HashSet<String> vNameSet = new HashSet<String>();
		vNameSet.add(vName);
		
		out.locals.put(left.toString(), vNameSet);
	}
	
	private void processReferenceToReferenceAssignment(Unit d, PointsToGraph out) {
		// Tenemos que hacer L'(left) = L(right)
		// Y como put() sobre escribe left si ya estaba esa clave, no hace falta borrarlo
		Value def = d.getDefBoxes().get(0).getValue();
		Value use = d.getUseBoxes().get(0).getValue();
		out.locals.put(def.toString(),out.locals.get(use.toString()));
	}
	
	private void processFieldToReferenceAssignment(Unit d, PointsToGraph out) {
		// Es una instruccion de la forma: x = y.f
		Value def =  d.getDefBoxes().get(0).getValue();
		Value use =  d.getUseBoxes().get(0).getValue();
		String x = def.toString();
		String y = use.getUseBoxes().get(0).getValue().toString();
	
		String f = getFieldNameForExpr(use);

		// Borramos lo que sea que tenga x
		out.locals.put(x, new HashSet<String>());
		
		// Para ln "libre".
		String ln = "l_" + IdGenerator.GenerateId() + "_" + y;
		out.nodes.add(ln);

		// R' = R U { (n,f,ln) | n in L(y) }
		for (String n : out.locals.get(y)){
			Edge e = new Edge(n, f, ln);
			out.r_edges.add(e);
		}
		
		// L'(x) = { ln }
		out.locals.get(x).add(ln);		
	}

	private void processInvocationAssignment(Unit d, InvokeExpr expr, PointsToGraph out_flow) {
		// int x = y.m();
		String x = d.getDefBoxes().get(0).getValue().toString();
		Set<String> retNodes = processInvocationStmt(expr, out_flow);
		out_flow.locals.put(x, retNodes);
	}
	
	private void processFieldToReferenceOrLiteralAssignment(Unit d, PointsToGraph out) {
		// Es una instruccion de la forma: x.f = y
		Value def =  d.getDefBoxes().get(0).getValue();
		Value use = d.getUseBoxes().get(1).getValue(); //OJO! Aca se accede al segundo elem.
		String x = def.getUseBoxes().get(0).getValue().toString();
		String y = use.toString();
		
		String f = getFieldNameForExpr(def);

		if ((!out.locals.containsKey(y)) || (!out.locals.containsKey(x)))
			return;
		
		for (String n : out.locals.get(y)) {
			for (String a : out.locals.get(x)) {
				Edge e = new Edge(a, f, n);
				out.edges.add(e);
			}
		}
	}

	private String getFieldNameForExpr(Value fieldAccessExpr) {
		String[] f_splice = fieldAccessExpr.toString().split(" +");
		String f = f_splice[f_splice.length - 1];
		f = f.substring(0, f.length() - 1);
		return f;
	}

	public PointsToGraph getPointsToGraph() {
		return pointsToGraph;
	}
}