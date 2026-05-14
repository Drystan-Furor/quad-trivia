package quad.solutions.trivia.session;

import java.util.List;

public record QuizSession(
		String id,
		List<StoredQuestion> questions) {
}
