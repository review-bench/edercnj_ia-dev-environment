package dev.iadev.application.ideation;

public record IdeationPromotionResult(boolean success, String resolvedId, String failureReason) {

    public static IdeationPromotionResult success(String resolvedId) {
        return new IdeationPromotionResult(true, resolvedId, null);
    }

    public static IdeationPromotionResult failed(String reason) {
        return new IdeationPromotionResult(false, null, reason);
    }
}
