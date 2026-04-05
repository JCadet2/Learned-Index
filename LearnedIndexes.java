import java.util.*;
import javax.swing.*;
import java.awt.*;

public class LearnedIndexes {
    public static void main(String[] args) {
        int[] sizes = {10_000, 100_000, 1_000_000};
        int queryCount = 100_000;
        int trials = 5;
        String[] distributions = {"Uniform", "Sequential", "Skewed"};

        String[] names = {
            "Binary Search",
            "Learned Index",
            "Skip List",
            "y-fast Trie"
        };

        // Results only for 1M (used for graphs and per-distribution ranking)
        long[][] build1M = new long[names.length][3];
        long[][] query1M = new long[names.length][3];
        long[][] mem1M   = new long[names.length][3];

        for (int size : sizes) {
            System.out.println("\n Running distributions for size: " + size + "\n");

            for (int d = 0; d < distributions.length; d++) {
                String dist = distributions[d];
                int[] data = switch (dist) {
                    case "Uniform" -> DatasetGenerator.uniform(size);
                    case "Sequential" -> DatasetGenerator.sequential(size);
                    case "Skewed" -> DatasetGenerator.skewed(size);
                    default -> new int[0];
                };

                int[] queries = Benchmark.queries(queryCount, size * 10);

                // Correctness reference
                PredecessorIndex reference = new BinarySearchIndex();
                reference.build(data);

                System.out.println(dist.toUpperCase() + " DISTRIBUTION:");

                for (int i = 0; i < names.length; i++) {
                    long buildSum = 0, querySum = 0, memSum = 0;

                    for (int r = 0; r < trials; r++) {
                        PredecessorIndex idx = switch (i) {
                            case 0 -> new BinarySearchIndex();
                            case 1 -> new LearnedIndex();
                            case 2 -> new SkipListIndex();
                            default -> new YFastTrieIndex();
                        };

                        // Small warmup
                        int warmSize = Math.min(10000, data.length);
                        int[] warmData = Arrays.copyOf(data, warmSize);
                        idx.build(warmData);
                        idx.predecessor(warmData[warmSize / 2]);

                        // Real measurement
                        long[] memAndBuild = Benchmark.measureMemoryAndBuild(idx, data);
                        long mem = memAndBuild[0];
                        long buildTime = memAndBuild[1];

                        memSum += mem;
                        buildSum += buildTime;

                        long queryTime = Benchmark.queryTime(idx, queries);
                        querySum += queryTime;

                        // Correctness check
                        boolean correct = true;
                        for (int q : queries) {
                            if (idx.predecessor(q) != reference.predecessor(q)) {
                                correct = false;
                                break;
                            }
                        }
                        if (!correct) {
                            System.out.println("ERROR: Correctness mismatch for " + names[i]);
                        }
                    }

                    long avgBuild = buildSum / trials;
                    long avgQuery = querySum / trials;
                    long avgMem   = memSum / trials;

                    long nsPerQ = avgQuery / queryCount;
                    System.out.printf("%-14s | Build: %7.3f ms | Query: %7.3f ms | ns/q: %4d | Memory: %6.3f MB\n",
                            names[i], avgBuild/1e6, avgQuery/1e6, nsPerQ, avgMem/1e6);

                    // Store only for 1M
                    if (size == 1_000_000) {
                        build1M[i][d] = avgBuild;
                        query1M[i][d] = avgQuery;
                        mem1M[i][d]   = avgMem;
                    }
                }
                System.out.println();
            }
            System.out.println("------------------------------------------------------------");
        }

        // RANKINGS FOR 1M ELEMENTS (PER DISTRIBUTION FOR QUERIES)
        System.out.println("\nKEY OBSERVATIONS FOR 1M ELEMENTS\n");

        // Overall averages for Build and Memory
        double[] avgBuild = new double[names.length];
        double[] avgMem   = new double[names.length];
        for (int i = 0; i < names.length; i++) {
            avgBuild[i] = (build1M[i][0] + build1M[i][1] + build1M[i][2]) / 3.0 / 1e6;
            avgMem[i]   = (mem1M[i][0] + mem1M[i][1] + mem1M[i][2]) / 3.0 / 1e6;
        }

        // Ranked Build Time (overall)
        Integer[] orderB = getRankedOrder(avgBuild);
        System.out.println("Build Time (fastest to slowest):");
        for (int r = 0; r < names.length; r++) {
            int idx = orderB[r];
            System.out.printf("(%d) %s - %.3f ms\n", r+1, names[idx], avgBuild[idx]);
        }

        // Ranked Memory (overall)
        Integer[] orderM = getRankedOrder(avgMem);
        System.out.println("\nMemory Usage (lowest to highest):");
        for (int r = 0; r < names.length; r++) {
            int idx = orderM[r];
            System.out.printf("(%d) %s - %.3f MB\n", r+1, names[idx], avgMem[idx]);
        }

        // PER-DISTRIBUTION QUERY RANKINGS
        System.out.println("\nQuery Performance per Distribution (1M elements):");
        for (int d = 0; d < distributions.length; d++) {
            System.out.println("\n" + distributions[d] + ":");
            Integer[] ranked = getRankedOrderForDist(query1M, d);
            for (int r = 0; r < ranked.length; r++) {
                int idx = ranked[r];
                System.out.printf("(%d) %s - %.3f ms\n",
                        r + 1, names[idx], query1M[idx][d] / 1_000_000.0);
            }
        }

        // Draw graphs
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Learned Indexes - Graphs (1M elements only)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1350, 980);
            frame.add(new GraphPanel(names, build1M, query1M, mem1M, queryCount));
            frame.setVisible(true);
        });
    }

    private static Integer[] getRankedOrder(double[] values) {
        Integer[] indices = new Integer[values.length];
        for (int i = 0; i < values.length; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Double.compare(values[a], values[b]));
        return indices;
    }

    private static Integer[] getRankedOrderForDist(long[][] data, int distIdx) {
        Integer[] indices = new Integer[data.length];
        for (int i = 0; i < data.length; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Long.compare(data[a][distIdx], data[b][distIdx]));
        return indices;
    }
}

