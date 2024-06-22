package composite;

import java.util.List;

public class Client {

	public static void main(String[] args) {
		Branch ceo = compositeCorpTree();
		//输出ceo信息
		System.out.println(ceo.getInfo());
		//输出所有员工信息
		System.out.println(getTreeInfo(ceo, 0));
	}
	
	private static Branch compositeCorpTree() {
		//总经理
		Branch root = new Branch("ceo", "总经理", 123213);
		//部门经理
		Branch develop = new Branch("Tom", "研发部经理", 10000);
		Branch sale = new Branch("Jack", "销售部经理", 10000);
		Branch finance = new Branch("Halen", "财务部经理", 10000);
		Leaf g = new Leaf("G", "秘书", 4000);
		//组长
		Branch z1 = new Branch("Z1", "开发一组组长", 5000);
		Branch z2 = new Branch("Z2", "开发二组组长", 5000);
		Leaf z3 = new Leaf("Z3", "开发三组组长", 4500);
		//员工
		Leaf a = new Leaf("A", "开发人员", 4000);
		Leaf b = new Leaf("B", "开发人员", 4000);
		Leaf c = new Leaf("C", "开发人员", 4000);
		Leaf d = new Leaf("D", "开发人员", 4000);
		Leaf e = new Leaf("E", "开发人员", 4000);
		Leaf f = new Leaf("F", "开发人员", 4000);
		Leaf h = new Leaf("H", "销售人员", 4000);
		Leaf i = new Leaf("I", "销售人员", 4000);
		Leaf j = new Leaf("J", "财务人员", 4000);
		
		//组装树结构
		root.addSub(develop);
		root.addSub(sale);
		root.addSub(finance);
		root.addSub(g);
		
		develop.addSub(z1);
		develop.addSub(z2);
		develop.addSub(z3);
		
		sale.addSub(h);
		sale.addSub(i);
		
		finance.addSub(j);
		
		z1.addSub(a);
		z1.addSub(b);
		z1.addSub(c);
		
		z2.addSub(d);
		z2.addSub(e);
		z2.addSub(f);
		return root;
	}
	
	private static String getTreeInfo(Branch root, int space) {
		List<Corp> list = root.getSub();
		String info = "";
		//展示层级结构
		String str = "";
		for(int i = 0; i < space; i++) {
			str += "\t";
		}
		for(Corp c : list) {
			if(c instanceof Leaf) {
				info += str + c.getInfo() + "\n";
			} else {
				info += str + c.getInfo() + "\n" + getTreeInfo((Branch)c, space + 1);
			}
		}
		return info;
	}

}
