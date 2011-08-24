package model;

import org.junit.Assert;
import org.junit.Test;

public class MaterialTest {
    @Test
    public void volumetricHeatCapacity() {
        Assert.assertEquals(3541500, Material.IRON.getVolumetricHeatCapacity(), 1);
        Assert.assertEquals(97500, Material.STYROFOAM.getVolumetricHeatCapacity(), 1);
        Assert.assertEquals(986000, Material.DOUGLAS_FIR.getVolumetricHeatCapacity(), 1);
        Assert.assertEquals(1232.35, Material.AIR_BOUNDARY_LAYER.getVolumetricHeatCapacity(), 0.001);
        Assert.assertEquals(100000, Material.FOR_TESTING.getVolumetricHeatCapacity(), 0.1);
        Assert.assertEquals(1232.35, Material.AIR_BULK_MIXED.getVolumetricHeatCapacity(), 0.001);
    }

}
