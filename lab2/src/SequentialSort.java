public class SequentialSort implements Sorter
{
    public SequentialSort()
    {}

    public void sort(int[] arr)
    {
        sort(arr, 0, arr.length - 1);
    }

    private void sort(int[] arr, int begin, int end)
    {
        if (end <= begin) // no elements to sort
            return;

        int split_position = Auxiliary.split(arr, begin, end);
        sort(arr, begin, split_position - 1);
        sort(arr, split_position + 1, end);
    }

    public int getThreads()
    {
        return 1;
    }
}
