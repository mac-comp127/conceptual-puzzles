package edu.macalester.conceptual.cli;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

class PuzzleOptions {
    private final Options options = new Options();
    private final Option help, parts, repeat;
    private final org.apache.commons.cli.CommandLine cmd;

    PuzzleOptions(String[] args) {
        parts = addOption("p", "parts", "i,j,...", "Show only parts with given numbers");
        repeat = addOption("r", "repeat", "num", "Generate <num> puzzles");
        help = addOption(null, "help", "Display this message");
        options.addOption(parts);

        try {
            cmd = new DefaultParser().parse(options, args);
        } catch(ParseException e) {
            usageError(e.getMessage());
            throw new Error();  // unreachable, but silences warnings
        }
    }

    public List<String> commandAndArgs() {
        return cmd.getArgList();
    }

    public boolean help() {
        return cmd.hasOption(help);
    }

    public int repeat() {
        return Integer.parseInt(cmd.getOptionValue(repeat, "1"));
    }

    public List<Integer> parts() {
        if(!cmd.hasOption(parts)) {
            return null;
        }
        return Arrays.stream(
                cmd.getOptionValue(parts)
                    .trim()
                    .split("\\s*,\\s*"))
            .mapToInt(Integer::parseInt)
            .boxed()
            .toList();
    }

    public void usageError(String message) {
        System.err.println("puzzle: " + message);
        System.err.println("Run with --help for usage");
        System.exit(0);
    }

    public void printOptions(PrintWriter out) {
        out.println("Options:");
        new HelpFormatter().printOptions(out, 80, options, 2, 2);
        out.println();
    }

    private Option addOption(
        String shortName,
        String longName,
        String decsription
    ) {
        var option = Option.builder(shortName)
            .longOpt(longName)
            .desc(decsription)
            .build();
        options.addOption(option);
        return option;
    }

    private Option addOption(
        String shortName,
        String longName,
        String argName,
        String decsription
    ) {
        var option = Option.builder(shortName)
            .longOpt(longName)
            .argName(argName)
            .hasArg()
            .desc(decsription)
            .build();
        options.addOption(option);
        return option;
    }
}
