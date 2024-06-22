package adapter;

public class UserInfo implements IUserInfo {

	@Override
	public String getUserName() {
		System.out.println("员工的姓名....");
		return null;
	}

	@Override
	public String getHomeAddress() {
		System.out.println("员工的家庭地址....");
		return null;
	}

	@Override
	public String getMobileNumber() {
		System.out.println("员工的手机号码...");
		return null;
	}

	@Override
	public String getOfficeTelNumber() {
		System.out.println("员工的办公号码....");
		return null;
	}

	@Override
	public String getJobPosition() {
		System.out.println("员工的职位信息....");
		return null;
	}

	@Override
	public String getHomeTelNumber() {
		System.out.println("员工的家庭号码....");
		return null;
	}

}
