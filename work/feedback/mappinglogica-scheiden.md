# Mappinglogica Scheiden

## User Story
Als developer wil ik mappinglogica apart houden van businesslogica, zodat transformaties gericht getest kunnen worden en services leesbaar blijven.

## Probleemstelling
Mappinglogica had beter gescheiden kunnen worden. Wanneer mapping door services of controllers heen staat, worden tests breder en verantwoordelijkheden minder duidelijk.

## Scope
- Verplaats niet-triviale DTO/model-transformaties naar mappercomponenten of stateless mappermethoden.
- Laat services focussen op use-case gedrag.
- Houd simpele eenregelige mapping lokaal als extractie geen duidelijk voordeel geeft.

## Acceptatiecriteria
- Niet-triviale mapping heeft eigen tests.
- Services bevatten geen verspreide veld-voor-veld transformaties.
- Mappercode is stateless en eenvoudig te gebruiken.

## TDD Aanpak
1. Red: schrijf mappertests voor bestaande transformaties en edge cases.
2. Green: verplaats mapping naar een mapper en laat bestaande flows deze gebruiken.
3. Refactor: verwijder dubbele mappingcode uit services/controllers.

## Verificatie
- Run mappertests.
- Run service/controller tests die de gemapte data gebruiken.
