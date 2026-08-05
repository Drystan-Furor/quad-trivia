document.documentElement.classList.add("js");

const quizForm = document.querySelector("[data-quiz-form]");

if (quizForm) {
    quizForm.noValidate = true;
    const questions = Array.from(quizForm.querySelectorAll("[data-quiz-question]"));
    const previousButton = quizForm.querySelector("[data-previous-button]");
    const nextButton = quizForm.querySelector("[data-next-button]");
    const submitButton = quizForm.querySelector("[data-submit-button]");
    const progress = quizForm.querySelector("[data-progress]");
    const progressFill = quizForm.querySelector("[data-progress-fill]");
    const progressLabel = quizForm.querySelector("[data-progress-label]");
    const quizHeader = quizForm.querySelector(".quiz-header");
    const validationMessage = quizForm.querySelector("[data-quiz-validation]");
    let currentQuestion = 0;

    const hasAnswer = (question) => Boolean(question.querySelector('input[type="radio"]:checked'));

    const showQuestion = (index, moveFocus = false) => {
        currentQuestion = index;
        questions.forEach((question, questionIndex) => {
            question.hidden = questionIndex !== currentQuestion;
        });

        const questionNumber = currentQuestion + 1;
        const progressText = `Vraag ${questionNumber} van ${questions.length}`;
        progressLabel.textContent = progressText;
        progress.setAttribute("aria-valuenow", String(questionNumber));
        progress.setAttribute("aria-valuetext", progressText);
        progressFill.style.width = `${(questionNumber / questions.length) * 100}%`;
        previousButton.hidden = currentQuestion === 0;
        nextButton.hidden = currentQuestion === questions.length - 1;
        submitButton.hidden = currentQuestion !== questions.length - 1;
        validationMessage.textContent = "";

        if (moveFocus) {
            questions[currentQuestion].querySelector("[data-question-title]")?.focus({ preventScroll: true });
            quizHeader.scrollIntoView({
                behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth",
                block: "start"
            });
        }
    };

    const showMissingAnswer = (questionIndex) => {
        showQuestion(questionIndex);
        validationMessage.textContent = "Kies eerst een antwoord om verder te gaan.";
        questions[questionIndex].querySelector('input[type="radio"]')?.focus();
    };

    questions.forEach((question) => {
        question.addEventListener("change", () => {
            validationMessage.textContent = "";
        });
    });

    previousButton.addEventListener("click", () => showQuestion(currentQuestion - 1, true));
    nextButton.addEventListener("click", () => {
        if (!hasAnswer(questions[currentQuestion])) {
            showMissingAnswer(currentQuestion);
            return;
        }
        showQuestion(currentQuestion + 1, true);
    });

    quizForm.addEventListener("submit", (event) => {
        const missingAnswerIndex = questions.findIndex((question) => !hasAnswer(question));
        if (missingAnswerIndex !== -1) {
            event.preventDefault();
            showMissingAnswer(missingAnswerIndex);
        }
    });

    showQuestion(0);
}

document.querySelectorAll("[data-question-form], [data-answer-form]").forEach((form) => {
    const submitButton = form.querySelector("[data-submit-button]");
    const idleLabel = submitButton?.querySelector("[data-idle-label]");
    const loadingLabel = submitButton?.querySelector("[data-loading-label]");
    const loadingIndicator = submitButton?.querySelector("[data-loading-indicator]");
    const formStatus = form.querySelector("[data-form-status]");

    if (!submitButton || !idleLabel || !loadingLabel || !loadingIndicator) {
        return;
    }

    form.addEventListener("submit", (event) => {
        if (event.defaultPrevented) {
            return;
        }
        submitButton.disabled = true;
        submitButton.setAttribute("aria-busy", "true");
        idleLabel.hidden = true;
        loadingLabel.hidden = false;
        loadingIndicator.hidden = false;
        if (formStatus) {
            formStatus.textContent = loadingLabel.textContent;
        }
    });
});
