package com.hjf.router.facade.service;

import com.hjf.router.facade.template.IProvider;

/**
 * Service for autowired.
 */
public interface AutowiredService extends IProvider {

    /**
     * Autowired core.
     * @param instance the instance who need autowired.
     */
    void autowire(Object instance);
}
