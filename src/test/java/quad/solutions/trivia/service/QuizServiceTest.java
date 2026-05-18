package quad.solutions.trivia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

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

		assertThat(storedQuiz.issuedAt()).isNotNull();
		assertThat(storedQuiz.expiresAt()).isAfter(storedQuiz.issuedAt());
		assertThat(storedQuiz.used()).isFalse();
		assertThat(storedQuiz.questions()).singleElement().satisfies(question -> {
			assertThat(question.correctAnswer()).isEqualTo("True");
			assertThat(question.options()).containsExactly("True", "False");
		});
	}

	@Test
	void createQuizPassesSelectedCategoryToOpenTriviaClient() {
		when(openTriviaClient.fetchQuestions(1, 18)).thenReturn(List.of(
				new OpenTriviaQuestion(
						"multiple",
						"easy",
						"Science: Computers",
						"What does CPU stand for?",
						"Central Processing Unit",
						List.of("Computer Personal Unit"))));

		QuizResponse response = quizService.createQuiz(1, 18);

		assertThat(response.questions()).singleElement().satisfies(question ->
				assertThat(question.category()).isEqualTo("Science: Computers"));
	}

	@Test
	void createQuizRejectsNonPositiveCategories() {
		assertThatThrownBy(() -> quizService.createQuiz(1, 0))
				.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
					assertThat(exception.getStatusCode()).isEqualTo(BAD_REQUEST);
					assertThat(exception.getReason()).isEqualTo("Invalid request parameters");
				});
	}

	@Test
	void createQuizAssignsExpectedSessionTtl() {
		Clock fixedClock = Clock.fixed(Instant.parse("2026-05-14T10:15:30Z"), ZoneOffset.UTC);
		InMemoryQuizSessionStore store = new InMemoryQuizSessionStore(fixedClock);
		QuizService service = new QuizService(openTriviaClient, store, fixedClock);

		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new OpenTriviaQuestion(
						"boolean",
						"easy",
						"Music",
						"Is Jazz a genre?",
						"True",
						List.of("False"))));

		QuizResponse response = service.createQuiz(1, null);
		QuizSession storedQuiz = store.findById(response.quizId()).orElseThrow();

		assertThat(storedQuiz.issuedAt()).isEqualTo(Instant.parse("2026-05-14T10:15:30Z"));
		assertThat(storedQuiz.expiresAt()).isEqualTo(Instant.parse("2026-05-14T10:30:30Z"));
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
	void checkAnswersEvaluatesSubmittedAnswersAgainstServerSideSession() {
		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new OpenTriviaQuestion(
						"multiple",
						"easy",
						"Science",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air"))));
		QuizResponse quiz = quizService.createQuiz(1, null);

		CheckAnswersResponse response = quizService.checkAnswers(new CheckAnswersRequest(
				quiz.quizId(),
				List.of(new AnswerSubmissionRequest(quiz.questions().getFirst().id(), "Water"))));

		assertThat(response.score()).isEqualTo(1);
		assertThat(response.totalQuestions()).isEqualTo(1);
		assertThat(response.results()).singleElement().satisfies(result -> {
			assertThat(result.questionId()).isEqualTo(quiz.questions().getFirst().id());
			assertThat(result.correct()).isTrue();
		});
		assertThat(quizSessionStore.findById(quiz.quizId())).get().extracting(QuizSession::used).isEqualTo(true);
	}

	@Test
	void checkAnswersDoesNotExposeCorrectAnswersInResponse() throws Exception {
		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new OpenTriviaQuestion(
						"multiple",
						"easy",
						"Science",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air"))));
		QuizResponse quiz = quizService.createQuiz(1, null);

		CheckAnswersResponse response = quizService.checkAnswers(new CheckAnswersRequest(
				quiz.quizId(),
				List.of(new AnswerSubmissionRequest(quiz.questions().getFirst().id(), "Fire"))));
		String json = objectMapper.writeValueAsString(response);

		assertThat(response.score()).isZero();
		assertThat(json).doesNotContain("correctAnswer");
		assertThat(json).doesNotContain("Water");
		assertThat(json).doesNotContain("token");
	}

	@Test
	void checkAnswersRejectsRepeatedSubmissions() {
		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new OpenTriviaQuestion(
						"multiple",
						"easy",
						"Science",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air"))));
		QuizResponse quiz = quizService.createQuiz(1, null);
		CheckAnswersRequest request = new CheckAnswersRequest(
				quiz.quizId(),
				List.of(new AnswerSubmissionRequest(quiz.questions().getFirst().id(), "Water")));

		quizService.checkAnswers(request);

		assertThatThrownBy(() -> quizService.checkAnswers(request))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("already been submitted");
	}

	@Test
	void checkAnswersRejectsIncompleteAnswerPayload() {
		when(openTriviaClient.fetchQuestions(2, null)).thenReturn(List.of(
				new OpenTriviaQuestion(
						"multiple",
						"easy",
						"Science",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air")),
				new OpenTriviaQuestion(
						"multiple",
						"easy",
						"Science",
						"What is CO2?",
						"Carbon dioxide",
						List.of("Oxygen", "Hydrogen", "Nitrogen"))));
		QuizResponse quiz = quizService.createQuiz(2, null);

		assertThatThrownBy(() -> quizService.checkAnswers(new CheckAnswersRequest(
				quiz.quizId(),
				List.of(new AnswerSubmissionRequest(quiz.questions().getFirst().id(), "Water")))))
				.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
					assertThat(exception.getStatusCode()).isEqualTo(BAD_REQUEST);
					assertThat(exception.getReason()).contains("complete answer");
				});
	}

	@Test
	void checkAnswersRejectsAnswersForUnexpectedQuestionIds() {
		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new OpenTriviaQuestion(
						"multiple",
						"easy",
						"Science",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air"))));
		QuizResponse quiz = quizService.createQuiz(1, null);

		assertThatThrownBy(() -> quizService.checkAnswers(new CheckAnswersRequest(
				quiz.quizId(),
				List.of(new AnswerSubmissionRequest("question-does-not-exist", "Water")))))
				.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
					assertThat(exception.getStatusCode()).isEqualTo(BAD_REQUEST);
					assertThat(exception.getReason()).contains("known questions");
				});
	}

	@Test
	void createQuizRejectsRequestsThatExceedLocalRateGuard() {
		Clock fixedClock = Clock.fixed(Instant.parse("2026-05-14T10:15:30Z"), ZoneOffset.UTC);
		InMemoryQuizSessionStore store = new InMemoryQuizSessionStore(fixedClock);
		QuizService service = new QuizService(openTriviaClient, store, fixedClock);

		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new OpenTriviaQuestion(
						"boolean",
						"easy",
						"Music",
						"Is Jazz a genre?",
						"True",
						List.of("False"))));

		service.createQuiz(1, null);

		assertThatThrownBy(() -> service.createQuiz(1, null))
				.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
					assertThat(exception.getStatusCode()).isEqualTo(TOO_MANY_REQUESTS);
					assertThat(exception.getReason()).contains("Please wait");
				});
	}

}
