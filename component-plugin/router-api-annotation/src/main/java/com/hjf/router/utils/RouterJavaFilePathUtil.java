package com.hjf.router.utils;


/**
 * APT 自动生成文件的命名规则
 * FIXME delete
 */
public class RouterJavaFilePathUtil {

    public static String getAutowiredJavaFilePath(String classSimpleName) {
        return classSimpleName + "$$Router$$Autowired";
    }


    public static String getRouterRootImplClassPath(String moduleName) {
        return "com.hjf.router.routes.Router$$Root$$" + moduleName;
    }


    public static String getRouterGroupImplClassPath(String moduleName) {
        return "com.hjf.router.routes.Router$$Group$$" + moduleName;
    }

    public static String genRouterTableFilePath(String moduleName) {
        return "./UIRouterTable/RouterTable_" + firstCharUpperCase(moduleName) + ".txt";
    }

    private static String firstCharUpperCase(String str) {
        char[] ch = str.toCharArray();
        if (ch[0] >= 'a' && ch[0] <= 'z') {
            ch[0] = (char) (ch[0] - 32);
        }
        return new String(ch);
    }
}
