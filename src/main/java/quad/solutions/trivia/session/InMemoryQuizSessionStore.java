package quad.solutions.trivia.session;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class InMemoryQuizSessionStore {

	private final Map<String, QuizSession> sessions = new ConcurrentHashMap<>();
	private final Clock clock;

	public InMemoryQuizSessionStore(Clock clock) {
		this.clock = clock;
	}

	public void save(QuizSession quizSession) {
		sessions.put(quizSession.id(), quizSession);
	}

	public Optional<QuizSession> findById(String id) {
		QuizSession session = sessions.get(id);

		if (session == null) {
			return Optional.empty();
		}

		if (session.isExpiredAt(Instant.now(clock))) {
			sessions.remove(id);
			return Optional.empty();
		}

		return Optional.of(session);
	}

}
