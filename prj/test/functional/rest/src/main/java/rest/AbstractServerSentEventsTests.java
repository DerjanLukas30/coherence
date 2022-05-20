/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest;


import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.rest.events.SimpleMapEvent;
import com.tangosol.net.NamedCache;

import rest.data.Persona;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.WebTarget;

import org.codehaus.jettison.json.JSONException;

import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;

import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.Matchers.is;


/**
 * @author Aleksandar Seovic  2015.06.26
 */
public abstract class AbstractServerSentEventsTests
        extends AbstractRestTests
    {
    /**
     * Default constructor.
     *
     * @param sPath the configuration resource name or file path
     */
    public AbstractServerSentEventsTests(String sPath)
        {
        super(sPath);
        }

    // ---- tests -----------------------------------------------------------
    @Test
    public void testCacheSSE()
            throws JSONException, InterruptedException
        {
        final Map<String, Integer> mapCounts = new HashMap<>();

        WebTarget   target = getWebTarget("dist-test-named-query");
        EventSource source = createEventSource(target, mapCounts);

        NamedCache cache = getNamedCache("dist-test-named-query");
        cache.remove(2);
        cache.put(2, new Persona("Aleks", 40));
        cache.put(1, new Persona("Ivan", 36));
        cache.remove(3);

        Eventually.assertDeferred(() -> mapCounts.size(), is(3));
        Eventually.assertDeferred(() -> mapCounts.get("insert"), is(1));
        Eventually.assertDeferred(() -> mapCounts.get("update"), is(1));
        Eventually.assertDeferred(() -> mapCounts.get("delete"), is(2));

        source.close();
        }

    @Test
    public void testFilterSSE()
            throws JSONException, InterruptedException
        {
        final Map<String, Integer> mapCounts = new HashMap<>();

        WebTarget   target = getWebTarget("dist-test-named-query?q=age=37");
        EventSource source = createEventSource(target, mapCounts);

        NamedCache cache = getNamedCache("dist-test-named-query");
        cache.put(1, new Persona("Ivan Cikic", 33));
        cache.put(1, new Persona("Ivan", 37));
        cache.put(2, new Persona("Aleks", 40));
        cache.put(3, new Persona("Vaso Putica", 37));
        cache.remove(2);
        cache.remove(3);

        Eventually.assertDeferred(() -> mapCounts.size(), is(3));
        Eventually.assertDeferred(() -> mapCounts.get("insert"), is(1));
        Eventually.assertDeferred(() -> mapCounts.get("update"), is(1));
        Eventually.assertDeferred(() -> mapCounts.get("delete"), is(2));

        source.close();
        }

    @Test
    public void testNamedQuerySSE()
            throws JSONException, InterruptedException
        {
        final Map<String, Integer> mapCounts = new HashMap<>();

        WebTarget   target = getWebTarget("dist-test-named-query/age-37-query");
        EventSource source = createEventSource(target, mapCounts);

        NamedCache cache = getNamedCache("dist-test-named-query");
        cache.put(1, new Persona("Ivan Cikic", 33));
        cache.put(1, new Persona("Ivan", 37));
        cache.put(2, new Persona("Aleks", 40));
        cache.put(3, new Persona("Vaso Putica", 37));
        cache.remove(2);
        cache.remove(3);

        Eventually.assertDeferred(() -> mapCounts.size(), is(3));
        Eventually.assertDeferred(() -> mapCounts.get("insert"), is(1));
        Eventually.assertDeferred(() -> mapCounts.get("update"), is(1));
        Eventually.assertDeferred(() -> mapCounts.get("delete"), is(2));

        source.close();
        }

    @Test
    public void testKeySSE()
            throws JSONException, InterruptedException
        {
        final Map<String, Integer> mapCounts = new HashMap<>();

        WebTarget   target = getWebTarget("dist-test-named-query/1");
        EventSource source = createEventSource(target, mapCounts);

        NamedCache cache = getNamedCache("dist-test-named-query");
        cache.put(1, new Persona("Ivan Cikic", 33));
        cache.put(1, new Persona("Ivan Cikic", 37));
        cache.put(2, new Persona("Aleks", 40));
        cache.put(3, new Persona("Vaso Putica", 37));
        cache.remove(1);
        cache.remove(2);
        cache.remove(3);
        cache.put(1, new Persona("Ivan", 37));

        Eventually.assertDeferred(() -> mapCounts.size(), is(3));
        Eventually.assertDeferred(() -> mapCounts.get("insert"), is(1));
        Eventually.assertDeferred(() -> mapCounts.get("update"), is(2));
        Eventually.assertDeferred(() -> mapCounts.get("delete"), is(1));

        source.close();
        }

    protected EventSource createEventSource(WebTarget target, Map<String, Integer> mapCounts)
        {
        int i = 0;
        while (i++ < 3)
            {
            EventSource source = new EventSource(target)
                {
                @Override
                public void onEvent(InboundEvent inboundEvent)
                    {
                    mapCounts.merge(inboundEvent.getName(), 1, (v1, v2) -> v1 + 1);
                    System.out.println("received " + inboundEvent.getName() + " event: "
                            + inboundEvent.readData(SimpleMapEvent.class));
                    }
                };

            try
                {
                Eventually.assertDeferred(() -> source.isOpen(), is(true), within(1, TimeUnit.MINUTES));
                return source;
                }
            catch (AssertionError e)
                {
                System.out.println("createEventSource() got an AssertionError: " + e);
                source.close();
                }
            }

        return null;
        }
    }
