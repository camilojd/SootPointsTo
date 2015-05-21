package dc.aap.analyzed;

public class SomeClass {
	public Class1 class1At;
	public Class2 class2At;

	public static void entryPoint() {
		SomeClass some = new SomeClass();
		Class1 c1 = new Class1();
		Class2 c2 = new Class2();
		Class2 c22 = new Class2();
		some.method(c1, c2);
		some.class1At.class2At = c22;
		some.class1At.class2At.intAt = 3;
		some.class1At.intAt = 7;
	}
	
	public void method(Class1 cl1, Class2 cl2) {
		class1At = cl1;
		class2At = cl2;
	}
	
	public static class Class1 {
		public Class2 class2At;
		public int intAt;
	}
	
	public static class Class2 {
		public int intAt;
	}	
}
