# Quad Trivia

Spring Boot trivia application backed by Open Trivia DB. The application keeps answer checking, quiz state, and
Open Trivia session tokens on the server so the browser never receives the correct answers before submission.

> This is an assesment repo for [Quad Solutions Assignemnt](https://www.quad.team/assignment)

>The application contacts TriviaDB API, more documentation: [Open Trivia DB](https://opentdb.com/api_config.php)

--- 
## Live demo
A deployed version is live on cloud platform [Render](https://render.com/) using Dockerfile.
This free instance will spin down with inactivity, which can delay requests by 50 seconds or more.
Due to this, the app needs time to start up if it was not used in the last 30 minutes.

Go to the [live demo here](https://quad-trivia-n5bu.onrender.com)

> https://quad-trivia-n5bu.onrender.com

The free instance will spin down with inactivity, which can delay requests by 50 seconds or more.
Allow the app time to start up if it was not used in the last 30 minutes.

---

## Builds

There is Spring Boot App and docker Compose with Docker Desktop choices:

### Requirements

- Java 25
- Node.js and npm for Playwright e2e tests
- Docker Desktop && Docker Compose

### Spring build

```bash
./mvnw clean test
```

### Run

Start the application against the real Open Trivia DB API:

```bash
./mvnw spring-boot:run
```

The default application URL is [http://localhost:8080](http://localhost:8080).

To point the app at another Open Trivia compatible endpoint:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--open-trivia.base-url=http://127.0.0.1:8098"
```


### Docker build

build the container and run it:

```sh
docker compose down -v
docker compose build --no-cache
docker compose up -d
```
The default application URL is http://localhost:4361/

---


## Test

Run the Spring test suite:

```bash
./mvnw test
```

Run the browser flow tests:

```bash
npm run e2e
```

The Playwright setup starts a local stub of Open Trivia DB and launches the Spring Boot app on `127.0.0.1:8097`.
Install the browser dependency first if needed:

```bash
npm run e2e:install
```

---

## Application Flow

- `GET /` renders the start page.
- `POST /questions` starts a UI quiz round and renders the quiz page.
- `GET /questions` returns quiz data for API clients.
- `POST /checkanswers` accepts JSON answer submissions for API clients.
- `POST /checkanswers` with form data checks answers for the Thymeleaf UI flow.

## Open Trivia Integration

- The backend calls Open Trivia DB through `OpenTriviaClient`.
- A session token is requested server-side through `/api_token.php` and is reused until it expires.
- Token inactivity expiry is treated as 6 hours.
- `response_code = 3` invalidates the current token and triggers a replacement request.
- `response_code = 4` resets the token first and falls back to a new token when reset does not succeed.
- `response_code = 5` is surfaced as a controlled rate-limit exception instead of retry spamming the upstream service.
- Question content is requested with `encode=url3986` and decoded server-side before use.

## Quiz Session Behavior

- The application creates a server-side quiz session for every issued round.
- Quiz sessions store the issued question ids, sanitized answer options, the correct answers, issue time, expiry time,
  and whether the quiz was already submitted.
- Quiz sessions live in memory and expire after 15 minutes.
- A submitted quiz is marked as used to block replay-style answer probing.
- A 5 second quiz creation guard reduces unnecessary bursts toward the upstream API.

## Security Summary

- Spring Security is enabled with explicit public route configuration for `/`, `/questions`, and `/checkanswers`.
- CSRF protection stays enabled for state-changing requests, including the HTML form flow.
- Any route outside the expected public surface is denied by default.
- Correct answers are never included in question responses or rendered HTML before answer submission.
- Open Trivia tokens stay server-side and are never returned to the browser.
- User-visible content is HTML-escaped before rendering.

More detail is documented in [SECURITY-NOTES.md](/Users/tristan/IdeaProjects/quad-trivia/SECURITY-NOTES.md).
