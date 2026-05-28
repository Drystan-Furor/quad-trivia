# Docker Build Werkend Maken Met Maven Wrapper Line Endings

## User Story
Als developer wil ik de applicatie lokaal in Docker kunnen starten, zodat onboarding en verificatie niet afhankelijk zijn van de hostmachine.

## Probleemstelling
De applicatie werkte lokaal niet in Docker. De vermoedelijke oorzaak is dat de Maven wrapper Linux-incompatibele line endings heeft in de container.

## Scope
- Reproduceer de Docker-start of build-fout lokaal.
- Controleer line endings en executierechten van `mvnw`.
- Borg LF-line endings via repositoryconfiguratie als dat de oorzaak is.

## Acceptatiecriteria
- Docker build/start gebruikt `./mvnw` succesvol in een Linux container.
- `mvnw` heeft LF-line endings en is executable waar nodig.
- De fix is vastgelegd zodat Windows/macOS edits dit niet opnieuw breken.
- docker-maven-wrapper-line-endings.md is als laatste stap verplaatst naar work/feedback/done

## TDD Aanpak
1. Red: voeg een reproduceerbare Docker-verificatie toe of documenteer het falende commando als teststap.
2. Green: corrigeer line endings, executable bit of Dockerfile-stap met de kleinste wijziging.
3. Refactor: voeg `.gitattributes` toe of actualiseer deze om LF voor wrapper scripts af te dwingen.

## Verificatie
- Run de Docker build of compose-start die eerder faalde.
- Run `file mvnw` of vergelijkbare check om LF-line endings te bevestigen.
