package memshell.killer;

import memshell.killer.util.Reflects;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ReflectsTest {
    @Test
    public void readsInheritedPrivateFieldsAndMethods() throws Exception {
        Child child = new Child();
        Assert.assertEquals("parent", Reflects.get(child, "value"));
        Assert.assertEquals("hello bob", Reflects.invoke(child, "hello", new Class[]{String.class}, new Object[]{"bob"}));
    }

    @Test
    public void convertsArraysAndListsToObjectLists() {
        Assert.assertEquals(Arrays.asList("a", "b"), Reflects.asList(new String[]{"a", "b"}));
        Assert.assertEquals(Arrays.asList("x"), Reflects.asList(Arrays.asList("x")));
    }

    static class Parent {
        private String value = "parent";

        private String hello(String name) {
            return "hello " + name;
        }
    }

    static class Child extends Parent {
    }
}
