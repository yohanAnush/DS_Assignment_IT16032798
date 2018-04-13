package sensor;

import java.text.DecimalFormat;
import java.util.concurrent.ThreadLocalRandom;

/*
 * This is sort of a helper class to generate random values to mimic the,
 * readings done by the sensor.
 */
public class Randoms {

	public static void main(String [] args) {
		Randoms r = new Randoms();
		
		r.getRandomDouble(20.0, 80.0);
		r.getRandomInt(1, 100);
	}
	
	/*
	 * This will generate a random double within the boundries we provide.
	 * 
	 * Java will generate a random double between 0 and 1 so,
	 * we have to adjust that number to fit our range.
	 * 
	 * By default we format the doubles to two decimal points.
	 */
	public double getRandomDouble(double lowerLimit, double upperLimit) {
		// we need to check if the upperLimit is actually greater than the lowerLimit,
		// otherwise the following calculation won't give a favourable output.
		if (upperLimit < lowerLimit) {
			// swap.
			double temp = upperLimit;
			upperLimit = lowerLimit;
			lowerLimit = temp;
		}
		
		double randomDouble = ThreadLocalRandom.current().nextDouble(lowerLimit, upperLimit + 1);
		
		return Double.parseDouble(new DecimalFormat("#.00").format(randomDouble));
		
		//return lowerLimit + (randomDouble * (upperLimit - lowerLimit));
		
		// if our range is 1 to 10, and imagine our randomDouble is 0.4,
		// so our result is going to be 1 + 0.4 * 9 = 4.6
	}
	
	public int getRandomInt(int lowerLimit, int upperLimit) {
		if (upperLimit < lowerLimit) {
			// swap.
			int temp = upperLimit;
			upperLimit = lowerLimit;
			lowerLimit = temp;
		}
		
		return ThreadLocalRandom.current().nextInt(lowerLimit, upperLimit + 1);

		//return lowerLimit + (randomInt * (upperLimit - lowerLimit + 1));
	}
}
