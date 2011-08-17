package model;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.IntegratorException;
import org.apache.commons.math.ode.nonstiff.MidpointIntegrator;
import org.apache.commons.math.ode.sampling.StepHandler;
import org.apache.commons.math.ode.sampling.StepInterpolator;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import util.Util;

public class DiffusiveMediumTest {
    private static final Logger logger = Logger.getLogger(DiffusiveMediumTest.class);

    @Test
    public void foo() {
        DiffusiveMedium m = new DiffusiveMedium();
    }

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
                if ((i == 0) || (i == dimension - 1))
                    // dirichlet condition, zero derivative, i.e. constant value.
                    dot[i] = 0;
                else
                    // heat equation, dot = A d2y/dx2.
                    dot[i] = -1 * y[i];
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
            try {
                logger.info("derivatives:" + Util.print(interpolator.getInterpolatedDerivatives()));
            } catch (DerivativeException e) {
                e.printStackTrace();
            }
            logger.info("time: " + interpolator.getInterpolatedTime());
            try {
                logger.info("state: " + Util.print(interpolator.getInterpolatedState()));
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
        final double step = 1.23456;
        // FirstOrderIntegrator integ = new EulerIntegrator(step);
        FirstOrderIntegrator integ = new MidpointIntegrator(step);
        StepHandler handler = new MyHandler(step);

        integ.addStepHandler(handler);

        int dimension = 3;
        double[] alpha = new double[dimension];
        for (int i = 0; i < dimension; ++i) {
            alpha[i] = 1;
        }
        FirstOrderDifferentialEquations eq = new MyEq(dimension, alpha);
        double t0 = 0;
        // initial values are zero
        double[] y0 = new double[dimension];
        // except the boundaries
        y0[0] = 1;
        y0[dimension - 1] = 2;
        double t = 10;
        double[] y = new double[dimension];
        integ.integrate(eq, t0, y0, t, y);
    }

}
