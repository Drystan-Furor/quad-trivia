# Onnodige JSON Conversie In Assertions Verwijderen

## User Story
Als developer wil ik assertions direct op objecten of responsevelden uitvoeren, zodat tests duidelijk maken welk gedrag wordt verwacht zonder onnodige JSON-serialisatie.

## Probleemstelling
Er wordt onnodige JSON-conversie gedaan voor assertions. Dit maakt tests indirecter en kan falen op serialisatiedetails in plaats van functioneel gedrag.

## Scope
- Vervang JSON-stringvergelijkingen door object assertions, `jsonPath`, AssertJ recursive comparison of field-level checks.
- Gebruik JSON-conversie alleen in controller/API-tests waar de JSON-contractvorm expliciet het onderwerp is.

## Acceptatiecriteria
- Unit tests assert niet via JSON wanneer objectvergelijking volstaat.
- API-contracttests gebruiken gerichte JSON assertions in plaats van volledige stringconversies.
- Testfailures tonen duidelijk welk veld afwijkt.

## TDD Aanpak
1. Red: behoud bestaande tests als regressiedekking.
2. Green: herschrijf assertions naar directe object- of veldassertions.
3. Refactor: verwijder overbodige `ObjectMapper` setup uit tests.

## Verificatie
- Run de aangepaste tests.
- Controleer dat assertion failures leesbare veldinformatie tonen.
