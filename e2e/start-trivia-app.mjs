import { createServer } from "node:http";
import { spawn } from "node:child_process";

const upstreamPort = 8098;
const appPort = 8097;
let tokenCounter = 0;

const upstream = createServer((request, response) => {
  const url = new URL(request.url, `http://127.0.0.1:${upstreamPort}`);
  response.setHeader("Content-Type", "application/json");

  if (url.pathname === "/api_token.php") {
    tokenCounter += 1;
    response.end(JSON.stringify({ response_code: 0, token: `e2e-token-${tokenCounter}` }));
    return;
  }

  if (url.pathname === "/api.php") {
    const category = url.searchParams.get("category") === "18" ? "Science: Computers" : "Science";
    const amount = Number(url.searchParams.get("amount") ?? "1");
    const questions = [
      {
        type: "multiple",
        difficulty: "easy",
        category,
        question: "What%20is%20H2O%3F",
        correct_answer: "Water",
        incorrect_answers: ["Fire", "Earth", "Air"]
      },
      {
        type: "multiple",
        difficulty: "medium",
        category,
        question: "What%20is%202%20%2B%202%3F",
        correct_answer: "4",
        incorrect_answers: ["3", "5", "22"]
      }
    ];
    response.end(JSON.stringify({
      response_code: 0,
      results: questions.slice(0, amount)
    }));
    return;
  }

  response.statusCode = 404;
  response.end(JSON.stringify({ error: "not found" }));
});

await new Promise((resolve) => upstream.listen(upstreamPort, "127.0.0.1", resolve));

const app = spawn("./mvnw", [
  "spring-boot:run",
  `-Dspring-boot.run.arguments=--server.port=${appPort} --open-trivia.base-url=http://127.0.0.1:${upstreamPort}`
], {
  stdio: "inherit"
});

const shutdown = () => {
  app.kill("SIGTERM");
  upstream.close(() => process.exit(0));
};

process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);

app.on("exit", (code) => {
  upstream.close(() => process.exit(code ?? 0));
});
