package quad.solutions.trivia.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

class TriviaCategoryTest {

	@Test
	void optionsMatchDocumentedOpenTriviaCategories() {
		assertThat(TriviaCategory.options())
				.extracting(TriviaCategory::id, TriviaCategory::displayName)
				.containsExactly(
						tuple(9, "General Knowledge"),
						tuple(10, "Entertainment: Books"),
						tuple(11, "Entertainment: Film"),
						tuple(12, "Entertainment: Music"),
						tuple(13, "Entertainment: Musicals & Theatres"),
						tuple(14, "Entertainment: Television"),
						tuple(15, "Entertainment: Video Games"),
						tuple(16, "Entertainment: Board Games"),
						tuple(17, "Science & Nature"),
						tuple(18, "Science: Computers"),
						tuple(19, "Science: Mathematics"),
						tuple(20, "Mythology"),
						tuple(21, "Sports"),
						tuple(22, "Geography"),
						tuple(23, "History"),
						tuple(24, "Politics"),
						tuple(25, "Art"),
						tuple(26, "Celebrities"),
						tuple(27, "Animals"),
						tuple(28, "Vehicles"),
						tuple(29, "Entertainment: Comics"),
						tuple(30, "Science: Gadgets"),
						tuple(31, "Entertainment: Japanese Anime & Manga"),
						tuple(32, "Entertainment: Cartoon & Animations"));
	}

}
