package quad.solutions.trivia.service;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.server.ResponseStatusException;

import quad.solutions.trivia.client.OpenTriviaClient;
import quad.solutions.trivia.dto.AnswerResultResponse;
import quad.solutions.trivia.dto.AnswerSubmissionRequest;
import quad.solutions.trivia.dto.CheckAnswersRequest;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.dto.QuestionResponse;
import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.model.TriviaQuestion;
import quad.solutions.trivia.session.InMemoryQuizSessionStore;
import quad.solutions.trivia.session.QuizSession;
import quad.solutions.trivia.session.StoredQuestion;

@Service
public class QuizService {

	private static final Duration QUIZ_TTL = Duration.ofMinutes(15);
	private static final Duration QUIZ_CREATION_GUARD_WINDOW = Duration.ofSeconds(5);

	private final OpenTriviaClient openTriviaClient;
	private final InMemoryQuizSessionStore quizSessionStore;
	private final Clock clock;
	private Instant lastQuizCreatedAt;

	public QuizService(OpenTriviaClient openTriviaClient, InMemoryQuizSessionStore quizSessionStore, Clock clock) {
		this.openTriviaClient = openTriviaClient;
		this.quizSessionStore = quizSessionStore;
		this.clock = clock;
	}

	public QuizResponse createQuiz(int amount, Integer category) {
		validateQuizRequest(amount, category);
		enforceQuizCreationGuard();
		List<TriviaQuestion> upstreamQuestions = openTriviaClient.fetchQuestions(amount, category);
		String quizId = UUID.randomUUID().toString();
		List<QuestionResponse> safeQuestions = new ArrayList<>();
		List<StoredQuestion> storedQuestions = new ArrayList<>();

		for (TriviaQuestion upstreamQuestion : upstreamQuestions) {
			String questionId = UUID.randomUUID().toString();
			List<String> sanitizedOptions = sanitizeOptions(upstreamQuestion);

			safeQuestions.add(new QuestionResponse(
					questionId,
					sanitize(upstreamQuestion.type()),
					sanitize(upstreamQuestion.difficulty()),
					sanitize(upstreamQuestion.category()),
					sanitize(upstreamQuestion.question()),
					sanitizedOptions));
			storedQuestions.add(new StoredQuestion(
					questionId,
					sanitize(upstreamQuestion.correctAnswer()),
					List.copyOf(sanitizedOptions)));
		}

		Instant issuedAt = Instant.now(clock);
		quizSessionStore.save(new QuizSession(
				quizId,
				List.copyOf(storedQuestions),
				issuedAt,
				issuedAt.plus(QUIZ_TTL),
				false));
		return new QuizResponse(quizId, List.copyOf(safeQuestions));
	}

	public CheckAnswersResponse checkAnswers(CheckAnswersRequest request) {
		validateAnswerRequest(request);

		if (!quizSessionStore.hasSession(request.quizId())) {
			throw new ResponseStatusException(NOT_FOUND, "Quiz session is not available");
		}

		QuizSession quizSession = quizSessionStore.findById(request.quizId())
				.orElseThrow(() -> new ResponseStatusException(GONE, "Quiz session has expired"));

		if (quizSession.used()) {
			throw new ResponseStatusException(CONFLICT, "Quiz answers have already been submitted");
		}

		List<AnswerSubmissionRequest> answers = request.answers() == null ? List.of() : request.answers();
		validateSubmittedAnswers(quizSession, answers);
		Map<String, String> submittedAnswers = answers.stream()
				.collect(Collectors.toMap(
						AnswerSubmissionRequest::questionId,
						AnswerSubmissionRequest::answer,
						(first, second) -> second));

		List<AnswerResultResponse> results = quizSession.questions().stream()
				.map(question -> new AnswerResultResponse(
						question.id(),
						question.correctAnswer().equals(submittedAnswers.get(question.id()))))
				.toList();

		int score = (int) results.stream().filter(AnswerResultResponse::correct).count();
		quizSessionStore.save(quizSession.markUsed());
		return new CheckAnswersResponse(score, quizSession.questions().size(), results);
	}

	private void validateQuizRequest(int amount, Integer category) {
		if (amount < 1 || amount > 50) {
			throw new ResponseStatusException(BAD_REQUEST, "Invalid request parameters");
		}
		if (category != null && category < 1) {
			throw new ResponseStatusException(BAD_REQUEST, "Invalid request parameters");
		}
	}

	private synchronized void enforceQuizCreationGuard() {
		Instant now = Instant.now(clock);
		if (lastQuizCreatedAt != null && lastQuizCreatedAt.plus(QUIZ_CREATION_GUARD_WINDOW).isAfter(now)) {
			throw new ResponseStatusException(TOO_MANY_REQUESTS,
					"Please wait a few seconds before starting another quiz.");
		}
		lastQuizCreatedAt = now;
	}

	private void validateAnswerRequest(CheckAnswersRequest request) {
		if (request == null || request.quizId() == null || request.quizId().isBlank()) {
			throw new ResponseStatusException(BAD_REQUEST, "Quiz session is required");
		}
		if (request.answers() == null || request.answers().isEmpty()) {
			throw new ResponseStatusException(BAD_REQUEST, "A complete answer set is required");
		}
		for (AnswerSubmissionRequest answer : request.answers()) {
			if (answer == null || answer.questionId() == null || answer.questionId().isBlank()
					|| answer.answer() == null || answer.answer().isBlank()) {
				throw new ResponseStatusException(BAD_REQUEST, "Each answer must include a question id and selected answer");
			}
		}
	}

	private void validateSubmittedAnswers(QuizSession quizSession, List<AnswerSubmissionRequest> answers) {
		Set<String> expectedQuestionIds = quizSession.questions().stream()
				.map(StoredQuestion::id)
				.collect(Collectors.toSet());
		Set<String> submittedQuestionIds = new HashSet<>();

		for (AnswerSubmissionRequest answer : answers) {
			submittedQuestionIds.add(answer.questionId());
		}

		if (answers.size() != quizSession.questions().size() || submittedQuestionIds.size() != answers.size()
				|| !submittedQuestionIds.equals(expectedQuestionIds)) {
			throw new ResponseStatusException(BAD_REQUEST, "A complete answer set for the known questions is required");
		}

		Map<String, StoredQuestion> questionsById = quizSession.questions().stream()
				.collect(Collectors.toMap(StoredQuestion::id, question -> question));
		for (AnswerSubmissionRequest answer : answers) {
			StoredQuestion question = questionsById.get(answer.questionId());
			if (question == null || !question.options().contains(answer.answer())) {
				throw new ResponseStatusException(BAD_REQUEST, "Submitted answers must match the issued quiz options");
			}
		}
	}

	private List<String> sanitizeOptions(TriviaQuestion upstreamQuestion) {
		return Stream.concat(
				Stream.of(upstreamQuestion.correctAnswer()),
				upstreamQuestion.incorrectAnswers().stream())
				.map(this::sanitize)
				.toList();
	}

	private String sanitize(String value) {
		return value == null ? null : HtmlUtils.htmlEscape(value);
	}

}
