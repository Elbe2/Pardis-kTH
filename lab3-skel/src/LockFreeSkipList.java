import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import java.util.function.Supplier;

public class LockFreeSkipList<T extends Comparable<T>> implements LockFreeSet<T>
{
    /* Number of levels */
    private static final int MAX_LEVEL = 16;

    private final Node<T> head = new Node<T>();
    private final Node<T> tail = new Node<T>();

    private ArrayList<Log.Entry> log;
    private Lock lock;

    public LockFreeSkipList()
    {
        for (int i = 0; i < head.next.length; i++)
        {
            head.next[i] = new AtomicMarkableReference<LockFreeSkipList.Node<T>>(tail, false);
        }
        log = new ArrayList<Log.Entry>();
        lock = new ReentrantLock();
    }

    private void submitEntry(Log.Entry e)
    {
        lock.lock();
        try
        {
            log.add(e);
        }
        finally
        {
            lock.unlock();
        }
    }

    private boolean setTimeStamp(Log.Entry e, Supplier<Boolean> lambda)
    {
        lock.lock();
        try
        {
            boolean ret = lambda.get();
            e.timestamp = System.nanoTime();
            return ret;
        }
        finally
        {
            lock.unlock();
        }
    }

    private static final class Node<T>
    {
        private final T value;
        private final AtomicMarkableReference<Node<T>>[] next;
        private final int topLevel;

        @SuppressWarnings("unchecked")
        public Node()
        {
            value = null;
            next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[MAX_LEVEL + 1];
            for (int i = 0; i < next.length; i++)
            {
                next[i] = new AtomicMarkableReference<Node<T>>(null, false);
            }
            topLevel = MAX_LEVEL;
        }

        @SuppressWarnings("unchecked")
        public Node(T x, int height)
        {
            value = x;
            next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[height + 1];
            for (int i = 0; i < next.length; i++)
            {
                next[i] = new AtomicMarkableReference<Node<T>>(null, false);
            }
            topLevel = height;
        }
    }

    /* Returns a level between 0 to MAX_LEVEL,
     * P[randomLevel() = x] = 1/2^(x+1), for x < MAX_LEVEL.
     */
    private static int randomLevel()
    {
        int r = ThreadLocalRandom.current().nextInt();
        int level = 0;
        r &= (1 << MAX_LEVEL) - 1;
        while ((r & 1) != 0)
        {
            r >>>= 1;
            level++;
        }
        return level;
    }

    public boolean add(T x)
    {
        return add(-1, x);
    }

    @SuppressWarnings("unchecked")
    public boolean add(int threadId, T x)
    {
        int topLevel = randomLevel();
        int bottomLevel = 0;
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Log.Entry entry = new Log.Entry(threadId, Log.Method.ADD, x.toString(), false, -1);
        while (true)
        {
            boolean found = find(x, preds, succs, entry);
            if (found)
            {
                submitEntry(entry); // --------------------------------------------- LINEAZIZATION POINT ----------------------------------------
                return false;
            }
            else
            {
                Node<T> newNode = new Node(x, topLevel);
                for (int level = bottomLevel; level <= topLevel; level++)
                {
                    Node<T> succ = succs[level];
                    newNode.next[level].set(succ, false);
                }
                Node<T> pred = preds[bottomLevel];
                Node<T> succ = succs[bottomLevel];
                boolean c;
                lock.lock();
                try // --------------------------------------------- LINEAZIZATION POINT ----------------------------------------
                {
                    c = pred.next[bottomLevel].compareAndSet(succ, newNode, false, false);
                    entry.timestamp = System.nanoTime();
                }
                finally
                {
                    lock.unlock();
                }
                if (!c)
                {
                    continue;
                }
                for (int level = bottomLevel + 1; level <= topLevel; level++)
                {
                    while (true)
                    {
                        pred = preds[level];
                        succ = succs[level];
                        if (pred.next[level].compareAndSet(succ, newNode, false, false))
                            break;
                        find(x, preds, succs, null);
                    }
                }
                entry.retval = true;
                submitEntry(entry);
                return true;
            }
        }
    }

    public boolean remove(T x)
    {
        return remove(-1, x);
    }

