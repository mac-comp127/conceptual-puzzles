package edu.macalester.conceptual.cli;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class PuzzleOptions {
    private final Options options = new Options();
    private final Option help, parts, repeat, difficulty, includeSolutions;
    private final org.apache.commons.cli.CommandLine cmd;

    PuzzleOptions(String[] args) {
        parts = addOption("p", "parts", "i,j,...", "Show only parts with given numbers");
        repeat = addOption("r", "repeat", "num", "Generate <num> different puzzles");
        difficulty = addOption("d", "difficulty", "num", "Change puzzle difficulty from default");
        includeSolutions = addOption("s", "include-solutions", "Show solutions immediately when generating puzzle");
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

    public Integer difficulty() {
        return cmd.hasOption(difficulty)
            ? Integer.parseInt(cmd.getOptionValue(difficulty))
            : null;
    }

    public boolean includeSolutions() {
        return cmd.hasOption(includeSolutions);
    }

    public Set<Integer> partsToShow() {
        if (!cmd.hasOption(parts)) {
            return null;
        }

        return Pattern.compile(",")
            .splitAsStream(cmd.getOptionValue(parts))
            .map(String::trim)
            .map(Integer::parseInt)
            .collect(Collectors.toSet());
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

    public String toCommandLineOptions() {
        StringBuffer out = new StringBuffer();
        for (var opt : cmd.getOptions()) {
            if (opt.getId() == includeSolutions.getId()) {
                continue;
            }
            out.append(" ");
            out.append(opt.getLongOpt() != null ? "--" + opt.getLongOpt() : "-" + opt.getOpt());
            if (opt.getValue() != null) {
                out.append(" ");
                out.append(opt.getValue());
            }
        }
        return out.toString();
    }
}
