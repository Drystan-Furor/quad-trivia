package quad.solutions.trivia.dto;

import java.util.List;

public record QuestionResponse(
		String id,
		String type,
		String difficulty,
		String category,
		String question,
		List<String> options) {
}
