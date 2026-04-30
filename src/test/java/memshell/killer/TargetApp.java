package memshell.killer;

public class TargetApp {
    private final Helper helper = new Helper();

    public static void main(String[] args) throws Exception {
        new TargetApp().a();
        Thread.sleep(Long.MAX_VALUE);
    }

    void a() {
        b();
    }

    void b() {
        c();
    }

    void c() {
    }

    void d() {
        helper.g();
    }

    static class Helper {
        void g() {
        }
    }
}
