import java.util.*;
import javax.swing.*;
import java.awt.*;

public class LearnedIndexes {
    public static void main(String[] args) {
        int[] sizes = {100_000, 500_000, 1_000_000};
        int queryCount = 10_000;
        int trials = 3;
        String[] distributions = {"Uniform", "Sequential", "Skewed"};

        String[] names = {
            "Binary Search",
            "Learned Index",
            "Skip List",
            //"Tree Map",
            "y-fast Trie"
        };

        // Results for graphs (only 1M)
        long[][] build1M = new long[names.length][3];
        long[][] query1M = new long[names.length][3];
        long[][] mem1M   = new long[names.length][3];
        double[] nsPerQuery1M = new double[names.length];

        for (int size : sizes) {
            System.out.println("\nRunning distributions for size: " + size + "\n");

            for (int d = 0; d < distributions.length; d++) {
                String dist = distributions[d];
                int[] data = switch (dist) {
                    case "Uniform" -> DatasetGenerator.uniform(size);
                    case "Sequential" -> DatasetGenerator.sequential(size);
                    case "Skewed" -> DatasetGenerator.skewed(size);
                    default -> new int[0];
                };

                int[] queries = Benchmark.queries(queryCount, size * 10);

                // Correctness reference (Binary Search)
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
                            //case 3 -> new TreeMapIndex();
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

        // Compute averaged ns/query for 1M
        for (int i = 0; i < names.length; i++) {
            long totalQ = query1M[i][0] + query1M[i][1] + query1M[i][2];
            nsPerQuery1M[i] = (double) totalQ / (3 * queryCount);
        }

        //  Ranked Key Observations for 1M 
        System.out.println("\nKey Observations (1M elements)\n");

        // Prepare averages
        double[] avgBuild = new double[names.length];
        double[] avgQuery = new double[names.length];
        double[] avgMem   = new double[names.length];

        for (int i = 0; i < names.length; i++) {
            avgBuild[i] = (build1M[i][0] + build1M[i][1] + build1M[i][2]) / 3.0 / 1e6;
            avgQuery[i] = (query1M[i][0] + query1M[i][1] + query1M[i][2]) / 3.0 / 1e6;
            avgMem[i]   = (mem1M[i][0] + mem1M[i][1] + mem1M[i][2]) / 3.0 / 1e6;
        }

        // Ranked by Query time (fastest first)
        Integer[] orderQ = getRankedOrder(avgQuery);
        System.out.println("Query Performance (fastest to slowest):");
        for (int r = 0; r < names.length; r++) {
            int idx = orderQ[r];
            System.out.printf("(%d) %s - %.3f ms\n", r+1, names[idx], avgQuery[idx]);
        }

        // Ranked by Build time
        Integer[] orderB = getRankedOrder(avgBuild);
        System.out.println("\nBuild Time (fastest to slowest):");
        for (int r = 0; r < names.length; r++) {
            int idx = orderB[r];
            System.out.printf("(%d) %s - %.3f ms\n", r+1, names[idx], avgBuild[idx]);
        }

        // Ranked by Memory (lowest to highest)
        Integer[] orderM = getRankedOrder(avgMem);
        System.out.println("\nMemory Usage (lowest to highest):");
        for (int r = 0; r < names.length; r++) {
            int idx = orderM[r];
            System.out.printf("(%d) %s - %.3f MB\n", r+1, names[idx], avgMem[idx]);
        }

        // Ranked Queries per Distribution (1M)
        System.out.println("\nQuery Performance per Distribution (1M elements):");
        for (int d = 0; d < distributions.length; d++) {
            System.out.println(distributions[d] + ":");
            // Get sorted indices by query time for this distribution
            Integer[] ranked = getRankedOrderForDist(query1M, d);
            for (int r = 0; r < ranked.length; r++) {
                int idx = ranked[r];
                System.out.printf("(%d) %s - %.3f ms\n", r + 1, names[idx], query1M[idx][d] / 1_000_000.0);
            }
            System.out.println();
        }

        // Draw graphs only for 1M
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Learned Indexes - Graphs (1M elements only)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1350, 980);
            frame.add(new GraphPanel(names, build1M, query1M, mem1M, nsPerQuery1M));
            frame.setVisible(true);
        });
    }

    // Helper to get ranked indices (smallest to largest)
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
// Benchmark class
class Benchmark {
    public static long queryTime(PredecessorIndex idx, int[] queries) {
        int warmQ = Math.min(2000, queries.length);
        // warm-up phase reduces JIT variability for more stable timing
        for (int i = 0; i < warmQ; i++) {
            idx.predecessor(queries[i]);
        }

        long start = System.nanoTime();
        for (int q : queries) {
            idx.predecessor(q);
        }
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

        long memoryUsed = Math.max(0, after - before);
        return new long[]{memoryUsed, buildTime};
    }

