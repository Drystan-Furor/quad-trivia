## Story 1 - Move quiz page behavior into a static script

**Status:** DONE

**Als** developer  
**wil ik** dat de JavaScript in `src/main/resources/templates/quiz.html` wordt verplaatst naar `src/main/resources/static/js/quiz.js`  
**zodat** ik de quizpagina beter kan onderhouden.

**Spec**

- De quiztemplate bevat geen inline JavaScript meer voor submitgedrag.
- De quizpagina laadt `"/js/quiz.js"` vanuit `<head>` met het `defer` attribuut.
- Het verplaatste script behoudt het bestaande DOM-contract van de quizpagina.
- De pagina houdt quiz-submitgedrag buiten de template en binnen static resources.

**TDD-plan**

1. Schrijf een failing MVC-test die een deferred script tag naar `"/js/quiz.js"` verwacht en een inline scriptblock in `quiz.html` afwijst.
2. Schrijf een failing Playwright-test die verifieert dat de quizflow blijft werken terwijl het script buiten de template geladen wordt.
3. Verplaats de minimale JavaScript naar `src/main/resources/static/js/quiz.js` en verwijder de inline code uit de template.
4. Refactor alleen wanneer de oplossing eenvoudiger wordt zonder extra gedrag toe te voegen.

**Acceptatiecriteria**

- `quiz.html` bevat geen inline pagina-JavaScript meer.
- `quiz.html` laadt `"/js/quiz.js"` met `defer` vanuit `<head>`.
- De quizflow blijft werken na het verplaatsen van het script.

## Story 2 - Standardize page script loading structure

**Status:** DONE

**Als** developer  
**wil ik** dat pagina-JavaScript volgens dezelfde static-resource-structuur wordt geladen en met `defer` in `<head>` staat  
**zodat** de frontend een consistenter ontwikkelpad krijgt.

**Spec**

- Templates blijven verantwoordelijk voor HTML-markup.
- Pagina-specifieke JavaScript staat onder `src/main/resources/static/js`.
- Pagina's met eigen client-side gedrag laden hun script via een deferred script tag in `<head>`.
- Binnen de huidige scope is de quizpagina de referentie-implementatie van deze structuur.

**TDD-plan**

1. Breid de failing MVC-asserties uit zodat de quiztemplate de beoogde static-resource-structuur expliciet rendert.
2. Houd de failing end-to-end flow in stand zodat de structurele wijziging aantoonbaar geen regressie veroorzaakt.
3. Implementeer de minimale resource-structuur en template-wiring voor de quizpagina.
4. Laat overige templates ongemoeid zolang ze geen eigen pagina-JavaScript bevatten.

**Acceptatiecriteria**

- Pagina-JavaScript voor de quiz staat onder `src/main/resources/static/js`.
- De quiztemplate laadt dat script deferred vanuit `<head>`.
- De wijziging blijft beperkt tot de pagina met bestaand client-side gedrag in deze scope.
