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

    public ConstructorChainParameters(byte goalDifficulty, byte difficulty, Random rand) {
        this.goalDifficulty = goalDifficulty;
        this.difficulty = difficulty;
        this.rand = rand;
    }

    public boolean addPrintLn() {
        /*         double probability = difficultyToPrintProbability(ctx);
        if (ctx.getRandom().nextDouble() < probability) {
*/
        return rand.nextDouble() < difficultyToPrintProbability();
    }

    public boolean addSuperCall() {
        return rand.nextDouble() < difficultyToSuperCallProbability();
    }

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

    public boolean typeNamesDiffer() {
        return rand.nextDouble() < difficultyToTypeNamesDiffer();
    }

    private double difficultyToTypeNamesDiffer() {
        if (difficulty < goalDifficulty) {
            return 0;
        }
        else if (difficulty < 5) {
            return 0.5;
        }
        else {
            return 0.75;
        }
    }

    public boolean addNonDefaultCtor() {
        return rand.nextDouble() < difficultyToNonDefaultCtorProbability();
    }

    public boolean addNonDefaultCtorObjectCreation() {
        return rand.nextDouble() < difficultyToNonDefaultCtorObjectCreationProbability();
    }

    /**
     * How deep should the class hierarchy be at this difficulty?
     *
     * @return
     */
    public int hierarchyDepth() {
        return 4 + rand.nextInt(difficulty);
    }

    public int numSiblings() {
        if (difficulty < 3) {
            return 1;
        } else {
            return rand.nextInt(1, 3);
        }
    }


    /**
     * for now, just 50-50 chance we add a print statement. Higher difficulty may be more likely, so there's more output?
     *
     */
    private static double difficultyToPrintProbability() {
        return 0.5;
    }

    /**
     * Probability that a constructor includes a super() call.,
     *
     * @return
     */
    private double difficultyToSuperCallProbability() {
        if (difficulty < goalDifficulty) {
            return 0.25;
        } else if (difficulty < 7) {
            return 0.5;
        } else {
            return 0.75;
        }
    }

    /**
     * Probability that a class declaration includes a non-default constructor.
     *
     * @return
     */
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
        // for now just using above
        return difficultyToNonDefaultCtorProbability();
    }
}
