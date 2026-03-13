package com.gymapp.core.model

/**
 * Computes the Levenshtein edit distance between two strings (case-insensitive).
 */
fun levenshteinDistance(a: String, b: String): Int {
    val s1 = a.lowercase()
    val s2 = b.lowercase()
    val m = s1.length
    val n = s2.length
    // Rolling two-row DP — O(m*n) time, O(n) space
    var prev = IntArray(n + 1) { it }
    var curr = IntArray(n + 1)
    for (i in 1..m) {
        curr[0] = i
        for (j in 1..n) {
            curr[j] = if (s1[i - 1] == s2[j - 1]) prev[j - 1]
                      else 1 + minOf(prev[j], curr[j - 1], prev[j - 1])
        }
        val tmp = prev; prev = curr; curr = tmp
    }
    return prev[n]
}

/**
 * Returns candidates from [pool] that are "close" to [query] but are NOT already
 * returned by a simple prefix/contains search.
 *
 * Threshold scales with query length:
 *   ≤4 chars  → distance ≤ 1  (e.g. "sqat" → "squat")
 *   5-7 chars → distance ≤ 2  (e.g. "benchp" → "bench press")
 *   8+ chars  → distance ≤ 3  (e.g. "deadliff" → "deadlift")
 */
fun findFuzzyMatches(
    query: String,
    pool: List<String>,
    maxResults: Int = 5,
): List<String> {
    if (query.isBlank() || pool.isEmpty()) return emptyList()
    val q = query.trim()
    val threshold = when {
        q.length <= 4 -> 1
        q.length <= 7 -> 2
        else -> 3
    }
    return pool
        .filter { c ->
            // exclude names already surfaced by prefix/contains matching
            !c.contains(q, ignoreCase = true) && !q.contains(c, ignoreCase = true)
        }
        .map { c -> c to levenshteinDistance(q, c) }
        .filter { (_, d) -> d <= threshold }
        .sortedBy { (_, d) -> d }
        .take(maxResults)
        .map { (name, _) -> name }
}
