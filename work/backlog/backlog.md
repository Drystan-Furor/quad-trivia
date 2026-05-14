# Backlog - Quad Trivia

## Doel

Dit backlogdocument vertaalt de assignment, de functionele specificatie en de Open Trivia DB API-documentatie naar een
uitvoerbaar Spring Boot backlog. De backlog is aangescherpt op sessiegedrag, upstream API-beperkingen en security by
default.

## Productvisie

Als eindgebruiker wil ik trivia-vragen kunnen beantwoorden in een eenvoudige webapplicatie, waarbij het juiste antwoord
niet vooraf zichtbaar is, zodat ik eerlijk een trivia-ronde kan spelen.

## Relevante API-uitgangspunten

- De backend gebruikt Open Trivia DB als externe bron.
- Open Trivia DB ondersteunt session tokens om dubbele vragen te voorkomen binnen een token-levensduur.
- Een Open Trivia DB session token vervalt na 6 uur inactiviteit.
- Open Trivia DB gebruikt `response_code`-waarden om fouten en tokenstatus te signaleren.
- `response_code = 3` betekent dat het token niet bestaat.
- `response_code = 4` betekent dat het token leeg is en reset of vervanging nodig heeft.
- `response_code = 5` betekent rate limiting; per IP is slechts 1 request per 5 seconden toegestaan.
- Open Trivia DB ondersteunt maximaal 50 vragen per request.
- Open Trivia DB ondersteunt slechts 1 categorie per request.
- Open Trivia DB levert encoded content; de applicatie moet veilig decoderen voordat data wordt getoond.

## Security-uitgangspunten

- De browser is niet vertrouwd voor antwoordcontrole.
- Het correcte antwoord mag nooit in frontend HTML, JavaScript, hidden fields of API-responses terechtkomen voordat
  controle plaatsvindt.
- Open Trivia DB session tokens mogen nooit naar de browser lekken.
- De applicatie moet server-side quiz-sessies gebruiken om uitgegeven vragen te koppelen aan ingediende antwoorden.
- De applicatie moet Spring Security benutten voor veilige defaults, ook als de applicatie publiek toegankelijk blijft.
- Alleen verwachte, minimale data mag gelogd worden; antwoorden, correcte antwoorden en tokens horen niet in logs.

## Technische richting

- Back-end: Spring Boot MVC
- UI: Thymeleaf server-rendered pagina's
- Externe integratie: Open Trivia DB via een interne client/service
- Security: Spring Security met expliciete configuratie voor publieke routes, CSRF-bescherming en veilige headers
- Quiz state: server-side, tijdelijk, zonder database in de minimale scope
- Testaanpak: spec-driven TDD per user story

## EPIC 1 - Lever een veilige trivia webapplicatie met afgeschermde quiz- en upstream-sessies

Als opdrachtgever wil ik een eenvoudige maar veilige trivia webapplicatie, zodat eindgebruikers een eerlijke
trivia-ronde kunnen spelen terwijl correcte antwoorden, interne quiz-state en externe session tokens afgeschermd
blijven.

### Epic acceptatiecriteria

- De applicatie biedt een eenvoudige UI waarmee een gebruiker een trivia-ronde kan starten, invullen en nakijken.
- De applicatie biedt minimaal `GET /questions` en `POST /checkanswers`.
- Correcte antwoorden zijn niet zichtbaar in `GET /questions`, HTML-output, browser-state of netwerkresponses vóór de
  controle.
- Open Trivia DB wordt alleen vanuit de backend aangeroepen.
- Open Trivia DB session tokens worden uitsluitend server-side beheerd.
- De applicatie handelt Open Trivia DB `response_code`-waarden 0, 3, 4 en 5 gecontroleerd af.
- De applicatie respecteert de upstream rate limit en voorkomt onnodige bursts richting Open Trivia DB.
- De applicatie gebruikt Spring Security voor veilige standaardinstellingen.

## WSJF-methode

- Business Value (BV): 1-10
- Time Criticality (TC): 1-10
- Risk Reduction / Opportunity Enablement (RR/OE): 1-10
- Job Size (JS): 1-10
- WSJF = (BV + TC + RR/OE) / JS

## User stories

### Story 1 - Security-first project bootstrap

**Status:** DONE

**Als** Java Spring Boot developer  
**wil ik** een werkende basisapplicatie met MVC, Thymeleaf, Spring Security en testconfiguratie  
**zodat** vervolgfunctionaliteit op veilige defaults gebouwd wordt.

