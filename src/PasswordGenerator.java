import java.util.Random;

// This class just makes the passwords that we're going to try to crack. Think of
// it as the "target maker". It can make two kinds of passwords: totally random
// ones (hard), and predictable human-style ones (easy, like people actually pick).
public class PasswordGenerator {

    // Random is Java's dice-roller. I use it whenever I need to pick something at
    // random - a random letter, a random word from a list, etc.
    private final Random random = new Random();

    // how long the random passwords should be (number of characters)
    private int passwordLength;
    // which characters the random passwords are allowed to use
    private char[] charSet;
    // just remembers the last password we handed out
    private String generatedPassword;

    // A list of realistic "weak" passwords - the kind of thing a real person might
    // actually use. They mix capital letters, numbers and symbols, which means a
    // dumb brute-force attack would take basically forever to grind through them...
    // BUT the smart (heuristic) cracker can still get them, because they're all
    // just common words with predictable tweaks. That contrast is the whole point.
    private final String[] predictablePool = {
            "password1", "H3llo123", "p@ssw0rd", "Welcome1",
            "qwerty123", "admin123", "dragon2024", "monkey!",
            "letmein1", "Football99"
    };

    // Short, all-lowercase real words. These are weak too, but they're short enough
    // that even the dumb brute-force cracker will actually finish and find them.
    // I need that so I can put real "guesses to crack" numbers next to each other
    // for both crackers, instead of one of them just giving up.
    private final String[] shortPredictablePool = {
            "cat", "sun", "moon", "hello", "admin", "login"
    };

    // The constructor - this runs when Main first creates the generator. It just
    // sets the starting length and character set.
    public PasswordGenerator(int length, char[] charSet) {
        this.passwordLength = length;
        this.charSet = charSet;
    }

    // These two let Main change the settings between experiments. That's how I sweep
    // through different lengths and different character-set sizes.
    public void setLength(int length) {
        this.passwordLength = length;
    }

    public void setCharSet(char[] charSet) {
        this.charSet = charSet;
    }

    // Builds a completely random password. It just loops "length" times and each
    // time grabs a random character from the allowed set and sticks it on the end.
    // These are the "hard" targets with no pattern to exploit.
    public String generateRandom() {
        StringBuilder sb = new StringBuilder(passwordLength);
        for (int i = 0; i < passwordLength; i++) {
            // charSet[random.nextInt(charSet.length)] = pick a random slot in the
            // character list and grab whatever character is there
            sb.append(charSet[random.nextInt(charSet.length)]);
        }
        generatedPassword = sb.toString();
        return generatedPassword;
    }

    // Picks a random weak password from the realistic list above.
    public String generatePredictable() {
        generatedPassword = predictablePool[random.nextInt(predictablePool.length)];
        return generatedPassword;
    }

    // Same list, but grabs a SPECIFIC one by its position number instead of a random
    // one. I use this so I can go through every password in order, one at a time.
    // (The % just wraps the number around so it can never go off the end of the list.)
    public String generatePredictable(int index) {
        generatedPassword = predictablePool[index % predictablePool.length];
        return generatedPassword;
    }

    // how many realistic passwords are in the list
    public int predictableCount() {
        return predictablePool.length;
    }

    // Same idea as generatePredictable(index) but for the short-word list.
    public String generateShortPredictable(int index) {
        generatedPassword = shortPredictablePool[index % shortPredictablePool.length];
        return generatedPassword;
    }

    // how many short words are in that list
    public int shortPredictableCount() {
        return shortPredictablePool.length;
    }

    // just hands back whatever password we made last
    public String getGeneratedPassword() {
        return generatedPassword;
    }
}
