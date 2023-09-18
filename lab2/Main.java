import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

class Sorting {
	public void sort(int[] arr, int max_threads)
	{
		sort(arr, 1, 1, max_threads, 0, arr.length-1);
	}

	private void swap(int[] arr, int a, int b) 
	{
		int temp = arr[a];
		arr[a] = arr[b];
		arr[b] = temp;
	}

	private void sort(int[] arr, int thread_nr, int current_threads,int max_threads, int begin, int end)
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

		if(current_threads+thread_nr <= max_threads && end-begin >16)
		{
			// can spawn new thread (and remaining length is big enough)
			Thread t = new Thread(new Runnable() 
			{
				public void run() 
				{
					// uncomment the following line to show that only max_threads-1 additional threads are spawned
//						System.out.println("Spawning new thread " + (current_threads + thread_nr) + " with current_threads = " + current_threads + " from thread_nr = " + thread_nr + " and end-begin = " + (split-begin));
					Sorting s = new Sorting();
					s.sort(arr, current_threads+thread_nr, current_threads*2, max_threads, begin, split-1); 
				}
			});
			t.start();
			sort(arr, thread_nr, current_threads*2, max_threads, split+1, end);
			try
			{
				t.join();
			}
			catch(Exception e)
			{}
		}
		else
		{
			//sorting without new threads
			sort(arr, thread_nr, current_threads, max_threads, begin, split-1);
			sort(arr, thread_nr, current_threads, max_threads, split+1, end);
		}
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

public class Main 
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
			sort.sort(cpy, max_threads);
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
			sort.sort(cpy, max_threads);
			long end_time = System.nanoTime();
			check(cpy, false);
			total_time+= end_time-begin_time;
		}
		System.out.println(real_runs + " runs took " + (total_time/1_000_000) + "ms on " + max_threads + " threads!");

	}


	public static void main(String args[])
	{
		// generate array of 10000000 integers
		arr_length = 10_000_000;
		for (int threads = 1; threads <= 64; threads*=2)
			measure(20, 10, threads, 1111);
	}
}
