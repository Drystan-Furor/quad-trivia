package quad.solutions.trivia.dto;

public record AnswerResultResponse(
		String questionId,
		String question,
		String selectedAnswer,
		String correctAnswer,
		boolean correct) {
}
