package quad.solutions.trivia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import quad.solutions.trivia.client.OpenTriviaClient;
import quad.solutions.trivia.client.OpenTriviaQuestion;
import quad.solutions.trivia.dto.AnswerSubmissionRequest;
import quad.solutions.trivia.dto.CheckAnswersRequest;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.session.InMemoryQuizSessionStore;
import quad.solutions.trivia.session.QuizSession;

class QuizServiceTest {

	private final OpenTriviaClient openTriviaClient = Mockito.mock(OpenTriviaClient.class);
	private final InMemoryQuizSessionStore quizSessionStore = new InMemoryQuizSessionStore();
	private final QuizService quizService = new QuizService(openTriviaClient, quizSessionStore);
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void createQuizDoesNotExposeCorrectAnswersInResponse() throws Exception {
		when(openTriviaClient.fetchQuestions(2, null)).thenReturn(List.of(
				new OpenTriviaQuestion(
						"multiple",
						"medium",
						"Science & Nature",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air"))));

		QuizResponse response = quizService.createQuiz(2, null);
		String json = objectMapper.writeValueAsString(response);

		assertThat(json).doesNotContain("correctAnswer");
		assertThat(json).doesNotContain("token");
		assertThat(response.questions()).singleElement().satisfies(question -> {
			assertThat(question.options()).containsExactly("Water", "Fire", "Earth", "Air");
			assertThat(question.question()).isEqualTo("What is H2O?");
		});
	}

	@Test
	void createQuizStoresCorrectAnswersServerSide() {
		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new OpenTriviaQuestion(
						"boolean",
						"easy",
						"Music",
						"Is Jazz a genre?",
						"True",
						List.of("False"))));

		QuizResponse response = quizService.createQuiz(1, null);
		QuizSession storedQuiz = quizSessionStore.findById(response.quizId()).orElseThrow();

		assertThat(storedQuiz.questions()).singleElement().satisfies(question -> {
			assertThat(question.correctAnswer()).isEqualTo("True");
			assertThat(question.options()).containsExactly("True", "False");
		});
	}

	@Test
	void createQuizDecodesAndSanitizesQuestionTextAndOptions() {
		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new OpenTriviaQuestion(
						"multiple",
						"hard",
						"Science & Nature",
						"<script>alert('x')</script> & already decoded",
						"<b>Water</b>",
						List.of("<i>Fire</i>", "Earth & Wind", "\"Air\""))));

		QuizResponse response = quizService.createQuiz(1, null);

		assertThat(response.questions()).singleElement().satisfies(question -> {
			assertThat(question.question()).isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; already decoded");
			assertThat(question.options()).containsExactly(
					"&lt;b&gt;Water&lt;/b&gt;",
					"&lt;i&gt;Fire&lt;/i&gt;",
					"Earth &amp; Wind",
					"&quot;Air&quot;");
		});
	}

	@Test
	void checkAnswersReturnsScoreBasedOnServerSideAnswers() {
		when(openTriviaClient.fetchQuestions(2, null)).thenReturn(List.of(
				new OpenTriviaQuestion(
						"multiple",
						"medium",
						"Science & Nature",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air")),
				new OpenTriviaQuestion(
						"multiple",
						"easy",
						"Geography",
						"Which planet is known as the Red Planet?",
						"Mars",
						List.of("Venus", "Jupiter", "Saturn"))));

		QuizResponse quiz = quizService.createQuiz(2, null);
		CheckAnswersResponse response = quizService.checkAnswers(new CheckAnswersRequest(
				quiz.quizId(),
				List.of(
						new AnswerSubmissionRequest(quiz.questions().get(0).id(), "Water"),
						new AnswerSubmissionRequest(quiz.questions().get(1).id(), "Venus"))));

		assertThat(response.score()).isEqualTo(1);
		assertThat(response.totalQuestions()).isEqualTo(2);
		assertThat(response.results()).containsExactlyInAnyOrder(
				new quad.solutions.trivia.dto.AnswerResultResponse(quiz.questions().get(0).id(), true),
				new quad.solutions.trivia.dto.AnswerResultResponse(quiz.questions().get(1).id(), false));
	}

	@Test
	void checkAnswersRejectsUnknownQuizSession() {
		assertThatThrownBy(() -> quizService.checkAnswers(new CheckAnswersRequest(
				"missing-quiz",
				List.of(new AnswerSubmissionRequest("question-1", "Water")))))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode()).isEqualTo(NOT_FOUND));
	}

	@Test
	void checkAnswersRejectsExpiredQuizSession() {
		Clock initialClock = Clock.fixed(Instant.parse("2026-05-14T10:15:30Z"), ZoneOffset.UTC);
		InMemoryQuizSessionStore store = new InMemoryQuizSessionStore(initialClock);
		QuizService service = new QuizService(openTriviaClient, store, initialClock);

		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new OpenTriviaQuestion(
						"boolean",
						"easy",
						"Music",
						"Is Jazz a genre?",
						"True",
						List.of("False"))));

		QuizResponse quiz = service.createQuiz(1, null);
		Clock expiredClock = Clock.fixed(Instant.parse("2026-05-14T10:31:00Z"), ZoneOffset.UTC);
		QuizService expiredService = new QuizService(openTriviaClient, store, expiredClock);

		assertThatThrownBy(() -> expiredService.checkAnswers(new CheckAnswersRequest(
				quiz.quizId(),
				List.of(new AnswerSubmissionRequest(quiz.questions().getFirst().id(), "True")))))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode()).isEqualTo(GONE));
	}

	@Test
	void checkAnswersRejectsDuplicateSubmit() {
		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new OpenTriviaQuestion(
						"boolean",
						"easy",
						"Music",
						"Is Jazz a genre?",
						"True",
						List.of("False"))));

		QuizResponse quiz = quizService.createQuiz(1, null);
		CheckAnswersRequest request = new CheckAnswersRequest(
				quiz.quizId(),
				List.of(new AnswerSubmissionRequest(quiz.questions().getFirst().id(), "True")));

		quizService.checkAnswers(request);

		assertThatThrownBy(() -> quizService.checkAnswers(request))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode()).isEqualTo(CONFLICT));
	}

}
