package bridge;

public abstract class Corp {
    
    //定义一个产品
    private Product product;
    
    public Corp(Product product) {
        this.product = product;
    }
    
    public void makeMoney() {
        //生产
        this.product.beProducted();
        //销售
        this.product.beSelled();
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
