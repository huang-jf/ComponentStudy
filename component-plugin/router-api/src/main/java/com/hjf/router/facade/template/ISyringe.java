package com.hjf.router.facade.template;

import android.os.Bundle;

import com.hjf.router.exceptions.ParamException;


/**
 * 注射器
 */
public interface ISyringe {

    /**
     * 开始注入
     *
     * @param self the container itself, members to be inject into have been annotated
     *             with one annotation called Autowired
     */
    void inject(Object self);
}
