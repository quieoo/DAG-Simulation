
import java.util.*;

/**
 * Implementation of {@link RatingCalculator} that calculates the cumulative weight
 * Calculates the weight recursively/on the fly for each transaction referencing {@code entryPoint}. <br>
 * Works using DFS search for new Integeres and a BFS calculation.
 * Uses cached values to prevent double database lookup for approvers
 */
public class CumulativeWeightCalculator implements RatingCalculator {
    public final Tangle tangle;
    public CumulativeWeightCalculator(Tangle tangle) {
        this.tangle = tangle;
    }

    @Override
    public Map<Integer, Integer> calculate(Integer entryPoint) throws Exception {

        return calculateRatingDfs(entryPoint);
    }

    private Map<Integer, Integer> calculateRatingDfs(Integer entryPoint) throws Exception {
        int depth = 1;
        // Estimated capacity per depth, assumes 5 minute gap in between milestones, at 3tps
        Map<Integer, Integer> IntegerWeightMap = createTxIntegerToCumulativeWeightMap( 5 * 60 * 3 * depth);

        Map<Integer, Set<Integer>> txToDirectApprovers = new HashMap<>();

        Deque<Integer> stack = new ArrayDeque<>();
        stack.addAll(getTxDirectApproversIntegeres(entryPoint, txToDirectApprovers));

        while (!stack.isEmpty()) {
            Integer txInteger = stack.pollLast();

            Set<Integer> approvers = getTxDirectApproversIntegeres(txInteger, txToDirectApprovers);

            // If its empty, its a tip!
            if (approvers.isEmpty()) {
                IntegerWeightMap.put(txInteger, 1);

            // Else we go deeper
            } else {
                // Add all approvers, given we didnt go there
                for (Integer h : approvers) {
                    if (!IntegerWeightMap.containsKey(h)) {
                        stack.add(h);
                    }
                }

                // Add the tx to the approvers list to count itself as +1 weight, preventing self-referencing
                approvers.add(txInteger);

                // calculate and add rating. Naturally the first time all approvers need to be looked up. Then its cached.
                IntegerWeightMap.put(txInteger, getRating(approvers, txToDirectApprovers));
            }
        }

        // If we have a self-reference, its already added, otherwise we save a big calculation
        if (!IntegerWeightMap.containsKey(entryPoint)) {
            IntegerWeightMap.put(entryPoint, IntegerWeightMap.size() + 1);
        }
        return IntegerWeightMap;
    }

    /**
     * Gets the rating of a set, calculated by checking its approvers
     *
     * @param startingSet All approvers of a certain Integer, including the Integer itself.
     *                    Should always start with at least 1 Integer.
     * @param txToDirectApproversCache The cache of approvers, used to prevent double db lookups
     * @return The weight, or rating, of the starting Integer
     * @throws Exception If we can't get the approvers
     */
    private int getRating(Set<Integer> startingSet, Map<Integer, Set<Integer>> txToDirectApproversCache) throws Exception {
        Deque<Integer> stack = new ArrayDeque<>(startingSet);
        while (!stack.isEmpty()) {
            Set<Integer> approvers = getTxDirectApproversIntegeres(stack.pollLast(), txToDirectApproversCache);
            for (Integer Integer : approvers) {
                if (startingSet.add(Integer)) {
                    stack.add(Integer);
                }
            }
        }

        return startingSet.size();
    }
    private Set<Integer> getTxDirectApproversIntegeres(Integer txInteger, Map<Integer, Set<Integer>> txToDirectApprovers)
            throws Exception {

        Set<Integer> txApprovers = txToDirectApprovers.get(txInteger);
        if (txApprovers == null) {
            Set<Integer> appIntegeres=tangle.GetApprovers(txInteger);

            txApprovers = new HashSet<>(appIntegeres.size());
            for (Integer appInteger : appIntegeres) {
                txApprovers.add(appInteger);
            }
            txToDirectApprovers.put(txInteger, txApprovers);
        }

        return new HashSet<Integer>(txApprovers);
    }

    private static Map<Integer, Integer> createTxIntegerToCumulativeWeightMap(int size) {
        return new HashMap<Integer, Integer>(size); //new TransformingMap<>(size, IntegerPrefix::createPrefix, null);
    }
}
