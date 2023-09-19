import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

class Sorting {
	private ExecutorService executor;
	private int min_split_size = 512;
	public void sort(int[] arr, int max_threads, Sorting self)
	{
		final Phaser phaser = new Phaser(1); // register self
		executor = Executors.newFixedThreadPool(max_threads);
		phaser.register();
		Runnable task = () -> {
			this.sort(arr, 0, arr.length-1, phaser);
			phaser.arriveAndDeregister();
		};
		executor.execute(task);
		try
		{
			phaser.arriveAndAwaitAdvance();
			executor.shutdown(); // wait for all tasks to end and close
			executor.awaitTermination(10, TimeUnit.SECONDS); // this should never wait
		}
		catch(Exception e){}
	}

	private void swap(int[] arr, int a, int b) 
	{
		int temp = arr[a];
		arr[a] = arr[b];
		arr[b] = temp;
	}

	private void sort(int[] arr, int begin, int end, Phaser phaser)
	{
		if (end-begin < 3)
		{
			if(end-begin <= 0) // only one (or zero) elements, return
				return;
			if(end-begin == 1) // only two elements, check if in order, then return
				if(arr[begin] > arr[end])
				{
					swap(arr, begin, end);
					return;
				}
				else
					return;
			// three elements, check twice
			if (arr[begin] > arr[end-1])
				swap(arr, begin, end-1);
			if (arr[begin+1] > arr[end])
				swap(arr, begin+1, end);
			if (arr[begin] > arr[end-1])
				swap(arr, begin, end-1);
			return;
		}
		int split = step(arr, begin, end);

		boolean manual_low = false;
		if (split-1-begin > min_split_size)
		{
			//sorting without new threads
			final Phaser phaser2 = phaser.getRegisteredParties() > 60000 ? new Phaser(phaser) : phaser; // check if we have to many registered things in the phaser, if yes create a new one
			phaser2.register();
			Runnable task1 = () -> {
				this.sort(arr, begin, split-1, phaser2);
				phaser2.arriveAndDeregister();
			};
			executor.execute(task1);
		}
		else
			manual_low = true;
		boolean manual_high = false;
		if (end-split+1 > min_split_size)
		{
			//sorting without new threads
			final Phaser phaser2 = phaser.getRegisteredParties() > 60000 ? new Phaser(phaser) : phaser; // check if we have to many registered things in the phaser, if yes create a new one
			phaser2.register();
			Runnable task2 = () -> {
				this.sort(arr, split+1, end, phaser2);
				phaser2.arriveAndDeregister();
			};
			executor.execute(task2);
		}
		else
			manual_high = true;
		if(manual_low)
			this.sort(arr, begin, split-1, phaser);
		if(manual_high)
			this.sort(arr, split+1, end, phaser);
	}

	public int step(int[] arr, int begin, int end) 
	{
		int pivot = arr[end];

		int split = begin - 1; // value where the split occurs of higher and lower values than pivot

		for (int i = begin; i < end; i++) 
		{
			if (arr[i] <= pivot)
			{
				swap(arr, i, split+1);
				split++;
			}
		}
		swap(arr, end, split+1); // put pivot at correct position
		return split+1;
	}
}

public class task4 
{
	private static int arr_length = 10;
	public static void print(int[] arr)
	{
		for(int i = 0;i<100;i++)
			System.out.print(arr[i] + " ");
		System.out.println();
	}
	
	private static void generate(int[] arr, long seed)
	{
		Random random = new Random(seed);
		
		for (int i = 0; i < arr_length; i++) {
			arr[i] = random.nextInt();
		}
	}

	private static void check(int[] arr, boolean only_unsorted)
	{
		boolean sorted = true;
		for(int i = 0; i < arr_length-1;i++)
			if(arr[i] > arr[i+1])
				sorted = false;
		if(!sorted)
			System.out.println("Array is not sorted!!!");
		else if (!only_unsorted)
			System.out.println("Array is sorted!!!");
	}

	public static void measure(int warmup_runs, int real_runs, int max_threads, int seed)
	{
		int[] cpy = new int[arr_length];
		Sorting sort = new Sorting();
		System.out.print("warmup");
		for (int i=0;i<warmup_runs;i++)
		{
			generate(cpy, seed+i);
			sort.sort(cpy, max_threads, sort);
			System.out.print(".");
			check(cpy, true);
		}
		System.out.println();
		// real runs
		long total_time = 0;
		for (int i=0;i<real_runs;i++)
		{
			generate(cpy, seed+i);
			long begin_time = System.nanoTime();
			sort.sort(cpy, max_threads, sort);
			long end_time = System.nanoTime();
			check(cpy, true);
			total_time+= end_time-begin_time;
		}
		System.out.println(real_runs + " runs took " + (total_time/1_000_000) + "ms on " + max_threads + " threads!");

	}


	public static void main(String args[])
	{
		// generate array of 10000000 integers
		arr_length = 1_000_000;
		for (int threads = 1; threads <= 64; threads*=2)
			measure(20, 10, threads, 1111);
	}
}
