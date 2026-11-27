package com.ticket.system;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Needs a real PostgreSQL and env vars. Replace with a Testcontainers test.")
@SpringBootTest
class TicketSystemApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
