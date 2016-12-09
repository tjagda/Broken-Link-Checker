package blc;

import java.awt.Color;
import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.jsoup.Jsoup;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

class ResponseCodeRunnable implements Runnable {
	private Thread t;

	private String foldername;
	private volatile FileWriter outputFile;
	private char browserChoice;
	private List<String> linkList;

	private volatile JTextPane textPaneMessages;
	private volatile JButton btnStop;
	private volatile JProgressBar progressBar;
	private SimpleAttributeSet invalid;

	private CountDownLatch startSignal;
	private CountDownLatch endSignal;

	private volatile static int counter = 1;
	private volatile static int total = 0;

	public ResponseCodeRunnable(String foldername, List<String> linkList, FileWriter outputFile, char browserChoice, int total, JTextPane textPaneMessages, JButton btnStop, JProgressBar progressBar, CountDownLatch startSignal, CountDownLatch endSignal) {
		this.foldername = foldername;
		this.linkList = linkList;
		this.outputFile = outputFile;
		this.browserChoice = browserChoice;

		this.textPaneMessages = textPaneMessages;
		this.btnStop = btnStop;
		this.progressBar = progressBar;

		invalid = new SimpleAttributeSet();
		StyleConstants.setBackground(invalid, Color.red);
		StyleConstants.setForeground(invalid, Color.white);

		this.startSignal = startSignal;
		this.endSignal = endSignal;

		ResponseCodeRunnable.total = total;
	}

	public static void resetVariables() {
		ResponseCodeRunnable.counter = 1;
		ResponseCodeRunnable.total = 0;
	}

	private void initiateScreenshots() {
		WebDriver driver;
		
		try {
			
			// Setup different browsers depending on which button they checked off
			switch (browserChoice) {
			
			// Internet Explorer
			case 'i':
				// Setup IE
				driver = new InternetExplorerDriver();
				driver.manage().window().maximize();
				driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

				// Run loop to enter each page of this thread's list and take a picture
				for (int i = 0; i < linkList.size() && btnStop.isEnabled(); i++) {
					if (HelperFunctions.isValidLink(linkList.get(i))) {
						try {
							driver.get(linkList.get(i));
						} catch (Exception e) {
						}
						HelperFunctions.takeScreenshot(driver, linkList.get(i), foldername);
					}
				}

				driver.quit();
				break;
				
			// Firefox
			case 'f':
				// Setup FF
				driver = new FirefoxDriver();
				driver.manage().window().maximize();
				driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

				// Run loop to enter each page of this thread's list and take a picture
				for (int i = 0; i < linkList.size() && btnStop.isEnabled(); i++) {
					if (HelperFunctions.isValidLink(linkList.get(i))) {
						try {
							driver.get(linkList.get(i));
						} catch (Exception e) {
						}

						HelperFunctions.takeScreenshot(driver, linkList.get(i), foldername);
					}
				}

				driver.quit();
				break;
				
			// Chrome
			case 'c':		
				// Setup chrome
				driver = new ChromeDriver();
				driver.manage().window().maximize();
				driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

				// Run loop to enter each page of this thread's list and take a picture
				for (int i = 0; i < linkList.size() && btnStop.isEnabled() == true; i++) {
					if (HelperFunctions.isValidLink(linkList.get(i))) {
						try {
							driver.get(linkList.get(i));
						} catch (Exception e) {
						}

						HelperFunctions.takeScreenshot(driver, linkList.get(i), foldername);
					}
				}

				driver.quit();
				break;
			
			// None of the above (do nothing)
			default:
				break;
			}
		} catch (Exception e) {
			HelperFunctions.appendToTextPane("ERROR: Something is wrong with your browser. It is either not installed correctly or its webdriver is not found.\n", textPaneMessages, null);
		}
	}

	public void run() {
		int code;
		int currentCounter;
		String url;
		String title;

		// Wait for starting signal from MainBLCRunnable (Main program thread)
		try {
			startSignal.await();
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}

		// Loop through given list of links
		for (int i = 0; i < linkList.size() && btnStop.isEnabled() == true; i++) {

			// Get URL and response code
			url = linkList.get(i);
			code = HelperFunctions.getResponseCode(url);
			
			// Get title
			title = "";
			try {
				title = Jsoup.connect(url).userAgent("Mozilla").timeout(50000).ignoreContentType(true).ignoreHttpErrors(true).get().title();
			} catch (Exception e) {
			}

			// Write to xls file + change progress bar + print to text pane
			currentCounter = counter++;
			if (!((code == 404) || (code == 505) || (code == 500))) {
				progressBar.setValue((int) ((double) currentCounter / (double) ResponseCodeRunnable.total * 100));
				progressBar.setString(progressBar.getValue() + "% (" + (currentCounter) + "/" + ResponseCodeRunnable.total + ")");
				HelperFunctions.appendToTextPane(url + "\nResponse code is: " + code + "\n\n", textPaneMessages, null);
				HelperFunctions.writeToFile(url + "\t" + title + "\t" + code + "\n", outputFile, textPaneMessages);
			} else {
				progressBar.setValue((int) ((double) currentCounter / (double) ResponseCodeRunnable.total * 100));
				progressBar.setString(progressBar.getValue() + "% (" + (currentCounter) + "/" + ResponseCodeRunnable.total + ")");
				HelperFunctions.appendToTextPane(url + "\nResponse code is: " + code + "\n\n", textPaneMessages, invalid);
				HelperFunctions.writeToFile(url + "\t" + title + "\t" + code + "\n", outputFile, textPaneMessages);
			}
		}

		// If scan is not stopped, initiate screenshots function
		if (btnStop.isEnabled()) {
			initiateScreenshots();
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
