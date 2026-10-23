package br.com.taskhub.audit;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class HeaderActorProvider implements ActorProvider {

    public static final String HEADER = "X-User-Id";
    static final String ANONYMOUS = "anonymous";
    static final String SYSTEM = "system";
    private static final int MAX_LENGTH = 100;

    @Override
    public String currentActor() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return SYSTEM;
        }
        String header = attributes.getRequest().getHeader(HEADER);
        if (!StringUtils.hasText(header)) {
            return ANONYMOUS;
        }
        String actor = header.trim();
        return actor.length() > MAX_LENGTH ? actor.substring(0, MAX_LENGTH) : actor;
    }
}
