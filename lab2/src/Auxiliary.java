import java.util.Arrays;
import java.util.Random;

public class Auxiliary
{
    /**
     * Generate a pseudo-random array of length `n`.
     */
    public static int[] arrayGenerate(int seed, int n)
    {
        Random prng = new Random(seed);
        int[] arr = new int[n];
        for (int i = 0; i < n; ++i)
            arr[i] = prng.nextInt();
        return arr;
    }

    public static void swap(int[] arr, int a, int b)
    {
        int temp = arr[a];
        arr[a] = arr[b];
        arr[b] = temp;
    }

    public static int split(int[] arr, int begin, int end)
    {
        int pivot = arr[end];
        int split_position = begin;

        for (int i = begin; i < end; i++)
        {
            if (arr[i] <= pivot)
            {
                swap(arr, i, split_position);
                split_position++;
            }
        }
        // put the pivot between lower and greater subarrays:
        swap(arr, end, split_position);
        return split_position;
    }

    /**
     * Measures the execution time of the 'sorter'.
     * 
     * @param sorter   Sorting algorithm
     * @param n        Size of list to sort
     * @param initSeed Initial seed used for array generation
     * @param m        Measurment rounds.
     * @return result[0]: average execution time result[1]: standard deviation
     *         of execution time
     */
    public static double[] measure(Sorter sorter, int n, int initSeed, int m)
    {
        double[] measurements = new double[m];
        for (int i = 0; i < m; ++i)
        {
            int[] arr = arrayGenerate(initSeed + i, n);
            long startTime = System.nanoTime();
            sorter.sort(arr);
            long endTime = System.nanoTime();
            // nanoseconds to milliseconds:
            measurements[i] = (endTime - startTime) / 1_000_000.0;
        }
        double[] result = new double[2];
        // mean:
        result[0] = Arrays.stream(measurements).average().getAsDouble();
        // standard deviation:
        result[1] = Math.sqrt(Arrays.stream(measurements).map(x -> Math.pow(x - result[0], 2)).average().getAsDouble());
        return result;
    }

    /**
     * Checks that the 'sorter' sorts.
     * 
     * @param sorter   Sorting algorithm
     * @param n        Size of list to sort
     * @param initSeed Initial seed used for array generation
     * @param m        Number of attempts.
     * @return True if the sorter successfully sorted all generated arrays.
     */
    public static boolean validate(Sorter sorter, int n, int initSeed, int m)
    {
        for (int i = 0; i < m; ++i)
        {
            int[] arr = arrayGenerate(initSeed + i, n);
            sorter.sort(arr);
            for (int j = 1; j < n; ++j)
                if (arr[j - 1] > arr[j])
                    return false;
        }
        return true;
    }
}
