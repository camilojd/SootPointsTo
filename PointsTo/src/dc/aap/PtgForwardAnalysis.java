package dc.aap;

import java.util.Iterator;

import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class PtgForwardAnalysis extends ForwardFlowAnalysis<Unit, FlowSet<Value>>{

	public PtgForwardAnalysis(DirectedGraph<Unit> graph) {
		super(graph);
		doAnalysis();
	}

	@Override
	protected void flowThrough(FlowSet<Value> in, Unit d, FlowSet<Value> out) {
		
		for (Iterator<ValueBox> it = d.getDefBoxes().iterator(); it.hasNext();) {
			ValueBox box = it.next();
		}
		System.out.println("def: " + d.getDefBoxes());
		System.out.println("use: " + d.getUseBoxes());
		System.out.println("================");
	}

	@Override
	protected FlowSet<Value> newInitialFlow() {
		return new ArraySparseSet<Value>();
	}

	@Override
	protected void merge(FlowSet<Value> in1, FlowSet<Value> in2,
			FlowSet<Value> out) {
		in1.union(in2, out);	
	}

	@Override
	protected void copy(FlowSet<Value> source, FlowSet<Value> dest) {
		source.copy(dest);
	}

}