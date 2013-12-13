
package info.freelibrary.djatoka.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import info.freelibrary.djatoka.iiif.Constants;

import nu.xom.Document;
import nu.xom.Element;

import org.junit.Test;

public class ImageInfoTest {

    @Test
    public void testImageInfo() {
        ImageInfo imageInfo = new ImageInfo("id", 24, 42);

        assertEquals("id", imageInfo.getIdentifier());
        assertEquals(24, imageInfo.getHeight());
        assertEquals(42, imageInfo.getWidth());
    }

    @Test
    public void testAddFormat() {
        ImageInfo imageInfo = new ImageInfo("id", 24, 42);
        imageInfo.addFormat("jpg");

        assertEquals("jpg", imageInfo.getFormats().get(0));
    }

    @Test
    public void testToXML() {
        ImageInfo imageInfo = new ImageInfo("id", 24, 42);
        Document xml = new Document(new Element("info", Constants.IIIF_NS));
        Element identifier = new Element("identifier", Constants.IIIF_NS);
        Element height = new Element("height", Constants.IIIF_NS);
        Element width = new Element("width", Constants.IIIF_NS);

        identifier.appendChild("id");
        height.appendChild("24");
        width.appendChild("42");

        // The order below is significant
        xml.getRootElement().appendChild(identifier);
        xml.getRootElement().appendChild(width);
        xml.getRootElement().appendChild(height);

        assertEquals(xml.toXML(), imageInfo.toXML());
    }

    @Test
    public void testToString() {
        ImageInfo imageInfo = new ImageInfo("id", 24, 42);
        Document xml = new Document(new Element("info", Constants.IIIF_NS));
        Element identifier = new Element("identifier", Constants.IIIF_NS);
        Element height = new Element("height", Constants.IIIF_NS);
        Element width = new Element("width", Constants.IIIF_NS);

        identifier.appendChild("id");
        height.appendChild("24");
        width.appendChild("42");

        // The order below is significant
        xml.getRootElement().appendChild(identifier);
        xml.getRootElement().appendChild(width);
        xml.getRootElement().appendChild(height);

        assertEquals(xml.toXML(), imageInfo.toString());
    }

    @Test
    public void testToJSON() {
        // TODO
        fail("Not yet implemented");
    }

}
