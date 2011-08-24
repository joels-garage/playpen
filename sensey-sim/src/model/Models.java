package model;

import org.apache.log4j.Logger;

public class Models {
    private static final Logger logger = Logger.getLogger(Models.class);

    public static HeatGraph firstHouse() {
        // roughly the surface area of a 2500 sq ft house, walls, floor, ceiling.
        double areaSquareMeters = 500;
        HeatGraph g = new HeatGraph();
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
            g.addEdge(v0, v1, new EdgeType(areaSquareMeters));
            v0 = v1;
        }
        for (int i = 0; i < 5; ++i) {
            VertexType v1 = new UnboundedVertex(Material.STYROFOAM, thicknessMeters, areaSquareMeters);
            v1.setTemperature(0);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(areaSquareMeters));
            v0 = v1;
        }
        for (int i = 0; i < 2; ++i) {
            VertexType v1 = new UnboundedVertex(Material.DOUGLAS_FIR, thicknessMeters, areaSquareMeters);
            v1.setTemperature(0);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(areaSquareMeters));
            v0 = v1;
        }

        // TODO: make a specific type for the boundary layer rather than specifying a thickness.
        VertexType v1 = new UnboundedVertex(Material.AIR_BOUNDARY_LAYER, Material.AIR_BOUNDARY_LAYER_THICKNESS,
                areaSquareMeters);
        v1.setTemperature(0);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(areaSquareMeters));
        v0 = v1;

        // TODO: make area a property of an edge.
        // 500 cubic meters == about 2500 sq ft, 8 ft ceiling
        v1 = new InternalHeatVertex(Material.AIR_BULK_MIXED, 1, areaSquareMeters, new InternalHeat() {

            @Override
            public double heatWatts() {
                // about three tons
                return 10000;
            }

        });

        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(areaSquareMeters));

        return g;
    }

    // conduction from stud wall only, steady state is 38 degrees delta T with a half ton, which seems high
    public static HeatGraph wallOnly() {
        double wallHeightMeters = 2.5;
        double floorAreaSquareMeters = 250;
        double wallAreaSquareMeters = Math.sqrt(floorAreaSquareMeters) * 4 * wallHeightMeters;
        logger.info("wall area sqm = " + wallAreaSquareMeters);
        double interiorVolume = floorAreaSquareMeters * wallHeightMeters;
        final double OATK = 305;

        HeatGraph g = new HeatGraph();

        // outside air; dimensions don't matter
        VertexType v0 = new DirichletVertex(Material.AIR_BULK_MIXED, 10, wallAreaSquareMeters, new TemperatureSource() {
            @Override
            double temperature() {
                // a realistic summer temperature
                // TODO: variable temperature
                return OATK;
            }

        });
        g.addVertex(v0);

        // outside boundary layer for walls
        VertexType v1 = new UnboundedVertex(Material.AIR_BOUNDARY_LAYER, Material.AIR_BOUNDARY_LAYER_THICKNESS,
                wallAreaSquareMeters);
        v1.setTemperature(OATK);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
        v0 = v1;

        // outside layer of wall
        v1 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.01, wallAreaSquareMeters);
        v1.setTemperature(OATK);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
        v0 = v1;

        // insulating part of wall
        v1 = new UnboundedVertex(Material.STYROFOAM, 0.09, wallAreaSquareMeters * 0.8);
        // stud part of wall
        VertexType v2 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.09, wallAreaSquareMeters * 0.2);
        v1.setTemperature(OATK);
        v2.setTemperature(OATK);
        g.addVertex(v1);
        g.addVertex(v2);

        // edge to foam
        g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters * 0.8));
        // edge to frame
        g.addEdge(v0, v2, new EdgeType(wallAreaSquareMeters * 0.2));

        // inside layer of wall
        VertexType v4 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.01, wallAreaSquareMeters);
        v4.setTemperature(OATK);
        g.addVertex(v4);
        // edge from foam
        g.addEdge(v1, v4, new EdgeType(wallAreaSquareMeters * 0.8));
        // edge from frame
        g.addEdge(v2, v4, new EdgeType(wallAreaSquareMeters * 0.2));

        v0 = v4;

        // inside boundary layer for walls
        v1 = new UnboundedVertex(Material.AIR_BOUNDARY_LAYER, Material.AIR_BOUNDARY_LAYER_THICKNESS,
                wallAreaSquareMeters);
        v1.setTemperature(OATK);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
        v0 = v1;

        // interior volume
        v1 = new InternalHeatVertex(Material.AIR_BULK_MIXED, interiorVolume / wallAreaSquareMeters,
                wallAreaSquareMeters, new InternalHeat() {
                    @Override
                    public double heatWatts() {
                        // steady state, no solar gain, say half a ton?
                        return -1500;
                    }

                });
        v1.setTemperature(OATK);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
        return g;
    }

    // conduction from wall and ceiling, steady state is 15 degrees F delta T with a half ton, which seems high
    public static HeatGraph wallAndCeilingConductionOnly() {
        double wallHeightMeters = 2.5;
        double floorAreaSquareMeters = 250;
        double wallAreaSquareMeters = Math.sqrt(floorAreaSquareMeters) * 4 * wallHeightMeters;
        logger.info("wall area sqm = " + wallAreaSquareMeters);
        double interiorVolume = floorAreaSquareMeters * wallHeightMeters;
        final double OATK = 305;

        HeatGraph g = new HeatGraph();

        // outside air; dimensions don't matter
        VertexType exteriorVertex = new DirichletVertex(Material.AIR_BULK_MIXED, 10, wallAreaSquareMeters,
                new TemperatureSource() {
                    @Override
                    double temperature() {
                        // a realistic summer temperature
                        // TODO: variable temperature
                        return OATK;
                    }

                });
        g.addVertex(exteriorVertex);

        // interior volume
        VertexType interiorVertex = new InternalHeatVertex(Material.AIR_BULK_MIXED, interiorVolume
                / wallAreaSquareMeters, wallAreaSquareMeters, new InternalHeat() {
            @Override
            public double heatWatts() {
                // steady state, no solar gain, say half a ton?
                return -1500;
            }

        });
        interiorVertex.setTemperature(OATK);
        g.addVertex(interiorVertex);

        {

            // outside boundary layer for walls
            VertexType v1 = new UnboundedVertex(Material.AIR_BOUNDARY_LAYER, Material.AIR_BOUNDARY_LAYER_THICKNESS,
                    wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(exteriorVertex, v1, new EdgeType(wallAreaSquareMeters));
            VertexType v0 = v1;

            // outside layer of wall
            v1 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.01, wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
            v0 = v1;

            // insulating part of wall
            v1 = new UnboundedVertex(Material.STYROFOAM, 0.09, wallAreaSquareMeters * 0.8);
            // stud part of wall
            VertexType v2 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.09, wallAreaSquareMeters * 0.2);
            v1.setTemperature(OATK);
            v2.setTemperature(OATK);
            g.addVertex(v1);
            g.addVertex(v2);

            // edge to foam
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters * 0.8));
            // edge to frame
            g.addEdge(v0, v2, new EdgeType(wallAreaSquareMeters * 0.2));

            // inside layer of wall
            VertexType v4 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.01, wallAreaSquareMeters);
            v4.setTemperature(OATK);
            g.addVertex(v4);
            // edge from foam
            g.addEdge(v1, v4, new EdgeType(wallAreaSquareMeters * 0.8));
            // edge from frame
            g.addEdge(v2, v4, new EdgeType(wallAreaSquareMeters * 0.2));

            v0 = v4;

            // inside boundary layer for walls
            v1 = new UnboundedVertex(Material.AIR_BOUNDARY_LAYER, Material.AIR_BOUNDARY_LAYER_THICKNESS,
                    wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
            v0 = v1;

            g.addEdge(v0, interiorVertex, new EdgeType(wallAreaSquareMeters));
        }

        // ceiling
        {
            // outside boundary layer for roof
            VertexType v1 = new UnboundedVertex(Material.AIR_BOUNDARY_LAYER, Material.AIR_BOUNDARY_LAYER_THICKNESS,
                    floorAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(exteriorVertex, v1, new EdgeType(floorAreaSquareMeters));
            VertexType v0 = v1;

            // outside layer of ceiling
            v1 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.01, floorAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(floorAreaSquareMeters));
            v0 = v1;

            // insulating part of ceiling
            v1 = new UnboundedVertex(Material.STYROFOAM, 0.09, floorAreaSquareMeters * 0.8);
            // stud part of wall
            VertexType v2 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.09, floorAreaSquareMeters * 0.2);
            v1.setTemperature(OATK);
            v2.setTemperature(OATK);
            g.addVertex(v1);
            g.addVertex(v2);

            // edge to foam
            g.addEdge(v0, v1, new EdgeType(floorAreaSquareMeters * 0.8));
            // edge to frame
            g.addEdge(v0, v2, new EdgeType(floorAreaSquareMeters * 0.2));

            // inside layer of wall
            VertexType v4 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.01, floorAreaSquareMeters);
            v4.setTemperature(OATK);
            g.addVertex(v4);
            // edge from foam
            g.addEdge(v1, v4, new EdgeType(floorAreaSquareMeters * 0.8));
            // edge from frame
            g.addEdge(v2, v4, new EdgeType(floorAreaSquareMeters * 0.2));

            v0 = v4;

            // inside boundary layer for walls
            v1 = new UnboundedVertex(Material.AIR_BOUNDARY_LAYER, Material.AIR_BOUNDARY_LAYER_THICKNESS,
                    floorAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(floorAreaSquareMeters));
            v0 = v1;

            g.addEdge(v0, interiorVertex, new EdgeType(floorAreaSquareMeters));

        }

        return g;
    }

    // transfer due to infiltration only
    public static HeatGraph infiltrationOnly() {
        double ACH = 0.5; // wild guess
        double wallHeightMeters = 2.5;
        double floorAreaSquareMeters = 250;
        double wallAreaSquareMeters = Math.sqrt(floorAreaSquareMeters) * 4 * wallHeightMeters;
        logger.info("wall area sqm = " + wallAreaSquareMeters);
        double interiorVolume = floorAreaSquareMeters * wallHeightMeters;

        double thickness = 0.1; // meaningless
        double airChangesPerSecond = ACH / 3600;
        double cubicMetersPerSecond = airChangesPerSecond * interiorVolume;
        logger.info("cubic meters per second: " + cubicMetersPerSecond);
        double wattsPerKelvin = cubicMetersPerSecond * Material.AIR_BULK_MIXED.getVolumetricHeatCapacity();
        double effectiveK = thickness * wattsPerKelvin / wallAreaSquareMeters;
        Material infiltrationMaterial = new Material("infiltration", effectiveK, Material.AIR_BULK_MIXED.rho,
                Material.AIR_BULK_MIXED.cp);
        final double OATK = 305;

        HeatGraph g = new HeatGraph();

        // outside air; dimensions don't matter
        VertexType outside = new DirichletVertex(Material.AIR_BULK_MIXED, 10, wallAreaSquareMeters,
                new TemperatureSource() {
                    @Override
                    double temperature() {
                        // a realistic summer temperature
                        // TODO: variable temperature
                        return OATK;
                    }
                });
        g.addVertex(outside);

        // infiltration layer
        VertexType infiltration = new UnboundedVertex(infiltrationMaterial, thickness, wallAreaSquareMeters);
        infiltration.setTemperature(OATK);
        g.addVertex(infiltration);

        // interior volume
        VertexType inside = new InternalHeatVertex(Material.AIR_BULK_MIXED, interiorVolume / wallAreaSquareMeters,
                wallAreaSquareMeters, new InternalHeat() {
                    @Override
                    public double heatWatts() {
                        // about one ton
                        return -3000;
                    }
                });
        inside.setTemperature(OATK);
        g.addVertex(inside);

        g.addEdge(outside, infiltration, new EdgeType(wallAreaSquareMeters));

        g.addEdge(infiltration, inside, new EdgeType(wallAreaSquareMeters));
        return g;
    }

    // conduction from wall and ceiling, steady state is 15 degrees F delta T with a half ton, which seems high
    public static HeatGraph wallAndCeilingConductionAndInfiltration() {
        double wallHeightMeters = 2.5;
        double floorAreaSquareMeters = 250;
        double wallAreaSquareMeters = Math.sqrt(floorAreaSquareMeters) * 4 * wallHeightMeters;
        logger.info("wall area sqm = " + wallAreaSquareMeters);
        double interiorVolume = floorAreaSquareMeters * wallHeightMeters;
        final double OATK = 305;

        HeatGraph g = new HeatGraph();

        // outside air; dimensions don't matter
        VertexType exteriorVertex = new DirichletVertex(Material.AIR_BULK_MIXED, 10, wallAreaSquareMeters,
                new TemperatureSource() {
                    @Override
                    double temperature() {
                        // a realistic summer temperature
                        // TODO: variable temperature
                        return OATK;
                    }

                });
        g.addVertex(exteriorVertex);

        // interior volume
        VertexType interiorVertex = new InternalHeatVertex(Material.AIR_BULK_MIXED, interiorVolume
                / wallAreaSquareMeters, wallAreaSquareMeters, new InternalHeat() {
            @Override
            public double heatWatts() {
                // steady state, no solar gain, say half a ton?
                return -1500;
            }

        });
        interiorVertex.setTemperature(OATK);
        g.addVertex(interiorVertex);

        {

            // outside boundary layer for walls
            VertexType v1 = new UnboundedVertex(Material.AIR_BOUNDARY_LAYER, Material.AIR_BOUNDARY_LAYER_THICKNESS,
                    wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(exteriorVertex, v1, new EdgeType(wallAreaSquareMeters));
            VertexType v0 = v1;

            // outside layer of wall
            v1 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.01, wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
            v0 = v1;

            // insulating part of wall
            v1 = new UnboundedVertex(Material.STYROFOAM, 0.09, wallAreaSquareMeters * 0.8);
            // stud part of wall
            VertexType v2 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.09, wallAreaSquareMeters * 0.2);
            v1.setTemperature(OATK);
            v2.setTemperature(OATK);
            g.addVertex(v1);
            g.addVertex(v2);

            // edge to foam
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters * 0.8));
            // edge to frame
            g.addEdge(v0, v2, new EdgeType(wallAreaSquareMeters * 0.2));

            // inside layer of wall
            VertexType v4 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.01, wallAreaSquareMeters);
            v4.setTemperature(OATK);
            g.addVertex(v4);
            // edge from foam
            g.addEdge(v1, v4, new EdgeType(wallAreaSquareMeters * 0.8));
            // edge from frame
            g.addEdge(v2, v4, new EdgeType(wallAreaSquareMeters * 0.2));

            v0 = v4;

            // inside boundary layer for walls
            v1 = new UnboundedVertex(Material.AIR_BOUNDARY_LAYER, Material.AIR_BOUNDARY_LAYER_THICKNESS,
                    wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
            v0 = v1;

            g.addEdge(v0, interiorVertex, new EdgeType(wallAreaSquareMeters));
        }

        // ceiling
        {
            // outside boundary layer for roof
            VertexType v1 = new UnboundedVertex(Material.AIR_BOUNDARY_LAYER, Material.AIR_BOUNDARY_LAYER_THICKNESS,
                    floorAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(exteriorVertex, v1, new EdgeType(floorAreaSquareMeters));
            VertexType v0 = v1;

            // outside layer of ceiling
            v1 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.01, floorAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(floorAreaSquareMeters));
            v0 = v1;

            // insulating part of ceiling
            v1 = new UnboundedVertex(Material.STYROFOAM, 0.09, floorAreaSquareMeters * 0.8);
            // stud part of wall
            VertexType v2 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.09, floorAreaSquareMeters * 0.2);
            v1.setTemperature(OATK);
            v2.setTemperature(OATK);
            g.addVertex(v1);
            g.addVertex(v2);

            // edge to foam
            g.addEdge(v0, v1, new EdgeType(floorAreaSquareMeters * 0.8));
            // edge to frame
            g.addEdge(v0, v2, new EdgeType(floorAreaSquareMeters * 0.2));

            // inside layer of wall
            VertexType v4 = new UnboundedVertex(Material.DOUGLAS_FIR, 0.01, floorAreaSquareMeters);
            v4.setTemperature(OATK);
            g.addVertex(v4);
            // edge from foam
            g.addEdge(v1, v4, new EdgeType(floorAreaSquareMeters * 0.8));
            // edge from frame
            g.addEdge(v2, v4, new EdgeType(floorAreaSquareMeters * 0.2));

            v0 = v4;

            // inside boundary layer for walls
            v1 = new UnboundedVertex(Material.AIR_BOUNDARY_LAYER, Material.AIR_BOUNDARY_LAYER_THICKNESS,
                    floorAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(floorAreaSquareMeters));
            v0 = v1;

            g.addEdge(v0, interiorVertex, new EdgeType(floorAreaSquareMeters));

        }

        return g;
    }

}
