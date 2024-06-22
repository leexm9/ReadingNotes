package bridge;

public class Client {

    public static void main(String[] args) {
        System.out.println("------房地产公司这样运作------");
        Product house = new House();
        Corp houseCorp = new HouseCorp(house);
        houseCorp.makeMoney();
        System.out.println();
        
        System.out.println("------代工厂这样运作------");
        Product iphone = new IPhone();
        Corp OEMCorp = new OEMCorp(iphone);
        OEMCorp.makeMoney();
        System.out.println();
        
        System.out.println("------代工厂改换代工产品了------");
        Product xiaoMi = new XiaoMI();
        OEMCorp.setProduct(xiaoMi);
        OEMCorp.makeMoney();
    }

}
