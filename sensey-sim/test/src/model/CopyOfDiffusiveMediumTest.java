package model;

import java.util.Set;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math.ode.IntegratorException;
import org.apache.commons.math.ode.sampling.StepHandler;
import org.apache.commons.math.ode.sampling.StepInterpolator;
import org.apache.log4j.Logger;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.Multigraph;
import org.junit.Assert;
import org.junit.Test;

import util.Util;

/*
 * The explicit finite difference.
 * 
 * TODO: replace the ODE solver thing with a direct method; there's no special
 * value to it, i can just use the finite difference method.
 * 
 * node types: fixed temp, floating, heat source (with no temp), is that a node type
 * or an aspect of a node?
 * edge types: conductive
 * 
 * 
 * TODO: control strategies:
 * 
 * A. detect occupancy, and allow giant swings in temperature (i.e. 10 degrees).  look at the response to surprise arrivals.
 * 
 * B. don't detect anything, keep within a (minimally noticeable) 2 degree band all the time.
 * 
 * C. allow noticeable but not extreme changes, e.g. precool 5 degrees in the morning.
 * 
 * D. track inside temp to outside, i.e. if it's 90, then inside is 75, not 72.  something like that.
 * 
 * 
 * TODO: freezers, like at 7-11.
 */

public class CopyOfDiffusiveMediumTest {
    private static final Logger logger = Logger.getLogger(CopyOfDiffusiveMediumTest.class);

    /** try the 1d diffusion eq */
    public class MyEq implements FirstOrderDifferentialEquations {
        private final int dimension;
        private final double[] alpha;

        public MyEq(int dimension, double[] alpha) {
            this.dimension = dimension;
            this.alpha = alpha;
        }

        public void computeDerivatives(double t, double[] y, double[] dot) {
            if (y.length != dimension)
                throw new RuntimeException("bug");
            if (dot.length != dimension)
                throw new RuntimeException("bug");
            for (int i = 0; i < dot.length; i++) {
                // try to enforce a boundary condition?
                if ((i == 0) || (i == dimension - 1)) {
                    // dirichlet condition, zero derivative, i.e. constant value.
                    dot[i] = 0;
                } else {
                    // heat equation, dot = A d2y/dx2.
                    // y is not on the edge here.
                    // TODO: in a network, it's all the neighbors
                    // (in 1d, it's just two.)
                    double left = y[i - 1];
                    // logger.info("left: " + i + " " + left);
                    double right = y[i + 1];
                    // logger.info("right: " + i + " " + right);
                    double center = y[i];
                    // logger.info("center: " + i + " " + center);
                    double leftHeat = center - left;
                    // logger.info("leftHeat: " + i + " " + leftHeat);
                    double rightHeat = right - center;
                    // logger.info("rightHeat: " + i + " " + rightHeat);
                    double netHeat = rightHeat - leftHeat;
                    // logger.info("netHeat: " + i + " " + netHeat);
                    // note, this is wrong; it should involve the alphas of the neighbors too.
                    // TODO: add the "q" term for internal (i.e. solar absorbed) heat.
                    // TODO: add some notion of units
                    // TODO: also infiltration, which is a constant exchange of fluid, thus
                    // heat flow proportional to delta-T, just like conduction.
                    dot[i] = alpha[i] * netHeat;
                }
            }
        }

        public int getDimension() {
            return dimension;
        }
    }

    /** just logs and has a test in it */
    public class MyHandler implements StepHandler {
        private final double step;

        public MyHandler(double step) {
            this.step = step;
        }

        public void handleStep(StepInterpolator interpolator, boolean isLast) {
            // try {
            // logger.info("derivatives:" + Util.print(interpolator.getInterpolatedDerivatives()));
            // } catch (DerivativeException e) {
            // e.printStackTrace();
            // }
            try {
                logger.info("state: " + String.format("%5.2f", interpolator.getInterpolatedTime()) + " : "
                        + Util.print(interpolator.getInterpolatedState()));
            } catch (DerivativeException e) {
                e.printStackTrace();
            }
            // make sure the step is what we want.
            if (!isLast) {
                Assert.assertEquals(step, interpolator.getCurrentTime() - interpolator.getPreviousTime(), 1.0e-12);
            }
        }

