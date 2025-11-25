package edu.macalester.conceptual.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.macalester.conceptual.context.PuzzleContext;

/**
 * A random selection utility that behaves like a repeatedly shuffled deck of cards: a client can
 * “draw” from the collection, and (1) choices appear in a random order, but (2) no choice is
 * selected more than one time more than any other choice.
 */
public class ChoiceDeck<ChoiceType> {
    private final PuzzleContext ctx;
    private final List<ChoiceType> cards, deck;

    public ChoiceDeck(PuzzleContext ctx, List<ChoiceType> cards) {
        this.ctx = ctx;
        this.cards = List.copyOf(cards);
        this.deck = new ArrayList<>();
    }

    public ChoiceType draw() {
        if (deck.isEmpty()) {
            deck.addAll(cards);
            Collections.shuffle(deck, ctx.getRandom());
        }
        return deck.remove(0);
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
