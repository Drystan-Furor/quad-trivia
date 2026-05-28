package quad.solutions.trivia.service;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import quad.solutions.trivia.client.OpenTriviaClient;
import quad.solutions.trivia.dto.AnswerSubmissionRequest;
import quad.solutions.trivia.dto.CheckAnswersRequest;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.mapper.QuizMapper;
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
	private final Validator validator;
	private final QuizMapper quizMapper;
	private final AtomicReference<Instant> lastQuizCreatedAt = new AtomicReference<>();

	public QuizService(OpenTriviaClient openTriviaClient, InMemoryQuizSessionStore quizSessionStore, Clock clock,
			Validator validator, QuizMapper quizMapper) {
		this.openTriviaClient = openTriviaClient;
		this.quizSessionStore = quizSessionStore;
		this.clock = clock;
		this.validator = validator;
		this.quizMapper = quizMapper;
	}

	public QuizResponse createQuiz(int amount, Integer category) {
		validateQuizRequest(amount, category);
		enforceQuizCreationGuard();
		List<TriviaQuestion> upstreamQuestions = openTriviaClient.fetchQuestions(amount, category);
		String quizId = UUID.randomUUID().toString();
		List<QuizMapper.IssuedQuestion> issuedQuestions = upstreamQuestions.stream()
				.map(upstreamQuestion -> quizMapper.toIssuedQuestion(upstreamQuestion, UUID.randomUUID().toString()))
				.toList();
		List<StoredQuestion> storedQuestions = issuedQuestions.stream()
				.map(QuizMapper.IssuedQuestion::storedQuestion)
				.toList();

		Instant issuedAt = Instant.now(clock);
		quizSessionStore.save(new QuizSession(
				quizId,
				List.copyOf(storedQuestions),
				issuedAt,
				issuedAt.plus(QUIZ_TTL),
				false));
		return new QuizResponse(quizId, issuedQuestions.stream()
				.map(QuizMapper.IssuedQuestion::response)
				.toList());
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

		CheckAnswersResponse response = quizMapper.toCheckAnswersResponse(quizSession.questions(), submittedAnswers);
		quizSessionStore.save(quizSession.markUsed());
		return response;
	}

	private void validateQuizRequest(int amount, Integer category) {
		if (amount < 1 || amount > 50) {
			throw new ResponseStatusException(BAD_REQUEST, "Invalid request parameters");
		}
		if (category != null && category < 1) {
			throw new ResponseStatusException(BAD_REQUEST, "Invalid request parameters");
		}
	}

	private void enforceQuizCreationGuard() {
		Instant now = Instant.now(clock);
		while (true) {
			Instant previous = lastQuizCreatedAt.get();
			if (previous != null && previous.plus(QUIZ_CREATION_GUARD_WINDOW).isAfter(now)) {
				throw new ResponseStatusException(TOO_MANY_REQUESTS,
						"Please wait a few seconds before starting another quiz.");
			}
			if (lastQuizCreatedAt.compareAndSet(previous, now)) {
				return;
			}
		}
	}

	private void validateAnswerRequest(CheckAnswersRequest request) {
		if (request == null) {
			throw new ResponseStatusException(BAD_REQUEST, "Quiz session is required");
		}

		Set<ConstraintViolation<CheckAnswersRequest>> violations = validator.validate(request);
		if (violations.isEmpty()) {
			return;
		}

		if (hasViolation(violations, "quizId")) {
			throw new ResponseStatusException(BAD_REQUEST, "Quiz session is required");
		}
		if (hasViolation(violations, "answers")) {
			throw new ResponseStatusException(BAD_REQUEST, "A complete answer set is required");
		}
		if (violations.stream().map(violation -> violation.getPropertyPath().toString())
				.anyMatch(path -> path.startsWith("answers["))) {
			throw new ResponseStatusException(BAD_REQUEST, "Each answer must include a question id and selected answer");
		}

		throw new ResponseStatusException(BAD_REQUEST, "Invalid request payload");
	}

	private boolean hasViolation(Set<ConstraintViolation<CheckAnswersRequest>> violations, String propertyPath) {
		return violations.stream()
				.map(violation -> violation.getPropertyPath().toString())
				.anyMatch(propertyPath::equals);
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

}
