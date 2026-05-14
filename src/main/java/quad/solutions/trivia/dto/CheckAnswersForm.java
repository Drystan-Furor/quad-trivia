package quad.solutions.trivia.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public class CheckAnswersForm {

	@NotBlank(message = "Quiz session is required")
	private String quizId;

	@Valid
	@NotEmpty(message = "At least one answer is required")
	private List<AnswerSubmissionForm> answers = new ArrayList<>();

	public String getQuizId() {
		return quizId;
	}

	public void setQuizId(String quizId) {
		this.quizId = quizId;
	}

	public List<AnswerSubmissionForm> getAnswers() {
		return answers;
	}

	public void setAnswers(List<AnswerSubmissionForm> answers) {
		this.answers = answers;
	}

	public CheckAnswersRequest toRequest() {
		List<AnswerSubmissionRequest> submittedAnswers = answers == null ? List.of() : answers.stream()
				.map(answer -> new AnswerSubmissionRequest(answer.getQuestionId(), answer.getAnswer()))
				.toList();
		return new CheckAnswersRequest(quizId, submittedAnswers);
	}

}
