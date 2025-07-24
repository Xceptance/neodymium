package com.xceptance.neodymium.util;

import org.apache.commons.text.TextRandomProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Utility class for random numbers and strings.
 * <p>
 * Note that this class maintains a separate random number generator instance per thread.
 *
 * @author René Schwietzke (Xceptance Software Technologies GmbH) (inital)
 * @author Marcel Pfotenhauer (Xceptance Software Technologies GmbH) (adjustments)
 */
public class NeodymiumRandom
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NeodymiumRandom.class);

    private static SeedState seedState = SeedState.INITIALIZED;

    /**
     * A subclass of {@link Random} that allows access to the seed value used to initialize an instance.
     */
    private static final class InternalRandom extends Random implements TextRandomProvider
    {
        /**
         *
         */
        private static final long serialVersionUID = 4427162532661078060L;

        /**
         * The seed of the RNG.
         */
        private long seed;

        /**
         * Creates a new {@link InternalRandom} and initializes it with the given seed.
         *
         * @param seed
         *     the seed
         */
        public InternalRandom(long seed)
        {
            super(seed);
            this.seed = seed;
        }

        /**
         * Returns the seed used to initialize this instance.
         *
         * @return the seed
         */
        public synchronized long getSeed()
        {
            return seed;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void setSeed(long seed)
        {
            this.seed = seed;
            super.setSeed(seed);

            NeodymiumRandom.seedState = NeodymiumRandom.seedState == SeedState.INITIALIZED ? SeedState.INITIALIZED : SeedState.RESEEDED;
        }

        /**
         * Reinitializes the current thread's random number generator with a new seed value that is derived from the current seed.
         */
        public synchronized void reseed()
        {
            setSeed(31 * seed + 1);

            NeodymiumRandom.seedState = SeedState.RESEEDED;
        }
    }

    /**
     * A thread based random pool.
     */
    private static final ThreadLocal<InternalRandom> random = new ThreadLocal<InternalRandom>()
    {
        @Override
        protected InternalRandom initialValue()
        {
            Long configuredInitialValue = Neodymium.configuration().initialRandomValue();
            long initialValue = configuredInitialValue == null ? System.currentTimeMillis() : configuredInitialValue;

            return new InternalRandom(initialValue);
        }
    };

    /**
     * Reinitializes the current thread's random number generator with the given seed value. Use this method together with {@link #getSeed()} to reset the
     * random number generator to a defined state in which it will produce the same sequence of random numbers.
     *
     * @param seed
     *     the seed
     */
    public static void setSeed(long seed)
    {
        random.get().setSeed(seed);
        seedState = seedState == SeedState.INITIALIZED ? SeedState.INITIALIZED : SeedState.RESEEDED;
    }

    /**
     * Returns the seed that was used to initialize the current thread's random number generator. Use this method together with {@link #setSeed(long)} to reset
     * the random number generator to a defined state in which it will produce the same sequence of random numbers.
     *
     * @return the seed
     */
    public static long getSeed()
    {
        return random.get().getSeed();
    }

    /**
     * Reinitializes the current thread's random number generator with a new seed value that is derived from the current seed.
     */
    public static void reseed()
    {
        random.get().reseed();
        seedState = SeedState.RESEEDED;
    }

    /**
     * Reinitialize the random seed to the initial one
     */
    public static void reinitializeRandomSeed()
    {
        if (Neodymium.configuration().initialRandomValue() != null)
        {
            // set the seed from the properties
            NeodymiumRandom.setSeed(Neodymium.configuration().initialRandomValue());
        }
        else
        {
            NeodymiumRandom.setSeed(random.get().nextLong());
        }

        seedState = seedState == SeedState.INITIALIZED ? SeedState.INITIALIZED : SeedState.RESEEDED;
    }

    /**
     * Reinitialize the random seed to the initial one
     */
    public static void reinitializeRandomSeed(SeedState state)
    {
        if (Neodymium.configuration().initialRandomValue() != null)
        {
            // set the seed from the properties
            NeodymiumRandom.setSeed(Neodymium.configuration().initialRandomValue());
        }
        else
        {
            NeodymiumRandom.setSeed(random.get().nextLong());
        }

        seedState = state;
    }

    /**
     * @return a random boolean value
     * @see java.util.Random#nextBoolean()
     */
    public static boolean nextBoolean()
    {
        logSeedIfNotLogged();
        return random.get().nextBoolean();
    }

    /**
     * Returns a random boolean value where the probability that <code>true</code> is returned is given as parameter. The probability value has to be specified
     * in the range of 0-100.
     * <ul>
     * <li>&le; 0 - never returns <code>true</code></li>
     * <li>1..99 - the probability of <code>true</code> being returned</li>
     * <li>&ge; 100 - always returns <code>true</code></li>
     * </ul>
     *
     * @param trueCaseProbability
     *     the probability of <code>true</code> being returned
     * @return a random boolean value
     */
    public static boolean nextBoolean(final int trueCaseProbability)
    {
        logSeedIfNotLogged();

        if (trueCaseProbability <= 0)
        {
            return false;
        }
        else if (trueCaseProbability >= 100)
        {
            return true;
        }
        else
        {
            // number from 0 to 100
            final int v = random.get().nextInt(101);

            return v <= trueCaseProbability;
        }
    }

    /**
     * @param bytes
     *     the byte array to fill with random bytes
     * @see java.util.Random#nextBytes(byte[])
     */
    public static void nextBytes(final byte[] bytes)
    {
        logSeedIfNotLogged();
        random.get().nextBytes(bytes);
    }

    /**
     * @return a random double value
     * @see java.util.Random#nextDouble()
     */
    public static double nextDouble()
    {
        logSeedIfNotLogged();
        return random.get().nextDouble();
    }

    /**
     * @return a random float value
     * @see java.util.Random#nextFloat()
     */
    public static float nextFloat()
    {
        logSeedIfNotLogged();
        return random.get().nextFloat();
    }

    /**
     * @return a random gaussian value
     * @see java.util.Random#nextGaussian()
     */
    public static double nextGaussian()
    {
        logSeedIfNotLogged();
        return random.get().nextGaussian();
    }

    /**
     * @return a random int value
     * @see java.util.Random#nextInt()
     */
    public static int nextInt()
    {
        logSeedIfNotLogged();
        return random.get().nextInt();
    }

    /**
     * @param n
     *     upper bound (exclusive)
     * @return a random int value
     * @see java.util.Random#nextInt(int) <br> ATTENTION: A difference to the standard implementation is that we return 0 for n=0 instead of an
     *     IllegalArgumentException
     */
    public static int nextInt(final int n)
    {
        logSeedIfNotLogged();
        return n != 0 ? random.get().nextInt(n) : 0;
    }

    /**
     * @return a random long value
     * @see java.util.Random#nextLong()
     */
    public static long nextLong()
    {
        logSeedIfNotLogged();
        return random.get().nextLong();
    }

    /**
     * Returns the random number generator singleton.
     *
     * @return the random number generator
     */
    public static Random getRandom()
    {
        logSeedIfNotLogged();
        return random.get();
    }

    public static InternalRandom getNeodymiumRandom()
    {
        logSeedIfNotLogged();
        return random.get();
    }

    /**
     * Returns a random number based on a given array of integers.
     *
     * @param data
     *     an array with integers to choose from
     * @return a random number from the array
     * @throws ArrayIndexOutOfBoundsException
     *     will be thrown when an empty array is given
     */
    public static int getRandom(final int[] data)
    {
        logSeedIfNotLogged();

        // no data available
        if (data == null || data.length == 0)
        {
            throw new ArrayIndexOutOfBoundsException("No data was given to pick from");
        }

        return data[nextInt(data.length)];
    }

    /**
     * Returns a pseudo-random, uniformly distributed number that lies within the range from [base - deviation, base + deviation].
     *
     * @param base
     *     base integer for the number
     * @param deviation
     *     the maximum deviation from base
     * @return a random number
     */
    public static int nextIntWithDeviation(final int base, int deviation)
    {
        logSeedIfNotLogged();

        if (deviation == 0)
        {
            return base;
        }

        if (deviation < 0)
        {
            deviation = -deviation;
        }

        return nextInt(base - deviation, base + deviation);
    }

    /**
     * Returns a pseudo-random, uniformly distributed number that lies within the range from [minimum, maximum].
     *
     * @param minimum
     *     the minimum value (inclusive)
     * @param maximum
     *     the maximum value (inclusive)
     * @return a random number
     */
    public static int nextInt(final int minimum, final int maximum)
    {
        logSeedIfNotLogged();

        if (minimum > maximum)
        {
            throw new IllegalArgumentException(String.format("The minimum value (%d) is greater than the maximum value (%d)", minimum,
                                                             maximum));
        }

        final int diff = maximum - minimum;
        if (diff < 0)
        {
            throw new IllegalArgumentException("The difference of maximum value and minimum value must not be greater than (Integer.MAX_VALUE-1).");
        }

        final int randomValue = nextInt(diff + 1);

        return minimum + randomValue;
    }

    private static void logSeedIfNotLogged()
    {
        switch (seedState)
        {
            case INITIALIZED:
                String infoText = "INFO: random initialized with seed: " + getSeed();

                AllureAddons.addInfoAsFirstStep(infoText);
                LOGGER.info(infoText);
                seedState = SeedState.LOGGED;
                break;
                
            case RESEEDED:
                infoText = "INFO: random reseeded with seed: " + getSeed();

                AllureAddons.addInfoBeforeStep(infoText);
                LOGGER.info(infoText);
                seedState = SeedState.LOGGED;
                break;

            case LOGGED:
                // do nothing
                break;
        }
    }

    public enum SeedState
    {
        INITIALIZED, RESEEDED, LOGGED;
    }
}
