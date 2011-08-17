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
        public static final Material IRON = new Material(80, 7870, 0.450);
        public static final Material STYROFOAM = new Material(0.033, 75, 1.3);
        public static final Material DOUGLAS_FIR = new Material(0.15, 580, 1.7);

        /** thermal conductivity (SI units: W/(m·K)) */
        public final double k;
        /** density (kg/m³) */
        public final double rho;
        /** specific heat capacity (J/(kg·K)) */
        public final double cp;

        public Material(double k, double rho, double cp) {
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
            return "Material [k=" + k + ", rho=" + rho + ", cp=" + cp + "]";
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

        @Override
        public String toString() {
            return "VertexType [material=" + material + ", thickness=" + thickness + ", temperature=" + temperature
                    + ", nextTemperature=" + nextTemperature + "]";
        }

    }

    public static class UnboundedVertex extends VertexType {
        public UnboundedVertex(Material material, double thickness) {
            super(material, thickness);
        }
    }

    /** vertex whose value is specified externally (perhaps as a function of time) */
    public static class DirichletVertex extends VertexType {
        // for now, it's a constant. TODO: specify a function
        private final double constantTemperature = 1;

        public DirichletVertex(Material material, double thickness) {
            super(material, thickness);
        }

        public double getTemperature() {
            return constantTemperature;
        }

        public void setTemperature(double temperature) {
            throw new UnsupportedOperationException();
        }

        public double getNextTemperature() {
            return constantTemperature;
        }

        public void setNextTemperature(double temperature) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "DirichletVertex [constantTemperature=" + constantTemperature + ", material=" + material
                    + ", thickness=" + thickness + "]";
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
        // make a linear graph
        int length = 10;
        UndirectedGraph<VertexType, EdgeType> g = new Multigraph<VertexType, EdgeType>(EdgeType.class);
        VertexType v0 = new DirichletVertex(Material.IRON, 0.01);
        g.addVertex(v0);
        for (int i = 1; i < length - 1; ++i) {
            VertexType v1 = new UnboundedVertex(Material.IRON, 0.01);
            v1.setTemperature(0);
            g.addVertex(v1);
            g.addEdge(v0, v1);
            v0 = v1;
        }
        VertexType v1 = new DirichletVertex(Material.IRON, 0.01);
        g.addVertex(v1);
        g.addEdge(v0, v1);

        // traversal order is unimportant, so don't bother with the jgrapht iterators.
        double timestepSec = 0.2;
        for (int step = 0; step < 100; ++step) {
            // what's the temp for the next iteration?
            for (VertexType v : g.vertexSet()) {
                logger.info("step: " + step + " v: " + v);
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
                        double distance = (v.thickness + other.thickness) / 2;
                        q += (other.getTemperature() - v.getTemperature()) / distance;
                    }
                    v.setNextTemperature(v.getTemperature() + timestepSec * v.material.alpha() * q);
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
