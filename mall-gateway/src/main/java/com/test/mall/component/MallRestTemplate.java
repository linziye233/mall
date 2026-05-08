package com.test.mall.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.client.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;

/**
 * 自定义 RestTemplate 增强类 - 实现服务发现与负载均衡
 * 
 * 【设计目的】
 * 1. 解决网关启动早期（InitializingBean阶段）无法使用 @LoadBalanced RestTemplate 的问题
 * 2. 在 AuthorizationFilter 初始化公钥时，需要通过服务名调用认证中心获取 JWT 公钥
 * 3. 标准的 @LoadBalanced RestTemplate 在 BeanPostProcessor 处理前未完成增强，无法使用
 * 
 * 【核心功能】
 * - 服务名称解析：从 URL 中提取微服务名称（如 http://tulingmall-authcenter/oauth/token_key）
 * - 服务发现：通过 Nacos DiscoveryClient 获取服务实例列表
 * - 负载均衡：采用随机算法选择一个服务实例
 * - URL 替换：将服务名替换为实际的 IP:Port 地址
 * 
 * 【使用场景】
 * - 网关启动时从认证中心获取 JWT 验签公钥
 * - 需要在 Bean 初始化阶段进行远程调用的场景
 * 
 * @author smlz
 * @date 2019/11/19
 */
@Slf4j
public class MallRestTemplate extends org.springframework.web.client.RestTemplate {

    /**
     * Spring Cloud 服务发现客户端
     * 用于从 Nacos 注册中心获取服务实例信息
     */
    private DiscoveryClient discoveryClient;

