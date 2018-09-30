package com.hjf.router.router;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseModuleClient implements ModuleClient {

    private boolean isInputtedService = false;

    protected Map<String, Class> serviceMapper = new HashMap<>();

    @Override
    public boolean hasClientService(String uriPath) {
        return serviceMapper.containsKey(uriPath);
    }

    @Override
    public Class getClientService(String uriPath) {
        return serviceMapper.get(uriPath);
    }

    @Override
    public void inputService() {
        isInputtedService = true;
    }

    public boolean isInputtedService() {
        return isInputtedService;
    }
}
