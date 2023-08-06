package org.joinmastodon.android.model;

import org.parceler.Parcel;

import java.util.List;

@Parcel
public class EmojiReaction {
    public List<Account> accounts;
    public List<String> accountIds;
    public int count;
    public boolean me;
    public String name;
    public String url;
	public String staticUrl;
}
