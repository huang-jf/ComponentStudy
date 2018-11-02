package com.hjf.plugin.copy_from_manifesteditor

import groovy.xml.Namespace

class ManifestParser {

    private File sourceFile
    private Node manifestRoot
    private HashMap<String, Namespace> xmlNameSpaceMap = new HashMap<>()
    private String packageName = ""

    ManifestParser(File manifestFile) {
        try {
            sourceFile = manifestFile
            manifestRoot = new XmlParser().parse(manifestFile)
            // 获取命名空间
            xmlNameSpaceMap = Utils.parseXMLNameSpace(manifestFile.text)
            // 获取包名
            packageName = manifestRoot.attribute("package")
            System.out.println("manifest package name:  ${packageName}")
        } catch (Exception e) {
            throw new Exception("ManifestParser : ${manifestFile.getAbsolutePath() A} file parsed error!")
        }
    }

    /**
     * 编辑节点
     * @param matchRuleString 内容格式参考：{@link ManifestEditExtension#editNodes} 第一条
     * -     "application/activity?android:name=.ModuleAMainActivity&android:label=aaaaaaaa"
     * @param setValues 内容格式参考：{@link ManifestEditExtension#editNodes} 第二条
     */
    void editNode(String matchRuleString, String setValues) {

        // 路径
        String[] nodeAllPaths = getTargetNodePaths(matchRuleString)
        System.out.println("Target Node Paths:  ${nodeAllPaths}")

        // 匹配条件
        Map<String, String> nodeMatchAttrMap = getTargetNodeMatchRuleMap(matchRuleString)

        System.out.println("Target Node Match Attr:  ${nodeMatchAttrMap.entrySet().toString()}")

        NodeList targetNodeList = Utils.getNodes4ManifestWithMatchRule(manifestRoot, nodeAllPaths, nodeMatchAttrMap, xmlNameSpaceMap)

        // 获取将要设置的值，并赋值给指定节点
        System.out.println("Edit Target Node List Num:  ${targetNodeList.size()}")
        targetNodeList.each { targetNode ->
            Utils.editNodeAttr(targetNode, setValues, xmlNameSpaceMap)
        }
    }

    /**
     * 将 Node 内容替换原本XML文件内容
     */
    void save() {
        new XmlNodePrinter(new PrintWriter(new FileWriter(sourceFile))).print(manifestRoot)
    }

    /**
     * 获取目标节点的完整路径
     * @param matchRuleString 内容格式参考：{@link ManifestEditExtension#editNodes} 第一条:节点全路径
     * -     "application/activity ?......"
     * @return
     */
    private static String[] getTargetNodePaths(String matchRuleString) {
        return matchRuleString.split("[?]")[0].split("[/]")
    }
    /**
     * 获取目标节点的属性值需要满足的 Map
     * @param matchRuleString 内容格式参考：{@link ManifestEditExtension#editNodes} 第一位:节点属性匹配
     * -     "........? android:name=.ModuleAMainActivity&android:label=aaaaaaaa&parent=1"
     * @return map： <android:name, com.cy_life.sample.module_a.ModuleAMainActivity>
     */
    private Map<String, String> getTargetNodeMatchRuleMap(String matchRuleString) {
        HashMap<String, String> nodeMatchAttrMap = new HashMap<>()
        if (matchRuleString.contains("?")) {
            matchRuleString = matchRuleString.split("[?]")[1]
            if (!"".equals(matchRuleString)) {
                String[] strs = matchRuleString.split("[&]")
                strs.each { str ->
                    String key = str.split("[=]")[0]
                    String value = str.split("[=]")[1]
                    // 添加包名的前提条件：
                    // 1. 小数点开头
                    // 2. 属性key值包含： name
                    if (value.startsWith(".") && key.contains("android:name")) {
                        value = packageName + value
                    }
                    nodeMatchAttrMap.put(key, value)
                }
            }
        }
        return nodeMatchAttrMap
    }
}
