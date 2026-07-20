import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// This class is basically the shared "notebook" for the whole experiment.
// Both crackers dump their results in here, and Main reads them back out.
// I made everything static so I don't have to pass an object around everywhere -
// there's only ever one experiment running, so one shared copy is fine.
public class Data {

        // ---- The character sets ----
        // "Character set" just means "which characters is the password allowed to
        // use". The bigger this list, the harder the password is to guess, because
        // there are more possibilities per letter. In the essay this is one of the
        // two things I change on purpose to see what happens (I call its size N).
        // I keep four different sizes below so I can compare them.

        // just the 10 digits, 0 through 9. size N = 10
        public static final char[] NUMERIC = {
                        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };

        // the 26 lowercase letters a-z. size N = 26
        public static final char[] LOWERCASE = {
                        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                        'u', 'v', 'w', 'x', 'y', 'z'
        };

        // letters (both cases) plus digits. 26 + 26 + 10 = 62 characters
        public static final char[] ALPHANUMERIC = {
                        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                        'u', 'v', 'w', 'x', 'y', 'z',
                        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                        'U', 'V', 'W', 'X', 'Y', 'Z',
                        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };

        // everything you can type on a normal keyboard: letters, digits, symbols,
        // and a space. 95 characters total. This is the "hardest" set to brute force.
        public static final char[] ASCII_chars = {
                // lowercase letters (a-z)
                        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                        'u', 'v', 'w', 'x', 'y', 'z',

                // uppercase letters (A-Z)
                        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                        'U', 'V', 'W', 'X', 'Y', 'Z',

                // digits (0-9)
                        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',

                // the punctuation / symbol keys
                        '!', '"', '#', '$',  '%', '&', '\'', '(', ')', '*',
                        '+', ',', '-', '.',  '/', ':', ';',  '<', '=', '>',
                        '?', '@', '[', '\\', ']', '^', '_',  '`', '{', '|',
                        '}', '~',

                // and finally the space bar
                        ' '
        };

        // ---- The stuff we actually measure ----
        // These are the results. I keep them here (not inside each cracker) so that
        // both crackers get measured by the EXACT same code - otherwise it wouldn't
        // be a fair comparison.

        // how many guesses the cracker made this run. this is measurement #1.
        public static long totalGuesses;

        // the clock readings for timing. nanoTime() is just a very precise stopwatch
        // that counts nanoseconds. startTime = when we started, endTime = when we
        // found the password.
        public static long startTime;
        public static long endTime;

        // the password we're currently trying to crack
        public static String targetPassword;
        // did we actually find it? starts as false, flips to true if we do.
        public static boolean passwordFound = false;

        // ---- The shared "did I get it right?" machinery ----
        // Both crackers call these methods, so the counting and the timing happen
        // the same way for both. That's the whole point of putting them here.

        // Call this right before a cracker starts. It wipes the previous run's
        // numbers and starts the stopwatch fresh.
        public static void startRun(String target) {
                targetPassword = target;   // remember what we're hunting for
                totalGuesses = 0;          // reset the guess counter to zero
                passwordFound = false;     // haven't found it yet
                endTime = 0;               // no finish time yet
                startTime = System.nanoTime();  // hit "start" on the stopwatch
        }

        // A cracker calls this every single time it makes a guess. It bumps the
        // counter up by one and then checks: is this guess the right password?
        // Returns true if yes (so the cracker knows to stop), false if no.
        public static boolean checkGuess(String guess) {
                totalGuesses++;   // that's one more guess made
                if (guess.equals(targetPassword)) {   // did we nail it?
                        passwordFound = true;
                        endTime = System.nanoTime();  // stop the stopwatch
                        return true;
                }
                return false;   // nope, keep going
        }

        // Works out how long the run took, in seconds. This is measurement #2.
        // If we found the password we use endTime; if we never did (gave up), we
        // just use "right now" as the end. Then divide by a billion to turn
        // nanoseconds into seconds.
        public static double elapsedSeconds() {
                long end = (endTime != 0) ? endTime : System.nanoTime();
                return (end - startTime) / 1_000_000_000.0;
        }

        // Prints one tidy line to the screen so I can watch results roll in while
        // the program runs. The weird %-14s stuff is just formatting to line the
        // columns up neatly.
        public static void printResult(String crackerName) {
                System.out.printf(
                        "%-14s | target=%-12s | found=%-5s | guesses=%-16d | time=%.6f s%n",
                        crackerName, targetPassword, passwordFound,
                        totalGuesses, elapsedSeconds());
        }

        // ---- Saving results to a file ----
        // Everything below builds up a big text table (CSV = comma separated values)
        // that I can open in Excel/Google Sheets later to make the graphs.

        // this is where all the result rows pile up, one long string
        private static final StringBuilder csvRaw = new StringBuilder();

        // Wipe the table clean and write the header row (the column titles).
        public static void csvInit() {
                csvRaw.setLength(0);
                csvRaw.append("series,cracker,charset,N,L,target_type,target,guesses,found,mean_time_s\n");
        }

        // Add one row to the table using whatever numbers are currently sitting in
        // Data. Each row is just the values glued together with commas.
        public static void csvRecord(String series, String cracker, String charsetName,
                                     int n, int l, String targetType) {
                csvRaw.append(series).append(',')
                      .append(cracker).append(',')
                      .append(charsetName).append(',')
                      .append(n).append(',')
                      .append(l).append(',')
                      .append(targetType).append(',')
                      .append(csvField(targetPassword)).append(',')
                      .append(totalGuesses).append(',')
                      .append(passwordFound).append(',')
                      .append(String.format("%.6f", elapsedSeconds()))
                      .append('\n');
        }

        // hand back the whole table as one big string
        public static String rawCsv() {
                return csvRaw.toString();
        }

        // Wraps a value in quotes and doubles up any quotes inside it. This stops a
        // password that contains a comma or a quote mark from accidentally breaking
        // the columns of the spreadsheet.
        private static String csvField(String value) {
                return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        // Saves a chunk of text to a file on disk. If the folder it's supposed to go
        // in doesn't exist yet, it makes the folder first. If something goes wrong
        // (like no permission to write) it just prints a warning instead of crashing.
        public static void writeFile(Path path, String content) {
                try {
                        if (path.getParent() != null) {
                                Files.createDirectories(path.getParent());
                        }
                        Files.writeString(path, content);
                } catch (IOException e) {
                        System.err.println("Could not write " + path + ": " + e.getMessage());
                }
        }

}
