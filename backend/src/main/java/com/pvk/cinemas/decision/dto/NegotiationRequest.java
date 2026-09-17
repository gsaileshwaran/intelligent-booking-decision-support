package com.pvk.cinemas.decision.dto;

public class NegotiationRequest {

    private int partySize = 2;
    private String targetRow;

    public NegotiationRequest() {}

    public NegotiationRequest(int partySize, String targetRow) {
        this.partySize = partySize;
        this.targetRow = targetRow;
    }

    public int getPartySize() { return partySize; }
    public void setPartySize(int partySize) { this.partySize = partySize; }

    public String getTargetRow() { return targetRow; }
    public void setTargetRow(String targetRow) { this.targetRow = targetRow; }
}
