package dc.aap;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;

import soot.*;
import soot.tagkit.*;

import org.apache.commons.lang3.builder.*;

import soot.jimple.*;
import soot.Unit;
import soot.Value;
//import soot.ValueBox;
import soot.toolkits.graph.DirectedGraph;
//import soot.toolkits.scalar.ArraySparseSet;
//import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

//import org.jgrapht.*;
//import org.jgrapht.graph.*;

public class PtgForwardAnalysis extends ForwardFlowAnalysis{
	
	public PtgForwardAnalysis(DirectedGraph<Unit> graph) {
		super(graph);
		doAnalysis();
	}

	@Override
	protected void flowThrough(Object in, Object u, Object out) {
		
		Unit d = (Unit)u;
		FlowInfo out_flow = (FlowInfo) out;
		FlowInfo in_flow = (FlowInfo) in;
		in_flow.copy(out_flow);

		// Si no es un assign statement, por ahora lo ignoramos
		Stmt stmt = (Stmt)d;
		if (!(stmt instanceof AssignStmt))
			return;

		// Si no tenemos lado iz o der, lo ignoramos
		if (d.getUseBoxes().isEmpty() || d.getDefBoxes().isEmpty())
			return;
		
		boolean rightField = false;
		boolean leftField = false;
		System.out.println("XXX:d.getUseBoxes() " + d.getUseBoxes());
		System.out.println("XXX:d.getDefBoxes() " + d.getDefBoxes());

		for (ValueBox i: d.getUseBoxes()) {
			if (i.getValue() instanceof FieldRef) {
				rightField = true;
				System.out.println("XXXXXX: encontre rightField");
			}
		}
		
		for (ValueBox i: d.getDefBoxes()) {
			if (i.getValue() instanceof FieldRef) {
				leftField = true;
				System.out.println("XXXXXX: encontre leftField");
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
		
		System.out.println("Nodes: " + out_flow.nodes);
		System.out.println("Locals: " + out_flow.locals);
		System.out.println("Edges: " + out_flow.edges);
		out_flow.toDotFile();
		
		System.out.println("================");
	}
	
	protected void proc_new(FlowInfo in, Unit d, FlowInfo out) {
		// La instruccion es del tipo:
		//   p: x = new A;

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
		out.locals.put(left.toString(), vNameSet);
	}
	
	protected void proc_ref_eq_ref(FlowInfo in, List<ValueBox> left_l, List<ValueBox> right_l, FlowInfo out) {
		// Tenemos que hacer:
		//   L'(left) = L(right)
		//
		// Y como put() sobre escribe left si ya estaba esa clave, no hace falta borrarlo si estaba
		//out.nodes.add(left.toString());
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
		// Es una instruccion de la forma:
		//   x = y.f
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

		System.out.println("proc_ref_eq_field:" + x);
		System.out.println("proc_ref_eq_field:" + y);
		System.out.println("proc_ref_eq_field:" + f);
		
		// Borramos lo que sea que tenga x
		in.locals.put(x, new HashSet<String>());
		
		for (String a : in.locals.get(y)){
			for (Edge n : in.edges) {
				if (n.vSource.equals(a) && n.field.equals(f))
					out.locals.get(x).add(n.vTarget);
			}
		}

	}
	
	protected void proc_field_eq_ref(FlowInfo in, List<ValueBox> left_l, List<ValueBox> right_l, FlowInfo out) {
		// Es una instruccion de la forma:
		//   x.f = y
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

		System.out.println("proc_field_eq_ref x:" + x);
		System.out.println("proc_field_eq_ref y:" + y);
		System.out.println("proc_field_eq_ref f:" + f);

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
		return new FlowInfo();
	}

	@Override
	protected void merge(Object in1, Object in2, Object out) {
		//in1.union(in2, out);
		System.out.println("Llamada a merge");
		//return;
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
		System.out.println("Llamada a copy()");
		((FlowInfo)source).copy((FlowInfo)dest);
		//((FlowInfo)dest).copy((FlowInfo)source);
	}

}

class FlowInfo {
	FlowInfo() {
		nodes = new HashSet<String>();
		edges = new HashSet<Edge>();
		locals = new HashMap<String, Set<String> >();
		wrongs = new HashSet<String>();
	}
	public Set<String> nodes;
	public Set<Edge> edges;
	public Map<String, Set<String> > locals;
	public Set<String> wrongs;

	public void copy(FlowInfo dest) {
		dest.nodes = this.nodes;
		//XXX: ANDA???????
		dest.edges = this.edges;
		dest.locals = this.locals;
		dest.wrongs = this.wrongs;
	}
	public void toDotFile() {
		PrintWriter writer;
		try {
			writer = new PrintWriter("file.dot");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		writer.println("digraph {");
		
		for (Map.Entry<String, Set<String>> local : locals.entrySet()) {
			for (String node : local.getValue() )
			writer.println("\"" + local.getKey() + "\"" + "->" + "\"" + node + "\"");
		}
		
		for(Edge e : edges) {
			writer.println("\"" + e.vSource + "\"" + "->" + "\"" + e.vTarget + "\"" + "[label=" + "\"" + e.field + "\"" + "]");
		}
		
		writer.println("}");
		writer.close();
	}
}

class Edge {
	Edge(String source, String field, String target) {
		vSource = source;
		vTarget = target;
		this.field = field;
	}
	String vSource;
	String vTarget;
	String field;
	
	@Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
            // if deriving: appendSuper(super.hashCode()).
            append(vSource).
            append(vTarget).
            append(field).
            toHashCode();
    }
	
	@Override
    public boolean equals(Object obj) {
       if (!(obj instanceof Edge))
            return false;
        if (obj == this)
            return true;

        Edge edge = (Edge) obj;
        return new EqualsBuilder().
            // if deriving: appendSuper(super.equals(obj)).
            append(vTarget, edge.vTarget).
            append(vSource, edge.vSource).
            append(field, edge.field).
            isEquals();
    }

	@Override
	public String toString() {
		return "Edge:{" + vSource + "-->" + field  + "-->" + vTarget + "}" ;  
	}
}
