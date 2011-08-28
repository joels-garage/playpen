package framework;

import java.util.Iterator;

import junit.framework.Assert;

import model.DirichletVertex;
import model.EdgeType;
import model.HeatGraph;
import model.InternalHeat;
import model.InternalHeatVertex;
import model.Material;
import model.TemperatureSource;
import model.UnboundedVertex;
import model.VertexType;

import org.junit.Test;

import framework.ForwardFiniteDifferenceSimulator;

/**
 * this is mostly comparison between simple analytic results and simulator results.
 * 
 * @author joel
 * 
 */
public class ForwardFiniteDifferenceSimulatorTest {

    @Test
    public void oneNodeOneSecSteps() {
        HeatGraph g = new HeatGraph();
        VertexType v0 = new InternalHeatVertex("v1", Material.FOR_TESTING, 1, 1, new InternalHeat() {
            @Override
            public double heatWatts() {
                return 50;
            }
        });
        g.addVertex(v0);
        Assert.assertEquals(1, v0.getVolume(), 0.01);
        Assert.assertEquals(100000, v0.getNodeHeatCapacity(), 0.01);
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 100);
        Assert.assertEquals(1, g.vertexSet().size());
        Iterator<VertexType> iter = g.vertexSet().iterator();
        Assert.assertEquals(0.05, iter.next().getTemperature(), 0.01);
        Assert.assertFalse(iter.hasNext());
    }

    @Test
    public void oneNodeShortSteps() {
        HeatGraph g = new HeatGraph();
        VertexType v0 = new InternalHeatVertex("v1", Material.FOR_TESTING, 1, 1, new InternalHeat() {
            @Override
            public double heatWatts() {
                return 50;
            }
        });
        g.addVertex(v0);
        Assert.assertEquals(1, v0.getVolume(), 0.01);
        Assert.assertEquals(100000, v0.getNodeHeatCapacity(), 0.01);

        /*
         * run for 100 seconds in 0.1 second steps
         * 
         * at 50 watts, total heat is 50j/s * 100s = 5000j.
         * 
         * so, delta t should be 50 degrees.
         */
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 0.1, 1000);
        Assert.assertEquals(1, g.vertexSet().size());
        Iterator<VertexType> iter = g.vertexSet().iterator();
        Assert.assertEquals(0.05, iter.next().getTemperature(), 0.01);
        Assert.assertFalse(iter.hasNext());
    }

    @Test
    public void oneNodeMoreVolume() {
        HeatGraph g = new HeatGraph();
        VertexType v0 = new InternalHeatVertex("v1", Material.FOR_TESTING, 1, 10, new InternalHeat() {
            @Override
            public double heatWatts() {
                return 50;
            }
        });
        g.addVertex(v0);
        Assert.assertEquals(10, v0.getVolume(), 0.01);
        Assert.assertEquals(1000000, v0.getNodeHeatCapacity(), 0.01);

        /*
         * run for 100 seconds in 1 second steps
         * 
         * at 50 watts, total heat is 50j/s * 100s = 5000j.
         * 
         * so, delta t should be 5 degrees.
         */
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 100);
        Assert.assertEquals(1, g.vertexSet().size());
        Iterator<VertexType> iter = g.vertexSet().iterator();
        Assert.assertEquals(0.005, iter.next().getTemperature(), 0.01);
        Assert.assertFalse(iter.hasNext());
    }

    @Test
    public void oneNodeBulkAir() {
        HeatGraph g = new HeatGraph();
        VertexType v0 = new InternalHeatVertex("v1", Material.AIR_BULK_MIXED, 1, 10, new InternalHeat() {
            @Override
            public double heatWatts() {
                return 50;
            }
        });
        g.addVertex(v0);
        Assert.assertEquals(10, v0.getVolume(), 0.01);
        Assert.assertEquals(12323, v0.getNodeHeatCapacity(), 1);

        /*
         * run for 100 seconds in 1 second steps
         * 
         * at 50 watts, total heat is 50j/s * 100s = 5000j.
         * 
         * 5000 j / 1232j/k = about 400 k
         */
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 100);
        Assert.assertEquals(1, g.vertexSet().size());
        Iterator<VertexType> iter = g.vertexSet().iterator();
        Assert.assertEquals(0.40, iter.next().getTemperature(), 10);
        Assert.assertFalse(iter.hasNext());

    }

    /**
     * two nodes, one edge between them.
     */
    @Test
    public void twoNodesOneEdge() {
        HeatGraph g = new HeatGraph();
        VertexType v0 = new InternalHeatVertex("v1", Material.FOR_TESTING, 1, 10, new InternalHeat() {
            @Override
            public double heatWatts() {
                return 5000;
            }
        });
        g.addVertex(v0);
        Assert.assertEquals(10, v0.getVolume(), 0.01);
        Assert.assertEquals(1000000, v0.getNodeHeatCapacity(), 0.01);

        VertexType v1 = new UnboundedVertex("v1", Material.FOR_TESTING, 1, 10);
        v1.setTemperature(0);
        g.addVertex(v1);
        Assert.assertEquals(10, v1.getVolume(), 0.01);
        Assert.assertEquals(1000000, v1.getNodeHeatCapacity(), 0.01);

        g.addEdge(v0, v1, new EdgeType(100));

        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 1000);
        Assert.assertEquals(2, g.vertexSet().size());
        Iterator<VertexType> iter = g.vertexSet().iterator();
        double x = iter.next().getTemperature();
        double y = iter.next().getTemperature();
        Assert.assertEquals(5, x + y, 0.01);
        // no theory for this value
        Assert.assertEquals(4.77, x, 0.01);
        // no theory for this value
        Assert.assertEquals(0.23, y, 0.01);
        Assert.assertFalse(iter.hasNext());
    }

    /**
     * Two nodes, two edges of same total area as above
     */
    @Test
    public void twoNodesTwoEdgesSameTotalArea() {
        HeatGraph g = new HeatGraph();
        VertexType v0 = new InternalHeatVertex("v1", Material.FOR_TESTING, 1, 10, new InternalHeat() {
            @Override
            public double heatWatts() {
                return 5000;
            }
        });
        g.addVertex(v0);
        Assert.assertEquals(10, v0.getVolume(), 0.01);
        Assert.assertEquals(1000000, v0.getNodeHeatCapacity(), 0.01);

        VertexType v1 = new UnboundedVertex("v1", Material.FOR_TESTING, 1, 10);
        v1.setTemperature(0);
        g.addVertex(v1);
        Assert.assertEquals(10, v1.getVolume(), 0.01);
        Assert.assertEquals(1000000, v1.getNodeHeatCapacity(), 0.01);

        g.addEdge(v0, v1, new EdgeType(50));
        // another edge, so same total K as above
        g.addEdge(v0, v1, new EdgeType(50));

        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 1000);
        Assert.assertEquals(2, g.vertexSet().size());
        Iterator<VertexType> iter = g.vertexSet().iterator();
        double x = iter.next().getTemperature();
        double y = iter.next().getTemperature();
        Assert.assertEquals(5, x + y, 0.01);
        // same values as above
        Assert.assertEquals(4.77, x, 0.01);
        Assert.assertEquals(0.23, y, 0.01);
        Assert.assertFalse(iter.hasNext());
    }

    /**
     * Two nodes, two edges of different area (but same total area as above)
     */
    @Test
    public void twoNodesTwoEdgesDifferentArea() {
        HeatGraph g = new HeatGraph();
        VertexType v0 = new InternalHeatVertex("heated", Material.FOR_TESTING, 1, 10, new InternalHeat() {
            @Override
            public double heatWatts() {
                return 5000;
            }
        });
        g.addVertex(v0);
        Assert.assertEquals(10, v0.getVolume(), 0.01);
        Assert.assertEquals(1000000, v0.getNodeHeatCapacity(), 0.01);

        VertexType v1 = new UnboundedVertex("v1", Material.FOR_TESTING, 1, 10);
        v1.setTemperature(0);
        g.addVertex(v1);
        Assert.assertEquals(10, v1.getVolume(), 0.01);
        Assert.assertEquals(1000000, v1.getNodeHeatCapacity(), 0.01);

        // same total area (10) but not evenly distributed
        g.addEdge(v0, v1, new EdgeType(75));
        g.addEdge(v0, v1, new EdgeType(25));

        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 1000);
        Assert.assertEquals(2, g.vertexSet().size());
        Iterator<VertexType> iter = g.vertexSet().iterator();
        double x = iter.next().getTemperature();
        double y = iter.next().getTemperature();
        Assert.assertEquals(5, x + y, 0.01);
        // same values as above
        Assert.assertEquals(4.76, x, 0.01);
        Assert.assertEquals(0.23, y, 0.01);
        Assert.assertFalse(iter.hasNext());
    }

    /**
     * three nodes, two edges of same areas
     */
    @Test
    public void threeNodesTwoEdgesSameArea() {
        HeatGraph g = new HeatGraph();
        VertexType v0 = new InternalHeatVertex("heated", Material.FOR_TESTING, 1, 10, new InternalHeat() {
            @Override
            public double heatWatts() {
                return 5000;
            }
        });
        g.addVertex(v0);
        Assert.assertEquals(10, v0.getVolume(), 0.01);
        Assert.assertEquals(1000000, v0.getNodeHeatCapacity(), 0.01);

        // each of these is the same size as the above so we can read the Q balance from T

        VertexType v1 = new UnboundedVertex("v1", Material.FOR_TESTING, 1, 10);
        v1.setTemperature(0);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(50));
        Assert.assertEquals(10, v1.getVolume(), 0.01);
        Assert.assertEquals(1000000, v1.getNodeHeatCapacity(), 0.01);

        v1 = new UnboundedVertex("v1", Material.FOR_TESTING, 1, 10);
        v1.setTemperature(0);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(50));
        Assert.assertEquals(10, v1.getVolume(), 0.01);
        Assert.assertEquals(1000000, v1.getNodeHeatCapacity(), 0.01);

        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 1000);
        Assert.assertEquals(3, g.vertexSet().size());
        Iterator<VertexType> iter = g.vertexSet().iterator();
        double x = iter.next().getTemperature();
        double y = iter.next().getTemperature();
        double z = iter.next().getTemperature();
        // assert correct total heat
        Assert.assertEquals(5, x + y + z, 0.01);
        // the heated node -- this is lower than the above case because the heat sink is bigger.
        Assert.assertEquals(4.76, x, 0.01);
        // more
        Assert.assertEquals(0.12, y, 0.01);
        // less
        Assert.assertEquals(0.12, z, 0.01);
        Assert.assertFalse(iter.hasNext());
    }

    /**
     * three nodes, two edges of different areas
     */
    @Test
    public void threeNodesTwoEdgesDifferentAreas() {
        HeatGraph g = new HeatGraph();
        VertexType v0 = new InternalHeatVertex("heated", Material.FOR_TESTING, 1, 10, new InternalHeat() {
            @Override
            public double heatWatts() {
                return 5000;
            }
        });
        g.addVertex(v0);
        Assert.assertEquals(10, v0.getVolume(), 0.01);
        Assert.assertEquals(1000000, v0.getNodeHeatCapacity(), 0.01);

        VertexType v1 = new UnboundedVertex("v1", Material.FOR_TESTING, 1, 10);
        v1.setTemperature(0);
        g.addVertex(v1);
        // lots of conduction
        g.addEdge(v0, v1, new EdgeType(150));
        Assert.assertEquals(10, v1.getVolume(), 0.01);
        Assert.assertEquals(1000000, v1.getNodeHeatCapacity(), 0.01);

        v1 = new UnboundedVertex("v1", Material.FOR_TESTING, 1, 10);
        v1.setTemperature(0);
        g.addVertex(v1);
        // less conduction
        g.addEdge(v0, v1, new EdgeType(50));
        Assert.assertEquals(10, v1.getVolume(), 0.01);
        Assert.assertEquals(1000000, v1.getNodeHeatCapacity(), 0.01);

        /*
         * run for 100 seconds in 1 second steps
         * 
         * at 50 watts, total heat is 50j/s * 100s = 5000j.
         * 
         * so, delta t should be 5 degrees.
         */
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 1000);
        Assert.assertEquals(3, g.vertexSet().size());
        Iterator<VertexType> iter = g.vertexSet().iterator();
        double x = iter.next().getTemperature();
        double y = iter.next().getTemperature();
        double z = iter.next().getTemperature();
        // assert correct total heat
        Assert.assertEquals(5, x + y + z, 0.01);
        // the heated node, less than the above because conduction to sink is higher
        Assert.assertEquals(4.55, x, 0.01);
        // more
        Assert.assertEquals(0.33, y, 0.01);
        // less than the above case because the "big" sink is taking heat
        Assert.assertEquals(0.11, z, 0.01);
        Assert.assertFalse(iter.hasNext());
    }

    /**
     * steady state resistive node
     */
    @Test
    public void pureK() {
        HeatGraph g = new HeatGraph();

        // supply heat at a fixed rate on one end
        VertexType v0 = new InternalHeatVertex("iron heated", Material.IRON, 1, 10, new InternalHeat() {
            @Override
            public double heatWatts() {
                return 50;
            }
        });
        g.addVertex(v0);
        Assert.assertEquals(10, v0.getVolume(), 0.01);
        Assert.assertEquals(35415000, v0.getNodeHeatCapacity(), 1e3);

        // an insulator in the middle
        VertexType v1 = new UnboundedVertex("foam", Material.STYROFOAM, 1, 10);
        v1.setTemperature(0);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(10));
        Assert.assertEquals(10, v1.getVolume(), 0.01);
        Assert.assertEquals(975000, v1.getNodeHeatCapacity(), 1e3);
        // k of 0.033, thickness 1m, so U of 0.033 W/m2K, area of 10m, 0.33 W/K
        Assert.assertEquals(0.33, v1.getConductance(), 0.01);

        // Dirichlet boundary on the other end
        VertexType v2 = new DirichletVertex("iron dirichlet", Material.IRON, 1, 10, new TemperatureSource() {

            @Override
            public double temperature() {
                return 0;
            }

        });
        g.addVertex(v2);
        g.addEdge(v1, v2, new EdgeType(10));
        Assert.assertEquals(10, v2.getVolume(), 0.01);
        Assert.assertEquals(35415000, v2.getNodeHeatCapacity(), 1e3);

        // run it for a long time, to get equilibrium.
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 5000, 200000);

        Assert.assertEquals(3, g.vertexSet().size());
        Iterator<VertexType> iter = g.vertexSet().iterator();
        double x = iter.next().getTemperature();
        double y = iter.next().getTemperature();
        double z = iter.next().getTemperature();
        // confirm dirichlet condition of zero
        Assert.assertEquals(0, z, 0.01);
        // insulator node is the controlling node.
        // so, with 50W across it, delta T is 151.51
        Assert.assertEquals(151.51, x, 0.1);
        // confirm average insulator temperature is half the range
        Assert.assertEquals(151.51 / 2, y, 0.1);
        Assert.assertFalse(iter.hasNext());
    }

    /**
     * steady state resistive node
     */
    @Test
    public void pureKThreeNodes() {
        HeatGraph g = new HeatGraph();

        // supply heat at a fixed rate on one end
        VertexType v0 = new InternalHeatVertex("iron heated", Material.IRON, 1, 10, new InternalHeat() {
            @Override
            public double heatWatts() {
                return 50;
            }
        });
        g.addVertex(v0);
        Assert.assertEquals(10, v0.getVolume(), 0.01);
        Assert.assertEquals(35415000, v0.getNodeHeatCapacity(), 1e3);

        // a three-node insulator in the middle, same total thickness as above
        VertexType v1 = new UnboundedVertex("foam", Material.STYROFOAM, 0.33333333, 10);
        v1.setTemperature(0);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(10));
        Assert.assertEquals(3.33, v1.getVolume(), 0.01);
        Assert.assertEquals(325000, v1.getNodeHeatCapacity(), 1e3);
        // k of 0.033, thickness 1m, so U of 0.033 W/m2K, area of 10m, 0.33 W/K
        Assert.assertEquals(1, v1.getConductance(), 0.01);
        v0 = v1;

        v1 = new UnboundedVertex("foam", Material.STYROFOAM, 0.33333333, 10);
        v1.setTemperature(0);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(10));
        Assert.assertEquals(3.33, v1.getVolume(), 0.01);
        Assert.assertEquals(325000, v1.getNodeHeatCapacity(), 1e3);
        // k of 0.033, thickness 1m, so U of 0.033 W/m2K, area of 10m, 0.33 W/K
        Assert.assertEquals(1, v1.getConductance(), 0.01);
        v0 = v1;

        v1 = new UnboundedVertex("foam", Material.STYROFOAM, 0.33333333, 10);
        v1.setTemperature(0);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(10));
        Assert.assertEquals(3.33, v1.getVolume(), 0.01);
        Assert.assertEquals(325000, v1.getNodeHeatCapacity(), 1e3);
        // k of 0.033, thickness 1m, so U of 0.033 W/m2K, area of 10m, 0.33 W/K
        Assert.assertEquals(1, v1.getConductance(), 0.01);

        // Dirichlet boundary on the other end
        VertexType v2 = new DirichletVertex("iron dirichlet", Material.IRON, 1, 10, new TemperatureSource() {
            public double temperature() {
                return 0;
            }

        });
        g.addVertex(v2);
        g.addEdge(v1, v2, new EdgeType(10));
        Assert.assertEquals(10, v2.getVolume(), 0.01);
        Assert.assertEquals(35415000, v2.getNodeHeatCapacity(), 1e3);

        // run it for a long time, to get equilibrium.
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1000, 2000000);

        Assert.assertEquals(5, g.vertexSet().size());
        Iterator<VertexType> iter = g.vertexSet().iterator();
        double x = iter.next().getTemperature();
        double y = iter.next().getTemperature();
        double z = iter.next().getTemperature();
        double a = iter.next().getTemperature();
        double b = iter.next().getTemperature();

        // confirm dirichlet condition of zero
        Assert.assertEquals(0, b, 0.01);
        // insulator node is the controlling node.
        // so, with 50W across it, delta T is 151.51
        Assert.assertEquals(151.51, x, 0.1);
        // confirm insulator center values
        Assert.assertEquals(151.51 * 5 / 6, y, 0.1);
        Assert.assertEquals(151.51 / 6, a, 0.1);
        Assert.assertEquals(151.51 / 2, z, 0.1);
        Assert.assertFalse(iter.hasNext());
    }
}
