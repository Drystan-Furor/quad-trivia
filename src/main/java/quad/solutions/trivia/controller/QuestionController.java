package quad.solutions.trivia.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import quad.solutions.trivia.dto.CheckAnswersRequest;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.service.QuizService;

@RestController
@Validated
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class QuestionController {

	private final QuizService quizService;

	@GetMapping("/questions")
	QuizResponse getQuestions(@RequestParam(defaultValue = "5") @Min(1) @Max(50) int amount,
			@RequestParam(required = false) @Min(1) Integer category) {
		return quizService.createQuiz(amount, category);
	}

	@PostMapping(value = "/checkanswers", consumes = APPLICATION_JSON_VALUE)
	CheckAnswersResponse checkAnswers(@Valid @RequestBody CheckAnswersRequest request) {
		return quizService.checkAnswers(request);
	}

}
