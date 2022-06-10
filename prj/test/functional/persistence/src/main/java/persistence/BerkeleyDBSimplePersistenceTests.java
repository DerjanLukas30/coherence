/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.persistence.PersistenceManager;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;

import com.tangosol.persistence.bdb.BerkeleyDBManager;

import org.junit.Test;

import javax.management.MBeanException;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

/**
 * Functional tests for simple cache persistence and recovery using the
 * BerkeleyDBPersistenceManager.
 *
 * @author jh  2012.10.18
 */
public class BerkeleyDBSimplePersistenceTests
        extends AbstractSimplePersistenceTests
    {

    // ----- AbstractSimplePersistenceTests methods -------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected PersistenceManager<ReadBuffer> createPersistenceManager(File file)
            throws IOException
        {
        return new BerkeleyDBManager(file, null, null);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPersistenceManagerName()
        {
        return "BDB";
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheConfigPath()
        {
        return "simple-persistence-bdb-cache-config.xml";
        }

    /**
     * Test multiple restarts with ensure cache for active persistence.
     */
    @Test
    public void testRestartsWithEnsureCacheForActiveMode()
            throws IOException, MBeanException
        {
        testMultipleRestartsWithClientEnsureCache("active");
        }

    /**
     * Test multiple restarts with ensure cache for async-active persistence.
     */
    @Test
    public void testRestartsWithEnsureCacheForAsyncActiveMode()
            throws IOException, MBeanException
        {
        testMultipleRestartsWithClientEnsureCache("active-async");
        }

    /**
     * Test multiple restarts with ensure cache for active-backup persistence.
     */
    @Test
    public void testRestartsWithEnsureCacheForActiveBackupMode()
            throws IOException, MBeanException
        {
        testMultipleRestartsWithClientEnsureCache("active-backup");
        }

    /**
     * Test 2 server storage, 1 server restart with backup persistence.
     */
    @Test
    public void testBackupPersistence2()
            throws IOException, MBeanException
        {
        testBackupPersistence("active-backup", 2, "simple-persistent");
        }

    /**
     * Test 3 server storage, 1 server restart with backup persistence.
     */
    @Test
    public void testBackupPersistence3()
            throws IOException, MBeanException
        {
        testBackupPersistence3("active-backup");
        }

    /**
     * Test 4 server storage, 1 server restart with backup persistence and
     * backup count at 2.
     */
    @Test
    public void testBackupPersistence4()
            throws IOException, MBeanException
        {
        testBackupPersistence("active-backup", 4, "simple-persistent");
        }

    /**
     * Test multiple restarts with ensure cache before storage nodes start.
     */
    public void testMultipleRestartsWithClientEnsureCache(String sMode)
            throws IOException, MBeanException
        {
        File fileSnapshot = FileHelper.createTempDir();
        File fileActive   = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();

        Properties props = new Properties();
        props.setProperty("test.persistence.mode", sMode);
        props.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.threads", "5");
        props.setProperty("test.persistence.members", "3");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        final String            sServer          = "testMultipleRestartsWithClientEnsureCache";
        final String            sPersistentCache = "simple-persistent";
        NamedCache              cache            = getNamedCache(sPersistentCache);
        DistributedCacheService service          = (DistributedCacheService) cache.getCacheService();
        Cluster                 cluster          = CacheFactory.ensureCluster();

        String sServer1;
        String sServer2;
        String sServer3;

        int i             = 0;
        int nRestartCount = 3;
        try
            {
            while (++i <= nRestartCount)
                {
                System.out.println("**** Iteration: " + i + " of " + nRestartCount);
                sServer1 = sServer + "-" + (i*3 - 1);
                sServer2 = sServer + "-" + (i*3 - 2);
                sServer3 = sServer + "-" + (i*3);
                startCacheServer(sServer1, getProjectName(), getCacheConfigPath(), props);
                startCacheServer(sServer2, getProjectName(), getCacheConfigPath(), props);
                startCacheServer(sServer3, getProjectName(), getCacheConfigPath(), props);

                Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(3));
                waitForBalanced(service);

                // populate with some data if first time only
                if (i == 1)
                    {
                    PersistenceTestHelper.populateData(cache, 5000);
                    }
                else
                    {
                    service = (DistributedCacheService) cache.getCacheService();
                    }

                // always assert the size to ensure we have not lost data
                assertEquals(cache.size(), 5000);

                String  sService = service.getInfo().getServiceName();

                cluster.suspendService(sService);

                try
                    {
                    // abruptly shutdown
                    stopCacheServer(sServer1);
                    stopCacheServer(sServer2);
                    stopCacheServer(sServer3);

                    Eventually.assertThat(cluster.getMemberSet().size(), is(1));
                    }
                finally
                    {
                    cluster.resumeService(sService);
                    }

                service.shutdown();

                try
                    {
                    cache = getNamedCache(sPersistentCache);
                    cache.size();
                    }
                catch (Throwable t)
                    {
                    CacheFactory.log("got Exception: " + t);
                    }
                }
            }
        finally
            {
            CacheFactory.shutdown();

            FileHelper.deleteDirSilent(fileActive);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    /**
     * Test 3 server restart with backup persistence.
     */
    public void testBackupPersistence3(String sMode)
            throws IOException, MBeanException
        {
        File fileSnapshot = FileHelper.createTempDir();
        File fileActive1 = FileHelper.createTempDir();
        File fileActive2 = FileHelper.createTempDir();
        File fileBackup1 = FileHelper.createTempDir();
        File fileBackup2 = FileHelper.createTempDir();
        File fileTrash = FileHelper.createTempDir();

        Properties props1 = new Properties();
        props1.setProperty("test.persistence.mode", sMode);
        props1.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props1.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props1.setProperty("test.threads", "5");
        props1.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        Properties props2 = new Properties();
        props2.setProperty("test.persistence.mode", sMode);
        props2.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props2.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props2.setProperty("test.threads", "5");
        props2.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        // use 2 different locations
        props1.setProperty("test.persistence.active.dir", fileActive1.getAbsolutePath());
        props1.setProperty("test.persistence.backup.dir", fileBackup1.getAbsolutePath());
        props2.setProperty("test.persistence.active.dir", fileActive2.getAbsolutePath());
        props2.setProperty("test.persistence.backup.dir", fileBackup2.getAbsolutePath());

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        final String sServer = "testBackupPersistence3";
        final String sPersistentCache = "simple-persistent";
        NamedCache cache = getNamedCache(sPersistentCache);
        DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
        Cluster cluster = CacheFactory.ensureCluster();

        try
            {
            System.out.println("**** Backup Persistence Start");
            String sServer1 = sServer + "-1";
            String sServer2 = sServer + "-2";
            String sServer3 = sServer + "-3";
            String sServer4 = sServer + "-4";

            // servers 1 and 2 use the same location
            startCacheServer(sServer1, getProjectName(), getCacheConfigPath(), props1);
            startCacheServer(sServer2, getProjectName(), getCacheConfigPath(), props1);
            startCacheServer(sServer3, getProjectName(), getCacheConfigPath(), props2);

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(3));
            waitForBalanced(service);

            // populate with some data
            PersistenceTestHelper.populateData(cache, 5000);

            // always assert the size to ensure we have not lost data
            assertEquals(cache.size(), 5000);

            // backups persistence is async., need some time to complete
            Base.sleep(10_000);

            String sService = service.getInfo().getServiceName();

            // debugging - print the final ownership
            SafeService      serviceSafe = (SafeService) cache.getCacheService();
            PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();
            try
                {
                String sOwnership = serviceReal.reportOwnership(Boolean.TRUE);
                CacheFactory.log(sOwnership, LOG_INFO);
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e);
                }

            cluster.suspendService(sService);

            try
                {
                // abruptly shutdown
                stopCacheServer(sServer1);
                stopCacheServer(sServer2);
                stopCacheServer(sServer3);

                Eventually.assertThat(cluster.getMemberSet().size(), is(1));
                }
            finally
                {
                cluster.resumeService(sService);
                }

            service.shutdown();

            try
                {
                cache = getNamedCache(sPersistentCache);
                cache.size();
                }
            catch (Throwable t)
                {
                CacheFactory.log("got Exception: " + t);
                }

            // start fourth server, recovering from stores location 1
            startCacheServer(sServer4, getProjectName(), getCacheConfigPath(), props1);

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(1));
            waitForBalanced(service);

            Eventually.assertDeferred(cache::size, is(5000));

            stopCacheServer(sServer3);
            }
        finally
            {
            getFactory().destroyCache(cache);

            stopAllApplications();

            CacheFactory.shutdown();

            FileHelper.deleteDirSilent(fileActive1);
            FileHelper.deleteDirSilent(fileActive2);
            FileHelper.deleteDirSilent(fileBackup1);
            FileHelper.deleteDirSilent(fileBackup2);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    /**
     * Test N server restart with backup persistence.
     */
    public void testBackupPersistence(String sMode, int nServers, String sCacheName)
            throws IOException, MBeanException
        {
        File fileSnapshot = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();

        File[]       fileActive = new File[nServers];
        File[]       fileBackup = new File[nServers];
        Properties[] props      = new Properties[nServers];

        for (int i = 0; i < nServers; i++)
            {
            fileActive[i] = FileHelper.createTempDir();
            fileBackup[i] = FileHelper.createTempDir();

            props[i] = new Properties();
            props[i].setProperty("test.persistence.mode", sMode);
            props[i].setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
            props[i].setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
            props[i].setProperty("test.threads", "5");
            if (nServers == 4)
                {
                props[i].setProperty("test.backupcount", "2");
                }
            props[i].setProperty("coherence.override", "common-tangosol-coherence-override.xml");

            props[i].setProperty("test.persistence.active.dir", fileActive[i].getAbsolutePath());
            props[i].setProperty("test.persistence.backup.dir", fileBackup[i].getAbsolutePath());
            }

        if (nServers == 4)
            {
            // 0 and 2 point to the same location
            props[2].setProperty("test.persistence.active.dir", props[0].getProperty("test.persistence.active.dir"));
            props[2].setProperty("test.persistence.backup.dir", fileBackup[0].getAbsolutePath());

            System.setProperty("coherence.distributed.backupcount", "2");

            // reset so service senior picks up BC count
            AbstractFunctionalTest.stopAllApplications();

            AbstractFunctionalTest._startup();
            }

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        final String sServer = "testBackupPersistence" + nServers;
        NamedCache cache = getNamedCache(sCacheName);
        DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
        Cluster cluster = CacheFactory.ensureCluster();

        try
            {
            System.out.println("**** Backup Persistence Start");
            String sServer1 = sServer + "-1";
            String sServer2 = sServer + "-2";
            String sServer3 = sServer + "-3";
            String sServer4 = sServer + "-4";

            String sServer5 = sServer + "-5";

            startCacheServer(sServer1, getProjectName(), getCacheConfigPath(), props[0]);
            startCacheServer(sServer2, getProjectName(), getCacheConfigPath(), props[1]);
            if (nServers == 4)
                {
                startCacheServer(sServer3, getProjectName(), getCacheConfigPath(), props[2]);
                startCacheServer(sServer4, getProjectName(), getCacheConfigPath(), props[3]);
                }

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(nServers));
            waitForBalanced(service);

            // populate with some data
            PersistenceTestHelper.populateData(cache, 5000);

            // always assert the size to ensure we have not lost data
            assertEquals(cache.size(), 5000);

            // populate with some data, on non-default cache to trigger extents init
            PersistenceTestHelper.populateData(getNamedCache("simple-persistent-new"), 5000);

            // backups persistence is async., need some time to complete
            Base.sleep(10_000);

            String sService = service.getInfo().getServiceName();

            // debugging - print the final ownership
            SafeService      serviceSafe = (SafeService) cache.getCacheService();
            PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();
            try
                {
                String sOwnership = serviceReal.reportOwnership(Boolean.TRUE);
                CacheFactory.log(sOwnership, LOG_INFO);
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e);
                }

            cluster.suspendService(sService);

            try
                {
                // abruptly shutdown
                stopCacheServer(sServer1);
                stopCacheServer(sServer2);
                if (nServers == 4)
                    {
                    stopCacheServer(sServer3);
                    stopCacheServer(sServer4);
                    }

                Eventually.assertThat(cluster.getMemberSet().size(), is(1));
                }
            finally
                {
                cluster.resumeService(sService);
                }

            service.shutdown();

            try
                {
                cache = getNamedCache(sCacheName);
                cache.size();
                }
            catch (Throwable t)
                {
                CacheFactory.log("got Exception: " + t);
                }

            // re-start recovering from half the original stores
            startCacheServer(sServer5, getProjectName(), getCacheConfigPath(), props[0]);

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(1));
            waitForBalanced(service);

            assertEquals(5000, cache.size());

            stopCacheServer(sServer5);
            }
        finally
            {
            System.clearProperty("coherence.distributed.backupcount");

            getFactory().destroyCache(cache);

            stopAllApplications();

            CacheFactory.shutdown();

            for (int i = 0; i < nServers; i++)
                {
                FileHelper.deleteDirSilent(fileActive[i]);
                FileHelper.deleteDirSilent(fileBackup[i]);
                }

            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }
    }