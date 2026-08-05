import { ui } from "./store.js";
import { initTheme, onThemeToggle } from "./portfolio-ui.js";
import { attachEvents, refreshPortfolioState, refreshRecommendations } from "./portfolio-actions.js";

init();

async function init() {
  initTheme();
  attachEvents(onThemeToggle);
  ui.removeQuantityInput.step = "1";
  ui.removeQuantityInput.min = "1";
  await Promise.all([refreshPortfolioState(), refreshRecommendations()]);
}
