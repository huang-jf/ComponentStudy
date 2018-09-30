package com.hjf.router.router;


import com.hjf.router.utils.RouterJavaFilePathUtil;

import java.util.HashMap;
import java.util.Map;

final class DNSServer {

    private Map<String, ModuleClient> moduleClientCache = new HashMap<>();

    public void addModuleClient(String host) {
        if (moduleClientCache.containsKey(host) && moduleClientCache.get(host) != null) {
            return;
        }
        String path = RouterJavaFilePathUtil.genModuleClientClassPath(host);
        try {
            Class cla = Class.forName(path);
            ModuleClient client = (ModuleClient) cla.newInstance();
            client.inputService();
            moduleClientCache.put(host, client);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    public void removeModuleClient(String host) {
        moduleClientCache.remove(host);
    }

    public ModuleClient getModuleClient(String host) {
        return moduleClientCache.get(host);
    }
}
