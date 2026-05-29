import java.util.Random;

public class PasswordGenerator {
    /*initialise variables*/
    private final Random random = new Random();
    private int passwordLength;
    private char[] charSet;
    private String generatedPassword;

    /*constructor*/
    public PasswordGenerator(int length, char[] charSet) {
        this.passwordLength = length;
        this.charSet = charSet;
    }
}
