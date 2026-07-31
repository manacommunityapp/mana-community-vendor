package com.manacommunity.api.user.service;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class LoggedInUserService {

    public AppUser getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getAppUser();
        }
        return null;
    }

    public AppUser resolve(UserPrincipal principal) {
        if (principal != null) {
            return principal.getAppUser();
        }
        return getLoggedInUser();
    }

    public Long getLoggedInUserCommunityId() {
        AppUser user = getLoggedInUser();
        if (user != null && user.getCommunity() != null) {
            return user.getCommunity().getId();
        }
        return null;
    }
}
