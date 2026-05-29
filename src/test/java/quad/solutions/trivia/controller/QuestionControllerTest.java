package quad.solutions.trivia.controller;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import quad.solutions.trivia.client.OpenTriviaClientException;
import quad.solutions.trivia.client.OpenTriviaRateLimitException;
import quad.solutions.trivia.dto.AnswerResultResponse;
import quad.solutions.trivia.dto.AnswerSubmissionRequest;
import quad.solutions.trivia.dto.CheckAnswersRequest;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.dto.QuestionResponse;
import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.difficulty.TriviaDifficulty;
import quad.solutions.trivia.service.QuizService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuestionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private QuizService quizService;

	@Test
	void getQuestionsReturnsSafeClientPayload() throws Exception {
		when(quizService.createQuiz(5, null, TriviaDifficulty.ANY)).thenReturn(new QuizResponse(
				"quiz-123",
				List.of(new QuestionResponse(
						"question-1",
						"multiple",
						"medium",
						"Science &amp; Nature",
						"What is H2O?",
						List.of("Fire", "Earth", "Air", "Water")))));

		mockMvc.perform(get("/questions"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(jsonPath("$.quizId").value("quiz-123"))
				.andExpect(jsonPath("$.questions[0].id").value("question-1"))
				.andExpect(jsonPath("$.questions[0].question").value("What is H2O?"))
				.andExpect(jsonPath("$.questions[0].options[0]").value("Fire"))
				.andExpect(jsonPath("$.questions[0].options[3]").value("Water"))
				.andExpect(jsonPath("$.questions[0].correctAnswer").doesNotExist())
				.andExpect(jsonPath("$.token").doesNotExist());
	}

	@Test
	void getQuestionsPassesSelectedCategoryToService() throws Exception {
		when(quizService.createQuiz(5, 18, TriviaDifficulty.ANY)).thenReturn(new QuizResponse(
				"quiz-123",
				List.of(new QuestionResponse(
						"question-1",
						"multiple",
						"medium",
						"Science: Computers",
						"What does CPU stand for?",
						List.of("Central Processing Unit", "Computer Personal Unit")))));

		mockMvc.perform(get("/questions")
				.param("amount", "5")
				.param("category", "18"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quizId").value("quiz-123"))
				.andExpect(jsonPath("$.questions[0].category").value("Science: Computers"));

		verify(quizService).createQuiz(5, 18, TriviaDifficulty.ANY);
	}

	@Test
	void getQuestionsPassesSelectedDifficultyToService() throws Exception {
		when(quizService.createQuiz(5, null, TriviaDifficulty.EASY)).thenReturn(new QuizResponse(
				"quiz-123",
				List.of(new QuestionResponse(
						"question-1",
						"multiple",
						"easy",
						"Science: Computers",
						"What does CPU stand for?",
						List.of("Central Processing Unit", "Computer Personal Unit")))));

		mockMvc.perform(get("/questions")
				.param("amount", "5")
				.param("difficulty", "easy"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quizId").value("quiz-123"))
				.andExpect(jsonPath("$.questions[0].difficulty").value("easy"));

		verify(quizService).createQuiz(5, null, TriviaDifficulty.EASY);
	}

	@Test
	void postCheckAnswersReturnsServerSideEvaluation() throws Exception {
		CheckAnswersRequest request = new CheckAnswersRequest(
				"quiz-123",
				List.of(new AnswerSubmissionRequest("question-1", "Water")));
		when(quizService.checkAnswers(request)).thenReturn(new CheckAnswersResponse(
				1,
				1,
				List.of(new AnswerResultResponse("question-1", true))));

		mockMvc.perform(post("/checkanswers")
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(jsonPath("$.score").value(1))
				.andExpect(jsonPath("$.totalQuestions").value(1))
				.andExpect(jsonPath("$.results[0].questionId").value("question-1"))
				.andExpect(jsonPath("$.results[0].correct").value(true))
				.andExpect(jsonPath("$.results[0].correctAnswer").doesNotExist())
				.andExpect(jsonPath("$.token").doesNotExist());
	}

	@Test
	void getQuestionsRejectsAmountsAboveTheSupportedLimit() throws Exception {
		when(quizService.createQuiz(51, null, TriviaDifficulty.ANY))
				.thenThrow(new ResponseStatusException(BAD_REQUEST, "Invalid request parameters"));

		mockMvc.perform(get("/questions").param("amount", "51"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid request parameters"));
	}

	@Test
	void getQuestionsRejectsNonPositiveCategories() throws Exception {
		when(quizService.createQuiz(5, 0, TriviaDifficulty.ANY))
				.thenThrow(new ResponseStatusException(BAD_REQUEST, "Invalid request parameters"));

		mockMvc.perform(get("/questions").param("category", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid request parameters"));
	}

	@Test
	void getQuestionsRejectsUnknownDifficulties() throws Exception {
		mockMvc.perform(get("/questions").param("difficulty", "expert"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid request parameters"));
	}

	@Test
	void postCheckAnswersRejectsIncompletePayload() throws Exception {
		mockMvc.perform(post("/checkanswers")
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content("""
						{"quizId":"quiz-123","answers":[]}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid request payload"));
	}

	@Test
	void postCheckAnswersMapsUnavailableQuizSessionToNotFound() throws Exception {
		CheckAnswersRequest request = new CheckAnswersRequest(
				"quiz-missing",
				List.of(new AnswerSubmissionRequest("question-1", "Water")));
		when(quizService.checkAnswers(request))
				.thenThrow(new ResponseStatusException(NOT_FOUND, "Quiz session is not available"));

		mockMvc.perform(post("/checkanswers")
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Quiz session is not available"));
	}

	@Test
	void getQuestionsMapsUpstreamRateLimitsToControlledResponse() throws Exception {
		when(quizService.createQuiz(5, null, TriviaDifficulty.ANY)).thenThrow(new OpenTriviaRateLimitException("upstream detail"));

		mockMvc.perform(get("/questions"))
				.andExpect(status().is(TOO_MANY_REQUESTS.value()))
				.andExpect(jsonPath("$.message").value("Trivia service is temporarily busy. Please try again shortly."));
	}

	@Test
	void getQuestionsHidesInternalUpstreamFailures() throws Exception {
		when(quizService.createQuiz(5, null, TriviaDifficulty.ANY)).thenThrow(new OpenTriviaClientException("internal upstream detail"));

		mockMvc.perform(get("/questions"))
				.andExpect(status().is(BAD_GATEWAY.value()))
				.andExpect(jsonPath("$.message").value("Trivia questions are temporarily unavailable."));
	}

}
