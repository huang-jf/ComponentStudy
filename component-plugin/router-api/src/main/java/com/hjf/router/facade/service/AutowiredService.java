package com.hjf.router.facade.service;

import com.hjf.router.facade.template.IProvider;

/**
 * Service for autowired.
 * 实现类: {@link com.hjf.router.core.AutowiredServerImpl}
 * 实现运作流程: {@link com.hjf.router.facade.template.ISyringe}
 */
public interface AutowiredService extends IProvider {

    /**
     * Autowired core.
     *
     * @param instance the instance who need autowired.
     */
    void autowire(Object instance);
}
