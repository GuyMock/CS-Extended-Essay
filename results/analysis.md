# What to discuss in the Extended Essay

Ground the discussion in the exported numbers (`raw_results.csv`, `summary.csv`).
The central comparison is that the two crackers scale in fundamentally different
ways. **Series A** isolates password length L: with the character set fixed at
N = 26, the brute-force cracker's mean guess count grows geometrically with each
extra character (roughly a factor of N per length), which is the empirical
signature of its O(N^L) search space, whereas the heuristic's effort is flat
because it never enumerates that space. **Series B** isolates the character-set
size N at fixed L = 3, and the brute-force guess count again rises steeply from
numeric (10) through to full ASCII (95), confirming the same N^L relationship from
the other variable. You should present these two sweeps as the guesses-against-L
and guesses-against-N graphs, note that both axes behave as the exponential model
predicts, and use a log-scaled y-axis so the exponential growth reads as a straight
line. Discuss why time tracks guesses closely but is noisier (JIT warm-up, OS
scheduling) which is exactly why each condition was timed over repeats and averaged.

The predictable-target results carry the argument to the real world. On the short
lowercase words the two crackers can be compared on finite numbers, and the
heuristic reaches the target in orders of magnitude fewer guesses because a common
word sits near the front of its dictionary rather than deep inside an alphabetical
enumeration. On the realistic passwords (e.g. `p@ssw0rd`, `H3llo123`) the heuristic
still succeeds within a few thousand guesses by composing dictionary words with
case, leetspeak and suffix rules, while brute force exhausts a 2,000,000-guess
budget without success — a concrete demonstration that exhaustive search is
computationally infeasible against an 8–10 character mixed-case password even
though that password is, to a human, obviously weak. The key evaluative point is
that guessing effort is not the same as password strength: the heuristic exposes
how predictability (low entropy), not length or character-set size alone,
determines real-world crackability. Conclude by linking this back to entropy — the
brute-force numbers measure the theoretical keyspace, the heuristic numbers measure
how little of it a smart attacker actually needs to search — and acknowledge the
limitations (single machine, small L ceiling for feasibility, a hand-built
dictionary) as directions the investigation could extend.