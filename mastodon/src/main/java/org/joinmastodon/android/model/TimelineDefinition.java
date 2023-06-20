package org.joinmastodon.android.model;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.HashtagTimelineFragment;
import org.joinmastodon.android.fragments.HomeTimelineFragment;
import org.joinmastodon.android.fragments.ListTimelineFragment;
import org.joinmastodon.android.fragments.NotificationsListFragment;
import org.joinmastodon.android.fragments.discover.BubbleTimelineFragment;
import org.joinmastodon.android.fragments.discover.FederatedTimelineFragment;
import org.joinmastodon.android.fragments.discover.LocalTimelineFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TimelineDefinition {
    private TimelineType type;
    private String title;
    private @Nullable Icon icon;

    private @Nullable String listId;
    private @Nullable String listTitle;
    private boolean listIsExclusive;

    private @Nullable String hashtagName;
    private @Nullable List<String> hashtagAny;
    private @Nullable List<String> hashtagAll;
    private @Nullable List<String> hashtagNone;
    private boolean hashtagLocalOnly;

    public static TimelineDefinition ofList(String listId, String listTitle, boolean listIsExclusive) {
        TimelineDefinition def = new TimelineDefinition(TimelineType.LIST);
        def.listId = listId;
        def.listTitle = listTitle;
        def.listIsExclusive = listIsExclusive;
        return def;
    }

    public static TimelineDefinition ofList(ListTimeline list) {
        return ofList(list.id, list.title, list.exclusive);
    }

    public static TimelineDefinition ofHashtag(String hashtag) {
        TimelineDefinition def = new TimelineDefinition(TimelineType.HASHTAG);
        def.hashtagName = hashtag;
        return def;
    }

    public static TimelineDefinition ofHashtag(Hashtag hashtag) {
        return ofHashtag(hashtag.name);
    }

    @SuppressWarnings("unused")
    public TimelineDefinition() {}

    public TimelineDefinition(TimelineType type) {
        this.type = type;
    }

    public boolean isCompatible(AccountSession session) {
        return true;
    }

    public boolean wantsDefault(AccountSession session) {
        return true;
    }

    public String getTitle(Context ctx) {
        return title != null ? title : getDefaultTitle(ctx);
    }

    public String getCustomTitle() {
        return title;
    }

    @Nullable
    public String getHashtagName() {
        return hashtagName;
    }

    @Nullable
    public List<String> getHashtagAny() {
        return hashtagAny;
    }

    @Nullable
    public List<String> getHashtagAll() {
        return hashtagAll;
    }

    @Nullable
    public List<String> getHashtagNone() {
        return hashtagNone;
    }

    public boolean isHashtagLocalOnly() {
        return hashtagLocalOnly;
    }

    public void setTitle(String title) {
        this.title = title == null || title.isBlank() ? null : title;
    }

    private List<String> sanitizeTagList(List<String> tags) {
        return tags.stream()
                .map(String::trim)
                .filter(str -> !TextUtils.isEmpty(str))
                .collect(Collectors.toList());
    }

    public void setTagOptions(String main, List<String> any, List<String> all, List<String> none, boolean localOnly) {
        this.hashtagName = main;
        this.hashtagAny = sanitizeTagList(any);
        this.hashtagAll = sanitizeTagList(all);
        this.hashtagNone = sanitizeTagList(none);
        this.hashtagLocalOnly = localOnly;
    }


    public String getDefaultTitle(Context ctx) {
        return switch (type) {
            case HOME -> ctx.getString(R.string.sk_timeline_home);
            case LOCAL -> ctx.getString(R.string.sk_timeline_local);
            case FEDERATED -> ctx.getString(R.string.sk_timeline_federated);
            case POST_NOTIFICATIONS -> ctx.getString(R.string.sk_timeline_posts);
            case LIST -> listTitle;
            case HASHTAG -> hashtagName;
            case BUBBLE -> ctx.getString(R.string.sk_timeline_bubble);
        };
    }

    public Icon getDefaultIcon() {
        return switch (type) {
            case HOME -> Icon.HOME;
            case LOCAL -> Icon.LOCAL;
            case FEDERATED -> Icon.FEDERATED;
            case POST_NOTIFICATIONS -> Icon.POST_NOTIFICATIONS;
            case LIST -> listIsExclusive ? Icon.EXCLUSIVE_LIST : Icon.LIST;
            case HASHTAG -> Icon.HASHTAG;
            case BUBBLE -> Icon.BUBBLE;
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
            case BUBBLE -> new BubbleTimelineFragment();
        };
    }

    @Nullable
    public Icon getIcon() {
        return icon == null ? getDefaultIcon() : icon;
    }

    public void setIcon(@Nullable Icon icon) {
        this.icon = icon;
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
        if (type == TimelineType.LIST) return Objects.equals(listId, that.listId);
        if (type == TimelineType.HASHTAG) {
            if (hashtagName == null && that.hashtagName == null) return true;
            if (hashtagName == null || that.hashtagName == null) return false;
            return Objects.equals(hashtagName.toLowerCase(), that.hashtagName.toLowerCase());
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, title, listId, hashtagName, hashtagAny, hashtagAll, hashtagNone);
    }

    public TimelineDefinition copy() {
        TimelineDefinition def = new TimelineDefinition(type);
        def.title = title;
        def.listId = listId;
        def.listTitle = listTitle;
        def.listIsExclusive = listIsExclusive;
        def.hashtagName = hashtagName;
        def.hashtagAny = hashtagAny;
        def.hashtagAll = hashtagAll;
        def.hashtagNone = hashtagNone;
        def.icon = icon == null ? null : Icon.values()[icon.ordinal()];
        return def;
    }

    public Bundle populateArguments(Bundle args) {
        if (type == TimelineType.LIST) {
            args.putString("listTitle", title);
            args.putString("listID", listId);
            args.putBoolean("listIsExclusive", listIsExclusive);
        } else if (type == TimelineType.HASHTAG) {
            args.putString("hashtag", hashtagName);
            args.putBoolean("localOnly", hashtagLocalOnly);
            args.putStringArrayList("any", hashtagAny == null ? new ArrayList<>() : new ArrayList<>(hashtagAny));
            args.putStringArrayList("all", hashtagAll == null ? new ArrayList<>() : new ArrayList<>(hashtagAll));
            args.putStringArrayList("none", hashtagNone == null ? new ArrayList<>() : new ArrayList<>(hashtagNone));
        }
        return args;
    }

    public enum TimelineType { HOME, LOCAL, FEDERATED, POST_NOTIFICATIONS, LIST, HASHTAG, BUBBLE }

    public enum Icon {
        HEART(R.drawable.ic_fluent_heart_24_regular, R.string.sk_icon_heart),
        STAR(R.drawable.ic_fluent_star_24_regular, R.string.sk_icon_star),
        PEOPLE(R.drawable.ic_fluent_people_24_regular, R.string.sk_icon_people),
        CITY(R.drawable.ic_fluent_city_24_regular, R.string.sk_icon_city),
        IMAGE(R.drawable.ic_fluent_image_24_regular, R.string.sk_icon_image),
        NEWS(R.drawable.ic_fluent_news_24_regular, R.string.sk_icon_news),
        FEED(R.drawable.ic_fluent_rss_24_regular, R.string.sk_icon_feed),
        COLOR_PALETTE(R.drawable.ic_fluent_color_24_regular, R.string.sk_icon_color_palette),
        CAT(R.drawable.ic_fluent_animal_cat_24_regular, R.string.sk_icon_cat),
        DOG(R.drawable.ic_fluent_animal_dog_24_regular, R.string.sk_icon_dog),
        RABBIT(R.drawable.ic_fluent_animal_rabbit_24_regular, R.string.sk_icon_rabbit),
        TURTLE(R.drawable.ic_fluent_animal_turtle_24_regular, R.string.sk_icon_turtle),
        ACADEMIC_CAP(R.drawable.ic_fluent_hat_graduation_24_regular, R.string.sk_icon_academic_cap),
        BOT(R.drawable.ic_fluent_bot_24_regular, R.string.sk_icon_bot),
        IMPORTANT(R.drawable.ic_fluent_important_24_regular, R.string.sk_icon_important),
        PIN(R.drawable.ic_fluent_pin_24_regular, R.string.sk_icon_pin),
        SHIELD(R.drawable.ic_fluent_shield_24_regular, R.string.sk_icon_shield),
        CHAT(R.drawable.ic_fluent_chat_multiple_24_regular, R.string.sk_icon_chat),
        TAG(R.drawable.ic_fluent_tag_24_regular, R.string.sk_icon_tag),
        TRAIN(R.drawable.ic_fluent_vehicle_subway_24_regular, R.string.sk_icon_train),
        BICYCLE(R.drawable.ic_fluent_vehicle_bicycle_24_regular, R.string.sk_icon_bicycle),
        MAP(R.drawable.ic_fluent_map_24_regular, R.string.sk_icon_map),
        BACKPACK(R.drawable.ic_fluent_backpack_24_regular, R.string.sk_icon_backpack),
        BRIEFCASE(R.drawable.ic_fluent_briefcase_24_regular, R.string.sk_icon_briefcase),
        BOOK(R.drawable.ic_fluent_book_open_24_regular, R.string.sk_icon_book),
        LANGUAGE(R.drawable.ic_fluent_local_language_24_regular, R.string.sk_icon_language),
        WEATHER(R.drawable.ic_fluent_weather_rain_showers_day_24_regular, R.string.sk_icon_weather),
        APERTURE(R.drawable.ic_fluent_scan_24_regular, R.string.sk_icon_aperture),
        MUSIC(R.drawable.ic_fluent_music_note_2_24_regular, R.string.sk_icon_music),
        LOCATION(R.drawable.ic_fluent_location_24_regular, R.string.sk_icon_location),
        GLOBE(R.drawable.ic_fluent_globe_24_regular, R.string.sk_icon_globe),
        MEGAPHONE(R.drawable.ic_fluent_megaphone_loud_24_regular, R.string.sk_icon_megaphone),
        MICROPHONE(R.drawable.ic_fluent_mic_24_regular, R.string.sk_icon_microphone),
        MICROSCOPE(R.drawable.ic_fluent_microscope_24_regular, R.string.sk_icon_microscope),
        STETHOSCOPE(R.drawable.ic_fluent_stethoscope_24_regular, R.string.sk_icon_stethoscope),
        KEYBOARD(R.drawable.ic_fluent_midi_24_regular, R.string.sk_icon_keyboard),
        COFFEE(R.drawable.ic_fluent_drink_coffee_24_regular, R.string.sk_icon_coffee),
        CLAPPER_BOARD(R.drawable.ic_fluent_movies_and_tv_24_regular, R.string.sk_icon_clapper_board),
        LAUGH(R.drawable.ic_fluent_emoji_laugh_24_regular, R.string.sk_icon_laugh),
        BALLOON(R.drawable.ic_fluent_balloon_24_regular, R.string.sk_icon_balloon),
        PI(R.drawable.ic_fluent_pi_24_regular, R.string.sk_icon_pi),
        MATH_FORMULA(R.drawable.ic_fluent_math_formula_24_regular, R.string.sk_icon_math_formula),
        GAMES(R.drawable.ic_fluent_games_24_regular, R.string.sk_icon_games),
        CODE(R.drawable.ic_fluent_code_24_regular, R.string.sk_icon_code),
        BUG(R.drawable.ic_fluent_bug_24_regular, R.string.sk_icon_bug),
        LIGHT_BULB(R.drawable.ic_fluent_lightbulb_24_regular, R.string.sk_icon_light_bulb),
        FIRE(R.drawable.ic_fluent_fire_24_regular, R.string.sk_icon_fire),
        LEAVES(R.drawable.ic_fluent_leaf_three_24_regular, R.string.sk_icon_leaves),
        SPORT(R.drawable.ic_fluent_sport_24_regular, R.string.sk_icon_sport),
        HEALTH(R.drawable.ic_fluent_heart_pulse_24_regular, R.string.sk_icon_health),
        PIZZA(R.drawable.ic_fluent_food_pizza_24_regular, R.string.sk_icon_pizza),
        GAVEL(R.drawable.ic_fluent_gavel_24_regular, R.string.sk_icon_gavel),
        GAUGE(R.drawable.ic_fluent_gauge_24_regular, R.string.sk_icon_gauge),
        HEADPHONES(R.drawable.ic_fluent_headphones_sound_wave_24_regular, R.string.sk_icon_headphones),
        HUMAN(R.drawable.ic_fluent_accessibility_24_regular, R.string.sk_icon_human),

        HOME(R.drawable.ic_fluent_home_24_regular, R.string.sk_timeline_home, true),
        LOCAL(R.drawable.ic_fluent_people_community_24_regular, R.string.sk_timeline_local, true),
        FEDERATED(R.drawable.ic_fluent_earth_24_regular, R.string.sk_timeline_federated, true),
        POST_NOTIFICATIONS(R.drawable.ic_fluent_chat_24_regular, R.string.sk_timeline_posts, true),
        LIST(R.drawable.ic_fluent_people_24_regular, R.string.sk_list, true),
        EXCLUSIVE_LIST(R.drawable.ic_fluent_rss_24_regular, R.string.sk_exclusive_list, true),
        HASHTAG(R.drawable.ic_fluent_number_symbol_24_regular, R.string.sk_hashtag, true),
        BUBBLE(R.drawable.ic_fluent_circle_24_regular, R.string.sk_timeline_bubble, true);

        public final int iconRes, nameRes;
        public final boolean hidden;

        Icon(@DrawableRes int iconRes, @StringRes int nameRes) {
            this(iconRes, nameRes, false);
        }

        Icon(@DrawableRes int iconRes, @StringRes int nameRes, boolean hidden) {
            this.iconRes = iconRes;
            this.nameRes = nameRes;
            this.hidden = hidden;
        }
    }

    public static final TimelineDefinition HOME_TIMELINE = new TimelineDefinition(TimelineType.HOME);
    public static final TimelineDefinition LOCAL_TIMELINE = new TimelineDefinition(TimelineType.LOCAL);
    public static final TimelineDefinition FEDERATED_TIMELINE = new TimelineDefinition(TimelineType.FEDERATED);
    public static final TimelineDefinition POSTS_TIMELINE = new TimelineDefinition(TimelineType.POST_NOTIFICATIONS);
    public static final TimelineDefinition BUBBLE_TIMELINE = new TimelineDefinition(TimelineType.BUBBLE) {
        @Override
        public boolean isCompatible(AccountSession session) {
            // still enabling the bubble timeline for all pleroma/akkoma instances since i know of
            // at least one instance that supports it, but doesn't list "bubble_timeline"
            return session.getInstance().map(Instance::isAkkoma).orElse(false);
        }

        @Override
        public boolean wantsDefault(AccountSession session) {
            return session.getInstance()
                    .map(i -> i.hasFeature(Instance.Feature.BUBBLE_TIMELINE))
                    .orElse(false);
        }
    };

    public static List<TimelineDefinition> getDefaultTimelines(String accountId) {
        AccountSession session = AccountSessionManager.getInstance().getAccount(accountId);
        return DEFAULT_TIMELINES.stream()
                .filter(tl -> tl.isCompatible(session) && tl.wantsDefault(session))
                .map(TimelineDefinition::copy)
                .collect(Collectors.toList());
    }

    public static List<TimelineDefinition> getAllTimelines(String accountId) {
        AccountSession session = AccountSessionManager.getInstance().getAccount(accountId);
        return ALL_TIMELINES.stream()
                .filter(tl -> tl.isCompatible(session))
                .map(TimelineDefinition::copy)
                .collect(Collectors.toList());
    }

    private static final List<TimelineDefinition> DEFAULT_TIMELINES = List.of(
            HOME_TIMELINE,
            LOCAL_TIMELINE,
            BUBBLE_TIMELINE,
            FEDERATED_TIMELINE
    );

    private static final List<TimelineDefinition> ALL_TIMELINES = List.of(
            HOME_TIMELINE,
            LOCAL_TIMELINE,
            FEDERATED_TIMELINE,
            POSTS_TIMELINE,
            BUBBLE_TIMELINE
    );
}
