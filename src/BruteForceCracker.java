// The "dumb" cracker. It doesn't think at all - it just tries every
// possible combination of characters, one after another, until it stumbles onto
// the right password. Like trying 000, 001, 002... on a combination lock until it
// opens. It ALWAYS works eventually, but "eventually" can mean billions of years
// for a long password, which is exactly the point I'm proving.
public class BruteForceCracker {

    // how many guesses we've made (just for reporting afterwards)
    private long guesses;
    // did we find the password?
    private boolean found;
    // the guess we're currently testing
    private String currentGuess;
    // the characters we're allowed to use in guesses
    private char[] charset;
    // the longest guess we're willing to build
    private int passwordLength;

    // A shortcut version with no guess limit - just tries forever until it finds it.
    // It calls the real version below and passes in the biggest number possible as
    // the "limit", which effectively means "no limit".
    public boolean crack(String target, char[] charset, int maxLength) {
        return crack(target, charset, maxLength, Long.MAX_VALUE);
    }

    // The real workhorse. It tries every guess of length 1, then every guess of
    // length 2, and so on up to maxLength, until it either finds the password or
    // hits the maxGuesses limit. The limit matters because for a long password over
    // the full keyboard there are more combinations than we could ever finish - so
    // I cap it and honestly report "couldn't find it within X guesses".
    public boolean crack(String target, char[] charset, int maxLength, long maxGuesses) {
        this.charset = charset;
        this.passwordLength = maxLength;
        this.found = false;

        // tell the shared notebook we're starting - resets counters + stopwatch
        Data.startRun(target);

        // Try length 1 first, then 2, then 3... up to maxLength.
        // The "&& !found" means: stop early the moment we find it.
        for (int len = 1; len <= maxLength && !found; len++) {

            // This int array is the clever trick. Instead of storing the guess as
            // letters, I store the POSITION of each letter in the charset. So idx
            // is like the dials on a combination lock. All zeros = "aaa", and we
            // count upwards from there. It starts full of zeros.
            int[] idx = new int[len];
            boolean exhausted = false;   // have we tried every combo of this length yet?

            while (!exhausted) {

                // Safety valve: if we've hit our guess budget, give up and report
                // that we didn't find it.
                if (Data.totalGuesses >= maxGuesses) {
                    return false;
                }

                // Turn the "dial positions" back into an actual text guess by
                // looking up which character sits at each position.
                StringBuilder sb = new StringBuilder(len);
                for (int i = 0; i < len; i++) {
                    sb.append(charset[idx[i]]);
                }
                currentGuess = sb.toString();

                // Test this guess. If it's right, mark it and bail out of the loop.
                if (Data.checkGuess(currentGuess)) {
                    found = true;
                    break;
                }

                // Now "add one" to the combination lock, exactly like an odometer
                // in a car rolling over. Start at the rightmost dial:
                int pos = len - 1;
                while (pos >= 0) {
                    idx[pos]++;   // click this dial up by one
                    // if it didn't roll past the end of the charset, we're done
                    if (idx[pos] < charset.length) {
                        break;
                    }
                    // otherwise it rolled over: reset this dial to 0 and carry over
                    // to the dial on its left (just like 099 -> 100)
                    idx[pos] = 0;
                    pos--;
                }
                // If pos went below 0, it means even the leftmost dial rolled over,
                // so we've tried every single combination of this length. Time to
                // move up to the next length.
                if (pos < 0) {
                    exhausted = true;
                }
            }
        }

        // remember the final guess count and hand back whether we found it
        this.guesses = Data.totalGuesses;
        return found;
    }

    // simple getters so Main can ask for the results afterwards
    public long getGuesses() {
        return guesses;
    }

    public boolean isFound() {
        return found;
    }
}
