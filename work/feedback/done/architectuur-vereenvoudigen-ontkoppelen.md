# Architectuur Vereenvoudigen En Ontkoppelen

## User Story
Als onderhoudende developer wil ik een eenvoudiger laagmodel met duidelijke verantwoordelijkheden, zodat wijzigingen lokaal blijven en tests minder brede objectgraphs nodig hebben.

## Probleemstelling
De architectuur voelt op meerdere plekken onnodig complex of sterk gekoppeld. Daardoor kost kleine functionaliteit relatief veel setup en raken lagen sneller met elkaar verweven.

## Scope
- Identificeer componenten met onduidelijke verantwoordelijkheden of te brede dependencies.
- Verklein koppeling door interfaces, services of mappers alleen toe te passen waar ze concrete waarde hebben.
- Verwijder geen lagen puur voor esthetiek; elke wijziging moet een testbaar onderhoudsprobleem oplossen.

## Acceptatiecriteria
- Betrokken componenten hebben een duidelijke verantwoordelijkheid.
- Tests voor kernlogica kunnen met minimale dependencies draaien.
- Er is geen nieuwe abstractie zonder directe use case.
- Publieke API-contracten blijven stabiel tenzij expliciet aangepast.
- architectuur-vereenvoudigen-ontkoppelen.md is als laatste stap verplaatst naar work/feedback/done


## TDD Aanpak
1. Red: schrijf characterization tests voor het bestaande gedrag van de gekoppelde flow.
2. Green: refactor verantwoordelijkheden zonder gedrag te wijzigen.
3. Refactor: vereenvoudig dependencygraphs en verwijder overbodige adapters of passthroughs.

## Verificatie
- Run characterization tests voor en na de refactor.
- Run relevante service- en controller-tests.
