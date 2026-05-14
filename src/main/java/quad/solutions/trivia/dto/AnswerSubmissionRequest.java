package quad.solutions.trivia.dto;

import jakarta.validation.constraints.NotBlank;

public record AnswerSubmissionRequest(
		@NotBlank(message = "Question id is required") String questionId,
		@NotBlank(message = "Selected answer is required") String answer) {
}
