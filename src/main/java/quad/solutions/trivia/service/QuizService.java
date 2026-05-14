package quad.solutions.trivia.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

	private final OpenTriviaClient openTriviaClient;
	private final InMemoryQuizSessionStore quizSessionStore;

	public QuizService(OpenTriviaClient openTriviaClient, InMemoryQuizSessionStore quizSessionStore) {
		this.openTriviaClient = openTriviaClient;
		this.quizSessionStore = quizSessionStore;
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
					upstreamQuestion.correctAnswer(),
					List.copyOf(sanitizedOptions)));
		}

		quizSessionStore.save(new QuizSession(quizId, List.copyOf(storedQuestions)));
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
