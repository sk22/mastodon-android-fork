package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Toolbar;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.utils.ElevationOnScrollListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.annotation.CallSuper;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public abstract class MastodonRecyclerFragment<T> extends BaseRecyclerFragment<T>{
	protected ElevationOnScrollListener elevationOnScrollListener;

	public MastodonRecyclerFragment(int perPage){
		super(perPage);
	}

	public MastodonRecyclerFragment(int layout, int perPage){
		super(layout, perPage);
	}

	protected List<View> getViewsForElevationEffect(){
		Toolbar toolbar=getToolbar();
		return toolbar!=null ? Collections.singletonList(toolbar) : Collections.emptyList();
	}

	@Override
	@CallSuper
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		if (getParentFragment() instanceof HasElevationOnScrollListener elevator)
			list.addOnScrollListener(elevator.getElevationOnScrollListener());
		else if(wantsElevationOnScrollEffect())
			list.addOnScrollListener(elevationOnScrollListener=new ElevationOnScrollListener((FragmentRootLinearLayout) view, getViewsForElevationEffect()));
		if(refreshLayout!=null)
			setRefreshLayoutColors(refreshLayout);
	}

	@Override
	@CallSuper
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		if(elevationOnScrollListener!=null){
			elevationOnScrollListener.setViews(getViewsForElevationEffect());
		}
	}

	protected boolean wantsElevationOnScrollEffect(){
		return true;
	}

	public List<T> getData() {
		return data;
	}

	public static void setRefreshLayoutColors(SwipeRefreshLayout l) {
		List<Integer> colors = new ArrayList<>(Arrays.asList(
				UiUtils.isDarkTheme() ? R.color.primary_200 : R.color.primary_600,
				UiUtils.isDarkTheme() ? R.color.red_primary_200 : R.color.red_primary_600,
				UiUtils.isDarkTheme() ? R.color.green_primary_200 : R.color.green_primary_600,
				UiUtils.isDarkTheme() ? R.color.blue_primary_200 : R.color.blue_primary_600,
				UiUtils.isDarkTheme() ? R.color.purple_200 : R.color.purple_600
		));
		int primary = UiUtils.getThemeColorRes(l.getContext(),
				UiUtils.isDarkTheme() ? R.attr.colorPrimary200 : R.attr.colorPrimary600);
		if (!colors.contains(primary)) colors.add(0, primary);
		int offset = colors.indexOf(primary);
		int[] sorted = new int[colors.size()];
		for (int i = 0; i < colors.size(); i++) {
			sorted[i] = colors.get((i + offset) % colors.size());
		}
		l.setColorSchemeResources(sorted);
		int colorBackground=UiUtils.getThemeColor(l.getContext(), R.attr.colorM3Background);
		int colorPrimary=UiUtils.getThemeColor(l.getContext(), R.attr.colorM3Primary);
		l.setProgressBackgroundColorSchemeColor(UiUtils.alphaBlendColors(colorBackground, colorPrimary, 0.11f));
	}
}
