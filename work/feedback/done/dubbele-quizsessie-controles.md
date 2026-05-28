# Dubbele Controles Rond Quizsessies Verminderen

## User Story
Als developer wil ik dat controles rond quizsessies op een duidelijke plek plaatsvinden, zodat sessieregels niet dubbel en mogelijk inconsistent worden afgedwongen.

## Probleemstelling
Er lijken dubbele controles rondom quizsessies te bestaan. Dit kan leiden tot complexere flows en verschillende foutpaden voor dezelfde sessiestaat.

## Scope
- Inventariseer controles op bestaan, status en geldigheid van quizsessies.
- Kies een primaire eigenaar voor sessievalidatie.
- Verwijder dubbele controles of maak hun boundary-doel expliciet.

## Acceptatiecriteria
- Elke quizsessie-invariant heeft een primaire eigenaar.
- Dubbele controles zijn verwijderd of aantoonbaar boundary-validatie.
- Foutgedrag voor ontbrekende of ongeldige sessies is getest.
- work/feedback/dubbele-quizsessie-controles.md is als laatste stap verplaatst naar work/feedback/done

## TDD Aanpak
1. Red: schrijf tests voor ontbrekende, verlopen of ongeldig gebruikte quizsessies.
2. Green: centraliseer de sessiecontrole op de juiste laag.
3. Refactor: verwijder dubbele checks en harmoniseer foutmeldingen/statussen.

## Verificatie
- Run quizsessie service-tests.
- Run controller/API-tests voor sessiefouten.
