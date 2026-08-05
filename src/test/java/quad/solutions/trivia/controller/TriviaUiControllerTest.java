package quad.solutions.trivia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
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
import org.springframework.web.server.ResponseStatusException;

import quad.solutions.trivia.client.OpenTriviaRateLimitException;
import quad.solutions.trivia.dto.AnswerResultResponse;
import quad.solutions.trivia.dto.AnswerSubmissionRequest;
import quad.solutions.trivia.dto.CheckAnswersRequest;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.dto.QuestionResponse;
import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.difficulty.TriviaDifficulty;
import quad.solutions.trivia.service.QuizService;
import quad.solutions.trivia.type.TriviaType;

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
		when(quizService.createQuiz(5, null, TriviaDifficulty.ANY)).thenReturn(new QuizResponse(
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
				.andExpect(content().string(containsString("data-quiz-form")))
				.andExpect(content().string(containsString("data-quiz-question")))
				.andExpect(content().string(containsString("role=\"progressbar\"")))
				.andExpect(content().string(containsString("Vraag 1 van 1")))
				.andExpect(content().string(containsString("href=\"/css/app.css\"")))
				.andExpect(content().string(not(containsString("cdn.tailwindcss.com"))))
				.andExpect(content().string(containsString("<script defer src=\"/js/global.js\"></script>")))
				.andExpect(content().string(not(containsString("<main class=\"max-w-4xl mx-auto p-4\">\n<script>"))))
				.andExpect(content().string(not(containsString("document.addEventListener(\"DOMContentLoaded\""))))
				.andExpect(content().string(not(containsString("correctAnswer"))))
				.andExpect(content().string(not(containsString("token"))))
				.andExpect(content().string(not(containsString("debug"))));
	}

	@Test
	void postQuestionsPassesSelectedCategoryToService() throws Exception {
		when(quizService.createQuiz(5, 18, TriviaDifficulty.ANY)).thenReturn(new QuizResponse(
				"quiz-123",
				List.of(new QuestionResponse(
						"question-1",
						"multiple",
						"medium",
						"Science: Computers",
						"What does CPU stand for?",
						List.of("Central Processing Unit", "Computer Personal Unit")))));

		mockMvc.perform(post("/questions")
				.with(csrf())
				.contentType(APPLICATION_FORM_URLENCODED)
				.param("amount", "5")
				.param("category", "18"))
				.andExpect(status().isOk())
				.andExpect(view().name("quiz"))
				.andExpect(content().string(containsString("Science: Computers")));

		verify(quizService).createQuiz(5, 18, TriviaDifficulty.ANY);
	}

	@Test
	void postQuestionsPassesSelectedDifficultyToService() throws Exception {
		when(quizService.createQuiz(5, null, TriviaDifficulty.HARD)).thenReturn(new QuizResponse(
				"quiz-123",
				List.of(new QuestionResponse(
						"question-1",
						"multiple",
						"hard",
						"Science: Computers",
						"What does CPU stand for?",
						List.of("Central Processing Unit", "Computer Personal Unit")))));

		mockMvc.perform(post("/questions")
				.with(csrf())
				.contentType(APPLICATION_FORM_URLENCODED)
				.param("amount", "5")
				.param("difficulty", "hard"))
				.andExpect(status().isOk())
				.andExpect(view().name("quiz"))
				.andExpect(content().string(containsString("Moeilijk")));

		verify(quizService).createQuiz(5, null, TriviaDifficulty.HARD);
	}

	@Test
	void postQuestionsPassesSelectedTriviaTypeToService() throws Exception {
		when(quizService.createQuiz(5, null, TriviaDifficulty.ANY, TriviaType.BOOLEAN)).thenReturn(new QuizResponse(
				"quiz-123",
				List.of(new QuestionResponse(
						"question-1",
						"boolean",
						"easy",
						"Science: Computers",
						"Does CPU stand for Central Processing Unit?",
						List.of("True", "False")))));

		mockMvc.perform(post("/questions")
				.with(csrf())
				.contentType(APPLICATION_FORM_URLENCODED)
				.param("amount", "5")
				.param("type", "boolean"))
				.andExpect(status().isOk())
				.andExpect(view().name("quiz"));

		verify(quizService).createQuiz(5, null, TriviaDifficulty.ANY, TriviaType.BOOLEAN);
	}

	@Test
	void postCheckAnswersRendersSafeResultsPage() throws Exception {
		CheckAnswersRequest request = new CheckAnswersRequest(
				"quiz-123",
				List.of(new AnswerSubmissionRequest("question-1", "Water")));
		when(quizService.checkAnswers(eq(request))).thenReturn(new CheckAnswersResponse(
				1,
				1,
				List.of(new AnswerResultResponse("question-1", "What is H2O?", "Water", "Water", true))));

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
				.andExpect(content().string(containsString("data-result-review")))
				.andExpect(content().string(not(containsString("correctAnswer"))))
				.andExpect(content().string(not(containsString("token"))))
				.andExpect(content().string(not(containsString("debug"))));
	}

	@Test
	void postQuestionsRendersFriendlyErrorWhenQuizCreationIsRateLimited() throws Exception {
		when(quizService.createQuiz(5, null, TriviaDifficulty.ANY)).thenThrow(new OpenTriviaRateLimitException("internal detail"));

		mockMvc.perform(post("/questions")
				.with(csrf())
				.contentType(APPLICATION_FORM_URLENCODED)
				.param("amount", "5"))
				.andExpect(status().isTooManyRequests())
				.andExpect(view().name("error"))
				.andExpect(model().attribute("errorMessage", "De triviadienst is even druk. Probeer het zo opnieuw."))
				.andExpect(content().string(not(containsString("internal detail"))));
	}

	@Test
	void postQuestionsRejectsInvalidAmountsWithFriendlyMessage() throws Exception {
		when(quizService.createQuiz(51, null, TriviaDifficulty.ANY))
				.thenThrow(new ResponseStatusException(BAD_REQUEST, "Invalid request parameters"));

		mockMvc.perform(post("/questions")
				.with(csrf())
				.contentType(APPLICATION_FORM_URLENCODED)
				.param("amount", "51"))
				.andExpect(status().isBadRequest())
				.andExpect(view().name("error"))
				.andExpect(model().attribute("errorMessage", "Controleer je invoer en probeer opnieuw."));
	}

	@Test
	void postQuestionsRejectsNonPositiveCategoriesWithFriendlyMessage() throws Exception {
		when(quizService.createQuiz(5, 0, TriviaDifficulty.ANY))
				.thenThrow(new ResponseStatusException(BAD_REQUEST, "Invalid request parameters"));

		mockMvc.perform(post("/questions")
				.with(csrf())
				.contentType(APPLICATION_FORM_URLENCODED)
				.param("amount", "5")
				.param("category", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(view().name("error"))
				.andExpect(model().attribute("errorMessage", "Controleer je invoer en probeer opnieuw."));
	}

	@Test
	void postQuestionsRejectsUnknownDifficultiesWithFriendlyMessage() throws Exception {
		mockMvc.perform(post("/questions")
				.with(csrf())
				.contentType(APPLICATION_FORM_URLENCODED)
				.param("amount", "5")
				.param("difficulty", "expert"))
				.andExpect(status().isBadRequest())
				.andExpect(view().name("error"))
				.andExpect(model().attribute("errorMessage", "Controleer je invoer en probeer opnieuw."));
	}

	@Test
	void postQuestionsRejectsUnknownTriviaTypesWithFriendlyMessage() throws Exception {
		mockMvc.perform(post("/questions")
				.with(csrf())
				.contentType(APPLICATION_FORM_URLENCODED)
				.param("amount", "5")
				.param("type", "essay"))
				.andExpect(status().isBadRequest())
				.andExpect(view().name("error"))
				.andExpect(model().attribute("errorMessage", "Controleer je invoer en probeer opnieuw."));
	}

}
