# Bit Inspector

A small Java command-line tool for seeing how an integer looks at a fixed bit width. It prints grouped binary, hexadecimal, and both signed and unsigned decimal interpretations of the same bits.

You need JDK 17 or newer and Make. There are no external libraries.

```sh
make compile
java -cp build BitInspector --width 8 -13
```

```text
Width: 8 bits
Binary: 1111 0011
Hex: 0xF3
Signed: -13
Unsigned: 243
```

Choose an 8-, 16-, 32-, or 64-bit width. Inputs can be decimal, nonnegative hexadecimal (`0xF3`), or nonnegative binary (`0b11110011`). Negative decimal values must fit the signed range; nonnegative values may use the full unsigned range. Values outside those ranges are rejected instead of silently wrapping. Run `--help` for syntax or `make demo` for two examples.

`BigInteger` keeps the unsigned 64-bit maximum representable. Negative values are converted with two's complement, and the top bit determines the signed interpretation. Output lists the most significant bit first; it does not describe bytes in memory.

Run `make test` for boundary, parsing, formatting, and actual CLI-process checks, including every 8-bit pattern. Invalid input exits with status 2 and writes to stderr. The tool accepts one integer per invocation, limits input to 128 characters, and handles integers only: no floating point, bit operations, or arbitrary widths.
