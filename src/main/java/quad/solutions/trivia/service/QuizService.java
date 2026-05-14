package quad.solutions.trivia.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import quad.solutions.trivia.client.OpenTriviaClient;
import quad.solutions.trivia.client.OpenTriviaQuestion;
import quad.solutions.trivia.dto.QuestionResponse;
import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.session.InMemoryQuizSessionStore;
import quad.solutions.trivia.session.QuizSession;
import quad.solutions.trivia.session.StoredQuestion;

@Service
public class QuizService {

	private static final Duration QUIZ_TTL = Duration.ofMinutes(15);

	private final OpenTriviaClient openTriviaClient;
	private final InMemoryQuizSessionStore quizSessionStore;
	private final Clock clock;

	@Autowired
	public QuizService(OpenTriviaClient openTriviaClient, InMemoryQuizSessionStore quizSessionStore) {
		this(openTriviaClient, quizSessionStore, Clock.systemUTC());
	}

	public QuizService(OpenTriviaClient openTriviaClient, InMemoryQuizSessionStore quizSessionStore, Clock clock) {
		this.openTriviaClient = openTriviaClient;
		this.quizSessionStore = quizSessionStore;
		this.clock = clock;
	}

	public QuizResponse createQuiz(int amount, Integer category) {
		List<OpenTriviaQuestion> upstreamQuestions = openTriviaClient.fetchQuestions(amount, category);
		String quizId = UUID.randomUUID().toString();
		List<QuestionResponse> safeQuestions = new ArrayList<>();
		List<StoredQuestion> storedQuestions = new ArrayList<>();

		for (OpenTriviaQuestion upstreamQuestion : upstreamQuestions) {
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

	private List<String> sanitizeOptions(OpenTriviaQuestion upstreamQuestion) {
		List<String> options = new ArrayList<>();
		options.add(sanitize(upstreamQuestion.correctAnswer()));
		options.addAll(upstreamQuestion.incorrectAnswers().stream().map(this::sanitize).toList());
		return List.copyOf(options);
	}

	private String sanitize(String value) {
		return value == null ? null : HtmlUtils.htmlEscape(value);
	}

}
