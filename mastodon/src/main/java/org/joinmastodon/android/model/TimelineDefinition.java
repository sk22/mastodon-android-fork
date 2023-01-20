package org.joinmastodon.android.model;

import android.app.Fragment;
import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.HashtagTimelineFragment;
import org.joinmastodon.android.fragments.HomeTimelineFragment;
import org.joinmastodon.android.fragments.ListTimelineFragment;
import org.joinmastodon.android.fragments.NotificationsListFragment;
import org.joinmastodon.android.fragments.discover.FederatedTimelineFragment;
import org.joinmastodon.android.fragments.discover.LocalTimelineFragment;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class TimelineDefinition {
    private TimelineType type;
    private String title;
    private @Nullable Icon icon;

    private @Nullable String listId;
    private @Nullable ListTimeline.RepliesPolicy listRepliesPolicy;

    public static TimelineDefinition forList(String id, String title, ListTimeline.RepliesPolicy repliesPolicy) {
        TimelineDefinition def = new TimelineDefinition(TimelineType.LIST, title);
        def.listId = id;
        def.listRepliesPolicy = repliesPolicy;
        return def;
    }

    public static TimelineDefinition forHashtag(String name) {
        return new TimelineDefinition(TimelineType.LIST, name);
    }

    public TimelineDefinition() {}

    public TimelineDefinition(TimelineType type) {
        this.type = type;
    }

    public TimelineDefinition(TimelineType type, String title) {
        this.type = type;
        this.title = title;
    }

    public String getTitle(Context ctx) {
        if (title != null) return title;
        return switch (type) {
            case HOME -> ctx.getString(R.string.sk_timeline_home);
            case LOCAL -> ctx.getString(R.string.sk_timeline_local);
            case FEDERATED -> ctx.getString(R.string.sk_timeline_federated);
            case POST_NOTIFICATIONS -> ctx.getString(R.string.sk_timeline_posts);
            default -> null;
        };
    }

    public Fragment getFragment() {
        return switch (type) {
            case HOME -> new HomeTimelineFragment();
            case LOCAL -> new LocalTimelineFragment();
            case FEDERATED -> new FederatedTimelineFragment();
            case LIST -> new ListTimelineFragment();
            case HASHTAG -> new HashtagTimelineFragment();
            case POST_NOTIFICATIONS -> new NotificationsListFragment();
        };
    }

    public @DrawableRes int getIconResource() {
        return icon == null ? switch (type) {
            case HOME -> R.drawable.ic_fluent_home_24_regular;
            case LOCAL -> R.drawable.ic_fluent_people_community_24_regular;
            case FEDERATED -> R.drawable.ic_fluent_earth_24_regular;
            case POST_NOTIFICATIONS -> R.drawable.ic_fluent_alert_24_regular;
            case LIST -> R.drawable.ic_fluent_people_list_24_regular;
            case HASHTAG -> R.drawable.ic_fluent_number_symbol_24_regular;
        } : switch (icon) {
            case HEART -> R.drawable.ic_fluent_heart_24_regular;
            case STAR -> R.drawable.ic_fluent_star_24_regular;
        };
    }

    public TimelineType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimelineDefinition that = (TimelineDefinition) o;
        if (type != that.type) return false;
        if (type == TimelineType.HASHTAG) return Objects.equals(title, that.title);
        if (type == TimelineType.LIST) return Objects.equals(listId, that.listId);
        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.ordinal() : 0;
        result = 31 * result + (listId != null ? listId.hashCode() : 0);
        return result;
    }

    public enum TimelineType { HOME, LOCAL, FEDERATED, POST_NOTIFICATIONS, LIST, HASHTAG }

    public enum Icon { HEART, STAR }

    public static final TimelineDefinition HOME_TIMELINE = new TimelineDefinition(TimelineType.HOME);
    public static final TimelineDefinition LOCAL_TIMELINE = new TimelineDefinition(TimelineType.LOCAL);
    public static final TimelineDefinition FEDERATED_TIMELINE = new TimelineDefinition(TimelineType.FEDERATED);
    public static final TimelineDefinition POSTS_TIMELINE = new TimelineDefinition(TimelineType.POST_NOTIFICATIONS);

    public static final List<TimelineDefinition> DEFAULT_TIMELINES = List.of(LOCAL_TIMELINE);
    public static final List<TimelineDefinition> ALL_TIMELINES = List.of(LOCAL_TIMELINE, FEDERATED_TIMELINE, POSTS_TIMELINE);
}
