# Assignment 4 — Dynamic Programming and String Processing

**Selected language:** Java (`submission.json: {"language":"Java"}`). All algorithm code is in `Java/source/Algorithms.java` and preserves the supplied JSON Lines adapters. Source is compiled with `javac --release 17` (Java 17-compatible bytecode).

## 1. Reproducibility and execution

### Environment used for the measurements

- Linux `6.18.57` on x86-64, AMD EPYC 9V74 80-Core Processor (shared container; only the available environment is claimed, not an exclusive CPU core).
- OpenJDK `21.0.12.1`; compiler `javac 21.0.12.1 --release 17`. Therefore the implementation targets Java 17, although the measurements used JDK 21.
- Python `3.13.5` for the template launcher, fixture oracles and stress scripts; available memory reported by the container: approximately 5.8 GiB. Java process heap capped at 384 MiB for local checks and benchmarks.
- Exact timing depends on hardware, JIT, garbage collection and container load; benchmark results are measurements, not an asymptotic proof.

Commands, from the repository root:

```bash
python tools/run.py --setup
python tools/run.py --check
python tests/student/generate_cases.py
python tools/run.py --input tests/student/inputs.jsonl > tests/student/actual_outputs.jsonl
python tests/student/verify_student.py
python tests/student/stress_limits.py
javac --release 17 -cp Java/build -d Java/build tests/student/SearchBenchmark.java
java -Xmx384m -cp Java/build SearchBenchmark > tests/student/search_benchmark.csv
```

`run.py` compiles `Java/source/*.java` to `Java/build/` and invokes the provided `Main` adapter. Build artifacts are deliberately excluded from the submission. `--input` expects one JSON object per line and writes one result per line. There are no extra stdout messages from the algorithms. Each call allocates its own state; there are no persistent global algorithm caches. The given `ping` is unchanged.

**Recorded local checks:** setup passed; **44/44 public cases passed**, plus a repeated mixed batch in one process; 170 individually generated small cases checked against independent references; and 8 maximum-domain/stress cases verified. These are local checks, not a promise of an official Moodle or GitHub grade. Logs: `tests/student/setup_log.txt`, `tests/student/public_checks_local.txt`, `tests/student/stress_results.txt`.

## 2. Algorithm explanations, correctness and complexity

In the bounds below, "auxiliary" means memory beyond input and returned output. Since Java's JSON parsing and serialization are provided, their costs are not included in algorithmic bounds. All sums of block sizes and tree weights use `long` (signed 64-bit), avoiding 32-bit overflow.

### 2.1 LCS — prefix dynamic programming

**State:** `dp[i][j]` is the maximum length of a common subsequence of prefixes `a[0..i)` and `b[0..j)`. Base cases `dp[i][0]=dp[0][j]=0` are correct because an empty prefix shares no nonempty subsequence.

**Transition:** When `a[i-1]==b[j-1]`, append that character to an optimal common subsequence of the smaller prefixes and set `dp[i][j]=dp[i-1][j-1]+1`. Otherwise an optimal subsequence must omit at least one of the two last characters; therefore `dp[i][j]=max(dp[i-1][j],dp[i][j-1])`. Rows are processed in increasing `i` and columns in increasing `j`, so all referenced states exist.

**Witness:** Starting at `(n,m)`, collect an equal last character and move diagonally; otherwise move to a neighbour attaining the optimal state (ties move toward decreasing `i`). Reverse the collected characters. Every emitted character appears in both strings in increasing original order, so the witness is a subsequence of both, with exactly `dp[n][m]` characters. Ties may yield different optimal witnesses. If one input is empty, result is `(0,"")`.

**Complexity:** O((n+1)(m+1)) time and auxiliary memory (including the DP table); O(min(n,m)+1) output characters. Boundaries tested: empty strings; `tea_time` versus `team` distinguishes a subsequence from a substring; `ab` versus `ba` demonstrates two valid optimal answers.

