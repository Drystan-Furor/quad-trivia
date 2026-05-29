package quad.solutions.trivia.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import quad.solutions.trivia.category.TriviaCategory;
import quad.solutions.trivia.difficulty.TriviaDifficulty;

@Controller
class HomeController {

	@GetMapping("/")
	String home(Model model) {
		model.addAttribute("categories", TriviaCategory.options());
		model.addAttribute("difficulties", TriviaDifficulty.options());
		return "home";
	}

}
