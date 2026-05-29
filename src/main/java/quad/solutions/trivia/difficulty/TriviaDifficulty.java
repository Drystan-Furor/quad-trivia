package quad.solutions.trivia.difficulty;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum TriviaDifficulty {

	ANY("any", "Any Difficulty"),
	EASY("easy", "Easy"),
	MEDIUM("medium", "Medium"),
	HARD("hard", "Hard");

	private final String apiValue;
	private final String displayName;

	TriviaDifficulty(String apiValue, String displayName) {
		this.apiValue = apiValue;
		this.displayName = displayName;
	}

	public String apiValue() {
		return apiValue;
	}

	public String displayName() {
		return displayName;
	}

	public Optional<String> openTriviaValue() {
		return this == ANY ? Optional.empty() : Optional.of(apiValue);
	}

	public static TriviaDifficulty fromRequestValue(String value) {
		if (value == null || value.isBlank()) {
			return ANY;
		}
		return Arrays.stream(values())
				.filter(difficulty -> difficulty.apiValue.equals(value))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Difficulty must be one of any, easy, medium, or hard"));
	}

	public static List<TriviaDifficulty> options() {
		return List.of(values());
	}

}
