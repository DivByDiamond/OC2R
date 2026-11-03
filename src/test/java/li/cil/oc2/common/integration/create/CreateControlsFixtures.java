package li.cil.oc2.common.integration.create;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Minimal independent doubles of the public methods used by the optional adapter. */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // Namespace for nested public API test doubles.
final class CreateControlsFixtures {
    public static final class Smart {
        private final List<Object> behaviours = new ArrayList<>();

        public Collection<Object> getAllBehaviours() {
            return List.copyOf(behaviours);
        }

        Smart with(final Object setting) {
            behaviours.add(setting);
            return this;
        }
    }

    public static class Scroll {
        public Component label = Component.literal("Rotation speed");
        private final int min;
        private final int max;
        int value;
        int callbacks;
        int changed;
        int syncs;
        boolean active = true;

        Scroll(final int min, final int max) {
            this.min = min;
            this.max = max;
        }

        public int getValue() {
            return value;
        }

        public void setValue(final int next) {
            final int clamped = Math.clamp(next, min, max);
            if (clamped == value) return;
            value = clamped;
            callbacks++;
            changed++;
            syncs++;
        }

        public String formatValue() {
            return value + " RPM";
        }

        public boolean isActive() {
            return active;
        }
    }

    enum Mode { KEEP, PLACE, PLACE_RETURNED }

    public static final class Options extends Scroll {
        Options() {
            super(0, Mode.values().length - 1);
        }

        public Mode get() {
            return Mode.values()[value];
        }
    }

    public static final class Broken extends Scroll {
        Broken() {
            super(-256, 256);
        }

        @Override
        public void setValue(final int value) {
            throw new IllegalStateException("Machine offline");
        }
    }

    private CreateControlsFixtures() {}
}
