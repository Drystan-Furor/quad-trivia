const form = document.querySelector("[data-answer-form]");
const submitButton = form?.querySelector("[data-submit-button]");
const idleLabel = submitButton?.querySelector("[data-idle-label]");
const loadingLabel = submitButton?.querySelector("[data-loading-label]");
const loadingIndicator = submitButton?.querySelector("[data-loading-indicator]");

if (form && submitButton && idleLabel && loadingLabel && loadingIndicator) {
    form.addEventListener("submit", () => {
        submitButton.disabled = true;
        submitButton.setAttribute("aria-busy", "true");
        idleLabel.classList.add("hidden");
        loadingLabel.classList.remove("hidden");
        loadingIndicator.classList.remove("hidden");
    });
}
