package dc.aap;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FlowInfo {
	FlowInfo() {
		nodes = new HashSet<String>();
		edges = new HashSet<Edge>();
		r_edges = new HashSet<Edge>();
		locals = new HashMap<String, Set<String> >();
		wrongs = new HashSet<String>();
	}
	public Set<String> nodes;
	public Set<Edge> edges;
	public Set<Edge> r_edges;
	public Map<String, Set<String> > locals;
	public Set<String> wrongs;

	public void merge(FlowInfo dest){
		dest.nodes.addAll(this.nodes);
		dest.edges.addAll(this.edges);
		dest.r_edges.addAll(this.r_edges);
		dest.locals.putAll(this.locals);
		dest.wrongs.addAll(this.wrongs);
	}
	
	public void mergeMethod(FlowInfo dest){
		String id = IdGenerator.GenerateId();
		for (String n : this.nodes) {
			dest.nodes.add(id + "_" + n);
		}
		for (Edge e : this.edges) {
			Edge ne = new Edge(id + "_" + e.vSource, e.field, id + "_" + e.vTarget);
			dest.edges.add(ne);
		}
		for (Edge e : this.r_edges) {
			Edge ne = new Edge(id + "_" + e.vSource, e.field, id + "_" + e.vTarget);
			dest.r_edges.add(ne);
		}
		for (Map.Entry<String, Set<String>> local : this.locals.entrySet()) {
			Set<String> nodos = new HashSet<String>();
			for (String n : local.getValue()) {
				nodos.add(id + "_" + n);
			}
			dest.locals.put(id + "_" + local.getKey(), nodos);
		}
		dest.wrongs.addAll(this.wrongs);
	}
	
	public void copy(FlowInfo dest) {
		dest.nodes = this.nodes;
		//XXX: ANDA???????
		dest.edges = this.edges;
		dest.r_edges = this.r_edges;
		dest.locals = this.locals;
		dest.wrongs = this.wrongs;
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
		for(Edge e : r_edges) {
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
		
		for(Edge e : r_edges) {
			writer.println("\"" + e.vSource + "\"" + "->" + "\"" + e.vTarget + "\"" + "[label=" + "\"" + e.field + "\"" + "]");
		}
		
		writer.println("}");
		writer.close();
	}
}