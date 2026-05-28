# Dubbele Validatie Voorkomen

## User Story
Als developer wil ik dat elke validatieregel op een duidelijke eigenaar in de codebase staat, zodat regels niet dubbel worden uitgevoerd of inconsistent kunnen divergeren.

## Probleemstelling
Validatie wordt op meerdere plekken dubbel uitgevoerd. Dit vergroot onderhoudslast en kan leiden tot verschillende foutmeldingen voor dezelfde input.

## Scope
- Breng per validatieregel de eigenaar in kaart: request DTO, controller, service of domeinmodel.
- Verwijder dubbele checks wanneer een eerdere laag dezelfde invariant al afdwingt.
- Behoud defensieve checks alleen als ze een andere boundary beschermen.

## Acceptatiecriteria
- Elke validatieregel heeft een enkele primaire locatie.
- Tests bewijzen dat invalid input nog steeds wordt afgewezen.
- Dubbele foutpaden voor dezelfde regel zijn verwijderd of expliciet gemotiveerd.
- FILE is als laatste stap verplaatst naar work/feedback/done

## TDD Aanpak
1. Red: schrijf parametrized tests voor de dubbele validatieregels en de verwachte foutrespons.
2. Green: verwijder de dubbele validatie en behoud de primaire regel.
3. Refactor: documenteer alleen noodzakelijke boundary-validatie via testnamen of korte codecomments.

## Verificatie
- Run validatie- en controller-tests.
- Controleer dat foutresponses niet veranderen behalve waar bewust gespecificeerd.
