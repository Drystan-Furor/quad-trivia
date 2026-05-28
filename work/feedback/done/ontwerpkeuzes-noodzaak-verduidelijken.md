# Ontwerpkeuzes Noodzaak Verduidelijken

## User Story
Als reviewer wil ik dat niet-obvious ontwerpkeuzes worden bewezen door tests of kort gemotiveerd in code, zodat de noodzaak ervan duidelijk is.

## Probleemstelling
Er zijn meerdere ontwerpkeuzes waarvan de noodzaak niet duidelijk werd. Onverklaarde complexiteit maakt onderhoud en review moeilijker.

## Scope
- Identificeer constructies die complexer zijn dan de directe use case lijkt te vragen.
- Verwijder de constructie als tests aantonen dat eenvoudiger code volstaat.
- Documenteer kort wanneer de constructie nodig blijft vanwege concurrency, lifecycle of boundarygedrag.

## Acceptatiecriteria
- Elke behouden niet-triviale ontwerpkeuze heeft testdekking of korte motivatie.
- Onnodige complexiteit wordt vereenvoudigd.
- Nieuwe comments verklaren waarom, niet wat de code doet.
- work/feedback/ontwerpkeuzes-noodzaak-verduidelijken.md is als laatste stap verplaatst naar work/feedback/done

## TDD Aanpak
1. Red: schrijf tests die de veronderstelde noodzaak van de ontwerpkeuze vastleggen.
2. Green: behoud of vereenvoudig de implementatie op basis van die test.
3. Refactor: voeg alleen bij resterende complexiteit een korte rationale toe.

## Verificatie
- Run de tests die de ontwerpkeuze rechtvaardigen.
- Review of de code zonder extra uitleg begrijpelijk is; zo niet, voeg rationale toe.
