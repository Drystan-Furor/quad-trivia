package quad.solutions.trivia.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.service.QuizService;

@RestController
class QuestionController {

	private final QuizService quizService;

	QuestionController(QuizService quizService) {
		this.quizService = quizService;
	}

	@GetMapping("/questions")
	QuizResponse getQuestions(@RequestParam(defaultValue = "5") int amount,
			@RequestParam(required = false) Integer category) {
		return quizService.createQuiz(amount, category);
	}

}
