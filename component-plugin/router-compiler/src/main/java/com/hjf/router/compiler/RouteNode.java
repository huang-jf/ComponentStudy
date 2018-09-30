package com.hjf.router.compiler;

import java.util.Map;

import javax.lang.model.element.Element;

public class RouteNode {

    private String nodeType = "Activity";
    private Element rawType;        // Raw type of route
    private Class<?> destination;   // Destination
    private String path;            // Path of route
    private String desc;            // Desc of route
    private Map<String, String> paramsTypeDesc;

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public Element getRawType() {
        return rawType;
    }

    public void setRawType(Element rawType) {
        this.rawType = rawType;
    }

    public Class<?> getDestination() {
        return destination;
    }

    public void setDestination(Class<?> destination) {
        this.destination = destination;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getParamsTypeDesc() {
        return paramsTypeDesc;
    }

    public void setParamsTypeDesc(Map<String, String> paramsTypeDesc) {
        this.paramsTypeDesc = paramsTypeDesc;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
