package quad.solutions.trivia.type;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum TriviaType {

	ANY("any", "Alle vraagtypen"),
	MULTIPLE("multiple", "Meerkeuze"),
	BOOLEAN("boolean", "Waar / onwaar");

	private final String apiValue;
	private final String displayName;

	TriviaType(String apiValue, String displayName) {
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

	public static TriviaType fromRequestValue(String value) {
		if (value == null || value.isBlank()) {
			return ANY;
		}
		return Arrays.stream(values())
				.filter(type -> type.apiValue.equals(value))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Type must be one of any, multiple, or boolean"));
	}

	public static List<TriviaType> options() {
		return List.of(values());
	}

}
