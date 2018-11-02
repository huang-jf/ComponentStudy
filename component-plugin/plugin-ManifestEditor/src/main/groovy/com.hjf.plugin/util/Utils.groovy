package com.hjf.plugin.util

import groovy.xml.Namespace


class Utils {

    // attr 属性值校验时，遇到特殊值便不校验, 校验方法：SPECIAL_MATCH_ATTR_KEYS.contains(","+keyString+",")
    public static final String SPECIAL_MATCH_ATTR_KEYS = ",parent,"

    public final static String ATTR_PACKAGE_KEY_STRING = "package"

    /**
     * 解析 Manifest 中的命名空间
     * 解析规则根据生成的 Manifest 文件数据为准，AS等编译期中的不一定是最后的文件
     * @param manifestText 最终 manifest 文件内容
     * @return map: <android, Namespace("http://schemas.android.com/apk/res/android", "android")>
     */
    static HashMap<String, Namespace> parseXMLNameSpace(String manifestText) {
        HashMap<String, Namespace> cacheMap = new HashMap<>()
        // 实际的 Manifest 内容
//        System.out.println("Manifest XML Content text:  ${manifestText}")

        // 解析命名空间并缓存，命名空间不一定在顶级XML内
        while (manifestText.contains("xmlns:")) {
            int startIndex = manifestText.indexOf("xmlns:")
            manifestText = manifestText.substring(startIndex + "xmlns:".length())
//            System.out.println("manifestText :  ${manifestText}")
            // key
            int midIndex = manifestText.indexOf("=")
            String key = manifestText.substring(0, midIndex)
//            System.out.println("Manifest XML key:  ${key}")
            // value
            startIndex = manifestText.indexOf("\"")
            manifestText = manifestText.substring(startIndex + "\"".length())
//            System.out.println("manifestText :  ${manifestText}")
            midIndex = manifestText.indexOf("\"")
            String value = manifestText.substring(0, midIndex)
//            System.out.println("Manifest XML value:  ${value}")
            // put cache
            cacheMap.put(key, new Namespace(value, key))
            // del this key-value
            manifestText = manifestText.substring(midIndex)
//            System.out.println("manifestText del:  ${manifestText}")
        }

        System.out.println("name space map key:  ${cacheMap.keySet()}")
        return cacheMap
    }

