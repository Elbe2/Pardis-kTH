import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorServiceSort implements Sorter
{
    private final int threads;
    private static final int threshold = 512;

    public ExecutorServiceSort(int threads)
    {
        this.threads = threads;
    }

    public void sort(int[] arr)
    {
        AtomicInteger pivots = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        executor.submit(new Worker(arr, 0, arr.length - 1, executor, pivots));
        while (pivots.get() < arr.length)
        {} // ensuring all pivot points are on their places
        executor.shutdown();
    }

    public int getThreads()
    {
        return threads;
    }

    private static class Worker implements Runnable
    {
        private int[] arr;
        private int begin;
        private int end;
        private ExecutorService executor;
        private AtomicInteger pivots;

        Worker(int[] arr, int begin, int end, ExecutorService executor, AtomicInteger pivots)
        {
            this.arr = arr;
            this.begin = begin;
            this.end = end;
            this.executor = executor;
            this.pivots = pivots;
        }

        public void run()
        {
            if (end == begin)
                pivots.incrementAndGet();
            if (end <= begin)
                return;

            int split_position = Auxiliary.split(arr, begin, end);
            pivots.incrementAndGet();

            boolean submit_lower = (split_position - 1) - begin > threshold;
            boolean submit_upper = end - (split_position + 1) > threshold;

            if (submit_lower)
                executor.submit(new Worker(arr, begin, split_position - 1, executor, pivots));
            if (submit_upper)
                executor.submit(new Worker(arr, split_position + 1, end, executor, pivots));

            if (!submit_lower)
            {
                int old = end;
                end = split_position - 1;
                run();
                end = old;
            }
            if (!submit_upper)
            {
                int old = begin;
                begin = split_position + 1;
                run();
                begin = old;
            }
        }
    }
}
