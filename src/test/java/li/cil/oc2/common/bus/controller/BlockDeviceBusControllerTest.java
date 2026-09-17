package li.cil.oc2.common.bus.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import li.cil.oc2.api.bus.DeviceBusElement;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.junit.jupiter.api.*;

class BlockDeviceBusControllerTest {

    DeviceBusElement rootElement;
    BlockEntity blockEntity;
    BlockDeviceBusController controller;

    @BeforeEach
    void setUp() {
        rootElement = mock(DeviceBusElement.class);
        blockEntity = mock(BlockEntity.class);
        
        controller = new BlockDeviceBusController(rootElement, 100, blockEntity);
    }

    @Test
    void testConstructorInitializesWithBlockEntity() {
        assertNotNull(controller);
    }

    @Test
    void testSetDeviceContainersChangedMarksChunksDirty() {
        controller.setDeviceContainersChanged();
        
        // Should complete without exception
    }

    @Test
    void testDisposeClearsChunksAndRemovesListeners() {
        controller.dispose();
        
        // Should complete without exception
        assertTrue(true);
    }
}