package quad.solutions.trivia.client;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class OpenTriviaClientConfig {

	@Bean
	Clock triviaClock() {
		return Clock.systemUTC();
	}

	@Bean
	RestClient openTriviaRestClient(RestClient.Builder builder,
			@Value("${open-trivia.base-url:https://opentdb.com}") String baseUrl) {
		return builder.baseUrl(baseUrl).build();
	}

	@Bean
	OpenTriviaClient openTriviaClient(RestClient openTriviaRestClient, Clock triviaClock) {
		return new OpenTriviaClient(openTriviaRestClient, triviaClock);
	}

}
