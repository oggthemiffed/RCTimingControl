package dev.monkeypatch.rctiming.api;

import dev.monkeypatch.rctiming.security.CurrentOfficial;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A controller that asks for the signed-in official when there is none answers 401, not 500. */
class NotSignedInTest {

    @RestController
    static class NeedsAnOfficial {
        @GetMapping("/needs-an-official")
        long official() {
            return CurrentOfficial.id();
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new NeedsAnOfficial())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noOfficial_isUnauthorized() throws Exception {
        mvc.perform(get("/needs-an-official")).andExpect(status().isUnauthorized());
    }
}
