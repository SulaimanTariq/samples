/* Copyright (C) 2015 Covisint. All Rights Reserved. */

package com.covisint.platform.sample.httpsdk;

import java.util.List;

import org.apache.http.protocol.BasicHttpContext;

import com.covisint.core.http.service.client.CacheSpec;
import com.covisint.core.http.service.client.CacheSpec.ExpirationMode;
import com.covisint.core.http.service.core.Page;
import com.covisint.core.http.service.core.ServiceException;
import com.covisint.core.http.service.core.SortCriteria;
import com.covisint.platform.user.client.person.PersonClient;
import com.covisint.platform.user.client.person.PersonSDK;
import com.covisint.platform.user.core.person.Person;
import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.CheckedFuture;

public class CommonSamples {

    /** Demonstrates how to configure and instantiate a client through the associated factory. */
    public static void configureClient() {

        /*
         * Create a new, basic SDK for the "person" service. This example, although minimal, is fully configured with
         * preset default values and is ready to create clients.
         */
        PersonSDK sdk = new PersonSDK(ServiceUrl.PERSON_V1.getValue());

        /*
         * We will configure this client to cache returned entity bodies. If no caching is desired, simply omit the
         * following setup steps.
         */
        CacheSpec.Builder cacheConfigBuilder = CacheSpec.builder();

        /*
         * Expire cache elements exactly 5 minutes after the element was put into the cache. The other alternative is
         * ExpirationMode.AFTER_ACCESS, which will expire the cache 5 minutes after the element was last retrieved from
         * a cache hit.
         */
        cacheConfigBuilder.expiration(300000, ExpirationMode.AFTER_WRITE);

        /*
         * Limit the number of elements in the cache. The strategy to keep an upper bound on the cache elements is
         * undocumented and subject to change at any time.
         */
        cacheConfigBuilder.maxElements(1000);

        /* We are finished, now build the configuration spec and set it into the SDK. */
        CacheSpec cacheConfig = cacheConfigBuilder.build();
        sdk.setCacheSpec(cacheConfig);

        /*
         * Sets the maximum period of inactivity between two consecutive data packets in milliseconds. In this case we
         * want to wait up to 2 seconds before we give up on the response and throw a timeout error.
         */
        sdk.setSocketTimeout(2000);

        /* Set the socket buffer size in bytes. In this example, a 2K buffer will be used. */
        sdk.setSocketBufferSize(2048);

        /* Set the source IP address of outbound connections. */
        sdk.setSourceAddress("0.0.0.0");

        /* Keep an upper bound on the number of concurrent HTTP requests issued by this SDK. */
        sdk.setMaxConcurrentRequests(20);

        /*
         * Set redirect (302) preferences. If redirects should not be followed, just set the option to false. Defaults
         * to true.
         */
        sdk.setFollowRedirects(true);
        sdk.setMaxRedirects(5);

        /* Turn content compression (i.e. gzip) on or off. Defaults to true. */
        sdk.setContentCompressionEnabled(false);

        /* Set the content charset. Defaults to UTF-8. */
        sdk.setContentCharSet(Charsets.UTF_8);

        /* We are done configuring the SDK to our preferences, now call #create to get our person client instance. */
        PersonClient personClient = sdk.create();

        /* And now we may invoke its methods. */
        personClient.activate("a15cab31e2a1", new BasicHttpContext());
    }

    /** Demonstrates the capabilities of the search APIs. */
    public static void searchOptions() {

        /* Build the client as needed. */
        PersonClient client = null;

        /*
         * Set up the filter criteria for our search. A multimap is used for the cases where multiple parameter values
         * can be supplied, for example, search with an array of resource ids. Keys and values are always string types,
         * to make things simpler to work with.
         * 
         * In this case, we are going to set up an empty person search, which will simply return all users available in
         * the system.
         */
        Multimap<String, String> searchCriteria = ArrayListMultimap.<String, String> create();

        /*
         * We can optionally request that the results be sorted. Multiple sort fields may be passed, in which case the
         * sort is applied in that specific order. By default, no prefix is required and the sort on the unprefixed
         * field will be applied ascending. If a descending sort is required, as shown below, then a '-' prefix must be
         * specified before the field name. In this case, we are requesting to sort the results in order of descending
         * creation instant (newest first). If we don't require a sort, we use the utility value SortCriteria.NONE
         */
        SortCriteria sortCriteria = SortCriteria.builder().parseSortField("-creation").build();

        /*
         * Pagination on results is applied with the Page object. The first constructor argument is the page index,
         * which always starts at 1. The second argument is the page size. This example shows how to request
         * "the first 30 results".
         */
        Page page = new Page(1, 30);

        /* Now we pass the search, sort and paging criteria to the search method. */
        List<Person> firstPage = client.search(searchCriteria, sortCriteria, page, new BasicHttpContext()).checkedGet();

        if (firstPage.size() == 30) {
            /*
             * If we have more results to show, then continue by fetching the next set of results to display the second
             * page.
             */
            List<Person> secondPage = client.search(searchCriteria, sortCriteria, new Page(2, 30),
                    new BasicHttpContext()).checkedGet();
        }
    }

    /** Shows how you can provide your own source of client credentials. */
    public static void customCredentialProvider() {

    }

    /** Shows how you may catch and work with exceptions through the client. */
    public static void errorHandling() {

        /* Build the client as needed. */
        PersonClient client = null;

        try {
            /* Each client method (activate, in this case) throws a ServiceException with the details. */
            client.activate("b1acee3510a01", new BasicHttpContext()).checkedGet();
        } catch (ServiceException e) {
            /* If we get here, there has been a service exception thrown. Let's do something meaningful with it. */

            /* A human-readable message describing the error. */
            System.out.println(e.getMessage());

            /*
             * The service status code is an error code that is defined within the API documentation. Consult the online
             * documentation for the full set of status codes for each API. We will make up a few below just for example
             * sake.
             */
            String statusCode = e.getServiceStatusCode();

            switch (statusCode) {
                case "framework:request:invalid:id":
                    // Do something.
                    break;

                case "framework:request:id:conflict":
                    // Do something.
                    break;

                case "person.illegal.state.change":
                    // Do something.
                    break;
                default:
                    // Just swallow everything else!
                    break;
            }

        }
    }

    /** Demonstrates how to execute multiple calls in parallel and join them later. */
    public static void asyncRequests() {

        /* Build the client as needed. */
        PersonClient client = null;

        /* Execute 3 non-blocking calls to the person service. */
        CheckedFuture<Person, ServiceException> getFuture = client.get("100ae20131cdbe1", new BasicHttpContext());

        CheckedFuture<List<Person>, ServiceException> searchFuture = client.search(
                ArrayListMultimap.<String, String> create(), SortCriteria.NONE, Page.DEFAULT, new BasicHttpContext());

        CheckedFuture<Person, ServiceException> deleteFuture = client.delete("55e10ee23cb10de", new BasicHttpContext());

        // Now retrieve them asynchronously.
        Person person = getFuture.checkedGet();
        List<Person> results = searchFuture.checkedGet();
        deleteFuture.checkedGet();
    }

}
