package org.joinmastodon.android.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.instance.GetDomainBlocks;
import org.joinmastodon.android.fragments.onboarding.GoogleMadeMeAddThisFragment;
import org.joinmastodon.android.model.DomainBlock;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Severity;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.utils.ElevationOnScrollListener;
import org.parceler.Parcels;

import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;
import me.grishka.appkit.views.UsableRecyclerView;

public class InstanceBlockListFragment extends LoaderFragment {
	private UsableRecyclerView list;
	private MergeRecyclerAdapter adapter;
	private View buttonBar;
	private Instance instance;
	private ElevationOnScrollListener onScrollListener;

	private List<DomainBlock> domainBlocks;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		loadData();
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setNavigationBarColor(UiUtils.getThemeColor(activity, R.attr.colorWindowBackground));
		instance=Parcels.unwrap(getArguments().getParcelable("instance"));
		setTitle(R.string.mo_instance_info_moderated_servers);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_onboarding_rules, container, false);

		list=view.findViewById(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));

		adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(new ItemsAdapter());
		list.setAdapter(adapter);
		list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorM3SurfaceVariant, 1, 0, 0, DividerItemDecoration.NOT_FIRST));

		buttonBar=view.findViewById(R.id.button_bar);

		view.findViewById(R.id.btn_back).setOnClickListener(v->Nav.finish(this));

		return view;
	}

	@Override
	protected void doLoadData() {
		currentRequest= new GetDomainBlocks().setCallback(new SimpleCallback<>(this) {
			@Override
			public void onSuccess(List<DomainBlock> result) {
				domainBlocks=result;
				dataLoaded();
			}
		}).execNoAuth(instance.uri);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		setStatusBarColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Background));
		view.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Background));
		list.addOnScrollListener(onScrollListener=new ElevationOnScrollListener((FragmentRootLinearLayout) view, buttonBar, getToolbar()));
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		getToolbar().setBackgroundResource(R.drawable.bg_onboarding_panel);
		getToolbar().setElevation(0);
		if(onScrollListener!=null){
			onScrollListener.setViews(buttonBar, getToolbar());
		}
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=27){
			int inset=insets.getSystemWindowInsetBottom();
			buttonBar.setPadding(0, 0, 0, inset>0 ? Math.max(inset, V.dp(36)) : 0);
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0));
		}else{
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
		}
	}


	@Override
	public void onRefresh(){
		doLoadData();
	}
	private class ItemsAdapter extends RecyclerView.Adapter<ItemViewHolder>{

		@NonNull
		@Override
		public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new ItemViewHolder();
		}

		@Override
		public void onBindViewHolder(@NonNull ItemViewHolder holder, int position){
			holder.bind(domainBlocks.get(position));
		}

		@Override
		public int getItemCount(){
			return domainBlocks.size();
		}
	}

	private class ItemViewHolder extends BindableViewHolder<DomainBlock>{
		private final TextView instanceUri, reason;
		private final ImageView severity;

		public ItemViewHolder(){
			super(getActivity(), R.layout.item_server_block, list);
			instanceUri=findViewById(R.id.instance);
			reason=findViewById(R.id.reason);
			severity=findViewById(R.id.severity);
		}

		@SuppressLint("DefaultLocale")
		@Override
		public void onBind(DomainBlock item){
			instanceUri.setText(item.domain);
			reason.setText(item.comment);
			switch (item.severity) {
				case SILENCE -> {
					severity.setImageDrawable(getContext().getDrawable(R.drawable.ic_fluent_speaker_mute_28_regular));
					severity.setContentDescription(getContext().getString(R.string.mo_severity_silence));
				}
				case SUSPEND -> {
					severity.setImageDrawable(getContext().getDrawable(R.drawable.ic_fluent_shield_prohibited_28_regular));
					severity.setContentDescription(getContext().getString(R.string.mo_severity_suspend));
				}
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				severity.setTooltipText(severity.getContentDescription());
			}
		}
	}
}