### 2.2 Adjacent-block merge — interval DP

**State:** `dp[l][r]` is the least cost of replacing the original contiguous blocks `l..r` by one block. With only one block, no merge is needed: `dp[i][i]=0`. Define `sum(l,r)=prefix[r+1]-prefix[l]` using 64-bit prefix sums.

**Transition:** Any final merge of interval `[l,r]` must combine exactly two neighbouring completed intervals `[l,k]` and `[k+1,r]`. Thus

`dp[l][r]=min over l<=k<r of (dp[l][k] + dp[k+1][r] + sum(l,r))`.

Evaluate increasing interval lengths, so the two subintervals have already been solved. Record the `k` attaining the minimum in `split[l][r]`.

**Witness:** Recursively output the left child's chronological plan, then the right child's, then the parent `[l,k,r]`. Each interval exists before it is merged into its parent, hence every plan step merges two adjacent current blocks. Induction over interval length proves both optimal cost and valid reconstruction. The recursion depth is at most 200, within the published bound. For `n<2`, cost 0 and an empty plan are correct; ties permit other valid schedules.

**Complexity:** O(n³) time, O(n²) auxiliary DP/split tables and O(n) plan/reconstruction stack for n≥1; O(1) for n=0. Boundaries: empty/singleton, zero sizes, tied `[1,1,1]` plans, a counterexample to unrestricted Huffman-style nonadjacent merges, and 200 blocks of `10^9` requiring 64-bit costs.

### 2.3 Tree maximum-weight independent set — take/skip DP

**State:** Root the tree at vertex 0. `take[v]` is the best weight of an independent vertex set in the subtree of `v` **including** `v`. `skip[v]` is the best value **excluding** `v`.

**Transition:** For each child `u`, inclusion of `v` forbids including `u`, whereas exclusion of `v` allows the better choice in `u`'s subtree:

`take[v]=weights[v]+Σ skip[u]` and `skip[v]=Σ max(take[u],skip[u])`.

An iterative traversal produces `parent[]` and `order[]`; processing `order[]` backward computes every child's states before its parent. For reconstruction traverse forward, include `v` only if its parent is not selected and `take[v]>skip[v]`; otherwise skip. This is an optimal decision at every subtree given its parent constraint; ties choose exclusion. An empty set is allowed, giving zero on all-negative trees. For `n=0` return weight 0 and no selected vertices. No recursive DFS is used, avoiding stack overflow on a 100,000-vertex chain.

**Correctness:** If a vertex is taken, every child must be skipped; if it is skipped, child subproblems are independent and can choose the larger option. This exhaustive local dichotomy proves the recurrences by postorder induction. Reconstruction exactly follows the states and never selects adjacent vertices; the sum equals the optimum.

**Complexity:** O(n+1) time and O(n+1) auxiliary memory for adjacency arrays, parents, order, both DP arrays and selection flags; O(n) output. Boundaries: empty/single node, negative values, zero/tie, star, randomly rooted trees, and 100k-deep chain.

### 2.4 Naive exact string search

Scan every possible start index `i=0..n-m` and compare `pattern[j]` to `text[i+j]` for all `j`. The loop reports exactly those starts where all `m` positions match; evaluating consecutive starts preserves overlaps and ascending order. For `m=0`, return all boundaries `0..n`; if `m>n`, return `[]`. There is no built-in substring-search call in the implementation.

**Complexity:** O(n*m+n+1) worst-case time (includes output), O(1) auxiliary memory and O(z+1) output memory, with z reported positions. Boundaries: empty text/pattern, longer pattern, punctuation and overlapping `aaa` in `aaaaa`.

### 2.5 KMP exact string search

`pi[i]` stores the length of the longest proper prefix of `pattern[0..i]` that is also a suffix. During prefix construction and search, a mismatch falls back to `pi[j-1]` without discarding valid candidate prefixes. The matched-prefix length `j` remains the longest prefix matching the text suffix already processed. When `j==m`, report the start and set `j=pi[j-1]` to preserve overlaps.

