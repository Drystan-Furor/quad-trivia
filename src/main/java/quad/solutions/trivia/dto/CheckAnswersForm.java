package quad.solutions.trivia.dto;

import java.util.ArrayList;
import java.util.List;

public class CheckAnswersForm {

	private String quizId;
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
