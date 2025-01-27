package com.hmall.gateway.filter;

import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.config.JwtProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;


@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    private final AuthProperties authProperties;
    private final JwtTool jwtTool;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        //1. 获取请求对象
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        // 2. 判断请求路径是否需要拦截
        if(excludePath(request.getPath().toString()))
        {
            // 放行
            return chain.filter(exchange);
        }
        //2. 获取请求头中的token
        String token = null;
        List<String> authorization = headers.get("authorization");
        if(authorization != null && authorization.size() != 0){
            token = authorization.get(0);
        }
        Long userId = null;
        // 3. 校验token
        try {
            userId = jwtTool.parseToken(token);
        } catch (Exception e) {
            // 4. 校验失败，返回401
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        String userInfo = userId.toString();

        ServerWebExchange swe = exchange.mutate()
                .request(builder -> builder.header("user-info", userInfo))
                .build();
        // 放行
        return chain.filter(swe);
    }

    private boolean excludePath(String string) {
        for(String path : authProperties.getExcludePaths()){
            if(antPathMatcher.match(path, string)){
                return true;
            }
        }
        return false;
    }

    // 设置过滤器的优先级 值越小优先级越高
    @Override
    public int getOrder() {
        return 0;
    }
}
