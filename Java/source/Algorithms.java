import java.util.*;

/** Assignment 4: eight algorithms with the official Java 17 JSONL adapters. */
public final class Algorithms {
    private Algorithms() {}

    private static long num(Object x) { return ((Number) x).longValue(); }
    private static int integer(Object x) { return ((Number) x).intValue(); }
    @SuppressWarnings("unchecked")
    private static List<Object> array(Map<String, Object> in, String name) {
        return (List<Object>) in.get(name);
    }

    // Prefix LCS dynamic programming, with an optimal subsequence witness.
    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = (String) input.get("a"), b = (String) input.get("b");
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; ++i)
            for (int j = 1; j <= m; ++j)
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                    ? dp[i - 1][j - 1] + 1 : Math.max(dp[i - 1][j], dp[i][j - 1]);
        StringBuilder backwards = new StringBuilder();
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                backwards.append(a.charAt(i - 1)); --i; --j;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) --i;
            else --j;
        }
        return Map.of("length", dp[n][m], "sequence", backwards.reverse().toString());
    }

    // Interval DP for ordered adjacent merges, including chronological witness.
    static Map<String, Object> merge(Map<String, Object> input) {
        List<Object> raw = array(input, "sizes");
        int n = raw.size();
        if (n < 2) return Map.of("cost", 0L, "plan", List.of());
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; ++i) prefix[i + 1] = prefix[i] + num(raw.get(i));
        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];
        for (int len = 2; len <= n; ++len) {
            for (int left = 0; left + len <= n; ++left) {
                int right = left + len - 1;
                long rangeSum = prefix[right + 1] - prefix[left];
                long best = Long.MAX_VALUE;
                int bestK = left;
                for (int k = left; k < right; ++k) {
                    long candidate = dp[left][k] + dp[k + 1][right] + rangeSum;
                    if (candidate < best) { best = candidate; bestK = k; }
                }
                dp[left][right] = best;
                split[left][right] = bestK;
            }
        }
        List<int[]> plan = new ArrayList<>(n - 1);
        reconstructMerge(0, n - 1, split, plan);
        return Map.of("cost", dp[0][n - 1], "plan", plan);
    }
    private static void reconstructMerge(int l, int r, int[][] split, List<int[]> plan) {
        if (l == r) return;
        int k = split[l][r];
        reconstructMerge(l, k, split, plan);
        reconstructMerge(k + 1, r, split, plan);
        plan.add(new int[]{l, k, r});
    }

    // Tree maximum-weight independent set, iterative traversal (safe for 100k depth).
    static Map<String, Object> tree(Map<String, Object> input) {
        int n = integer(input.get("n"));
        if (n == 0) return Map.of("weight", 0L, "selected", List.of());
        List<Object> rawWeights = array(input, "weights"), rawEdges = array(input, "edges");
        long[] weights = new long[n];
        for (int i = 0; i < n; ++i) weights[i] = num(rawWeights.get(i));
        int[] head = new int[n]; Arrays.fill(head, -1);
        int[] to = new int[2 * (n - 1)], next = new int[2 * (n - 1)];
        int count = 0;
        for (Object edge : rawEdges) {
            List<?> e = (List<?>) edge;
            int u = integer(e.get(0)), v = integer(e.get(1));
            to[count] = v; next[count] = head[u]; head[u] = count++;
            to[count] = u; next[count] = head[v]; head[v] = count++;
        }
        int[] parent = new int[n], order = new int[n];
        Arrays.fill(parent, -2);
        parent[0] = -1; order[0] = 0;
        int visited = 1;
        for (int idx = 0; idx < visited; ++idx) {
            int v = order[idx];
            for (int e = head[v]; e != -1; e = next[e]) {
                int child = to[e];
                if (child == parent[v]) continue;
                parent[child] = v;
                order[visited++] = child;
            }
        }
        long[] take = new long[n], skip = new long[n];
        for (int idx = n - 1; idx >= 0; --idx) {
            int v = order[idx];
            take[v] = weights[v];
            for (int e = head[v]; e != -1; e = next[e]) {
                int child = to[e];
                if (parent[child] == v) {
                    take[v] += skip[child];
                    skip[v] += Math.max(take[child], skip[child]);
                }
            }
        }
        boolean[] chosen = new boolean[n];
        List<Integer> selected = new ArrayList<>();
        for (int idx = 0; idx < n; ++idx) {
            int v = order[idx];
            boolean parentChosen = parent[v] != -1 && chosen[parent[v]];
            if (!parentChosen && take[v] > skip[v]) {
                chosen[v] = true;
                selected.add(v);
            }
        }
        return Map.of("weight", Math.max(take[0], skip[0]), "selected", selected);
    }

    private static List<Integer> emptyPatternPositions(int textLength) {
        List<Integer> out = new ArrayList<>(textLength + 1);
        for (int i = 0; i <= textLength; ++i) out.add(i);
        return out;
    }

    static Map<String, Object> naive(Map<String, Object> input) {
        String text = (String) input.get("text"), pattern = (String) input.get("pattern");
        int n = text.length(), m = pattern.length();
        if (m == 0) return Map.of("positions", emptyPatternPositions(n));
        List<Integer> found = new ArrayList<>();
        for (int start = 0; start + m <= n; ++start) {
            int j = 0;
            while (j < m && text.charAt(start + j) == pattern.charAt(j)) ++j;
            if (j == m) found.add(start);
        }
        return Map.of("positions", found);
    }

    static Map<String, Object> kmp(Map<String, Object> input) {
        String text = (String) input.get("text"), pattern = (String) input.get("pattern");
        int n = text.length(), m = pattern.length();
        if (m == 0) return Map.of("positions", emptyPatternPositions(n));
        int[] pi = new int[m];
        for (int i = 1, j = 0; i < m; ++i) {
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (pattern.charAt(i) == pattern.charAt(j)) ++j;
            pi[i] = j;
        }
        List<Integer> found = new ArrayList<>();
        for (int i = 0, j = 0; i < n; ++i) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (text.charAt(i) == pattern.charAt(j)) ++j;
            if (j == m) {
                found.add(i - m + 1);
                j = pi[j - 1];
            }
        }
        return Map.of("positions", found);
    }

    static Map<String, Object> rk(Map<String, Object> input) {
        String text = (String) input.get("text"), pattern = (String) input.get("pattern");
        int n = text.length(), m = pattern.length();
        if (m == 0) return Map.of("positions", emptyPatternPositions(n));
        List<Integer> found = new ArrayList<>();
        if (m > n) return Map.of("positions", found);
        final long base = 31, mod = 101;
        long highestPower = 1, targetHash = 0, windowHash = 0;
        for (int j = 0; j < m; ++j) {
            targetHash = (targetHash * base + pattern.charAt(j)) % mod;
            windowHash = (windowHash * base + text.charAt(j)) % mod;
            if (j + 1 < m) highestPower = highestPower * base % mod;
        }
        for (int i = 0; i + m <= n; ++i) {
            if (windowHash == targetHash) {
                int j = 0;
                while (j < m && text.charAt(i + j) == pattern.charAt(j)) ++j;
                if (j == m) found.add(i);
            }
            if (i + m < n) {
                windowHash = (windowHash - text.charAt(i) * highestPower) % mod;
                if (windowHash < 0) windowHash += mod;
                windowHash = (windowHash * base + text.charAt(i + m)) % mod;
            }
        }
        return Map.of("positions", found);
    }

    // Stable counting sort of suffix indices by one rank component.
    private static void rankCountingSort(int[] from, int[] to, int[] rank,
                                         int offset, int maxRank, int[] count) {
        Arrays.fill(count, 0, maxRank + 1, 0);
        int n = rank.length;
        for (int id : from) ++count[id + offset < n ? rank[id + offset] : 0];
        int prefix = 0;
        for (int i = 0; i <= maxRank; ++i) {
            int value = count[i]; count[i] = prefix; prefix += value;
        }
        for (int id : from) {
            int key = id + offset < n ? rank[id + offset] : 0;
            to[count[key]++] = id;
        }
    }

    static Map<String, Object> suffix(Map<String, Object> input) {
        String text = (String) input.get("text");
        int n = text.length();
        if (n == 0) return Map.of("sa", new int[0], "lcp", new int[0], "repeated", "");
        int[] sa = new int[n], rank = new int[n], nextRank = new int[n];
        int[] tmp = new int[n], count = new int[Math.max(256, n + 2)];
        for (int i = 0; i < n; ++i) {
            sa[i] = i;
            rank[i] = text.charAt(i) + 1;
        }
        int maxRank = 128;
        for (int span = 1; span < n; span *= 2) {
            rankCountingSort(sa, tmp, rank, span, maxRank, count);
            rankCountingSort(tmp, sa, rank, 0, maxRank, count);
            int classes = 1;
            nextRank[sa[0]] = classes;
            for (int i = 1; i < n; ++i) {
                int first = sa[i - 1], second = sa[i];
                int firstTail = first + span < n ? rank[first + span] : 0;
                int secondTail = second + span < n ? rank[second + span] : 0;
                if (rank[first] != rank[second] || firstTail != secondTail) ++classes;
                nextRank[second] = classes;
            }
            int[] old = rank; rank = nextRank; nextRank = old;
            maxRank = classes;
            if (classes == n) break;
        }
        // For n=1 or very short identical prefix groups the initial sa remains correct.
        int[] inverse = new int[n];
        for (int i = 0; i < n; ++i) inverse[sa[i]] = i;
        int[] lcp = new int[n];
        int common = 0, best = 0, bestIndex = 0;
        for (int i = 0; i < n; ++i) {
            int position = inverse[i];
            if (position == 0) { common = 0; continue; }
            int other = sa[position - 1];
            while (i + common < n && other + common < n &&
                   text.charAt(i + common) == text.charAt(other + common)) ++common;
            lcp[position] = common;
            if (common > best) { best = common; bestIndex = i; }
            if (common > 0) --common;
        }
        return Map.of("sa", sa, "lcp", lcp,
                      "repeated", best == 0 ? "" : text.substring(bestIndex, bestIndex + best));
    }

    // Dense ASCII Aho-Corasick automaton; reverse failure propagation avoids
    // per-match work even for very dense overlapping/deduplicated dictionaries.
    static Map<String, Object> aho(Map<String, Object> input) {
        List<Object> patterns = array(input, "patterns"), texts = array(input, "texts");
        int p = patterns.size(), totalLength = 0;
        for (Object pattern : patterns) totalLength += ((String) pattern).length();
        int[][] go = new int[totalLength + 1][95];
        int[] failure = new int[totalLength + 1], terminals = new int[p];
        int size = 1;
        for (int i = 0; i < p; ++i) {
            String pattern = (String) patterns.get(i);
            int v = 0;
            for (int k = 0; k < pattern.length(); ++k) {
                int key = pattern.charAt(k) - 32;
                if (go[v][key] == 0) go[v][key] = size++;
                v = go[v][key];
            }
            terminals[i] = v;
        }
        int[] bfsOrder = new int[size];
        int tail = 0;
        for (int c = 0; c < 95; ++c) {
            int u = go[0][c];
            if (u != 0) bfsOrder[tail++] = u;
        }
        for (int index = 0; index < tail; ++index) {
            int v = bfsOrder[index];
            for (int c = 0; c < 95; ++c) {
                int u = go[v][c];
                if (u != 0) {
                    failure[u] = go[failure[v]][c];
                    bfsOrder[tail++] = u;
                } else {
                    go[v][c] = go[failure[v]][c];
                }
            }
        }
        List<long[]> results = new ArrayList<>(texts.size());
        for (Object rawText : texts) {
            String text = (String) rawText;
            long[] visits = new long[size];
            int state = 0;
            for (int i = 0; i < text.length(); ++i) {
                state = go[state][text.charAt(i) - 32];
                ++visits[state];
            }
            for (int i = tail - 1; i >= 0; --i) {
                int v = bfsOrder[i];
                visits[failure[v]] += visits[v];
            }
            long[] row = new long[p];
            for (int i = 0; i < p; ++i) row[i] = visits[terminals[i]];
            results.add(row);
        }
        return Map.of("counts", results);
    }
}
