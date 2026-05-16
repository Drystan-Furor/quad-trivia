# Front-end Backlog - Quad Trivia

## Story 1 - Disable submit during answer processing

**Status:** DONE

**Als** gebruiker  
**wil ik** dat de submitknop direct niet meer klikbaar is nadat ik mijn antwoorden verstuur  
**zodat** ik niet per ongeluk meerdere submits verstuur terwijl de backend nog bezig is.

**Spec**

- De quizpagina rendert een submitknop voor het antwoordenformulier.
- Zodra het antwoordenformulier wordt verstuurd, wordt dezelfde submitknop disabled.
- Tijdens de lopende request accepteert de submitknop geen extra clicks of submits meer.
- De loading-state wordt client-side geactiveerd zonder extra backend-endpoint.
- Na een succesvolle response navigeert de browser door naar de resultatenpagina.

**TDD-plan**

1. Schrijf een failing MVC-test die verifieert dat de quizpagina submit-loading hooks rendert.
2. Schrijf een failing Playwright-test die een vertraagde submit simuleert en controleert dat de knop disabled wordt.
3. Implementeer de minimale frontendlogica om de knop bij submit direct te vergrendelen.
4. Refactor alleen wanneer de oplossing nog eenvoudiger of duidelijker kan zonder extra gedrag.

**Acceptatiecriteria**

- De submitknop krijgt direct een disabled state na het starten van submit.
- Een tweede click op de knop tijdens dezelfde request is niet mogelijk.
- De bestaande submitflow naar de resultatenpagina blijft werken.

## Story 2 - Show processing feedback on submit button

**Status:** DONE

**Als** gebruiker  
**wil ik** een spinner of andere zichtbare verwerkingstatus op de submitknop zien  
**zodat** ik bevestiging krijg dat mijn actie verwerkt wordt en ik moet wachten.

**Spec**

- De quizpagina rendert een visuele processing-indicator in of naast de submitknop.
- De indicator is standaard verborgen voordat submit start.
- Zodra het antwoordenformulier wordt verstuurd, wordt de indicator zichtbaar en verandert het knoplabel naar een lopende status.
- De processing-indicator blijft zichtbaar totdat de response de pagina vervangt.
- De feedback is zichtbaar zonder afhankelijk te zijn van browser-alerts of extra pagina-elementen buiten de knop.

**TDD-plan**

1. Breid de failing MVC-test uit zodat de knop een verborgen processing-indicator en processing-label bevat.
2. Breid de failing Playwright-test uit zodat tijdens een vertraagde submit de indicator zichtbaar is en het label wijzigt.
3. Implementeer de minimale UI-wijziging in de quiztemplate om de indicator en labelwissel te tonen.
4. Houd de markup toegankelijk en beperkt tot de submitknop.

**Acceptatiecriteria**

- De submitknop toont tijdens submit een zichtbare processing-indicator.
- De submitknop toont tijdens submit een duidelijk wachtlabel.
- De indicator is vóór submit niet zichtbaar.
