package quad.solutions.trivia.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerSubmissionForm {

	@NotBlank(message = "Question id is required")
	private String questionId;

	@NotBlank(message = "Selected answer is required")
	private String answer;

}
