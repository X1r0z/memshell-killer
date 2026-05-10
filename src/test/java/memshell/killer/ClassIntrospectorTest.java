package memshell.killer;

import memshell.killer.inspect.ClassInfo;
import memshell.killer.inspect.ClassIntrospector;
import org.junit.Assert;
import org.junit.Test;

public class ClassIntrospectorTest {
    @Test
    public void inspectsDeclaredFieldsAndMethodsOnly() {
        ClassInfo classInfo = ClassIntrospector.inspect(Child.class);
        Assert.assertEquals(Child.class.getName(), classInfo.className);
        Assert.assertEquals(Parent.class.getName(), classInfo.superClass);
        Assert.assertTrue(classInfo.fields.toString(), classInfo.fields.toString().contains("childField"));
        Assert.assertFalse(classInfo.fields.toString(), classInfo.fields.toString().contains("parentField"));
        Assert.assertTrue(classInfo.methods.toString(), classInfo.methods.toString().contains("childMethod"));
        Assert.assertFalse(classInfo.methods.toString(), classInfo.methods.toString().contains("parentMethod"));
    }

    static class Parent {
        String parentField;
        void parentMethod() {
        }
    }

    static class Child extends Parent implements Runnable {
        private int childField;

        public void run() {
        }

        String childMethod(String value) {
            return value;
        }
    }
}
