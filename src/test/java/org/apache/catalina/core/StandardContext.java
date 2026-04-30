package org.apache.catalina.core;

import java.util.LinkedHashMap;
import java.util.Map;

public class StandardContext {
    public static Map<Object, Object> instances = new LinkedHashMap<Object, Object>();
    public static Object lastContext;
}