**Correctness:** The failure function enumerates all possible proper border lengths in descending order, so every skipped restart is provably impossible and every possible match is retained. Positions are emitted when their entire pattern has been read, hence sorted and exact. Empty pattern matches all n+1 boundaries.

**Complexity:** O(n+m+z+1) time, O(m+1) auxiliary memory for prefix values and O(z+1) output memory. Boundary: `abababa` versus `aba`, matches at 0, 2 and 4; huge repetitive text with 100k-character pattern.

### 2.6 Exact Rabin–Karp

Use polynomial fingerprint `h(c0..c(m-1)) = Σ code(cj)*31^(m-1-j) mod 101`, with the **leftmost** character highest-power. Compute the pattern hash, first window hash and `power=31^(m-1) mod 101`. On sliding from window `i` to `i+1`, remove `code(text[i])*power`, normalize any negative residue to `[0,100]`, multiply by 31 and append the new rightmost code. Compare actual characters for **every equal-hash candidate**, never trusting a hash equality alone.

**Correctness:** The update is algebraically the hash of the new window; unequal hashes cannot be equal strings. When hashes match, an explicit character-by-character comparison decides exact equality, so collisions create only extra work, not false positives. Preserve overlaps and the same empty-pattern contract as naive/KMP.

**Complexity:** For 1≤m≤n, O(n+m+c*m) time, c being the number of equal-hash windows; worst-case O(n*m+n+m+1). O(1) auxiliary memory (stored rolling values) and O(z+1) output. The small modulus intentionally allows collisions; `ab` and `dj` hash alike (75) but are *not* equal, and `text=abdj, pattern=ab` must output only `[0]`. The three methods are cross-checked on identical text/pattern inputs.

### 2.7 Suffix array — doubling with radix/counting sort; Kasai LCP

For nonempty text, initially rank each suffix start by its first ASCII character (rank >0, 0 reserved for nonexistent suffix parts). In doubling round `span`, a suffix is ordered by the pair `(rank[i], rank[i+span] or 0)`. Two **stable counting-sort passes** sort indices by the second component and then the first; pair equality determines new equivalence classes. Inductively, after each round the pairs represent equal/ordered prefixes of doubled length. Rounds end when all suffixes receive different ranks or enough characters have been considered. Exactly the n nonempty suffix starts are produced: no sentinel suffix is included. Output array `sa` is a permutation of `[0,n)` in lexicographic order.

Kasai first constructs `inverse[sa[i]]=i`. For each original start `i`, compare suffix `i` with its lexicographic predecessor, reusing at least `common-1` characters from the previous iteration. Set `lcp[inverse[i]]=common`, except `lcp[0]=0`. Kasai's reuse follows because removing the first character of two suffixes with common prefix length k leaves a common prefix of at least k-1. Each match extension and reduction amortizes to O(n) total.

The maximum adjacent `lcp` value is the longest repeated-substring length: every repeated substring is a common prefix of at least two suffixes; in lexicographically sorted order, two suffixes sharing that prefix lie in one contiguous interval, and some adjacent pair shares it. Return any substring attaining the maximum. Overlap is allowed. For n=0 return empty arrays and `""`.

**Complexity:** Stable radix/counting sort costs O(n) per doubling round, giving O(n log(n+1)) time, O(n+1) auxiliary and O(n+1) output memory; Kasai adds O(n) time/memory. Boundaries: empty and one-char, `banana`/`ana`, all equal, all distinct, 50k identical characters, and a direct small-input sorted-suffix/LCP oracle.

### 2.8 Aho–Corasick — multiple dictionary patterns