    private static void sleep() {
        try {
            Thread.sleep(20);
        } catch (Exception ignored) {}
    }

    public static int[] queries(int n, int max) {
        Random r = new Random(99);
        int[] q = new int[n];
        for (int i = 0; i < n; i++) {
            q[i] = r.nextInt(max);
        }
        return q;
    }
}

// GraphPanel
class GraphPanel extends JPanel {
    String[] names;
    long[][] buildRes, queryRes, memRes;
    double[] nsPerQuery;

    GraphPanel(String[] n, long[][] b, long[][] q, long[][] m, double[] npq) {
        this.names = n;
        this.buildRes = b;
        this.queryRes = q;
        this.memRes = m;
        this.nsPerQuery = npq;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int gw = w / 2 - 70;
        int gh = h / 2 - 90;

        // left column: query time (multi-bar) and memory (single-bar)
        drawMultiBarGraph(g2, "1. Query Time (ms)", 40, 80, gw, gh, queryRes,
                new Color[]{new Color(0, 100, 200), new Color(0, 180, 0), new Color(200, 140, 0)});

        drawSingleBarGraph(g2, "2. Build Time (ms)", w/2 + 30, 50, gw, gh, average(buildRes));
        drawSingleBarGraph(g2, "3. Memory Usage (MB)", 40, h/2 + 50, gw, gh, average(memRes));

        // right column: ns per query (single bar, converted from double array)
        long[][] nsMatrix = new long[names.length][1];
        for (int i = 0; i < names.length; i++) {
            nsMatrix[i][0] = (long) nsPerQuery[i];
        }
        drawSingleBarGraph(g2, "4. ns per Query", w/2 + 30, h/2 + 50, gw, gh, nsMatrix);
    }

    private long[][] average(long[][] data) {
        long[][] avg = new long[names.length][1];
        for (int i = 0; i < names.length; i++) {
            avg[i][0] = (data[i][0] + data[i][1] + data[i][2]) / 3;
        }
        return avg;
    }

    private void drawMultiBarGraph(Graphics2D g, String title, int x, int y, int gw, int gh, long[][] data, Color[] colors) {
        int trials = 3;

        g.setColor(Color.BLACK);
        g.drawString(title, x, y - 40);

        String[] distNames = {"Uniform", "Sequential", "Skewed"};
        for (int i = 0; i < trials; i++) {
            g.setColor(colors[i]);
            g.fillRect(x + i * 130, y - 35, 15, 15);
            g.setColor(Color.BLACK);
            g.drawString(distNames[i], x + i * 130 + 20, y - 22);
        }

        // find global max for scaling all bars consistently
        long maxVal = 1;
        for (long[] row : data) {
            for (long v : row) {
                if (v > maxVal) maxVal = v;
            }
        }

        int barW = gw / (names.length * 5);
        int groupW = barW * 5;

        for (int i = 0; i < names.length; i++) {
            int gx = x + i * groupW + 15;
            g.drawString(names[i], gx - 10, y + gh + 30);

            for (int j = 0; j < trials; j++) {
                long val = data[i][j];
                int barH = (int) ((val * (gh - 60.0)) / maxVal);
                g.setColor(colors[j]);
                g.fillRect(gx + j * (barW + 10), y + gh - barH - 30, barW, barH);

                g.setColor(Color.BLACK);
                g.drawString(String.format("%.1f", val / 1_000_000.0), gx + j*(barW + 10) + 2, y + gh - barH - 35);
            }
        }
    }

    private void drawSingleBarGraph(Graphics2D g, String title, int x, int y, int gw, int gh, long[][] data) {
        g.setColor(Color.BLACK);
        g.drawString(title, x, y - 15);

        // find max for vertical scaling
        long maxVal = 1;
        for (int i = 0; i < names.length; i++) {
            if (data[i][0] > maxVal) maxVal = data[i][0];
        }

        int barW = gw / (names.length * 2);
        for (int i = 0; i < names.length; i++) {
            int gx = x + i * (barW * 2 + 40);
            long val = data[i][0];
            int barH = (int) ((val * (gh - 60.0)) / maxVal);

            g.setColor(new Color(70, 130, 180));
            g.fillRect(gx, y + gh - barH - 30, barW, barH);

            g.setColor(Color.BLACK);
            g.drawString(names[i], gx - 5, y + gh + 30);
            g.drawString(String.format("%.1f", val / 1_000_000.0), gx + 2, y + gh - barH - 35);
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