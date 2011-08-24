package model;

import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import framework.ForwardFiniteDifferenceSimulator;

public class ModelsTest {
    /**
     * random house model i started with
     */
    @Test
    public void firstHouse() {
        HeatGraph g = Models.firstHouse();
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 0.001, 500000);
        // once we're in a near-solution state, is the convergence issue still that bad? yes it is.
        s.doit(g, 1, 50);
    }

    /**
     * conduction through walls
     */
    @Test
    public void wallOnly() {
        HeatGraph g = Models.wallOnly();
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 500000);
    }

    /**
     * TODO: make this work with a larger step.
     */
    @Test
    public void wallAndCeilingConductionOnly() {
        HeatGraph g = Models.wallAndCeilingConductionOnly();
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 500000);
    }

    /**
     * infiltration is a TINY effect.  ?
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
        Assert.assertEquals(258, y, 1);
        Assert.assertEquals(211, z, 1);
        // analytically ...
        double volume = 250 * 2.5;
        double ACH = 0.5;
        double m3PerSec = volume * ACH / 3600;
        double wattsPerKelvin = 1006 * 1.225 * m3PerSec;
        double watts = 10;
        double deltaTKelvin = watts / wattsPerKelvin;
        Assert.assertEquals(deltaTKelvin, x - z, 1);
    }
}
