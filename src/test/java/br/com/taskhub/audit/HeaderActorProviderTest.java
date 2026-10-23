package br.com.taskhub.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class HeaderActorProviderTest {

    private final HeaderActorProvider provider = new HeaderActorProvider();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void bindRequest(String header) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (header != null) {
            request.addHeader(HeaderActorProvider.HEADER, header);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void shouldReturnHeaderValueWhenPresent() {
        bindRequest("  ana  ");

        assertThat(provider.currentActor()).isEqualTo("ana");
    }

    @Test
    void shouldReturnAnonymousWhenHeaderIsMissing() {
        bindRequest(null);

        assertThat(provider.currentActor()).isEqualTo("anonymous");
    }

    @Test
    void shouldReturnAnonymousWhenHeaderIsBlank() {
        bindRequest("   ");

        assertThat(provider.currentActor()).isEqualTo("anonymous");
    }

    @Test
    void shouldTruncateActorToColumnLength() {
        bindRequest("a".repeat(150));

        assertThat(provider.currentActor()).hasSize(100);
    }

    @Test
    void shouldReturnSystemOutsideHttpRequest() {
        assertThat(provider.currentActor()).isEqualTo("system");
    }
}
