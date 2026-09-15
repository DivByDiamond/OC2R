package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.Util;

public class GameTestTicker {
    public static final GameTestTicker SINGLETON = new GameTestTicker();
    private final Collection<GameTestInfo> testInfos = Lists.newCopyOnWriteArrayList();
    @Nullable
    private GameTestRunner runner;

    private GameTestTicker() {
    }

    public void add(GameTestInfo testInfo) {
        this.testInfos.add(testInfo);
    }

    public void clear() {
        this.testInfos.clear();
        if (this.runner != null) {
            this.runner.stop();
            this.runner = null;
        }
    }

    public void setRunner(GameTestRunner runner) {
        if (this.runner != null) {
            Util.logAndPauseIfInIde("The runner was already set in GameTestTicker");
        }

        this.runner = runner;
    }

    public void tick() {
        if (this.runner != null) {
            this.testInfos.forEach(p_319813_ -> p_319813_.tick(this.runner));
            this.testInfos.removeIf(GameTestInfo::isDone);
        }
    }
}
