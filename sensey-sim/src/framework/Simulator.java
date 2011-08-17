package framework;

import model.Controller;
import model.House;
import model.Time;
import model.Weather;

/**
 * runs the simulation. 
 * */
public class Simulator {
	House house = new House();
	Weather weather = new Weather();
	Controller controller = new Controller();

	@SuppressWarnings("unused")
	public void run() {
		for (int i = 0; i < 10; ++i) {
			Time t = new Time();
			double outsideTemp = weather.temperature(t);
		}

	}

}
