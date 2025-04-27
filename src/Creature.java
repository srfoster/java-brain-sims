import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;

import java.util.Random;

public class Creature {
	public static boolean FORCE_OPTIMAL = false;
	
    public static int FULL_HEALTH = 20;
    public static int ON_EAT_HEALTH = 2;
    public static int HEALTH_LOSS_PER_TICK = 1;
    public static int MAX_CHILDREN = 20;//20;
    public static int MIN_CHILDREN = 10;
    public static int REPRODUCTIVE_AGE = 10;

    private static int INPUT_SIZE = 2; // dx, dy to food, dx, dy to mate, health
    private static int HIDDEN_SIZE = 4;
    private static int HIDDEN_LAYERS = 0;
    private static int OUTPUT_SIZE = 2;
    
    public static int MATING_RADIUS = 1000;
    
    public static boolean KILL_NON_MOVERS = true; 

    public static double CHANCE_OF_MUTATED_CHILD = 1; 
    public static double PERCENT_OF_GENES_TO_MUTATE_AT_BIRTH = 1; // mutation chance per gene
    public static double BIRTH_MUTATION_STRENGTH_MIN = 1;
    public static double BIRTH_MUTATION_STRENGTH_MAX = 2;
    
    public static double SPONTANEOUS_MUTATION_RATE = 0; // Chance per creature per timestep
    public static double PERCENT_OF_GENES_TO_SPONTANEOUSLY_MUTATE = 0.25; 
    public static double NO_MUTATION_AFTER_STEP = 10000; 

    public static int PUNISHMENT_FOR_FAILURE = 10; //Fraction of health to leave if it moves away from nearest food
    
    public static double MATE_AWARENESS = 0; //0 or 1 please
    

	
    protected int x, y;
    protected boolean isFood;
    protected int health;
    private int stepsAlive = 0;
    private int prevX, prevY;
    private boolean didMoveLastTurn = true;
    private int moveTowardFoodAttempts = 0;
    private int moveTowardFoodSuccesses = 0;

    private BasicNetwork brain;
    private static Random random = new Random();

   // private int timesEaten = 0;

    private Creature nearestFood;
    private Creature nearestMate;

    public Creature(BasicNetwork brain, boolean isFood) {
        this.brain = brain;
        this.isFood = isFood;
        this.health = isFood ? 0 : FULL_HEALTH;
    }
    
    public void setNearestFood(Creature food) {
        this.nearestFood = food;
    }

    public void setNearestMate(Creature mate) {
        this.nearestMate = mate;
    }

    public Creature getNearestFood() {
        return nearestFood;
    }

    public Creature getNearestMate() {
        return nearestMate;
    }

    public static Creature randomCreature(int x_min, int x_max, int y_min, int y_max, boolean isFood) {
        BasicNetwork net = null;
        if (!isFood) {
            net = new BasicNetwork();
            net.addLayer(new BasicLayer(null, true, INPUT_SIZE));
            for(int i = 0; i < HIDDEN_LAYERS; i++)
            	net.addLayer(new BasicLayer(new ActivationSigmoid(), true, HIDDEN_SIZE));
            net.addLayer(new BasicLayer(new ActivationSigmoid(), false, OUTPUT_SIZE));
            net.getStructure().finalizeStructure();
            net.reset();
        }

        Creature c = new Creature(net, isFood);
        c.x = random.nextInt(x_min, x_max);
        c.y = random.nextInt(y_min, y_max);

        c.prevX = c.x;
        c.prevY = c.y;

        return c;
    }
    


   // public int getTimesEaten() {
   //     return timesEaten;
   // }

    public void eat(Creature f) {
       // timesEaten++;
       // health += ON_EAT_HEALTH;
       if(f==nearestFood)
    	   health *= 2;
    }

    public boolean isDead() {
        return !isFood && health <= 0;
    }

    public boolean isMature() {
        return !isFood && stepsAlive >= REPRODUCTIVE_AGE;
    }

    public void move(int dx, int dy, int simWidth, int simHeight) {
        this.x += dx;
        this.y += dy;
        this.x = Math.max(-(simWidth / 2), Math.min(simWidth / 2 - 1, this.x));
        this.y = Math.max(-(simHeight / 2), Math.min(simHeight / 2 - 1, this.y));
    }

