package quad.solutions.trivia.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import quad.solutions.trivia.dto.CheckAnswersRequest;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.difficulty.TriviaDifficulty;
import quad.solutions.trivia.service.QuizService;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class QuestionController {

	private final QuizService quizService;

	@GetMapping("/questions")
	QuizResponse getQuestions(@RequestParam(defaultValue = "5") int amount,
			@RequestParam(required = false) Integer category,
			@RequestParam(required = false) String difficulty) {
		return quizService.createQuiz(amount, category, parseDifficulty(difficulty));
	}

	@PostMapping(value = "/checkanswers", consumes = APPLICATION_JSON_VALUE)
	CheckAnswersResponse checkAnswers(@Valid @RequestBody CheckAnswersRequest request) {
		return quizService.checkAnswers(request);
	}

	private TriviaDifficulty parseDifficulty(String difficulty) {
		try {
			return TriviaDifficulty.fromRequestValue(difficulty);
		}
		catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(BAD_REQUEST, "Invalid request parameters", exception);
		}
	}

}
