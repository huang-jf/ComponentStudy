package com.hjf.router.router;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.hjf.router.facade.service.AutowiredService;

/**
 * 为了进一步了解ARouter源码
 * 仿造ARouter试验性质的实现
 */
public class Router {

    private Router() {
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
                String path = "/" + TextUtils.join("/", uri.getPathSegments());
                return new Navigator(uri.getPath(), bundle);
            }
        }
        return null;
    }
    /**
     * Inject params and services.
     */
    public void inject(Object thiz) {
        AutowiredService autowiredService = ((AutowiredService) build("/router/service/autowired").navigation());
        if (null != autowiredService) {
            autowiredService.autowire(thiz);
        }
    }

}
