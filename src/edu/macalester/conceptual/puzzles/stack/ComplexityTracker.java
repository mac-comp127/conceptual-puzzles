package edu.macalester.conceptual.puzzles.stack;

import java.util.Collection;

/**
 * Keeps track of puzzle element counts to help with difficulty balance.
 */
class ComplexityTracker {
    private final int puzzleDifficulty;
    private long objectsRemaining, complicationsRemaining, propAssignmentsRemaining;
    private long objectCount = 0, arrowCount = 0;

    ComplexityTracker(int puzzleDifficulty) {
        this.puzzleDifficulty = puzzleDifficulty;
        objectsRemaining = puzzleDifficulty + 2;
        complicationsRemaining = puzzleDifficulty;
        propAssignmentsRemaining = 1 + puzzleDifficulty / 2;
    }

    public boolean isWellBalanced() {
        return objectCount >= puzzleDifficulty + 1
            && arrowCount >= puzzleDifficulty * 1.9f + 1
            && arrowCount <= puzzleDifficulty * 2.4f + 3;
    }

    public void countObject() {
        objectCount++;
        objectsRemaining--;
    }

    public boolean hasObjectsRemaining() {
        return objectsRemaining > 0;
    }

    public void countPropAssignment() {
        propAssignmentsRemaining--;
    }

    public boolean hasPropAssignmentsRemaining() {
        return propAssignmentsRemaining > 0;
    }

    public void countArrow() {
        arrowCount++;
    }

    public void countReferencesAsArrows(Collection<Variable> vars) {
        arrowCount += vars.stream()
            .filter(v -> v.value() instanceof Value.Reference)
            .count();
    }

    public void countComplication() {
        complicationsRemaining--;
    }

    public long getComplicationsRemaining() {
        return complicationsRemaining;
    }
}
