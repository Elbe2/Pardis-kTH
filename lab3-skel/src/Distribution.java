import java.util.Random;
import java.util.Arrays;

public interface Distribution {
        int next();
        Distribution copy();
        Distribution copy(int seed);

        public static class Uniform implements Distribution {
                private final int min, max, seed;
                private final Random prng;

                public Uniform(int seed, int min, int max) {
                        this.min = min;
                        this.max = max;
                        this.seed = seed;
                        this.prng = new Random(seed);
                }

                public int next() {
                        return prng.nextInt(max - min) + min;
                }

                public Distribution copy() {
                        return copy(this.seed);
                }

                public Distribution copy(int seed) {
                        return new Uniform(seed, min, max);
                }
        }

        public static class Discrete implements Distribution {
                private final int[] prob;
                private final int seed, max;
                private final Random prng;

                public Discrete(int seed, int[] prob) {
                        int max = 0;
                        for (int i = 0; i < prob.length; ++i)
                                max += prob[i];
                        this.max = max;
                        this.prob = Arrays.copyOf(prob, prob.length);
                        this.seed = seed;
                        this.prng = new Random(seed);
                }

                public int next() {
                        int p = prng.nextInt(max);
                        for (int i = 0; i < prob.length; ++i) {
                                if (p < prob[i])
                                        return i;
                                p -= prob[i];
                        }
                        // Should never get here.
                        return -1;
                }

                public Distribution copy() {
                        return copy(this.seed);
                }

                public Distribution copy(int seed) {
                        return new Discrete(seed, prob);
                }
        }

        // Approximation of normal distribution by using the central limit theorem.
        // Increasing samples causes the distribution to converge to the normal distribution.
        public static class Normal implements Distribution {
                private final int min, max, samples, seed;
                private final Random prng;

                public Normal(int seed, int samples, int min, int max) {
                        this.min = min;
                        this.max = max;
                        this.samples = samples;
                        this.seed = seed;
                        this.prng = new Random(seed);
                }

                public int next() {
                        int sum = 0;
                        for (int i = 0; i < samples; ++i)
                                sum += prng.nextInt(max - min) + min;
                        return sum / samples;
                }

                public Distribution copy() {
                        return copy(this.seed);
                }

                public Distribution copy(int seed) {
                        return new Normal(seed, samples, min, max);
                }
        }
}
