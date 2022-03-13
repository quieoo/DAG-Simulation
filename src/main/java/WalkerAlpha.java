import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of <tt>Walker</tt> that performs a weighted random walk
 * with <CODE>e^(alpha*Hy)</CODE> as the transition function.
 *
 */
public class WalkerAlpha implements Walker {

    /**
    * {@code alpha}: a positive number that controls the randomness of the walk.
    * The closer it is to 0, the less bias the random walk will be.
    */
    private double alpha;
    private final Random random;

    private final Tangle tangle;
    //private final Logger log = LoggerFactory.getLogger(Walker.class);


    public WalkerAlpha( Tangle tangle, Random random) {
        this.tangle = tangle;
        this.random = random;
    }

    /**
     * @return {@link WalkerAlpha#alpha}
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * @param alpha {@link WalkerAlpha#alpha}
     */
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    @Override
    public Integer walk(Integer entryPoint, Map<Integer, Integer> ratings) throws Exception {

        Optional<Integer> nextStep;
        Deque<Integer> traversedTails = new LinkedList<>();
        traversedTails.add(entryPoint);

        //Walk
        do {
            if(Thread.interrupted()){
                throw new InterruptedException();
            }
            nextStep = selectApprover(traversedTails.getLast(), ratings);
            nextStep.ifPresent(traversedTails::add);
         } while (nextStep.isPresent());

//        log.debug("{} tails traversed to find tip", traversedTails.size());
//        tangle.publish("mctn %d", traversedTails.size());
        return traversedTails.getLast();
    }

    private Optional<Integer> selectApprover(Integer tailHash, Map<Integer, Integer> ratings) throws Exception {
        Set<Integer> approvers = getApprovers(tailHash);
        return findNextValidTail(ratings, approvers);
    }

    private Set<Integer> getApprovers(Integer tailHash) throws Exception {
        return tangle.GetApprovers(tailHash);
    }

    private Optional<Integer> findNextValidTail(Map<Integer, Integer> ratings, Set<Integer> approvers) throws Exception {
        Optional<Integer> nextTailHash = Optional.empty();

        //select next tail to step to
        while (!nextTailHash.isPresent()) {
            Optional<Integer> nextTxHash = select(ratings, approvers);
            if (!nextTxHash.isPresent()) {
                //no existing approver = tip
                return Optional.empty();
            }

            nextTailHash = findTailIfValid(nextTxHash.get());
            approvers.remove(nextTxHash.get());
            //if next tail is not valid, re-select while removing it from approvers set
        }

        return nextTailHash;
    }

    private Optional<Integer> select(Map<Integer, Integer> ratings, Set<Integer> approversSet) {

        //filter based on tangle state when starting the walk
        List<Integer> approvers = approversSet.stream().filter(ratings::containsKey).collect(Collectors.toList());

        //After filtering, if no approvers are available, it's a tip.
        if (approvers.size() == 0) {
            return Optional.empty();
        }

        //calculate the probabilities
        List<Integer> walkRatings = approvers.stream().map(ratings::get).collect(Collectors.toList());

        Integer maxRating = walkRatings.stream().max(Integer::compareTo).orElse(0);
        //walkRatings.stream().reduce(0, Integer::max);

        //transition probability function (normalize ratings based on Hmax)
        List<Integer> normalizedWalkRatings = walkRatings.stream().map(w -> w - maxRating).collect(Collectors.toList());
        List<Double> weights = normalizedWalkRatings.stream().map(w -> Math.exp(alpha * w)).collect(Collectors.toList());

        //select the next transaction
        Double weightsSum = weights.stream().reduce(0.0, Double::sum);
        double target = random.nextDouble() * weightsSum;

        int approverIndex;
        for (approverIndex = 0; approverIndex < weights.size() - 1; approverIndex++) {
            target -= weights.get(approverIndex);
            if (target <= 0) {
                break;
            }
        }

        return Optional.of(approvers.get(approverIndex));
    }

    private Optional<Integer> findTailIfValid(Integer transactionHash) throws Exception {
        Optional<Integer> tailHash = Optional.ofNullable(transactionHash);
        return  tailHash;
}
}
