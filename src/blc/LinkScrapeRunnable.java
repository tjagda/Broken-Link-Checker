package blc;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

class LinkScrapeRunnable implements Runnable {
	private Thread t;
	private String originalPage;
	private boolean finalThread;
	private int pageLimit;

	private CountDownLatch startSignal;
	private CountDownLatch endSignal;

	private volatile JTextPane textPaneMessages;
	private volatile JButton btnStop;
	private volatile JProgressBar progressBar;
	private SimpleAttributeSet error;

	private static volatile FileWriter outputFile;
	private static volatile List<String> linkList = new ArrayList<String>();
	private static volatile Set<String> linkSet = new HashSet<String>();
	private static volatile int currentIndex = 0;
	private static volatile int numOfWorkingLinks = 0;

	public LinkScrapeRunnable(String currentPage, boolean finalThread, int pageLimit, CountDownLatch startSignal, CountDownLatch endSignal, JTextPane textPaneMessages, JButton btnStop, JProgressBar progressBar) {
		this.originalPage = currentPage;
		this.finalThread = finalThread;
		this.pageLimit = pageLimit;
		this.startSignal = startSignal;
		this.endSignal = endSignal;
		this.textPaneMessages = textPaneMessages;
		this.btnStop = btnStop;
		this.progressBar = progressBar;

		if (finalThread)
			linkList.add(currentPage);

		error = new SimpleAttributeSet();
		StyleConstants.setForeground(error, Color.RED);
	}

	public static void resetVariables() {

		try {
			outputFile = new FileWriter(new File("temp.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		linkList = new ArrayList<String>();
		linkSet = new HashSet<String>();
		currentIndex = 0;
		numOfWorkingLinks = 0;
	}
	
	private synchronized void addToLinkList(String currentUrl, String originalDomain) {
		// Check page if it is within the domain and is an actual webpage before adding it to the link list
		if (!linkList.contains(currentUrl) && currentUrl.indexOf(originalDomain) == 0 && !(currentUrl.toLowerCase().contains(".pdf")
				|| currentUrl.toLowerCase().contains(".zip")
				|| currentUrl.toLowerCase().contains(".docx")
				|| currentUrl.toLowerCase().contains(".jpg")
				|| currentUrl.toLowerCase().contains(".jpeg")
				|| currentUrl.toLowerCase().contains(".png")
				|| currentUrl.toLowerCase().contains(".gif")
				|| currentUrl.toLowerCase().contains(".ashx")
				|| currentUrl.toLowerCase().contains(".eps"))) {
			linkList.add(currentUrl);
		}
	}

	private void getPageLinks(String originalDomain, String currentPage) {
		String currentUrl = "";
		String currentLinkText = "";
		Connection.Response response = null;

		try {
			// Try to make initial connection
			response = Jsoup.connect(currentPage).userAgent("Mozilla").timeout(60000).ignoreContentType(true).ignoreHttpErrors(true).execute();

			// Continue if valid response code
			if (!(response.statusCode() == 404 || response.statusCode() == 505 || response.statusCode() == 500)) {

				// Read from page and get all links
				Document doc = response.parse();
				Elements links = doc.select("a");

				// Loop through links and add any new unique links into
				// set(total list) and/or list (pages to go through)
				for (int i = 0; i < links.size() && linkSet.size() < pageLimit; i++) {
					currentUrl = links.get(i).absUrl("href");

					// Check if page is already in the link set and is a valid and new link
					if (!links.get(i).attr("href").toString().contains("#") && !(currentUrl.toLowerCase().contains("javascript:") || currentUrl.toLowerCase().contains("mailto:") || currentUrl.toLowerCase().contains("tel:") || currentUrl.length() <= 1)) {
						
						// To hashset to keep track of page count
						linkSet.add(currentUrl);

						// Get link text for link
						currentLinkText = links.get(i).text();

						// Write result to file
						HelperFunctions.writeToFile(currentPage + "\t" + currentUrl + "\t" + currentLinkText + "\n", outputFile, textPaneMessages);
						addToLinkList(currentUrl, originalDomain);
					}
				}
			} else {
				HelperFunctions.appendToTextPane("ERROR: Invalid response code (" + response.statusCode() + ")\n\n", textPaneMessages, error);
			}
		} catch (IOException e) {
			HelperFunctions.appendToTextPane("ERROR: " + e.getMessage() + "\n\n", textPaneMessages, error);
		}
	}

	public void run() {
		int index;

		// Wait for main thread to give signal to start
		try {
			startSignal.await();
		} catch (InterruptedException e) {
			HelperFunctions.appendToTextPane("ERROR: " + e.getMessage() + "\n", textPaneMessages, error);
		}

		// Loop until all work is done and there are no more threads finding links
		while ((currentIndex < linkList.size() || numOfWorkingLinks > 0) && btnStop.isEnabled() && linkSet.size() < pageLimit) {
			// Wait for a little bit if there are still threads finding links even though there are no more links to check at the moment
			if (currentIndex >= linkList.size() && numOfWorkingLinks > 0) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					HelperFunctions.appendToTextPane("ERROR: " + e.getMessage() + "\n", textPaneMessages, error);
				}
			} else {
				// Increase number to let other threads know to wait and that there is at least one thread finding links
				numOfWorkingLinks++;

				// Capture current index
				index = currentIndex++;
				
				// Scan page for links if either limit is not reached
				if (index < linkList.size() && linkSet.size() < pageLimit) {
					progressBar.setString("On page " + (index + 1) + " | " + linkList.size() + " pages found to scan in total");
					HelperFunctions.appendToTextPane("Scanning... \t" + linkList.get(index) + "\n", textPaneMessages, null);
					getPageLinks(originalPage, linkList.get(index));
				}

				// Decrease number, if 0 then other threads can finish and terminate
				numOfWorkingLinks--;
			}
		}

		if (btnStop.isEnabled()) {
			// Final thread puts all links found into a temporary text file to read later by main thread
			if (finalThread) {
				try {
					progressBar.setString("Please Wait...");
					Thread.sleep(5000);
					outputFile.close();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}

		// Finish thread by signaling the end countodown
		endSignal.countDown();
	}

	public void start() {
		if (t == null) {
			t = new Thread(this);
			t.start();
		}
	}
}
