import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class Log
{
    private Log()
    {
        // Do not implement
    }

    public static boolean validate(Log.Entry[] log)
    {
        Set<Integer> seqSet = new HashSet<>();

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
            return false;
        }
        return true;
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
