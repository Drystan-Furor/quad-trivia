package quad.solutions.trivia.controller;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;
import quad.solutions.trivia.client.OpenTriviaClientException;
import quad.solutions.trivia.client.OpenTriviaRateLimitException;

@ControllerAdvice(assignableTypes = TriviaUiController.class)
class TriviaUiControllerAdvice {

	@ExceptionHandler({
			ConstraintViolationException.class,
			HandlerMethodValidationException.class,
			MethodArgumentNotValidException.class })
	org.springframework.web.servlet.ModelAndView handleValidation(Exception exception) {
		org.springframework.web.servlet.ModelAndView modelAndView = new org.springframework.web.servlet.ModelAndView("error");
		modelAndView.setStatus(org.springframework.http.HttpStatus.BAD_REQUEST);
		modelAndView.addObject("errorMessage", "Controleer je invoer en probeer opnieuw.");
		return modelAndView;
	}

	@ExceptionHandler(ResponseStatusException.class)
	org.springframework.web.servlet.ModelAndView handleResponseStatus(ResponseStatusException exception) {
		org.springframework.web.servlet.ModelAndView modelAndView = new org.springframework.web.servlet.ModelAndView("error");
		modelAndView.setStatus(exception.getStatusCode());
		modelAndView.addObject("errorMessage", localizedMessage(exception));
		return modelAndView;
	}

	@ExceptionHandler(OpenTriviaRateLimitException.class)
	org.springframework.web.servlet.ModelAndView handleRateLimit(OpenTriviaRateLimitException exception) {
		org.springframework.web.servlet.ModelAndView modelAndView = new org.springframework.web.servlet.ModelAndView("error");
		modelAndView.setStatus(TOO_MANY_REQUESTS);
		modelAndView.addObject("errorMessage", "De triviadienst is even druk. Probeer het zo opnieuw.");
		return modelAndView;
	}

	@ExceptionHandler(OpenTriviaClientException.class)
	org.springframework.web.servlet.ModelAndView handleUpstream(OpenTriviaClientException exception) {
		org.springframework.web.servlet.ModelAndView modelAndView = new org.springframework.web.servlet.ModelAndView("error");
		modelAndView.setStatus(BAD_GATEWAY);
		modelAndView.addObject("errorMessage", "De triviavragen zijn tijdelijk niet beschikbaar.");
		return modelAndView;
	}

	private String localizedMessage(ResponseStatusException exception) {
		return switch (exception.getStatusCode().value()) {
			case 400 -> "Controleer je invoer en probeer opnieuw.";
			case 404 -> "Deze quiz is verlopen of niet meer beschikbaar.";
			case 409 -> "Deze quiz is al ingeleverd.";
			case 429 -> "Wacht enkele seconden voordat je een nieuwe quiz start.";
			default -> "Het verzoek kon niet worden verwerkt.";
		};
	}

}
