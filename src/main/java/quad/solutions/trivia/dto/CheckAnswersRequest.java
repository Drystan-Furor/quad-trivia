package quad.solutions.trivia.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CheckAnswersRequest(
		@NotBlank(message = "Quiz session is required") String quizId,
		@NotEmpty(message = "At least one answer is required") List<@Valid @NotNull AnswerSubmissionRequest> answers) {
}