    @SuppressWarnings("unchecked")
    public boolean remove(int threadId, T x)
    {
        int bottomLevel = 0;
        Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T> succ;
        Log.Entry entry = new Log.Entry(threadId, Log.Method.REMOVE, x.toString(), false, -1);
        while (true)
        {
            boolean found = find(x, preds, succs, entry);
            if (!found)
            {
                submitEntry(entry);
                return false;
            }
            else
            {
                Node<T> nodeToRemove = succs[bottomLevel];
                for (int level = nodeToRemove.topLevel; level >= bottomLevel + 1; level--)
                {
                    boolean[] marked = { false };
                    succ = nodeToRemove.next[level].get(marked);
                    while (!marked[0])
                    {
                        nodeToRemove.next[level].compareAndSet(succ, succ, false, true);
                        succ = nodeToRemove.next[level].get(marked);
                    }
                }
                boolean[] marked = { false };
                succ = nodeToRemove.next[bottomLevel].get(marked);
                while (true)
                {
                    boolean iMarkedIt;
                    lock.lock();
                    try
                    {
                        iMarkedIt = nodeToRemove.next[bottomLevel].compareAndSet(succ, succ, false, true);
                        entry.timestamp = System.nanoTime();
                    }
                    finally
                    {
                        lock.unlock();
                    }
                    succ = succs[bottomLevel].next[bottomLevel].get(marked);
                    if (iMarkedIt)
                    {
                        find(x, preds, succs, null);
                        entry.retval = true;
                        submitEntry(entry);
                        return true;
                    }
                    else if (marked[0])
                    {
                        submitEntry(entry);
                        return false;
                    }
                }
            }
        }
    }

    public boolean contains(T x)
    {
        return contains(-1, x);
    }

    public boolean contains(int threadId, T x)
    {
        int bottomLevel = 0;
        int key = x.hashCode();
        boolean[] marked = { false };
        Log.Entry entry = new Log.Entry(threadId, Log.Method.CONTAINS, x.toString(), false, -1);
        Node<T> pred;
        lock.lock();
        try
        {
            entry.timestamp = System.nanoTime();
            pred = head;
        }
        finally
        {
            lock.unlock();
        }
        Node<T> curr = null;
        Node<T> succ = null;
        for (int level = MAX_LEVEL; level >= bottomLevel; level--)
        {
            curr = pred.next[level].getReference();
            while (true)
            {
                succ = curr.next[level].get(marked);
                while (marked[0])
                {
                    curr = succ;
                    succ = curr.next[level].get(marked);
                }
                if (curr.value != null && x.compareTo(curr.value) < 0)
                {
                    pred = curr;
                    curr = succ;
                }
                else
                {
                    break;
                }
            }
        }
        entry.retval = curr.value != null && x.compareTo(curr.value) == 0;
        submitEntry(entry);
        return entry.retval;
    }

    private boolean find(T x, Node<T>[] preds, Node<T>[] succs, Log.Entry entry)
    {
        int bottomLevel = 0;
        boolean[] marked = { false };
        boolean snip;
        Node<T> pred = null;
        Node<T> curr = null;
        Node<T> succ = null;
        retry: while (true)
        {
            if (entry != null)
            {
                lock.lock();
                try
                {
                    pred = head;
                    entry.timestamp = System.nanoTime();
                }
                finally
                {
                    lock.unlock();
                }
            }
            else
            {
                pred = head;
            }
            for (int level = MAX_LEVEL; level >= bottomLevel; level--)
            {
                curr = pred.next[level].getReference();
                while (true)
                {
                    succ = curr.next[level].get(marked);
                    while (marked[0])
                    {
                        if (entry != null && level == 0)
                        {
                            lock.lock();
                            try
                            {
                                snip = pred.next[level].compareAndSet(curr, succ, false, false);
                                entry.timestamp = System.nanoTime();
                            }
                            finally
                            {
                                lock.unlock();
                            }
                        }
                        else
                            snip = pred.next[level].compareAndSet(curr, succ, false, false);
                        if (!snip)
                            continue retry;
                        curr = succ;
                        succ = curr.next[level].get(marked);
                    }
                    if (curr.value != null && x.compareTo(curr.value) < 0)
                    {
                        pred = curr;
                        curr = succ;
                    }
                    else
                    {
                        break;
                    }
                }

                preds[level] = pred;
                succs[level] = curr;
            }
            return curr.value != null && x.compareTo(curr.value) == 0;
        }
    }

    public Log.Entry[] getLog()
    {
        return log.toArray(new Log.Entry[0]);
    }
}
