package quad.solutions.trivia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import quad.solutions.trivia.client.OpenTriviaRateLimitException;
import quad.solutions.trivia.dto.AnswerResultResponse;
import quad.solutions.trivia.dto.AnswerSubmissionRequest;
import quad.solutions.trivia.dto.CheckAnswersRequest;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.dto.QuestionResponse;
import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.service.QuizService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TriviaUiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private QuizService quizService;

	@Test
	void postQuestionsRendersSafeTriviaPage() throws Exception {
		when(quizService.createQuiz(5, null)).thenReturn(new QuizResponse(
				"quiz-123",
				List.of(new QuestionResponse(
						"question-1",
						"multiple",
						"medium",
						"Science &amp; Nature",
						"What is H2O?",
						List.of("Water", "Fire", "Earth", "Air")))));

		mockMvc.perform(post("/questions")
				.with(csrf())
				.contentType(APPLICATION_FORM_URLENCODED)
				.param("amount", "5"))
				.andExpect(status().isOk())
				.andExpect(view().name("quiz"))
				.andExpect(model().attributeExists("quiz"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(content().string(containsString("What is H2O?")))
				.andExpect(content().string(containsString("name=\"answers[0].answer\"")))
				.andExpect(content().string(containsString("value=\"question-1\"")))
				.andExpect(content().string(containsString("data-submit-button")))
				.andExpect(content().string(containsString("data-idle-label")))
				.andExpect(content().string(containsString("data-loading-label")))
				.andExpect(content().string(containsString("data-loading-indicator")))
				.andExpect(content().string(not(containsString("correctAnswer"))))
				.andExpect(content().string(not(containsString("token"))))
				.andExpect(content().string(not(containsString("debug"))));
	}

	@Test
	void postCheckAnswersRendersSafeResultsPage() throws Exception {
		CheckAnswersRequest request = new CheckAnswersRequest(
				"quiz-123",
				List.of(new AnswerSubmissionRequest("question-1", "Water")));
		when(quizService.checkAnswers(eq(request))).thenReturn(new CheckAnswersResponse(
				1,
				1,
				List.of(new AnswerResultResponse("question-1", true))));

		mockMvc.perform(post("/checkanswers")
				.with(csrf())
				.contentType(APPLICATION_FORM_URLENCODED)
				.param("quizId", "quiz-123")
				.param("answers[0].questionId", "question-1")
				.param("answers[0].answer", "Water"))
				.andExpect(status().isOk())
				.andExpect(view().name("results"))
				.andExpect(model().attributeExists("results"))
				.andExpect(content().string(containsString("Score 1 / 1")))
				.andExpect(content().string(not(containsString("correctAnswer"))))
				.andExpect(content().string(not(containsString("token"))))
				.andExpect(content().string(not(containsString("debug"))));
	}

	@Test
	void postQuestionsRendersFriendlyErrorWhenQuizCreationIsRateLimited() throws Exception {
		when(quizService.createQuiz(5, null)).thenThrow(new OpenTriviaRateLimitException("internal detail"));

		mockMvc.perform(post("/questions")
				.with(csrf())
				.contentType(APPLICATION_FORM_URLENCODED)
				.param("amount", "5"))
				.andExpect(status().isTooManyRequests())
				.andExpect(view().name("error"))
				.andExpect(model().attribute("errorMessage", "Trivia service is temporarily busy. Please try again shortly."))
				.andExpect(content().string(not(containsString("internal detail"))));
	}

	@Test
	void postQuestionsRejectsInvalidAmountsWithFriendlyMessage() throws Exception {
		mockMvc.perform(post("/questions")
				.with(csrf())
				.contentType(APPLICATION_FORM_URLENCODED)
				.param("amount", "51"))
				.andExpect(status().isBadRequest())
				.andExpect(view().name("error"))
				.andExpect(model().attribute("errorMessage", "Invalid request parameters"));
	}

}
