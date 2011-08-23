package model;

import junit.framework.Assert;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.Multigraph;
import org.junit.Ignore;
import org.junit.Test;

import framework.ForwardFiniteDifferenceSimulator;

public class ForwardFiniteDifferenceSimulatorText {

    public UndirectedGraph<VertexType, EdgeType> makeGraph() {
        double areaSquareMeters = 10;
        UndirectedGraph<VertexType, EdgeType> g = new Multigraph<VertexType, EdgeType>(EdgeType.class);
        double thicknessMeters = 0.01;
        VertexType v0 = new DirichletVertex(Material.DOUGLAS_FIR, thicknessMeters, areaSquareMeters,
                new TemperatureSource() {
                    @Override
                    double temperature() {
                        return 0.0;
                    }

                });
        g.addVertex(v0);
        for (int i = 0; i < 2; ++i) {
            VertexType v1 = new UnboundedVertex(Material.DOUGLAS_FIR, thicknessMeters, areaSquareMeters);
            v1.setTemperature(0);
            g.addVertex(v1);
            g.addEdge(v0, v1);
            v0 = v1;
        }
        thicknessMeters = 0.01;
        for (int i = 0; i < 5; ++i) {
            VertexType v1 = new UnboundedVertex(Material.STYROFOAM, thicknessMeters, areaSquareMeters);
            v1.setTemperature(0);
            g.addVertex(v1);
            g.addEdge(v0, v1);
            v0 = v1;
        }
        thicknessMeters = 0.01;
        for (int i = 0; i < 2; ++i) {
            VertexType v1 = new UnboundedVertex(Material.DOUGLAS_FIR, thicknessMeters, areaSquareMeters);
            v1.setTemperature(0);
            g.addVertex(v1);
            g.addEdge(v0, v1);
            v0 = v1;
        }

        // TODO: make a specific type for the boundary layer rather than specifying a thickness.
        VertexType v1 = new UnboundedVertex(Material.AIR_BOUNDARY_LAYER, thicknessMeters, areaSquareMeters);
        v1.setTemperature(0);
        g.addVertex(v1);
        g.addEdge(v0, v1);
        v0 = v1;

        v1 = new InternalHeatVertex(Material.AIR_BULK_MIXED, 1, 10, new InternalHeat() {

            @Override
            public double heatWatts() {
                return 50;
            }

        });

        g.addVertex(v1);
        g.addEdge(v0, v1);

        return g;
    }

    @Test
    @Ignore
    public void directFiniteDifferenceGraph() {
        UndirectedGraph<VertexType, EdgeType> g = makeGraph();
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 0.3, 20000000);
    }
    
    @Test
    public void specificHeatTest1() {
        /*
         * a single node
         * 
         * thermal conductivity 1 W/(m·K)
         * 
         * density 100 kg/m³
         * 
         * specific heat capacity 1 J/(kg·K)
         * 
         * 100 J/m3K
         */
        UndirectedGraph<VertexType, EdgeType> g = new Multigraph<VertexType, EdgeType>(EdgeType.class);
        /*
         * 1 meter thick, 1 m area = 1 cubic meters
         * 
         * so 100 j/k
         */
        VertexType v0 = new InternalHeatVertex(Material.FOR_TESTING, 1, 1, new InternalHeat() {
            @Override
            public double heatWatts() {
                return 50;
            }
        });
        g.addVertex(v0);
        /*
         * run for 100 seconds in 1 second steps
         * 
         * at 50 watts, total heat is 50j/s * 100s = 5000j.
         * 
         * so, delta t should be 50 degrees.
         */
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 100);
        Assert.assertEquals(1, g.vertexSet().size());
        Assert.assertEquals(50, g.vertexSet().iterator().next().getTemperature(), 0.01);
    }
    
    @Test
    public void specificHeatTest2() {
        /*
         * a single node
         * 
         * thermal conductivity 1 W/(m·K)
         * 
         * density 100 kg/m³
         * 
         * specific heat capacity 1 J/(kg·K)
         * 
         * 100 J/m3K
         */
        UndirectedGraph<VertexType, EdgeType> g = new Multigraph<VertexType, EdgeType>(EdgeType.class);
        /*
         * 1 meter thick, 1 m area = 1 cubic meters
         * 
         * so 100 j/k
         */
        VertexType v0 = new InternalHeatVertex(Material.FOR_TESTING, 1, 1, new InternalHeat() {
            @Override
            public double heatWatts() {
                return 50;
            }
        });
        g.addVertex(v0);
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
        Assert.assertEquals(50, g.vertexSet().iterator().next().getTemperature(), 0.01);
    }

    @Test
    public void specificHeatTest3() {
        /*
         * a single node
         * 
         * thermal conductivity 1 W/(m·K)
         * 
         * density 100 kg/m³
         * 
         * specific heat capacity 1 J/(kg·K)
         * 
         * 100 J/m3K
         */
        UndirectedGraph<VertexType, EdgeType> g = new Multigraph<VertexType, EdgeType>(EdgeType.class);
        /*
         * 1 meter thick, 10 m area = 10 cubic meters
         * 
         * so 1000 j/k
         */
        VertexType v0 = new InternalHeatVertex(Material.FOR_TESTING, 1, 10, new InternalHeat() {
            @Override
            public double heatWatts() {
                return 50;
            }
        });
        g.addVertex(v0);
        /*
         * run for 100 seconds in 1 second steps
         * 
         * at 50 watts, total heat is 50j/s * 100s = 5000j.
         * 
         * so, delta t should be 5 degrees.  this doesn't work yet because the area is not used,
         * i.e. it's as if the area is one meter.
         */
        ForwardFiniteDifferenceSimulator s = new ForwardFiniteDifferenceSimulator();
        s.doit(g, 1, 100);
        Assert.assertEquals(1, g.vertexSet().size());
        Assert.assertEquals(5, g.vertexSet().iterator().next().getTemperature(), 0.01);
    }

}
