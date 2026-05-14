package quad.solutions.trivia.session;

import java.util.List;

public record StoredQuestion(
		String id,
		String correctAnswer,
		List<String> options) {
}
