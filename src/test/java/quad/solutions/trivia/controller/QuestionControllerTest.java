package quad.solutions.trivia.controller;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

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
class QuestionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private QuizService quizService;

	@Test
	void getQuestionsReturnsSafeClientPayload() throws Exception {
		when(quizService.createQuiz(5, null)).thenReturn(new QuizResponse(
				"quiz-123",
				List.of(new QuestionResponse(
						"question-1",
						"multiple",
						"medium",
						"Science &amp; Nature",
						"What is H2O?",
						List.of("Water", "Fire", "Earth", "Air")))));

		mockMvc.perform(get("/questions"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(jsonPath("$.quizId").value("quiz-123"))
				.andExpect(jsonPath("$.questions[0].id").value("question-1"))
				.andExpect(jsonPath("$.questions[0].question").value("What is H2O?"))
				.andExpect(jsonPath("$.questions[0].options[0]").value("Water"))
				.andExpect(jsonPath("$.questions[0].correctAnswer").doesNotExist())
				.andExpect(jsonPath("$.token").doesNotExist());
	}

	@Test
	void postCheckAnswersReturnsServerSideEvaluation() throws Exception {
		CheckAnswersRequest request = new CheckAnswersRequest(
				"quiz-123",
				List.of(
						new AnswerSubmissionRequest("question-1", "Water"),
						new AnswerSubmissionRequest("question-2", "Mars")));

		when(quizService.checkAnswers(request)).thenReturn(new CheckAnswersResponse(
				1,
				2,
				List.of(
						new AnswerResultResponse("question-1", true),
						new AnswerResultResponse("question-2", false))));

		mockMvc.perform(post("/checkanswers")
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(jsonPath("$.score").value(1))
				.andExpect(jsonPath("$.totalQuestions").value(2))
				.andExpect(jsonPath("$.results[0].questionId").value("question-1"))
				.andExpect(jsonPath("$.results[0].correct").value(true))
				.andExpect(jsonPath("$.results[1].questionId").value("question-2"))
				.andExpect(jsonPath("$.results[1].correct").value(false))
				.andExpect(jsonPath("$.results[0].correctAnswer").doesNotExist());
	}

}
