package quad.solutions.trivia.dto;

import java.util.List;

public record CheckAnswersResponse(
		int score,
		int totalQuestions,
		List<AnswerResultResponse> results) {
}
