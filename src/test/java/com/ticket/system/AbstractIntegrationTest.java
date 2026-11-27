package com.ticket.system;

import com.jayway.jsonpath.JsonPath;
import com.ticket.system.security.JwtAuthFilter;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mvc;

    protected static final String TICKET_BODY =
            """
            {"title":"Printer broken","description":"Floor 2","priority":"HIGH"}""";

    // ADJUST the register body to your RegisterRequest (name, role, ...)
    protected Cookie registerAndLogin(String email) throws Exception {
        String body = """
                {"email":"%s","password":"Passw0rd!123"}""".formatted(email);
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        MvcResult login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        return login.getResponse().getCookie(JwtAuthFilter.COOKIE_NAME);   // import JwtAuthFilter via the IDE
    }

    protected ResultActions createTicket(Cookie cookie, String idempotencyKey) throws Exception {
        var request = post("/tickets").cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON).content(TICKET_BODY);
        if (idempotencyKey != null) request = request.header("Idempotency-Key", idempotencyKey);
        return mvc.perform(request);
    }

    protected long idOf(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
