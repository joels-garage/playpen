package model;

/**
 * time-dependent internal heat for a single node.
 * 
 * TODO: how to tell this thing what time it is?
 * 
 * TODO: attach this to an equipment model, i.e. a thermostat, a capacity.
 */
public interface InternalHeat {
    double heatWatts();
}