package com.netflix.discovery;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.shared.resolver.DefaultEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.jersey.JerseyEurekaHttpClientFactory;
import com.netflix.discovery.shared.transport.TransportClientFactory;
import com.netflix.discovery.util.InstanceInfoGenerator;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.resources.DefaultServerCodecs;
import com.netflix.eureka.resources.ServerCodecs;
import com.netflix.eureka.transport.JerseyReplicationClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David Liu
 */
public class DiscoveryClientRegisterUpdateTest {

    private TestApplicationInfoManager applicationInfoManager;
    private MockRemoteEurekaServer mockLocalEurekaServer;
    private TestClient client;

    @Before
    public void setUp() throws Exception {
        mockLocalEurekaServer = new MockRemoteEurekaServer();
        mockLocalEurekaServer.start();

        ConfigurationManager.getConfigInstance().setProperty("eureka.name", "EurekaTestApp-" + UUID.randomUUID());
        ConfigurationManager.getConfigInstance().setProperty("eureka.registration.enabled", "true");
        ConfigurationManager.getConfigInstance().setProperty("eureka.appinfo.replicate.interval", 4);
        ConfigurationManager.getConfigInstance().setProperty("eureka.shouldFetchRegistry", "false");
        ConfigurationManager.getConfigInstance().setProperty("eureka.serviceUrl.default",
                "http://localhost:" + mockLocalEurekaServer.getPort() +
                        MockRemoteEurekaServer.EUREKA_API_BASE_PATH);

        InstanceInfo seed = InstanceInfoGenerator.takeOne();
        LeaseInfo leaseSeed = seed.getLeaseInfo();
        LeaseInfo leaseInfo = LeaseInfo.Builder.newBuilder()
                .setDurationInSecs(leaseSeed.getDurationInSecs())
                .setEvictionTimestamp(leaseSeed.getEvictionTimestamp())
                .setRegistrationTimestamp(leaseSeed.getRegistrationTimestamp())
                .setServiceUpTimestamp(leaseSeed.getServiceUpTimestamp())
                .setRenewalTimestamp(leaseSeed.getRenewalTimestamp())
                .setRenewalIntervalInSecs(4) // make this more frequent for testing
                .build();
        InstanceInfo instanceInfo = new InstanceInfo.Builder(seed)
                .setStatus(InstanceInfo.InstanceStatus.STARTING)
                .setLeaseInfo(leaseInfo)
                .build();
        applicationInfoManager = new TestApplicationInfoManager(instanceInfo);
        //mockito.spy 可以直接模拟这个对象出来
        client = Mockito.spy(new TestClient(applicationInfoManager, new DefaultEurekaClientConfig()));

        // force the initial registration to eagerly run
        InstanceInfoReplicator instanceInfoReplicator = ((DiscoveryClient) client).getInstanceInfoReplicator();
        instanceInfoReplicator.run();

        // give some execution time for the initial registration to process
        expectStatus(InstanceInfo.InstanceStatus.STARTING, 4000, TimeUnit.MILLISECONDS);
        mockLocalEurekaServer.registrationStatuses.clear();  // and then clear the validation list
        mockLocalEurekaServer.registerCount.set(0l);
    }

    @After
    public void tearDown() throws Exception {
        client.shutdown();
        mockLocalEurekaServer.stop();
        ConfigurationManager.getConfigInstance().clear();
    }

    @Test
    public void registerUpdateLifecycleTest() throws Exception {
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        // give some execution time
        expectStatus(InstanceInfo.InstanceStatus.UP, 5, TimeUnit.SECONDS);
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UNKNOWN);
        // give some execution time
        expectStatus(InstanceInfo.InstanceStatus.UNKNOWN, 5, TimeUnit.SECONDS);
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
        // give some execution time
        expectStatus(InstanceInfo.InstanceStatus.DOWN, 5, TimeUnit.SECONDS);

