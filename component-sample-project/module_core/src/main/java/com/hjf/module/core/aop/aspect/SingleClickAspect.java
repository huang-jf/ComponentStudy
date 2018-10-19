package com.hjf.module.core.aop.aspect;

import android.util.Log;
import android.view.View;


import com.hjf.module.core.R;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Calendar;

/**
 * 防止View被连续点击,间隔时间600ms
 */
@Aspect
public class SingleClickAspect {

    public static int TIME_TAG = R.id.click_time;
    public static final int MIN_CLICK_DELAY_TIME = 666;

    //方法切入点
    @Pointcut("execution(@com.hjf.module.core.aop.SingleClick * *(..))")
    public void methodAnnotated() {
    }

    //在连接点进行方法替换
    @Around("methodAnnotated()")
    public void aroundJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        View view = null;
        for (Object arg : joinPoint.getArgs())
            if (arg instanceof View) view = (View) arg;
        if (view != null) {
            Object tag = view.getTag(TIME_TAG);
            long lastClickTime = ((tag != null) ? (long) tag : 0);
            Log.d("SingleClickAspect", "lastClickTime:" + lastClickTime);
            long currentTime = Calendar.getInstance().getTimeInMillis();
            //过滤掉 666 毫秒内的连续点击
            if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
                view.setTag(TIME_TAG, currentTime);
                Log.d("SingleClickAspect", "currentTime:" + currentTime);
                //执行原方法
                joinPoint.proceed();
            }
        }
    }
}
