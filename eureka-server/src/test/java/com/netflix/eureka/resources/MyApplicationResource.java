package com.netflix.eureka.resources;

import com.netflix.eureka.Version;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

/**
 * @ProjectName: eureka
 * @Package: com.netflix.eureka.resources
 * @ClassName: MyApplicationResource
 * @Author: xiaof
 * @Description: ${description}
 * @Date: 2019/6/21 18:03
 * @Version: 1.0
 */
@Path("/{version}/test")
@Produces({"application/xml", "application/json"})
public class MyApplicationResource extends Application {


    @Path("{appId}")
    public DemoReource1 getApplicationResource(
            @PathParam("version") String version,
            @PathParam("appId") String appId) {
        return new DemoReource1(appId, MyApplicationResource.class.getName());
    }

    @POST
    @Path("/test1")
    public void test1() {
        System.out.println("post资源" + MyApplicationResource.class.getName());
    }

}
