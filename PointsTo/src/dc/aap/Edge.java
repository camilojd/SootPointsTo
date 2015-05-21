package dc.aap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class Edge {
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
        return new HashCodeBuilder(17, 31).
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
            append(vTarget, edge.vTarget).
            append(vSource, edge.vSource).
            append(field, edge.field).
            isEquals();
    }

	@Override
	public String toString() {
		return "(" + vSource + "," + field  + "," + vTarget + ")" ;  
	}
}