import java.util.Map;

/**
 * Calculates the rating for a sub graph
 */
@FunctionalInterface
public interface RatingCalculator {

    /**
     * Rating calculator
     * <p>
     * Calculates the rating of all the transactions that reference
     * a given {@code entryPoint}.
     * </p>
     *
     * @param entryPoint  Transaction Integer of a selected entry point.
     * @return Map of ratings for each transaction that references entryPoint.
     * @throws Exception If DB fails to retrieve transactions
     */

    Map<Integer, Integer> calculate(Integer entryPoint) throws Exception;
}