        public boolean requiresDenseOutput() {
            return false;
        }

        public void reset() {
        }
    }

    @Test
    public void testStepSize() throws DerivativeException, IntegratorException {
        final double step = 0.3;

        int dimension = 15;
        double[] alpha = new double[dimension];
        for (int i = 0; i < dimension; ++i) {
            alpha[i] = 1.5;
        }
        FirstOrderDifferentialEquations eq = new MyEq(dimension, alpha);
        double t0 = 0;
        // initial values are zero
        double[] y0 = new double[dimension];
        // except the boundaries
        y0[0] = 1;
        y0[dimension - 1] = 2;
        double t1 = 20;
        double[] y1 = new double[dimension];
        // integ.integrate(eq, t0, y0, t1, y1);
        for (double t = t0; t < t1; ++t) {

        }
        // and then do it some more, with a new boundary value
        // TODO: do i need to do it this way?
        y1[dimension - 1] = -1;
        double t2 = 40;
        double[] y2 = new double[dimension];
        // integ.integrate(eq, t1, y1, t2, y2);
    }

    public static class Material {
        /** these are from wikipedia, engineering toolbox */
        public static final Material IRON = new Material("Iron", 80, 7870, 0.450);
        public static final Material STYROFOAM = new Material("Styrofoam", 0.033, 75, 1.3);
        public static final Material DOUGLAS_FIR = new Material("Douglas Fir", 0.15, 580, 1.7);
        /** effective K is like 5 w/m2k, but the k here is w/mk */
        public static final Material AIR_BOUNDARY_LAYER = new Material("Air Boundary Layer", 0.1, 1.225, 1.006);
        /** very high conductivity (like diamond :-); assumes infinitely well mixed air */
        public static final Material AIR_BULK_MIXED = new Material("Air Bulk Mixed", 1000, 1.225, 1.006);

        public final String name;
        /** thermal conductivity (SI units: W/(m·K)) */
        public final double k;
        /** density (kg/m³) */
        public final double rho;
        /** specific heat capacity (J/(kg·K)) */
        public final double cp;

        public Material(String name, double k, double rho, double cp) {
            this.name = name;
            this.k = k;
            this.rho = rho;
            this.cp = cp;
        }

        /** thermal diffusivity */
        public double alpha() {
            return k / (rho * cp);
        }

        @Override
        public String toString() {
            return name;
            // return "Material [k=" + k + ", rho=" + rho + ", cp=" + cp + "]";
        }
    }

    // a vertex is a 1-dimensional element.
    public static abstract class VertexType {
        public final Material material;
        /** (m) */
        public final double thickness;
        /** for the current time step (K) */
        private double temperature;
        /** for the next time step (K) */
        private double nextTemperature;

        public VertexType(Material material, double thickness) {
            this.material = material;
            this.thickness = thickness;
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
    }

    /**
     * T is free, heat conducted from neighbors, no internal heat.
     * 
     * @author joel
     * 
     */
    public static class UnboundedVertex extends VertexType {
        public UnboundedVertex(Material material, double thickness) {
            super(material, thickness);
        }

        @Override
        public String toString() {
            return "UnboundedVertex [material=" + material + ", thickness=" + thickness + ", temperature="
                    + getTemperature() + "]";
        }
    }

    /**
     * time-dependent internal heat for a single node.
     * 
     * TODO: how to tell this thing what time it is?
     * 
     * TODO: attach this to an equipment model, i.e. a thermostat, a capacity.
     */
    public static class InternalHeat {
        double heatWatts() {
            return 1;
        }
    }

    /** time-dependent temperature for dirichlet nodes */
    public static class TemperatureSource {
        double temperature() {
            return 0;
        }
    }

