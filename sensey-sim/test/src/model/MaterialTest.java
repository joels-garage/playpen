package model;

import org.junit.Assert;
import org.junit.Test;

public class MaterialTest {
    @Test
    public void volumetricHeatCapacity() {
        Assert.assertEquals(3541, Material.IRON.getVolumetricHeatCapacity(), 1);
        Assert.assertEquals(97.5, Material.STYROFOAM.getVolumetricHeatCapacity(), 1);
        Assert.assertEquals(986, Material.DOUGLAS_FIR.getVolumetricHeatCapacity(), 1);
        Assert.assertEquals(1.2324, Material.AIR_BOUNDARY_LAYER.getVolumetricHeatCapacity(), 0.001);
        Assert.assertEquals(100, Material.FOR_TESTING.getVolumetricHeatCapacity(), 0.1);
        Assert.assertEquals(1.2324, Material.AIR_BULK_MIXED.getVolumetricHeatCapacity(), 0.001);
    }

}
