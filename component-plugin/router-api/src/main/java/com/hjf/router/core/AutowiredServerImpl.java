package com.hjf.router.core;

import android.content.Context;
import android.util.LruCache;

import com.hjf.router.facade.annotation.Route;
import com.hjf.router.utils.RouterJavaFilePathUtil;
import com.hjf.router.facade.service.AutowiredService;
import com.hjf.router.facade.template.ISyringe;

import java.util.ArrayList;
import java.util.List;


@Route(path = "/router/service/autowired")
public class AutowiredServerImpl implements AutowiredService {

    private LruCache<String, ISyringe> classCache;
    private List<String> blackList;

    @Override
    public void init(Context context) {
        classCache = new LruCache<>(66);
        blackList = new ArrayList<>();
    }

    @Override
    public void autowire(Object instance) {
        String className = instance.getClass().getName();
        try {
            if (!blackList.contains(className)) {
                ISyringe autowiredHelper = classCache.get(className);
                if (null == autowiredHelper) {  // No cache.
                    autowiredHelper = (ISyringe) Class.forName(RouterJavaFilePathUtil.getAutowiredJavaFilePath(instance.getClass().getName())).getConstructor().newInstance();
                }
                autowiredHelper.inject(instance);
                classCache.put(className, autowiredHelper);
            }
        } catch (Exception ex) {
            blackList.add(className);    // This instance need not autowired.
        }
    }
}
