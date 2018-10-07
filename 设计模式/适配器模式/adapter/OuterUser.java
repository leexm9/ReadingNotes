package adapter;

import java.util.HashMap;
import java.util.Map;

public class OuterUser implements IOuterUser {

	@Override
	public Map<String, String> getBaseInfo() {
		Map<String,String> map = new HashMap<String,String>();
		map.put("userName", "Tom");
		map.put("mobileNumber", "员工电话是...");
		return map;
	}

	@Override
	public Map<String, String> getUserOfficeInfo() {
		Map<String,String> map = new HashMap<String,String>();
		map.put("jobPosition", "员工的职位是...");
		map.put("officeTelNumber", "员工的办公电话是...");
		return map;
	}

	@Override
	public Map<String, String> getUserHomeInfo() {
		Map<String,String> map = new HashMap<String,String>();
		map.put("homeTelNumber", "员工的家庭电话是...");
		map.put("homeAddress", "员工的家庭地址是...");
		return map;
	}

}
