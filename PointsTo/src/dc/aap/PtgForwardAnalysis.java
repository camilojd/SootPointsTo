package dc.aap;

import java.util.Dictionary;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import soot.*;

import org.apache.commons.lang3.builder.*;

//import org.jgrapht.graph.DefaultEdge;

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
		
		FlowInfo flowInfo = (FlowInfo)in;
		flowInfo.nodes.add("pepe");
		flowInfo.nodes.add("pepe2");
		flowInfo.edges.add(new Edge("pepe", "f", "pepe2"));
		System.out.println(flowInfo.edges);
		
		if (!d.getUseBoxes().isEmpty() && !d.getDefBoxes().isEmpty()) {
			Value right = d.getUseBoxes().get(0).getValue();
			Value left = d.getDefBoxes().get(0).getValue();
		
			Stmt stmt = (Stmt)d;
			if (stmt instanceof AssignStmt) {
				if (right instanceof AnyNewExpr) {
					System.out.println("Es un new!");
			    } else if ((left instanceof FieldRef) && (right instanceof Ref || right instanceof Local)) {
					System.out.println("Es un assign x.f = y");
				} else if ((left instanceof FieldRef) && (right instanceof Constant)) {
					System.out.println("Es un assign x.f = 5");
				} else if ((left instanceof Ref || left instanceof Local) && (right instanceof FieldRef)) {
					System.out.println("Es un assign x = y.f");
				} else if ((left instanceof Ref || left instanceof Local) && (right instanceof Ref || right instanceof Local)) {
					System.out.println("Es un assign x = y");
				} else if ((left instanceof Ref || left instanceof Local) && (right instanceof Constant)) {
					System.out.println("Es un assign x = cte");
				} else if ((left instanceof Ref || left instanceof Local) && (right instanceof InvokeExpr)) {
					System.out.println("Es un assign x = m()");
				}
			}// else if (stmt instanceof InvokeStmt)
			//	System.out.println("Es un call()");
			// Esto, por algun motivo, no anda
		}
		
		flowInfo.edges.remove((new Edge("pepe", "f", "pepe2")));
		System.out.println(flowInfo.edges);
		
		out = flowInfo;
		
		System.out.println("================");
	}

	@Override
	protected Object newInitialFlow() {
		return new FlowInfo();
	}

	@Override
	protected void merge(Object in1, Object in2, Object out) {
		//in1.union(in2, out);
	}

	@Override
	protected void copy(Object source, Object dest) {
		//source.copy(dest);
	}

}

class FlowInfo {
	FlowInfo() {
		nodes = new HashSet<String>();
		edges = new HashSet<Edge>();
		locals = new HashMap<String, String>();
		wrongs = new HashSet<String>();
	}
	public Set<String> nodes;
	public Set<Edge> edges;
	public Map<String, String> locals;
	public Set<String> wrongs;
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
		return vSource + " " + field  + " " + vTarget ;  
	}
}
