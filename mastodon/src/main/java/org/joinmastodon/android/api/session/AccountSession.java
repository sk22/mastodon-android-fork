package org.joinmastodon.android.api.session;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.joinmastodon.android.E;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.CacheController;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.PushSubscriptionManager;
import org.joinmastodon.android.api.StatusInteractionController;
import org.joinmastodon.android.api.requests.accounts.GetPreferences;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentialsPreferences;
import org.joinmastodon.android.api.requests.markers.GetMarkers;
import org.joinmastodon.android.api.requests.markers.SaveMarkers;
import org.joinmastodon.android.api.requests.oauth.RevokeOauthToken;
import org.joinmastodon.android.events.NotificationsMarkerUpdatedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Application;
import org.joinmastodon.android.model.FilterAction;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.FilterResult;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.LegacyFilter;
import org.joinmastodon.android.model.Preferences;
import org.joinmastodon.android.model.PushSubscription;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.TimelineMarkers;
import org.joinmastodon.android.model.Token;
import org.joinmastodon.android.utils.ObjectIdComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class AccountSession{
	private static final String TAG="AccountSession";

	public Token token;
	public Account self;
	public String domain;
	public Application app;
	public long infoLastUpdated;
	public boolean activated=true;
	public String pushPrivateKey;
	public String pushPublicKey;
	public String pushAuthKey;
	public PushSubscription pushSubscription;
	public boolean needUpdatePushSettings;
	public long filtersLastUpdated;
	public List<LegacyFilter> wordFilters=new ArrayList<>();
	public String pushAccountID;
	public AccountActivationInfo activationInfo;
	public Preferences preferences;
	private transient MastodonAPIController apiController;
	private transient StatusInteractionController statusInteractionController, remoteStatusInteractionController;
	private transient CacheController cacheController;
	private transient PushSubscriptionManager pushSubscriptionManager;
	private transient SharedPreferences prefs;
	private transient boolean preferencesNeedSaving;
	private transient AccountLocalPreferences localPreferences;

	AccountSession(Token token, Account self, Application app, String domain, boolean activated, AccountActivationInfo activationInfo){
		this.token=token;
		this.self=self;
		this.domain=domain;
		this.app=app;
		this.activated=activated;
		this.activationInfo=activationInfo;
		infoLastUpdated=System.currentTimeMillis();
	}

	AccountSession(){}

	public String getID(){
		return domain+"_"+self.id;
	}

	public MastodonAPIController getApiController(){
		if(apiController==null)
			apiController=new MastodonAPIController(this);
		return apiController;
	}

	public StatusInteractionController getStatusInteractionController(){
		if(statusInteractionController==null)
			statusInteractionController=new StatusInteractionController(getID());
		return statusInteractionController;
	}

	public StatusInteractionController getRemoteStatusInteractionController(){
		if(remoteStatusInteractionController==null)
			remoteStatusInteractionController=new StatusInteractionController(getID(), false);
		return remoteStatusInteractionController;
	}

	public CacheController getCacheController(){
		if(cacheController==null)
			cacheController=new CacheController(getID());
		return cacheController;
	}

	public PushSubscriptionManager getPushSubscriptionManager(){
		if(pushSubscriptionManager==null)
			pushSubscriptionManager=new PushSubscriptionManager(getID());
		return pushSubscriptionManager;
	}

	public String getFullUsername(){
		return '@'+self.username+'@'+domain;
	}

	public void preferencesFromAccountSource(Account account) {
		if (account != null && account.source != null && preferences != null) {
			if (account.source.privacy != null)
				preferences.postingDefaultVisibility = account.source.privacy;
			if (account.source.language != null)
				preferences.postingDefaultLanguage = account.source.language;
		}
	}

	public void reloadPreferences(Consumer<Preferences> callback){
		new GetPreferences()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Preferences result){
						preferences=result;
						preferencesFromAccountSource(self);
						if(callback!=null)
							callback.accept(result);
						AccountSessionManager.getInstance().writeAccountsFile();
					}

					@Override
					public void onError(ErrorResponse error){
						Log.w(TAG, "Failed to load preferences for account "+getID()+": "+error);
						if (preferences==null)
							preferences=new Preferences();
						preferencesFromAccountSource(self);
					}
				})
				.exec(getID());
	}

	public SharedPreferences getRawLocalPreferences(){
		if(prefs==null)
			prefs=MastodonApp.context.getSharedPreferences(getID(), Context.MODE_PRIVATE);
		return prefs;
	}

	public void reloadNotificationsMarker(Consumer<String> callback){
		new GetMarkers()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(TimelineMarkers result){
						if(result.notifications!=null && !TextUtils.isEmpty(result.notifications.lastReadId)){
							String id=result.notifications.lastReadId;
							String lastKnown=getLastKnownNotificationsMarker();
							if(ObjectIdComparator.INSTANCE.compare(id, lastKnown)<0){
								// Marker moved back -- previous marker update must have failed.
								// Pretend it didn't happen and repeat the request.
								id=lastKnown;
								new SaveMarkers(null, id).exec(getID());
							}
							callback.accept(id);
							setNotificationsMarker(id, false);
						}
					}

					@Override
					public void onError(ErrorResponse error){}
				})
				.exec(getID());
	}

	public String getLastKnownNotificationsMarker(){
		return getRawLocalPreferences().getString("notificationsMarker", null);
	}

	public void setNotificationsMarker(String id, boolean clearUnread){
		getRawLocalPreferences().edit().putString("notificationsMarker", id).apply();
		E.post(new NotificationsMarkerUpdatedEvent(getID(), id, clearUnread));
	}

	public void logOut(Activity activity, Runnable onDone){
		new RevokeOauthToken(app.clientId, app.clientSecret, token.accessToken)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Object result){
						AccountSessionManager.getInstance().removeAccount(getID());
						onDone.run();
					}

					@Override
					public void onError(ErrorResponse error){
						AccountSessionManager.getInstance().removeAccount(getID());
						onDone.run();
					}
				})
				.wrapProgress(activity, R.string.loading, false)
				.exec(getID());
	}

	public void savePreferencesLater(){
		preferencesNeedSaving=true;
	}

	public void savePreferencesIfPending(){
		if(preferencesNeedSaving){
			new UpdateAccountCredentialsPreferences(preferences, self.locked, self.discoverable, self.source.indexable)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Account result){
							preferencesNeedSaving=false;
							self=result;
							AccountSessionManager.getInstance().writeAccountsFile();
						}

						@Override
						public void onError(ErrorResponse error){
							Log.e(TAG, "failed to save preferences: "+error);
						}
					})
					.exec(getID());
		}
	}

	public AccountLocalPreferences getLocalPreferences(){
		if(localPreferences==null)
			localPreferences=new AccountLocalPreferences(getRawLocalPreferences(), this);
		return localPreferences;
	}

	public void filterStatuses(List<Status> statuses, FilterContext context){
		filterStatuses(statuses, context, null);
	}

	public void filterStatuses(List<Status> statuses, FilterContext context, Account profile){
		filterStatusContainingObjects(statuses, Function.identity(), context, profile);
	}

	public <T> void filterStatusContainingObjects(List<T> objects, Function<T, Status> extractor, FilterContext context){
		filterStatusContainingObjects(objects, extractor, context, null);
	}

	private boolean statusIsOnOwnProfile(Status s, Account profile){
		return self != null && profile != null && s.account != null
				&& Objects.equals(self.id, profile.id) && Objects.equals(self.id, s.account.id);
	}

	private boolean isFilteredType(Status s){
		AccountLocalPreferences localPreferences = getLocalPreferences();
		return (!localPreferences.showReplies && s.inReplyToId != null)
				|| (!localPreferences.showBoosts && s.reblog != null);
	}

	public <T> void filterStatusContainingObjects(List<T> objects, Function<T, Status> extractor, FilterContext context, Account profile){
		AccountLocalPreferences localPreferences = getLocalPreferences();
		if(!localPreferences.serverSideFiltersSupported) for(T obj:objects){
			Status s=extractor.apply(obj);
			if(s!=null && s.filtered!=null){
				localPreferences.serverSideFiltersSupported=true;
				localPreferences.save();
				break;
			}
		}

		List<T> removeUs=new ArrayList<>();
		for(int i=0; i<objects.size(); i++){
			T o=objects.get(i);
			if(filterStatusContainingObject(o, extractor, context, profile)){
				Status s=extractor.apply(o);
				removeUs.add(o);
				if(s!=null && s.hasGapAfter!=null && i>0){
					// oops, we're about to remove an item that has a gap after...
					// gotta find the previous status that's not also about to be removed
					for(int j=i-1; j>=0; j--){
						T p=objects.get(j);
						Status prev=extractor.apply(objects.get(j));
						if(prev!=null && !removeUs.contains(p)){
							prev.hasGapAfter=s.hasGapAfter;
							break;
						}
					}
				}
			}
		}
		objects.removeAll(removeUs);
	}

	public <T> boolean filterStatusContainingObject(T object, Function<T, Status> extractor, FilterContext context, Account profile){
		Status s=extractor.apply(object);
		if(s==null)
			return false;
		// don't hide own posts in own profile
		if(statusIsOnOwnProfile(s, profile))
			return false;
		if(isFilteredType(s) && (context == FilterContext.HOME || context == FilterContext.PUBLIC))
			return true;
		// Even with server-side filters, clients are expected to remove statuses that match a filter that hides them
		if(getLocalPreferences().serverSideFiltersSupported){
			for(FilterResult filter : s.filtered){
				if(filter.filter.isActive() && filter.filter.filterAction==FilterAction.HIDE)
					return true;
			}
		}else if(wordFilters!=null){
			for(LegacyFilter filter : wordFilters){
				if(filter.context.contains(context) && filter.matches(s) && filter.isActive())
					return true;
			}
		}
		return false;
	}

	public void updateAccountInfo(){
		AccountSessionManager.getInstance().updateSessionLocalInfo(this);
	}

	public Optional<Instance> getInstance() {
		return Optional.ofNullable(AccountSessionManager.getInstance().getInstanceInfo(domain));
	}

	public Uri getInstanceUri() {
		return new Uri.Builder()
				.scheme("https")
				.authority(getInstance().map(i -> i.normalizedUri).orElse(domain))
				.build();
	}

	public String getDefaultAvatarUrl() {
		return getInstance()
				.map(instance->"https://"+domain+(instance.isAkkoma() ? "/images/avi.png" : "/avatars/original/missing.png"))
				.orElse("");
	}
}
