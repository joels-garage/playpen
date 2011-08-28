package model;

/** time-dependent temperature for dirichlet nodes */
public interface TemperatureSource {
    double temperature();
}