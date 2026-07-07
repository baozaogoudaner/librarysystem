package com.library.gateway.filter;

import com.library.common.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 全局鉴权过滤器
 * 校验 JWT Token，将 userId 和 username 放入请求头传递给下游服务
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 白名单路径（不需要登录） */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/user/login",
            "/api/user/register",
            "/api/user/captcha",
            "/api/book/search",
            "/api/book/list",
            "/api/book/hot",
            "/api/book/category-stats",
            "/api/seat/room/list",
            "/doc.html",
            "/swagger-ui",
            "/v2/api-docs",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单放行
        for (String whitePath : WHITE_LIST) {
            if (path.startsWith(whitePath)) {
                return chain.filter(exchange);
            }
        }

        // 获取 Token
        String token = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (token == null || token.isEmpty()) {
            return unauthorizedResponse(exchange, "缺少Token，请先登录");
        }

        // 去掉 Bearer 前缀
        if (token.startsWith(JwtUtils.TOKEN_PREFIX)) {
            token = token.substring(JwtUtils.TOKEN_PREFIX.length());
        }

        // 验证 Token
        if (!JwtUtils.validateToken(token)) {
            return unauthorizedResponse(exchange, "Token无效或已过期，请重新登录");
        }

        // 解析用户信息并放入请求头
        Long userId = JwtUtils.getUserId(token);
        String username = JwtUtils.getUsername(token);
        Integer role = JwtUtils.getRole(token);

        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-Username", username)
                .header("X-Role", String.valueOf(role))
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * 返回 401 响应
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
