package org.joinmastodon.android;

import android.Manifest;
import android.app.Fragment;
import android.app.assist.AssistContent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.requests.search.GetSearchResults;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.fragments.HomeFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.fragments.onboarding.AccountActivationFragment;
import org.joinmastodon.android.fragments.onboarding.CustomWelcomeFragment;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.SearchResults;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;
import org.joinmastodon.android.utils.ProvidesAssistContent;
import org.parceler.Parcels;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

import me.grishka.appkit.FragmentStackActivity;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class MainActivity extends FragmentStackActivity implements ProvidesAssistContent {
	private static final String TAG="MainActivity";

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState){
		AccountSession session=getCurrentSession();
		UiUtils.setUserPreferredTheme(this, session);
		super.onCreate(savedInstanceState);

		Thread.UncaughtExceptionHandler defaultHandler=Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((t, e)->{
			File file=new File(MastodonApp.context.getFilesDir(), "crash.log");
			try(FileOutputStream out=new FileOutputStream(file)){
				PrintWriter writer=new PrintWriter(out);
				writer.println(BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")");
				writer.println(Instant.now().toString());
				writer.println();
				e.printStackTrace(writer);
				writer.flush();
			}catch(IOException x){
				Log.e(TAG, "Error writing crash.log", x);
			}finally{
				defaultHandler.uncaughtException(t, e);
			}
		});

		if(savedInstanceState==null){
			restartHomeFragment();
		}

		if(GithubSelfUpdater.needSelfUpdating()){
			GithubSelfUpdater.getInstance().maybeCheckForUpdates();
		}
	}

	@Override
	protected void onNewIntent(Intent intent){
		super.onNewIntent(intent);
		AccountSessionManager.getInstance().maybeUpdateLocalInfo();
		if (intent.hasExtra("fromExternalShare")) showFragmentForExternalShare(intent.getExtras());
		else if (intent.getBooleanExtra("fromNotification", false)) {
			String accountID=intent.getStringExtra("accountID");
			try{
				AccountSessionManager.getInstance().getAccount(accountID);
			}catch(IllegalStateException x){
				return;
			}
			if(intent.hasExtra("notification")){
				Notification notification=Parcels.unwrap(intent.getParcelableExtra("notification"));
				showFragmentForNotification(notification, accountID);
			}else{
				AccountSessionManager.getInstance().setLastActiveAccountID(accountID);
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putString("tab", "notifications");
				Fragment fragment=new HomeFragment();
				fragment.setArguments(args);
				showFragmentClearingBackStack(fragment);
			}
		}else if(intent.getBooleanExtra("compose", false)){
			showCompose();
		}else if(Intent.ACTION_VIEW.equals(intent.getAction())){
			handleURL(intent.getData(), null);
		}/*else if(intent.hasExtra(PackageInstaller.EXTRA_STATUS) && GithubSelfUpdater.needSelfUpdating()){
			GithubSelfUpdater.getInstance().handleIntentFromInstaller(intent, this);
		}*/
	}

	public void handleURL(Uri uri, String accountID){
		if(uri==null)
			return;
		if(!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme()))
			return;
		AccountSession session;
		if(accountID==null)
			session=AccountSessionManager.getInstance().getLastActiveAccount();
		else
			session=AccountSessionManager.get(accountID);
		if(session==null || !session.activated)
			return;
		openSearchQuery(uri.toString(), session.getID(), R.string.opening_link, false);
	}

	public void openSearchQuery(String q, String accountID, int progressText, boolean fromSearch){
		new GetSearchResults(q, null, true, null, 0, 0)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(SearchResults result){
						Bundle args=new Bundle();
						args.putString("account", accountID);
						if(result.statuses!=null && !result.statuses.isEmpty()){
							args.putParcelable("status", Parcels.wrap(result.statuses.get(0)));
							Nav.go(MainActivity.this, ThreadFragment.class, args);
						}else if(result.accounts!=null && !result.accounts.isEmpty()){
							args.putParcelable("profileAccount", Parcels.wrap(result.accounts.get(0)));
							Nav.go(MainActivity.this, ProfileFragment.class, args);
						}else{
							Toast.makeText(MainActivity.this, fromSearch ? R.string.no_search_results : R.string.link_not_supported, Toast.LENGTH_SHORT).show();
						}
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(MainActivity.this);
					}
				})
				.wrapProgress(this, progressText, true)
				.exec(accountID);
	}

	private void showFragmentForNotification(Notification notification, String accountID){
		try{
			notification.postprocess();
		}catch(ObjectValidationException x){
			Log.w("MainActivity", x);
			return;
		}
		Bundle args = new Bundle();
		args.putBoolean("noTransition", true);
		UiUtils.showFragmentForNotification(this, notification, accountID, args);
	}

	private void showFragmentForExternalShare(Bundle args) {
		String className = args.getString("fromExternalShare");
		Fragment fragment = switch (className) {
			case "ThreadFragment" -> new ThreadFragment();
			case "ProfileFragment" -> new ProfileFragment();
			default -> null;
		};
		if (fragment == null) return;
		args.putBoolean("_can_go_back", true);
		fragment.setArguments(args);
		showFragment(fragment);
	}

	private void showCompose(){
		AccountSession session=AccountSessionManager.getInstance().getLastActiveAccount();
		if(session==null || !session.activated)
			return;
		ComposeFragment compose=new ComposeFragment();
		Bundle composeArgs=new Bundle();
		composeArgs.putString("account", session.getID());
		compose.setArguments(composeArgs);
		showFragment(compose);
	}

	private void maybeRequestNotificationsPermission(){
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
			requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
		}
	}

	/**
	 * when opening app through a notification: if (thread) fragment "can go back", clear back stack
	 * and show home fragment. upstream's implementation doesn't require this as it opens home first
	 * and then immediately switches to the notification's ThreadFragment. this causes a black
	 * screen in megalodon, for some reason, so i'm working around this that way.
 	 */
	@Override
	public void onBackPressed() {
		Fragment currentFragment = getFragmentManager().findFragmentById(
				(fragmentContainers.get(fragmentContainers.size() - 1)).getId()
		);
		Bundle currentArgs = currentFragment.getArguments();
		if (fragmentContainers.size() != 1
				|| currentArgs == null
				|| !currentArgs.getBoolean("_can_go_back", false)) {
			super.onBackPressed();
			return;
		}
		if (currentArgs.getBoolean("_finish_on_back", false)) {
			finish();
		} else if (currentArgs.containsKey("account")) {
			Bundle args = new Bundle();
			args.putString("account", currentArgs.getString("account"));
			if (getIntent().getBooleanExtra("fromNotification", false)) {
				args.putString("tab", "notifications");
			}
			Fragment fragment=new HomeFragment();
			fragment.setArguments(args);
			showFragmentClearingBackStack(fragment);
		}
	}

	public Fragment getCurrentFragment() {
		for (int i = fragmentContainers.size() - 1; i >= 0; i--) {
			FrameLayout fl = fragmentContainers.get(i);
			if (fl.getVisibility() == View.VISIBLE) {
				return getFragmentManager().findFragmentById(fl.getId());
			}
		}
		return null;
	}

	@Override
	public void onProvideAssistContent(AssistContent assistContent) {
		super.onProvideAssistContent(assistContent);
		Fragment fragment = getCurrentFragment();
		if (fragment != null) callFragmentToProvideAssistContent(fragment, assistContent);
	}

	public AccountSession getCurrentSession(){
		AccountSession session;
		Bundle args=new Bundle();
		Intent intent=getIntent();
		if(intent.hasExtra("fromExternalShare")) {
			return AccountSessionManager.getInstance()
					.getAccount(intent.getStringExtra("account"));
		}

		boolean fromNotification = intent.getBooleanExtra("fromNotification", false);
		boolean hasNotification = intent.hasExtra("notification");
		if(fromNotification){
			String accountID=intent.getStringExtra("accountID");
			try{
				session=AccountSessionManager.getInstance().getAccount(accountID);
				if(!hasNotification) args.putString("tab", "notifications");
			}catch(IllegalStateException x){
				session=AccountSessionManager.getInstance().getLastActiveAccount();
			}
		}else{
			session=AccountSessionManager.getInstance().getLastActiveAccount();
		}
		return session;
	}

	public void restartActivity(){
		finish();
		startActivity(new Intent(this, MainActivity.class));
	}

	public void restartHomeFragment(){
		if(AccountSessionManager.getInstance().getLoggedInAccounts().isEmpty()){
			showFragmentClearingBackStack(new CustomWelcomeFragment());
		}else{
			AccountSession session;
			Bundle args=new Bundle();
			Intent intent=getIntent();
			if(intent.hasExtra("fromExternalShare")) {
				AccountSessionManager.getInstance()
						.setLastActiveAccountID(intent.getStringExtra("account"));
				AccountSessionManager.getInstance().maybeUpdateLocalInfo(
						AccountSessionManager.getInstance().getLastActiveAccount());
				showFragmentForExternalShare(intent.getExtras());
				return;
			}

			boolean fromNotification = intent.getBooleanExtra("fromNotification", false);
			boolean hasNotification = intent.hasExtra("notification");
			if(fromNotification){
				String accountID=intent.getStringExtra("accountID");
				try{
					session=AccountSessionManager.getInstance().getAccount(accountID);
					if(!hasNotification) args.putString("tab", "notifications");
				}catch(IllegalStateException x){
					session=AccountSessionManager.getInstance().getLastActiveAccount();
				}
			}else{
				session=AccountSessionManager.getInstance().getLastActiveAccount();
			}
			AccountSessionManager.getInstance().maybeUpdateLocalInfo(session);
			args.putString("account", session.getID());
			Fragment fragment=session.activated ? new HomeFragment() : new AccountActivationFragment();
			fragment.setArguments(args);
			if(fromNotification && hasNotification){
				Notification notification=Parcels.unwrap(intent.getParcelableExtra("notification"));
				showFragmentForNotification(notification, session.getID());
			} else if (intent.getBooleanExtra("compose", false)){
				showCompose();
			} else if (Intent.ACTION_VIEW.equals(intent.getAction())){
				handleURL(intent.getData(), null);
			} else {
				showFragmentClearingBackStack(fragment);
				maybeRequestNotificationsPermission();
			}
		}
	}
}
