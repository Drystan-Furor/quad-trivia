package quad.solutions.trivia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import quad.solutions.trivia.client.OpenTriviaClient;
import quad.solutions.trivia.dto.AnswerResultResponse;
import quad.solutions.trivia.dto.AnswerSubmissionRequest;
import quad.solutions.trivia.dto.CheckAnswersRequest;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.difficulty.TriviaDifficulty;
import quad.solutions.trivia.mapper.QuizMapper;
import quad.solutions.trivia.model.TriviaQuestion;
import quad.solutions.trivia.session.InMemoryQuizSessionStore;
import quad.solutions.trivia.session.QuizSession;
import quad.solutions.trivia.session.StoredQuestion;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

	private static final Instant DEFAULT_INSTANT = Instant.parse("2026-05-14T10:20:00Z");

	@Mock
	private OpenTriviaClient openTriviaClient;

	@Mock
	private Clock clock;

	private InMemoryQuizSessionStore quizSessionStore;
	private QuizMapper quizMapper;
	private QuizService quizService;

	@BeforeEach
	void setUpClock() {
		quizSessionStore = new InMemoryQuizSessionStore(Clock.fixed(DEFAULT_INSTANT, ZoneOffset.UTC));
		quizMapper = new QuizMapper(QuizServiceTest::moveFirstAnswerToLast);
		quizService = new QuizService(openTriviaClient, quizSessionStore, clock, quizMapper);
		lenient().when(clock.instant()).thenReturn(DEFAULT_INSTANT);
	}

	@Test
	void createQuizDoesNotExposeCorrectAnswerFieldsInResponse() {
		when(openTriviaClient.fetchQuestions(2, null, TriviaDifficulty.ANY)).thenReturn(List.of(
				new TriviaQuestion(
						"multiple",
						"medium",
						"Science & Nature",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air"))));

		QuizResponse response = quizService.createQuiz(2, null, TriviaDifficulty.ANY);

		assertRecordFieldsDoNotContain(QuizResponse.class, "correctAnswer", "token");
		assertRecordFieldsDoNotContain(response.questions().getFirst().getClass(), "correctAnswer", "token");
		assertThat(response.questions()).singleElement().satisfies(question -> {
			assertThat(question.options()).containsExactly("Fire", "Earth", "Air", "Water");
			assertThat(question.question()).isEqualTo("What is H2O?");
		});
	}

	@Test
	void createQuizStoresCorrectAnswersServerSide() {
		when(openTriviaClient.fetchQuestions(1, null, TriviaDifficulty.ANY)).thenReturn(List.of(
				new TriviaQuestion(
						"boolean",
						"easy",
						"Music",
						"Is Jazz a genre?",
						"True",
						List.of("False"))));

		QuizResponse response = quizService.createQuiz(1, null, TriviaDifficulty.ANY);
		QuizSession storedQuiz = quizSessionStore.findById(response.quizId()).orElseThrow();

		assertThat(storedQuiz.issuedAt()).isNotNull();
		assertThat(storedQuiz.expiresAt()).isAfter(storedQuiz.issuedAt());
		assertThat(storedQuiz.used()).isFalse();
		assertThat(storedQuiz.questions()).singleElement().satisfies(question -> {
			assertThat(question.correctAnswer()).isEqualTo("True");
			assertThat(question.options()).containsExactly("False", "True");
		});
	}

	@Test
	void createQuizPassesSelectedCategoryToOpenTriviaClient() {
		when(openTriviaClient.fetchQuestions(1, 18, TriviaDifficulty.ANY)).thenReturn(List.of(
				new TriviaQuestion(
						"multiple",
						"easy",
						"Science: Computers",
						"What does CPU stand for?",
						"Central Processing Unit",
						List.of("Computer Personal Unit"))));

		QuizResponse response = quizService.createQuiz(1, 18, TriviaDifficulty.ANY);

		assertThat(response.questions()).singleElement().satisfies(question ->
				assertThat(question.category()).isEqualTo("Science: Computers"));
		verify(openTriviaClient).fetchQuestions(1, 18, TriviaDifficulty.ANY);
	}

	@Test
	void createQuizPassesSelectedDifficultyToOpenTriviaClient() {
		when(openTriviaClient.fetchQuestions(1, null, TriviaDifficulty.MEDIUM)).thenReturn(List.of(
				new TriviaQuestion(
						"multiple",
						"medium",
						"Science: Computers",
						"What does CPU stand for?",
						"Central Processing Unit",
						List.of("Computer Personal Unit"))));

		QuizResponse response = quizService.createQuiz(1, null, TriviaDifficulty.MEDIUM);

		assertThat(response.questions()).singleElement().satisfies(question ->
				assertThat(question.difficulty()).isEqualTo("medium"));
		verify(openTriviaClient).fetchQuestions(1, null, TriviaDifficulty.MEDIUM);
	}

	@ParameterizedTest
	@MethodSource("invalidQuizRequests")
	void createQuizRejectsInvalidQuizRequestParameters(int amount, Integer category) {
		assertThatThrownBy(() -> quizService.createQuiz(amount, category, TriviaDifficulty.ANY))
				.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
					assertThat(exception.getStatusCode()).isEqualTo(BAD_REQUEST);
					assertThat(exception.getReason()).isEqualTo("Invalid request parameters");
				});
		verifyNoInteractions(openTriviaClient);
	}

	@Test
	void createQuizRejectsMissingDifficulty() {
		assertThatThrownBy(() -> quizService.createQuiz(1, null, null))
				.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
					assertThat(exception.getStatusCode()).isEqualTo(BAD_REQUEST);
					assertThat(exception.getReason()).isEqualTo("Invalid request parameters");
				});
		verifyNoInteractions(openTriviaClient);
	}

	@Test
	void createQuizAssignsExpectedSessionTtl() {
		Clock fixedClock = Clock.fixed(Instant.parse("2026-05-14T10:15:30Z"), ZoneOffset.UTC);
		InMemoryQuizSessionStore store = new InMemoryQuizSessionStore(fixedClock);
		QuizService service = new QuizService(openTriviaClient, store, fixedClock, quizMapper);

		when(openTriviaClient.fetchQuestions(1, null, TriviaDifficulty.ANY)).thenReturn(List.of(
				new TriviaQuestion(
						"boolean",
						"easy",
						"Music",
						"Is Jazz a genre?",
						"True",
						List.of("False"))));

		QuizResponse response = service.createQuiz(1, null, TriviaDifficulty.ANY);
		QuizSession storedQuiz = store.findById(response.quizId()).orElseThrow();

		assertThat(storedQuiz.issuedAt()).isEqualTo(Instant.parse("2026-05-14T10:15:30Z"));
		assertThat(storedQuiz.expiresAt()).isEqualTo(Instant.parse("2026-05-14T10:30:30Z"));
	}

	@Test
	void createQuizDecodesAndSanitizesQuestionTextAndOptions() {
		when(openTriviaClient.fetchQuestions(1, null, TriviaDifficulty.ANY)).thenReturn(List.of(
				new TriviaQuestion(
						"multiple",
						"hard",
						"Science & Nature",
						"<script>alert('x')</script> & already decoded",
						"<b>Water</b>",
						List.of("<i>Fire</i>", "Earth & Wind", "\"Air\""))));

		QuizResponse response = quizService.createQuiz(1, null, TriviaDifficulty.ANY);

		assertThat(response.questions()).singleElement().satisfies(question -> {
			assertThat(question.question()).isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; already decoded");
			assertThat(question.options()).containsExactly(
					"&lt;i&gt;Fire&lt;/i&gt;",
					"Earth &amp; Wind",
					"&quot;Air&quot;",
					"&lt;b&gt;Water&lt;/b&gt;");
		});
	}

	@Test
	void checkAnswersEvaluatesSubmittedAnswersAgainstServerSideSession() {
		when(openTriviaClient.fetchQuestions(1, null, TriviaDifficulty.ANY)).thenReturn(List.of(
				new TriviaQuestion(
						"multiple",
						"easy",
						"Science",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air"))));
		QuizResponse quiz = quizService.createQuiz(1, null, TriviaDifficulty.ANY);

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
		verify(openTriviaClient).fetchQuestions(1, null, TriviaDifficulty.ANY);
	}

	@Test
	void checkAnswersScoresCorrectlyWhenCorrectAnswerWasNotPresentedFirst() {
		when(openTriviaClient.fetchQuestions(1, null, TriviaDifficulty.ANY)).thenReturn(List.of(
				new TriviaQuestion(
						"multiple",
						"easy",
						"Science",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air"))));
		QuizResponse quiz = quizService.createQuiz(1, null, TriviaDifficulty.ANY);

		assertThat(quiz.questions().getFirst().options()).containsExactly("Fire", "Earth", "Air", "Water");

		CheckAnswersResponse response = quizService.checkAnswers(new CheckAnswersRequest(
				quiz.quizId(),
				List.of(new AnswerSubmissionRequest(quiz.questions().getFirst().id(), "Water"))));

		assertThat(response.score()).isEqualTo(1);
		assertThat(response.results()).singleElement().extracting(AnswerResultResponse::correct).isEqualTo(true);
	}

	@Test
	void checkAnswersReturnsOnlyScoreAndCorrectness() {
		when(openTriviaClient.fetchQuestions(1, null, TriviaDifficulty.ANY)).thenReturn(List.of(
				new TriviaQuestion(
						"multiple",
						"easy",
						"Science",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air"))));
		QuizResponse quiz = quizService.createQuiz(1, null, TriviaDifficulty.ANY);

		CheckAnswersResponse response = quizService.checkAnswers(new CheckAnswersRequest(
				quiz.quizId(),
				List.of(new AnswerSubmissionRequest(quiz.questions().getFirst().id(), "Fire"))));

		assertThat(response).usingRecursiveComparison()
				.isEqualTo(new CheckAnswersResponse(
						0,
						1,
						List.of(new AnswerResultResponse(quiz.questions().getFirst().id(), false))));
		assertRecordFieldsDoNotContain(CheckAnswersResponse.class, "correctAnswer", "answer", "token");
		assertRecordFieldsDoNotContain(AnswerResultResponse.class, "correctAnswer", "answer", "token");
	}

	@Test
	void checkAnswersRejectsRepeatedSubmissions() {
		when(openTriviaClient.fetchQuestions(1, null, TriviaDifficulty.ANY)).thenReturn(List.of(
				new TriviaQuestion(
						"multiple",
						"easy",
						"Science",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air"))));
		QuizResponse quiz = quizService.createQuiz(1, null, TriviaDifficulty.ANY);
		CheckAnswersRequest request = new CheckAnswersRequest(
				quiz.quizId(),
				List.of(new AnswerSubmissionRequest(quiz.questions().getFirst().id(), "Water")));

		quizService.checkAnswers(request);

		assertThatThrownBy(() -> quizService.checkAnswers(request))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("already been submitted");
	}

	@Test
	void checkAnswersRejectsMissingQuizSessionAsUnavailable() {
		CheckAnswersRequest request = new CheckAnswersRequest(
				"quiz-does-not-exist",
				List.of(new AnswerSubmissionRequest("question-1", "Water")));

		assertThatThrownBy(() -> quizService.checkAnswers(request))
				.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
					assertThat(exception.getStatusCode()).isEqualTo(NOT_FOUND);
					assertThat(exception.getReason()).isEqualTo("Quiz session is not available");
				});
	}

	@Test
	void checkAnswersRejectsExpiredQuizSessionAsUnavailable() {
		Clock fixedClock = Clock.fixed(Instant.parse("2026-05-14T10:31:00Z"), ZoneOffset.UTC);
		InMemoryQuizSessionStore store = new InMemoryQuizSessionStore(fixedClock);
		QuizService service = new QuizService(openTriviaClient, store, fixedClock, quizMapper);
		store.save(new QuizSession(
				"quiz-expired",
				List.of(new StoredQuestion("question-1", "Water", List.of("Water", "Fire"))),
				Instant.parse("2026-05-14T10:15:30Z"),
				Instant.parse("2026-05-14T10:30:30Z"),
				false));

		assertThatThrownBy(() -> service.checkAnswers(new CheckAnswersRequest(
				"quiz-expired",
				List.of(new AnswerSubmissionRequest("question-1", "Water")))))
				.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
					assertThat(exception.getStatusCode()).isEqualTo(NOT_FOUND);
					assertThat(exception.getReason()).isEqualTo("Quiz session is not available");
				});
	}

	@ParameterizedTest
	@MethodSource("invalidAnswerRequests")
	void checkAnswersRejectsInvalidRequestPayloads(CheckAnswersRequest request, String expectedReason) {
		assertThatThrownBy(() -> quizService.checkAnswers(request))
				.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
					assertThat(exception.getStatusCode()).isEqualTo(BAD_REQUEST);
					assertThat(exception.getReason()).isEqualTo(expectedReason);
				});
		verifyNoInteractions(openTriviaClient);
	}

	@Test
	void checkAnswersRejectsIncompleteAnswerPayload() {
		when(openTriviaClient.fetchQuestions(2, null, TriviaDifficulty.ANY)).thenReturn(List.of(
				new TriviaQuestion(
						"multiple",
						"easy",
						"Science",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air")),
				new TriviaQuestion(
						"multiple",
						"easy",
						"Science",
						"What is CO2?",
						"Carbon dioxide",
						List.of("Oxygen", "Hydrogen", "Nitrogen"))));
		QuizResponse quiz = quizService.createQuiz(2, null, TriviaDifficulty.ANY);

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
		when(openTriviaClient.fetchQuestions(1, null, TriviaDifficulty.ANY)).thenReturn(List.of(
				new TriviaQuestion(
						"multiple",
						"easy",
						"Science",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air"))));
		QuizResponse quiz = quizService.createQuiz(1, null, TriviaDifficulty.ANY);

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
		QuizService service = new QuizService(openTriviaClient, store, fixedClock, quizMapper);

		when(openTriviaClient.fetchQuestions(1, null, TriviaDifficulty.ANY)).thenReturn(List.of(
				new TriviaQuestion(
						"boolean",
						"easy",
						"Music",
						"Is Jazz a genre?",
						"True",
						List.of("False"))));

		service.createQuiz(1, null, TriviaDifficulty.ANY);

		assertThatThrownBy(() -> service.createQuiz(1, null, TriviaDifficulty.ANY))
				.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
					assertThat(exception.getStatusCode()).isEqualTo(TOO_MANY_REQUESTS);
					assertThat(exception.getReason()).contains("Please wait");
				});
	}

	@Test
	void createQuizAllowsOnlyOneParallelRequestWithinRateGuardWindow() throws Exception {
		Clock fixedClock = Clock.fixed(Instant.parse("2026-05-14T10:15:30Z"), ZoneOffset.UTC);
		InMemoryQuizSessionStore store = new InMemoryQuizSessionStore(fixedClock);
		QuizService service = new QuizService(openTriviaClient, store, fixedClock, quizMapper);
		int attempts = 16;

		when(openTriviaClient.fetchQuestions(1, null, TriviaDifficulty.ANY)).thenReturn(List.of(
				new TriviaQuestion(
						"boolean",
						"easy",
						"Music",
						"Is Jazz a genre?",
						"True",
						List.of("False"))));

		ExecutorService executor = Executors.newFixedThreadPool(attempts);
		CountDownLatch ready = new CountDownLatch(attempts);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<Boolean>> results = IntStream.range(0, attempts)
					.mapToObj(index -> executor.submit(() -> {
						ready.countDown();
						assertThat(start.await(1, TimeUnit.SECONDS)).isTrue();
						try {
							service.createQuiz(1, null, TriviaDifficulty.ANY);
							return true;
						}
						catch (ResponseStatusException exception) {
							assertThat(exception.getStatusCode()).isEqualTo(TOO_MANY_REQUESTS);
							return false;
						}
					}))
					.toList();

			assertThat(ready.await(1, TimeUnit.SECONDS)).isTrue();
			start.countDown();

			assertThat(results)
					.extracting(Future::get)
					.containsOnlyOnce(true)
					.contains(false);
			verify(openTriviaClient, times(1)).fetchQuestions(1, null, TriviaDifficulty.ANY);
		}
		finally {
			executor.shutdownNow();
		}
	}

	private void assertRecordFieldsDoNotContain(Class<?> recordType, String... fieldNames) {
		assertThat(recordType.getRecordComponents())
				.extracting(RecordComponent::getName)
				.doesNotContain(fieldNames);
	}

	private static Stream<Arguments> invalidQuizRequests() {
		return Stream.of(
				Arguments.of(0, null),
				Arguments.of(51, null),
				Arguments.of(1, 0));
	}

	private static Stream<Arguments> invalidAnswerRequests() {
		return Stream.of(
				Arguments.of(null, "Quiz session is required"),
				Arguments.of(new CheckAnswersRequest("", List.of(new AnswerSubmissionRequest("question-1", "Water"))),
						"Quiz session is required"),
				Arguments.of(new CheckAnswersRequest("quiz-1", null), "A complete answer set is required"),
				Arguments.of(new CheckAnswersRequest("quiz-1", List.of()), "A complete answer set is required"),
				Arguments.of(new CheckAnswersRequest("quiz-1", Collections.singletonList(null)),
						"Each answer must include a question id and selected answer"),
				Arguments.of(new CheckAnswersRequest("quiz-1", List.of(new AnswerSubmissionRequest("", "Water"))),
						"Each answer must include a question id and selected answer"),
				Arguments.of(new CheckAnswersRequest("quiz-1", List.of(new AnswerSubmissionRequest("question-1", " "))),
						"Each answer must include a question id and selected answer"));
	}

	private static List<String> moveFirstAnswerToLast(List<String> options) {
		List<String> shuffled = new ArrayList<>(options);
		Collections.rotate(shuffled, -1);
		return shuffled;
	}

}
