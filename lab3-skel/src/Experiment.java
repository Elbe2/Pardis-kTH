import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Experiment {
	private static final int MAX_NUMBER = 100_000;

	public static long run_experiment(int threads, int count, LockFreeSet<Integer> list, Distribution ops, Distribution values) throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(threads);

		Task[] tasks = new Task[threads];
		for (int i = 0; i < tasks.length; ++i) {
			tasks[i] = new Task(i, count, list, ops.copy(i), values.copy(-i));
		}

		long startTime = System.nanoTime();
		executorService.invokeAll(Arrays.asList(tasks));
		long endTime = System.nanoTime();
		executorService.shutdown();

		return endTime - startTime;
	}

	public static class Task implements Callable<Void> {
		private final int threadId, count;
		private final LockFreeSet<Integer> set;
		private final Distribution ops, values;

		public Task(int threadId, int count, LockFreeSet<Integer> set, Distribution ops, Distribution values) {
			this.threadId = threadId;
			this.set = set;
			this.ops = ops;
			this.values = values;
            this.count = count;
		}

		public Void call() throws Exception {
			for (int i = 0; i < count; ++i) {
				int val = values.next();
				int op = ops.next();
				switch (op) {
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

	public static void main(String[] args) {
		for (Distribution values : new Distribution[]{new Distribution.Uniform(84, 0, MAX_NUMBER), new Distribution.Normal(84,15,0,MAX_NUMBER)}) {
            for (int[] distr : new int[][]{new int[]{1, 1, 8}, new int[]{1, 1, 0}}) {
                for (int num_threads : new int[]{1, 2, 4, 8, 16, 32, 64, 96}) {
                    int warmup = 9;
                    for(int i=0;i<warmup+1;i++) {
                        try {
                            // Create a standard lock free skip list
                            LockFreeSet<Integer> lockFreeSet = new LockFreeSkipList<>();

                            // Create a discrete distribution with seed 42 such that,
                            // p(0) = 1/10, p(1) = 1/10, p(2) = 8/10.
                            Distribution ops = new Distribution.Discrete(42, distr);

                            // Run experiment with 16 threads.
                            long time = run_experiment(num_threads, MAX_NUMBER, lockFreeSet, ops, values);
                            if(i==warmup) // after warmup
                                System.out.printf("Took %f ms to finish for %d workers.\n", (time)/1_000_000.0f, num_threads);
                                
                            
                            // Get the log
                            Log.Entry[] log = lockFreeSet.getLog();

                            // Check sequential consistency
                            Log.validate(log);
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                }
            }
        }
    }
}