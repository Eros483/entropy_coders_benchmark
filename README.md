# Benchmarking Huffman, Arithmetic and rANS Entropy Coding Pipelines

## Project Overview

This project benchmarks multiple lossless compression pipelines written in plain Java. The transform layer uses `LZ77`, `LZSS`, and `BWT + MTF`, and the entropy layer uses `Huffman`, `Arithmetic`, and `rANS` where supported.

The codebase is focused on:

- round-trip correctness
- comparing compressed size across schemes
- running the same pipelines across a mixed text and genome dataset

No build tool or external dependencies are required. The project compiles with `javac`.

## Compilation And Execution

## Download Data Folder: [Google Drive](https://drive.google.com/file/d/1Atd143ZsA6HaBL1MPAJuAJvgYFYET0Jo/view)


- Compilation
````bash
javac -d bin *.java
````

- Basic Tests

````bash
java -cp bin Test
java -cp bin TestRANS
````

- Running the benchmark
````bash
java -cp bin RunBench
````

- Arguments for running the benchmark with more specifications
````bash
# Unified benchmark: LZ77, LZSS, BWT/MTF x Huffman / Arithmetic / rANS

java -cp bin RunBench                                                   # all 8 schemes, all Data/ files
java -cp bin RunBench --files tiny.txt,L.monocytogenes.fna              # all schemes, specific files
java -cp bin RunBench --schemes LZ77+rANS,LZ77+Arith                    # LZ77+rANS and LZ77+Arith, all files
java -cp bin RunBench --schemes LZ77+rANS,LZ77+Arith --files bible.txt  # two schemes on one file
````

- **Note**:
JVM may run into OOM on java heap space on `T.nigroviridis.fna`. Use the following command to circumvent it.
````bash
java -Xmx6g -cp bin RunBench --schemes LZSS+Huffman,BWT+MTF+Huffman --files T.nigroviridis.fna
````


Current entry-point roles:

- `Test` runs correctness checks for the core algorithms and then runs the Huffman-based `Master` pipelines over the default dataset.
- `TestRANS` runs standalone rANS unit tests plus an `LZ77 + rANS` pipeline test.
- `RunBench` is the main benchmark driver.
- `Benchmark` and `BenchmarkArithmetic` still compile, but they are legacy runners now.

## Active Benchmark Matrix

`RunBench` currently benchmarks these 5 schemes:

| Scheme | Transform path | Entropy coder |
|---|---|---|
| `LZ77+Huffman` | Data -> LZ77 triplets | Huffman |
| `LZ77+Arith` | Data -> LZ77 triplets | Arithmetic |
| `LZ77+rANS` | Data -> LZ77 triplets | rANS |
| `LZSS+Huffman` | Data -> LZSS streams | Huffman |
| `BWT+MTF+Huffman` | Data -> BWT -> MTF | Huffman |

Although the codebase still contains arithmetic and rANS support outside this exact matrix, `RunBench` itself currently activates the 5 schemes above.

## High-Level Architecture

### Pipelines

| Pipeline | Output representation | Notes |
|---|---|---|
| `LZ77` | `delta`, `length`, `next` streams | `WINDOW = 1024`, `LOOKAHEAD = 128` |
| `LZSS` | identifier bits + `delta`, `length`, `next` streams | `LENGTH_THRESHOLD = 4` |
| `BWT + MTF` | MTF index stream + stored alphabet | alphabet is saved separately for decode |

### Entropy Coders

| Coder | Input type | On-disk format | Notes |
|---|---|---|---|
| `Huffman` | `ArrayList<Integer>` | `.huffman.encoding` + `.huffman.map` | tree-based variable-length coding |
| `Arithmetic` | `int[]` | single `.ar`-style binary blob via `ArithmeticIOHelper` | 16-bit Witten-Neal-Cleary style coder |
| `rANS` | `int[]` | single `.rans`-style binary blob via `rANSIOHelper` | 64-bit state with byte renormalization |

### Important Support Files

| File | Purpose |
|---|---|
| `FilePaths.java` | central path constants, file lists, and extensions |
| `IOHelper.java` | raw file, byte, map, alphabet, and size I/O |
| `HelperFunctions.java` | conversions, alphabet helpers, and equality checking |
| `BitPacker.java` | packs and unpacks identifier bitstrings |
| `ArithmeticIOHelper.java` | serialization for `ArithmeticElement` |
| `rANSIOHelper.java` | serialization for `rANSElement` |
| `SuffixArray.java` | suffix-array support for BWT |

## Entry Points

| Class | Role |
|---|---|
| `Test` | unit tests for Huffman, suffix arrays, bit packing, LZ77, LZSS, BWT, and MTF, followed by full Huffman pipeline tests through `Master` |
| `TestRANS` | standalone rANS unit tests and an `LZ77 + rANS` end-to-end round trip |
| `Master` | runs compress + expand for `LZ77`, `LZSS`, and `BWT + MTF`, all with Huffman |
| `RunBench` | primary benchmark runner with `--files` and `--schemes` filtering |
| `Benchmark` | older benchmark runner kept for comparison / backup |
| `BenchmarkArithmetic` | older wider benchmark runner kept for comparison / backup |

## Dataset

The default dataset list used by `Master` and `RunBench` comes from [`FilePaths.java`](/home/arnab/code/collegeCode/sem6/applications/Project-Initial/FilePaths.java) and currently includes:

- `C.elegans.fna`
- `E.Coli.fna`
- `H.pylori.fna`
- `lcet10.txt`
- `L.monocytogenes.fna`
- `N.crassa.fna`
- `S.cerevisiae.fna`
- `T.nigroviridis.fna`
- `alice29.txt`
- `aliceinwonderland.txt`
- `asyoulik.txt`
- `bible.txt`
- `cp.html`
- `medium.txt`
- `tiny.txt`
- `xargs.1`
- `fields.c`
- `plrabn12.txt`

These are a mix of genome files and text/program-source style benchmark files, including Canterbury-style additions such as `alice29.txt`, `asyoulik.txt`, `cp.html`, `fields.c`, `lcet10.txt`, `plrabn12.txt`, and `xargs.1`.

## TODO
- [x] Implement rANS and Arithmetic entropy encodings in O(n)
- [x] Implement LZ78 and LZW
    - [x] LZ78 and LZW need optimization
        - Before: LZW had String for Dictonary Key and bit handling was manual
        - After: LZW now uses Long for Dictonary Key and native bitwise shift
        - Before : LZ78: Time Complexity: O(N^2), Heavy concatenation every where, String keys in HashMap, Nested loops searching matches.
        - After: LZ78:  Time Complexity: O(N), No string creation during search, Tree nodes with Character maps, Trie traverses character by character.
- [x] Implement BWT+MTF, LZ78 and LZW varations with rANS and Arithmetic coding, and benchmark it.
- [ ] Determine at what size is there scope for JVM running out of memory.
    - Refer to the note in instructions on how to run the benchmark.
    - Note: Failed to run BWT+MTF+Huffman on `T.nigroviridis.fna` with even 6 gb heap.

