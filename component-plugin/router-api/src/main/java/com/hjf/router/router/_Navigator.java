package com.hjf.router.router;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * 导航者
 * TODO return fragment
 */
public final class _Navigator {

    private String uriPath;
    private ModuleClient client;
    private Bundle mBundle;

    public _Navigator(String uriPath, ModuleClient client, Bundle bundle) {
        this.uriPath = uriPath;
        this.client = client;
        this.mBundle = (null == bundle ? new Bundle() : bundle);
    }

    // Animation
    private Bundle optionsCompat;    // The transition animation of activity
    private int enterAnim = -1;
    private int exitAnim = -1;

    public Object navigation(Context context) {
        Class target = client.getClientService(uriPath);
        Intent intent = new Intent(context, target);
        intent.putExtras(mBundle);
        context.startActivity(intent);
        return true;
    }

    public Object navigation(Activity context, int requestCode) {
        Class target = client.getClientService(uriPath);
        Intent intent = new Intent(context, target);
        intent.putExtras(mBundle);
        context.startActivityForResult(intent, requestCode);
        return true;
    }
}
