package quad.solutions.trivia.session;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class InMemoryQuizSessionStore {

	private final Map<String, QuizSession> sessions = new ConcurrentHashMap<>();

	public void save(QuizSession quizSession) {
		sessions.put(quizSession.id(), quizSession);
	}

	public Optional<QuizSession> findById(String id) {
		return Optional.ofNullable(sessions.get(id));
	}

}
