# Stijlconsistentie Tussen Streams En For Loops

## User Story
Als developer wil ik consistente keuzes tussen streams en for-loops, zodat code makkelijk te lezen blijft en dezelfde soort transformatie niet telkens anders wordt geschreven.

## Probleemstelling
Er zit inconsistentie in stijlkeuzes zoals streams versus for-loops. Dit maakt de codebase minder voorspelbaar.

## Scope
- Kies per patroon een voorkeursstijl: streams voor eenvoudige declaratieve transformaties, for-loops voor control flow met meerdere stappen of side effects.
- Pas alleen code aan die door de huidige wijziging geraakt wordt of waar inconsistentie directe onderhoudslast geeft.
- Vermijd brede format-only refactors.

## Acceptatiecriteria
- Vergelijkbare codepaden gebruiken dezelfde stijl.
- Leesbaarheid gaat boven het afdwingen van streams of loops.
- Functioneel gedrag blijft ongewijzigd.
- work/feedback/stijlconsistentie-streams-forloops.md is als laatste stap verplaatst naar work/feedback/done

## TDD Aanpak
1. Red: behoud bestaande tests als characterization suite voor de betrokken code.
2. Green: herschrijf de specifieke inconsistenties zonder gedrag te wijzigen.
3. Refactor: verwijder overbodige tijdelijke variabelen of complexe streamketens alleen waar dat direct samenhangt.

## Verificatie
- Run tests voor de aangepaste classes.
- Review diff op surgical scope: geen unrelated style cleanup.
