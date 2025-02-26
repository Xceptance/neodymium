package com.xceptance.neodymium.util;

import java.util.Random;

import org.apache.commons.text.TextRandomProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
         *            the seed
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
        }

        /**
         * Reinitializes the current thread's random number generator with a new seed value that is derived from the
         * current seed.
         */
        public synchronized void reseed()
        {
            setSeed(31 * seed + 1);
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
     * Reinitializes the current thread's random number generator with the given seed value. Use this method together
     * with {@link #getSeed()} to reset the random number generator to a defined state in which it will produce the same
     * sequence of random numbers.
     * 
     * @param seed
     *            the seed
     */
    public static void setSeed(long seed)
    {
        random.get().setSeed(seed);
    }

    /**
     * Returns the seed that was used to initialize the current thread's random number generator. Use this method
     * together with {@link #setSeed(long)} to reset the random number generator to a defined state in which it will
     * produce the same sequence of random numbers.
     * 
     * @return the seed
     */
    public static long getSeed()
    {
        return random.get().getSeed();
    }

    /**
     * Reinitializes the current thread's random number generator with a new seed value that is derived from the current
     * seed.
     */
    public static void reseed()
    {
        random.get().reseed();
    }

    /**
     * @see java.util.Random#nextBoolean()
     * @return a random boolean value
     */
    public static boolean nextBoolean()
    {
        return random.get().nextBoolean();
    }

    /**
     * Returns a random boolean value where the probability that <code>true</code> is returned is given as parameter.
     * The probability value has to be specified in the range of 0-100.
     * <ul>
     * <li>&le; 0 - never returns <code>true</code></li>
     * <li>1..99 - the probability of <code>true</code> being returned</li>
     * <li>&ge; 100 - always returns <code>true</code></li>
     * </ul>
     * 
     * @param trueCaseProbability
     *            the probability of <code>true</code> being returned
     * @return a random boolean value
     */
    public static boolean nextBoolean(final int trueCaseProbability)
    {
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
     * @see java.util.Random#nextBytes(byte[])
     * @param bytes
     *            the byte array to fill with random bytes
     */
    public static void nextBytes(final byte[] bytes)
    {
        random.get().nextBytes(bytes);
    }

    /**
     * @see java.util.Random#nextDouble()
     * @return a random double value
     */
    public static double nextDouble()
    {
        return random.get().nextDouble();
    }

    /**
     * @see java.util.Random#nextFloat()
     * @return a random float value
     */
    public static float nextFloat()
    {
        return random.get().nextFloat();
    }

    /**
     * @see java.util.Random#nextGaussian()
     * @return a random gaussian value
     */
    public static double nextGaussian()
    {
        return random.get().nextGaussian();
    }

    /**
     * @see java.util.Random#nextInt()
     * @return a random int value
     */
    public static int nextInt()
    {
        return random.get().nextInt();
    }

    /**
     * @see java.util.Random#nextInt(int) <br>
     *      ATTENTION: A difference to the standard implementation is that we return 0 for n=0 instead of an
     *      IllegalArgumentException
     * @param n
     *            upper bound (exclusive)
     * @return a random int value
     */
    public static int nextInt(final int n)
    {
        return n != 0 ? random.get().nextInt(n) : 0;
    }

    /**
     * @see java.util.Random#nextLong()
     * @return a random long value
     */
    public static long nextLong()
    {
        return random.get().nextLong();
    }

    /**
     * Returns the random number generator singleton.
     * 
     * @return the random number generator
     */
    public static Random getRandom()
    {
        return random.get();
    }

    public static InternalRandom getNeodymiumRandom()
    {
        String initText = "NeodymiumRandom initialized with seed: " + getSeed();
        AllureAddons.addToReport(initText, null);
        LOGGER.info(initText);
        return random.get();
    }

    /**
     * Returns a random number based on a given array of integers.
     * 
     * @param data
     *            an array with integers to choose from
     * @return a random number from the array
     * @throws ArrayIndexOutOfBoundsException
     *             will be thrown when an empty array is given
     */
    public static int getRandom(final int[] data)
    {
        // no data available
        if (data == null || data.length == 0)
        {
            throw new ArrayIndexOutOfBoundsException("No data was given to pick from");
        }

        return data[nextInt(data.length)];
    }

    /**
     * Returns a pseudo-random, uniformly distributed number that lies within the range from [base - deviation, base +
     * deviation].
     * 
     * @param base
     *            base integer for the number
     * @param deviation
     *            the maximum deviation from base
     * @return a random number
     */
    public static int nextIntWithDeviation(final int base, int deviation)
    {
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
     *            the minimum value (inclusive)
     * @param maximum
     *            the maximum value (inclusive)
     * @return a random number
     */
    public static int nextInt(final int minimum, final int maximum)
    {
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
}
