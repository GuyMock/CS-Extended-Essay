import java.nio.file.Path;
import java.nio.file.Paths;

// This is the "boss" class - the one that actually runs. It sets up the two
// crackers and the password maker, then runs the whole set of experiments, prints
// what's happening as it goes, and saves the results to files at the end. Nothing
// clever happens in here itself; it just gives orders to the other classes and
// collects their numbers.
public class Main {

    // the dumb cracker (tries every combination)
    BruteForceCracker bruteForceCracker;
    // the smart cracker (guesses common words + tweaks)
    HeuristicCracker heuristicCracker;
    // the thing that makes the passwords we're trying to crack
    PasswordGenerator passwordGenerator;

    // How many times we re-run the same crack just for timing. Computers are noisy -
    // the exact same job takes a slightly different amount of time each go. So I run
    // it 10 times and average the times to smooth that out. The number of GUESSES
    // never changes for the same target, so there's no point averaging that.
    private static final int REPEATS = 10;

    // For each experiment setup, I don't just test one random password - I test 5
    // different random ones and average their results. One single random password
    // might get lucky or unlucky, so averaging a few gives a fairer picture.
    private static final int TARGETS_PER_CONDITION = 5;

    // A cap on how many guesses the brute-force cracker is allowed against the really
    // hard passwords. Trying every keyboard combination for an 8-letter password would
    // take longer than the age of the universe, so I stop it at 2 million guesses and
    // just honestly report "didn't find it in time".
    private static final long ASCII_BUDGET = 2_000_000L;

    // the folder where the result files get saved
    private static final Path RESULTS_DIR = Paths.get("results");

    // a running text table that collects the neat summary numbers as we go
    private final StringBuilder summary = new StringBuilder();

    // The starting point of the whole program. Java runs this first. It builds the
    // objects it needs and then kicks off the experiments.
    public static void main(String[] args) {
        Main app = new Main();
        app.bruteForceCracker = new BruteForceCracker();
        app.heuristicCracker = new HeuristicCracker();
        // start the generator making 4-letter lowercase passwords (this gets changed
        // as we go, this is just the initial setting)
        app.passwordGenerator = new PasswordGenerator(4, Data.LOWERCASE);
        app.runExperiments();
    }

    // Runs all four experiments in order, then saves everything. This is the recipe
    // for the entire study.
    private void runExperiments() {
        Data.csvInit();   // set up a fresh empty results table
        // write the header row (column titles) for the summary table
        summary.append("series,charset,N,L,target_type,")
               .append("bf_mean_guesses,bf_mean_time_s,bf_found_rate,")
               .append("heur_mean_guesses,heur_mean_time_s,heur_found_rate\n");

        seriesA();                      // experiment 1: change password length
        seriesB();                      // experiment 2: change character-set size
        shortPredictableTargets();      // experiment 3: short real words
        realisticPredictableTargets();  // experiment 4: realistic weak passwords

        export();                       // save all the results to files
    }

    // ---- Experiment 1 (Series A): keep the character set the same, make the
    // password longer and longer, and watch the brute-force effort explode. ----
    private void seriesA() {
        char[] charset = Data.LOWERCASE;   // always lowercase a-z here (26 characters)
        banner("Series A: N held constant (lowercase, N=26), L varied 1..5");
        // try password lengths 1, 2, 3, 4, 5
        for (int length = 1; length <= 5; length++) {
            System.out.printf("-- L=%d, N=%d --%n", length, charset.length);
            sweepRandomCondition("A", charset, length);
        }
        System.out.println();
    }

    // ---- Experiment 2 (Series B): keep the password length the same (3), but use
    // bigger and bigger character sets, and watch brute-force effort explode again. ----
    private void seriesB() {
        int length = 3;   // always 3 characters long here
        // four character sets, getting bigger each time: 10, then 26, then 62, then 95
        char[][] charsets = { Data.NUMERIC, Data.LOWERCASE, Data.ALPHANUMERIC, Data.ASCII_chars };
        banner("Series B: L held constant (L=3), N varied 10 -> 26 -> 62 -> 95");
        for (char[] charset : charsets) {
            System.out.printf("-- L=%d, N=%d --%n", length, charset.length);
            sweepRandomCondition("B", charset, length);
        }
        System.out.println();
    }

