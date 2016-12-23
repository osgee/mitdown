package mitdown;

public class Feature {
	private String name;
	private String url;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String toHtml(){
		return "<li><a href=\""+url+"\">"+name+"</a></li>";
	}
}