    /**
     * 构造函数 - 注入 DiscoveryClient
     * 
     * @param discoveryClient Nacos 服务发现客户端，用于查询服务实例列表
     */
    public MallRestTemplate(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    /**
     * 重写 RestTemplate 的核心执行方法 - 拦截所有 HTTP 请求
     * 
     * 【执行流程】
     * 1. 接收原始 URL（包含服务名，如 http://tulingmall-authcenter/xxx）
     * 2. 调用 replaceUrl() 将服务名替换为实际 IP:Port
     * 3. 创建 HTTP 请求并执行
     * 4. 处理响应并返回结果
     * 
     * 【为什么重写这个方法】
     * - doExecute 是 RestTemplate 所有 HTTP 请求（get/post/put/delete）的底层入口
     * - 在此处拦截可以统一处理服务发现和负载均衡逻辑
     * - 对上层调用者透明，使用方式与标准 RestTemplate 完全一致
     * 
     * @param url              请求URL（可能包含服务名）
     * @param method           HTTP 方法（GET/POST/PUT/DELETE等）
     * @param requestCallback  请求回调（用于设置请求头等）
     * @param responseExtractor 响应提取器（用于解析响应数据）
     * @return 响应数据
     * @throws RestClientException 客户端异常
     */
    @Override
    protected <T> T doExecute(URI url, @Nullable HttpMethod method, 
                              @Nullable RequestCallback requestCallback,
                              @Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {

        Assert.notNull(url, "URI is required");
        Assert.notNull(method, "HttpMethod is required");
        
        ClientHttpResponse response = null;
        try {
            // 【关键步骤】在发起请求前，将服务名替换为实际的 IP:Port
            // 例如：http://tulingmall-authcenter/oauth/token_key 
            //      → http://192.168.1.100:9999/oauth/token_key
            log.info("【MallRestTemplate】原始请求URL: {}", url);
            url = replaceUrl(url);
            log.info("【MallRestTemplate】服务发现后URL: {}", url);
            
            // 创建并执行 HTTP 请求（使用父类的标准逻辑）
            ClientHttpRequest request = createRequest(url, method);
            if (requestCallback != null) {
                requestCallback.doWithRequest(request);
            }
            response = request.execute();
            handleResponse(url, method, response);
            return (responseExtractor != null ? responseExtractor.extractData(response) : null);
        }
        catch (IOException ex) {
            String resource = url.toString();
            String query = url.getRawQuery();
            resource = (query != null ? resource.substring(0, resource.indexOf('?')) : resource);
            throw new ResourceAccessException("I/O error on " + method.name() +
                    " request for \"" + resource + "\": " + ex.getMessage(), ex);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


    /**
     * 服务名称解析与替换 - 核心负载均衡逻辑
     * 
     * 【处理流程】
     * 1. 从 URL 中提取服务名称
     *    示例：http://tulingmall-authcenter/oauth/token_key 
     *          → 提取出 "tulingmall-authcenter"
     * 
     * 2. 通过 DiscoveryClient 查询 Nacos 获取该服务的所有实例列表
     *    返回：[ServiceInstance{host=192.168.1.100, port=9999}, 
     *          ServiceInstance{host=192.168.1.101, port=9999}]
     * 
     * 3. 使用随机算法选择一个实例（简单负载均衡策略）
     *    Random.nextInt(size) 生成 [0, size) 的随机数
     * 
     * 4. 将 URL 中的服务名替换为选中实例的实际地址
     *    示例：http://tulingmall-authcenter/xxx 
     *          → http://192.168.1.100:9999/xxx
     * 
     * 【为什么不使用 Ribbon】
     * - Ribbon 的 @LoadBalanced 依赖于 BeanPostProcessor 在容器启动后期进行增强
     * - 而网关需要在 InitializingBean 阶段（更早）调用认证中心获取公钥
     * - 此时 Ribbon 尚未完成初始化，无法使用
     * 
     * @param url 原始URL（包含服务名）
     * @return 替换后的URL（包含实际IP:Port）
     */
    private URI replaceUrl(URI url){
        // ==================== 第一步：解析服务名称 ====================
        String sourceUrl = url.toString();
        String[] httpUrl = sourceUrl.split("//");
        
        // 提取服务名：找到第一个 "/" 或 "@" 之前的部分
        // 示例：httpUrl[1] = "tulingmall-authcenter/oauth/token_key"
        //      替换第一个 "/" 为 "@" → "tulingmall-authcenter@oauth/token_key"
        //      查找 "@" 位置 → 索引为 "tulingmall-authcenter" 的长度
        int index = httpUrl[1].replaceFirst("/", "@").indexOf("@");
        String serviceName = httpUrl[1].substring(0, index);
        
        log.info("【服务发现】解析出的服务名称: {}", serviceName);

        // ==================== 第二步：从 Nacos 获取服务实例列表 ====================
        List<ServiceInstance> serviceInstanceList = discoveryClient.getInstances(serviceName);
        
        // 容错处理：如果没有可用实例，抛出异常
        if(serviceInstanceList.isEmpty()) {
            throw new RuntimeException("没有可用的微服务实例列表: " + serviceName);
        }
        
        log.info("【服务发现】服务 [{}] 可用实例数量: {}", serviceName, serviceInstanceList.size());

        // ==================== 第三步：随机选择一个实例（负载均衡）====================
        Random random = new Random();
        Integer randomIndex = random.nextInt(serviceInstanceList.size());
        log.info("【负载均衡】随机选择的实例下标: {}", randomIndex);
        
        ServiceInstance selectedInstance = serviceInstanceList.get(randomIndex);
        String serviceIp = selectedInstance.getUri().toString();
        log.info("【负载均衡】选中的服务实例: {} (ID: {})", serviceIp, selectedInstance.getInstanceId());

        // ==================== 第四步：替换 URL 中的服务名为实际地址 ====================
        String targetSource = httpUrl[1].replace(serviceName, serviceIp);
        
        try {
            return new URI(targetSource);
        } catch (URISyntaxException e) {
            log.error("【URL转换】URI语法错误: {}", targetSource, e);
            e.printStackTrace();
        }
        
        // 如果转换失败，返回原始URL（降级处理）
        return url;
    }

}
