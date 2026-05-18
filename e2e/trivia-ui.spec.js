const { test, expect } = require("@playwright/test");

test("user can start and submit a safe trivia round", async ({ page }) => {
  await page.goto("/");

  await expect(page.locator("main")).toHaveClass(/max-w-4xl mx-auto/);
  await expect(page.locator('head script[src="/js/global.js"][defer]')).toHaveCount(1);
  await page.getByLabel("Categorie").selectOption("18");
  await page.getByRole("button", { name: "Start ronde" }).click();

  await expect(page.getByRole("heading", { name: "Trivia ronde" })).toBeVisible();
  await expect(page.getByText("Science: Computers")).toBeVisible();
  await expect(page.getByText("What is H2O?")).toBeVisible();
  await expect(page.locator('head script[src="/js/global.js"][defer]')).toHaveCount(1);

  const quizHtml = await page.content();
  expect(quizHtml).not.toContain("correctAnswer");
  expect(quizHtml).not.toContain("e2e-token");
  expect(quizHtml).not.toContain("debug");

  await page.getByLabel("Water").check();
  await page.getByRole("button", { name: "Verstuur antwoorden" }).click();

  await expect(page.getByText("Score 1 / 1")).toBeVisible();
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
  await page.getByRole("button", { name: "Start ronde" }).click();
  await expect(page.getByRole("heading", { name: "Trivia ronde" })).toBeVisible();
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
