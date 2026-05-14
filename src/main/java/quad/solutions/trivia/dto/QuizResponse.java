package quad.solutions.trivia.dto;

import java.util.List;

public record QuizResponse(
		String quizId,
		List<QuestionResponse> questions) {
}