Insert each nonempty pattern into a trie of ASCII transitions, remembering the terminal node of **each original dictionary position**; duplicate dictionary positions can share a terminal. BFS builds failure links to the longest proper suffix that is also a trie prefix. Missing transitions inherit the failure-state transition, yielding a complete 95-character ASCII automaton. For a text, process characters once; visit its reached automaton state. In reverse BFS order, aggregate each state's visit count to its failure parent, which adds occurrences of suffix patterns. Finally, each pattern's count is the visits at its terminal, including duplicate dictionary entries.

**Correctness:** After consuming each character, the state encodes the longest dictionary-prefix suffix of the text seen so far. A pattern ends at the current text position exactly when its terminal is on the failure chain of the current state. Reverse failure propagation counts that state once for every such ending position without enumerating dense matches. Because each text receives a fresh visits array and starts in root state, identical independent texts return identical counts and empty texts return zero rows. An empty dictionary yields one empty output row for each text; an empty texts array returns `[]`.

**Complexity:** Let L=sum of pattern lengths, p=number of patterns, S=number of trie states (≤L+1), q=number of texts and T=total text length. Trie building and BFS: O(L+95S+p) time, O(95S+p) auxiliary memory (dense int transition rows). Per text: O(length(text)+S+p) for scan, reverse propagation and row output. Across all texts: O(L+95S+T+q(S+p)) time and O(95S+p+max text work) auxiliary memory plus O(q*p) output. Terminal lookup avoids per-match traversal, which can otherwise explode for repeated `a` patterns. Boundaries: no patterns/no texts, duplicates, overlaps, suffix patterns (`he` ends inside `she`), 10k length-10 random patterns, and 10k duplicate dense patterns.

## 3. Student tests: independent expected values and actual outputs

`tests/student/generate_cases.py` creates 170 deterministic mixed requests with seed **20261107** and writes both `inputs.jsonl` and `expected.jsonl`. Small-domain reference oracles were implemented independently of the submitted Java algorithms: exhaustive subsequence enumeration for LCS; enumerated final splits for ordered merge; exhaustive independent subsets for trees; direct window comparison for search and Aho counts; and actual suffix sorting plus direct LCP comparison. `actual_outputs.jsonl` was captured by running the Java adapter on **the entire JSONL input in one process**; `verify_student.py` checks all 170 stored requests and witnesses against their independent optimums and invariants.

| Command | Input fixture | Expected / actual verification | Defect targeted |
|---|---|---|---|
| `lcs` | `"tea_time", "team"` | length 4; valid `team` subsequence | Subsequence vs substring |
| `lcs` | `"ab", "ba"` | length 1; `a` and `b` both accepted; invalid `ab` rejected | Non-unique optimal witnesses |
| `merge` | `[2,3,4]`, `[1,1,1]`, zeros | optimal cost and legal chronological plan; both tied schedules valid | Nonadjacent merge and chronology |
| `tree` | `[4,9,6]`, edges `(0,1),(1,2)` | weight 10 with nonadjacent vertices; negatives return 0 | Independent set / empty set |
| `naive`/`kmp`/`rk` | `aaaaa` / `aaa`, empty pattern | `[0,1,2]`; every empty-pattern boundary | Overlapping and zero-length matches |
| `rk` | `abdj` / `ab` | `[0]`; `dj` is a hash collision, not a match | Hash collision verification |
| `suffix` | `banana`, empty, `aaaaa`, random strings | exact suffix permutation and LCP, maximal valid repeated substring | Missing sentinel, prefix ordering, overlaps |
| `aho` | `[he,she,he]`, texts `[sheshe,sheshe,""]` | `[[2,2,2],[2,2,2],[0,0,0]]` | Duplicates, suffixes, reset |

For remaining randomized cases, compare `tests/student/expected.jsonl` and `tests/student/actual_outputs.jsonl` line by line and run `python tests/student/verify_student.py`. The witness checker verifies validity and optimal value rather than demanding the same witness as one reference output. It explicitly accepts alternative tie witnesses for LCS, merging and trees and rejects structurally invalid witnesses.

