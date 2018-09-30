package com.hjf.router.utils;


import android.net.Uri;
import android.text.TextUtils;

import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 负责解析URI的参数
 */
public class UriUtils {

    public static HashMap<String, String> parseParams(Uri uri) {
        if (uri == null) {
            return new HashMap<String, String>();
        }
        HashMap<String, String> temp = new HashMap<String, String>();
        Set<String> keys = getQueryParameterNames(uri);
        for (String key : keys) {
            temp.put(key, uri.getQueryParameter(key));
        }
        return temp;
    }

    public static Set<String> getQueryParameterNames(Uri uri) {
        String query = uri.getEncodedQuery();
        if (query == null) {
            return Collections.emptySet();
        }

        Set<String> names = new LinkedHashSet<String>();
        int start = 0;
        do {
            int next = query.indexOf('&', start);
            int end = (next == -1) ? query.length() : next;

            int separator = query.indexOf('=', start);
            if (separator > end || separator == -1) {
                separator = end;
            }

            String name = query.substring(start, separator);
            try {
                names.add(URLDecoder.decode(name, "UTF-8"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            start = end + 1;
        } while (start < query.length());

        return Collections.unmodifiableSet(names);
    }


    public static int parseInt(String src) {
        return parseInt(src, 0);
    }

    public static int parseInt(String src, int defaultValue) {
        if (TextUtils.isEmpty(src)) {
            return defaultValue;
        }
        int index = src.indexOf(".");
        if (index > 0) {
            src = src.substring(0, index);
        }
        try {
            return Integer.parseInt(src);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * used to generate safe string of uri for log output
     *
     * @param uri uri
     * @return string
     */
    public static String toSafeString(Uri uri) {
        if (uri == null)
            return "null uri";

        String scheme = uri.getScheme();
        String ssp = uri.getSchemeSpecificPart();
        if (scheme != null) {
            if (scheme.equalsIgnoreCase("tel") || scheme.equalsIgnoreCase("sip")
                    || scheme.equalsIgnoreCase("sms") || scheme.equalsIgnoreCase("smsto")
                    || scheme.equalsIgnoreCase("mailto")) {
                StringBuilder builder = new StringBuilder(64);
                builder.append(scheme);
                builder.append(':');
                if (ssp != null) {
                    for (int i = 0; i < ssp.length(); i++) {
                        char c = ssp.charAt(i);
                        if (c == '-' || c == '@' || c == '.') {
                            builder.append(c);
                        } else {
                            builder.append('x');
                        }
                    }
                }
                return builder.toString();
            } else if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")
                    || scheme.equalsIgnoreCase("ftp")) {
                ssp = "//" + ((uri.getHost() != null) ? uri.getHost() : "")
                        + ((uri.getPort() != -1) ? (":" + uri.getPort()) : "")
                        + "/...";
            }
        }
        // Not a sensitive scheme, but let's still be conservative about
        // the data we include -- only the ssp, not the query params or
        // fragment, because those can often have sensitive info.
        StringBuilder builder = new StringBuilder(64);
        if (scheme != null) {
            builder.append(scheme);
            builder.append(':');
        }
        if (ssp != null) {
            builder.append(ssp);
        }
        return builder.toString();
    }
}
