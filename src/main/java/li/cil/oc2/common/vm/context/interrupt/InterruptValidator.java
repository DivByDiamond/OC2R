package li.cil.oc2.common.vm.context.interrupt;

public interface InterruptValidator {
    boolean isMaskValid(int mask);

    int getMaskedInterrupts(int interrupts);
}
