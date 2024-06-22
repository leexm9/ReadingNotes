package adapter;

import java.util.Map;

public interface IOuterUser {
	//员工的基本信息，名字、性别、电话等
	public Map<String,String> getBaseInfo();
	//员工的工作信息
	public Map<String,String> getUserOfficeInfo();
	//员工的家庭信息
	public Map<String,String> getUserHomeInfo();
}
