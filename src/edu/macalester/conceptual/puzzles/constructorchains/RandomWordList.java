package edu.macalester.conceptual.puzzles.constructorchains;

import java.util.List;

import edu.macalester.conceptual.context.PuzzleContext;
import edu.macalester.conceptual.util.ChoiceDeck;

// I've been tryingto do all statics here -- but perhaps I do want an instance, so that
// it can track the state of the choice deck? Otherwise, two calls to this could return the
// same value TODO FIXME
public class RandomWordList {
    public static String getTypeName(PuzzleContext ctx, List<String> sources) {
        List<String> typeNames = sources.stream().map(s -> s.replace(" ", "")).toList();
        var deck = new ChoiceDeck<>(ctx, typeNames);
        return deck.draw();
    }
}
