package com.hjf.plugin.util

class ManifestEditExtension {


    public static final String EditInfoHint = "[application/activity?android:name=.ModuleAMainActivity&android:label=aaaaaaaa,android:label=del&android:taskAffinity=123]";

    /**
     * 是否启用
     */
    boolean enabled

    /**
     * 编辑节点信息，数组集合，长度为2
     * {
     *  "application/activity?android:name=.ModuleAMainActivity&android:label=aaaaaaaa&parent=1",
     *  "android:label=del&android:taskAffinity=123"
     * }
     *
     * 第一位是节点匹配信息
     * -    节点全路径： application , activity
     * -    节点属性匹配： android:name=.ModuleAMainActivity && xx=xx
     * -       匹配条件注意点：
     * -        .ModuleAMainActivity： 要自动添加包名，manifest顶级节点中有包名值
     * -        android:name： 域名替换成全值，manifest顶级节点中有包名值
     * -        parent： 表示取父节点，1取一次父节点，2取两次父节点
     *
     * 第二位是节点赋值信息
     * -    android:taskAffinity = 123： 修改或新增节点属性值
     * -    android:taskAffinity = del： 删除指定节点属性值
     * -    child:intent-filter = del： 删除指定子节点 <intent-filter>
     */
    Iterable<List<String>> editNodes
}
