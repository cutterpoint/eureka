package com.netflix.eureka.resources;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.resolver.DefaultEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import com.netflix.discovery.shared.transport.TransportClientFactory;
import com.netflix.discovery.shared.transport.jersey.JerseyEurekaHttpClientFactory;
import com.netflix.discovery.util.InstanceInfoGenerator;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.transport.JerseyReplicationClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @ProjectName: eureka
 * @Package: com.netflix.eureka.resources
 * @ClassName: MyEurekaServer
 * @Author: xiaof
 * @Description: ${description}
 * @Date: 2019/6/17 17:32
 * @Version: 1.0
 */
public class MyEurekaServer {

    private static EurekaServerConfig eurekaServerConfig;

    private static Server server;

    private static TransportClientFactory httpClientFactory;

    private static EurekaHttpClient jerseyEurekaClient;
    private static JerseyReplicationClient jerseyReplicationClient;

    private static String eurekaServiceUrl;

    private static final InstanceInfoGenerator infoGenerator = InstanceInfoGenerator.newBuilder(10, 2).withAsg(false).build();
    private static final Iterator<InstanceInfo> instanceInfoIt = infoGenerator.serviceIterator();


    @Test
    public void test1StartServerByJetty() throws Exception {

        //初始化环境数据配置
        String myServiceUrl = "http://127.0.0.1:8081/v2/";
        System.setProperty("eureka.region", "default");
        System.setProperty("eureka.name", "eureka");
        System.setProperty("eureka.vipAddress", "eureka.mydomain.net");
        System.setProperty("eureka.port", "8081");
        System.setProperty("eureka.preferSameZone", "false");
        System.setProperty("eureka.shouldUseDns", "false");
        System.setProperty("eureka.shouldFetchRegistry", "false");
        System.setProperty("eureka.serviceUrl.defaultZone", myServiceUrl);
        System.setProperty("eureka.serviceUrl.default.defaultZone", myServiceUrl);
        System.setProperty("eureka.awsAccessId", "fake_aws_access_id");
        System.setProperty("eureka.awsSecretKey", "fake_aws_secret_key");
        System.setProperty("eureka.numberRegistrySyncRetries", "0");

        server = new Server(8081);

//        File webappFiledir = new File("./eureka-server/src/main/webapp");
//        File webappWebXmlFiledir = new File("./eureka-server/src/main/webapp/WEB-INF/web.xml");
//        File resourceFiledir = new File("./eureka-server/src/main/resources");

        File webappFiledir = new File("H:\\1-study\\1-spring\\1-springcloud\\1-eureka\\1-source\\eureka\\eureka-server\\src\\main\\webapp");
        File webappWebXmlFiledir = new File("H:\\1-study\\1-spring\\1-springcloud\\1-eureka\\1-source\\eureka\\eureka-server\\src\\main\\webapp\\WEB-INF\\web.xml");
        File resourceFiledir = new File("H:\\1-study\\1-spring\\1-springcloud\\1-eureka\\1-source\\eureka\\eureka-server\\src\\main\\resources");

        if(webappFiledir.exists()) {
            System.out.println(webappFiledir.getAbsolutePath());
            System.out.println(webappWebXmlFiledir.getAbsolutePath());
            System.out.println(resourceFiledir.getAbsolutePath());
        }


        WebAppContext webAppCtx = new WebAppContext(webappFiledir.getAbsolutePath(), "/");
        webAppCtx.setDescriptor(webappWebXmlFiledir.getAbsolutePath());
        webAppCtx.setResourceBase(resourceFiledir.getAbsolutePath());
        webAppCtx.setClassLoader(Thread.currentThread().getContextClassLoader());
        server.setHandler(webAppCtx);
        server.start();


        while(true) {
            Thread.sleep(1000);
        }

    }

    @Test
    public void test2Re() {

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

        for(int i = 0; i < 2; ++i) {
            InstanceInfo instanceInfo = instanceInfoIt.next();
            EurekaHttpResponse<Void> httpResponse = jerseyEurekaClient.register(instanceInfo);

            assertThat(httpResponse.getStatusCode(), is(equalTo(204)));
        }
    }

}
