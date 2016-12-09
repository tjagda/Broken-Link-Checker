package blc;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

class MainBLCRunnable implements Runnable {
	private Thread t;

	private String foldername;
	private String currentPage;
	private int pageLimit;
	private char browserChoice;
	private List<Link> linkList;
	private List<String> urlList;
	
	private HashMap<String, Integer> responseCodeMap = new HashMap<>();
	private HashMap<String, String> titleMap = new HashMap<>();

	private JTextPane textPaneMessages;
	private SimpleAttributeSet error;
	private JButton btnStop;
	private JButton btnScan;
	private volatile JProgressBar progressBar;

	public MainBLCRunnable(String foldername, int pageLimit, String currentPage, char browserChoice, JTextPane textPaneMessages, JButton btnScan, JButton btnStop, JProgressBar progressBar) {
		// Initializing values from parameters
		this.foldername = foldername.replace("\\", "\\\\") + "\\";
		this.currentPage = currentPage;
		this.pageLimit = pageLimit;
		this.browserChoice = browserChoice;
		this.linkList = new ArrayList<Link>();

		this.textPaneMessages = textPaneMessages;
		this.btnScan = btnScan;
		this.btnStop = btnStop;
		this.progressBar = progressBar;

		// Initializing values not from parameters
		error = new SimpleAttributeSet();
		StyleConstants.setForeground(error, Color.RED);

		// Set page limit to an extremely high number to "simulate" no page limit.
		// Program will run out of memory before reaching this limit.
		if (this.pageLimit == 0)
			this.pageLimit = 1000000000;

		// Add "http" if the url given doesn't have it
		if (!this.currentPage.contains("http"))
			this.currentPage = "http://" + this.currentPage;

		// Add a final slash (/) if url given doesn't have it
		if (this.currentPage.charAt(this.currentPage.length() - 1) != '/')
			this.currentPage += "/";
		
		// Initialize hashmaps
		responseCodeMap = new HashMap<>();
		titleMap = new HashMap<>();

		// Create new folder based on given url
		File newFolder;
		this.foldername += this.currentPage.substring(this.currentPage.indexOf("://")+3).replace("/", " ").trim();
		newFolder = new File(this.foldername);
		newFolder.mkdir();
	}

