package framework;

import java.util.Set;

import model.EdgeType;
import model.HeatGraph;
import model.InternalHeatVertex;
import model.UnboundedVertex;
import model.VertexType;

import org.apache.log4j.Logger;

/**
 * TODO: control strategies:
 * 
 * A. detect occupancy, and allow giant swings in temperature (i.e. 10 degrees). look at the response to surprise
 * arrivals.
 * 
 * B. don't detect anything, keep within a (minimally noticeable) 2 degree band all the time.
 * 
 * C. allow noticeable but not extreme changes, e.g. precool 5 degrees in the morning.
 * 
 * D. track inside temp to outside, i.e. if it's 90, then inside is 75, not 72. something like that.
 * 
 * 
 * TODO: freezers, like at 7-11.
 * 
 * @author joel
 */

public class ForwardFiniteDifferenceSimulator {
    private static final Logger logger = Logger.getLogger(ForwardFiniteDifferenceSimulator.class);

    public void doit(HeatGraph g, double timestepSec, int steps) {
        doit(g, timestepSec, steps, true);
    }

    public void doit(HeatGraph g, double timestepSec, int steps, boolean withLog) {

        // traversal order is unimportant, so don't bother with the jgrapht iterators.
        // max time step might depend on alpha?
        // TODO: detect nonconvergence

        for (int step = 0; step < steps; ++step) {
            boolean log = (step % (Math.round(steps / 100) + 1) == 0) && withLog;
            if (log)
                logger.info("step: " + step);
            // what's the temp for the next iteration?
            for (VertexType v : g.vertexSet()) {
                if (log)
                    logger.info("v: " + v);
                if (v instanceof UnboundedVertex) {
                    Set<EdgeType> edges = g.edgesOf(v);
                    double q = 0;
                    for (EdgeType e : edges) {
                        VertexType source = g.getEdgeSource(e);
                        VertexType target = g.getEdgeTarget(e);
                        VertexType other = null;
                        if (source != v) {
                            other = source;
                        } else if (target != v) {
                            other = target;
                        } else {
                            logger.trace("skip it, it's a loop.");
                            continue;
                        }
                        if (log)
                            logger.trace("this: " + v.toString() + " other: " + other.toString());

                        // TODO: move these calcs to the graph, since they never change.
                        double myHalfThickness = v.getHalfThickness();
                        double otherHalfThickness = other.getHalfThickness();
                        // w/mK
                        double effectiveK = myHalfThickness
                                / ((otherHalfThickness / other.material.k) + (myHalfThickness / v.material.k));
                        double deltaT = other.getTemperature() - v.getTemperature();
                        // q in watts
                        q += deltaT * effectiveK * e.area / myHalfThickness;
                        if (log)
                            logger.trace("deltaT: " + deltaT + " q: " + q);
                    }
                    if (v instanceof InternalHeatVertex) {
                        q += ((InternalHeatVertex) v).getInternalHeat().heatWatts();
                    }

                    double totalDeltaT = timestepSec * q / v.getNodeHeatCapacity();
                    if (log)
                        logger.trace("totalDeltaT: " + totalDeltaT + " totalQ: " + q);

                    v.setNextTemperature(v.getTemperature() + totalDeltaT);
                }
            }
            // now set the next
            for (VertexType v : g.vertexSet()) {
                if (v instanceof UnboundedVertex)
                    v.setTemperature(v.getNextTemperature());
            }
        }
    }

}
