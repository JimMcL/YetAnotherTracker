package com.jim.util;

import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Represents a distance with units.
 */
public class Dimension {
    private final double distance;
    private final String units;

    /** Parses a number string with the format "<number> <units>".
     *
     * @param s the String to be parsed.
     * @return a new {@link Dimension} instance.
     */
    public static Dimension parse(String s) {
        NumberFormat f = NumberFormat.getInstance();
        ParsePosition parsePosition = new ParsePosition(0);
        Number distance = f.parse(s, parsePosition);
        if (parsePosition.getIndex() > 0) {
            String units = s.substring(parsePosition.getIndex(), s.length()).trim();
            if (units.length() > 0)
                return new Dimension(distance.doubleValue(), units);
        }
        throw new InvalidDimensionString(s);
    }

    public Dimension(double distance, String units) {
        this.distance = distance;
        this.units = units;
    }

    public double getDistance() {
        return distance;
    }

    public String getUnits() {
        return units;
    }

    // ===
    // Inner & nested classes

    private static class InvalidDimensionString extends RuntimeException {
        private final String s;

        public InvalidDimensionString(String s) {
            this.s = s;
        }

        public String getS() {
            return s;
        }
    }
}
