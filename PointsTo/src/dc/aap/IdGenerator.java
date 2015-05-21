package dc.aap;

public class IdGenerator {
	static int counter = 0;
	public static String GenerateId() {
		counter++;
		return String.valueOf(counter);
	}
}