### Maximum-size / stress checks

All the following were actually run with the Java process heap set to 384 MiB and a 20-second timeout **per case**; complete wall-clock measurements and output sizes are in `tests/student/stress_results.txt`:

- LCS: 1000×1000 equal characters; 1000-length optimal sequence.
- Merge: n=200 blocks each `10^9`; checked chronological merge plan and optimum of `1544 * 10^9` from equal-leaf binary-tree depth lower bound.
- Tree: 100,000-vertex chain; selected sum `50,000 * 10^9`, no adjacent picks.
- KMP: 200,000-character text and 100,000-character pattern; exactly 100,001 overlapping occurrences.
- RK: text 10,000 and pattern 2,000; worst published length product 20 million; exact no-match case.
- Suffix: 50,000 identical characters; SA is descending starts; LCP `[0,1,...,49999]` and repeated substring of length 49,999.
- Aho sparse: 10,000 random dictionary strings totalling 100,000 characters, 100 texts totalling 200,000 characters; expected zero matches by disjoint alphabets.
- Aho dense: 10,000 dictionary positions with many duplicates and suffix overlaps, one text of 200,000 `a` characters; verified each expected occurrence count.

## 4. Reproducible Naive/KMP/Rabin–Karp benchmark

**Correctness first:** For every benchmark input, all three algorithm outputs were checked against an independent Java testing oracle that uses `String.startsWith(pattern,i)` at all possible starts. This built-in function is used **only in the test/benchmark verification**, not inside the submitted algorithms. A mismatch terminates the benchmark.

**Generator and sizes:** Java `Random(seed)` with seeds 20261107, 20261108 and 20261109 respectively; random printable ASCII text with a guaranteed embedded `m`-character substring pattern taken from that same text. The repetitive family uses a text of `n` copies of `a` and a pattern of `(m-1)` copies of `a` plus one terminal `b` (long near-matches, no actual match). For both families, `(n,m)=(250,25),(1000,100),(3000,400)` and each input is held fixed across all three algorithms. All pairs are within the common published domain.

**Timer and boundary:** `System.nanoTime()` in Java, including one algorithm invocation and construction of the returned list/map, excluding JSON parsing, JVM startup, benchmark input generation, the correctness oracle, printing and compilation. For each algorithm and input: 300 unmeasured warm-up invocations plus two unmeasured stabilization batches, then **five measured batches**. Each raw CSV time is mean microseconds per invocation in a batch of 40 calls (random) or 12 calls (repetitive); the median is the third sorted batch mean. Same batch size/boundary across algorithms on a given input. Measurements use a volatile sink to prevent unused results. JIT, GC and shared CPU load can still affect the microbenchmarks.

**Commands:** `javac --release 17 -cp Java/build -d Java/build tests/student/SearchBenchmark.java` then `java -Xmx384m -cp Java/build SearchBenchmark > tests/student/search_benchmark.csv`; compile `Java/source/*.java` first using `python tools/run.py --setup`. The original unrounded samples are limited by three-decimal CSV precision. The table below records all five raw batch measurements and medians in microseconds per algorithm invocation (µs/call).

