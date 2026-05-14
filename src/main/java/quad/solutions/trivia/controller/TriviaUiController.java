package quad.solutions.trivia.controller;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import quad.solutions.trivia.dto.CheckAnswersForm;
import quad.solutions.trivia.dto.CheckAnswersResponse;
import quad.solutions.trivia.dto.QuizResponse;
import quad.solutions.trivia.service.QuizService;

@Controller
@Validated
class TriviaUiController {

	private final QuizService quizService;

	TriviaUiController(QuizService quizService) {
		this.quizService = quizService;
	}

	@PostMapping(value = "/questions", consumes = APPLICATION_FORM_URLENCODED_VALUE)
	String startQuiz(@RequestParam(defaultValue = "5") @Min(1) @Max(50) int amount,
			@RequestParam(required = false) @Min(1) Integer category,
			Model model) {
		QuizResponse quiz = quizService.createQuiz(amount, category);
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

}
