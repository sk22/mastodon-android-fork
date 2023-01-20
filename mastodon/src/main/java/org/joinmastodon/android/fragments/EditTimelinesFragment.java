package org.joinmastodon.android.fragments;

import static android.view.Menu.NONE;

import android.annotation.SuppressLint;
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
        this.optionsMenu = menu;
        updateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_back) {
            updateOptionsMenu();
            optionsMenu.performIdentifierAction(R.id.menu_add_timeline, 0);
            return true;
        }
        TimelineDefinition tl = timelineByMenuItem.get(item);
        if (tl != null) {
            data.add(tl);
            adapter.notifyItemInserted(data.indexOf(tl));
            saveTimelines();
            updateOptionsMenu();
        };
        return true;
    }

    private void addTimelineToOptions(TimelineDefinition tl, Menu menu) {
        if (data.contains(tl)) return;
        MenuItem item = menu.add(0, View.generateViewId(), Menu.NONE, tl.getTitle(getContext()));
        item.setIcon(tl.getIconResource());
        timelineByMenuItem.put(item, tl);
    }

    private MenuItem makeBackItem(Menu m) {
        MenuItem back = m.add(0, R.id.menu_back, NONE, R.string.back);
        back.setIcon(R.drawable.ic_arrow_back);
        return back;
    }

    private void updateOptionsMenu() {
        optionsMenu.clear();
        timelineByMenuItem.clear();

        SubMenu menu = optionsMenu.addSubMenu(0, R.id.menu_add_timeline, NONE, R.string.sk_timelines_add);
        menu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.getItem().setIcon(R.drawable.ic_fluent_add_24_regular);

        SubMenu timelinesMenu = menu.addSubMenu(R.string.sk_timeline);
        timelinesMenu.getItem().setIcon(R.drawable.ic_fluent_timeline_24_regular);
        SubMenu listsMenu = menu.addSubMenu(R.string.sk_list);
        listsMenu.getItem().setIcon(R.drawable.ic_fluent_people_list_24_regular);
        SubMenu hashtagsMenu = menu.addSubMenu(R.string.sk_hashtag);
        hashtagsMenu.getItem().setIcon(R.drawable.ic_fluent_number_symbol_24_regular);

        makeBackItem(timelinesMenu);
        makeBackItem(listsMenu);
        makeBackItem(hashtagsMenu);

        TimelineDefinition.ALL_TIMELINES.forEach(tl -> addTimelineToOptions(tl, timelinesMenu));
        listTimelines.stream().map(TimelineDefinition::ofList).forEach(tl -> addTimelineToOptions(tl, listsMenu));
        hashtags.stream().map(TimelineDefinition::ofHashtag).forEach(tl -> addTimelineToOptions(tl, hashtagsMenu));

        timelinesMenu.getItem().setVisible(timelinesMenu.size() > 0);
        listsMenu.getItem().setVisible(listsMenu.size() > 0);
        hashtagsMenu.getItem().setVisible(hashtagsMenu.size() > 0);

        UiUtils.enableOptionsMenuIcons(getContext(), optionsMenu, R.id.menu_add_timeline);
    }

    private void saveTimelines() {
        updated = true;
        GlobalUserPreferences.pinnedTimelines.put(accountID, data.size() > 0 ? data : List.of(TimelineDefinition.HOME_TIMELINE));
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
        if (updated) UiUtils.restartApp();
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
            saveTimelines();
            updateOptionsMenu();
        }
    }
}
