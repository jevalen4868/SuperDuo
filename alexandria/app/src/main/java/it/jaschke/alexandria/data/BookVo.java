package it.jaschke.alexandria.data;

/**
 * Created by jeremyvalenzuela on 11/7/15.
 */
public class BookVo {
    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }

    public String getVolumeInfo() {
        return volumeInfo;
    }

    public void setVolumeInfo(String volumeInfo) {
        this.volumeInfo = volumeInfo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public String getImageLinks() {
        return imageLinks;
    }

    public void setImageLinks(String imageLinks) {
        this.imageLinks = imageLinks;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getBookJson() {
        return bookJson;
    }

    public void setBookJson(String bookJson) {
        this.bookJson = bookJson;
    }

    private String items;
    private String volumeInfo;
    private String title;
    private String subtitle;
    private String authors;
    private String description;
    private String categories;
    private String imageLinks;
    private String thumbnail;
    private String bookJson;

}
