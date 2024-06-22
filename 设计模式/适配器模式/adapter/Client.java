package adapter;

public class Client {

	public static void main(String[] args) {
		IUserInfo userInfo = new OuterUserInfo();
		System.out.println(userInfo.getUserName());
		System.out.println(userInfo.getJobPosition());
	}

}
