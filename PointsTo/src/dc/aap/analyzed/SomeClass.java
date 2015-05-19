package dc.aap.analyzed;

public class SomeClass {
	public static void entryPoint() {
		Class1 x = new Class1();
		Class1 y = new Class1();
		//Class2 z = new Class2();
		//x.class2 = z;
		//x.campoLargo_y_rar0 = y;
		y = x.campoLargo_y_rar0;
		x.g = 5;
		//int z = x.g;
		//int t = x.m();
		//x.n();
	}
	
	public static class Class1 { 
		public Class1 campoLargo_y_rar0;
		public Class2 class2;
		public int g;
		public int m() {
			return 5;
		}
		public void n() {
			return;
		}
	}
	public static class Class2 {
		int rata;
	}
}
