# Benchmarking Huffman, Arithmetic and rANS entropy encoding mechanisms

## Project Overview

A Java implementation of multiple data compression algorithms for a college project. Pipelines combine transforms (BWT, LZ77, LZSS) with entropy coders (Huffman, Arithmetic, rANS), with full encode/decode round-trip verification.

**Java 17** — no build tool or external dependencies.

## Compilation & Execution

```bash
javac -d bin *.java              # compile all sources
java -cp bin Test                # correctness + load tests (all pipelines with Huffman)
java -cp bin TestRANS            # rANS unit tests + LZ77+rANS pipeline round-trip
java -cp bin RunBench            # unified benchmark: all 5 schemes on all Data/ files
java -cp bin RunBench --files f1.txt,f2.txt  # all schemes on specific files
java -cp bin RunBench --schemes LZ77+rANS,LZ77+Arith  # specific schemes, all files
java -cp bin RunBench --schemes LZ77+Arith --files bible.txt  # one scheme, one file
```

`Benchmark.java` (4 schemes) and `BenchmarkArithmetic.java` (8 schemes) still exist but `RunBench` is the primary entry point.

`Master` has no `main` method — its pipelines (`lz77()`, `lzss()`, `bwt()`) are invoked from `Test.loadTest()`.

## Directory Structure

- All `.java` source files are in the repository root.
- `Data/` (gitignored) — input text/genome files for benchmarking.
- `Data/OutputJava/` (gitignored) — compressed output files.
- `bin/` (gitignored) — compiled `.class` files.

## High-Level Architecture

### Compression Pipelines

The project runs data through a transform, then entropy-codes the result. Each pipeline is reversible — decompress and verify round-trip equality.

| Pipeline | Flow | Entropy coder | Output streams |
|---|---|---|---|
| **LZ77** | Data → LZ77 triplets (delta, length, next) | Huffman (Arithmetic/rANS also supported, not benchmarked) | 3 encoded streams |
| **LZSS** | Data → LZSS (delta, length, next + identifier bits) | Huffman per stream | identifier (raw bits) + 3 encoded streams |
| **BWT** | Data → BWT → MTF | Huffman only (Arithmetic/rANS also supported, not benchmarked) | MTF index stream + alphabet |

LZ77 uses WINDOW=1024, LOOKAHEAD=128. LZSS adds LENGTH_THRESHOLD=4 (matches ≤4 chars are encoded as literals rather than back-references).

### Entropy Coders

Each entropy coder follows the same pattern: encode produces an Element object, decode consumes it. IO helpers write/read to disk.

| Coder | Input/Output | Disk format | Notes |
|---|---|---|---|
| **Huffman** | `ArrayList<Integer>` → `HuffmanElement` | `.huffman.encoding` (packed bytes) + `.huffman.map` (text mapping) | PriorityQueue-based tree, variable-length codes |
| **Arithmetic** | `int[]` → `ArithmeticElement` | `.ar` (single binary blob) | Witten-Neal-Cleary with 16-bit precision, TF=4096. O(n) **BUT** buffers entire output in memory — see Known Issues below |
| **rANS** | `int[]` → `rANSElement` | `.rans` (single binary blob) | 64-bit state, byte-level renormalization. O(n) streaming — see Known Issues below |

### Key Design Notes

- **Arithmetic and rANS accept `int[]` input**, while Huffman accepts `ArrayList<Integer>`. The helper `toInt(ArrayList<Integer>)` bridges between them.
- **LZSS identifiers** (bit string of literal vs back-reference flags) are stored raw via `BitPacker`, not entropy-coded.
- **BWT pipeline stores the alphabet** separately (`.alpha` file) because MTF decoding requires it. This is true for all BWT entropy coder variants.
- **Huffman writes two files** per stream (packed data + mapping table), while Arithmetic and rANS each write a single blob. This affects compressed size calculations.

### Element Classes

| Class | Streams | Used by |
|---|---|---|
| `HuffmanElement` | encoding string + mapping table | All Huffman encoding |
| `ArithmeticElement` | encoded bytes + cumFreqs | Arithmetic coding |
| `rANSElement` | encoded bytes + alphabet + cumFreqs + originalLength | rANS coding |
| `LZ77Element` | delta (int) + length (int) + next (char) | LZ77 |
| `LZSSElement` | identifier (String) + delta + length + next | LZSS |
| `MTFElement` | alphabet (char) + mtf (int) | MTF |

### Supporting Utilities

| File | Role |
|---|---|
| `HelperFunctions.java` | Type conversions (char↔int), alphabet extraction, frequency counting, equality verification |
| `IOHelper.java` | File read/write, byte I/O, alphabet/mapping serialization, file size queries |
| `BitPacker.java` | Packs/unpacks bit strings to/from byte arrays |
| `FilePaths.java` | Path constants, data file list, extension definitions |
| `SuffixArray.java` | Two implementations: brute-force O(n² log n) and Manber-Myers O(n log² n). Controlled by `USE_MANBER_MYERS` flag (default: false) |

### Entry Points

| Class | What it does |
|---|---|
| `Test` | Unit correctness tests for individual algorithms + full pipeline load test via `Master` |
| `TestRANS` | Standalone rANS correctness tests + LZ77+rANS round-trip on a data file |
| `Master` | Pipeline runner — compresses then decompresses all `Data/` files via LZ77, LZSS, BWT (with Huffman). No standalone `main()` |
| `RunBench` | **Primary benchmark** — all 8 schemes on `Data/` files. Supports `--schemes` and `--files` flags for selective runs. Supersedes `Benchmark` and `BenchmarkArithmetic`. |
| `Benchmark` | Legacy 4-scheme runner. Still functional but use `RunBench` for new work. |
| `BenchmarkArithmetic` | Legacy 8-scheme runner. Still functional but use `RunBench` for new work. |

### Dataset

`FilePaths.DATA` lists benchmark files: `tiny.txt`, `medium.txt`, `aliceinwonderland.txt`, several `.fna` genome files, `bible.txt`. These must exist in `Data/` for load tests and benchmarks to run.

## Known Issues

### Arithmetic OOM on large files

`Arithmetic.encode()` buffers the entire compressed output in a byte array before returning. For bible.txt (4MB), the MTF stream produces ~2MB of output bytes. The `BitBuf` grows via `Arrays.copyOf` which allocates a full copy each time. With 25% growth + 1MB chunks, reaching ~2MB requires multiple full-array copies, and the peak memory (old + new buffer simultaneously) exceeds the heap limit.

**Root cause**: The in-memory `ArithmeticElement` must hold all encoded bytes. The fix is to stream encoded bytes directly to a file (via `DataOutputStream`) instead of buffering, then read them back with a custom file reader that doesn't load the entire stream into memory at once.

### rANS decode failure

`TestRANS` fails on first test case with `sIdx=-21332` in `rANS.decode()`. The negative index indicates the internal state variable is corrupted — likely from byte-ordering issues in how the final state is serialized/deserialized, or from the renormalization byte sequence being read in wrong order. The rANS encode/ decode pair works correctly in isolation when tested via `java -cp bin Test` for other transforms but breaks in the standalone TestRANS tests.

**Status**: Needs investigation. The rANS.java file was rewritten from a BigInteger-based O(n²) version to a 64-bit streaming version. The transition may have introduced a subtle bug in the decode path.

### Testable Files
- Only works on `tiny.txt`, and `medium.txt` so far, other implementations are buggy.
