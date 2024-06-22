package state;

public class Client {

    public static void main(String[] args) {
        Context left = new Context();
        
        left.setCurrent(Context.closingState);
        left.open();
        left.close();
        left.run();
        left.stop();
        
        System.out.println("\n------------");
        left.setCurrent(Context.runningState);
        left.open();
        left.close();
        left.run();
        left.stop();
    }

}
