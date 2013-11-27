/*
 *  Assignment Name: AP3 EXERCISE 2
 *  This is the work of Ong Ming Thye, Derrick
 *  StudentID : 13AGC039H (SIT) / 2110010O (GUID)
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class includeCrawler {

	// the required global variable
	static ArrayList<String> dir = new ArrayList<String>();
	static ConcurrentLinkedQueue<String> workQ = new ConcurrentLinkedQueue<String>();
	static ConcurrentHashMap<String, ArrayList<String>> ht = new ConcurrentHashMap<String, ArrayList<String>>();

	// i ran a class in the include Crawler file so that i can make use of the
	// same global variable.
	public static class WorkerThread implements Runnable {

		// the required variable
		private String name;
		private ConcurrentLinkedQueue<String> forMove = new ConcurrentLinkedQueue<String>();
		private ArrayList<String> headerFile = new ArrayList<String>();

		public void run() {
			// do the recurrsive method.
			while (forMove.peek() != null) {
				grabFile(forMove.poll());
			}

			// once done stored it into the hashmap.
			ht.put(name, headerFile);
		}

		// this method will go thru the file, if it finds something that start
		// with #include and it has "" right after, it will grab the file, put
		// into two places.
		// 1. headerFile -> this will be stored and printed out
		// 2. forMove -> this will put into the queue and wait for it's true to
		// go through that file to find it dependency.
		public void grabFile(String name) {
			BufferedReader br = null;
			String sCurrentLine;

			try {
				if (checkFile(name) != null) {

					br = new BufferedReader(new FileReader(checkFile(name)));

					while ((sCurrentLine = br.readLine()) != null) {

						if (sCurrentLine.trim().startsWith("#include")) {
							if (sCurrentLine.contains("\"")) {
								String[] temp = sCurrentLine.trim().split("\"");

								if (!headerFile.contains(temp[1])) {
									forMove.add(temp[1]);
									headerFile.add(temp[1]);

								}
							}
						}

					}

					br.close();

				}

			} catch (IOException e) {
				defaultMsg();

			}

		}

		// this is the constructor for the thread
		public WorkerThread(String name) {
			this.name = name;
			forMove.add(name);
		}
	}

	public static void main(String[] args) throws InterruptedException {

		long start = System.currentTimeMillis();
		int CRAWLER_THREADS = 2;
		// add current folder to the directory list
		dir.add(".");

		// get system variable for the Crawler_threads. if there is a value,
		// check if there is number. if it is not a number, it will exit.
		if (System.getenv("CRAWLER_THREADS") != null) {
			if (isInteger(System.getenv("CRAWLER_THREADS"))) {
				CRAWLER_THREADS = Integer.parseInt(System.getenv("CRAWLER_THREADS"));
				
				//System.out.println(System.getenv("CRAWLER_THREADS"));
			}
		}

		// specified folder, the -I will specified the folder that it will look
		// for the file, this will add the folder behind the -I
		for (String a : args) {
			if (a.contains("-I")) {
				dir.add(a.substring(2, a.length()));
			}
		}

		// getting the CPATH in the system variable. Once it get it, it will
		// search through and find the file.
		if (System.getenv("CPATH") != null) {
			String[] cpath = System.getenv("CPATH").split(":");
			// this will check what are the path that is specified inside the
			// CPATH variable
			for (String a : cpath) {
				dir.add(a);
			}
		}

		// We will need a holding list for the hashmap. This will hold all the
		// variable os that it will be available for printing in the future.
		ArrayList<String> forStorage = new ArrayList<String>();

		// This loop will file into the work queue and also the arraylist for
		// printing in the future. If one of the file cannot be found, it will
		// exit the program.
		for (String a : args) {
			if (!a.contains("-I")) {
				if (checkExt(a)) {
					if (checkFile(a) != null) {
						workQ.add(a);
						forStorage.add(a);
					} else {
						System.err.println("Error opening " + a);
						return;
					}
				} else {
					defaultMsg(); // only if the file provided is not .c, .y or
									// .l, it will prompt the user that the
									// wrong file is given.
				}
			}
		}

		// this is for threadpool. It will initiate the amount of thread that is
		// specified by the user.
		ExecutorService executor = Executors
				.newFixedThreadPool(CRAWLER_THREADS);

		// this loop will start the thread using all the workers that is in the
		// queue.
		while (workQ.peek() != null) {
			// this create the worker by placing whatever that is stored in the
			// workQ and create a worker which will push to the executor to
			// start.
			Runnable worker = new WorkerThread(workQ.poll());
			executor.execute(worker);
		}

		executor.shutdown(); // once the thread had finish, it will terminate
								// the execurtor.

		// this is to make sure that all the executor is terminated before
		// moving on.
		while (!executor.isTerminated()) {
		}

		// this nested for loop is to print out the item. We have already stored
		// all the item that we are supposed to find dependency on, we rely on
		// that list to give us the key for the hashmap.
		for (String pump : forStorage) {
			// by having the key to the hashmap, the arraylist is being
			// extracted and is able to print from the following for loop.
			ArrayList<String> temp = ht.get(pump);
			System.out.print(pump.substring(0, pump.length() - 2) + ".o: "
					+ pump);

			for (String k : temp) {
				System.out.print(" " + k);
			}
			System.out.println();
		}
		
		long end = System.currentTimeMillis();
		System.out.println("\nElapsed time: " + (double) (end - start)
				/ 1000 + " seconds");
	}

	// check extension of the file.
	public static boolean checkExt(String file) {
		char ext = file.charAt(file.length() - 1);
		if (ext == 'c' || ext == 'y' || ext == 'l') {
			return true;
		} else {
			return false;
		}
	}

	// print the default message.
	public static void defaultMsg() {
		System.err.println("Illegal extension: - must be .c, .y or .l");
	}

	// check if the string pass in is integer.
	public static boolean isInteger(String str) {
		int size = str.length();

		for (int i = 0; i < size; i++) {
			if (!Character.isDigit(str.charAt(i))) {
				return false;
			}
		}

		return size > 0;
	}

	// check if the file exist, if it doesnt exist, it will return a null, if it
	// exist in any of the directory, it will return the file.
	public static File checkFile(String file) {
		for (String path : dir) {
			File f = new File(path + "/" + file);

			if (f.exists()) {
				return f;
			}
		}

		return null;
	}
}