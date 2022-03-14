package simple.caller.callee;

public class C {
    public void baz() {
        int i = 0;
        for (; ++i < 100;) {
            new C();
        }
        return;
    }

}
