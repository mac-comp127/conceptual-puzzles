package edu.macalester.conceptual.puzzles.constructorchains;

import java.util.Random;

/**
 * This class handles parameters for the constructor chain puzzles: for a given parameter,
 * this class will take into account the goal difficulty, the current difficulty, and return
 * a probability (based on the current context random generator) with which the parameter should
 * be present.
 * <p>
 * The aim is that if you want to change the details of the generated code at a certain difficulty,
 * all your changes to the related probabilities are done here.
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

    public ConstructorChainParameters(byte goalDifficulty, byte difficulty, Random rand) {
        this.goalDifficulty = goalDifficulty;
        this.difficulty = difficulty;
        this.rand = rand;
    }

    /*
     * @return probability that a println statement is added
     */
    public boolean addPrintLn() {
        return rand.nextDouble() < difficultyToPrintProbability();
    }

    /*
     * @return probability that a super(...) call is added as the first line of the constructor -- can be default or non-default
     */
    public boolean addSuperCall() {
        return rand.nextDouble() < difficultyToSuperCallProbability();
    }

    /*
     * @return probability that an object creation statement is added
     */
    public boolean addObjectCreationStatement() {
        return rand.nextDouble() < difficultyToAddObjCreationProbability();
    }

    private double difficultyToAddObjCreationProbability() {
        if (difficulty < goalDifficulty) {
            return 0.25;
        } else if (difficulty < 5) {
            return 0.5;
        } else {
            return 0.75;
        }
    }

    /*
     * @return probability that the static and dynamic type differ for an object creation statement
     */
    public boolean typeNamesDiffer() {
        return rand.nextDouble() < difficultyToTypeNamesDiffer();
    }

    private double difficultyToTypeNamesDiffer() {
        if (difficulty < goalDifficulty) {
            return 0;
        } else if (difficulty < 5) {
            return 0.5;
        } else {
            return 0.75;
        }
    }

    /*
     * @return probability that this class includes a non-default constructor
     */
    public boolean addNonDefaultCtor() {
        return rand.nextDouble() < difficultyToNonDefaultCtorProbability();
    }

    /*
     * @return probability that an object creation statement using a non-default constructor is added
     */
    public boolean addNonDefaultCtorObjectCreation() {
        return rand.nextDouble() < difficultyToNonDefaultCtorObjectCreationProbability();
    }

    /**
     * @return positive integer, depth of the heirarchy at this difficulty
     */
    public int hierarchyDepth() {
        return 4 + rand.nextInt(difficulty);
    }

    /*
     * @return the number of siblings at this level of the hierarchy
     */
    public int numSiblings() {
        if (difficulty < 3) {
            return 1;
        } else {
            return rand.nextInt(1, 3);
        }
    }

    /**
     * A 50-50 chance we add a print statement. Higher difficulty may be more likely, so there's more output?
     */
    private static double difficultyToPrintProbability() {
        return 0.5;
    }

    private double difficultyToSuperCallProbability() {
        if (difficulty < goalDifficulty) {
            return 0.25;
        } else if (difficulty < 7) {
            return 0.5;
        } else {
            return 0.75;
        }
    }

    private double difficultyToNonDefaultCtorProbability() {
        if (difficulty < goalDifficulty) {
            return 0.25;
        } else if (difficulty < 7) {
            return 0.5;
        } else {
            return 0.75;
        }
    }

    private double difficultyToNonDefaultCtorObjectCreationProbability() {
        // for now same as adding a non-default constructor
        return difficultyToNonDefaultCtorProbability();
    }
}
