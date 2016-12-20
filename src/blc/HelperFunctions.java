package blc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

public class HelperFunctions {
	public static void appendToTextPane(String message, JTextPane textPaneMessages, SimpleAttributeSet style) {
		try {
			// Insert message to text pane
			textPaneMessages.getDocument().insertString(textPaneMessages.getDocument().getLength(),	message, style);
			
			// Move position to the end of the document
			textPaneMessages.setCaretPosition(textPaneMessages.getDocument().getLength());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static <T> List<List<T>> chopped(List<T> list, final int L) {
		// Create a list of lists
		List<List<T>> parts = new ArrayList<List<T>>();
		
		// Set integer as the given list's size
		final int N = list.size();
		
		// Create lists within parts and add subsets of the given list
		for (int i = 0; i < N; i += L) {
			parts.add(new ArrayList<T>(list.subList(i, Math.min(N, i + L))));
		}
	
		return parts;
	}
	
	public static void convertToExcel(List<Link> linkList, String foldername) {
		// Get current calender info
		DateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh.mm a");
		Calendar cal = Calendar.getInstance();

		// Create a new workbook + sheet
		SXSSFWorkbook wb = new SXSSFWorkbook();
		Sheet sheet = wb.createSheet("Results");

		// Set column widths
		sheet.setColumnWidth(0, 80 * 256);
		sheet.setColumnWidth(1, 80 * 256);
		sheet.setColumnWidth(2, 30 * 256);
		sheet.setColumnWidth(3, 50 * 256);
		sheet.setColumnWidth(4, 20 * 256);

		// Setup styles
		CellStyle titleCells = wb.createCellStyle();
		CellStyle titleCenterCells = wb.createCellStyle();
		CellStyle centerCells = wb.createCellStyle();
		CellStyle invalidCells = wb.createCellStyle();
		CellStyle invalidCenterCells = wb.createCellStyle();

		// Setup alignments for some styles
		titleCenterCells.setAlignment(CellStyle.ALIGN_CENTER);
		centerCells.setAlignment(CellStyle.ALIGN_CENTER);
		invalidCenterCells.setAlignment(CellStyle.ALIGN_CENTER);

		// Setup colours of cells
		invalidCells.setFillForegroundColor(IndexedColors.RED.getIndex());
		invalidCells.setFillPattern(CellStyle.SOLID_FOREGROUND);
		invalidCenterCells.setFillForegroundColor(IndexedColors.RED.getIndex());
		invalidCenterCells.setFillPattern(CellStyle.SOLID_FOREGROUND);

		// Setup bold and underline
		Font titleFont = wb.createFont();
		titleFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		titleFont.setUnderline(HSSFFont.U_SINGLE);

		// Setup colour of font
		Font invalidFont = wb.createFont();
		invalidFont.setColor(IndexedColors.WHITE.getIndex());

		// Set fonts
		titleCells.setFont(titleFont);
		titleCenterCells.setFont(titleFont);
		invalidCells.setFont(invalidFont);
		invalidCenterCells.setFont(invalidFont);

		// For loop to insert values into the new Excel file using the lines read from the temp file
		Row row;
		for (int i = 0; i < linkList.size()+1; i++) {
			row = sheet.createRow(i);

			if (i == 0) {
				// First row is always the same values
				row.createCell(0).setCellValue("Url");
				row.createCell(1).setCellValue("Origin");
				row.createCell(2).setCellValue("Text");
				row.createCell(3).setCellValue("Title");
				row.createCell(4).setCellValue("Response Code");

				// Add special styles to the header row
				row.getCell(0).setCellStyle(titleCells);
				row.getCell(1).setCellStyle(titleCells);
				row.getCell(2).setCellStyle(titleCells);
				row.getCell(3).setCellStyle(titleCells);
				row.getCell(4).setCellStyle(titleCenterCells);
			} else {
				// A copy of the iterator integer i but 1 less, so that it will loop through every linkList element
				int j = i-1;
				
				// Add the collected values
				row.createCell(0).setCellValue(linkList.get(j).getUrl());
				row.createCell(1).setCellValue(linkList.get(j).getOrigin());
				row.createCell(2).setCellValue(linkList.get(j).getLinkText());
				row.createCell(3).setCellValue(linkList.get(j).getLinkTitle());
				row.createCell(4).setCellValue(linkList.get(j).getCode());

				// Set different styles if link is broken
				if (!(linkList.get(j).getCode() == 404 || linkList.get(j).getCode() == 505)) {
					// Valid: only center the response code
					row.getCell(4).setCellStyle(centerCells);
				} else {
					// Invalid: all cells in this row are now red with white font & response code is centered
					row.getCell(0).setCellStyle(invalidCells);
					row.getCell(1).setCellStyle(invalidCells);
					row.getCell(2).setCellStyle(invalidCells);
					row.getCell(3).setCellStyle(invalidCells);
					row.getCell(4).setCellStyle(invalidCenterCells);
				}
			}
		}

		// Freeze the header row
		sheet.createFreezePane(0, 1, 0, 1);

		// Set filters for header row
		sheet.setAutoFilter(new CellRangeAddress(0, linkList.size(), 0, 4));

		// Create file and save
		try {
			FileOutputStream fileOut = new FileOutputStream(foldername + "\\results (" + dateFormat.format(cal.getTime()) + ").xlsx");
			wb.write(fileOut);
			fileOut.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void disableSslVerification() {
	    try
	    {
	        // Create a trust manager that does not validate certificate chains
	        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
	            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                return null;
	            }
	            public void checkClientTrusted(X509Certificate[] certs, String authType) {
	            }
	            public void checkServerTrusted(X509Certificate[] certs, String authType) {
	            }
	        }
	        };

	        // Install the all-trusting trust manager
	        SSLContext sc = SSLContext.getInstance("SSL");
	        sc.init(null, trustAllCerts, new java.security.SecureRandom());
	        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

	        // Create all-trusting host name verifier
	        HostnameVerifier allHostsValid = new HostnameVerifier() {
	            public boolean verify(String hostname, SSLSession session) {
	                return true;
	            }
	        };

	        // Install the all-trusting host verifier
	        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	    } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    } catch (KeyManagementException e) {
	        e.printStackTrace();
	    }
	}

