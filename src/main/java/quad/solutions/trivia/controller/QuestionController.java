package quad.solutions.trivia.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import quad.solutions.trivia.dto.CheckAnswersRequest;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.service.QuizService;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class QuestionController {

	private final QuizService quizService;

	@GetMapping("/questions")
	QuizResponse getQuestions(@RequestParam(defaultValue = "5") int amount,
			@RequestParam(required = false) Integer category) {
		return quizService.createQuiz(amount, category);
	}

	@PostMapping(value = "/checkanswers", consumes = APPLICATION_JSON_VALUE)
	CheckAnswersResponse checkAnswers(@Valid @RequestBody CheckAnswersRequest request) {
		return quizService.checkAnswers(request);
	}

}
