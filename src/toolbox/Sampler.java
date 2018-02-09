package toolbox;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public abstract class Sampler {
	
	protected final Point2D.Float p0;
	protected final Point2D.Float p1;

	public Sampler(Float p0, Float p1) {
		this.p0 = p0;
		this.p1 = p1;
	}

	public abstract List<Point2D.Float> sample();
	public abstract void sample(int batchSize, BlockingQueue<List<Point2D.Float>> batchQueue)
			throws InterruptedException;
	public abstract boolean samplingDone();
	
}
