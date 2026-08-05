package quad.solutions.trivia.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import quad.solutions.trivia.difficulty.TriviaDifficulty;
import quad.solutions.trivia.model.TriviaQuestion;
import quad.solutions.trivia.type.TriviaType;

class OpenTriviaClientTest {

	private RestClient.Builder restClientBuilder;
	private MockRestServiceServer server;
	private MutableClock clock;

	@BeforeEach
	void setUp() {
		restClientBuilder = RestClient.builder().baseUrl("https://opentdb.com");
		server = MockRestServiceServer.bindTo(restClientBuilder).ignoreExpectOrder(false).build();
		clock = new MutableClock(Instant.parse("2026-05-14T09:00:00Z"));
	}

	@Test
	void fetchQuestionsReusesTokenUntilItExpiresFromInactivity() {
		server.expect(requestTo("https://opentdb.com/api_token.php?command=request"))
				.andRespond(json("""
						{"response_code":0,"response_message":"Token Generated Successfully!","token":"session-token-1"}
						"""));
		server.expect(requestTo(
				"https://opentdb.com/api.php?amount=2&encode=url3986&token=session-token-1"))
				.andRespond(json("""
						{
						  "response_code":0,
						  "results":[
						    {
						      "type":"multiple",
						      "difficulty":"medium",
						      "category":"Science%20%26%20Nature",
						      "question":"What%20is%20H2O%3F",
						      "correct_answer":"Water",
						      "incorrect_answers":["Fire","Earth","Air"]
						    }
						  ]
						}
						"""));
		server.expect(requestTo(
				"https://opentdb.com/api.php?amount=1&category=12&encode=url3986&token=session-token-1"))
				.andRespond(json("""
						{
						  "response_code":0,
						  "results":[
						    {
						      "type":"boolean",
						      "difficulty":"easy",
						      "category":"Music",
						      "question":"Is%20Jazz%20a%20genre%3F",
						      "correct_answer":"True",
						      "incorrect_answers":["False"]
						    }
						  ]
						}
						"""));
		server.expect(requestTo("https://opentdb.com/api_token.php?command=request"))
				.andRespond(json("""
						{"response_code":0,"response_message":"Token Generated Successfully!","token":"session-token-2"}
						"""));
		server.expect(requestTo(
				"https://opentdb.com/api.php?amount=1&encode=url3986&token=session-token-2"))
				.andRespond(json("""
						{
						  "response_code":0,
						  "results":[
						    {
						      "type":"multiple",
						      "difficulty":"hard",
						      "category":"History",
						      "question":"Who%20was%20the%20first%20president%20of%20the%20USA%3F",
						      "correct_answer":"George%20Washington",
						      "incorrect_answers":["John%20Adams","Thomas%20Jefferson","James%20Madison"]
						    }
						  ]
						}
						"""));

		OpenTriviaClient client = new OpenTriviaClient(restClientBuilder.build(), clock);

		List<TriviaQuestion> firstBatch = client.fetchQuestions(2, null, TriviaDifficulty.ANY);
		List<TriviaQuestion> secondBatch = client.fetchQuestions(1, 12, TriviaDifficulty.ANY);
		clock.advance(Duration.ofHours(6).plusSeconds(1));
		List<TriviaQuestion> thirdBatch = client.fetchQuestions(1, null, TriviaDifficulty.ANY);

		assertThat(firstBatch).singleElement().satisfies(question -> {
			assertThat(question.question()).isEqualTo("What is H2O?");
			assertThat(question.category()).isEqualTo("Science & Nature");
			assertThat(question.correctAnswer()).isEqualTo("Water");
		});
		assertThat(secondBatch).singleElement().satisfies(question -> {
			assertThat(question.category()).isEqualTo("Music");
			assertThat(question.correctAnswer()).isEqualTo("True");
		});
		assertThat(thirdBatch).singleElement().satisfies(question -> {
			assertThat(question.question()).isEqualTo("Who was the first president of the USA?");
			assertThat(question.correctAnswer()).isEqualTo("George Washington");
		});

		server.verify();
	}

	@Test
	void responseCodeThreeReplacesInvalidTokenAndRetriesOnce() {
		server.expect(requestTo("https://opentdb.com/api_token.php?command=request"))
				.andRespond(json("""
						{"response_code":0,"response_message":"Token Generated Successfully!","token":"stale-token"}
						"""));
		server.expect(requestTo(
				"https://opentdb.com/api.php?amount=1&encode=url3986&token=stale-token"))
				.andRespond(json("""
						{"response_code":3,"results":[]}
						"""));
		server.expect(requestTo("https://opentdb.com/api_token.php?command=request"))
				.andRespond(json("""
						{"response_code":0,"response_message":"Token Generated Successfully!","token":"fresh-token"}
						"""));
		server.expect(requestTo(
				"https://opentdb.com/api.php?amount=1&encode=url3986&token=fresh-token"))
				.andRespond(json("""
						{
						  "response_code":0,
						  "results":[
						    {
						      "type":"multiple",
						      "difficulty":"easy",
						      "category":"General%20Knowledge",
						      "question":"Which%20planet%20is%20known%20as%20the%20Red%20Planet%3F",
						      "correct_answer":"Mars",
						      "incorrect_answers":["Venus","Jupiter","Mercury"]
						    }
						  ]
						}
						"""));

		OpenTriviaClient client = new OpenTriviaClient(restClientBuilder.build(), clock);

		List<TriviaQuestion> questions = client.fetchQuestions(1, null, TriviaDifficulty.ANY);

		assertThat(questions).extracting(TriviaQuestion::correctAnswer).containsExactly("Mars");
		server.verify();
	}

