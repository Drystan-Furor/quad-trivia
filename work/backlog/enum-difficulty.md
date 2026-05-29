# Enum Difficulty

## Stories

- [story]: As a User I want to be able to select difficulty and not only amount, so I have more control over the quiz.
- [story]: As a developer I want implement a ENUM based on the documentation of TriviaDB: "difficulty list from Trivai DB", so I can use it to build a front-end select element while keeping structure and safety in my java app.

## Spec-Driven TDD

### Specification

- The home page exposes a difficulty select next to amount and category.
- The select options match `work/backlog/difficulty.html`: `any`, `easy`, `medium`, `hard`.
- Difficulty is represented in Java by an enum, including display labels for UI rendering.
- Selecting `any` omits the Open Trivia DB `difficulty` query parameter.
- Selecting `easy`, `medium`, or `hard` sends that lowercase value to Open Trivia DB.
- Invalid difficulty values are rejected before calling Open Trivia DB.

### Tests

- JUnit: enum options match the documented TriviaDB difficulty list.
- JUnit: home page renders the enum-backed difficulty select.
- JUnit: MVC and JSON question endpoints pass selected difficulty to the service.
- JUnit: service passes selected difficulty to the Open Trivia client and rejects invalid values.
- JUnit: Open Trivia client includes the selected difficulty in the upstream request.
- Playwright: user can select difficulty and start a quiz.

### Status

- DONE: Implement enum-backed difficulty selection.
