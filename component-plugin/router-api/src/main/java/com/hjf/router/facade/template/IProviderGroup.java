package com.hjf.router.facade.template;


import com.hjf.router.facade.model.RouteMeta;

import java.util.Map;

/**
 * Template of provider group.
 */
public interface IProviderGroup {
    /**
     * Load providers map to input
     *
     * @param providers input
     */
    void loadInto(Map<String, RouteMeta> providers);
}