// Class Benchmark
class Benchmark {
    public static long queryTime(PredecessorIndex idx, int[] queries) {
        int warmQ = Math.min(10000, queries.length);
        for (int i = 0; i < warmQ; i++) idx.predecessor(queries[i]);

        long start = System.nanoTime();
        for (int q : queries) idx.predecessor(q);
        return System.nanoTime() - start;
    }

    public static long[] measureMemoryAndBuild(PredecessorIndex idx, int[] data) {
        Runtime rt = Runtime.getRuntime();
        rt.gc();
        sleep();
        long before = rt.totalMemory() - rt.freeMemory();

        long start = System.nanoTime();
        idx.build(data);
        long buildTime = System.nanoTime() - start;

        rt.gc();
        sleep();
        long after = rt.totalMemory() - rt.freeMemory();

        return new long[]{Math.max(0, after - before), buildTime};
    }

    private static void sleep() {
        try { Thread.sleep(20); } catch (Exception ignored) {}
    }

    public static int[] queries(int n, int max) {
        Random r = new Random(99);
        int[] q = new int[n];
        for (int i = 0; i < n; i++) q[i] = r.nextInt(max);
        return q;
    }
}

// Class GraphPanel 
class GraphPanel extends JPanel {
    String[] names;
    long[][] buildRes, queryRes, memRes;
    int queryCount;

    GraphPanel(String[] n, long[][] b, long[][] q, long[][] m, int qc) {
        this.names = n;
        this.buildRes = b;
        this.queryRes = q;
        this.memRes = m;
        this.queryCount = qc;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int gw = (w - 120) / 2;
        int gh = (h - 200) / 2;

        Color[] colors = {
            new Color(0, 100, 200),   // Uniform - blue
            new Color(0, 180, 0),     // Sequential - green
            new Color(200, 140, 0)    // Skewed - orange
        };

        // 1. Query Time (ms) - top left
        drawMultiBarGraph(g2, "Query Time (ms)", 40, 80, gw, gh, queryRes, colors, true);

        // 2. Build Time (ms) - top right
        drawMultiBarGraph(g2, "Build Time (ms)", w/2 + 30, 80, gw, gh, buildRes, colors, true);

        // 3. Memory Usage (MB) - bottom left
        drawMultiBarGraph(g2, "Memory Usage (MB)", 40, h/2 + 100, gw, gh, memRes, colors, true);

        // 4. ns per Query - bottom right
        long[][] nsRes = new long[names.length][3];
        for (int i = 0; i < names.length; i++) {
            for (int d = 0; d < 3; d++) {
                nsRes[i][d] = queryRes[i][d] / queryCount;
            }
        }
        drawMultiBarGraph(g2, "ns per Query", w/2 + 30, h/2 + 100, gw, gh, nsRes, colors, false);
    }

