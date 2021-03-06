package com.oeasycloud.myeurekaclient.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.oeasycloud.myeurekaclient.config.ThreadPoolRunner;
import com.oeasycloud.myeurekaclient.service.AsyncTask;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
public class ClientController {
    @Autowired
    private ThreadPoolRunner threadPoolRunner;

    @Value("${server.port}")
    String port;
    @RequestMapping("/hi")
    public String home(@RequestParam String name) {
        ThreadPoolExecutor threadPoolExecutor = threadPoolRunner.getThreadPoolExecutor();
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("");
            }
        });

        return "hello "+name+",i am from port:" +port;
    }

    /**
     * 当前版本
     */
    @Value("${app.version}")
    private String version;
    /**
     * 打包时间
     */
    @Value("${app.build.time}")
    private String buildTime;

    @GetMapping("/getVersion")
    public String getVersion() {
        Map<String,String> ret = new HashMap<>();
        ret.put("version",version);
        ret.put("buildTime",buildTime);

        return JSON.toJSONString(ret);
    }

    /**
     * @EnableAsync注释启动了Spring在后台线程池中运行@Async方法的能力
     * 该类还自定义使用的Executor 参考MyEurekaClientApplication中的asyncExecutor()
     * 默认情况下，使用SimpleAsyncTaskExecutor
     * 注意：
     *      文档里写了@Async有两个使用的限制：
     *      它必须仅适用于public方法
     *      在同一个类中调用异步方法将无法正常工作（self-invocation）
     *      被@Async注解的方法与调用方法在同一类中如testAsync1()(其实这里testAsync1()与asyncTask中的doTaskThree()内容一致)，
     *        则注解失效。因同一类中调用，不会生成代理类，导致spring容器中会无托管对象。
     *
     * 在EnableAsync注解中有@Import AsyncConfigurationSelector.class（用来导入一个或多个class，这些类会注入到spring容器中，
     *   或者配置类，配置类里面定义的bean都会被spring容器托管）。
     *
     * 在@Async注解的方法中可设置需要配置的线程池
     */
    @Autowired
    private AsyncTask asyncTask;

    @GetMapping("/testAsync")
    public String testAsync() {
        System.out.println("测试 @Async");
        System.out.println("testAsync---" + Thread.currentThread().getName());
        testAsync1();
        long start = System.currentTimeMillis();

        try {
            Future<String> task1 = asyncTask.doTaskOne();
            Future<String> task2 = asyncTask.doTaskTwo();
            asyncTask.doTaskThree();
            while (true) {
                if (task1.isDone() && task2.isDone()) {
                    break;
                }
                Thread.sleep(100);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("完成任务所有任务  耗时:" + (System.currentTimeMillis() - start) + "ms");

        return "testAsync";
    }

    @Async
    public void testAsync1() {
        System.out.println("testAsync1---" + Thread.currentThread().getName());
        try {
            Thread.sleep(2000);
        } catch (Exception e){
            System.out.println(e);
        }
    }

    /**
     * 使用request获取body中的数据
     */
    /**
     * request payload请求方式 (传递的为json数据)
     *  postman中使用body-raw(text)方式，数据只有value(eg:{"abc","123"},注意:无换行符)
     * @param request
     */
    @PostMapping("/callback")
    public void getBodyByRequest(HttpServletRequest request) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
            String str = reader.readLine();
            System.out.println(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * formData请求方式
     *  postman中在params中传入key-value
     * @param request
     */
    @PostMapping("/callback2")
    public void getBodyByRequest2(HttpServletRequest request) {
        try {
            String paraName = null;
            Map<String, Object> parameters = new HashMap<>();
            //获取请求参数并转换
            //
            Enumeration<String> enu = request.getParameterNames();
            while (enu.hasMoreElements()) {
                paraName = enu.nextElement();
                parameters.put(paraName, request.getParameter(paraName));
            }
            System.out.println(JSONObject.toJSONString(parameters));
            //return parameters;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
