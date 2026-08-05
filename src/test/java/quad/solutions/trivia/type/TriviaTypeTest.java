package quad.solutions.trivia.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

class TriviaTypeTest {

	@Test
	void optionsMatchDocumentedOpenTriviaTypes() {
		assertThat(TriviaType.options())
				.extracting(TriviaType::apiValue, TriviaType::displayName)
				.containsExactly(
						tuple("any", "Any Type"),
						tuple("multiple", "Multiple Choice"),
						tuple("boolean", "True / False"));
	}

	@Test
	void fromRequestValueAcceptsDocumentedValues() {
		assertThat(TriviaType.fromRequestValue(null)).isEqualTo(TriviaType.ANY);
		assertThat(TriviaType.fromRequestValue("")).isEqualTo(TriviaType.ANY);
		assertThat(TriviaType.fromRequestValue("multiple")).isEqualTo(TriviaType.MULTIPLE);
		assertThat(TriviaType.fromRequestValue("boolean")).isEqualTo(TriviaType.BOOLEAN);
	}

	@Test
	void fromRequestValueRejectsUnknownValues() {
		assertThatThrownBy(() -> TriviaType.fromRequestValue("essay"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Type");
	}

}
