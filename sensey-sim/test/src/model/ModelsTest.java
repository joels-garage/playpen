package model;

import java.util.Iterator;
import java.util.Set;

import model.Models.VertexObserver;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import framework.ForwardFiniteDifferenceSimulator;

public class ModelsTest {
    private static final Logger logger = Logger.getLogger(ModelsTest.class);

    private void verifyMaxAndMin(double min, double max, HeatGraph g) {
        double myMax = -1e6;
        double myMin = 1e6;
        Set<VertexType> v = g.vertexSet();
        Iterator<VertexType> iter = v.iterator();
        while (iter.hasNext()) {
            double val = iter.next().getTemperature();
            if (val > myMax)
                myMax = val;
            if (val < myMin)
                myMin = val;
        }
        Assert.assertEquals(max, myMax, 1);
        Assert.assertEquals(min, myMin, 1);
    }

    /**
     * random house model i started with
     */
    @Test
    public void firstHouse() {
        HeatGraph g = Models.firstHouse();
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 200000);
        verifyMaxAndMin(0, 40, g);
    }

    /**
     * conduction through walls
     */
    @Test
    public void wallOnly() {
        HeatGraph g = Models.wallOnly();
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 200000);
        verifyMaxAndMin(284, 305, g);
    }

    /**
     * 
     */
    @Test
    public void wallAndCeilingConductionOnly() {
        HeatGraph g = Models.wallAndCeilingConductionOnly();
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 100000);
        verifyMaxAndMin(297, 305, g);
    }

    /**
     * about 1 ton, 28 Kelvin of delta T. that's kind of high, but whatever, close enough.
     */
    @Test
    public void infiltrationOnly() {
        HeatGraph g = Models.infiltrationOnly();
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 100000);
        Set<VertexType> v = g.vertexSet();
        Assert.assertEquals(3, v.size());
        Iterator<VertexType> iter = v.iterator();
        double x = iter.next().getTemperature();
        double y = iter.next().getTemperature();
        double z = iter.next().getTemperature();
        Assert.assertFalse(iter.hasNext());
        // dirichlet
        Assert.assertEquals(305, x, 1);
        Assert.assertEquals(291, y, 1);
        Assert.assertEquals(276, z, 1);
        // analytically ...
        double volume = 250 * 2.5;
        double ACH = 0.5;
        double m3PerSec = volume * ACH / 3600;
        double wattsPerKelvin = 1006 * 1.225 * m3PerSec;
        double watts = 3000;
        double deltaTKelvin = watts / wattsPerKelvin;
        Assert.assertEquals(28, deltaTKelvin, 1);
        Assert.assertEquals(deltaTKelvin, x - z, 1);
        verifyMaxAndMin(276, 305, g);
    }

    /**
     *
     */
    @Test
    public void wallAndCeilingConductionAndInfiltration() {
        // this model includes 3kw of cooling output
        HeatGraph g = Models.wallAndCeilingConductionAndInfiltration();
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 100000);
        // 295K is about 72F.
        verifyMaxAndMin(293, 305, g);
    }

    public static class SwitchableAC implements InternalHeat {
        public boolean on = false;
        private final double output;

        public SwitchableAC(double output) {
            this.output = output;
        }

        public double heatWatts() {
            return on ? output : 0;
        }
    }

    /**
     * bleah, this is wrong, needs convective loss from the roof.
     * 
     * use a different boundary; 17 w/m2k rather than 5.
     * 
     * ok, that's not enough. use radiation.
     * 
     * see http://eetd.lbl.gov/coolroof/ref_01.htm
     * 
     * sky is 10K less than OAT.
     * 
     * radiative cooling at roughly OAT is 6.1W/m2K times difference between surface and sky, i.e. ignore T^4.
     * 
     * so, always subtract 61W/m2, all the time, for sky-facing surfaces.
     * 
     * and also it says that convective loss is 12.4W/m2K, not 17. TODO: think about that.
     */
    @Test
    public void wallAndCeilingConductionAndSolarAbsorption() {
        final double acOutput = -9000; // a little under 3 tons
        SwitchableAC ac = new SwitchableAC(acOutput);
        ac.on = true;
        VertexObserver obs = new VertexObserver();
        HeatGraph g = Models.wallAndCeilingConductionAndSolarAbsorption(ac, obs);
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        // this isn't actually all the way to steady state.
        s.doit(g, 0.1, 100000);
        // 295K is about 72F.
        // todo: roof surface temp should be about 350 kelvin, max, maybe 365 is close enough.
        verifyMaxAndMin(295, 365, g);
    }

    @Test
    public void switchableAC() {
        // 9kw, 2 K deadband => 20 minute cycle period== too short.
        // 12kw, 4 K => 22 minute cycles.
        // ah, the cycle period is dominated by the heat rate of the unconditioned house, which is pretty high!
        final double setpointHigh = 298;
        final double setpointLow = 294;
        final double acOutput = -12000;
        SwitchableAC ac = new SwitchableAC(acOutput);
        // start in the on (cooling) state.
        ac.on = true;
        VertexObserver obs = new VertexObserver();
        HeatGraph g = Models.wallAndCeilingConductionAndSolarAbsorption(ac, obs);
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        // first get kinda close to steady state
        s.doit(g, 0.1, 100000, false);
        // this is the control loop
        for (int i = 0; i < 1440; ++i) {
            // 0.1 second steps, 600 of them
            double stepSizeSec = 0.1;
            int stepsPerControl = 600;
            // so control is evaluated once per minute.
            s.doit(g, stepSizeSec, stepsPerControl, false);
            double t = obs.getVertex().getTemperature();
            logger.info(String.format("%5d %8.3f", i, obs.getVertex().getTemperature()));
            if (t < setpointLow) {
                ac.on = false;
            } else if (t > setpointHigh) {
                ac.on = true;
            } else {
                // it's somewhere in the deadband, doing whatever it was doing before; leave it alone.
            }
        }
    }

    /**
     * oops, now the slab dominates. adding carpet on top of the slab produces about 1 hour cycles, which seems right. i
     * think the slab is still too large an influence, due to stratification, but whatever, i can add stratification
     * someday.
     * 
     * but the duty cycle is still much too high, 86%, because the heat rate is still very high, probably due to low
     * shell mass; add something conductive and massive.
     */
    @Test
    public void switchableACWithSlab() {
        final double setpointHigh = 298;
        final double setpointLow = 294;
        final double setpointOffset = 2;
        //final double acOutput = -8000; // 2.3 tons, good for 90F
        final double acOutput = -12000;
        // final double acOutput = -12000; // 3 tons, not quite enough for 105F
        SwitchableAC ac = new SwitchableAC(acOutput);
        // start in the on (cooling) state.
        ac.on = true;
        VertexObserver obs = new VertexObserver();
        //double outsideAirTempK = 305; // 90F
        double outsideAirTempK = 313;
        // double outsideAirTempK = 313; // 105F

        HeatGraph g = Models.wallAndCeilingAndFloorConductionAndSolarAbsorption(ac, obs, outsideAirTempK);
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        // first get kinda close to steady state
        s.doit(g, 0.1, 100000, false);
        // this is the control loop
        int duty = 0;
        double power[] = new double[1440];
        for (int i = 0; i < 1440; ++i) {
            if (ac.on) {
                power[i] = acOutput;
                ++duty;
            }
            // 0.1 second steps, 600 of them
            double stepSizeSec = 0.1;
            int stepsPerControl = 600;
            // so control is evaluated once per minute.
            s.doit(g, stepSizeSec, stepsPerControl, false);
            double t = obs.getVertex().getTemperature();
            logger.info(String.format("%5d %8.3f", i, obs.getVertex().getTemperature()));

            if (i > 600 && i < 720) {
                // precool for 2 hours
                if (t < setpointLow - setpointOffset) {
                    ac.on = false;
                } else if (t > setpointHigh - setpointOffset) {
                    ac.on = true;
                } else {
                    // it's somewhere in the deadband, doing whatever it was doing before; leave it alone.
                    continue;
                }
            } else if (i >= 720 && i < 840) {
                // overheat
                if (t < setpointLow + setpointOffset) {
                    ac.on = false;
                } else if (t > setpointHigh + setpointOffset) {
                    ac.on = true;
                } else {
                    // it's somewhere in the deadband, doing whatever it was doing before; leave it alone.
                    continue;
                }
            } else {
                if (t < setpointLow) {
                    ac.on = false;
                } else if (t > setpointHigh) {
                    ac.on = true;
                } else {
                    // it's somewhere in the deadband, doing whatever it was doing before; leave it alone.
                    continue;
                }
            }
            // spit out the temps  at mode switches so we can see what's up
            //s.doit(g, 0.1, 1, true);
        }
        // just to spit out the final temperatures
        s.doit(g, 0.1, 1, true);
        logger.info("duty cycle: " + (double) duty / 1440);
        for (int i = 0; i < 24; ++i) {
            double powerByHour = 0;
            for (int j = 0; j < 60; ++j) {
                powerByHour += power[j + i * 60] / (60 * 1000);
            }
            logger.info("hour: " + i + " kwh: " + powerByHour);
        }
    }
}
