package com.jellymc.cosmetics.core.model;

public class Animation {
    private final String[] frames;
    private final int speed;
    private final boolean loop;

    /**
     * Creates a new animation
     *
     * @param frames The frames of the animation
     * @param speed The speed of the animation in ticks per frame
     * @param loop Whether the animation should loop
     */
    public Animation(String[] frames, int speed, boolean loop) {
        this.frames = frames;
        this.speed = speed;
        this.loop = loop;
    }

    /**
     * Gets the frames of the animation
     *
     * @return The animation frames
     */
    public String[] getFrames() {
        return frames;
    }

    /**
     * Gets the speed of the animation
     *
     * @return The animation speed in ticks per frame
     */
    public int getSpeed() {
        return speed;
    }

    /**
     * Checks if the animation should loop
     *
     * @return True if the animation should loop
     */
    public boolean isLoop() {
        return loop;
    }

    /**
     * Gets the current frame of the animation based on the current tick
     *
     * @param tick The current tick
     * @return The current frame
     */
    public String getCurrentFrame(long tick) {
        if (frames == null || frames.length == 0) {
            return "";
        }

        if (frames.length == 1) {
            return frames[0];
        }

        if (!loop && tick / speed >= frames.length) {
            return frames[frames.length - 1];
        }

        int frameIndex = (int) ((tick / speed) % frames.length);
        return frames[frameIndex];
    }

    /**
     * Gets the total duration of the animation in ticks
     *
     * @return The total duration
     */
    public int getTotalDuration() {
        return frames.length * speed;
    }
}
