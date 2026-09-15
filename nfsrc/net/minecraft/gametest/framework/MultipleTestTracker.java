package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MultipleTestTracker {
    private static final char NOT_STARTED_TEST_CHAR = ' ';
    private static final char ONGOING_TEST_CHAR = '_';
    private static final char SUCCESSFUL_TEST_CHAR = '+';
    private static final char FAILED_OPTIONAL_TEST_CHAR = 'x';
    private static final char FAILED_REQUIRED_TEST_CHAR = 'X';
    private final Collection<GameTestInfo> tests = Lists.newArrayList();
    private final Collection<GameTestListener> listeners = Lists.newArrayList();

    public MultipleTestTracker() {
    }

    public MultipleTestTracker(Collection<GameTestInfo> testInfos) {
        this.tests.addAll(testInfos);
    }

    public void addTestToTrack(GameTestInfo testInfo) {
        this.tests.add(testInfo);
        this.listeners.forEach(testInfo::addListener);
    }

    public void addListener(GameTestListener testListener) {
        this.listeners.add(testListener);
        this.tests.forEach(p_127815_ -> p_127815_.addListener(testListener));
    }

    public void addFailureListener(final Consumer<GameTestInfo> onFail) {
        this.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo p_127830_) {
            }

            @Override
            public void testPassed(GameTestInfo p_177685_, GameTestRunner p_319909_) {
            }

            @Override
            public void testFailed(GameTestInfo p_127832_, GameTestRunner p_319772_) {
                onFail.accept(p_127832_);
            }

            @Override
            public void testAddedForRerun(GameTestInfo p_320337_, GameTestInfo p_319975_, GameTestRunner p_320688_) {
            }
        });
    }

    public int getFailedRequiredCount() {
        return (int)this.tests.stream().filter(GameTestInfo::hasFailed).filter(GameTestInfo::isRequired).count();
    }

    public int getFailedOptionalCount() {
        return (int)this.tests.stream().filter(GameTestInfo::hasFailed).filter(GameTestInfo::isOptional).count();
    }

    public int getDoneCount() {
        return (int)this.tests.stream().filter(GameTestInfo::isDone).count();
    }

    public boolean hasFailedRequired() {
        return this.getFailedRequiredCount() > 0;
    }

    public boolean hasFailedOptional() {
        return this.getFailedOptionalCount() > 0;
    }

    public Collection<GameTestInfo> getFailedRequired() {
        return this.tests.stream().filter(GameTestInfo::hasFailed).filter(GameTestInfo::isRequired).collect(Collectors.toList());
    }

    public Collection<GameTestInfo> getFailedOptional() {
        return this.tests.stream().filter(GameTestInfo::hasFailed).filter(GameTestInfo::isOptional).collect(Collectors.toList());
    }

    public int getTotalCount() {
        return this.tests.size();
    }

    public boolean isDone() {
        return this.getDoneCount() == this.getTotalCount();
    }

    public String getProgressBar() {
        StringBuffer stringbuffer = new StringBuffer();
        stringbuffer.append('[');
        this.tests.forEach(p_127806_ -> {
            if (!p_127806_.hasStarted()) {
                stringbuffer.append(' ');
            } else if (p_127806_.hasSucceeded()) {
                stringbuffer.append('+');
            } else if (p_127806_.hasFailed()) {
                stringbuffer.append((char)(p_127806_.isRequired() ? 'X' : 'x'));
            } else {
                stringbuffer.append('_');
            }
        });
        stringbuffer.append(']');
        return stringbuffer.toString();
    }

    @Override
    public String toString() {
        return this.getProgressBar();
    }

    public void remove(GameTestInfo gameTestInfo) {
        this.tests.remove(gameTestInfo);
    }
}
