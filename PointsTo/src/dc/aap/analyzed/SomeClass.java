package dc.aap.analyzed;

public class SomeClass {
	public static void entryPoint(int formalPar1, Class2 formalPar2) {
		int lala = formalPar1;
		Class1 x = new Class1();
		Class1 y = new Class1();
		Class2 z = new Class2();
		x.class2 = z;
		x.campoLargo_y_rar0 = y;
		y = x.campoLargo_y_rar0;
		x.g = lala;
		int a = x.g;
		//int t = x.m();
		//Class2 lala = y.class2;
		//lala = x.class2;
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
