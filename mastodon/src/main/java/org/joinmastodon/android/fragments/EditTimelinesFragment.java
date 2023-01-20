package org.joinmastodon.android.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.lists.GetLists;
import org.joinmastodon.android.api.requests.tags.GetFollowedHashtags;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.ListTimeline;
import org.joinmastodon.android.model.TimelineDefinition;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class EditTimelinesFragment extends BaseRecyclerFragment<TimelineDefinition> implements ScrollableToTop {
    private String accountID;
    private TimelinesAdapter adapter;
    private final ItemTouchHelper itemTouchHelper;
    private @ColorInt int backgroundColor;
    private Menu optionsMenu;
    private boolean updated;
    private final Map<MenuItem, TimelineDefinition> timelineByMenuItem = new HashMap<>();
    private final List<ListTimeline> listTimelines = new ArrayList<>();
    private final List<Hashtag> hashtags = new ArrayList<>();

    public EditTimelinesFragment() {
        super(10);
        ItemTouchHelper.SimpleCallback itemTouchCallback = new ItemTouchHelperCallback() ;
        itemTouchHelper = new ItemTouchHelper(itemTouchCallback);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setTitle(R.string.sk_timelines);
        TypedValue outValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.colorWindowBackground, outValue, true);
        backgroundColor = outValue.data;

        accountID = getArguments().getString("account");

        new GetLists().setCallback(new Callback<>() {
            @Override
            public void onSuccess(List<ListTimeline> result) {
                listTimelines.addAll(result);
                updateOptionsMenu();
            }

            @Override
            public void onError(ErrorResponse error) {
                error.showToast(getContext());
            }
        }).exec(accountID);

        new GetFollowedHashtags().setCallback(new Callback<>() {
            @Override
            public void onSuccess(HeaderPaginationList<Hashtag> result) {
                hashtags.addAll(result);
                updateOptionsMenu();
            }

            @Override
            public void onError(ErrorResponse error) {
                error.showToast(getContext());
            }
        }).exec(accountID);
    }

    @Override
    protected void onShown(){
        super.onShown();
        if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
            loadData();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        itemTouchHelper.attachToRecyclerView(list);
        refreshLayout.setEnabled(false);
        list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorPollVoted, 0.5f, 56, 16));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_timelines, menu);
        this.optionsMenu = menu;
        updateOptionsMenu();
        UiUtils.enableOptionsMenuIcons(getContext(), optionsMenu, R.id.add);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        TimelineDefinition tl = timelineByMenuItem.get(item);
        if (tl == null) return false;
        data.add(tl);
        updated = true;
        adapter.notifyItemInserted(data.indexOf(tl));
        updateOptionsMenu();
        saveTimelines();
        return true;
    }

    private void addTimelineToOptions(TimelineDefinition tl, Menu menu) {
        if (data.contains(tl)) return;
        MenuItem item = menu.add(0, View.generateViewId(), Menu.NONE, tl.getTitle(getContext()));
        item.setIcon(tl.getIconResource());
        timelineByMenuItem.put(item, tl);
        UiUtils.insetPopupMenuIcon(getContext(), item);
    }

    private void updateOptionsMenu() {
        MenuItem timelinesItem = optionsMenu.findItem(R.id.add_timeline);
        Menu timelinesMenu = timelinesItem.getSubMenu();
        MenuItem listsItem = optionsMenu.findItem(R.id.add_list);
        Menu listsMenu = listsItem.getSubMenu();
        MenuItem hashtagsItem = optionsMenu.findItem(R.id.add_hashtag);
        Menu hashtagsMenu = hashtagsItem.getSubMenu();

        timelinesMenu.clear();
        listsMenu.clear();
        hashtagsMenu.clear();
        timelineByMenuItem.clear();

        TimelineDefinition.ALL_TIMELINES.forEach(tl -> addTimelineToOptions(tl, timelinesMenu));
        listTimelines.stream().map(TimelineDefinition::ofList).forEach(tl -> addTimelineToOptions(tl, listsMenu));
        hashtags.stream().map(TimelineDefinition::ofHashtag).forEach(tl -> addTimelineToOptions(tl, hashtagsMenu));

        timelinesItem.setVisible(timelinesItem.getSubMenu().size() > 0);
        listsItem.setVisible(listsItem.getSubMenu().size() > 0);
        hashtagsItem.setVisible(hashtagsItem.getSubMenu().size() > 0);
    }

    private void saveTimelines() {
        GlobalUserPreferences.pinnedTimelines.put(accountID, data);
        GlobalUserPreferences.save();
    }

    @Override
    protected void doLoadData(int offset, int count){
        onDataLoaded(GlobalUserPreferences.pinnedTimelines.getOrDefault(accountID, TimelineDefinition.DEFAULT_TIMELINES), false);
        updateOptionsMenu();
    }

    @Override
    protected RecyclerView.Adapter<TimelineViewHolder> getAdapter() {
        return adapter = new TimelinesAdapter();
    }

    @Override
    public void scrollToTop() {
        smoothScrollRecyclerViewToTop(list);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!updated) return;
        Intent intent = Intent.makeRestartActivityTask(MastodonApp.context.getPackageManager().getLaunchIntentForPackage(MastodonApp.context.getPackageName()).getComponent());
        MastodonApp.context.startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    private class TimelinesAdapter extends RecyclerView.Adapter<TimelineViewHolder>{
        @NonNull
        @Override
        public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            return new TimelineViewHolder();
        }

        @Override
        public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
            holder.bind(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private class TimelineViewHolder extends BindableViewHolder<TimelineDefinition> implements UsableRecyclerView.Clickable{
        private final TextView title;
        private final ImageView dragger;

        public TimelineViewHolder(){
            super(getActivity(), R.layout.item_text, list);
            title=findViewById(R.id.title);
            dragger=findViewById(R.id.dragger_thingy);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBind(TimelineDefinition item) {
            title.setText(item.getTitle(getContext()));
            title.setCompoundDrawablesRelativeWithIntrinsicBounds(itemView.getContext().getDrawable(item.getIconResource()), null, null, null);
            dragger.setVisibility(View.VISIBLE);
            itemView.setOnLongClickListener(l -> {
                itemTouchHelper.startDrag(this);
                return true;
            });
            dragger.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            dragger.setOnTouchListener((View v, MotionEvent event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(this);
                    return true;
                }
                return false;
            });
        }

        @Override
        public void onClick() {}
    }

    private class ItemTouchHelperCallback extends ItemTouchHelper.SimpleCallback {
        public ItemTouchHelperCallback() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAbsoluteAdapterPosition();
            int toPosition = target.getAbsoluteAdapterPosition();
            if (Math.max(fromPosition, toPosition) >= data.size() || Math.min(fromPosition, toPosition) < 0) {
                return false;
            } else {
                Collections.swap(data, fromPosition, toPosition);
                updated = true;
                adapter.notifyItemMoved(fromPosition, toPosition);
                saveTimelines();
                return true;
            }
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                viewHolder.itemView.animate().alpha(0.65f);
                viewHolder.itemView.setBackgroundColor(backgroundColor);
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.animate().alpha(1f);
            viewHolder.itemView.setBackgroundColor(0);
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAbsoluteAdapterPosition();
            updated = true;
            data.remove(position);
            adapter.notifyItemRemoved(position);
            updateOptionsMenu();
        }
    }
}
