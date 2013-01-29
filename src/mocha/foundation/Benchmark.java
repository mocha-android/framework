/*
 *  @author Shaun
 *	@date 12/9/12
 *	@copyright	2012 TV Guide, Inc. All rights reserved.
 */
package mocha.foundation;

public class Benchmark extends Object {

	private long start;
	private long end;
	private long[] steps;
	private String[] stepsName;
	private int currentStep;

	public Benchmark() {
		this(25);
	}

	public Benchmark(int approxNumberOfSteps) {
		this.steps = new long[(int)(approxNumberOfSteps * 1.5)];
		this.stepsName = new String[(int)(approxNumberOfSteps * 1.5)];
	}

	public void start() {
		this.start = android.os.SystemClock.uptimeMillis();
		this.currentStep = 0;
	}

	public void step(String name) {
		this.steps[currentStep] = android.os.SystemClock.uptimeMillis();
		this.stepsName[currentStep] = name;
		this.currentStep++;
	}

	public void end() {
		this.end = android.os.SystemClock.uptimeMillis();
	}

	public void log() {
		long elapsed = end - start;
		long last = start;

		int longestName = 0;

		for(int step = 0; step < this.currentStep; step++) {
			String name = stepsName[step];
			if(name.length() > longestName) {
				longestName = name.length();
			}
		}

		longestName += 1;

		StringBuilder builder = new StringBuilder("-\n");
		builder.append("+===============================+\n");
		builder.append("| Benchmark Results:            |\n");
		builder.append("+-------------------------------+\n");


		for(int step = 0; step < this.currentStep; step++) {
			builder.append(String.format("| %1$" + longestName + "s: ", this.stepsName[step]));
			builder.append(steps[step] - last);
			builder.append(" | ");
			builder.append(String.format("%.03f", (double)(steps[step] - last) / (double)elapsed)).append("\n");
			last = steps[step];
		}

		builder.append("+-------------------------------+\n");
		builder.append("| Elapsed: ").append(elapsed).append("\n");
		builder.append("+===============================+");

		MLog(builder.toString());
	}

}
