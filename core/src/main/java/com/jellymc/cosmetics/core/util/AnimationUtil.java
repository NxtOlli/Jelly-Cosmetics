package com.jellymc.cosmetics.core.util;

import com.jellymc.cosmetics.core.model.Animation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnimationUtil {
    private static final Pattern COLOR_PATTERN = Pattern.compile("§[0-9a-fk-or]");

    private AnimationUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a gradient animation between two colors
     *
     * @param startColor The starting color code (e.g. "§c")
     * @param endColor The ending color code (e.g. "§9")
     * @param steps The number of steps in the gradient
     * @param speed The speed of the animation in ticks per frame
     * @return The created animation
     */
    public static Animation createGradient(String startColor, String endColor, int steps, int speed) {
        if (steps < 2) {
            steps = 2;
        }

        String[] frames = new String[steps];
        frames[0] = startColor;
        frames[steps - 1] = endColor;

        // For simplicity, we're just alternating between the two colors
        // In a real implementation, you would interpolate between RGB values
        for (int i = 1; i < steps - 1; i++) {
            frames[i] = (i % 2 == 0) ? startColor : endColor;
        }

        return new Animation(frames, speed, true);
    }

    /**
     * Creates a rainbow animation
     *
     * @param speed The speed of the animation in ticks per frame
     * @return The created animation
     */
    public static Animation createRainbow(int speed) {
        String[] frames = {
                "§c", "§6", "§e", "§a", "§b", "§9", "§d"
        };

        return new Animation(frames, speed, true);
    }

    /**
     * Creates a blinking animation
     *
     * @param color The color to blink
     * @param speed The speed of the animation in ticks per frame
     * @return The created animation
     */
    public static Animation createBlinking(String color, int speed) {
        String[] frames = {
                color, "§f", color, "§f"
        };

        return new Animation(frames, speed, true);
    }

    /**
     * Creates a typing animation for a message
     *
     * @param message The message to animate
     * @param color The color of the message
     * @param speed The speed of the animation in ticks per frame
     * @return The created animation
     */
    public static Animation createTyping(String message, String color, int speed) {
        List<String> frames = new ArrayList<>();

        for (int i = 1; i <= message.length(); i++) {
            frames.add(color + message.substring(0, i));
        }

        // Add a few frames with the complete message to pause at the end
        for (int i = 0; i < 5; i++) {
            frames.add(color + message);
        }

        return new Animation(frames.toArray(new String[0]), speed, true);
    }

    /**
     * Applies an animation to a message
     *
     * @param message The message to animate
     * @param animation The animation to apply
     * @param tick The current tick
     * @return The animated message
     */
    public static String applyAnimation(String message, Animation animation, long tick) {
        String currentFrame = animation.getCurrentFrame(tick);

        // Replace all existing color codes with the current frame
        Matcher matcher = COLOR_PATTERN.matcher(message);
        return matcher.replaceAll(currentFrame);
    }

    /**
     * Applies a color code to a message
     *
     * @param message The message to color
     * @param colorCode The color code to apply
     * @return The colored message
     */
    public static String applyColor(String message, String colorCode) {
        // Replace all existing color codes with the new one
        Matcher matcher = COLOR_PATTERN.matcher(message);
        return matcher.replaceAll(colorCode);
    }

    /**
     * Strips all color codes from a message
     *
     * @param message The message to strip
     * @return The stripped message
     */
    public static String stripColor(String message) {
        return COLOR_PATTERN.matcher(message).replaceAll("");
    }
}
