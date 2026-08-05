package quad.solutions.trivia.difficulty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

class TriviaDifficultyTest {

	@Test
	void optionsMatchDocumentedOpenTriviaDifficulties() {
		assertThat(TriviaDifficulty.options())
				.extracting(TriviaDifficulty::apiValue, TriviaDifficulty::displayName)
				.containsExactly(
						tuple("any", "Alle niveaus"),
						tuple("easy", "Makkelijk"),
						tuple("medium", "Gemiddeld"),
						tuple("hard", "Moeilijk"));
	}

	@Test
	void fromRequestValueAcceptsDocumentedValues() {
		assertThat(TriviaDifficulty.fromRequestValue(null)).isEqualTo(TriviaDifficulty.ANY);
		assertThat(TriviaDifficulty.fromRequestValue("")).isEqualTo(TriviaDifficulty.ANY);
		assertThat(TriviaDifficulty.fromRequestValue("easy")).isEqualTo(TriviaDifficulty.EASY);
		assertThat(TriviaDifficulty.fromRequestValue("medium")).isEqualTo(TriviaDifficulty.MEDIUM);
		assertThat(TriviaDifficulty.fromRequestValue("hard")).isEqualTo(TriviaDifficulty.HARD);
	}

	@Test
	void fromRequestValueRejectsUnknownValues() {
		assertThatThrownBy(() -> TriviaDifficulty.fromRequestValue("expert"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Difficulty");
	}

}
