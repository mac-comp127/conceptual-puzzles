package edu.macalester.conceptual.puzzles.stack;

/**
 * A variable, either in a stack frame or as a field of an object.
 */
record Variable(String name, Value value) { }
