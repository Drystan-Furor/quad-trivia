# Onnodige Constructors Voor Tests Verwijderen

## User Story
Als developer wil ik dat tests bestaande productieconstructors en Spring-testfaciliteiten gebruiken, zodat productiecode niet wordt aangepast puur om tests mogelijk te maken.

## Probleemstelling
Er worden constructors toegevoegd die alleen testing ondersteunen. Daardoor lekt testbehoefte naar productiecode en wordt de publieke API van classes onnodig groter.

## Scope
- Zoek constructors die niet door productiecode of Spring nodig zijn.
- Vervang testgebruik door Mockito, Spring test configuration of bestaande constructors.
- Verwijder constructors die alleen voor tests bestaan.

## Acceptatiecriteria
- Geen constructor bestaat alleen voor testgemak.
- Tests blijven leesbaar en expliciet over hun collaborators.
- Productieclasses houden de kleinste benodigde constructor-API.

## TDD Aanpak
1. Red: schrijf of behoud tests die het gedrag afdekken waarvoor de test-only constructor werd gebruikt.
2. Green: pas de testsetup aan naar Mockito of Spring-test wiring zonder productieconstructor toe te voegen.
3. Refactor: verwijder de overbodige constructor en eventuele ongebruikte imports.

## Verificatie
- Run de aangepaste testklasse.
- Run compilatie om te bevestigen dat geen call sites op de verwijderde constructor steunen.
