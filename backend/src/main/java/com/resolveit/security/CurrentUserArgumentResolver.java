package com.resolveit.security;

import com.resolveit.domain.user.PortalUser;
import com.resolveit.service.AuthService;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Resolves @CurrentUser parameters from the "Authorization: Bearer <token>" header. */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    private final AuthService auth;

    public CurrentUserArgumentResolver(@org.springframework.context.annotation.Lazy AuthService auth) { this.auth = auth; }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) && PortalUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container, NativeWebRequest request,
                                  WebDataBinderFactory binderFactory) {
        return auth.authenticate(request.getHeader("Authorization"));
    }
}
