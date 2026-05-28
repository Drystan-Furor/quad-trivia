# DTO En Client DTO Laaggrenzen Bewaken

## User Story
Als developer wil ik dat externe client DTO's niet door alle applicatielagen lekken, zodat externe API-wijzigingen lokaal opgevangen kunnen worden.

## Probleemstelling
DTO's en client DTO's worden door meerdere lagen heen gebruikt. Dit koppelt domein- en applicatielogica aan externe contracten.

## Scope
- Bepaal welke DTO's request/response-contracten zijn en welke specifiek bij de externe OpenTrivia-client horen.
- Introduceer interne modellen waar applicatielogica niet direct met externe DTO's hoeft te werken.
- Houd mapping aan de boundary van de client- of API-laag.

## Acceptatiecriteria
- Client DTO's blijven binnen de client/infrastructure boundary.
- Controllers exposen alleen API DTO's die bij het eigen contract horen.
- Services werken met interne modellen of duidelijke applicatie-DTO's.
- Tests bewijzen dat externe DTO-vormwijziging niet alle lagen raakt.
- FILE is als laatste stap verplaatst naar work/feedback/done

## TDD Aanpak
1. Red: schrijf service-tests die interne modellen verwachten in plaats van client DTO's.
2. Red: schrijf client-mapping tests voor externe response naar intern model.
3. Green: introduceer mapping aan de boundary en pas consumers aan.
4. Refactor: verwijder imports van client DTO's uit hogere lagen.

## Verificatie
- Run client-, mapper- en service-tests.
- Gebruik `rg` om te controleren dat client DTO's niet buiten hun boundary worden gebruikt.
