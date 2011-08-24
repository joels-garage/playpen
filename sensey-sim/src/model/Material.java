package model;

public class Material {
    /** these are from wikipedia, engineering toolbox */
    public static final Material IRON = new Material("Iron", 80, 7870, 450);
    public static final Material STYROFOAM = new Material("Styrofoam", 0.033, 75, 1300);
    public static final Material DOUGLAS_FIR = new Material("Douglas Fir", 0.15, 580, 1700);
    /**
     * actual effective K is like 5 w/m2k, but the k here is w/mk
     * 
     * TODO: make a type for this rather than this hokey thing
     */
    public static final Material AIR_BOUNDARY_LAYER = new Material("Air Boundary Layer", 0.05, 1.225, 1006);
    /** total guess of 17 w/m2k, might be too high. */
    public static final Material CONVECTION = new Material("Convection", 0.17, 1.225, 1006);

    /** for radiative transfer around 300 k. */
    public static final Material RADIATION = new Material("Radiation", 0.06, 1.225, 1006);

    /**
     * actual boundary layer thickness is like 1cm.
     * 
     * so, 0.05 w/mk / 0.01m = 5.
     * 
     * TODO: handle this differently
     */
    public static final double AIR_BOUNDARY_LAYER_THICKNESS = 0.01;
    /**
     * k=1, rho=100, cp=1
     */
    public static final Material FOR_TESTING = new Material("for testing", 1, 100, 1000);
    /**
     * very high conductivity; assumes infinitely well mixed air TODO: hard to converge this. do it another way.
     */
    public static final Material AIR_BULK_MIXED = new Material("Air Bulk Mixed", 10000, 1.225, 1006);

    public static final Material SOIL = new Material("Soil", 3, 1500, 1480);
    public static final Material CONCRETE = new Material("Concrete", 1.7, 2300, 750);
    public static final Material CARPET = new Material("Carpet", 0.05, 200, 1300);
    public static final Material SHEETROCK = new Material("Sheetrock", 0.17, 1100, 1090);

    public final String name;
    /** thermal conductivity (SI units: W/(m·K)) */
    public final double k;
    /** density (kg/m³) */
    public final double rho;
    /** specific heat capacity (J/(kg·K)) */
    public final double cp;

    /**
     * @param name
     * @param k
     *            conductivity W/mK
     * @param rho
     *            density kg/m3
     * @param cp
     *            heat capacity J/kgK
     */
    public Material(String name, double k, double rho, double cp) {
        this.name = name;
        this.k = k;
        this.rho = rho;
        this.cp = cp;
    }

    /** thermal diffusivity */
    public double alpha() {
        return k / (rho * cp);
    }

    @Override
    public String toString() {
        return name;
        // return "Material [k=" + k + ", rho=" + rho + ", cp=" + cp + "]";
    }

    /** j/m3k */
    public double getVolumetricHeatCapacity() {
        return cp * rho;
    }
}