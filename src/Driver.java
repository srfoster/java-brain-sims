import javax.swing.*;
import java.util.*;

/*
 * Good ones?
 * 0.48285355787278045,-0.0020767069255899356,0.4478292473844125,0.22369742179804997,0.5202869315428544,0.6128937390914361,
0.5916442879754714,-0.055515458744895954,0.28076206157583206,0.17400719666305492,0.663285534615732,0.624893372235466,
0.5916442879754714,-0.055515458744895954,0.28076206157583206,0.17400719666305492,0.663285534615732,0.624893372235466,
 * Extra good?
 * 
 * 
2.8363366657378517,0.7551800647778475,-1.7076103538644647,-0.0631507194715229,2.909747034097308,-1.4633456321546616,
2.87526378737064,0.37317546817127056,-1.3384713591904607,-0.8178910735254703,2.830531891397168,-1.0354417136864928,
2.50608378695712,0.20051499590890642,-1.4541780531005808,-0.6014627010636199,2.9074509701252755,-1.1266278142805497,
2.6167879310295934,-0.4526209819760326,-1.245985519546042,-0.3971314616684611,2.681904112718101,-0.758319464053476,



-17.548393026066606,-9.446891222207848,5.3771476672839436,-0.2708329470723774,-12.811789080828813,-3.0374109368938855,16.173257100142667,9.253938231544112,-22.115836340040644,0.9304350064249696,1.4152850346598547,-9.378350702528067,16.83822388224059,-6.19465495598678,-25.644334857326562,5.488609847648275,1.4026597119332944,0.8198095871022526,8.289661054373358,-3.008311539145966,-22.627947807150722,6.196186776035061,9.807833874709763,3.745174032078239,7.539024583210647,-8.456314562449055,-6.6088129637044455,-3.6783009462692107,1.4081751140658456,-1.6902844421665097,-11.739672778201042,-6.0147719539256155,-1.410174066391375,4.785787116217691,-10.53591884847415,-2.9167971091186007,-4.934803043731565,-1.1816853927102313,8.824850097397842,1.4834689874544105,7.181219013692424,15.521291367023299,2.2220601888855898,8.16129419223545,

 */

public class Driver {
    public static int SIM_WIDTH = 100;
    public static int SIM_HEIGHT = 100;
    public static int INITIAL_BRAINFUL = 500;
    public static int INITIAL_FOOD = 10;
    public static int FOOD_PER_STEP = 10;
    public static double CHANCE_OF_POISON_PER_STEP = 0.1;
    public static int MAX_TOTAL_POPULATION = 700;
    public static int MAX_TOTAL_FOOD = 400;
    public static int OVERLAP_DISTRIBUTION = 1;
    public static int MARGIN = 30;
    public static int TIMES_TO_EAT_BEFORE_MATING = 5;
    public static int RANDOM_MUTANTS_PER_STEP = 0; 

    private static final Random random = new Random();

