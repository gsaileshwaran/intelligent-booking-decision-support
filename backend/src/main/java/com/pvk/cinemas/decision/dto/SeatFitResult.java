package com.pvk.cinemas.decision.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SeatFitResult {

    private List<Long> seatIds = new ArrayList<>();
    private List<String> seatLabels = new ArrayList<>();
    private int groupScore = 50;
    private int maxContiguousBlock = 0;
    private String bestRow = null;
    private String splitDescription;
    private String seatingTradeoff;
    private String rationale;
    private boolean allTogether = false;
    private boolean adjacentRows = false;
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private List<RecommendedBlock> candidateBlocks = new ArrayList<>();

    public SeatFitResult() {}

    public List<Long> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Long> seatIds) { this.seatIds = seatIds; }

    public List<String> getSeatLabels() { return seatLabels; }
    public void setSeatLabels(List<String> seatLabels) { this.seatLabels = seatLabels; }

    public int getGroupScore() { return groupScore; }
    public void setGroupScore(int groupScore) { this.groupScore = groupScore; }

    public int getMaxContiguousBlock() { return maxContiguousBlock; }
    public void setMaxContiguousBlock(int maxContiguousBlock) { this.maxContiguousBlock = maxContiguousBlock; }

    public String getBestRow() { return bestRow; }
    public void setBestRow(String bestRow) { this.bestRow = bestRow; }

    public String getSplitDescription() { return splitDescription; }
    public void setSplitDescription(String splitDescription) { this.splitDescription = splitDescription; }

    public String getSeatingTradeoff() { return seatingTradeoff; }
    public void setSeatingTradeoff(String seatingTradeoff) { this.seatingTradeoff = seatingTradeoff; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public boolean isAllTogether() { return allTogether; }
    public void setAllTogether(boolean allTogether) { this.allTogether = allTogether; }

    public boolean isAdjacentRows() { return adjacentRows; }
    public void setAdjacentRows(boolean adjacentRows) { this.adjacentRows = adjacentRows; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public List<RecommendedBlock> getCandidateBlocks() { return candidateBlocks; }
    public void setCandidateBlocks(List<RecommendedBlock> candidateBlocks) { this.candidateBlocks = candidateBlocks; }
}
