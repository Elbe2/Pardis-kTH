import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorServiceSort implements Sorter
{
    public final int threads;

    public ExecutorServiceSort(int threads)
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

    private static class Worker implements Runnable
    {
        Worker()
        {}

        public void run()
        {}
    }
}
