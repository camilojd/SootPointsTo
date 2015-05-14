package dc.aap;

import soot.*;
import java.util.Iterator;
import soot.util.*;
import java.util.*;
import soot.jimple.*;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class PtgForwardAnalysis extends ForwardFlowAnalysis{

	public PtgForwardAnalysis(DirectedGraph<Unit> graph) {
		super(graph);
		doAnalysis();
	}

	@Override
	protected void flowThrough(Object in, Object u, Object out) {
		Unit d = (Unit)u;
		
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
		System.out.println("================");
	}

	@Override
	protected FlowSet<Value> newInitialFlow() {
		return new ArraySparseSet<Value>();
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