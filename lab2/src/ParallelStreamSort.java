
/**
 * Sort using Java's ParallelStreams and Lambda functions.
 *
 * Hints: - Do not take advice from StackOverflow. - Think outside the box. -
 * Stream of threads? - Stream of function invocations?
 *
 * By default, the number of threads in parallel stream is limited by the number
 * of cores in the system. You can limit the number of threads used by parallel
 * streams by wrapping it in a ForkJoinPool. ForkJoinPool myPool = new
 * ForkJoinPool(threads); myPool.submit(() -> "my parallel stream method /
 * function");
 */

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;

public class ParallelStreamSort implements Sorter
{
    public final int threads;
    private static final int threshold = 512;

    private Collection<int[]> todo;

    public ParallelStreamSort(int threads)
    {
        this.threads = threads;
    }

    public void sort(int[] arr)
    {
        todo = new ConcurrentLinkedQueue<int[]>();
        todo.add(new int[] { 0, arr.length - 1 });
        try
        {
            ForkJoinPool pool = new ForkJoinPool(threads);
            pool.submit(() -> {
                while (todo.size() > 0)
                {
                    Collection<int[]> old_todo = todo;
                    todo = new ConcurrentLinkedQueue<int[]>();

                    old_todo.parallelStream().forEach((int[] range) -> {
                        int begin = range[0];
                        int end = range[1];

                        if (end <= begin)
                            return;

                        if (end - begin < threshold)
                        {
                            new SequentialSort().sort(arr, begin, end);
                            return;
                        }

                        int split_position = Auxiliary.split(arr, begin, end);

                        todo.add(new int[] { begin, split_position - 1 });
                        todo.add(new int[] { split_position + 1, end });
                    });
                }
            }).get();
            pool.shutdown();
        }
        catch (Exception e)
        {}
    }

    public int getThreads()
    {
        return threads;
    }
}
