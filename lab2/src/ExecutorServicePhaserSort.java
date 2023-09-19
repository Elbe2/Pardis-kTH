import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

public class ExecutorServicePhaserSort implements Sorter
{
    private final int threads;
    private static final int threshold = 512;
    private ExecutorService executor;

    public ExecutorServicePhaserSort(int threads)
    {
        this.threads = threads;
    }

    public void sort(int[] arr)
    {
        final Phaser phaser = new Phaser(1); // register self
        executor = Executors.newFixedThreadPool(threads);
        phaser.register();
        Runnable task = () ->
        {
            this.sort(arr, 0, arr.length - 1, phaser);
            phaser.arriveAndDeregister();
        };
        executor.execute(task);
        try
        {
            phaser.arriveAndAwaitAdvance();
            executor.shutdown(); // wait for all tasks to end and close
            executor.awaitTermination(10, TimeUnit.SECONDS); // this should never wait
        }
        catch (Exception e)
        {}
    }

    private void sort(int[] arr, int begin, int end, Phaser phaser)
    {
        if (end - begin <= 0) // only one (or zero) elements, return
            return;
        int split = Auxiliary.split(arr, begin, end);

        boolean manual_low = false;
        if ((split - 1) - begin > threshold)
        {
            //sorting without new threads
            final Phaser phaser2 = phaser.getRegisteredParties() > 60000 ? new Phaser(phaser) : phaser; // check if we have to many registered things in the phaser, if yes create a new one
            phaser2.register();
            Runnable task1 = () ->
            {
                this.sort(arr, begin, split - 1, phaser2);
                phaser2.arriveAndDeregister();
            };
            executor.execute(task1);
        }
        else
            manual_low = true;
        boolean manual_high = false;
        if (end - (split + 1) > threshold)
        {
            //sorting without new threads
            final Phaser phaser2 = phaser.getRegisteredParties() > 60000 ? new Phaser(phaser) : phaser; // check if we have to many registered things in the phaser, if yes create a new one
            phaser2.register();
            Runnable task2 = () ->
            {
                this.sort(arr, split + 1, end, phaser2);
                phaser2.arriveAndDeregister();
            };
            executor.execute(task2);
        }
        else
            manual_high = true;
        if (manual_low)
            this.sort(arr, begin, split - 1, phaser);
        if (manual_high)
            this.sort(arr, split + 1, end, phaser);
    }

    public int getThreads()
    {
        return threads;
    }
}