    public static void main(String[] args) {
        List<Creature> creatures = new ArrayList<>();

        for (int i = 0; i < INITIAL_BRAINFUL; i++) {
            creatures.add(Creature.randomCreature(-(SIM_WIDTH / 2), SIM_WIDTH / 2,
                    -(SIM_HEIGHT / 2), SIM_HEIGHT / 2, false));
        }

        for (int i = 0; i < INITIAL_FOOD; i++) {
            creatures.add(Creature.randomCreature(-(SIM_WIDTH / 2)+MARGIN, (SIM_WIDTH / 2)-MARGIN,
                    -(SIM_HEIGHT / 2)+MARGIN, (SIM_HEIGHT / 2)-MARGIN, true));
        }

        JFrame frame = new JFrame("Creature Simulation");
        WorldPanel worldPanel = new WorldPanel(creatures, SIM_WIDTH, SIM_HEIGHT, 10);
        frame.add(worldPanel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        new Thread(() -> {
        	int gen = 0;
            while (true) {
            	gen++;
                //System.out.println("=== Generation " + gen + " ===");

                List<Creature> newGeneration = new ArrayList<>();
                List<Creature> food = filterByType(creatures, true);
                List<Creature> brainful = filterByType(creatures, false);
                
                List<Creature> sorted = new ArrayList<>(brainful);
                sorted.sort((a, b) -> Double.compare(b.health, a.health)); // descending order

                // Pick top N as elites
                int eliteCount = Math.max(2, (int) (sorted.size() * 0.1)); // at least 2 elites
                List<Creature> elites = sorted.subList(0, eliteCount);
                
                // Pass to world panel
                SwingUtilities.invokeLater(() -> {
                    worldPanel.setCreatures(creatures);
                    worldPanel.setEliteCreatures(elites);
                    worldPanel.repaint();
                });

                // Step all brainful creatures
                for (Creature c : brainful) {
                    Creature nearestFood = findNearestFood(c, food);
                    c.setNearestFood(nearestFood);
                    
                    
                    //Creature farthestMate = findFarthest(c, brainful.stream().filter(o -> o != c).toList());
                    Creature nearestMate = findNearestMate(c, brainful.stream().filter(o -> o != c).toList());
                    c.setNearestMate(nearestMate);

                    
                    c.step(nearestFood, nearestMate, SIM_WIDTH, SIM_HEIGHT);
                    c.maybeMutate();
                }

                // Eating
                for (Creature c : brainful) {
                    for (Creature f : food) {
                        if (c.x == f.x && c.y == f.y && f.health != -1) {
                            c.eat(f);
                            f.health = -1;
                        }
                    }
                }
                
                for (int i = 0; i < brainful.size(); i += 1) {
                    Creature a = brainful.get(i);
                    Creature b = a.getNearestMate();
                    if(a.gender == Creature.Gender.MALE && b != null && a.health > 0 && b.health > 0) {
                        int numKids = //a.health*10;
                        		random.nextInt(Creature.MIN_CHILDREN, Creature.MAX_CHILDREN);

                        for (int k = 0; k < numKids; k++) {
                        	
                            newGeneration.add(Creature.mate(a, b));
                        }
                    }
                }

                //Only elites mate
                //Collections.shuffle(elites, random);
                /*
                for (int i = 0; i + 1 < brainful.size(); i += 2) {
                    Creature a = brainful.get(i);
                    Creature b = brainful.get(i + 1);
                    
                    // Ensure a = male, b = female
                    if (a.gender == Creature.Gender.FEMALE && b.gender == Creature.Gender.MALE) {
                        Creature temp = a;
                        a = b;
                        b = temp;
                    }

                    if (a.gender != Creature.Gender.MALE || b.gender != Creature.Gender.FEMALE) {
                        continue; // skip invalid pairs (optional: you could retry instead)
                    }

                    //int averageEats = (a.getTimesEaten() + b.getTimesEaten()) / 2;
                    //int bonusChildren = averageEats;

                    int numKids = random.nextInt(Creature.MIN_CHILDREN, Creature.MAX_CHILDREN);

                    for (int k = 0; k < numKids; k++) {
                        newGeneration.add(Creature.mate(a, b));
                    }
                }*/
                
                // Mating (only if both are mature)
                //if(gen % 10 == 0) {
                /*
	                Set<String> alreadyMated = new HashSet<>();
	                for (int i = 0; i < brainful.size(); i++) {
	                    for (int j = i + 1; j < brainful.size(); j++) {
	                        Creature a = brainful.get(i);
	                        Creature b = brainful.get(j);
	                        if (a.isMature() && b.isMature() && a.getTimesEaten() > TIMES_TO_EAT_BEFORE_MATING && b.getTimesEaten() > TIMES_TO_EAT_BEFORE_MATING && distance(a, b) <= Creature.MATING_RADIUS) {
	                            String key = Math.min(a.hashCode(), b.hashCode()) + ":" + Math.max(a.hashCode(), b.hashCode());
	                            if (!alreadyMated.contains(key)) {
	                                alreadyMated.add(key);
	                                int averageEats = (a.getTimesEaten() + b.getTimesEaten()) / 2;
	                                int bonusChildren = averageEats; 
	
	                                int numKids = 
	                                		Math.min(Creature.MAX_CHILDREN,
	                                				Creature.MIN_CHILDREN + bonusChildren);   
	                                
	                                for (int k = 0; k < numKids; k++) {
	                                    newGeneration.add(Creature.mate(a, b));
	                                }
	                            }
	                        }
	                    }
	                }
	                */
                //}

                // Survivors (not eaten or dead)
                List<Creature> survivors = new ArrayList<>();
                for (Creature c : creatures) {
                    if (c.isFood && c.health != -1) survivors.add(c);
                    if (!c.isFood && !c.isDead()) survivors.add(c);
                }

                // Cap children to max allowed
                int allowed = MAX_TOTAL_POPULATION - survivors.size();
                if (newGeneration.size() > allowed) {
                    newGeneration = newGeneration.subList(0, Math.max(0, allowed));
                }

                // Add new food
                
                int currentFoodCount = filterByType(survivors, true).size();
                int foodToSpawn = Math.min(FOOD_PER_STEP, MAX_TOTAL_FOOD - currentFoodCount);

                for (int i = 0; i < foodToSpawn; i++) {
                	Creature f;
                	
                	if(random.nextDouble() < CHANCE_OF_POISON_PER_STEP) {
                		f = Creature.randomCreature(
                        		-(SIM_WIDTH / 2)+ (MARGIN+10), 
                        		(SIM_WIDTH / 2) - (MARGIN+10),
                                -(SIM_HEIGHT / 2) + (MARGIN+10), 
                                (SIM_HEIGHT / 2) - (MARGIN+10), true);
                		f.makePoison(true);
                	} else {
                		f = Creature.randomCreature(
                        		-(SIM_WIDTH / 2)+MARGIN, 
                        		(SIM_WIDTH / 2) - MARGIN,
                                -(SIM_HEIGHT / 2) + MARGIN, 
                                (SIM_HEIGHT / 2) - MARGIN, true);
                		f.makePoison(false);
                	}
                	
                    survivors.add(f);
                }
                
                
                
             // Spawn random mutants
                for (int i = 0; i < RANDOM_MUTANTS_PER_STEP; i++) {
                    survivors.add(Creature.randomCreature(
                        -(SIM_WIDTH / 2), (SIM_WIDTH / 2),
                        -(SIM_HEIGHT / 2), (SIM_HEIGHT / 2),
                        false)); // brainful = false
                }


                // Update population
                survivors.addAll(newGeneration);
                creatures.clear();
                creatures.addAll(survivors);

                resolveOverlaps(creatures, OVERLAP_DISTRIBUTION);
                
                // Visual update
                SwingUtilities.invokeLater(() -> {
                    worldPanel.setCreatures(creatures);
                    worldPanel.repaint();
                });
                
                if (brainful.isEmpty()) {
                    System.out.println("All brainful creatures extinct at generation " + gen);
                    break;
                }


                // Log
                /*
                System.out.println("Total: " + creatures.size() +
                        " | Brainful: " + filterByType(creatures, false).size() +
                        " | Mature: " + filterByType(creatures, false).stream().filter(Creature::isMature).count() +
                        " | Food: " + filterByType(creatures, true).size());
*/
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static List<Creature> filterByType(List<Creature> all, boolean isFood) {
        List<Creature> result = new ArrayList<>();
        for (Creature c : all) {
            if (c.isFood == isFood) result.add(c);
        }
        return result;
    }

    private static Creature findNearestFood(Creature seeker, List<Creature> targets) {
        Creature closest = null;
        double bestDist = Double.MAX_VALUE;
        for (Creature t : targets) {
            double d = Math.pow(t.x - seeker.x, 2) + Math.pow(t.y - seeker.y, 2);
            if (d < bestDist) {
                bestDist = d;
                closest = t;
            }
        }
        return closest;
    }
    
    private static Creature findNearestMate(Creature seeker, List<Creature> targets) {
        Creature closest = null;
        double bestDist = Double.MAX_VALUE;
        for (Creature t : targets) {
            double d = Math.pow(t.x - seeker.x, 2) + Math.pow(t.y - seeker.y, 2);
            if (d < bestDist && t.gender != seeker.gender) {
                bestDist = d;
                closest = t;
            }
        }
        return closest;
    }
    
    private static Creature findFarthest(Creature seeker, List<Creature> targets) {
        Creature closest = null;
        double bestDist = -Double.MAX_VALUE;
        for (Creature t : targets) {
            double d = Math.pow(t.x - seeker.x, 2) + Math.pow(t.y - seeker.y, 2);
            if (d > bestDist) {
                bestDist = d;
                closest = t;
            }
        }
        return closest;
    }
    
    private static double distance(Creature a, Creature b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    private static void resolveOverlaps(List<Creature> creatures, int radius) {
        Map<String, List<Creature>> positionMap = new HashMap<>();

        for (Creature c : creatures) {
            String key = c.x + "," + c.y;
            positionMap.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }

        for (List<Creature> group : positionMap.values()) {
            if (group.size() > 1) {
                for (Creature c : group) {
                	if(c.isFood) continue;
                	
                    int dx = random.nextInt(-radius, radius + 1);
                    int dy = random.nextInt(-radius, radius + 1);
                    c.x += dx;
                    c.y += dy;

                    // Clamp within bounds
                    c.x = Math.max(-(SIM_WIDTH / 2), Math.min(SIM_WIDTH / 2 - 1, c.x));
                    c.y = Math.max(-(SIM_HEIGHT / 2), Math.min(SIM_HEIGHT / 2 - 1, c.y));
                }
            }
        }
    }


}