    public void step(Creature nearestFood, Creature nearestMate, int simWidth, int simHeight) {
        if (isFood || brain == null) return;

        stepsAlive++;
        health -= HEALTH_LOSS_PER_TICK;

        double dxFood = (nearestFood != null) ? normalize01(nearestFood.x - this.x, simWidth) : 0;
        double dyFood = (nearestFood != null) ? normalize01(nearestFood.y - this.y, simHeight) : 0;
        double dxMate = (nearestMate != null) ? normalize01(nearestMate.x - this.x, simWidth) : 0;
        double dyMate = (nearestMate != null) ? normalize01(nearestMate.y - this.y, simHeight) : 0;
        double normHealth = (double) health / FULL_HEALTH;
        
        //System.out.println("FOOD DIR: " + dxFood + "," + dyFood);
        
        double stuckInput = didMoveLastTurn ? 0.0 : 1.0;

        double[] inputs = new double[]{
            (dxFood + 1)/2, 
            (dyFood+1)/2, 
          //  ((dxMate+1)/2)*MATE_AWARENESS, 
          //  ((dyMate+1)/2)*MATE_AWARENESS, normHealth, stuckInput
            //dxFood, dyFood, 0, 0, normHealth, stuckInput
        };

        double[] outputs = think(inputs);

        // Determine movement
        
        //Hack their brains to insert the correct answer
        //outputs[0] = dxFood;
        //outputs[1] = dyFood;

        int dx = 0;
        if(outputs[0]>0.6666) 
        	dx = 1;
        else if(outputs[0]>0.3333)
        	dx = 0;
        else
        	dx = -1;
        
        int dy = 0;
        if(outputs[1]>0.6666) 
        	dy = 1;
        else if(outputs[1]>0.3333)
        	dy = 0;
        else
        	dy = -1;
        
        
        //Simulate optimal food finding strategy
        if(FORCE_OPTIMAL) {
	        dx = (int) dxFood;
	        dy = (int) dyFood;
        }
        
        
        if (nearestFood != null) {
            moveTowardFoodAttempts++;
            
            

            int foodDx = nearestFood.x - x;
            int foodDy = nearestFood.y - y;

            // If moving closer to food
            boolean movingTowardFood = (Integer.signum(foodDx) == dx)&& (Integer.signum(foodDy) == dy);

            if (movingTowardFood) {
                moveTowardFoodSuccesses++;
                health += 1;
            } else {
            	health = health / PUNISHMENT_FOR_FAILURE;
            }
        }
        
        //if(!isFood)
        //	System.out.println(outputs[0]+","+outputs[1]+"|"+dx+","+dy);

        //Kill creatures that don't desire to move
        if (KILL_NON_MOVERS && dx == 0 && dy == 0) {
            health = 0;
            return;
        }

        didMoveLastTurn = !(prevX == x && prevY == y);

        //if (stepsAlive == 0 && !didMoveLastTurn) {
        //    health = 0; // eliminate idle babies
        //    return;
        //}
        
        move(dx, dy, simWidth, simHeight);

        prevX = x;
        prevY = y;

    }

    private double normalize(int diff, int bound) {
        return (double) diff / (bound / 2.0);
    }
    
    private double normalize01(int diff, int bound) {
    	if(diff < 0) return -1;
    	
    	if(diff > 0) return 1;
    	
    	return 0;
    }

    public double[] think(double[] inputs) {
        if (brain == null) return new double[]{0, 0};
        MLData input = new BasicMLData(inputs);
        MLData output = brain.compute(input);
        return output.getData();
    }

    public double[] getGeneticCode() {
        return brain.getFlat().getWeights();
    }

    public static Creature fromGeneticCode(double[] genes, int x, int y) {
        Creature c = randomCreature(x, x + 1, y, y + 1, false);
        double[] weights = c.getGeneticCode();
        System.arraycopy(genes, 0, weights, 0, Math.min(genes.length, weights.length));
        c.brain.getFlat().setWeights(weights);
        return c;
    }

    public static Creature mate(Creature a, Creature b) {
        double[] genesA = a.getGeneticCode();
        double[] genesB = b.getGeneticCode();
        double[] childGenes = new double[genesA.length];

        boolean shouldMutate = random.nextDouble() < CHANCE_OF_MUTATED_CHILD;
        for (int i = 0; i < genesA.length; i++) {
            childGenes[i] = (genesA[i] + genesB[i]) / 2.0;

            // Small chance of mutation
            if (shouldMutate && random.nextDouble() < PERCENT_OF_GENES_TO_MUTATE_AT_BIRTH) {
                childGenes[i] += (random.nextDouble() - 0.5) * (random.nextDouble(BIRTH_MUTATION_STRENGTH_MIN, BIRTH_MUTATION_STRENGTH_MAX));
            }
        }

        Creature child = fromGeneticCode(childGenes, a.x, a.y);
        
        if(random.nextBoolean()) {
    	  child.x = a.x;
          child.y = a.y;
        } else {
      	  child.x = b.x;
          child.y = b.y;
        }
      
        return child;
    }


    @Override
    public String toString() {
        return (isFood ? "Food" : "Brainful") + "@(" + x + "," + y + ") H:" + health;
    }
    
    public void maybeMutate() {
        if (isFood || brain == null) return;
        if (random.nextDouble() < SPONTANEOUS_MUTATION_RATE 
        		&& stepsAlive < NO_MUTATION_AFTER_STEP) {
        	stepsAlive = 0;
            double[] weights = brain.getFlat().getWeights();
            for (int i = 0; i < weights.length; i++) {
                if (random.nextDouble() < PERCENT_OF_GENES_TO_SPONTANEOUSLY_MUTATE) {
                    weights[i] += (random.nextDouble() * 0.1) - 0.05; //= random.nextDouble(-1, 1);
                }
            }
            brain.getFlat().setWeights(weights);
        }
    }

}
