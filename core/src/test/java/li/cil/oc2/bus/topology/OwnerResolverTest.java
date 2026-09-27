package li.cil.oc2.bus.topology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class OwnerResolverTest {
    private record Ctl(String name, long key) {}

    private static final Comparator<Ctl> BY_NAME = Comparator.comparing(Ctl::name);

    @Test
    void lowestKeyOwns() {
        final var owner = OwnerResolver.owner(
                List.of(new Ctl("b", 20), new Ctl("a", 10), new Ctl("c", 30)), Ctl::key, BY_NAME);
        assertEquals("a", owner.orElseThrow().name());
    }

    @Test
    void failoverWhenOwnerDisappears() {
        final var a = new Ctl("a", 10);
        final var b = new Ctl("b", 20);
        assertEquals(a, OwnerResolver.owner(List.of(a, b), Ctl::key, BY_NAME).orElseThrow());
        assertEquals(b, OwnerResolver.owner(List.of(b), Ctl::key, BY_NAME).orElseThrow());
    }

    @Test
    void tiesAreStable() {
        final var x = new Ctl("x", 5);
        final var y = new Ctl("y", 5);
        assertEquals(x, OwnerResolver.owner(List.of(y, x), Ctl::key, BY_NAME).orElseThrow());
        assertEquals(x, OwnerResolver.owner(List.of(x, y), Ctl::key, BY_NAME).orElseThrow());
    }

    @Test
    void noControllersMeansNoOwner() {
        assertTrue(OwnerResolver.owner(List.<Ctl>of(), Ctl::key, BY_NAME).isEmpty());
    }
}
