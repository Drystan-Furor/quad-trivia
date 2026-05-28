# Dependency Injection Consistent Toepassen

## User Story
Als onderhoudende developer wil ik dat dependency injection overal via een consistente Spring-stijl gebeurt, zodat componenten minder gekoppeld zijn en tests eenvoudiger en betrouwbaarder worden.

## Probleemstelling
Dependency injection lijkt niet overal goed begrepen of consequent toegepast. Dit vergroot de kans op handmatige wiring, extra constructors en tests die implementatiedetails kennen.

## Scope
- Inventariseer services, clients, controllers en stores waar dependencies handmatig of inconsistent worden geïnjecteerd.
- Kies constructor injection via Spring als standaard.
- Verwijder alternatieve wiring alleen wanneer die geen functionele waarde toevoegt.

## Acceptatiecriteria
- Productiebeans gebruiken een consistente DI-vorm.
- Tests hoeven geen productiewiring te dupliceren.
- Er zijn geen nieuwe test-only constructors nodig.
- Bestaande functionaliteit blijft gelijk.
- dependency-injection-consistentie.md is als laatste stap verplaatst naar work/feedback/done


## TDD Aanpak
1. Red: voeg of actualiseer een context-load test die de relevante Spring beans via de application context opstart.
2. Red: voeg een gerichte test toe die faalt wanneer een dependency niet door Spring kan worden geïnjecteerd.
3. Green: refactor de betrokken beans naar consistente constructor injection.
4. Refactor: verwijder overbodige handmatige wiring en test-only constructies.

## Verificatie
- Run de relevante unit tests.
- Run de Spring context test.
- Run `./mvnw test` of, bij bekende Surefire-fork issues, dezelfde suite met `-DforkCount=0`.
