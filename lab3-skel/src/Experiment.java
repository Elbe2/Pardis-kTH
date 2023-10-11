import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.Arrays;

public class Experiment
{
    private static final int WARMUPS = 10;
    private static final int MEASURMENTS = 10;

    public static long run_experiment(int threads, int count, LockFreeSet<Integer> list, Distribution ops,
            Distribution values) throws Exception
    {
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        Task[] tasks = new Task[threads];
        for (int i = 0; i < tasks.length; ++i)
        {
            tasks[i] = new Task(i, count, list, ops.copy(i), values.copy(-i));
        }

        long startTime = System.nanoTime();
        executorService.invokeAll(Arrays.asList(tasks));
        long endTime = System.nanoTime();
        executorService.shutdown();

        return endTime - startTime;
    }

    public static class Task implements Callable<Void>
    {
        private final int threadId, count;
        private final LockFreeSet<Integer> set;
        private final Distribution ops, values;

        public Task(int threadId, int count, LockFreeSet<Integer> set, Distribution ops, Distribution values)
        {
            this.threadId = threadId;
            this.set = set;
            this.ops = ops;
            this.values = values;
            this.count = count;
        }

        public Void call() throws Exception
        {
            for (int i = 0; i < count; ++i)
            {
                int val = values.next();
                int op = ops.next();
                switch (op)
                {
                case 0:
                    set.add(threadId, val);
                    break;
                case 1:
                    set.remove(threadId, val);
                    break;
                case 2:
                    set.contains(threadId, val);
                    break;
                }
            }
            return null;
        }
    }

    public static void main(String[] args)
    {
        int val_seed, ops_seed;
        if (args.length == 3)
        {
            val_seed = Integer.parseInt(args[1]);
            ops_seed = Integer.parseInt(args[2]);
        }
        else if (args.length == 1)
        {
            val_seed = (int) System.nanoTime();
            ops_seed = (int) System.nanoTime();
            System.out.println("Values seed: " + val_seed);
            System.out.println("Operations seed: " + ops_seed);
        }
        else
        {
            System.err.println("Usage: java Experiment <scenario> [<valSeed> <opsSeed>]");
            System.err.println("scenario can be either 'local' or 'dardel'");
            return;
        }
        int[] nums;
        int count;
        if (args[0].equals("local"))
        {
            nums = new int[] { 1, 2, 4, 8 };
            count = 100_000;
        }
        else if (args[0].equals("dardel"))
        {
            nums = new int[] { 1, 2, 4, 8, 16, 32, 64, 96 };
            count = 1_000_000;
        }
        else
        {
            System.err.println("Usage: java Experiment <scenario> [<valSeed> <opsSeed>]");
            System.err.println("scenario can be either 'local' or 'dardel'");
            return;
        }
        Distribution[] val_distrs = new Distribution[] { new Distribution.Uniform(val_seed, 0, count),
                new Distribution.Normal(val_seed, 15, 0, count) };
        int[][] distrs = new int[][] { new int[] { 1, 1, 8 }, new int[] { 1, 1, 0 } };
        for (int values = 0; values != val_distrs.length; ++values)
        {
            for (int distr = 0; distr != distrs.length; ++distr)
            {
                for (int num_threads : nums)
                {
                    double[] times = new double[MEASURMENTS];
                    int total_wrong = 0;
                    for (int i = 0; i < WARMUPS + MEASURMENTS; i++)
                    {
                        try
                        {
                            // Create a standard lock free skip list
                            LockFreeSet<Integer> lockFreeSet = new LockFreeSkipList<>();

                            // Create a discrete distribution with seed 42 such that,
                            // p(0) = 1/10, p(1) = 1/10, p(2) = 8/10.
                            Distribution ops = new Distribution.Discrete(ops_seed, distrs[distr]);

                            // Run experiment with 16 threads.
                            long time = run_experiment(num_threads, count, lockFreeSet, ops, val_distrs[values]);
                            if (i < WARMUPS)
                                continue;

                            times[i - WARMUPS] = (double) time / 1_000_000.0; // get times in ms, so we can actually interpret them

                            // Get the log
                            Log.Entry[] log = lockFreeSet.getLog();

                            // Check sequential consistency
                            int wrong = Log.validate(log);
                            total_wrong += wrong;
                            if (wrong != 0)
                                System.err.println(i - WARMUPS + ": " + wrong + " are wrong out of " + log.length);
                            if (log.length != num_threads * count)
                                System.err.println(i - WARMUPS + ": " + "Log size is " + log.length + " instead of "
                                        + num_threads * count);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                    // calc std_dev and mean etc:
                    // mean:
                    double mean = Arrays.stream(times).average().getAsDouble();
                    // standard deviation:
                    double std_dev = Math
                            .sqrt(Arrays.stream(times).map(x -> Math.pow(x - mean, 2)).average().getAsDouble());
                    if (total_wrong != 0)
                        System.out.println(
                                "Total wrong: " + total_wrong + " out of " + num_threads * count * MEASURMENTS);
                    System.out.println("Took " + mean + "ms (std=" + std_dev + ") for " + num_threads + " workers, "
                            + distrToString(distr) + ", " + valuesToString(values) + " values.\n");
                }
            }
        }
    }

    private static String distrToString(int distr)
    {
        switch (distr)
        {
        case 0:
            return "10% add, 10% remove, 80% contains";
        case 1:
            return "50% add, 50% remove, no contains";
        default:
            return "???";
        }
    }

    private static String valuesToString(int distr)
    {
        switch (distr)
        {
        case 0:
            return "uniform";
        case 1:
            return "normal";
        default:
            return "???";
        }
    }
}
