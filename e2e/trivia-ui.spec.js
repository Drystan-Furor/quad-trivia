const { test, expect } = require("@playwright/test");

test("user can start and submit a safe trivia round", async ({ page }) => {
  await page.goto("/");

  await expect(page.locator("main")).toHaveClass(/max-w-4xl mx-auto/);
  await page.getByRole("button", { name: "Start ronde" }).click();

  await expect(page.getByRole("heading", { name: "Trivia ronde" })).toBeVisible();
  await expect(page.getByText("What is H2O?")).toBeVisible();

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

test("submit button disables and shows progress while answers are processing", async ({ page }) => {
  await page.route("**/checkanswers", async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 750));
    await route.continue();
  });

  await page.goto("/");
  await page.getByRole("button", { name: "Start ronde" }).click();
  await page.getByLabel("Water").check();

  const submitButton = page.locator("[data-submit-button]");
  await Promise.all([
    page.waitForFunction(() => {
      const button = document.querySelector("[data-submit-button]");
      const idleLabel = document.querySelector("[data-idle-label]");
      const loadingLabel = document.querySelector("[data-loading-label]");
      const loadingIndicator = document.querySelector("[data-loading-indicator]");

      return Boolean(
        button?.disabled &&
        idleLabel?.classList.contains("hidden") &&
        !loadingLabel?.classList.contains("hidden") &&
        !loadingIndicator?.classList.contains("hidden")
      );
    }),
    submitButton.click({ noWaitAfter: true })
  ]);

  await expect(page.getByText("Score 1 / 1")).toBeVisible();
});