    private void drawMultiBarGraph(Graphics2D g, String title, int x, int y, int gw, int gh,
                                   long[][] data, Color[] colors, boolean isLargeValue) {

        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString(title, x, y - 40);

        // Legend
        String[] distNames = {"Uniform", "Sequential", "Skewed"};
        for (int i = 0; i < 3; i++) {
            g.setColor(colors[i]);
            g.fillRect(x + i * 130, y - 35, 15, 15);
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString(distNames[i], x + i * 130 + 20, y - 22);
        }

        // Find max for scaling
        long maxVal = 1;
        for (long[] row : data) {
            for (long v : row) if (v > maxVal) maxVal = v;
        }

        int barW = gw / (names.length * 5);
        int groupW = barW * 5;

        for (int i = 0; i < names.length; i++) {
            int gx = x + i * groupW + 15;
            g.setColor(Color.BLACK);
            g.drawString(names[i], gx - 10, y + gh + 30);

            for (int j = 0; j < 3; j++) {
                long val = data[i][j];
                double ratio = (double) val / maxVal;
                int barH = (int) (ratio * (gh - 60));
                if (barH < 4) barH = 4;   // minimum visible height

                g.setColor(colors[j]);
                g.fillRect(gx + j * (barW + 10), y + gh - barH - 30, barW, barH);

                // Label
                g.setColor(Color.BLACK);
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                if (isLargeValue) {
                    g.drawString(String.format("%.1f", val / 1_000_000.0), gx + j*(barW + 10) + 2, y + gh - barH - 35);
                } else {
                    g.drawString(String.valueOf(val), gx + j*(barW + 10) + 2, y + gh - barH - 35);
                }
            }
        }
    }
}

class DatasetGenerator {
    public static int[] uniform(int n) {
        Random r = new Random(42);
        int[] data = new int[n];

        for (int i = 0; i < n; i++) {
            // evenly spread values for testing general performance without clustering
            data[i] = r.nextInt(n * 10);
        }

        return data;
    }

    public static int[] sequential(int n) {
        int[] data = new int[n];

        for (int i = 0; i < n; i++) {
            data[i] = i * 10;  // evenly spaced values for testing sorted-case performance
        }

        return data;
    }

    public static int[] skewed(int n) {
        Random r = new Random(42);
        int[] data = new int[n];

        for (int i = 0; i < n; i++) {
            // 80% small values, 20% large values creates realistic hot/cold distribution
            if (r.nextDouble() < 0.8) {
                data[i] = r.nextInt(n);
            } else {
                data[i] = r.nextInt(n * 10);
            }
        }

        return data;
    }
}

interface PredecessorIndex {
    void build(int[] data);
    int predecessor(int key);
}

class BinarySearchIndex implements PredecessorIndex {
    private int[] data;

    public void build(int[] input) {
        data = input.clone();
        Arrays.sort(data);  // sorting once enables fast predecessor queries later
    }

    public int predecessor(int key) {
        int pos = Arrays.binarySearch(data, key);

        if (pos >= 0) {
            return data[pos];  // exact match found
        }

        int insertion = -pos - 1;

        if (insertion == 0) {
            return -1;  // key smaller than all elements
        }

        return data[insertion - 1];  // the largest value strictly before the insertion point
    }
}

class SkipListIndex implements PredecessorIndex {
    private static class Node {
        int key;
        Node[] forward;

