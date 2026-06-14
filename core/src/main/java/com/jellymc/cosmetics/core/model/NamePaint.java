package com.jellymc.cosmetics.core.model;

public class NamePaint {
    private final String id;
    private final String name;
    private final String format;
    private final String permission;
    private final boolean animated;
    private final String[] animationFrames;
    private final int animationSpeed;

    /**
     * Creates a new static name paint
     *
     * @param id The unique identifier
     * @param name The display name
     * @param format The format string (e.g. "§6{name}")
     * @param permission The permission required to use this name paint
     */
    public NamePaint(String id, String name, String format, String permission) {
        this(id, name, format, permission, false, null, 0);
    }

    /**
     * Creates a new name paint, which can be animated
     *
     * @param id The unique identifier
     * @param name The display name
     * @param format The format string (e.g. "§6{name}")
     * @param permission The permission required to use this name paint
     * @param animated Whether this name paint is animated
     * @param animationFrames The frames of the animation
     * @param animationSpeed The speed of the animation in ticks per frame
     */
    public NamePaint(String id, String name, String format, String permission,
                     boolean animated, String[] animationFrames, int animationSpeed) {
        this.id = id;
        this.name = name;
        this.format = format;
        this.permission = permission;
        this.animated = animated;
        this.animationFrames = animationFrames;
        this.animationSpeed = animationSpeed;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFormat() {
        return format;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isAnimated() {
        return animated;
    }

    public String[] getAnimationFrames() {
        return animationFrames;
    }

    public int getAnimationSpeed() {
        return animationSpeed;
    }

    /**
     * Gets the current frame of the animation based on the current tick
     *
     * @param tick The current tick
     * @return The current format string
     */
    public String getCurrentFrame(long tick) {
        if (!animated || animationFrames == null || animationFrames.length == 0) {
            return format;
        }

        int frameIndex = (int) ((tick / animationSpeed) % animationFrames.length);
        return animationFrames[frameIndex];
    }

    /**
     * Formats a player name using this name paint
     *
     * @param playerName The player name to format
     * @return The formatted name
     */
    public String formatName(String playerName) {
        return format.replace("{name}", playerName);
    }

    /**
     * Formats a player name using the current animation frame
     *
     * @param playerName The player name to format
     * @param tick The current tick
     * @return The formatted name
     */
    public String formatNameWithFrame(String playerName, long tick) {
        String currentFrame = getCurrentFrame(tick);
        return currentFrame.replace("{name}", playerName);
    }
}
