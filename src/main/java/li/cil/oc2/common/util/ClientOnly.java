package li.cil.oc2.common.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks client-only classes and members on Minecraft 26.x, where NeoForge no longer strips members
 * by {@code @OnlyIn}. The 26.x source replacement swaps {@code @OnlyIn(Dist.CLIENT)} for this.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface ClientOnly {}
