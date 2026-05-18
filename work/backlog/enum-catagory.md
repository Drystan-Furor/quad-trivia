# Enum Catagory Story

## User Stories

- As a User I want to be able to select category and not only amount, so I have more control over the quiz.
- As a developer I want implement a ENUM based on the documentation of TriviaDB: "category list from Trivai DB", so I can use it to build a front-end select element while keeping structure and safety in my java app.

## Spec-Driven TDD

Status: DONE

### Assumptions

- `work/backlog/categories.html` is the source document for the category ids and display names.
- The public service contract remains `QuizResponse createQuiz(int amount, Integer category)`.
- A missing category means "Any Category"; a provided category must be positive.
- The enum provides the known Open Trivia DB category options for the home-page select.

### Acceptance Criteria

- `TriviaCategory` enum contains the documented Open Trivia DB category ids and labels.
- The home page renders an "Any Category" option and enum-backed category options.
- Starting a quiz from the UI posts the selected category to `QuizService.createQuiz`.
- `GET /questions?amount=5&category=...` continues through the same service flow.
- Positive category validation is covered by tests.
- Playwright proves a user can select a category before starting a quiz.

### TDD Plan

1. Red: add JUnit coverage for enum values, home-page options, API category forwarding, UI category forwarding, and category validation.
2. Red: add Playwright coverage for selecting a category before starting a quiz.
3. Green: implement the enum and wire it into the home page model/template.
4. Refactor: keep the implementation small and aligned with the existing controller/service flow.
5. Verify: run targeted JUnit and Playwright tests.

### Verification

- PASS: `./mvnw -Dtest=TriviaCategoryTest,HomeControllerTest,QuestionControllerTest,TriviaUiControllerTest,QuizServiceTest test`
- PASS: `./mvnw test`
- PASS: `npm run e2e`
- PASS: Browser check confirmed 25 category options, default "Any Category", and selecting `Science: Computers` submits value `18`.
