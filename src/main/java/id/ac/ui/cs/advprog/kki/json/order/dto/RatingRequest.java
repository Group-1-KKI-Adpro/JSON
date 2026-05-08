package id.ac.ui.cs.advprog.kki.json.order.dto;

public class RatingRequest {

    private Integer jastiperRating;
    private Integer productRating;
    private String review;

    public Integer getJastiperRating() {
        return jastiperRating;
    }

    public void setJastiperRating(Integer jastiperRating) {
        this.jastiperRating = jastiperRating;
    }

    public Integer getProductRating() {
        return productRating;
    }

    public void setProductRating(Integer productRating) {
        this.productRating = productRating;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }
}