package com.hjf.router.utils;


/**
 * APT 自动生成文件的命名规则
 */
public class RouterJavaFilePathUtil {

    public static String getAutowiredJavaFilePath(String classSimpleName) {
        return classSimpleName + "$$Router$$Autowired";
    }

    public static String genModuleClientClassPath(String host) {
        return "com.hjf.router.gen.Router$$" + firstCharUpperCase(host);
//        return "com.hjf.gen.router.RouterClient$$" + firstCharUpperCase(host);
    }

    public static String genRouterTable(String host) {
        return  "./UIRouterTable/RouterTable_" + firstCharUpperCase(host) + ".txt";
    }

    private static String firstCharUpperCase(String str) {
        char[] ch = str.toCharArray();
        if (ch[0] >= 'a' && ch[0] <= 'z') {
            ch[0] = (char) (ch[0] - 32);
        }
        return new String(ch);
    }
}
