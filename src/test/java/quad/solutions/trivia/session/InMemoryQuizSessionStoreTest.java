package quad.solutions.trivia.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

class InMemoryQuizSessionStoreTest {

	@Test
	void saveAndFindByIdReturnsStoredSession() {
		Clock fixedClock = Clock.fixed(Instant.parse("2026-05-14T10:20:00Z"), ZoneOffset.UTC);
		InMemoryQuizSessionStore store = new InMemoryQuizSessionStore(fixedClock);
		QuizSession session = new QuizSession(
				"quiz-1",
				List.of(new StoredQuestion("question-1", "What is H2O?", "Water", List.of("Water", "Fire"))),
				Instant.parse("2026-05-14T10:15:30Z"),
				Instant.parse("2026-05-14T10:30:30Z"),
				false);

		store.save(session);

		assertThat(store.findById("quiz-1")).contains(session);
	}

	@Test
	void findByIdRemovesExpiredSession() {
		Clock fixedClock = Clock.fixed(Instant.parse("2026-05-14T10:31:00Z"), ZoneOffset.UTC);
		InMemoryQuizSessionStore store = new InMemoryQuizSessionStore(fixedClock);
		QuizSession expiredSession = new QuizSession(
				"quiz-2",
				List.of(new StoredQuestion("question-1", "What is H2O?", "Water", List.of("Water", "Fire"))),
				Instant.parse("2026-05-14T10:15:30Z"),
				Instant.parse("2026-05-14T10:30:30Z"),
				false);

		store.save(expiredSession);

		assertThat(store.findById("quiz-2")).isEmpty();
	}

	@Test
	void findByIdTreatsExactExpiryInstantAsExpired() {
		Instant expiresAt = Instant.parse("2026-05-14T10:30:30Z");
		InMemoryQuizSessionStore store = new InMemoryQuizSessionStore(Clock.fixed(expiresAt, ZoneOffset.UTC));
		store.save(new QuizSession(
				"quiz-at-expiry",
				List.of(new StoredQuestion("question-1", "What is H2O?", "Water", List.of("Water", "Fire"))),
				Instant.parse("2026-05-14T10:15:30Z"),
				expiresAt,
				false));

		assertThat(store.findById("quiz-at-expiry")).isEmpty();
	}

	@Test
	void markUsedUpdatesStoredSession() {
		Clock fixedClock = Clock.fixed(Instant.parse("2026-05-14T10:20:00Z"), ZoneOffset.UTC);
		InMemoryQuizSessionStore store = new InMemoryQuizSessionStore(fixedClock);
		QuizSession session = new QuizSession(
				"quiz-3",
				List.of(new StoredQuestion("question-1", "What is H2O?", "Water", List.of("Water", "Fire"))),
				Instant.parse("2026-05-14T10:15:30Z"),
				Instant.parse("2026-05-14T10:30:30Z"),
				false);
		store.save(session);

		store.save(session.markUsed());

		assertThat(store.findById("quiz-3")).get().extracting(QuizSession::used).isEqualTo(true);
	}

	@Test
	void markUsedOnlySucceedsForCurrentUnusedSession() {
		Clock fixedClock = Clock.fixed(Instant.parse("2026-05-14T10:20:00Z"), ZoneOffset.UTC);
		InMemoryQuizSessionStore store = new InMemoryQuizSessionStore(fixedClock);
		QuizSession session = new QuizSession(
				"quiz-4",
				List.of(new StoredQuestion("question-1", "What is H2O?", "Water", List.of("Water", "Fire"))),
				Instant.parse("2026-05-14T10:15:30Z"),
				Instant.parse("2026-05-14T10:30:30Z"),
				false);
		store.save(session);

		assertThat(store.markUsed(session)).isTrue();
		assertThat(store.markUsed(session)).isFalse();
		assertThat(store.markUsed(store.findById("quiz-4").orElseThrow())).isFalse();
	}

}
