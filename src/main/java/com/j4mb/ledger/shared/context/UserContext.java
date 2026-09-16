package com.j4mb.ledger.shared.context;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserContext {

    private String userId;

    public void initialize(String userId) {
        if (userId != null && !userId.isBlank()) {
            this.userId = userId.trim();
        }
    }

    public String getUserId() {
        return userId != null ? userId : "system";
    }

    public boolean isInitialized() {
        return userId != null;
    }
}
