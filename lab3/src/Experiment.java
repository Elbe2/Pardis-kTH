import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.io.BufferedWriter;
import java.io.FileWriter;
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
            System.out.println("Usage: java Experiment <scenario> [<valSeed> <opsSeed>]");
            System.out.println("scenario can be either 'local' or 'dardel'");
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
            System.out.println("Usage: java Experiment <scenario> [<valSeed> <opsSeed>]");
            System.out.println("scenario can be either 'local' or 'dardel'");
            return;
        }
        Distribution[] val_distrs = new Distribution[] { new Distribution.Uniform(val_seed, 0, count),
                new Distribution.Normal(val_seed, 15, 0, count) };
        int[][] distrs = new int[][] { new int[] { 1, 1, 8 }, new int[] { 1, 1, 0 } };
        for (int logging = 0; logging != 4; ++logging)
        {
            for (int i = 0; i != WARMUPS; ++i)
            {
                try
                {
                    LockFreeSet<Integer> lockFreeSet = newList(logging, 1);
                    Distribution ops = new Distribution.Discrete(ops_seed, distrs[0]);
                    run_experiment(1, count, lockFreeSet, ops, val_distrs[0]);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
        for (int logging = 0; logging != 4; ++logging)
        {
            for (int values = 0; values != val_distrs.length; ++values)
            {
                for (int distr = 0; distr != distrs.length; ++distr)
                {
                    for (int num_threads : nums)
                    {
                        double[] times = new double[MEASURMENTS];
                        int total_wrong = 0;
                        for (int i = 0; i < MEASURMENTS; i++)
                        {
                            try
                            {
                                LockFreeSet<Integer> lockFreeSet = newList(logging, num_threads);
                                Distribution ops = new Distribution.Discrete(ops_seed, distrs[distr]);
                                long time = run_experiment(num_threads, count, lockFreeSet, ops, val_distrs[values]);

                                times[i] = (double) time / 1_000_000.0; // get times in ms, so we can actually interpret them

                                Log.Entry[] log = lockFreeSet.getLog();

                                // Check sequential consistency
                                if (log != null)
                                {
                                    int wrong = Log.validate(log);
                                    total_wrong += wrong;
                                    if (wrong != 0)
                                    {
                                        System.err.println(i + ": " + wrong + " are wrong out of " + log.length);
                                        // Save logs with errors to files:
                                        BufferedWriter writer = new BufferedWriter(new FileWriter(
                                                logging + "-" + values + "-" + distr + "-" + num_threads + "-" + i,
                                                false));
                                        for (Log.Entry entry : log)
                                        {
                                            writer.write(entry.toString());
                                            writer.newLine();
                                        }
                                        writer.close();
                                    }
                                    if (log.length != num_threads * count)
                                        System.err.println(i + ": " + "Log size is " + log.length + " instead of "
                                                + num_threads * count);
                                }
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
                                + distrToString(distr) + ", " + valuesToString(values) + " values, "
                                + loggingToString(logging) + " logging.\n");
                    }
                }
            }
        }
    }

    private static LockFreeSet<Integer> newList(int logging, int num_threads)
    {
        switch (logging)
        {
        case 0:
            return new LockFreeSkipListPure<Integer>();
        case 1:
            return new LockFreeSkipList<Integer>();
        case 2:
            return new LockFreeSkipListLocal<Integer>(num_threads);
        case 3:
            return new LockFreeSkipListGlobal<Integer>();
        default:
            throw new RuntimeException("Unknown logging: " + logging);
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

    private static String loggingToString(int logging)
    {
        switch (logging)
        {
        case 0:
            return "no";
        case 1:
            return "lock";
        case 2:
            return "local";
        case 3:
            return "global";
        default:
            return "???";
        }
    }
}
