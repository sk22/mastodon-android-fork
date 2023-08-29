package org.joinmastodon.android;

import android.content.Context;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.PaginatedResponse;
import org.unifiedpush.android.connector.MessagingReceiver;

import java.util.List;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class UnifiedPushNotificationReceiver extends MessagingReceiver{
	private static final String TAG="UnifiedPushNotificationReceiver";

	public UnifiedPushNotificationReceiver() {
		super();
	}

	@Override
	public void onNewEndpoint(@NotNull Context context, @NotNull String endpoint, @NotNull String instance) {
		// Called when a new endpoint be used for sending push messages
		Log.d(TAG, "onNewEndpoint: New Endpoint " + endpoint + " for "+ instance);
		AccountSession account = AccountSessionManager.getInstance().tryGetAccount(instance);
		if (account != null)
			account.getPushSubscriptionManager().registerAccountForPush(null, endpoint);
	}

	@Override
	public void onRegistrationFailed(@NotNull Context context, @NotNull String instance) {
		// called when the registration is not possible, eg. no network
		Log.d(TAG, "onRegistrationFailed: " + instance);
		//re-register for gcm
		AccountSession account = AccountSessionManager.getInstance().tryGetAccount(instance);
		if (account != null)
			account.getPushSubscriptionManager().registerAccountForPush(null);
	}

	@Override
	public void onUnregistered(@NotNull Context context, @NotNull String instance) {
		// called when this application is unregistered from receiving push messages
		Log.d(TAG, "onUnregistered: " + instance);
		//re-register for gcm
		AccountSession account = AccountSessionManager.getInstance().tryGetAccount(instance);
		if (account != null)
			account.getPushSubscriptionManager().registerAccountForPush(null);
	}

	@Override
	public void onMessage(@NotNull Context context, @NotNull byte[] message, @NotNull String instance) {
		// Called when a new message is received. The message contains the full POST body of the push message
		AccountSession account = AccountSessionManager.getInstance().tryGetAccount(instance);

		if (account == null)
		    return;

		//this is stupid
		// Mastodon stores the info to decrypt the message in the HTTP headers, which are not accessible in UnifiedPush,
		// thus it is not possible to decrypt them. SO we need to re-request them from the server and transform them later on
		// The official uses fcm and moves the headers to extra data, see
		// https://github.com/mastodon/webpush-fcm-relay/blob/cac95b28d5364b0204f629283141ac3fb749e0c5/webpush-fcm-relay.go#L116
		// https://github.com/tuskyapp/Tusky/pull/2303#issue-1112080540
		account.getCacheController().getNotifications(null, 1, false, false, true, new Callback<>(){
			@Override
			public void onSuccess(PaginatedResponse<List<Notification>> result){
				result.items
						.stream()
						.findFirst()
						.ifPresent(value->MastodonAPIController.runInBackground(()->new PushNotificationReceiver().notifyUnifiedPush(context, instance, value)));
			}

			@Override
			public void onError(ErrorResponse error){
				//professional error handling
			}
		});
	}
}
