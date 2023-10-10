import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

// Testing that the LockFreeSet is implemented correctly
public class Testing2 {

	public static void main(String [] args)
	{
/*		Set<Integer> seqSet = new HashSet<>();
		LockFreeSet<Integer> lockFreeSet = new LockFreeSkipList<>();

		Random rng = new Random();
		for (int i = 0; i < 1000000; ++i) {
			int val = rng.nextInt(1000);
			int op = rng.nextInt(3);
			String opName;
			boolean resSeq, resLockFree;
			if (op == 0) {
				resSeq = seqSet.add(val);
				resLockFree = lockFreeSet.add(val);
				opName = "add";
			} else if (op == 1) {
				resSeq = seqSet.remove(val);
				resLockFree = lockFreeSet.remove(val);
				opName = "remove";
			} else {
				resSeq = seqSet.contains(val);
				resLockFree = lockFreeSet.contains(val);
				opName = "contains";
			}
			if (resSeq == resLockFree)
				continue;
			System.out.println(": value of lock free (" + resLockFree + ") not matching sequential (" + resSeq + ")");
		}*/
        for(int i=1;i<16;i*=2)
            for(int j=0;j<10;j++)
                run(i,false);
            run(i);
        System.out.println("Worked, hurraay!");
	}
    private static void run(int num_workers, boolean print)
    {
		LockFreeSet<Integer> lockFreeSet = new LockFreeSkipList<>();
		ExecutorService serv = new ForkJoinPool(num_workers);
		Distribution sample_dist = new Distribution.Uniform(1, 0, 100_000);
		Distribution op_dist = new Distribution.Discrete(1,  new int[]{1, 1, 8});
		List<Callable<Void>> workers = new ArrayList<>();
		for(int i = 0; i < num_workers; i++)
			workers.add(new Worker(sample_dist.copy(i), op_dist.copy(i), lockFreeSet));
        long endtime = 0 , starttime = 0;
		try
		{
            starttime = System.nanoTime();
			serv.invokeAll(workers);
			// for (Future<Void> future : futures)
			// 	future.get(); // let all threads finish
            endtime = System.nanoTime();
		}
		catch(Exception e ){}
        if(print)
            System.out.printf("Took %f ns to finish for %d workers.\n", (endtime-starttime)/1_000_000.0f, num_workers);        
    }
}
class Worker implements Callable<Void> {
	private final Distribution distribution;
	private final Distribution op_dist;
	private final LockFreeSet<Integer> skiplist;

	public Worker(Distribution dist, Distribution opdist, LockFreeSet<Integer> skiplist) {
		this.distribution = dist;
		this.op_dist = opdist;
		this.skiplist = skiplist;
	}

	@Override
	public Void call() {
		int max_op = 100_000;
		int next;
		for(int i=0;i<max_op;i++)
		{
			next = this.distribution.next();
			switch(this.op_dist.next())
			{
			case 0:
				skiplist.add(next);
				break;
			case 1:
				skiplist.remove(next);
				break;
			case 2:
				skiplist.contains(next);
				break;
			}

		}
		return null; // the fuck?
	}
}

