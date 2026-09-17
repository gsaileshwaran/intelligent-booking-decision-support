package com.pvk.cinemas.booking.dto;

import jakarta.validation.constraints.NotBlank;

public class CheckoutRequest {

    @NotBlank(message = "Hold token is required for checkout")
    private String holdToken;

    @NotBlank(message = "Payment method is required (DUMMY_CARD, DUMMY_UPI, TEST_WALLET)")
    private String paymentMethod;

    /**
     * Simulation outcome: "SUCCESS" or "FAILED". Default is "SUCCESS".
     */
    private String simulateOutcome = "SUCCESS";

    public CheckoutRequest() {}

    public CheckoutRequest(String holdToken, String paymentMethod, String simulateOutcome) {
        this.holdToken = holdToken;
        this.paymentMethod = paymentMethod;
        this.simulateOutcome = simulateOutcome != null ? simulateOutcome : "SUCCESS";
    }

    public String getHoldToken() { return holdToken; }
    public void setHoldToken(String holdToken) { this.holdToken = holdToken; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getSimulateOutcome() { return simulateOutcome; }
    public void setSimulateOutcome(String simulateOutcome) { this.simulateOutcome = simulateOutcome; }
}