	@Test
	void fetchQuestionsIncludesSelectedDifficultyWhenProvided() {
		server.expect(requestTo("https://opentdb.com/api_token.php?command=request"))
				.andRespond(json("""
						{"response_code":0,"response_message":"Token Generated Successfully!","token":"session-token"}
						"""));
		server.expect(requestTo(
				"https://opentdb.com/api.php?amount=1&difficulty=hard&encode=url3986&token=session-token"))
				.andRespond(json("""
						{
						  "response_code":0,
						  "results":[
						    {
						      "type":"multiple",
						      "difficulty":"hard",
						      "category":"History",
						      "question":"Who%20was%20the%20first%20president%20of%20the%20USA%3F",
						      "correct_answer":"George%20Washington",
						      "incorrect_answers":["John%20Adams","Thomas%20Jefferson","James%20Madison"]
						    }
						  ]
						}
						"""));

		OpenTriviaClient client = new OpenTriviaClient(restClientBuilder.build(), clock);

		List<TriviaQuestion> questions = client.fetchQuestions(1, null, TriviaDifficulty.HARD);

		assertThat(questions).singleElement().satisfies(question ->
				assertThat(question.difficulty()).isEqualTo("hard"));
		server.verify();
	}

	@Test
	void fetchQuestionsIncludesSelectedTriviaTypeWhenProvided() {
		server.expect(requestTo("https://opentdb.com/api_token.php?command=request"))
				.andRespond(json("""
						{"response_code":0,"response_message":"Token Generated Successfully!","token":"session-token"}
						"""));
		server.expect(requestTo(
				"https://opentdb.com/api.php?amount=1&type=boolean&encode=url3986&token=session-token"))
				.andRespond(json("""
						{
						  "response_code":0,
						  "results":[
						    {
						      "type":"boolean",
						      "difficulty":"easy",
						      "category":"History",
						      "question":"Is%20this%20a%20boolean%20question%3F",
						      "correct_answer":"True",
						      "incorrect_answers":["False"]
						    }
						  ]
						}
						"""));

		OpenTriviaClient client = new OpenTriviaClient(restClientBuilder.build(), clock);

		List<TriviaQuestion> questions = client.fetchQuestions(1, null, TriviaDifficulty.ANY, TriviaType.BOOLEAN);

		assertThat(questions).singleElement().satisfies(question ->
				assertThat(question.type()).isEqualTo("boolean"));
		server.verify();
	}

	@Test
	void responseCodeFourResetsTokenAndRetriesOnce() {
		server.expect(requestTo("https://opentdb.com/api_token.php?command=request"))
				.andRespond(json("""
						{"response_code":0,"response_message":"Token Generated Successfully!","token":"drained-token"}
						"""));
		server.expect(requestTo(
				"https://opentdb.com/api.php?amount=1&encode=url3986&token=drained-token"))
				.andRespond(json("""
						{"response_code":4,"results":[]}
						"""));
		server.expect(requestTo("https://opentdb.com/api_token.php?command=reset&token=drained-token"))
				.andRespond(json("""
						{"response_code":0,"token":"drained-token"}
						"""));
		server.expect(requestTo(
				"https://opentdb.com/api.php?amount=1&encode=url3986&token=drained-token"))
				.andRespond(json("""
						{
						  "response_code":0,
						  "results":[
						    {
						      "type":"boolean",
						      "difficulty":"medium",
						      "category":"Computers",
						      "question":"Does%20HTTP%20stand%20for%20HyperText%20Transfer%20Protocol%3F",
						      "correct_answer":"True",
						      "incorrect_answers":["False"]
						    }
						  ]
						}
						"""));

		OpenTriviaClient client = new OpenTriviaClient(restClientBuilder.build(), clock);

		List<TriviaQuestion> questions = client.fetchQuestions(1, null, TriviaDifficulty.ANY);

		assertThat(questions).extracting(TriviaQuestion::question)
				.containsExactly("Does HTTP stand for HyperText Transfer Protocol?");
		server.verify();
	}

