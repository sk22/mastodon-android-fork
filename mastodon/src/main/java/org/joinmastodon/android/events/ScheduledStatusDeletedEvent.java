package org.joinmastodon.android.events;

import org.joinmastodon.android.model.ScheduledStatus;

public class ScheduledStatusDeletedEvent{
	public final String id;
	public final String accountID;

	public ScheduledStatusDeletedEvent(String id, String accountID){
		this.id=id;
		this.accountID=accountID;
	}
}
