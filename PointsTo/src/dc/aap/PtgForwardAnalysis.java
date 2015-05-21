package dc.aap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import soot.*;
import soot.tagkit.*;
import soot.jimple.*;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.internal.JimpleLocalBox;
import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class PtgForwardAnalysis extends ForwardFlowAnalysis{

	public FlowInfo global;
	
	public PtgForwardAnalysis(DirectedGraph<Unit> _graph) {
		super(_graph);
		doAnalysis();
	}

	@Override
	protected void flowThrough(Object in, Object u, Object out) {
		
		Unit d = (Unit)u;
		FlowInfo out_flow = (FlowInfo) out;
		FlowInfo in_flow = (FlowInfo) in;
		in_flow.copy(out_flow);
		
		if (d instanceof IdentityStmt) {
			// i0 := @parameter0: int;
			// Cuando le da nombre a los parametros de la funcion.
			// Def: JimpleLocalBox(i0), Use: IdentityRefBox(@parameter0: int)
			// Creo que solo interesa el Def.
			System.out.println("Es un parametro");
			String formalParam = d.getDefBoxes().get(0).getValue().toString();
			String paramNode = "parameter_" + formalParam;
			out_flow.p_nodes.add(paramNode);
			Set<String> s = new HashSet<String>();
			s.add(paramNode);
			out_flow.locals.put(formalParam, s);
		}
		
		if (d instanceof InvokeStmt) {
			InvokeExpr invoke = ((InvokeStmt) d).getInvokeExpr();
			SootMethod method = invoke.getMethod();
			//String className = invoke.getMethodRef().declaringClass().getName();

			if (method.getName().equals("<init>")) {
				System.out.println("TODO: <init> Es una invocacion de un constructor");
			} else {
				Body b = method.retrieveActiveBody();
				UnitGraph g = new BriefUnitGraph(b);
				PtgForwardAnalysis analysis = new PtgForwardAnalysis(g);
				
				// Saco primero los parametros formales y luego hago el merge.
				String id = IdGenerator.GenerateId();
				List<String> invocationParamNodes = getInvocationParamNodes(analysis, id);
				analysis.global.mergeMethod(out_flow, id);
				
				// Se reemplazan los parametros de la invocacion
				List<Value> args = invoke.getArgs();		
				for (int i = 1; i < invocationParamNodes.size(); i++) {
					String formalParamNode = out_flow.p_nodes.get(i);
					String localArg = args.get(i-1).toString();
					Set<String> sl = out_flow.locals.get(localArg);
					if (sl == null) {
						System.out.println("Error, no se encontro el key " + localArg + " en locals.");
						continue;
					}
					for (String localParamNode : sl) {
						//System.out.println(formalParamNode + " - " + localParamNode );
						//out_flow.replaceNode(formalParamNode, localParamNode);
						Edge e = new Edge(formalParamNode, "alias", localParamNode);
						out_flow.edges.add(e);
					}
				}
				
				// Se reemplaza el this.
				//Quizas hay una forma mas facil :-P
				String local_this = invoke.getUseBoxes().get(invoke.getArgCount()).getValue().toString();
				String invocationThisNode =  invocationParamNodes.get(0);
				for (String localThisNode : out_flow.locals.get(local_this)) {
					//out_flow.replaceNode(invocationThisNode, localThisNode);
					Edge e = new Edge(invocationThisNode, "alias", localThisNode);
					out_flow.edges.add(e);
				}
			}
		}
		
		if (d instanceof ReturnVoidStmt) {
			System.out.println("TODO: Es un return void");
		}
		
		if (d instanceof ReturnStmt) {
			System.out.println("TODO: Es un return");
		}
				
		if (d instanceof AssignStmt) {
			// Si no tenemos lado iz o der, lo ignoramos
			if (d.getUseBoxes().isEmpty() || d.getDefBoxes().isEmpty())
				return;
			
			boolean rightField = false;
			boolean leftField = false;
			//System.out.println("XXX:d.getUseBoxes() " + d.getUseBoxes());
			//System.out.println("XXX:d.getDefBoxes() " + d.getDefBoxes());

			for (ValueBox i: d.getUseBoxes()) {
				if (i.getValue() instanceof FieldRef) {
					rightField = true;
					//System.out.println("XXXXXX: encontre rightField");
				}
			}
			
			for (ValueBox i: d.getDefBoxes()) {
				if (i.getValue() instanceof FieldRef) {
					leftField = true;
					//System.out.println("XXXXXX: encontre leftField");
				}
			}
			
			List<ValueBox> def = d.getDefBoxes();
			List<ValueBox> use = d.getUseBoxes();
			
			Value expr = d.getUseBoxes().get(0).getValue();
			if (expr instanceof AnyNewExpr) {
				System.out.println("Es un new");
				this.proc_new(in_flow, d, out_flow);
			} else if (expr instanceof InvokeExpr) {
				System.out.println("Es un assign x = m()");
				proc_wrongs(in_flow, def, use, out_flow);
			} else if (leftField && rightField) {
				System.out.println("Es un assign x.f = y.f");
				proc_ref_eq_ref(in_flow, def, use, out_flow);
			} else if (leftField) {
				System.out.println("Es un assign x.f = 5 o y");
				proc_field_eq_ref(in_flow, def, use, out_flow);
			} else if (rightField) {
				System.out.println("Es un assign x = y.f");
				proc_ref_eq_field(in_flow, def, use, out_flow);
			} else if (!leftField && !rightField) {
				System.out.println("Es un assign x = y o cte");
				proc_ref_eq_ref(in_flow, def, use, out_flow);
			}
		}

		out_flow.toDotFile();
		System.out.println(out_flow);
		
		//global.merge(out_flow);
		out_flow.merge(global);
		System.out.println("================");
	}
	
	private List<String> getInvocationParamNodes(PtgForwardAnalysis analysis, String id) {
		List<String> retNodes = new ArrayList<String>();
		for (String n : analysis.global.p_nodes) {
			retNodes.add(id + "_" + n);
		}
		return retNodes;
	}

	protected void proc_new(FlowInfo in, Unit d, FlowInfo out) {
		Value right = d.getUseBoxes().get(0).getValue();
		Value left = d.getDefBoxes().get(0).getValue();
		
		int line = -2;
		LineNumberTag lineTag = (LineNumberTag)d.getTag("LineNumberTag ");
		if (lineTag != null) {
			line = lineTag.getLineNumber();
		}
		
		// El nombre del nodo es de la forma: A_p
		String vName = right.getType() + "_" + d.getJavaSourceStartLineNumber();		
		out.nodes.add(vName);
		
		// La instruccion es del tipo:
		//   p: x = new A;
		// Entonces ponemos en el map de locals:
		//   x --> {A_p}
		HashSet<String> vNameSet = new HashSet<String>();
		vNameSet.add(vName);
		
		System.out.println("Se pone en locals - L:" + left.toString() + " N: " + vNameSet);
		
		out.locals.put(left.toString(), vNameSet);
	}
	
	protected void proc_ref_eq_ref(FlowInfo in, List<ValueBox> left_l, List<ValueBox> right_l, FlowInfo out) {
		// Tenemos que hacer L'(left) = L(right)
		// Y como put() sobre escribe left si ya estaba esa clave, no hace falta borrarlo
		Value left = left_l.get(0).getValue();
		Value right = right_l.get(0).getValue();
		out.locals.put(left.toString(),out.locals.get(right.toString()));
	}
	
	protected void proc_wrongs(FlowInfo in, List<ValueBox> left_l, List<ValueBox> right_l, FlowInfo out) {
		Value left = left_l.get(0).getValue();
		Value right = right_l.get(0).getValue();
		out.wrongs.add(right.toString());
	}

	protected void proc_ref_eq_field(FlowInfo in, List<ValueBox> left_l, List<ValueBox> right_l, FlowInfo out) {
		// Es una instruccion de la forma: x = y.f
		Value left = left_l.get(0).getValue();
		Value right = right_l.get(0).getValue();
		String x = left.toString();
		String y = right.getUseBoxes().get(0).getValue().toString();
		
		// XXX: Parseamos el string para sacar el nombre del field.
		// Es un asco y puede fallar en otras versiones, pero al menos
		// nos deja avanzar con el tp
		String[] f_splice = right.toString().split(" +");
		String f = f_splice[f_splice.length - 1];
		f = f.substring(0, f.length() - 1);

		// Borramos lo que sea que tenga x
		in.locals.put(x, new HashSet<String>());
		
		// Para ln "libre".
		String ln = "l_" + IdGenerator.GenerateId() + "_" + y;
		out.nodes.add(ln);

		// R' = R U { (n,f,ln) | n in L(y) }
		for (String n : in.locals.get(y)){
			Edge e = new Edge(n, f, ln);
			out.r_edges.add(e);
		}
		
		// L'(x) = { ln }
		out.locals.get(x).add(ln);		
	}
	
	protected void proc_field_eq_ref(FlowInfo in, List<ValueBox> left_l, List<ValueBox> right_l, FlowInfo out) {
		// Es una instruccion de la forma: x.f = y
		Value left = left_l.get(0).getValue();
		Value right = right_l.get(1).getValue();
		String x = left.getUseBoxes().get(0).getValue().toString();
		String y = right.toString();
		
		// XXX: Parseamos el string para sacar el nombre del field.
		// Es un asco y puede fallar en otras versiones, pero al menos
		// nos deja avanzar con el tp
		String[] f_splice = left.toString().split(" +");
		String f = f_splice[f_splice.length - 1];
		f = f.substring(0, f.length() - 1);

		if ((!in.locals.containsKey(y)) || (!in.locals.containsKey(x)))
			return;
		
		for (String n : in.locals.get(y)) {
			for (String a : in.locals.get(x)) {
				Edge e = new Edge(a, f, n);
				out.edges.add(e);
			}
		}
	}
	
	@Override
	protected Object newInitialFlow() {
		global = new FlowInfo();
		return global;
	}

	@Override
	protected void merge(Object in1, Object in2, Object out) {
		// Merge se usa con los if, que no hay en nuestro lenguaje, no ?
		// Como que lo hicimos por deporte, parece :)
		FlowInfo in1_flow = (FlowInfo) in1;
		FlowInfo in2_flow = (FlowInfo) in2;
		FlowInfo out_flow = (FlowInfo) out;
		
		out_flow.nodes = in1_flow.nodes;
		out_flow.locals = in1_flow.locals;
		out_flow.edges = in1_flow.edges;
		out_flow.wrongs = in1_flow.wrongs;
		
		out_flow.nodes.addAll(in2_flow.nodes);
		out_flow.locals.putAll(in2_flow.locals);
		out_flow.edges.addAll(in2_flow.edges);
		out_flow.wrongs.addAll(in2_flow.wrongs);
		
		// XXX: Hace falta ? O es todo referencia y es no-op ?
		out = out_flow;
	}

	@Override
	protected void copy(Object source, Object dest) {
		((FlowInfo)source).copy((FlowInfo)dest);
	}

}