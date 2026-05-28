# Spring En Bean Validation Benutten

## User Story
Als gebruiker wil ik consistente foutmeldingen krijgen bij ongeldige input, terwijl developers standaard Spring- en Bean Validation-mechanismen gebruiken in plaats van handmatige validatielogica.

## Probleemstelling
Veel logica wordt handmatig uitgeprogrammeerd terwijl Spring of Bean Validation hier bestaande oplossingen voor biedt. Dit maakt validatie verspreid en moeilijker te onderhouden.

## Scope
- Identificeer handmatige validatie die direct door Bean Validation-annotaties kan worden vervangen.
- Gebruik `@Valid`, constraint annotaties en gerichte exception handling waar passend.
- Behoud domeinvalidatie alleen waar die echte businessregels afdwingt.

## Acceptatiecriteria
- Request-validatie gebruikt Bean Validation waar mogelijk.
- Controllers of handlers bevatten geen duplicatieve null/blank/range checks wanneer annotaties volstaan.
- Foutresponses blijven voorspelbaar en getest.
- Businessregels blijven expliciet in de juiste laag.

## TDD Aanpak
1. Red: schrijf controller- of validation-tests voor ongeldige requests.
2. Green: vervang handmatige checks door Bean Validation-annotaties en `@Valid`.
3. Refactor: centraliseer validatiefoutafhandeling als dat bestaande duplicatie verwijdert.

## Verificatie
- Run request-validatietests.
- Run relevante controller/service tests.
