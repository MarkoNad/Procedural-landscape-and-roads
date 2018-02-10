package toolbox;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.lwjgl.util.vector.Vector3f;

public class Globals {
	
	public static final Vector3f X_AXIS = new Vector3f(1f, 0f, 0f);
	public static final Vector3f Y_AXIS = new Vector3f(0f, 1f, 0f);
	public static final Vector3f Z_AXIS = new Vector3f(0f, 0f, 1f);
	
	private static ExecutorService threadPool;
	
	public static ExecutorService getThreadPool() {
		if(threadPool == null) {
			final int threads = Runtime.getRuntime().availableProcessors();
			
			threadPool = Executors.newFixedThreadPool(threads, new ThreadFactory() {	
				@Override
				public Thread newThread(Runnable r) {
					Thread thread = new Thread(r);
					thread.setDaemon(true);
					return thread;
				}
			});
		}

		return threadPool;
	}

}
