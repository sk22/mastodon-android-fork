package org.joinmastodon.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.joinmastodon.android.api.session.AccountSessionManager;


public class PanicResponderActivity extends Activity {
    public static final String PANIC_TRIGGER_ACTION = "info.guardianproject.panic.action.TRIGGER";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        if (intent != null && PANIC_TRIGGER_ACTION.equals(intent.getAction())) {
            AccountSessionManager.getInstance().nuke();
        }

        finishAndRemoveTask();
    }
}