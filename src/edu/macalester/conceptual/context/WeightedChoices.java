package edu.macalester.conceptual.context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WeightedChoices<Choice> {
    private final List<WeightedChoice> choices;
    private long totalWeight = 0;

    public static WeightedChoices<String> fromResource(String resPath) {
        var result = new WeightedChoices<String>();
        var reader = new BufferedReader(
            new InputStreamReader(
                Objects.requireNonNull(
                    WeightedChoices.class.getClassLoader().getResourceAsStream(resPath))));
        try (reader) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.isBlank()) {
                    continue;
                }
                var fields = line.split("\\s+");
                if (fields.length != 2) {
                    throw new IOException("Illegal line format: " + line);
                }
                result.add(fields[0], Long.parseLong(fields[1]));
            }
        } catch(Exception e) {
            throw new RuntimeException("Unable to load choices from " + resPath, e);
        }
        return result;
    }

    public WeightedChoices() {
        choices = new ArrayList<>();
    }

    public void add(Choice choice, long weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("weight " + weight + " cannot be < 0");
        }
        choices.add(new WeightedChoice(choice, weight));
        totalWeight += weight;
    }

    public Choice choose(PuzzleContext context) {
        if (totalWeight <= 0) {
            throw new IllegalStateException("WeightedChoices has no choices with nonzero weight");
        }
        long w = context.getRandom().nextLong(totalWeight);
        for (var cur : choices) {
            w -= cur.weight;
            if (w <= 0) {
                return cur.choice;
            }
        }
        throw new AssertionError("Ran out of choices with " + w + " weight left");
    }

    private class WeightedChoice {
        private final Choice choice;
        private final long weight;

        public WeightedChoice(Choice choice, long weight) {
            this.choice = choice;
            this.weight = weight;
        }
    }
}
