package quad.solutions.trivia.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import quad.solutions.trivia.dto.AnswerResultResponse;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.dto.QuestionResponse;
import quad.solutions.trivia.model.TriviaQuestion;
import quad.solutions.trivia.session.StoredQuestion;

@Component
public class QuizMapper {

	public IssuedQuestion toIssuedQuestion(TriviaQuestion upstreamQuestion, String questionId) {
		List<String> sanitizedOptions = sanitizeOptions(upstreamQuestion);
		return new IssuedQuestion(
				new QuestionResponse(
						questionId,
						sanitize(upstreamQuestion.type()),
						sanitize(upstreamQuestion.difficulty()),
						sanitize(upstreamQuestion.category()),
						sanitize(upstreamQuestion.question()),
						sanitizedOptions),
				new StoredQuestion(
						questionId,
						sanitize(upstreamQuestion.correctAnswer()),
						List.copyOf(sanitizedOptions)));
	}

	public CheckAnswersResponse toCheckAnswersResponse(List<StoredQuestion> questions, Map<String, String> submittedAnswers) {
		List<AnswerResultResponse> results = questions.stream()
				.map(question -> new AnswerResultResponse(
						question.id(),
						question.correctAnswer().equals(submittedAnswers.get(question.id()))))
				.toList();
		int score = (int) results.stream().filter(AnswerResultResponse::correct).count();
		return new CheckAnswersResponse(score, questions.size(), results);
	}

	private List<String> sanitizeOptions(TriviaQuestion upstreamQuestion) {
		return Stream.concat(
				Stream.of(upstreamQuestion.correctAnswer()),
				upstreamQuestion.incorrectAnswers().stream())
				.map(this::sanitize)
				.toList();
	}

	private String sanitize(String value) {
		return value == null ? null : HtmlUtils.htmlEscape(value);
	}

	public record IssuedQuestion(
			QuestionResponse response,
			StoredQuestion storedQuestion) {
	}

}
