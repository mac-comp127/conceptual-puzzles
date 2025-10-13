/* let's mess with constructors and inheritance!

 scaling difficulty: include or exclude various pieces.

 very easiest: just a chain with default ctors

 harder: multiple ctors

 harder yet: ctor for one class in the hierarchy creates instance of another

that could be part 1; part 2 the stuff from Dan Kluwer

part 3 maybe method calls and override -- super.method and this.method calls that are polymorphic

this.foo() doesn't do your own foo()! It calls into child's override
*/

class Organism {
    public Organism() {
        System.out.println("Organism default ctor");
    }

    public Organism(int n) {
        System.out.println("Organism one-int ctor n=" + n);
    }
}

class Animal extends Organism {
    public Animal() {
        super(); // want examples that do and do not explicitly call parent ctor
        System.out.println("animal default ctor");
    }

    public Animal(int m) {
        super(m + 1); // want examples of calling non-default ctor
        System.out.println("animal int ctor m=" + m);
    }

    public Animal(String s) {
        System.out.println("animal string ctor s=" + s);
    }
}

class Plant extends Organism { // want multiple children of some class
    public Plant() {
        super(2);
        System.out.println("plant def ctor");
    }

    public Plant(String type) {
        System.out.println("instantiating a '" + type + "' plant.");
    }
}

class Herbivore extends Animal() {

    private Plant favoriteFood;

    public Herbivore() {
        favoriteFood = new Plant();
    }
}