| Case | n | m | Seed | Algorithm | Run 1 | Run 2 | Run 3 | Run 4 | Run 5 | Median | Verified |
|---|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---|
| random | 250 | 25 | 20261107 | naive | 6.354 | 6.034 | 5.631 | 6.926 | 6.705 | 6.354 | yes |
| random | 250 | 25 | 20261107 | kmp | 8.236 | 10.034 | 10.062 | 9.042 | 7.972 | 9.042 | yes |
| random | 250 | 25 | 20261107 | rk | 7.665 | 8.494 | 7.762 | 7.788 | 8.170 | 7.788 | yes |
| repetitive | 250 | 25 | 20261107 | naive | 13.084 | 13.162 | 13.163 | 12.984 | 13.522 | 13.162 | yes |
| repetitive | 250 | 25 | 20261107 | kmp | 16.824 | 21.338 | 19.345 | 16.541 | 15.465 | 16.824 | yes |
| repetitive | 250 | 25 | 20261107 | rk | 6.989 | 9.517 | 7.166 | 8.221 | 7.073 | 7.166 | yes |
| random | 1000 | 100 | 20261108 | naive | 14.934 | 14.052 | 15.586 | 14.109 | 5.389 | 14.109 | yes |
| random | 1000 | 100 | 20261108 | kmp | 2.637 | 2.517 | 2.237 | 2.178 | 2.182 | 2.237 | yes |
| random | 1000 | 100 | 20261108 | rk | 7.601 | 7.602 | 8.089 | 7.944 | 7.654 | 7.654 | yes |
| repetitive | 1000 | 100 | 20261108 | naive | 144.031 | 107.723 | 116.692 | 106.505 | 107.779 | 107.779 | yes |
| repetitive | 1000 | 100 | 20261108 | kmp | 4.453 | 4.447 | 4.384 | 4.452 | 4.371 | 4.447 | yes |
| repetitive | 1000 | 100 | 20261108 | rk | 7.306 | 7.304 | 8.589 | 7.308 | 7.302 | 7.306 | yes |
| random | 3000 | 400 | 20261109 | naive | 4.230 | 4.254 | 4.195 | 4.199 | 4.231 | 4.230 | yes |
| random | 3000 | 400 | 20261109 | kmp | 6.953 | 8.395 | 8.012 | 7.230 | 6.321 | 7.230 | yes |
| random | 3000 | 400 | 20261109 | rk | 26.078 | 23.144 | 24.020 | 25.062 | 24.681 | 24.681 | yes |
| repetitive | 3000 | 400 | 20261109 | naive | 1227.887 | 1223.659 | 1218.680 | 1268.112 | 1276.073 | 1227.887 | yes |
| repetitive | 3000 | 400 | 20261109 | kmp | 13.110 | 13.059 | 14.274 | 13.130 | 13.078 | 13.110 | yes |
| repetitive | 3000 | 400 | 20261109 | rk | 21.527 | 21.498 | 21.483 | 22.446 | 21.499 | 21.499 | yes |

**Interpretation:** On long near-miss repetitive strings, naive search repeats many comparisons at each start and is increasingly slower; KMP reuses its prefix borders and remains linear. RK quickly rejects unequal hashes in this specific case, so it can also be fast. On random data, naive often stops its candidate checks at the first character, and constants/JIT noise matter more than worst-case asymptotics. This RK uses a deliberately tiny modulus 101: on collision-rich data it must verify candidates character-by-character and may take O(nm), so these measurements are **not** a guarantee for RK performance in general. None of these timings proves complexity; the bounds follow from the algorithms.

## 5. Sources, assistance and verification

- **Primary specification and interface:** the supplied `RZhaniya/daa-assignment-4-archive` template, its `README.md`, `docs/guide/CONTRACTS.md`, `docs/guide/TESTING.md`, `docs/guide/RUBRIC.md`, and supplied adapters/launcher.
- **Algorithm concepts:** dynamic programming, KMP prefix fallback, polynomial rolling hashes, suffix doubling/Kasai and failure links as described by the supplied lecture/reference notes; no external code was copied.
- **AI assistance:** OpenAI ChatGPT assisted with drafting the Java implementation, independent test scripts, explanations and this report. The assistant ran the official public launcher, 170 independent student fixtures, scale tests and the benchmark in a Linux container. The student must personally review and understand the submission, verify it in their own GitHub Actions run and disclose assistance as required by university policy. No AI help is permitted during the individual assessed defence.
- **Limit of verification:** local successful tests do not assert the final instructor score, official private tests or access to the student's private GitHub/Moodle accounts.
