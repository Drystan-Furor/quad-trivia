# Curl commands om de API te testen

Deze commando's gaan ervan uit dat de Spring Boot applicatie lokaal draait op
`http://localhost:8080`.

Voor de voorbeelden die waarden uit JSON halen is `jq` handig:

```sh
jq --version
```

## Basisinstellingen

```sh
BASE_URL="http://localhost:8080"
COOKIE_JAR="$(mktemp)"
```

## CSRF-token ophalen

`POST /checkanswers` gebruikt JSON, maar Spring Security verwacht nog steeds een
CSRF-token. Haal daarom eerst een sessie-cookie en token op via de homepage:

```sh
CSRF_TOKEN="$(
  curl -sS -c "$COOKIE_JAR" "$BASE_URL/" \
    | sed -n 's/.*name="_csrf" value="\([^"]*\)".*/\1/p'
)"

printf 'CSRF token: %s\n' "$CSRF_TOKEN"
```

## Vragen ophalen

Standaard haalt de API 5 vragen op:

```sh
curl -i -sS "$BASE_URL/questions"
```

Haal 1 vraag op uit de categorie `Science: Computers`:

```sh
curl -i -sS "$BASE_URL/questions?amount=1&category=18"
```

Let op: de applicatie blokkeert nieuwe quizaanvragen die binnen 5 seconden na
elkaar komen. Wacht dus kort tussen meerdere calls naar `/questions`:

```sh
sleep 5
```

## Quiz ophalen en waarden bewaren

Gebruik deze flow om een quiz te maken en de velden te bewaren die nodig zijn
voor `POST /checkanswers`:

```sh
QUIZ_JSON="$(curl -sS "$BASE_URL/questions?amount=1&category=18")"

printf '%s\n' "$QUIZ_JSON" | jq

QUIZ_ID="$(printf '%s\n' "$QUIZ_JSON" | jq -r '.quizId')"
QUESTION_ID="$(printf '%s\n' "$QUIZ_JSON" | jq -r '.questions[0].id')"
ANSWER="$(printf '%s\n' "$QUIZ_JSON" | jq -r '.questions[0].options[0]')"

printf 'quizId=%s\nquestionId=%s\nanswer=%s\n' "$QUIZ_ID" "$QUESTION_ID" "$ANSWER"
```

## Antwoord controleren

Post een antwoord voor de eerder opgehaalde quiz:

```sh
curl -i -sS \
  -b "$COOKIE_JAR" \
  -H "X-CSRF-TOKEN: $CSRF_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$(
    jq -n \
      --arg quizId "$QUIZ_ID" \
      --arg questionId "$QUESTION_ID" \
      --arg answer "$ANSWER" \
      '{
        quizId: $quizId,
        answers: [
          {
            questionId: $questionId,
            answer: $answer
          }
        ]
      }'
  )" \
  "$BASE_URL/checkanswers"
```

Verwachte responsevorm:

```json
{
  "score": 0,
  "totalQuestions": 1,
  "results": [
    {
      "questionId": "uuid-van-de-vraag",
      "correct": false
    }
  ]
}
```

`score` kan `0` of `1` zijn, omdat het voorbeeld de eerste antwoordoptie kiest
zonder te weten welke optie correct is.

## Validatiefouten testen

`amount` mag maximaal `50` zijn:

```sh
curl -i -sS "$BASE_URL/questions?amount=51"
```

`category` moet positief zijn:

```sh
curl -i -sS "$BASE_URL/questions?category=0"
```

`answers` mag niet leeg zijn:

```sh
curl -i -sS \
  -b "$COOKIE_JAR" \
  -H "X-CSRF-TOKEN: $CSRF_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"quizId":"'"$QUIZ_ID"'","answers":[]}' \
  "$BASE_URL/checkanswers"
```

Verwachte error-responsevorm:

```json
{
  "message": "Invalid request payload"
}
```
