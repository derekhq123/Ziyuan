package testdata.benchmark;

public class GCD {

	public int gcdTest(int f, int s)
    {
		if (s == 0) {
			return f;
		}
		return gcdTest(s, f % s);
    }
}
