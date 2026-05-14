package quad.solutions.trivia.dto;

import java.util.List;

public record CheckAnswersRequest(
		String quizId,
		List<AnswerSubmissionRequest> answers) {
}
