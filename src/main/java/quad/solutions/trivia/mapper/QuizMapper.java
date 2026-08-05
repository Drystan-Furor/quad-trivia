package quad.solutions.trivia.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
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

	private final UnaryOperator<List<String>> optionShuffler;

	public QuizMapper() {
		this(QuizMapper::shuffleOptions);
	}

	public QuizMapper(UnaryOperator<List<String>> optionShuffler) {
		this.optionShuffler = optionShuffler;
	}

	public IssuedQuestion toIssuedQuestion(TriviaQuestion upstreamQuestion, String questionId) {
		List<String> sanitizedOptions = sanitizeOptions(upstreamQuestion);
		List<String> shuffledOptions = List.copyOf(optionShuffler.apply(sanitizedOptions));
		return new IssuedQuestion(
				new QuestionResponse(
						questionId,
						sanitize(upstreamQuestion.type()),
						sanitize(upstreamQuestion.difficulty()),
						sanitize(upstreamQuestion.category()),
						sanitize(upstreamQuestion.question()),
						shuffledOptions),
				new StoredQuestion(
						questionId,
						sanitize(upstreamQuestion.correctAnswer()),
						List.copyOf(shuffledOptions)));
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

	private static List<String> shuffleOptions(List<String> options) {
		List<String> shuffledOptions = new ArrayList<>(options);
		Collections.shuffle(shuffledOptions);
		return List.copyOf(shuffledOptions);
	}

	public record IssuedQuestion(
			QuestionResponse response,
			StoredQuestion storedQuestion) {
	}

}
