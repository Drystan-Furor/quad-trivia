package quad.solutions.trivia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import quad.solutions.trivia.client.OpenTriviaClient;
import quad.solutions.trivia.dto.AnswerResultResponse;
import quad.solutions.trivia.dto.AnswerSubmissionRequest;
import quad.solutions.trivia.dto.CheckAnswersRequest;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.mapper.QuizMapper;
import quad.solutions.trivia.model.TriviaQuestion;
import quad.solutions.trivia.session.InMemoryQuizSessionStore;
import quad.solutions.trivia.session.QuizSession;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

	private static final Instant DEFAULT_INSTANT = Instant.parse("2026-05-14T10:20:00Z");

	@Mock
	private OpenTriviaClient openTriviaClient;

	@Mock
	private Clock clock;

	@Spy
	private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Spy
	private InMemoryQuizSessionStore quizSessionStore = new InMemoryQuizSessionStore(
			Clock.fixed(DEFAULT_INSTANT, ZoneOffset.UTC));

	@Spy
	private QuizMapper quizMapper = new QuizMapper();

	@InjectMocks
	private QuizService quizService;

	@BeforeEach
	void setUpClock() {
		lenient().when(clock.instant()).thenReturn(DEFAULT_INSTANT);
	}

	@Test
	void createQuizDoesNotExposeCorrectAnswerFieldsInResponse() {
		when(openTriviaClient.fetchQuestions(2, null)).thenReturn(List.of(
				new TriviaQuestion(
						"multiple",
						"medium",
						"Science & Nature",
						"What is H2O?",
						"Water",
						List.of("Fire", "Earth", "Air"))));

		QuizResponse response = quizService.createQuiz(2, null);

		assertRecordFieldsDoNotContain(QuizResponse.class, "correctAnswer", "token");
		assertRecordFieldsDoNotContain(response.questions().getFirst().getClass(), "correctAnswer", "token");
		assertThat(response.questions()).singleElement().satisfies(question -> {
			assertThat(question.options()).containsExactly("Water", "Fire", "Earth", "Air");
			assertThat(question.question()).isEqualTo("What is H2O?");
		});
	}

	@Test
	void createQuizStoresCorrectAnswersServerSide() {
		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new TriviaQuestion(
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
				new TriviaQuestion(
						"multiple",
						"easy",
						"Science: Computers",
						"What does CPU stand for?",
						"Central Processing Unit",
						List.of("Computer Personal Unit"))));

		QuizResponse response = quizService.createQuiz(1, 18);

		assertThat(response.questions()).singleElement().satisfies(question ->
				assertThat(question.category()).isEqualTo("Science: Computers"));
		verify(openTriviaClient).fetchQuestions(1, 18);
	}

	@ParameterizedTest
	@MethodSource("invalidQuizRequests")
	void createQuizRejectsInvalidQuizRequestParameters(int amount, Integer category) {
		assertThatThrownBy(() -> quizService.createQuiz(amount, category))
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
		QuizService service = new QuizService(openTriviaClient, store, fixedClock, validator, quizMapper);

		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new TriviaQuestion(
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
				new TriviaQuestion(
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
				new TriviaQuestion(
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
		verify(openTriviaClient).fetchQuestions(1, null);
	}

	@Test
	void checkAnswersReturnsOnlyScoreAndCorrectness() {
		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new TriviaQuestion(
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
		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new TriviaQuestion(
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
		when(openTriviaClient.fetchQuestions(2, null)).thenReturn(List.of(
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
				new TriviaQuestion(
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
		QuizService service = new QuizService(openTriviaClient, store, fixedClock, validator, quizMapper);

		when(openTriviaClient.fetchQuestions(1, null)).thenReturn(List.of(
				new TriviaQuestion(
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

}