        Node(int key, int level) {
            this.key = key;
            forward = new Node[level + 1];
        }
    }

    private static final double P = 0.5;
    private Node head;
    private int maxLevel;
    private int level;
    private Random rand;

    public void build(int[] data) {
        maxLevel = 16; // fixed reasonable maximum for typical integer keys
        head = new Node(-1, maxLevel);
        level = 0;
        rand = new Random(42);  // fixed seed keeps build deterministic across runs

        Arrays.sort(data);
        for (int v : data) insert(v);
    }

    private int randomLevel() {
        int lvl = 0;
        while (rand.nextDouble() < P && lvl < maxLevel) {
            lvl++;
        }
        return lvl;
    }

    private void insert(int key) {
        Node[] update = new Node[maxLevel + 1];
        Node x = head;

        // find the rightmost nodes at each level that precede the new key
        for (int i = level; i >= 0; i--) {
            while (x.forward[i] != null && x.forward[i].key < key) {
                x = x.forward[i];
            }
            update[i] = x;
        }

        x = x.forward[0];

        int lvl = randomLevel();
        if (lvl > level) {
            for (int i = level + 1; i <= lvl; i++) {
                update[i] = head;
            }
            level = lvl;
        }

        Node newNode = new Node(key, lvl);
        for (int i = 0; i <= lvl; i++) {
            newNode.forward[i] = update[i].forward[i];
            update[i].forward[i] = newNode;
        }
    }

    public int predecessor(int key) {
        Node x = head;
        for (int i = level; i >= 0; i--) {
            while (x.forward[i] != null && x.forward[i].key <= key) {
                x = x.forward[i];
            }
        }
        return x.key == -1 ? -1 : x.key;  // head sentinel guarantees safe return of -1 when empty
    }
}

class TreeMapIndex implements PredecessorIndex {
    private TreeMap<Integer, Integer> tree;

    public void build(int[] data) {
        tree = new TreeMap<>();
        for (int v : data) {
            tree.put(v, v);  // value is irrelevant; TreeMap provides ordered keys
        }
    }

    public int predecessor(int key) {
        Integer val = tree.floorKey(key);
        return (val == null) ? -1 : val;  // floorKey directly gives predecessor or exact match
    }
}

class YFastTrieIndex implements PredecessorIndex {
    private static final int CLUSTER_THRESHOLD = 32; // balances binary search cost inside cluster vs number of clusters

    private static class Cluster {
        ArrayList<Integer> keys = new ArrayList<>();

        Cluster() {}

        Cluster(int[] arr) {
            for (int x : arr) keys.add(x);
        }

        int predecessor(int key) {
            if (keys.isEmpty()) return -1;

            int l = 0, r = keys.size() - 1;
            int ans = -1;
            while (l <= r) {
                int m = (l + r) >>> 1;
                int val = keys.get(m);
                if (val <= key) {
                    ans = val;
                    l = m + 1;
                } else {
                    r = m - 1;
                }
            }
            return ans;  // rightmost value <= key inside this cluster
        }

        void insert(int key) {
            int idx = Collections.binarySearch(keys, key);
            if (idx < 0) keys.add(-idx - 1, key);
        }

        void delete(int key) {
            int idx = Collections.binarySearch(keys, key);
            if (idx >= 0) keys.remove(idx);
        }
    }

    private ArrayList<Cluster> clusters = new ArrayList<>();
    private ArrayList<Integer> separators = new ArrayList<>();

    @Override
    public void build(int[] data) {
        clusters.clear();
        separators.clear();
        if (data == null || data.length == 0) return;

        Arrays.sort(data);
        int n = data.length;
        int i = 0;
        while (i < n) {
            int end = Math.min(i + CLUSTER_THRESHOLD, n);
            int[] sub = Arrays.copyOfRange(data, i, end);
            Cluster c = new Cluster(sub);
            clusters.add(c);
            separators.add(c.keys.get(0));  // first key acts as separator for quick cluster lookup
            i = end;
        }
    }

