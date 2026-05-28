# Synchronization Rond EnforceQuizCreationGuard Onderbouwen

## User Story
Als developer wil ik weten of `synchronized` bij `enforceQuizCreationGuard` noodzakelijk is, zodat concurrencybescherming correct maar niet zwaarder dan nodig wordt toegepast.

## Probleemstelling
De noodzaak van `synchronized` bij `enforceQuizCreationGuard` is niet duidelijk. Mogelijk maskeert het een state-managementprobleem of is het overbodig bij de huidige bean lifecycle.

## Scope
- Bepaal welke gedeelde mutable state door de guard wordt beschermd.
- Schrijf concurrencytests voor dubbele quizcreatie als dat risico reëel is.
- Vervang `synchronized` door een eenvoudiger of gerichter mechanisme als dat volstaat.

## Acceptatiecriteria
- Er is een test die het risico op dubbele quizcreatie of race conditions afdekt.
- `synchronized` blijft alleen staan als de test de noodzaak aantoont.
- Als het wordt verwijderd, blijft quizcreatie correct onder parallelle requests.

## TDD Aanpak
1. Red: schrijf een concurrencytest die parallelle quizcreatie probeert te forceren.
2. Green: implementeer de minimale thread-safe oplossing.
3. Refactor: verwijder of beperk `synchronized` als een beter afgebakend mechanisme mogelijk is.

## Verificatie
- Run de concurrencytest meerdere keren.
- Run service-tests rond quizcreatie.
