import java.util.Set;
import java.util.HashSet;
import java.util.Random;

// Testing that the LockFreeSet is implemented correctly
public class Testing {

	public static void main(String [] args)
	{
		Set<Integer> seqSet = new HashSet<>();
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
		}
	}
}
