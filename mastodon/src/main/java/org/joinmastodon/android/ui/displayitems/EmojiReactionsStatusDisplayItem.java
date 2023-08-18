package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.graphics.Paint;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.requests.statuses.AddStatusReaction;
import org.joinmastodon.android.api.requests.statuses.DeleteStatusReaction;
import org.joinmastodon.android.api.requests.statuses.PleromaAddStatusReaction;
import org.joinmastodon.android.api.requests.statuses.PleromaDeleteStatusReaction;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.account_list.StatusEmojiReactionsListFragment;
import org.joinmastodon.android.model.EmojiReaction;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.utils.TextDrawable;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.RecyclerViewDelegate;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class EmojiReactionsStatusDisplayItem extends StatusDisplayItem {
	public final Status status;
	private final Drawable placeholder;
	private List<ImageLoaderRequest> requests;

    public EmojiReactionsStatusDisplayItem(String parentID, BaseStatusListFragment<?> parentFragment, Status status) {
		super(parentID, parentFragment);
		this.status=status;
		placeholder=parentFragment.getContext().getDrawable(R.drawable.image_placeholder).mutate();
		placeholder.setBounds(0, 0, V.sp(24), V.sp(24));
    }

	private void refresh(Holder holder) {
		requests=status.reactions.stream()
				.map(e->e.url!=null ? new UrlImageLoaderRequest(e.url, V.sp(24), V.sp(24)) : null)
				.collect(Collectors.toList());
		holder.list.setPadding(holder.list.getPaddingLeft(),
				status.reactions.isEmpty() ? 0 : V.dp(8), holder.list.getPaddingRight(), 0);
	}

	@Override
	public int getImageCount(){
		return (int) status.reactions.stream().filter(r->r.url != null).count();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return requests.get(index);
	}

    @Override
    public Type getType(){
        return Type.EMOJI_REACTIONS;
    }

    public static class Holder extends StatusDisplayItem.Holder<EmojiReactionsStatusDisplayItem> implements ImageLoaderViewHolder {
        private final UsableRecyclerView list;

		public Holder(Activity activity, ViewGroup parent) {
			super(new UsableRecyclerView(activity) {
				@Override
				public boolean onTouchEvent(MotionEvent e){
					super.onTouchEvent(e);
					// to pass through touch events (i.e. clicking the status) to the parent view
					return false;
				}
			});
			list=(UsableRecyclerView) itemView;
			list.setPadding(V.dp(12), 0, V.dp(12), 0);
			list.setClipToPadding(false);
        }

        @Override
        public void onBind(EmojiReactionsStatusDisplayItem item) {
			ListImageLoaderWrapper imgLoader=new ListImageLoaderWrapper(item.parentFragment.getContext(), list, new RecyclerViewDelegate(list), null);
			list.setAdapter(new EmojiReactionsAdapter(this, imgLoader));
			list.setLayoutManager(new LinearLayoutManager(item.parentFragment.getContext(), LinearLayoutManager.HORIZONTAL, false));
			item.refresh(this);
        }

		@Override
		public void setImage(int index, Drawable image){
			View child=list.getChildAt(index);
			if(child==null) return;
			((EmojiReactionViewHolder) list.getChildViewHolder(child)).setImage(index, image);
		}

		@Override
		public void clearImage(int index){
			setImage(index, item.placeholder);
		}

		private class EmojiReactionsAdapter extends UsableRecyclerView.Adapter<EmojiReactionViewHolder> implements ImageLoaderRecyclerAdapter{
			RecyclerView list;
			ListImageLoaderWrapper imgLoader;
			Holder parentHolder;

			public EmojiReactionsAdapter(Holder parentHolder, ListImageLoaderWrapper imgLoader){
				super(imgLoader);
				this.parentHolder=parentHolder;
				this.imgLoader=imgLoader;
			}

			@Override
			public void onAttachedToRecyclerView(@NonNull RecyclerView list){
				super.onAttachedToRecyclerView(list);
				this.list=list;
			}

			@NonNull
			@Override
			public EmojiReactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
				Button btn=new Button(parent.getContext(), null, 0, R.style.Widget_Mastodon_M3_Button_Outlined_Icon);
				ViewGroup.MarginLayoutParams params=new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.setMarginEnd(V.dp(8));
				btn.setLayoutParams(params);
				btn.setCompoundDrawableTintList(null);
				btn.setBackgroundResource(R.drawable.bg_button_m3_tonal);
				btn.setCompoundDrawables(item.placeholder, null, null, null);
				return new EmojiReactionViewHolder(btn, item);
			}

			@Override
			public void onBindViewHolder(EmojiReactionViewHolder holder, int position){
				holder.bind(item.status.reactions.get(position));
				super.onBindViewHolder(holder, position);
			}

			@Override
			public int getItemCount(){
				return item.status.reactions.size();
			}

			@Override
			public int getImageCountForItem(int position){
				return item.status.reactions.get(position).url == null ? 0 : 1;
			}

			@Override
			public ImageLoaderRequest getImageRequest(int position, int image){
				return item.requests.get(position);
			}
		}

		private static class EmojiReactionViewHolder extends BindableViewHolder<EmojiReaction> implements ImageLoaderViewHolder{
			private final Button btn;
			private final EmojiReactionsStatusDisplayItem parent;

			public EmojiReactionViewHolder(@NonNull View itemView, EmojiReactionsStatusDisplayItem parent){
				super(itemView);
				btn=(Button) itemView;
				this.parent=parent;
			}

			@Override
			public void setImage(int index, Drawable drawable){
				drawable.setBounds(0, 0, V.sp(24), V.sp(24));
				btn.setCompoundDrawablesRelative(drawable, null, null, null);
				if(drawable instanceof Animatable) ((Animatable) drawable).start();
			}

			@Override
			public void clearImage(int index){
				setImage(index, parent.placeholder);
			}

			@Override
			public void onBind(EmojiReaction item){
				btn.setText(UiUtils.abbreviateNumber(item.count));
				btn.setContentDescription(item.name);
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)btn.setTooltipText(item.name);
				if(item.url==null){
					Paint p=new Paint();
					p.setTextSize(V.sp(18));
					TextDrawable drawable=new TextDrawable(p, item.name);
					btn.setCompoundDrawablesRelative(drawable, null, null, null);
				}else{
					btn.setCompoundDrawablesRelative(parent.placeholder, null, null, null);
				}
				btn.setSelected(item.me);
				btn.setOnClickListener(e -> {
					boolean deleting=item.me;
					boolean ak=parent.parentFragment.isInstanceAkkoma();
					MastodonAPIRequest<Status> req = deleting
							? (ak ? new PleromaDeleteStatusReaction(parent.status.id, item.name) : new DeleteStatusReaction(parent.status.id, item.name))
							: (ak ? new PleromaAddStatusReaction(parent.status.id, item.name) : new AddStatusReaction(parent.status.id, item.name));
					req.setCallback(new Callback<>() {
								@Override
								public void onSuccess(Status result) {
									List<EmojiReaction> oldList=new ArrayList<>(parent.status.reactions);
									parent.status.reactions.clear();
									parent.status.reactions.addAll(result.reactions);
									EmojiReactionsAdapter adapter = (EmojiReactionsAdapter) getBindingAdapter();

									// this handles addition/removal of new reactions
									UiUtils.updateList(oldList, result.reactions, adapter.list, adapter,
											(e1, e2) -> e1.name.equals(e2.name));

									// update the existing reactions' counts
									for(int i=0; i<result.reactions.size(); i++){
										int index=i;
										EmojiReaction newReaction=result.reactions.get(index);
										oldList.stream().filter(r->r.name.equals(newReaction.name)).findAny().ifPresent(r->{
											if(newReaction.count!=r.count) adapter.notifyItemChanged(index);
										});
									}
									parent.refresh(adapter.parentHolder);
									adapter.imgLoader.updateImages();
									E.post(new StatusCountersUpdatedEvent(result, adapter.parentHolder));
								}

								@Override
								public void onError(ErrorResponse error) {
									error.showToast(itemView.getContext());
								}
							})
							.exec(parent.parentFragment.getAccountID());
				});

				if (parent.parentFragment.isInstanceAkkoma()) {
					// glitch-soc doesn't have this, afaik
					btn.setOnLongClickListener(e->{
						EmojiReaction emojiReaction=parent.status.reactions.stream().filter(r->r.name.equals(item.name)).findAny().orElseThrow();
						Bundle args=new Bundle();
						args.putString("account", parent.parentFragment.getAccountID());
						args.putString("statusID", parent.status.id);
						int atSymbolIndex = emojiReaction.name.indexOf("@");
						args.putString("emoji", atSymbolIndex != -1 ? emojiReaction.name.substring(0, atSymbolIndex) : emojiReaction.name);
						args.putString("url", emojiReaction.url);
						args.putInt("count", emojiReaction.count);
						Nav.go(parent.parentFragment.getActivity(), StatusEmojiReactionsListFragment.class, args);
						return true;
					});
				}
			}
		}
	}
}
