const form = document.getElementById("analyze-form");
const fileInput = document.getElementById("resume-file");
const fileLabel = document.getElementById("file-label");
const jobDescriptionInput = document.getElementById("job-description");
const submitBtn = document.getElementById("submit-btn");
const errorBox = document.getElementById("error-box");
const resultCard = document.getElementById("result-card");
const historyBody = document.getElementById("history-body");
const aiFeedbackBtn = document.getElementById("ai-feedback-btn");
const aiErrorBox = document.getElementById("ai-error-box");
const aiFeedbackResult = document.getElementById("ai-feedback-result");
const aiFeedbackText = document.getElementById("ai-feedback-text");

let currentAnalysisId = null;

fileInput.addEventListener("change", () => {
  fileLabel.textContent = fileInput.files.length
    ? fileInput.files[0].name
    : "Click to choose a resume (PDF, DOCX or TXT)";
});

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  hideError();

  if (!fileInput.files.length) {
    showError("Please choose a resume file first.");
    return;
  }

  const formData = new FormData();
  formData.append("resume", fileInput.files[0]);
  formData.append("jobDescription", jobDescriptionInput.value);

  setLoading(true);
  try {
    const response = await fetch("/api/analyses", { method: "POST", body: formData });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || "Something went wrong while analyzing the resume.");
    }
    renderResult(data);
    await loadHistory();
  } catch (err) {
    showError(err.message);
  } finally {
    setLoading(false);
  }
});

function setLoading(isLoading) {
  submitBtn.disabled = isLoading;
  submitBtn.textContent = isLoading ? "Analyzing..." : "Analyze Resume";
}

function showError(message) {
  errorBox.textContent = message;
  errorBox.hidden = false;
}

function hideError() {
  errorBox.hidden = true;
  errorBox.textContent = "";
}

function scoreColor(score) {
  if (score >= 75) return "#1d9a6c";
  if (score >= 50) return "#d9822b";
  return "#d84545";
}

aiFeedbackBtn.addEventListener("click", async () => {
  if (!currentAnalysisId) return;
  aiErrorBox.hidden = true;
  aiFeedbackResult.hidden = true;
  aiFeedbackBtn.disabled = true;
  aiFeedbackBtn.textContent = "Thinking...";

  try {
    const params = new URLSearchParams({ jobDescription: jobDescriptionInput.value });
    const response = await fetch(`/api/analyses/${currentAnalysisId}/ai-feedback?${params}`, {
      method: "POST",
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || "Could not get AI feedback.");
    }
    aiFeedbackText.textContent = data.aiFeedback;
    aiFeedbackResult.hidden = false;
  } catch (err) {
    aiErrorBox.textContent = err.message;
    aiErrorBox.hidden = false;
  } finally {
    aiFeedbackBtn.disabled = false;
    aiFeedbackBtn.textContent = "Get AI Feedback";
  }
});

function renderResult(data) {
  resultCard.hidden = false;
  currentAnalysisId = data.id;
  aiErrorBox.hidden = true;
  if (data.aiFeedback) {
    aiFeedbackText.textContent = data.aiFeedback;
    aiFeedbackResult.hidden = false;
  } else {
    aiFeedbackResult.hidden = true;
  }

  const circle = document.getElementById("overall-score");
  circle.textContent = data.overallScore;
  const color = scoreColor(data.overallScore);
  circle.style.background = `conic-gradient(${color} ${data.overallScore * 3.6}deg, #d9dee5 0deg)`;
  circle.style.color = data.overallScore >= 50 ? "white" : "white";

  document.getElementById("file-meta").textContent =
    `${data.fileName} · ${data.wordCount} words` + (data.jobTitle ? ` · scored against job description` : "");

  const subscores = document.getElementById("subscores");
  subscores.innerHTML = "";
  [
    ["Keywords", data.keywordScore],
    ["Sections", data.sectionScore],
    ["Contact Info", data.contactScore],
    ["Impact", data.impactScore],
    ["Format", data.formatScore],
  ].forEach(([label, value]) => {
    const div = document.createElement("div");
    div.className = "subscore";
    div.innerHTML = `<div class="value" style="color:${scoreColor(value)}">${value}</div><div class="label">${label}</div>`;
    subscores.appendChild(div);
  });

  renderChips("matched-skills", data.matchedSkills);
  renderChips("missing-skills", data.missingSkills);

  const suggestions = document.getElementById("suggestions");
  suggestions.innerHTML = "";
  data.suggestions.forEach((s) => {
    const li = document.createElement("li");
    li.textContent = s;
    suggestions.appendChild(li);
  });

  resultCard.scrollIntoView({ behavior: "smooth", block: "start" });
}

function renderChips(containerId, items) {
  const container = document.getElementById(containerId);
  container.innerHTML = "";
  if (!items.length) {
    const span = document.createElement("span");
    span.textContent = "None";
    span.style.opacity = "0.6";
    container.appendChild(span);
    return;
  }
  items.forEach((item) => {
    const span = document.createElement("span");
    span.textContent = item;
    container.appendChild(span);
  });
}

async function loadHistory() {
  const response = await fetch("/api/analyses");
  const items = await response.json();
  historyBody.innerHTML = "";
  items.forEach((item) => {
    const tr = document.createElement("tr");
    const scoreClass = item.overallScore >= 75 ? "score-good" : item.overallScore >= 50 ? "score-warn" : "score-bad";
    tr.innerHTML = `
      <td>${item.fileName}</td>
      <td class="${scoreClass}">${item.overallScore}</td>
      <td>${new Date(item.createdAt).toLocaleString()}</td>
      <td><button class="link-btn" data-id="${item.id}">View</button></td>
    `;
    historyBody.appendChild(tr);
  });

  historyBody.querySelectorAll("button[data-id]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const response = await fetch(`/api/analyses/${btn.dataset.id}`);
      const data = await response.json();
      renderResult(data);
    });
  });
}

loadHistory().catch(() => {});
