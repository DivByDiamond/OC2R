package li.cil.oc2.common.inet.internet;

import java.util.Optional;
import net.minecraft.nbt.Tag;

public interface InternetConnection {
    Optional<Tag> saveAdapterState();

    void stop();
}