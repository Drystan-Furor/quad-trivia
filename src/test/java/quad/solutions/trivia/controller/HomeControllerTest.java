package quad.solutions.trivia.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HomeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void getRootReturnsHomePage() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("home"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("max-w-4xl mx-auto")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/questions\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<script defer src=\"/js/global.js\"></script>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"category\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"\">Any Category</option>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"18\">Science: Computers</option>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"32\">Entertainment: Cartoon &amp; Animations</option>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"difficulty\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"any\">Any Difficulty</option>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"easy\">Easy</option>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"medium\">Medium</option>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"hard\">Hard</option>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"type\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"any\">Any Type</option>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"multiple\">Multiple Choice</option>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<option value=\"boolean\">True / False</option>")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-submit-button")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-idle-label")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-loading-label")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-loading-indicator")))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("document.addEventListener(\"DOMContentLoaded\""))))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Start ronde")));
	}

}
