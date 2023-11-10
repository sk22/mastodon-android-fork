package org.joinmastodon.android.ui.utils;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.res.Resources;
import android.util.Pair;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.model.Instance;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class UiUtilsTest {
	@BeforeClass
	public static void createDummySession() {
		Instance dummyInstance = new Instance();
		dummyInstance.uri = "test.tld";
		Account dummyAccount = new Account();
		dummyAccount.id = "123456";
		AccountSessionManager.getInstance().addAccount(dummyInstance, null, dummyAccount, null, null);
	}

	@AfterClass
	public static void cleanUp() {
		AccountSessionManager.getInstance().removeAccount("test.tld_123456");
	}

	@Test
	public void parseFediverseHandle() {
		assertEquals(
				Optional.of(Pair.create("megalodon", Optional.of("floss.social"))),
				UiUtils.parseFediverseHandle("megalodon@floss.social")
		);

		assertEquals(
				Optional.of(Pair.create("megalodon", Optional.of("floss.social"))),
				UiUtils.parseFediverseHandle("@megalodon@floss.social")
		);

		assertEquals(
				Optional.of(Pair.create("megalodon", Optional.empty())),
				UiUtils.parseFediverseHandle("@megalodon")
		);

		assertEquals(
				Optional.of(Pair.create("megalodon", Optional.of("floss.social"))),
				UiUtils.parseFediverseHandle("mailto:megalodon@floss.social")
		);

		assertEquals(
				Optional.empty(),
				UiUtils.parseFediverseHandle("megalodon")
		);

		assertEquals(
				Optional.empty(),
				UiUtils.parseFediverseHandle("this is not a fedi handle")
		);

		assertEquals(
				Optional.empty(),
				UiUtils.parseFediverseHandle("not@a-domain")
		);
	}

	@Test
	public void acctMatches() {
		assertTrue("local account, domain not specified", UiUtils.acctMatches(
				"test.tld_123456",
				"someone",
				"someone",
				null
		));

		assertTrue("domain not specified", UiUtils.acctMatches(
				"test.tld_123456",
				"someone@somewhere.social",
				"someone",
				null
		));

		assertTrue("local account, domain specified, different casing", UiUtils.acctMatches(
				"test.tld_123456",
				"SomeOne",
				"someone",
				"Test.TLD"
		));

		assertFalse("username doesn't match", UiUtils.acctMatches(
				"test.tld_123456",
				"someone-else@somewhere.social",
				"someone",
				"somewhere.social"
		));

		assertFalse("domain doesn't match", UiUtils.acctMatches(
				"test.tld_123456",
				"someone@somewhere.social",
				"someone",
				"somewhere.else"
		));
	}

	private final String[] args = new String[] { "Megalodon", "♡" };

	private String gen(String format, CharSequence... args) {
		return UiUtils.generateFormattedString(format, args).toString();
	}
	@Test
	public void generateFormattedString() {
		assertEquals(
				"ordered substitution",
				"Megalodon reacted with ♡",
				gen("%s reacted with %s", args)
		);

		assertEquals(
				"1 2 3 4 5",
				gen("%s %s %s %s %s", "1", "2", "3", "4", "5")
		);

		assertEquals(
				"indexed substitution",
				"with ♡ was reacted by Megalodon",
				gen("with %2$s was reacted by %1$s", args)
		);

		assertEquals(
				"indexed substitution, in order",
				"Megalodon reacted with ♡",
				gen("%1$s reacted with %2$s", args)
		);

		assertEquals(
				"indexed substitution, 0-based",
				"Megalodon reacted with ♡",
				gen("%0$s reacted with %1$s", args)
		);

		assertEquals(
				"indexed substitution, 5 items",
				"5 4 3 2 1",
				gen("%5$s %4$s %3$s %2$s %1$s", "1", "2", "3", "4", "5")
		);

		assertEquals(
				"one argument missing",
				"Megalodon reacted with ♡",
				gen("reacted with %s", args)
		);

		assertEquals(
				"multiple arguments missing",
				"Megalodon reacted with ♡",
				gen("reacted with", args)
		);

		assertEquals(
				"multiple arguments missing, numbers in expeced positions",
				"1 2 x 3 4 5",
				gen("%s x %s", "1", "2", "3", "4", "5")
		);

		assertEquals(
				"one leading and trailing space",
				"Megalodon reacted with ♡",
				gen(" reacted with ", args)
		);

		assertEquals(
				"multiple leading and trailing spaces",
				"Megalodon  reacted  with  ♡",
				gen("  reacted  with  ", args)
		);

		assertEquals(
				"invalid format produces expected invalid result",
				"Megalodon reacted with % s ♡",
				gen("reacted with % s", args)
		);

		assertEquals(
				"plain string as format, all arguments get added",
				"a x b c",
				gen("x", new String[] { "a", "b", "c" })
		);

		assertEquals("empty input produces empty output", "", gen(""));

		// not supported:
//		assertEquals("a b a", gen("%1$s %2$s %2$s %1$s", new String[] { "a", "b", "c" }));
//		assertEquals("x", gen("%s %1$s %2$s %1$s %s", new String[] { "a", "b", "c" }));
	}

	private AccountField makeField(String name, String value) {
		AccountField f = new AccountField();
		f.name = name;
		f.value = value;
		return f;
	}

	private Account fakeAccount(AccountField... fields) {
		Account a = new Account();
		a.fields = Arrays.asList(fields);
		return a;
	}

	@Test
	public void extractPronouns() {
		assertEquals("they", UiUtils.extractPronouns(MastodonApp.context, fakeAccount(
				makeField("name and pronouns", "https://pronouns.site"),
				makeField("pronouns", "they"),
				makeField("pronouns something", "bla bla")
		)).orElseThrow());

		assertTrue(UiUtils.extractPronouns(MastodonApp.context, fakeAccount()).isEmpty());

		assertEquals("it/its", UiUtils.extractPronouns(MastodonApp.context, fakeAccount(
				makeField("pronouns pronouns pronouns", "hi hi hi"),
				makeField("pronouns", "it/its"),
				makeField("the pro's nouns", "professional")
		)).orElseThrow());

		assertEquals("she/he", UiUtils.extractPronouns(MastodonApp.context, fakeAccount(
				makeField("my name is", "jeanette shork, apparently"),
				makeField("my pronouns are", "she/he")
		)).orElseThrow());

		assertEquals("they/them", UiUtils.extractPronouns(MastodonApp.context, fakeAccount(
				makeField("pronouns", "https://pronouns.cc/pronouns/they/them")
		)).orElseThrow());

		Context german = UiUtils.getLocalizedContext(MastodonApp.context, Locale.GERMAN);

		assertEquals("sie/ihr", UiUtils.extractPronouns(german, fakeAccount(
				makeField("pronomen lauten", "sie/ihr"),
				makeField("pronouns are", "she/her"),
				makeField("die pronomen", "stehen oben")
		)).orElseThrow());

		assertEquals("er/ihm", UiUtils.extractPronouns(german, fakeAccount(
				makeField("die pronomen", "stehen unten"),
				makeField("pronomen sind", "er/ihm"),
				makeField("pronouns are", "he/him")
		)).orElseThrow());

		assertEquals("* (asterisk)", UiUtils.extractPronouns(MastodonApp.context, fakeAccount(
				makeField("pronouns", "-- * (asterisk) --")
		)).orElseThrow());

		assertEquals("they/(she?)", UiUtils.extractPronouns(MastodonApp.context, fakeAccount(
				makeField("pronouns", "they/(she?)...")
		)).orElseThrow());
	}
}