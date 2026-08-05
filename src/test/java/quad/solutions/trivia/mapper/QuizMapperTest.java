package quad.solutions.trivia.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import quad.solutions.trivia.dto.AnswerResultResponse;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.model.TriviaQuestion;
import quad.solutions.trivia.session.StoredQuestion;

class QuizMapperTest {

	private final QuizMapper quizMapper = new QuizMapper(QuizMapperTest::moveFirstAnswerToLast);

	@Test
	void toIssuedQuestionSanitizesAndShufflesQuestionResponseAndStoredQuestion() {
		TriviaQuestion question = new TriviaQuestion(
				"multiple",
				"hard",
				"Science & Nature",
				"<script>alert('x')</script> & already decoded",
				"<b>Water</b>",
				List.of("<i>Fire</i>", "Earth & Wind", "\"Air\""));

		QuizMapper.IssuedQuestion issuedQuestion = quizMapper.toIssuedQuestion(question, "question-1");

		assertThat(issuedQuestion.response().id()).isEqualTo("question-1");
		assertThat(issuedQuestion.response().type()).isEqualTo("multiple");
		assertThat(issuedQuestion.response().difficulty()).isEqualTo("hard");
		assertThat(issuedQuestion.response().category()).isEqualTo("Science &amp; Nature");
		assertThat(issuedQuestion.response().question())
				.isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; already decoded");
		assertThat(issuedQuestion.response().options()).containsExactly(
				"&lt;i&gt;Fire&lt;/i&gt;",
				"Earth &amp; Wind",
				"&quot;Air&quot;",
				"&lt;b&gt;Water&lt;/b&gt;");
		assertThat(issuedQuestion.storedQuestion()).isEqualTo(new StoredQuestion(
				"question-1",
				"&lt;b&gt;Water&lt;/b&gt;",
				List.of("&lt;i&gt;Fire&lt;/i&gt;", "Earth &amp; Wind", "&quot;Air&quot;", "&lt;b&gt;Water&lt;/b&gt;")));
		assertThat(issuedQuestion.response().options())
				.containsExactlyInAnyOrder("&lt;b&gt;Water&lt;/b&gt;", "&lt;i&gt;Fire&lt;/i&gt;", "Earth &amp; Wind", "&quot;Air&quot;");
		assertThat(issuedQuestion.response().options().getFirst()).isNotEqualTo(issuedQuestion.storedQuestion().correctAnswer());
	}

	@Test
	void toCheckAnswersResponseMapsStoredQuestionsToScoreAndResultFlags() {
		List<StoredQuestion> questions = List.of(
				new StoredQuestion("question-1", "Water", List.of("Water", "Fire")),
				new StoredQuestion("question-2", "Earth", List.of("Earth", "Air")));

		CheckAnswersResponse response = quizMapper.toCheckAnswersResponse(questions, Map.of(
				"question-1", "Water",
				"question-2", "Air"));

		assertThat(response.score()).isEqualTo(1);
		assertThat(response.totalQuestions()).isEqualTo(2);
		assertThat(response.results()).containsExactly(
				new AnswerResultResponse("question-1", true),
				new AnswerResultResponse("question-2", false));
	}

	@Test
	void toIssuedQuestionCopiesMutableShufflerOutput() {
		AtomicReference<List<String>> mutableOutput = new AtomicReference<>();
		QuizMapper mapper = new QuizMapper(options -> {
			List<String> shuffled = new ArrayList<>(options);
			mutableOutput.set(shuffled);
			return shuffled;
		});
		TriviaQuestion question = new TriviaQuestion(
				"boolean", "easy", "Science", "Is water wet?", "True", List.of("False"));

		QuizMapper.IssuedQuestion issuedQuestion = mapper.toIssuedQuestion(question, "question-1");
		mutableOutput.get().set(0, "tampered");

		assertThat(issuedQuestion.response().options()).containsExactly("True", "False");
		assertThat(issuedQuestion.storedQuestion().options()).containsExactly("True", "False");
	}

	private static List<String> moveFirstAnswerToLast(List<String> options) {
		List<String> shuffled = new ArrayList<>(options);
		Collections.rotate(shuffled, -1);
		return shuffled;
	}

}
