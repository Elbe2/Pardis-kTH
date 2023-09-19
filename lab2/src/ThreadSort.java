/**
 * Sort using Java's Thread, Runnable, start(), and join().
 */

public class ThreadSort implements Sorter
{
    private final int threads;
    private int available_threads;

    public ThreadSort(int threads)
    {
        this.threads = threads;
        this.available_threads = threads;
    }

    public void sort(int[] arr)
    {
        available_threads = threads;
        sort(arr, 0, arr.length - 1);
    }

    private void sort(int[] arr, int begin, int end)
    {
        if (end <= begin) // no elements to sort
            return;

        int split_position = Auxiliary.split(arr, begin, end);
        if (available_threads > 1)
        {
            // Do balancing dependant on the size of subarrays:
            // Ceil is used to ensure that at least one new thread will be spawned.
            int giveaway_threads = (int) Math
                    .ceil((available_threads - 1.0) * (split_position - begin) / (end - begin));
            // Spawn new sorting thread with giveaway_threads available and the task to sort lower subarray:
            Thread t = new Thread(new Runnable()
            {
                public void run()
                {
                    new ThreadSort(giveaway_threads).sort(arr, begin, split_position - 1);
                }
            });
            t.start();
            // Now we update available threads and recurse:
            available_threads -= giveaway_threads;
            sort(arr, split_position + 1, end);
            try
            { // Each thread waits for its subthread (and, recursively, sub...subthreads) to finish:
                t.join();
            }
            catch (Exception e)
            {
                System.out.println("Exception: " + e);
            }
        }
        else
        {
            sort(arr, begin, split_position - 1);
            sort(arr, split_position + 1, end);
        }
    }

    public int getThreads()
    {
        return threads;
    }
}
