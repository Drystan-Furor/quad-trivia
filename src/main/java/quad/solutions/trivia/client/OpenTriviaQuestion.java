package quad.solutions.trivia.client;

import java.util.List;

public record OpenTriviaQuestion(
		String type,
		String difficulty,
		String category,
		String question,
		String correctAnswer,
		List<String> incorrectAnswers) {
}
