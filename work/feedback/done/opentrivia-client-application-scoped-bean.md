# OpenTriviaClient Als Application Scoped Bean

## User Story
Als developer wil ik dat `OpenTriviaClient` als normale application-scoped Spring bean beschikbaar is, zodat consumers hem via dependency injection gebruiken in plaats van handmatig aanmaken.

## Probleemstelling
Sommige beans hadden eenvoudiger opgezet kunnen worden. `OpenTriviaClient` lijkt een kandidaat voor een application-scoped bean, omdat clientconfiguratie centraal hoort te staan.

## Scope
- Maak `OpenTriviaClient` een Spring-managed bean.
- Centraliseer benodigde configuratie, zoals base URL en HTTP-client dependencies.
- Verwijder handmatige instantiatie waar DI volstaat.

## Acceptatiecriteria
- `OpenTriviaClient` wordt eenmaal als application-scoped bean beheerd door Spring.
- Consumers injecteren de client via constructor injection.
- Tests kunnen de client mocken met `@MockBean` of Mockito zonder handmatige wiring.
- Runtimegedrag van trivia-ophalingen blijft gelijk.
- work/feedback/opentrivia-client-application-scoped-bean.md is als laatste stap verplaatst naar work/feedback/done

## TDD Aanpak
1. Red: voeg een Spring context test toe die verifieert dat `OpenTriviaClient` als bean beschikbaar is.
2. Red: voeg een consumer-test toe die faalt wanneer de client handmatig moet worden geconstrueerd.
3. Green: registreer `OpenTriviaClient` als bean en injecteer hem waar nodig.
4. Refactor: verwijder dubbele clientconstructie en verplaats configuratie naar een passende config/bean-definitie.

## Verificatie
- Run de context test.
- Run tests voor services die `OpenTriviaClient` gebruiken.
