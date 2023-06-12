package org.joinmastodon.android.fragments.account_list;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.PleromaGetStatusReactions;
import org.joinmastodon.android.model.EmojiReaction;

import java.util.List;
import java.util.stream.Collectors;

import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;

public class StatusEmojiReactionsListFragment extends BaseAccountListFragment {
    private String id;
    private String emoji;
    private int count;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        id = getArguments().getString("statusID");
        emoji = getArguments().getString("emoji");
        count = getArguments().getInt("count");

        setTitle(getResources().getString(R.string.sk_x_reacted_with_x, count, emoji));
    }

    @Override
    public void dataLoaded() {
        super.dataLoaded();
        footerProgress.setVisibility(View.GONE);
    }

    @Override
    protected void doLoadData(int offset, int count){
        currentRequest = new PleromaGetStatusReactions(id, emoji)
                .setCallback(new SimpleCallback<>(StatusEmojiReactionsListFragment.this){
                    @Override
                    public void onSuccess(List<EmojiReaction> result) {
                        if (getActivity() == null)
                            return;

                        List<AccountItem> items = result.get(0).accounts.stream()
                                .map(AccountItem::new)
                                .collect(Collectors.toList());

                        onDataLoaded(items);
                    }

                    @Override
                    public void onError(ErrorResponse error) {
                        super.onError(error);
                    }
                })
                .exec(accountID);
    }

    @Override
    public void onResume(){
        super.onResume();
        if(!loaded && !dataLoading)
            loadData();
    }

    @Override
    public Uri getWebUri(Uri.Builder base) {
        return null;
    }
}