	public static int getResponseCode(String currentURL) {
		int code = 404;
	
		try {	
			// Try Https connection without having the certificate in the truststore
			URL url = new URL(currentURL);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
			code = connection.getResponseCode();
			connection.disconnect();
		} catch (Exception e) {
			// Try a regular Http connection
			try {
				URL url = new URL(currentURL);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.connect();
				code = connection.getResponseCode();
				connection.disconnect();
			} catch (Exception e2) {
			}
		}
		return code;
	}

	public static boolean isValidLink(String currentPage) {
		return !(currentPage.toLowerCase().contains("javascript:") || currentPage.toLowerCase().contains("mailto:")
				|| currentPage.toLowerCase().contains("tel:") || currentPage.toLowerCase().contains(".pdf")
				|| currentPage.toLowerCase().contains(".zip") || currentPage.toLowerCase().contains(".docx")
				|| currentPage.toLowerCase().contains(".jpg") || currentPage.toLowerCase().contains(".jpeg")
				|| currentPage.toLowerCase().contains(".png") || currentPage.toLowerCase().contains(".gif")
				|| currentPage.toLowerCase().contains(".ashx") || currentPage.toLowerCase().contains(".eps"));
	}
	
	public static List<String> noDuplVersion(List<String> urlList) {
		// Iteratively removing duplicates of urlList by creating a new list
		List<String> noDuplList = new ArrayList<>();
		for (int i=0; i<urlList.size(); i++) {
			if (!noDuplList.contains(urlList.get(i))) {
				noDuplList.add(urlList.get(i));
			}
		}
		return noDuplList;
	}

	public static void takeScreenshot(WebDriver driver, String URL, String foldername) {
		File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		String newURL = URL.replace("://", " ").replace("/", " ");

		// Change all special characters that cannot be used in filenames
		newURL = newURL.replace("\\", " ").replace("?", " ").replace(":", " ").replace("*", " ").replace("\\", " ")
				.replace("<", " ").replace(">", " ").replace("|", " ").replace("\"", " ");

		try {
			// Determine what type of browser is being used
			if (driver instanceof InternetExplorerDriver) {
				new File(foldername + "\\IE\\" + newURL + ".png").delete();
				FileUtils.copyFile(scrFile, new File(foldername + "\\IE\\" + newURL + ".png"));

			} else if (driver instanceof FirefoxDriver) {
				new File(foldername + "\\FF\\" + newURL + ".png").delete();
				FileUtils.copyFile(scrFile, new File(foldername + "\\FF\\" + newURL + ".png"));

			} else if (driver instanceof ChromeDriver) {
				new File(foldername + "\\chrome\\" + newURL + ".png").delete();
				FileUtils.copyFile(scrFile, new File(foldername + "\\chrome\\" + newURL + ".png"));

			} else {
				// Take screenshot even though browser is not known
				new File(foldername + "\\" + newURL + ".png").delete();
				FileUtils.copyFile(scrFile, new File(foldername + "\\" + newURL + ".png"));
			}
		} catch (IOException e) {
			e.printStackTrace();;
		}
	}

	public synchronized static void writeToFile(String message, FileWriter outputFile, JTextPane textPaneMessages) throws OutOfMemoryError {
		try {
			outputFile.write(message);
			outputFile.flush();
		} catch (IOException e) {
			e.printStackTrace();
			appendToTextPane("ERROR: " + e.getMessage() + "\n", textPaneMessages, null);
		} catch (OutOfMemoryError e) {
			throw e;
		}
	}
}
