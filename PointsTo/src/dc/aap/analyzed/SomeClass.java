package dc.aap.analyzed;

public class SomeClass {
	public static void entryPoint() {
		Class1 x = new Class1();
//		x.f = new Class1();
//		Class1 a = new Class1();
//		a.f = x.f;
	}
	
	public static class Class1 { 
		public Class1 f;
	}
}
