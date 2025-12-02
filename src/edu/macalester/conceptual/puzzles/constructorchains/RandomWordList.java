package edu.macalester.conceptual.puzzles.constructorchains;

import java.util.List;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.ChoiceDeck;

public abstract class RandomWordList {

    private final ChoiceDeck<String> deck;

    public RandomWordList() {
        throw new IllegalArgumentException("you need to provide a puzzle context and a list of strings");
    }

    public RandomWordList(PuzzleContext ctx, List<String> sources) {
        deck = new ChoiceDeck<>(ctx, sources.stream().map(s -> s.replace(" ", "")).toList());
    }

    public String draw() {
        return deck.draw();
    }
}
