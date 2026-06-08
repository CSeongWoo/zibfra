package com.example.zipfra;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class ZipfraApplicationTests {

	static {
		try {
			io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
					.directory("./")
					.ignoreIfMalformed()
					.ignoreIfMissing()
					.load();
			dotenv.entries().forEach(entry -> {
				System.setProperty(entry.getKey(), entry.getValue());
			});
		} catch (Exception e) {
			// 무시
		}
	}

	@MockitoBean
	private RedisTemplate<String, String> redisTemplate;

	@Test
	void contextLoads() {
	}

}
