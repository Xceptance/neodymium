package com.xceptance.neodymium.common.xtc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * A utility class with static methods for common HTTP operations, including a request sender with a retry mechanism.
 */
public final class HttpUtils
{
    // Configuration for the retry mechanism
    private static final int MAX_RETRIES = XtcApiContext.configuration.xtcApiNumberOfRetries();

    private static final long INITIAL_RETRY_DELAY_MS = XtcApiContext.configuration.xtcApiRetryDelay();

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

    /**
     * A private constructor to prevent instantiation of this utility class.
     */
    private HttpUtils()
    {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Sends an HttpRequest with a retry mechanism using exponential backoff. It retries on 5xx server errors or IOExceptions.
     *
     * @param client
     *     The HttpClient to use for sending the request.
     * @param request
     *     The HttpRequest to send.
     * @return The HttpResponse if successful or after exhausting retries.
     * @throws IOException
     *     If a non-retryable error occurs or retries are exhausted.
     * @throws InterruptedException
     *     If the thread is interrupted while waiting.
     */
    public static HttpResponse<String> sendWithRetries(HttpClient client, HttpRequest request) throws IOException, InterruptedException
    {
        int attempt = 0;
        long delay = INITIAL_RETRY_DELAY_MS;
        HttpResponse<String> response = null;

        while (attempt < MAX_RETRIES)
        {
            attempt++;
            try
            {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // If status is a server error (5xx), we should retry.
                if (response.statusCode() >= 500)
                {
                    if (attempt >= MAX_RETRIES)
                    {
                        return response; // Return the last failed response after all retries
                    }
                    LOGGER.warn("Request to {} failed with status {}. Retrying in {} ms... (Attempt {}/{})",
                                request.uri(), response.statusCode(), delay, attempt, MAX_RETRIES);

                    Thread.sleep(delay);
                    delay *= 2; // Exponential backoff

                    continue; // Go to the next iteration of the loop
                }
                // For any other status (success 2xx, client error 4xx), we return immediately.
                return response;
            }
            catch (IOException e)
            {
                // This catches network errors, timeouts, etc.
                if (attempt >= MAX_RETRIES)
                {
                    throw e; // Rethrow the exception if we've exhausted all retries
                }
                LOGGER.warn("Request to {} failed with IOException: {}. Retrying in {} ms... (Attempt {}/{})",
                            request.uri(), e.getMessage(), delay, attempt, MAX_RETRIES);

                Thread.sleep(delay);
                delay *= 2; // Exponential backoff
            }
        }

        // This line should ideally not be reached, but in case it is, throw an exception.
        LOGGER.error("Failed to get a successful response for {} after {} attempts.", request.uri(), MAX_RETRIES);
        throw new IOException("Failed to get a successful response for " + request.uri() + " after " + MAX_RETRIES + " attempts.");
    }
}
