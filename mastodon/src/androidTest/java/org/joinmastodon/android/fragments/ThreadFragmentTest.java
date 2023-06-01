package org.joinmastodon.android.fragments;

import static org.junit.Assert.*;

import android.util.Pair;

import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusContext;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class ThreadFragmentTest {

	private Status fakeStatus(String id, String inReplyTo) {
		Status status = Status.ofFake(id, null, null);
		status.inReplyToId = inReplyTo;
		return status;
	}

	@Test
	public void countAncestryLevels() {
		StatusContext context = new StatusContext();
		context.ancestors = List.of(
				fakeStatus("oldest ancestor", null),
				fakeStatus("younger ancestor", "oldest ancestor")
		);
		context.descendants = List.of(
				fakeStatus("first reply", "main status"),
				fakeStatus("reply to first reply", "first reply"),
				fakeStatus("third level reply", "reply to first reply"),
				fakeStatus("another reply", "main status")
		);
		List<Pair<String, Integer>> actual =
				ThreadFragment.countAncestryLevels("main status", context);

		List<Pair<String, Integer>> expected = List.of(
				Pair.create("oldest ancestor", -2),
				Pair.create("younger ancestor", -1),
				Pair.create("main status", 0),
				Pair.create("first reply", 1),
				Pair.create("reply to first reply", 2),
				Pair.create("third level reply", 3),
				Pair.create("another reply", 1)
		);
		assertEquals(
				"status ids are in the right order",
				expected.stream().map(p -> p.first).collect(Collectors.toList()),
				actual.stream().map(p -> p.first).collect(Collectors.toList())
		);
		assertEquals(
				"counted levels match",
				expected.stream().map(p -> p.second).collect(Collectors.toList()),
				actual.stream().map(p -> p.second).collect(Collectors.toList())
		);
	}

	@Test
	public void sortStatusContext() {
		StatusContext context = new StatusContext();
		context.ancestors = List.of(
				fakeStatus("younger ancestor", "oldest ancestor"),
				fakeStatus("oldest ancestor", null)
		);
		context.descendants = List.of(
				fakeStatus("reply to first reply", "first reply"),
				fakeStatus("third level reply", "reply to first reply"),
				fakeStatus("first reply", "main status"),
				fakeStatus("another reply", "main status")
		);

		ThreadFragment.sortStatusContext(
				fakeStatus("main status", "younger ancestor"),
				context
		);
		List<Status> expectedAncestors = List.of(
				fakeStatus("oldest ancestor", null),
				fakeStatus("younger ancestor", "oldest ancestor")
		);
		List<Status> expectedDescendants = List.of(
				fakeStatus("first reply", "main status"),
				fakeStatus("reply to first reply", "first reply"),
				fakeStatus("third level reply", "reply to first reply"),
				fakeStatus("another reply", "main status")
		);

		// TODO: ??? i have no idea how this code works. it certainly doesn't return what i'd expect
	}
}