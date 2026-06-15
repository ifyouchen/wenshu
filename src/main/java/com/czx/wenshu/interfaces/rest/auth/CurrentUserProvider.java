package com.czx.wenshu.interfaces.rest.auth;

import com.czx.wenshu.domain.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class CurrentUserProvider {

    public User getCurrentUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("No current request context");
        }
        HttpServletRequest request = attributes.getRequest();
        User user = (User) request.getAttribute("currentUser");
        if (user == null) {
            throw new IllegalStateException("No authenticated user in current request");
        }
        if (user.isDeleted()) {
            throw new IllegalStateException("Account has been deactivated");
        }
        return user;
    }
}