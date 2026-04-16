# Benchmarking Huffman, Arithmetic and rANS Entropy Coding Pipelines

## Project Overview

This project benchmarks multiple lossless compression pipelines written in plain Java. The transform layer uses `LZ77`, `LZSS`, `BWT + MTF`, `LZ78`, and `LZW`, and the entropy layer uses `Huffman`, `Arithmetic`, and `rANS`.

Refer to `/docs` for further information on classes.

No build tool or external dependencies are required. The project compiles with `javac`.

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

The project includes 17 test files, including genome files and usual text documents, chosen from the `Canterbury corpus`.
## TODO

- Implement the objectives instructed in class.
    - [ ] Measure peak memory consumption
