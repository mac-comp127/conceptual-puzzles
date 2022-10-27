package edu.macalester.conceptual.context;

/**
 * A puzzle code was malformed or did not have a valid checksum.
 */
public class InvalidPuzzleCodeException extends Exception {
    public InvalidPuzzleCodeException(String message) {
        super(message);
    }
}
