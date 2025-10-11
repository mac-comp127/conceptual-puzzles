# refactor the show, generate, solve commands

each does:

1. validate input
2. turn input into data needed to perform the command
3. do the command

For 2, some refactoring can be done:

```java
// generate:
        Puzzle puzzle = Puzzle.findByName(puzzleName);
        if (puzzle == null) {
            System.err.println("Unknown puzzle type: " + puzzleName);
            System.err.println("Use `puzzle list` to see available options");
            return;
        }
        PuzzleContext ctx = PuzzleContext.generate(
            puzzle.id(),
            options.difficulty() != null
                ? options.difficulty()
                : puzzle.goalDifficulty());

        applyOptionsToContext(options, ctx, puzzle, false);

// show:
        PuzzleContext ctx = PuzzleContext.fromPuzzleCode(options.commandAndArgs().get(1));
        Puzzle puzzle = Puzzle.findByID(ctx.getPuzzleID());
        if (puzzle == null) {
            System.err.println("This puzzle code refers to a puzzle type that no longer exists.");
            System.err.println("Are you using an outdated code from a previous semester?");
            return;
        }
        applyOptionsToContext(options, ctx, puzzle, false);

// solve:
        PuzzleContext ctx = PuzzleContext.fromPuzzleCode(options.commandAndArgs().get(1));
        Puzzle puzzle = Puzzle.findByID(ctx.getPuzzleID());
        if (puzzle == null) {
            System.err.println("This puzzle code refers to a puzzle type that no longer exists.");
            System.err.println("Are you using an outdated code from a previous semester?");
            return;
        }
        applyOptionsToContext(options, ctx, puzzle, true);
        ctx.setPuzzleTitle(puzzle.description() + ": Solution");
```

# include puzzle code in html output

then you can print, come back later and re-show or get a solution
