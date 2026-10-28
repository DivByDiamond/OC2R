package li.cil.oc2.common.vm.device.frame;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.OptionalLong;
import li.cil.oc2.api.bus.device.vm.context.MemoryAllocator;
import li.cil.oc2.api.bus.device.vm.context.MemoryRangeAllocator;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class FramePresentAttachmentTest {
    @Test
    void oldNbtRemainsLegacyAndClaimsSeparateBudgetedAperture() {
        final FramePresentAttachment attachment = new FramePresentAttachment();
        attachment.load(new CompoundTag());
        final VMContext context = context();
        final SimpleFramebufferDevice fb = framebuffer();
        assertTrue(attachment.mount(context, fb));
        assertEquals(0, fb.presentationStatus());
        verify(context.getMemoryAllocator()).claimMemory(fb.getLength() + 4096);
        verify(context.getMemoryRangeAllocator()).claimMemoryRange(isA(FramePresentDevice.class));
    }

    @Test
    void nbtAndUnmountPreserveCompletedFrameDespitePartialDraw() throws Exception {
        final FramePresentAttachment original = new FramePresentAttachment();
        final SimpleFramebufferDevice fb = framebuffer();
        assertTrue(original.mount(context(), fb));
        fb.store(0, 1234, 1);
        fb.present(FramePresentDevice.BEGIN);
        fb.store(0, 5678, 1);
        final CompoundTag saved = original.save(fb);
        final FramePresentAttachment restored = new FramePresentAttachment();
        restored.load(saved);
        final SimpleFramebufferDevice next = framebuffer();
        next.store(0, 9999, 1);
        assertTrue(restored.mount(context(), next));
        final ByteBuffer image = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        assertTrue(next.copyFrame(image, true));
        assertEquals(1234, image.getShort(0));
        assertEquals(3, next.presentationStatus());
        restored.unmount(next);
        next.close();
        final SimpleFramebufferDevice remount = framebuffer();
        assertTrue(restored.mount(context(), remount));
        assertTrue(remount.copyFrame(image, true));
        assertEquals(1234, image.getShort(0));
        remount.present(FramePresentDevice.ABORT);
        assertEquals(1234, remount.load(0, 1));
    }

    @Test
    void unavailableSnapshotBudgetOmitsNewOptionalCompanionBeforeMapping() {
        final VMContext context = context();
        when(context.getMemoryAllocator().claimMemory(anyInt())).thenReturn(false);
        assertTrue(new FramePresentAttachment().mount(context, framebuffer()));
        verifyNoInteractions(context.getMemoryRangeAllocator());
    }

    @Test
    void previouslyExposedCompanionCannotDisappearOnBudgetFailureEvenWhileLegacy() {
        final FramePresentAttachment attachment = new FramePresentAttachment();
        final SimpleFramebufferDevice fb = framebuffer();
        assertTrue(attachment.mount(context(), fb));
        final FramePresentAttachment restored = new FramePresentAttachment();
        restored.load(attachment.save(fb));
        final VMContext limited = context();
        when(limited.getMemoryAllocator().claimMemory(anyInt())).thenReturn(false);
        assertFalse(restored.mount(limited, framebuffer()));
        verifyNoInteractions(limited.getMemoryRangeAllocator());
    }

    @Test
    void failedMountSaveAndCleanupPreserveActiveFrameForRetry() throws Exception {
        final FramePresentAttachment original = new FramePresentAttachment();
        final SimpleFramebufferDevice first = framebuffer();
        assertTrue(original.mount(context(), first));
        first.store(0, 1234, 1);
        first.store(2, 1234, 1);
        first.present(FramePresentDevice.BEGIN);
        first.store(0, 5678, 1);
        final FramePresentAttachment restored = new FramePresentAttachment();
        restored.load(original.save(first));
        final VMContext limited = context();
        when(limited.getMemoryAllocator().claimMemory(anyInt())).thenReturn(false);
        final ByteBuffer drawBlob = ByteBuffer.allocate(4);
        final SimpleFramebufferDevice failed = new SimpleFramebufferDevice(2, 1, drawBlob);
        failed.store(0, 5678, 1);
        failed.store(2, 1234, 1);
        assertFalse(restored.mount(limited, failed));
        final FramePresentAttachment savedDuringFailure = new FramePresentAttachment();
        savedDuringFailure.load(restored.save(failed));
        restored.unmount(failed);
        failed.close();
        final SimpleFramebufferDevice retry = new SimpleFramebufferDevice(2, 1, drawBlob);
        assertTrue(restored.mount(context(), retry));
        assertEquals(3, retry.presentationStatus());
        final SimpleFramebufferDevice fromSave = framebuffer();
        assertTrue(savedDuringFailure.mount(context(), fromSave));
        assertEquals(3, fromSave.presentationStatus());
        retry.store(2, 5678, 1);
        retry.present(FramePresentDevice.COMMIT);
        final ByteBuffer complete = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        assertTrue(retry.copyFrame(complete, true));
        assertEquals(5678, complete.getShort(0));
        assertEquals(5678, complete.getShort(2));
    }

    @Test
    void loadingLegacyStateClearsPreviouslyRetainedCompanionAddress() {
        final FramePresentAttachment reused = new FramePresentAttachment();
        assertTrue(reused.mount(context(), framebuffer()));
        reused.load(new CompoundTag());
        final VMContext limited = context();
        when(limited.getMemoryAllocator().claimMemory(anyInt())).thenReturn(false);
        assertTrue(reused.mount(limited, framebuffer()));
    }

    private static VMContext context() {
        final VMContext context = mock(VMContext.class);
        final MemoryAllocator allocator = mock(MemoryAllocator.class);
        final MemoryRangeAllocator ranges = mock(MemoryRangeAllocator.class);
        when(context.getMemoryAllocator()).thenReturn(allocator);
        when(allocator.claimMemory(anyInt())).thenReturn(true);
        when(context.getMemoryRangeAllocator()).thenReturn(ranges);
        when(ranges.claimMemoryRange(any())).thenReturn(OptionalLong.of(0x20000000));
        when(ranges.claimMemoryRange(anyLong(), any())).thenReturn(true);
        return context;
    }

    private static SimpleFramebufferDevice framebuffer() {
        return new SimpleFramebufferDevice(2, 1, ByteBuffer.allocate(4));
    }
}
