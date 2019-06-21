package com.netflix.eureka.resources;

import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Test;

import java.io.File;

/**
 * @ProjectName: eureka
 * @Package: com.netflix.eureka.resources
 * @ClassName: RestTest1
 * @Author: xiaof
 * @Description: ${description}
 * @Date: 2019/6/21 18:15
 * @Version: 1.0
 */
public class RestTest1 {

    @Test
    public void test1() throws Exception {
        Server server = new Server(8082);

//        WebAppContext webAppCtx = new WebAppContext();
//        webAppCtx.setContextPath("/");
//        webAppCtx.setClassLoader(Thread.currentThread().getContextClassLoader());

        // 设置初始化参数
        ServletHolder servlet = new ServletHolder(ServletContainer.class);
        servlet.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
        servlet.setInitParameter("com.sun.jersey.config.property.packages", "com.netflix.eureka.resources");
        servlet.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true"); // 自动将对象映射成json返回

        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath("/");
        handler.addServlet(servlet, "/*");
        server.setHandler(handler);
        server.start();

        while(true) {
            Thread.sleep(1000);
        }

    }

}
