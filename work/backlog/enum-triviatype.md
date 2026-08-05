# Enum Trivia Type

## Stories

- [story]: As a User I want to be able to select trivia type and not only amount, so I have more control over the quiz.
- [story]: As a developer I want implement a ENUM based on the documentation of TriviaDB: "triviatype list from Trivai DB", so I can use it to build a front-end select element while keeping structure and safety in my java app.

## Spec-Driven TDD

### Specification

- The home page exposes a trivia type select next to amount, category, and difficulty.
- The select options match `work/backlog/trivia-type.html`: `any`, `multiple`, `boolean`.
- Trivia type is represented in Java by an enum, including display labels for UI rendering.
- Selecting `any` omits the Open Trivia DB `type` query parameter.
- Selecting `multiple` or `boolean` sends that lowercase value to Open Trivia DB.
- Invalid trivia type values are rejected before calling Open Trivia DB.

### Tests

- JUnit: enum options match the documented TriviaDB trivia type list.
- JUnit: home page renders the enum-backed trivia type select.
- JUnit: MVC and JSON question endpoints pass selected trivia type to the service.
- JUnit: service passes selected trivia type to the Open Trivia client and rejects invalid values.
- JUnit: Open Trivia client includes the selected trivia type in the upstream request.
- Playwright: user can select trivia type and start a quiz.

### Status

- DONE: Implement enum-backed trivia type selection.
