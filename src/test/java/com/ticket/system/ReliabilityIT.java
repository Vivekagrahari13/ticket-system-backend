package com.ticket.system;

import com.ticket.system.ticket.entity.Ticket;
import com.ticket.system.ticket.repository.TicketRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReliabilityIT extends AbstractIntegrationTest {

    @Autowired
    TicketRepository ticketRepository;

    @Test
    void duplicateKey_returnsTheSameTicket() throws Exception {
        Cookie cookie = registerAndLogin("idem@test.com");

        MvcResult first = createTicket(cookie, "key-123").andExpect(status().isCreated()).andReturn();
        MvcResult second = createTicket(cookie, "key-123").andExpect(status().isCreated()).andReturn();

        assertEquals(idOf(first), idOf(second));
        mvc.perform(get("/tickets").cookie(cookie))
                .andExpect(jsonPath("$.page.totalElements").value(1));   // only one ticket exists
    }

    @Test
    void staleVersion_isRejectedByTheDatabase() throws Exception {
        Cookie cookie = registerAndLogin("lock@test.com");
        long id = idOf(createTicket(cookie, null).andReturn());

        // Outside a transaction, each repository call returns its own detached copy
        Ticket copyA = ticketRepository.findById(id).orElseThrow();
        Ticket copyB = ticketRepository.findById(id).orElseThrow();

        copyA.setTitle("Changed by A");
        ticketRepository.save(copyA);                    // version 0 -> 1

        copyB.setTitle("Changed by B");                  // still holds version 0
        assertThrows(OptimisticLockingFailureException.class, () -> ticketRepository.save(copyB));
    }

    @Test
    void sixthLoginInAMinute_returns429() throws Exception {
        String body = """
                {"email":"nobody@test.com","password":"wrong-Password-1"}""";
        for (int i = 1; i <= 5; i++) {
            mvc.perform(post("/auth/login").with(fromIp("10.0.0.99"))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/auth/login").with(fromIp("10.0.0.99"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    // Own IP per test, so other tests' logins can't use up this bucket
    private static RequestPostProcessor fromIp(String ip) {
        return request -> { request.setRemoteAddr(ip); return request; };
    }
}