package blc;

public class Link {
	private String url;
	private String urlOrigin;
	private String linkText;
	private String linkTitle;
	private int responseCode;
	
	// Constructor
	public Link(String origin, String urlAddress, String text) {
		url = urlAddress;
		urlOrigin = origin;
		linkText = text;
		linkTitle = " ";
		responseCode = 404;
	}
	
	// Getter functions
	public String getUrl() {
		return url;
	}

	public String getOrigin() {
		return urlOrigin;
	}
	
	public String getLinkText() {
		return linkText;
	}
	
	public String getLinkTitle() {
		return linkTitle;
	}
	
	public int getCode() {
		return responseCode;
	}
	
	// Setter functions
	public void setUrl(String urlAddress) {
		url = urlAddress;
	}
	
	public void setOrigin(String origin) {
		urlOrigin = origin;
	}
	
	public void setLinkText(String text) {
		linkText = text;
	}
	
	public void setTitle(String title) {
		linkTitle = title;
	}
	
	public void setCode(int code) {
		responseCode = code;
	}
}
