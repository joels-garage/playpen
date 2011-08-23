package model;

// a vertex is a 1-dimensional element.
public abstract class VertexType {
    public final Material material;
    /**
     * used to calculate the gradient across the node.
     * 
     * for very high conductivity nodes, it's not relevant.
     * 
     * TODO: make a different type for zero-gradient nodes.
     * 
     * meters
     */
    private final double thickness;
    /** for the current time step (K) */
    private double temperature;
    /** for the next time step (K) */
    private double nextTemperature;
    /**
     * used for volume calculation only, for heat capacity. m^2
     */
    private final double area;

    public VertexType(Material material, double thickness, double area) {
        this.material = material;
        this.thickness = thickness;
        this.area = area;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getNextTemperature() {
        return nextTemperature;
    }

    public void setNextTemperature(double nextTemperature) {
        this.nextTemperature = nextTemperature;
    }

    /** cubic meters */
    public double getVolume() {
        return area * thickness;
    }
    
    public double getHalfThickness() {
        return thickness / 2;
    }

    /** j/m3k */
    public double getVolumetricHeatCapacity() {
        return material.getVolumetricHeatCapacity();
    }

    /** capacity of the entire node j/k */
    public double getNodeHeatCapacity() {
        return getVolumetricHeatCapacity() * getVolume();
    }

    /** W/K */
    public double getConductance() {
        return material.k * area / thickness;

    }

    @Override
    public String toString() {
        return String.format("class: %20s material: %20s thickness: %4.3f temperature: %12.6f", getClass()
                .getSimpleName(), material, thickness, getTemperature());
    }
}