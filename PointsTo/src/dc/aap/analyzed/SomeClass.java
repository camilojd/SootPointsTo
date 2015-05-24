package dc.aap.analyzed;

public class SomeClass {
	public Class1 class1At;
	public Class2 class2At;
	public int pepe;

	public static void entryPoint() {
		SomeClass some = new SomeClass();
		Class1 c1 = new Class1();
		Class2 c2 = new Class2();
		some.class2At = c2;
		Class2 c22 = new Class2();
		c22 = some.crazyMethod(c1);
	}
	
	public SomeClass(){
		Class1 c1 = new Class1();
		class1At = c1;
		pepe = 0;
	}
	
	public Class2 crazyMethod(Class1 cl1) {
		class1At = cl1;
		return class2At;
	}
	
	public static class Class1 {
		public Class1() {
			intAt = 1;
		}
		public Class2 class2At;
		public int intAt;
	}
	
	public static class Class2 {
		public int intAt;
	}	
}
