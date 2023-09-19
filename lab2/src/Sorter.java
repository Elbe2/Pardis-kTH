/**
 * Interface for the sorting algorithms
 */

public interface Sorter
{
    /** Sort 'arr' */
    public void sort(int[] arr);

    /** Get the number of threads used by the implementation. */
    public int getThreads();
}
