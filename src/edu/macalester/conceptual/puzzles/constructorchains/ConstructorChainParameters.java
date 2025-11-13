package edu.macalester.conceptual.puzzles.constructorchains;

import java.util.Random;

/**
 * This class handles parameters for the constructor chain puzzles: for a given parameter,
 * this class will take into account the goal difficulty, the current difficulty, and return
 * a probability (based on the current context random generator) with which the parameter should
 * be present.
 */
public class ConstructorChainParameters {
    private final byte goalDifficulty;
    private final byte difficulty;
    private final Random rand;

    /**
     * Doesn't make sense to have an instance of this without difficulties or a puzzle context random generator.
     */
    private ConstructorChainParameters() {
        throw new IllegalArgumentException("You need to provide a random number generator and difficulty.");
    }

    public ConstructorChainParameters(byte goalDifficulty, byte difficulty, Random rand)
    {
        this.goalDifficulty = goalDifficulty;
        this.difficulty = difficulty;
        this.rand = rand;
    }}
