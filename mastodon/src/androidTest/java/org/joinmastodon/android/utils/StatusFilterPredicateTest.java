package org.joinmastodon.android.utils;

import static org.joinmastodon.android.model.FilterAction.*;
import static org.joinmastodon.android.model.FilterContext.*;
import static org.junit.Assert.*;

import org.joinmastodon.android.model.LegacyFilter;
import org.joinmastodon.android.model.Status;
import org.junit.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

public class StatusFilterPredicateTest {

    private static final LegacyFilter hideMeFilter = new LegacyFilter(), warnMeFilter = new LegacyFilter();
    private static final List<LegacyFilter> allFilters = List.of(hideMeFilter, warnMeFilter);

    private static final Status
            hideInHomePublic = Status.ofFake(null, "hide me, please", Instant.now()),
            warnInHomePublic = Status.ofFake(null, "display me with a warning", Instant.now());

    static {
        hideMeFilter.phrase = "hide me";
        hideMeFilter.filterAction = HIDE;
        hideMeFilter.context = EnumSet.of(PUBLIC, HOME);

        warnMeFilter.phrase = "warning";
        warnMeFilter.filterAction = WARN;
        warnMeFilter.context = EnumSet.of(PUBLIC, HOME);
    }

    @Test
    public void testHide() {
        assertFalse("should not pass because matching filter applies to given context",
                new StatusFilterPredicate(allFilters, HOME).test(hideInHomePublic));
    }

    @Test
    public void testHideRegardlessOfContext() {
        assertTrue("filters without context should always pass",
                new StatusFilterPredicate(allFilters, null).test(hideInHomePublic));
    }

    @Test
    public void testHideInDifferentContext() {
        assertTrue("should pass because matching filter does not apply to given context",
                new StatusFilterPredicate(allFilters, THREAD).test(hideInHomePublic));
    }

    @Test
    public void testHideWithWarningText() {
        assertTrue("should pass because matching filter is for warnings",
                new StatusFilterPredicate(allFilters, HOME).test(warnInHomePublic));
    }

    @Test
    public void testWarn() {
        assertFalse("should not pass because filter applies to given context",
                new StatusFilterPredicate(allFilters, HOME, WARN).test(warnInHomePublic));
    }

    @Test
    public void testWarnRegardlessOfContext() {
        assertTrue("filters without context should always pass",
                new StatusFilterPredicate(allFilters, null, WARN).test(warnInHomePublic));
    }

    @Test
    public void testWarnInDifferentContext() {
        assertTrue("should pass because filter does not apply to given context",
                new StatusFilterPredicate(allFilters, THREAD, WARN).test(warnInHomePublic));
    }

    @Test
    public void testWarnWithHideText() {
        assertTrue("should pass because matching filter is for hiding",
                new StatusFilterPredicate(allFilters, HOME, WARN).test(hideInHomePublic));
    }
}