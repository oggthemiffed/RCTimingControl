package dev.monkeypatch.rctiming.domain.audit;

import dev.monkeypatch.rctiming.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the audit log complete (#138): every endpoint that changes data is either marked {@link Audited}
 * (its service records who did it) or listed, with a reason, in {@code audit-allowlist.txt}. A new endpoint
 * that is neither fails here, so it cannot ship unrecorded by accident. A stale list entry fails too, so
 * the list only shrinks as the gaps in #139 and #140 are closed.
 */
class AuditCoverageIT extends AbstractIntegrationTest {

    private static final Set<RequestMethod> READS = Set.of(RequestMethod.GET, RequestMethod.HEAD, RequestMethod.OPTIONS);
    private static final Set<RequestMethod> WRITES =
            Set.of(RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE);

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping handlerMapping;

    @Test
    void everyEndpointThatChangesDataIsAuditedOrListedWithAReason() throws IOException {
        Set<String> audited = new TreeSet<>();
        Set<String> unaudited = new TreeSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> mapping : handlerMapping.getHandlerMethods().entrySet()) {
            if (mapping.getValue().getBeanType().getName().startsWith("org.springframework.")
                    || mapping.getKey().getPathPatternsCondition() == null) {
                continue;   // the framework's own endpoints, such as /error
            }
            boolean isAudited = AnnotatedElementUtils.hasAnnotation(mapping.getValue().getMethod(), Audited.class);
            // A mapping with no method named accepts every method, so it counts as one that can change data
            Set<RequestMethod> declared = mapping.getKey().getMethodsCondition().getMethods();
            Set<RequestMethod> methods = declared.isEmpty() ? WRITES : declared;
            for (RequestMethod method : methods) {
                if (READS.contains(method)) {
                    continue;
                }
                for (String pattern : mapping.getKey().getPathPatternsCondition().getPatternValues()) {
                    (isAudited ? audited : unaudited).add(method + " " + pattern);
                }
            }
        }
        Set<String> allowed = allowlist();

        Set<String> missing = new TreeSet<>(unaudited);
        missing.removeAll(allowed);
        Set<String> stale = new TreeSet<>(allowed);
        stale.removeAll(unaudited);

        assertThat(missing)
                .as("These endpoints change data but are not marked @Audited and are not in audit-allowlist.txt."
                        + " Record who did it (AuditService) and mark the method @Audited, or list it with a reason.")
                .isEmpty();
        assertThat(stale)
                .as("These audit-allowlist.txt entries are stale: the endpoint no longer exists or is now @Audited."
                        + " Remove them.")
                .isEmpty();
        assertThat(audited).as("the endpoints marked @Audited").isNotEmpty();
    }

    /** The allowlist's keys ({@code METHOD /pattern}), without the reasons; comments and blank lines skipped. */
    private static Set<String> allowlist() throws IOException {
        String text = new ClassPathResource("audit-allowlist.txt").getContentAsString(StandardCharsets.UTF_8);
        Set<String> keys = new TreeSet<>();
        for (String line : text.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int bar = trimmed.indexOf(" | ");
            assertThat(bar).as("allowlist line needs a reason after ' | ': " + trimmed).isPositive();
            assertThat(keys.add(trimmed.substring(0, bar).strip())).as("duplicate allowlist entry: " + trimmed).isTrue();
        }
        return keys;
    }
}
