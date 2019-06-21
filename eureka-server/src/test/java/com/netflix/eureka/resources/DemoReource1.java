package com.netflix.eureka.resources;

import javax.annotation.Generated;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;

/**
 * @ProjectName: eureka
 * @Package: com.netflix.eureka.resources
 * @ClassName: DemoReource1
 * @Author: xiaof
 * @Description: ${description}
 * @Date: 2019/6/21 18:11
 * @Version: 1.0
 */
@Produces({"application/xml", "application/json"})
public class DemoReource1 {

    private String appId;
    private String fatherClassName;

    public DemoReource1(String appId, String fatherClassName) {
        this.appId = appId;
        this.fatherClassName = fatherClassName;
    }

    @GET
    public void testGet() {
        System.out.println(DemoReource1.class.getSimpleName() + "->get请求收到{appId:" + appId + "," + "fatherClassName:" + fatherClassName + "}");
    }
}
