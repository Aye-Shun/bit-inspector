import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BitInspectorTest {
    private static int passed;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void equal(Object expected, Object actual) {
        check(expected.equals(actual), "expected " + expected + ", got " + actual);
    }

    private static void test(String name, Runnable body) {
        body.run();
        passed++;
        System.out.println("PASS " + name);
    }

    private static void invalid(int width, String value) {
        try {
            BitInspector.inspect(width, value);
            throw new AssertionError("accepted invalid input: " + value);
        } catch (IllegalArgumentException expected) {
            check(!expected.getMessage().isBlank(), "missing explanation");
        }
    }

    private record Run(int code, String out, String err) {}

    private static Run run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = BitInspector.run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Run(code, out.toString(StandardCharsets.UTF_8),
                err.toString(StandardCharsets.UTF_8));
    }

    private static Run process(String... args) {
        try {
            List<String> command = new ArrayList<>();
            command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add("BitInspector");
            command.addAll(List.of(args));
            Process child = new ProcessBuilder(command).start();
            String out = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String err = new String(child.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            return new Run(child.waitFor(), out, err);
        } catch (Exception ex) {
            throw new AssertionError("CLI process failed", ex);
        }
    }

    public static void main(String[] args) {
        test("negative eight-bit example", () -> {
            var i = BitInspector.inspect(8, "-13");
            equal(BigInteger.valueOf(243), i.unsignedValue());
            equal(BigInteger.valueOf(-13), i.signedValue());
            equal("1111 0011", i.binary());
            equal("0xF3", i.hex());
        });
        test("zero padding", () -> {
            var i = BitInspector.inspect(16, "1");
            equal("0000 0000 0000 0001", i.binary());
            equal("0x0001", i.hex());
        });
        test("ordinary positive decimal", () -> {
            var i = BitInspector.inspect(8, "42");
            equal(BigInteger.valueOf(42), i.signedValue());
            equal("0010 1010", i.binary());
            equal("0x2A", i.hex());
        });
        test("equivalent radices", () -> {
            equal(BitInspector.inspect(8, "243"), BitInspector.inspect(8, "0xf3"));
            equal(BitInspector.inspect(8, "243"), BitInspector.inspect(8, "0B11110011"));
            equal(BitInspector.inspect(16, "65535"), BitInspector.inspect(16, "0XFFFF"));
        });
        test("all width boundaries", () -> {
            for (int width : new int[]{8, 16, 32, 64}) {
                BigInteger modulus = BigInteger.ONE.shiftLeft(width);
                BigInteger sign = BigInteger.ONE.shiftLeft(width - 1);
                equal(BigInteger.ZERO, BitInspector.inspect(width, "0").signedValue());
                equal(sign.negate(), BitInspector.inspect(width, sign.negate().toString()).signedValue());
                equal(sign.subtract(BigInteger.ONE), BitInspector.inspect(width, sign.subtract(BigInteger.ONE).toString()).signedValue());
                equal(sign.negate(), BitInspector.inspect(width, sign.toString()).signedValue());
                equal(BigInteger.valueOf(-1), BitInspector.inspect(width, modulus.subtract(BigInteger.ONE).toString()).signedValue());
                invalid(width, sign.negate().subtract(BigInteger.ONE).toString());
                invalid(width, modulus.toString());
            }
        });
        test("unsigned 64-bit maximum", () -> {
            var i = BitInspector.inspect(64, "18446744073709551615");
            equal("0xFFFFFFFFFFFFFFFF", i.hex());
            equal(BigInteger.valueOf(-1), i.signedValue());
            equal("18446744073709551615", i.unsignedValue().toString());
        });
        test("signed 64-bit minimum", () -> {
            var i = BitInspector.inspect(64, "-9223372036854775808");
            equal("0x8000000000000000", i.hex());
            equal("9223372036854775808", i.unsignedValue().toString());
        });
        test("complete eight-bit representation sweep", () -> {
            for (int n = 0; n < 256; n++) {
                var i = BitInspector.inspect(8, Integer.toString(n));
                equal(BigInteger.valueOf(n), i.unsignedValue());
                equal(BigInteger.valueOf(n < 128 ? n : n - 256), i.signedValue());
                equal(BigInteger.valueOf(n), new BigInteger(i.binary().replace(" ", ""), 2));
                equal(BigInteger.valueOf(n), new BigInteger(i.hex().substring(2), 16));
            }
        });
        test("negative zero and leading zeroes", () -> {
            equal(BitInspector.inspect(8, "0"), BitInspector.inspect(8, "-0"));
            equal(BitInspector.inspect(8, "42"), BitInspector.inspect(8, "00042"));
        });
        test("malformed numbers rejected", () -> {
            for (String value : new String[]{"", "+1", "-", "1.5", "1e2", " 42", "42 ",
                    "0x", "0xGG", "0b", "0b102", "-0x1", "-0b1", "1_000", "１２", "1\n"}) {
                invalid(8, value);
            }
            invalid(8, null);
            invalid(8, "0".repeat(129));
        });
        test("unsupported widths rejected", () -> {
            for (int width : new int[]{-1, 0, 1, 7, 9, 24, 128}) invalid(width, "0");
        });
        test("too-wide radix values rejected", () -> {
            invalid(8, "0x100");
            invalid(8, "0b100000000");
            invalid(64, "0x10000000000000000");
        });
        test("help uses stdout", () -> {
            Run r = run("--help");
            equal(0, r.code()); equal("", r.err());
            check(r.out().contains("--width <8|16|32|64>"), "missing usage");
        });
        test("CLI exact output", () -> {
            Run r = run("--width", "8", "-13");
            equal(0, r.code()); equal("", r.err());
            equal("Width: 8 bits\nBinary: 1111 0011\nHex: 0xF3\nSigned: -13\nUnsigned: 243\n", r.out());
        });
        test("bad flags and missing arguments", () -> {
            for (String[] a : new String[][]{{}, {"--width"}, {"--width", "8"},
                    {"--width", "8", "1", "2"}, {"--bits", "8", "1"},
                    {"--width", "08", "1"}, {"--width", "abc", "1"}, {"--help", "1"}}) {
                Run r = run(a);
                equal(2, r.code()); equal("", r.out());
                check(r.err().startsWith("Error: "), "missing error");
                check(r.err().contains("Usage:"), "missing usage");
            }
        });
        test("actual process success", () -> {
            Run r = process("--width", "16", "65535");
            equal(0, r.code()); equal("", r.err());
            check(r.out().contains("Signed: -1\nUnsigned: 65535"), "wrong process output");
        });
        test("actual process range error", () -> {
            Run r = process("--width", "8", "256");
            equal(2, r.code()); equal("", r.out());
            check(r.err().contains("does not fit"), "missing range error");
        });
        test("actual process help and usage", () -> {
            Run help = process("--help");
            equal(0, help.code()); equal("", help.err());
            Run missing = process();
            equal(2, missing.code()); equal("", missing.out());
        });
        System.out.println(passed + " tests passed.");
    }
}
