package model;

import java.util.Set;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.Multigraph;

/**
 * Just the parts of a graph we need.
 * 
 * @author joel
 * 
 */
public class HeatGraph {
    private final UndirectedGraph<VertexType, EdgeType> g = new Multigraph<VertexType, EdgeType>(EdgeType.class);

    public boolean addEdge(VertexType v0, VertexType v1, EdgeType e) {
        return g.addEdge(v0, v1, e);
    }

    public boolean addVertex(VertexType v1) {
        return g.addVertex(v1);
    }

    public Set<EdgeType> edgesOf(VertexType v) {
        return g.edgesOf(v);
    }

    public VertexType getEdgeSource(EdgeType e) {
        return g.getEdgeSource(e);
    }

    public VertexType getEdgeTarget(EdgeType e) {
        return g.getEdgeTarget(e);
    }

    public Set<VertexType> vertexSet() {
        return g.vertexSet();
    }
}
