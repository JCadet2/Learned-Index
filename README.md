Learned Indexes Benchmark
=========================
A Java demonstration comparing several predecessor search data structures
on large integer arrays (static predecessor problem).

Structures Compared
-------------------
- Binary Search (sorted array + Arrays.binarySearch)
- Learned Index (simple linear regression segments + local binary search)
- Skip List (probabilistic layered list)
- y-fast Trie (small sorted clusters with separator binary search)

The benchmark runs on three distributions (Uniform, Sequential, Skewed),
performs multiple trials, validates correctness against Binary Search,
prints tables, ranks the indexes, and shows Swing bar graphs.

Expected Theoretical Complexities (predecessor query)
-----------------------------------------------------
- Binary Search:          O(log n)          - classic comparison-based
- Skip List:              O(log n) expected - probabilistic, similar to balanced trees
- y-fast Trie:            O(log log U)      - where U is the universe size (very fast for integers)
- Learned Index:          O(log n) worst    - but often near-constant with good predictions

Build is usually O(n log n) for sorting-heavy structures.

How to Build and Run
--------------------
Using Makefile (recommended):
  make          # compile all .java files
  make run      # run the full benchmark
  make clean    # remove all .class files

Or manually:
  javac *.java
  java LearnedIndexes

Requirements: Java 8+ (Java 14+ recommended for switch expressions). No external libraries.

Project Files
-------------
- LearnedIndexes.java     
- Makefile

Performance Notes (1M elements, averaged over 5 trials)
-------------------------------------------------------
- Binary Search delivers the fastest and most consistent queries across all distributions.
- Learned Index builds very quickly and performs well on predictable data.
- y-fast Trie offers fast build times with moderate memory use.
- Skip List uses significantly more memory while delivering solid but not top-tier query speed.
- Memory stays very low for array-based approaches (Binary Search and Learned Index).

Key Observations from 1M runs (Latest Run)
-------------------------------------------
Build Time (fastest to slowest):
(1) Binary Search   - 49.855 ms
(2) Learned Index   - 50.103 ms
(3) y-fast Trie     - 71.431 ms
(4) Skip List       - 274.116 ms

Memory Usage (lowest to highest):
(1) Binary Search   - 4.154 MB
(2) Learned Index   - 4.198 MB
(3) y-fast Trie     - 22.035 MB
(4) Skip List       - 50.192 MB

Query Performance per Distribution (1M elements):

Uniform:
(1) Binary Search   - 19.811 ms
(2) Learned Index   - 46.242 ms
(3) y-fast Trie     - 48.084 ms
(4) Skip List       - 68.025 ms

Sequential:
(1) Binary Search   - 22.287 ms
(2) Learned Index   - 52.674 ms
(3) y-fast Trie     - 54.444 ms
(4) Skip List       - 70.426 ms

Skewed:
(1) Binary Search   - 13.108 ms
(2) y-fast Trie     - 30.999 ms
(3) Skip List       - 60.114 ms
(4) Learned Index   - 73.185 ms

Created as an educational comparison of classic vs. learned data structures for predecessor search.