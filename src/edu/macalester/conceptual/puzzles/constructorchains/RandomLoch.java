package edu.macalester.conceptual.puzzles.constructorchains;

import java.util.List;

import edu.macalester.conceptual.context.PuzzleContext;

// get a randomly-chosen Scottish loch: https://en.wikipedia.org/wiki/List_of_lochs_of_Scotland

public class RandomLoch extends RandomWordList {
    public static final List<String> sources = List.of(
        "Ness",
        "Lomond",
        "Morar",
        "Tay",
        "Awe",
        "Maree",
        "Ericht",
        "Lochy",
        "Rannoch",
        "Shiel",
        "Katrine",
        "Arkaig",
        "Shin",
        "Ailsh",
        "Affric",
        "Barnshean",
        "Barr",
        "Dowally",
        "Dornell",
        "Eircill",
        "Eilean",
        "Kirkaldy",
        "Kernsary",
        "Macaterick",
        "Maragan",
        "Racadal",
        "Ruthven",
        "Tralaig",
        "Tromlee",
        "Tulleybelton",
        "Uanagan",
        "Coire Shubh",
        "Knockewart",
        "Lambroughton",
        "Halket",
        "Helenton",
        "Lochlea",
        "Lochspouts",
        "Newfarm",
        "South Palmerston",
        "Trindlemoss");

    public static String getTypeName(PuzzleContext ctx) {
        return RandomWordList.getTypeName(ctx, sources);
    }

}