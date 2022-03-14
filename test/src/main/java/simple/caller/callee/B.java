package simple.caller.callee;

public class B {
    public void bar() {
        C c = new C();
        c.baz();
        return;
    }

}