package edu.macalester.conceptual.util;

import java.util.ArrayList;
import java.util.Arrays;
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

    public static ChoiceDeck<Boolean> makeBooleanDeck(PuzzleContext ctx, int numTrue, int numFalse) {
        Boolean[] trues = new Boolean[numTrue];
        Boolean[] falses = new Boolean[numFalse];

        Arrays.fill(trues, true);
        Arrays.fill(falses, false);

        ArrayList<Boolean> booleans = new ArrayList<>();
        booleans.addAll(List.of(trues));
        booleans.addAll(List.of(falses));

        return new ChoiceDeck<>(ctx, booleans);
    }

    public List<ChoiceType> dealEntireDeck() {
        // need a copy since 'cards' isn't modifiable
        List<ChoiceType> shuffled = new ArrayList<>(cards);
        Collections.shuffle(shuffled, ctx.getRandom());

        // return unmodifiable list, not because we don't want consumers to modify our list (it goes out of scope when this method finishes, after all),
        // but so that they don't shoot themselves in the foot by modifying this -- after this class has shuffled the deck, it shouldn't get changed or reshuffled.
        // Also prevents consumers from accidentally asking for an out-of-bounds result.
        return List.copyOf(shuffled);
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
