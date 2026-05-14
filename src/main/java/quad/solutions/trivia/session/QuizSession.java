package quad.solutions.trivia.session;

import java.time.Instant;
import java.util.List;

public record QuizSession(
		String id,
		List<StoredQuestion> questions,
		Instant expiresAt,
		boolean used) {

	public boolean isExpiredAt(Instant instant) {
		return expiresAt != null && instant.isAfter(expiresAt);
	}

	public QuizSession markUsed() {
		return new QuizSession(id, questions, expiresAt, true);
	}
}