**WSJF**

- BV: 9
- TC: 9
- RR/OE: 10
- JS: 3
- Score: 9.33

**Scope**

- Richt package-structuur in voor controller, service, client, security, dto en session state.
- Zorg dat de applicatie start met een basisroute voor de UI.
- Voeg een expliciete Spring Security configuratie toe voor publieke quizroutes.
- Houd CSRF actief voor formulier- en state-changing requests.
- Zet veilige HTTP headers aan via Spring Security defaults of expliciete configuratie.

**TDD-plan**

1. Schrijf een failing context-load test.
2. Schrijf een failing webtest voor `GET /`.
3. Schrijf een failing securitytest die bevestigt dat `POST` zonder geldige CSRF-token wordt geweigerd.
4. Implementeer de minimale MVC- en securityconfiguratie om de tests groen te krijgen.
5. Refactor naar een heldere projectstructuur zonder extra gedrag toe te voegen.

**Acceptatiecriteria**

- De Spring Boot applicatie start succesvol.
- Er is een bereikbare basisroute voor de UI.
- CSRF is actief op state-changing routes.
- De codebasis heeft een duidelijke structuur voor verdere uitbreiding.

### Story 2 - Upstream Open Trivia client met token lifecycle

**Status:** DONE

**Als** backend  
**wil ik** Open Trivia DB via een interne client met session-tokenbeheer benaderen  
**zodat** dubbele vragen beperkt blijven en upstream foutcodes gecontroleerd worden afgehandeld.

**WSJF**

- BV: 10
- TC: 10
- RR/OE: 10
- JS: 5
- Score: 6.00

**Scope**

- Implementeer een serviceclient voor Open Trivia DB.
- Vraag server-side een Open Trivia session token aan en hergebruik deze veilig.
- Handel tokenverval en tokenuitputting af op basis van `response_code`.
- Ondersteun reset of vervanging van tokens wanneer upstream dat vereist.
- Respecteer de upstream rate limit in de clientlaag.
- Vraag encoded content op in een vorm die gecontroleerd server-side gedecodeerd kan worden.

**TDD-plan**

1. Schrijf failing clienttests voor succesvolle vraagophaalresponsen.
2. Schrijf failing tests voor `response_code = 3` en `response_code = 4`.
3. Schrijf een failing test voor rate-limit gedrag bij `response_code = 5`.
4. Implementeer token retrieval, token reset/renewal en foutafhandeling.
5. Refactor client- en foutmodellen voor leesbaarheid.

**Acceptatiecriteria**

- De backend beheert Open Trivia session tokens volledig server-side.
- Bij `response_code = 3` wordt een ongeldig token gecontroleerd vervangen.
- Bij `response_code = 4` wordt een token gecontroleerd gereset of vervangen.
- Bij `response_code = 5` wordt niet blind opnieuw gespamd richting upstream.
- De client respecteert de API-limieten van maximaal 50 vragen en maximaal 1 categorie per request.

### Story 3 - Veilige vraaguitgifte via `GET /questions`

**Status:** DONE

**Als** eindgebruiker  
**wil ik** via de applicatie een trivia-set kunnen ophalen  
**zodat** ik een ronde kan starten zonder direct contact met de externe bron of zicht op de juiste antwoorden.

**WSJF**

- BV: 10
- TC: 10
- RR/OE: 10
- JS: 5
- Score: 6.00

**Scope**

- Implementeer `GET /questions`.
- Map upstream trivia-data naar een intern, veilig responsemodel.
- Decodeer upstream content server-side en ontsmet deze voor weergave.
- Geef nooit het correcte antwoord of upstream token door aan de client.
- Maak een interne quiz-sessie aan waarin de uitgegeven vragen en correcte antwoorden tijdelijk server-side bewaard
  blijven.
- Geef alleen een quiz-identificatie terug die bruikbaar is voor vervolgacties, zonder gevoelige inhoud.

**TDD-plan**

1. Schrijf een failing controller test voor `GET /questions`.
2. Schrijf een failing servicetest die verifieert dat het correcte antwoord niet in de response voorkomt.
3. Schrijf een failing test die bevestigt dat een quiz-sessie server-side wordt opgeslagen.
4. Schrijf een failing test voor veilige decode-/sanitizelogica van vraagtekst en antwoordopties.
5. Implementeer controller, service en session-opslag totdat de tests groen zijn.

**Acceptatiecriteria**

