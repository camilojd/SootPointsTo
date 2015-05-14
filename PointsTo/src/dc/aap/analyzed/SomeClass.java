package dc.aap.analyzed;

public class SomeClass {
	public static void entryPoint() {
		Class1 x = new Class1();
		Class1 y = new Class1();
		x = y;
		x.f = y;
		y = x.f;
		x.g = 5;
		int z = x.g;
		int t = x.m();
		x.n();
	}
	
	public static class Class1 { 
		public Class1 f;
		public int g;
		public int m() {
			return 5;
		}
		public void n() {
			return;
		}
	}
}
