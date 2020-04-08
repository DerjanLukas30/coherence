/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package rest;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

/**
 * A collection of functional tests for Coherence*Extend-REST that use
 * Netty as the embedded HttpServer.
 *
 * @author lh 2015.12.16
 */
public class NettyRestTests
        extends AbstractServerSentEventsTests
    {
    public NettyRestTests()
        {
        super(FILE_SERVER_CFG_CACHE);
        }

    // -- --- test lifecycle ------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember clusterMember = startCacheServer("NettyRestTests", "rest", FILE_SERVER_CFG_CACHE);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("ExtendHttpProxyService"), is(true));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("NettyRestTests");
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_SERVER_CFG_CACHE = "server-cache-config-netty.xml";
    }
