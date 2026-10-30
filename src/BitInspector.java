import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Locale;

/** Shows both decimal meanings of a fixed-width bit pattern. */
public final class BitInspector {
    private static final BigInteger ONE = BigInteger.ONE;
    private static final String USAGE =
            "Usage: java -cp build BitInspector --width <8|16|32|64> <integer>";

    private BitInspector() {}

    public record Inspection(int width, BigInteger unsignedValue,
                             BigInteger signedValue, String binary, String hex) {
        public String format() {
            return "Width: " + width + " bits\n"
                    + "Binary: " + binary + "\n"
                    + "Hex: " + hex + "\n"
                    + "Signed: " + signedValue + "\n"
                    + "Unsigned: " + unsignedValue;
        }
    }

    public static Inspection inspect(int width, String literal) {
        if (width != 8 && width != 16 && width != 32 && width != 64) {
            throw new IllegalArgumentException("width must be 8, 16, 32, or 64");
        }
        if (literal == null || literal.isEmpty() || literal.length() > 128) {
            throw new IllegalArgumentException("integer must contain 1 to 128 characters");
        }
        BigInteger value;
        if (literal.matches("0[xX][0-9a-fA-F]+")) {
            value = new BigInteger(literal.substring(2), 16);
        } else if (literal.matches("0[bB][01]+")) {
            value = new BigInteger(literal.substring(2), 2);
        } else if (literal.matches("-?[0-9]+")) {
            value = new BigInteger(literal, 10);
        } else {
            throw new IllegalArgumentException(
                    "use decimal, nonnegative 0x hexadecimal, or nonnegative 0b binary");
        }
        BigInteger modulus = ONE.shiftLeft(width);
        BigInteger signBit = ONE.shiftLeft(width - 1);
        if (value.compareTo(signBit.negate()) < 0 || value.compareTo(modulus) >= 0) {
            throw new IllegalArgumentException("integer does not fit the selected width");
        }
        BigInteger unsigned = value.signum() < 0 ? value.add(modulus) : value;
        BigInteger signed = unsigned.testBit(width - 1)
                ? unsigned.subtract(modulus) : unsigned;
        String bits = pad(unsigned.toString(2), width);
        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < bits.length(); i++) {
            if (i > 0 && i % 4 == 0) grouped.append(' ');
            grouped.append(bits.charAt(i));
        }
        String hex = "0x" + pad(unsigned.toString(16).toUpperCase(Locale.ROOT), width / 4);
        return new Inspection(width, unsigned, signed, grouped.toString(), hex);
    }

    private static String pad(String value, int length) {
        return "0".repeat(length - value.length()) + value;
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 1 && args[0].equals("--help")) {
            out.println(USAGE);
            out.println("Decimal may be negative; 0x and 0b literals must be nonnegative.");
            out.println("Negative values use two's complement. Positive values may fill all bits.");
            out.println("Example: java -cp build BitInspector --width 8 -13");
            return 0;
        }
        try {
            if (args.length != 3 || !args[0].equals("--width")) {
                throw new IllegalArgumentException("provide a width and one integer");
            }
            int width = switch (args[1]) {
                case "8" -> 8;
                case "16" -> 16;
                case "32" -> 32;
                case "64" -> 64;
                default -> throw new IllegalArgumentException(
                        "width must be 8, 16, 32, or 64");
            };
            out.println(inspect(width, args[2]).format());
            return 0;
        } catch (IllegalArgumentException ex) {
            err.println("Error: " + ex.getMessage());
            err.println(USAGE);
            return 2;
        }
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }
}
