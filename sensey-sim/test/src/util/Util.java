package util;

public class Util {
    public static String print(double[] x) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < x.length; i++) {
            b.append(String.format("%5.2f", x[i]));
            b.append(" ");
        }
        return b.toString();
    }
}
