# Quiz Antwoorden Randomiseren

## User Story
Als quizspeler wil ik dat het juiste antwoord niet voorspelbaar altijd als eerste staat, zodat de quiz functioneel eerlijk en bruikbaar is.

## Probleemstelling
Binnen de quiz wordt steeds het juiste antwoord als eerste getoond. Daardoor kan een speler altijd de eerste optie kiezen en werkt de quiz niet zoals bedoeld.

## Scope
- Combineer correct en incorrect answers in een antwoordlijst zonder vaste positie voor het juiste antwoord.
- Bewaar wel welke optie correct is voor scoring.
- Maak randomisatie testbaar door een injecteerbare of controleerbare shuffle-strategie te gebruiken.

## Acceptatiecriteria
- Het juiste antwoord staat niet deterministisch altijd op index 0.
- Alle antwoorden blijven aanwezig en exact een antwoord is correct.
- Scoring blijft correct na randomisatie.
- Tests zijn deterministisch en niet flaky.

## TDD Aanpak
1. Red: schrijf een test die faalt wanneer het correcte antwoord altijd als eerste wordt aangeboden.
2. Red: schrijf een scoringtest die bewijst dat de juiste optie na shuffle nog correct wordt beoordeeld.
3. Green: introduceer minimale randomisatie met injecteerbare `Random` of shuffle-service.
4. Refactor: houd mapping en scoring gescheiden zodat randomisatie geen client DTO's door lagen lekt.

## Verificatie
- Run quiz-mapping en scoring tests.
- Run een controller/API-test die antwoordvolgorde en correctheid valideert zonder afhankelijk te zijn van echte randomness.