    // Runs a single experiment setup: makes several random passwords, throws both
    // crackers at each one, and keeps a running total so it can report the averages.
    // "series" is just a label ("A" or "B") so I can tell the rows apart in the file.
    private void sweepRandomCondition(String series, char[] charset, int length) {
        // point the generator at this experiment's settings
        passwordGenerator.setCharSet(charset);
        passwordGenerator.setLength(length);
        String name = charsetName(charset);   // a friendly name like "lowercase"

        // running totals so we can work out averages at the end
        long bfGuessSum = 0, heurGuessSum = 0;   // guess totals
        double bfTimeSum = 0, heurTimeSum = 0;   // time totals
        int bfFound = 0, heurFound = 0;          // how many each cracker actually solved

        // test several different random passwords for this setup
        for (int t = 0; t < TARGETS_PER_CONDITION; t++) {
            String target = passwordGenerator.generateRandom();   // make a random password
            System.out.printf("   target[%d]=%s%n", t + 1, target);

            // --- brute force has a go ---
            timeBruteForce(target, charset, length, Long.MAX_VALUE);
            bfGuessSum += Data.totalGuesses;         // add its guesses to the total
            bfTimeSum += Data.elapsedSeconds();      // add its time to the total
            if (Data.passwordFound) bfFound++;       // count it if it succeeded
            Data.printResult("   BruteForce");       // print the row to the screen
            Data.csvRecord(series, "BruteForce", name, charset.length, length, "random");  // save the row

            // --- now the heuristic cracker has a go at the SAME password ---
            timeHeuristic(target);
            heurGuessSum += Data.totalGuesses;
            heurTimeSum += Data.elapsedSeconds();
            if (Data.passwordFound) heurFound++;
            Data.printResult("   Heuristic");
            Data.csvRecord(series, "Heuristic", name, charset.length, length, "random");
        }

        // work out and print/save the averages for this setup
        int n = TARGETS_PER_CONDITION;
        System.out.printf("   >> mean brute-force guesses over %d random targets: %d%n",
                n, bfGuessSum / n);
        summaryRow(series, name, charset.length, length, "random",
                bfGuessSum / n, bfTimeSum / n, (double) bfFound / n,
                heurGuessSum / n, heurTimeSum / n, (double) heurFound / n);
    }

    // ---- Experiment 3: short, real, lowercase words (like "cat", "hello"). These
    // are weak AND short enough that even the dumb brute force finishes, so I can
    // compare both crackers on actual finish-line numbers. ----
    private void shortPredictableTargets() {
        banner("Short predictable targets (lowercase words): both crackers finish");
        char[] charset = Data.LOWERCASE;
        String name = charsetName(charset);
        // go through every short word in the generator's list
        for (int i = 0; i < passwordGenerator.shortPredictableCount(); i++) {
            String target = passwordGenerator.generateShortPredictable(i);
            int l = target.length();   // this word's length
            System.out.printf("-- target=%s (len=%d) --%n", target, l);

            // brute force it, print + save, and stash its numbers to compare later
            timeBruteForce(target, charset, l, Long.MAX_VALUE);
            Data.printResult("BruteForce");
            Data.csvRecord("short_predictable", "BruteForce", name, charset.length, l, "predictable");
            long bfGuesses = Data.totalGuesses;
            double bfTime = Data.elapsedSeconds();
            int bfFound = Data.passwordFound ? 1 : 0;

            // now the heuristic cracker, then write one summary row comparing the two
            timeHeuristic(target);
            Data.printResult("Heuristic");
            Data.csvRecord("short_predictable", "Heuristic", name, charset.length, l, "predictable");
            summaryRow("short_predictable", name, charset.length, l, target,
                    bfGuesses, bfTime, bfFound,
                    Data.totalGuesses, Data.elapsedSeconds(), Data.passwordFound ? 1 : 0);
        }
        System.out.println();
    }

    // ---- Experiment 4: realistic weak passwords (like "p@ssw0rd"). This is the big
    // reveal - the smart cracker cracks them fast, but brute force can't finish and
    // gives up at its budget. It shows a password can be "weak" to a human yet still
    // beat a brute-force attack, because brute force doesn't know the tricks. ----
    private void realisticPredictableTargets() {
        banner("Realistic predictable targets: heuristic vs budgeted ASCII brute force");
        char[] charset = Data.ASCII_chars;   // the full 95-character keyboard set
        String name = charsetName(charset);
        for (int i = 0; i < passwordGenerator.predictableCount(); i++) {
            String target = passwordGenerator.generatePredictable(i);
            int l = target.length();
            System.out.printf("-- target=%s (len=%d) --%n", target, l);

            // let the smart cracker go first and remember how it did
            timeHeuristic(target);
            Data.printResult("Heuristic");
            Data.csvRecord("realistic_predictable", "Heuristic", name, charset.length, l, "predictable");
            long heurGuesses = Data.totalGuesses;
            double heurTime = Data.elapsedSeconds();
            int heurFound = Data.passwordFound ? 1 : 0;

            // Brute-forcing the full keyboard set here is basically impossible, so I
            // run it just once (no timing repeats - it always gives up at the same
            // spot) with the 2-million guess budget. The result is an honest "nope,
            // didn't find it in 2 million guesses".
            bruteForceCracker.crack(target, charset, l, ASCII_BUDGET);
            Data.printResult("BruteForce");
            Data.csvRecord("realistic_predictable", "BruteForce", name, charset.length, l, "predictable");
            summaryRow("realistic_predictable", name, charset.length, l, target,
                    Data.totalGuesses, Data.elapsedSeconds(), Data.passwordFound ? 1 : 0,
                    heurGuesses, heurTime, heurFound);
        }
        System.out.println();
    }

