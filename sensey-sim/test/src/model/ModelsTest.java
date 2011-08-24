package model;

import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import framework.ForwardFiniteDifferenceSimulator;

public class ModelsTest {

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
        verifyMaxAndMin(295, 305, g);
    }

}
