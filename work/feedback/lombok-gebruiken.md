# Lombok Gericht Gebruiken

## User Story
Als developer wil ik Lombok gericht gebruiken voor boilerplate zoals constructors en getters, zodat code compacter wordt zonder lifecycle of testbaarheid onduidelijk te maken.

## Probleemstelling
De tip is om Lombok te gebruiken. Dit kan boilerplate verminderen, maar moet consistent en voorzichtig gebeuren om dubbele constructors of verborgen gedrag te voorkomen.

## Scope
- Gebruik Lombok alleen voor repetitieve boilerplate waar het bestaande gedrag exact behoudt.
- Vermijd annotaties die conflicteren met expliciete constructors.
- Houd domeinlogica zichtbaar in normale code.

## Acceptatiecriteria
- Boilerplate wordt verminderd zonder gedragswijziging.
- Classes met expliciete constructors krijgen geen Lombok-annotatie die duplicaten genereert.
- Tests en compilatie bewijzen dat constructorcontracten intact blijven.

## TDD Aanpak
1. Red: behoud bestaande compilatie- en gedragstests als safety net.
2. Green: vervang boilerplate door passende Lombok-annotaties, bijvoorbeeld `@RequiredArgsConstructor` waar final dependencies bestaan.
3. Refactor: verwijder overbodige handgeschreven constructors/getters alleen als Lombok ze exact dekt.

## Verificatie
- Run compilatie en relevante tests.
- Controleer expliciet dat `InMemoryQuizSessionStore` geen dubbele constructor krijgt.
