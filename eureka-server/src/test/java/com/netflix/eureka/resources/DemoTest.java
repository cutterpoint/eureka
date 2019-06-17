package com.netflix.eureka.resources;

import com.netflix.discovery.shared.resolver.DefaultEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.TransportClientFactory;
import com.netflix.discovery.shared.transport.jersey.JerseyEurekaHttpClientFactory;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.transport.JerseyReplicationClient;
import org.junit.Before;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @ProjectName: eureka
 * @Package: com.netflix.eureka.resources
 * @ClassName: DemoTest
 * @Author: xiaof
 * @Description: ${description}
 * @Date: 2019/6/17 17:52
 * @Version: 1.0
 */
public class DemoTest {

    private static TransportClientFactory httpClientFactory;
    private static EurekaServerConfig eurekaServerConfig;
    private static EurekaHttpClient jerseyEurekaClient;
    private static JerseyReplicationClient jerseyReplicationClient;
    private static String eurekaServiceUrl;

    @Before
    public void before() {
        eurekaServiceUrl = "http://localhost:8081/v2";

        //serverconfig
        eurekaServerConfig = mock(EurekaServerConfig.class);

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

        httpClientFactory = JerseyEurekaHttpClientFactory.newBuilder()
                .withClientName("testEurekaClient")
                .withConnectionTimeout(1000)
                .withReadTimeout(1000)
                .withMaxConnectionsPerHost(1)
                .withMaxTotalConnections(1)
                .withConnectionIdleTimeout(1000)
                .build();

        jerseyEurekaClient = httpClientFactory.newClient(new DefaultEndpoint(eurekaServiceUrl));

        ServerCodecs serverCodecs = new DefaultServerCodecs(eurekaServerConfig);
        jerseyReplicationClient = JerseyReplicationClient.createReplicationClient(
                eurekaServerConfig,
                serverCodecs,
                eurekaServiceUrl
        );
    }

}
