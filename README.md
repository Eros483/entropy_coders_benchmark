# Benchmarking Huffman, Arithmetic and rANS Entropy Coding Pipelines

## Project Overview

This project benchmarks multiple lossless compression pipelines written in plain Java. The transform layer uses `LZ77`, `LZSS`, `BWT + MTF`, `LZ78`, and `LZW`, and the entropy layer uses `Huffman`, `Arithmetic`, and `rANS`.

No build tool or external dependencies are required. The project compiles with `javac`.

## Directory Structure
```
Entropy encoders Benchmark
├── artifacts        # contains graphs to highlight relations between algorithms
├── benchmark        # code for base tests
├── bin              # java compiled classes
├── Data             # all dataset files
├── docs             # cleaned benchmark results and documentation on how benchmark works
├── raw              # raw terminal output from benchmark
└── src              # contains all primary algorithm files
```


## Data Setup

Download the data folder from [Google Drive](https://drive.google.com/file/d/1Atd143ZsA6HaBL1MPAJuAJvgYFYET0Jo/view) and extract it so that the `Data/` directory is in the project root.

## Compilation And Execution

### Compilation

```bash
# Compile all packages in order (core -> compress -> benchmark -> RunBench)
mkdir -p bin
javac -d bin src/core/*.java
javac -d bin -cp bin src/compress/*.java
javac -d bin -cp bin benchmark/*.java
javac -d bin -cp bin RunBench.java
```

### Basic Tests
Checks compression by decoding the output, and verifying.

```bash
java -cp bin benchmark.Test
java -cp bin benchmark.TestRANS
```

### Running the Benchmark
**Note**: Running the benchmark can take over 1.5 hours.
- Refer to `docs/results.txt` for benchmark results.
- Can refer to `artifacts/benchmark_run_raw.txt` if raw output of benchmark desired.

```bash
java -cp bin RunBench
```

#### Benchmark Arguments

The benchmark supports filtering by files and schemes:

```bash
# All schemes, all Data/ files
java -cp bin RunBench

# All schemes, specific files
java -cp bin RunBench --files tiny.txt,L.monocytogenes.fna

# Specific schemes on all files
java -cp bin RunBench --schemes LZ77+rANS,LZ77+Arith

# Specific schemes on specific file
java -cp bin RunBench --schemes LZ78+rANS,LZW+Arith --files bible.txt
```

## Dataset

The project includes 17 test files, including genome files, code files and text documents, chosen from the `Canterbury corpus`.
