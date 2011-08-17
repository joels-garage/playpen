package model;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.IntegratorException;
import org.apache.commons.math.ode.nonstiff.AdamsBashforthIntegrator;
import org.apache.commons.math.ode.nonstiff.EulerIntegrator;
import org.apache.commons.math.ode.sampling.StepHandler;
import org.apache.commons.math.ode.sampling.StepInterpolator;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import util.Util;

/*
 * TODO: replace the ODE solver thing with a direct method; there's no special
 * value to it, i can just use the finite difference method.
 * 
 * node types: fixed temp, floating, heat source (with no temp), is that a node type
 * or an aspect of a node?
 * edge types: conductive
 */

public class DiffusiveMediumTest {
    private static final Logger logger = Logger.getLogger(DiffusiveMediumTest.class);

    /** try the 1d diffusion eq */
    public class MyEq implements FirstOrderDifferentialEquations {
        private final int dimension;
        private final double[] alpha;

        public MyEq(int dimension, double[] alpha) {
            this.dimension = dimension;
            this.alpha = alpha;
        }

        public void computeDerivatives(double t, double[] y, double[] dot) {
            logger.info("compute derivative at t: " + t);
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
            logger.info("derivatives:" + Util.print(dot));
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
//            try {
//                logger.info("derivatives:" + Util.print(interpolator.getInterpolatedDerivatives()));
//            } catch (DerivativeException e) {
//                e.printStackTrace();
//            }
            try {
                logger.info("state: " + String.format("%5.2f", interpolator.getInterpolatedTime()) + " : "
                        + Util.print(interpolator.getInterpolatedState()));
            } catch (DerivativeException e) {
                e.printStackTrace();
            }
            // make sure the step is what we want.
            // (not for the adaptive one)
            if (!isLast) {
                // Assert.assertEquals(step, interpolator.getCurrentTime() - interpolator.getPreviousTime(), 1.0e-12);
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
        // works with step 0.3
        int nSteps = 40;
        double minStep = 0.0001;
        double maxStep = 2;
        double scalAbsoluteTolerance = 0.1;
        double scalRelativeTolerance = 0.1;
        FirstOrderIntegrator integ = new AdamsBashforthIntegrator(nSteps, minStep, maxStep, scalAbsoluteTolerance,
                scalRelativeTolerance);
        // FirstOrderIntegrator integ = new EulerIntegrator(step);
        // FirstOrderIntegrator integ = new MidpointIntegrator(step);
        // works with step 0.4
        // FirstOrderIntegrator integ = new ClassicalRungeKuttaIntegrator(step);
        StepHandler handler = new MyHandler(step);

        integ.addStepHandler(handler);

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
        integ.integrate(eq, t0, y0, t1, y1);
        // and then do it some more, with a new boundary value
        // TODO: do i need to do it this way?
        y1[dimension - 1] = -1;
        double t2 = 40;
        double[] y2 = new double[dimension];
        integ.integrate(eq, t1, y1, t2, y2);
    }
}
