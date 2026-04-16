## How The Pipelines Work

### Transform Layer

The transform layer converts raw input bytes into a more compressible intermediate representation.

| Transform | Output | Parameters | Notes |
|-----------|--------|------------|-------|
| **LZ77** | `delta`, `length`, `next` streams | `WINDOW=1024`, `LOOKAHEAD=128` | Sliding window. `delta` = offset back in window, `length` = match length, `next` = next literal character |
| **LZSS** | identifier bits + `delta`, `length`, `next` streams | `LENGTH_THRESHOLD=4` | Like LZ77 but uses a bit to flag literal vs match |
| **BWT + MTF** | MTF index stream + stored alphabet | block-based | Burrows-Wheeler Transform followed by Move-To-Front. Alphabet saved separately for decode |
| **LZ78** | `(dictionary_index, character)` pairs | dictionary-based | Lempel-Ziv 78. Uses trie with O(N) traversal |
| **LZW** | encoded index streams | dynamic dictionary | Lempel-Ziv-Welch. Uses `Long` dictionary keys |

### Entropy Coding Layer

The entropy coder compresses the transform output based on symbol frequency.

| Coder | Input | Output Format | Algorithm |
|-------|-------|----------------|-----------|
| **Huffman** | `ArrayList<Integer>` | `.huffman.encoding` + `.huffman.map` | Binary tree, variable-length codes |
| **Arithmetic** | `int[]` | `.ar` binary blob | 16-bit Witten-Neal-Cleary |
| **rANS** | `int[]` | `.rans` binary blob | 64-bit state with byte renormalization |

### Pipeline Combinations

Each transform can be paired with any entropy coder (though some combinations are more effective):

| Scheme | Transform Path | Entropy Coder |
|--------|-----------------|---------------|
| `LZ77+Huffman` | Data → LZ77 triplets | Huffman |
| `LZ77+Arith` | Data → LZ77 triplets | Arithmetic |
| `LZ77+rANS` | Data → LZ77 triplets | rANS |
| `LZSS+Huffman` | Data → LZSS streams | Huffman |
| `BWT+MTF+Huffman` | Data → BWT → MTF | Huffman |
| `BWT+MTF+Arith` | Data → BWT → MTF | Arithmetic |
| `BWT+MTF+rANS` | Data → BWT → MTF | rANS |
| `LZ78+Arith` | Data → LZ78 pairs | Arithmetic |
| `LZ78+rANS` | Data → LZ78 pairs | rANS |
| `LZW+Arith` | Data → LZW indices | Arithmetic |
| `LZW+rANS` | Data → LZW indices | rANS |

## Program Structure

### Core Classes (`src/core/`)

| Class | Purpose |
|-------|---------|
| `LZ77.java` | LZ77 sliding window compression |
| `LZSS.java` | LZSS with literal/match flags |
| `BWT.java` | Burrows-Wheeler transform |
| `MTF.java` | Move-to-front encoding |
| `LZ78.java` | LZ78 dictionary compression |
| `LZW.java` | Lempel-Ziv-Welch compression |
| `Huffman.java` | Huffman coding |
| `Arithmetic.java` | Arithmetic coding |
| `rANS.java` | Range ANS coding |

### Support Classes (`src/compress/`)

| Class | Purpose |
|-------|---------|
| `FilePaths.java` | Central path constants, file lists, extensions |
| `IOHelper.java` | Raw file, byte, map, alphabet I/O |
| `HelperFunctions.java` (core) | Conversions, alphabet helpers |
| `BitPacker.java` (core) | Packs/unpacks identifier bitstrings |
| `ArithmeticIOHelper.java` | Serialization for ArithmeticElement |
| `rANSIOHelper.java` | Serialization for rANSElement |
| `HuffmanIOHelper.java` | Serialization for HuffmanElement |
| `SuffixArray.java` (core) | Suffix array for BWT |

### Entry Points

| Class | Role |
|-------|------|
| `benchmark/Test` | Unit tests for Huffman, suffix arrays, bit packing, LZ77, LZSS, BWT, MTF; then full Huffman pipeline tests |
| `benchmark/TestRANS` | Standalone rANS unit tests + LZ77+rANS round trip |
| `benchmark/Master` | Runs compress+expand for LZ77, LZSS, BWT+MTF with Huffman |
| `RunBench` | Primary benchmark driver with --files and --schemes filtering |