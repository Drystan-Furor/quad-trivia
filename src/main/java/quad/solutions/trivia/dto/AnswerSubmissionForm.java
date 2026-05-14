package quad.solutions.trivia.dto;

import jakarta.validation.constraints.NotBlank;

public class AnswerSubmissionForm {

	@NotBlank(message = "Question id is required")
	private String questionId;

	@NotBlank(message = "Selected answer is required")
	private String answer;

	public String getQuestionId() {
		return questionId;
	}

	public void setQuestionId(String questionId) {
		this.questionId = questionId;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

}
