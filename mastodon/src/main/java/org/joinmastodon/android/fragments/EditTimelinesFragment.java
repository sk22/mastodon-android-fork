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
import org.joinmastodon.android.model.TimelineDefinition;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.Collections;
import java.util.HashMap;
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
        changed = true;
        adapter.notifyItemInserted(data.indexOf(tl));
        updateOptionsMenu();
        saveTimelines();
        return true;
    }

    private void updateOptionsMenu() {
        MenuItem timelines = optionsMenu.findItem(R.id.add_timeline);
        MenuItem lists = optionsMenu.findItem(R.id.add_list);
        MenuItem hashtags = optionsMenu.findItem(R.id.add_hashtag);
        timelines.getSubMenu().clear();
        lists.getSubMenu().clear();
        hashtags.getSubMenu().clear();
        timelineByMenuItem.clear();
        TimelineDefinition.ALL_TIMELINES.forEach(tl -> {
            if (data.contains(tl)) return;
            MenuItem item = timelines.getSubMenu().add(0, View.generateViewId(), Menu.NONE, tl.getTitle(getContext()));
            item.setIcon(tl.getIconResource());
            timelineByMenuItem.put(item, tl);
            UiUtils.insetPopupMenuIcon(getContext(), item);
        });
        timelines.setVisible(timelines.getSubMenu().size() > 0);
        lists.setVisible(lists.getSubMenu().size() > 0);
        hashtags.setVisible(lists.getSubMenu().size() > 0);
    }

    private void saveTimelines() {
        GlobalUserPreferences.pinnedTimelines = data;
        GlobalUserPreferences.save();
    }

    @Override
    protected void doLoadData(int offset, int count){
        onDataLoaded(GlobalUserPreferences.pinnedTimelines, false);
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
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAbsoluteAdapterPosition();
            data.remove(position);
            adapter.notifyItemRemoved(position);
            updateOptionsMenu();
        }
    }
}