    /**
     * 检测节点是否符合节点匹配规则
     * @param node 节点对象
     * @param targetNodeAttrMap 目标节点Attr设置
     * @param xmlNameSpaceMap 命名空间缓存
     * @return true 符合
     */
    static boolean isMatch(Node node, Map<String, String> targetNodeAttrMap, HashMap<String, Namespace> xmlNameSpaceMap) {
//        System.out.println(" isMatch node attributes:  ${node.attributes()} ")

        for (Map.Entry<String, String> entry : targetNodeAttrMap.entrySet()) {

            // 特殊匹配条件，略过
            if (SPECIAL_MATCH_ATTR_KEYS.contains("," + entry.getKey() + ",")) {
                continue
            }

//            System.out.println("isMatch key：${entry.getKey()} value：${entry.getValue()}")

            // key: android:name
            String[] keys = entry.getKey().trim().split(":")
            if (keys.length == 2) {
                Namespace np = xmlNameSpaceMap.get(keys[0])
                String nodeValue = node.attribute(np.get(keys[1]))
//                System.out.println("node attr value ${nodeValue}")
                if (!entry.getValue().equals(nodeValue)) {
//                    System.out.println("isMatch false")
                    return false
                }
            }
        }
//        System.out.println("isMatch true")
        return true
    }
    /**
     * 获取 Manifest 文件中符合条件的所有节点
     * @param manifestRootNode XML文件顶级节点
     * @param targetNodePaths 目标节点全路径
     * @param targetNodeAttrMap 目标节点Attr设置
     * @param xmlNameSpaceMap 命名空间
     */
    public
    static NodeList getNodes4ManifestWithMatchRule(Node manifestRootNode, String[] targetNodePaths, Map<String, String> targetNodeAttrMap, HashMap<String, Namespace> xmlNameSpaceMap) {
        // 3 获取所有路径匹配的节点
        // 从顶级节点获取第一个路径匹配的所有节点
        NodeList targetNodeList = manifestRootNode.get(targetNodePaths[0])
//        System.out.println("1. 获取所有的第一节点:  ${targetNodeList}")
        // 从 1 开始处理
        for (int i = 1; i < targetNodePaths.length; i++) {
            // 临时记录 targetNodeList 再次筛选后的结果
            NodeList tempNodeList = new NodeList()
            for (Node node : targetNodeList) {
//                System.out.println("2. 遍历节点:  ${node}")
                NodeList indexNodeList = node.get(targetNodePaths[i])
//                System.out.println("3. 获取 ${nodeAllPaths[i]} 的结果：\n${indexNodeList}")
                // 空数据，不处理
                if (indexNodeList.isEmpty()) {
                    continue
                }
                // 最后一个节点路径，开始进行条件匹配
                if (i == targetNodePaths.length - 1) {
//                    System.out.println("4. 最后一个节点路径，开始进行条件匹配")
                    indexNodeList.each { matchNode ->
                        // 符合条件则添加保存
                        if (Utils.isMatch(matchNode, targetNodeAttrMap, xmlNameSpaceMap)) {
                            int parentNum = 0
                            try {
                                parentNum = Integer.parseInt(targetNodeAttrMap.get("parent"))
                            } catch (Exception ignored) {
                            }
                            Node targetNode = matchNode
                            for (int j = 0; j < parentNum; j++) {
                                targetNode = targetNode.parent()
                            }
                            tempNodeList.add(targetNode)
                        }
                    }
                }
                // 中间节点处理，无需进行条件匹配
                else {
                    tempNodeList.addAll(indexNodeList)
                }
            }
            targetNodeList.clear()
            targetNodeList.addAll(tempNodeList)

//            System.out.println("第${i}节点 NodeList 内容:  ${targetNodeList}")
        }

//        System.out.println("符合条件的 NodeList 内容:  ${targetNodeList}")
        return targetNodeList
    }

    /**
     * 给指定节点修改指定的属性值
     * @param targetNodeList 目标节点集合
     * @param setValues 要设置的属性值。按规则解析  {@link ManifestEditExtension#editNodes}
     * -             "android:label=del&android:taskAffinity=123&child:intent-filter"
     * -             del: 删除
     * -            child:intent-filter = del： 删除指定子节点 <intent-filter>
     * @param xmlNameSpaceMap 命名空间
     */
    public
    static void editNodeAttr(Node targetNode, String setValues, HashMap<String, Namespace> xmlNameSpaceMap) {

        String[] keyAndValues = setValues.split("&")

        for (int i = 0; i < keyAndValues.length; i++) {
            // get keys and value
            String[] keys = keyAndValues[i].split("=")[0].trim().split(":")
            String value = keyAndValues[i].split("=")[1]
            if (keys != null && keys.length >= 2) {
                // 子节点删除
                if ("child".equals(keys[0]) && "del".equals(value)) {
                    NodeList delNodes = targetNode.get(keys[1])
                    delNodes.each { delNode ->
                        targetNode.remove(delNode)
                        System.out.println("${targetNode.name()} 删除子节点->${keys[1]}")
                    }
                }
                // 属性更改
                else {
                    Namespace np = xmlNameSpaceMap.get(keys[0])
                    if (np == null) {
                        continue
                    }
                    // 没有正确获取到 value
                    if (value == null || "".equals(value)) {
                        continue
                    }
                    // 删除 attr attr
                    if ("del".equals(value)) {
                        targetNode.attributes().remove(np.get(keys[1]))
                        System.out.println("${targetNode.name()} 删除ATTR属性->${keys[1]}")
                    }
                    // 添加或修改 attr
                    else {
                        targetNode.attributes().put(np.get(keys[1]), value)
                        System.out.println("${targetNode.name()} 修改ATTR属性->${keys[1]}")
                    }
                }
            }
        }
    }
}
