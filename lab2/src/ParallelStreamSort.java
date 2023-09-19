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

public class ParallelStreamSort implements Sorter
{
    public final int threads;

    public ParallelStreamSort(int threads)
    {
        this.threads = threads;
    }

    public void sort(int[] arr)
    {
        // TODO: sort arr.
    }

    public int getThreads()
    {
        return threads;
    }
}
