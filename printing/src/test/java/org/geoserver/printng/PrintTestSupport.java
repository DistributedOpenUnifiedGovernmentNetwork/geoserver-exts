/*
 */
package org.geoserver.printng;

import com.mockrunner.mock.web.MockHttpServletResponse;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.geoserver.data.util.IOUtils;
import org.geoserver.test.GeoServerTestSupport;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class PrintTestSupport extends GeoServerTestSupport {
    
    boolean showResult = false;
    
    protected void checkPDF(MockHttpServletResponse response) throws Exception {
        assertEquals("application/pdf",response.getContentType());
        InputStream stream = getBinaryInputStream(response);
        byte[] buf = new byte[8];
        stream.read(buf);
        assertEquals("%PDF-1.4",new String(buf));
    }
    
    protected void checkImage(MockHttpServletResponse response, String mimeType) throws IOException {
        assertEquals(mimeType, response.getContentType());
        ByteArrayInputStream bytes = getBinaryInputStream(response);
        try {
            BufferedImage image = ImageIO.read(bytes);
            assertNotNull(image);
            assertEquals(image.getWidth(), 512);
            assertEquals(image.getHeight(), 256);
        } catch (Throwable t) {
            t.printStackTrace();
            fail("Could not read image :" + t.getLocalizedMessage());
        }
        if (showResult) {
            bytes.reset();
            File res = new File(getDataDirectory().findDataRoot(),"image." + mimeType.split("/")[1]);
            IOUtils.copy(bytes, res);
            Desktop.getDesktop().open(res);
        }
    }
}