import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class ForkJoinPoolSort implements Sorter
{
    public final int threads;

    public ForkJoinPoolSort(int threads)
    {
        this.threads = threads;
    }

    public void sort(int[] arr)
    {
        ForkJoinPool pool = new ForkJoinPool(threads);
        pool.invoke(new Worker(arr, 0, arr.length - 1));
        pool.shutdown();
    }

    public int getThreads()
    {
        return threads;
    }

    private static class Worker extends RecursiveAction
    {
        private int[] arr;
        private int begin;
        private int end;

        Worker(int[] arr, int begin, int end)
        {
            this.arr = arr;
            this.begin = begin;
            this.end = end;
        }

        protected void compute()
        {
            if (end <= begin)
                return;

            int split_position = Auxiliary.split(arr, begin, end);
            invokeAll(new Worker(arr, begin, split_position - 1), new Worker(arr, split_position + 1, end));
        }
    }
}
