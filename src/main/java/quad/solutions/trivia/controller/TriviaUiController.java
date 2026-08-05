package quad.solutions.trivia.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import quad.solutions.trivia.dto.CheckAnswersForm;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.difficulty.TriviaDifficulty;
import quad.solutions.trivia.service.QuizService;
import quad.solutions.trivia.type.TriviaType;

@Controller
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class TriviaUiController {

	private final QuizService quizService;

	@PostMapping(value = "/questions", consumes = APPLICATION_FORM_URLENCODED_VALUE)
	String startQuiz(@RequestParam(defaultValue = "5") int amount,
			@RequestParam(required = false) Integer category,
			@RequestParam(required = false) String difficulty,
			@RequestParam(required = false) String type,
			Model model) {
		TriviaDifficulty selectedDifficulty = parseDifficulty(difficulty);
		TriviaType selectedType = parseType(type);
		QuizResponse quiz = selectedType == TriviaType.ANY
				? quizService.createQuiz(amount, category, selectedDifficulty)
				: quizService.createQuiz(amount, category, selectedDifficulty, selectedType);
		model.addAttribute("quiz", quiz);
		model.addAttribute("answerForm", new CheckAnswersForm());
		return "quiz";
	}

	@PostMapping(value = "/checkanswers", consumes = APPLICATION_FORM_URLENCODED_VALUE)
	String checkAnswers(@Valid @ModelAttribute CheckAnswersForm answerForm, Model model) {
		CheckAnswersResponse results = quizService.checkAnswers(answerForm.toRequest());
		model.addAttribute("results", results);
		return "results";
	}

	private TriviaDifficulty parseDifficulty(String difficulty) {
		try {
			return TriviaDifficulty.fromRequestValue(difficulty);
		}
		catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(BAD_REQUEST, "Invalid request parameters", exception);
		}
	}

	private TriviaType parseType(String type) {
		try {
			return TriviaType.fromRequestValue(type);
		}
		catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(BAD_REQUEST, "Invalid request parameters", exception);
		}
	}

}
