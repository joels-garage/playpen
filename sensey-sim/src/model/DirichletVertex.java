package model;

/**
 * vertex whose value is specified externally (perhaps as a function of time)
 * 
 * TODO: extract the "set temperature" stuff to a different subclass of VertextType
 */
public class DirichletVertex extends VertexType {
    private final TemperatureSource temperatureSource;

    public DirichletVertex(String name, Material material, double thickness, double area, TemperatureSource temperatureSource) {
        super(name, material, thickness, area);
        this.temperatureSource = temperatureSource;
    }

    public double getTemperature() {
        return temperatureSource.temperature();
    }

    public void setTemperature(double temperature) {
        throw new UnsupportedOperationException();
    }

    public double getNextTemperature() {
        return temperatureSource.temperature();
    }

    public void setNextTemperature(double temperature) {
        throw new UnsupportedOperationException();
    }
}