- `GET /questions` retourneert trivia-vragen in een clientvriendelijk formaat.
- De response bevat nooit het correcte antwoord.
- De response bevat nooit een Open Trivia session token.
- De backend bewaart uitgegeven quiz-state server-side.
- Vragen en antwoorden worden veilig gedecodeerd voordat ze worden getoond.

### Story 4 - Antwoorden server-side controleren via `POST /checkanswers`

**Als** eindgebruiker  
**wil ik** mijn antwoorden kunnen indienen en laten nakijken  
**zodat** ik feedback krijg zonder dat de client de juiste antwoorden hoeft te kennen.

**WSJF**

- BV: 10
- TC: 10
- RR/OE: 10
- JS: 5
- Score: 6.00

**Scope**

- Implementeer `POST /checkanswers`.
- Accepteer alleen antwoorden die gekoppeld zijn aan een bestaande server-side quiz-sessie.
- Vergelijk ingediende antwoorden uitsluitend met server-side bewaarde waarheid.
- Weiger requests met onbekende, verlopen of gemanipuleerde quiz-identificaties.
- Beperk de response tot goed/fout-resultaten en eventueel totaalscore.
- Voorkom dat herhaalde submits misbruikt kunnen worden om antwoorden iteratief af te leiden.

**TDD-plan**

1. Schrijf een failing controller test voor `POST /checkanswers`.
2. Schrijf failing servicetests voor correcte en incorrecte antwoordcontrole.
3. Schrijf failing tests voor onbekende quiz-sessie, verlopen quiz-sessie en dubbele submit.
4. Implementeer minimale checklogica, expiry en replay-bescherming.
5. Refactor request- en responsemodellen.

**Acceptatiecriteria**

- `POST /checkanswers` accepteert antwoorden voor een geldige quiz-sessie.
- Antwoordcontrole gebeurt volledig server-side.
- Onbekende of verlopen quiz-sessies leveren een gecontroleerde fout op.
- De response bevat geen extra gegevens waarmee correcte antwoorden vooraf of iteratief afgeleid kunnen worden.

### Story 5 - Quiz-sessiebeheer en tijdelijke state

**Status:** DONE

**Als** systeem  
**wil ik** quiz-state tijdelijk en gecontroleerd beheren  
**zodat** de applicatie zonder database toch veilig kan vaststellen welke vragen zijn uitgegeven en hoe antwoorden
moeten worden gecontroleerd.

**WSJF**

- BV: 9
- TC: 9
- RR/OE: 10
- JS: 4
- Score: 7.00

**Scope**

- Ontwerp een in-memory quiz-session model met TTL.
- Koppel quiz-sessie aan uitgegeven vragen, correcte antwoorden en uitgiftetijd.
- Ruim verlopen quiz-sessies op of laat ze gecontroleerd verlopen.
- Markeer een quiz-sessie als gebruikt wanneer het controlepad éénmalig mag zijn.
- Log alleen technische metadata, geen correcte antwoorden of tokenwaarden.

**TDD-plan**

1. Schrijf failing tests voor aanmaken, ophalen en expireren van quiz-sessies.
2. Schrijf een failing test voor one-time submit of idempotent submitbeleid.
3. Schrijf een failing test die controleert dat gevoelige velden niet gelogd of geëxporteerd worden.
4. Implementeer minimale in-memory session-opslag en cleanup.

**Acceptatiecriteria**

- Quiz-sessies verlopen automatisch of worden gecontroleerd ongeldig.
- De server kan bepalen welke vragen bij welke quiz horen zonder clientvertrouwen.
- Gevoelige quiz-data blijft beperkt tot server-side state.
- Logging bevat geen correcte antwoorden of upstream tokens.

### Story 6 - Veilige en eenvoudige trivia UI

**Als** eindgebruiker  
**wil ik** een eenvoudige webinterface gebruiken om vragen te beantwoorden  
**zodat** ik de applicatie zonder API-kennis kan gebruiken.

**WSJF**

- BV: 9
- TC: 8
- RR/OE: 7
- JS: 5
- Score: 4.80

**Scope**

- Bouw een pagina waarop een gebruiker een trivia-ronde kan starten.
- Toon vragen en antwoordopties duidelijk in de UI.
- Maak het mogelijk om antwoorden te selecteren en te versturen.
- Gebruik alleen server-side aangeleverde veilige viewmodellen.
- Zorg dat de UI geen verborgen correcte antwoorden, tokens of debugdata bevat.

**TDD-plan**

