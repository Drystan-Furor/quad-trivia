const { defineConfig } = require("@playwright/test");

module.exports = defineConfig({
  testDir: "./e2e",
  timeout: 120000,
  use: {
    baseURL: "http://127.0.0.1:8097"
  },
  webServer: {
    command: "node e2e/start-trivia-app.mjs",
    url: "http://127.0.0.1:8097",
    timeout: 120000,
    reuseExistingServer: false
  }
});
