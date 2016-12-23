package mitdown;

import java.util.Iterator;
import java.util.List;

public class Course {
	private String id;
	private String name;
	private String url;
	private String downurl;
	private boolean isBatch;
	private List<Feature> features;
	private List<Video> videos;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
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
	public String getDownurl() {
		return downurl;
	}
	public void setDownurl(String downurl) {
		this.downurl = downurl;
	}
	public List<Feature> getFeatures() {
		return features;
	}
	public void setFeatures(List<Feature> features) {
		this.features = features;
	}
	
	public boolean isBatch() {
		return isBatch;
	}
	public void setBatch(boolean isBatch) {
		this.isBatch = isBatch;
	}
	
	public List<Video> getVideos() {
		return videos;
	}
	public void setVideos(List<Video> videos) {
		this.videos = videos;
	}
	public boolean hasVideo(){
		Iterator<Feature> fiter=features.iterator();
		while (fiter.hasNext()) {
			Feature feature = (Feature) fiter.next();
			if("Video lectures".equalsIgnoreCase(feature.getName())){
				return true;
			}
		}
		return false;
	}
	public String getVideoUrl(){
		Iterator<Feature> fiter=features.iterator();
		while (fiter.hasNext()) {
			Feature feature = (Feature) fiter.next();
			if("Video lectures".equalsIgnoreCase(feature.getName())){
				return feature.getUrl();
			}
		}
		return null;
	}
	public String toHtml(){
		String html="<h1><a href=\""+url+"\">"+name+"</a></h1>\n";
		html+="<ul>Features";
		for (Feature feature : features) {
			html+=feature.toHtml()+"\n";
		}
		html+="</ul>\n";
		if(!isBatch){
			html+="<ul>Videos";
			for (Video video : videos) {
				html+=video.toHtml()+"\n";
			}
			html+="</ul>\n";
		}
		return html;
	}
}
