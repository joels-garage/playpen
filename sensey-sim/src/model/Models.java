package model;

import org.apache.log4j.Logger;

public class Models {
    private static final Logger logger = Logger.getLogger(Models.class);

    public static HeatGraph firstHouse() {
        // roughly the surface area of a 2500 sq ft house, walls, floor, ceiling.
        double areaSquareMeters = 500;
        HeatGraph g = new HeatGraph();
        double thicknessMeters = 0.01;
        VertexType v0 = new DirichletVertex("v0", Material.DOUGLAS_FIR, thicknessMeters, areaSquareMeters,
                new TemperatureSource() {
                    @Override
                    double temperature() {
                        return 0.0;
                    }

                });
        g.addVertex(v0);
        for (int i = 0; i < 2; ++i) {
            VertexType v1 = new UnboundedVertex("v1", Material.DOUGLAS_FIR, thicknessMeters, areaSquareMeters);
            v1.setTemperature(0);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(areaSquareMeters));
            v0 = v1;
        }
        for (int i = 0; i < 5; ++i) {
            VertexType v1 = new UnboundedVertex("v1", Material.STYROFOAM, thicknessMeters, areaSquareMeters);
            v1.setTemperature(0);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(areaSquareMeters));
            v0 = v1;
        }
        for (int i = 0; i < 2; ++i) {
            VertexType v1 = new UnboundedVertex("v1", Material.DOUGLAS_FIR, thicknessMeters, areaSquareMeters);
            v1.setTemperature(0);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(areaSquareMeters));
            v0 = v1;
        }

        // TODO: make a specific type for the boundary layer rather than specifying a thickness.
        VertexType v1 = new UnboundedVertex("v1", Material.AIR_BOUNDARY_LAYER, Material.AIR_BOUNDARY_LAYER_THICKNESS,
                areaSquareMeters);
        v1.setTemperature(0);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(areaSquareMeters));
        v0 = v1;

        // TODO: make area a property of an edge.
        // 500 cubic meters == about 2500 sq ft, 8 ft ceiling
        v1 = new InternalHeatVertex("v1", Material.AIR_BULK_MIXED, 1, areaSquareMeters, new InternalHeat() {

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
        VertexType v0 = new DirichletVertex("outside air", Material.AIR_BULK_MIXED, 10, wallAreaSquareMeters,
                new TemperatureSource() {
                    @Override
                    double temperature() {
                        // a realistic summer temperature
                        // TODO: variable temperature
                        return OATK;
                    }

                });
        g.addVertex(v0);

        // outside boundary layer for walls
        VertexType v1 = new UnboundedVertex("outside boundary", Material.AIR_BOUNDARY_LAYER,
                Material.AIR_BOUNDARY_LAYER_THICKNESS, wallAreaSquareMeters);
        v1.setTemperature(OATK);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
        v0 = v1;

        // outside layer of wall
        v1 = new UnboundedVertex("sheathing", Material.DOUGLAS_FIR, 0.01, wallAreaSquareMeters);
        v1.setTemperature(OATK);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
        v0 = v1;

        // insulating part of wall
        v1 = new UnboundedVertex("insulation", Material.STYROFOAM, 0.09, wallAreaSquareMeters * 0.8);
        // stud part of wall
        VertexType v2 = new UnboundedVertex("stud", Material.DOUGLAS_FIR, 0.09, wallAreaSquareMeters * 0.2);
        v1.setTemperature(OATK);
        v2.setTemperature(OATK);
        g.addVertex(v1);
        g.addVertex(v2);

        // edge to foam
        g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters * 0.8));
        // edge to frame
        g.addEdge(v0, v2, new EdgeType(wallAreaSquareMeters * 0.2));

        // inside layer of wall
        VertexType v4 = new UnboundedVertex("paneling", Material.DOUGLAS_FIR, 0.01, wallAreaSquareMeters);
        v4.setTemperature(OATK);
        g.addVertex(v4);
        // edge from foam
        g.addEdge(v1, v4, new EdgeType(wallAreaSquareMeters * 0.8));
        // edge from frame
        g.addEdge(v2, v4, new EdgeType(wallAreaSquareMeters * 0.2));

        v0 = v4;

        // inside boundary layer for walls
        v1 = new UnboundedVertex("inside boundary", Material.AIR_BOUNDARY_LAYER, Material.AIR_BOUNDARY_LAYER_THICKNESS,
                wallAreaSquareMeters);
        v1.setTemperature(OATK);
        g.addVertex(v1);
        g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
        v0 = v1;

        // interior volume
        v1 = new InternalHeatVertex("interior volume", Material.AIR_BULK_MIXED, interiorVolume / wallAreaSquareMeters,
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
        VertexType exteriorVertex = new DirichletVertex("outside air", Material.AIR_BULK_MIXED, 10,
                wallAreaSquareMeters, new TemperatureSource() {
                    @Override
                    double temperature() {
                        // a realistic summer temperature
                        // TODO: variable temperature
                        return OATK;
                    }

                });
        g.addVertex(exteriorVertex);

        // interior volume
        VertexType interiorVertex = new InternalHeatVertex("interior volume", Material.AIR_BULK_MIXED, interiorVolume
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
            VertexType v1 = new UnboundedVertex("outside boundary", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(exteriorVertex, v1, new EdgeType(wallAreaSquareMeters));
            VertexType v0 = v1;

            // outside layer of wall
            v1 = new UnboundedVertex("sheathing", Material.DOUGLAS_FIR, 0.01, wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
            v0 = v1;

            // insulating part of wall
            v1 = new UnboundedVertex("insulation", Material.STYROFOAM, 0.09, wallAreaSquareMeters * 0.8);
            // stud part of wall
            VertexType v2 = new UnboundedVertex("stud", Material.DOUGLAS_FIR, 0.09, wallAreaSquareMeters * 0.2);
            v1.setTemperature(OATK);
            v2.setTemperature(OATK);
            g.addVertex(v1);
            g.addVertex(v2);

            // edge to foam
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters * 0.8));
            // edge to frame
            g.addEdge(v0, v2, new EdgeType(wallAreaSquareMeters * 0.2));

            // inside layer of wall
            VertexType v4 = new UnboundedVertex("paneling", Material.DOUGLAS_FIR, 0.01, wallAreaSquareMeters);
            v4.setTemperature(OATK);
            g.addVertex(v4);
            // edge from foam
            g.addEdge(v1, v4, new EdgeType(wallAreaSquareMeters * 0.8));
            // edge from frame
            g.addEdge(v2, v4, new EdgeType(wallAreaSquareMeters * 0.2));

            v0 = v4;

            // inside boundary layer for walls
            v1 = new UnboundedVertex("inside wall boundary", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
            v0 = v1;

            g.addEdge(v0, interiorVertex, new EdgeType(wallAreaSquareMeters));
        }

        // ceiling
        {
            // outside boundary layer for roof
            VertexType v1 = new UnboundedVertex("outside roof boundary", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, floorAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(exteriorVertex, v1, new EdgeType(floorAreaSquareMeters));
            VertexType v0 = v1;

            // outside layer of ceiling
            v1 = new UnboundedVertex("shingles", Material.DOUGLAS_FIR, 0.01, floorAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(floorAreaSquareMeters));
            v0 = v1;

            // insulating part of ceiling
            v1 = new UnboundedVertex("ceiling insulation", Material.STYROFOAM, 0.09, floorAreaSquareMeters * 0.8);
            // stud part of wall
            VertexType v2 = new UnboundedVertex("rafters", Material.DOUGLAS_FIR, 0.09, floorAreaSquareMeters * 0.2);
            v1.setTemperature(OATK);
            v2.setTemperature(OATK);
            g.addVertex(v1);
            g.addVertex(v2);

            // edge to foam
            g.addEdge(v0, v1, new EdgeType(floorAreaSquareMeters * 0.8));
            // edge to frame
            g.addEdge(v0, v2, new EdgeType(floorAreaSquareMeters * 0.2));

            // inside layer of wall
            VertexType v4 = new UnboundedVertex("ceiling paneling", Material.DOUGLAS_FIR, 0.01, floorAreaSquareMeters);
            v4.setTemperature(OATK);
            g.addVertex(v4);
            // edge from foam
            g.addEdge(v1, v4, new EdgeType(floorAreaSquareMeters * 0.8));
            // edge from frame
            g.addEdge(v2, v4, new EdgeType(floorAreaSquareMeters * 0.2));

            v0 = v4;

            // inside boundary layer for walls
            v1 = new UnboundedVertex("ceiling boundary", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, floorAreaSquareMeters);
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
        VertexType outside = new DirichletVertex("outside air", Material.AIR_BULK_MIXED, 10, wallAreaSquareMeters,
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
        VertexType infiltration = new UnboundedVertex("infiltration", infiltrationMaterial, thickness,
                wallAreaSquareMeters);
        infiltration.setTemperature(OATK);
        g.addVertex(infiltration);

        // interior volume
        VertexType inside = new InternalHeatVertex("interior volume", Material.AIR_BULK_MIXED, interiorVolume
                / wallAreaSquareMeters, wallAreaSquareMeters, new InternalHeat() {
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
        VertexType exteriorVertex = new DirichletVertex("outside air", Material.AIR_BULK_MIXED, 10,
                wallAreaSquareMeters, new TemperatureSource() {
                    @Override
                    double temperature() {
                        // a realistic summer temperature
                        // TODO: variable temperature
                        return OATK;
                    }
                });
        // see http://eetd.lbl.gov/coolroof/ref_01.htm
        VertexType sky = new DirichletVertex("sky", Material.AIR_BULK_MIXED, 10, wallAreaSquareMeters,
                new TemperatureSource() {
                    @Override
                    double temperature() {
                        // a realistic summer temperature
                        // TODO: variable temperature
                        return OATK - 10;
                    }
                });

        // interior volume
        VertexType interiorVertex = new InternalHeatVertex("interior volume", Material.AIR_BULK_MIXED, interiorVolume
                / wallAreaSquareMeters, wallAreaSquareMeters, new InternalHeat() {
            @Override
            public double heatWatts() {
                // steady state, no solar gain, say a little less than a ton?
                return -3000;
            }

        });
        interiorVertex.setTemperature(OATK);

        g.addVertex(exteriorVertex);
        g.addVertex(sky);
        g.addVertex(interiorVertex);

        {
            // TODO: extract these to an infiltration type
            double thickness = 0.1; // meaningless
            double ACH = 0.5; // wild guess
            double airChangesPerSecond = ACH / 3600;
            double cubicMetersPerSecond = airChangesPerSecond * interiorVolume;
            double wattsPerKelvin = cubicMetersPerSecond * Material.AIR_BULK_MIXED.getVolumetricHeatCapacity();
            double effectiveK = thickness * wattsPerKelvin / wallAreaSquareMeters;
            Material infiltrationMaterial = new Material("infiltration", effectiveK, Material.AIR_BULK_MIXED.rho,
                    Material.AIR_BULK_MIXED.cp);

            // infiltration layer
            VertexType infiltration = new UnboundedVertex("infiltration", infiltrationMaterial, thickness,
                    wallAreaSquareMeters);
            infiltration.setTemperature(OATK);
            g.addVertex(infiltration);

            g.addEdge(exteriorVertex, infiltration, new EdgeType(wallAreaSquareMeters));
            g.addEdge(infiltration, interiorVertex, new EdgeType(wallAreaSquareMeters));
        }

        {
            // outside boundary layer for walls
            VertexType v1 = new UnboundedVertex("wall outside boundary", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(exteriorVertex, v1, new EdgeType(wallAreaSquareMeters));
            VertexType v0 = v1;

            // outside layer of wall
            v1 = new UnboundedVertex("sheathing", Material.DOUGLAS_FIR, 0.01, wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
            v0 = v1;

            // insulating part of wall
            v1 = new UnboundedVertex("wall insulation", Material.STYROFOAM, 0.09, wallAreaSquareMeters * 0.8);
            // stud part of wall
            VertexType v2 = new UnboundedVertex("stud", Material.DOUGLAS_FIR, 0.09, wallAreaSquareMeters * 0.2);
            v1.setTemperature(OATK);
            v2.setTemperature(OATK);
            g.addVertex(v1);
            g.addVertex(v2);

            // edge to foam
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters * 0.8));
            // edge to frame
            g.addEdge(v0, v2, new EdgeType(wallAreaSquareMeters * 0.2));

            // inside layer of wall
            VertexType v4 = new UnboundedVertex("wall paneling", Material.DOUGLAS_FIR, 0.01, wallAreaSquareMeters);
            v4.setTemperature(OATK);
            g.addVertex(v4);
            // edge from foam
            g.addEdge(v1, v4, new EdgeType(wallAreaSquareMeters * 0.8));
            // edge from frame
            g.addEdge(v2, v4, new EdgeType(wallAreaSquareMeters * 0.2));

            v0 = v4;

            // inside boundary layer for walls
            v1 = new UnboundedVertex("wall inside boundary", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
            v0 = v1;

            g.addEdge(v0, interiorVertex, new EdgeType(wallAreaSquareMeters));
        }

        // ceiling
        {
            VertexType roofConvection = new UnboundedVertex("roof convection", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, floorAreaSquareMeters);
            roofConvection.setTemperature(OATK);
            g.addVertex(roofConvection);
            g.addEdge(exteriorVertex, roofConvection, new EdgeType(floorAreaSquareMeters));

            VertexType radiation = new UnboundedVertex("roof radiation", Material.RADIATION,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, floorAreaSquareMeters);
            radiation.setTemperature(OATK);
            g.addVertex(radiation);
            g.addEdge(sky, radiation, new EdgeType(floorAreaSquareMeters));

            VertexType v1 = new UnboundedVertex("shingles", Material.DOUGLAS_FIR, 0.01, floorAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            // two sink paths
            g.addEdge(roofConvection, v1, new EdgeType(floorAreaSquareMeters));
            g.addEdge(radiation, v1, new EdgeType(floorAreaSquareMeters));
            VertexType v0 = v1;

            v1 = new UnboundedVertex("ceiling insulation", Material.STYROFOAM, 0.09, floorAreaSquareMeters * 0.8);
            VertexType v2 = new UnboundedVertex("joist", Material.DOUGLAS_FIR, 0.09, floorAreaSquareMeters * 0.2);
            v1.setTemperature(OATK);
            v2.setTemperature(OATK);
            g.addVertex(v1);
            g.addVertex(v2);

            // edge to foam
            g.addEdge(v0, v1, new EdgeType(floorAreaSquareMeters * 0.8));
            // edge to frame
            g.addEdge(v0, v2, new EdgeType(floorAreaSquareMeters * 0.2));

            // inside layer of wall
            VertexType v4 = new UnboundedVertex("ceiling paneling", Material.DOUGLAS_FIR, 0.01, floorAreaSquareMeters);
            v4.setTemperature(OATK);
            g.addVertex(v4);
            // edge from foam
            g.addEdge(v1, v4, new EdgeType(floorAreaSquareMeters * 0.8));
            // edge from frame
            g.addEdge(v2, v4, new EdgeType(floorAreaSquareMeters * 0.2));

            v0 = v4;

            // inside boundary layer for walls
            v1 = new UnboundedVertex("ceiling boundary", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, floorAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(floorAreaSquareMeters));
            v0 = v1;

            g.addEdge(v0, interiorVertex, new EdgeType(floorAreaSquareMeters));

        }

        return g;
    }

    public static class VertexObserver {
        VertexType v;

        public VertexType getVertex() {
            return v;
        }

        public void setVertex(VertexType v) {
            this.v = v;
        }
    }

    // conduction from wall and ceiling, with absorption on the roof top surface
    /**
     * 
     * @param acOutput
     * @param vertexObserver
     *            for the thermostat to see the right node
     * @return
     */
    public static HeatGraph wallAndCeilingConductionAndSolarAbsorption(final InternalHeat acOutput,
            VertexObserver vertexObserver) {
        double wallHeightMeters = 2.5;
        final double floorAreaSquareMeters = 250;
        double wallAreaSquareMeters = Math.sqrt(floorAreaSquareMeters) * 4 * wallHeightMeters;
        logger.info("wall area sqm = " + wallAreaSquareMeters);
        double interiorVolume = floorAreaSquareMeters * wallHeightMeters;
        final double OATK = 305;

        final double insolationWperM2 = 1000;

        HeatGraph g = new HeatGraph();

        // outside air; dimensions don't matter
        VertexType exteriorVertex = new DirichletVertex("outside air", Material.AIR_BULK_MIXED, 10,
                wallAreaSquareMeters, new TemperatureSource() {
                    @Override
                    double temperature() {
                        // a realistic summer temperature
                        // TODO: variable temperature
                        return OATK;
                    }
                });
        // see http://eetd.lbl.gov/coolroof/ref_01.htm
        VertexType sky = new DirichletVertex("sky", Material.AIR_BULK_MIXED, 10, wallAreaSquareMeters,
                new TemperatureSource() {
                    @Override
                    double temperature() {
                        // a realistic summer temperature
                        // TODO: variable temperature
                        return OATK - 10;
                    }
                });
        g.addVertex(exteriorVertex);
        g.addVertex(sky);

        // solar gains from windows affect the interior node directly.
        // this is not totally realistic, but good enough.

        double windowAreaSquareMeters = 5;
        final double windowSolarGainWatts = windowAreaSquareMeters * insolationWperM2;

        // interior volume
        VertexType interiorVertex = new InternalHeatVertex("interior volume", Material.AIR_BULK_MIXED, interiorVolume
                / wallAreaSquareMeters, wallAreaSquareMeters, new InternalHeat() {
            @Override
            public double heatWatts() {
                return acOutput.heatWatts() + windowSolarGainWatts;
            }
        });
        interiorVertex.setTemperature(OATK);
        g.addVertex(interiorVertex);
        vertexObserver.setVertex(interiorVertex);

        {

            // outside boundary layer for walls
            VertexType v1 = new UnboundedVertex("wall outside boundary", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(exteriorVertex, v1, new EdgeType(wallAreaSquareMeters));
            VertexType v0 = v1;

            // outside layer of wall
            v1 = new UnboundedVertex("sheathing", Material.DOUGLAS_FIR, 0.01, wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
            v0 = v1;

            // insulating part of wall
            v1 = new UnboundedVertex("wall insulation", Material.STYROFOAM, 0.09, wallAreaSquareMeters * 0.8);
            // stud part of wall
            VertexType v2 = new UnboundedVertex("wall stud", Material.DOUGLAS_FIR, 0.09, wallAreaSquareMeters * 0.2);
            v1.setTemperature(OATK);
            v2.setTemperature(OATK);
            g.addVertex(v1);
            g.addVertex(v2);

            // edge to foam
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters * 0.8));
            // edge to frame
            g.addEdge(v0, v2, new EdgeType(wallAreaSquareMeters * 0.2));

            // inside layer of wall
            VertexType v4 = new UnboundedVertex("wall paneling", Material.DOUGLAS_FIR, 0.01, wallAreaSquareMeters);
            v4.setTemperature(OATK);
            g.addVertex(v4);
            // edge from foam
            g.addEdge(v1, v4, new EdgeType(wallAreaSquareMeters * 0.8));
            // edge from frame
            g.addEdge(v2, v4, new EdgeType(wallAreaSquareMeters * 0.2));

            v0 = v4;

            // inside boundary layer for walls
            v1 = new UnboundedVertex("wall inside boundary", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
            v0 = v1;

            g.addEdge(v0, interiorVertex, new EdgeType(wallAreaSquareMeters));
        }

        // ceiling
        {
            VertexType convection = new UnboundedVertex("convection", Material.CONVECTION,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, floorAreaSquareMeters);
            convection.setTemperature(OATK);
            g.addVertex(convection);
            g.addEdge(exteriorVertex, convection, new EdgeType(floorAreaSquareMeters));

            VertexType radiation = new UnboundedVertex("radiation", Material.RADIATION,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, floorAreaSquareMeters);
            radiation.setTemperature(OATK);
            g.addVertex(radiation);
            g.addEdge(sky, radiation, new EdgeType(floorAreaSquareMeters));

            // outside layer of ceiling
            // this is the one that is heated by the sun.
            // say it's steady state.
            // say the absorptivity is, mmm, what, 80%?
            // solar radiation at peak, is, like, 1kw/m2 more or less, direct normal.
            // so total absorbed radiation is 800 w/m2.
            //
            // the model really should take care of everything; conduction both to outside and inside.
            // maybe the outside air might be better modeled with more nodes?
            final double absorptivity = 0.8;
            // kinda thick
            VertexType v0 = new InternalHeatVertex("shingles", Material.DOUGLAS_FIR, 0.02, floorAreaSquareMeters,
                    new InternalHeat() {
                        @Override
                        public double heatWatts() {
                            return insolationWperM2 * absorptivity * floorAreaSquareMeters;
                        }
                    });
            v0.setTemperature(OATK);
            g.addVertex(v0);
            g.addEdge(convection, v0, new EdgeType(floorAreaSquareMeters));
            g.addEdge(radiation, v0, new EdgeType(floorAreaSquareMeters));

            // insulating part of ceiling; really thick
            VertexType v1 = new UnboundedVertex("ceiling insulation", Material.STYROFOAM, 0.35,
                    floorAreaSquareMeters * 0.9);
            // stud part of wall
            VertexType v2 = new UnboundedVertex("joist", Material.DOUGLAS_FIR, 0.35, floorAreaSquareMeters * 0.1);
            v1.setTemperature(OATK);
            v2.setTemperature(OATK);
            g.addVertex(v1);
            g.addVertex(v2);

            // edge to foam
            g.addEdge(v0, v1, new EdgeType(floorAreaSquareMeters * 0.9));
            // edge to frame
            g.addEdge(v0, v2, new EdgeType(floorAreaSquareMeters * 0.1));

            // inside layer of wall
            VertexType v4 = new UnboundedVertex("ceiling paneling", Material.DOUGLAS_FIR, 0.01, floorAreaSquareMeters);
            v4.setTemperature(OATK);
            g.addVertex(v4);
            // edge from foam
            g.addEdge(v1, v4, new EdgeType(floorAreaSquareMeters * 0.9));
            // edge from frame
            g.addEdge(v2, v4, new EdgeType(floorAreaSquareMeters * 0.1));

            v0 = v4;

            v1 = new UnboundedVertex("ceiling boundary", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, floorAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(floorAreaSquareMeters));
            v0 = v1;

            g.addEdge(v0, interiorVertex, new EdgeType(floorAreaSquareMeters));

        }

        return g;
    }

    // conduction from wall and ceiling, with absorption on the roof top surface
    /**
     * 
     * @param acOutput
     * @param vertexObserver
     *            for the thermostat to see the right node
     * @param OATK
     *            outside air temperature, kelvin
     * @return
     */
    public static HeatGraph wallAndCeilingAndFloorConductionAndSolarAbsorption(final InternalHeat acOutput,
            VertexObserver vertexObserver, final double OATK) {
        double wallHeightMeters = 2.5;
        final double floorAreaSquareMeters = 250;
        double wallAreaSquareMeters = Math.sqrt(floorAreaSquareMeters) * 4 * wallHeightMeters;
        logger.info("wall area sqm = " + wallAreaSquareMeters);
        double interiorVolume = floorAreaSquareMeters * wallHeightMeters;

        final double insolationWperM2 = 1000;

        HeatGraph g = new HeatGraph();

        // outside air; dimensions don't matter
        VertexType exteriorVertex = new DirichletVertex("outside air", Material.AIR_BULK_MIXED, 10,
                wallAreaSquareMeters, new TemperatureSource() {
                    @Override
                    double temperature() {
                        // a realistic summer temperature
                        // TODO: variable temperature
                        return OATK;
                    }
                });
        // see http://eetd.lbl.gov/coolroof/ref_01.htm
        VertexType sky = new DirichletVertex("sky", Material.AIR_BULK_MIXED, 10, wallAreaSquareMeters,
                new TemperatureSource() {
                    @Override
                    double temperature() {
                        // a realistic summer temperature
                        // TODO: variable temperature
                        return OATK - 10;
                    }
                });
        g.addVertex(exteriorVertex);
        g.addVertex(sky);

        // solar gains from windows affect the interior node directly.
        // this is not totally realistic, but good enough.

        double windowAreaSquareMeters = 5;
        final double windowSolarGainWatts = windowAreaSquareMeters * insolationWperM2;

        // interior volume
        VertexType interiorVertex = new InternalHeatVertex("interior volume", Material.AIR_BULK_MIXED, interiorVolume
                / wallAreaSquareMeters, wallAreaSquareMeters, new InternalHeat() {
            @Override
            public double heatWatts() {
                return acOutput.heatWatts() + windowSolarGainWatts;
            }
        });
        interiorVertex.setTemperature(OATK);
        g.addVertex(interiorVertex);
        vertexObserver.setVertex(interiorVertex);
 
        {
            VertexType floorBoundary = new UnboundedVertex("floor boundary", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, floorAreaSquareMeters);
            floorBoundary.setTemperature(OATK);
            g.addVertex(floorBoundary);
            g.addEdge(interiorVertex, floorBoundary, new EdgeType(floorAreaSquareMeters));

            // slab is too influential; insulate it for now
            VertexType carpet = new UnboundedVertex("carpet", Material.CARPET, 0.03, floorAreaSquareMeters);
            carpet.setTemperature(OATK);
            g.addVertex(carpet);
            g.addEdge(floorBoundary, carpet, new EdgeType(floorAreaSquareMeters));

            VertexType slab = new UnboundedVertex("slab", Material.CONCRETE, 0.10, floorAreaSquareMeters);
            slab.setTemperature(295);
            g.addVertex(slab);
            g.addEdge(carpet, slab, new EdgeType(floorAreaSquareMeters));

            final double soilTemp = 286;
            // roughly the average soil temp
            VertexType previous = slab;
            for (int i = 0; i < 3; ++i) {
                VertexType earth = new UnboundedVertex("soil", Material.SOIL, 0.01, floorAreaSquareMeters);
                earth.setTemperature(soilTemp);
                g.addVertex(earth);
                g.addEdge(previous, earth, new EdgeType(floorAreaSquareMeters));
                previous = earth;
            }

            VertexType deepEarth = new DirichletVertex("deep soil", Material.SOIL, 10, floorAreaSquareMeters,
                    new TemperatureSource() {
                        @Override
                        double temperature() {
                            // TODO: make it variable
                            return soilTemp;
                        }
                    });
            g.addVertex(deepEarth);
            g.addEdge(previous, deepEarth, new EdgeType(floorAreaSquareMeters));

        }
        
        {

            // outside boundary layer for walls
            VertexType v1 = new UnboundedVertex("wall outside boundary", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(exteriorVertex, v1, new EdgeType(wallAreaSquareMeters));
            VertexType v0 = v1;

            // outside layer of wall
            v1 = new UnboundedVertex("sheathing", Material.DOUGLAS_FIR, 0.015, wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
            v0 = v1;

            // insulating part of wall
            v1 = new UnboundedVertex("wall insulation", Material.STYROFOAM, 0.09, wallAreaSquareMeters * 0.8);
            // stud part of wall
            VertexType v2 = new UnboundedVertex("wall stud", Material.DOUGLAS_FIR, 0.09, wallAreaSquareMeters * 0.2);
            v1.setTemperature(OATK);
            v2.setTemperature(OATK);
            g.addVertex(v1);
            g.addVertex(v2);

            // edge to foam
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters * 0.8));
            // edge to frame
            g.addEdge(v0, v2, new EdgeType(wallAreaSquareMeters * 0.2));

            // inside layer of wall
            VertexType v4 = new UnboundedVertex("wall sheetrock", Material.SHEETROCK, 0.015, wallAreaSquareMeters);
            v4.setTemperature(OATK);
            g.addVertex(v4);
            // edge from foam
            g.addEdge(v1, v4, new EdgeType(wallAreaSquareMeters * 0.8));
            // edge from frame
            g.addEdge(v2, v4, new EdgeType(wallAreaSquareMeters * 0.2));

            v0 = v4;

            // inside boundary layer for walls
            v1 = new UnboundedVertex("wall inside boundary", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, wallAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(wallAreaSquareMeters));
            v0 = v1;

            g.addEdge(v0, interiorVertex, new EdgeType(wallAreaSquareMeters));
        }

        // ceiling
        {
            VertexType convection = new UnboundedVertex("convection", Material.CONVECTION,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, floorAreaSquareMeters);
            convection.setTemperature(OATK);
            g.addVertex(convection);
            g.addEdge(exteriorVertex, convection, new EdgeType(floorAreaSquareMeters));

            VertexType radiation = new UnboundedVertex("radiation", Material.RADIATION,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, floorAreaSquareMeters);
            radiation.setTemperature(OATK);
            g.addVertex(radiation);
            g.addEdge(sky, radiation, new EdgeType(floorAreaSquareMeters));

            // outside layer of ceiling
            // this is the one that is heated by the sun.
            // say it's steady state.
            // say the absorptivity is, mmm, what, 80%?
            // solar radiation at peak, is, like, 1kw/m2 more or less, direct normal.
            // so total absorbed radiation is 800 w/m2.
            //
            // the model really should take care of everything; conduction both to outside and inside.
            // maybe the outside air might be better modeled with more nodes?
            final double absorptivity = 0.8;
            // kinda thick
            VertexType v0 = new InternalHeatVertex("shingles", Material.DOUGLAS_FIR, 0.02, floorAreaSquareMeters,
                    new InternalHeat() {
                        @Override
                        public double heatWatts() {
                            return insolationWperM2 * absorptivity * floorAreaSquareMeters;
                        }
                    });
            v0.setTemperature(OATK);
            g.addVertex(v0);
            g.addEdge(convection, v0, new EdgeType(floorAreaSquareMeters));
            g.addEdge(radiation, v0, new EdgeType(floorAreaSquareMeters));

            // insulating part of ceiling; really thick
            VertexType v1 = new UnboundedVertex("ceiling insulation", Material.STYROFOAM, 0.35,
                    floorAreaSquareMeters * 0.9);
            // stud part of wall
            VertexType v2 = new UnboundedVertex("joist", Material.DOUGLAS_FIR, 0.35, floorAreaSquareMeters * 0.1);
            v1.setTemperature(OATK);
            v2.setTemperature(OATK);
            g.addVertex(v1);
            g.addVertex(v2);

            // edge to foam
            g.addEdge(v0, v1, new EdgeType(floorAreaSquareMeters * 0.9));
            // edge to frame
            g.addEdge(v0, v2, new EdgeType(floorAreaSquareMeters * 0.1));

            // inside layer of wall
            VertexType v4 = new UnboundedVertex("ceiling sheetrock", Material.SHEETROCK, 0.015, floorAreaSquareMeters);
            v4.setTemperature(OATK);
            g.addVertex(v4);
            // edge from foam
            g.addEdge(v1, v4, new EdgeType(floorAreaSquareMeters * 0.9));
            // edge from frame
            g.addEdge(v2, v4, new EdgeType(floorAreaSquareMeters * 0.1));

            v0 = v4;

            v1 = new UnboundedVertex("ceiling boundary", Material.AIR_BOUNDARY_LAYER,
                    Material.AIR_BOUNDARY_LAYER_THICKNESS, floorAreaSquareMeters);
            v1.setTemperature(OATK);
            g.addVertex(v1);
            g.addEdge(v0, v1, new EdgeType(floorAreaSquareMeters));
            v0 = v1;

            g.addEdge(v0, interiorVertex, new EdgeType(floorAreaSquareMeters));

        }

        return g;
    }
}
