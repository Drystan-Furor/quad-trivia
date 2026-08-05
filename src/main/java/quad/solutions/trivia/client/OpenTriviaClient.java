package quad.solutions.trivia.client;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import quad.solutions.trivia.difficulty.TriviaDifficulty;
import quad.solutions.trivia.model.TriviaQuestion;
import quad.solutions.trivia.type.TriviaType;

@Component
@RequiredArgsConstructor
public class OpenTriviaClient {

	private static final int SUCCESS = 0;
	private static final int INVALID_TOKEN = 3;
	private static final int TOKEN_EMPTY = 4;
	private static final int RATE_LIMITED = 5;
	private static final Duration TOKEN_INACTIVITY_TIMEOUT = Duration.ofHours(6);

	private final RestClient restClient;
	private final Clock clock;

	// Open Trivia DB tokens are stateful and can expire or drain between requests.
	private String sessionToken;
	private Instant lastTokenActivityAt;

	public List<TriviaQuestion> fetchQuestions(int amount, Integer category, TriviaDifficulty difficulty) {
		return fetchQuestions(amount, category, difficulty, TriviaType.ANY);
	}

	public synchronized List<TriviaQuestion> fetchQuestions(int amount, Integer category, TriviaDifficulty difficulty,
			TriviaType type) {
		validateAmount(amount);
		validateCategory(category);
		TriviaDifficulty selectedDifficulty = validateDifficulty(difficulty);
		TriviaType selectedType = validateType(type);

		try {
			String token = getActiveToken();
			TriviaApiResponse response = fetchQuestionsOnce(amount, category, selectedDifficulty, selectedType, token);

			if (response.responseCode() == INVALID_TOKEN) {
				invalidateToken();
				token = getActiveToken();
				response = fetchQuestionsOnce(amount, category, selectedDifficulty, selectedType, token);
			}
			else if (response.responseCode() == TOKEN_EMPTY) {
				token = resetOrReplaceToken(token);
				response = fetchQuestionsOnce(amount, category, selectedDifficulty, selectedType, token);
			}

			if (response.responseCode() == RATE_LIMITED) {
				throw new OpenTriviaRateLimitException("Open Trivia DB rate limit reached");
			}

			if (response.responseCode() != SUCCESS) {
				throw new OpenTriviaClientException("Open Trivia DB returned response_code=" + response.responseCode());
			}

			return response.results().stream()
					.map(this::mapQuestion)
					.toList();
		}
		catch (RestClientException exception) {
			throw new OpenTriviaClientException("Open Trivia DB request failed", exception);
		}
	}

	private void validateAmount(int amount) {
		if (amount < 1 || amount > 50) {
			throw new IllegalArgumentException("Open Trivia DB supports between 1 and 50 questions per request");
		}
	}

	private void validateCategory(Integer category) {
		if (category != null && category < 1) {
			throw new IllegalArgumentException("Category must be a positive identifier");
		}
	}

	private TriviaDifficulty validateDifficulty(TriviaDifficulty difficulty) {
		if (difficulty == null) {
			throw new IllegalArgumentException("Difficulty is required");
		}
		return difficulty;
	}

	private TriviaType validateType(TriviaType type) {
		if (type == null) {
			throw new IllegalArgumentException("Type is required");
		}
		return type;
	}

	private String getActiveToken() {
		if (!StringUtils.hasText(sessionToken) || isExpired()) {
			TokenApiResponse tokenResponse = executeTokenCommand("request", null);
			requireSuccessfulTokenResponse(tokenResponse, "request");
			sessionToken = tokenResponse.token();
		}

		lastTokenActivityAt = clock.instant();
		return sessionToken;
	}

	private boolean isExpired() {
		return lastTokenActivityAt != null
				&& lastTokenActivityAt.plus(TOKEN_INACTIVITY_TIMEOUT).isBefore(clock.instant());
	}

	private String resetOrReplaceToken(String token) {
		TokenApiResponse tokenResponse = executeTokenCommand("reset", token);
		if (tokenResponse.responseCode() == SUCCESS && StringUtils.hasText(tokenResponse.token())) {
			sessionToken = tokenResponse.token();
			lastTokenActivityAt = clock.instant();
			return sessionToken;
		}

		invalidateToken();
		return getActiveToken();
	}

	private void invalidateToken() {
		sessionToken = null;
		lastTokenActivityAt = null;
	}

	private void requireSuccessfulTokenResponse(TokenApiResponse tokenResponse, String command) {
		if (tokenResponse.responseCode() != SUCCESS || !StringUtils.hasText(tokenResponse.token())) {
			throw new OpenTriviaClientException("Open Trivia DB token " + command + " failed");
		}
	}

	private TokenApiResponse executeTokenCommand(String command, String token) {
		String uri = UriComponentsBuilder.fromPath("/api_token.php")
				.queryParam("command", command)
				.queryParamIfPresent("token", java.util.Optional.ofNullable(token))
				.build(true)
				.toUriString();

		TokenApiResponse response = restClient.get()
				.uri(uri)
				.retrieve()
				.body(TokenApiResponse.class);

		if (response == null) {
			throw new OpenTriviaClientException("Open Trivia DB token " + command + " returned no body");
		}

		return response;
	}

	private TriviaApiResponse fetchQuestionsOnce(int amount, Integer category, TriviaDifficulty difficulty, TriviaType type, String token) {
		String uri = buildQuestionsUri(amount, category, difficulty, type, token);
		TriviaApiResponse response = restClient.get()
				.uri(uri)
				.retrieve()
				.body(TriviaApiResponse.class);

		if (response == null) {
			throw new OpenTriviaClientException("Open Trivia DB questions request returned no body");
		}

		lastTokenActivityAt = clock.instant();
		return response;
	}

	private String buildQuestionsUri(int amount, Integer category, TriviaDifficulty difficulty, TriviaType type, String token) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api.php")
				.queryParam("amount", amount)
				.queryParamIfPresent("category", java.util.Optional.ofNullable(category))
				.queryParamIfPresent("difficulty", difficulty.openTriviaValue())
				.queryParamIfPresent("type", type.openTriviaValue())
				.queryParam("encode", "url3986")
				.queryParam("token", token);
		return builder.build(true).toUriString();
	}

	private TriviaQuestion mapQuestion(TriviaQuestionPayload payload) {
		return new TriviaQuestion(
				decode(payload.type()),
				decode(payload.difficulty()),
				decode(payload.category()),
				decode(payload.question()),
				decode(payload.correctAnswer()),
				payload.incorrectAnswers() == null ? List.of() : payload.incorrectAnswers().stream().map(this::decode).toList());
	}

	private String decode(String value) {
		return value == null ? null : URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private record TriviaApiResponse(
			@JsonProperty("response_code") int responseCode,
			List<TriviaQuestionPayload> results) {
	}

	private record TriviaQuestionPayload(
			String type,
			String difficulty,
			String category,
			String question,
			@JsonProperty("correct_answer") String correctAnswer,
			@JsonProperty("incorrect_answers") List<String> incorrectAnswers) {
	}

	private record TokenApiResponse(
			@JsonProperty("response_code") int responseCode,
			String token) {
	}

}
