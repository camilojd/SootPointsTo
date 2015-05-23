package dc.aap;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PointsToGraph {
	public List<String> p_nodes;
	public Set<String> nodes;
	public Set<Edge> edges;
	public Set<Edge> l_edges;
	public Map<String, Set<String> > locals;
	public Set<String> wrongs;

	PointsToGraph() {
		nodes = new HashSet<String>();
		p_nodes = new ArrayList<String>();
		edges = new HashSet<Edge>();
		l_edges = new HashSet<Edge>();
		locals = new HashMap<String, Set<String> >();
		wrongs = new HashSet<String>();
	}

	public void merge(PointsToGraph dest){
		dest.nodes.addAll(this.nodes);
		dest.p_nodes.addAll(this.nodes);
		dest.edges.addAll(this.edges);
		dest.l_edges.addAll(this.l_edges);
		dest.locals.putAll(this.locals);
		dest.wrongs.addAll(this.wrongs);
	}
	
	public void copy(PointsToGraph dest) {
		dest.p_nodes = this.p_nodes;
		dest.nodes = this.nodes;
		dest.edges = this.edges;
		dest.l_edges = this.l_edges;
		dest.locals = this.locals;
		dest.wrongs = this.wrongs;
	}
	
	public PointsToGraph getPtgWithoutLocalsWithPrefix(int prefix) {
		PointsToGraph dest = new PointsToGraph();
		for (String n : this.p_nodes) {
			dest.p_nodes.add(prefix + "_" + n);
		}
		for (String n : this.nodes) {
			dest.nodes.add(prefix + "_" + n);
		}
		for (Edge e : this.edges) {
			Edge ne = new Edge(prefix + "_" + e.vSource, e.field, prefix + "_" + e.vTarget);
			dest.edges.add(ne);
		}
		for (Edge e : this.l_edges) {
			Edge ne = new Edge(prefix + "_" + e.vSource, e.field, prefix + "_" + e.vTarget);
			dest.l_edges.add(ne);
		}
		dest.wrongs.addAll(this.wrongs);
		return dest;
	}
	
	public void replaceNode(String formalParamNode, String localParamNode) {
		Set<Edge> edgesToRemove = new HashSet<Edge>();
		Set<Edge> edgesToAdd = new HashSet<Edge>();;
		for (Edge e : edges) {
			Edge replace = null;
			if (e.vSource.equals(formalParamNode)) {
				replace = new Edge(localParamNode, e.field, e.vTarget);
			}
			if (e.vTarget.equals(formalParamNode)) {
				replace = new Edge(e.vSource, e.field, localParamNode);
			}
			if (replace != null) {
				edgesToRemove.add(e);
				edgesToAdd.add(replace);	
			}
		}
		edges.removeAll(edgesToRemove);
		edges.addAll(edgesToAdd);
		
		Set<Edge> r_edgesToRemove = new HashSet<Edge>();
		Set<Edge> r_edgesToAdd = new HashSet<Edge>();;
		for (Edge e : l_edges) {
			Edge replace = null;
			if (e.vSource.equals(formalParamNode)) {
				replace = new Edge(localParamNode, e.field, e.vTarget);
			}
			if (e.vTarget.equals(formalParamNode)) {
				replace = new Edge(e.vSource, e.field, localParamNode);
			}
			r_edgesToRemove.add(e);
			r_edgesToAdd.add(replace);
		}
		l_edges.removeAll(r_edgesToRemove);
		l_edges.addAll(r_edgesToAdd);
		
		for(Map.Entry<String, Set<String>> l : locals.entrySet()) {
			if (l.getValue().contains(formalParamNode)){
				l.getValue().remove(formalParamNode);
				l.getValue().add(localParamNode);
			}
		}
	}

	public void mergeNodes() {
		Set<Edge> edgesToRemove = new HashSet<Edge>();
		for (Edge edgeToRemove : l_edges) {
			String n = edgeToRemove.vSource;
			String nodeToRemove = edgeToRemove.vTarget;
			for (Edge realEdge : edges) {
				if (realEdge.vSource.equals(n) && realEdge.field.equals(edgeToRemove.field)) {
					for (Map.Entry<String, Set<String>> l : locals.entrySet()) {
						if (l.getValue().contains(nodeToRemove)) {
							l.getValue().remove(nodeToRemove);
							edgesToRemove.add(edgeToRemove);
							l.getValue().add(realEdge.vTarget);
						}
					}
				}
			}
		}
		l_edges.removeAll(edgesToRemove);
	}

	@Override
	public String toString() {
		String s = "LOCALES: \n";
		for (Map.Entry<String, Set<String>> local : locals.entrySet()) {
			for (String node : local.getValue() ) {
				s = s.concat("\t" + local.getKey() + " --> " + node + "\n");
			}
		}
		
		s = s.concat("EJES: \n");		
		for(Edge e : edges) {
			s = s.concat("\t(" + e.vSource + ", " + e.field + ", " + e.vTarget + ")\n");
		}
		
		s = s.concat("R_EJES: \n");
		for(Edge e : l_edges) {
			s = s.concat("\t(" + e.vSource + ", " + e.field + ", " + e.vTarget + ")\n");
		}
		return s;
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
		
		for(Edge e : l_edges) {
			writer.println("\"" + e.vSource + "\"" + "->" + "\"" + e.vTarget + "\"" + "[label=" + "\"" + e.field + "\"" + "]");
		}
		
		writer.println("}");
		writer.close();
	}


}