    @Override
    public int predecessor(int key) {
        if (clusters.isEmpty()) return -1;

        int idx = Collections.binarySearch(separators, key);
        if (idx >= 0) return separators.get(idx); // exact match on a cluster start

        int ins = -idx - 1;
        if (ins == 0) return -1; // before first cluster

        int clusterIdx = ins - 1;
        int pred = clusters.get(clusterIdx).predecessor(key);

        if (pred != -1) return pred;

        // fall back to last element of previous cluster if none in current
        return clusterIdx > 0 
            ? clusters.get(clusterIdx - 1).keys.get(clusters.get(clusterIdx - 1).keys.size() - 1) 
            : -1;
    }

    public void insert(int key) {
        if (clusters.isEmpty()) {
            Cluster c = new Cluster();
            c.insert(key);
            clusters.add(c);
            separators.add(key);
            return;
        }

        int idx = Collections.binarySearch(separators, key);
        if (idx < 0) {
            idx = -idx - 2;  // convert to predecessor cluster index
        }
        idx = Math.max(idx, 0);

        Cluster c = clusters.get(idx);
        c.insert(key);

        // split oversized cluster to keep search times bounded
        if (c.keys.size() > CLUSTER_THRESHOLD * 2) {
            int mid = c.keys.size() / 2;
            Cluster newC = new Cluster();
            for (int i = mid; i < c.keys.size(); i++) newC.keys.add(c.keys.get(i));
            while (c.keys.size() > mid) c.keys.remove(c.keys.size() - 1);

            clusters.add(idx + 1, newC);
            separators.add(idx + 1, newC.keys.get(0));
        }
    }

    public void delete(int key) {
        if (clusters.isEmpty()) return;

        int idx = Collections.binarySearch(separators, key);
        if (idx < 0) {
            idx = -idx - 2;
        }
        idx = Math.max(idx, 0);

        Cluster c = clusters.get(idx);
        c.delete(key);

        if (c.keys.isEmpty()) {
            clusters.remove(idx);
            separators.remove(idx);
        }
    }
}

class LearnedIndex implements PredecessorIndex {
    private int[] data;
    private static final int SEARCH_RADIUS = 64; // generous enough to cover prediction errors in most cases

    private static class Segment {
        int startKey;
        double slope;
        double intercept;
        int startPos;
        int endPos;
    }

    private ArrayList<Segment> segments;

    public void build(int[] input) {
        data = input.clone();
        Arrays.sort(data);

        segments = new ArrayList<>();
        int segmentSize = 1024;  // larger segments reduce model size while keeping predictions useful

        for (int i = 0; i < data.length; i += segmentSize) {
            int end = Math.min(i + segmentSize - 1, data.length - 1);
            Segment s = new Segment();

            s.startPos = i;
            s.endPos = end;

            int x1 = data[i];
            int x2 = data[end];

            s.slope = (x2 == x1) ? 0 : (double)(end - i) / (x2 - x1);
            s.intercept = i - s.slope * x1;
            s.startKey = x1;

            segments.add(s);
        }
    }

    public int predecessor(int key) {
        if (data.length == 0) {
            return -1;
        }

        // find the rightmost segment that could contain the key
        Segment seg = segments.get(0);
        for (Segment s : segments) {
            if (key >= s.startKey) {
                seg = s;
            } else {
                break;
            }
        }

        int predicted = (int)(seg.slope * key + seg.intercept);
        predicted = Math.max(seg.startPos, Math.min(seg.endPos, predicted));

        int left = Math.max(seg.startPos, predicted - SEARCH_RADIUS);
        int right = Math.min(seg.endPos, predicted + SEARCH_RADIUS);

        int pos = Arrays.binarySearch(data, left, right + 1, key);

        if (pos >= 0) {
            return data[pos];
        }

        int insertion = -pos - 1;

        if (insertion <= left || insertion > right) {
            // prediction was way off; fall back to full binary search in segment
            pos = Arrays.binarySearch(data, seg.startPos, seg.endPos + 1, key);

            if (pos >= 0) {
                return data[pos];
            }
            insertion = -pos - 1;

            if (insertion == seg.startPos) {
                return -1;
            }
            return data[insertion - 1];
        }

        return data[insertion - 1];  // safe because we stayed inside the segment range
    }
}