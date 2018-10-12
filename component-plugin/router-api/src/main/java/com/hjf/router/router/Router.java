package com.hjf.router.router;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * 存放所有的 DNS server 并自动分配 open uri 任务给能相关的 dns server
 */
public class Router {

    private Router() {
    }

    // TODO
    public static void init(Context context){

    }

    private final static class SingletonHolder {
        private static Router INSTANCE = new Router();
    }

    public static Router getInstance() {
        return SingletonHolder.INSTANCE;
    }


    public Navigator build(String url) {
        return build(url, null);
    }

    public Navigator build(String url, Bundle bundle) {
        if (!TextUtils.isEmpty(url)) {
            if (!url.trim().contains("://")) {
                if (!url.startsWith("tel:") || !url.startsWith("smsto:") || !url.startsWith("file:")) {
                    url = "http://" + url;
                }
            }
            Uri uri = Uri.parse(url);
            if (uri != null) {
//                ModuleClient client = Router.getInstance().dnsServer.getModuleClient(uri.getHost());
//                String path = "/" + TextUtils.join("/", uri.getPathSegments());
//                if (client != null && client.hasClientService(path)) {
//                    return new Navigator(uri.getPath(), client, bundle);
//                }
            }
        }
        return null;
    }
}
