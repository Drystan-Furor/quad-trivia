# Security Notes

## Implemented Controls

- Spring Security is active for the full application.
- Only `/`, `/questions`, and `/checkanswers` are allowed; any other route is denied by default.
- CSRF protection remains enabled, which protects the HTML form flow from cross-site request forgery.
- Open Trivia DB session tokens are managed only on the server.
- Correct answers are stored only in server-side quiz sessions until answer checking occurs.
- The browser receives question text and answer options, but not the correct answer, token, or internal debug state.
- Quiz sessions expire after 15 minutes and are marked as used after one successful submission.
- Quiz creation is guarded by a 5 second window to reduce avoidable upstream pressure.
- Open Trivia content is URL-decoded server-side and escaped before being rendered in Thymeleaf templates.

## Intended Boundaries

- The application uses an in-memory session store only. Restarting the process clears active quiz sessions.
- There is no authentication or authorization layer because the assignment scope is a public trivia application.
- There is no persistent audit trail or external rate limiter in this scope.
- Abuse controls are intentionally light and focus on protecting the upstream trivia API from rapid repeat quiz starts.

## Review Pointers

- Security configuration: `src/main/java/quad/solutions/trivia/security/SecurityConfig.java`
- Token lifecycle and upstream handling: `src/main/java/quad/solutions/trivia/client/OpenTriviaClient.java`
- Quiz issuance and answer checking: `src/main/java/quad/solutions/trivia/service/QuizService.java`
- Session expiry behavior: `src/main/java/quad/solutions/trivia/session/InMemoryQuizSessionStore.java`
