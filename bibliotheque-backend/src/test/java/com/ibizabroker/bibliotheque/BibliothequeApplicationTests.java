package com.ibizabroker.bibliotheque;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Le contexte applicatif charge avec le profil "test" (H2 en memoire) :
 * `mvnw test` s'execute sans aucune base PostgreSQL qui tourne.
 */
@SpringBootTest
@ActiveProfiles("test")
class BibliothequeApplicationTests {

	@Test
	void contextLoads() {
	}

}