	private void initiateLinkScrapeThreads() {
		int numOfScrapeThreads;
		
		// Determine how many threads to use for link scraping
		if (pageLimit < 100) {
			numOfScrapeThreads = 10;
		} else if (pageLimit < 500) {
			numOfScrapeThreads = 50;
		} else {
			numOfScrapeThreads = 100;
		}
		
		// Initializing countdown latches
		CountDownLatch startSignal = new CountDownLatch(1);
		CountDownLatch endSignal = new CountDownLatch(numOfScrapeThreads);

		// Starts page by adding the input page
		HelperFunctions.appendToTextPane("Creating Threads...\n", textPaneMessages, null);
		LinkScrapeRunnable[] test = new LinkScrapeRunnable[numOfScrapeThreads];
		for (int i = 0; i < numOfScrapeThreads; i++) {
			if (i == 0) {
				test[i] = new LinkScrapeRunnable(currentPage, true, pageLimit, startSignal, endSignal, textPaneMessages, btnStop, progressBar);
			} else {
				test[i] = new LinkScrapeRunnable(currentPage, false, pageLimit, startSignal, endSignal, textPaneMessages, btnStop, progressBar);
			}
			test[i].start();
		}

		// Setup status if user is not stopping the scan
		if (btnStop.isEnabled()) {
			// Starting all link scrape runnables
			progressBar.setIndeterminate(true);
			HelperFunctions.appendToTextPane("\nInitiating Link Scan...\n", textPaneMessages, null);
		}

		try {
			// Run all threads (even when the stop button has been clicked)
			startSignal.countDown();
			endSignal.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void readLinkScrapeResults() {
		// Reading from file and inserting it into the temp file
		try (BufferedReader br = new BufferedReader(new FileReader(new File("temp.txt")))) {
			String line;
			String origin;
			String url;
			String text;
			
			// Initialize list as an Arraylist
			urlList = new ArrayList<>();
			
			// Read every line of the temp file and parse the values to give to the the link list and url list
			while ((line = br.readLine()) != null) {
				origin = "";
				url = "";
				text = "";
				
				// If the text field is not empty, then pick them all up. Otherwise just pick up the origin and url values
				if (line.split("\t").length > 2) {
					origin = line.split("\t")[0];
					url = line.split("\t")[1];
					text = line.split("\t")[2];
				} else {
					origin = line.split("\t")[0];
					url = line.split("\t")[1];
				}
				
				// Add to linkList
				linkList.add(new Link(origin, url, text));
				urlList.add(url);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initiateResponseCodeThreads(List<String> noDuplList) {
		// Create temp1 file
		FileWriter responseCode = null;
		try {
			responseCode = new FileWriter(new File ("temp1.txt"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// Split list of links into multiple smaller lists
		List<List<String>> responseParts;
		if (browserChoice == '0') {
			if (noDuplList.size() < 50) {
				responseParts = HelperFunctions.chopped(noDuplList, 5);
			} else if (noDuplList.size() < 100) {
				responseParts = HelperFunctions.chopped(noDuplList, 20);
			} else {
				responseParts = HelperFunctions.chopped(noDuplList, (int) Math.abs(Math.ceil(noDuplList.size() / 100)));
			}
		} else {
			if (noDuplList.size() < 5) {
				responseParts = HelperFunctions.chopped(noDuplList, 1);
			} else {
				responseParts = HelperFunctions.chopped(noDuplList, (int) Math.ceil(noDuplList.size() / 5));
			}
		}

		// Create countdown latches
		CountDownLatch startSignal = new CountDownLatch(1);
		CountDownLatch endSignal = new CountDownLatch(responseParts.size());

		// Creating lists of links that were split for threads to use
		ResponseCodeRunnable[] linkScanner = new ResponseCodeRunnable[responseParts.size()];
		for (int i = 0; i < responseParts.size(); i++) {
			linkScanner[i] = new ResponseCodeRunnable(foldername, responseParts.get(i), responseCode, browserChoice, noDuplList.size(), textPaneMessages, btnStop, progressBar, startSignal, endSignal);
			linkScanner[i].start();
		}

		// Start all threads using the countdown latches and wait for all of them to end
		try {
			startSignal.countDown();
			endSignal.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Close and save file after all threads finish
		try {
			responseCode.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	private void readResponseCodeResults() {
		// Deleting temp file
		File toDelete = new File("temp.txt");
		if (toDelete.exists()) {
			toDelete.delete();
		}
		
		// Reading from file and inserting it into the temp file
		try (BufferedReader br = new BufferedReader(new FileReader(new File("temp1.txt")))) {
			int code;
			String url;
			String title;
			String line;
			
			// While there is still stuff to read, take in the variables and put them into the hashmaps
			while ((line = br.readLine()) != null) {
				url = "";
				title = "";
				code = 404;
				
				url = line.split("\t")[0];
				title = line.split("\t")[1];
				code = Integer.parseInt(line.split("\t")[2]);
				
				responseCodeMap.put(url, code);
				titleMap.put(url, title);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		long startTime;
		long endTime;
		long duration;
		
		// Timing the beginning of scan
		startTime = System.nanoTime();

		// Initialize variables in threads and set progress bar to 0
		LinkScrapeRunnable.resetVariables();
		ResponseCodeRunnable.resetVariables();
		progressBar.setValue(0);
		progressBar.setString("Please Wait...");

		// Clears all the text pane messages
		if (textPaneMessages.getText().length() > 0) {
			textPaneMessages.setText("");
		}

		// Disable SSL Verification
		HelperFunctions.disableSslVerification();
		initiateLinkScrapeThreads();

		// Only proceed if scan is not stopped
		if (btnStop.isEnabled()) {
			
			// Read results from temp file
			readLinkScrapeResults();

			// Remove duplicates of urlList by creating a new list
			List<String> noDuplList = HelperFunctions.noDuplVersion(urlList);
			
			// Response code scan
			progressBar.setIndeterminate(false);
			HelperFunctions.appendToTextPane("\n---------------\nInitiating response code scan... (" + noDuplList.size() + " unique links)\n\n", textPaneMessages, null);
			initiateResponseCodeThreads(noDuplList);
			
			if (btnStop.isEnabled()) {
				// Read results from temp1 file
				readResponseCodeResults();	
				
				// Assembling all the data into one csv file
				progressBar.setValue(0);
				HelperFunctions.appendToTextPane("\n---------------\nAssembling results... (" + linkList.size() + " links in total)\n", textPaneMessages, null);
				
				// Insert data found from the response code threads
				for (int i=0; i<linkList.size(); i++) {
					linkList.get(i).setCode(responseCodeMap.get(linkList.get(i).getUrl()));
					linkList.get(i).setTitle(titleMap.get(linkList.get(i).getUrl()));
					
					progressBar.setValue((int)((double) i / (double) linkList.size() * 100));
					progressBar.setString(progressBar.getValue() + "% (" + (i) + "/" + linkList.size() + ")");
				}
				
				// Create excel file
				progressBar.setIndeterminate(true);
				progressBar.setString("Creating Excel Document...");
				HelperFunctions.appendToTextPane("\n---------------\nCreating Excel Document...\n", textPaneMessages, null);
				HelperFunctions.convertToExcel(linkList, foldername);
				progressBar.setIndeterminate(false);
			}

			// Return complete message
			HelperFunctions.appendToTextPane("\n---------------\n", textPaneMessages, null);
			if (btnStop.isEnabled()) {
				// Timing the end of scan
				endTime = System.nanoTime();
				duration = (endTime - startTime) / (1000000 * 1000);				
				
				// Deleting temp1 file
				File toDelete = new File("temp1.txt");
				if (toDelete.exists()) {
					toDelete.delete();
				}
				
				// Scan finish message on text pane and progress bar
				HelperFunctions.appendToTextPane("Scan Complete\n", textPaneMessages, null);
				HelperFunctions.appendToTextPane(linkList.size() + " links found in " + Math.round((double) duration / 60 * 100) / 100 + " min\n", textPaneMessages, null);
				
				// Changes buttons to let GUI know that scan is finished
				btnScan.setEnabled(true);
				btnStop.setEnabled(false);
				progressBar.setValue(100);
				progressBar.setString("Scan Complete");
				
				HelperFunctions.appendToTextPane("Check Excel file for search results\n(If scan results are much lower than expected, scan again with the same url but starting with \"https://\")\n", textPaneMessages, null);
			} else {
				// Scan stop message on text pane and progress bar
				HelperFunctions.appendToTextPane("Scan Stopped\n", textPaneMessages, null);
				btnScan.setEnabled(true);
				progressBar.setValue(0);
				progressBar.setIndeterminate(false);
				progressBar.setString("Scan Stopped");
			}
		}  else {
			// Scan stop message on text pane and progress bar
			HelperFunctions.appendToTextPane("\n---------------\n", textPaneMessages, null);
			HelperFunctions.appendToTextPane("Scan Stopped\n", textPaneMessages, null);
			btnScan.setEnabled(true);
			progressBar.setValue(0);
			progressBar.setIndeterminate(false);
			progressBar.setString("Scan Stopped");
		}
	}

	public void start() {
		if (t == null) {
			t = new Thread(this);
			t.start();
		}
	}
}
