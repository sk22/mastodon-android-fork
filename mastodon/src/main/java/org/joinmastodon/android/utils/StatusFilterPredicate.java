package org.joinmastodon.android.utils;

import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.LegacyFilter;
import org.joinmastodon.android.model.FilterAction;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.Status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StatusFilterPredicate implements Predicate<Status>{
	private final List<LegacyFilter> filters;
	private final FilterContext context;
	private final FilterAction action;
	private LegacyFilter applyingFilter;

	/**
	 * @param context null makes the predicate pass automatically
	 * @param action defines what the predicate should check:
	 * 	             status should not be hidden or should not display with warning
	 */
	public StatusFilterPredicate(List<LegacyFilter> filters, FilterContext context, FilterAction action){
		this.filters = filters;
		this.context = context;
		this.action = action;
	}

	public StatusFilterPredicate(List<LegacyFilter> filters, FilterContext context){
		this(filters, context, FilterAction.HIDE);
	}

	/**
	 * @param context null makes the predicate pass automatically
	 * @param action defines what the predicate should check:
	 *               status should not be hidden or should not display with warning
	 */
	public StatusFilterPredicate(String accountID, FilterContext context, FilterAction action){
		filters=AccountSessionManager.getInstance().getAccount(accountID).wordFilters.stream().filter(f->f.context.contains(context)).collect(Collectors.toList());
		this.context = context;
		this.action = action;
	}

	/**
	 * @param context null makes the predicate pass automatically
	 */
	public StatusFilterPredicate(String accountID, FilterContext context){
		this(accountID, context, FilterAction.HIDE);
	}

	/**
	 * @return whether the status should be displayed without being hidden/warned about.
	 *         will always return true if the context is null.
	 *         true = display this status,
	 *         false = filter this status
	 */
	@Override
	public boolean test(Status status){
		if (context == null) return true;

		Stream<LegacyFilter> matchingFilters = status.filtered != null
				// use server-provided per-status info (status.filtered) if available
				? status.filtered.stream().map(f -> f.filter)
				// or fall back to cached filters
				: filters.stream().filter(filter -> filter.matches(status));

		Optional<LegacyFilter> applyingFilter = matchingFilters
				// discard expired filters
				.filter(filter -> filter.expiresAt == null || filter.expiresAt.isAfter(Instant.now()))
				// only apply filters for given context
				.filter(filter -> filter.context.contains(context))
				// treating filterAction = null (from filters list) as FilterAction.HIDE
				.filter(filter -> filter.filterAction == null ? action == FilterAction.HIDE : filter.filterAction == action)
				.findAny();

		this.applyingFilter = applyingFilter.orElse(null);
		return applyingFilter.isEmpty();
	}

	public LegacyFilter getApplyingFilter() {
		return applyingFilter;
	}
}