1. Schrijf een failing MVC-test voor het renderen van de trivia-pagina.
2. Schrijf een failing test voor het submitten van antwoorden vanuit de UI-flow.
3. Schrijf een failing test die bevestigt dat gevoelige data niet in het HTML-model terechtkomt.
4. Implementeer controller en Thymeleaf views om de flow werkend te maken.
5. Refactor viewmodellen en template-structuur voor eenvoud.

**Acceptatiecriteria**

- De gebruiker kan via de browser een trivia-ronde starten.
- De gebruiker ziet vragen en antwoordopties op een duidelijke pagina.
- De gebruiker kan antwoorden insturen via de UI.
- De gerenderde HTML bevat geen correcte antwoorden, tokens of interne debugdata.

### Story 7 - Validatie, foutafhandeling en abuse controls

**Als** gebruiker en beheerder  
**wil ik** dat ongeldige input, upstream fouten en misbruik gecontroleerd worden afgehandeld  
**zodat** de applicatie stabiel en minder eenvoudig misbruikbaar blijft.

**WSJF**

- BV: 8
- TC: 8
- RR/OE: 10
- JS: 4
- Score: 6.50

**Scope**

- Valideer lege, incomplete of inconsistente answer payloads.
- Valideer requestgrenzen zoals vraagaantal en categoriegebruik.
- Vertaal upstream foutcodes naar gecontroleerde applicatiefouten.
- Voeg eenvoudige throttling of request-guarding toe aan quiz-aanmaak als dat nodig is om upstream limieten te
  beschermen.
- Zorg voor nette foutmeldingen in UI en API zonder interne details te lekken.

**TDD-plan**

1. Schrijf failing tests voor ongeldige requestpayloads.
2. Schrijf failing tests voor teveel gevraagde vragen en ongeldige parameters.
3. Schrijf failing tests voor upstream fouten en rate limiting.
4. Implementeer minimale validatie, exception mapping en abuse controls.

**Acceptatiecriteria**

- Ongeldige input levert een duidelijke maar niet-lekkende foutrespons op.
- De applicatie vraagt niet meer dan 50 vragen per upstream call aan.
- De applicatie accepteert niet meer dan 1 categorie per upstream request.
- Upstream rate limiting wordt gecontroleerd afgehandeld.

### Story 8 - README en security-notes opleveren

**Als** developer  
**wil ik** duidelijke build-, run-, test- en securityinstructies opleveren  
**zodat** de reviewer de applicatie correct kan starten en de gemaakte keuzes kan beoordelen.

**WSJF**

- BV: 8
- TC: 6
- RR/OE: 6
- JS: 2
- Score: 10.00

**Scope**

- Beschrijf vereisten, buildstappen en runstappen in `README`.
- Beschrijf hoe de Open Trivia integratie, token lifecycle en quiz-sessies werken.
- Beschrijf hoe tests uitgevoerd kunnen worden.
- Beschrijf expliciet welke securitykeuzes gemaakt zijn en welke scope bewust buiten beeld blijft.

**TDD-plan**

1. Verifieer eerst handmatig welke commando's en stappen echt nodig zijn.
2. Documenteer alleen bewezen build-, run- en teststappen.
3. Werk README bij nadat de implementatie stabiel is.

**Acceptatiecriteria**

- De root `README` bevat build- en run-instructies.
- De root `README` beschrijft testuitvoering.
- De root `README` beschrijft tokenbeheer, quiz-sessiegedrag en securitykeuzes op hoofdlijnen.

## Aanbevolen uitvoervolgorde

1. Story 1 - Security-first project bootstrap
2. Story 2 - Upstream Open Trivia client met token lifecycle
3. Story 3 - Veilige vraaguitgifte via `GET /questions`
4. Story 5 - Quiz-sessiebeheer en tijdelijke state
5. Story 4 - Antwoorden server-side controleren via `POST /checkanswers`
6. Story 7 - Validatie, foutafhandeling en abuse controls
7. Story 6 - Veilige en eenvoudige trivia UI
8. Story 8 - README en security-notes opleveren

## Definition of Done

- Elke story is test-first opgepakt.
- Elke story heeft expliciete acceptance criteria.
- Correcte antwoorden blijven server-side afgeschermd tot de checkfase.
- Open Trivia session tokens blijven server-side afgeschermd.
- Relevante tests voor security, session state en error handling zijn groen.
- Nieuwe code blijft beperkt tot de gevraagde scope zonder onnodige abstrahering.
