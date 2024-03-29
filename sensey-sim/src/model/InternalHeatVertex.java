package model;

/**
 * free T, heat from neighbors and from internal heat. TODO: add this to UnboundedVertex; it's just a term that can be
 * zero.
 */
public class InternalHeatVertex extends UnboundedVertex {
    private final InternalHeat internalHeat;

    public InternalHeatVertex(String name, Material material, double thickness, double area, InternalHeat internalHeat) {
        super(name, material, thickness, area);
        this.internalHeat = internalHeat;
    }

    public InternalHeat getInternalHeat() {
        return internalHeat;
    }
}