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
		String message = exception instanceof MethodArgumentNotValidException
				? "Invalid request payload"
				: "Invalid request parameters";
		org.springframework.web.servlet.ModelAndView modelAndView = new org.springframework.web.servlet.ModelAndView("error");
		modelAndView.setStatus(org.springframework.http.HttpStatus.BAD_REQUEST);
		modelAndView.addObject("errorMessage", message);
		return modelAndView;
	}

	@ExceptionHandler(ResponseStatusException.class)
	org.springframework.web.servlet.ModelAndView handleResponseStatus(ResponseStatusException exception) {
		org.springframework.web.servlet.ModelAndView modelAndView = new org.springframework.web.servlet.ModelAndView("error");
		modelAndView.setStatus(exception.getStatusCode());
		modelAndView.addObject("errorMessage", exception.getReason() == null ? "Request failed" : exception.getReason());
		return modelAndView;
	}

	@ExceptionHandler(OpenTriviaRateLimitException.class)
	org.springframework.web.servlet.ModelAndView handleRateLimit(OpenTriviaRateLimitException exception) {
		org.springframework.web.servlet.ModelAndView modelAndView = new org.springframework.web.servlet.ModelAndView("error");
		modelAndView.setStatus(TOO_MANY_REQUESTS);
		modelAndView.addObject("errorMessage", "Trivia service is temporarily busy. Please try again shortly.");
		return modelAndView;
	}

	@ExceptionHandler(OpenTriviaClientException.class)
	org.springframework.web.servlet.ModelAndView handleUpstream(OpenTriviaClientException exception) {
		org.springframework.web.servlet.ModelAndView modelAndView = new org.springframework.web.servlet.ModelAndView("error");
		modelAndView.setStatus(BAD_GATEWAY);
		modelAndView.addObject("errorMessage", "Trivia questions are temporarily unavailable.");
		return modelAndView;
	}

}
