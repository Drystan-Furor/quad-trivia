const { test, expect } = require("@playwright/test");

test("user can start and submit a safe trivia round", async ({ page }) => {
  await page.goto("/");

  await expect(page.locator("html")).toHaveAttribute("lang", "nl");
  await expect(page.locator('head link[href="/css/app.css"]')).toHaveCount(1);
  await expect(page.locator('head script[src="https://cdn.tailwindcss.com"]')).toHaveCount(0);
  await expect(page.locator('head script[src="/js/global.js"][defer]')).toHaveCount(1);
  await page.getByLabel("Aantal vragen").fill("2");
  await page.getByLabel("Categorie").selectOption("18");
  await page.getByLabel("Moeilijkheid").selectOption("medium");
  await page.getByLabel("Vraagtype").selectOption("multiple");
  await page.getByRole("button", { name: "Start quiz" }).click();

  await expect(page.getByRole("heading", { name: "Jouw quiz" })).toBeVisible();
  await expect(page.locator("[data-quiz-question]:not([hidden])").getByText("Science: Computers")).toBeVisible();
  await expect(page.getByRole("heading", { name: "What is H2O?" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "What is 2 + 2?" })).toBeHidden();
  await expect(page.getByText("Vraag 1 van 2")).toBeVisible();
  await expect(page.locator('head script[src="/js/global.js"][defer]')).toHaveCount(1);

  const quizHtml = await page.content();
  expect(quizHtml).not.toContain("correctAnswer");
  expect(quizHtml).not.toContain("e2e-token");
  expect(quizHtml).not.toContain("debug");

  await page.getByLabel("Water").check();
  await page.getByRole("button", { name: "Volgende vraag" }).click();
  await expect(page.getByRole("heading", { name: "What is 2 + 2?" })).toBeVisible();
  await expect(page.getByText("Vraag 2 van 2")).toBeVisible();
  await page.getByLabel("3").check();
  await page.getByRole("button", { name: "Verstuur antwoorden" }).click();

  await expect(page.getByText("Score 1 / 2")).toBeVisible();
  await expect(page.getByText("Jouw antwoord: Water")).toBeVisible();
  await expect(page.getByText("Juiste antwoord: 4")).toBeVisible();
  const resultsHtml = await page.content();
  expect(resultsHtml).not.toContain("correctAnswer");
  expect(resultsHtml).not.toContain("e2e-token");
  expect(resultsHtml).not.toContain("debug");
});

test("start button disables and shows progress while quiz is loading", async ({ page }) => {
  await page.goto("/");
  await page.evaluate(() => {
    document.querySelector("[data-question-form]")?.addEventListener("submit", (event) => {
      event.preventDefault();
    });
  });

  const startButton = page.locator("[data-submit-button]");
  await startButton.click({ noWaitAfter: true });
  await expect(startButton).toBeDisabled();
  await expect(page.locator("[data-idle-label]")).toBeHidden();
  await expect(page.locator("[data-loading-label]")).toBeVisible();
  await expect(page.locator("[data-loading-indicator]")).toBeVisible();
});

test("submit button disables and shows progress while answers are processing", async ({ page }) => {
  await page.goto("/");
  await page.waitForTimeout(5000);
  await page.getByLabel("Aantal vragen").fill("1");
  await page.getByRole("button", { name: "Start quiz" }).click();
  await expect(page.getByRole("heading", { name: "Jouw quiz" })).toBeVisible();
  await page.getByLabel("Water").check();
  await page.evaluate(() => {
    document.querySelector("[data-answer-form]")?.addEventListener("submit", (event) => {
      event.preventDefault();
    });
  });

  const submitButton = page.locator("[data-submit-button]");
  await submitButton.click({ noWaitAfter: true });
  await expect(submitButton).toBeDisabled();
  await expect(page.locator("[data-idle-label]")).toBeHidden();
  await expect(page.locator("[data-loading-label]")).toBeVisible();
  await expect(page.locator("[data-loading-indicator]")).toBeVisible();
});
