package org.joinmastodon.android.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
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
import org.joinmastodon.android.model.TimelineDefinition;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class EditTimelinesFragment extends BaseRecyclerFragment<TimelineDefinition> implements ScrollableToTop {
    private TimelinesAdapter adapter;
    private final ItemTouchHelper itemTouchHelper;
    private @ColorInt int backgroundColor;
    private Menu optionsMenu;
    private boolean changed;

    private final Map<MenuItem, TimelineDefinition> timelineByMenuItem = new HashMap<>();
    private final Map<TimelineDefinition, MenuItem> menuItemByTimeline = new HashMap<>();

    public EditTimelinesFragment() {
        super(10);
        ItemTouchHelper.SimpleCallback itemTouchCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAbsoluteAdapterPosition();
                int toPosition = target.getAbsoluteAdapterPosition();
                if (Math.max(fromPosition, toPosition) >= data.size() || Math.min(fromPosition, toPosition) < 0) {
                    return false;
                } else {
                    Collections.swap(data, fromPosition, toPosition);
                    changed = true;
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
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        };
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
        this.optionsMenu = menu;
        inflater.inflate(R.menu.edit_timelines, menu);
        SubMenu timelines = menu.findItem(R.id.add_timeline).getSubMenu();
        timelineByMenuItem.clear();
        menuItemByTimeline.clear();
        TimelineDefinition.ALL_TIMELINES.forEach(tl -> {
            if (data.contains(tl)) return;
            MenuItem item = timelines.add(0, View.generateViewId(), Menu.NONE, tl.getTitle(getContext()));
            item.setIcon(tl.getIconResource());
            timelineByMenuItem.put(item, tl);
            menuItemByTimeline.put(tl, item);
        });
        if (timelines.size() == 0) menu.findItem(R.id.add).getSubMenu().removeItem(R.id.add_timeline);
        UiUtils.enableOptionsMenuIcons(getContext(), menu, R.id.add);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        TimelineDefinition tl = timelineByMenuItem.get(item);
        if (tl == null) return false;
        if (!removeTimelineFromOptions(tl)) return false;
        data.add(tl);
        changed = true;
        adapter.notifyItemInserted(data.indexOf(tl));
        saveTimelines();
        return true;
    }

    private boolean removeTimelineFromOptions(TimelineDefinition tl) {
        MenuItem menuItem = menuItemByTimeline.get(tl);
        assert menuItem != null;
        MenuItem containingMenuItem = switch (tl.getType()) {
            case HOME, LOCAL, FEDERATED, POST_NOTIFICATIONS -> optionsMenu.findItem(R.id.add_timeline);
            case LIST -> optionsMenu.findItem(R.id.add_list);
            case HASHTAG -> optionsMenu.findItem(R.id.add_hashtag);
        };
        Menu containingMenu = containingMenuItem.getSubMenu();
        containingMenu.removeItem(menuItem.getItemId());
        if (containingMenu.size() == 0) {
            optionsMenu.findItem(R.id.add).getSubMenu().removeItem(containingMenuItem.getItemId());
        }
        return true;
    }

    private void saveTimelines() {
        GlobalUserPreferences.pinnedTimelines = data;
        GlobalUserPreferences.save();
    }

    @Override
    protected void doLoadData(int offset, int count){
        onDataLoaded(GlobalUserPreferences.pinnedTimelines, false);
        GlobalUserPreferences.pinnedTimelines.forEach(this::removeTimelineFromOptions);
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
        if (!changed) return;
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
}
