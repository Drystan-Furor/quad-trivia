# Mockito Functionaliteit Beter Benutten

## User Story
Als testschrijver wil ik Mockito gebruiken voor mocks, stubbing en verificatie, zodat tests korter, explicieter en minder gekoppeld aan implementatiedetails zijn.

## Probleemstelling
Mockito-functionaliteit wordt nauwelijks benut. Hierdoor ontstaan handmatige test doubles en complexere testsetup dan nodig.

## Scope
- Vervang handmatige mocks of fakes door Mockito waar gedrag eenvoudig te stubben is.
- Gebruik `@Mock`, `@InjectMocks`, `when`, `verify` en argument matchers waar passend.
- Behoud handgeschreven fakes alleen als ze aantoonbaar meer duidelijkheid geven.

## Acceptatiecriteria
- Tests maken collaborators duidelijk via Mockito-annotaties of expliciete `mock(...)` calls.
- Interacties worden geverifieerd waar dat onderdeel van het gedrag is.
- Testsetup bevat minder handmatige boilerplate.

## TDD Aanpak
1. Red: behoud of schrijf gedragstests die falen wanneer collaborator-interacties ontbreken.
2. Green: vervang handmatige test doubles door Mockito-stubs en verificaties.
3. Refactor: verwijder custom mockklassen of helpermethoden die geen waarde meer toevoegen.

## Verificatie
- Run de aangepaste unit tests.
- Controleer dat tests nog steeds falen bij verkeerde collaborator-aanroepen.
