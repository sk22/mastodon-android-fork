package org.joinmastodon.android.fragments;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.utils.UiUtils;

import me.grishka.appkit.Nav;

public abstract class FabStatusListFragment extends StatusListFragment {
    protected ImageButton fab;

    public FabStatusListFragment() {
        setListLayoutId(R.layout.recycler_fragment_with_fab);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(this::onFabClick);
        fab.setOnLongClickListener(this::onFabLongClick);
    }

    protected void onFabClick(View v){
        Bundle args=new Bundle();
        args.putString("account", accountID);
        Nav.go(getActivity(), ComposeFragment.class, args);
    }

    protected boolean onFabLongClick(View v) {
        return UiUtils.pickAccountForCompose(getActivity(), accountID);
    }
}
