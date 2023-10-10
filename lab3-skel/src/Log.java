import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Log
{
    private Log()
    {
        // Do not implement
    }

    public static boolean validate(Log.Entry[] log, int num_threads)
    {
        Set<Integer> seqSet = new HashSet<>();
        int wrong = 0;

        // sort log entries so timestampt is in order
        Entry[] sortedLog = Arrays.copyOf(log, log.length);
        Arrays.sort(sortedLog, Comparator.comparingLong(entry -> entry.timestamp));

        int i = 0;
        for (Log.Entry logi : sortedLog)
        {
            int val = Integer.parseInt(logi.argument); // TODO think
            boolean resSeq;
            switch (logi.method)
            {
            case ADD:
                resSeq = seqSet.add(val);
                break;
            case REMOVE:
                resSeq = seqSet.remove(val);
                break;
            case CONTAINS:
                resSeq = seqSet.contains(val);
                break;
            default:
                throw new RuntimeException("Unknown method: " + logi.method);
            }
            i++;
            if (resSeq == logi.retval)
                continue;
            System.out.println(i + " " + logi.method.toString() + "(" + logi.argument + "): value of lock free ("
                    + logi.retval + ") not matching sequential (" + resSeq + ")");

            wrong += 1;

        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("file-"+num_threads+".txt", false))) {
            for (Entry entry : sortedLog) {
                writer.write(entry.toString());
                writer.newLine(); // Write a new line after each entry
            }
            writer.newLine();
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception appropriately, e.g., by logging or throwing it
        }

        System.out.println("Difference is " + wrong + " out of " + sortedLog.length);
        return wrong>0;
    }

    public static enum Method
    {
        ADD, REMOVE, CONTAINS
    };

    // Log entry for linearization point.
    public static class Entry
    {
        int threadId;
        Method method;
        String argument;
        boolean retval;
        long timestamp;

        public Entry(int threadId, Method method, String argument, boolean retval, long timestamp)
        {
            this.threadId = threadId;
            this.method = method;
            this.argument = argument;
            this.retval = retval;
            this.timestamp = timestamp;

            // TODO add method, arguments, return value and timestamp.
        }

        public String toString()
        {
            return "Entry{" + "threadId=" + threadId + ", method=" + method + ", argument='" + argument + '\''
                    + ", retval=" + retval + ", timestamp=" + timestamp + '}';
        }
    }
}
