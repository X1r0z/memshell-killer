package memshell.killer;

import memshell.killer.core.ClassMetadata;
import memshell.killer.util.ClassIntrospector;
import org.junit.Assert;
import org.junit.Test;

public class ClassIntrospectorTest {
    @Test
    public void inspectsDeclaredFieldsAndMethodsOnly() {
        ClassMetadata metadata = ClassIntrospector.inspect(Child.class);
        Assert.assertEquals(Child.class.getName(), metadata.className);
        Assert.assertEquals(Parent.class.getName(), metadata.superClass);
        Assert.assertTrue(metadata.fields.toString(), metadata.fields.toString().contains("childField"));
        Assert.assertFalse(metadata.fields.toString(), metadata.fields.toString().contains("parentField"));
        Assert.assertTrue(metadata.methods.toString(), metadata.methods.toString().contains("childMethod"));
        Assert.assertFalse(metadata.methods.toString(), metadata.methods.toString().contains("parentMethod"));
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
