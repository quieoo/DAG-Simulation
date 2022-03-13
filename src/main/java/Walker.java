import java.util.Map;

/**
 * Walks the tangle from an entry point towards tips
 *
 */

public interface Walker {
    Integer walk(Integer entryPoint, Map<Integer, Integer> ratings) throws Exception;
}