	@Test
	void responseCodeOneFailsWhenNoResultsAreAvailable() {
		server.expect(requestTo("https://opentdb.com/api_token.php?command=request"))
				.andRespond(json("""
						{"response_code":0,"response_message":"Token Generated Successfully!","token":"session-token"}
						"""));
		server.expect(requestTo(
				"https://opentdb.com/api.php?amount=1&encode=url3986&token=session-token"))
				.andRespond(json("""
						{"response_code":1,"results":[]}
						"""));

		OpenTriviaClient client = new OpenTriviaClient(restClientBuilder.build(), clock);

		assertThatThrownBy(() -> client.fetchQuestions(1, null, TriviaDifficulty.ANY))
				.isInstanceOf(OpenTriviaClientException.class)
				.hasMessageContaining("response_code=1");

		server.verify();
	}

	@Test
	void responseCodeFiveFailsWithoutBlindRetrying() {
		server.expect(requestTo("https://opentdb.com/api_token.php?command=request"))
				.andRespond(json("""
						{"response_code":0,"response_message":"Token Generated Successfully!","token":"session-token"}
						"""));
		server.expect(requestTo(
				"https://opentdb.com/api.php?amount=1&encode=url3986&token=session-token"))
				.andRespond(json("""
						{"response_code":5,"results":[]}
						"""));

		OpenTriviaClient client = new OpenTriviaClient(restClientBuilder.build(), clock);

		assertThatThrownBy(() -> client.fetchQuestions(1, null, TriviaDifficulty.ANY))
				.isInstanceOf(OpenTriviaRateLimitException.class)
				.hasMessageContaining("rate limit");

		server.verify();
	}

	@Test
	void rejectsQuestionAmountsAboveTheUpstreamLimit() {
		OpenTriviaClient client = new OpenTriviaClient(restClientBuilder.build(), clock);

		assertThatThrownBy(() -> client.fetchQuestions(51, null, TriviaDifficulty.ANY))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("50");
	}

	@Test
	void mapsUpstreamHttpErrorsToClientException() {
		server.expect(requestTo("https://opentdb.com/api_token.php?command=request"))
				.andRespond(withServerError());
		OpenTriviaClient client = new OpenTriviaClient(restClientBuilder.build(), clock);

		assertThatThrownBy(() -> client.fetchQuestions(1, null, TriviaDifficulty.ANY))
				.isInstanceOf(OpenTriviaClientException.class)
				.hasMessageContaining("request failed");

		server.verify();
	}

	@Test
	void fetchQuestionsSerializesSharedTokenLifecycle() throws Exception {
		RestClient.Builder concurrentBuilder = RestClient.builder().baseUrl("https://opentdb.com");
		MockRestServiceServer concurrentServer = MockRestServiceServer.bindTo(concurrentBuilder)
				.ignoreExpectOrder(true)
				.build();
		CountDownLatch firstTokenRequestStarted = new CountDownLatch(1);
		CountDownLatch releaseFirstTokenRequest = new CountDownLatch(1);
		CountDownLatch secondRequestStarted = new CountDownLatch(1);
		concurrentServer.expect(requestTo("https://opentdb.com/api_token.php?command=request"))
				.andRespond(request -> {
					firstTokenRequestStarted.countDown();
					try {
						assertThat(releaseFirstTokenRequest.await(1, TimeUnit.SECONDS)).isTrue();
					}
					catch (InterruptedException exception) {
						Thread.currentThread().interrupt();
						throw new AssertionError(exception);
					}
					return json("""
							{"response_code":0,"token":"session-token"}
							""").createResponse(request);
				});
		concurrentServer.expect(twice(), requestTo(
				"https://opentdb.com/api.php?amount=1&encode=url3986&token=session-token"))
				.andRespond(json("""
						{"response_code":0,"results":[]}
						"""));

		OpenTriviaClient client = new OpenTriviaClient(concurrentBuilder.build(), clock);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<List<TriviaQuestion>> first = executor.submit(
					() -> client.fetchQuestions(1, null, TriviaDifficulty.ANY, TriviaType.ANY));
			assertThat(firstTokenRequestStarted.await(1, TimeUnit.SECONDS)).isTrue();
			Future<List<TriviaQuestion>> second = executor.submit(() -> {
				secondRequestStarted.countDown();
				return client.fetchQuestions(1, null, TriviaDifficulty.ANY, TriviaType.ANY);
			});

			assertThat(secondRequestStarted.await(1, TimeUnit.SECONDS)).isTrue();
			assertThatThrownBy(() -> second.get(250, TimeUnit.MILLISECONDS))
					.isInstanceOf(java.util.concurrent.TimeoutException.class);
			releaseFirstTokenRequest.countDown();
			assertThat(first.get(1, TimeUnit.SECONDS)).isEmpty();
			assertThat(second.get(1, TimeUnit.SECONDS)).isEmpty();
			concurrentServer.verify();
		}
		finally {
			releaseFirstTokenRequest.countDown();
			executor.shutdownNow();
		}
	}

	private org.springframework.test.web.client.ResponseCreator json(String body) {
		return withSuccess(body, new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
	}

	private static final class MutableClock extends Clock {

		private Instant instant;

		private MutableClock(Instant instant) {
			this.instant = instant;
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}

		private void advance(Duration duration) {
			instant = instant.plus(duration);
		}
	}

}
