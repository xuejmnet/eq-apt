package com.eq.autopops;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;

/**
 * create time 2024/5/10 08:53
 * 文件说明
 *
 * @author xuejiaming
 */
public class Permit {
    private static final long ACCESSIBLE_OVERRIDE_FIELD_OFFSET;
    private static final IllegalAccessException INIT_ERROR;
    private static final sun.misc.Unsafe UNSAFE = (sun.misc.Unsafe) reflectiveStaticFieldAccess(sun.misc.Unsafe.class, "theUnsafe");

    static {
        Field f;
        long g;
        Throwable ex;

        try {
            g = getOverrideFieldOffset();
            ex = null;
        } catch (Throwable t) {
            f = null;
            g = -1L;
            ex = t;
        }

        ACCESSIBLE_OVERRIDE_FIELD_OFFSET = g;
        if (ex == null) INIT_ERROR = null;
        else if (ex instanceof IllegalAccessException) INIT_ERROR = (IllegalAccessException) ex;
        else {
            INIT_ERROR = new IllegalAccessException("Cannot initialize Unsafe-based permit");
            INIT_ERROR.initCause(ex);
        }
    }
    private static long getOverrideFieldOffset() throws Throwable {
        Field f = null;
        Throwable saved = null;
        try {
            f = AccessibleObject.class.getDeclaredField("override");
        } catch (Throwable t) {
            saved = t;
        }

        if (f != null) {
            return UNSAFE.objectFieldOffset(f);
        }
        // The below seems very risky, but for all AccessibleObjects in java today it does work, and starting with JDK12, making the field accessible is no longer possible.
        try {
            return UNSAFE.objectFieldOffset(Fake.class.getDeclaredField("override"));
        } catch (Throwable t) {
            throw saved;
        }
    }
    static class Fake {
        boolean override;
        Object accessCheckCache;
    }

    private static Object reflectiveStaticFieldAccess(Class<?> c, String fName) {
        try {
            Field f = c.getDeclaredField(fName);
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            return null;
        }
    }
    public static Field getField(Class<?> c, String fName) throws NoSuchFieldException {
        Field f = null;
        Class<?> oc = c;
        while (c != null) {
            try {
                f = c.getDeclaredField(fName);
                break;
            } catch (NoSuchFieldException e) {}
            c = c.getSuperclass();
        }

        if (f == null) throw new NoSuchFieldException(oc.getName() + " :: " + fName);

        return setAccessible(f);
    }
    public static <T extends AccessibleObject> T setAccessible(T accessor) {
        if (INIT_ERROR == null) {
            UNSAFE.putBoolean(accessor, ACCESSIBLE_OVERRIDE_FIELD_OFFSET, true);
        } else {
            accessor.setAccessible(true);
        }

        return accessor;
    }

}
