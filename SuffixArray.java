import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * To compare two Indexes based on the suffix starting at those Indexes.
 * Used for brute-force implementation.
 */
class Index {

    public int i;

    public Index(int i) {
        this.i = i;
    }

    public String toString() {
        return String.valueOf(i);
    }
}

/**
 * To keep track of the triplets formed during Manber-Myers.
 *
 * Ideally, we should not use this to make it more space-efficient. If I ever
 * get over my laziness, I will get rid of it to save about 3 bytes of space per
 * input character.
 *
 */
class Triplet implements Comparable<Triplet> {

    int firstRank;
    int secondRank;
    int index;

    @Override
    public int compareTo(Triplet that) {
        return (this.firstRank == that.firstRank) ? (this.secondRank - that.secondRank)
                : (this.firstRank - that.firstRank);
    }

    @Override
    public String toString() {
        return String.format("(%d, %d, %d)", firstRank, secondRank, index);
    }
}

public class SuffixArray {

    private static final boolean USE_MANBER_MYERS = false; // set to true for Manber-Myers, false for brute-force

    public static int[] compute(ArrayList<Character> str) {
        if (USE_MANBER_MYERS)
            return manberMyers(str);
        return bruteforce(str);
    }

    /**
     * Should run in O(n log^2 n) expected time. Much more space-efficient than Manber-Myers.
     * Practically faster as well.
     */
    public static int[] bruteforce(ArrayList<Character> str) {
        Integer[] SA = new Integer[str.size()];
        for (int i = 0; i < str.size(); i++)
            SA[i] = i;
        Arrays.sort(SA, (x, y) -> {
            if (y == x)
                return 0;
            int i = 0;
            // whichever is earlier: x reaches end or y reaches end (both cannot happen), or mismatch
            while (x + i < str.size() && y + i < str.size() && str.get(x + i) == str.get(y + i))
                i++;
            // if x reached end, x is smaller
            if (x + i == str.size())
                return -1;
            // if y reached end, y is smaller
            if (y + i == str.size())
                return 1;
            // compare
            return str.get(x + i) - str.get(y + i);
        });

        return Arrays.stream(SA).mapToInt(Integer::intValue).toArray();
    }

    /**
     * A Manber-Myers implementation that runs in O(n log^2 n) time due to the use
     * of Timsort instead of radix-sort.
     *
     * Although this is worse asymptotically, due to the use of hybrid sorting
     * algorithm, its performance has been found to be superior to radix-sort (about
     * 2-3 times faster on average).
     *
     * Moreover, it uses far less space than radix sort.
     *
     */
    public static int[] manberMyers(ArrayList<Character> str) {
        int n = str.size();

        int[] RANK = new int[n]; // Stores current suffix ranks; basically base-1 indexed inverse suffix array
        Triplet[] triplets = new Triplet[n];
        for (int i = 0; i < n; i++)
            triplets[i] = new Triplet();

        ArrayList<Character> sigma = HelperFunctions.getSortedAlphabet(str);

        for (int i = 0; i < n; i++)
            RANK[i] = Collections.binarySearch(sigma, str.get(i));

        int e = 1; // will grow in powers of 2
        int rank; // to keep track of suffix ranks based on the first e characters
        do {

            int i;
            for (i = 0; i + e < n; i++) {
                // (current rank = ISA[i], next rank = ISA[i + e], current index)
                triplets[i].firstRank = RANK[i];
                triplets[i].secondRank = RANK[i + e];
                triplets[i].index = i;
            }
            for (; i < n; i++) {
                // if i + e >= n, then next rank = -1 (simulates $)
                triplets[i].firstRank = RANK[i];
                triplets[i].secondRank = -1;
                triplets[i].index = i;
            }

            Arrays.sort(triplets);

            rank = 0; // smallest suffix has rank 0
            RANK[triplets[0].index] = 0;
            for (i = 1; i < n; i++) {
                if (triplets[i].firstRank != triplets[i - 1].firstRank
                        || triplets[i].secondRank != triplets[i - 1].secondRank) // if current triplet ranks are
                    // different from previous triplet
                    // ranks, then increment rank
                    rank++;
                RANK[triplets[i].index] = rank;
            }
            e *= 2;
        } while (rank < n - 1); // if rank reaches n-1, all suffixes have achieved distinct rank
        int[] SA = new int[n];
        for (int i = 0; i < n; i++)
            SA[RANK[i]] = i;
        return SA;
    }
}
