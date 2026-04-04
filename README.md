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
- TreeMap (commented out by default)

The benchmark runs on three distributions (Uniform, Sequential, Skewed),
performs multiple trials, validates correctness against Binary Search,
prints tables, ranks the indexes, and shows Swing bar graphs.

Expected Theoretical Complexities (predecessor query)
-----------------------------------------------------
- Binary Search:          O(log n)          - classic comparison-based
- Skip List:              O(log n) expected - probabilistic, similar to balanced trees
- y-fast Trie:            O(log log U)      - where U is the universe size (very fast for integers)
- Learned Index:          O(log n) worst    - but often near-constant with good predictions
- TreeMap:                O(log n)          - red-black tree floor operation

Build is usually O(n log n) for sorting-heavy structures.

How to Build and Run
--------------------
Using Makefile (recommended):
  make          # compile all .java files
  make run   # run the full benchmark
  make clean    # remove all .class files

Or manually:
  javac *.java
  java LearnedIndexes

Requirements: Java 8+ (Java 14+ recommended for switch expressions). No external libraries.

Project Files
-------------
- LearnedIndexes.java     
- Makefile

Performance Notes (1M elements, averaged over 3 trials)
-------------------------------------------------------
- Binary Search delivers the fastest and most consistent queries across all distributions.
- y-fast Trie offers the quickest build times with moderate memory use.
- Learned Index builds fast on sequential data and shines on skewed sets but varies more on uniform.
- Skip List uses significantly more memory while delivering solid but not top-tier query speed.
- Memory stays very low for array-based approaches (Binary Search and Learned Index).

Key Observations from 1M runs (Sample Run)
-----------------------------
Query Performance (fastest to slowest overall):
(1) Binary Search - 6.431 ms
(2) Skip List - 13.534 ms
(3) y-fast Trie - 15.972 ms
(4) Learned Index - 28.583 ms

Build Time (fastest to slowest):
(1) y-fast Trie - 140.937 ms
(2) Binary Search - 143.168 ms
(3) Learned Index - 181.654 ms
(4) Skip List - 944.452 ms

Memory Usage (lowest to highest):
(1) Binary Search - 4.154 MB
(2) Learned Index - 4.198 MB
(3) y-fast Trie - 22.036 MB
(4) Skip List - 50.205 MB

Created as an educational comparison of classic vs. learned data structures for predecessor search.