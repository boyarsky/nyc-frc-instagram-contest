package com.jeanneboyarsky.instagram;

import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.htmlunit.*;

import com.fasterxml.jackson.databind.*;

public class CountLikesIT {

	private static final String TAG = "frcnyc2020";

	private WebDriver driver;

	@BeforeEach
	void setup() {
		driver = new HtmlUnitDriver();
	}

	@AfterEach
	void tearDown() {
		if (driver != null) {
			driver.close();
		}
	}

	@Test
	void graphQlJson() throws Exception {
		// https://stackoverflow.com/questions/43655098/how-to-get-all-instagram-posts-by-hashtag-with-the-api-not-only-the-posts-of-my
		// "count" shows up 258 times (this is three times per image)
		// 1) edge_media_to_comment
		// 2) edge_liked_by
		// 3) edge_media_preview_like - looks same as #2
		String json = getJson();

		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(json);

		List<JsonNode> nodes = rootNode.findValues("node");

		Map<String, Integer> result = nodes.stream()
				// node occurs at multiple levels; we only want the ones that go with posts
				.filter(this::isPost)
				.collect(Collectors.toMap(this::getUrl, this::getNumLikes,
						// ignore duplicates by choosing either (top/featured posts appear twice)
						(k, v) -> v));

		printDescendingByLikes(result);

	}

	private String getUrl(JsonNode node) {
		JsonNode shortCodeNode = node.findValue("shortcode");
		return "https://instagram.com/p/" + shortCodeNode.asText();
	}

	private int getNumLikes(JsonNode node) {
		JsonNode likeNode = node.get("edge_liked_by");
		return likeNode.get("count").asInt();
	}

	private boolean isPost(JsonNode node) {
		return node.findValue("display_url") != null;
	}

	private String getJson() {
		driver.get("https://www.instagram.com/explore/tags/" + TAG + "/?__a=1");
		String pageSource = driver.getPageSource();

		return removeHtmlTagsSinceReturnedAsWebPage(pageSource);
	}

	private String removeHtmlTagsSinceReturnedAsWebPage(String pageSource) {
		String openTag = "<";
		String closeTag = ">";
		String anyCharactersInTag = "[^>]*";

		String regex = openTag + anyCharactersInTag + closeTag;
		return pageSource.replaceAll(regex, "");
	}

	private void printDescendingByLikes(Map<String, Integer> result) {
		Comparator<Entry<String, Integer>> comparator = Comparator.comparing((Entry<String, Integer> e) -> e.getValue())
				.reversed();
		result.entrySet().stream()
				.sorted(comparator)
				.map(e -> e.getValue() + "\t" + e.getKey())
				.forEach(System.out::println);
	}

}
