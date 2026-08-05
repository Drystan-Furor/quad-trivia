package quad.solutions.trivia.session;

import java.time.Instant;
import java.util.List;

public record QuizSession(
		String id,
		List<StoredQuestion> questions,
		Instant issuedAt,
		Instant expiresAt,
		boolean used) {

	public boolean isExpiredAt(Instant instant) {
		return expiresAt != null && !instant.isBefore(expiresAt);
	}

	public QuizSession markUsed() {
		return new QuizSession(id, questions, issuedAt, expiresAt, true);
	}
}
