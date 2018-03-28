package hr.fer.zemris.engine.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.lwjgl.util.vector.Vector3f;

public class Globals {
	
	public static final Vector3f X_AXIS = new Vector3f(1f, 0f, 0f);
	public static final Vector3f Y_AXIS = new Vector3f(0f, 1f, 0f);
	public static final Vector3f Z_AXIS = new Vector3f(0f, 0f, 1f);
	
	public static final String RESOURCES_ROOT = "resources/";
	
	private static ExecutorService threadPool;
	public static final int DEFAULT_BACKGROUND_THREADS = 1;
	
	public static void initializeThreadPool(int threads) {
		threadPool = Executors.newFixedThreadPool(threads, new ThreadFactory() {	
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setDaemon(true);
				thread.setPriority(Thread.MIN_PRIORITY);
				return thread;
			}
		});
	}
	
	public static ExecutorService getThreadPool() {
		if(threadPool == null) {
			initializeThreadPool(DEFAULT_BACKGROUND_THREADS);
		}

		return threadPool;
	}

}
