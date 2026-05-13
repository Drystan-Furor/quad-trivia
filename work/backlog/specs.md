# Functionele specificatie - Quad Trivia

## Doel
Bouw een eenvoudige webapplicatie waarmee een gebruiker trivia-vragen kan ophalen, beantwoorden en laten nakijken, zonder dat het juiste antwoord vooraf via de client of netwerkresponses zichtbaar wordt.

## Context
De applicatie gebruikt de Open Trivia Database als bron voor trivia-vragen. Omdat deze bron de juiste antwoorden direct meegeeft in de JSON-respons, moet de oplossing een tussenliggende back-end bevatten die dit afschermt voor de eindgebruiker.

## Gebruiker
- Eindgebruiker die trivia-vragen wil beantwoorden via een eenvoudige webinterface.

## Scope
- Een frontend met een eenvoudige UI voor het spelen van een trivia-ronde.
- Een back-end API die trivia-vragen ophaalt uit de externe bron en veilige endpoints aanbiedt aan de frontend.
- Basisdocumentatie om de applicatie lokaal te bouwen en uit te voeren.

## Functionele requirements

### 1. Vragen ophalen
- Het systeem moet trivia-vragen ophalen uit de Open Trivia Database.
- Het systeem moet hiervoor een eigen endpoint aanbieden op `GET /questions`.
- De frontend mag de Open Trivia Database niet direct aanroepen.
- De response van `GET /questions` mag het juiste antwoord niet prijsgeven.
- De response van `GET /questions` moet voldoende gegevens bevatten om de vraag in de UI te tonen en om de gebruiker een antwoord te laten kiezen of invullen.

### 2. Antwoorden indienen en controleren
- Het systeem moet een endpoint aanbieden op `POST /checkanswers`.
- De frontend moet via `POST /checkanswers` de antwoorden van de gebruiker kunnen indienen.
- Het systeem moet de ingediende antwoorden controleren op basis van de originele trivia-data.
- Het systeem moet per ingediend antwoord kunnen teruggeven of het correct of onjuist is.
- Het systeem moet voorkomen dat de gebruiker via dit proces vooraf het juiste antwoord kan afleiden zonder daadwerkelijk een antwoord in te dienen.

### 3. Trivia-spelverloop in de UI
- De UI moet een gebruiker in staat stellen een trivia-ronde te starten.
- De UI moet de opgehaalde vragen duidelijk tonen.
- De UI moet de beschikbare antwoordopties per vraag tonen wanneer de vraag meerkeuze bevat.
- De UI moet de gebruiker in staat stellen antwoorden te selecteren of in te voeren.
- De UI moet de gebruiker in staat stellen de ingevulde antwoorden te versturen.
- De UI moet na controle zichtbaar maken welke antwoorden goed en fout zijn.

### 4. Gebruiksvriendelijkheid
- De UI moet eenvoudig en begrijpelijk zijn.
- De gebruiker moet zonder technische kennis een vraag kunnen beantwoorden en resultaat kunnen ontvangen.
- De interactie tussen frontend en backend moet volledig via de eigen applicatie verlopen.

### 5. Applicatie-architectuur
- De oplossing moet bestaan uit een frontend en een tussenliggende backend.
- De backend moet gerealiseerd worden in Java of C# conform de opdrachtomschrijving.
- De backend moet fungeren als proxy/façade richting de Open Trivia Database.
- Een database is niet verplicht voor deze opdracht.

## Niet-functionele randvoorwaarden
- De oplossing moet broncode bevatten in een Git-repository.
- De hoofdmap van het project moet een `README` bevatten met build- en run-instructies.
- Een deployment naar een cloud-platform is optioneel, maar indien aanwezig moet hiervan een link beschikbaar zijn.

## Aanbevolen technische invulling
- Voor een Java-implementatie heeft Spring Boot of Quarkus de voorkeur volgens de opdrachttekst.
- Unit tests zijn niet expliciet verplicht, maar worden wel aanbevolen.
- De keuze van frontendtechnologie is vrij, zolang deze een moderne en bruikbare UI oplevert.

## Acceptatiecriteria
- Een gebruiker kan via de UI trivia-vragen ophalen.
- Een gebruiker kan via de UI antwoorden indienen.
- De backend schermt correcte antwoorden af totdat de controle plaatsvindt.
- De backend biedt minimaal de endpoints `GET /questions` en `POST /checkanswers`.
- De applicatiebron is beschikbaar via GitHub of GitLab.
- Het project bevat een `README` met instructies voor lokaal gebruik.

## Aannames
- Het aantal vragen per ronde, categorieën, moeilijkheidsgraad en scoreberekening zijn niet expliciet gespecificeerd en vallen buiten de minimale scope, tenzij later aanvullend gevraagd.
- Authenticatie, gebruikersaccounts en persistentie zijn geen onderdeel van deze opdracht.