    // ---- Saving everything to files ----
    // Dumps the raw table, the summary table, and a written notes file into the
    // results folder, then prints where they went.
    private void export() {
        Data.writeFile(RESULTS_DIR.resolve("raw_results.csv"), Data.rawCsv());
        Data.writeFile(RESULTS_DIR.resolve("summary.csv"), summary.toString());
        Data.writeFile(RESULTS_DIR.resolve("analysis.md"), analysisText());

        System.out.println("=== Results exported ===");
        System.out.println("Folder: " + RESULTS_DIR.toAbsolutePath());
        System.out.println("  raw_results.csv  - every trial (one row per target per cracker)");
        System.out.println("  summary.csv      - per-condition means (use for the graphs)");
        System.out.println("  analysis.md      - what to discuss in the EE");
    }

    // ---- Little helper methods ----

    // Adds one line to the summary table. It just takes all the numbers and glues
    // them together with commas (that's what a CSV file is). The quote/replace bit
    // on "type" stops a stray comma in the target from splitting the columns.
    private void summaryRow(String series, String charset, int n, int l, String type,
                            long bfGuesses, double bfTime, double bfFound,
                            long heurGuesses, double heurTime, double heurFound) {
        summary.append(series).append(',')
               .append(charset).append(',')
               .append(n).append(',')
               .append(l).append(',')
               .append('"').append(type.replace("\"", "\"\"")).append('"').append(',')
               .append(bfGuesses).append(',')
               .append(String.format("%.6f", bfTime)).append(',')
               .append(String.format("%.2f", bfFound)).append(',')
               .append(heurGuesses).append(',')
               .append(String.format("%.6f", heurTime)).append(',')
               .append(String.format("%.2f", heurFound))
               .append('\n');
    }

    // Turns a character set into a readable name based on how many characters it has.
    // Just makes the output files easier to read than a raw number.
    private String charsetName(char[] charset) {
        switch (charset.length) {
            case 10: return "numeric";
            case 26: return "lowercase";
            case 62: return "alphanumeric";
            case 95: return "ascii";
            default: return "charset" + charset.length;
        }
    }

    // ---- Timing helpers ----
    // Runs the brute-force crack REPEATS times and works out the average time. The
    // guess count from the last run is left sitting in Data for printing (it's the
    // same every run anyway, so it doesn't matter which run's we keep).
    private void timeBruteForce(String target, char[] charset, int maxLength, long budget) {
        double totalSeconds = 0;
        for (int r = 0; r < REPEATS; r++) {
            bruteForceCracker.crack(target, charset, maxLength, budget);
            totalSeconds += Data.elapsedSeconds();
        }
        overwriteMeanTime(totalSeconds / REPEATS);
    }

    // Same idea for the heuristic cracker: run it a bunch and average the time.
    private void timeHeuristic(String target) {
        double totalSeconds = 0;
        for (int r = 0; r < REPEATS; r++) {
            heuristicCracker.crack(target);
            totalSeconds += Data.elapsedSeconds();
        }
        overwriteMeanTime(totalSeconds / REPEATS);
    }

    // A small trick: after averaging the times, I fake Data's stopwatch readings so
    // that when something later asks "how long did it take?" it gets the AVERAGE
    // time, not just the last single run's time. Start is set to 0 and end is set to
    // the average time converted back into nanoseconds.
    private void overwriteMeanTime(double meanSeconds) {
        Data.startTime = 0;
        Data.endTime = (long) (meanSeconds * 1_000_000_000.0);
    }

    // just prints a title line with === around it, so the console output is readable
    private void banner(String title) {
        System.out.println("=== " + title + " ===");
    }

    // Builds the text for the analysis.md notes file - basically my reminders to
    // myself about what the results mean and what to write about in the essay.
    private String analysisText() {
        return String.join("\n",
            "# What to discuss in the Extended Essay",
            "",
            "");
    }
}
