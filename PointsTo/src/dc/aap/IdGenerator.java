package dc.aap;

public class IdGenerator {
	static int counter = 0;
	public static int GenerateId() {
		counter++;
		return counter;
	}
}
