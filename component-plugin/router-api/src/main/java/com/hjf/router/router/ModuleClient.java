package com.hjf.router.router;

/**
 * 各模块客服端，用模块的host最为key存放在DNS服务器上
 * 职责：根据uri中的path找到指定的服务界面：Activity、Fragment
 */
public interface ModuleClient {

    String getClientHost();

    boolean hasClientService(String uriPath);

    Class getClientService(String uriPath);

    void inputService();
}
