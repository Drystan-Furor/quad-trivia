package quad.solutions.trivia.controller;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;
import quad.solutions.trivia.client.OpenTriviaClientException;
import quad.solutions.trivia.client.OpenTriviaRateLimitException;
import quad.solutions.trivia.dto.ErrorResponse;

@RestControllerAdvice(assignableTypes = QuestionController.class)
class QuestionControllerAdvice {

	@ExceptionHandler({
			ConstraintViolationException.class,
			HandlerMethodValidationException.class,
			MethodArgumentNotValidException.class })
	ResponseEntity<ErrorResponse> handleValidationExceptions(Exception exception) {
		String message = exception instanceof MethodArgumentNotValidException
				? "Invalid request payload"
				: "Invalid request parameters";
		return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(message));
	}

	@ExceptionHandler(ResponseStatusException.class)
	ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException exception) {
		return ResponseEntity.status(exception.getStatusCode())
				.body(new ErrorResponse(exception.getReason() == null ? "Request failed" : exception.getReason()));
	}

	@ExceptionHandler(OpenTriviaRateLimitException.class)
	ResponseEntity<ErrorResponse> handleRateLimit(OpenTriviaRateLimitException exception) {
		return ResponseEntity.status(TOO_MANY_REQUESTS)
				.body(new ErrorResponse("Trivia service is temporarily busy. Please try again shortly."));
	}

	@ExceptionHandler(OpenTriviaClientException.class)
	ResponseEntity<ErrorResponse> handleUpstream(OpenTriviaClientException exception) {
		return ResponseEntity.status(BAD_GATEWAY)
				.body(new ErrorResponse("Trivia questions are temporarily unavailable."));
	}

}
