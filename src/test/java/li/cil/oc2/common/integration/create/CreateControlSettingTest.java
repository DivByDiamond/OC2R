package li.cil.oc2.common.integration.create;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateControlSettingTest {
    @Test
    void settingMetadataCannotBeMutatedThroughAnExternalOptionList() {
        final List<String> options = new ArrayList<>(List.of("NORMAL", "REVERSE"));
        final CreateControlSetting setting = new CreateControlSetting(0, "Mode", 0, "Normal", true, options);
        options.clear();
        assertEquals(List.of("NORMAL", "REVERSE"), setting.options());
        assertThrows(UnsupportedOperationException.class, () -> setting.options().clear());
    }
}
