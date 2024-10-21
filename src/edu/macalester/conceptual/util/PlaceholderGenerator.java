package edu.macalester.conceptual.util;

public class PlaceholderGenerator {
    private int counter = -1;

    public void next() {
        counter++;
    }

    public String currentName() {
        if (counter < 0) {
            throw new IllegalStateException("next() never called");
        }

        // Placeholders naming: A B C … X Y Z AA AB AC … AX AY AZ BA BB BC … ZX ZY ZZ AAA AAB …
        String result = "";
        for (int n = counter; n >= 0; n = n / 26 - 1) {
            result = (char) ('A' + n % 26) + result;  // more efficient than StringBuilder bc typically one letter
        }
        return result;
    }

    public String current() {
        return "___" + currentName() + "___";
    }
}
