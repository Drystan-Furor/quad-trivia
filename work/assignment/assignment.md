De opdracht
Onze klant heeft een eenvoudige webapplicatie nodig waarmee gebruikers trivia-vragen kunnen beantwoorden. Ze hebben een
service gekozen die de vragen en antwoorden genereert, maar deze heeft bepaalde beperkingen en mist een
gebruikersinterface (UI). Kun jij de applicatie bouwen die ze willen?

Vereisten
De Open Trivia Database biedt een simpele API met één endpoint om trivia-vragen en antwoorden in JSON-formaat op te
vragen: https://opentdb.com/api_config.php
De Open Trivia API zelf is echter niet ideaal, omdat een slimme gebruiker de JSON kan bekijken en het juiste antwoord
kan ontdekken. Een oplossing in de vorm van een tussenliggende API (Java of C# back-end) die twee endpoints biedt, zou
dit probleem oplossen:
GET /questions
POST /checkanswers
Daarnaast is er een simpele gebruikersinterface (UI) nodig zodat de gebruiker makkelijk kan communiceren met de
applicatie.

Oplevering
Een link naar Github waar de broncode kan worden gevonden;
Een README in de hoofdmap met instructies voor het bouwen en uitvoeren van de applicatie;
Een link naar de werkende applicatie op een cloud-platform (optioneel).
