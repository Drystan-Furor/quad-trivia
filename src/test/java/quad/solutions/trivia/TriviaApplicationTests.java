package quad.solutions.trivia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import quad.solutions.trivia.client.OpenTriviaClient;
import quad.solutions.trivia.service.QuizService;
import quad.solutions.trivia.session.InMemoryQuizSessionStore;

@SpringBootTest
@ActiveProfiles("test")
class TriviaApplicationTests {

	@Autowired
	private QuizService quizService;

	@Autowired
	private OpenTriviaClient openTriviaClient;

	@Autowired
	private InMemoryQuizSessionStore quizSessionStore;

	@Autowired
	private Clock clock;

	@Autowired
	private RestClient restClient;

	@Test
	void contextLoads() {
		assertThat(quizService).isNotNull();
		assertThat(openTriviaClient).isNotNull();
		assertThat(quizSessionStore).isNotNull();
		assertThat(clock).isNotNull();
		assertThat(restClient).isNotNull();
	}

}
