# Handmatige Mock Injectie Verminderen

## User Story
Als developer wil ik mocks via Mockito of Spring-testmechanismen laten injecteren, zodat tests minder afhankelijk zijn van constructorvolgorde en interne wiring.

## Probleemstelling
Mocks worden handmatig opgebouwd en geïnjecteerd. Dit maakt tests langdradig en gevoelig voor constructorwijzigingen.

## Scope
- Gebruik `@ExtendWith(MockitoExtension.class)` met `@Mock` en `@InjectMocks` voor pure unit tests.
- Gebruik `@MockBean` of testconfiguratie voor Spring-integratietests.
- Verwijder handmatige mockconstructie waar automatische injectie hetzelfde gedrag levert.

## Acceptatiecriteria
- Unit tests initialiseren mocks via MockitoExtension.
- Spring tests gebruiken Spring test support in plaats van eigen wiring.
- Testcode blijft expliciet genoeg om dependencies te begrijpen.
- work/feedback/handmatige-mock-injectie-verminderen.md is als laatste stap verplaatst naar work/feedback/done

## TDD Aanpak
1. Red: zorg dat bestaande gedragstests de huidige functionaliteit afdekken.
2. Green: migreer de testsetup naar Mockito- of Spring-injectie.
3. Refactor: verwijder overbodige setup-methoden, helperconstructors en handmatige objectgraphs.

## Verificatie
- Run de gemigreerde testklassen.
- Run compilatie om ongebruikte helpers te vinden.
