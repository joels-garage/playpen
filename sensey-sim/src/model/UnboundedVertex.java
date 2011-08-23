package model;

/**
 * T is free, heat conducted from neighbors, no internal heat.
 * 
 * @author joel
 * 
 */
public class UnboundedVertex extends VertexType {
    public UnboundedVertex(Material material, double thickness, double area) {
        super(material, thickness, area);
    }

}