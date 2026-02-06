package ai.wealthwise.util;

import ai.wealthwise.model.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Security Utils - Helper methods for security related operations
 */
@Component
public class SecurityUtils {

    /**
     * Get currently authenticated user
     * 
     * @return User object from security context
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                !(authentication.getPrincipal() instanceof User)) {
            return null;
        }
        return (User) authentication.getPrincipal();
    }

    public User getRequiredCurrentUser() {
        User user = getCurrentUser();
        if (user == null) {
            throw new RuntimeException("Current user not found in security context");
        }
        return user;
    }

    /**
     * Get currently authenticated user's ID
     * 
     * @return User ID
     */
    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }
}
