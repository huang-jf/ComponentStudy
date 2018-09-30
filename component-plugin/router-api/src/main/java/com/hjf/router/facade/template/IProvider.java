package com.hjf.router.facade.template;


import android.content.Context;

/**
 * Provider interface, base of other interface.
 */
public interface IProvider {

    /**
     * Do your init work in this method, it well be call when processor has been load.
     *
     * @param context ctx
     */
    void init(Context context);
}
