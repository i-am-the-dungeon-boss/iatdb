package com.watabou.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class StringsTest {

	@Test
	@DisplayName("isBlank treats null empty and whitespace as blank")
	void isBlankNullEmptyAndWhitespace() {
		Assertions.assertThat(Strings.isBlank(null)).isTrue();
		Assertions.assertThat(Strings.isBlank("")).isTrue();
		Assertions.assertThat(Strings.isBlank("   ")).isTrue();
		Assertions.assertThat(Strings.isBlank("\t\n")).isTrue();
		Assertions.assertThat(Strings.isBlank("ok")).isFalse();
		Assertions.assertThat(Strings.isBlank(" ok ")).isFalse();
	}

	@Test
	@DisplayName("join concatenates iterable with delimiter")
	void joinConcatenatesWithDelimiter() {
		List<String> parts = new ArrayList<>();
		parts.add("a");
		parts.add("b");
		parts.add("c");
		Assertions.assertThat(Strings.join(",", parts)).isEqualTo("a,b,c");
		Assertions.assertThat(Strings.join(",", new ArrayList<String>())).isEqualTo("");
		Assertions.assertThat(Strings.join(",", null)).isEqualTo("");
	}
}
