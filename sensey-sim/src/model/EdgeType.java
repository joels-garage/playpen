package model;

/**
 * 
 */
public class EdgeType {
    /**
     * conduction area between vertices, m^2
     */
    public final double area;

    public EdgeType(double area) {
        this.area = area;
    }

    @Override
    public String toString() {
        return "EdgeType";
    }

}