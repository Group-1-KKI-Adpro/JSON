package id.ac.ui.cs.advprog.kki.json.auth.dto;

public class UpdateReputationRequest {

    private Boolean transactionCompleted;
    private Boolean transactionSuccessful;
    private Integer jastiperRating;

    public UpdateReputationRequest() {}

    public UpdateReputationRequest(Boolean transactionCompleted, Boolean transactionSuccessful, Integer jastiperRating) {
        this.transactionCompleted = transactionCompleted;
        this.transactionSuccessful = transactionSuccessful;
        this.jastiperRating = jastiperRating;
    }

    public Boolean getTransactionCompleted() {
        return transactionCompleted;
    }

    public void setTransactionCompleted(Boolean transactionCompleted) {
        this.transactionCompleted = transactionCompleted;
    }

    public Boolean getTransactionSuccessful() {
        return transactionSuccessful;
    }

    public void setTransactionSuccessful(Boolean transactionSuccessful) {
        this.transactionSuccessful = transactionSuccessful;
    }

    public Integer getJastiperRating() {
        return jastiperRating;
    }

    public void setJastiperRating(Integer jastiperRating) {
        this.jastiperRating = jastiperRating;
    }
}