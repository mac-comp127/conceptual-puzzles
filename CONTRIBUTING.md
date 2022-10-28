# How to Create a New Puzzle

Please start by creating a branch for your work. Once puzzles are live for students, we will work by pull request for new puzzles.

## Create your new puzzle class

- Create a new subpackage of `edu.macalester.conceptual.puzzles` with the short name of your new puzzle.
- In that package, create a new implementation of the `Puzzle` interface.
    - You will need to assign it a constant ID number in its `id()` method. This ID is embedded in the puzzle codes, and must be unique among all the other implementations of `Puzzle`.
    - You can use the default implementations of the various difficulty-related methods unless you want to support multiple difficulty levels.
    - The magic happens in the `generate()` method.

Puzzles are subject to several constraints:

- They must consistently generate the same puzzle for the same code.
- They should all follow consistent output and formatting conventions.
- They should respect command-line options.

In order to ensure that your puzzle class honors these contraints, your `generate()` method must strictly adhere to two rules:

1. Do not use any source of randomness, or any other varying input, other than `ctx.getRandom()`.
    - No `new Random()`!
    - No `Math.random()`!
    - No console input!
2. Only produce output via the methods of `ctx.output()`.
    - No `System.out.println()`!
    - No `new CanvasWindow()`!

The basic structure of a `generate()` method is something like this:

```java
ctx.output().paragraph("...some instructions...");
ctx.output().codeBlock("...some code...")
ctx.solution(() -> {
    ctx.output().codeBlock("...solution code...");
    ctx.solutionChecklist("Did you do X?", "Did you do Y?")
});
```

Here is a puzzle with multiple parts:

```java
ctx.output().paragraph("...general instructions...");

ctx.section(() -> {
    ctx.output().paragraph("...instructions for first part...");
    ctx.solution(() -> {
        ctx.output().codeBlock("...solution code...");
    });
});

ctx.section(() -> {
    ctx.output().paragraph("...instructions for second part...");
    ctx.solution(() -> {
        ctx.output().codeBlock("...solution code...");
    });
});
```

Look in the existing puzzle implementations for examples! Also study the API for `PuzzleContext` and `PuzzleOutput` to learn about the available structure & formatting options.

If you follow these conventions, using the methods of `ctx` and `ctx.output()` to structure your puzzle, then the infrastructure will automatically handle:

- Generating a new random puzzle when the user requests a brand new one
- Regenerating the same puzzle from the same puzzle code
- Showing / hiding solutions as appropriate
- Showing / hiding sections based on the `--parts` command line option
- Console formatting & colors
- Word wrapping

### Generating code

The most awkward part of authoring puzzles is usually generating the output code. This project provides facilities to assist you with two very different approaches:

- Approach 1: **Generating code as strings.**

    - Advantages:
        - Strings are familiar, and easy to construct in code.
        - It’s obvious how to create the things you want to create; you only need to know familiar Java syntax.
    - Disadvantages:
        - Structural manipulation of already-created things can be hard.
        - You have to make sure to always generate structurally valid code.
        - You have to worry about syntactic delimeters and corner cases.
        - It is up to you to ensure proper formatting. **However,** note that this project includes a prettifier (`CodeFormatter`), so in most cases it is easiest to generate an unformatted blob of code and let the prettifier sort out the formatting.
    - Examples:
        - The code that assembles loop fragments into [while and for loops](src/edu/macalester/conceptual/puzzles/loops/LoopForm.java) is a good example of a situation where string concatenation is easy and syntactically safe.
        - The [AST puzzle generator](src/edu/macalester/conceptual/puzzles/ast/Generator.java) is just a big mess of string concatenation! Also demonstrates many of this project’s randomness helpers.

- Approach 2: **Generating code using the [JavaParser](https://javaparser.org) abstract syntax tree (AST) library.**

    - Advantages:
        - You don't have to worry about inserting spaces and delimiters; you can focus on structure.
        - Everything you generate will be syntactically valid.
        - You can analyze, manipulate, transform, and combine your generated code structures in logically consistent ways, without worrying about (for example) regular expressions handling every syntactic surprise.
    - Disadvantages:
        - AST construction is far more verbose than the equivalent Java syntax.
        - Every syntactic construct has its a corresponding AST node type, which can be difficult to discover and awkward to construct. Use the included `bin/astprinter` script to see the AST for arbitrary code (run the script for usage examples), and look at `AstUtils` for shortcuts and analysis helpers.
    - Examples:
        - The [`generateBooleanExpr()` method](src/edu/macalester/conceptual/puzzles/booleans/Generator.java) from the bool puzzle is a nice example of how working with a tree data type can mix analysis with recombination to generate random structures within certain constraints.
        - The [`negated()` method in `AstUtils`](src/edu/macalester/conceptual/util/AstUtils.java) does a job that would be very difficult to do with strings.

### Formatting output

This project uses a form of **structured output**: instead of using raw print statements, the output API provides meaningful units such as “paragraph,” “bullet list,” and “block of code.” Read the [Javadoc in `PuzzlePrinter`](src/edu/macalester/conceptual/context/PuzzlePrinter.java) to learn about the available structured output methods and text formatting features.

### Testing your new puzzle class during development

The `ManualPuzzleTest` class provides a main method you can use as simple scaffolding to test a puzzle in development. This has a couple of advantages over adding your puzzle to the command line interface right away:

- It launches faster, and launches straight from your IDE.
- The debugger works with it.
- Once the puzzle is in the CLI, the tests will fail until you add integration tests for it.


## Add puzzle to the CLI and add an integration test

Once your puzzle is working, open up the `Puzzle` interface and add your new puzzle type to the `Puzzle.ALL` constant. This will make it show up in the CLI.

It will also make the tests insist that you provide an integration test for your new puzzle type. Because the puzzles are random, it is difficult to unit test them! Instead of writing individual tests for each random branch and each feature, we have integration tests that run the puzzle through the CLI, using the same script that students use, and ensure that previously generated puzzle codes continue to generate exactly the same output.

The `IntegrationTest` class has a list of puzzle codes to test, and their expected output lives in `test/fixtures/integration/`. To add a new test, run the integration tests, let them fail, and then read the test error message for a `mv` command you can use to move the actual integration test output into place as the expected output. Once you do that, **carefully inspect the puzzle output for correctness**. That output will become the new gold standard for your puzzle!

This testing approach means that if you ever change _anything_ about a puzzle — even adding one new random branch — the existing integration test will become useless. The thing to do in that case is to **carefully inspect the new output for correctness**, and then replace the old test with the new one.


## Create a pull request

Once you create pull request for your new branch, the repo’s CI server will run your integration test (and all the existing tests, of course) via GitHub actions.

Make sure CI passes. Keep in mind that if **any integration tests for _existing_ puzzles fail because of your changes**, then all puzzle codes for that puzzle previously issued to students are now useless! Be very careful about breaking existing puzzles.

If CI passes, have somebody give your PR a review, then merge so students can enjoy!