        Assert.assertTrue(mockLocalEurekaServer.registerCount.get() >= 3);  // at least 3
    }

    /**
     * This test is similar to the normal lifecycle test, but don't sleep between calls of setInstanceStatus
     */
    @Test
    public void registerUpdateQuickLifecycleTest() throws Exception {
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UNKNOWN);
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
        expectStatus(InstanceInfo.InstanceStatus.DOWN, 5, TimeUnit.SECONDS);
        // this call will be rate limited, but will be transmitted by the automatic update after 10s
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        expectStatus(InstanceInfo.InstanceStatus.UP, 5, TimeUnit.SECONDS);

        Assert.assertTrue(mockLocalEurekaServer.registerCount.get() >= 2);  // at least 2
    }

    @Test
    public void registerUpdateShutdownTest() throws Exception {
        Assert.assertEquals(1, applicationInfoManager.getStatusChangeListeners().size());
        client.shutdown();
        Assert.assertEquals(0, applicationInfoManager.getStatusChangeListeners().size());
        Mockito.verify(client, Mockito.times(1)).unregister();
    }

    @Test
    public void testShutDownHttp() {
        String eurekaServiceUrl = "http://localhost:8081/v2";

        //serverconfig
        EurekaServerConfig eurekaServerConfig = mock(EurekaServerConfig.class);

        // Cluster management related
        when(eurekaServerConfig.getPeerEurekaNodesUpdateIntervalMs()).thenReturn(1000);
        // Replication logic related
        when(eurekaServerConfig.shouldSyncWhenTimestampDiffers()).thenReturn(true);
        when(eurekaServerConfig.getMaxTimeForReplication()).thenReturn(1000);
        when(eurekaServerConfig.getMaxElementsInPeerReplicationPool()).thenReturn(10);
        when(eurekaServerConfig.getMinThreadsForPeerReplication()).thenReturn(1);
        when(eurekaServerConfig.getMaxThreadsForPeerReplication()).thenReturn(1);
        when(eurekaServerConfig.shouldBatchReplication()).thenReturn(true);
        // Peer node connectivity (used by JerseyReplicationClient)
        when(eurekaServerConfig.getPeerNodeTotalConnections()).thenReturn(1);
        when(eurekaServerConfig.getPeerNodeTotalConnectionsPerHost()).thenReturn(1);
        when(eurekaServerConfig.getPeerNodeConnectionIdleTimeoutSeconds()).thenReturn(1000);

        JerseyEurekaHttpClientFactory httpClientFactory = JerseyEurekaHttpClientFactory.newBuilder()
                .withClientName("testEurekaClient")
                .withConnectionTimeout(1000)
                .withReadTimeout(1000)
                .withMaxConnectionsPerHost(1)
                .withMaxTotalConnections(1)
                .withConnectionIdleTimeout(1000)
                .build();

        EurekaHttpClient jerseyEurekaClient = httpClientFactory.newClient(new DefaultEndpoint(eurekaServiceUrl));

        ServerCodecs serverCodecs = new DefaultServerCodecs(eurekaServerConfig);
        JerseyReplicationClient jerseyReplicationClient = JerseyReplicationClient.createReplicationClient(
                eurekaServerConfig,
                serverCodecs,
                eurekaServiceUrl
        );

        client.eurekaTransport.setRegistrationClient(jerseyReplicationClient);
        client.shutdown();

    }

    @Test
    public void testRegistrationDisabled() throws Exception {
        client.shutdown();  // shutdown the default @Before client first

        ConfigurationManager.getConfigInstance().setProperty("eureka.registration.enabled", "false");
        client = new TestClient(applicationInfoManager, new DefaultEurekaClientConfig());
        Assert.assertEquals(0, applicationInfoManager.getStatusChangeListeners().size());
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        Thread.sleep(400);
        client.shutdown();
        Assert.assertEquals(0, applicationInfoManager.getStatusChangeListeners().size());
    }

    @Test
    public void testDoNotUnregisterOnShutdown() throws Exception {
        client.shutdown();  // shutdown the default @Before client first

        ConfigurationManager.getConfigInstance().setProperty("eureka.shouldUnregisterOnShutdown", "false");
        client = Mockito.spy(new TestClient(applicationInfoManager, new DefaultEurekaClientConfig()));
        client.shutdown();
        Mockito.verify(client, Mockito.never()).unregister();
    }

    public class TestApplicationInfoManager extends ApplicationInfoManager {
        TestApplicationInfoManager(InstanceInfo instanceInfo) {
            super(new MyDataCenterInstanceConfig(), instanceInfo, null);
        }

        Map<String, StatusChangeListener> getStatusChangeListeners() {
            return this.listeners;
        }
    }

    private void expectStatus(InstanceInfo.InstanceStatus expected, long timeout, TimeUnit timeUnit) throws InterruptedException {
            String status = mockLocalEurekaServer.registrationStatusesQueue.poll(timeout, timeUnit);
            Assert.assertEquals(expected.name(), status);
    }

    private static <T> T getLast(List<T> list) {
        return list.get(list.size() - 1);
    }

    private static class TestClient extends DiscoveryClient {

        public TestClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig config) {
            super(applicationInfoManager, config);
        }

        @Override
        public void unregister() {
            super.unregister();
        }
    }
}
