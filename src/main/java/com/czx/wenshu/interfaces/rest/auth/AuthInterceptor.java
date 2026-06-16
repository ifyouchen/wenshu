package com.czx.wenshu.interfaces.rest.auth;

import com.czx.wenshu.application.auth.OpaqueAuthTokenService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final OpaqueAuthTokenService authTokenService;

    public AuthInterceptor(OpaqueAuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put("traceId", traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        request.setAttribute("traceId", traceId);

        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = query != null ? path + "?" + query : path;

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("[AuthInterceptor] 缺少 token method={} path={}", method, fullPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            MDC.remove("traceId");
            return false;
        }
        String rawToken = authorization.substring(7).trim();
        try {
            User user = authTokenService.resolveAccessToken(rawToken);
            request.setAttribute("currentUser", user);
            log.info("[AuthInterceptor] token 验证通过 userId={} method={} path={}", user.id(), method, fullPath);
            return true;
        } catch (ApiException exception) {
            log.warn("[AuthInterceptor] token 无效 userId 未知 method={} path={} error={}", method, fullPath, exception.getMessage());
            if (exception.errorCode() == ErrorCode.UNAUTHORIZED) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                MDC.remove("traceId");
                return false;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            MDC.remove("traceId");
            return false;
        } catch (Exception exception) {
            log.error("[AuthInterceptor] token 解析异常 method={} path={}", method, fullPath, exception);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            MDC.remove("traceId");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove("traceId");
    }
}