    /**
     * free T, heat from neighbors and from internal heat. TODO: add this to UnboundedVertex; it's just a term that can
     * be zero.
     */
    public static class InternalHeatVertex extends UnboundedVertex {
        private final InternalHeat internalHeat;

        public InternalHeatVertex(Material material, double thickness, InternalHeat internalHeat) {
            super(material, thickness);
            this.internalHeat = internalHeat;
        }

        public InternalHeat getInternalHeat() {
            return internalHeat;
        }

        public String toString() {
            return "InternalHeatVertex [temperature=" + getTemperature() + ", material=" + material + ", thickness="
                    + thickness + "]";
        }
    }

    /**
     * vertex whose value is specified externally (perhaps as a function of time)
     * 
     * TODO: extract the "set temperature" stuff to a different subclass of VertextType
     */
    public static class DirichletVertex extends VertexType {
        private final TemperatureSource temperatureSource;

        public DirichletVertex(Material material, double thickness, TemperatureSource temperatureSource) {
            super(material, thickness);
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

        @Override
        public String toString() {
            return "DirichletVertex [temperature=" + getTemperature() + ", material=" + material + ", thickness="
                    + thickness + "]";
        }
    }

    /**
     * edges serve no purpose other than to describe adjacency. TODO: think about ditching jgrapht, just make an
     * adjacency matrix
     */
    public static class EdgeType {

        @Override
        public String toString() {
            return "EdgeType";
        }

    }

    @Test
    public void directFiniteDifferenceGraph() {
        UndirectedGraph<VertexType, EdgeType> g = new Multigraph<VertexType, EdgeType>(EdgeType.class);
        double thicknessMeters = 0.01;
        VertexType v0 = new DirichletVertex(Material.DOUGLAS_FIR, thicknessMeters, new TemperatureSource() {
            @Override
            double temperature() {
                return 0.0;
            }

        });
        g.addVertex(v0);
        for (int i = 0; i < 2; ++i) {
            VertexType v1 = new UnboundedVertex(Material.DOUGLAS_FIR, thicknessMeters);
            v1.setTemperature(0);
            g.addVertex(v1);
            g.addEdge(v0, v1);
            v0 = v1;
        }
        thicknessMeters = 0.01;
        for (int i = 0; i < 5; ++i) {
            VertexType v1 = new UnboundedVertex(Material.STYROFOAM, thicknessMeters);
            v1.setTemperature(0);
            g.addVertex(v1);
            g.addEdge(v0, v1);
            v0 = v1;
        }
        thicknessMeters = 0.01;
        for (int i = 0; i < 2; ++i) {
            VertexType v1 = new UnboundedVertex(Material.DOUGLAS_FIR, thicknessMeters);
            v1.setTemperature(0);
            g.addVertex(v1);
            g.addEdge(v0, v1);
            v0 = v1;
        }
        VertexType v1 = new InternalHeatVertex(Material.DOUGLAS_FIR, thicknessMeters, new InternalHeat() {

            @Override
            double heatWatts() {
                return 0.05;
            }

        });
        g.addVertex(v1);
        g.addEdge(v0, v1);

        // traversal order is unimportant, so don't bother with the jgrapht iterators.
        // max time step might depend on alpha?
        // TODO: detect nonconvergence
        double timestepSec = 3000;
        for (int step = 0; step < 3000; ++step) {
            logger.info("step: " + step);
            // what's the temp for the next iteration?
            for (VertexType v : g.vertexSet()) {
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
                            logger.info("skip it, it's a loop.");
                            continue;
                        }
                        double effectiveK = v.thickness
                                / ((other.thickness / other.material.k) + (v.thickness / v.material.k));
                        double deltaT = other.getTemperature() - v.getTemperature();
                        q += (deltaT) * effectiveK;
                    }
                    if (v instanceof InternalHeatVertex) {
                        q += ((InternalHeatVertex) v).getInternalHeat().heatWatts();
                    }
                    v.setNextTemperature(v.getTemperature() + timestepSec * q / (v.material.cp * v.material.rho));
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
