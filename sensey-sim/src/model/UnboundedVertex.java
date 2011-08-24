package model;

/**
 * T is free, heat conducted from neighbors, no internal heat.
 * 
 * @author joel
 * 
 */
public class UnboundedVertex extends VertexType {
    public UnboundedVertex(String name, Material material, double thickness, double area) {
        super(name, material, thickness, area);
    }

}