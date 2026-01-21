import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * Sequential covering procedure that uses a GA to create rules.
 *
 * @author Fernando Otero
 * @version 1.0
 */
public class SeCoGA {
    /**
     * Number of bits of the individual encoding.
     */
    private static int BITS;

    /**
     * The population size.
     */
    private static final int POPULATION_SIZE = 100;
    
    /**
     * The number of generations.
     */
    private static final int MAX_GENERATION = 50;
    
    /**
     * Probability of the mutation operator.
     */
    private static final double MUTATION_PROBABILITY = 0.1;
    
    /**
     * Probability of the crossover operator.
     */
    private static final double CROSSOVER_PROBABILITY = 0.9;
    
    /**
     * Random number generation.
     */
    private Random random = new Random();
        
    /**
     * The current population.
     */
    private boolean[][] population;
    
    /**
     * Fitness values of each individual of the population.
     */
    private double[] fitness = new double[POPULATION_SIZE];
    
    /**
     * The training data.
     */
    private Dataset training;
    
    /**
     * The sequential covering procedure.
     * 
     * @param filename the dataset file name.
     */
    public void covering(String filename) throws IOException {
        training = new Dataset();
        
        ArrayList<boolean[]> data = training.read(filename);
        BITS = data.get(0).length;

        int minimum = (int) Math.ceil(data.size() * 0.1);

        boolean best[];
        
        do {
            // 1) finds a well performing rule using the GA
            best = run(data);
            
            // prints the rule
            
            // 2) removes covered instances
            for (Iterator<boolean[]> i = data.iterator(); i.hasNext(); ) {
                boolean[] instance = i.next();
                
                if (training.covers(best, instance)) {
                    i.remove();
                }
            }
            System.out.println(data.size());
            // 3) checks if we have remaining training data
        } while (data.size() >= minimum);
        System.out.println(training.toString(best));
    }
    
    // Genetic Algorithm -----------------------------------------------//
    
    /**
     * Starts the execution of the GA.
     */
    private boolean[] run(ArrayList<boolean[]> data) {
        //--------------------------------------------------------------//
        // initialises the population                                   //
        //--------------------------------------------------------------//
        initialise();
        
        //--------------------------------------------------------------//
        // evaluates the propulation                                    //
        //--------------------------------------------------------------//
        evaluate(data);
        
        for (int g = 0; g < MAX_GENERATION; g++) {
            //----------------------------------------------------------//
            // creates a new population                                 //
            //----------------------------------------------------------//
            
            boolean[][] newPopulation = new boolean[POPULATION_SIZE][BITS];
            // index of the current individual to be created
            int current = 0;
            
            while (current < POPULATION_SIZE) {
                double probability = random.nextDouble();
                
                // should we perform mutation?
                if (probability <= MUTATION_PROBABILITY || (POPULATION_SIZE - current) == 1) {
                    int parent = select();

                    boolean[] offspring = mutation(parent);
                    // copies the offspring to the new population
                    newPopulation[current] = offspring;
                    current += 1;
                }
                // otherwise we perform a crossover
                else {
                    int first = select();
                    int second = select();

                    boolean[][] offspring = crossover(first, second);
                    // copies the offspring to the new population
                    newPopulation[current] = offspring[0];
                    current += 1;
                    newPopulation[current] = offspring[1];
                    current += 1;
                }
            }
            
            population = newPopulation;
            
            //----------------------------------------------------------//
            // evaluates the new population                             //
            //----------------------------------------------------------//
            evaluate(data);
        }
        
        // prints the value of the best individual
        int best = 0;
        
        for (int i = 1; i < POPULATION_SIZE; i++) {
            if (fitness[best] < fitness[i]) {
                best = i;
            }
        }
        
        return population[best];
    }
    
    /**
     * Retuns the index of the selected parent.
     * 
     * @return the index of the selected parent.
     */
    private int select() {
        int TOURNAMENT_SIZE = POPULATION_SIZE;

        boolean selected[] = new boolean[POPULATION_SIZE];
        int[] tournament = new int[TOURNAMENT_SIZE];

        for(int i = 0; TOURNAMENT_SIZE > i; i++) {
            int chosen = -1;

            while(chosen == -1) {
                chosen = random.nextInt(POPULATION_SIZE);
                if(!selected[chosen]) {
                    tournament[i] = chosen;
                    selected[chosen] = true;
                } else {
                    chosen = -1;
                }
            }
        }
        int winner = 0;
        for(int i = 1; TOURNAMENT_SIZE > i; i++) {
            if(fitness[tournament[winner]] < fitness[tournament[i]]){
                winner = i;
            }
        }
        return tournament[winner];
    }
    
    /**
     * Initialises the population.
     */
    private void initialise() {
        population = new boolean[POPULATION_SIZE][BITS];
        for(int i = 0; POPULATION_SIZE > i; i++) {
            for(int j = 0; BITS > j; j++) {
                population[i][j] = random.nextBoolean();
            }
        }
    }
    
    /**
     * Calculates the fitness of each individual.
     */
    private void evaluate(ArrayList<boolean[]> data) {
        for(int i = 0; POPULATION_SIZE > i; i++) {
            int tp = 0, tn = 0, fp = 0, fn = 0;
            for(int j = 0; data.size() > j; j++) {
                boolean cover = training.covers(population[i], data.get(j));
                boolean target = training.target(data.get(j));

                //Conditional checks
                if(cover) {
                    if(target) tp++;
                    else fp++;
                } else {
                    if(target) fn++;
                    else tn++;
                }
            }
            double sensitivity = (tp==0 && fn==0) ? 0 : tp/(tp+fn);
            double specificity = (fp==0 && tn==0) ? 0 : tn/(fp+tn);
            fitness[i] = specificity * sensitivity;
        }
    }
    
    /**
     * Point mutation operator.
     * 
     * @param parent index of the parent individual from the population.
     * //@param newPopulation the new population.
     * //@param current index of the individual being created in the new population.
     */
    private boolean[] mutation(int parent) {
        boolean[] offspring = new boolean[BITS];
        int point = random.nextInt(BITS);
        
        for (int i = 0; i < BITS; i++) {
            if (i == point) {
                offspring[i] = random.nextBoolean();
            }
            else {
                offspring[i] = population[parent][i];
            }
        }
        return offspring;
    }
    
    /**
     * One-point crossover operator. Note that the crossover generates two offsprings,
     * so both current and current+1 position in the new population must be filled.
     * 
     * @param first index of the first parent individual from the population.
     * @param second index of the second parent individual from the population.
     */
    private boolean[][] crossover(int first, int second) {
        boolean[][] offspring = new boolean[2][BITS];
        int point = random.nextInt(BITS);
        
        for (int i = 0; i < BITS; i++) {
            if (i == point) {
                int k = first;
                first = second;
                second = k;
            }
            offspring[0][i] = population[first][i];
            offspring[1][i] = population[second][i];

        }
        return offspring;
    }
}