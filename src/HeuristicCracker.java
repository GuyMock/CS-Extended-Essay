import java.util.ArrayList;
import java.util.List;

// The "smart" cracker. Instead of blindly trying every combination like the brute
// force one, this one guesses like a real hacker would: it starts with a list of
// common words, and then tries the obvious human tricks people use to "strengthen"
// them - Capitalising the first letter, swapping letters for lookalike numbers
// (like a -> @, e -> 3), and sticking common endings on the end (like "123" or "!").
// Because most weak passwords ARE just a common word with these tweaks, it finds
// them ridiculously fast compared to brute force.
public class HeuristicCracker {

    // how many guesses we made
    private long guesses;
    // did we find it?
    private boolean found;

    // the list of common base words to start from
    private String[] dictionary;
    // common things people tack onto the end of passwords
    private String[] commonSuffixes;
    // letter -> lookalike-symbol swaps (this is called "leetspeak")
    private String[][] substitutions;
    // the guess we're currently testing
    private String currentGuess;

    // This fills in all the word lists. I only do it once, the first time crack()
    // gets called - the "if not null, just leave" check makes sure it doesn't waste
    // time redoing it on every run.
    private void init() {
        if (dictionary != null) {
            return;
        }

        // The common words to build guesses from. These are the passwords that show
        // up over and over on "most common passwords" leaked lists.
        dictionary = new String[] {
                "password", "hello", "admin", "welcome", "qwerty",
                "dragon", "monkey", "football", "letmein", "login",
                "master", "shadow", "superman", "iloveyou", "sunshine"
        };

        // Endings people love to add. Notice the FIRST one is empty ("") - that's on
        // purpose, so we try the plain word with nothing added before we bother
        // adding anything.
        commonSuffixes = new String[] {
                "", "1", "12", "123", "1234", "!", "123!",
                "99", "2023", "2024", "2025", "007", "69", "00"
        };

        // The letter swaps. Each pair is {letter, what to swap it for}. So people
        // write "a" as "@" or "4", "e" as "3", "o" as "0", and so on.
        substitutions = new String[][] {
                {"a", "@"}, {"a", "4"},
                {"e", "3"},
                {"o", "0"},
                {"i", "1"}, {"i", "!"},
                {"s", "$"}, {"s", "5"},
                {"t", "7"}
        };
    }

    // The main routine. For every word in the dictionary, it tries every capital-
    // letter version, and for each of those every leetspeak version, and for each
    // of THOSE every common ending - checking each combination as a guess. It goes
    // from most-likely tweaks to least-likely, so easy passwords get caught first.
    public boolean crack(String target) {
        init();
        this.found = false;

        // reset the shared counters + stopwatch
        Data.startRun(target);

        // "outer:" is just a label so that when we find the password deep inside all
        // these nested loops, we can break out of ALL of them at once.
        outer:
        for (String word : dictionary) {                         // pick a base word
            for (String caseForm : caseVariants(word)) {         // try its capital versions
                for (String leet : leetVariants(caseForm)) {     // try its leetspeak versions
                    for (String suffix : commonSuffixes) {       // try each ending
                        currentGuess = leet + suffix;            // glue word + ending together
                        if (Data.checkGuess(currentGuess)) {     // is that the password?
                            found = true;
                            break outer;                         // got it - stop everything
                        }
                    }
                }
            }
        }

        // record the guess count and report back
        this.guesses = Data.totalGuesses;
        return found;
    }

    // Gives back three versions of a word: the plain one, the Capitalised one, and
    // the ALL-CAPS one - in that order because plain/Capitalised are more common.
    private String[] caseVariants(String word) {
        // build the Capitalised version: uppercase the first letter, keep the rest.
        // (the isEmpty check just avoids crashing on an empty string)
        String capital = word.isEmpty()
                ? word
                : Character.toUpperCase(word.charAt(0)) + word.substring(1);
        return new String[] { word, capital, word.toUpperCase() };
    }

    // Gives back EVERY possible leetspeak version of a word. E.g. for "cat" with the
    // rules above you'd get "cat", "c@t", "c4t", "ca7", "c@7", "c47" and so on -
    // every mix of swapping or not-swapping each letter. It hands the work off to
    // the recursive helper below.
    private List<String> leetVariants(String s) {
        List<String> results = new ArrayList<>();
        buildLeet(s, 0, new StringBuilder(), results);
        return results;
    }

    // This builds the leetspeak versions letter by letter, and it calls itself -
    // that's called recursion. Here's the plain-English idea: go through the word
    // one letter at a time. At each letter you have a choice - keep it as-is, OR
    // swap it for a lookalike. It explores BOTH choices for every letter, which
    // ends up producing every possible combination.
    //
    //   s   = the word we're transforming
    //   i   = which letter we're currently on
    //   cur = the version we've built so far
    //   out = the list where finished versions get dropped
    private void buildLeet(String s, int i, StringBuilder cur, List<String> out) {
        // if we've placed every letter, this version is finished - save it
        if (i == s.length()) {
            out.add(cur.toString());
            return;
        }
        char c = s.charAt(i);   // the current letter

        // Choice 1: keep the letter as it is. Add it, carry on to the next letter,
        // then remove it again so we can try the other choice cleanly.
        cur.append(c);
        buildLeet(s, i + 1, cur, out);
        cur.deleteCharAt(cur.length() - 1);

        // Choice 2 (and more): for each swap rule that matches this letter, add the
        // swapped symbol instead, carry on, then remove it and try the next rule.
        for (String[] sub : substitutions) {
            if (sub[0].length() == 1 && sub[0].charAt(0) == c) {
                cur.append(sub[1]);
                buildLeet(s, i + 1, cur, out);
                cur.deleteCharAt(cur.length() - 1);
            }
        }
    }

    // getters so Main can read the results afterwards
    public long getGuesses() {
        return guesses;
    }

    public boolean isFound() {
        return found;
    }
}
